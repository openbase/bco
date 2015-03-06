/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.csra.dm.view.struct.node;

import java.util.Collection;
import javafx.scene.control.TreeItem;
import rst.homeautomation.service.ServiceConfigType;

/**
 *
 * @author thuxohl
 */
public class ServiceConfigListContainer extends TreeItem<Node> implements Node {
    
    public ServiceConfigListContainer(final Collection<ServiceConfigType.ServiceConfig> serviceConfigs) {
        serviceConfigs.stream().forEach((serviceConfig) -> {
            this.getChildren().add(new ServiceConfigContainer(serviceConfig));
        });
    }

    @Override
    public String getDescriptor() {
        return "Service Configs";
    }
}
