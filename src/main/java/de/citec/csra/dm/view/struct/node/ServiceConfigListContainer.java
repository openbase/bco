/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.csra.dm.view.struct.node;

import java.util.Collection;
import rst.homeautomation.service.ServiceConfigType;

/**
 *
 * @author thuxohl
 */
public class ServiceConfigListContainer extends NodeContainer<Collection<ServiceConfigType.ServiceConfig>> {

    public ServiceConfigListContainer(final Collection<ServiceConfigType.ServiceConfig> serviceConfigs) {
        super("Service Configurations", serviceConfigs);
        serviceConfigs.stream().forEach((serviceConfig) -> {
            super.add(new ServiceConfigContainer(serviceConfig));
        });
    }
}
