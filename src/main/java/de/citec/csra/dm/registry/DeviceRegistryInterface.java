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

    public DeviceConfigType.DeviceConfig registerDeviceConfig(DeviceConfigType.DeviceConfig deviceConfig) throws CouldNotPerformException;

    public boolean containsDeviceConfig(DeviceConfigType.DeviceConfig deviceConfig) throws CouldNotPerformException;

    public boolean containsDeviceConfigById(String deviceConfigId) throws CouldNotPerformException;

    public DeviceConfigType.DeviceConfig updateDeviceConfig(DeviceConfigType.DeviceConfig deviceConfig) throws CouldNotPerformException;

    public DeviceConfigType.DeviceConfig removeDeviceConfig(DeviceConfigType.DeviceConfig deviceConfig) throws CouldNotPerformException;

    public DeviceClassType.DeviceClass registerDeviceClass(DeviceClassType.DeviceClass deviceClass) throws CouldNotPerformException;

    public boolean containsDeviceClassById(String deviceClassId) throws CouldNotPerformException;

    public boolean containsDeviceClass(DeviceClassType.DeviceClass deviceClass) throws CouldNotPerformException;

    public DeviceClassType.DeviceClass updateDeviceClass(DeviceClassType.DeviceClass deviceClass) throws CouldNotPerformException;

    public DeviceClassType.DeviceClass removeDeviceClass(DeviceClassType.DeviceClass deviceClass) throws CouldNotPerformException;

}
