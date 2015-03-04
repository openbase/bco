/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.csra.dm.registry;

import de.citec.jul.exception.CouldNotPerformException;
import rst.homeautomation.device.DeviceClassType;
import rst.homeautomation.device.DeviceConfigType;

/**
 *
 * @author mpohling
 */
public interface DeviceRegistryInterface {

    public void registerDeviceConfig(DeviceConfigType.DeviceConfig deviceConfig) throws CouldNotPerformException;

    public void updateDeviceConfig(DeviceConfigType.DeviceConfig deviceConfig) throws CouldNotPerformException;

    public void removeDeviceConfig(DeviceConfigType.DeviceConfig deviceConfig) throws CouldNotPerformException;
    
    public void registerDeviceClass(DeviceClassType.DeviceClass deviceClass) throws CouldNotPerformException;

    public void updateDeviceClass(DeviceClassType.DeviceClass deviceClass) throws CouldNotPerformException;

    public void removeDeviceClass(DeviceClassType.DeviceClass deviceClass) throws CouldNotPerformException;
}
