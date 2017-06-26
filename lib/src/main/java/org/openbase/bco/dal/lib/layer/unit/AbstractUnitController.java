package org.openbase.bco.dal.lib.layer.unit;

/*
 * #%L
 * BCO DAL Library
 * %%
 * Copyright (C) 2014 - 2017 openbase.org
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
import com.google.protobuf.GeneratedMessage;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import org.openbase.bco.dal.lib.layer.service.Service;
import org.openbase.bco.dal.lib.layer.service.ServiceJSonProcessor;
import org.openbase.bco.dal.lib.layer.service.consumer.ConsumerService;
import org.openbase.bco.dal.lib.layer.service.operation.OperationService;
import org.openbase.bco.dal.lib.layer.service.provider.ProviderService;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.bco.registry.unit.remote.CachedUnitRegistryRemote;
import org.openbase.bco.registry.unit.remote.UnitRegistryRemote;
import org.openbase.bco.authentication.core.AuthenticatorController;
import org.openbase.bco.authentication.lib.ClientRemote;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InitializationException;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.exception.InvalidStateException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.exception.NotSupportedException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.extension.protobuf.MessageObservable;
import org.openbase.jul.extension.rsb.com.AbstractConfigurableController;
import org.openbase.jul.extension.rsb.com.RPCHelper;
import org.openbase.jul.extension.rsb.iface.RSBLocalServer;
import org.openbase.jul.extension.rsb.scope.ScopeGenerator;
import org.openbase.jul.extension.rsb.scope.ScopeTransformer;
import org.openbase.jul.extension.rst.iface.ScopeProvider;
import org.openbase.jul.extension.rst.processing.ActionDescriptionProcessor;
import org.openbase.jul.pattern.Observable;
import org.openbase.jul.pattern.Observer;
import org.openbase.jul.processing.StringProcessor;
import org.openbase.jul.schedule.GlobalCachedExecutorService;
import rsb.Scope;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rst.domotic.action.ActionFutureType.ActionFuture;
import rst.domotic.action.ActionDescriptionType.ActionDescription;
import rst.domotic.authentication.TicketAuthenticatorWrapperType.TicketAuthenticatorWrapper;
import rst.domotic.registry.UnitRegistryDataType.UnitRegistryData;
import rst.domotic.service.ServiceDescriptionType.ServiceDescription;
import rst.domotic.service.ServiceTemplateType.ServiceTemplate;
import static rst.domotic.service.ServiceTemplateType.ServiceTemplate.ServicePattern.CONSUMER;
import static rst.domotic.service.ServiceTemplateType.ServiceTemplate.ServicePattern.OPERATION;
import static rst.domotic.service.ServiceTemplateType.ServiceTemplate.ServicePattern.PROVIDER;
import rst.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import rst.domotic.state.EnablingStateType.EnablingState;
import rst.domotic.unit.UnitConfigType.UnitConfig;
import rst.domotic.unit.UnitTemplateType.UnitTemplate;
import rst.rsb.ScopeType;

/**
 *
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 *
 * @param <D> the data type of this unit used for the state synchronization.
 * @param <DB> the builder used to build the unit data instance.
 */
public abstract class AbstractUnitController<D extends GeneratedMessage, DB extends D.Builder<DB>> extends AbstractConfigurableController<D, DB, UnitConfig> implements UnitController<D, DB> {

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(ActionDescription.getDefaultInstance()));
    }

    public static long initTime = 0;
    public static long constructorTime = 0;

    protected UnitRegistryRemote unitRegistry;

//    private final UnitResourceAllocator<D, DB> resourceAllocator;
    private final List<Service> serviceList;
    private final ServiceJSonProcessor serviceJSonProcessor;
    private UnitTemplate template;
    private final Map<ServiceType, MessageObservable> serviceStateObservableMap;
    private final ClientRemote clientRemote;

    public AbstractUnitController(final Class unitClass, final DB builder) throws InstantiationException {
        super(builder);
        this.serviceJSonProcessor = new ServiceJSonProcessor();
        this.serviceList = new ArrayList<>();
        this.serviceStateObservableMap = new HashMap<>();
//        this.resourceAllocator = new UnitResourceAllocator<>(this);
        this.clientRemote = new ClientRemote();
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
            this.unitRegistry = Registries.getUnitRegistry();
            this.unitRegistry.waitForData();
            super.init(CachedUnitRegistryRemote.getRegistry().getUnitConfigByScope(scope));
        } catch (CouldNotPerformException ex) {
            throw new InitializationException(this, ex);
        }
    }

    public void init(final String label, final ScopeProvider location) throws InitializationException, InterruptedException {
        try {
            this.unitRegistry = Registries.getUnitRegistry();
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

            this.unitRegistry = Registries.getUnitRegistry();
            super.init(config);
        } catch (CouldNotPerformException ex) {
            throw new InitializationException(this, ex);
        }
    }

    @Override
    protected void postInit() throws InitializationException, InterruptedException {
        try {
            super.postInit();
            this.unitRegistry.addDataObserver((Observable<UnitRegistryData> source, UnitRegistryData data) -> {
                try {
                    final UnitConfig newUnitConfig = CachedUnitRegistryRemote.getRegistry().getUnitConfigById(getId());
                    if (!newUnitConfig.equals(getConfig())) {
                        applyConfigUpdate(newUnitConfig);
                    }
                } catch (CouldNotPerformException ex) {
                    ExceptionPrinter.printHistory("Could not update unit config of " + this, ex, logger);
                }
            });

            // TODO: move to applyConfigUpdate
            try {
                for (ServiceDescription serviceDescription : getTemplate().getServiceDescriptionList()) {
                    if (!serviceStateObservableMap.containsKey(serviceDescription.getType())) {
                        serviceStateObservableMap.put(serviceDescription.getType(), new MessageObservable(this));
                    }
                }
            } catch (NotAvailableException ex) {

            }
            this.addDataObserver(new Observer<D>() {

                @Override
                public void update(Observable<D> source, D data) throws Exception {
                    final Set<ServiceType> serviceTypeMap = new HashSet<>();
                    for (ServiceDescription serviceDescription : getTemplate().getServiceDescriptionList()) {
                        if (!serviceTypeMap.contains(serviceDescription.getType())) {
                            serviceTypeMap.add(serviceDescription.getType());
                            try {
                                Object serviceData = Service.invokeProviderServiceMethod(serviceDescription.getType(), data);
                                serviceStateObservableMap.get(serviceDescription.getType()).notifyObservers(serviceData);
                            } catch (CouldNotPerformException ex) {
                                logger.info("Could not notify state update for service[" + serviceDescription.getType() + "]", ex);
                            }
                        }
                    }
                }
            });
        } catch (CouldNotPerformException ex) {
            throw new InitializationException(this, ex);
        }
    }

    @Override
    public void addServiceStateObserver(ServiceType serviceType, Observer observer) {
        serviceStateObservableMap.get(serviceType).addObserver(observer);
    }

    @Override
    public void removeServiceStateObserver(ServiceType serviceType, Observer observer) {
        serviceStateObservableMap.get(serviceType).removeObserver(observer);
    }

    public boolean isEnabled() {
        try {
            return getConfig().getEnablingState().getValue().equals(EnablingState.State.ENABLED);
        } catch (CouldNotPerformException ex) {
            ExceptionPrinter.printHistory(ex, logger);
        }
        return false;
    }

    public UnitRegistryRemote getUnitRegistry() {
        return unitRegistry;
    }

    @Override
    public UnitConfig applyConfigUpdate(final UnitConfig config) throws CouldNotPerformException, InterruptedException {
        assert config != null;
        unitRegistry.waitForData();
        template = unitRegistry.getUnitTemplateByType(config.getType());
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
    public UnitTemplate.UnitType getType() throws NotAvailableException {
        return getConfig().getType();
    }

    @Override
    public UnitTemplate getTemplate() throws NotAvailableException {
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
        for (ServiceDescription serviceDescription : getTemplate().getServiceDescriptionList()) {
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
    public void applyDataUpdate(ServiceTemplate.ServiceType serviceType, Object serviceArgument) throws CouldNotPerformException {
        try {

            if (serviceArgument == null) {
                throw new NotAvailableException("ServiceArgument");
            }

            final Method updateMethod = getUpdateMethod(serviceType, serviceArgument.getClass());

            try {
                updateMethod.invoke(this, serviceArgument);
            } catch (IllegalAccessException ex) {
                throw new CouldNotPerformException("Cannot access related Method [" + updateMethod.getName() + "]", ex);
            } catch (IllegalArgumentException ex) {
                throw new CouldNotPerformException("Does not match [" + updateMethod.getParameterTypes()[0].getName() + "] which is needed by [" + updateMethod.getName() + "]!", ex);
            } catch (InvocationTargetException ex) {
                throw new CouldNotPerformException("The related method [" + updateMethod.getName() + "] throws an exception during invocation!", ex);
            }
        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not apply " + serviceType.name() + " Update[" + serviceArgument + "] for Unit[" + getLabel() + "]!", ex);
        }
    }

    @Override
    public Method getUpdateMethod(final ServiceTemplate.ServiceType serviceType, Class serviceArgumentClass) throws CouldNotPerformException {
        try {
            Method updateMethod;
            String updateMethodName = ProviderService.getUpdateMethodName(serviceType);
            try {
                updateMethod = getClass().getMethod(updateMethodName, serviceArgumentClass);
                if (updateMethod == null) {
                    throw new NotAvailableException(updateMethod);
                }
            } catch (NoSuchMethodException | SecurityException | NotAvailableException ex) {
                throw new NotAvailableException("Method " + this + "." + updateMethodName + "(" + serviceArgumentClass + ")", ex);
            }
            return updateMethod;
        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Unit not compatible!", ex);
        }
    }

    @Override
    public Future<ActionFuture> applyAction(final ActionDescription actionDescription, boolean test) throws CouldNotPerformException, InterruptedException {
        try {
            logger.debug("applyAction: " + actionDescription.getLabel());
            final Object attribute = serviceJSonProcessor.deserialize(actionDescription.getServiceStateDescription().getServiceAttribute(), actionDescription.getServiceStateDescription().getServiceAttributeType());

            // Since its an action it has to be an operation service pattern
            final ServiceDescription serviceDescription = ServiceDescription.newBuilder().setType(actionDescription.getServiceStateDescription().getServiceType()).setPattern(ServiceTemplate.ServicePattern.OPERATION).build();

            // Set resource allocation interval if not defined yet
            ActionDescription.Builder actionDescriptionBuilder = actionDescription.toBuilder();
            if (!actionDescription.getResourceAllocation().getSlot().hasBegin()) {
                ActionDescriptionProcessor.updateResourceAllocationSlot(actionDescriptionBuilder);
            }
            
            // authenticate and (also authorize?)
            clientRemote.init();
            clientRemote.activate();
            clientRemote.waitForActivation();
            TicketAuthenticatorWrapper wrapper = clientRemote.validateClientServerTicket(actionDescription.getActionAuthority().getTicketAuthenticatorWrapper()).get();
            clientRemote.shutdown();
            ActionFuture.Builder actionFutureBuilder = ActionFuture.newBuilder().setTicketAuthenticatorWrapper(wrapper);
            
            return GlobalCachedExecutorService.submit(() -> {
//                UnitResourceAllocator unitResourceAllocator = new UnitResourceAllocator(actionDescription) {
//
//                    @Override
//                    public Object onAllocation() throws ExecutionException, InterruptedException {
//                        try {
//                            return Service.invokeServiceMethod(serviceTemplate, AbstractUnitController.this, attribute);
//                        } catch (CouldNotPerformException ex) {
//                            throw new ExecutionException(ex);
//                        }
//                    }
//                };

//                unitResourceAllocator.allocate();
//                unitResourceAllocator.waitForExecution();
                // should only be done if completionType is EXPIRE
//                unitResourceAllocator.deallocate();
                //TODO: allocate and if executionTimePeriod = 0 free resource
                Service.invokeServiceMethod(serviceDescription, AbstractUnitController.this, attribute);
//                unitResourceAllocator.w
                return actionFutureBuilder.build();
            });
        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not apply action!", ex);
        }
    }

    @Override
    public String toString() {
        try {
            return getClass().getSimpleName() + "[" + getConfig().getType() + "[" + getLabel() + "]]";
        } catch (NotAvailableException | NullPointerException e) {
            return getClass().getSimpleName() + "[?]";
        }
    }
}
