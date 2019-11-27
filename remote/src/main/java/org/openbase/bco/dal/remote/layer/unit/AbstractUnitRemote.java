package org.openbase.bco.dal.remote.layer.unit;

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

import com.google.protobuf.Message;
import org.openbase.bco.authentication.lib.AuthenticatedServiceProcessor;
import org.openbase.bco.authentication.lib.SessionManager;
import org.openbase.bco.authentication.lib.com.AbstractAuthenticatedConfigurableRemote;
import org.openbase.bco.authentication.lib.com.AuthenticatedGenericMessageProcessor;
import org.openbase.bco.authentication.lib.future.AuthenticatedValueFuture;
import org.openbase.bco.dal.lib.action.ActionDescriptionProcessor;
import org.openbase.bco.dal.lib.layer.service.ServiceDataFilteredObservable;
import org.openbase.bco.dal.lib.layer.service.ServiceStateProvider;
import org.openbase.bco.dal.lib.layer.service.Services;
import org.openbase.bco.dal.lib.layer.unit.UnitDataFilteredObservable;
import org.openbase.bco.dal.lib.layer.unit.UnitRemote;
import org.openbase.bco.dal.remote.layer.unit.location.LocationRemote;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.jps.core.JPService;
import org.openbase.jul.communication.controller.RPCHelper;
import org.openbase.jul.exception.*;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.exception.printer.LogLevel;
import org.openbase.jul.extension.protobuf.MessageObservable;
import org.openbase.jul.extension.rsb.scope.ScopeTransformer;
import org.openbase.jul.extension.type.processing.LabelProcessor;
import org.openbase.jul.extension.type.processing.ScopeProcessor;
import org.openbase.jul.extension.type.util.TransactionSynchronizationFuture;
import org.openbase.jul.pattern.Observer;
import org.openbase.jul.pattern.provider.DataProvider;
import org.openbase.jul.schedule.FutureProcessor;
import org.openbase.type.communication.ScopeType;
import org.openbase.type.domotic.action.ActionDescriptionType.ActionDescription;
import org.openbase.type.domotic.action.ActionParameterType.ActionParameterOrBuilder;
import org.openbase.type.domotic.action.SnapshotType.Snapshot;
import org.openbase.type.domotic.authentication.AuthTokenType.AuthToken;
import org.openbase.type.domotic.authentication.AuthenticatedValueType.AuthenticatedValue;
import org.openbase.type.domotic.database.QueryType;
import org.openbase.type.domotic.database.QueryType.Query;
import org.openbase.type.domotic.database.RecordCollectionType;
import org.openbase.type.domotic.database.RecordCollectionType.RecordCollection;
import org.openbase.type.domotic.database.RecordType;
import org.openbase.type.domotic.registry.UnitRegistryDataType.UnitRegistryData;
import org.openbase.type.domotic.service.ServiceDescriptionType.ServiceDescription;
import org.openbase.type.domotic.service.ServiceTemplateType.ServiceTemplate.ServicePattern;
import org.openbase.type.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import org.openbase.type.domotic.service.ServiceTempusTypeType.ServiceTempusType.ServiceTempus;
import org.openbase.type.domotic.state.AggregatedServiceStateType;
import org.openbase.type.domotic.state.AggregatedServiceStateType.AggregatedServiceState;
import org.openbase.type.domotic.state.DoorStateType;
import org.openbase.type.domotic.state.EnablingStateType.EnablingState;
import org.openbase.type.domotic.unit.UnitConfigType.UnitConfig;
import org.openbase.type.domotic.unit.UnitTemplateType.UnitTemplate;
import org.openbase.type.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;
import org.slf4j.LoggerFactory;
import rsb.Scope;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * @param <D> The unit data type.
 *
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public abstract class AbstractUnitRemote<D extends Message> extends AbstractAuthenticatedConfigurableRemote<D, UnitConfig> implements UnitRemote<D> {

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(ActionDescription.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(Snapshot.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(AuthenticatedValue.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(AggregatedServiceState.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(Query.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(RecordCollection.getDefaultInstance()));
    }

    private final Observer<DataProvider<UnitRegistryData>, UnitRegistryData> unitRegistryObserver;
    private final Map<ServiceTempus, UnitDataFilteredObservable<D>> unitDataObservableMap;
    private final Map<ServiceTempus, Map<ServiceType, MessageObservable<ServiceStateProvider<Message>, Message>>> serviceTempusServiceTypeObservableMap;
    private UnitTemplate template;
    private boolean initialized = false;
    private SessionManager sessionManager;
    private boolean infrastructure = false;

    public AbstractUnitRemote(final Class<D> dataClass) {
        super(dataClass, UnitConfig.class);

        this.unitRegistryObserver = new Observer<DataProvider<UnitRegistryData>, UnitRegistryData>() {
            @Override
            public void update(final DataProvider<UnitRegistryData> source, UnitRegistryData data) throws Exception {
                try {
                    final UnitConfig newUnitConfig = Registries.getUnitRegistry(true).getUnitConfigById(getId());
                    if (!newUnitConfig.equals(getConfig())) {
                        applyConfigUpdate(newUnitConfig);
                    }
                } catch (NotAvailableException ex) {
                    // unit config has been removed, probably because of deletion, Units will shutdown this remote
                    logger.debug("Could not update unit remote", ex);
                } catch (CouldNotPerformException ex) {
                    ExceptionPrinter.printHistory("Could not update unit config of " + this, ex, logger);
                }
            }
        };

        this.unitDataObservableMap = new HashMap<>();
        this.serviceTempusServiceTypeObservableMap = new HashMap<>();
        for (ServiceTempus serviceTempus : ServiceTempus.values()) {
            this.unitDataObservableMap.put(serviceTempus, new UnitDataFilteredObservable<>(this, serviceTempus));
            super.addDataObserver((DataProvider<D> source, D data1) -> {
                unitDataObservableMap.get(serviceTempus).notifyObservers(data1);
            });

            // skip registration of service state observable if tempus unknown
            if (serviceTempus == ServiceTempus.UNKNOWN) {
                continue;
            }

            this.serviceTempusServiceTypeObservableMap.put(serviceTempus, new HashMap<>());
            this.addDataObserver(serviceTempus, (source, data1) -> {
                final Set<ServiceType> serviceTypeSet = new HashSet<>();
                for (final ServiceDescription serviceDescription : AbstractUnitRemote.this.getUnitTemplate().getServiceDescriptionList()) {
                    if (serviceDescription.getPattern() == ServicePattern.PROVIDER && serviceTempus == ServiceTempus.REQUESTED) {
                        continue;
                    }
                    // check if already handled
                    if (serviceTypeSet.contains(serviceDescription.getServiceType())) {
                        continue;
                    }
                    serviceTypeSet.add(serviceDescription.getServiceType());

                    try {
                        Message serviceData = (Message) Services.invokeServiceMethod(serviceDescription.getServiceType(), ServicePattern.PROVIDER, serviceTempus, data1);
                        serviceTempusServiceTypeObservableMap.get(serviceTempus).get(serviceDescription.getServiceType()).notifyObservers(serviceData);
                    } catch (CouldNotPerformException ex) {
                        // todo: implement handling of super types which not always support all fields. e.g. A LightRemote can receive a ColorableLightData but can for sure not handle all field.
                        // checkout log output of test ColorableLightRemoteTest:testControllingViaLightRemote to reproduce the issue.
                        // close issue openbase/bco.dal#147 after fix
                        ExceptionPrinter.printHistory("Could not notify state update for service[" + serviceDescription.getServiceType() + "] in tempus[" + serviceTempus.name() + "]", ex, logger, LogLevel.WARN);
                    }
                }
            });
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param id
     *
     * @throws org.openbase.jul.exception.InitializationException
     * @throws java.lang.InterruptedException
     */
    @Override
    public void initById(final String id) throws InitializationException, InterruptedException {
        try {
            init(Registries.getUnitRegistry(false).getUnitConfigById(id));
        } catch (CouldNotPerformException ex) {
            throw new InitializationException(this, ex);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param scope
     *
     * @throws org.openbase.jul.exception.InitializationException
     * @throws java.lang.InterruptedException
     */
    @Override
    public void init(final ScopeType.Scope scope) throws InitializationException, InterruptedException {
        try {
            init(Registries.getUnitRegistry().getUnitConfigByScope(scope));
        } catch (CouldNotPerformException ex) {
            throw new InitializationException(this, ex);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param scope
     *
     * @throws org.openbase.jul.exception.InitializationException
     * @throws java.lang.InterruptedException
     */
    @Override
    public void init(final Scope scope) throws InitializationException, InterruptedException {
        try {
            this.init(ScopeTransformer.transform(scope));
        } catch (CouldNotPerformException ex) {
            throw new InitializationException(this, ex);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param scope
     *
     * @throws org.openbase.jul.exception.InitializationException
     * @throws java.lang.InterruptedException
     */
    @Override
    public void init(final String scope) throws InitializationException, InterruptedException {
        try {
            this.init(ScopeProcessor.generateScope(scope));
        } catch (CouldNotPerformException ex) {
            throw new InitializationException(this, ex);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @throws InitializationException {@inheritDoc}
     * @throws InterruptedException    {@inheritDoc}
     */
    @Override
    protected void postInit() throws InitializationException, InterruptedException {
        super.postInit();
        try {
            this.setMessageProcessor(new AuthenticatedGenericMessageProcessor<>(getDataClass()));

            if (!initialized) {
                Registries.getUnitRegistry().addDataObserver(unitRegistryObserver);
                initialized = true;
            }
        } catch (CouldNotPerformException ex) {
            throw new InitializationException(this, ex);
        }
    }

    @Override
    public void addServiceStateObserver(final ServiceTempus serviceTempus, final ServiceType serviceType, final Observer<ServiceStateProvider<Message>, Message> observer) {
        if (serviceTempus == ServiceTempus.UNKNOWN) {
            // if unknown tempus add observer on all other tempi
            for (ServiceTempus value : ServiceTempus.values()) {
                if (value == ServiceTempus.UNKNOWN) {
                    continue;
                }

                addServiceStateObserver(value, serviceType, observer);
            }
        } else {
            try {
                serviceTempusServiceTypeObservableMap.get(serviceTempus).get(serviceType).addObserver(observer);
            } catch (NullPointerException ex) {
                logger.warn("Non supported observer registration requested! {} does not support Service[{}] in ServiceTempus[{}]", this, serviceType, serviceTempus );
            }
        }
    }

    @Override
    public void removeServiceStateObserver(final ServiceTempus serviceTempus, final ServiceType serviceType, final Observer<ServiceStateProvider<Message>, Message> observer) {
        if (serviceTempus == ServiceTempus.UNKNOWN) {
            // if unknown tempus remove observer on all other tempi
            for (ServiceTempus value : ServiceTempus.values()) {
                if (value == ServiceTempus.UNKNOWN) {
                    continue;
                }

                removeServiceStateObserver(value, serviceType, observer);
            }
        } else {
            try {
                serviceTempusServiceTypeObservableMap.get(serviceTempus).get(serviceType).removeObserver(observer);
            } catch (NullPointerException ex) {
                logger.warn("Non supported observer removal requested! {} does not support Service[{}] in ServiceTempus[{}]", this, serviceType, serviceTempus );
            }
        }
    }

    @Override
    public void addDataObserver(Observer<DataProvider<D>, D> observer) {
        addDataObserver(ServiceTempus.CURRENT, observer);
    }

    @Override
    public void addDataObserver(ServiceTempus serviceTempus, Observer<DataProvider<D>, D> observer) {
        unitDataObservableMap.get(serviceTempus).addObserver(observer);
    }

    @Override
    public void removeDataObserver(Observer<DataProvider<D>, D> observer) {
        removeDataObserver(ServiceTempus.CURRENT, observer);
    }

    @Override
    public void removeDataObserver(ServiceTempus serviceTempus, Observer<DataProvider<D>, D> observer) {
        unitDataObservableMap.get(serviceTempus).removeObserver(observer);
    }

    /**
     * {@inheritDoc}
     *
     * @param unitConfig {@inheritDoc}
     *
     * @return {@inheritDoc}
     *
     * @throws org.openbase.jul.exception.CouldNotPerformException {@inheritDoc}
     * @throws java.lang.InterruptedException                      {@inheritDoc}
     */
    @Override
    public UnitConfig applyConfigUpdate(final UnitConfig unitConfig) throws CouldNotPerformException, InterruptedException {
        if (unitConfig == null) {
            throw new NotAvailableException("UnitConfig");
        }

        // non change filter
        try {
            if (getConfig().equals(unitConfig)) {
                logger.debug("Skip config update because no config change detected!");
                return unitConfig;
            }
        } catch (final NotAvailableException ex) {
            logger.trace("Unit config change check failed because config is not available yet.");
        }

        // update unit template
        template = Registries.getTemplateRegistry(true).getUnitTemplateByType(unitConfig.getUnitType());

        // register service observable which are not handled yet.
        for (ServiceTempus serviceTempus : ServiceTempus.values()) {
            unitDataObservableMap.get(serviceTempus).updateToUnitTemplateChange(template);

            // skip creation of service state observable if tempus unknown
            if (serviceTempus == ServiceTempus.UNKNOWN) {
                continue;
            }

            for (final ServiceDescription serviceDescription : template.getServiceDescriptionList()) {

                // filter because provider do not offer an requested state.
                if (serviceDescription.getPattern() == ServicePattern.PROVIDER && serviceTempus == ServiceTempus.REQUESTED) {
                    continue;
                }

                // create observable if new
                if (!serviceTempusServiceTypeObservableMap.get(serviceTempus).containsKey(serviceDescription.getServiceType())) {
                    serviceTempusServiceTypeObservableMap.get(serviceTempus).put(serviceDescription.getServiceType(), new ServiceDataFilteredObservable<>(new ServiceStateProvider<>(serviceDescription.getServiceType(), serviceTempus, this)));
                }
            }
        }

        // cleanup service observable related to new unit template
        for (Map<ServiceType, MessageObservable<ServiceStateProvider<Message>, Message>> serviceTypeObservableMap : serviceTempusServiceTypeObservableMap.values()) {
            outer:
            for (final ServiceType serviceType : serviceTypeObservableMap.keySet()) {
                for (final ServiceDescription serviceDescription : template.getServiceDescriptionList()) {

                    // verify if service type is still valid.
                    if (serviceType == serviceDescription.getServiceType()) {
                        // continue because service type is still valid
                        continue outer;
                    }
                }

                // remove and shutdown service observable because its not valid
                serviceTypeObservableMap.remove(serviceType).shutdown();
            }
        }

        final UnitConfig result = super.applyConfigUpdate(unitConfig);

        // Note: this block has to be executed after the super call because generating the variable pool uses
        // the internal unit config which is set in the super call
        try {
            infrastructure = Boolean.parseBoolean(generateVariablePool().getValue(META_CONFIG_UNIT_INFRASTRUCTURE_FLAG));
        } catch (NotAvailableException ex) {
            // pool or flag not available so set infrastructure to false
            infrastructure = false;
        }

        return result;
    }

    /**
     * {@inheritDoc}
     *
     * @throws InterruptedException     {@inheritDoc}
     * @throws CouldNotPerformException {@inheritDoc}
     */
    @Override
    public void activate() throws InterruptedException, CouldNotPerformException {
        if (!isEnabled()) {
            throw new InvalidStateException("The activation of an remote is not allowed if the referred unit is disabled!");
        }
        if (!Units.contains(this)) {
            logger.warn("You are using a unit remote which is not maintained by the global unit remote pool! This is extremely inefficient! Please use \"Units.getUnit(...)\" instead creating your own instances!");
        }
        super.activate();
    }

    /**
     * {@inheritDoc}
     *
     * @throws CouldNotPerformException {@inheritDoc}
     * @throws InterruptedException     {@inheritDoc}
     */
    @Override
    public void waitForData() throws CouldNotPerformException, InterruptedException {
        verifyEnablingState();
        super.waitForData();
    }

    /**
     * {@inheritDoc}
     *
     * @param timeout  {@inheritDoc}
     * @param timeUnit {@inheritDoc}
     *
     * @throws CouldNotPerformException       {@inheritDoc}
     * @throws java.lang.InterruptedException {@inheritDoc}
     */
    @Override
    public void waitForData(final long timeout, final TimeUnit timeUnit) throws CouldNotPerformException, InterruptedException {
        verifyEnablingState();
        super.waitForData(timeout, timeUnit);
    }

    /**
     * {@inheritDoc}
     *
     * @return {@inheritDoc}
     */
    @Override
    public boolean isEnabled() {
        try {
            assert (getConfig() instanceof UnitConfig);
            return getConfig().getEnablingState().getValue().equals(EnablingState.State.ENABLED);
        } catch (CouldNotPerformException ex) {
            LoggerFactory.getLogger(org.openbase.bco.dal.lib.layer.unit.UnitRemote.class).warn("isEnabled() was called on non initialized unit!");
            assert false;
        }
        return false;
    }

    private void verifyEnablingState() throws FatalImplementationErrorException {
        if (!isEnabled()) {
            if (JPService.testMode()) {
                throw new FatalImplementationErrorException("Waiting for data of a disabled unit should be avoided!", "Calling instance");
            } else {
                logger.warn("Waiting for data of an disabled unit should be avoided! Probably this method will block forever!");
            }
        }
    }

    /**
     * {@inheritDoc}
     *
     * @throws org.openbase.jul.exception.NotAvailableException
     */
    @Override
    public UnitType getUnitType() throws NotAvailableException {
        try {
            return getConfig().getUnitType();
        } catch (NullPointerException | NotAvailableException ex) {
            throw new NotAvailableException("unit type", ex);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @throws org.openbase.jul.exception.NotAvailableException
     */
    @Override
    public UnitTemplate getUnitTemplate() throws NotAvailableException {
        if (template == null) {
            throw new NotAvailableException("UnitTemplate");
        }
        return template;
    }

    /**
     * {@inheritDoc}
     *
     * @return
     *
     * @throws org.openbase.jul.exception.NotAvailableException
     */
    @Override
    public String getLabel() throws NotAvailableException {
        try {
            if (getSessionManager().isLoggedIn()) {
                try {
                    UnitConfig user = Registries.getUnitRegistry().getUnitConfigById(getSessionManager().getUserClientPair().getUserId());
                    return LabelProcessor.getLabelByLanguage(user.getUserConfig().getLanguage(), getConfig().getLabel());
                } catch (CouldNotPerformException ex) {
                    // as a backup use the first label as seen below
                    //TODO: this should parse a value from the root location meta config that defines a default label lang.
                }
            }
            return LabelProcessor.getBestMatch(getConfig().getLabel());
        } catch (NullPointerException | NotAvailableException ex) {
            throw new NotAvailableException("unit label", ex);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @return {@inheritDoc}
     *
     * @throws org.openbase.jul.exception.NotAvailableException {@inheritDoc}
     */
    @Override
    public ScopeType.Scope getScope() throws NotAvailableException {
        try {
            return getConfig().getScope();
        } catch (NullPointerException | CouldNotPerformException ex) {
            throw new NotAvailableException("unit label", ex);
        }
    }

    /**
     * This method returns the parent location remote of this unit.
     * If this unit is a location, than its parent location remote is returned,
     * otherwise the parent location remote is returned which refers the location where this unit is placed in.
     *
     * @param waitForData flag defines if the method should block until the remote is fully synchronized.
     *
     * @return a location remote instance.
     *
     * @throws NotAvailableException          is thrown if the location remote is currently not available.
     * @throws java.lang.InterruptedException is thrown if the current was externally interrupted.
     */
    public LocationRemote getParentLocationRemote(final boolean waitForData) throws NotAvailableException, InterruptedException {
        return Units.getUnit(getConfig().getPlacementConfig().getLocationId(), waitForData, Units.LOCATION);
    }

    public Future<ActionDescription> applyAction(ActionDescription.Builder actionDescriptionBuilder) {
        return applyAction(actionDescriptionBuilder.build());
    }

    /**
     * {@inheritDoc}
     *
     * @param actionDescription {@inheritDoc}
     *
     * @return {@inheritDoc}
     */
    @Override
    public Future<ActionDescription> applyAction(ActionDescription actionDescription) {

        // validate action
        if ((actionDescription.getCancel() || actionDescription.getExtend()) && !actionDescription.hasActionId()) {
            return FutureProcessor.canceledFuture(ActionDescription.class, new InvalidStateException("Action Id is required when an action is extended or canceled!"));
        }

        if ((!actionDescription.getCancel() && !actionDescription.getExtend()) && actionDescription.hasActionId()) {
            logger.warn("New action offers an id which will be overwritten by controller!");
        }

        return AuthenticatedServiceProcessor.requestAuthenticatedAction(actionDescription, ActionDescription.class, this.getSessionManager(), authenticatedValue -> applyActionAuthenticated(authenticatedValue));
    }

    @Override
    public Future<ActionDescription> applyAction(final ActionDescription actionDescription, final AuthToken authToken) {
        try {
            final ActionDescription.Builder actionDescripBuilder = actionDescription.toBuilder();
            if (actionDescripBuilder.getServiceStateDescriptionBuilder().getUnitId().isEmpty()) {
                actionDescripBuilder.getServiceStateDescriptionBuilder().setUnitId(getId());
            }
            if (SessionManager.getInstance().isLoggedIn() && (authToken != null)) {
                final AuthenticatedValue authenticatedValue = SessionManager.getInstance().initializeRequest(actionDescripBuilder.build(), authToken);
                final Future<AuthenticatedValue> future = applyActionAuthenticated(authenticatedValue);
                return new AuthenticatedValueFuture<>(future, ActionDescription.class, authenticatedValue.getTicketAuthenticatorWrapper(), SessionManager.getInstance());
            } else {
                return applyAction(actionDescripBuilder.build());
            }
        } catch (CouldNotPerformException ex) {
            return FutureProcessor.canceledFuture(ActionDescription.class, ex);
        }
    }

    @Override
    public Future<AuthenticatedValue> applyActionAuthenticated(final AuthenticatedValue authenticatedValue) {
        return new TransactionSynchronizationFuture<>(RPCHelper.callRemoteMethod(authenticatedValue, this, AuthenticatedValue.class), this);
    }

    @Override
    public SessionManager getSessionManager() {
        return sessionManager;
    }

    /**
     * Sets the session Manager which is used for the authentication of the
     * client/user
     *
     * @param sessionManager an instance of SessionManager
     */
    @Override
    public void setSessionManager(SessionManager sessionManager) {
        this.sessionManager = sessionManager;
    }

    @Override
    public void shutdown() {
        super.shutdown();

        try {
            // shutdown registry observer
            Registries.getUnitRegistry().removeDataObserver(unitRegistryObserver);
        } catch (final NotAvailableException ex) {
            // if the registry is not any longer available (in case of registry shutdown) than the observer is already cleared
        } catch (final Exception ex) {
            ExceptionPrinter.printHistory(new CouldNotPerformException("Could not remove unit registry observer.", ex), logger);
        }

        for (final ServiceTempus serviceTempus : ServiceTempus.values()) {
            // skip unknown tempus
            if (serviceTempus == ServiceTempus.UNKNOWN) {
                continue;
            }

            // shutdown service observer
            for (final MessageObservable serviceObservable : serviceTempusServiceTypeObservableMap.get(serviceTempus).values()) {
                serviceObservable.shutdown();
            }
            unitDataObservableMap.get(serviceTempus).shutdown();
        }
    }

    @Override
    public Future<Void> restoreSnapshot(Snapshot snapshot) {
        return AuthenticatedServiceProcessor.requestAuthenticatedAction(snapshot, Void.class, getSessionManager(), authenticatedSnapshot -> restoreSnapshotAuthenticated(authenticatedSnapshot));
    }

    @Override
    public Future<AuthenticatedValue> restoreSnapshotAuthenticated(AuthenticatedValue authenticatedSnapshot) {
        return new TransactionSynchronizationFuture<>(RPCHelper.callRemoteMethod(authenticatedSnapshot, this, AuthenticatedValue.class), this);
    }

    @Override
    public Message getServiceState(final ServiceType serviceType) throws NotAvailableException {
        try {
            return Services.invokeProviderServiceMethod(serviceType, getData());
        } catch (CouldNotPerformException ex) {
            throw new NotAvailableException("ServiceState", ex);
        }
    }

    /**
     * Method prints a class instance representation.
     *
     * @return the class string representation.
     */
    @Override
    public String toString() {
        try {
            return getClass().getSimpleName() + "[scope:" + ScopeProcessor.generateStringRep(scope) + "]";
        } catch (CouldNotPerformException ex) {
            try {
                return getClass().getSimpleName() + "[label:" + getLabel() + "]";
            } catch (CouldNotPerformException exx) {
                return super.toString();
            }
        }
    }

    /**
     * {@inheritDoc}
     *
     * @return {@inheritDoc}
     */
    @Override
    public boolean isInfrastructure() {
        return infrastructure;
    }


    public Future<AuthenticatedValue> queryAggregatedServiceStateAuthenticated(final AuthenticatedValue databaseQuery) {
        return RPCHelper.callRemoteMethod(databaseQuery, this, AuthenticatedValue.class);
    }

    @Override
    public Future<AggregatedServiceStateType.AggregatedServiceState> queryAggregatedServiceState(final QueryType.Query databaseQuery) {
        return AuthenticatedServiceProcessor.requestAuthenticatedAction(databaseQuery, AggregatedServiceState.class, SessionManager.getInstance(), authenticatedValue -> queryAggregatedServiceStateAuthenticated(authenticatedValue));

    }

    public Future<AuthenticatedValue> queryRecordAuthenticated(final AuthenticatedValue databaseQuery) {
        return RPCHelper.callRemoteMethod(databaseQuery, this, AuthenticatedValue.class);
    }

    @Override
    public Future<RecordCollectionType.RecordCollection> queryRecord(final QueryType.Query databaseQuery) {
        return AuthenticatedServiceProcessor.requestAuthenticatedAction(databaseQuery, RecordCollection.class, SessionManager.getInstance(), authenticatedValue -> queryRecordAuthenticated(authenticatedValue));
    }
}
