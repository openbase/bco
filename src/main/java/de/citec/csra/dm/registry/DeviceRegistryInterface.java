/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.csra.dm.registry;

import de.citec.jul.exception.CouldNotPerformException;
import java.util.List;
import rst.homeautomation.device.DeviceClassType.DeviceClass;
import rst.homeautomation.device.DeviceConfigType.DeviceConfig;
import rst.homeautomation.service.ServiceConfigType.ServiceConfig;
import rst.homeautomation.unit.UnitConfigType.UnitConfig;

/**
 *
 * @author mpohling
 */
public interface DeviceRegistryInterface {

    public DeviceConfig registerDeviceConfig(DeviceConfig deviceConfig) throws CouldNotPerformException;

    public Boolean containsDeviceConfig(DeviceConfig deviceConfig) throws CouldNotPerformException;

    public Boolean containsDeviceConfigById(String deviceConfigId) throws CouldNotPerformException;

    public DeviceConfig updateDeviceConfig(DeviceConfig deviceConfig) throws CouldNotPerformException;

    public DeviceConfig removeDeviceConfig(DeviceConfig deviceConfig) throws CouldNotPerformException;

    public DeviceClass registerDeviceClass(DeviceClass deviceClass) throws CouldNotPerformException;

    public Boolean containsDeviceClassById(String deviceClassId) throws CouldNotPerformException;

    public Boolean containsDeviceClass(DeviceClass deviceClass) throws CouldNotPerformException;

    public DeviceClass updateDeviceClass(DeviceClass deviceClass) throws CouldNotPerformException;

    public DeviceClass removeDeviceClass(DeviceClass deviceClass) throws CouldNotPerformException;
    
    public DeviceClass getDeviceClassById(final String deviceClassId) throws CouldNotPerformException;
    
    public DeviceConfig getDeviceConfigById(final String deviceConfigId) throws CouldNotPerformException;
    
    public UnitConfig getUnitConfigById(final String unitConfigId) throws CouldNotPerformException;

    public List<DeviceClass> getDeviceClasses() throws CouldNotPerformException;

    public List<DeviceConfig> getDeviceConfigs() throws CouldNotPerformException;

    public List<UnitConfig> getUnitConfigs() throws CouldNotPerformException;
    
    public List<ServiceConfig> getServiceConfigs() throws CouldNotPerformException;
    
    public void shutdown();
}
