/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.hal.service;

import de.citec.dal.bindings.openhab.service.OpenHABService;
import de.citec.dal.hal.provider.EnergyProvider;
import de.citec.dal.hal.provider.ButtonProvider;
import de.citec.dal.hal.provider.HandleProvider;
import de.citec.dal.hal.provider.MotionProvider;
import de.citec.dal.hal.provider.ReedSwitchProvider;
import de.citec.dal.hal.provider.TamperProvider;
import de.citec.dal.hal.provider.TemperatureProvider;
import de.citec.jul.exception.CouldNotPerformException;
import de.citec.jul.exception.NotAvailableException;
import de.citec.jul.exception.NotSupportedException;
import de.citec.jul.extension.rsb.iface.RSBLocalServerInterface;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rsb.patterns.Callback;
import rst.homeautomation.service.ServiceTypeHolderType;

/**
 *
 * @author mpohling
 */
public enum ServiceType {

    MULTI(MultiService.class, ServiceTypeHolderType.ServiceTypeHolder.ServiceType.UNKNOWN),
    BATTERY(EnergyProvider.class, ServiceTypeHolderType.ServiceTypeHolder.ServiceType.BATTERY_PROVIDER),
    BRIGHTNESS(BrightnessService.class, ServiceTypeHolderType.ServiceTypeHolder.ServiceType.BRIGHTNESS_SERVICE),
    BUTTON(ButtonProvider.class, ServiceTypeHolderType.ServiceTypeHolder.ServiceType.BUTTON_PROVIDER),
    COLOR(ColorService.class, ServiceTypeHolderType.ServiceTypeHolder.ServiceType.COLOR_SERVICE),
    HANDLE(HandleProvider.class, ServiceTypeHolderType.ServiceTypeHolder.ServiceType.HANDLE_PROVIDER),
    TAMPER(TamperProvider.class, ServiceTypeHolderType.ServiceTypeHolder.ServiceType.TAMPER_PROVIDER),
    TEMPERATURE(TemperatureProvider.class, ServiceTypeHolderType.ServiceTypeHolder.ServiceType.TEMPERATURE_PROVIDER),
    POWER(PowerService.class, ServiceTypeHolderType.ServiceTypeHolder.ServiceType.POWER_SERVICE),
    SHUTTER(ShutterService.class, ServiceTypeHolderType.ServiceTypeHolder.ServiceType.SHUTTER_SERVICE),
    OPENING_RATIO(OpeningRatioService.class, ServiceTypeHolderType.ServiceTypeHolder.ServiceType.OPENING_RATIO_PROVIDER),
    MOTION(MotionProvider.class, ServiceTypeHolderType.ServiceTypeHolder.ServiceType.MOTION_PROVIDER),
    DIM(DimService.class, ServiceTypeHolderType.ServiceTypeHolder.ServiceType.DIM_SERVICE),
    REED_SWITCH(ReedSwitchProvider.class, ServiceTypeHolderType.ServiceTypeHolder.ServiceType.REED_SWITCH_PROVIDER);

    public static final String SET = "set";
    public static final String UPDATE = "update";
    public static final String PROVIDER = "Provider";
    public static final String SERVICE = "Service";

    private static final Logger logger = LoggerFactory.getLogger(ServiceType.class);

    private final Class<? extends Service> serviceClass;
    private final Method[] methodDeclarations;
    private final ServiceTypeHolderType.ServiceTypeHolder.ServiceType rstType;

    private ServiceType(final Class<? extends Service> serviceClass, ServiceTypeHolderType.ServiceTypeHolder.ServiceType rstType) {
        this.serviceClass = serviceClass;
        this.methodDeclarations = serviceClass.getDeclaredMethods();
        this.rstType = rstType;
    }

    public ServiceTypeHolderType.ServiceTypeHolder.ServiceType getRSTType() {
        return rstType;
    }
    
    public Class<? extends Service> getServiceClass() {
        return serviceClass;
    }

    public List<String> getUpdateMethods() {
        final List<String> updateMethodList = new ArrayList<>();
        for (Method method : getDeclaredMethods()) {
            if (!method.getName().startsWith(SET)) {
                continue;
            }
            updateMethodList.add(UPDATE + method.getName().replaceFirst(SET, ""));
        }
        return updateMethodList;
    }

    public Method[] getDeclaredMethods() {
        return methodDeclarations;
    }

    public String getServiceName() {
        return getServiceClass().getSimpleName().replace(PROVIDER, "").replace(SERVICE, "");
    }

    public static List<ServiceType> getServiceTypeList(final Service service) {
        List<ServiceType> serviceTypeList = new ArrayList<>();
        for (ServiceType serviceType : values()) {
            if (serviceType.getServiceClass().isAssignableFrom(service.getClass())) {
                serviceTypeList.add(serviceType);
            }
        }
        return serviceTypeList;
    }

    public static List<Class<? extends Service>> getServiceClassList(final Service service) {
        List<Class<? extends Service>> serviceClassList = new ArrayList<>();
        for (ServiceType serviceType : getServiceTypeList(service)) {
            serviceClassList.add(serviceType.getServiceClass());
        }
        return serviceClassList;
    }

    public static void registerServiceMethods(final RSBLocalServerInterface server, final Service service) {
        for (ServiceType serviceType : ServiceType.getServiceTypeList(service)) {
            for (Method method : serviceType.getDeclaredMethods()) {
                try {
                    server.addMethod(method.getName(), getCallback(method, service, serviceType));
                } catch (CouldNotPerformException ex) {
                    logger.warn("Could not register callback for service methode " + method.toGenericString(), ex);
                }
            }
        }
    }

    private static Callback getCallback(final Method method, final Service service, final ServiceType serviceType) throws CouldNotPerformException {
        String callbackName = method.getName().concat(Callback.class.getSimpleName());
        try {
            for (Class callbackClass : serviceType.getServiceClass().getDeclaredClasses()) {
                if (callbackClass.getSimpleName().equalsIgnoreCase(callbackName)) {
                    return (Callback) callbackClass.getConstructor(serviceType.getServiceClass()).newInstance(service);
                }
            }
        } catch (Exception ex) {
            throw new NotAvailableException(callbackName);
        }
        throw new NotSupportedException(callbackName, service);
    }

    public static ServiceType valueOfByServiceName(String serviceName) throws NotSupportedException {
        try {
            for (ServiceType serviceType : ServiceType.values()) {
                if (serviceType.getServiceName().equalsIgnoreCase(serviceName)) {
                    return serviceType;
                }
            }
            throw new NotAvailableException(serviceName);
        } catch (IllegalArgumentException | CouldNotPerformException ex) {
            throw new NotSupportedException(serviceName, OpenHABService.class.getSimpleName());
        }
    }
}
