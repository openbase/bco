package org.openbase.bco.dal.remote.layer.service;

/*
 * #%L
 * BCO DAL Remote
 * %%
 * Copyright (C) 2014 - 2019 openbase.org
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-3.0.html>.
 * #L%
 */

import com.google.protobuf.ByteString;
import com.google.protobuf.Descriptors.EnumValueDescriptor;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import com.google.protobuf.ProtocolMessageEnum;
import org.openbase.bco.authentication.lib.AuthenticationClientHandler;
import org.openbase.bco.authentication.lib.EncryptionHelper;
import org.openbase.bco.authentication.lib.SessionManager;
import org.openbase.bco.dal.lib.action.ActionDescriptionProcessor;
import org.openbase.bco.dal.lib.action.ActionIdGenerator;
import org.openbase.bco.dal.lib.layer.service.*;
import org.openbase.bco.dal.lib.layer.unit.UnitRemote;
import org.openbase.bco.dal.remote.layer.unit.Units;
import org.openbase.bco.registry.lib.util.UnitConfigProcessor;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.jul.exception.*;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.exception.printer.LogLevel;
import org.openbase.jul.extension.type.processing.ScopeProcessor;
import org.openbase.jul.extension.type.processing.TimestampProcessor;
import org.openbase.jul.pattern.CompletableFutureLite;
import org.openbase.jul.pattern.ObservableImpl;
import org.openbase.jul.pattern.Observer;
import org.openbase.jul.pattern.controller.Remote;
import org.openbase.jul.pattern.provider.DataProvider;
import org.openbase.jul.schedule.FutureProcessor;
import org.openbase.jul.schedule.GlobalCachedExecutorService;
import org.openbase.jul.schedule.SyncObject;
import org.openbase.type.domotic.action.ActionDescriptionType.ActionDescription;
import org.openbase.type.domotic.action.ActionDescriptionType.ActionDescription.Builder;
import org.openbase.type.domotic.authentication.AuthenticatedValueType.AuthenticatedValue;
import org.openbase.type.domotic.service.ServiceConfigType.ServiceConfig;
import org.openbase.type.domotic.service.ServiceTemplateType.ServiceTemplate;
import org.openbase.type.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import org.openbase.type.domotic.state.ConnectionStateType.ConnectionState;
import org.openbase.type.domotic.state.EnablingStateType.EnablingState.State;
import org.openbase.type.domotic.unit.UnitConfigType.UnitConfig;
import org.openbase.type.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;
import org.openbase.type.timing.TimestampType.Timestamp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.openbase.bco.dal.lib.layer.service.ServiceStateProcessor.*;

/**
 * @param <S>  generic definition of the overall service type for this remote.
 * @param <ST> the corresponding state for the service type of this remote.
 *
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public abstract class AbstractServiceRemote<S extends Service, ST extends Message> implements ServiceRemote<S, ST> {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    public static final ActionIdGenerator ACTION_ID_GENERATOR = new ActionIdGenerator();

    private boolean active;
    private boolean shutdownInitiated = false;
    private boolean filterInfrastructureUnits;
    private final ServiceType serviceType;
    private ServiceTemplate serviceTemplate;
    private long connectionPing;
    private final Class<ST> serviceDataClass;
    private final Map<String, UnitRemote<?>> unitRemoteMap;
    private final Map<String, UnitRemote<?>> disabledUnitRemoteMap;
    private final Map<String, UnitRemote<?>> infrastructureUnitMap;
    private final Map<UnitType, List<S>> unitRemoteTypeMap;
    private final Map<String, S> serviceMap;
    private final Observer dataObserver;
    private final Observer unitConfigObserver;
    private final Observer connectionStateObserver;
    protected final ObservableImpl<DataProvider<ST>, ST> serviceStateObservable = new ObservableImpl<>();
    private final ObservableImpl<ServiceStateProvider<ST>, ST> serviceStateProviderObservable = new ObservableImpl<>();
    private final SyncObject syncObject = new SyncObject("ServiceStateComputationLock");
    private final SyncObject maintainerLock = new SyncObject("MaintainerLock");
    private final SyncObject connectionStateLock = new SyncObject("ConnectionStateLock");
    protected Object maintainer;
    private ServiceRemoteManager<?> serviceRemoteManager;

    /**
     * AbstractServiceRemote constructor.
     * Activates filtering infrastructure units per default.
     *
     * @param serviceType      The remote service type.
     * @param serviceDataClass The service data class.
     */
    public AbstractServiceRemote(final ServiceType serviceType, final Class<ST> serviceDataClass) {
        this(serviceType, serviceDataClass, true);
    }

    /**
     * AbstractServiceRemote constructor.
     *
     * @param serviceType               The remote service type.
     * @param serviceDataClass          The service data class.
     * @param filterInfrastructureUnits Flag determining if units marked as infrastructure will be used by this service remote.
     */
    public AbstractServiceRemote(final ServiceType serviceType, final Class<ST> serviceDataClass, final boolean filterInfrastructureUnits) {
        this.serviceType = serviceType;
        this.serviceDataClass = serviceDataClass;
        this.filterInfrastructureUnits = filterInfrastructureUnits;
        this.unitRemoteMap = new HashMap<>();
        this.unitRemoteTypeMap = new HashMap<>();
        this.disabledUnitRemoteMap = new HashMap<>();
        this.infrastructureUnitMap = new HashMap<>();
        this.serviceMap = new HashMap<>();
        this.dataObserver = (source, data) -> {
            try {
                updateServiceState();
            } catch (CouldNotPerformException ex) {
                ExceptionPrinter.printHistory("Initial service state computation failed. This can be the case if any required date is not available yet.", ex, logger, LogLevel.DEBUG);
            }
        };
        this.unitConfigObserver = (source, data) -> {
            final UnitConfig unitConfig = (UnitConfig) data;
            updateIgnoredUnitMaps(unitConfig.getId(), disabledUnitRemoteMap, unitConfig.getEnablingState().getValue() == State.DISABLED);
            if (filterInfrastructureUnits) {
                updateIgnoredUnitMaps(unitConfig.getId(), infrastructureUnitMap, Units.getUnit(unitConfig, false).isInfrastructure());
            }
        };
        this.connectionStateObserver = (source, data) -> {
            synchronized (connectionStateLock) {
                connectionStateLock.notifyAll();
            }
        };
        this.serviceStateObservable.setExecutorService(GlobalCachedExecutorService.getInstance().getExecutorService());
        this.serviceStateProviderObservable.setExecutorService(GlobalCachedExecutorService.getInstance().getExecutorService());
    }

    /**
     * Compute the service state of this service collection if an underlying
     * service changes.
     *
     * @return the computed server state is returned.
     *
     * @throws CouldNotPerformException if an underlying service throws an
     *                                  exception
     */
    protected abstract ST computeServiceState() throws CouldNotPerformException;

    /**
     * Compute the current service state and notify observer if the update was successful.
     *
     * @throws CouldNotPerformException if the computation fails
     */
    private void updateServiceState() throws CouldNotPerformException {
        final ST serviceState;
        synchronized (syncObject) {
            serviceState = computeServiceState();
        }
        serviceStateObservable.notifyObservers(serviceState);
        serviceStateProviderObservable.notifyObservers(serviceState);
        assert serviceStateObservable.isValueAvailable();
    }

    /**
     * @return the current service state
     *
     * @throws NotAvailableException if the service state data has not been set at
     *                               least once.
     */
    @Override
    public ST getData() throws NotAvailableException {
        if (!serviceStateObservable.isValueAvailable()) {
            throw new NotAvailableException("Data");
        }
        return serviceStateObservable.getValue();
    }

    @Override
    public String getId() throws NotAvailableException {
        try {
            return getServiceRemoteManager().getResponsibleUnit().getId();
        } catch (NotAvailableException ex) {
            throw new NotAvailableException("ServiceRemote", "Id", ex);
        }
    }

    /**
     * Add an observer to get notifications when the service state changes.
     *
     * @param observer the observer which is notified
     */
    @Override
    public void addDataObserver(final Observer<DataProvider<ST>, ST> observer) {
        serviceStateObservable.addObserver(observer);
    }

    /**
     * Remove an observer for the service state.
     *
     * @param observer the observer which has been registered
     */
    @Override
    public void removeDataObserver(final Observer<DataProvider<ST>, ST> observer) {
        serviceStateObservable.removeObserver(observer);
    }

    @Override
    public void addServiceStateObserver(final ServiceType serviceType, final Observer<ServiceStateProvider<ST>, ST> observer) {
        try {
            if (serviceType != getServiceType()) {
                throw new VerificationFailedException("ServiceType[" + serviceType.name() + "] is not compatible with " + this);
            }
            serviceStateProviderObservable.addObserver(observer);
        } catch (final CouldNotPerformException ex) {
            ExceptionPrinter.printHistory(new CouldNotPerformException("Could not add service state observer!", ex), logger);
        }
    }

    @Override
    public void removeServiceStateObserver(final ServiceType serviceType, final Observer<ServiceStateProvider<ST>, ST> observer) {
        try {
            if (serviceType != getServiceType()) {
                throw new VerificationFailedException("ServiceType[" + serviceType.name() + "] is not compatible with " + this);
            }
            serviceStateProviderObservable.removeObserver(observer);
        } catch (final CouldNotPerformException ex) {
            ExceptionPrinter.printHistory(new CouldNotPerformException("Could not remove service state observer!", ex), logger, LogLevel.WARN);
        }
    }

    @Override
    public Class<ST> getDataClass() {
        return serviceDataClass;
    }

    /**
     * Method request the data of all internal unit remotes.
     *
     * @param failOnError flag decides if an exception should be thrown in case one data request fails.
     *
     * @return the recalculated server state data based on the newly requested data.
     */
    @Override
    public Future<ST> requestData(final boolean failOnError) {
        final CompletableFutureLite<ST> requestDataFuture = new CompletableFutureLite<>();
        GlobalCachedExecutorService.submit(() -> {
            try {
                final List<Future> taskList = new ArrayList<>();
                MultiException.ExceptionStack exceptionStack = null;
                for (final Remote remote : getInternalUnits()) {
                    taskList.add(remote.requestData());
                }
                boolean noResponse = true;
                for (final Future task : taskList) {
                    try {
                        task.get();
                        noResponse = false;
                    } catch (ExecutionException ex) {
                        MultiException.push(task, ex, exceptionStack);
                    }
                }

                try {
                    MultiException.checkAndThrow(() -> "Could not request status of all internal remotes!", exceptionStack);
                } catch (MultiException ex) {
                    if (failOnError || noResponse) {
                        throw ex;
                    }
                    ExceptionPrinter.printHistory(new CouldNotPerformException("Could not request data of all internal unit remotes!", ex), logger, LogLevel.WARN);
                }
                requestDataFuture.complete(getData());
            } catch (InterruptedException | CouldNotPerformException ex) {
                requestDataFuture.completeExceptionally(ex);
            }
        });

        return requestDataFuture;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Note: Method blocks until the template registry is available to resolve further properties.
     *
     * @param unitConfig {@inheritDoc}
     *
     * @throws InitializationException {@inheritDoc}
     * @throws InterruptedException    {@inheritDoc}
     */
    @Override
    public void init(final UnitConfig unitConfig) throws InitializationException, InterruptedException {
        try {
            if (unitRemoteMap.containsKey(unitConfig.getId()) || disabledUnitRemoteMap.containsKey(unitConfig.getId()) || infrastructureUnitMap.containsKey(unitConfig.getId())) {
                // skip duplicated units
                return;
            }

            verifyMaintainability();

            if (!verifyServiceCompatibility(unitConfig, serviceType)) {
                throw new NotSupportedException("UnitTemplate[" + serviceType.name() + "]", unitConfig.getLabel());
            }

            final UnitRemote<?> unitRemote = Units.getUnit(unitConfig, false);

            if (filterInfrastructureUnits && unitRemote.isInfrastructure()) {
                // filter is on so put on map and ignore for now
                infrastructureUnitMap.put(unitConfig.getId(), unitRemote);
                return;
            }

            if (!unitRemoteTypeMap.containsKey(unitRemote.getUnitType())) {
                unitRemoteTypeMap.put(unitRemote.getUnitType(), new ArrayList());
                for (UnitType superType : Registries.getTemplateRegistry(true).getSuperUnitTypes(unitRemote.getUnitType())) {
                    if (!unitRemoteTypeMap.containsKey(superType)) {
                        unitRemoteTypeMap.put(superType, new ArrayList<>());
                    }
                }
            }

            unitRemote.addConfigObserver(unitConfigObserver);

            if (unitConfig.getEnablingState().getValue() == State.DISABLED) {
                disabledUnitRemoteMap.put(unitConfig.getId(), unitRemote);
                return;
            }

            addUnitRemoteToActiveMaps(unitRemote);

            if (active) {
                unitRemote.addDataObserver(dataObserver);
                unitRemote.addConnectionStateObserver(connectionStateObserver);
            }
        } catch (CouldNotPerformException ex) {
            throw new InitializationException(this, ex);
        }
    }

    /**
     * Add a unit remote to all according internal maps for active units. Active means that the remote will be
     * considered by this service remote.
     * This includes the unitRemoteMap, the serviceMap, to the unitRemoteTypeMap for each unit type and super type.
     *
     * @param unitRemote the unit remote added to the internal maps
     *
     * @throws CouldNotPerformException thrown if the unit does not implement the service specified by this service remote
     */
    private void addUnitRemoteToActiveMaps(final UnitRemote<?> unitRemote) throws CouldNotPerformException {
        try {
            serviceMap.put(unitRemote.getId(), (S) unitRemote);
            unitRemoteTypeMap.get(unitRemote.getUnitType()).add((S) unitRemote);
            for (UnitType superType : Registries.getTemplateRegistry().getSuperUnitTypes(unitRemote.getUnitType())) {
                unitRemoteTypeMap.get(superType).add((S) unitRemote);
            }
        } catch (ClassCastException ex) {
            throw new NotSupportedException("ServiceInterface[" + serviceType.name() + "]", unitRemote, "Remote does not implement the service interface!", ex);
        }
        unitRemoteMap.put(unitRemote.getId(), unitRemote);
    }

    /**
     * Remove a unit remote from all according internal maps for active units. Active means that the remote will be
     * considered by this service remote.
     * This includes the unitRemoteMap, the serviceMap, to the unitRemoteTypeMap for each unit type and super type.
     *
     * @param unitId the id of the unit remote to be removed
     *
     * @return the removed unit remote
     *
     * @throws CouldNotPerformException thrown if the super unit types of the remote could not be resolved
     */
    private UnitRemote<?> removeUnitRemoteFromActiveMaps(final String unitId) throws CouldNotPerformException {
        // unit is now ignored
        final UnitRemote<?> unitRemote = unitRemoteMap.remove(unitId);

        // remove unit from maps
        serviceMap.remove(unitRemote.getId());
        unitRemoteTypeMap.get(unitRemote.getUnitType()).remove(unitRemote);
        for (UnitType superType : Registries.getTemplateRegistry().getSuperUnitTypes(unitRemote.getUnitType())) {
            unitRemoteTypeMap.get(superType).remove(unitRemote);
        }

        return unitRemote;
    }

    /**
     * Update a map containing ignored units. If the unit remote specified by the unit id is inside
     * the ignoredUnitMap and it is now longer ignored it will be removed from the map and added to the internal active maps.
     * If the unit remote is not contained in the ignoredUnitMap but should now be ignored it will be removed from
     * the active unit maps and added to the ignored map.
     *
     * @param unitId               the id of the unit tested
     * @param ignoredUnitRemoteMap the map containing ignored units
     * @param ignored              flag determining if the unit should now be ignored
     *
     * @throws CouldNotPerformException
     */
    private void updateIgnoredUnitMaps(final String unitId, final Map<String, UnitRemote<?>> ignoredUnitRemoteMap, final boolean ignored) throws CouldNotPerformException {
        if (ignoredUnitRemoteMap.containsKey(unitId) && !ignored) {
            // unit is now longer ignored
            final UnitRemote<?> unitRemote = ignoredUnitRemoteMap.remove(unitId);

            // add unit to maps
            addUnitRemoteToActiveMaps(unitRemote);
        } else if (!ignoredUnitRemoteMap.containsKey(unitId) && ignored) {
            // unit is now ignored
            final UnitRemote<?> unitRemote = removeUnitRemoteFromActiveMaps(unitId);

            // put on filtered unit map
            ignoredUnitRemoteMap.put(unitRemote.getId(), unitRemote);
        }
    }

    /**
     * Initializes this service remote with a set of unit configurations. Each
     * of the units referred by the given configurations should provide the
     * service type of this service remote.
     *
     * @param configs a set of unit configurations.
     *
     * @throws InitializationException is thrown if the service remote could not
     *                                 be initialized.
     * @throws InterruptedException    is thrown if the current thread is
     *                                 externally interrupted.
     */
    @Override
    public void init(final Collection<UnitConfig> configs) throws InitializationException, InterruptedException {
        try {
            verifyMaintainability();
            MultiException.ExceptionStack exceptionStack = null;
            for (UnitConfig config : configs) {
                try {
                    init(config);
                } catch (CouldNotPerformException ex) {
                    exceptionStack = MultiException.push(this, ex, exceptionStack);
                }
            }
            MultiException.checkAndThrow(() -> "Could not activate all service units!", exceptionStack);
        } catch (CouldNotPerformException ex) {
            throw new InitializationException(this, ex);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @throws CouldNotPerformException {@inheritDoc}
     * @throws InterruptedException     {@inheritDoc}
     */
    @Override
    public void activate() throws CouldNotPerformException, InterruptedException {
        verifyMaintainability();
        active = true;
        for (UnitRemote<?> remote : unitRemoteMap.values()) {
            remote.addDataObserver(dataObserver);
            remote.addConnectionStateObserver(connectionStateObserver);
        }

        try {
            updateServiceState();
        } catch (CouldNotPerformException ex) {
            ExceptionPrinter.printHistory("Initial service state computation failed. This can be the case if any required date is not available yet.", ex, logger, LogLevel.DEBUG);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @throws CouldNotPerformException {@inheritDoc}
     * @throws InterruptedException     {@inheritDoc}
     */
    @Override
    public void activate(Object maintainer) throws InterruptedException, CouldNotPerformException {
        if (!isLocked() || this.maintainer.equals(maintainer)) {
            synchronized (maintainerLock) {
                unlock(maintainer);
                activate();
                lock(maintainer);
            }
        } else {
            throw new VerificationFailedException("[" + maintainer + "] is not the current maintainer of this remote");
        }
    }

    /**
     * {@inheritDoc}
     *
     * @throws CouldNotPerformException {@inheritDoc}
     * @throws InterruptedException     {@inheritDoc}
     */
    @Override
    public void deactivate() throws CouldNotPerformException, InterruptedException {
        verifyMaintainability();
        active = false;
        for (UnitRemote<?> remote : unitRemoteMap.values()) {
            remote.removeDataObserver(dataObserver);
            remote.removeConnectionStateObserver(connectionStateObserver);
        }
    }

    public <D extends Message> void setServiceRemoteManager(final ServiceRemoteManager<D> serviceRemoteManager) {
        this.serviceRemoteManager = serviceRemoteManager;
    }

    public boolean hasServiceRemoteManager() {
        return serviceRemoteManager != null;
    }

    public ServiceRemoteManager<?> getServiceRemoteManager() {
        return serviceRemoteManager;
    }

    /**
     * {@inheritDoc}
     *
     * @return {@inheritDoc}
     */
    @Override
    public boolean isActive() {
        return active;
    }

    @Override
    public void removeUnit(UnitConfig unitConfig) throws CouldNotPerformException, InterruptedException {
        UnitRemote unitRemote;
        if (unitRemoteMap.containsKey(unitConfig.getId())) {
            unitRemote = unitRemoteMap.remove(unitConfig.getId());

            serviceMap.remove(unitConfig.getId());
            unitRemoteTypeMap.get(unitConfig.getUnitType()).remove(unitRemote);
            for (UnitType superType : Registries.getTemplateRegistry().getSuperUnitTypes(unitRemote.getUnitType())) {
                unitRemoteTypeMap.get(superType).remove(unitRemote);
            }
        } else if (disabledUnitRemoteMap.containsKey(unitConfig.getId())) {
            unitRemote = disabledUnitRemoteMap.remove(unitConfig.getId());
            infrastructureUnitMap.remove(unitConfig.getId());
        } else if (infrastructureUnitMap.containsKey(unitConfig.getId())) {
            unitRemote = infrastructureUnitMap.remove(unitConfig.getId());
        } else {
            throw new NotAvailableException("UnitConfig[" + ScopeProcessor.generateStringRep(unitConfig.getScope()) + "]");
        }

        unitRemote.removeDataObserver(dataObserver);
        unitRemote.removeConnectionStateObserver(connectionStateObserver);
        unitRemote.removeConfigObserver(unitConfigObserver);
    }

    /**
     * Returns a collection of all internally used unit remotes.
     *
     * @return a collection of unit remotes limited to the service interface.
     */
    @Override
    public Collection<org.openbase.bco.dal.lib.layer.unit.UnitRemote> getInternalUnits() {
        return Collections.unmodifiableCollection(unitRemoteMap.values());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<org.openbase.bco.dal.lib.layer.unit.UnitRemote> getInternalUnits(UnitType unitType) throws CouldNotPerformException {
        List<UnitRemote> unitRemotes = new ArrayList<>();
        for (UnitRemote unitRemote : unitRemoteMap.values()) {
            if (unitType == UnitType.UNKNOWN || unitType == unitRemote.getUnitType() || UnitConfigProcessor.isBaseUnit(unitRemote.getUnitType()) || Registries.getTemplateRegistry().getSubUnitTypes(unitType).contains(unitRemote.getUnitType())) {
                unitRemotes.add(unitRemote);
            }
        }
        return Collections.unmodifiableCollection(unitRemotes);
    }

    @Override
    public boolean hasInternalRemotes() {
        return !unitRemoteMap.isEmpty();
    }

    /**
     * Returns a collection of all internally used unit remotes.
     *
     * @return a collection of unit remotes limited to the service interface.
     */
    @Override
    public Collection<S> getServices() {
        return Collections.unmodifiableCollection(serviceMap.values());
    }

    /**
     * Returns a collection of all internally used unit remotes filtered by the
     * given unit type.
     *
     * @return a collection of unit remotes limited to the service interface.
     */
    @Override
    public Collection<S> getServices(final UnitType unitType) {
        if (unitType == UnitType.UNKNOWN) {
            return Collections.unmodifiableCollection(serviceMap.values());
        }

        if (!unitRemoteTypeMap.containsKey(unitType)) {
            return new ArrayList<>();
        }

        return Collections.unmodifiableCollection(unitRemoteTypeMap.get(unitType));
    }

    /**
     * Returns the service type of this remote.
     *
     * @return the remote service type.
     */
    @Override
    public ServiceType getServiceType() {
        return serviceType;
    }


    public Future<ActionDescription> applyAction(final ActionDescription actionDescription) {
        return applyAction(actionDescription.toBuilder());
    }

    @Override
    public Future<ActionDescription> applyAction(final ActionDescription.Builder actionDescriptionBuilder) {
        try {
            if (!actionDescriptionBuilder.getServiceStateDescription().getServiceType().equals(getServiceType())) {
                throw new VerificationFailedException("Service type is not compatible to given action config!");
            }

            final boolean newSubmission = !actionDescriptionBuilder.getCancel() && !actionDescriptionBuilder.getExtend();

            // only setup id of this intermediary action if this is an new submission
            if (newSubmission) {

                // validate that the action is really a new one
                if (!actionDescriptionBuilder.getActionId().isEmpty()) {
                    throw new InvalidStateException("Action[" + actionDescriptionBuilder + "] has been applied twice which is an invalid operation!");
                }

                // resolve responsible unit
                final UnitConfig responsibleUnitConfig = Registries.getUnitRegistry().getUnitConfigById(actionDescriptionBuilder.getServiceStateDescription().getUnitId());

                // setup new intermediary action
                ActionDescriptionProcessor.verifyActionDescription(actionDescriptionBuilder, responsibleUnitConfig, true);

                // mark this actions as intermediary
                actionDescriptionBuilder.setIntermediary(true);
            }

            // generate and apply impact actions
            final List<Future<ActionDescription>> actionTaskList = new ArrayList<>();
            for (final UnitRemote<?> unitRemote : getInternalUnits(actionDescriptionBuilder.getServiceStateDescription().getUnitType())) {

                // prepare impact action by copy cause action description and setup individual
                final Builder unitActionDescriptionBuilder = ActionDescription.newBuilder(actionDescriptionBuilder.build());
                unitActionDescriptionBuilder.getServiceStateDescriptionBuilder().setUnitId(unitRemote.getId());

                // update action cause if this is a new submission
                if (newSubmission) {
                    unitActionDescriptionBuilder.clearActionId();
                    unitActionDescriptionBuilder.clearIntermediary();
                    ActionDescriptionProcessor.updateActionCause(unitActionDescriptionBuilder, actionDescriptionBuilder);
                }

                // apply action on remote
                actionTaskList.add(unitRemote.applyAction(unitActionDescriptionBuilder));
            }

            // collect results and setup action impact list of intermediary action
            return FutureProcessor.allOf(input -> {

                // we are done if this is not a new action
                if (!newSubmission) {
                    return actionDescriptionBuilder.build();
                }

                for (final Future<ActionDescription> actionTask : input) {
                    try {
                        ActionDescriptionProcessor.updateActionImpacts(actionDescriptionBuilder, actionTask.get());
                    } catch (ExecutionException ex) {
                        throw new FatalImplementationErrorException("AllOf called result processable even though some futures did not finish", GlobalCachedExecutorService.getInstance(), ex);
                    }
                }
                return actionDescriptionBuilder.build();
            }, actionTaskList);
        } catch (CouldNotPerformException ex) {
            return FutureProcessor.canceledFuture(ActionDescription.class, new CouldNotPerformException("Could not apply action!", ex));
        }
    }

    /**
     * Apply an authenticated action with the default session manager. The authenticated value has to contain an action
     * description encrypted with the session key from the default manager if a user is logged in. Else an action description
     * as a byte string. This method internally calls {@link #applyActionAuthenticated(AuthenticatedValue, Builder, byte[])}.
     * <p>
     * Note: Future is canceled if no action description is available or can be extracted from the authenticated value.
     *
     * @param authenticatedValue The authenticated value containing an action description to be applied. Created with the
     *                           default session manager.
     *
     * @return An authenticated value containing an updated action description in which resulting actions are added as impacts.
     */
    @Override
    public Future<AuthenticatedValue> applyActionAuthenticated(final AuthenticatedValue authenticatedValue) {
        try {
            if (!authenticatedValue.hasValue() || authenticatedValue.getValue().isEmpty()) {
                throw new NotAvailableException("Value in AuthenticatedValue");
            }

            final ActionDescription actionDescription;
            try {
                if (SessionManager.getInstance().isLoggedIn()) {
                    actionDescription = EncryptionHelper.decryptSymmetric(authenticatedValue.getValue(), SessionManager.getInstance().getSessionKey(), ActionDescription.class);
                } else {
                    actionDescription = ActionDescription.parseFrom(authenticatedValue.getValue());
                }
            } catch (CouldNotPerformException | InvalidProtocolBufferException ex) {
                throw new CouldNotPerformException("Could not extract ActionDescription from AuthenticatedValue", ex);
            }

            return applyActionAuthenticated(authenticatedValue, actionDescription.toBuilder(), SessionManager.getInstance().getSessionKey());
        } catch (CouldNotPerformException ex) {
            return FutureProcessor.canceledFuture(AuthenticatedValue.class, ex);
        }
    }

    /**
     * Apply an authenticated action on all units contained in this service remote.
     * <p>
     * Note: Future is canceled if something fails, e.g. the service provided in the action description does not match this service remote.
     *
     * @param authenticatedValue       the authenticated value with which the action is applied on all internal units. If a session key
     *                                 is provided it has to contain a valid and matching ticket. Optionally, it may contain tokens for the request.
     * @param actionDescriptionBuilder the action description builder describes the action applied to each unit. For each action on an internal unit,
     *                                 it is copied, the unit id adjusted and the action chain updated. After the result of this method is awaited,
     *                                 its action impact list is filled with the ids of actions invoked on the internal units.
     * @param sessionKey               the session key used to encrypt and decrypt actions descriptions. If it is provided it needs to match to the
     *                                 authenticated value. If it is null then actions will be performed with other permissions.
     *
     * @return a future which returns the same authenticated value as in the request with an updated action description as its value. Calling get on this future makes sure that the
     * action description builder is updated properly and that all internal actions finished successfully.
     */
    public Future<AuthenticatedValue> applyActionAuthenticated(final AuthenticatedValue authenticatedValue, final ActionDescription.Builder actionDescriptionBuilder, final byte[] sessionKey) {
        try {
            if (authenticatedValue == null) {
                throw new NotAvailableException("AuthenticatedValue");
            }

            if (!actionDescriptionBuilder.getServiceStateDescription().getServiceType().equals(getServiceType())) {
                throw new VerificationFailedException("Service type is not compatible to given action config!");
            }

            final boolean newSubmission = !actionDescriptionBuilder.getCancel() && !actionDescriptionBuilder.getExtend();

            // only setup id of this intermediary action if this is an new submission
            if (newSubmission) {

                // validate that the action is really a new one
                if (!actionDescriptionBuilder.getActionId().isEmpty()) {
                    throw new InvalidStateException("Action[" + actionDescriptionBuilder + "] has been applied twice which is an invalid operation!");
                }

                // resolve responsible unit
                final UnitConfig responsibleUnitConfig = Registries.getUnitRegistry().getUnitConfigById(actionDescriptionBuilder.getServiceStateDescription().getUnitId());

                // setup new intermediary action
                ActionDescriptionProcessor.verifyActionDescription(actionDescriptionBuilder, responsibleUnitConfig, true);

                // mark this actions as intermediary
                actionDescriptionBuilder.setIntermediary(true);
            }

            // generate and apply impact actions
            final List<Future<AuthenticatedValue>> actionTaskList = new ArrayList<>();
            for (final UnitRemote<?> unitRemote : getInternalUnits(actionDescriptionBuilder.getServiceStateDescription().getUnitType())) {

                // prepare impact action by copy cause action description and setup individual
                final Builder unitActionDescriptionBuilder = ActionDescription.newBuilder(actionDescriptionBuilder.build());
                unitActionDescriptionBuilder.getServiceStateDescriptionBuilder().setUnitId(unitRemote.getId());

                // update action cause if this is a new submission
                if (newSubmission) {
                    unitActionDescriptionBuilder.clearActionId();
                    unitActionDescriptionBuilder.clearIntermediary();
                    ActionDescriptionProcessor.updateActionCause(unitActionDescriptionBuilder, actionDescriptionBuilder);
                }

                // encrypt action description again
                final AuthenticatedValue authValue;
                if (sessionKey != null) {
                    final ByteString encrypt = EncryptionHelper.encryptSymmetric(unitActionDescriptionBuilder.build(), sessionKey);
                    authValue = authenticatedValue.toBuilder().setValue(encrypt).build();
                } else {
                    authValue = authenticatedValue.toBuilder().setValue(unitActionDescriptionBuilder.build().toByteString()).build();
                }

                // apply action on remote
                actionTaskList.add(unitRemote.applyActionAuthenticated(authValue));
            }

            // collect results and setup action impact list of intermediary cause action
            return FutureProcessor.allOf(input -> {

                // we are done if this is not a new action
                if (!newSubmission) {
                    return authenticatedValue;
                }

                // generate impact list set store it into the authenticated value
                for (final Future<AuthenticatedValue> future : input) {
                    try {
                        final AuthenticatedValue unitAuthenticatedValue = future.get();
                        final ActionDescription unitActionResponse;

                        // validate responses and decrypt results
                        if (sessionKey != null) {
                            AuthenticationClientHandler.handleServiceServerResponse(sessionKey, authenticatedValue.getTicketAuthenticatorWrapper(), unitAuthenticatedValue.getTicketAuthenticatorWrapper());
                            unitActionResponse = EncryptionHelper.decryptSymmetric(unitAuthenticatedValue.getValue(), sessionKey, ActionDescription.class);
                        } else {
                            unitActionResponse = ActionDescription.parseFrom(unitAuthenticatedValue.getValue());
                        }

                        // add resulting actions as impacts
                        ActionDescriptionProcessor.updateActionImpacts(actionDescriptionBuilder, unitActionResponse);

                    } catch (ExecutionException ex) {
                        throw new FatalImplementationErrorException("AllOf called result processable even though some futures did not finish", GlobalCachedExecutorService.getInstance(), ex);
                    } catch (InvalidProtocolBufferException ex) {
                        throw new CouldNotPerformException("Could not parse result from unauthenticated applyAction request as action description", ex);
                    }
                }

                // encrypt and return result
                final AuthenticatedValue.Builder responseAuthValue = authenticatedValue.toBuilder();
                if (sessionKey != null) {
                    responseAuthValue.setValue(EncryptionHelper.encryptSymmetric(actionDescriptionBuilder.build(), sessionKey));
                } else {
                    responseAuthValue.setValue(actionDescriptionBuilder.build().toByteString());
                }
                return responseAuthValue.build();
            }, actionTaskList);
        } catch (CouldNotPerformException ex) {
            return FutureProcessor.canceledFuture(AuthenticatedValue.class, ex);
        }
    }

    /**
     * Method generates a new service state out of the compatible service providers provided the units referred by the {@code unitType}.
     *
     * @param unitType       the unit type to filter the service provider collection. Use UNKNOWN as wildcard.
     * @param neutralState   the neutral state which is only used if all other instances are in the neutral state.
     * @param effectiveState the effective state which is set if at least one provider is referring this state.
     *
     * @return a new generated service state builder containing all fused states.
     *
     * @throws CouldNotPerformException is thrown in case the fusion fails.
     */
    protected Message.Builder generateAggregatedState(final UnitType unitType, final ProtocolMessageEnum neutralState, final ProtocolMessageEnum effectiveState) throws CouldNotPerformException {

        try {
            // generate builder
            final Message.Builder serviceStateBuilder = Services.generateServiceStateBuilder(getCommunicationType(), neutralState);

            // lookup field descriptors
            final FieldDescriptor valueDescriptor = serviceStateBuilder.getDescriptorForType().findFieldByName(FIELD_NAME_VALUE);
            final FieldDescriptor mapFieldDescriptor = serviceStateBuilder.getDescriptorForType().findFieldByName(FIELD_NAME_LAST_VALUE_OCCURRENCE);
            final FieldDescriptor timestampDescriptor = serviceStateBuilder.getDescriptorForType().findFieldByName(FIELD_NAME_TIMESTAMP);

            // verify field descriptors
            if (valueDescriptor == null) {
                throw new NotAvailableException("Field[" + FIELD_NAME_VALUE + "] does not exist for type " + serviceStateBuilder.getClass().getName());
            } else if (mapFieldDescriptor == null) {
                throw new NotAvailableException("Field[" + FIELD_NAME_LAST_VALUE_OCCURRENCE + "] does not exist for type " + serviceStateBuilder.getClass().getName());
            } else if (timestampDescriptor == null) {
                throw new NotAvailableException("Field[" + FIELD_NAME_TIMESTAMP + "] does not exist for type " + serviceStateBuilder.getClass().getName());
            }

            // pre init fields to collect
            long timestamp = 0;
            ActionDescription latestAction = null;

            FieldDescriptor mapEntryKeyDescriptor = null;
            FieldDescriptor mapEntryValueDescriptor = null;

            for (S service : getServices(unitType)) {

                // do not handle if data is not synced yet.
                if (!((UnitRemote) service).isDataAvailable()) {
                    continue;
                }

                // handle state
                final Message state = Services.invokeProviderServiceMethod(getServiceType(), service);
                if (state.hasField(valueDescriptor) && state.getField(valueDescriptor).equals(effectiveState.getValueDescriptor())) {
                    serviceStateBuilder.setField(valueDescriptor, state.getField(valueDescriptor));
                }

                // handle latest occurrence timestamps
                for (int i = 0; i < state.getRepeatedFieldCount(mapFieldDescriptor); i++) {
                    final Message entry = (Message) state.getRepeatedField(mapFieldDescriptor, i);

                    if (mapEntryKeyDescriptor == null) {
                        mapEntryKeyDescriptor = entry.getDescriptorForType().findFieldByName(FIELD_NAME_KEY);
                        mapEntryValueDescriptor = entry.getDescriptorForType().findFieldByName(FIELD_NAME_VALUE);

                        if (mapEntryKeyDescriptor == null) {
                            throw new NotAvailableException("Field[" + FIELD_NAME_KEY + "] does not exist for type " + entry.getClass().getName());
                        } else if (mapEntryValueDescriptor == null) {
                            throw new NotAvailableException("Field[" + FIELD_NAME_VALUE + "] does not exist for type " + entry.getClass().getName());
                        }
                    }

                    try {
                        ServiceStateProcessor.updateLatestValueOccurrence((EnumValueDescriptor) entry.getField(mapEntryKeyDescriptor), (Timestamp) entry.getField(mapEntryValueDescriptor), serviceStateBuilder);
                    } catch (CouldNotPerformException ex) {
                        ExceptionPrinter.printHistory("Could not update latest occurrence timestamp of Entry[" + entry + "]", ex, logger);
                    }
                }

                // handle timestamp
                timestamp = Math.max(timestamp, ((Timestamp) state.getField(timestampDescriptor)).getTime());

                // select latest action
                latestAction = selectLatestAction(state, latestAction);
            }

            // update final timestamp
            TimestampProcessor.updateTimestamp(timestamp, serviceStateBuilder, TimeUnit.MICROSECONDS, logger);

            // setup responsible action with latest action as cause.
            setupResponsibleActionForNewAggregatedServiceState(serviceStateBuilder, latestAction);

            // return merged state
            return serviceStateBuilder;
        } catch (final CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not fuse service state!", ex);
        }
    }

    protected ActionDescription selectLatestAction(final Message state, final ActionDescription latestAction) {
        try {
            final ActionDescription responsibleAction = Services.getResponsibleAction(state);
            if (latestAction == null || latestAction.getTimestamp().getTime() < responsibleAction.getTimestamp().getTime()) {
                return responsibleAction;
            }
        } catch (NotAvailableException ex) {
            // just return the current latest action...
        }
        return latestAction;
    }

    protected void setupResponsibleActionForNewAggregatedServiceState(final Message.Builder serviceStateBuilder, ActionDescription latestActionCause) {
        if (hasServiceRemoteManager()) {
            try {

                if (latestActionCause != null && (!latestActionCause.hasActionId() || latestActionCause.getActionId().isEmpty())) {
                    logger.warn("Skip latest action since it does not offer an action id!");
                    latestActionCause = null;
                }

                // generate and set responsible action and set last action as cause if available.
                ActionDescriptionProcessor.generateAndSetResponsibleAction(serviceStateBuilder, getServiceType(), getServiceRemoteManager().getResponsibleUnit(), latestActionCause);

            } catch (CouldNotPerformException ex) {
                ExceptionPrinter.printHistory("Could not generate responsible action for aggregated service state!", ex, logger);
            }
        }
    }

    /**
     * Method blocks until an initial data message was dataObserverreceived from
     * every remote controller.
     *
     * @throws CouldNotPerformException is thrown if any error occurs.
     * @throws InterruptedException     is thrown in case the thread is externally
     *                                  interrupted.
     */
    @Override
    public void waitForData() throws CouldNotPerformException, InterruptedException {
        if (unitRemoteMap.isEmpty()) {
            return;
        }

        for (UnitRemote remote : unitRemoteMap.values()) {
            remote.waitForData();
        }
        serviceStateObservable.waitForValue();
    }

    /**
     * Method blocks until an initial data message was received from every
     * remote controller or the given timeout is reached.
     *
     * @param timeout  maximal time to wait for the main controller data. After
     *                 the timeout is reached a TimeoutException is thrown.
     * @param timeUnit the time unit of the timeout.
     *
     * @throws CouldNotPerformException is thrown in case the any error occurs,
     *                                  or if the given timeout is reached. In this case a TimeoutException is
     *                                  thrown.
     * @throws InterruptedException     is thrown in case the thread is externally
     *                                  interrupted.
     */
    @Override
    public void waitForData(final long timeout, final TimeUnit timeUnit) throws CouldNotPerformException, InterruptedException {
        if (unitRemoteMap.isEmpty()) {
            return;
        }
        //todo reimplement with respect to the given timeout.
        for (UnitRemote remote : unitRemoteMap.values()) {
            remote.waitForData(timeout, timeUnit);
        }
        serviceStateObservable.waitForValue(timeout, timeUnit);
    }

    /**
     * Checks if a server connection is established for every underlying remote.
     *
     * @return is true in case that the connection for every underlying remote
     * it established.
     */
    @Override
    public boolean isConnected() {

        final Iterator<UnitRemote> iterator = getInternalUnits().iterator();

        while (iterator.hasNext()) {
            if (!iterator.next().isConnected()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Check if the data object is already available for every underlying
     * remote.
     *
     * @return is true in case that for every underlying remote data is
     * available.
     */
    @Override
    public boolean isDataAvailable() {
        if (!hasInternalRemotes()) {
            return false;
        }
        return serviceStateObservable.isValueAvailable();
    }

    @Override
    public void validateData() throws InvalidStateException {

        if (isShutdownInitiated()) {
            throw new InvalidStateException(new ShutdownInProgressException(this));
        }

        if (isDataAvailable()) {
            throw new InvalidStateException(new NotAvailableException("Data"));
        }
    }

    public static boolean verifyServiceCompatibility(final UnitConfig unitConfig, final ServiceType serviceType) {
        for (ServiceConfig serviceConfig : unitConfig.getServiceConfigList()) {
            if ((serviceConfig.getServiceDescription().getServiceType() == serviceType)) {
                return true;
            }
        }
        return false;
    }

    /**
     * {@inheritDoc}
     *
     * @throws VerificationFailedException {@inheritDoc}
     */
    @Override
    public void verifyMaintainability() throws VerificationFailedException {
        if (isLocked()) {
            throw new VerificationFailedException("Manipulation of " + this + "is currently not valid because the maintains is protected by another instance! "
                    + "Did you try to modify an instance which is locked by a managed instance pool?");
        }
    }

    /**
     * {@inheritDoc}
     *
     * @return {@inheritDoc}
     */
    @Override
    public boolean isLocked() {
        synchronized (maintainerLock) {
            return maintainer != null;
        }
    }

    /**
     * {@inheritDoc}
     *
     * @throws CouldNotPerformException {@inheritDoc}
     */
    @Override
    public void lock(final Object maintainer) throws CouldNotPerformException {
        synchronized (maintainerLock) {
            if (this.maintainer != null) {
                throw new CouldNotPerformException("Could not lock remote for because remote is already locked by another instance!");
            }
            this.maintainer = maintainer;
        }
    }

    /**
     * Method unlocks this instance.
     *
     * @param maintainer the instance which currently holds the lock.
     *
     * @throws CouldNotPerformException is thrown if the instance could not be
     *                                  unlocked.
     */
    @Override
    public void unlock(final Object maintainer) throws CouldNotPerformException {
        synchronized (maintainerLock) {
            if (this.maintainer != null && this.maintainer != maintainer) {
                throw new CouldNotPerformException("Could not unlock remote for because remote is locked by another instance!");
            }
            this.maintainer = null;
        }
    }

    public void setInfrastructureFilter(final boolean filterInfrastructureUnits) throws CouldNotPerformException {
        if (this.filterInfrastructureUnits != filterInfrastructureUnits) {
            this.filterInfrastructureUnits = filterInfrastructureUnits;
            if (filterInfrastructureUnits) {
                for (UnitRemote<?> unitRemote : new ArrayList<>(unitRemoteMap.values())) {
                    updateIgnoredUnitMaps(unitRemote.getId(), infrastructureUnitMap, unitRemote.isInfrastructure());
                }
            } else {
                for (UnitRemote<?> unitRemote : new ArrayList<>(infrastructureUnitMap.values())) {
                    updateIgnoredUnitMaps(unitRemote.getId(), infrastructureUnitMap, unitRemote.isInfrastructure());
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     *
     * @return {@inheritDoc}
     */
    @Override
    public Future<Long> ping() {
        if (unitRemoteMap.isEmpty()) {
            return FutureProcessor.completedFuture(0L);
        }

        final List<Future<Long>> futurePings = new ArrayList<>();

        for (final UnitRemote remote : unitRemoteMap.values()) {
            if (remote.isConnected()) {
                futurePings.add(remote.ping());
            }
        }

        return FutureProcessor.allOf(input -> {
            try {
                long sum = 0;
                for (final Future<Long> future : input) {
                    sum += future.get();
                }

                long ping;
                if (!input.isEmpty()) {
                    ping = sum / input.size();
                } else {
                    ping = 0;
                }
                connectionPing = ping;
                return ping;
            } catch (ExecutionException ex) {
                throw new CouldNotPerformException("Could not compute ping!", ex);
            }
        }, futurePings);
    }

    /**
     * {@inheritDoc}
     *
     * @return {@inheritDoc}
     */
    @Override
    public Long getPing() {
        return connectionPing;
    }

    /**
     * Returns a short instance description.
     *
     * @return a description as string.
     */
    @Override
    public String toString() {
        if (serviceType == null) {
            return getClass().getSimpleName() + "[serviceType: ? ]";
        }
        return getClass().getSimpleName() + "[serviceType:" + serviceType.name() + "]";
    }

    @Override
    public void waitForConnectionState(ConnectionState.State connectionState, long timeout) throws InterruptedException, TimeoutException {
        synchronized (connectionStateLock) {
            if (connectionState == getConnectionState()) {
                return;
            }

            final long startingTime = System.currentTimeMillis();
            long timeWaited = 0;
            while (connectionState != getConnectionState()) {
                if (timeout > 0) {
                    connectionStateLock.wait(timeout - timeWaited);

                    timeWaited = System.currentTimeMillis() - startingTime;
                    if (timeout - timeWaited <= 0) {
                        throw new TimeoutException();
                    }
                } else {
                    connectionState.wait();
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param serviceType {@inheritDoc}
     *
     * @return {@inheritDoc}
     *
     * @throws NotAvailableException {@inheritDoc}
     */
    @Override
    public ST getServiceState(ServiceType serviceType) throws NotAvailableException {
        if (serviceType != this.serviceType) {
            throw new NotAvailableException("ServiceState", new InvalidStateException("ServiceType[" + serviceType.name() + "] not compatible with " + this));
        }
        return getData();
    }

    /**
     * {@inheritDoc}
     *
     * @return {@inheritDoc}
     */
    @Override
    public ServiceProvider getServiceProvider() {
        return this;
    }

    public ServiceTemplate getServiceTemplate() throws NotAvailableException {
        if (serviceTemplate == null) {
            // setup service template
            try {
                serviceTemplate = Registries.getTemplateRegistry().getServiceTemplateByType(serviceType);
            } catch (CouldNotPerformException ex) {
                throw new NotAvailableException("ServiceRemote", "ServiceTemplate", ex);
            }
        }
        return serviceTemplate;
    }

    public boolean isShutdownInitiated() {
        return shutdownInitiated;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void shutdown() {
        try {
            shutdownInitiated = true;

            verifyMaintainability();
            deactivate();
            for (UnitRemote<?> unitRemote : unitRemoteMap.values()) {
                unitRemote.removeConfigObserver(unitConfigObserver);
            }
            for (UnitRemote<?> unitRemote : disabledUnitRemoteMap.values()) {
                unitRemote.removeConfigObserver(unitConfigObserver);
            }
            for (UnitRemote<?> remote : infrastructureUnitMap.values()) {
                remote.removeConfigObserver(unitConfigObserver);
            }
        } catch (CouldNotPerformException | InterruptedException ex) {
            ExceptionPrinter.printHistory(new ShutdownException(this, ex), logger);
        }
    }
}
