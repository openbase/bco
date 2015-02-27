/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.csra.dm.view;

import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableView;
import rst.homeautomation.device.DeviceClassType.DeviceClass;
import rst.homeautomation.registry.DeviceRegistryType.DeviceRegistry;

/**
 *
 * @author thuxohl
 */
public class DeviceClassTreeView extends TreeTableView {

    public DeviceClassTreeView(DeviceRegistry registry) {
        TreeItem<String> root = new TreeItem<>("Devices");
        for(DeviceClass deviceClass : registry.getDeviceClassesList()) {
            TreeItem<DeviceClass> device = new TreeItem(deviceClass);
        }
        
        TreeTableColumn column = new TreeTableColumn("DeviceID");
        column.setPrefWidth(80);
    }
    
}
