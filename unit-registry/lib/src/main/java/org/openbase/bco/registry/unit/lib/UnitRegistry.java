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
import java.util.List;
import java.util.concurrent.Future;
import org.openbase.bco.registry.lib.util.UnitConfigProcessor;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.VerificationFailedException;
import org.openbase.jul.iface.Shutdownable;
import rst.domotic.service.ServiceConfigType.ServiceConfig;
import rst.domotic.service.ServiceTemplateType.ServiceTemplate;
import rst.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import rst.domotic.unit.UnitConfigType.UnitConfig;
import rst.domotic.unit.UnitTemplateType.UnitTemplate;
import rst.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;
import rst.rsb.ScopeType.Scope;

public interface UnitRegistry extends Shutdownable {

    public Future<UnitConfig> registerUnitConfig(final UnitConfig unitConfig) throws CouldNotPerformException;

    public Future<UnitConfig> updateUnitConfig(final UnitConfig unitConfig) throws CouldNotPerformException;

    public Future<UnitConfig> removeUnitConfig(final UnitConfig unitConfig) throws CouldNotPerformException;

    public Boolean containsUnitConfig(final UnitConfig unitConfig) throws CouldNotPerformException;

    public Boolean containsUnitConfigById(final String unitConfigId) throws CouldNotPerformException;

    public UnitConfig getUnitConfigById(final String unitConfigId) throws CouldNotPerformException;

    public List<UnitConfig> getUnitConfigs() throws CouldNotPerformException;

    public Boolean isUnitConfigRegistryReadOnly() throws CouldNotPerformException;

    /**
     * Method returns true if the underling registry is marked as consistent.
     *
     * @return if the UnitConfig registry is consistent
     * @throws CouldNotPerformException if the check fails
     */
    public Boolean isUnitConfigRegistryConsistent() throws CouldNotPerformException;

    public Future<UnitTemplate> updateUnitTemplate(final UnitTemplate unitTemplate) throws CouldNotPerformException;

    public Boolean containsUnitTemplate(final UnitTemplate unitTemplate) throws CouldNotPerformException;

    public Boolean containsUnitTemplateById(final String unitTemplateId) throws CouldNotPerformException;

    public UnitTemplate getUnitTemplateById(final String unitTemplate) throws CouldNotPerformException;

    public List<UnitTemplate> getUnitTemplates() throws CouldNotPerformException;

    public UnitTemplate getUnitTemplateByType(final UnitType type) throws CouldNotPerformException;

    public Boolean isUnitTemplateRegistryReadOnly() throws CouldNotPerformException;

    public Boolean isUnitTemplateRegistryConsistent() throws CouldNotPerformException;

    public Future<UnitConfig> registerUnitGroupConfig(final UnitConfig groupConfig) throws CouldNotPerformException;

    public Future<UnitConfig> updateUnitGroupConfig(final UnitConfig groupConfig) throws CouldNotPerformException;

    public Future<UnitConfig> removeUnitGroupConfig(final UnitConfig groupConfig) throws CouldNotPerformException;

    public Boolean containsUnitGroupConfig(final UnitConfig groupConfig) throws CouldNotPerformException;

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

    public Boolean isUnitGroupConfigRegistryReadOnly() throws CouldNotPerformException;

    public Boolean isUnitGroupConfigRegistryConsistent() throws CouldNotPerformException;

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
    public UnitConfig getUnitConfigByScope(final Scope scope) throws CouldNotPerformException;

    public List<UnitType> getSubUnitTypesOfUnitType(final UnitType type) throws CouldNotPerformException;

    public default void verifyUnitGroupUnitConfig(UnitConfig unitConfig) throws VerificationFailedException {
        UnitConfigProcessor.verifyUnitConfig(unitConfig, UnitType.UNIT_GROUP);
    }

    public Boolean isDalUnitConfigRegistryReadOnly() throws CouldNotPerformException;

    public Boolean isUserUnitRegistryReadOnly() throws CouldNotPerformException;

    public Boolean isAuthorizationGroupUnitRegistryReadOnly() throws CouldNotPerformException;

    public Boolean isDeviceUnitRegistryReadOnly() throws CouldNotPerformException;

    public Boolean isUnitGroupUnitRegistryReadOnly() throws CouldNotPerformException;

    public Boolean isLocationUnitRegistryReadOnly() throws CouldNotPerformException;

    public Boolean isConnectionUnitRegistryReadOnly() throws CouldNotPerformException;

    public Boolean isAgentUnitRegistryReadOnly() throws CouldNotPerformException;

    public Boolean isAppUnitRegistryReadOnly() throws CouldNotPerformException;

    public Boolean isSceneUnitRegistryReadOnly() throws CouldNotPerformException;

    public Boolean isDalUnitConfigRegistryConsistent() throws CouldNotPerformException;

    public Boolean isUserUnitRegistryConsistent() throws CouldNotPerformException;

    public Boolean isAuthorizationGroupUnitRegistryConsistent() throws CouldNotPerformException;

    public Boolean isDeviceUnitRegistryConsistent() throws CouldNotPerformException;

    public Boolean isUnitGroupUnitRegistryConsistent() throws CouldNotPerformException;

    public Boolean isLocationUnitRegistryConsistent() throws CouldNotPerformException;

    public Boolean isConnectionUnitRegistryConsistent() throws CouldNotPerformException;

    public Boolean isAgentUnitRegistryConsistent() throws CouldNotPerformException;

    public Boolean isAppUnitRegistryConsistent() throws CouldNotPerformException;

    public Boolean isSceneUnitRegistryConsistent() throws CouldNotPerformException;
}
