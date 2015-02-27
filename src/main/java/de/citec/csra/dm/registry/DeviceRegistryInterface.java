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

    public void register(DeviceConfigType.DeviceConfig deviceConfig) throws CouldNotPerformException;

    public void update(DeviceConfigType.DeviceConfig deviceConfig) throws CouldNotPerformException;

    public void remove(DeviceConfigType.DeviceConfig deviceConfig) throws CouldNotPerformException;
    
    public void register(DeviceClassType.DeviceClass deviceClass) throws CouldNotPerformException;

    public void update(DeviceClassType.DeviceClass deviceClass) throws CouldNotPerformException;

    public void remove(DeviceClassType.DeviceClass deviceClass) throws CouldNotPerformException;
}
