/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.hal.device;

import de.citec.dm.remote.DeviceRegistryRemote;
import de.citec.jul.exception.CouldNotPerformException;
import de.citec.jul.pattern.Factory;
import rst.homeautomation.device.DeviceClassType.DeviceClass;
import rst.homeautomation.device.DeviceConfigType.DeviceConfig;

/**
 *
 * @author mpohling
 */
public interface DeviceFactory extends Factory<Device, DeviceConfig> {

    public Device newInstance(final DeviceConfig deviceConfig, final DeviceRegistryRemote deviceRegistryRemote) throws CouldNotPerformException;
    
    public Device newInstance(final DeviceConfig deviceConfig, final DeviceClass deviceClass) throws CouldNotPerformException;

}
