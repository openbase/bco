package org.openbase.bco.dal.control.layer.unit;

/*
 * #%L
 * BCO DAL Control
 * %%
 * Copyright (C) 2014 - 2021 openbase.org
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */

import com.google.protobuf.AbstractMessage;
import com.google.protobuf.Descriptors;
import com.google.protobuf.Descriptors.EnumValueDescriptor;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.Message;
import lombok.val;
import org.openbase.bco.authentication.lib.*;
import org.openbase.bco.authentication.lib.AuthenticatedServiceProcessor.InternalIdentifiedProcessable;
import org.openbase.bco.authentication.lib.AuthorizationHelper.PermissionType;
import org.openbase.bco.authentication.lib.com.AbstractAuthenticatedConfigurableController;
import org.openbase.bco.authentication.lib.jp.JPAuthentication;
import org.openbase.bco.dal.control.action.ActionImpl;
import org.openbase.bco.dal.lib.action.Action;
import org.openbase.bco.dal.lib.action.ActionComparator;
import org.openbase.bco.dal.lib.action.ActionDescriptionProcessor;
import org.openbase.bco.dal.lib.action.SchedulableAction;
import org.openbase.bco.dal.lib.jp.JPProviderControlMode;
import org.openbase.bco.dal.lib.layer.service.*;
import org.openbase.bco.dal.lib.layer.service.consumer.ConsumerService;
import org.openbase.bco.dal.lib.layer.service.operation.OperationService;
import org.openbase.bco.dal.lib.layer.service.provider.ProviderService;
import org.openbase.bco.dal.lib.layer.service.stream.StreamService;
import org.openbase.bco.dal.lib.layer.unit.Unit;
import org.openbase.bco.dal.lib.layer.unit.UnitController;
import org.openbase.bco.dal.lib.layer.unit.UnitDataFilteredObservable;
import org.openbase.bco.dal.lib.layer.unit.UnitProcessor;
import org.openbase.bco.dal.lib.layer.unit.agent.AgentController;
import org.openbase.bco.dal.lib.layer.unit.app.AppController;
import org.openbase.bco.dal.lib.layer.unit.user.User;
import org.openbase.bco.dal.lib.state.States.Activation;
import org.openbase.bco.dal.lib.state.States.Power;
import org.openbase.bco.dal.remote.layer.unit.Units;
import org.openbase.bco.dal.remote.layer.unit.location.LocationRemote;
import org.openbase.bco.registry.lib.util.UnitConfigProcessor;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.bco.registry.unit.lib.UnitRegistry;
import org.openbase.bco.registry.unit.lib.auth.AuthorizationWithTokenHelper;
import org.openbase.jps.core.JPService;
import org.openbase.jps.exception.JPNotAvailableException;
import org.openbase.jul.communication.controller.RPCUtils;
import org.openbase.jul.communication.iface.RPCServer;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.exception.*;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.exception.printer.LogLevel;
import org.openbase.jul.extension.protobuf.BuilderSyncSetup;
import org.openbase.jul.extension.protobuf.BuilderSyncSetup.NotificationStrategy;
import org.openbase.jul.extension.protobuf.ClosableDataBuilder;
import org.openbase.jul.extension.protobuf.MessageObservable;
import org.openbase.jul.extension.protobuf.processing.ProtoBufFieldProcessor;
import org.openbase.jul.extension.type.iface.ScopeProvider;
import org.openbase.jul.extension.type.processing.*;
import org.openbase.jul.iface.TimedProcessable;
import org.openbase.jul.pattern.Observer;
import org.openbase.jul.pattern.provider.DataProvider;
import org.openbase.jul.processing.StringProcessor;
import org.openbase.jul.schedule.*;
import org.openbase.type.communication.ScopeType;
import org.openbase.type.domotic.action.ActionDescriptionType.ActionDescription;
import org.openbase.type.domotic.action.ActionDescriptionType.ActionDescription.Builder;
import org.openbase.type.domotic.action.ActionEmphasisType.ActionEmphasis.Category;
import org.openbase.type.domotic.action.ActionInitiatorType.ActionInitiator;
import org.openbase.type.domotic.action.ActionInitiatorType.ActionInitiator.InitiatorType;
import org.openbase.type.domotic.action.ActionParameterType.ActionParameter;
import org.openbase.type.domotic.action.ActionParameterType.ActionParameterOrBuilder;
import org.openbase.type.domotic.action.ActionPriorityType.ActionPriority.Priority;
import org.openbase.type.domotic.action.ActionReferenceType.ActionReference;
import org.openbase.type.domotic.action.SnapshotType.Snapshot;
import org.openbase.type.domotic.authentication.AuthenticatedValueType.AuthenticatedValue;
import org.openbase.type.domotic.authentication.UserClientPairType.UserClientPair;
import org.openbase.type.domotic.database.QueryType;
import org.openbase.type.domotic.database.RecordCollectionType.RecordCollection;
import org.openbase.type.domotic.registry.UnitRegistryDataType.UnitRegistryData;
import org.openbase.type.domotic.service.ServiceDescriptionType.ServiceDescription;
import org.openbase.type.domotic.service.ServiceStateDescriptionType;
import org.openbase.type.domotic.service.ServiceTemplateType.ServiceTemplate.ServicePattern;
import org.openbase.type.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import org.openbase.type.domotic.service.ServiceTempusTypeType.ServiceTempusType.ServiceTempus;
import org.openbase.type.domotic.state.ActionStateType.ActionState;
import org.openbase.type.domotic.state.ActionStateType.ActionState.State;
import org.openbase.type.domotic.state.AggregatedServiceStateType.AggregatedServiceState;
import org.openbase.type.domotic.unit.UnitConfigType.UnitConfig;
import org.openbase.type.domotic.unit.UnitTemplateType.UnitTemplate;
import org.openbase.type.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;
import org.openbase.type.timing.TimestampType.Timestamp;

import java.io.Serializable;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static org.openbase.type.domotic.service.ServiceTemplateType.ServiceTemplate.ServicePattern.OPERATION;
import static org.openbase.type.domotic.service.ServiceTemplateType.ServiceTemplate.ServicePattern.PROVIDER;

/**
 * @param <D>  the data type of this unit used for the state synchronization.
 * @param <DB> the builder used to build the unit data instance.
 *
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public abstract class AbstractUnitController<D extends AbstractMessage & Serializable, DB extends D.Builder<DB>> extends AbstractAuthenticatedConfigurableController<D, DB, UnitConfig> implements UnitController<D, DB> {

    private static final SessionManager MOCKUP_SESSION_MANAGER = new SessionManager();
    private static final String TERMINATION_ACTION_NOT_AVAILABLE = "n/a";

    /**
     * Timeout defining how long finished actions will be minimally kept in the action list.
     */
    private static final long FINISHED_ACTION_REMOVAL_TIMEOUT = JPService.testMode() ? TimeUnit.SECONDS.toMillis(30) : TimeUnit.SECONDS.toMillis(60);

    private static final long SUBMISSION_ACTION_MATCHING_TIMEOUT = JPService.testMode() ? TimeUnit.SECONDS.toMillis(1) : TimeUnit.SECONDS.toMillis(20);
    private static final ServiceJSonProcessor SERVICE_JSON_PROCESSOR = new ServiceJSonProcessor();

    private static final String LOCK_CONSUMER_NOTIFICATION = AbstractUnitController.class.getSimpleName() + ".notifyScheduledActionList(..)";
    private static final String LOCK_CONSUMER_SCHEDULING = AbstractUnitController.class.getSimpleName() + ".reschedule(..)";
    private static final String LOCK_CONSUMER_INDEX_LOOKUP = AbstractUnitController.class.getSimpleName() + ".getSchedulingIndex()";
    private static final String LOCK_CONSUMER_CANCEL_ACTION = AbstractUnitController.class.getSimpleName() + ".cancelAction(..)";
    private static final String LOCK_CONSUMER_EXTEND_ACTION = AbstractUnitController.class.getSimpleName() + ".extendAction(..)";

    final BuilderSyncSetup<DB> builderSetup;
    private final Observer<DataProvider<UnitRegistryData>, UnitRegistryData> unitRegistryObserver;
    private final Map<ServiceTempus, UnitDataFilteredObservable<D>> unitDataObservableMap;
    private final Map<ServiceTempus, Map<ServiceType, MessageObservable<ServiceStateProvider<Message>, Message>>> serviceTempusServiceTypeObservableMap;
    private final ActionComparator actionComparator;
    private final SyncObject requestedStateCacheSync = new SyncObject("RequestedStateCacheSync");
    private final Map<ServiceType, Message> requestedStateCache;
    private final Map<ServiceType, OperationService> operationServiceMap;
    private UnitTemplate template;
    private boolean initialized = false;
    private String classDescription = "";
    private final ArrayList<SchedulableAction> scheduledActionList;
    private final Timeout scheduleTimeout;
    private boolean infrastructure = false;

    public AbstractUnitController(final DB builder) throws InstantiationException {
        super(builder);
        this.unitDataObservableMap = new HashMap<>();
        this.operationServiceMap = new TreeMap<>();
        this.scheduledActionList = new ArrayList<>();
        this.serviceTempusServiceTypeObservableMap = new HashMap<>();
        this.builderSetup = getBuilderSetup();
        for (final ServiceTempus serviceTempus : ServiceTempus.values()) {
            unitDataObservableMap.put(serviceTempus, new UnitDataFilteredObservable<>(this, serviceTempus));
            super.addDataObserver((DataProvider<D> source, D data) -> {
                unitDataObservableMap.get(serviceTempus).notifyObservers(data);
            });

            if (serviceTempus == ServiceTempus.UNKNOWN) {
                continue;
            }
            serviceTempusServiceTypeObservableMap.put(serviceTempus, new HashMap<>());
        }
        this.unitRegistryObserver = new Observer<DataProvider<UnitRegistryData>, UnitRegistryData>() {
            @Override
            public void update(DataProvider<UnitRegistryData> source, UnitRegistryData data) throws Exception {
                try {
                    final UnitConfig newUnitConfig = Registries.getUnitRegistry(true).getUnitConfigById(getId());
                    if (!newUnitConfig.equals(getConfig())) {
                        applyConfigUpdate(newUnitConfig);
                    }
                } catch (NotAvailableException ex) {
                    // unit config has been removed, probably because of deletion and a higher controller will do the shutdown in this case
                    logger.debug("Could not update unit controller", ex);
                } catch (CouldNotPerformException ex) {
                    ExceptionPrinter.printHistory("Could not update unit config of " + this, ex, logger);
                }
            }
        };

        this.requestedStateCache = new HashMap<>();

        this.addDataObserver(ServiceTempus.REQUESTED, (source, data) -> {
            // update requested state cache and create timeouts
            for (ServiceDescription serviceDescription : getUnitTemplate().getServiceDescriptionList()) {

                // skip if the service is not an operation service
                if (serviceDescription.getPattern() != OPERATION) {
                    continue;
                }

                // skip if the update contains a cleared requested state
                if (!Services.hasServiceState(serviceDescription.getServiceType(), ServiceTempus.REQUESTED, data)) {
                    continue;
                }

                // synchronize access to requestedStateCache
                synchronized (requestedStateCacheSync) {
                    // put new value into the cache
                    requestedStateCache.put(serviceDescription.getServiceType(), Services.invokeProviderServiceMethod(serviceDescription.getServiceType(), ServiceTempus.REQUESTED, data));
                }
            }
        });

        this.scheduleTimeout = new Timeout(1000) {
            @Override
            public void expired() {
                logger.debug("Reschedule by timer.");
                try {
                    reschedule();
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                } catch (CouldNotPerformException ex) {
                    if (!ExceptionProcessor.isCausedBySystemShutdown(ex)) {
                        ExceptionPrinter.printHistory(new CouldNotPerformException("Could not reschedule via timer!", ex), logger, LogLevel.WARN);
                    }
                }
            }
        };

        this.actionComparator = new ActionComparator(() -> getParentLocationRemote(false).getEmphasisState());
    }

    public static Class<? extends UnitController> detectUnitControllerClass(final UnitConfig unitConfig) throws CouldNotTransformException {

        String className = AbstractUnitController.class.getPackage().getName() + "." + StringProcessor.transformUpperCaseToPascalCase(unitConfig.getUnitType().name()) + "Controller";
        try {
            return (Class<? extends UnitController>) Class.forName(className);
        } catch (ClassNotFoundException ex) {
            try {
                throw new CouldNotTransformException(ScopeProcessor.generateStringRep(unitConfig.getScope()), UnitController.class, new NotAvailableException("Class", ex));
            } catch (CouldNotPerformException ex1) {
                throw new CouldNotTransformException(unitConfig.getLabel(), UnitController.class, new NotAvailableException("Class", ex));
            }
        }
    }

    protected long getShutdownDelay() {
        if (this instanceof AppController) {
            return 0;
        } else if (this instanceof AgentController) {
            return 0;
        } else {
            return 2000;
        }
    }

    @Override
    public void init(ScopeType.Scope scope) throws InitializationException, InterruptedException {
        try {
            super.init(Registries.getUnitRegistry(true).getUnitConfigByScope(scope));
        } catch (CouldNotPerformException ex) {
            throw new InitializationException(this, ex);
        }
    }

    public void init(final String label, final ScopeProvider location) throws InitializationException, InterruptedException {
        try {
            init(ScopeProcessor.generateScope(label, getClass().getSimpleName(), location.getScope()));
        } catch (CouldNotPerformException | NullPointerException ex) {
            throw new InitializationException(this, ex);
        }
    }

    @Override
    public void init(final UnitConfig config) throws InitializationException, InterruptedException {
        try {
            if (config == null) {
                throw new NotAvailableException("config");
            }

            if (!config.hasId()) {
                throw new NotAvailableException("config.id");
            }

            if (config.getId().isEmpty()) {
                throw new NotAvailableException("Field config.id is empty!");
            }

            if (!config.hasLabel()) {
                throw new NotAvailableException("config.label");
            }

            if (LabelProcessor.isEmpty(config.getLabel())) {
                throw new NotAvailableException("Field config.label is empty!");
            }

            super.init(config);

            // init terminating action if not yet done
            registerTerminatingAction();
        } catch (CouldNotPerformException ex) {
            throw new InitializationException(this, ex);
        }
    }

    @Override
    protected void postInit() throws InitializationException, InterruptedException {
        try {
            super.postInit();
            if (!initialized) {
                Registries.getUnitRegistry().addDataObserver(unitRegistryObserver);
                initialized = true;
            }
        } catch (CouldNotPerformException ex) {
            throw new InitializationException(this, ex);
        }
    }

    @Override
    protected void notifyDataUpdate(final D data) throws CouldNotPerformException {
        super.notifyDataUpdate(data);

        for (final ServiceTempus serviceTempus : ServiceTempus.values()) {
            if (serviceTempus == ServiceTempus.UNKNOWN) {
                continue;
            }

            final Set<ServiceType> serviceTypeSet = new HashSet<>();
            for (final ServiceDescription serviceDescription : getUnitTemplate().getServiceDescriptionList()) {
                // check if already handled
                if (!serviceTypeSet.contains(serviceDescription.getServiceType())) {
                    serviceTypeSet.add(serviceDescription.getServiceType());
                    try {
                        Message serviceData = (Message) Services.invokeServiceMethod(serviceDescription.getServiceType(), ServicePattern.PROVIDER, serviceTempus, data);
                        serviceTempusServiceTypeObservableMap.get(serviceTempus).get(serviceDescription.getServiceType()).notifyObservers(serviceData);
                    } catch (CouldNotPerformException ex) {
                        if (logger.isDebugEnabled()) {
                            logger.trace("Could not notify state update for service[{}] because this service is not supported by this controller: {}", serviceDescription.getServiceType(), ExceptionProcessor.getInitialCauseMessage(ex));
                        }
                    }
                }
            }
        }
    }

    public boolean isEnabled() {
        try {
            return UnitConfigProcessor.isEnabled(getConfig());
        } catch (CouldNotPerformException ex) {
            ExceptionPrinter.printHistory(ex, logger);
        }
        return false;
    }

    @Override
    public UnitConfig applyConfigUpdate(final UnitConfig config) throws CouldNotPerformException, InterruptedException {

        try (final CloseableWriteLockWrapper ignored = getManageWriteLockInterruptible(this)) {

            if (config == null) {
                throw new NotAvailableException("UnitConfig");
            }

            // non change filter
            try {
                if (getConfig().equals(config)) {
                    logger.debug("Skip config update because no config change detected!");
                    return config;
                }
            } catch (NotAvailableException ex) {
                logger.trace("Unit config change check failed because config is not available yet.");
            }

            try {
                classDescription = getClass().getSimpleName() + "[" + config.getUnitType() + "[" + LabelProcessor.getBestMatch(config.getLabel()) + "]]";
            } catch (NullPointerException | NotAvailableException ex) {
                classDescription = getClass().getSimpleName() + "[?]";
            }

            template = Registries.getTemplateRegistry(true).getUnitTemplateByType(config.getUnitType());

            // register service observable which are not handled yet.
            for (final ServiceTempus serviceTempus : ServiceTempus.values()) {
                unitDataObservableMap.get(serviceTempus).updateToUnitTemplateChange(template);

                if (serviceTempus == ServiceTempus.UNKNOWN) {
                    continue;
                }
                for (final ServiceDescription serviceDescription : template.getServiceDescriptionList()) {

                    // create observable if new
                    if (!serviceTempusServiceTypeObservableMap.get(serviceTempus).containsKey(serviceDescription.getServiceType())) {
                        serviceTempusServiceTypeObservableMap.get(serviceTempus).put(serviceDescription.getServiceType(), new ServiceDataFilteredObservable<>(new ServiceStateProvider<>(serviceDescription.getServiceType(), serviceTempus, this)));
                    }
                }
            }

            for (final ServiceTempus serviceTempus : ServiceTempus.values()) {
                if (serviceTempus == ServiceTempus.UNKNOWN) {
                    continue;
                }
                // cleanup service observable related to new unit template
                outer:
                for (final ServiceType serviceType : serviceTempusServiceTypeObservableMap.get(serviceTempus).keySet()) {
                    for (final ServiceDescription serviceDescription : template.getServiceDescriptionList()) {

                        // verify if service type is still valid.
                        if (serviceType == serviceDescription.getServiceType()) {
                            // continue because service type is still valid
                            continue outer;
                        }
                    }

                    // remove and shutdown service observable because its not valid
                    serviceTempusServiceTypeObservableMap.get(serviceTempus).remove(serviceType).shutdown();
                }
            }

            final UnitConfig result = super.applyConfigUpdate(config);

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
    }

    @Override
    public final String getId() throws NotAvailableException {
        try {
            UnitConfig tmpConfig = getConfig();
            if (!tmpConfig.hasId()) {
                throw new NotAvailableException("unitconfig.id");
            }

            if (tmpConfig.getId().isEmpty()) {
                throw new InvalidStateException("unitconfig.id is empty");
            }

            return tmpConfig.getId();
        } catch (CouldNotPerformException ex) {
            throw new NotAvailableException("Unit", "id", ex);
        }
    }

    @Override
    public String getLabel() throws NotAvailableException {
        try {
            UnitConfig tmpConfig = getConfig();
            if (!tmpConfig.hasLabel()) {
                throw new NotAvailableException("unitconfig.label");
            }

            if (LabelProcessor.isEmpty(tmpConfig.getLabel())) {
                throw new InvalidStateException("unitconfig.label is empty");
            }
            return LabelProcessor.getBestMatch(getConfig().getLabel());
        } catch (CouldNotPerformException ex) {
            throw new NotAvailableException("Unit", "label", ex);
        }
    }

    @Override
    public UnitType getUnitType() throws NotAvailableException {
        return getConfig().getUnitType();
    }

    @Override
    public UnitTemplate getUnitTemplate() throws NotAvailableException {
        if (template == null) {
            throw new NotAvailableException("UnitTemplate");
        }
        return template;
    }

    @Override
    public void registerMethods(final RPCServer server) throws CouldNotPerformException {
        super.registerMethods(server);

        server.registerMethods(Unit.class, this);

        // collect and register service interface methods via unit templates
        HashMap<String, ServiceDescription> serviceInterfaceMap = new HashMap<>();
        for (ServiceDescription serviceDescription : getUnitTemplate().getServiceDescriptionList()) {
            final String serviceInterfaceName =
                    StringProcessor.transformUpperCaseToPascalCase(serviceDescription.getServiceType().name()) +
                            StringProcessor.transformUpperCaseToPascalCase(serviceDescription.getPattern().name());
            serviceInterfaceMap.put(serviceInterfaceName, serviceDescription);
        }

        Class<? extends Service> serviceInterfaceClass = null;
        Package servicePackage = null;
        for (Entry<String, ServiceDescription> serviceInterfaceMapEntry : serviceInterfaceMap.entrySet()) {
            try {
                if (null != serviceInterfaceMapEntry.getValue().getPattern()) // Identify package
                {
                    switch (serviceInterfaceMapEntry.getValue().getPattern()) {
                        case CONSUMER:
                            servicePackage = ConsumerService.class.getPackage();
                            break;
                        case OPERATION:
                            servicePackage = OperationService.class.getPackage();
                            break;
                        case PROVIDER:
                            servicePackage = ProviderService.class.getPackage();
                            break;
                        case STREAM:
                            servicePackage = StreamService.class.getPackage();
                            break;
                        default:
                            throw new NotSupportedException(serviceInterfaceMapEntry.getKey(), this);
                    }
                }

                // Identify interface class
                String serviceDataTypeName = StringProcessor.transformUpperCaseToPascalCase(serviceInterfaceMapEntry.getValue().getServiceType().name()).replaceAll("Service", "");
                String servicePatternName = StringProcessor.transformUpperCaseToPascalCase(serviceInterfaceMapEntry.getValue().getPattern().name());
                String serviceClassName = servicePackage.getName() + "." + serviceDataTypeName + servicePatternName + "Service";
                try {
                    serviceInterfaceClass = (Class<? extends Service>) Class.forName(serviceClassName);
                } catch (ClassNotFoundException | ClassCastException ex) {
                    throw new CouldNotPerformException("Could not load service interface!", ex);
                }

                if (!serviceInterfaceClass.isAssignableFrom(this.getClass())) {
                    // interface not supported dummy.
                    throw new CouldNotPerformException("Interface[" + serviceInterfaceClass.getName() + "] is not supported by " + this);
                }

                server.registerMethods((Class) serviceInterfaceClass, this);
            } catch (CouldNotPerformException ex) {
                ExceptionPrinter.printHistory(new CouldNotPerformException("Could not register Interface[" + serviceInterfaceClass + "] Method [" + serviceInterfaceMapEntry.getKey() + "] for Unit[" + this.getLabel() + "].", ex), logger);
            }
        }
    }

    private String terminatingActionId;
    private static final Map<ServiceType, Message> SERVICE_TYPE_TERMINATION_MAPPING = new HashMap<>();

    static {
        SERVICE_TYPE_TERMINATION_MAPPING.put(ServiceType.POWER_STATE_SERVICE, Power.OFF);
        SERVICE_TYPE_TERMINATION_MAPPING.put(ServiceType.ACTIVATION_STATE_SERVICE, Activation.INACTIVE);
    }

    public void registerTerminatingAction() throws InterruptedException {
        // this is just a workaround to avoid duplicated registration.
        if (terminatingActionId != null) {
            return;
        }

        builderSetup.lockWriteInterruptibly(LOCK_CONSUMER_SCHEDULING);
        try {
            terminatingActionId = TERMINATION_ACTION_NOT_AVAILABLE;

            try {
                // do not register any termination action for infrastructure units, since they should only be modified in especially requested.
                if (isInfrastructure()) {
                    return;
                }

                // resolve supported service types
                final HashSet<ServiceType> serviceTypes = new HashSet<>();
                for (ServiceDescription serviceDescription : getUnitTemplate().getServiceDescriptionList()) {

                    // termination actions make only sense for operation services, so skip the rest.
                    if (serviceDescription.getPattern() != OPERATION) {
                        continue;
                    }

                    // ignore aggregated services since actions would affect downstream units.
                    if (serviceDescription.getAggregated()) {
                        continue;
                    }

                    // skip not supported services
                    if (!SERVICE_TYPE_TERMINATION_MAPPING.containsKey(serviceDescription.getServiceType())) {
                        continue;
                    }

                    // store all supported ones that are left
                    serviceTypes.add(serviceDescription.getServiceType());
                }

                // no termination required if none of the services the unit offers are supported.
                if (serviceTypes.isEmpty()) {
                    return;
                }

                // warn if too many services need to be terminated.
                if (serviceTypes.size() > 2) {
                    // this is an issue because there can only be one termination action be active at a time.
                    logger.warn("Unit does support more than one service that has to be terminated. This is not supported until the scheduling is done on service level.");
                }

                // generate action parameter for service to terminate
                final ServiceType serviceToTerminate = serviceTypes.iterator().next();

                final ActionParameter.Builder actionParameter = ActionDescriptionProcessor.generateDefaultActionParameter(SERVICE_TYPE_TERMINATION_MAPPING.get(serviceToTerminate), serviceToTerminate, this);
                actionParameter.setInterruptible(true);
                actionParameter.setSchedulable(true);
                actionParameter.setPriority(Priority.TERMINATION);
                actionParameter.addCategory(Category.ECONOMY);
                actionParameter.getActionInitiatorBuilder().setInitiatorType(InitiatorType.SYSTEM);
                actionParameter.setExecutionTimePeriod(TimeUnit.MILLISECONDS.toMicros(TimedProcessable.INFINITY_TIMEOUT));

                final ActionImpl action = new ActionImpl(ActionDescriptionProcessor.generateActionDescriptionBuilder(actionParameter).build(), this, dataLock);
                action.schedule();

                // add action to action list and sync it back into the data builder
                this.scheduledActionList.add(action);
                syncActionList(builderSetup.getBuilder());

                this.terminatingActionId = action.getId();

            } catch (CouldNotPerformException ex) {
                ExceptionPrinter.printHistory("Could not register state termination!", ex, logger);
            }
        } finally {
            builderSetup.unlockWrite(NotificationStrategy.SKIP);
        }
    }

    @Override
    public Future<ActionDescription> applyAction(ActionParameterOrBuilder actionParameter) {
        try {
            final ActionParameter.Builder builder;
            if (actionParameter instanceof ActionParameter.Builder) {
                builder = ((ActionParameter.Builder) actionParameter);
            } else {
                builder = ((ActionParameter) actionParameter).toBuilder();
            }

            // validate target unit
            if (!builder.getServiceStateDescriptionBuilder().hasUnitId()) {
                builder.getServiceStateDescriptionBuilder().setUnitId(getId());
            } else if (!builder.getServiceStateDescriptionBuilder().getUnitId().equals(getId())) {
                logger.warn("{} is not applied to {} which is not its correct TargetUnit {} and will be skipped...", actionParameter, this, builder.getServiceStateDescriptionBuilder().getUnitId());
            }

            // If this action was received unencrypted from a remote instance, its authority can not be guaranteed.
            // In this case we perform an unauthorized action.
            for (StackTraceElement stackTraceElement : Thread.currentThread().getStackTrace()) {
                if (stackTraceElement.getClassName().equals(RPCUtils.class.getName())) {
                    logger.warn("incoming unauthorized action: " + builder.toString());
                    return applyUnauthorizedAction(ActionDescriptionProcessor.generateActionDescriptionBuilder(builder).build());
                }
            }
            return applyAction(ActionDescriptionProcessor.generateActionDescriptionBuilder(builder).build());
        } catch (CouldNotPerformException ex) {
            return FutureProcessor.canceledFuture(ActionDescription.class, new CouldNotPerformException("Could not apply action!", ex));
        }
    }

    public Future<ActionDescription> applyUnauthorizedAction(final ActionDescription actionDescription) {
        return AuthenticatedServiceProcessor.requestAuthenticatedAction(actionDescription, ActionDescription.class, MOCKUP_SESSION_MANAGER, authenticatedValue -> applyActionAuthenticated(authenticatedValue));
    }

    @Override
    public Future<ActionDescription> applyAction(final ActionDescription actionDescription) {
        try {
            return scheduleAction(new ActionImpl(actionDescription, this, dataLock));
        } catch (CouldNotPerformException ex) {
            return FutureProcessor.canceledFuture(ActionDescription.class, ExceptionPrinter.printHistoryAndReturnThrowable(new CouldNotPerformException("Could not apply action!", ex), logger));
        }
    }

    /**
     * Resolve an action by its id. This method either checks if the action itself has the provided id or if one
     * of its causes does.
     *
     * @param actionId     the id of the action retrieved.
     * @param lockConsumer string identifying the task. Required because this method has to lock the builder setup because
     *                     of access to the {@link #scheduledActionList}.
     *
     * @return the action identified by the provided id as described above.
     *
     * @throws NotAvailableException if not action with the provided id could be found.
     */
    public SchedulableAction getActionById(final String actionId, final String lockConsumer) throws NotAvailableException, InterruptedException {
        builderSetup.lockReadInterruptibly(lockConsumer);
        try {
            // lookup action to cancel
            for (SchedulableAction action : scheduledActionList) {
                // provided action id is a direct match
                if (action.getId().equals(actionId)) {
                    return action;
                }

                // provided action id appears in the action chain of the action
                for (final ActionReference actionReference : action.getActionDescription().getActionCauseList()) {
                    if (actionReference.getActionId().equals(actionId)) {
                        return action;
                    }
                }
            }
            throw new NotAvailableException("Action[" + actionId + "]");
        } finally {
            builderSetup.unlockRead(false);
        }
    }

    /**
     * Validate that a user has permissions to modify an action (cancelling or extending). This is the case
     * if it is an admin or if it is the executor of the action or one of its causes.
     *
     * @param userId the id of the user whose permissions are checked.
     * @param action the action checked.
     *
     * @throws PermissionDeniedException if the user has no permissions to modify the provided action.
     * @throws CouldNotPerformException  if the permissions check could not be performed.
     */
    private void validateActionPermissions(String userId, final Action action) throws CouldNotPerformException {
        try {
            if (userId.isEmpty()) {
                userId = User.OTHER;
            }

            if(action.getId().equals(terminatingActionId)) {
                throw new InvalidStateException("Its not allowed to cancel the termination action!");
            }

            if (!userId.equals(User.OTHER) && Registries.getUnitRegistry().getUnitConfigByAlias(UnitRegistry.ADMIN_GROUP_ALIAS).getAuthorizationGroupConfig().getMemberIdList().contains(userId)) {
                // user is an admin
                return;
            }

            if (action.getActionDescription().getActionInitiator().getInitiatorId().equals(userId)) {
                // user is the direct initiator
                return;
            }

            for (final ActionReference actionReference : action.getActionDescription().getActionCauseList()) {
                if (actionReference.getActionInitiator().getInitiatorId().equals(userId)) {
                    // user is initiator of one of its references
                    return;
                }
            }

            // build nice error description
            String cancelingInstance;
            try {
                cancelingInstance = LabelProcessor.getBestMatch(Registries.getUnitRegistry().getUnitConfigById(userId).getLabel(), userId);
            } catch (NotAvailableException ex) {
                cancelingInstance = userId;
            }

            final String ownerInstanceList = StringProcessor.transformCollectionToString(action.getActionDescription().getActionCauseList(), actionReference -> {
                try {
                    return LabelProcessor.getBestMatch(Registries.getUnitRegistry().getUnitConfigById(actionReference.getActionInitiator().getInitiatorId()).getLabel());
                } catch (NotAvailableException e) {
                    return actionReference.getActionInitiator().getInitiatorId();
                }
            }, ", ");

            final String description = MultiLanguageTextProcessor.getBestMatch(action.getActionDescription().getDescription(), action.getActionDescription().getActionId());
            throw ExceptionPrinter.printHistoryAndReturnThrowable(new PermissionDeniedException("User [" + cancelingInstance + "] is not allowed to modify action [" + description + "] owned by " + ownerInstanceList), logger, LogLevel.WARN);
        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not validate permissions for user [" + userId + "]", ex);
        }
    }

    @Override
    public Future<ActionDescription> cancelAction(final ActionDescription actionDescription) {
        return cancelAction(actionDescription, "");
    }

    protected Future<ActionDescription> cancelAction(final ActionDescription actionDescription, final String authenticatedId) {
        logger.trace("cancel action " + actionDescription.getActionId() + " on controller with " + authenticatedId);
        try {
            try {
                builderSetup.lockWriteInterruptibly(LOCK_CONSUMER_CANCEL_ACTION);
            } catch (InterruptedException ex) {
                return FutureProcessor.canceledFuture(ActionDescription.class, ex);
            }
            try {
                // retrieve action
                final Action actionToCancel;
                try {
                    actionToCancel = getActionById(actionDescription.getActionId(), LOCK_CONSUMER_CANCEL_ACTION);
                } catch (NotAvailableException ex) {
                    // if the action is not any longer available, then it can be marked as canceled to inform the remote instance.
                    return FutureProcessor.completedFuture(actionDescription.toBuilder().setActionState(ActionState.newBuilder().setValue(State.CANCELED).build()).build());
                } catch (InterruptedException e) {
                    return FutureProcessor.canceledFuture(ActionDescription.class, e);
                }

                // validate permissions
                validateActionPermissions(authenticatedId, actionToCancel);

                // cancel the action which automatically triggers a reschedule.
                return actionToCancel.cancel();
            } finally {
                builderSetup.unlockWrite();
            }
        } catch (CouldNotPerformException ex) {
            return FutureProcessor.canceledFuture(ActionDescription.class, new CouldNotPerformException("Could not cancel Action[" + actionDescription.getActionId() + "]", ex));
        }
    }

    @Override
    public Future<ActionDescription> extendAction(final ActionDescription actionDescription) {
        return extendAction(actionDescription, "");
    }

    protected Future<ActionDescription> extendAction(final ActionDescription actionDescription, final String authenticatedId) {
        try {
            builderSetup.lockWriteInterruptibly(LOCK_CONSUMER_EXTEND_ACTION);
            try {
                // retrieve action
                final SchedulableAction actionToExtend = getActionById(actionDescription.getActionId(), LOCK_CONSUMER_EXTEND_ACTION);

                if (!actionToExtend.isValid()) {
                    throw new CouldNotPerformException("Extension of " + actionToExtend + " skipped, because the action is not longer valid!");
                }

                // validate permissions
                validateActionPermissions(authenticatedId, actionToExtend);

                // extend action.
                actionToExtend.extend();

                //TODO: for performance reasons it could be nice to update the schedulingTimeout here
                // because now rescheduling is guaranteed to be done every 15 minutes...

                // sync update
                syncActionList(builderSetup.getBuilder());

                // return updated action description
                return FutureProcessor.completedFuture(actionToExtend.getActionDescription());
            } finally {
                // unlock and notify
                builderSetup.unlockWrite();
            }
        } catch (InterruptedException ex) {
            return FutureProcessor.canceledFuture(ActionDescription.class, ex);
        } catch (CouldNotPerformException ex) {
            return FutureProcessor.canceledFuture(ActionDescription.class, new CouldNotPerformException("Could not extend Action[" + actionDescription.getActionId() + "]", ex));
        }
    }

    private Future<ActionDescription> scheduleAction(final SchedulableAction actionToSchedule) {
        final Action executingAction;
        try {
            executingAction = reschedule(actionToSchedule);

            if (actionToSchedule != executingAction) {
                logger.trace("================================================================================");
                if (executingAction == null) {
                    logger.error("{} seems not to be valid and was excluded from execution of {}.", actionToSchedule, this);
                } else {
                    if (JPService.verboseMode()) {
                        logger.info("{} was postponed because of {} and added to the scheduling queue of {} at position {}.", actionToSchedule, executingAction, this, getSchedulingIndex(actionToSchedule));
                    } else {
                        logger.trace("{} was postponed because of {} and added to the scheduling queue of {} at position {}.", actionToSchedule, executingAction, this, getSchedulingIndex(actionToSchedule));
                    }
                }
            }

        } catch (CouldNotPerformException | InterruptedException ex) {
            FutureProcessor.canceledFuture(ActionDescription.class, ex);
        }

        return FutureProcessor.completedFuture(actionToSchedule.getActionDescription());
    }

    private int getSchedulingIndex(Action action) throws InterruptedException {

        builderSetup.lockReadInterruptibly(LOCK_CONSUMER_INDEX_LOOKUP);
        try {
            return scheduledActionList.indexOf(action);
        } finally {
            builderSetup.unlockRead(LOCK_CONSUMER_INDEX_LOOKUP);
        }
    }

    /**
     * Recalculate the action ranking and execute the action with the highest ranking if not already executing or finished.
     * If the current action is not finished it will be rejected.
     *
     * @return the {@code action} which is ranked highest and which is therefore currently allocating this unit.
     * If there is no action left to schedule null is returned.
     *
     * @throws CouldNotPerformException is throw in case the scheduling is currently not possible, e.g. because of a system shutdown.
     */
    public Action reschedule() throws CouldNotPerformException, InterruptedException {
        return reschedule(null);
    }

    /**
     * Recalculate the action ranking and execute the action with the highest ranking if not already executing or finished.
     * If the current action is not finished it will be rejected.
     *
     * @param actionToSchedule a new action to schedule. If null it will be ignored.
     *
     * @return the {@code action} which is ranked highest and which is therefore currently allocating this unit.
     * If there is no action left to schedule null is returned.
     *
     * @throws CouldNotPerformException is throw in case the scheduling is currently not possible, e.g. because of a system shutdown.
     */
    private Action reschedule(final SchedulableAction actionToSchedule) throws CouldNotPerformException, InterruptedException {

        // avoid scheduling during shutdown
        if (isShutdownInProgress()) {
            throw new ShutdownInProgressException(this);
        }

        builderSetup.lockWriteInterruptibly(LOCK_CONSUMER_SCHEDULING);
        try {
            try {
                // cancel timer if still running because it will be restarted at the end of the schedule anyway.
                if (!scheduleTimeout.isExpired()) {
                    scheduleTimeout.cancel();
                }

                if (actionToSchedule != null) {

                    // test if there is another action already in the list by the same initiator
                    try {
                        final ActionInitiator newInitiator = ActionDescriptionProcessor.getInitialInitiator(actionToSchedule.getActionDescription());

                        logger.debug("schedule new incoming action: {}", MultiLanguageTextProcessor.getBestMatch(actionToSchedule.getActionDescription().getDescription(), "?"));

                        for (final SchedulableAction schedulableAction : new ArrayList<>(scheduledActionList)) {

                            if (schedulableAction.isDone()) {
                                // skip actions which are done and remain on the stack for notification purposes
                                continue;
                            }

                            // preload same states
                            final ActionDescription scheduledActionDescription = schedulableAction.getActionDescription();
                            final ActionInitiator currentInitiator = ActionDescriptionProcessor.getInitialInitiator(scheduledActionDescription);

                            // if both initiators are not the same and there is not more than one human action on the stack than we can skip the rejection
                            if (!newInitiator.getInitiatorId().equals(currentInitiator.getInitiatorId()) &&
                                    !(currentInitiator.getInitiatorType() == InitiatorType.HUMAN && newInitiator.getInitiatorType() == InitiatorType.HUMAN)) {
                                // actions do not have the same initiator
                                continue;
                            }

                            // do not cancel the termination action.
                            if (scheduledActionDescription.getPriority() == Priority.TERMINATION) {
                                continue;
                            }

                            // do not cancel the action if any action of it is non replaceable
                            if (!ActionDescriptionProcessor.getReplaceable(scheduledActionDescription) &&
                                    // we have to make sure that external units can only put one non replaceable action on this stack.
                                    // therefore, we check if the unit chain suffix till the non replaceable flag is not the same, otherwise we still reject the action.
                                    !ActionDescriptionProcessor.getUnitChainSuffixForNonReplaceableAction(scheduledActionDescription).equals(ActionDescriptionProcessor.getUnitChainSuffixForNonReplaceableAction(actionToSchedule.getActionDescription()))) {
                                continue;
                            }

                            // same initiator - schedule the newer one
                            if (actionToSchedule.getCreationTime() < schedulableAction.getCreationTime()) {
                                // new action is actually older than one already scheduled by the same initiator, so reject it
                                actionToSchedule.reject();
                                logger.warn("New Action {} from initiator {} is older than a currently scheduled one and will be rejected!", actionToSchedule, newInitiator.getInitiatorId());
                            } else {
                                // actionToSchedule is newer, so reject old one
                                schedulableAction.reject();
                            }
                        }
                    } catch (NotAvailableException ex) {
                        ExceptionPrinter.printHistory("Could not detect initiator or creation time!", ex, logger);
                    }

                    // add new action to the list
                    scheduledActionList.add(actionToSchedule);
                }

                try {
                    // save if there is at least one action still awaiting execution
                    boolean atLeastOneActionToSchedule = false;
                    // save if there is at least one action which is done but remains on the list
                    boolean atLeastOneDoneActionOnList = false;

                    // reject outdated and finish completed actions
                    for (final SchedulableAction action : scheduledActionList) {

                        // only terminate if not valid and still not done yet
                        if (!action.isValid() && !action.isDone()) {

                            // handle auto extension if flag is set and human is initiator
                            try {
                                if (action.isAutoContinueWithLowPriorityIntended()) {
                                    // extend with low priority
                                    action.autoExtendWithLowPriority();
                                    continue;
                                }
                            } catch (VerificationFailedException ex) {
                                ExceptionPrinter.printHistory(ex, logger);
                            }

                            // set action as done
                            if (action.getActionState() == State.EXECUTING) {
                                action.finish();
                            } else {
                                action.reject();
                            }
                        }
                    }

                    // remove actions which are in a final state for more than 15 seconds
                    for (final SchedulableAction action : new ArrayList<>(scheduledActionList)) {
                        if (action.isDone()) {
                            try {
                                final Timestamp latestValueOccurrence = ServiceStateProcessor.getLatestValueOccurrence(action.getActionState().getValueDescriptor(), action.getActionDescription().getActionState());
                                if ((System.currentTimeMillis() - TimestampJavaTimeTransform.transform(latestValueOccurrence)) > FINISHED_ACTION_REMOVAL_TIMEOUT) {
                                    scheduledActionList.remove(action);
                                } else {
                                    atLeastOneDoneActionOnList = true;
                                }
                            } catch (NotAvailableException ex) {
                                // action timestamp could not be evaluated so print warning and remove action so that it does not stay on the list forever
                                ExceptionPrinter.printHistory("Remove finished " + action + " because its finishing time could not be evaluated!", ex, logger, LogLevel.WARN);
                                scheduledActionList.remove(action);
                            }
                        } else {

                            // check if action is still valid. It will be cleaned next time, however in this case we do not need to reschedule again.
                            if (action.isValid()) {
                                atLeastOneActionToSchedule = true;
                            }
                        }
                    }

                    // skip further steps if no actions are scheduled
                    if (!atLeastOneActionToSchedule) {
                        logger.debug("No valid action on stack, so finish scheduling.");
                        return null;
                    }

                    // detect and store current action
                    SchedulableAction currentAction = null;
                    for (final SchedulableAction schedulableAction : scheduledActionList) {
                        if (schedulableAction.isProcessing()) {
                            currentAction = schedulableAction;
                            break;
                        }
                    }

                    // sort valid actions by priority
                    scheduledActionList.sort(actionComparator);

                    // detect action with highest ranking
                    final SchedulableAction nextAction = scheduledActionList.get(0);

                    // if new action is not schedulable and not immediately scheduled reject it
                    if (actionToSchedule != null && !actionToSchedule.equals(nextAction)) {
                        if (actionToSchedule.getActionDescription().getSchedulable()) {
                            if (!actionToSchedule.isDone()) {
                                actionToSchedule.schedule();
                            }
                        } else {
                            actionToSchedule.reject();
                            atLeastOneDoneActionOnList = true;
                        }
                    }

                    // execute action with the highest ranking if it is not already the currently executing one.
                    if (nextAction != currentAction) {

                        // abort current action before executing the next one.
                        if (currentAction != null) {
                            currentAction.abort();
                        }
                        nextAction.execute();
                        currentAction = nextAction;
                    }

                    // setup timed rescheduling.
                    try {
                        // setup timer only if action needs to be removed or the current action provides a limited execution time.
                        if (atLeastOneDoneActionOnList || currentAction.getExecutionTimePeriod(TimeUnit.MICROSECONDS) != 0) {
                            final long rescheduleTimeout = atLeastOneDoneActionOnList ? Math.min(FINISHED_ACTION_REMOVAL_TIMEOUT, currentAction.getExecutionTime()) : Math.min(currentAction.getExecutionTime(), Action.MAX_EXECUTION_TIME_PERIOD);
                            logger.debug("Reschedule scheduled in {} ms.", rescheduleTimeout);
                            // since the execution time of an action can be zero, we should wait at least a bit before reschedule via timer.
                            // this should not cause any latency because new incoming actions are scheduled anyway.
                            scheduleTimeout.restart(Math.max(rescheduleTimeout, 50), TimeUnit.MILLISECONDS);
                        }
                    } catch (ShutdownInProgressException ex) {
                        // skip reschedule when shutdown is initiated.
                    } catch (CouldNotPerformException ex) {
                        ExceptionPrinter.printHistory(new FatalImplementationErrorException("Could not setup rescheduling timeout! ", this, ex), logger);
                    }

                    return currentAction;
                } finally {
                    try {
                        updateTransactionId();
                    } catch (CouldNotPerformException ex) {
                        ExceptionPrinter.printHistory("Could not update transaction id", ex, logger);
                    }
                }
            } finally {
                // sync action list but do not notify since builder is still locked.
                try (final ClosableDataBuilder<DB> dataBuilder = getDataBuilderInterruptible(this)) {
                    // sync
                    syncActionList(dataBuilder.getInternalBuilder());
                } catch (Exception ex) {
                    ExceptionPrinter.printHistory("Could not update action list!", ex, logger);
                }
            }
        } finally {
            builderSetup.unlockWrite();
        }
    }

    public void initRescheduling() {
        try {
            scheduleTimeout.restart(3, TimeUnit.MILLISECONDS);
        } catch (CouldNotPerformException ex) {
            if (!ExceptionProcessor.isCausedBySystemShutdown(ex)) {
                ExceptionPrinter.printHistory("Could not init rescheduling!", ex, logger);
            }
        }
    }

    /**
     * Update the action list in the data builder and notify.
     * <p>
     * Note: The sync and notification is skip if the unit is currently rescheduling actions or the write lock is still hold.
     */
    public void notifyScheduledActionList() throws InterruptedException {

        // skip notification when builder setup is locked since then the notification is performed anyway.
        if(!builderSetup.tryLockWrite(10, TimeUnit.SECONDS, LOCK_CONSUMER_NOTIFICATION)) {
            logger.warn("Skip action list sync since builder setup is busy.");
            // todo: we might want to mark the data object as dirty, since the action list is not up to date in this case.
            return;
        }
        try {
            try (final ClosableDataBuilder<DB> dataBuilder = getDataBuilderInterruptible(this)) {
                // sync
                syncActionList(dataBuilder.getInternalBuilder());
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            } catch (Exception ex) {
                ExceptionPrinter.printHistory("Could not update action list!", ex, logger);
            }
        } finally {
            builderSetup.unlockWrite();
        }
    }

    /**
     * Syncs the action list into the given {@code dataBuilder}.
     *
     * @param dataBuilder used to synchronize with.
     *
     * @throws CouldNotPerformException is thrown if the sync failed.
     */
    private void syncActionList(final DB dataBuilder) throws CouldNotPerformException {
        try {
            final FieldDescriptor actionFieldDescriptor = ProtoBufFieldProcessor.getFieldDescriptor(dataBuilder, Action.TYPE_FIELD_NAME_ACTION);
            // get actions descriptions for all actions and sync to data builder...
            dataBuilder.clearField(actionFieldDescriptor);
            for (final Action action : scheduledActionList) {
                dataBuilder.addRepeatedField(actionFieldDescriptor, action.getActionDescription());
            }
        } catch (final CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not sync action list!", ex);
        }
    }

    @Override
    public Future<AuthenticatedValue> applyActionAuthenticated(final AuthenticatedValue authenticatedValue) {

        final InternalIdentifiedProcessable<ActionDescription, ActionDescription> internalIdentifiedProcessable = (actionDescription, authenticationBaseData) -> {

            final AuthPair authPair = AbstractUnitController.this.verifyAccessPermission(authenticationBaseData, actionDescription.getServiceStateDescription().getServiceType());

            final Builder actionDescriptionBuilder = actionDescription.toBuilder();

            // clear auth fields which are in the following recomputed by the given auth values.
            actionDescriptionBuilder.getActionInitiatorBuilder().clear();

            // if initiator is declared we need to recover it.
            if (actionDescription.getActionInitiator().hasInitiatorType()) {
                actionDescriptionBuilder.getActionInitiatorBuilder().setInitiatorType(actionDescription.getActionInitiator().getInitiatorType());
            }

            // if an authentication token is send replace the initiator in any case
            if (authenticationBaseData != null && authenticationBaseData.getAuthenticationToken() != null) {
                ActionDescriptionProcessor.setActionInitiator(actionDescriptionBuilder, authenticationBaseData.getAuthenticationToken().getUserId(), true);
            }

            // setup auth fields
            if (authPair.getAuthenticatedBy() != null) {
                actionDescriptionBuilder.getActionInitiatorBuilder().setAuthenticatedBy(authPair.getAuthenticatedBy());

                // if not yet available, setup authenticated instance as action initiator via auth pair.
                if (!actionDescriptionBuilder.getActionInitiatorBuilder().hasInitiatorId() || actionDescriptionBuilder.getActionInitiatorBuilder().getInitiatorId().isEmpty()) {
                    ActionDescriptionProcessor.setActionInitiator(actionDescriptionBuilder, authPair.getAuthenticatedBy(), false);
                }
            }
            if (authPair.getAuthorizedBy() != null) {
                actionDescriptionBuilder.getActionInitiatorBuilder().setAuthorizedBy(authPair.getAuthorizedBy());

                // if not yet available, setup authorizing instance as action initiator via auth pair.
                if (!actionDescriptionBuilder.getActionInitiatorBuilder().hasInitiatorId() || actionDescriptionBuilder.getActionInitiatorBuilder().getInitiatorId().isEmpty()) {
                    ActionDescriptionProcessor.setActionInitiator(actionDescriptionBuilder, authPair.getAuthorizedBy(), false);
                }
            }

            // if not yet available, setup other as initiator.
            if (!actionDescriptionBuilder.getActionInitiatorBuilder().hasInitiatorId() || actionDescriptionBuilder.getActionInitiatorBuilder().getInitiatorId().isEmpty()) {
                ActionDescriptionProcessor.setActionInitiator(actionDescriptionBuilder, User.OTHER, false);
            }

            final Future<ActionDescription> actionDescriptionFuture = AbstractUnitController.this.internalApplyActionAuthenticated(authenticatedValue, actionDescriptionBuilder, authenticationBaseData, authPair);

            try {
                return actionDescriptionFuture.get(30, TimeUnit.SECONDS);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                throw new CouldNotPerformException("Authenticated action was interrupted!", ex);
            } catch (ExecutionException | TimeoutException ex) {
                throw new CouldNotPerformException("Could not apply authenticated action!", ex);
            } finally {
                if (!actionDescriptionFuture.isDone()) {
                    actionDescriptionFuture.cancel(true);
                }
            }
        };

        // build result
        try {
            return FutureProcessor.completedFuture(AuthenticatedServiceProcessor.authenticatedAction(authenticatedValue, ActionDescription.class, AbstractUnitController.this, internalIdentifiedProcessable));
        } catch (CouldNotPerformException ex) {
            return FutureProcessor.canceledFuture(AuthenticatedValue.class, ex);
        }
    }

    protected Future<ActionDescription> internalApplyActionAuthenticated(final AuthenticatedValue authenticatedValue, final ActionDescription.Builder actionDescriptionBuilder, final AuthenticationBaseData authenticationBaseData, final AuthPair authPair) {

        try {
            // handle action cancellation
            if (actionDescriptionBuilder.getCancel()) {
                try {
                    if (!actionDescriptionBuilder.hasActionId() || actionDescriptionBuilder.getActionId().isEmpty()) {
                        throw new NotAvailableException("ActionId");
                    }
                    return cancelAction(actionDescriptionBuilder.build(), authPair.getAuthenticatedBy());
                } catch (CouldNotPerformException ex) {
                    throw new CouldNotPerformException("Could not cancel authenticated action!", ex);
                }
            }

            // handle action execution time extension
            if (actionDescriptionBuilder.getExtend()) {
                try {
                    if (!actionDescriptionBuilder.hasActionId() || actionDescriptionBuilder.getActionId().isEmpty()) {
                        throw new NotAvailableException("ActionId");
                    }
                    return extendAction(actionDescriptionBuilder.build(), authPair.getAuthenticatedBy());
                } catch (CouldNotPerformException ex) {
                    throw new CouldNotPerformException("Could not extend authenticated action!", ex);
                }
            }

            // handle new actions
            return applyAction(actionDescriptionBuilder.build());

        } catch (CouldNotPerformException ex) {
            return FutureProcessor.canceledFuture(ActionDescription.class, ex);
        }
    }

    @Override
    public void addServiceStateObserver(ServiceTempus serviceTempus, ServiceType serviceType, Observer<ServiceStateProvider<Message>, Message> observer) throws CouldNotPerformException {
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
                if (serviceType == ServiceType.UNKNOWN) {
                    for (MessageObservable<ServiceStateProvider<Message>, Message> observable : serviceTempusServiceTypeObservableMap.get(serviceTempus).values()) {
                        observable.addObserver(observer);
                    }
                } else {
                    serviceTempusServiceTypeObservableMap.get(serviceTempus).get(serviceType).addObserver(observer);
                }
            } catch (NullPointerException ex) {
                throw new InvalidStateException("Non supported observer registration requested! " + this + " does not support Service[" + serviceType + "] in ServiceTempus[" + serviceTempus + "]", ex);
            }
        }
    }

    @Override
    public void removeServiceStateObserver(ServiceTempus serviceTempus, ServiceType serviceType, Observer<ServiceStateProvider<Message>, Message> observer) {
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
                logger.warn("Non supported Observer[{}] removal requested! {} does not support Service[{}] in ServiceTempus[{}]", observer, this, serviceType, serviceTempus);
            }
        }
    }

    @Override
    public String toString() {
        return classDescription;
    }

    @Override
    public void shutdown() {
        super.shutdown();

        try {
            // shutdown registry observer
            Registries.getUnitRegistry().removeDataObserver(unitRegistryObserver);
        } catch (final NotAvailableException ex) {
            // if the registry is not any longer available (in case of registry shutdown) than the observer is already cleared.
        } catch (final Exception ex) {
            ExceptionPrinter.printHistory(new CouldNotPerformException("Could not remove unit registry observer.", ex), logger);
        }

        // shutdown service observer
        for (final ServiceTempus serviceTempus : ServiceTempus.values()) {
            if (serviceTempus == ServiceTempus.UNKNOWN) {
                continue;
            }

            for (final MessageObservable serviceObservable : serviceTempusServiceTypeObservableMap.get(serviceTempus).values()) {
                serviceObservable.shutdown();
            }
            unitDataObservableMap.get(serviceTempus).shutdown();
        }


        for (SchedulableAction schedulableAction : new ArrayList<>(scheduledActionList)) {
            if(!schedulableAction.isDone()) {
                schedulableAction.reject();
            }
        }
    }

    @Override
    public void applyDataUpdate(Message newState, final ServiceType serviceType) throws CouldNotPerformException {
        try {
            logger.error("wait for lock...");
//            if (!builderSetup.tryLockWrite(5, TimeUnit.SECONDS, this)) {
//                throw new InvalidStateException("Unit seems to be stuck!");
//            }

            while (!builderSetup.tryLockWrite(100, TimeUnit.MILLISECONDS, this)) {

                if(Thread.currentThread().isInterrupted()) {
                    throw new InterruptedException();
                }

                logger.warn("stucked");
            }



            // builder write unlock  block
            try {
                try (ClosableDataBuilder<DB> dataBuilder = getDataBuilderInterruptible(this)) {
                    final DB internalBuilder = dataBuilder.getInternalBuilder();
                    try {
                        // compute new state by resolving requested value, detecting hardware feedback loops of already applied states and handling the rescheduling process.
                        try {
                            newState = computeNewState(newState, serviceType, internalBuilder);
                        } catch (RejectedException ex) {
                            // update not required since its compatible with the currently applied state, therefore we just skip the update.
                            ExceptionPrinter.printHistory("update not required since its compatible with the currently applied state, therefore we just skip the update", ex, logger, LogLevel.DEBUG);
                            return;
                        }

                        // verify the service state
                        newState = Services.verifyAndRevalidateServiceState(newState);

                        // move current state to last state
                        updateLastWithCurrentState(serviceType, internalBuilder);

                        // copy latestValueOccurrence map from current state, only if available
                        try {
                            Descriptors.FieldDescriptor latestValueOccurrenceField = ProtoBufFieldProcessor.getFieldDescriptor(newState, ServiceStateProcessor.FIELD_NAME_LAST_VALUE_OCCURRENCE);
                            Message oldServiceState = Services.invokeProviderServiceMethod(serviceType, internalBuilder);
                            newState = newState.toBuilder().setField(latestValueOccurrenceField, oldServiceState.getField(latestValueOccurrenceField)).build();
                        } catch (NotAvailableException ex) {
                            // skip last value update if field is missing because some states do not contain latest value occurrences (ColorState, PowerConsumptionState, ...)
                        }

                        // log state transition
                        if (logger.isInfoEnabled()) {
                            final String from = LabelProcessor.getBestMatch(Services.generateServiceStateLabel(Services.invokeProviderServiceMethod(serviceType, internalBuilder), serviceType));
                            final String to = LabelProcessor.getBestMatch(Services.generateServiceStateLabel(newState, serviceType));
                            logger.info(getLabel("?") + " is updated " + (from.isEmpty() ? "" : "from " + from + " ") + "to " + to + ".");
                        }

                        if (!Services.hasResponsibleAction(newState)) {
                            StackTracePrinter.printStackTrace("Applied data update does not offer an responsible action!", logger, LogLevel.WARN);
                        }

                        // update the current state
                        Services.invokeServiceMethod(serviceType, OPERATION, ServiceTempus.CURRENT, internalBuilder, newState);

                        // Update timestamps
                        updatedAndValidateTimestamps(serviceType, internalBuilder);

                        // do custom state depending update in sub classes
                        applyCustomDataUpdate(internalBuilder, serviceType);

                    } finally {
                        // sync updated action list
                        syncActionList(internalBuilder);
                    }
                }
            } finally {
                builderSetup.unlockWrite(NotificationStrategy.AFTER_LAST_RELEASE);
            }
        } catch (InterruptedException ex) {
            logger.error("recover interruption");
            Thread.currentThread().interrupt();
            throw new CouldNotPerformException("Update was interrupted!",ex);
        } catch (Throwable ex) {
            logger.error("Failed");
            throw new CouldNotPerformException("Could not apply Service[" + serviceType.name() + "] Update[" + newState + "] for " + this + "!", ex);
        }
    }

    /**
     * Update the state timestamp and the timestamps of the value occurrence map.
     *
     * @param serviceType     the service type of the state to update.
     * @param internalBuilder the internal builder instance holding the service state.
     */
    private void updatedAndValidateTimestamps(final ServiceType serviceType, final DB internalBuilder) {
        try {
            Message.Builder serviceStateBuilder = (Message.Builder) internalBuilder.getClass().getMethod("get" + Services.getServiceStateName(serviceType) + "Builder").invoke(internalBuilder);

            //Set timestamp if missing
            if (!serviceStateBuilder.hasField(serviceStateBuilder.getDescriptorForType().findFieldByName("timestamp"))) {
                logger.warn("State[" + Services.getServiceStateName(serviceType) + "] of " + this + " does not contain any state related timestamp!");
                TimestampProcessor.updateTimestampWithCurrentTime(serviceStateBuilder, logger);
            }

            // update state value occurrence timestamp
            try {
                FieldDescriptor valueFieldDescriptor = serviceStateBuilder.getDescriptorForType().findFieldByName("value");
                FieldDescriptor timestampFieldDescriptor = serviceStateBuilder.getDescriptorForType().findFieldByName("timestamp");
                if (valueFieldDescriptor != null) {
                    ServiceStateProcessor.updateLatestValueOccurrence((EnumValueDescriptor) serviceStateBuilder.getField(valueFieldDescriptor), ((Timestamp) serviceStateBuilder.getField(timestampFieldDescriptor)), serviceStateBuilder);
                }
            } catch (CouldNotPerformException ex) {
                throw new CouldNotPerformException("Could not update state value occurrence timestamp!", ex);
            }
        } catch (Exception ex) {
            ExceptionPrinter.printHistory("Could not update timestamp!", ex, logger);
        }
    }

    protected Action getCurrentAction() throws NotAvailableException {
        try {
            builderSetup.lockReadInterruptibly(this);
            try {
                return scheduledActionList.get(0);
            } finally {
                builderSetup.unlockRead(this);
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new NotAvailableException("CurrentAction", ex);
        } catch (Exception ex) {
            throw new NotAvailableException("CurrentAction", ex);
        }
    }

    /**
     * @param serviceState    the prototype of the new state.
     * @param serviceType     the service type of the new state.
     * @param internalBuilder the builder object used to access the currently applied state.
     *
     * @return the computed state.
     *
     * @throws RejectedException        in case the state would not change anything compared to the current one.
     * @throws CouldNotPerformException if the state could not be computed.
     */
    private Message computeNewState(final Message serviceState, final ServiceType serviceType, final DB internalBuilder) throws CouldNotPerformException, RejectedException, InterruptedException {
        try {
            // if the given state is a provider service, than no further steps have to be performed
            // because only operation service actions can be remapped
            if (!hasOperationServiceForType(serviceType)) {
                return serviceState;
            }

            Message requestedState = null;

            // in case the new incoming state is matching the requested one we use the requested
            // one since it probably contains more information about the actions origin like initiator
            // and further fields which are lost during the hardware feedback loop like the executor and the action cain.
            if (Services.hasServiceState(serviceType, ServiceTempus.REQUESTED, internalBuilder)) {
                requestedState = (Message) Services.invokeServiceMethod(serviceType, PROVIDER, ServiceTempus.REQUESTED, internalBuilder);

                // validate responsible action
                if (Services.hasResponsibleAction(requestedState)) {

                    // choose with which value to update
                    if (Services.equalServiceStates(serviceState, requestedState)) {
                        try {
                            // use the requested state but update the timestamp if not available
                            if (TimestampProcessor.hasTimestamp(serviceState)) {
                                Descriptors.FieldDescriptor timestampField = ProtoBufFieldProcessor.getFieldDescriptor(serviceState, TimestampProcessor.TIMESTAMP_FIELD_NAME);
                                return requestedState.toBuilder().setField(timestampField, serviceState.getField(timestampField)).build();
                            } else {
                                return requestedState;
                            }
                        } finally {
                            // remove requested state since it will be applied now and is no longer requested.
                            resetRequestedServiceState(serviceType, internalBuilder);
                        }
                    } else {
                        // requested state does not match the current state
                    }
                } else {
                    logger.error("Requested service state does no offer an responsible action and will be ignored!");
                }
            } else {
                // requested state is not available so obviously it does not match
            }

            // if a lot of actions are executed over a short time period it can happen that the requested
            // state currently does not match the external update from openHAB
            // thus, match the service state to all actions of the same service that were submitted in the last
            // three seconds
            for (final SchedulableAction action : scheduledActionList) {
                final ServiceStateDescriptionType.ServiceStateDescription serviceStateDescription = action.getActionDescription().getServiceStateDescription();

                // do not consider actions for a different service
                if (serviceStateDescription.getServiceType() != serviceType) {
                    continue;
                }

                // do not consider actions which were not executing in the last three seconds
                try {
                    final Timestamp lastTimeExecuting = ServiceStateProcessor.getLatestValueOccurrence(State.SUBMISSION, action.getActionDescription().getActionState());

                    if ((System.currentTimeMillis() - TimestampJavaTimeTransform.transform(lastTimeExecuting)) > SUBMISSION_ACTION_MATCHING_TIMEOUT) {
                        continue;
                    }
                } catch (NotAvailableException ex) {
                    // it the last executing time is not available skip comparison
                    continue;
                }

                // when priorities are both available, but they do not match, then it can not be the same action.
                final ActionDescription responsibleAction = ServiceStateProcessor.getResponsibleAction(serviceState);
                if (action.getActionDescription().hasPriority() &&
                        responsibleAction.hasPriority() &&
                        action.getActionDescription().getPriority() != responsibleAction.getPriority()) {
                    continue;
                }

                // compare service states
                final Message actionServiceState = SERVICE_JSON_PROCESSOR.deserialize(serviceStateDescription.getServiceState(), serviceStateDescription.getServiceStateClassName());

                if (Services.equalServiceStates(serviceState, actionServiceState)) {
                    throw new RejectedException("New state has already been scheduled!");
                }
            }

            // because the requested action does not match this action was triggered outside BCO e.g. via openHAB or is just a state sync.

            // however, the problem remains that openHAB notifies all service states as new updates
            // e.g. if the power of a colorable light is set a brightness state and a color state are also updated
            // furthermore the power state is notified twice
            // this is a problem because it overwrites the scheduling of actions performed through bco
            // e.g. an agent controlling something would always be superseded by these actions scheduled through
            // the openHAB user
            // therefore, in the following a hack strategy is implemented which tests if a matching requested
            // state was cached and if yes this update will not be scheduled

            // skip if update is still compatible and would change nothing
            if (Services.isCompatible(serviceState, serviceType, Services.invokeProviderServiceMethod(serviceType, internalBuilder))) {
                throw new RejectedException("Incoming state already applied!");
            }

            // check if incoming state is compatible with the current one
            synchronized (requestedStateCacheSync) {

                // check if any requested service state is compatible to given one
                if (requestedStateCache.containsKey(serviceType) && Services.isCompatible(serviceState, serviceType, requestedStateCache.get(serviceType))) {

                    // there are two reasons why we can reach this state
                    // 1. The incoming state update is just an update of a further affected state.
                    // 2. It`s just an additional synchronization update
                    // in case we are compatible with the current state we don't care about the update.
                    return serviceState;
                }
            }

            // clear requested state if set but not compatible any more with the incoming one.
            if (requestedState != null && !Services.isCompatible(serviceState, serviceType, requestedState)) {
                resetRequestedServiceState(serviceType, internalBuilder);
            }

            // in case of a system update, we can just apply the service state if no actions are currently scheduled.
            try {
                // todo: maybe we need to filter actions which are already done as well but then we have to deal with the scheduling lock which is may not a good idea because of deadlocks.
                if (scheduledActionList.isEmpty() && Services.getResponsibleAction(serviceState).getActionInitiator().getInitiatorType() == InitiatorType.SYSTEM && Services.getResponsibleAction(serviceState).getPriority().getNumber() > Priority.NO.getNumber()
                ) {
                    // Because the service state update was not remapped we can be sure its triggered externally and not via bco.
                    // If in this case the event is initiated by the system, we can be sure that it is caused by a hardware synchronization purpose.
                    // Therefore, we can just apply the update and can skip to force the action execution which could otherwise block some
                    // low priority future action for a certain amount of time in case this system action has any priority defined.
                    return serviceState;
                }
            } catch (NotAvailableException ex) {
                // if responsible action is not available, then we should continue since this action was maybe externally triggered by a human.
            }

            // force execution to properly apply new state synchronized with the current action scheduling
            return forceActionExecution(serviceState, serviceType, internalBuilder);
        } catch (RejectedException ex) {
            // pass through rejection to make it comparable to the error case.
            throw ex;
        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not compute new state!", ex);
        }
    }

    private void resetRequestedServiceState(final ServiceType serviceType, final DB internalBuilder) {
        try {
            requestedStateCache.remove(serviceType);
            internalBuilder.clearField(ProtoBufFieldProcessor.getFieldDescriptor(internalBuilder, Services.getServiceFieldName(serviceType, ServiceTempus.REQUESTED)));
        } catch (CouldNotPerformException ex) {
            ExceptionPrinter.printHistory("Could not reset requested service state of type " + serviceType.name(), ex, logger);
        }
    }

    private Message forceActionExecution(final Message serviceState, final ServiceType serviceType, final DB internalBuilder) throws CouldNotPerformException, InterruptedException {

        try {
            final Message.Builder serviceStateBuilder = serviceState.toBuilder();

            // retrieve the responsible action field descriptor.
            final ActionDescription.Builder responsibleActionBuilder;

            // compute responsible action if not exist
            if (!Services.hasResponsibleAction(serviceStateBuilder)) {
                logger.warn("Incoming data update does not provide its responsible action! Recover responsible action and continue...");
                StackTracePrinter.printStackTrace(logger);
                ActionDescriptionProcessor.generateAndSetResponsibleAction(serviceStateBuilder, serviceType, this, Timeout.INFINITY_TIMEOUT, TimeUnit.MILLISECONDS, false, false, false, Priority.NO, null);
            }

            // if there was another action executing before, abort it because its anyway outdated and if it is important, than it gets executed after rescheduling anyway.
            if (!scheduledActionList.isEmpty()) {
                final SchedulableAction schedulableAction = scheduledActionList.get(0);
                if (schedulableAction.isProcessing()) {
                    // only abort/reject action if the action is processing
                    if (schedulableAction.getActionDescription().getInterruptible()) {
                        schedulableAction.abort();
                    } else {
                        schedulableAction.reject();
                    }
                }
            }

            // add action to force

            scheduledActionList.add(0, new ActionImpl(serviceStateBuilder.build(), this, dataLock));

            // trigger a reschedule which can trigger the action with a higher priority again
            reschedule();

            // update action list in builder
            syncActionList(internalBuilder);
            return serviceStateBuilder.build();
        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not force action execution!", ex);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @throws InterruptedException     {@inheritDoc}
     * @throws CouldNotPerformException {@inheritDoc}
     */
    @Override
    public void cancelAllActions() throws InterruptedException, CouldNotPerformException {

        builderSetup.lockWriteInterruptibly(LOCK_CONSUMER_SCHEDULING);
        try {
            // cancel requested action cache
            requestedStateCache.clear();

            // cancel all actions except the service termination
            for (int i = scheduledActionList.size() - 1; i >= 0; i--) {

                // do not cancel the termination action
                if (scheduledActionList.get(i).getActionDescription().getPriority() == Priority.TERMINATION) {
                    continue;
                }

                // cancel all other actions on the stack
                final Action removedAction = scheduledActionList.remove(i);
                if(!removedAction.isDone()) {
                    removedAction.cancel();
                }
            }

            // final reschedule for cleanup
            reschedule();
        } finally {
            builderSetup.unlockWrite(NotificationStrategy.AFTER_LAST_RELEASE);
        }
    }

    /**
     * Method stores the current state into the last state.
     *
     * @param serviceType     defines which service state will be transferred.
     * @param internalBuilder the builder where the changes are applied on.
     */
    protected void updateLastWithCurrentState(final ServiceType serviceType, final DB internalBuilder) {
        try {
            final Object currentState = Services.invokeServiceMethod(serviceType, PROVIDER, ServiceTempus.CURRENT, internalBuilder);
            Services.invokeServiceMethod(serviceType, OPERATION, ServiceTempus.LAST, internalBuilder, currentState);
        } catch (NotAvailableException ex) {
            // skip last state update if the current state is not available.
        } catch (CouldNotPerformException ex) {
            ExceptionPrinter.printHistory("Could not save last state!", ex, logger);
        }
    }

    /**
     * Method copies the responsible action of the source state to the target state.
     * Source as well as the target state are resolved via the builder instance.
     * The timestamp of the service state is copied as well.
     *
     * @param sourceServiceType the type which refers the source service state.
     * @param targetServiceType the type which refers the target service state.
     * @param builder           the builder used to resolve the service states.
     */
    protected void copyResponsibleAction(final ServiceType sourceServiceType, final ServiceType targetServiceType, final DB builder) {
        try {
            // extract source and target message
            final Message sourceServiceState = (Message) Services.invokeServiceMethod(sourceServiceType, PROVIDER, ServiceTempus.CURRENT, builder);
            Message targetServiceState = (Message) Services.invokeServiceMethod(targetServiceType, PROVIDER, ServiceTempus.CURRENT, builder);
            targetServiceState = Services.setResponsibleAction(Services.getResponsibleAction(sourceServiceState), targetServiceState);

            // copy timestamp
            targetServiceState = TimestampProcessor.copyTimestamp(sourceServiceState, targetServiceState);

            // copy state
            Services.invokeOperationServiceMethod(targetServiceType, builder, targetServiceState);
        } catch (CouldNotPerformException ex) {
            ExceptionPrinter.printHistory("Could not copy responsible action!", ex, logger);
        }
    }

    /**
     * Method can be implemented by sub classes to apply other than the default changes.
     *
     * @param internalBuilder The data builder of this unit which already contains the updated state.
     * @param serviceType     The service type which has been updated.
     *
     * @throws InterruptedException is thrown if the thread is externally interrupted.
     */
    protected void applyCustomDataUpdate(DB internalBuilder, ServiceType serviceType) throws InterruptedException {
        // overwrite in sub classes if a change in one service also results in a change of another
    }

    private boolean hasOperationServiceForType(ServiceType serviceType) throws NotAvailableException {
        for (ServiceDescription serviceDescription : getUnitTemplate().getServiceDescriptionList()) {
            if (serviceDescription.getServiceType() == serviceType && serviceDescription.getPattern() == OPERATION) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void addDataObserver(ServiceTempus serviceTempus, Observer<DataProvider<D>, D> observer) {
        unitDataObservableMap.get(serviceTempus).addObserver(observer);
    }

    @Override
    public void removeDataObserver(ServiceTempus serviceTempus, Observer<DataProvider<D>, D> observer) {
        unitDataObservableMap.get(serviceTempus).removeObserver(observer);
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

    @Override
    public Future<Void> restoreSnapshot(final Snapshot snapshot) {
        return UnitProcessor.restoreSnapshot(snapshot, logger, this);
    }

    @Override
    public Future<AuthenticatedValue> restoreSnapshotAuthenticated(final AuthenticatedValue authenticatedSnapshot) {
        try {
            return UnitProcessor.restoreSnapshotAuthenticated(authenticatedSnapshot, logger, getConfig(), this);
        } catch (NotAvailableException ex) {
            return FutureProcessor.canceledFuture(AuthenticatedValue.class, new CouldNotPerformException("Could not restore authenticated snapshot!", ex));
        }
    }

    protected AuthPair verifyAccessPermission(final AuthenticationBaseData authenticationBaseData, final ServiceType serviceType) throws CouldNotPerformException {
        try {
            if (JPService.getProperty(JPAuthentication.class).getValue()) {
                return AuthorizationWithTokenHelper.canDo(authenticationBaseData, getConfig(), PermissionType.ACCESS, Registries.getUnitRegistry(), getUnitType(), serviceType);
            } else {
                return new AuthPair();
            }
        } catch (JPNotAvailableException ex) {
            throw new CouldNotPerformException("Could not check JPEnableAuthentication property", ex);
        }
    }

    @Override
    public Message getServiceState(final ServiceType serviceType) throws NotAvailableException {
        return getServiceState(serviceType, null);
    }

    protected Message getServiceState(final ServiceType serviceType, String userId) throws NotAvailableException {
        try {
            // check if user has permissions to read the service state
            if (!AuthorizationHelper.canRead(getConfig(), userId, Registries.getUnitRegistry().getAuthorizationGroupUnitConfigRemoteRegistry(true).getEntryMap(), Registries.getUnitRegistry().getLocationUnitConfigRemoteRegistry(true).getEntryMap())) {
                if (userId == null) {
                    throw new PermissionDeniedException("User[Other] has no permission to read " + serviceType.name() + " of " + this);
                }
                throw new PermissionDeniedException("User[" + Registries.getUnitRegistry().getUnitConfigById(userId).getUserConfig().getUserName() + "] has no permission to read " + serviceType.name() + " of " + this);
            }
            return Services.invokeProviderServiceMethod(serviceType, getData());
        } catch (CouldNotPerformException ex) {
            throw new NotAvailableException("ServiceState", ex);
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

    @SuppressWarnings("unchecked")
    protected D filterDataForUser(DB dataBuilder, UserClientPair userClientPair) throws CouldNotPerformException {
        try {

            if (Registries.getUnitRegistry().isDataAvailable()) {
                // test if user or client is inside the admin group, if yes return the unfiltered data builder
                try {
                    final UnitConfig adminGroup = Registries.getUnitRegistry().getUnitConfigByAlias(UnitRegistry.ADMIN_GROUP_ALIAS);
                    for (final String memberId : adminGroup.getAuthorizationGroupConfig().getMemberIdList()) {
                        if (userClientPair.getUserId().equals(memberId) || userClientPair.getClientId().equals(memberId)) {
                            return (D) dataBuilder.build();
                        }
                    }
                } catch (CouldNotPerformException ex) {
                    // admin group not available so just continue
                }

                // no admin so test normal permissions
                if (AuthorizationHelper.canRead(getConfig(), userClientPair, Registries.getUnitRegistry().getAuthorizationGroupUnitConfigRemoteRegistry(true).getEntryMap(), Registries.getUnitRegistry().getLocationUnitConfigRemoteRegistry(true).getEntryMap())) {
                    // user has read permissions so send everything
                    return (D) dataBuilder.build();
                }
            }

            // otherwise filter everything

            // filter all service states
            for (final FieldDescriptor fieldDescriptor : dataBuilder.getDescriptorForType().getFields()) {
                if (fieldDescriptor.getType() == FieldDescriptor.Type.MESSAGE) {
                    dataBuilder.clearField(fieldDescriptor);
                }
            }
            // filter executing or scheduled action list
            dataBuilder.clearField(ProtoBufFieldProcessor.getFieldDescriptor(dataBuilder, Action.TYPE_FIELD_NAME_ACTION));
            return (D) dataBuilder.build();
        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not filter data by user permissions!", ex);
        }
    }

    @Override
    public void activate() throws InterruptedException, CouldNotPerformException {
        super.activate();
        // in test mode we should directly reschedule so the termination action takes place.
        if(JPService.testMode()) {
            try {
                if(!terminatingActionId.equals(TERMINATION_ACTION_NOT_AVAILABLE)) {
                    getActionById(terminatingActionId, getClass().getSimpleName()).execute().get(3, TimeUnit.SECONDS);
                }
            } catch (CouldNotPerformException | ExecutionException | TimeoutException ex) {
                ExceptionPrinter.printHistory("Could not reschedule termination action!", ex, logger);
            }
        }
    }

    /**
     * Method can be used to register a new operation service for this controller.
     *
     * @param serviceType      the type of the new service.
     * @param operationService the service which performes the operation.
     *
     * @throws CouldNotPerformException is thrown if the type of the service is already registered.
     */
    protected void registerOperationService(final ServiceType serviceType, final OperationService operationService) throws CouldNotPerformException {

        if (operationServiceMap.containsKey(serviceType)) {
            throw new VerificationFailedException("OperationService for Type[" + serviceType.name() + "] already registered!");
        }
        operationServiceMap.put(serviceType, operationService);
    }

    /**
     * Method can be used to remove a previously registered operation service for this controller.
     *
     * @param serviceType the type of the service to remove.
     */
    protected void removeOperationService(final ServiceType serviceType) {
        operationServiceMap.remove(serviceType);
    }

    /**
     * Method returns a map of all registered operation services.
     *
     * @return an unmodifiable map of operation services where the service type is used as key.
     */
    public Map<ServiceType, OperationService> getOperationServiceMap() {
        return Collections.unmodifiableMap(operationServiceMap);
    }

    /**
     * {@inheritDoc}
     *
     * @param serviceState {@inheritDoc}
     * @param serviceType  {@inheritDoc}
     *
     * @return {@inheritDoc}
     */
    @Override
    public Future<ActionDescription> performOperationService(final Message serviceState, final ServiceType serviceType) {
        try {

            // handle default case
            if (operationServiceMap.containsKey(serviceType)) {

                // resolve operation service impl
                final OperationService operationService = operationServiceMap.get(serviceType);

                // handle simple state loop back
                if (operationService == OperationService.SIMPLE_STATE_ADOPTER) {
                    try {
                        applyDataUpdate(TimestampProcessor.updateTimestampWithCurrentTimeIfMissing(serviceState), serviceType);
                        return FutureProcessor.completedFuture(ServiceStateProcessor.getResponsibleAction(serviceState, () -> ActionDescription.getDefaultInstance()));
                    } catch (CouldNotPerformException ex) {
                        return FutureProcessor.canceledFuture(ActionDescription.class, ex);
                    }
                }

                // invoke operation service routine
                return (Future<ActionDescription>) Services.invokeOperationServiceMethod(serviceType, operationService, serviceState);
            }

            // handle provider control mode
            if (JPService.getValue(JPProviderControlMode.class, false)) {

                // validating time
                final Message.Builder providerServiceState = TimestampProcessor.updateTimestampWithCurrentTimeIfMissing(serviceState.toBuilder());

                // if provider control enabled for debugging and this action is a provider service only, then we need to reset the initiator type to system
                // since it would be responsible for an provider state update during normal operation anyway.
                final ActionDescription.Builder responsibleActionBuilder = ServiceStateProcessor.getResponsibleActionBuilder(providerServiceState);
                responsibleActionBuilder.getActionInitiatorBuilder().setInitiatorType(InitiatorType.SYSTEM);

                applyDataUpdate(providerServiceState, serviceType);
                return FutureProcessor.completedFuture(responsibleActionBuilder.build());
            }

            // handle invalid case
            return FutureProcessor.canceledFuture(ActionDescription.class, new CouldNotPerformException("Operation service for type[" + serviceType.name() + "] not registered for "+ this));

        } catch (CouldNotPerformException ex) {
            return FutureProcessor.canceledFuture(ActionDescription.class, ex);
        } catch (Exception ex) {
            ExceptionPrinter.printHistory("Unexpected exception while performing operation service!", ex, logger);
            return FutureProcessor.canceledFuture(ActionDescription.class, ex);
        }
    }

    @Override
    public Future<AggregatedServiceState> queryAggregatedServiceState(QueryType.Query databaseQuery) {
        return InfluxDbProcessor.queryAggregatedServiceState(databaseQuery);
    }

    @Override
    public Future<AuthenticatedValue> queryAggregatedServiceStateAuthenticated(final AuthenticatedValue databaseQuery) {
        return GlobalCachedExecutorService.submit(() ->
                AuthenticatedServiceProcessor.authenticatedAction(databaseQuery, QueryType.Query.class, (message, authenticationBaseData) -> {
                    AuthorizationWithTokenHelper.canDo(authenticationBaseData, getConfig(), PermissionType.READ, Registries.getUnitRegistry(), getUnitType(), ServiceType.UNKNOWN);

                    final Future<AggregatedServiceState> aggregatedServiceStateFuture = queryAggregatedServiceState(message);
                    try {
                        return aggregatedServiceStateFuture.get(5, TimeUnit.SECONDS);
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                        throw new CouldNotPerformException("Authenticated action was interrupted!", ex);
                    } catch (ExecutionException | TimeoutException ex) {
                        throw new CouldNotPerformException("Authenticated action failed!", ex);
                    } finally {
                        if (!aggregatedServiceStateFuture.isDone()) {
                            aggregatedServiceStateFuture.cancel(true);
                        }
                    }
                }));
    }

    @Override
    public Future<RecordCollection> queryRecord(QueryType.Query databaseQuery) {
        return InfluxDbProcessor.queryRecord(databaseQuery);
    }

    @Override
    public Future<AuthenticatedValue> queryRecordAuthenticated(final AuthenticatedValue databaseQuery) {
        return GlobalCachedExecutorService.submit(() ->
                AuthenticatedServiceProcessor.authenticatedAction(databaseQuery, QueryType.Query.class, (message, authenticationBaseData) -> {
                    AuthorizationWithTokenHelper.canDo(authenticationBaseData, getConfig(), PermissionType.READ, Registries.getUnitRegistry(), getUnitType(), ServiceType.UNKNOWN);

                    final Future<RecordCollection> recordCollectionFuture = queryRecord(message);
                    try {
                        return recordCollectionFuture.get(5, TimeUnit.SECONDS);
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                        throw new CouldNotPerformException("Authenticated action was interrupted!", ex);
                    } catch (ExecutionException | TimeoutException ex) {
                        throw new CouldNotPerformException("Authenticated action failed!", ex);
                    } finally {
                        if (!recordCollectionFuture.isDone()) {
                            recordCollectionFuture.cancel(true);
                        }
                    }
                }));
    }
}
