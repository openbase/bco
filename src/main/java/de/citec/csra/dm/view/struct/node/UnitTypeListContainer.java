/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.csra.dm.view.struct.node;

import rst.homeautomation.device.DeviceClassType;

/**
 *
 * @author thuxohl
 */
public class UnitTypeListContainer extends NodeContainer<DeviceClassType.DeviceClass.Builder> {

    public UnitTypeListContainer(final DeviceClassType.DeviceClass.Builder deviceClass) {
        super("Unit Types", deviceClass);
        deviceClass.getUnitsList().stream().forEach((unitType) -> {
            super.add(new UnitTypeContainer(unitType.toBuilder()));
        });
    }
}
