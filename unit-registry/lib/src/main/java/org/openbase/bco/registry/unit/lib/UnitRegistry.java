package org.openbase.bco.registry.unit.lib;

/*
 * #%L
 * BCO Registry Unit Library
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
 *
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Future;
import org.openbase.bco.registry.lib.util.UnitConfigProcessor;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InvalidStateException;
import org.openbase.jul.exception.VerificationFailedException;
import org.openbase.jul.extension.rsb.scope.ScopeGenerator;
import org.openbase.jul.iface.Shutdownable;
import org.openbase.jul.iface.annotations.RPCMethod;
import org.openbase.jul.pattern.provider.DataProvider;
import rst.domotic.registry.UnitRegistryDataType.UnitRegistryData;
import rst.domotic.service.ServiceConfigType;
import rst.domotic.service.ServiceConfigType.ServiceConfig;
import rst.domotic.service.ServiceTemplateType.ServiceTemplate;
import rst.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import rst.domotic.unit.UnitConfigType.UnitConfig;
import rst.domotic.unit.UnitTemplateType.UnitTemplate;
import rst.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;
import rst.rsb.ScopeType.Scope;

public interface UnitRegistry extends DataProvider<UnitRegistryData>, Shutdownable {

    @RPCMethod
    public Future<UnitConfig> registerUnitConfig(final UnitConfig unitConfig) throws CouldNotPerformException;

    @RPCMethod
    public Future<UnitConfig> updateUnitConfig(final UnitConfig unitConfig) throws CouldNotPerformException;

    @RPCMethod
    public Future<UnitConfig> removeUnitConfig(final UnitConfig unitConfig) throws CouldNotPerformException;

    @RPCMethod
    public Boolean containsUnitConfig(final UnitConfig unitConfig) throws CouldNotPerformException;

    @RPCMethod
    public Boolean containsUnitConfigById(final String unitConfigId) throws CouldNotPerformException;

    @RPCMethod
    public UnitConfig getUnitConfigById(final String unitConfigId) throws CouldNotPerformException;

    public List<UnitConfig> getUnitConfigs() throws CouldNotPerformException;

    public default List<UnitConfig> getUnitConfigsByServices(final ServiceType... serviceTypes) throws CouldNotPerformException {
        return getUnitConfigsByService(Arrays.asList(serviceTypes));
    }

    public default List<UnitConfig> getUnitConfigsByService(final List<ServiceType> serviceTypes) throws CouldNotPerformException {
        validateData();
        final List<UnitConfig> unitConfigs = getUnitConfigs();
        boolean foundServiceType;

        for (final UnitConfig unitConfig : new ArrayList<>(unitConfigs)) {
            foundServiceType = false;
            for (final ServiceType serviceType : serviceTypes) {
                for (ServiceConfigType.ServiceConfig serviceConfig : unitConfig.getServiceConfigList()) {
                    if (serviceConfig.getServiceDescription().getType() == serviceType) {
                        foundServiceType = true;
                    }
                }
                if (!foundServiceType) {
                    unitConfigs.remove(unitConfig);
                }
            }
        }
        return unitConfigs;
    }

    @RPCMethod
    default public String getUnitScopeById(final String id) throws CouldNotPerformException {
        return ScopeGenerator.generateStringRep(getUnitConfigById(id).getScope());
    }

    @RPCMethod
    public Boolean isUnitConfigRegistryReadOnly() throws CouldNotPerformException;

    /**
     * Method returns true if the underling registry is marked as consistent.
     *
     * @return if the UnitConfig registry is consistent
     * @throws CouldNotPerformException if the check fails
     */
    @RPCMethod
    public Boolean isUnitConfigRegistryConsistent() throws CouldNotPerformException;

    @RPCMethod
    public Future<UnitTemplate> updateUnitTemplate(final UnitTemplate unitTemplate) throws CouldNotPerformException;

    @RPCMethod
    public Boolean containsUnitTemplate(final UnitTemplate unitTemplate) throws CouldNotPerformException;

    @RPCMethod
    public Boolean containsUnitTemplateById(final String unitTemplateId) throws CouldNotPerformException;

    @RPCMethod
    public UnitTemplate getUnitTemplateById(final String unitTemplate) throws CouldNotPerformException;

    public List<UnitTemplate> getUnitTemplates() throws CouldNotPerformException;

    @RPCMethod
    public UnitTemplate getUnitTemplateByType(final UnitType type) throws CouldNotPerformException;

    @RPCMethod
    public Boolean isUnitTemplateRegistryReadOnly() throws CouldNotPerformException;

    @RPCMethod
    public Boolean isUnitTemplateRegistryConsistent() throws CouldNotPerformException;

    @RPCMethod
    public Future<UnitConfig> registerUnitGroupConfig(final UnitConfig groupConfig) throws CouldNotPerformException;

    @RPCMethod
    public Future<UnitConfig> updateUnitGroupConfig(final UnitConfig groupConfig) throws CouldNotPerformException;

    @RPCMethod
    public Future<UnitConfig> removeUnitGroupConfig(final UnitConfig groupConfig) throws CouldNotPerformException;

    @RPCMethod
    public Boolean containsUnitGroupConfig(final UnitConfig groupConfig) throws CouldNotPerformException;

    @RPCMethod
    public Boolean containsUnitGroupConfigById(final String groupConfigId) throws CouldNotPerformException;

    /**
     * Method returns all registered units with the given label. Label resolving
     * is done case insensitive!
     *
     * @param unitConfigLabel
     * @return
     * @throws CouldNotPerformException
     */
    public List<UnitConfig> getUnitConfigsByLabel(final String unitConfigLabel) throws CouldNotPerformException;

    public default List<UnitConfig> getUnitConfigsByLabelAndUnitType(final String unitConfigLabel, final UnitTemplate.UnitType unitType) throws CouldNotPerformException {
        validateData();
        List<UnitConfig> unitConfigs = Collections.synchronizedList(new ArrayList<>());
        getUnitConfigs().parallelStream().filter((unitConfig) -> (unitConfig.getType().equals(unitType) && unitConfig.getLabel().equalsIgnoreCase(unitConfigLabel))).forEach((unitConfig) -> {
            unitConfigs.add(unitConfig);
        });
        return unitConfigs;
    }

    public List<UnitConfig> getUnitConfigs(final UnitType type) throws CouldNotPerformException;

    /**
     * Method returns a list of all globally registered dal units.
     * Dal units are all units introduced via the unit templates which are not base units.
     * Base units are units of the following types: LOCATION, CONNECTION, SCENE, AGENT, APP, DEVICE, USER, AUTHORIZATION_GROUP, UNIT_GROUP
     *
     * @return a list of dal units.
     * @throws CouldNotPerformException is thrown in case something goes wrong during the request.
     */
    public List<UnitConfig> getDalUnitConfigs() throws CouldNotPerformException;

    /**
     * Method returns a list of all globally registered base units.
     * Base units are units of the following types: LOCATION, CONNECTION, SCENE, AGENT, APP, DEVICE, USER, AUTHORIZATION_GROUP, UNIT_GROUP
     *
     * @return a list of base units.
     * @throws CouldNotPerformException is thrown in case something goes wrong during the request.
     */
    public List<UnitConfig> getBaseUnitConfigs() throws CouldNotPerformException;

    public List<ServiceConfig> getServiceConfigs() throws CouldNotPerformException;

    public List<ServiceConfig> getServiceConfigs(final ServiceTemplate.ServiceType serviceType) throws CouldNotPerformException;

    @RPCMethod
    public Boolean isUnitGroupConfigRegistryReadOnly() throws CouldNotPerformException;

    @RPCMethod
    public Boolean isUnitGroupConfigRegistryConsistent() throws CouldNotPerformException;

    @RPCMethod
    public UnitConfig getUnitGroupConfigById(final String groupConfigId) throws CouldNotPerformException;

    public List<UnitConfig> getUnitGroupConfigs() throws CouldNotPerformException;

    public List<UnitConfig> getUnitGroupConfigsByUnitConfig(final UnitConfig unitConfig) throws CouldNotPerformException;

    public List<UnitConfig> getUnitGroupConfigsByUnitType(final UnitType type) throws CouldNotPerformException;

    public List<UnitConfig> getUnitGroupConfigsByServiceTypes(final List<ServiceType> serviceTypes) throws CouldNotPerformException;

    public List<UnitConfig> getUnitConfigsByUnitGroupConfig(final UnitConfig groupConfig) throws CouldNotPerformException;

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
    @RPCMethod
    public UnitConfig getUnitConfigByScope(final Scope scope) throws CouldNotPerformException;

    /**
     * Get all sub types of a unit type. E.g. COLORABLE_LIGHT and DIMMABLE_LIGHT are
     * sub types of LIGHT.
     *
     * @param type the super type whose sub types are searched
     * @return all types of which the given type is a super type
     * @throws CouldNotPerformException
     * @deprecated please use getSubUnitTypes instead
     */
    @Deprecated
    public default List<UnitType> getSubUnitTypesOfUnitType(final UnitType type) throws CouldNotPerformException {
        return getSubUnitTypes(type);
    }

    /**
     * Get all sub types of a unit type. E.g. COLORABLE_LIGHT and DIMMABLE_LIGHT are
     * sub types of LIGHT.
     *
     * @param type the super type whose sub types are searched
     * @return all types of which the given type is a super type
     * @throws CouldNotPerformException
     */
    public List<UnitType> getSubUnitTypes(final UnitType type) throws CouldNotPerformException;

    /**
     * Get all super types of a unit type. E.g. DIMMABLE_LIGHT and LIGHT are
     * super types of COLORABLE_LIGHT.
     *
     * @param type the type whose super types are returned
     * @return all super types of a given unit type
     * @throws CouldNotPerformException
     */
    public List<UnitType> getSuperUnitTypes(final UnitType type) throws CouldNotPerformException;

    public default void verifyUnitGroupUnitConfig(UnitConfig unitConfig) throws VerificationFailedException {
        UnitConfigProcessor.verifyUnitConfig(unitConfig, UnitType.UNIT_GROUP);
    }

    @RPCMethod
    public Boolean isDalUnitConfigRegistryReadOnly() throws CouldNotPerformException;

    @RPCMethod
    public Boolean isUserUnitRegistryReadOnly() throws CouldNotPerformException;

    @RPCMethod
    public Boolean isAuthorizationGroupUnitRegistryReadOnly() throws CouldNotPerformException;

    @RPCMethod
    public Boolean isDeviceUnitRegistryReadOnly() throws CouldNotPerformException;

    @RPCMethod
    public Boolean isUnitGroupUnitRegistryReadOnly() throws CouldNotPerformException;

    @RPCMethod
    public Boolean isLocationUnitRegistryReadOnly() throws CouldNotPerformException;

    @RPCMethod
    public Boolean isConnectionUnitRegistryReadOnly() throws CouldNotPerformException;

    @RPCMethod
    public Boolean isAgentUnitRegistryReadOnly() throws CouldNotPerformException;

    @RPCMethod
    public Boolean isAppUnitRegistryReadOnly() throws CouldNotPerformException;

    @RPCMethod
    public Boolean isSceneUnitRegistryReadOnly() throws CouldNotPerformException;

    @RPCMethod
    public Boolean isDalUnitConfigRegistryConsistent() throws CouldNotPerformException;

    @RPCMethod
    public Boolean isUserUnitRegistryConsistent() throws CouldNotPerformException;

    @RPCMethod
    public Boolean isAuthorizationGroupUnitRegistryConsistent() throws CouldNotPerformException;

    @RPCMethod
    public Boolean isDeviceUnitRegistryConsistent() throws CouldNotPerformException;

    @RPCMethod
    public Boolean isUnitGroupUnitRegistryConsistent() throws CouldNotPerformException;

    @RPCMethod
    public Boolean isLocationUnitRegistryConsistent() throws CouldNotPerformException;

    @RPCMethod
    public Boolean isConnectionUnitRegistryConsistent() throws CouldNotPerformException;

    @RPCMethod
    public Boolean isAgentUnitRegistryConsistent() throws CouldNotPerformException;

    @RPCMethod
    public Boolean isAppUnitRegistryConsistent() throws CouldNotPerformException;

    @RPCMethod
    public Boolean isSceneUnitRegistryConsistent() throws CouldNotPerformException;

    public void validateData() throws InvalidStateException;

    @RPCMethod
    public Future<ServiceTemplate> updateServiceTemplate(final ServiceTemplate serviceTemplate) throws CouldNotPerformException;

    @RPCMethod
    public Boolean containsServiceTemplate(final ServiceTemplate serviceTemplate) throws CouldNotPerformException;

    @RPCMethod
    public Boolean containsServiceTemplateById(final String serviceTemplateId) throws CouldNotPerformException;

    @RPCMethod
    public ServiceTemplate getServiceTemplateById(final String serviceTemplateId) throws CouldNotPerformException;

    public List<ServiceTemplate> getServiceTemplates() throws CouldNotPerformException;

    @RPCMethod
    public ServiceTemplate getServiceTemplateByType(final ServiceType type) throws CouldNotPerformException;

    @RPCMethod
    public Boolean isServiceTemplateRegistryReadOnly() throws CouldNotPerformException;

    @RPCMethod
    public Boolean isServiceTemplateRegistryConsistent() throws CouldNotPerformException;
}
