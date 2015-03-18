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
public class DeviceClassContainer extends SendableNode<DeviceClass.Builder> {

    public DeviceClassContainer(final DeviceClass.Builder deviceClass) {
        super("Device Class", deviceClass);
        super.add(deviceClass.getId(), "id");
        super.add(deviceClass.getLabel(), "label");
        super.add(deviceClass.getProductNumber(), "product_number");
        super.add(new BindingConfigContainer(deviceClass.getBindingConfig().toBuilder()));
        super.add(new UnitTypeListContainer(deviceClass));
        super.add(deviceClass.getDescription(), "description");
    }
}
