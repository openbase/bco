/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.csra.dm.view.struct.node;

import rst.homeautomation.device.DeviceClassType.DeviceClass;
import java.util.Collection;

/**
 *
 * @author thuxohl
 */
public class DeviceClassList extends NodeContainer<Collection<DeviceClass>> {

    public DeviceClassList(final Collection<DeviceClass> deviceClasses) {
        super("Device Classes", deviceClasses);
        deviceClasses.stream().forEach((deviceClass) -> {
            super.add(new DeviceClassContainer(deviceClass));
        });
    }
}
