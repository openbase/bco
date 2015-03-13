/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.csra.dm.view.struct.node;

import rst.homeautomation.device.DeviceClassType.DeviceClass;

/**
 *
 * @author thuxohl
 */
public class DeviceClassContainer extends NodeContainer<DeviceClass> {

    public DeviceClassContainer(final DeviceClass deviceClass) {
        super(deviceClass.getId(), deviceClass);
//        super.add(deviceClass.getLabel(), "ID");
        super.add(deviceClass.getLabel(), "Label");
        super.add(deviceClass.getProductNumber(), "Product Number");
        super.add(new BindingConfigContainer(deviceClass.getBindingConfig()));
        super.add(new UnitTypeListContainer(deviceClass.getUnitsList()));
        super.add(deviceClass.getDescription(), "Description");
    }
}
