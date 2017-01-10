package org.openbase.bco.registry.device.lib;

/*
 * #%L
 * BCO Registry Device Library
 * %%
 * Copyright (C) 2014 - 2017 openbase.org
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
import org.openbase.jul.exception.CouldNotPerformException;
import rst.domotic.service.ServiceConfigType.ServiceConfig;
import rst.domotic.service.ServiceTemplateType.ServiceTemplate;
import rst.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import rst.domotic.unit.UnitConfigType.UnitConfig;
import rst.domotic.unit.UnitTemplateType.UnitTemplate;
import rst.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;
import rst.domotic.unit.device.DeviceClassType.DeviceClass;
import rst.rsb.ScopeType.Scope;

/**
 *
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
// TODO mpohling: write java doc
public interface DeviceRegistry {

    public Future<UnitConfig> registerDeviceConfig(final UnitConfig deviceConfig) throws CouldNotPerformException;

    public Future<UnitConfig> updateDeviceConfig(final UnitConfig deviceConfig) throws CouldNotPerformException;

    public Future<UnitConfig> removeDeviceConfig(final UnitConfig deviceConfig) throws CouldNotPerformException;

    public Future<DeviceClass> registerDeviceClass(final DeviceClass deviceClass) throws CouldNotPerformException;

    public Future<DeviceClass> updateDeviceClass(final DeviceClass deviceClass) throws CouldNotPerformException;

    public Future<DeviceClass> removeDeviceClass(final DeviceClass deviceClass) throws CouldNotPerformException;

    @Deprecated
    public Future<UnitTemplate> updateUnitTemplate(final UnitTemplate unitTemplate) throws CouldNotPerformException;

    @Deprecated
    public Future<UnitConfig> registerUnitGroupConfig(final UnitConfig groupConfig) throws CouldNotPerformException;

    @Deprecated
    public Future<UnitConfig> updateUnitGroupConfig(final UnitConfig groupConfig) throws CouldNotPerformException;

    @Deprecated
    public Future<UnitConfig> removeUnitGroupConfig(final UnitConfig groupConfig) throws CouldNotPerformException;

    @Deprecated
    public Boolean containsUnitTemplate(final UnitTemplate unitTemplate) throws CouldNotPerformException;

    @Deprecated
    public Boolean containsUnitTemplateById(final String unitTemplateId) throws CouldNotPerformException;

    public Boolean containsDeviceConfig(final UnitConfig deviceConfig) throws CouldNotPerformException;

    public Boolean containsDeviceConfigById(final String deviceConfigId) throws CouldNotPerformException;

    public Boolean containsDeviceClassById(final String deviceClassId) throws CouldNotPerformException;

    public Boolean containsDeviceClass(final DeviceClass deviceClass) throws CouldNotPerformException;

    @Deprecated
    public UnitTemplate getUnitTemplateById(final String unitTemplate) throws CouldNotPerformException, InterruptedException;

    public DeviceClass getDeviceClassById(final String deviceClassId) throws CouldNotPerformException, InterruptedException;

    public UnitConfig getDeviceConfigById(final String deviceConfigId) throws CouldNotPerformException, InterruptedException;

    @Deprecated
    public UnitConfig getUnitConfigById(final String unitConfigId) throws CouldNotPerformException, InterruptedException;

    /**
     * Method returns all registered units with the given label. Label resolving
     * is done case insensitive!
     *
     * @param unitConfigLabel
     * @return
     * @throws CouldNotPerformException
     */
    @Deprecated
    public List<UnitConfig> getUnitConfigsByLabel(final String unitConfigLabel) throws CouldNotPerformException;

    @Deprecated
    public List<UnitTemplate> getUnitTemplates() throws CouldNotPerformException;

    public List<DeviceClass> getDeviceClasses() throws CouldNotPerformException;

    public List<UnitConfig> getDeviceConfigs() throws CouldNotPerformException;

    @Deprecated
    public List<UnitConfig> getUnitConfigs() throws CouldNotPerformException;

    @Deprecated
    public List<UnitConfig> getUnitConfigs(final UnitType type) throws CouldNotPerformException;

    @Deprecated
    public List<ServiceConfig> getServiceConfigs() throws CouldNotPerformException;

    @Deprecated
    public List<ServiceConfig> getServiceConfigs(final ServiceTemplate.ServiceType serviceType) throws CouldNotPerformException;

    @Deprecated
    public UnitTemplate getUnitTemplateByType(final UnitType type) throws CouldNotPerformException;

    @Deprecated
    public Boolean isUnitTemplateRegistryReadOnly() throws CouldNotPerformException;

    public Boolean isDeviceClassRegistryReadOnly() throws CouldNotPerformException;

    public Boolean isDeviceConfigRegistryReadOnly() throws CouldNotPerformException;

    @Deprecated
    public Boolean isUnitGroupConfigRegistryReadOnly() throws CouldNotPerformException;

    @Deprecated
    public Boolean isUnitTemplateRegistryConsistent() throws CouldNotPerformException;

    public Boolean isDeviceClassRegistryConsistent() throws CouldNotPerformException;

    public Boolean isDeviceConfigRegistryConsistent() throws CouldNotPerformException;

    @Deprecated
    public Boolean isUnitGroupConfigRegistryConsistent() throws CouldNotPerformException;

    @Deprecated
    public Boolean containsUnitGroupConfig(final UnitConfig groupConfig) throws CouldNotPerformException;

    @Deprecated
    public Boolean containsUnitGroupConfigById(final String groupConfigId) throws CouldNotPerformException;

    @Deprecated
    public UnitConfig getUnitGroupConfigById(final String groupConfigId) throws CouldNotPerformException;

    @Deprecated
    public List<UnitConfig> getUnitGroupConfigs() throws CouldNotPerformException;

    @Deprecated
    public List<UnitConfig> getUnitGroupConfigsbyUnitConfig(final UnitConfig unitConfig) throws CouldNotPerformException;

    @Deprecated
    public List<UnitConfig> getUnitGroupConfigsByUnitType(final UnitType type) throws CouldNotPerformException;

    @Deprecated
    public List<UnitConfig> getUnitGroupConfigsByServiceTypes(final List<ServiceType> serviceTypes) throws CouldNotPerformException;

    @Deprecated
    public List<UnitConfig> getUnitConfigsByUnitGroupConfig(final UnitConfig groupConfig) throws CouldNotPerformException;

    @Deprecated
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
    @Deprecated
    public UnitConfig getUnitConfigByScope(final Scope scope) throws CouldNotPerformException;

    @Deprecated
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
                    return;
                }
            } catch (CouldNotPerformException ex) {
            }
        }
    }
}
