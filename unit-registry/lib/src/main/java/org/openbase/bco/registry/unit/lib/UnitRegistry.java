package org.openbase.bco.registry.unit.lib;

/*
 * #%L
 * BCO Registry Unit Library
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
 *
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */

import org.openbase.bco.registry.lib.provider.UnitConfigCollectionProvider;
import org.openbase.bco.registry.lib.util.UnitConfigProcessor;
import org.openbase.bco.registry.unit.lib.generator.UntShapeGenerator;
import org.openbase.bco.registry.unit.lib.provider.UnitTransformationProviderRegistry;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InvalidStateException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.exception.VerificationFailedException;
import org.openbase.jul.extension.rsb.scope.ScopeGenerator;
import org.openbase.jul.iface.Shutdownable;
import org.openbase.jul.iface.annotations.RPCMethod;
import org.openbase.jul.pattern.provider.DataProvider;
import rst.domotic.authentication.AuthenticatedValueType.AuthenticatedValue;
import rst.domotic.registry.UnitRegistryDataType.UnitRegistryData;
import rst.domotic.service.ServiceConfigType;
import rst.domotic.service.ServiceConfigType.ServiceConfig;
import rst.domotic.service.ServiceTemplateType.ServiceTemplate;
import rst.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import rst.domotic.unit.UnitConfigType.UnitConfig;
import rst.domotic.unit.UnitProbabilityCollectionType.UnitProbabilityCollection;
import rst.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;
import rst.domotic.unit.agent.AgentClassType.AgentClass;
import rst.domotic.unit.app.AppClassType.AppClass;
import rst.domotic.unit.location.LocationConfigType.LocationConfig.LocationType;
import rst.math.Vec3DDoubleType.Vec3DDouble;
import rst.rsb.ScopeType.Scope;
import rst.spatial.ShapeType.Shape;
import rst.tracking.PointingRay3DFloatCollectionType.PointingRay3DFloatCollection;
import rst.tracking.PointingRay3DFloatType.PointingRay3DFloat;

import javax.vecmath.Point3d;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

public interface UnitRegistry extends DataProvider<UnitRegistryData>, UnitTransformationProviderRegistry<UnitRegistryData>, UnitConfigCollectionProvider, Shutdownable {

    /**
     * The default radius used for the unit by coordinate lookup is set to 1 metre.
     */
    static final double DEFAULT_RADIUS = 1d;

    /**
     * This method registers the given unit config in the registry.
     *
     * @param unitConfig the unit config to register.
     *
     * @return the registered unit config with all applied consistency changes.
     *
     * @throws CouldNotPerformException is thrown if the entry already exists or results in an inconsistent registry
     */
    @RPCMethod
    Future<UnitConfig> registerUnitConfig(final UnitConfig unitConfig) throws CouldNotPerformException;

    @RPCMethod
    Future<AuthenticatedValue> registerUnitConfigAuthenticated(final AuthenticatedValue authenticatedValue) throws CouldNotPerformException;

    /**
     * Method updates the given unit config.
     *
     * @param unitConfig the updated unit config.
     *
     * @return the updated unit config with all applied consistency changes.
     *
     * @throws CouldNotPerformException is thrown if the request fails.
     */
    @RPCMethod
    Future<UnitConfig> updateUnitConfig(final UnitConfig unitConfig) throws CouldNotPerformException;

    @RPCMethod
    Future<AuthenticatedValue> updateUnitConfigAuthenticated(final AuthenticatedValue authenticatedValue) throws CouldNotPerformException;

    /**
     * Method removes the given unit config out of the global registry.
     *
     * @param unitConfig the unit config to remove.
     *
     * @return The removed unit config.
     *
     * @throws CouldNotPerformException is thrown if the removal fails.
     */
    @RPCMethod
    Future<UnitConfig> removeUnitConfig(final UnitConfig unitConfig) throws CouldNotPerformException;

    @RPCMethod
    Future<AuthenticatedValue> removeUnitConfigAuthenticated(final AuthenticatedValue authenticatedValue) throws CouldNotPerformException;

    /**
     * Method returns true if the unit config with the given id is
     * registered, otherwise false. The unit config id field is used for the
     * comparison.
     *
     * @param unitConfig the unit config used for the identification.
     *
     * @return true if the unit exists.
     *
     * @throws CouldNotPerformException is thrown if the check fails.
     */
    @RPCMethod
    Boolean containsUnitConfig(final UnitConfig unitConfig) throws CouldNotPerformException;

    @RPCMethod
    default Boolean containsUnitConfigByAlias(final String alias) throws CouldNotPerformException {
        try {
            getUnitConfigByAlias(alias);
        } catch (final NotAvailableException ex) {
            return false;
        }
        return true;
    }

    default List<UnitConfig> getUnitConfigsByServices(final ServiceType... serviceTypes) throws CouldNotPerformException {
        return getUnitConfigsByService(Arrays.asList(serviceTypes));
    }

    default List<UnitConfig> getUnitConfigsByService(final List<ServiceType> serviceTypes) throws CouldNotPerformException {
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
    default String getUnitScopeById(final String id) throws CouldNotPerformException {
        return ScopeGenerator.generateStringRep(getUnitConfigById(id).getScope());
    }

    @RPCMethod
    default String getUnitScopeByAlias(final String alias) throws CouldNotPerformException {
        return ScopeGenerator.generateStringRep(getUnitConfigByAlias(alias).getScope());
    }

    /**
     * Method returns true if the underling registry is marked as read only. A
     * registry is marked as read only in case of inconsistently data entries or
     * if the underling database is loaded out of a version tag.
     *
     * @return true if read only.
     *
     * @throws CouldNotPerformException is thrown if the check fails.
     */
    @RPCMethod
    Boolean isUnitConfigRegistryReadOnly() throws CouldNotPerformException;

    /**
     * Method returns the unit matching the given alias. An alias is an unique identifiere of units.
     * <p>
     * Hint: If you want to address more than one unit with an alias than create a unit group of such units and define an alias for those group.
     *
     * @param unitAlias the alias to identify the unit.
     *
     * @return the unit config referred by the alias.
     *
     * @throws NotAvailableException    is thrown if no unit is matching the given alias.
     * @throws CouldNotPerformException is thrown if something went wrong during the lookup.
     */
    @RPCMethod
    default UnitConfig getUnitConfigByAlias(final String unitAlias) throws CouldNotPerformException {
        validateData();
        for (UnitConfig unitConfig : getUnitConfigs()) {
            for (final String alias : unitConfig.getAliasList()) {
                if (alias.equalsIgnoreCase(unitAlias)) {
                    return unitConfig;
                }
            }
        }
        throw new NotAvailableException("UnitConfig", "alias:" + unitAlias);
    }

    /**
     * Method returns all registered units with the given label. Label resolving
     * is done case insensitive!
     * <p>
     * Note: PLEASE DO NOT USE THIS METHOD TO REQUEST DEVICES FOR THE CONTROLLING PURPOSE BECAUSE LABELS ARE NOT A STABLE IDENTIFIER! USE ID OR ALIAS INSTEAD!
     *
     * @param unitConfigLabel the label to identify a set of units.
     *
     * @throws CouldNotPerformException is thrown if the request fails.
     * @returna list of the requested unit configs.
     */
    List<UnitConfig> getUnitConfigsByLabel(final String unitConfigLabel) throws CouldNotPerformException;

    default List<UnitConfig> getUnitConfigsByLabelAndUnitType(final String unitConfigLabel, final UnitType unitType) throws CouldNotPerformException {
        validateData();
        List<UnitConfig> unitConfigs = Collections.synchronizedList(new ArrayList<>());
        getUnitConfigs().parallelStream().filter((unitConfig) -> (unitConfig.getType().equals(unitType) && unitConfig.getLabel().equalsIgnoreCase(unitConfigLabel))).forEach((unitConfig) -> {
            unitConfigs.add(unitConfig);
        });
        return unitConfigs;
    }

    /**
     * Method returns a list of all globally registered units of the given {@code type}.
     * <p>
     * Note: The type {@code UnitType.UNKNOWN} is used as wildcard and will return a list of all registered units.
     *
     * @param type the unit type to filter.
     *
     * @return a list of unit configurations.
     *
     * @throws CouldNotPerformException is thrown in case something goes wrong during the request.
     */
    default List<UnitConfig> getUnitConfigs(final UnitType type) throws CouldNotPerformException {
        validateData();
        List<UnitConfig> unitConfigs = new ArrayList<>();
        for (UnitConfig unitConfig : getUnitConfigs()) {
            if (type == UnitType.UNKNOWN || unitConfig.getType() == type || getSubUnitTypes(type).contains(unitConfig.getType())) {
                unitConfigs.add(unitConfig);
            }
        }
        return unitConfigs;
    }

    /**
     * Method returns a list of all globally registered dal units.
     * Dal units are all units introduced via the unit templates which are not base units.
     * Base units are units of the following types: LOCATION, CONNECTION, SCENE, AGENT, APP, DEVICE, USER, AUTHORIZATION_GROUP, UNIT_GROUP
     *
     * @return a list of dal units.
     *
     * @throws CouldNotPerformException is thrown in case something goes wrong during the request.
     */
    List<UnitConfig> getDalUnitConfigs() throws CouldNotPerformException;

    /**
     * Method returns a list of all globally registered base units.
     * Base units are units of the following types: LOCATION, CONNECTION, SCENE, AGENT, APP, DEVICE, USER, AUTHORIZATION_GROUP, UNIT_GROUP
     *
     * @return a list of base units.
     *
     * @throws CouldNotPerformException is thrown in case something goes wrong during the request.
     */
    List<UnitConfig> getBaseUnitConfigs() throws CouldNotPerformException;

    List<ServiceConfig> getServiceConfigs() throws CouldNotPerformException;

    List<ServiceConfig> getServiceConfigs(final ServiceTemplate.ServiceType serviceType) throws CouldNotPerformException;

    /**
     * Method returns true if the underling registry is marked as read only. A
     * registry is marked as read only in case of inconsistently data entries or
     * if the underling database is loaded out of a version tag.
     *
     * @return true if read only.
     *
     * @throws CouldNotPerformException is thrown if the check fails.
     */
    @RPCMethod
    Boolean isUnitGroupConfigRegistryReadOnly() throws CouldNotPerformException;

    List<UnitConfig> getUnitGroupUnitConfigsByUnitConfig(final UnitConfig unitConfig) throws CouldNotPerformException;

    List<UnitConfig> getUnitGroupUnitConfigsByUnitType(final UnitType type) throws CouldNotPerformException;

    List<UnitConfig> getUnitGroupUnitConfigsByServiceTypes(final List<ServiceType> serviceTypes) throws CouldNotPerformException;

    List<UnitConfig> getUnitConfigsByUnitGroupConfig(final UnitConfig groupConfig) throws CouldNotPerformException;

    List<UnitConfig> getUnitConfigsByUnitTypeAndServiceTypes(final UnitType type, final List<ServiceType> serviceTypes) throws CouldNotPerformException;

    /**
     * Method return the unit config which is registered for the given scope. A
     * NotAvailableException is thrown if no unit config is registered for the
     * given scope.
     *
     * @param scope
     *
     * @return the unit config matching the given scope.
     *
     * @throws CouldNotPerformException
     */
    @RPCMethod
    UnitConfig getUnitConfigByScope(final Scope scope) throws CouldNotPerformException;

    /**
     * Get all sub types of a unit type. E.g. COLORABLE_LIGHT and DIMMABLE_LIGHT are
     * sub types of LIGHT.
     *
     * @param type the super type whose sub types are searched
     *
     * @return all types of which the given type is a super type
     *
     * @throws CouldNotPerformException
     */
    List<UnitType> getSubUnitTypes(final UnitType type) throws CouldNotPerformException;

    /**
     * Get all super types of a unit type. E.g. DIMMABLE_LIGHT and LIGHT are
     * super types of COLORABLE_LIGHT.
     *
     * @param type the type whose super types are returned
     *
     * @return all super types of a given unit type
     *
     * @throws CouldNotPerformException
     */
    List<UnitType> getSuperUnitTypes(final UnitType type) throws CouldNotPerformException;

    default void verifyUnitGroupUnitConfig(UnitConfig unitConfig) throws VerificationFailedException {
        UnitConfigProcessor.verifyUnitConfig(unitConfig, UnitType.UNIT_GROUP);
    }

    /**
     * Method returns true if the underling registry is marked as read only. A
     * registry is marked as read only in case of inconsistently data entries or
     * if the underling database is loaded out of a version tag.
     *
     * @return true if read only.
     *
     * @throws CouldNotPerformException is thrown if the check fails.
     */
    @RPCMethod
    Boolean isDalUnitConfigRegistryReadOnly() throws CouldNotPerformException;

    /**
     * Method returns true if the underling registry is marked as read only. A
     * registry is marked as read only in case of inconsistently data entries or
     * if the underling database is loaded out of a version tag.
     *
     * @return true if read only.
     *
     * @throws CouldNotPerformException is thrown if the check fails.
     */
    @RPCMethod
    Boolean isUserUnitRegistryReadOnly() throws CouldNotPerformException;

    /**
     * Method returns true if the underling registry is marked as read only. A
     * registry is marked as read only in case of inconsistently data entries or
     * if the underling database is loaded out of a version tag.
     *
     * @return true if read only.
     *
     * @throws CouldNotPerformException is thrown if the check fails.
     */
    @RPCMethod
    Boolean isAuthorizationGroupUnitRegistryReadOnly() throws CouldNotPerformException;

    /**
     * Method returns true if the underling registry is marked as read only. A
     * registry is marked as read only in case of inconsistently data entries or
     * if the underling database is loaded out of a version tag.
     *
     * @return true if read only.
     *
     * @throws CouldNotPerformException is thrown if the check fails.
     */
    @RPCMethod
    Boolean isDeviceUnitRegistryReadOnly() throws CouldNotPerformException;

    /**
     * Method returns true if the underling registry is marked as read only. A
     * registry is marked as read only in case of inconsistently data entries or
     * if the underling database is loaded out of a version tag.
     *
     * @return true if read only.
     *
     * @throws CouldNotPerformException is thrown if the check fails.
     */
    @RPCMethod
    Boolean isUnitGroupUnitRegistryReadOnly() throws CouldNotPerformException;

    /**
     * Method returns true if the underling registry is marked as read only. A
     * registry is marked as read only in case of inconsistently data entries or
     * if the underling database is loaded out of a version tag.
     *
     * @return true if read only.
     *
     * @throws CouldNotPerformException is thrown if the check fails.
     */
    @RPCMethod
    Boolean isLocationUnitRegistryReadOnly() throws CouldNotPerformException;

    /**
     * Method returns true if the underling registry is marked as read only. A
     * registry is marked as read only in case of inconsistently data entries or
     * if the underling database is loaded out of a version tag.
     *
     * @return true if read only.
     *
     * @throws CouldNotPerformException is thrown if the check fails.
     */
    @RPCMethod
    Boolean isConnectionUnitRegistryReadOnly() throws CouldNotPerformException;

    /**
     * Method returns true if the underling registry is marked as read only. A
     * registry is marked as read only in case of inconsistently data entries or
     * if the underling database is loaded out of a version tag.
     *
     * @return true if read only.
     *
     * @throws CouldNotPerformException is thrown if the check fails.
     */
    @RPCMethod
    Boolean isAgentUnitRegistryReadOnly() throws CouldNotPerformException;

    /**
     * Method returns true if the underling registry is marked as read only. A
     * registry is marked as read only in case of inconsistently data entries or
     * if the underling database is loaded out of a version tag.
     *
     * @return true if read only.
     *
     * @throws CouldNotPerformException is thrown if the check fails.
     */
    @RPCMethod
    Boolean isAppUnitRegistryReadOnly() throws CouldNotPerformException;

    /**
     * Method returns true if the underling registry is marked as read only. A
     * registry is marked as read only in case of inconsistently data entries or
     * if the underling database is loaded out of a version tag.
     *
     * @return true if read only.
     *
     * @throws CouldNotPerformException is thrown if the check fails.
     */
    @RPCMethod
    Boolean isSceneUnitRegistryReadOnly() throws CouldNotPerformException;

    /**
     * Method returns true if the underling registry is marked as consistent.
     *
     * @return true if consistent
     *
     * @throws CouldNotPerformException if the check fails
     */
    @RPCMethod
    Boolean isUnitConfigRegistryConsistent() throws CouldNotPerformException;

    /**
     * Method returns true if the underling registry is marked as consistent.
     *
     * @return true if consistent
     *
     * @throws CouldNotPerformException if the check fails
     */
    @RPCMethod
    Boolean isUnitGroupConfigRegistryConsistent() throws CouldNotPerformException;

    /**
     * Method returns true if the underling registry is marked as consistent.
     *
     * @return true if consistent
     *
     * @throws CouldNotPerformException if the check fails
     */
    @RPCMethod
    Boolean isDalUnitConfigRegistryConsistent() throws CouldNotPerformException;

    /**
     * Method returns true if the underling registry is marked as consistent.
     *
     * @return true if consistent
     *
     * @throws CouldNotPerformException if the check fails
     */
    @RPCMethod
    Boolean isUserUnitRegistryConsistent() throws CouldNotPerformException;

    /**
     * Method returns true if the underling registry is marked as consistent.
     *
     * @return true if consistent
     *
     * @throws CouldNotPerformException if the check fails
     */
    @RPCMethod
    Boolean isAuthorizationGroupUnitRegistryConsistent() throws CouldNotPerformException;

    /**
     * Method returns true if the underling registry is marked as consistent.
     *
     * @return true if consistent
     *
     * @throws CouldNotPerformException if the check fails
     */
    @RPCMethod
    Boolean isDeviceUnitRegistryConsistent() throws CouldNotPerformException;

    /**
     * Method returns true if the underling registry is marked as consistent.
     *
     * @return true if consistent
     *
     * @throws CouldNotPerformException if the check fails
     */
    @RPCMethod
    Boolean isUnitGroupUnitRegistryConsistent() throws CouldNotPerformException;

    /**
     * Method returns true if the underling registry is marked as consistent.
     *
     * @return true if consistent
     *
     * @throws CouldNotPerformException if the check fails
     */
    @RPCMethod
    Boolean isLocationUnitRegistryConsistent() throws CouldNotPerformException;

    /**
     * Method returns true if the underling registry is marked as consistent.
     *
     * @return true if consistent
     *
     * @throws CouldNotPerformException if the check fails
     */
    @RPCMethod
    Boolean isConnectionUnitRegistryConsistent() throws CouldNotPerformException;

    /**
     * Method returns true if the underling registry is marked as consistent.
     *
     * @return true if consistent
     *
     * @throws CouldNotPerformException if the check fails
     */
    @RPCMethod
    Boolean isAgentUnitRegistryConsistent() throws CouldNotPerformException;

    /**
     * Method returns true if the underling registry is marked as consistent.
     *
     * @return true if consistent
     *
     * @throws CouldNotPerformException if the check fails
     */
    @RPCMethod
    Boolean isAppUnitRegistryConsistent() throws CouldNotPerformException;

    /**
     * Method returns true if the underling registry is marked as consistent.
     *
     * @return true if consistent
     *
     * @throws CouldNotPerformException if the check fails
     */
    @RPCMethod
    Boolean isSceneUnitRegistryConsistent() throws CouldNotPerformException;

    void validateData() throws InvalidStateException;

    @RPCMethod
    Future<ServiceTemplate> updateServiceTemplate(final ServiceTemplate serviceTemplate) throws CouldNotPerformException;

    @RPCMethod
    Boolean containsServiceTemplate(final ServiceTemplate serviceTemplate) throws CouldNotPerformException;

    @RPCMethod
    Boolean containsServiceTemplateById(final String serviceTemplateId) throws CouldNotPerformException;

    @RPCMethod
    ServiceTemplate getServiceTemplateById(final String serviceTemplateId) throws CouldNotPerformException;

    List<ServiceTemplate> getServiceTemplates() throws CouldNotPerformException;

    @RPCMethod
    ServiceTemplate getServiceTemplateByType(final ServiceType type) throws CouldNotPerformException;

    @RPCMethod
    Boolean isServiceTemplateRegistryReadOnly() throws CouldNotPerformException;

    @RPCMethod
    Boolean isServiceTemplateRegistryConsistent() throws CouldNotPerformException;

    default List<UnitConfig> getUnitConfigsByCoordinate(final Vec3DDouble coordinate) throws CouldNotPerformException {
        return getUnitConfigsByCoordinate(coordinate, DEFAULT_RADIUS, UnitType.UNKNOWN);
    }

    default List<UnitConfig> getUnitConfigsByCoordinate(final Vec3DDouble coordinate, final double radius) throws CouldNotPerformException {
        return getUnitConfigsByCoordinate(coordinate, radius, UnitType.UNKNOWN);
    }

    /**
     * Method returns a list of {@code UnitConfig} instances sorted by the distance to the given {@code coordinate} starting with the lowest one.
     * The lookup time can be reduced by filtering the results with a {@code UnitType} where the {@code UnitType.UNKNOWN} is used as wildcard.
     * The given radius can be used to limit the result as well but will not speed up the lookup.
     *
     * @param coordinate
     * @param radius
     * @param unitType
     *
     * @return
     *
     * @throws CouldNotPerformException
     */
    default List<UnitConfig> getUnitConfigsByCoordinate(final Vec3DDouble coordinate, final double radius, final UnitType unitType) throws CouldNotPerformException {

        // init
        TreeMap<Double, UnitConfig> result = new TreeMap<>();
        final Point3d unitPosition = new Point3d(coordinate.getX(), coordinate.getY(), coordinate.getZ());

        // lookup distances
        for (final UnitConfig unitConfig : getUnitConfigs(unitType)) {
            final double distance = unitPosition.distance(getUnitPositionGlobalPoint3d(unitConfig));
            if (distance <= radius) {
                result.put(radius, unitConfig);
            }
        }
        return new ArrayList<>(result.values());
    }

    /**
     * Retrieves a user ID according to a given user name.
     * If multiple users happen to have the same user name, the first one is returned.
     *
     * @param userName
     *
     * @return User ID
     *
     * @throws CouldNotPerformException
     * @throws NotAvailableException    If no user with the given user name could be found.
     */
    @RPCMethod
    String getUserUnitIdByUserName(final String userName) throws CouldNotPerformException, NotAvailableException;

    /**
     * Method returns all location unit configs which are of the given location type.
     *
     * @param locationType the type of the location.
     *
     * @return a list of the requested unit configs.
     *
     * @throws CouldNotPerformException is thrown if the request fails.
     */
    default List<UnitConfig> getLocationUnitConfigsByType(final LocationType locationType) throws CouldNotPerformException {
        return getUnitConfigs(UnitType.LOCATION)
                .stream()
                .filter(locationUnitConfig -> locationUnitConfig.getLocationConfig().getType() == locationType)
                .collect(Collectors.toList());
    }

    /**
     * Method returns all the locations which contain the given coordinate.
     *
     * @param coordinate
     *
     * @return a list of the requested unit configs.
     *
     * @throws CouldNotPerformException                is thrown if the request fails.
     * @throws java.lang.InterruptedException
     * @throws java.util.concurrent.ExecutionException
     */
    default List<UnitConfig> getLocationUnitConfigsByCoordinate(final Vec3DDouble coordinate) throws CouldNotPerformException, InterruptedException, ExecutionException {
        return getLocationUnitConfigsByCoordinate(coordinate, LocationType.UNKNOWN);
    }

    /**
     * Method returns all the locations which contain the given coordinate and
     * belong to the given location type.
     *
     * @param coordinate
     * @param locationType
     *
     * @return a list of the requested unit configs.
     *
     * @throws CouldNotPerformException                is thrown if the request fails.
     * @throws java.lang.InterruptedException
     * @throws java.util.concurrent.ExecutionException
     */
    List<UnitConfig> getLocationUnitConfigsByCoordinate(final Vec3DDouble coordinate, LocationType locationType) throws CouldNotPerformException, InterruptedException, ExecutionException;

    /**
     * Method returns all unit configurations which are direct or recursive related to the given location id.
     *
     * @param locationId the id of the location which provides the units.
     *
     * @return A collection of unit configs.
     *
     * @throws CouldNotPerformException is thrown if the request fails.
     */
    List<UnitConfig> getUnitConfigsByLocation(final String locationId) throws CouldNotPerformException;

    /**
     * Method returns all unit configurations which are direct related to the given location id.
     * In case the {@code recursive} flag is set to true than recursive related units are included as well.
     *
     * @param locationId the id of the location which provides the units.
     * @param recursive  defines if recursive related unit should be included as well.
     *
     * @return A collection of unit configs.
     *
     * @throws CouldNotPerformException is thrown if the request fails.
     */
    List<UnitConfig> getUnitConfigsByLocation(final String locationId, final boolean recursive) throws CouldNotPerformException;

    /**
     * Method returns all unit configurations which are direct or recursive
     * related to the given location id and an instance of the given unit type.
     * Label resolving is done case insensitive!
     *
     * @param type
     * @param locationConfigId
     *
     * @return A collection of unit configs.
     *
     * @throws CouldNotPerformException is thrown if the request fails.
     * @throws NotAvailableException
     */
    List<UnitConfig> getUnitConfigsByLocation(final UnitType type, final String locationConfigId) throws CouldNotPerformException, NotAvailableException;

    /**
     * Method returns all unit configurations which are direct or recursive
     * related to the given location id and an implement the given service type.
     *
     * @param type             service type filter.
     * @param locationConfigId related location.
     *
     * @return A collection of unit configs.
     *
     * @throws CouldNotPerformException is thrown if the request fails.
     * @throws NotAvailableException
     */
    List<UnitConfig> getUnitConfigsByLocation(final ServiceType type, final String locationConfigId) throws CouldNotPerformException, NotAvailableException;

    /**
     * Method returns all service configurations which are direct or recursive
     * related to the given location id.
     *
     * @param locationId
     *
     * @return the list of service configurations.
     *
     * @throws CouldNotPerformException is thrown if the request fails.
     * @throws NotAvailableException    is thrown if the given location config id
     *                                  is unknown.
     */
    List<ServiceConfig> getServiceConfigsByLocation(final String locationId) throws CouldNotPerformException;

    /**
     * Method returns all unit configurations which are direct or recursive
     * related to the given location label which can represent more than one
     * location. Label resolving is done case insensitive!
     *
     * @param locationLabel
     *
     * @return A collection of unit configs.
     *
     * @throws CouldNotPerformException is thrown if the request fails.
     * @throws NotAvailableException
     */
    List<UnitConfig> getUnitConfigsByLocationLabel(final String locationLabel) throws CouldNotPerformException;

    /**
     * Method returns all unit configurations with a given type which are direct
     * or recursive related to the given location label which can represent more
     * than one location. Label resolving is done case insensitive!
     *
     * @param unitType
     * @param locationLabel
     *
     * @return A collection of unit configs.
     *
     * @throws CouldNotPerformException is thrown if the request fails.
     * @throws NotAvailableException
     */
    List<UnitConfig> getUnitConfigsByLocationLabel(final UnitType unitType, final String locationLabel) throws CouldNotPerformException;

    /**
     * Method returns a collection of unit configs which are located within the
     * defined location and match the given unit label. Label resolving is done
     * case insensitive!
     *
     * @param unitLabel
     * @param locationId
     *
     * @return
     *
     * @throws CouldNotPerformException is thrown if the request fails.
     */
    List<UnitConfig> getUnitConfigsByLabelAndLocation(final String unitLabel, final String locationId) throws CouldNotPerformException;

    /**
     * Method returns all unit configurations which are direct or recursive
     * related to the given location alias which can represent more than one
     * location. Alias resolving is done case insensitive!
     *
     * @param locationAlias
     *
     * @return A collection of unit configs.
     *
     * @throws CouldNotPerformException is thrown if the request fails.
     * @throws NotAvailableException
     */
    List<UnitConfig> getUnitConfigsByLocationAlias(final String locationAlias) throws CouldNotPerformException;

    /**
     * Method returns all unit configurations with a given type which are direct
     * or recursive related to the given location alias which can represent more
     * than one location. Alias resolving is done case insensitive!
     *
     * @param unitType
     * @param locationAlias
     *
     * @return A collection of unit configs.
     *
     * @throws CouldNotPerformException is thrown if the request fails.
     * @throws NotAvailableException
     */
    List<UnitConfig> getUnitConfigsByLocationAlias(final UnitType unitType, final String locationAlias) throws CouldNotPerformException;

    /**
     * Method returns a collection of unit configs which are located within the
     * defined location and match the given unit alias. Alias resolving is done
     * case insensitive!
     *
     * @param unitAlias
     * @param locationId
     *
     * @return
     *
     * @throws CouldNotPerformException is thrown if the request fails.
     */
    List<UnitConfig> getUnitConfigsByAliasAndLocation(final String unitAlias, final String locationId) throws CouldNotPerformException;

    /**
     * Method generates a list of service types supported by the given location.
     *
     * @param locationId the location to filter the types.
     *
     * @return a list of supported service types.
     *
     * @throws NotAvailableException is thrown in case the list could not be computed.
     */
    default Set<ServiceType> getServiceTypesByLocation(final String locationId) throws CouldNotPerformException {
        final Set<ServiceType> serviceTypeSet = new HashSet<>();
        for (final UnitConfig unitConfig : getUnitConfigsByLocation(locationId)) {
            for (final ServiceConfig serviceConfig : unitConfig.getServiceConfigList()) {
                serviceTypeSet.add(serviceConfig.getServiceDescription().getType());
            }
        }
        return serviceTypeSet;
    }

    /**
     * Method returns all unit configurations which are related to the given
     * connection id.
     *
     * @param connectionConfigId
     *
     * @return A collection of unit configs.
     *
     * @throws CouldNotPerformException is thrown if the request fails.
     */
    List<UnitConfig> getUnitConfigsByConnection(final String connectionConfigId) throws CouldNotPerformException;

    /**
     * Method returns all unit configurations which are related to the given
     * connection id and an instance of the given unit type.
     *
     * @param type
     * @param connectionConfigId
     *
     * @return A collection of unit configs.
     *
     * @throws CouldNotPerformException is thrown if the request fails.
     * @throws NotAvailableException
     */
    List<UnitConfig> getUnitConfigsByConnection(final UnitType type, final String connectionConfigId) throws CouldNotPerformException, NotAvailableException;

    /**
     * Method returns all unit configurations which are related to the given
     * connection id and an implement the given service type.
     *
     * @param type               service type filter.
     * @param connectionConfigId related connection.
     *
     * @return A collection of unit configs.
     *
     * @throws CouldNotPerformException is thrown if the request fails.
     * @throws NotAvailableException
     */
    List<UnitConfig> getUnitConfigsByConnection(final ServiceType type, final String connectionConfigId) throws CouldNotPerformException, NotAvailableException;

    /**
     * Method returns all service configurations which are related to the given
     * connection id.
     *
     * @param connectionConfigId
     *
     * @return the list of service configurations.
     *
     * @throws CouldNotPerformException is thrown if the request fails.
     * @throws NotAvailableException    is thrown if the given connection config id
     *                                  is unknown.
     */
    List<ServiceConfig> getServiceConfigsByConnection(final String connectionConfigId) throws CouldNotPerformException;

    /**
     * Method returns all neighbor tiles for a tile. If the given locationId
     * does not belong to a tile, the could not perform exception is thrown.
     *
     * @param locationId the id of the location which neighbors you want to get
     *
     * @return all neighbor tiles
     *
     * @throws CouldNotPerformException is thrown if the request fails.
     */
    List<UnitConfig> getNeighborLocations(final String locationId) throws CouldNotPerformException;

    /**
     * Method returns a list of probably intersected units by the given 3D ray.
     * This could for example be useful for selecting units by pointing gestures.
     *
     * @param pointingRay3DFloat ray which probably intersects with a specific unit priorized by a given certainty.
     *
     * @return a collection of probably intersected units referred by there id.
     *
     * @throws CouldNotPerformException is thrown in case the computation could not be performed.
     */
    @RPCMethod
    Future<UnitProbabilityCollection> computeUnitIntersection(final PointingRay3DFloat pointingRay3DFloat) throws CouldNotPerformException;

    /**
     * Method returns a list of probably intersected units by the given 3D rays.
     * This could for example be useful for selecting units by pointing gestures.
     *
     * @param pointingRay3DFloatCollection a collection of rays which probably intersects with a specific unit priorized by a given certainty.
     *
     * @return a collection of probably intersected units referred by there id.
     *
     * @throws CouldNotPerformException is thrown in case the computation could not be performed.
     */
    @RPCMethod
    Future<UnitProbabilityCollection> computeUnitIntersection(final PointingRay3DFloatCollection pointingRay3DFloatCollection) throws CouldNotPerformException;

    @Override
    default Shape getUnitShape(final UnitConfig unitConfig) throws NotAvailableException {
        try {
            return UntShapeGenerator.generateUnitShape(unitConfig, this, CachedClassRegistryRemote.getRegistry());
        } catch (InterruptedException ex) {
            // because registries should not throw interrupted exceptions in a future release this exception is already transformed into a NotAvailableException.
            Thread.currentThread().interrupt();
            throw new NotAvailableException("UnitShape", new CouldNotPerformException("Shutdown in progress"));
        }
    }

    List<UnitConfig> getAgentUnitConfigsByAgentClass(final AgentClass agentClass) throws CouldNotPerformException;

    List<UnitConfig> getAgentUnitConfigsByAgentClassId(final String agentClassId) throws CouldNotPerformException;

    List<UnitConfig> getAppUnitConfigsByAppClass(final AppClass appClass) throws CouldNotPerformException, InterruptedException;

    List<UnitConfig> getAppUnitConfigsByAppClassId(final String appClassId) throws CouldNotPerformException, InterruptedException;

}
