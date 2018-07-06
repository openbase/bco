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

import org.apache.commons.math3.geometry.euclidean.twod.PolygonsSet;
import org.apache.commons.math3.geometry.euclidean.twod.Vector2D;
import org.apache.commons.math3.geometry.partitioning.Region.Location;
import org.openbase.bco.registry.clazz.remote.CachedClassRegistryRemote;
import org.openbase.bco.registry.lib.provider.UnitConfigCollectionProvider;
import org.openbase.bco.registry.lib.util.UnitConfigProcessor;
import org.openbase.bco.registry.template.remote.CachedTemplateRegistryRemote;
import org.openbase.bco.registry.unit.lib.provider.UnitTransformationProviderRegistry;
import org.openbase.jul.annotation.RPCMethod;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.exception.VerificationFailedException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.extension.rsb.scope.ScopeGenerator;
import org.openbase.jul.extension.rst.processing.LabelProcessor;
import org.openbase.jul.iface.Shutdownable;
import org.openbase.jul.pattern.provider.DataProvider;
import org.openbase.jul.storage.registry.RegistryService;
import org.slf4j.LoggerFactory;
import rst.domotic.authentication.AuthenticatedValueType.AuthenticatedValue;
import rst.domotic.registry.UnitRegistryDataType.UnitRegistryData;
import rst.domotic.service.ServiceConfigType;
import rst.domotic.service.ServiceConfigType.ServiceConfig;
import rst.domotic.service.ServiceDescriptionType.ServiceDescription;
import rst.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import rst.domotic.unit.UnitConfigType.UnitConfig;
import rst.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;
import rst.domotic.unit.agent.AgentClassType.AgentClass;
import rst.domotic.unit.app.AppClassType.AppClass;
import rst.domotic.unit.location.LocationConfigType.LocationConfig.LocationType;
import rst.math.Vec3DDoubleType;
import rst.math.Vec3DDoubleType.Vec3DDouble;
import rst.rsb.ScopeType.Scope;

import javax.media.j3d.Transform3D;
import javax.vecmath.Point3d;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

public interface UnitRegistry extends DataProvider<UnitRegistryData>, UnitTransformationProviderRegistry<UnitRegistryData>, UnitConfigCollectionProvider, Shutdownable, RegistryService {

    /**
     * This alias can be used for fast lookups of the admin authorization group.
     */
    String ADMIN_GROUP_ALIAS = "AdminGroup";
    /**
     * This alias can be used for fast lookups of the bco authorization group.
     */
    String BCO_GROUP_ALIAS = "BCOGroup";
    /**
     * This alias can be used for fast lookups of the admin user.
     */
    String ADMIN_USER_ALIAS = "AdminUser";
    /**
     * This alias can be used for fast lookups of the bco user.
     */
    String BCO_USER_ALIAS = "BCOUser";

    /**
     * The default radius used for the unit by coordinate lookup is set to 1 metre.
     */
    double DEFAULT_RADIUS = 1d;

    /**
     * This method registers the given unit config in the registry.
     *
     * @param unitConfig the unit config to register.
     * @return the registered unit config with all applied consistency changes.
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
     * @return the updated unit config with all applied consistency changes.
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
     * @return The removed unit config.
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
     * @return true if the unit exists.
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
                    if (serviceConfig.getServiceDescription().getServiceType() == serviceType) {
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
     * @throws CouldNotPerformException is thrown if the check fails.
     */
    @RPCMethod
    Boolean isUnitConfigRegistryReadOnly() throws CouldNotPerformException;

    /**
     * Method returns the unit matching the given alias. An alias is a unique identifier of units.
     * <p>
     * Hint: If you want to address more than one unit with an alias than create a unit group of such units and define an alias for those group.
     *
     * @param unitAlias the alias to identify the unit.
     * @return the unit config referred by the alias.
     * @throws NotAvailableException    is thrown if no unit is matching the given alias.
     * @throws CouldNotPerformException is thrown if something went wrong during the lookup.
     */
    @RPCMethod
    UnitConfig getUnitConfigByAlias(final String unitAlias) throws CouldNotPerformException;

    /**
     * Method returns all registered units with the given label. Label resolving
     * is done case insensitive!
     * <p>
     * Note: PLEASE DO NOT USE THIS METHOD TO REQUEST DEVICES FOR THE CONTROLLING PURPOSE BECAUSE LABELS ARE NOT A STABLE IDENTIFIER! USE ID OR ALIAS INSTEAD!
     *
     * @param unitConfigLabel the label to identify a set of units.
     * @return a list of the requested unit configs.
     * @throws CouldNotPerformException is thrown if the request fails.
     */
    default List<UnitConfig> getUnitConfigsByLabel(final String unitConfigLabel) throws CouldNotPerformException {
        List<UnitConfig> unitConfigs = Collections.synchronizedList(new ArrayList<>());
        getUnitConfigs().parallelStream().filter((unitConfig) -> (LabelProcessor.contains(unitConfig.getLabel(), unitConfigLabel))).forEach((unitConfig) -> {
            unitConfigs.add(unitConfig);
        });
        return unitConfigs;
    }

    default List<UnitConfig> getUnitConfigsByLabelAndUnitType(final String unitConfigLabel, final UnitType unitType) throws CouldNotPerformException {
        List<UnitConfig> unitConfigs = Collections.synchronizedList(new ArrayList<>());
        getUnitConfigs().parallelStream().filter((unitConfig) -> (unitConfig.getUnitType().equals(unitType) && LabelProcessor.contains(unitConfig.getLabel(), unitConfigLabel))).forEach((unitConfig) -> {
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
     * @return a list of unit configurations.
     * @throws CouldNotPerformException is thrown in case something goes wrong during the request.
     */
    default List<UnitConfig> getUnitConfigs(final UnitType type) throws CouldNotPerformException {
        validateData();
        List<UnitConfig> unitConfigs = new ArrayList<>();
        for (UnitConfig unitConfig : getUnitConfigs()) {
            if (type == UnitType.UNKNOWN || unitConfig.getUnitType() == type || CachedTemplateRegistryRemote.getRegistry().getSubUnitTypes(type).contains(unitConfig.getUnitType())) {
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
     * @throws CouldNotPerformException is thrown in case something goes wrong during the request.
     */
    List<UnitConfig> getDalUnitConfigs() throws CouldNotPerformException;

    /**
     * Method returns a list of all globally registered base units.
     * Base units are units of the following types: LOCATION, CONNECTION, SCENE, AGENT, APP, DEVICE, USER, AUTHORIZATION_GROUP, UNIT_GROUP
     *
     * @return a list of base units.
     * @throws CouldNotPerformException is thrown in case something goes wrong during the request.
     */
    List<UnitConfig> getBaseUnitConfigs() throws CouldNotPerformException;

    /**
     * @return a list containing all service configs of all units.
     * @throws CouldNotPerformException is thrown if the config list could not be generated.
     */
    default List<ServiceConfig> getServiceConfigs() throws CouldNotPerformException {
        final List<ServiceConfig> serviceConfigs = new ArrayList<>();
        for (UnitConfig unitConfig : getUnitConfigs()) {
            serviceConfigs.addAll(unitConfig.getServiceConfigList());
        }
        return serviceConfigs;
    }

    /**
     * Method returns all service configs of all units filtered by the given {@code serviceType}.
     *
     * @param serviceType the service type to filter.
     * @return a list of service configs matching the given {@code serviceType}.
     * @throws CouldNotPerformException is thrown if the config list could not be generated.
     */
    default List<ServiceConfig> getServiceConfigs(final ServiceType serviceType) throws CouldNotPerformException {
        List<ServiceConfig> serviceConfigs = new ArrayList<>();
        for (UnitConfig unitConfig : getUnitConfigs()) {
            for (ServiceConfig serviceConfig : unitConfig.getServiceConfigList()) {
                if (serviceConfig.getServiceDescription().getServiceType() == serviceType) {
                    serviceConfigs.add(serviceConfig);
                }
            }
        }
        return serviceConfigs;
    }

    /**
     * Method returns true if the underling registry is marked as read only. A
     * registry is marked as read only in case of inconsistently data entries or
     * if the underling database is loaded out of a version tag.
     *
     * @return true if read only.
     * @throws CouldNotPerformException is thrown if the check fails.
     */
    @RPCMethod
    Boolean isUnitGroupConfigRegistryReadOnly() throws CouldNotPerformException;

    /**
     * Method returns all unit configs filtered by the given {@code unitType} and {@code serviceType}.
     *
     * @param unitType    the unit type to filter.
     * @param serviceType the service type to filter.
     * @return a list of unit types matching the given unit and service type.
     * @throws CouldNotPerformException is thrown if the list could not be generated.
     */
    default List<UnitConfig> getUnitConfigsByUnitTypeAndServiceTypes(final UnitType unitType, final ServiceType serviceType) throws CouldNotPerformException {
        final List<UnitConfig> unitConfigs = getUnitConfigs(unitType);
        boolean foundServiceType;

        for (final UnitConfig unitConfig : new ArrayList<>(unitConfigs)) {
            foundServiceType = false;
            for (final ServiceConfig serviceConfig : unitConfig.getServiceConfigList()) {
                if (serviceConfig.getServiceDescription().getServiceType() == serviceType) {
                    foundServiceType = true;
                }
            }
            if (!foundServiceType) {
                unitConfigs.remove(unitConfig);
            }
        }
        return unitConfigs;
    }

    /**
     * Method returns all unit configs filtered by the given {@code unitType} and {@code serviceTypes}.
     *
     * @param unitType     the unit type to filter.
     * @param serviceTypes a list of service types to filter.
     * @return a list of unit types matching the given unit and service type.
     * @throws CouldNotPerformException is thrown if the list could not be generated.
     */
    default List<UnitConfig> getUnitConfigsByUnitTypeAndServiceTypes(final UnitType unitType, final List<ServiceType> serviceTypes) throws CouldNotPerformException {
        final List<UnitConfig> unitConfigs = getUnitConfigs(unitType);
        boolean foundServiceType;

        for (final UnitConfig unitConfig : new ArrayList<>(unitConfigs)) {
            foundServiceType = false;
            for (ServiceType serviceType : serviceTypes) {
                for (final ServiceConfig serviceConfig : unitConfig.getServiceConfigList()) {
                    if (serviceConfig.getServiceDescription().getServiceType() == serviceType) {
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

    /**
     * Method returns a list of all unit group configs where the given unit config is a member of the group.
     *
     * @param unitConfig the unit config used to identify the member unit.
     * @return a list of unit group configs.
     * @throws CouldNotPerformException is thrown if the list could not be generated.
     */
    default List<UnitConfig> getUnitGroupUnitConfigsByUnitConfig(final UnitConfig unitConfig) throws CouldNotPerformException {
        return getUnitGroupUnitConfigsByUnitConfig(unitConfig.getId());
    }

    /**
     * Method returns a list of all unit group configs where the given unit is a member of the group.
     *
     * @param unitId the unit id defining the member unit.
     * @return a list of unit group configs.
     * @throws CouldNotPerformException is thrown if the list could not be generated.
     */
    default List<UnitConfig> getUnitGroupUnitConfigsByUnitConfig(final String unitId) throws CouldNotPerformException {
        List<UnitConfig> unitConfigList = new ArrayList<>();
        for (UnitConfig unitGroupUnitConfig : getUnitConfigs(UnitType.UNIT_GROUP)) {
            if (unitGroupUnitConfig.getUnitGroupConfig().getMemberIdList().contains(unitId)) {
                unitConfigList.add(unitGroupUnitConfig);
            }
        }
        return unitConfigList;
    }

    /**
     * Method returns a list of all unit group configs which are providing at least one member of the given {@code unitType}.
     *
     * @param unitType the unit type to filter the groups.
     * @return a list of unit group configs.
     * @throws CouldNotPerformException is thrown if the list could not be generated.
     */
    default List<UnitConfig> getUnitGroupUnitConfigsByUnitType(final UnitType unitType) throws CouldNotPerformException {
        List<UnitConfig> unitConfigList = new ArrayList<>();
        for (UnitConfig unitGroupUnitConfig : getUnitConfigs(UnitType.UNIT_GROUP)) {
            if (unitGroupUnitConfig.getUnitType() == unitType || CachedTemplateRegistryRemote.getRegistry().getSubUnitTypes(unitType).contains(unitGroupUnitConfig.getUnitType())) {
                unitConfigList.add(unitGroupUnitConfig);
            }
        }
        return unitConfigList;
    }

    /**
     * Method returns all unit configs filtered by the given {@code serviceType}.
     *
     * @param serviceType the service types to filter.
     * @return a list of unit types matching the given service types.
     * @throws CouldNotPerformException is thrown if the list could not be generated.
     */
    default List<UnitConfig> getUnitGroupUnitConfigsByServiceTypes(final ServiceType serviceType) throws CouldNotPerformException {
        final List<UnitConfig> unitGroups = new ArrayList<>();
        for (final UnitConfig unitGroupUnitConfig : getUnitConfigs(UnitType.UNIT_GROUP)) {
            for (final ServiceDescription serviceDescription : unitGroupUnitConfig.getUnitGroupConfig().getServiceDescriptionList()) {
                if (serviceType == serviceDescription.getServiceType()) {
                    unitGroups.add(unitGroupUnitConfig);
                    break;
                }
            }
        }
        return unitGroups;
    }

    /**
     * Method returns all unit configs filtered by the given {@code serviceTypes}.
     *
     * @param serviceTypes a list of service types to filter.
     * @return a list of unit types matching the given service types.
     * @throws CouldNotPerformException is thrown if the list could not be generated.
     */
    default List<UnitConfig> getUnitGroupUnitConfigsByServiceTypes(final List<ServiceType> serviceTypes) throws CouldNotPerformException {
        List<UnitConfig> unitGroups = new ArrayList<>();
        for (UnitConfig unitGroupUnitConfig : getUnitConfigs(UnitType.UNIT_GROUP)) {
            boolean skipGroup = false;
            for (ServiceDescription serviceDescription : unitGroupUnitConfig.getUnitGroupConfig().getServiceDescriptionList()) {
                if (!serviceTypes.contains(serviceDescription.getServiceType())) {
                    skipGroup = true;
                }
            }
            if (skipGroup) {
                continue;
            }
            unitGroups.add(unitGroupUnitConfig);
        }
        return unitGroups;
    }

    /**
     * Method collects all member unit configs of the given unit group and returns those as list.
     *
     * @param unitGroupUnitConfig the unit group of the members.
     * @return a list of unit configs.
     * @throws CouldNotPerformException is thrown if the list could not be generated.
     */
    default List<UnitConfig> getUnitConfigsByUnitGroupConfig(final UnitConfig unitGroupUnitConfig) throws CouldNotPerformException {
        verifyUnitGroupUnitConfig(unitGroupUnitConfig);
        final List<UnitConfig> unitConfigs = new ArrayList<>();
        for (String unitId : unitGroupUnitConfig.getUnitGroupConfig().getMemberIdList()) {
            unitConfigs.add(getUnitConfigById(unitId));
        }
        return unitConfigs;
    }

    /**
     * Method return the unit config which is registered for the given scope. A
     * NotAvailableException is thrown if no unit config is registered for the
     * given scope.
     *
     * @param scope the scope of the unit used as identifier.
     * @return the unit config matching the given scope.
     * @throws CouldNotPerformException
     */
    @RPCMethod
    default UnitConfig getUnitConfigByScope(final Scope scope) throws CouldNotPerformException {
        for (final UnitConfig unitConfig : getUnitConfigs()) {
            if (unitConfig.getScope().equals(scope)) {
                return unitConfig;
            }
        }
        throw new NotAvailableException("No unit config available for given scope!");
    }

    default void verifyUnitGroupUnitConfig(UnitConfig unitConfig) throws VerificationFailedException {
        UnitConfigProcessor.verifyUnitConfig(unitConfig, UnitType.UNIT_GROUP);
    }

    /**
     * Method returns true if the underling registry is marked as read only. A
     * registry is marked as read only in case of inconsistently data entries or
     * if the underling database is loaded out of a version tag.
     *
     * @return true if read only.
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
     * @throws CouldNotPerformException is thrown if the check fails.
     */
    @RPCMethod
    Boolean isSceneUnitRegistryReadOnly() throws CouldNotPerformException;

    /**
     * Method returns true if the underling registry is marked as consistent.
     *
     * @return true if consistent
     * @throws CouldNotPerformException if the check fails
     */
    @RPCMethod
    Boolean isUnitConfigRegistryConsistent() throws CouldNotPerformException;

    /**
     * Method returns true if the underling registry is marked as consistent.
     *
     * @return true if consistent
     * @throws CouldNotPerformException if the check fails
     */
    @RPCMethod
    Boolean isUnitGroupConfigRegistryConsistent() throws CouldNotPerformException;

    /**
     * Method returns true if the underling registry is marked as consistent.
     *
     * @return true if consistent
     * @throws CouldNotPerformException if the check fails
     */
    @RPCMethod
    Boolean isDalUnitConfigRegistryConsistent() throws CouldNotPerformException;

    /**
     * Method returns true if the underling registry is marked as consistent.
     *
     * @return true if consistent
     * @throws CouldNotPerformException if the check fails
     */
    @RPCMethod
    Boolean isUserUnitRegistryConsistent() throws CouldNotPerformException;

    /**
     * Method returns true if the underling registry is marked as consistent.
     *
     * @return true if consistent
     * @throws CouldNotPerformException if the check fails
     */
    @RPCMethod
    Boolean isAuthorizationGroupUnitRegistryConsistent() throws CouldNotPerformException;

    /**
     * Method returns true if the underling registry is marked as consistent.
     *
     * @return true if consistent
     * @throws CouldNotPerformException if the check fails
     */
    @RPCMethod
    Boolean isDeviceUnitRegistryConsistent() throws CouldNotPerformException;

    /**
     * Method returns true if the underling registry is marked as consistent.
     *
     * @return true if consistent
     * @throws CouldNotPerformException if the check fails
     */
    @RPCMethod
    Boolean isUnitGroupUnitRegistryConsistent() throws CouldNotPerformException;

    /**
     * Method returns true if the underling registry is marked as consistent.
     *
     * @return true if consistent
     * @throws CouldNotPerformException if the check fails
     */
    @RPCMethod
    Boolean isLocationUnitRegistryConsistent() throws CouldNotPerformException;

    /**
     * Method returns true if the underling registry is marked as consistent.
     *
     * @return true if consistent
     * @throws CouldNotPerformException if the check fails
     */
    @RPCMethod
    Boolean isConnectionUnitRegistryConsistent() throws CouldNotPerformException;

    /**
     * Method returns true if the underling registry is marked as consistent.
     *
     * @return true if consistent
     * @throws CouldNotPerformException if the check fails
     */
    @RPCMethod
    Boolean isAgentUnitRegistryConsistent() throws CouldNotPerformException;

    /**
     * Method returns true if the underling registry is marked as consistent.
     *
     * @return true if consistent
     * @throws CouldNotPerformException if the check fails
     */
    @RPCMethod
    Boolean isAppUnitRegistryConsistent() throws CouldNotPerformException;

    /**
     * Method returns true if the underling registry is marked as consistent.
     *
     * @return true if consistent
     * @throws CouldNotPerformException if the check fails
     */
    @RPCMethod
    Boolean isSceneUnitRegistryConsistent() throws CouldNotPerformException;

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
     * @return
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
     * @return User ID
     * @throws CouldNotPerformException
     * @throws NotAvailableException    If no user with the given user name could be found.
     */
    @RPCMethod
    default String getUserUnitIdByUserName(final String userName) throws CouldNotPerformException, NotAvailableException {
        validateData();
        List<UnitConfig> messages = getUnitConfigs(UnitType.USER);

        for (UnitConfig message : messages) {
            if (message.getUserConfig().getUserName().equalsIgnoreCase(userName)) {
                return message.getId();
            }
        }
        throw new NotAvailableException(userName);
    }

    /**
     * Method returns all location unit configs which are of the given location type.
     *
     * @param locationType the type of the location.
     * @return a list of the requested unit configs.
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
     * @return a list of the requested unit configs.
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
     * @return a list of the requested unit configs.
     * @throws CouldNotPerformException                is thrown if the request fails.
     * @throws java.lang.InterruptedException
     * @throws java.util.concurrent.ExecutionException
     */
    default List<UnitConfig> getLocationUnitConfigsByCoordinate(final Vec3DDouble coordinate, LocationType locationType) throws CouldNotPerformException, InterruptedException, ExecutionException {
        validateData();
        List<UnitConfig> result = new ArrayList<>();

        for (UnitConfig locationUnitConfig : getUnitConfigs(UnitType.LOCATION)) {
            // Check if the unit meets the requirements of the filter
            if (!locationType.equals(LocationType.UNKNOWN) && !locationType.equals(locationUnitConfig.getLocationConfig().getType())) {
                continue;
            }

            // Get the shape of the floor
            List<Vec3DDoubleType.Vec3DDouble> floorList = locationUnitConfig.getPlacementConfig().getShape().getFloorList();

            // Convert the shape into a PolygonsSet
            List<Vector2D> vertices = floorList.stream()
                    .map(vec3DDouble -> new Vector2D(vec3DDouble.getX(), vec3DDouble.getY()))
                    .collect(Collectors.toList());
            PolygonsSet polygonsSet = new PolygonsSet(0.1, vertices.toArray(new Vector2D[]{}));

            // Transform the given coordinate
            Transform3D unitTransform = getRootToUnitTransformationFuture(locationUnitConfig).get().getTransform();
            Point3d transformedCoordinate = new Point3d(coordinate.getX(), coordinate.getY(), coordinate.getZ());
            unitTransform.transform(transformedCoordinate);

            // NOTE: Hence apache-math builds its polygons counter clockwise unlike bco, the resulting polygon is inverted.
            // Therefore we check whether the point lies on the outside of the polygon.
            if (polygonsSet.checkPoint(new Vector2D(transformedCoordinate.x, transformedCoordinate.y)) == Location.OUTSIDE) {
                result.add(locationUnitConfig);
            }
        }

        return result;
    }

    /**
     * Method returns all unit configurations which are direct or recursive related to the given location id.
     *
     * @param locationId the id of the location which provides the units.
     * @return A collection of unit configs.
     * @throws CouldNotPerformException is thrown if the request fails.
     */
    default List<UnitConfig> getUnitConfigsByLocation(final String locationId) throws CouldNotPerformException {
        return getUnitConfigsByLocation(locationId, true);
    }

    /**
     * Method returns all unit configurations which are direct related to the given location id.
     * In case the {@code recursive} flag is set to true than recursive related units are included as well.
     *
     * @param locationId the id of the location which provides the units.
     * @param recursive  defines if recursive related unit should be included as well.
     * @return A collection of unit configs.
     * @throws CouldNotPerformException is thrown if the request fails.
     */
    default List<UnitConfig> getUnitConfigsByLocation(final String locationId, final boolean recursive) throws CouldNotPerformException {
        final List<UnitConfig> unitConfigList = new ArrayList<>();
        for (String unitConfigId : getUnitConfigById(locationId).getLocationConfig().getUnitIdList()) {
            final UnitConfig unitConfig = getUnitConfigById(unitConfigId);
            if (recursive || unitConfig.getPlacementConfig().getLocationId().equals(locationId)) {
                unitConfigList.add(unitConfig);
            }
        }
        return unitConfigList;
    }

    /**
     * Method returns all unit configurations which are direct or recursive
     * related to the given location id and an instance of the given unit type.
     * Label resolving is done case insensitive!
     *
     * @param type
     * @param locationConfigId
     * @return A collection of unit configs.
     * @throws CouldNotPerformException is thrown if the request fails.
     * @throws NotAvailableException
     */
    default List<UnitConfig> getUnitConfigsByLocation(final UnitType type, final String locationConfigId) throws CouldNotPerformException, NotAvailableException {
        List<UnitConfig> unitConfigList = new ArrayList<>();
        UnitConfig unitConfig;

        for (String unitConfigId : getUnitConfigById(locationConfigId).getLocationConfig().getUnitIdList()) {
            try {
                unitConfig = getUnitConfigById(unitConfigId);
                if (unitConfig.getUnitType().equals(type) || CachedTemplateRegistryRemote.getRegistry().getSubUnitTypes(type).contains(unitConfig.getUnitType())) {
                    unitConfigList.add(unitConfig);
                }
            } catch (CouldNotPerformException ex) {
                ExceptionPrinter.printHistory(new CouldNotPerformException("Could not resolve UnitConfigId[" + unitConfigId + "] by UnitRegitryRemote!", ex), LoggerFactory.getLogger(UnitRegistry.class));
            }
        }
        return unitConfigList;
    }

    /**
     * Method returns all unit configurations which are direct or recursive
     * related to the given location id and an implement the given service type.
     *
     * @param type             service type filter.
     * @param locationConfigId related location.
     * @return A collection of unit configs.
     * @throws CouldNotPerformException is thrown if the request fails.
     * @throws NotAvailableException
     */
    default List<UnitConfig> getUnitConfigsByLocation(final ServiceType type, final String locationConfigId) throws CouldNotPerformException, NotAvailableException {
        List<UnitConfig> unitConfigList = new ArrayList<>();
        UnitConfig unitConfig;

        for (String unitConfigId : getUnitConfigById(locationConfigId).getLocationConfig().getUnitIdList()) {
            try {
                unitConfig = getUnitConfigById(unitConfigId);
                for (ServiceConfig serviceConfig : unitConfig.getServiceConfigList()) {
                    if (serviceConfig.getServiceDescription().getServiceType().equals(type)) {
                        unitConfigList.add(unitConfig);
                    }
                }
            } catch (CouldNotPerformException ex) {
                ExceptionPrinter.printHistory(new CouldNotPerformException("Could not resolve UnitConfigId[" + unitConfigId + "] by device registry!", ex), LoggerFactory.getLogger(UnitRegistry.class));
            }
        }
        return unitConfigList;
    }


    /**
     * Method returns all service configurations which are direct or recursive
     * related to the given location id.
     *
     * @param locationId
     * @return the list of service configurations.
     * @throws CouldNotPerformException is thrown if the request fails.
     * @throws NotAvailableException    is thrown if the given location config id
     *                                  is unknown.
     */
    default List<ServiceConfig> getServiceConfigsByLocation(final String locationId) throws CouldNotPerformException {
        List<ServiceConfig> serviceConfigList = new ArrayList<>();
        for (UnitConfig unitConfig : getUnitConfigsByLocation(locationId)) {
            serviceConfigList.addAll(unitConfig.getServiceConfigList());
        }
        return serviceConfigList;
    }

    /**
     * Method returns all unit configurations which are direct or recursive
     * related to the given location alias.
     *
     * @param locationAlias the alias to identify the location.
     * @return A collection of unit configs.
     * @throws CouldNotPerformException is thrown if the request fails.
     * @throws NotAvailableException
     */
    default List<UnitConfig> getUnitConfigsByLocationAlias(final String locationAlias) throws CouldNotPerformException {
        final HashMap<String, UnitConfig> unitConfigMap = new HashMap<>();
        for (UnitConfig unitConfig : getUnitConfigsByLocation(getUnitConfigByAlias(locationAlias).getId())) {
            unitConfigMap.put(unitConfig.getId(), unitConfig);
        }
        return new ArrayList<>(unitConfigMap.values());
    }

    /**
     * Method returns a collection of unit configs which are located within the
     * defined location and match the given unit label. Label resolving is done
     * case insensitive!
     *
     * @param unitLabel
     * @param locationId
     * @return
     * @throws CouldNotPerformException is thrown if the request fails.
     */
    default List<UnitConfig> getUnitConfigsByLabelAndLocation(final String unitLabel, final String locationId) throws CouldNotPerformException {
        return getUnitConfigsByLabel(unitLabel).stream()
                .filter(u -> u.getPlacementConfig().getLocationId().equals(locationId))
                .collect(Collectors.toCollection(ArrayList::new));
    }

    /**
     * Method returns all unit configurations with a given type which are direct
     * or recursive related to the given location alias which can represent more
     * than one location. Alias resolving is done case insensitive!
     *
     * @param unitType
     * @param locationAlias
     * @return A collection of unit configs.
     * @throws CouldNotPerformException is thrown if the request fails.
     * @throws NotAvailableException
     */
    default List<UnitConfig> getUnitConfigsByLocationAlias(final UnitType unitType, final String locationAlias) throws CouldNotPerformException {
        final HashMap<String, UnitConfig> unitConfigMap = new HashMap<>();
        for (UnitConfig unitConfig : getUnitConfigsByLocation(getUnitConfigByAlias(locationAlias).getId())) {
            unitConfigMap.put(unitConfig.getId(), unitConfig);
        }
        return new ArrayList<>(unitConfigMap.values());
    }

    /**
     * Method generates a list of service types supported by the given location.
     *
     * @param locationId the location to filter the types.
     * @return a list of supported service types.
     * @throws NotAvailableException is thrown in case the list could not be computed.
     */
    default Set<ServiceType> getServiceTypesByLocation(final String locationId) throws CouldNotPerformException {
        final Set<ServiceType> serviceTypeSet = new HashSet<>();
        for (final UnitConfig unitConfig : getUnitConfigsByLocation(locationId)) {
            for (final ServiceConfig serviceConfig : unitConfig.getServiceConfigList()) {
                serviceTypeSet.add(serviceConfig.getServiceDescription().getServiceType());
            }
        }
        return serviceTypeSet;
    }

    /**
     * Method returns all unit configurations which are related to the given
     * connection id.
     *
     * @param connectionConfigId
     * @return A collection of unit configs.
     * @throws CouldNotPerformException is thrown if the request fails.
     */
    default List<UnitConfig> getUnitConfigsByConnection(final String connectionConfigId) throws CouldNotPerformException {
        List<UnitConfig> unitConfigList = new ArrayList<>();
        for (String unitConfigId : getUnitConfigById(connectionConfigId).getConnectionConfig().getUnitIdList()) {
            unitConfigList.add(getUnitConfigById(unitConfigId));
        }
        return unitConfigList;
    }

    /**
     * Method returns all unit configurations which are related to the given
     * connection id and an instance of the given unit type.
     *
     * @param type
     * @param connectionConfigId
     * @return A collection of unit configs.
     * @throws CouldNotPerformException is thrown if the request fails.
     * @throws NotAvailableException
     */
    default List<UnitConfig> getUnitConfigsByConnection(final UnitType type, final String connectionConfigId) throws CouldNotPerformException, NotAvailableException {
        List<UnitConfig> unitConfigList = new ArrayList<>();
        UnitConfig unitConfig;

        for (String unitConfigId : getUnitConfigById(connectionConfigId).getConnectionConfig().getUnitIdList()) {
            try {
                unitConfig = getUnitConfigById(unitConfigId);
                if (unitConfig.getUnitType().equals(type) || CachedTemplateRegistryRemote.getRegistry().getSubUnitTypes(type).contains(unitConfig.getUnitType())) {
                    unitConfigList.add(unitConfig);
                }
            } catch (CouldNotPerformException ex) {
                ExceptionPrinter.printHistory(new CouldNotPerformException("Could not resolve UnitConfigId[" + unitConfigId + "] by device registry!", ex), LoggerFactory.getLogger(this.getClass()));
            }
        }
        return unitConfigList;
    }

    /**
     * Method returns all unit configurations which are related to the given
     * connection id and an implement the given service type.
     *
     * @param type               service type filter.
     * @param connectionConfigId related connection.
     * @return A collection of unit configs.
     * @throws CouldNotPerformException is thrown if the request fails.
     * @throws NotAvailableException
     */
    default List<UnitConfig> getUnitConfigsByConnection(final ServiceType type, final String connectionConfigId) throws CouldNotPerformException, NotAvailableException {
        List<UnitConfig> unitConfigList = new ArrayList<>();
        UnitConfig unitConfig;

        for (String unitConfigId : getUnitConfigById(connectionConfigId).getConnectionConfig().getUnitIdList()) {
            try {
                unitConfig = getUnitConfigById(unitConfigId);
                for (ServiceConfig serviceConfig : unitConfig.getServiceConfigList()) {
                    if (serviceConfig.getServiceDescription().getServiceType().equals(type)) {
                        unitConfigList.add(unitConfig);
                    }
                }
            } catch (CouldNotPerformException ex) {
                ExceptionPrinter.printHistory(new CouldNotPerformException("Could not resolve UnitConfigId[" + unitConfigId + "] by device registry!", ex), LoggerFactory.getLogger(this.getClass()));
            }
        }
        return unitConfigList;
    }

    /**
     * Method returns all service configurations which are related to the given
     * connection id.
     *
     * @param connectionConfigId
     * @return the list of service configurations.
     * @throws CouldNotPerformException is thrown if the request fails.
     * @throws NotAvailableException    is thrown if the given connection config id
     *                                  is unknown.
     */
    default List<ServiceConfig> getServiceConfigsByConnection(final String connectionConfigId) throws CouldNotPerformException {
        List<ServiceConfig> serviceConfigList = new ArrayList<>();
        for (UnitConfig unitConfig : getUnitConfigsByConnection(connectionConfigId)) {
            serviceConfigList.addAll(unitConfig.getServiceConfigList());
        }
        return serviceConfigList;
    }

    /**
     * Method returns all neighbor tiles for a tile. If the given locationId
     * does not belong to a tile, the could not perform exception is thrown.
     *
     * @param locationId the id of the location which neighbors you want to get
     * @return all neighbor tiles
     * @throws CouldNotPerformException is thrown if the request fails.
     */
    default List<UnitConfig> getNeighborLocations(final String locationId) throws CouldNotPerformException {
        final UnitConfig locationConfig = getUnitConfigById(locationId, UnitType.LOCATION);
        if (locationConfig.getLocationConfig().getType() != LocationType.TILE) {
            throw new CouldNotPerformException("Id[" + locationId + "] does not belong to a tile and therefore its neighbors aren't defined!");
        }

        final Map<String, UnitConfig> neighborMap = new HashMap<>();
        for (UnitConfig connectionConfig : getUnitConfigs(UnitType.CONNECTION)) {
            if (connectionConfig.getConnectionConfig().getTileIdList().contains(locationId)) {
                for (String id : connectionConfig.getConnectionConfig().getTileIdList()) {
                    if (id.equals(locationId)) {
                        continue;
                    }

                    neighborMap.put(id, getUnitConfigById(id));
                }
            }
        }

        return new ArrayList<>(neighborMap.values());
    }

    /**
     * {@inheritDoc}
     *
     * @throws org.openbase.jul.exception.CouldNotPerformException {@inheritDoc}
     */
    default List<UnitConfig> getLocationConfigsByCoordinate(final Vec3DDouble coordinate, final LocationType locationType) throws CouldNotPerformException, InterruptedException, ExecutionException {
        validateData();
        List<UnitConfig> result = new ArrayList<>();

        for (UnitConfig unitConfig : getUnitConfigs(UnitType.LOCATION)) {
            // Check if the unit meets the requirements of the filter
            if (!locationType.equals(LocationType.UNKNOWN) && !locationType.equals(unitConfig.getLocationConfig().getType())) {
                continue;
            }

            // Get the shape of the floor
            List<Vec3DDoubleType.Vec3DDouble> floorList = unitConfig.getPlacementConfig().getShape().getFloorList();

            // Convert the shape into a PolygonsSet
            List<Vector2D> vertices = floorList.stream()
                    .map(vec3DDouble -> new Vector2D(vec3DDouble.getX(), vec3DDouble.getY()))
                    .collect(Collectors.toList());
            PolygonsSet polygonsSet = new PolygonsSet(0.1, vertices.toArray(new Vector2D[]{}));

            // Transform the given coordinate
            Transform3D unitTransform = getRootToUnitTransformationFuture(unitConfig).get().getTransform();
            Point3d transformedCoordinate = new Point3d(coordinate.getX(), coordinate.getY(), coordinate.getZ());
            unitTransform.transform(transformedCoordinate);

            // NOTE: Hence apache-math builds its polygons counter clockwise unlike bco, the resulting polygon is inverted.
            // Therefore we check whether the point lies on the outside of the polygon.
            if (polygonsSet.checkPoint(new Vector2D(transformedCoordinate.x, transformedCoordinate.y)) == Location.OUTSIDE) {
                result.add(unitConfig);
            }
        }

        return result;
    }

    /**
     * Method returns all agent unit configs which are based on the given agent class.
     *
     * @param agentClass the agent class to identify the units.
     * @return a list of matching agent configs.
     * @throws CouldNotPerformException is thrown in case the list could not be generated.
     */
    default List<UnitConfig> getAgentUnitConfigsByAgentClass(final AgentClass agentClass) throws CouldNotPerformException {
        return getAgentUnitConfigsByAgentClassId(agentClass.getId());
    }

    /**
     * Method returns all agent unit configs which are based on the given agent class.
     *
     * @param agentClassId the if of the agent class to identify the units.
     * @return a list of matching agent configs.
     * @throws CouldNotPerformException is thrown in case the list could not be generated.
     */
    default List<UnitConfig> getAgentUnitConfigsByAgentClassId(final String agentClassId) throws CouldNotPerformException {
        if (!CachedClassRegistryRemote.getRegistry().containsAgentClassById(agentClassId)) {
            throw new NotAvailableException("agentClassId [" + agentClassId + "]");
        }

        List<UnitConfig> agentConfigs = new ArrayList<>();
        for (UnitConfig agentConfig : getUnitConfigs(UnitType.AGENT)) {
            if (agentConfig.getAgentConfig().getAgentClassId().equals(agentClassId)) {
                agentConfigs.add(agentConfig);
            }
        }
        return agentConfigs;
    }

    /**
     * Method returns all app unit configs which are based on the given app class.
     *
     * @param appClass the app class to identify the units.
     * @return a list of matching app configs.
     * @throws CouldNotPerformException is thrown in case the list could not be generated.
     */
    default List<UnitConfig> getAppUnitConfigsByAppClass(final AppClass appClass) throws CouldNotPerformException, InterruptedException {
        return getAppUnitConfigsByAppClassId(appClass.getId());
    }

    /**
     * Method returns all app unit configs which are based on the given app class.
     *
     * @param appClassId the if of the app class to identify the units.
     * @return a list of matching app configs.
     * @throws CouldNotPerformException is thrown in case the list could not be generated.
     */
    default List<UnitConfig> getAppUnitConfigsByAppClassId(final String appClassId) throws CouldNotPerformException, InterruptedException {
        if (!CachedClassRegistryRemote.getRegistry().containsAppClassById(appClassId)) {
            throw new NotAvailableException("appClassId [" + appClassId + "]");
        }

        final List<UnitConfig> appConfigs = new ArrayList<>();
        for (UnitConfig appConfig : getUnitConfigs(UnitType.APP)) {
            if (appConfig.getAppConfig().getAppClassId().equals(appClassId)) {
                appConfigs.add(appConfig);
            }
        }
        return appConfigs;
    }

//    Not yet implemented so temporally removed from interface
//
//    /**
//     * Method returns a list of probably intersected units by the given 3D ray.
//     * This could for example be useful for selecting units by pointing gestures.
//     *
//     * @param pointingRay3DFloat ray which probably intersects with a specific unit priorized by a given certainty.
//     *
//     * @return a collection of probably intersected units referred by there id.
//     *
//     * @throws CouldNotPerformException is thrown in case the computation could not be performed.
//     */
//    @RPCMethod
//    Future<UnitProbabilityCollection> computeUnitIntersection(final PointingRay3DFloat pointingRay3DFloat) throws CouldNotPerformException;
//
//    /**
//     * Method returns a list of probably intersected units by the given 3D rays.
//     * This could for example be useful for selecting units by pointing gestures.
//     *
//     * @param pointingRay3DFloatCollection a collection of rays which probably intersects with a specific unit priorized by a given certainty.
//     *
//     * @return a collection of probably intersected units referred by there id.
//     *
//     * @throws CouldNotPerformException is thrown in case the computation could not be performed.
//     */
//    @RPCMethod
//    Future<UnitProbabilityCollection> computeUnitIntersection(final PointingRay3DFloatCollection pointingRay3DFloatCollection) throws CouldNotPerformException;

}
