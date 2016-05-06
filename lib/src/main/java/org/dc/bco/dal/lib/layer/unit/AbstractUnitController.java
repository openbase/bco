package org.dc.bco.dal.lib.layer.unit;

/*
 * #%L
 * DAL Library
 * %%
 * Copyright (C) 2014 - 2016 DivineCooperation
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
import org.dc.bco.dal.lib.layer.service.Service;
import org.dc.bco.dal.lib.layer.service.ServiceFactory;
import org.dc.bco.dal.lib.layer.service.ServiceFactoryProvider;
import org.dc.bco.dal.lib.layer.service.ServiceType;
import org.dc.bco.dal.lib.layer.service.consumer.ConsumerService;
import org.dc.bco.dal.lib.layer.service.operation.OperationService;
import org.dc.bco.dal.lib.layer.service.provider.ProviderService;
import org.dc.bco.registry.device.remote.CachedDeviceRegistryRemote;
import org.dc.jul.exception.CouldNotPerformException;
import org.dc.jul.exception.InitializationException;
import org.dc.jul.exception.InstantiationException;
import org.dc.jul.exception.InvalidStateException;
import org.dc.jul.exception.MultiException;
import org.dc.jul.exception.NotAvailableException;
import org.dc.jul.exception.NotSupportedException;
import org.dc.jul.exception.printer.ExceptionPrinter;
import org.dc.jul.extension.rsb.com.AbstractConfigurableController;
import org.dc.jul.extension.rsb.com.RPCHelper;
import org.dc.jul.extension.rsb.iface.RSBLocalServerInterface;
import org.dc.jul.extension.rsb.scope.ScopeGenerator;
import org.dc.jul.extension.rsb.scope.ScopeTransformer;
import org.dc.jul.extension.rst.iface.ScopeProvider;
import org.dc.jul.processing.StringProcessor;
import rsb.Scope;
import rst.homeautomation.service.ServiceTemplateType.ServiceTemplate;
import rst.homeautomation.unit.UnitConfigType.UnitConfig;
import rst.homeautomation.unit.UnitTemplateType.UnitTemplate;
import rst.rsb.ScopeType;

/**
 *
 * @author mpohling
 * @param <M> Underling message type.
 * @param <MB> Message related builder.
 */
public abstract class AbstractUnitController<M extends GeneratedMessage, MB extends M.Builder<MB>> extends AbstractConfigurableController<M, MB, UnitConfig> implements UnitController, ServiceFactoryProvider {

    private final UnitHost unitHost;
    private final List<Service> serviceList;
    private final ServiceFactory serviceFactory;
    private UnitTemplate unitTemplate;

    public AbstractUnitController(final Class unitClass, final UnitHost unitHost, final MB builder) throws CouldNotPerformException {
        super(builder);
        try {

            if (unitHost.getServiceFactory() == null) {
                throw new NotAvailableException("service factory");
            }

            this.unitHost = unitHost;
            this.serviceFactory = unitHost.getServiceFactory();
            this.serviceList = new ArrayList<>();

            try {
                validateUpdateServices();
            } catch (MultiException ex) {
                logger.error(this + " is not valid!", ex);
                ex.printExceptionStack();
            }

        } catch (CouldNotPerformException ex) {
            throw new InstantiationException(this, ex);
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
    public UnitConfig applyConfigUpdate(final UnitConfig config) throws CouldNotPerformException, InterruptedException {
        unitTemplate = CachedDeviceRegistryRemote.getDeviceRegistry().getUnitTemplateByType(config.getType());
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
    public UnitTemplate getTemplate() throws NotAvailableException {
        if (unitTemplate == null) {
            throw new NotAvailableException("unit template");
        }
        return unitTemplate;
    }

    @Override
    public void registerMethods(RSBLocalServerInterface server) throws CouldNotPerformException {

        // collect service interface methods
        HashMap<String, ServiceTemplate.ServiceType> methodMap = new HashMap<>();
        for (ServiceTemplate.ServiceType serviceType : getTemplate().getServiceTypeList()) {
            methodMap.put(StringProcessor.transformUpperCaseToCamelCase(serviceType.name()), serviceType);
        }

        for (Entry<String, ServiceTemplate.ServiceType> methodEntry : methodMap.entrySet()) {
            Class<? extends Service> serviceInterfaceClass = null;

            try {
                // Identify package
                Package servicePackage;
                if (methodEntry.getKey().contains(Service.CONSUMER_SERVICE_LABEL)) {
                    servicePackage = ConsumerService.class.getPackage();
                } else if (methodEntry.getKey().contains(Service.OPERATION_SERVICE_LABEL)) {
                    servicePackage = OperationService.class.getPackage();
                } else if (methodEntry.getKey().contains(Service.PROVIDER_SERVICE_LABEL)) {
                    servicePackage = ProviderService.class.getPackage();
                } else {
                    throw new NotSupportedException(methodEntry.getValue() + " is not supported!", this);
                }

                // Identify interface class
                try {
                    serviceInterfaceClass = (Class<? extends Service>) Class.forName(servicePackage.getName() + "." + methodEntry.getKey());
                } catch (ClassNotFoundException | ClassCastException ex) {
                    throw new CouldNotPerformException("Could not load service interface!", ex);
                }

                if (!serviceInterfaceClass.isAssignableFrom(this.getClass())) {
                    // interface not supported dummy.
                }

                Class<? extends Service> asSubclass = getClass().asSubclass(serviceInterfaceClass);

//                RPCHelper.registerServiceInterface(serviceInterfaceClass, this, server);
                RPCHelper.registerInterface((Class) serviceInterfaceClass, this, server);
            } catch (CouldNotPerformException ex) {
                ExceptionPrinter.printHistory(new CouldNotPerformException("Could not register Interface[" + serviceInterfaceClass + "] Methode [" + methodEntry.getKey() + "] for Unit[" + this.getLabel() + "].", ex), logger);
            }
        }
//        for (ServiceType serviceType : ServiceType.getServiceTypeList(this)) {
//            for (Method method : serviceType.getDeclaredMethods()) {
//                try {
//                    server.addMethod(method.getName(), getCallback(method, this, serviceType));
//                } catch (CouldNotPerformException ex) {
//                    ExceptionPrinter.printHistory(new CouldNotPerformException("Could not register callback for service methode " + method.toGenericString(), ex), logger);
//                }
//            }
//        }
    }

    @Override
    public ServiceFactory getServiceFactory() throws NotAvailableException {
        return serviceFactory;
    }

    @Override
    public void applyUpdate(ServiceTemplate.ServiceType serviceType, Object serviceArgument) throws CouldNotPerformException {
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
            String updateMethodName = Service.UPDATE_METHOD_PREFIX + Service.getServiceBaseName(serviceType);
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
     * *
     *
     * @throws MultiException
     */
    private void validateUpdateServices() throws MultiException {
        logger.debug("Validating unit update methods...");

        MultiException.ExceptionStack exceptionStack = null;
        List<String> unitMethods = new ArrayList<>();

        // === Transform unit methods into string list. ===
        for (Method medhod : getClass().getMethods()) {
            unitMethods.add(medhod.getName());
        }

        // === Validate if all update methods are registrated. ===
        for (ServiceType service : ServiceType.getServiceTypeList(this)) {
            for (String serviceMethod : service.getUpdateMethods()) {
                if (!unitMethods.contains(serviceMethod)) {
                    exceptionStack = MultiException.push(service, null, exceptionStack);
                }
            }
        }

        // === throw multi exception in error case. ===
        MultiException.checkAndThrow("Update service not valid!", exceptionStack);
    }

    @Override
    public String toString() {
        try {
            return getClass().getSimpleName() + "[" + getConfig().getType() + "[" + getConfig().getLabel() + "]]";
        } catch (NotAvailableException e) {
            return getClass().getSimpleName() + "[?]";
        }
    }
}
