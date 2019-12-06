package org.openbase.bco.dal.remote.layer.service;

/*-
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

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import org.openbase.bco.authentication.lib.EncryptionHelper;
import org.openbase.bco.authentication.lib.SessionManager;
import org.openbase.bco.authentication.lib.iface.AuthenticatedSnapshotable;
import org.openbase.bco.dal.lib.layer.service.Services;
import org.openbase.bco.dal.lib.layer.unit.Unit;
import org.openbase.bco.dal.lib.layer.unit.UnitProcessor;
import org.openbase.bco.dal.lib.layer.unit.UnitRemote;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.jul.exception.*;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.exception.printer.LogLevel;
import org.openbase.jul.communication.controller.AbstractRemoteClient;
import org.openbase.jul.iface.Activatable;
import org.openbase.jul.iface.provider.PingProvider;
import org.openbase.jul.pattern.Observer;
import org.openbase.jul.pattern.controller.Remote;
import org.openbase.jul.pattern.provider.DataProvider;
import org.openbase.jul.schedule.*;
import org.openbase.type.domotic.action.ActionDescriptionType.ActionDescription;
import org.openbase.type.domotic.action.SnapshotType;
import org.openbase.type.domotic.action.SnapshotType.Snapshot;
import org.openbase.type.domotic.authentication.AuthenticatedValueType.AuthenticatedValue;
import org.openbase.type.domotic.service.ServiceConfigType.ServiceConfig;
import org.openbase.type.domotic.service.ServiceTemplateType.ServiceTemplate.ServicePattern;
import org.openbase.type.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import org.openbase.type.domotic.service.ServiceTempusTypeType.ServiceTempusType.ServiceTempus;
import org.openbase.type.domotic.unit.UnitConfigType.UnitConfig;
import org.openbase.type.domotic.unit.UnitTemplateType;
import org.openbase.type.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public abstract class ServiceRemoteManager<D extends Message> implements Activatable, AuthenticatedSnapshotable, PingProvider, DataProvider<D> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServiceRemoteManager.class);

    private boolean active;
    private long connectionPing;
    private final ServiceRemoteFactory serviceRemoteFactory;
    private final Map<ServiceType, AbstractServiceRemote> serviceRemoteMap;
    private final Observer<Unit, Message> serviceDataObserver;
    private final Unit<D> responsibleUnit;
    private boolean filterInfrastructureUnits;
    private final CloseableLockProvider lockProvider;

    public ServiceRemoteManager(final Unit<D> responsibleUnit, final CloseableLockProvider lockProvider) {
        this(responsibleUnit, lockProvider, true);
    }

    public ServiceRemoteManager(final Unit<D> responsibleUnit, final CloseableLockProvider lockProvider, final boolean filterInfrastructureUnits) {
        this.responsibleUnit = responsibleUnit;
        this.lockProvider = lockProvider;
        this.filterInfrastructureUnits = filterInfrastructureUnits;
        this.serviceRemoteMap = new HashMap<>();
        this.serviceRemoteFactory = ServiceRemoteFactoryImpl.getInstance();
        this.serviceDataObserver = (source, data) -> notifyServiceUpdate(source, data);
    }

    public synchronized void applyConfigUpdate(final List<UnitConfig> unitConfigList) throws CouldNotPerformException, InterruptedException {
        Registries.waitForData();
        try (final CloseableWriteLockWrapper ignored = lockProvider.getCloseableWriteLock(this)) {
            // shutdown all existing instances.
            for (final AbstractServiceRemote serviceRemote : serviceRemoteMap.values()) {
                serviceRemote.removeDataObserver(serviceDataObserver);
                serviceRemote.shutdown();
            }
            serviceRemoteMap.clear();

            // init a new set for each supported service type.
            Map<ServiceType, Set<UnitConfig>> serviceMap = new HashMap<>();
            for (final ServiceType serviceType : ServiceType.values()) {
                serviceMap.put(serviceType, new HashSet<>());
            }

            // init service unit map
            for (final UnitConfig unitConfig : unitConfigList) {
                // sort dal unit by service type
                // register unit for each service type. UnitConfigs can be added twice because of duplicated types with different service patterns but are filtered by the set.
                for (ServiceConfig serviceConfig : unitConfig.getServiceConfigList()) {
                    serviceMap.get(serviceConfig.getServiceDescription().getServiceType()).add(unitConfig);
                }
            }

            // initialize service remotes
            for (final ServiceType serviceType : getManagedServiceTypes()) {
                final AbstractServiceRemote serviceRemote = serviceRemoteFactory.newInitializedInstance(serviceType, serviceMap.get(serviceType), filterInfrastructureUnits);
                serviceRemote.setServiceRemoteManager(this);
                serviceRemoteMap.put(serviceType, serviceRemote);

                // if already active than update the current location state.
                if (isActive()) {
                    serviceRemote.addDataObserver(serviceDataObserver);
                    serviceRemote.activate();
                }
            }
        }
    }

    @Override
    public void activate() throws CouldNotPerformException, InterruptedException {
        try (final CloseableReadLockWrapper ignored = lockProvider.getCloseableReadLock(this)) {
            active = true;
            for (AbstractServiceRemote serviceRemote : serviceRemoteMap.values()) {
                serviceRemote.addDataObserver(serviceDataObserver);
                serviceRemote.activate();
            }
        }
    }

    @Override
    public void deactivate() throws InterruptedException, CouldNotPerformException {
        try (final CloseableReadLockWrapper ignored = lockProvider.getCloseableReadLock(this)) {
            active = false;
            for (AbstractServiceRemote serviceRemote : serviceRemoteMap.values()) {
                serviceRemote.removeDataObserver(serviceDataObserver);
                serviceRemote.deactivate();
            }
        }
    }

    @Override
    public boolean isActive() {
        return active;
    }

    /**
     * Method checks if the given {@code ServiceType} is currently available by this {@code ServiceRemoteManager}
     *
     * @param serviceType the {@code ServiceType} to check.
     *
     * @return returns true if the {@code ServiceType} is available, otherwise false.
     */
    public boolean isServiceAvailable(final ServiceType serviceType) {
        try {
            return getServiceRemote(serviceType).hasInternalRemotes();
        } catch (NotAvailableException ex) {
            // no service entry means the service is not available.
            return false;
        }
    }

    /**
     * Generates a list of all internal service remotes of this service remote manager.
     *
     * @return a list of service remotes
     */
    public List<AbstractServiceRemote> getServiceRemoteList() {
        try (final CloseableReadLockWrapper ignored = lockProvider.getCloseableReadLock(this)) {
            return new ArrayList<>(serviceRemoteMap.values());
        }
    }

    /**
     * Generates a list of all unit remotes used by the internal service remotes of this service remote manager.
     *
     * @return a list of unit remotes.
     */
    public List<UnitRemote<?>> getInternalUnitRemoteList() {
        final ArrayList<UnitRemote<?>> unitRemoteList = new ArrayList<>();
        for (AbstractServiceRemote serviceRemote : getServiceRemoteList()) {
            unitRemoteList.addAll(serviceRemote.getInternalUnits());
        }
        return unitRemoteList;
    }

    /**
     * Returns the service remote responsible for the aggregation and control of the given service type.
     *
     * @param serviceType the service type used to identify the remote instance.
     *
     * @return the requested service remote
     *
     * @throws NotAvailableException is thrown if the remotes are not available yet.
     */
    public AbstractServiceRemote<?, ?> getServiceRemote(final ServiceType serviceType) throws NotAvailableException {
        try (final CloseableReadLockWrapper ignored = lockProvider.getCloseableReadLock(this)) {
            AbstractServiceRemote<?, ?> serviceRemote = serviceRemoteMap.get(serviceType);
            if (serviceRemote == null) {
                final String responsible = (responsibleUnit != null ? responsibleUnit.toString() : "the underlying instance");
                throw new NotAvailableException("ServiceRemote", serviceType.name(), new NotSupportedException("ServiceType[" + serviceType + "]", responsible));
            }
            return serviceRemote;
        }
    }

    public <B> B updateBuilderWithAvailableServiceStates(final B builder) throws InterruptedException, CouldNotPerformException {
        return updateBuilderWithAvailableServiceStates(builder, responsibleUnit.getDataClass(), getManagedServiceTypes());
    }

    public <B> B updateBuilderWithAvailableServiceStates(final B builder, final Class dataClass, final Set<ServiceType> supportedServiceTypeSet) throws CouldNotPerformException {
        try {
            for (final ServiceType serviceType : supportedServiceTypeSet) {

                final Object serviceState;

                // compute current service state
                try {
                    final AbstractServiceRemote<?, ?> serviceRemote = getServiceRemote(serviceType);
                    /* When the locationRemote is active and a config update occurs the serviceRemoteManager clears
                     * its map of service remotes and fills it with new ones. When they are activated an update is triggered while
                     * the map is not completely filled. Therefore the serviceRemote can be null.
                     */
                    if (serviceRemote == null) {
                        continue;
                    }
                    if (!serviceRemote.isDataAvailable()) {
                        continue;
                    }

                    serviceState = Services.invokeProviderServiceMethod(serviceType, serviceRemote);
                } catch (NotAvailableException ex) {
                    ExceptionPrinter.printHistory("No service data for type[" + serviceType + "] on location available!", ex, LOGGER);
                    continue;
                } catch (NotSupportedException | IllegalArgumentException ex) {
                    ExceptionPrinter.printHistory(new FatalImplementationErrorException(this, ex), LOGGER);
                    continue;
                } catch (CouldNotPerformException ex) {
                    ExceptionPrinter.printHistory("Could not update ServiceState[" + serviceType.name() + "] for " + this, ex, LOGGER);
                    continue;
                }

                // copy current into last service state if possible
                try {
                    final Message newLastState = Services.invokeProviderServiceMethod(serviceType, ServiceTempus.CURRENT, builder);
                    Services.invokeServiceMethod(serviceType, ServicePattern.OPERATION, ServiceTempus.LAST, builder, newLastState);
                } catch (CouldNotPerformException ex) {
                    ExceptionPrinter.printHistory(new NotSupportedException("Could not store last service state. Field[" + serviceType.name().toLowerCase().replace("_service", "") + "Last] is missing in protobuf type " + dataClass + "!", this, ex), LOGGER);
                }

                // store new current state
                try {
                    Services.invokeOperationServiceMethod(serviceType, builder, serviceState);
                } catch (CouldNotPerformException ex) {
                    ExceptionPrinter.printHistory(new NotSupportedException("Field[" + serviceType.name().toLowerCase().replace("_service", "") + "] is missing in protobuf type " + dataClass + "!", this, ex), LOGGER);
                }
            }
        } catch (Exception ex) {
            throw new CouldNotPerformException("Could not update current status!", ex);
        }
        return builder;
    }

    @Override
    public Future<SnapshotType.Snapshot> recordSnapshot() {
        return recordSnapshot(UnitTemplateType.UnitTemplate.UnitType.UNKNOWN);
    }

    public Future<Snapshot> recordSnapshot(final UnitType unitType) {
        return GlobalCachedExecutorService.submit(() -> {
            try {
                SnapshotType.Snapshot.Builder snapshotBuilder = SnapshotType.Snapshot.newBuilder();
                Set<UnitRemote> unitRemoteSet = new HashSet<>();

                if (unitType == UnitType.UNKNOWN) {
                    // if the type is unknown then take the snapshot for all units
                    for (AbstractServiceRemote<?, ?> abstractServiceRemote : getServiceRemoteList()) {
                        unitRemoteSet.addAll(abstractServiceRemote.getInternalUnits());
                    }
                } else {
                    // for efficiency reasons only one serviceType implemented by the unitType is regarded because the unitRemote is part of
                    // every abstractServiceRemotes internal units if the serviceType is implemented by the unitType
                    ServiceType serviceType;
                    try {
                        serviceType = Registries.getTemplateRegistry().getUnitTemplateByType(unitType).getServiceDescriptionList().get(0).getServiceType();
                    } catch (IndexOutOfBoundsException ex) {
                        // if there is not at least one serviceType for the unitType then the snapshot is empty
                        return snapshotBuilder.build();
                    }

                    for (final AbstractServiceRemote<?, ?> abstractServiceRemote : getServiceRemoteList()) {
                        if (!(serviceType == abstractServiceRemote.getServiceType())) {
                            continue;
                        }

                        Collection<UnitRemote> internalUnits = abstractServiceRemote.getInternalUnits();
                        for (final UnitRemote unitRemote : internalUnits) {
                            // just add units with the according type
                            if (unitRemote.getUnitType() == unitType) {
                                unitRemoteSet.add(unitRemote);
                            }
                        }
                    }
                }

                // take the snapshot
                final Map<UnitRemote, Future<SnapshotType.Snapshot>> snapshotFutureMap = new HashMap<UnitRemote, Future<SnapshotType.Snapshot>>();
                for (final UnitRemote<?> remote : unitRemoteSet) {
                    try {
                        if (UnitProcessor.isDalUnit(remote)) {
                            if (!remote.isConnected()) {
                                throw new NotAvailableException("Unit[" + remote.getLabel() + "] is currently not reachable!");
                            }
                            snapshotFutureMap.put(remote, remote.recordSnapshot());
                        }
                    } catch (CouldNotPerformException ex) {
                        ExceptionPrinter.printHistory(new CouldNotPerformException("Could not record snapshot of " + remote.getLabel(), ex), LOGGER, LogLevel.WARN);
                    }
                }

                // build snapshot
                for (final Map.Entry<UnitRemote, Future<SnapshotType.Snapshot>> snapshotFutureEntry : snapshotFutureMap.entrySet()) {
                    try {
                        snapshotBuilder.addAllServiceStateDescription(snapshotFutureEntry.getValue().get(5, TimeUnit.SECONDS).getServiceStateDescriptionList());
                    } catch (ExecutionException | TimeoutException ex) {
                        ExceptionPrinter.printHistory(new CouldNotPerformException("Could not record snapshot of " + snapshotFutureEntry.getKey().getLabel(), ex), LOGGER);
                    }
                }
                return snapshotBuilder.build();
            } catch (final CouldNotPerformException ex) {
                throw new CouldNotPerformException("Could not record snapshot!", ex);
            }
        });
    }

    @Override
    public Future<Void> restoreSnapshot(final Snapshot snapshot) {
        return UnitProcessor.restoreSnapshot(snapshot, LOGGER, getInternalUnitRemoteList());
    }

    @Override
    public Future<AuthenticatedValue> restoreSnapshotAuthenticated(AuthenticatedValue authenticatedSnapshot) {
        try {
            return UnitProcessor.restoreSnapshotAuthenticated(authenticatedSnapshot, LOGGER, responsibleUnit.getConfig() ,getInternalUnitRemoteList());
        } catch (NotAvailableException ex) {
            return FutureProcessor.canceledFuture(AuthenticatedValue.class, new CouldNotPerformException("Could not restore authenticated snapshot!", ex));
        }
    }

    /**
     * {@inheritDoc}
     *
     * @return {@inheritDoc}
     */
    @Override
    public Future<Long> ping() {
        try (final CloseableReadLockWrapper ignored = lockProvider.getCloseableReadLock(this)) {
            if (serviceRemoteMap.isEmpty()) {
                return FutureProcessor.completedFuture(0L);
            }

            final List<Future<Long>> futurePings = new ArrayList<>();

            for (final Remote<?> remote : serviceRemoteMap.values()) {
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

    public Future<ActionDescription> applyAction(ActionDescription actionDescription) {
        try {
            if (actionDescription.getServiceStateDescription().getUnitType().equals(responsibleUnit.getUnitType())) {
                ActionDescription.Builder builder = actionDescription.toBuilder();
                builder.getServiceStateDescriptionBuilder().setUnitType(UnitType.UNKNOWN);
                actionDescription = builder.build();
            }
            return getServiceRemote(actionDescription.getServiceStateDescription().getServiceType()).applyAction(actionDescription);
        } catch (NotAvailableException ex) {
            return FutureProcessor.canceledFuture(ex);
        }
    }

    /**
     * Apply an authenticated action with the default session manager. The authenticated value has to contain an action
     * description encrypted with the session key from the default manager if a user is logged in. Else an action description
     * as a byte string.
     * Select a service remote based on the service type provided in the action description and apply an authenticated action
     * on it. For detailed information refer to {@link AbstractServiceRemote#applyActionAuthenticated(AuthenticatedValue, ActionDescription.Builder, byte[])}.
     * <p>
     * Note: Future is canceled if no action description is available or can be extracted from the authenticated value.
     *
     * @param authenticatedValue The authenticated value containing an action description to be applied. Created with the
     *                           default session manager.
     *
     * @return An authenticated value containing an updated action description in which resulting actions are added as impacts.
     */
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
            return getServiceRemote(actionDescription.getServiceStateDescription().getServiceType()).applyActionAuthenticated(authenticatedValue, actionDescription.toBuilder(), SessionManager.getInstance().getSessionKey());
        } catch (CouldNotPerformException ex) {
            return FutureProcessor.canceledFuture(ex);
        }
    }

    /**
     * Select a service remote based on the service type provided in the action description and apply an authenticated action
     * on it. For detailed information refer to {@link AbstractServiceRemote#applyActionAuthenticated(AuthenticatedValue, ActionDescription.Builder, byte[])}.
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
            return getServiceRemote(actionDescriptionBuilder.getServiceStateDescription().getServiceType()).applyActionAuthenticated(authenticatedValue, actionDescriptionBuilder, sessionKey);
        } catch (NotAvailableException ex) {
            return FutureProcessor.canceledFuture(ex);
        }
    }

    protected abstract Set<ServiceType> getManagedServiceTypes() throws NotAvailableException, InterruptedException;

    protected abstract void notifyServiceUpdate(final Unit<?> source, final Message data) throws NotAvailableException, InterruptedException;

    @Override
    public boolean isDataAvailable() {
        return responsibleUnit.isDataAvailable();
    }

    @Override
    public Class<D> getDataClass() {
        return responsibleUnit.getDataClass();
    }

    @Override
    public D getData() throws NotAvailableException {
        return responsibleUnit.getData();
    }

    @Override
    public Future<D> getDataFuture() {
        return responsibleUnit.getDataFuture();
    }

    @Override
    public void addDataObserver(final Observer<DataProvider<D>, D> observer) {
        try (final CloseableReadLockWrapper ignored = lockProvider.getCloseableReadLock(this)) {
            for (final Remote<D> remote : serviceRemoteMap.values()) {
                remote.addDataObserver(observer);
            }
        }
    }

    @Override
    public void removeDataObserver(Observer<DataProvider<D>, D> observer) {
        try (final CloseableReadLockWrapper ignored = lockProvider.getCloseableReadLock(this)) {
            for (final Remote<D> remote : serviceRemoteMap.values()) {
                remote.removeDataObserver(observer);
            }
        }
    }

    @Override
    public void waitForData() throws CouldNotPerformException, InterruptedException {
        try (final CloseableReadLockWrapper ignored = lockProvider.getCloseableReadLock(this)) {
            for (final Remote<D> remote : serviceRemoteMap.values()) {
                remote.waitForData();
            }
        }
    }

    @Override
    public void waitForData(long timeout, TimeUnit timeUnit) throws CouldNotPerformException, InterruptedException {
        try (final CloseableReadLockWrapper ignored = lockProvider.getCloseableReadLock(this)) {
            for (final Remote<D> remote : serviceRemoteMap.values()) {
                // todo: split timeout
                remote.waitForData(timeout, timeUnit);
            }
        }
    }

    public <B> Future<B> requestData(final B builder) {
        try (final CloseableReadLockWrapper ignored = lockProvider.getCloseableReadLock(this)) {
            final List<Future<?>> futureData = new ArrayList<>();

            for (final Remote<?> remote : serviceRemoteMap.values()) {
                futureData.add(remote.requestData());
            }

            return FutureProcessor.allOf(() -> {
                try {
                    return updateBuilderWithAvailableServiceStates(builder);
                } catch (CouldNotPerformException ex) {
                    throw ExceptionPrinter.printHistoryAndReturnThrowable(new CouldNotPerformException("Could not generate data!", ex), LOGGER);
                }
            }, futureData);
        }
    }

    public Unit<D> getResponsibleUnit() {
        return responsibleUnit;
    }

    public void validateMiddleware() throws InvalidStateException {
        try (final CloseableReadLockWrapper ignored = lockProvider.getCloseableReadLock(this)) {
            for (AbstractServiceRemote<?, ?> value : serviceRemoteMap.values()) {
                for (Object internalUnit : value.getInternalUnits()) {
                    ((AbstractRemoteClient) internalUnit).validateMiddleware();
                }
            }
        }
    }

    @Override
    public void validateData() throws InvalidStateException {
        if (isDataAvailable()) {
            throw new InvalidStateException(new NotAvailableException("Data"));
        }
    }
}
