/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dc.bco.dal.lib.layer.service;

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

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.dc.bco.dal.lib.layer.service.operation.BrightnessOperationService;
import org.dc.bco.dal.lib.layer.service.operation.ColorOperationService;
import org.dc.bco.dal.lib.layer.service.operation.DimOperationService;
import org.dc.bco.dal.lib.layer.service.operation.OpeningRatioOperationService;
import org.dc.bco.dal.lib.layer.service.operation.PowerOperationService;
import org.dc.bco.dal.lib.layer.service.operation.ShutterOperationService;
import org.dc.bco.dal.lib.layer.service.operation.TargetTemperatureOperationService;
import org.dc.bco.dal.lib.layer.service.provider.BatteryProviderService;
import org.dc.bco.dal.lib.layer.service.provider.ButtonProviderService;
import org.dc.bco.dal.lib.layer.service.provider.HandleProviderService;
import org.dc.bco.dal.lib.layer.service.provider.MotionProviderService;
import org.dc.bco.dal.lib.layer.service.provider.ReedSwitchProviderService;
import org.dc.bco.dal.lib.layer.service.provider.SmokeAlarmStateProviderService;
import org.dc.bco.dal.lib.layer.service.provider.SmokeStateProviderService;
import org.dc.bco.dal.lib.layer.service.provider.TamperProviderService;
import org.dc.bco.dal.lib.layer.service.provider.TemperatureAlarmStateProviderService;
import org.dc.bco.dal.lib.layer.service.provider.TemperatureProviderService;
import org.dc.jps.core.JPService;
import org.dc.jul.exception.CouldNotPerformException;
import org.dc.jul.exception.NotAvailableException;
import org.dc.jul.exception.NotSupportedException;
import org.dc.jul.extension.rsb.iface.RSBLocalServerInterface;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rsb.patterns.Callback;
import rst.homeautomation.service.ServiceTemplateType;

/**
 *
 * @author mpohling
 * @deprecated This enum is mainly outdated. Please use the rst ServiceType instead!
 */
@Deprecated
public enum ServiceType {

    BATTERY(BatteryProviderService.class, ServiceTemplateType.ServiceTemplate.ServiceType.BATTERY_PROVIDER),
    BRIGHTNESS(BrightnessOperationService.class, ServiceTemplateType.ServiceTemplate.ServiceType.BRIGHTNESS_SERVICE),
    BUTTON(ButtonProviderService.class, ServiceTemplateType.ServiceTemplate.ServiceType.BUTTON_PROVIDER),
    COLOR(ColorOperationService.class, ServiceTemplateType.ServiceTemplate.ServiceType.COLOR_SERVICE),
    HANDLE(HandleProviderService.class, ServiceTemplateType.ServiceTemplate.ServiceType.HANDLE_PROVIDER),
    TAMPER(TamperProviderService.class, ServiceTemplateType.ServiceTemplate.ServiceType.TAMPER_PROVIDER),
    TEMPERATURE(TemperatureProviderService.class, ServiceTemplateType.ServiceTemplate.ServiceType.TEMPERATURE_PROVIDER),
    TARGET_TEMPERATURE(TargetTemperatureOperationService.class, ServiceTemplateType.ServiceTemplate.ServiceType.TARGET_TEMPERATURE_SERVICE),
    POWER(PowerOperationService.class, ServiceTemplateType.ServiceTemplate.ServiceType.POWER_SERVICE),
    SHUTTER(ShutterOperationService.class, ServiceTemplateType.ServiceTemplate.ServiceType.SHUTTER_SERVICE),
    OPENING_RATIO(OpeningRatioOperationService.class, ServiceTemplateType.ServiceTemplate.ServiceType.OPENING_RATIO_PROVIDER),
    MOTION(MotionProviderService.class, ServiceTemplateType.ServiceTemplate.ServiceType.MOTION_PROVIDER),
    DIM(DimOperationService.class, ServiceTemplateType.ServiceTemplate.ServiceType.DIM_SERVICE),
    REED_SWITCH(ReedSwitchProviderService.class, ServiceTemplateType.ServiceTemplate.ServiceType.REED_SWITCH_PROVIDER),
    SMOKE_ALARM_STATE(SmokeAlarmStateProviderService.class, ServiceTemplateType.ServiceTemplate.ServiceType.SMOKE_ALARM_STATE_PROVIDER),
    TEMPERATURE_ALARM_STATE(TemperatureAlarmStateProviderService.class, ServiceTemplateType.ServiceTemplate.ServiceType.TEMPERATURE_ALARM_STATE_PROVIDER),
    SMOKE_STATE(SmokeStateProviderService.class, ServiceTemplateType.ServiceTemplate.ServiceType.SMOKE_STATE_PROVIDER);


    public static final String SET = "set";
    public static final String UPDATE = "update";
    public static final String PROVIDER = "Provider";
    public static final String SERVICE = "Service";

    private static final Logger logger = LoggerFactory.getLogger(ServiceType.class);

    private final Class<? extends Service> serviceClass;
    private final Method[] methodDeclarations;
    private final ServiceTemplateType.ServiceTemplate.ServiceType rstType;

    private ServiceType(final Class<? extends Service> serviceClass, ServiceTemplateType.ServiceTemplate.ServiceType rstType) {
        this.serviceClass = serviceClass;
        this.methodDeclarations = detectServiceMethods(serviceClass);
        this.rstType = rstType;
    }

    private Method[] detectServiceMethods(final Class<? extends Service> serviceClass) {
        List<Method> methods = new ArrayList<>();

        // add service methods
        methods.addAll(Arrays.asList(serviceClass.getMethods()));

        // remove internal service methods
        methods.removeAll(Arrays.asList(Service.class.getMethods()));

        serviceClass.getMethods();
        return methods.toArray(new Method[methods.size()]);
    }

    public ServiceTemplateType.ServiceTemplate.ServiceType getRSTType() {
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

    @Deprecated
    public static void registerServiceMethods(final RSBLocalServerInterface server, final Service service) {
//        for (ServiceType serviceType : ServiceType.getServiceTypeList(service)) {
//            for (Method method : serviceType.getDeclaredMethods()) {
//                try {
//                    server.addMethod(method.getName(), getCallback(method, service, serviceType));
//                } catch (CouldNotPerformException ex) {
//                    ExceptionPrinter.printHistory(new CouldNotPerformException("Could not register callback for service methode " + method.toGenericString(), ex),logger);
//                }
//            }
//        }
    }

    private static Callback getCallback(final Method method, final Service service, final ServiceType serviceType) throws CouldNotPerformException {
        String callbackName = method.getName().concat(Callback.class.getSimpleName());
        try {
            Class serviceClass = serviceType.getServiceClass();
            List<Class> interfaces = new ArrayList<>(Arrays.asList(serviceClass.getInterfaces()));
            while (serviceClass != null) {
                for (Class callbackClass : serviceClass.getDeclaredClasses()) {
                    if (callbackClass.getSimpleName().equalsIgnoreCase(callbackName)) {
                        return (Callback) callbackClass.getConstructor(serviceClass).newInstance(service);
                    }
                }
                if(interfaces.isEmpty()) {
                    break;
                }
                serviceClass = interfaces.remove(0);
            }
        } catch (Exception ex) {
            throw new NotAvailableException(callbackName);
        }
        throw new NotSupportedException(service.toString(), service);
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
            throw new NotSupportedException(serviceName, JPService.getApplicationName());
        }
    }
}
