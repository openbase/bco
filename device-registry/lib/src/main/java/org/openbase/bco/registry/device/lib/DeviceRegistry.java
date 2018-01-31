package org.openbase.bco.registry.device.lib;

/*
 * #%L
 * BCO Registry Device Library
 * %%
 * Copyright (C) 2014 - 2018 openbase.org
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

import org.openbase.bco.registry.lib.provider.DeviceClassCollectionProvider;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.iface.Shutdownable;
import org.openbase.jul.iface.annotations.RPCMethod;
import org.openbase.jul.pattern.provider.DataProvider;
import rst.domotic.registry.DeviceRegistryDataType.DeviceRegistryData;
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
public interface DeviceRegistry extends DataProvider<DeviceRegistryData>, DeviceClassCollectionProvider, Shutdownable {

    @RPCMethod
    public Future<UnitConfig> registerDeviceConfig(final UnitConfig deviceConfig) throws CouldNotPerformException;

    @RPCMethod
    public Future<UnitConfig> updateDeviceConfig(final UnitConfig deviceConfig) throws CouldNotPerformException;

    @RPCMethod
    public Future<UnitConfig> removeDeviceConfig(final UnitConfig deviceConfig) throws CouldNotPerformException;

    @RPCMethod
    public Future<DeviceClass> registerDeviceClass(final DeviceClass deviceClass) throws CouldNotPerformException;

    @RPCMethod
    public Future<DeviceClass> updateDeviceClass(final DeviceClass deviceClass) throws CouldNotPerformException;

    @RPCMethod
    public Future<DeviceClass> removeDeviceClass(final DeviceClass deviceClass) throws CouldNotPerformException;

    /**
     *
     * @param unitTemplate
     * @return
     * @throws CouldNotPerformException
     * @deprecated this method has been moved to the unit registry.
     */
    @RPCMethod
    @Deprecated
    public Future<UnitTemplate> updateUnitTemplate(final UnitTemplate unitTemplate) throws CouldNotPerformException;

    /**
     *
     * @param groupConfig
     * @return
     * @throws CouldNotPerformException
     * @deprecated this method has been moved to the unit registry.
     */
    @RPCMethod
    @Deprecated
    public Future<UnitConfig> registerUnitGroupConfig(final UnitConfig groupConfig) throws CouldNotPerformException;

    /**
     *
     * @param groupConfig
     * @return
     * @throws CouldNotPerformException
     * @deprecated this method has been moved to the unit registry.
     */
    @RPCMethod
    @Deprecated
    public Future<UnitConfig> updateUnitGroupConfig(final UnitConfig groupConfig) throws CouldNotPerformException;

    /**
     *
     * @param groupConfig
     * @return
     * @throws CouldNotPerformException
     * @deprecated this method has been moved to the unit registry.
     */
    @RPCMethod
    @Deprecated
    public Future<UnitConfig> removeUnitGroupConfig(final UnitConfig groupConfig) throws CouldNotPerformException;

    /**
     *
     * @param unitTemplate
     * @return
     * @throws CouldNotPerformException
     * @deprecated this method has been moved to the unit registry.
     */
    @RPCMethod
    @Deprecated
    public Boolean containsUnitTemplate(final UnitTemplate unitTemplate) throws CouldNotPerformException;

    /**
     *
     * @param unitTemplateId
     * @return
     * @throws CouldNotPerformException
     * @deprecated this method has been moved to the unit registry.
     */
    @RPCMethod
    @Deprecated
    public Boolean containsUnitTemplateById(final String unitTemplateId) throws CouldNotPerformException;

    @RPCMethod
    public Boolean containsDeviceConfig(final UnitConfig deviceConfig) throws CouldNotPerformException;

    @RPCMethod
    public Boolean containsDeviceConfigById(final String deviceConfigId) throws CouldNotPerformException;

    @RPCMethod
    public Boolean containsDeviceClassById(final String deviceClassId) throws CouldNotPerformException;

    @RPCMethod
    public Boolean containsDeviceClass(final DeviceClass deviceClass) throws CouldNotPerformException;

    /**
     *
     * @param unitTemplate
     * @return
     * @throws CouldNotPerformException
     * @throws InterruptedException
     * @deprecated this method has been moved to the unit registry.
     */
    @RPCMethod
    @Deprecated
    public UnitTemplate getUnitTemplateById(final String unitTemplate) throws CouldNotPerformException, InterruptedException;

    @RPCMethod
    public UnitConfig getDeviceConfigById(final String deviceConfigId) throws CouldNotPerformException, InterruptedException;

    /**
     *
     * @param unitConfigId
     * @return
     * @throws CouldNotPerformException
     * @throws InterruptedException
     * @deprecated this method has been moved to the unit registry.
     */
    @RPCMethod
    @Deprecated
    public UnitConfig getUnitConfigById(final String unitConfigId) throws CouldNotPerformException, InterruptedException;

    /**
     * Method returns all registered units with the given label. Label resolving
     * is done case insensitive!
     *
     * @param unitConfigLabel
     * @return
     * @throws CouldNotPerformException
     * @deprecated this method has been moved to the unit registry.
     */
    @Deprecated
    public List<UnitConfig> getUnitConfigsByLabel(final String unitConfigLabel) throws CouldNotPerformException;

    /**
     *
     * @return
     * @throws CouldNotPerformException
     * @deprecated this method has been moved to the unit registry.
     */
    @Deprecated
    public List<UnitTemplate> getUnitTemplates() throws CouldNotPerformException;

    public List<UnitConfig> getDeviceConfigs() throws CouldNotPerformException;

    /**
     *
     * @return
     * @throws CouldNotPerformException
     * @deprecated this method has been moved to the unit registry.
     */
    @Deprecated
    public List<UnitConfig> getUnitConfigs() throws CouldNotPerformException;

    /**
     *
     * @param type
     * @return
     * @throws CouldNotPerformException
     * @deprecated this method has been moved to the unit registry.
     */
    @Deprecated
    public List<UnitConfig> getUnitConfigs(final UnitType type) throws CouldNotPerformException;

    /**
     *
     * @return
     * @throws CouldNotPerformException
     * @deprecated this method has been moved to the unit registry.
     */
    @Deprecated
    public List<ServiceConfig> getServiceConfigs() throws CouldNotPerformException;

    /**
     *
     * @param serviceType
     * @return
     * @throws CouldNotPerformException
     * @deprecated this method has been moved to the unit registry.
     */
    @RPCMethod
    @Deprecated
    public List<ServiceConfig> getServiceConfigs(final ServiceTemplate.ServiceType serviceType) throws CouldNotPerformException;

    /**
     *
     * @param type
     * @return
     * @throws CouldNotPerformException
     * @deprecated this method has been moved to the unit registry.
     */
    @Deprecated
    public UnitTemplate getUnitTemplateByType(final UnitType type) throws CouldNotPerformException;

    /**
     *
     * @return
     * @throws CouldNotPerformException
     * @deprecated this method has been moved to the unit registry.
     */
    @RPCMethod
    @Deprecated
    public Boolean isUnitTemplateRegistryReadOnly() throws CouldNotPerformException;

    @RPCMethod
    public Boolean isDeviceClassRegistryReadOnly() throws CouldNotPerformException;

    @RPCMethod
    public Boolean isDeviceConfigRegistryReadOnly() throws CouldNotPerformException;

    /**
     *
     * @return
     * @throws CouldNotPerformException
     * @deprecated this method has been moved to the unit registry.
     */
    @RPCMethod
    @Deprecated
    public Boolean isUnitGroupConfigRegistryReadOnly() throws CouldNotPerformException;

    /**
     *
     * @return
     * @throws CouldNotPerformException
     * @deprecated this method has been moved to the unit registry.
     */
    @RPCMethod
    @Deprecated
    public Boolean isUnitTemplateRegistryConsistent() throws CouldNotPerformException;

    @RPCMethod
    public Boolean isDeviceClassRegistryConsistent() throws CouldNotPerformException;

    @RPCMethod
    public Boolean isDeviceConfigRegistryConsistent() throws CouldNotPerformException;

    /**
     *
     * @return
     * @throws CouldNotPerformException
     * @deprecated this method has been moved to the unit registry.
     */
    @RPCMethod
    @Deprecated
    public Boolean isUnitGroupConfigRegistryConsistent() throws CouldNotPerformException;

    /**
     *
     * @param groupConfig
     * @return
     * @throws CouldNotPerformException
     * @deprecated this method has been moved to the unit registry.
     */
    @RPCMethod
    @Deprecated
    public Boolean containsUnitGroupConfig(final UnitConfig groupConfig) throws CouldNotPerformException;

    /**
     *
     * @param groupConfigId
     * @return
     * @throws CouldNotPerformException
     * @deprecated this method has been moved to the unit registry.
     */
    @RPCMethod
    @Deprecated
    public Boolean containsUnitGroupConfigById(final String groupConfigId) throws CouldNotPerformException;

    /**
     *
     * @param groupConfigId
     * @return
     * @throws CouldNotPerformException
     * @deprecated this method has been moved to the unit registry.
     */
    @RPCMethod
    @Deprecated
    public UnitConfig getUnitGroupConfigById(final String groupConfigId) throws CouldNotPerformException;

    /**
     *
     * @return
     * @throws CouldNotPerformException
     * @deprecated this method has been moved to the unit registry.
     */
    @RPCMethod
    @Deprecated
    public List<UnitConfig> getUnitGroupConfigs() throws CouldNotPerformException;

    /**
     *
     * @param unitConfig
     * @return
     * @throws CouldNotPerformException
     * @deprecated this method has been moved to the unit registry.
     */
    @RPCMethod
    @Deprecated
    public List<UnitConfig> getUnitGroupConfigsbyUnitConfig(final UnitConfig unitConfig) throws CouldNotPerformException;

    /**
     *
     * @param type
     * @return
     * @throws CouldNotPerformException
     * @deprecated
     */
    @RPCMethod
    @Deprecated
    public List<UnitConfig> getUnitGroupConfigsByUnitType(final UnitType type) throws CouldNotPerformException;

    /**
     *
     * @param serviceTypes
     * @return
     * @throws CouldNotPerformException
     * @deprecated
     */
    @RPCMethod
    @Deprecated
    public List<UnitConfig> getUnitGroupConfigsByServiceTypes(final List<ServiceType> serviceTypes) throws CouldNotPerformException;

    /**
     *
     * @param groupConfig
     * @return
     * @throws CouldNotPerformException
     * @deprecated
     */
    @RPCMethod
    @Deprecated
    public List<UnitConfig> getUnitConfigsByUnitGroupConfig(final UnitConfig groupConfig) throws CouldNotPerformException;

    /**
     *
     * @param type
     * @param serviceTypes
     * @return
     * @throws CouldNotPerformException
     * @deprecated
     */
    @RPCMethod
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
     * @deprecated this method has been moved to the unit registry.
     */
    @RPCMethod
    @Deprecated
    public UnitConfig getUnitConfigByScope(final Scope scope) throws CouldNotPerformException;

    /**
     *
     * @param type
     * @return
     * @throws CouldNotPerformException
     * @deprecated this method has been moved to the unit registry.
     */
    @RPCMethod
    @Deprecated
    public List<UnitType> getSubUnitTypesOfUnitType(final UnitType type) throws CouldNotPerformException;

    //    /**
//     *
//     * @throws InterruptedException
//     * @deprecated this method has been moved to the unit registry.
//     */
//    @Deprecated
//    public default void waitForConsistency() throws InterruptedException {
//        while (!Thread.currentThread().isInterrupted()) {
//            try {
//                if (isDeviceClassRegistryConsistent()
//                        && isDeviceConfigRegistryConsistent()
//                        && isUnitGroupConfigRegistryConsistent()
//                        && isUnitTemplateRegistryConsistent()) {
//                    return;
//                }
//                Thread.sleep(5);
//            } catch (CouldNotPerformException ex) {
//            }
//        }
//    }
}
