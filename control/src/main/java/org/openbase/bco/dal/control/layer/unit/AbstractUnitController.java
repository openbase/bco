package org.openbase.bco.dal.control.layer.unit;

/*
 * #%L
 * BCO DAL Control
 * %%
 * Copyright (C) 2014 - 2019 openbase.org
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
import org.openbase.bco.authentication.lib.*;
import org.openbase.bco.authentication.lib.AuthorizationHelper.PermissionType;
import org.openbase.bco.authentication.lib.com.AbstractAuthenticatedConfigurableController;
import org.openbase.bco.authentication.lib.jp.JPAuthentication;
import org.openbase.bco.dal.control.action.ActionImpl;
import org.openbase.bco.dal.lib.action.Action;
import org.openbase.bco.dal.lib.action.ActionComparator;
import org.openbase.bco.dal.lib.action.ActionDescriptionProcessor;
import org.openbase.bco.dal.lib.action.SchedulableAction;
import org.openbase.bco.dal.lib.jp.JPProviderControlMode;
import org.openbase.bco.dal.lib.jp.JPUnitAllocation;
import org.openbase.bco.dal.lib.layer.service.*;
import org.openbase.bco.dal.lib.layer.service.consumer.ConsumerService;
import org.openbase.bco.dal.lib.layer.service.operation.OperationService;
import org.openbase.bco.dal.lib.layer.service.provider.ProviderService;
import org.openbase.bco.dal.lib.layer.service.stream.StreamService;
import org.openbase.bco.dal.lib.layer.unit.Unit;
import org.openbase.bco.dal.lib.layer.unit.UnitController;
import org.openbase.bco.dal.lib.layer.unit.UnitDataFilteredObservable;
import org.openbase.bco.dal.remote.layer.unit.Units;
import org.openbase.bco.dal.remote.layer.unit.location.LocationRemote;
import org.openbase.bco.registry.lib.util.UnitConfigProcessor;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.bco.registry.unit.lib.UnitRegistry;
import org.openbase.bco.registry.unit.lib.auth.AuthorizationWithTokenHelper;
import org.openbase.bco.registry.unit.remote.UnitRegistryRemote;
import org.openbase.jps.core.JPService;
import org.openbase.jps.exception.JPNotAvailableException;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.exception.*;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.extension.protobuf.ClosableDataBuilder;
import org.openbase.jul.extension.protobuf.MessageObservable;
import org.openbase.jul.extension.protobuf.processing.ProtoBufFieldProcessor;
import org.openbase.jul.extension.rsb.com.RPCHelper;
import org.openbase.jul.extension.rsb.iface.RSBLocalServer;
import org.openbase.jul.extension.rsb.scope.ScopeGenerator;
import org.openbase.jul.extension.rsb.scope.ScopeTransformer;
import org.openbase.jul.extension.type.iface.ScopeProvider;
import org.openbase.jul.extension.type.processing.LabelProcessor;
import org.openbase.jul.extension.type.processing.TimestampJavaTimeTransform;
import org.openbase.jul.extension.type.processing.TimestampProcessor;
import org.openbase.jul.pattern.Observer;
import org.openbase.jul.pattern.provider.DataProvider;
import org.openbase.jul.processing.StringProcessor;
import org.openbase.jul.schedule.FutureProcessor;
import org.openbase.jul.schedule.GlobalCachedExecutorService;
import org.openbase.jul.schedule.SyncObject;
import org.openbase.jul.schedule.Timeout;
import org.openbase.type.com.ScopeType;
import org.openbase.type.domotic.action.ActionDescriptionType.ActionDescription;
import org.openbase.type.domotic.action.ActionDescriptionType.ActionDescription.Builder;
import org.openbase.type.domotic.action.ActionInitiatorType.ActionInitiator;
import org.openbase.type.domotic.action.ActionParameterType.ActionParameter;
import org.openbase.type.domotic.action.ActionParameterType.ActionParameterOrBuilder;
import org.openbase.type.domotic.action.ActionReferenceType.ActionReference;
import org.openbase.type.domotic.action.SnapshotType;
import org.openbase.type.domotic.action.SnapshotType.Snapshot;
import org.openbase.type.domotic.authentication.AuthenticatedValueType.AuthenticatedValue;
import org.openbase.type.domotic.registry.UnitRegistryDataType.UnitRegistryData;
import org.openbase.type.domotic.service.ServiceDescriptionType.ServiceDescription;
import org.openbase.type.domotic.service.ServiceStateDescriptionType.ServiceStateDescription;
import org.openbase.type.domotic.service.ServiceTemplateType.ServiceTemplate.ServicePattern;
import org.openbase.type.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import org.openbase.type.domotic.service.ServiceTempusTypeType.ServiceTempusType.ServiceTempus;
import org.openbase.type.domotic.state.ActionStateType.ActionState;
import org.openbase.type.domotic.state.ActionStateType.ActionState.State;
import org.openbase.type.domotic.unit.UnitConfigType.UnitConfig;
import org.openbase.type.domotic.unit.UnitTemplateType.UnitTemplate;
import org.openbase.type.timing.TimestampType.Timestamp;
import rsb.Scope;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;

import java.io.Serializable;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
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
    /**
     * Timeout defining how long finished actions will be minimally kept in the action list.
     */
    private static final long FINISHED_ACTION_REMOVAL_TIMEOUT = TimeUnit.SECONDS.toMillis(15);

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(ActionDescription.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(ActionDescription.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(SnapshotType.Snapshot.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(AuthenticatedValue.getDefaultInstance()));
    }

    private final Observer<DataProvider<UnitRegistryData>, UnitRegistryData> unitRegistryObserver;
    private final Map<ServiceTempus, UnitDataFilteredObservable<D>> unitDataObservableMap;
    private final Map<ServiceTempus, Map<ServiceType, MessageObservable<ServiceStateProvider<Message>, Message>>> serviceTempusServiceTypeObservableMap;
    private final SyncObject scheduledActionListLock = new SyncObject("ScheduledActionListLock");
    private final ReentrantReadWriteLock actionListNotificationLock = new ReentrantReadWriteLock();
    private final ActionComparator actionComparator;
    private Map<ServiceType, OperationService> operationServiceMap;
    private UnitTemplate template;
    private boolean initialized = false;
    private String classDescription = "";
    private ArrayList<SchedulableAction> scheduledActionList;
    private Timeout scheduleTimeout;
    private boolean actionNotificationSkipped = false;

    public AbstractUnitController(final DB builder) throws InstantiationException {
        super(builder);
        this.unitDataObservableMap = new HashMap<>();
        this.operationServiceMap = new TreeMap<>();
        this.scheduledActionList = new ArrayList<>();
        this.serviceTempusServiceTypeObservableMap = new HashMap<>();
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

        this.scheduleTimeout = new Timeout(1000) {
            @Override
            public void expired() {
                reschedule();
            }
        };

        this.actionComparator = new ActionComparator(() -> getBaseLocationRemote(false).getEmphasisState());
    }

    @Override
    public void init(Scope scope) throws InitializationException, InterruptedException {
        try {
            init(ScopeTransformer.transform(scope));
        } catch (CouldNotPerformException ex) {
            throw new InitializationException(this, ex);
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
            init(ScopeGenerator.generateScope(label, getClass().getSimpleName(), location.getScope()));
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
                        logger.debug("Could not notify state update for service[" + serviceDescription.getServiceType() + "] because this service is not supported by this controller.", ex);
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

    /**
     * @return
     *
     * @deprecated please use Registries.getUnitRegistry(true) instead;
     */
    @Deprecated
    public UnitRegistryRemote getUnitRegistry() {
        try {
            return Registries.getUnitRegistry(true);
        } catch (Exception ex) {
            ExceptionPrinter.printHistory(ex, logger);
            return null;
        }
    }

    @Override
    public UnitConfig applyConfigUpdate(final UnitConfig config) throws CouldNotPerformException, InterruptedException {

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
                    serviceTempusServiceTypeObservableMap.get(serviceTempus).put(serviceDescription.getServiceType(), new ServiceDataFilteredObservable<>(new ServiceStateProvider<>(serviceDescription.getServiceType(), this)));
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

        return super.applyConfigUpdate(config);
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
    public UnitTemplate.UnitType getUnitType() throws NotAvailableException {
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
    public void registerMethods(final RSBLocalServer server) throws CouldNotPerformException {
        super.registerMethods(server);

        RPCHelper.registerInterface(Unit.class, this, server);

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
                    if (serviceInterfaceClass == null) {
                        throw new NotAvailableException(serviceInterfaceMapEntry.getKey());
                    }
                } catch (ClassNotFoundException | ClassCastException ex) {
                    throw new CouldNotPerformException("Could not load service interface!", ex);
                }

                if (!serviceInterfaceClass.isAssignableFrom(this.getClass())) {
                    // interface not supported dummy.
                    throw new CouldNotPerformException("Could not register methods for ServiceInterface [" + serviceInterfaceClass.getName() + "]");
                }

                RPCHelper.registerInterface((Class) serviceInterfaceClass, this, server);
            } catch (CouldNotPerformException ex) {
                ExceptionPrinter.printHistory(new CouldNotPerformException("Could not register Interface[" + serviceInterfaceClass + "] Method [" + serviceInterfaceMapEntry.getKey() + "] for Unit[" + this.getLabel() + "].", ex), logger);
            }
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
            builder.getServiceStateDescriptionBuilder().setUnitId(getId());

            return applyUnauthorizedAction(ActionDescriptionProcessor.generateActionDescriptionBuilder(builder).build());
        } catch (CouldNotPerformException ex) {
            return FutureProcessor.canceledFuture(ActionDescription.class, new CouldNotPerformException("Could not apply action!", ex));
        }
    }

    public Future<ActionDescription> applyUnauthorizedAction(final ActionDescription actionDescription) {
        return AuthenticatedServiceProcessor.requestAuthenticatedAction(actionDescription, ActionDescription.class, MOCKUP_SESSION_MANAGER, this::applyActionAuthenticated);
    }

    @Override
    public Future<ActionDescription> applyAction(final ActionDescription actionDescription) {
        try {
            final ActionImpl action = new ActionImpl(actionDescription, this);
            try {
                if (JPService.getProperty(JPUnitAllocation.class).getValue()) {
                    return scheduleAction(action);
                } else {
                    return action.execute();
                }
            } catch (JPNotAvailableException ex) {
                throw new CouldNotPerformException("Could not check unit allocation flag.", ex);
            }
        } catch (CouldNotPerformException ex) {
            return FutureProcessor.canceledFuture(ActionDescription.class, new CouldNotPerformException("Could not apply action!", ex));
        }
    }

    @Override
    public Future<ActionDescription> cancelAction(final ActionDescription actionDescription) {
        return cancelAction(actionDescription, null);
    }

    protected Future<ActionDescription> cancelAction(final ActionDescription actionDescription, String authenticatedId) {
        //TODO validate that on other permissions the initiator id is an empty string
        if (authenticatedId == null) {
            authenticatedId = "";
        }

        try {
            Action actionToCancel = null;
            synchronized (scheduledActionListLock) {

                // lookup action to cancel
                for (Action action : scheduledActionList) {
                    // provided action id is a direct match
                    if (action.getId().equals(actionDescription.getId())) {
                        actionToCancel = action;
                        break;
                    }

                    // provided action id appears in the action chain of the action
                    for (final ActionReference actionReference : action.getActionDescription().getActionCauseList()) {
                        if (actionReference.getActionId().equals(actionDescription.getId())) {
                            actionToCancel = action;
                            break;
                        }
                    }
                }

                // handle if action was not found
                if (actionToCancel == null) {
                    logger.debug("Cannot cancel an unknown action, but than its not executing anyway.");
                    return CompletableFuture.completedFuture(actionDescription.toBuilder().setActionState(ActionState.newBuilder().setValue(State.UNKNOWN).build()).build());
                }

                if (!Registries.getUnitRegistry().getUnitConfigByAlias(UnitRegistry.ADMIN_GROUP_ALIAS).getAuthorizationGroupConfig().getMemberIdList().contains(authenticatedId)) {
                    // authenticated is not an admin
                    if (!actionToCancel.getActionDescription().getActionInitiator().getInitiatorId().equals(authenticatedId)) {
                        // authenticated user is not the direct initiator
                        boolean isAuthenticated = false;
                        // // check if the authenticated user appears somewhere in the chain
                        for (ActionReference actionReference : actionToCancel.getActionDescription().getActionCauseList()) {
                            if (actionReference.getActionInitiator().getInitiatorId().equals(authenticatedId)) {
                                isAuthenticated = true;
                                break;
                            }
                        }

                        if (!isAuthenticated) {
                            throw new PermissionDeniedException("User [" + authenticatedId + "] is not allowed to cancel action [" + actionDescription.getId() + "]");
                        }
                    }
                }
                // cancel the action which automatically triggers a reschedule.
                return actionToCancel.cancel();
            }
        } catch (CouldNotPerformException ex) {
            return FutureProcessor.canceledFuture(ActionDescription.class, new CouldNotPerformException("Could not cancel Action[" + actionDescription.getId() + "]", ex));
        }
    }

    private Future<ActionDescription> scheduleAction(final SchedulableAction actionToSchedule) {
        Action executingAction = reschedule(actionToSchedule);

        if (actionToSchedule != executingAction) {
            logger.info("================================================================================");
            if (executingAction == null) {
                logger.error("{} seems not to be valid and was excluded from execution of {}.", actionToSchedule, this);
            }
            logger.info("{} was postponed because of {} and added to the scheduling queue of {} at position {}.", actionToSchedule, executingAction, this, getSchedulingIndex(actionToSchedule));
        }

        return CompletableFuture.completedFuture(actionToSchedule.getActionDescription());
    }

    private int getSchedulingIndex(Action action) {
        synchronized (scheduledActionListLock) {
            return scheduledActionList.indexOf(action);
        }
    }

    /**
     * Recalculate the action ranking and execute the action with the highest ranking if not already executing or finished.
     * If the current action is not finished it will be rejected.
     *
     * @return the {@code action} which is ranked highest and which is therefore currently allocating this unit.
     * If there is no action left to schedule null is returned.
     */
    public Action reschedule() {
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
     */
    public Action reschedule(final SchedulableAction actionToSchedule) {
        synchronized (scheduledActionListLock) {
            // lock the notification lock so that action state changes applied during rescheduling do not trigger notifications
            actionListNotificationLock.writeLock().lock();

            if (actionToSchedule != null) {
                // test if there is another action already in the list by the same initiator
                try {
                    final ActionInitiator newInitiator = ActionDescriptionProcessor.getInitialInitiator(actionToSchedule.getActionDescription());
                    for (final SchedulableAction schedulableAction : new ArrayList<>(scheduledActionList)) {
                        if (schedulableAction.isDone()) {
                            // skip actions which are done and remain on the stack for notification purposes
                            continue;
                        }

                        final ActionInitiator currentInitiator = ActionDescriptionProcessor.getInitialInitiator(schedulableAction.getActionDescription());
                        if (!newInitiator.getInitiatorId().equals(currentInitiator.getInitiatorId())) {
                            // actions do not have the same initiator
                            continue;
                        }

                        // same initiator - schedule the newer one
                        if (actionToSchedule.getCreationTime() < schedulableAction.getCreationTime()) {
                            // new action is actually older than one already scheduled by the same initiator, so reject it
                            actionToSchedule.reject();
                            logger.warn("New Action {} from initiator {} is older than a currently scheduled one", actionToSchedule, newInitiator.getInitiatorId());
                        } else {
                            // actionToSchedule is newer, so reject old one
                            schedulableAction.reject();
                        }
                    }
                } catch (NotAvailableException ex) {
                    ExceptionPrinter.printHistory("Could detect initiator or creation time!", ex, logger);
                }

                // add new action to the list
                scheduledActionList.add(actionToSchedule);
            }

            try {
                // save if there is at least one action still awaiting execution
                boolean atLeastOneActionNoteDone = false;
                // save if there is at least one action which is done but remains on the list
                boolean atLeastOneDoneActionOnList = false;

                // reject outdated and finish completed actions
                for (final SchedulableAction action : scheduledActionList) {
                    if (!action.isValid()) {
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
                            logger.warn("Remove finished action {} because its finishing time could not be evaluated", action);
                            scheduledActionList.remove(action);
                        }
                    } else {
                        atLeastOneActionNoteDone = true;
                    }
                }

                // skip if no actions are available
                if (!atLeastOneActionNoteDone) {
                    return null;
                }

                // detect and store current action
                SchedulableAction currentAction = null;
                for (final SchedulableAction schedulableAction : scheduledActionList) {
                    if (schedulableAction.getActionDescription().getActionState().getValue() == State.EXECUTING) {
                        currentAction = schedulableAction;
                        break;
                    }
                }

                // sort valid actions by priority
                scheduledActionList.sort(actionComparator);

                // detect action with highest ranking
                final SchedulableAction nextAction = scheduledActionList.get(0);

                // handle current action
                if (currentAction != null) {
                    // if the next action is still the same than we are finished
                    if (nextAction == currentAction) {
                        return currentAction;
                    }

                    // abort the current action
                    logger.warn("Abort current action {}, because new one {} has higher priority", currentAction, nextAction);
                    currentAction.abort();
                }

                // execute action with highest ranking
                nextAction.execute();

                // setup next schedule trigger
                try {
                    final long rescheduleTimeout = atLeastOneDoneActionOnList ? Math.min(FINISHED_ACTION_REMOVAL_TIMEOUT, nextAction.getExecutionTime()) : nextAction.getExecutionTime();
                    scheduleTimeout.restart(rescheduleTimeout);
                } catch (CouldNotPerformException ex) {
                    ExceptionPrinter.printHistory(new FatalImplementationErrorException("Could not setup rescheduling timeout! ", this, ex), logger);
                }

                return nextAction;
            } finally {
                try {
                    updateTransactionId();
                } catch (CouldNotPerformException ex) {
                    ExceptionPrinter.printHistory("Could not update transaction id", ex, logger);
                }
                // update action description list in unit builder
                notifyScheduledActionListUnlocked();
                // unlock notification lock so that notifications for action state changes are notified again
//                actionListNotificationLock.writeLock().unlock();
            }
        }
    }

    /**
     * Update the action list in the data builder and notify.
     */
    private void notifyScheduledActionListUnlocked() {
        try (final ClosableDataBuilder<DB> dataBuilder = getDataBuilder(this)) {
            final FieldDescriptor actionFieldDescriptor = ProtoBufFieldProcessor.getFieldDescriptor(dataBuilder.getInternalBuilder(), Action.TYPE_FIELD_NAME_ACTION);
            // get actions descriptions for all actions, repeat until no more notifications where skipped
            do {
                actionNotificationSkipped = false;
                dataBuilder.getInternalBuilder().clearField(actionFieldDescriptor);
                for (final Action action : scheduledActionList) {
                    dataBuilder.getInternalBuilder().addRepeatedField(actionFieldDescriptor, action.getActionDescription());
                }
            } while (actionNotificationSkipped);
            if (actionListNotificationLock.isWriteLockedByCurrentThread()) {
                actionListNotificationLock.writeLock().unlock();
            }
        } catch (Exception ex) {
            ExceptionPrinter.printHistory("Could not update action list!", ex, logger);
        }
    }


    /**
     * Update the action list in the data builder and notify. Skip updates if the unit is currently rescheduling actions.
     */
    public void notifyScheduledActionList() {
        if (actionListNotificationLock.isWriteLocked()) {
            // save if the notification was skipped
            actionNotificationSkipped = true;
            return;
        }

        notifyScheduledActionListUnlocked();
    }

    @Override
    public Future<AuthenticatedValue> applyActionAuthenticated(final AuthenticatedValue authenticatedValue) {
        return GlobalCachedExecutorService.submit(() -> AuthenticatedServiceProcessor.authenticatedAction(authenticatedValue, ActionDescription.class, this, (actionDescription, authenticationBaseData) -> {
            try {
                final AuthPair authPair = verifyAccessPermission(authenticationBaseData, actionDescription.getServiceStateDescription().getServiceType());
                final Builder actionDescriptionBuilder = actionDescription.toBuilder();

                // clear auth fields
                actionDescriptionBuilder.getActionInitiatorBuilder().clearAuthenticatedBy().clearAuthorizedBy();

                // if an authentication token is send replace the initiator in any case
                if (authenticationBaseData != null && authenticationBaseData.getAuthenticationToken() != null) {
                    actionDescriptionBuilder.getActionInitiatorBuilder().setInitiatorId(authenticationBaseData.getAuthenticationToken().getUserId());
                }

                // setup auth fields
                if (authPair.getAuthenticatedBy() != null) {
                    actionDescriptionBuilder.getActionInitiatorBuilder().setAuthenticatedBy(authPair.getAuthenticatedBy());
                }
                if (authPair.getAuthorizedBy() != null) {
                    actionDescriptionBuilder.getActionInitiatorBuilder().setAuthorizedBy(authPair.getAuthorizedBy());
                }


                return internalApplyActionAuthenticated(authenticatedValue, actionDescriptionBuilder, authenticationBaseData, authPair);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                throw new CouldNotPerformException("Authenticated action was interrupted!", ex);
            } catch (ExecutionException ex) {
                throw new CouldNotPerformException("Could not apply authenticated action!", ex);
            }
        }));
    }

    protected ActionDescription internalApplyActionAuthenticated(final AuthenticatedValue authenticatedValue, final ActionDescription.Builder actionDescriptionBuilder, final AuthenticationBaseData authenticationBaseData, final AuthPair authPair) throws InterruptedException, CouldNotPerformException, ExecutionException {
        if (actionDescriptionBuilder.hasId() && !actionDescriptionBuilder.getId().isEmpty() && actionDescriptionBuilder.getCancel()) {
            try {
                return cancelAction(actionDescriptionBuilder.build(), authPair.getAuthenticatedBy()).get();
            } catch (ExecutionException ex) {
                throw new CouldNotPerformException("Could not cancel authenticated action!", ex);
            }
        }

        return applyAction(actionDescriptionBuilder.build()).get();
    }

    @Override
    public void addServiceStateObserver(ServiceTempus serviceTempus, ServiceType serviceType, Observer<ServiceStateProvider<Message>, Message> observer) {
        if (serviceTempus == ServiceTempus.UNKNOWN) {
            // if unknown tempus add observer on all other tempi
            for (ServiceTempus value : ServiceTempus.values()) {
                if (value == ServiceTempus.UNKNOWN) {
                    continue;
                }

                addServiceStateObserver(value, serviceType, observer);
            }
        } else {
            serviceTempusServiceTypeObservableMap.get(serviceTempus).get(serviceType).addObserver(observer);
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
            serviceTempusServiceTypeObservableMap.get(serviceTempus).get(serviceType).removeObserver(observer);
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
    }

    @Override
    public void applyDataUpdate(final Message serviceState, final ServiceType serviceType) throws CouldNotPerformException {
        logger.trace("Apply service[" + serviceType + "] update[" + serviceState + "] for " + this + ".");

        try (ClosableDataBuilder<DB> dataBuilder = getDataBuilder(this)) {
            DB internalBuilder = dataBuilder.getInternalBuilder();

            // move current state to last state
            updateLastWithCurrentState(serviceType, internalBuilder);

            Message newState;

            // only operation service action can be remapped
            if (hasOperationServiceForType(serviceType) && Services.hasServiceState(serviceType, ServiceTempus.REQUESTED, internalBuilder)) {
                // if it is an operation service test if the requested state is the new current state

                // test if all fields match to the last request
                boolean equalFields = true;

                Message requestedState = (Message) Services.invokeServiceMethod(serviceType, PROVIDER, ServiceTempus.REQUESTED, internalBuilder);
                for (Descriptors.FieldDescriptor field : serviceState.getDescriptorForType().getFields()) {
                    // ignore repeated fields, which should be last value occurrences
                    if (field.isRepeated()) {
                        continue;
                    }

                    // ignore timestamps
                    if (field.getName().equals(TimestampProcessor.TIMESTEMP_FIELD_NAME)) {
                        continue;
                    }

                    if (serviceState.hasField(field) && requestedState.hasField(field) && !(serviceState.getField(field).equals(requestedState.getField(field)))) {
                        equalFields = false;
                        break;
                    }
                }

                // choose with which value to update
                if (equalFields) {

                    // use the requested state but update the timestamp if not available
                    if (TimestampProcessor.hasTimestamp(serviceState)) {
                        Descriptors.FieldDescriptor timestampField = ProtoBufFieldProcessor.getFieldDescriptor(serviceState, TimestampProcessor.TIMESTEMP_FIELD_NAME);
                        newState = requestedState.toBuilder().setField(timestampField, serviceState.getField(timestampField)).build();
                    } else {
                        newState = requestedState;
                    }

                    // clear requested state
                    Descriptors.FieldDescriptor requestedStateField = ProtoBufFieldProcessor.getFieldDescriptor(internalBuilder, Services.getServiceFieldName(serviceType, ServiceTempus.REQUESTED));
                    internalBuilder.clearField(requestedStateField);
                } else {
                    newState = serviceState;
                }
            } else {
                // no operation service or no requested state, so just update the current state
                newState = serviceState;
            }

            // verify the service state
            newState = Services.verifyAndRevalidateServiceState(newState);

            // update the action description
            Descriptors.FieldDescriptor descriptor = ProtoBufFieldProcessor.getFieldDescriptor(newState, Service.RESPONSIBLE_ACTION_FIELD_NAME);
            newState = newState.toBuilder().setField(descriptor, newState.getField(descriptor)).build();

            // copy latestValueOccurrence map from current state, only if available
            try {
                Descriptors.FieldDescriptor latestValueOccurrenceField = ProtoBufFieldProcessor.getFieldDescriptor(newState, ServiceStateProcessor.FIELD_NAME_LAST_VALUE_OCCURRENCE);
                Message oldServiceState = Services.invokeProviderServiceMethod(serviceType, internalBuilder);
                newState = newState.toBuilder().setField(latestValueOccurrenceField, oldServiceState.getField(latestValueOccurrenceField)).build();
            } catch (NotAvailableException ex) {
                // skip update if field is missing because some states do not contain latest value occurrences (ColorState, PowerConsumptionState, ...)
            }

            // update the current state
            Services.invokeServiceMethod(serviceType, OPERATION, ServiceTempus.CURRENT, internalBuilder, newState);

            // Update timestamps
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

            // do custom state depending update in sub classes
            applyCustomDataUpdate(internalBuilder, serviceType);
        } catch (Exception ex) {
            throw new CouldNotPerformException("Could not apply service[" + serviceType.name() + "] update[" + serviceState + "] for " + this + "!", ex);
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

            // copy state
            Services.invokeOperationServiceMethod(targetServiceType, builder, targetServiceState);

            // copy timestamp
            TimestampProcessor.copyTimestamp(sourceServiceState, targetServiceState);
        } catch (CouldNotPerformException ex) {
            ExceptionPrinter.printHistory("Could not copy responsible action!", ex, logger);
        }
    }

    /**
     * Method can be implemented by sub classes to apply other than the default changes.
     *
     * @param internalBuilder The data builder of this unit which already contains the updated state.
     * @param serviceType     The service type which has been updated.
     */
    protected void applyCustomDataUpdate(DB internalBuilder, ServiceType serviceType) {
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

    @Override
    public Future<Void> restoreSnapshot(final Snapshot snapshot) {
//        return internalRestoreSnapshot(snapshot, null);
        Collection<Future> futureCollection = new ArrayList<>();
        for (final ServiceStateDescription serviceStateDescription : snapshot.getServiceStateDescriptionList()) {
            ActionDescription actionDescription = ActionDescription.newBuilder().setServiceStateDescription(serviceStateDescription).build();
            futureCollection.add(applyAction(actionDescription));
        }
        return FutureProcessor.allOf(futureCollection);
    }

    protected Future<Void> internalRestoreSnapshot(final Snapshot snapshot, final AuthenticationBaseData authenticationBaseData) {
        return restoreSnapshot(snapshot);
        //TODO: implementation has to be fixed like in ServiceRemoteManager
        //        try {
//            Collection<Future<ActionDescription>> futureCollection = new ArrayList<>();
//            for (final ServiceStateDescription serviceStateDescription : snapshot.getServiceStateDescriptionList()) {
//                ActionDescription.Builder actionDescription = ActionDescription.newBuilder().setServiceStateDescription(serviceStateDescription);
//
//                if (ticketEvaluationWrapper != null) {
//                    actionDescription.getActionAuthorityBuilder().setTicketAuthenticatorWrapper(ticketEvaluationWrapper.getTicketAuthenticatorWrapper());
//                }
//
//                futureCollection.add(applyAction(actionDescription.build()));
//            }
//            return GlobalCachedExecutorService.allOf(input -> {
//                if (ticketEvaluationWrapper != null) {
//                    try {
//                        for (Future<ActionDescription> ActionDescription : input) {
//                            AuthenticationClientHandler.handleServiceServerResponse(ticketEvaluationWrapper.getSessionKey(), ticketEvaluationWrapper.getTicketAuthenticatorWrapper(), ActionDescription.get().getTicketAuthenticatorWrapper());
//                        }
//                    } catch (IOException | BadPaddingException ex) {
//                        throw new CouldNotPerformException("Could not validate response because it could not be decrypted", ex);
//                    } catch (ExecutionException ex) {
//                        throw new FatalImplementationErrorException("AllOf called result processable even though some futures did not finish", GlobalCachedExecutorService.getInstance(), ex);
//                    }
//                }
//                return null;
//            }, futureCollection);
//        } catch (CouldNotPerformException ex) {
//            throw new CouldNotPerformException("Could not record snapshot!", ex);
//        }
    }

    @Override
    public Future<AuthenticatedValue> restoreSnapshotAuthenticated(final AuthenticatedValue authenticatedSnapshot) {
        return GlobalCachedExecutorService.submit(() -> AuthenticatedServiceProcessor.authenticatedAction(authenticatedSnapshot, Snapshot.class, this, (snapshot, authenticationBaseData) -> {
            try {
                verifyAccessPermission(authenticationBaseData, ServiceType.UNKNOWN);

                try {
//                    internalRestoreSnapshot(snapshot, ticketEvaluationWrapper).get();
                    restoreSnapshot(snapshot).get();
                } catch (ExecutionException ex) {
                    throw new CouldNotPerformException("Could not restore snapshot authenticated", ex);
                }
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }

            return null;
        }));
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
            if (!AuthorizationHelper.canRead(getConfig(), userId, Registries.getUnitRegistry().getAuthorizationGroupUnitConfigRemoteRegistry().getEntryMap(), Registries.getUnitRegistry().getLocationUnitConfigRemoteRegistry().getEntryMap())) {
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
     * This method returns the base location remote of this unit.
     * If this unit is a location, than its parent location remote is returned,
     * otherwise the base location remote is returned which refers the location where this unit is placed in.
     *
     * @param waitForData flag defines if the method should block until the remote is fully synchronized.
     *
     * @return a location remote instance.
     *
     * @throws NotAvailableException          is thrown if the location remote is currently not available.
     * @throws java.lang.InterruptedException is thrown if the current was externally interrupted.
     */
    public LocationRemote getBaseLocationRemote(final boolean waitForData) throws NotAvailableException, InterruptedException {
        return Units.getUnit(getConfig().getPlacementConfig().getLocationId(), waitForData, Units.LOCATION);
    }

    @SuppressWarnings("unchecked")
    protected D filterDataForUser(DB dataBuilder, String userId) throws CouldNotPerformException {
        try {
            // test if user or client is inside the admin group, if yes return the unfiltered data builder
            if (userId != null) {
                try {
                    final UnitConfig adminGroup = Registries.getUnitRegistry().getUnitConfigByAlias(UnitRegistry.ADMIN_GROUP_ALIAS);
                    for (final String id : userId.split("@")) {
                        for (final String memberId : adminGroup.getAuthorizationGroupConfig().getMemberIdList()) {
                            if (id.equals(memberId)) {
                                return (D) dataBuilder.build();
                            }
                        }
                    }
                } catch (CouldNotPerformException ex) {
                    // admin group not available so just continue
                }
            }

            // no admin so test normal permissions
            if (AuthorizationHelper.canRead(getConfig(), userId, Registries.getUnitRegistry().getAuthorizationGroupUnitConfigRemoteRegistry().getEntryMap(), Registries.getUnitRegistry().getLocationUnitConfigRemoteRegistry().getEntryMap())) {
                // user has read permissions so send everything
                return (D) dataBuilder.build();
            } else {
                // filter all service states
                for (final FieldDescriptor fieldDescriptor : dataBuilder.getDescriptorForType().getFields()) {
                    if (fieldDescriptor.getType() == FieldDescriptor.Type.MESSAGE) {
                        dataBuilder.clearField(fieldDescriptor);
                    }
                }
                // filter executing or scheduled action list
                dataBuilder.clearField(ProtoBufFieldProcessor.getFieldDescriptor(dataBuilder, Action.TYPE_FIELD_NAME_ACTION));
                return (D) dataBuilder.build();
            }
        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not filter data by user permissions!", ex);
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
    public Future<Void> performOperationService(final Message serviceState, final ServiceType serviceType) {
        try {
            if (!operationServiceMap.containsKey(serviceType)) {
                if (JPService.getProperty(JPProviderControlMode.class).getValue()) {
                    applyDataUpdate(TimestampProcessor.updateTimestampWithCurrentTime(serviceState), serviceType);
                    return CompletableFuture.completedFuture(null);
                } else {
                    return FutureProcessor.canceledFuture(Void.class, new CouldNotPerformException("Operation service for type[" + serviceType.name() + "] not registered"));
                }
            }
            return (Future<Void>) Services.invokeOperationServiceMethod(serviceType, operationServiceMap.get(serviceType), serviceState);
        } catch (CouldNotPerformException ex) {
            return FutureProcessor.canceledFuture(Void.class, ex);
        } catch (Exception ex) {
            ExceptionPrinter.printHistory("Unexpected exception while performing operation service!", ex, logger);
            return FutureProcessor.canceledFuture(Void.class, ex);
        }
    }

    public static Class<? extends UnitController> detectUnitControllerClass(final UnitConfig unitConfig) throws CouldNotTransformException {

        String className = AbstractUnitController.class.getPackage().getName() + "." + StringProcessor.transformUpperCaseToPascalCase(unitConfig.getUnitType().name()) + "Controller";
        try {
            return (Class<? extends UnitController>) Class.forName(className);
        } catch (ClassNotFoundException ex) {
            try {
                throw new CouldNotTransformException(ScopeGenerator.generateStringRep(unitConfig.getScope()), UnitController.class, new NotAvailableException("Class", ex));
            } catch (CouldNotPerformException ex1) {
                throw new CouldNotTransformException(unitConfig.getLabel(), UnitController.class, new NotAvailableException("Class", ex));
            }
        }
    }
}
