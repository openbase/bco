/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.csra.dm.view.struct.node;

import de.citec.csra.dm.view.struct.leave.DescriptionContainer;
import de.citec.csra.dm.view.struct.leave.InventoryStateContainer;
import de.citec.csra.dm.view.struct.leave.LabelContainer;
import de.citec.csra.dm.view.struct.leave.ScopeContainer;
import de.citec.csra.dm.view.struct.leave.SerialNumberContainer;
import javafx.scene.control.TreeItem;
import rst.homeautomation.device.DeviceConfigType.DeviceConfig;

/**
 *
 * @author thuxohl
 */
public class DeviceConfigContainer extends TreeItem<Node> implements Node {
    
    private final DeviceConfig deviceConfig;

    public DeviceConfigContainer(DeviceConfig deviceConfig) {
        this.deviceConfig = deviceConfig;
        TreeItem<Node> label = new TreeItem(new LabelContainer(deviceConfig.getLabel()));
        TreeItem<Node> deviceClass = new DeviceClassContainer(deviceConfig.getDeviceClass());
        TreeItem<Node> unitConfig = new UnitConfigListContainer(deviceConfig.getUnitConfigsList());
        TreeItem<Node> serialNumber = new TreeItem(new SerialNumberContainer(deviceConfig.getSerialNumber()));
        TreeItem<Node> inventoryState = new TreeItem(new InventoryStateContainer(deviceConfig.getInventoryState()));
        TreeItem<Node> placement = new PlacementConfigContainer(deviceConfig.getConfig());
        TreeItem<Node> scope = new TreeItem(new ScopeContainer(deviceConfig.getScope()));
        TreeItem<Node> description = new TreeItem(new DescriptionContainer(deviceConfig.getDescription()));
        this.getChildren().addAll(label,unitConfig,serialNumber,inventoryState,placement,scope,description);
    }

    @Override
    public String getDescriptor() {
        return this.deviceConfig.getId();
    }  
}
