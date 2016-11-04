package org.openbase.bco.dal.lib.layer.unit;

/*
 * #%L
 * DAL Library
 * %%
 * Copyright (C) 2014 - 2016 openbase.org
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
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import org.openbase.bco.dal.lib.layer.service.Service;
import org.openbase.bco.dal.lib.layer.service.ServiceFactory;
import org.openbase.bco.dal.lib.layer.service.ServiceFactoryProvider;
import org.openbase.bco.dal.lib.layer.service.ServiceJSonProcessor;
import org.openbase.bco.dal.lib.layer.service.consumer.ConsumerService;
import org.openbase.bco.dal.lib.layer.service.operation.OperationService;
import org.openbase.bco.dal.lib.layer.service.provider.ProviderService;
import org.openbase.bco.registry.device.remote.CachedDeviceRegistryRemote;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InitializationException;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.exception.InvalidStateException;
import org.openbase.jul.exception.MultiException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.exception.NotSupportedException;
import org.openbase.jul.exception.VerificationFailedException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.extension.rsb.com.AbstractConfigurableController;
import org.openbase.jul.extension.rsb.com.RPCHelper;
import org.openbase.jul.extension.rsb.scope.ScopeGenerator;
import org.openbase.jul.extension.rsb.scope.ScopeTransformer;
import org.openbase.jul.extension.rst.iface.ScopeProvider;
import org.openbase.jul.processing.StringProcessor;
import org.openbase.jul.schedule.Stopwatch;
import rsb.Scope;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rst.domotic.action.ActionConfigType;
import rst.domotic.action.ActionConfigType.ActionConfig;
import rst.domotic.service.ServiceTemplateType.ServiceTemplate;
import rst.domotic.unit.UnitConfigType.UnitConfig;
import rst.domotic.unit.UnitTemplateType.UnitTemplate;
import rst.rsb.ScopeType;
import org.openbase.jul.extension.rsb.iface.RSBLocalServer;

/**
 *
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 * @param <M> Underling message type.
 * @param <MB> Message related builder.
 */
public abstract class AbstractUnitController<M extends GeneratedMessage, MB extends M.Builder<MB>> extends AbstractConfigurableController<M, MB, UnitConfig> implements UnitController, ServiceFactoryProvider {

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(ActionConfig.getDefaultInstance()));
    }

    public static long registerMethodTime = 0;
    public static long updateMethodVerificationTime = 0;
    public static long initTime = 0;
    public static long constructorTime = 0;

    private final UnitHost unitHost;
    private final List<Service> serviceList;
    private final ServiceFactory serviceFactory;
    private UnitTemplate template;
    private final ServiceJSonProcessor serviceJSonProcessor;
    private final Stopwatch stopWatch = new Stopwatch();

    public AbstractUnitController(final Class unitClass, final UnitHost unitHost, final MB builder) throws CouldNotPerformException {
        super(builder);
        stopWatch.start();
        try {

            if (unitHost.getServiceFactory() == null) {
                throw new NotAvailableException("service factory");
            }
            this.serviceJSonProcessor = new ServiceJSonProcessor();
            this.unitHost = unitHost;
            this.serviceFactory = unitHost.getServiceFactory();
            this.serviceList = new ArrayList<>();

        } catch (CouldNotPerformException ex) {
            throw new InstantiationException(this, ex);
        }
        try {
            synchronized (this) {
                constructorTime += stopWatch.stop();
            }
        } catch (CouldNotPerformException ex) {

        }
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
            super.init(unitHost.getDeviceRegistry().getUnitConfigByScope(scope));
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
        Stopwatch stopwatchTmp = new Stopwatch();
        stopwatchTmp.start();
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
            try {
                verifyUnitConfig();
            } catch (VerificationFailedException ex) {
                ExceptionPrinter.printHistory(new InvalidStateException(this + " is not valid!", ex), logger);
            }
        } catch (CouldNotPerformException ex) {
            throw new InitializationException(this, ex);
        }

        try {
            synchronized (this) {
                initTime += stopwatchTmp.stop();
            }
        } catch (CouldNotPerformException ex) {

        }
    }

    @Override
    public UnitConfig applyConfigUpdate(final UnitConfig config) throws CouldNotPerformException, InterruptedException {
        template = CachedDeviceRegistryRemote.getRegistry().getUnitTemplateByType(config.getType());
        CachedDeviceRegistryRemote.waitForData();
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

    public UnitHost getUnitHost() {
        return unitHost;
    }

    public Collection<Service> getServices() {
        return Collections.unmodifiableList(serviceList);
    }

    public void registerService(final Service service) {
        serviceList.add(service);
    }

    @Override
    public void registerMethods(RSBLocalServer server) throws CouldNotPerformException {
        stopWatch.start();
        // collect service interface methods
        HashMap<String, ServiceTemplate> serviceInterfaceMap = new HashMap<>();
        for (ServiceTemplate serviceTemplate : getTemplate().getServiceTemplateList()) {
            serviceInterfaceMap.put(StringProcessor.transformUpperCaseToCamelCase(serviceTemplate.getType().name())
                    + StringProcessor.transformUpperCaseToCamelCase(serviceTemplate.getPattern().name()), serviceTemplate);
        }

        Class<? extends Service> serviceInterfaceClass = null;
        Package servicePackage;
        for (Entry<String, ServiceTemplate> serviceInterfaceMapEntry : serviceInterfaceMap.entrySet()) {
            try {
                // Identify package
                if (serviceInterfaceMapEntry.getValue().getPattern() == ServiceTemplate.ServicePattern.CONSUMER) {
                    servicePackage = ConsumerService.class.getPackage();
                } else if (serviceInterfaceMapEntry.getValue().getPattern() == ServiceTemplate.ServicePattern.OPERATION) {
                    servicePackage = OperationService.class.getPackage();
                } else if (serviceInterfaceMapEntry.getValue().getPattern() == ServiceTemplate.ServicePattern.PROVIDER) {
                    servicePackage = ProviderService.class.getPackage();
                } else {
                    throw new NotSupportedException(serviceInterfaceMapEntry.getKey(), this);
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

        synchronized (this) {
            registerMethodTime += stopWatch.stop();
        }
    }

    @Override
    public ServiceFactory getServiceFactory() throws NotAvailableException {
        return serviceFactory;
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
                throw new CouldNotPerformException("The related method [" + updateMethod.getName() + "] throws an exceptioin during invocation!", ex);
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

    /**
     * Verify if all provider service update methods are registered for given
     * configuration.
     *
     * @throws VerificationFailedException is thrown if the check fails or at
     * least on update method is not available.
     */
    private void verifyUnitConfig() throws VerificationFailedException {
        stopWatch.start();

        try {
            logger.debug("Validating unit update methods...");

            MultiException.ExceptionStack exceptionStack = null;
            List<String> unitMethods = new ArrayList<>();
            String updateMethod;

            // === Load unit methods. ===
            for (Method medhod : getClass().getMethods()) {
                unitMethods.add(medhod.getName());
            }

            // === Verify if all update methods are registered. ===
            for (ServiceTemplate serviceTemplate : getTemplate().getServiceTemplateList()) {

                // filter other services than provider
                if (serviceTemplate.getPattern() != ServiceTemplate.ServicePattern.PROVIDER) {
                    continue;
                }

                // verify
                updateMethod = ProviderService.getUpdateMethodName(serviceTemplate.getType());
                if (!unitMethods.contains(updateMethod)) {
                    exceptionStack = MultiException.push(serviceTemplate, new NotAvailableException("Method", updateMethod), exceptionStack);
                }
            }

            // === throw multi exception in error case. ===
            MultiException.checkAndThrow("At least one update method missing!", exceptionStack);
        } catch (CouldNotPerformException ex) {
            throw new VerificationFailedException("UnitTemplate is not compatible for configured unit controller!", ex);
        }

        try {
            synchronized (this) {
                updateMethodVerificationTime += stopWatch.stop();
            }
        } catch (CouldNotPerformException ex) {
            logger.error("Coul not stop StopWatch");
        }
    }

    @Override
    public Future<Void> applyAction(final ActionConfigType.ActionConfig actionConfig) throws CouldNotPerformException, InterruptedException {
        try {
            logger.info("applyAction: " + actionConfig.getLabel());
            Object attribute = serviceJSonProcessor.deserialize(actionConfig.getServiceAttribute(), actionConfig.getServiceAttributeType());
            // Since its an action it has to be an operation service pattern
            ServiceTemplate serviceTemplate = ServiceTemplate.newBuilder().setType(actionConfig.getServiceType()).setPattern(ServiceTemplate.ServicePattern.OPERATION).build();
            Service.invokeServiceMethod(serviceTemplate, this, attribute);
            return CompletableFuture.completedFuture(null); // TODO Should be asynchron!
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
