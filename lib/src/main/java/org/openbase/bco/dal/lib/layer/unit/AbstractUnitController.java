package org.openbase.bco.dal.lib.layer.unit;

/*
 * #%L
 * BCO DAL Library
 * %%
 * Copyright (C) 2014 - 2018 openbase.org
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

import com.google.protobuf.Descriptors;
import com.google.protobuf.Descriptors.EnumValueDescriptor;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.GeneratedMessage;
import com.google.protobuf.Message;
import org.openbase.bco.authentication.lib.AuthenticatedServiceProcessor;
import org.openbase.bco.authentication.lib.AuthenticationBaseData;
import org.openbase.bco.authentication.lib.AuthorizationHelper;
import org.openbase.bco.authentication.lib.AuthorizationHelper.PermissionType;
import org.openbase.bco.authentication.lib.com.AbstractAuthenticatedConfigurableController;
import org.openbase.bco.authentication.lib.jp.JPAuthentication;
import org.openbase.bco.dal.lib.action.ActionDescriptionProcessor;
import org.openbase.bco.dal.lib.action.ActionImpl;
import org.openbase.bco.dal.lib.layer.service.Service;
import org.openbase.bco.dal.lib.layer.service.ServiceStateProcessor;
import org.openbase.bco.dal.lib.layer.service.Services;
import org.openbase.bco.dal.lib.layer.service.consumer.ConsumerService;
import org.openbase.bco.dal.lib.layer.service.operation.OperationService;
import org.openbase.bco.dal.lib.layer.service.provider.ProviderService;
import org.openbase.bco.dal.lib.layer.service.stream.StreamService;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.bco.registry.unit.lib.auth.AuthorizationWithTokenHelper;
import org.openbase.bco.registry.unit.remote.UnitRegistryRemote;
import org.openbase.jps.core.JPService;
import org.openbase.jps.exception.JPNotAvailableException;
import org.openbase.jul.exception.*;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.extension.protobuf.ClosableDataBuilder;
import org.openbase.jul.extension.protobuf.MessageObservable;
import org.openbase.jul.extension.protobuf.processing.ProtoBufFieldProcessor;
import org.openbase.jul.extension.rsb.com.RPCHelper;
import org.openbase.jul.extension.rsb.iface.RSBLocalServer;
import org.openbase.jul.extension.rsb.scope.ScopeGenerator;
import org.openbase.jul.extension.rsb.scope.ScopeTransformer;
import org.openbase.jul.extension.rst.iface.ScopeProvider;
import org.openbase.jul.extension.rst.processing.LabelProcessor;
import org.openbase.jul.extension.rst.processing.TimestampProcessor;
import org.openbase.jul.pattern.Observable;
import org.openbase.jul.pattern.Observer;
import org.openbase.jul.processing.StringProcessor;
import org.openbase.jul.schedule.FutureProcessor;
import org.openbase.jul.schedule.GlobalCachedExecutorService;
import rsb.Scope;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rst.domotic.action.ActionDescriptionType.ActionDescription;
import rst.domotic.action.ActionFutureType.ActionFuture;
import rst.domotic.action.SnapshotType;
import rst.domotic.action.SnapshotType.Snapshot;
import rst.domotic.authentication.AuthenticatedValueType.AuthenticatedValue;
import rst.domotic.registry.UnitRegistryDataType.UnitRegistryData;
import rst.domotic.service.ServiceDescriptionType.ServiceDescription;
import rst.domotic.service.ServiceStateDescriptionType.ServiceStateDescription;
import rst.domotic.service.ServiceTemplateType.ServiceTemplate.ServicePattern;
import rst.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import rst.domotic.service.ServiceTempusTypeType.ServiceTempusType.ServiceTempus;
import rst.domotic.state.EnablingStateType.EnablingState;
import rst.domotic.unit.UnitConfigType.UnitConfig;
import rst.domotic.unit.UnitTemplateType.UnitTemplate;
import rst.rsb.ScopeType;
import rst.timing.TimestampType.Timestamp;

import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import static rst.domotic.service.ServiceTemplateType.ServiceTemplate.ServicePattern.OPERATION;
import static rst.domotic.service.ServiceTemplateType.ServiceTemplate.ServicePattern.PROVIDER;

/**
 * @param <D>  the data type of this unit used for the state synchronization.
 * @param <DB> the builder used to build the unit data instance.
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public abstract class AbstractUnitController<D extends GeneratedMessage, DB extends D.Builder<DB>> extends AbstractAuthenticatedConfigurableController<D, DB, UnitConfig> implements UnitController<D, DB> {

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(ActionFuture.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(ActionDescription.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(SnapshotType.Snapshot.getDefaultInstance()));
    }

    private final Observer<UnitRegistryData> unitRegistryObserver;
    private final Map<ServiceTempus, UnitDataFilteredObservable<D>> unitDataObservableMap;
    private final Map<ServiceTempus, Map<ServiceType, MessageObservable>> serviceTempusServiceTypeObservableMap;
    private Map<ServiceType, OperationService> operationServiceMap;
    private UnitTemplate template;
    private boolean initialized = false;

    private String classDescription = "";

    public AbstractUnitController(final Class unitClass, final DB builder) throws InstantiationException {
        super(builder);
        this.unitDataObservableMap = new HashMap<>();
        this.operationServiceMap = new TreeMap<>();
        this.serviceTempusServiceTypeObservableMap = new HashMap<>();
        for (final ServiceTempus serviceTempus : ServiceTempus.values()) {
            unitDataObservableMap.put(serviceTempus, new UnitDataFilteredObservable<>(this, serviceTempus));
            super.addDataObserver((Observable<D> source, D data) -> {
                unitDataObservableMap.get(serviceTempus).notifyObservers(data);
            });

            serviceTempusServiceTypeObservableMap.put(serviceTempus, new HashMap<>());
        }
        this.unitRegistryObserver = new Observer<UnitRegistryData>() {
            @Override
            public void update(Observable<UnitRegistryData> source, UnitRegistryData data) throws Exception {
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
            final Set<ServiceType> serviceTypeSet = new HashSet<>();
            for (final ServiceDescription serviceDescription : getUnitTemplate().getServiceDescriptionList()) {

                // check if already handled
                if (!serviceTypeSet.contains(serviceDescription.getServiceType())) {
                    serviceTypeSet.add(serviceDescription.getServiceType());
                    try {
                        Object serviceData = Services.invokeServiceMethod(serviceDescription.getServiceType(), ServicePattern.PROVIDER, serviceTempus, data);
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
            return getConfig().getEnablingState().getValue().equals(EnablingState.State.ENABLED);
        } catch (CouldNotPerformException ex) {
            ExceptionPrinter.printHistory(ex, logger);
        }
        return false;
    }

    /**
     * @return
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
            assert config != null;
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

            for (final ServiceDescription serviceDescription : template.getServiceDescriptionList()) {

                // create observable if new
                if (!serviceTempusServiceTypeObservableMap.get(serviceTempus).containsKey(serviceDescription.getServiceType())) {
                    serviceTempusServiceTypeObservableMap.get(serviceTempus).put(serviceDescription.getServiceType(), new MessageObservable(this));
                }
            }
        }

        for (final ServiceTempus serviceTempus : ServiceTempus.values()) {
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
            throw new NotAvailableException("id", ex);
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
            throw new NotAvailableException("label", ex);
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
                    StringProcessor.transformUpperCaseToCamelCase(serviceDescription.getServiceType().name()) +
                            StringProcessor.transformUpperCaseToCamelCase(serviceDescription.getPattern().name());
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
                String serviceDataTypeName = StringProcessor.transformUpperCaseToCamelCase(serviceInterfaceMapEntry.getValue().getServiceType().name()).replaceAll("Service", "");
                String servicePatternName = StringProcessor.transformUpperCaseToCamelCase(serviceInterfaceMapEntry.getValue().getPattern().name());
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
                    throw new CouldNotPerformException("Could not register methods for serviceInterface [" + serviceInterfaceClass.getName() + "]");
                }

                RPCHelper.registerInterface((Class) serviceInterfaceClass, this, server);
            } catch (CouldNotPerformException ex) {
                ExceptionPrinter.printHistory(new CouldNotPerformException("Could not register Interface[" + serviceInterfaceClass + "] Method [" + serviceInterfaceMapEntry.getKey() + "] for Unit[" + this.getLabel() + "].", ex), logger);
            }
        }
    }

    public Future<ActionFuture> applyUnauthorizedAction(final Message serviceAttribute, final ServiceType serviceType) throws CouldNotPerformException {
        // todo pleminoq: please authenticate action with rsb user token.
        return applyAction(ActionDescriptionProcessor.generateActionDescriptionBuilderAndUpdate(serviceAttribute, serviceType, this, false).build());
    }

    @Override
    public Future<ActionFuture> applyAction(final ActionDescription actionDescription) throws CouldNotPerformException {
        try {
            if (!actionDescription.hasDescription() || actionDescription.getDescription().isEmpty()) {
                // Fallback print in case the description is not available. 
                // Please make sure all action descriptions provide a description.
                logger.info("Action[" + actionDescription.getServiceStateDescription().getServiceType() + "] for unit[" + ScopeGenerator.generateStringRep(getScope()) + "] is without a description");
            } else {
                logger.info(actionDescription.getDescription());
            }
            logger.info("================");

            final ActionImpl action = new ActionImpl(this);
            action.init(actionDescription);
            return action.execute();
        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not apply action!", ex);
        }
    }

    @Override
    public Future<AuthenticatedValue> applyActionAuthenticated(final AuthenticatedValue authenticatedValue) {
        return GlobalCachedExecutorService.submit(() -> AuthenticatedServiceProcessor.authenticatedAction(authenticatedValue, ActionDescription.class, this, (actionDescription, authenticationBaseData) -> {
            try {
                final String authorityString = verifyAccessPermission(authenticationBaseData, actionDescription.getServiceStateDescription().getServiceType());
                final String description = actionDescription.getDescription().replace(ActionDescriptionProcessor.AUTHORITY_KEY, authorityString);
                // TODO: user string should be set in action description ... all authentication info should be updated here
                try {
                    applyAction(actionDescription.toBuilder().setDescription(description).build()).get();
                } catch (ExecutionException ex) {
                    throw new CouldNotPerformException("Could not restore snapshot authenticated", ex);
                }
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }

            return null;
        }));
    }

    @Override
    public void addServiceStateObserver(ServiceTempus serviceTempus, ServiceType serviceType, Observer observer) {
        serviceTempusServiceTypeObservableMap.get(serviceTempus).get(serviceType).addObserver(observer);
    }

    @Override
    public void removeServiceStateObserver(ServiceTempus serviceTempus, ServiceType serviceType, Observer observer) {
        serviceTempusServiceTypeObservableMap.get(serviceTempus).get(serviceType).removeObserver(observer);
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
            for (final MessageObservable serviceObservable : serviceTempusServiceTypeObservableMap.get(serviceTempus).values()) {
                serviceObservable.shutdown();
            }
            unitDataObservableMap.get(serviceTempus).shutdown();
        }
    }


    @Override
    public void applyDataUpdate(final Message serviceState, final ServiceType serviceType) throws CouldNotPerformException {
        logger.debug("Apply service[" + serviceType + "] update[" + serviceState + "] for " + this + ".");

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
            Services.verifyServiceState(newState);

            updateTransactionId();

            // update the action description
            Descriptors.FieldDescriptor descriptor = ProtoBufFieldProcessor.getFieldDescriptor(newState, Service.RESPONSIBLE_ACTION_FIELD_NAME);
            newState = newState.toBuilder().setField(descriptor, newState.getField(descriptor)).build();

            // copy latestValueOccurrence map from current state, only if available
            Descriptors.FieldDescriptor latestValueOccurrenceField = ProtoBufFieldProcessor.getFieldDescriptor(newState, ServiceStateProcessor.FIELD_NAME_LAST_VALUE_OCCURRENCE);
            if (latestValueOccurrenceField != null) {
                Message oldServiceState = Services.invokeProviderServiceMethod(serviceType, this);
                newState = newState.toBuilder().setField(latestValueOccurrenceField, oldServiceState.getField(latestValueOccurrenceField)).build();
            }

            // update the current state
            Services.invokeServiceMethod(serviceType, OPERATION, ServiceTempus.CURRENT, internalBuilder, newState);

            // Update timestamps
            try {
                GeneratedMessage.Builder serviceStateBuilder = (GeneratedMessage.Builder) internalBuilder.getClass().getMethod("get" + Services.getServiceStateName(serviceType) + "Builder").invoke(internalBuilder);

                //Set timestamp if missing
                if (!serviceStateBuilder.hasField(serviceStateBuilder.getDescriptorForType().findFieldByName("timestamp"))) {
                    logger.warn("State[" + serviceStateBuilder.getClass().getSimpleName() + "] of " + this + " does not contain any state related timestamp!");
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
     *
     * @param sourceServiceType the type which refers the source service state.
     * @param targetServiceType the type which refers the target service state.
     * @param builder           the builder used to resolve the service states.
     */
    protected void copyResponsibleAction(final ServiceType sourceServiceType, final ServiceType targetServiceType, final DB builder) {
        try {
            final Message sourceServiceState = (Message) Services.invokeServiceMethod(sourceServiceType, PROVIDER, ServiceTempus.CURRENT, builder);
            Message targetServiceState = (Message) Services.invokeServiceMethod(targetServiceType, PROVIDER, ServiceTempus.CURRENT, builder);
            targetServiceState = Services.setResponsibleAction(Services.getResponsibleAction(sourceServiceState), targetServiceState);
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
    public void addDataObserver(ServiceTempus serviceTempus, Observer<D> observer) {
        unitDataObservableMap.get(serviceTempus).addObserver(observer);
    }

    @Override
    public void removeDataObserver(ServiceTempus serviceTempus, Observer<D> observer) {
        unitDataObservableMap.get(serviceTempus).removeObserver(observer);
    }

    @Override
    public Future<Void> restoreSnapshot(final Snapshot snapshot) throws CouldNotPerformException, InterruptedException {
//        return internalRestoreSnapshot(snapshot, null);
        try {
            Collection<Future> futureCollection = new ArrayList<>();
            for (final ServiceStateDescription serviceStateDescription : snapshot.getServiceStateDescriptionList()) {
                ActionDescription actionDescription = ActionDescription.newBuilder().setServiceStateDescription(serviceStateDescription).build();
                futureCollection.add(applyAction(actionDescription));
            }
            return GlobalCachedExecutorService.allOf(futureCollection);
        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not record snapshot!", ex);
        }
    }

    protected Future<Void> internalRestoreSnapshot(final Snapshot snapshot, final AuthenticationBaseData authenticationBaseData) throws CouldNotPerformException, InterruptedException {
        return restoreSnapshot(snapshot);
        //TODO: implementation has to be fixed like in ServiceRemoteManager
        //        try {
//            Collection<Future<ActionFuture>> futureCollection = new ArrayList<>();
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
//                        for (Future<ActionFuture> actionFuture : input) {
//                            AuthenticationClientHandler.handleServiceServerResponse(ticketEvaluationWrapper.getSessionKey(), ticketEvaluationWrapper.getTicketAuthenticatorWrapper(), actionFuture.get().getTicketAuthenticatorWrapper());
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

    private String verifyAccessPermission(final AuthenticationBaseData authenticationBaseData, final ServiceType serviceType) throws CouldNotPerformException {
        try {
            if (JPService.getProperty(JPAuthentication.class).getValue()) {
                return AuthorizationWithTokenHelper.canDo(authenticationBaseData, getConfig(), PermissionType.ACCESS, Registries.getUnitRegistry(), getUnitType(), serviceType);
            } else {
                return "Other";
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
                throw new PermissionDeniedException("User[" + Registries.getUnitRegistry().getUnitConfigById(userId).getUserConfig().getUserName() + "] has no permission to read " + serviceType.name() + " of " + this);
            }
            return Services.invokeProviderServiceMethod(serviceType, getData());
        } catch (CouldNotPerformException ex) {
            throw new NotAvailableException("ServiceState", ex);
        }
    }

    protected D filterDataForUser(DB dataBuilder, String userId) throws CouldNotPerformException {
        try {
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
     * @return {@inheritDoc}
     */
    @Override
    public Future<Void> performOperationService(final Message serviceState, final ServiceType serviceType) {
        //logger.debug("Set " + getUnitType().name() + "[" + getLabel() + "] to PowerState [" + serviceState + "]");
        if (!operationServiceMap.containsKey(serviceType)) {
            return FutureProcessor.canceledFuture(Void.class, new CouldNotPerformException("Operation service for type[" + serviceType.name() + "] not registered"));
        }
        try {
            return (Future<Void>) Services.invokeOperationServiceMethod(serviceType, operationServiceMap.get(serviceType), serviceState);
        } catch (CouldNotPerformException ex) {
            return FutureProcessor.canceledFuture(Void.class, ex);
        }
    }
}
