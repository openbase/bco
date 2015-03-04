/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.csra.dm.view;

import javafx.scene.control.TreeItem;
import rst.homeautomation.device.DeviceClassType.DeviceClass;
import de.citec.csra.dm.view.struct.node.DeviceClassContainer;
import de.citec.csra.dm.view.struct.node.Node;
import java.util.Collection;

/**
 *
 * @author thuxohl
 */
public class DeviceClassTree extends TreeItem<Node> {

    public DeviceClassTree(final Collection<DeviceClass> deviceClasses) {
        deviceClasses.stream().forEach((deviceClass) -> {
            this.getChildren().add(new DeviceClassContainer(deviceClass));
        });
    }
}
