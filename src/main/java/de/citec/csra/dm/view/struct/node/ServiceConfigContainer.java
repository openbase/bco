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
public class ServiceConfigContainer extends NodeContainer<ServiceConfig> {
    
    public ServiceConfigContainer(ServiceConfig serviceConfig) {
        super("Service Configuration", serviceConfig);
        super.add(new BindingConfigContainer(serviceConfig.getBindingConfig()));
        super.add(new OpenhabServiceConfigContainer(serviceConfig.getOpenhabServiceConfig()));
        super.add(new MieleAtHomeServiceConfigContainer(serviceConfig.getMieleAtHomeServiceConfig()));
        super.add(new HandlesServiceConfigContainer(serviceConfig.getHandlesServiceConfig()));
        super.add(new SinactServiceConfigContainer(serviceConfig.getSinactServiceConfig()));
    }
}
