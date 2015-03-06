/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.csra.dm.view;

import de.citec.csra.dm.view.struct.node.DeviceConfigContainer;
import de.citec.csra.dm.view.struct.node.Node;
import java.util.Collection;
import javafx.scene.control.TreeItem;
import rst.homeautomation.device.DeviceConfigType.DeviceConfig;

/**
 *
 * @author thuxohl
 */
public class DeviceConfigTree extends TreeItem<Node> {

    public DeviceConfigTree(final Collection<DeviceConfig> deviceConfigs) {
        deviceConfigs.stream().forEach((deviceConfig) -> {
            this.getChildren().add(new DeviceConfigContainer(deviceConfig));
        });
    }
}
