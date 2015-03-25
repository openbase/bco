/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.csra.dm.registry;

import de.citec.jul.exception.CouldNotPerformException;
import java.util.List;
import rst.homeautomation.device.DeviceClassType;
import rst.homeautomation.device.DeviceConfigType;
import rst.homeautomation.service.ServiceConfigType;
import rst.homeautomation.unit.UnitConfigType;

/**
 *
 * @author mpohling
 */
public interface DeviceRegistryInterface {

    public DeviceConfigType.DeviceConfig registerDeviceConfig(DeviceConfigType.DeviceConfig deviceConfig) throws CouldNotPerformException;

    public Boolean containsDeviceConfig(DeviceConfigType.DeviceConfig deviceConfig) throws CouldNotPerformException;

    public Boolean containsDeviceConfigById(String deviceConfigId) throws CouldNotPerformException;

    public DeviceConfigType.DeviceConfig updateDeviceConfig(DeviceConfigType.DeviceConfig deviceConfig) throws CouldNotPerformException;

    public DeviceConfigType.DeviceConfig removeDeviceConfig(DeviceConfigType.DeviceConfig deviceConfig) throws CouldNotPerformException;

    public DeviceClassType.DeviceClass registerDeviceClass(DeviceClassType.DeviceClass deviceClass) throws CouldNotPerformException;

    public Boolean containsDeviceClassById(String deviceClassId) throws CouldNotPerformException;

    public Boolean containsDeviceClass(DeviceClassType.DeviceClass deviceClass) throws CouldNotPerformException;

    public DeviceClassType.DeviceClass updateDeviceClass(DeviceClassType.DeviceClass deviceClass) throws CouldNotPerformException;

    public DeviceClassType.DeviceClass removeDeviceClass(DeviceClassType.DeviceClass deviceClass) throws CouldNotPerformException;

    public List<UnitConfigType.UnitConfig> getUnits() throws CouldNotPerformException;
    
    public List<ServiceConfigType.ServiceConfig> getServices() throws CouldNotPerformException;
}
