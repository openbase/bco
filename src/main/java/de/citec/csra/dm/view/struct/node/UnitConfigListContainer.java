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
        deviceConfig.getUnitConfigsList().stream().forEach((unitConfig) -> {
            super.add(new UnitConfigContainer(unitConfig.toBuilder()));
        });
    }
}
