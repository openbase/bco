package org.dc.bco.registry.device.lib;

/*
 * #%L
 * REM DeviceRegistry Library
 * %%
 * Copyright (C) 2014 - 2016 DivineCooperation
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-3.0.html>.
 * #L%
 */
import java.util.List;
import java.util.concurrent.Future;
import org.dc.jul.exception.CouldNotPerformException;
import rst.homeautomation.device.DeviceClassType.DeviceClass;
import rst.homeautomation.device.DeviceConfigType.DeviceConfig;
import rst.homeautomation.service.ServiceConfigType.ServiceConfig;
import rst.homeautomation.service.ServiceTemplateType;
import rst.homeautomation.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import rst.homeautomation.unit.UnitConfigType.UnitConfig;
import rst.homeautomation.unit.UnitGroupConfigType.UnitGroupConfig;
import rst.homeautomation.unit.UnitTemplateType.UnitTemplate;
import rst.homeautomation.unit.UnitTemplateType.UnitTemplate.UnitType;
import rst.rsb.ScopeType.Scope;

/**
 *
 * @author mpohling
 */
// TODO mpohling: write java doc
public interface DeviceRegistry {

    public Future<DeviceConfig> registerDeviceConfig(final DeviceConfig deviceConfig) throws CouldNotPerformException;

    public Future<DeviceConfig> updateDeviceConfig(final DeviceConfig deviceConfig) throws CouldNotPerformException;

    public Future<DeviceConfig> removeDeviceConfig(final DeviceConfig deviceConfig) throws CouldNotPerformException;

    public Future<DeviceClass> registerDeviceClass(final DeviceClass deviceClass) throws CouldNotPerformException;

    public Future<DeviceClass> updateDeviceClass(final DeviceClass deviceClass) throws CouldNotPerformException;

    public Future<DeviceClass> removeDeviceClass(final DeviceClass deviceClass) throws CouldNotPerformException;

    public Future<UnitTemplate> updateUnitTemplate(final UnitTemplate unitTemplate) throws CouldNotPerformException;

    public Future<UnitGroupConfig> registerUnitGroupConfig(final UnitGroupConfig groupConfig) throws CouldNotPerformException;

    public Future<UnitGroupConfig> updateUnitGroupConfig(final UnitGroupConfig groupConfig) throws CouldNotPerformException;

    public Future<UnitGroupConfig> removeUnitGroupConfig(final UnitGroupConfig groupConfig) throws CouldNotPerformException;

    public Boolean containsUnitTemplate(final UnitTemplate unitTemplate) throws CouldNotPerformException;

    public Boolean containsUnitTemplateById(final String unitTemplateId) throws CouldNotPerformException;

    public Boolean containsDeviceConfig(final DeviceConfig deviceConfig) throws CouldNotPerformException;

    public Boolean containsDeviceConfigById(final String deviceConfigId) throws CouldNotPerformException;

    public Boolean containsDeviceClassById(final String deviceClassId) throws CouldNotPerformException;

    public Boolean containsDeviceClass(final DeviceClass deviceClass) throws CouldNotPerformException;

    public UnitTemplate getUnitTemplateById(final String unitTemplate) throws CouldNotPerformException, InterruptedException;

    public DeviceClass getDeviceClassById(final String deviceClassId) throws CouldNotPerformException, InterruptedException;

    public DeviceConfig getDeviceConfigById(final String deviceConfigId) throws CouldNotPerformException, InterruptedException;

    public UnitConfig getUnitConfigById(final String unitConfigId) throws CouldNotPerformException, InterruptedException;

    /**
     * Method returns all registered units with the given label. Label resolving
     * is done case insensitive!
     *
     * @param unitConfigLabel
     * @return
     * @throws CouldNotPerformException
     */
    public List<UnitConfig> getUnitConfigsByLabel(final String unitConfigLabel) throws CouldNotPerformException;

    public List<UnitTemplate> getUnitTemplates() throws CouldNotPerformException;

    public List<DeviceClass> getDeviceClasses() throws CouldNotPerformException;

    public List<DeviceConfig> getDeviceConfigs() throws CouldNotPerformException;

    public List<UnitConfig> getUnitConfigs() throws CouldNotPerformException;

    public List<UnitConfig> getUnitConfigs(final UnitType type) throws CouldNotPerformException;

    public List<ServiceConfig> getServiceConfigs() throws CouldNotPerformException;

    public List<ServiceConfig> getServiceConfigs(final ServiceTemplateType.ServiceTemplate.ServiceType serviceType) throws CouldNotPerformException;

    public UnitTemplate getUnitTemplateByType(final UnitType type) throws CouldNotPerformException;

    public Boolean isUnitTemplateRegistryReadOnly() throws CouldNotPerformException;

    public Boolean isDeviceClassRegistryReadOnly() throws CouldNotPerformException;

    public Boolean isDeviceConfigRegistryReadOnly() throws CouldNotPerformException;

    public Boolean isUnitGroupConfigRegistryReadOnly() throws CouldNotPerformException;

    public Boolean isUnitTemplateRegistryConsistent() throws CouldNotPerformException;

    public Boolean isDeviceClassRegistryConsistent() throws CouldNotPerformException;

    public Boolean isDeviceConfigRegistryConsistent() throws CouldNotPerformException;

    public Boolean isUnitGroupConfigRegistryConsistent() throws CouldNotPerformException;

    public Boolean containsUnitGroupConfig(final UnitGroupConfig groupConfig) throws CouldNotPerformException;

    public Boolean containsUnitGroupConfigById(final String groupConfigId) throws CouldNotPerformException;

    public UnitGroupConfig getUnitGroupConfigById(final String groupConfigId) throws CouldNotPerformException;

    public List<UnitGroupConfig> getUnitGroupConfigs() throws CouldNotPerformException;

    public List<UnitGroupConfig> getUnitGroupConfigsbyUnitConfig(final UnitConfig unitConfig) throws CouldNotPerformException;

    public List<UnitGroupConfig> getUnitGroupConfigsByUnitType(final UnitType type) throws CouldNotPerformException;

    public List<UnitGroupConfig> getUnitGroupConfigsByServiceTypes(final List<ServiceType> serviceTypes) throws CouldNotPerformException;

    public List<UnitConfig> getUnitConfigsByUnitGroupConfig(final UnitGroupConfig groupConfig) throws CouldNotPerformException;

    public List<UnitConfig> getUnitConfigsByUnitTypeAndServiceTypes(final UnitType type, final List<ServiceType> serviceTypes) throws CouldNotPerformException;

    /**
     * Method return the unit config which is registered for the given scope. A
     * NotAvailableException is thrown if no unit config is registered for the
     * given scope.
     *
     * @param scope
     * @return the unit config matching the given scope.
     * @throws CouldNotPerformException
     */
    public UnitConfig getUnitConfigByScope(final Scope scope) throws CouldNotPerformException;

    public List<UnitType> getSubUnitTypesOfUnitType(final UnitType type) throws CouldNotPerformException;

    public void shutdown();

    @Deprecated
    public default void waitForConsistency() throws InterruptedException {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                if (isDeviceClassRegistryConsistent()
                        && isDeviceConfigRegistryConsistent()
                        && isUnitGroupConfigRegistryConsistent()
                        && isUnitTemplateRegistryConsistent()) {
                    System.out.println("WaitForConsistency finished!");
                    return;
                }
            } catch (CouldNotPerformException ex) {
            }
            Thread.sleep(1000);
            System.out.println("wait for consistency...");
        }
    }
}
