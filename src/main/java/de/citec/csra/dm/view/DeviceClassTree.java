/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.csra.dm.view;

import javafx.scene.control.TreeItem;
import rst.homeautomation.device.DeviceClassType.DeviceClass;
import rst.homeautomation.registry.DeviceRegistryType.DeviceRegistry;
import de.citec.csra.dm.view.struct.node.DeviceClassContainer;
import de.citec.csra.dm.view.struct.node.Node;

/**
 *
 * @author thuxohl
 */
public class DeviceClassTree extends TreeItem<Node> {

    public DeviceClassTree(DeviceRegistry registry) {
        for (DeviceClass deviceClass : registry.getDeviceClassesList()) {
            this.getChildren().add(new DeviceClassContainer(deviceClass));
        }
    }
}
