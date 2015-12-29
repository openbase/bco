/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dm.lib.registry;

import org.dc.jul.exception.CouldNotPerformException;
import java.util.List;
import java.util.concurrent.Future;
import rst.homeautomation.device.DeviceClassType.DeviceClass;
import rst.homeautomation.device.DeviceConfigType.DeviceConfig;
import rst.homeautomation.service.ServiceConfigType.ServiceConfig;
import rst.homeautomation.service.ServiceTemplateType;
import rst.homeautomation.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import rst.homeautomation.unit.UnitConfigType.UnitConfig;
import rst.homeautomation.unit.UnitGroupConfigType.UnitGroupConfig;
import rst.homeautomation.unit.UnitTemplateType.UnitTemplate;
import rst.homeautomation.unit.UnitTemplateType.UnitTemplate.UnitType;

/**
 *
 * @author mpohling
 */
// TODO mpohling: write java doc
public interface DeviceRegistryInterface {

    public DeviceConfig registerDeviceConfig(final DeviceConfig deviceConfig) throws CouldNotPerformException;

    public Boolean containsUnitTemplate(final UnitTemplate unitTemplate) throws CouldNotPerformException;

    public Boolean containsUnitTemplateById(final String unitTemplateId) throws CouldNotPerformException;

    public Boolean containsDeviceConfig(final DeviceConfig deviceConfig) throws CouldNotPerformException;

    public Boolean containsDeviceConfigById(final String deviceConfigId) throws CouldNotPerformException;

    public UnitTemplate updateUnitTemplate(final UnitTemplate unitTemplate) throws CouldNotPerformException;

    public DeviceConfig updateDeviceConfig(final DeviceConfig deviceConfig) throws CouldNotPerformException;

    public DeviceConfig removeDeviceConfig(final DeviceConfig deviceConfig) throws CouldNotPerformException;

    public DeviceClass registerDeviceClass(final DeviceClass deviceClass) throws CouldNotPerformException;

    public Boolean containsDeviceClassById(final String deviceClassId) throws CouldNotPerformException;

    public Boolean containsDeviceClass(final DeviceClass deviceClass) throws CouldNotPerformException;

    public DeviceClass updateDeviceClass(final DeviceClass deviceClass) throws CouldNotPerformException;

    public DeviceClass removeDeviceClass(final DeviceClass deviceClass) throws CouldNotPerformException;

    public UnitTemplate getUnitTemplateById(final String unitTemplate) throws CouldNotPerformException;

    public DeviceClass getDeviceClassById(final String deviceClassId) throws CouldNotPerformException;

    public DeviceConfig getDeviceConfigById(final String deviceConfigId) throws CouldNotPerformException;

    public UnitConfig getUnitConfigById(final String unitConfigId) throws CouldNotPerformException;

    public List<UnitConfig> getUnitConfigsByLabel(final String unitConfigLabel) throws CouldNotPerformException;

    public List<UnitTemplate> getUnitTemplates() throws CouldNotPerformException;

    public List<DeviceClass> getDeviceClasses() throws CouldNotPerformException;

    public List<DeviceConfig> getDeviceConfigs() throws CouldNotPerformException;

    public List<UnitConfig> getUnitConfigs() throws CouldNotPerformException;

    public List<UnitConfig> getUnitConfigs(final UnitType type) throws CouldNotPerformException;

    public List<ServiceConfig> getServiceConfigs() throws CouldNotPerformException;

    public List<ServiceConfig> getServiceConfigs(final ServiceTemplateType.ServiceTemplate.ServiceType serviceType) throws CouldNotPerformException;

    public UnitTemplate getUnitTemplateByType(final UnitType type) throws CouldNotPerformException;

    public Future<Boolean> isUnitTemplateRegistryReadOnly() throws CouldNotPerformException;

    public Future<Boolean> isDeviceClassRegistryReadOnly() throws CouldNotPerformException;

    public Future<Boolean> isDeviceConfigRegistryReadOnly() throws CouldNotPerformException;

    public UnitGroupConfig registerUnitGroupConfig(UnitGroupConfig groupConfig) throws CouldNotPerformException;

    public Boolean containsUnitGroupConfig(UnitGroupConfig groupConfig) throws CouldNotPerformException;

    public Boolean containsUnitGroupConfigById(String groupConfigId) throws CouldNotPerformException;

    public UnitGroupConfig updateUnitGroupConfig(UnitGroupConfig groupConfig) throws CouldNotPerformException;

    public UnitGroupConfig removeUnitGroupConfig(UnitGroupConfig groupConfig) throws CouldNotPerformException;

    public UnitGroupConfig getUnitGroupConfigById(final String groupConfigId) throws CouldNotPerformException;

    public List<UnitGroupConfig> getUnitGroupConfigs() throws CouldNotPerformException;

    public List<UnitGroupConfig> getUnitGroupConfigsbyUnitConfig(UnitConfig unitConfig) throws CouldNotPerformException;

    public List<UnitGroupConfig> getUnitGroupConfigsByUnitType(UnitType type) throws CouldNotPerformException;

    public List<UnitGroupConfig> getUnitGroupConfigsByServiceTypes(List<ServiceType> serviceTypes) throws CouldNotPerformException;

    public List<UnitConfig> getUnitConfigsByUnitGroupConfig(UnitGroupConfig groupConfig) throws CouldNotPerformException;

    public List<UnitConfig> getUnitConfigsByUnitTypeAndServiceTypes(UnitType type, List<ServiceType> serviceTypes) throws CouldNotPerformException;

    public Future<Boolean> isUnitGroupConfigRegistryReadOnly() throws CouldNotPerformException;

    public void shutdown();
}
