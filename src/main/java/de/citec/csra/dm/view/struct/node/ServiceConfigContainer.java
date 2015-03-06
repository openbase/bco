/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.csra.dm.view.struct.node;

import de.citec.csra.dm.view.struct.leave.BindingConfigContainer;
import javafx.scene.control.TreeItem;
import rst.homeautomation.service.ServiceConfigType;

/**
 *
 * @author thuxohl
 */
public class ServiceConfigContainer extends TreeItem<Node> implements Node {
    
    private ServiceConfigType.ServiceConfig serviceConfig;

    public ServiceConfigContainer(ServiceConfigType.ServiceConfig serviceConfig) {
        this.serviceConfig = serviceConfig;
        TreeItem<Node> bindingType = new TreeItem<>(new BindingConfigContainer(serviceConfig.getBindingConfig()));
        //TODO
        this.getChildren().addAll(bindingType);
    }

    @Override
    public String getDescriptor() {
        return "Service Config";
    } 
}
