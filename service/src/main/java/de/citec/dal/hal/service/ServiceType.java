/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.hal.service;

import de.citec.dal.bindings.openhab.service.OpenHABService;
import de.citec.jul.exception.NotSupportedException;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author mpohling
 */
public enum ServiceType {

    BRIGHTNESS(BrightnessService.class),
    COLOR(ColorService.class),
    POWER(PowerService.class);

    private final Class serviceClass;
    private final String[] methodDeclarations;

    private ServiceType(final Class serviceClass) {
        this.serviceClass = serviceClass;
    }

    public Class getServiceClass() {
        return serviceClass;
    }
    public String[] getMethodDeclarations() {
        
        return methodDeclarations;
    }
    
    public static List<ServiceType> getServiceTypeList(final Service service) {
        List<ServiceType> serviceTypeList = new ArrayList<>();
        for(ServiceType serviceType : values()) {
            if(service.getClass().isAssignableFrom(serviceType.getServiceClass())) {
                serviceTypeList.add(serviceType);
            }
        }
        return serviceTypeList;
    }
    
    public static List<Class<? extends Service>> getServiceClassList(final Service service) {
        List<Class<? extends Service>> serviceClassList = new ArrayList<>();
        for(ServiceType serviceType : getServiceTypeList(service)) {
            serviceClassList.add(serviceType.serviceClass);
        }
        return serviceClassList;
    }

    public static ServiceType valueOfByServiceName(String serviceName) throws NotSupportedException {
        try {
            return valueOf(serviceName.toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new NotSupportedException(serviceName, OpenHABService.class.getSimpleName());
        }
    }
}
