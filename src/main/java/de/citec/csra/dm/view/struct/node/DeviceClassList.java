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
public class DeviceClassList extends NodeContainer<DeviceRegistryType.DeviceRegistry.Builder> {

    public DeviceClassList(final DeviceRegistryType.DeviceRegistry.Builder deviceRegistry) {
        super("Device Classes", deviceRegistry);
        deviceRegistry.getDeviceClassesBuilderList().stream().forEach((deviceClassBuilder) -> {
            super.add(new DeviceClassContainer(deviceClassBuilder));
        });
    }
}
