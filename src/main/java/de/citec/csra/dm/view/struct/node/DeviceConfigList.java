/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.csra.dm.view.struct.node;

import rst.homeautomation.registry.DeviceRegistryType;

/**
 *
 * @author thuxohl
 */
public class DeviceConfigList extends NodeContainer<DeviceRegistryType.DeviceRegistry.Builder> {

    public DeviceConfigList(final DeviceRegistryType.DeviceRegistry.Builder deviceRegistry) {
        super("Device Configurations", deviceRegistry);
        deviceRegistry.getDeviceConfigsList().stream().forEach((deviceConfig) -> {
            super.add(new DeviceConfigContainer(deviceConfig.toBuilder()));
        });
    }
}
