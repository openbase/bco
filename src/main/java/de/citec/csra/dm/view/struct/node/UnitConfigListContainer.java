/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.csra.dm.view.struct.node;

import rst.homeautomation.device.DeviceConfigType;

/**
 *
 * @author thuxohl
 */
public class UnitConfigListContainer extends NodeContainer<DeviceConfigType.DeviceConfig.Builder> {

    public UnitConfigListContainer(final DeviceConfigType.DeviceConfig.Builder deviceConfig) {
        super("Unit Configurations", deviceConfig);
        deviceConfig.getUnitConfigsBuilderList().stream().forEach((unitConfigBuilder) -> {
            super.add(new UnitConfigContainer(unitConfigBuilder));
        });
        if (deviceConfig.getUnitConfigsBuilderList().isEmpty()) {
            super.add(new UnitConfigContainer(deviceConfig.addUnitConfigsBuilder()));
        }
    }
}
