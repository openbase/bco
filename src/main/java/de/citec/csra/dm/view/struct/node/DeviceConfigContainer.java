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
public class DeviceConfigContainer extends NodeContainer<DeviceConfig.Builder> {

    public DeviceConfigContainer(DeviceConfig.Builder deviceConfig) {
        super("Device Configuration", deviceConfig);
        super.add(deviceConfig.getId(), "id");
        super.add(deviceConfig.getLabel(), "label");
        super.add(deviceConfig.getSerialNumber(), "serial_number");
        super.add(new PlacementConfigContainer(deviceConfig.getPlacementConfig().toBuilder()));
        super.add(new ScopeContainer(deviceConfig.getScope().toBuilder()));
        super.add(new InventoryStateContainer(deviceConfig.getInventoryState().toBuilder()));
        super.add(new DeviceClassContainer(deviceConfig.getDeviceClass().toBuilder()));
        super.add(new UnitConfigListContainer(deviceConfig));
        super.add(deviceConfig.getDescription(), "description");
    }
}
