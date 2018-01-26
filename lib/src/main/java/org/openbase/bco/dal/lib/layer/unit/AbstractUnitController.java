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
import com.google.protobuf.GeneratedMessage;
import com.google.protobuf.Message;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.Future;

import org.openbase.bco.authentication.lib.AuthenticatedServerManager;
import org.openbase.bco.authentication.lib.AuthorizationHelper;
import org.openbase.bco.authentication.lib.jp.JPAuthentication;
import org.openbase.bco.dal.lib.action.ActionImpl;
import org.openbase.bco.dal.lib.layer.service.Service;
import org.openbase.bco.dal.lib.layer.service.Services;
import org.openbase.bco.dal.lib.layer.service.consumer.ConsumerService;
import org.openbase.bco.dal.lib.layer.service.operation.OperationService;
import org.openbase.bco.dal.lib.layer.service.provider.ProviderService;
import org.openbase.bco.dal.lib.layer.service.stream.StreamService;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.bco.registry.unit.remote.UnitRegistryRemote;
import org.openbase.jps.core.JPService;
import org.openbase.jps.exception.JPNotAvailableException;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InitializationException;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.exception.InvalidStateException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.exception.NotSupportedException;
import org.openbase.jul.exception.PermissionDeniedException;
import org.openbase.jul.exception.VerificationFailedException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.extension.protobuf.ClosableDataBuilder;
import org.openbase.jul.extension.protobuf.IdentifiableMessage;
import org.openbase.jul.extension.protobuf.MessageObservable;
import org.openbase.jul.extension.protobuf.processing.ProtoBufFieldProcessor;
import org.openbase.jul.extension.rsb.com.AbstractConfigurableController;
import org.openbase.jul.extension.rsb.com.RPCHelper;
import org.openbase.jul.extension.rsb.iface.RSBLocalServer;
import org.openbase.jul.extension.rsb.scope.ScopeGenerator;
import org.openbase.jul.extension.rsb.scope.ScopeTransformer;
import org.openbase.jul.extension.rst.iface.ScopeProvider;
import org.openbase.jul.extension.rst.processing.TimestampProcessor;
import org.openbase.jul.pattern.Observable;
import org.openbase.jul.pattern.Observer;
import org.openbase.jul.processing.StringProcessor;
import rsb.Scope;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rst.domotic.action.ActionAuthorityType.ActionAuthority;
import rst.domotic.action.ActionDescriptionType.ActionDescription;
import rst.domotic.authentication.TicketAuthenticatorWrapperType.TicketAuthenticatorWrapper;
import rst.domotic.action.ActionFutureType.ActionFuture;
import rst.domotic.action.SnapshotType;
import rst.domotic.registry.UnitRegistryDataType.UnitRegistryData;
import rst.domotic.service.ServiceDescriptionType.ServiceDescription;
import rst.domotic.service.ServiceTemplateType.ServiceTemplate.ServicePattern;

import static rst.domotic.service.ServiceTemplateType.ServiceTemplate.ServicePattern.CONSUMER;
import static rst.domotic.service.ServiceTemplateType.ServiceTemplate.ServicePattern.OPERATION;
import static rst.domotic.service.ServiceTemplateType.ServiceTemplate.ServicePattern.PROVIDER;

import rst.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import rst.domotic.service.ServiceTempusTypeType.ServiceTempusType.ServiceTempus;
import rst.domotic.state.EnablingStateType.EnablingState;
import rst.domotic.unit.UnitConfigType.UnitConfig;
import rst.domotic.unit.UnitTemplateType.UnitTemplate;
import rst.domotic.unit.dal.RollerShutterDataType.RollerShutterData;
import rst.rsb.ScopeType;

/**
 * @param <D>  the data type of this unit used for the state synchronization.
 * @param <DB> the builder used to build the unit data instance.
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public abstract class AbstractUnitController<D extends GeneratedMessage, DB extends D.Builder<DB>> extends AbstractConfigurableController<D, DB, UnitConfig> implements UnitController<D, DB> {

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(ActionFuture.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(ActionDescription.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(SnapshotType.Snapshot.getDefaultInstance()));
    }

    private final Observer<UnitRegistryData> unitRegistryObserver;
    private final Map<ServiceTempus, UnitDataFilteredObservable<D>> unitDataObservableMap;
    private final Map<ServiceTempus, Map<ServiceType, MessageObservable>> serviceTempusServiceTypeObservableMap;
    private final List<Service> serviceList;

    private UnitTemplate template;
    private boolean initialized = false;

    private long transactionId = 0;

    public AbstractUnitController(final Class unitClass, final DB builder) throws InstantiationException {
        super(builder);
        this.serviceList = new ArrayList<>();
        this.unitDataObservableMap = new HashMap<>();
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

            if (config.getLabel().isEmpty()) {
                throw new NotAvailableException("Field config.label is emty!");
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
                if (!serviceTypeSet.contains(serviceDescription.getType())) {
                    serviceTypeSet.add(serviceDescription.getType());
                    try {
                        Object serviceData = Services.invokeServiceMethod(serviceDescription.getType(), ServicePattern.PROVIDER, serviceTempus, data);
                        serviceTempusServiceTypeObservableMap.get(serviceTempus).get(serviceDescription.getType()).notifyObservers(serviceData);
                    } catch (CouldNotPerformException ex) {
                        logger.debug("Could not notify state update for service[" + serviceDescription.getType() + "] because this service is not supported by this controller.", ex);
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

        template = Registries.getUnitRegistry(true).getUnitTemplateByType(config.getType());

        // register service observable which are not handled yet.
        for (final ServiceTempus serviceTempus : ServiceTempus.values()) {
            unitDataObservableMap.get(serviceTempus).updateToUnitTemplateChange(template);

            for (final ServiceDescription serviceDescription : template.getServiceDescriptionList()) {

                // create observable if new
                if (!serviceTempusServiceTypeObservableMap.get(serviceTempus).containsKey(serviceDescription.getType())) {
                    serviceTempusServiceTypeObservableMap.get(serviceTempus).put(serviceDescription.getType(), new MessageObservable(this));
                }
            }
        }

        for (final ServiceTempus serviceTempus : ServiceTempus.values()) {
            // cleanup service observable related to new unit template
            outer:
            for (final ServiceType serviceType : serviceTempusServiceTypeObservableMap.get(serviceTempus).keySet()) {
                for (final ServiceDescription serviceDescription : template.getServiceDescriptionList()) {

                    // verify if service type is still valid.
                    if (serviceType == serviceDescription.getType()) {
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
            if (!tmpConfig.hasId()) {
                throw new NotAvailableException("unitconfig.label");
            }

            if (tmpConfig.getId().isEmpty()) {
                throw new InvalidStateException("unitconfig.label is empty");
            }
            return getConfig().getLabel();
        } catch (CouldNotPerformException ex) {
            throw new NotAvailableException("label", ex);
        }
    }

    @Override
    public UnitTemplate.UnitType getUnitType() throws NotAvailableException {
        return getConfig().getType();
    }

    @Override
    public UnitTemplate getUnitTemplate() throws NotAvailableException {
        if (template == null) {
            throw new NotAvailableException("UnitTemplate");
        }
        return template;
    }

    public Collection<Service> getServices() {
        return Collections.unmodifiableList(serviceList);
    }

    public void registerService(final Service service) {
        serviceList.add(service);
    }

    @Override
    public void registerMethods(RSBLocalServer server) throws CouldNotPerformException {
        RPCHelper.registerInterface(Unit.class, this, server);

        // collect and register service interface methods via unit templates
        HashMap<String, ServiceDescription> serviceInterfaceMap = new HashMap<>();
        for (ServiceDescription serviceDescription : getUnitTemplate().getServiceDescriptionList()) {
            serviceInterfaceMap.put(StringProcessor.transformUpperCaseToCamelCase(serviceDescription.getType().name())
                    + StringProcessor.transformUpperCaseToCamelCase(serviceDescription.getPattern().name()), serviceDescription);
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
                String serviceDataTypeName = StringProcessor.transformUpperCaseToCamelCase(serviceInterfaceMapEntry.getValue().getType().name()).replaceAll("Service", "");
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

    @Override
    public Future<ActionFuture> applyAction(final ActionDescription actionDescription) throws CouldNotPerformException, InterruptedException {
        try {
            if (!actionDescription.hasDescription() && actionDescription.getDescription().isEmpty()) {
                // Fallback print in case the description is not available. 
                // Please make sure all action descriptions provide a description.
                logger.info("Apply action on " + this);
            } else {
                if (!actionDescription.hasDescription() || actionDescription.getDescription().isEmpty()) {
                    logger.info("Action[" + actionDescription.getServiceStateDescription().getServiceType() + ", " + actionDescription.getServiceStateDescription().getServiceAttribute() + "] for unit[" + ScopeGenerator.generateStringRep(getScope()) + "] is without a description");
                } else {
                    logger.info(actionDescription.getDescription());
                }
            }
            logger.info("================");

            final ActionImpl action = new ActionImpl(this);
            action.init(actionDescription);
            return action.execute();
        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not apply action!", ex);
        }
    }

    /**
     * Verifies the authority by verifying its internal TicketAuthenticationWrapper with the authenticator and updates the given {@code ticketAuthenticatorWrapperBuilder}.
     * It the authenticator has no TicketAuthenticationWrapper, the given {@code ticketAuthenticatorWrapperBuilder} is just not updated.
     *
     * @param actionAuthority                   the authority verified
     * @param ticketAuthenticatorWrapperBuilder the ticketAuthenticator to update.
     * @throws VerificationFailedException                          if someone is logged in but the verification with the authenticator fails
     * @throws org.openbase.jul.exception.PermissionDeniedException is thrown in case the authority has no permission for the related action.
     * @throws java.lang.InterruptedException
     */
    public void verifyAndUpdateAuthority(final ActionAuthority actionAuthority, final TicketAuthenticatorWrapper.Builder ticketAuthenticatorWrapperBuilder) throws VerificationFailedException, PermissionDeniedException, InterruptedException, CouldNotPerformException {

        // check if authentication is enabled
        try {
            if (!JPService.getProperty(JPAuthentication.class).getValue()) {
                return;
            }
        } catch (JPNotAvailableException ex) {
            throw new CouldNotPerformException("Could not check JPEncableAuthentication property", ex);
        }

        // If there is no TicketAuthenticationWrapper, check permissions without userId and groups.
        if (!actionAuthority.hasTicketAuthenticatorWrapper()) {
            try {
                Map<String, IdentifiableMessage<String, UnitConfig, UnitConfig.Builder>> locations = Registries.getUnitRegistry().getLocationUnitConfigRemoteRegistry().getEntryMap();

                if (!AuthorizationHelper.canAccess(getConfig(), null, null, locations)) {
                    throw new PermissionDeniedException("You have no permission to execute this action.");
                }
                return;
            } catch (NotAvailableException ex) {
                throw new VerificationFailedException("Verifying authority failed", ex);
            }
        }

        try {
            TicketAuthenticatorWrapper wrapper = actionAuthority.getTicketAuthenticatorWrapper();
            AuthenticatedServerManager.TicketEvaluationWrapper validatedTicketWrapper = AuthenticatedServerManager.getInstance().evaluateClientServerTicket(wrapper);
            Map<String, IdentifiableMessage<String, UnitConfig, UnitConfig.Builder>> groups = Registries.getUnitRegistry().getAuthorizationGroupUnitConfigRemoteRegistry().getEntryMap();
            Map<String, IdentifiableMessage<String, UnitConfig, UnitConfig.Builder>> locations = Registries.getUnitRegistry().getLocationUnitConfigRemoteRegistry().getEntryMap();

            if (!AuthorizationHelper.canAccess(getConfig(), validatedTicketWrapper.getUserId(), groups, locations)) {
                throw new PermissionDeniedException("You have no permission to execute this action.");
            }

            // update current ticketAuthenticatorWrapperBuilder
            ticketAuthenticatorWrapperBuilder.setAuthenticator(validatedTicketWrapper.getTicketAuthenticatorWrapper().getAuthenticator());
            ticketAuthenticatorWrapperBuilder.setTicket(validatedTicketWrapper.getTicketAuthenticatorWrapper().getTicket());
        } catch (IOException | CouldNotPerformException ex) {
            throw new VerificationFailedException("Verifying authority failed", ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw ExceptionPrinter.printHistoryAndReturnThrowable(new VerificationFailedException("Interrupted while verifrying authority", ex), logger);
        }
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
        try {
            return getClass().getSimpleName() + "[" + getConfig().getType() + "[" + getLabel() + "]]";
        } catch (NotAvailableException | NullPointerException ex) {
            return getClass().getSimpleName() + "[?]";
        }
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

    public synchronized long getTransactionId() {
        return transactionId;
    }

    /**
     * Transaction id should never be 0 because thats the builder default value.
     *
     * @return the next transactionId
     */
    public long generateTransactionId() {
        return ++transactionId;
    }

    @Override
    public void applyDataUpdate(final Object serviceArgument, final ServiceType serviceType) throws CouldNotPerformException {
        logger.debug("Apply service[" + serviceType + "] update[" + serviceArgument + "] for " + this + ".");

        Message value = (Message) serviceArgument;
        try (ClosableDataBuilder<DB> dataBuilder = getDataBuilder(this)) {
            DB internalBuilder = dataBuilder.getInternalBuilder();
            // move current state to last state
            Object currentState = Services.invokeServiceMethod(serviceType, PROVIDER, ServiceTempus.CURRENT, internalBuilder);
            Services.invokeServiceMethod(serviceType, OPERATION, ServiceTempus.LAST, internalBuilder, currentState);

            Message newState;

            // only operation service action can be remapped
            if (hasOperationServiceForType(serviceType) && Services.hasServiceState(serviceType, ServiceTempus.REQUESTED, internalBuilder)) {
                // if it is an operation service test if the requested state is the new current state

                // test if all fields match to the last request
                boolean equalFields = true;

                Message requestedState = (Message) Services.invokeServiceMethod(serviceType, PROVIDER, ServiceTempus.REQUESTED, internalBuilder);
                for (Descriptors.FieldDescriptor field : value.getDescriptorForType().getFields()) {
                    // ignore timestamps
                    if (field.getName().equals(TimestampProcessor.TIMESTEMP_FIELD.toLowerCase())) {
                        continue;
                    }
                    if (value.hasField(field) && requestedState.hasField(field) && !(value.getField(field).equals(requestedState.getField(field)))) {
                        equalFields = false;
                        break;
                    }
                }

                // choose with which value to update
                if (equalFields) {

                    // use the requested state but update the timestamp
                    Descriptors.FieldDescriptor timestampField = ProtoBufFieldProcessor.getFieldDescriptor(value, TimestampProcessor.TIMESTEMP_FIELD.toLowerCase());
                    newState = requestedState.toBuilder().setField(timestampField, value.getField(timestampField)).build();

                    // clear requested state
                    Descriptors.FieldDescriptor requestedStateField = ProtoBufFieldProcessor.getFieldDescriptor(internalBuilder, Services.getServiceFieldName(serviceType, ServiceTempus.REQUESTED));
                    internalBuilder.clearField(requestedStateField);

                } else {
                    newState = value;
                }
            } else {
                // no operation service or no requested state, so just update the current state
                newState = value;
            }

            // verify the service state
            Services.verifyServiceState(newState);

            // update the action description
            Descriptors.FieldDescriptor descriptor = ProtoBufFieldProcessor.getFieldDescriptor(newState, Service.RESPONSIBLE_ACTION_FIELD_NAME);
            ActionDescription actionDescription = (ActionDescription) newState.getField(descriptor);
            actionDescription = actionDescription.toBuilder().setTransactionId(generateTransactionId()).build();
            newState = newState.toBuilder().setField(descriptor, actionDescription).build();

            // update the current state
            Services.invokeServiceMethod(serviceType, OPERATION, ServiceTempus.CURRENT, internalBuilder, newState);

            // do other state depending update in sub classes
            applyDataUpdate(internalBuilder, serviceType);
        } catch (Exception ex) {
            throw new CouldNotPerformException("Could not apply service[" + serviceType.name() + "] update[" + value + "] for " + this + "!", ex);
        }
    }

    /**
     * Method can be implemented by sub classes to apply other than the default changes.
     *
     * @param internalBuilder The data builder of this unit which already contains the updated state.
     * @param serviceType     The service type which has been updated.
     */
    protected void applyDataUpdate(DB internalBuilder, ServiceType serviceType) {
        // overwrite in sub classes if a change in one service also results in a change of another
    }

    private boolean hasOperationServiceForType(ServiceType serviceType) throws NotAvailableException {
        for (ServiceDescription serviceDescription : getUnitTemplate().getServiceDescriptionList()) {
            if (serviceDescription.getType() == serviceType && serviceDescription.getPattern() == OPERATION) {
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
}
