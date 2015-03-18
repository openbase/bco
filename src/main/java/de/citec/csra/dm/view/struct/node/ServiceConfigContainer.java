/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.csra.dm.view.struct.node;

import rst.homeautomation.service.ServiceConfigType.ServiceConfig;

/**
 *
 * @author thuxohl
 */
public class ServiceConfigContainer extends NodeContainer<ServiceConfig.Builder> {
    
    public ServiceConfigContainer(ServiceConfig.Builder serviceConfig) {
        super("Service Configuration", serviceConfig);
        super.add(new BindingConfigContainer(serviceConfig.getBindingConfigBuilder()));
        super.add(new OpenhabServiceConfigContainer(serviceConfig.getOpenhabServiceConfigBuilder()));
        super.add(new MieleAtHomeServiceConfigContainer(serviceConfig.getMieleAtHomeServiceConfigBuilder()));
        super.add(new HandlesServiceConfigContainer(serviceConfig.getHandlesServiceConfigBuilder()));
        super.add(new SinactServiceConfigContainer(serviceConfig.getSinactServiceConfigBuilder()));
    }
}
