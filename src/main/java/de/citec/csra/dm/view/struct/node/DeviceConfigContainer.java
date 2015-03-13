/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.csra.dm.view.struct.node;

import rst.homeautomation.device.DeviceConfigType.DeviceConfig;

/**
 *
 * @author thuxohl
 */
public class DeviceConfigContainer extends NodeContainer<DeviceConfig> {

    public DeviceConfigContainer(DeviceConfig deviceConfig) {
        super("Device Configuration", deviceConfig);
        super.add(deviceConfig.getId(), "ID");
        super.add(deviceConfig.getLabel(), "Label");
        super.add(deviceConfig.getSerialNumber(), "Serial Number");
        super.add(new PlacementConfigContainer(deviceConfig.getPlacementConfig()));
        super.add(new ScopeContainer(deviceConfig.getScope()));
        super.add(new InventoryStateContainer(deviceConfig.getInventoryState()));
        super.add(new DeviceClassContainer(deviceConfig.getDeviceClass()));
        super.add(new UnitConfigListContainer(deviceConfig.getUnitConfigsList()));
        super.add(deviceConfig.getDescription(), "Description");
    }
}
