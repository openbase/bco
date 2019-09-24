package org.openbase.bco.registry.unit.lib;

/*
 * #%L
 * BCO Registry Unit Library
 * %%
 * Copyright (C) 2014 - 2019 openbase.org
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
import org.openbase.jul.extension.protobuf.IdentifiableMessage;
import org.openbase.jul.extension.type.processing.ScopeProcessor;
import org.openbase.jul.extension.type.processing.LabelProcessor;
import org.openbase.jul.extension.type.processing.ScopeProcessor;
import org.openbase.jul.iface.Shutdownable;
import org.openbase.jul.pattern.provider.DataProvider;
import org.openbase.jul.storage.registry.RegistryService;
import org.slf4j.LoggerFactory;
import org.openbase.type.domotic.authentication.AuthenticatedValueType.AuthenticatedValue;
import org.openbase.type.domotic.authentication.AuthenticationTokenType.AuthenticationToken;
import org.openbase.type.domotic.authentication.AuthorizationTokenType.AuthorizationToken;
import org.openbase.type.domotic.registry.UnitRegistryDataType.UnitRegistryData;
import org.openbase.type.domotic.service.ServiceConfigType;
import org.openbase.type.domotic.service.ServiceConfigType.ServiceConfig;
import org.openbase.type.domotic.service.ServiceDescriptionType.ServiceDescription;
import org.openbase.type.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import org.openbase.type.domotic.unit.UnitConfigType.UnitConfig;
import org.openbase.type.domotic.unit.UnitConfigType.UnitConfig.Builder;
import org.openbase.type.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;
import org.openbase.type.domotic.unit.agent.AgentClassType.AgentClass;
import org.openbase.type.domotic.unit.app.AppClassType.AppClass;
import org.openbase.type.domotic.unit.location.LocationConfigType.LocationConfig.LocationType;
import org.openbase.type.language.LabelType.Label;
import org.openbase.type.math.Vec3DDoubleType;
import org.openbase.type.math.Vec3DDoubleType.Vec3DDouble;
import org.openbase.type.communication.ScopeType.Scope;

import javax.media.j3d.Transform3D;
import javax.vecmath.Point3d;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

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
     * This alias can be used for fast lookups of the openhab user.
     */
    String OPENHAB_USER_ALIAS = "OpenHABUser";

    /**
     * The default radius used for the unit by coordinate lookup is set to 1 metre.
     */
    double DEFAULT_RADIUS = 1d;

    /**
     * This method registers the given unit config in the registry.
     * Future get canceled if the entry already exists or results in an inconsistent registry
     *
     * @param unitConfig the unit config to register.
     *
     * @return the registered unit config with all applied consistency changes.
     */
    @RPCMethod
    Future<UnitConfig> registerUnitConfig(final UnitConfig unitConfig);

    @RPCMethod
    Future<AuthenticatedValue> registerUnitConfigAuthenticated(final AuthenticatedValue authenticatedValue);

    /**
     * Method updates the given unit config.
     *
     * @param unitConfig the updated unit config.
     *
     * @return the updated unit config with all applied consistency changes.
     */
    @RPCMethod
    Future<UnitConfig> updateUnitConfig(final UnitConfig unitConfig);

    @RPCMethod
    Future<AuthenticatedValue> updateUnitConfigAuthenticated(final AuthenticatedValue authenticatedValue);

    /**
     * Method removes the given unit config out of the global registry.
     *
     * @param unitConfig the unit config to remove.
     *
     * @return The removed unit config.
     */
    @RPCMethod
    Future<UnitConfig> removeUnitConfig(final UnitConfig unitConfig);

    @RPCMethod
    Future<AuthenticatedValue> removeUnitConfigAuthenticated(final AuthenticatedValue authenticatedValue);

    /**
     * Method returns true if the unit config with the given id is
     * registered, otherwise false. The unit config id field is used for the
     * comparison.
     *
     * @param unitConfig the unit config used for the identification.
     *
     * @return true if the unit exists or false if the entry does not exists or the registry is not available.
     */
    @RPCMethod
    Boolean containsUnitConfig(final UnitConfig unitConfig);

    @RPCMethod
    default Boolean containsUnitConfigByAlias(final String alias) {
        try {
            getUnitConfigByAlias(alias);
        } catch (final NotAvailableException ex) {
            return false;
        }
        return true;
    }

    default List<UnitConfig> getUnitConfigsByServices(final ServiceType... serviceTypes) throws CouldNotPerformException {
        return getUnitConfigsByServiceList(Arrays.asList(serviceTypes));
    }

    default List<UnitConfig> getUnitConfigsByServiceList(final List<ServiceType> serviceTypes) throws CouldNotPerformException {
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
        return ScopeProcessor.generateStringRep(getUnitConfigById(id).getScope());
    }

    @RPCMethod
    default String getUnitScopeByAlias(final String alias) throws CouldNotPerformException {
        return ScopeProcessor.generateStringRep(getUnitConfigByAlias(alias).getScope());
    }

    /**
     * Method returns true if the underling registry is marked as read only. A
     * registry is marked as read only in case of inconsistently data entries or
     * if the underling database is loaded out of a version tag.
     * <p>
     * Note: Method returns true in case the registry is not available. Maybe you need to check this in advance.
     *
     * @return true if read only.
     */
    @RPCMethod
    Boolean isUnitConfigRegistryReadOnly();

    /**
     * Method returns the unit matching the given alias. An alias is a unique identifier of units.
     * <p>
     * Hint: If you want to address more than one unit with an alias than create a unit group of such units and define an alias for those group.
     *
     * @param unitAlias the alias to identify the unit.
     *
     * @return the unit config referred by the alias.
     *
     * @throws NotAvailableException is thrown if no unit is matching the given alias.
     */
    @RPCMethod
    UnitConfig getUnitConfigByAlias(final String unitAlias) throws NotAvailableException;


    /**
     * Method returns the unit matching the given alias. An alias is a unique identifier of units.
     * <p>
     * Hint: If you want to address more than one unit with an alias than create a unit group of such units and define an alias for those group.
     *
     * @param unitAlias the alias to identify the unit.
     * @param unitType  the type to validate the resulting unit.
     *
     * @return the unit config referred by the alias.
     *
     * @throws NotAvailableException is thrown if no unit is matching the given alias.
     * @deprecated please use getUnitConfigByAliasAndUnitType(...) instead.
     */
    @Deprecated
    default UnitConfig getUnitConfigByAlias(String unitAlias, final UnitType unitType) throws NotAvailableException {
        return getUnitConfigByAliasAndUnitType(unitAlias, unitType);
    }

    /**
     * Method returns the unit matching the given alias. An alias is a unique identifier of units.
     * <p>
     * Hint: If you want to address more than one unit with an alias than create a unit group of such units and define an alias for those group.
     *
     * @param unitAlias the alias to identify the unit.
     * @param unitType  the type to validate the resulting unit.
     *
     * @return the unit config referred by the alias.
     *
     * @throws NotAvailableException is thrown if no unit is matching the given alias.
     */
    UnitConfig getUnitConfigByAliasAndUnitType(String unitAlias, final UnitType unitType) throws NotAvailableException;

    /**
     * Method returns all registered units with the given label. Label resolving
     * is done case insensitive!
     * <p>
     * Note: PLEASE DO NOT USE THIS METHOD TO REQUEST DEVICES FOR THE CONTROLLING PURPOSE BECAUSE LABELS ARE NOT A STABLE IDENTIFIER! USE ID OR ALIAS INSTEAD!
     *
     * @param unitConfigLabel the label to identify a set of units.
     *
     * @return a list of the requested unit configs.
     *
     * @throws CouldNotPerformException is thrown if the request fails.
     */
    default List<UnitConfig> getUnitConfigsByLabel(final String unitConfigLabel) throws CouldNotPerformException {
        List<UnitConfig> unitConfigs = new ArrayList<>();
        for (UnitConfig unitConfig : getUnitConfigs()) {
            if (LabelProcessor.contains(unitConfig.getLabel(), unitConfigLabel)) {
                unitConfigs.add(unitConfig);
            }
        }
        return unitConfigs;
    }

    default List<UnitConfig> getUnitConfigsByLabelAndUnitType(final String unitConfigLabel, final UnitType unitType) throws CouldNotPerformException {
        List<UnitConfig> unitConfigs = new ArrayList<>();
        for (UnitConfig unitConfig : getUnitConfigs()) {
            if (unitConfig.getUnitType().equals(unitType) && LabelProcessor.contains(unitConfig.getLabel(), unitConfigLabel)) {
                unitConfigs.add(unitConfig);
            }
        }
        return unitConfigs;
    }

    /**
     * Method returns a list of all globally registered units of the given {@code type}.
     * <p>
     * Note: The type {@code UnitType.UNKNOWN} is used as wildcard and will return a list of all registered units.
     *
     * @param unitType the unit type to filter.
     *
     * @return a list of unit configurations.
     *
     * @throws CouldNotPerformException is thrown in case something goes wrong during the request.
     * @deprecated since 2.0 and will be removed in 3.0: please use getUnitConfigsByUnitType(...) instead.
     */
    @Deprecated
    default List<UnitConfig> getUnitConfigs(final UnitType unitType) throws CouldNotPerformException {
        return getUnitConfigsByUnitType(unitType);
    }

    /**
     * Method returns a list of all globally registered units of the given {@code type}.
     * <p>
     * Note: The type {@code UnitType.UNKNOWN} is used as wildcard and will return a list of all registered units.
     *
     * @param unitType the unit type to filter.
     *
     * @return a list of unit configurations.
     *
     * @throws CouldNotPerformException is thrown in case something goes wrong during the request.
     */
    default List<UnitConfig> getUnitConfigsByUnitType(final UnitType unitType) throws CouldNotPerformException {
        validateData();
        List<UnitConfig> unitConfigs = new ArrayList<>();
        for (UnitConfig unitConfig : getUnitConfigs()) {
            if (unitType == UnitType.UNKNOWN || unitConfig.getUnitType() == unitType || CachedTemplateRegistryRemote.getRegistry().getSubUnitTypes(unitType).contains(unitConfig.getUnitType())) {
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

    /**
     * @return a list containing all service configs of all units.
     *
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
     *
     * @return a list of service configs matching the given {@code serviceType}.
     *
     * @throws CouldNotPerformException is thrown if the config list could not be generated.
     * @deprecated please use getServiceConfigsByServiceType(...) instead.
     */
    @Deprecated
    default List<ServiceConfig> getServiceConfigs(final ServiceType serviceType) throws CouldNotPerformException {
        return getServiceConfigsByServiceType(serviceType);
    }

    /**
     * Method returns all service configs of all units filtered by the given {@code serviceType}.
     *
     * @param serviceType the service type to filter.
     *
     * @return a list of service configs matching the given {@code serviceType}.
     *
     * @throws CouldNotPerformException is thrown if the config list could not be generated.
     */
    default List<ServiceConfig> getServiceConfigsByServiceType(final ServiceType serviceType) throws CouldNotPerformException {
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
     * <p>
     * Note: Method returns true in case the registry is not available. Maybe you need to check this in advance.
     *
     * @return true if read only.
     */
    @RPCMethod
    Boolean isUnitGroupConfigRegistryReadOnly();

    /**
     * Method returns all unit configs filtered by the given {@code unitType} and {@code serviceType}.
     *
     * @param unitType    the unit type to filter.
     * @param serviceType the service type to filter.
     *
     * @return a list of unit types matching the given unit and service type.
     *
     * @throws CouldNotPerformException is thrown if the list could not be generated.
     * @deprecated since 2.0 and will be removed in 3.0: please use getUnitConfigsByUnitTypeAndServiceType(...) instead.
     */
    @Deprecated
    default List<UnitConfig> getUnitConfigsByUnitTypeAndServiceTypes(final UnitType unitType, final ServiceType serviceType) throws CouldNotPerformException {
        return getUnitConfigsByUnitTypeAndServiceType(unitType, serviceType);
    }

    /**
     * Method returns all unit configs filtered by the given {@code unitType} and {@code serviceType}.
     *
     * @param unitType    the unit type to filter.
     * @param serviceType the service type to filter.
     *
     * @return a list of unit types matching the given unit and service type.
     *
     * @throws CouldNotPerformException is thrown if the list could not be generated.
     */
    default List<UnitConfig> getUnitConfigsByUnitTypeAndServiceType(final UnitType unitType, final ServiceType serviceType) throws CouldNotPerformException {
        final List<UnitConfig> unitConfigs = getUnitConfigsByUnitType(unitType);
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
     *
     * @return a list of unit types matching the given unit and service type.
     *
     * @throws CouldNotPerformException is thrown if the list could not be generated.
     */
    default List<UnitConfig> getUnitConfigsByUnitTypeAndServiceTypes(final UnitType unitType, final List<ServiceType> serviceTypes) throws CouldNotPerformException {
        final List<UnitConfig> unitConfigs = getUnitConfigsByUnitType(unitType);
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
     *
     * @return a list of unit group configs.
     *
     * @throws CouldNotPerformException is thrown if the list could not be generated.
     */
    default List<UnitConfig> getUnitGroupUnitConfigsByUnitConfig(final UnitConfig unitConfig) throws CouldNotPerformException {
        return getUnitGroupUnitConfigsByUnitId(unitConfig.getId());
    }

    /**
     * Method returns a list of all unit group configs where the given unit is a member of the group.
     *
     * @param unitId the unit id defining the member unit.
     *
     * @return a list of unit group configs.
     *
     * @throws CouldNotPerformException is thrown if the list could not be generated.
     * @deprecated since 2.0 and will be removed in 3.0: please use getUnitGroupUnitConfigsByUnitId(...) instead.
     */
    @Deprecated
    default List<UnitConfig> getUnitGroupUnitConfigsByUnitConfig(final String unitId) throws CouldNotPerformException {
        return getUnitGroupUnitConfigsByUnitId(unitId);
    }


    /**
     * Method returns a list of all unit group configs where the given unit is a member of the group.
     *
     * @param unitId the unit id defining the member unit.
     *
     * @return a list of unit group configs.
     *
     * @throws CouldNotPerformException is thrown if the list could not be generated.
     */
    default List<UnitConfig> getUnitGroupUnitConfigsByUnitId(final String unitId) throws CouldNotPerformException {
        List<UnitConfig> unitConfigList = new ArrayList<>();
        for (UnitConfig unitGroupUnitConfig : getUnitConfigsByUnitType(UnitType.UNIT_GROUP)) {
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
     *
     * @return a list of unit group configs.
     *
     * @throws CouldNotPerformException is thrown if the list could not be generated.
     */
    default List<UnitConfig> getUnitGroupUnitConfigsByUnitType(final UnitType unitType) throws CouldNotPerformException {
        List<UnitConfig> unitConfigList = new ArrayList<>();
        for (UnitConfig unitGroupUnitConfig : getUnitConfigsByUnitType(UnitType.UNIT_GROUP)) {
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
     *
     * @return a list of unit types matching the given service types.
     *
     * @throws CouldNotPerformException is thrown if the list could not be generated.
     * @deprecated since 2.0 and will be removed in 3.0: please use getUnitGroupUnitConfigsByServiceType(...) instead.
     */
    @Deprecated
    default List<UnitConfig> getUnitGroupUnitConfigsByServiceTypes(final ServiceType serviceType) throws CouldNotPerformException {
        return getUnitGroupUnitConfigsByServiceType(serviceType);
    }

    /**
     * Method returns all unit configs filtered by the given {@code serviceType}.
     *
     * @param serviceType the service types to filter.
     *
     * @return a list of unit types matching the given service types.
     *
     * @throws CouldNotPerformException is thrown if the list could not be generated.
     */
    default List<UnitConfig> getUnitGroupUnitConfigsByServiceType(final ServiceType serviceType) throws CouldNotPerformException {
        final List<UnitConfig> unitGroups = new ArrayList<>();
        for (final UnitConfig unitGroupUnitConfig : getUnitConfigsByUnitType(UnitType.UNIT_GROUP)) {
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
     *
     * @return a list of unit types matching the given service types.
     *
     * @throws CouldNotPerformException is thrown if the list could not be generated.
     */
    default List<UnitConfig> getUnitGroupUnitConfigsByServiceTypes(final List<ServiceType> serviceTypes) throws CouldNotPerformException {
        List<UnitConfig> unitGroups = new ArrayList<>();
        for (UnitConfig unitGroupUnitConfig : getUnitConfigsByUnitType(UnitType.UNIT_GROUP)) {
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
     *
     * @return a list of unit configs.
     *
     * @throws CouldNotPerformException is thrown if the list could not be generated.
     */
    default List<UnitConfig> getUnitConfigsByUnitGroupConfig(final UnitConfig unitGroupUnitConfig) throws CouldNotPerformException {
        UnitConfigProcessor.verifyUnitConfig(unitGroupUnitConfig, UnitType.UNIT_GROUP);
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
     *
     * @return the unit config matching the given scope.
     *
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

    /**
     * @param unitConfig
     *
     * @throws VerificationFailedException
     * @deprecated since 2.0 and will be removed in 3.0: please use UnitConfigProcessor.verifyUnitConfig(unitConfig, UnitType.UNIT_GROUP) instead.
     */
    @Deprecated
    default void verifyUnitGroupUnitConfig(UnitConfig unitConfig) throws VerificationFailedException {
        UnitConfigProcessor.verifyUnitConfig(unitConfig, UnitType.UNIT_GROUP);
    }

    /**
     * Method returns true if the underling registry is marked as read only. A
     * registry is marked as read only in case of inconsistently data entries or
     * if the underling database is loaded out of a version tag.
     * <p>
     * Note: Method returns true in case the registry is not available. Maybe you need to check this in advance.
     *
     * @return true if read only.
     */
    @RPCMethod
    Boolean isDalUnitConfigRegistryReadOnly();

    /**
     * Method returns true if the underling registry is marked as read only. A
     * registry is marked as read only in case of inconsistently data entries or
     * if the underling database is loaded out of a version tag.
     * <p>
     * Note: Method returns true in case the registry is not available. Maybe you need to check this in advance.
     *
     * @return true if read only.
     */
    @RPCMethod
    Boolean isUserUnitRegistryReadOnly();

    /**
     * Method returns true if the underling registry is marked as read only. A
     * registry is marked as read only in case of inconsistently data entries or
     * if the underling database is loaded out of a version tag.
     * <p>
     * Note: Method returns true in case the registry is not available. Maybe you need to check this in advance.
     *
     * @return true if read only.
     */
    @RPCMethod
    Boolean isAuthorizationGroupUnitRegistryReadOnly();

    /**
     * Method returns true if the underling registry is marked as read only. A
     * registry is marked as read only in case of inconsistently data entries or
     * if the underling database is loaded out of a version tag.
     * <p>
     * Note: Method returns true in case the registry is not available. Maybe you need to check this in advance.
     *
     * @return true if read only.
     */
    @RPCMethod
    Boolean isDeviceUnitRegistryReadOnly();

    /**
     * Method returns true if the underling registry is marked as read only. A
     * registry is marked as read only in case of inconsistently data entries or
     * if the underling database is loaded out of a version tag.
     * <p>
     * Note: Method returns true in case the registry is not available. Maybe you need to check this in advance.
     *
     * @return true if read only.
     */
    @RPCMethod
    Boolean isUnitGroupUnitRegistryReadOnly();

    /**
     * Method returns true if the underling registry is marked as read only. A
     * registry is marked as read only in case of inconsistently data entries or
     * if the underling database is loaded out of a version tag.
     * <p>
     * Note: Method returns true in case the registry is not available. Maybe you need to check this in advance.
     *
     * @return true if read only.
     */
    @RPCMethod
    Boolean isLocationUnitRegistryReadOnly();

    /**
     * Method returns true if the underling registry is marked as read only. A
     * registry is marked as read only in case of inconsistently data entries or
     * if the underling database is loaded out of a version tag.
     * <p>
     * Note: Method returns true in case the registry is not available. Maybe you need to check this in advance.
     *
     * @return true if read only.
     */
    @RPCMethod
    Boolean isConnectionUnitRegistryReadOnly();

    /**
     * Method returns true if the underling registry is marked as read only. A
     * registry is marked as read only in case of inconsistently data entries or
     * if the underling database is loaded out of a version tag.
     * <p>
     * Note: Method returns true in case the registry is not available. Maybe you need to check this in advance.
     *
     * @return true if read only.
     */
    @RPCMethod
    Boolean isAgentUnitRegistryReadOnly();

    /**
     * Method returns true if the underling registry is marked as read only. A
     * registry is marked as read only in case of inconsistently data entries or
     * if the underling database is loaded out of a version tag.
     * <p>
     * Note: Method returns true in case the registry is not available. Maybe you need to check this in advance.
     *
     * @return true if read only.
     */
    @RPCMethod
    Boolean isAppUnitRegistryReadOnly();

    /**
     * Method returns true if the underling registry is marked as read only. A
     * registry is marked as read only in case of inconsistently data entries or
     * if the underling database is loaded out of a version tag.
     * <p>
     * Note: Method returns true in case the registry is not available. Maybe you need to check this in advance.
     *
     * @return true if read only.
     */
    @RPCMethod
    Boolean isSceneUnitRegistryReadOnly();

    /**
     * Method returns true if the underling registry is marked as read only. A
     * registry is marked as read only in case of inconsistent data entries or
     * if the underling database is loaded out of a version tag.
     * <p>
     * Note: Method returns true in case the registry is not available. Maybe you need to check this in advance.
     *
     * @return true if read only.
     */
    @RPCMethod
    Boolean isObjectUnitRegistryReadOnly();

    /**
     * Method returns true if the underling registry is marked as consistent.
     * <p>
     * Note: Method returns true in case the registry is not available. Maybe you need to check this in advance.
     *
     * @return true if consistent
     */
    @RPCMethod
    Boolean isUnitConfigRegistryConsistent();

    /**
     * Method returns true if the underling registry is marked as consistent.
     * <p>
     * Note: Method returns true in case the registry is not available. Maybe you need to check this in advance.
     *
     * @return true if consistent
     */
    @RPCMethod
    Boolean isUnitGroupConfigRegistryConsistent();

    /**
     * Method returns true if the underling registry is marked as consistent.
     * <p>
     * Note: Method returns true in case the registry is not available. Maybe you need to check this in advance.
     *
     * @return true if consistent
     */
    @RPCMethod
    Boolean isDalUnitConfigRegistryConsistent();

    /**
     * Method returns true if the underling registry is marked as consistent.
     * <p>
     * Note: Method returns true in case the registry is not available. Maybe you need to check this in advance.
     *
     * @return true if consistent
     */
    @RPCMethod
    Boolean isUserUnitRegistryConsistent();

    /**
     * Method returns true if the underling registry is marked as consistent.
     * <p>
     * Note: Method returns true in case the registry is not available. Maybe you need to check this in advance.
     *
     * @return true if consistent
     */
    @RPCMethod
    Boolean isAuthorizationGroupUnitRegistryConsistent();

    /**
     * Method returns true if the underling registry is marked as consistent.
     * <p>
     * Note: Method returns true in case the registry is not available. Maybe you need to check this in advance.
     *
     * @return true if consistent
     */
    @RPCMethod
    Boolean isDeviceUnitRegistryConsistent();

    /**
     * Method returns true if the underling registry is marked as consistent.
     * <p>
     * Note: Method returns true in case the registry is not available. Maybe you need to check this in advance.
     *
     * @return true if consistent
     */
    @RPCMethod
    Boolean isUnitGroupUnitRegistryConsistent();

    /**
     * Method returns true if the underling registry is marked as consistent.
     * <p>
     * Note: Method returns true in case the registry is not available. Maybe you need to check this in advance.
     *
     * @return true if consistent
     */
    @RPCMethod
    Boolean isLocationUnitRegistryConsistent();

    /**
     * Method returns true if the underling registry is marked as consistent.
     * <p>
     * Note: Method returns true in case the registry is not available. Maybe you need to check this in advance.
     *
     * @return true if consistent
     */
    @RPCMethod
    Boolean isConnectionUnitRegistryConsistent();

    /**
     * Method returns true if the underling registry is marked as consistent.
     * <p>
     * Note: Method returns true in case the registry is not available. Maybe you need to check this in advance.
     *
     * @return true if consistent
     */
    @RPCMethod
    Boolean isAgentUnitRegistryConsistent();

    /**
     * Method returns true if the underling registry is marked as consistent.
     * <p>
     * Note: Method returns true in case the registry is not available. Maybe you need to check this in advance.
     *
     * @return true if consistent
     */
    @RPCMethod
    Boolean isAppUnitRegistryConsistent();

    /**
     * Method returns true if the underling registry is marked as consistent.
     * <p>
     * Note: Method returns true in case the registry is not available. Maybe you need to check this in advance.
     *
     * @return true if consistent
     */
    @RPCMethod
    Boolean isSceneUnitRegistryConsistent();

    /**
     * Method returns true if the underling registry is marked as consistent.
     * <p>
     * Note: Method returns true in case the registry is not available. Maybe you need to check this in advance.
     *
     * @return true if consistent
     */
    @RPCMethod
    Boolean isObjectUnitRegistryConsistent();

    default List<UnitConfig> getUnitConfigsByCoordinate(final Vec3DDouble coordinate) throws CouldNotPerformException {
        return getUnitConfigsByCoordinate(coordinate, DEFAULT_RADIUS, UnitType.UNKNOWN);
    }

    /**
     * Returns all units which are placed within the sphere which center is defined by the given {@code coordinate} and the size defined by the {@code radius}.
     *
     * @param coordinate the center of the sphere.
     * @param radius     the radius of the sphere.
     *
     * @return all units placed in the sphere.
     *
     * @throws CouldNotPerformException thrown if the computation fails.
     * @deprecated since 2.0 and will be removed in 3.0: please use getUnitConfigsByCoordinateAndRadius(...) instead.
     */
    @Deprecated
    default List<UnitConfig> getUnitConfigsByCoordinate(final Vec3DDouble coordinate, final double radius) throws CouldNotPerformException {
        return getUnitConfigsByCoordinateAndRadius(coordinate, radius);
    }

    /**
     * Returns all units which are placed within the sphere which center is defined by the given {@code coordinate} and the size defined by the {@code radius}.
     *
     * @param coordinate the center of the sphere.
     * @param radius     the radius of the sphere.
     *
     * @return all units included in the sphere.
     *
     * @throws CouldNotPerformException thrown if the computation fails.
     */
    default List<UnitConfig> getUnitConfigsByCoordinateAndRadius(final Vec3DDouble coordinate, final double radius) throws CouldNotPerformException {
        return getUnitConfigsByCoordinate(coordinate, radius, UnitType.UNKNOWN);
    }

    /**
     * Method detects Returns all units which are placed within the sphere which center is defined by the given {@code coordinate} and the size defined by the {@code radius}.
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
     * @deprecated since 2.0 and will be removed in 3.0: please use getUnitConfigsByCoordinateAndRadiusAndUnitType(...) instead.
     */
    @Deprecated
    default List<UnitConfig> getUnitConfigsByCoordinate(final Vec3DDouble coordinate, final double radius, final UnitType unitType) throws CouldNotPerformException {
        return getUnitConfigsByCoordinateAndRadiusAndUnitType(coordinate, radius, unitType);
    }

    /**
     * Method returns a list of {@code UnitConfig} instances sorted by the distance to the given {@code coordinate} starting with the lowest one.
     * The lookup time can be reduced by filtering the results with a {@code UnitType} where the {@code UnitType.UNKNOWN} is used as wildcard.
     * The given radius can be used to limit the result as well but will not speed up the lookup.
     *
     * @param coordinate the center of the sphere.
     * @param radius     the radius of the sphere.
     * @param unitType   filter lets only pass units of this declared type.
     *
     * @return all units placed in the sphere.
     *
     * @throws CouldNotPerformException thrown if the computation fails.
     */
    default List<UnitConfig> getUnitConfigsByCoordinateAndRadiusAndUnitType(final Vec3DDouble coordinate, final double radius, final UnitType unitType) throws CouldNotPerformException {

        // init
        TreeMap<Double, UnitConfig> result = new TreeMap<>();
        final Point3d unitPosition = new Point3d(coordinate.getX(), coordinate.getY(), coordinate.getZ());

        // lookup distances
        for (final UnitConfig unitConfig : getUnitConfigsByUnitType(unitType)) {
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
    default String getUserUnitIdByUserName(final String userName) throws CouldNotPerformException, NotAvailableException {
        validateData();
        List<UnitConfig> messages = getUnitConfigsByUnitType(UnitType.USER);

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
     *
     * @return a list of the requested unit configs.
     *
     * @throws CouldNotPerformException is thrown if the request fails.
     * @deprecated since 2.0 and will be removed in 3.0: please use getLocationUnitConfigsByTypeLocation(...) instead.
     */
    @Deprecated
    default List<UnitConfig> getLocationUnitConfigsByType(final LocationType locationType) throws CouldNotPerformException {
        return getLocationUnitConfigsByTypeLocation(locationType);
    }

    /**
     * Method returns all location unit configs which are of the given location type.
     *
     * @param locationType the type of the location.
     *
     * @return a list of the requested unit configs.
     *
     * @throws CouldNotPerformException is thrown if the request fails.
     */
    default List<UnitConfig> getLocationUnitConfigsByTypeLocation(final LocationType locationType) throws CouldNotPerformException {
        List<UnitConfig> list = new ArrayList<>();
        for (UnitConfig locationUnitConfig : getUnitConfigsByUnitType(UnitType.LOCATION)) {
            if (locationUnitConfig.getLocationConfig().getLocationType() == locationType) {
                list.add(locationUnitConfig);
            }
        }
        return list;
    }

    /**
     * Call to {@link #getLocationUnitConfigsByCoordinateAndLocationType(Vec3DDouble, LocationType)} with location type unknown.
     *
     * @param coordinate the coordinate for which it is checked if it is inside a location.
     *
     * @return a list of the requested unit configs sorted by location type.
     *
     * @throws CouldNotPerformException       is thrown if the request fails.
     * @throws java.lang.InterruptedException is thrown if the process is interrupted
     */
    default List<UnitConfig> getLocationUnitConfigsByCoordinate(final Vec3DDouble coordinate) throws CouldNotPerformException, InterruptedException {
        return getLocationUnitConfigsByCoordinateAndLocationType(coordinate, LocationType.UNKNOWN);
    }

    /**
     * Method returns all the locations which contain the given coordinate and
     * belong to the given location type.
     * In case the location type is unknown all locations are considered and the resulting
     * list is sorted so that regions are in the front followed by a tile and zones.
     *
     * @param coordinate   the coordinate for which it is checked if it is inside a location
     * @param locationType the type of locations checked, unknown means all locations
     *
     * @return a list of the requested unit configs sorted by location type
     *
     * @throws CouldNotPerformException       is thrown if the request fails.
     * @throws java.lang.InterruptedException is thrown if the process is interrupted
     * @deprecated since 2.0 and will be removed in 3.0: please use getLocationUnitConfigsByCoordinateAndLocationType(...) instead.
     */
    @Deprecated
    default List<UnitConfig> getLocationUnitConfigsByCoordinate(final Vec3DDouble coordinate, final LocationType locationType) throws CouldNotPerformException, InterruptedException {
        return getLocationUnitConfigsByCoordinateAndLocationType(coordinate, locationType);
    }

    /**
     * Method returns all the locations which contain the given coordinate and
     * belong to the given location type.
     * In case the location type is unknown all locations are considered and the resulting
     * list is sorted so that regions are in the front followed by a tile and zones.
     *
     * @param coordinate   the coordinate for which it is checked if it is inside a location
     * @param locationType the type of locations checked, unknown means all locations
     *
     * @return a list of the requested unit configs sorted by location type
     *
     * @throws CouldNotPerformException       is thrown if the request fails.
     * @throws java.lang.InterruptedException is thrown if the process is interrupted
     */
    default List<UnitConfig> getLocationUnitConfigsByCoordinateAndLocationType(final Vec3DDouble coordinate, final LocationType locationType) throws CouldNotPerformException, InterruptedException {
        validateData();
        List<UnitConfig> result = new ArrayList<>();

        try {
            for (UnitConfig locationUnitConfig : getUnitConfigsByUnitType(UnitType.LOCATION)) {
                // Check if the unit meets the requirements of the filter
                if (!locationType.equals(LocationType.UNKNOWN) && !locationType.equals(locationUnitConfig.getLocationConfig().getLocationType())) {
                    continue;
                }

                // Get the shape of the floor
                List<Vec3DDoubleType.Vec3DDouble> floorList = locationUnitConfig.getPlacementConfig().getShape().getFloorList();

                // Convert the shape into a PolygonsSet
                List<Vector2D> vertices = new ArrayList<>();
                for (Vec3DDouble vec3DDouble : floorList) {
                    Vector2D vector2D = new Vector2D(vec3DDouble.getX(), vec3DDouble.getY());
                    vertices.add(vector2D);
                }
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
        } catch (ExecutionException ex) {
            throw new CouldNotPerformException("Could not resolve location configs by coordinate", ex);
        }

        if (locationType == LocationType.UNKNOWN) {
            Collections.sort(result, (o1, o2) -> {
                switch (o1.getLocationConfig().getLocationType()) {
                    case REGION:
                        switch (o2.getLocationConfig().getLocationType()) {
                            case REGION:
                                return 0;
                            default:
                                // o1 is smaller than o2
                                return -1;
                        }
                    case TILE:
                        switch (o2.getLocationConfig().getLocationType()) {
                            case REGION:
                                // o1 is bigger than o2
                                return 1;
                            case TILE:
                                return 0;
                            case ZONE:
                                // o1 is smaller than o2
                                return -1;
                        }
                    case ZONE:
                        switch (o2.getLocationConfig().getLocationType()) {
                            case REGION:
                            case TILE:
                                return 1;
                            case ZONE:
                                return 0;
                        }
                }
                // location type is unknown so move to the end
                return 1;
            });
        }

        return result;
    }

    /**
     * Method returns all unit configurations which are direct or recursive related to the given location id.
     *
     * @param locationId the id of the location which provides the units.
     *
     * @return A collection of unit configs.
     *
     * @throws CouldNotPerformException is thrown if the request fails.
     * @deprecated since 2.0 and will be removed in 3.0: please use getUnitConfigsByLocationId(...) instead.
     */
    @Deprecated
    default List<UnitConfig> getUnitConfigsByLocation(final String locationId) throws CouldNotPerformException {
        return getUnitConfigsByLocationId(locationId);
    }

    /**
     * Method returns all unit configurations which are direct or recursive related to the given location id.
     *
     * @param locationId the id of the location which provides the units.
     *
     * @return A collection of unit configs.
     *
     * @throws CouldNotPerformException is thrown if the request fails.
     */
    default List<UnitConfig> getUnitConfigsByLocationId(final String locationId) throws CouldNotPerformException {
        return getUnitConfigsByLocation(locationId, true);
    }

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
     * @deprecated since 2.0 and will be removed in 3.0: please use getUnitConfigsByLocationIdRecursive(...) instead.
     */
    @Deprecated
    default List<UnitConfig> getUnitConfigsByLocation(final String locationId, final boolean recursive) throws CouldNotPerformException {
        return getUnitConfigsByLocationIdRecursive(locationId, recursive);
    }

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
    default List<UnitConfig> getUnitConfigsByLocationIdRecursive(final String locationId, final boolean recursive) throws CouldNotPerformException {
        final List<UnitConfig> unitConfigList = new ArrayList<>();
        for (final String unitConfigId : getUnitConfigById(locationId).getLocationConfig().getUnitIdList()) {
            final UnitConfig unitConfig = getUnitConfigById(unitConfigId);
            if (recursive || unitConfig.getPlacementConfig().getLocationId().equals(locationId)) {
                unitConfigList.add(unitConfig);
            }
        }
        return unitConfigList;
    }

    /**
     * Method returns all unit configurations which are direct or recursive related to the given location id and an
     * instance of the given unit type. If the unit type is unknown or location, all child locations of the provided
     * locations are resolved.
     *
     * @param unitType   the unit type after which unit configs are filtered.
     * @param locationId the location inside which unit configs are resolved.
     *
     * @return A collection of unit configs.
     *
     * @throws CouldNotPerformException is thrown if the request fails.
     * @deprecated since 2.0 and will be removed in 3.0: please use getUnitConfigsByLocationIdAndUnitType(...) instead.
     */
    @Deprecated
    default List<UnitConfig> getUnitConfigsByLocation(final UnitType unitType, final String locationId) throws CouldNotPerformException {
        return getUnitConfigsByLocationIdAndUnitType(locationId, unitType);
    }

    /**
     * Method returns all unit configurations which are direct or recursive related to the given location id and an
     * instance of the given unit type. If the unit type is unknown or location, all child locations of the provided
     * locations are resolved.
     *
     * @param unitType   the unit type after which unit configs are filtered.
     * @param locationId the location inside which unit configs are resolved.
     *
     * @return A collection of unit configs.
     *
     * @throws CouldNotPerformException is thrown if the request fails.
     */
    default List<UnitConfig> getUnitConfigsByLocationIdAndUnitType(final String locationId, final UnitType unitType) throws CouldNotPerformException {
        return getUnitConfigsByLocation(unitType, locationId, true);
    }

    /**
     * Method returns all unit configurations which are direct related to the given location id and an
     * instance of the given unit type. If the unit type is unknown or location, all child locations of the provided
     * locations are resolved. In case the {@code recursive} flag is set to true than recursive related units are included as well.
     *
     * @param unitType   the unit type after which unit configs are filtered.
     * @param locationId the location inside which unit configs are resolved.
     * @param recursive  defines if recursive related unit should be included as well.
     *
     * @return A collection of unit configs.
     *
     * @throws CouldNotPerformException is thrown if the request fails.
     * @deprecated since 2.0 and will be removed in 3.0: please use getUnitConfigsByCoordinateAndRadiusAndUnitType(...) instead.
     */
    @Deprecated
    default List<UnitConfig> getUnitConfigsByLocation(final UnitType unitType, final String locationId, final boolean recursive) throws CouldNotPerformException {
        return getUnitConfigsByLocationIdAndUnitTypeRecursive(locationId, unitType, recursive);
    }

    /**
     * Method returns all unit configurations which are direct related to the given location id and an
     * instance of the given unit type. If the unit type is unknown or location, all child locations of the provided
     * locations are resolved. In case the {@code recursive} flag is set to true than recursive related units are included as well.
     *
     * @param unitType   the unit type after which unit configs are filtered.
     * @param locationId the location inside which unit configs are resolved.
     * @param recursive  defines if recursive related unit should be included as well.
     *
     * @return A collection of unit configs.
     *
     * @throws CouldNotPerformException is thrown if the request fails.
     */
    default List<UnitConfig> getUnitConfigsByLocationIdAndUnitTypeRecursive(final String locationId, final UnitType unitType, final boolean recursive) throws CouldNotPerformException {
        final List<UnitConfig> unitConfigList = new ArrayList<>();
        final UnitConfig location = getUnitConfigById(locationId);

        // if unit type is unknown or location resolve all child locations
        if (unitType == UnitType.UNKNOWN || unitType == UnitType.LOCATION) {
            for (final String childId : location.getLocationConfig().getChildIdList()) {
                final UnitConfig unitConfig = getUnitConfigById(childId);
                if (recursive || unitConfig.getPlacementConfig().getLocationId().equals(locationId)) {
                    unitConfigList.add(unitConfig);
                }
            }

            for (int i = 0; i < unitConfigList.size(); i++) {
                for (final String childId : unitConfigList.get(i).getLocationConfig().getChildIdList()) {
                    final UnitConfig unitConfig = getUnitConfigById(childId);
                    if (recursive || unitConfig.getPlacementConfig().getLocationId().equals(locationId)) {
                        unitConfigList.add(unitConfig);
                    }
                }
            }
        }

        // if unit type is not location resolve all units with given type inside the location
        if (unitType != UnitType.LOCATION) {
            UnitConfig unitConfig;
            for (final String unitConfigId : getUnitConfigById(locationId).getLocationConfig().getUnitIdList()) {
                unitConfig = getUnitConfigById(unitConfigId);
                if (unitType == UnitType.UNKNOWN || unitConfig.getUnitType().equals(unitType) || CachedTemplateRegistryRemote.getRegistry().getSubUnitTypes(unitType).contains(unitConfig.getUnitType())) {
                    if (recursive || unitConfig.getPlacementConfig().getLocationId().equals(locationId)) {
                        unitConfigList.add(unitConfig);
                    }
                }
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
     *
     * @return A collection of unit configs.
     *
     * @throws CouldNotPerformException is thrown if the request fails.
     * @throws NotAvailableException
     * @deprecated since 2.0 and will be removed in 3.0: please use getUnitConfigsByLocationIdAndServiceType(...) instead.
     */
    @Deprecated
    default List<UnitConfig> getUnitConfigsByLocation(final ServiceType type, final String locationConfigId) throws CouldNotPerformException, NotAvailableException {
        return getUnitConfigsByLocationIdAndServiceType(locationConfigId, type);
    }

    /**
     * Method returns all unit configurations which are direct or recursive
     * related to the given location id and an implement the given service type.
     *
     * @param locationConfigId related location.
     * @param serviceType      service type filter.
     *
     * @return A collection of unit configs.
     *
     * @throws CouldNotPerformException is thrown if the request fails.
     * @throws NotAvailableException
     */
    default List<UnitConfig> getUnitConfigsByLocationIdAndServiceType(final String locationConfigId, final ServiceType serviceType) throws CouldNotPerformException, NotAvailableException {
        List<UnitConfig> unitConfigList = new ArrayList<>();
        UnitConfig unitConfig;

        for (String unitConfigId : getUnitConfigById(locationConfigId).getLocationConfig().getUnitIdList()) {
            try {
                unitConfig = getUnitConfigById(unitConfigId);
                for (ServiceConfig serviceConfig : unitConfig.getServiceConfigList()) {
                    if (serviceConfig.getServiceDescription().getServiceType().equals(serviceType)) {
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
     *
     * @return the list of service configurations.
     *
     * @throws CouldNotPerformException is thrown if the request fails.
     * @throws NotAvailableException    is thrown if the given location config id
     *                                  is unknown.
     * @deprecated since 2.0 and will be removed in 3.0: please use getServiceConfigsByLocationId(...) instead.
     */
    @Deprecated
    default List<ServiceConfig> getServiceConfigsByLocation(final String locationId) throws CouldNotPerformException {
        return getServiceConfigsByLocationId(locationId);
    }

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
    default List<ServiceConfig> getServiceConfigsByLocationId(final String locationId) throws CouldNotPerformException {
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
     *
     * @return A collection of unit configs.
     *
     * @throws CouldNotPerformException is thrown if the request fails.
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
     * defined location and match the given unit label.
     * Testing the label is done using {@link LabelProcessor#contains(Label, String)}.
     *
     * @param unitLabel  the label tested
     * @param locationId the id of the location from which units are returned
     *
     * @return a list of unit configs containing the given label withing the given location
     *
     * @throws CouldNotPerformException is thrown if the request fails.
     * @deprecated since 2.0 and will be removed in 3.0: please use getUnitConfigsByLocationIdAndUnitLabel(...) instead.
     */
    @Deprecated
    default List<UnitConfig> getUnitConfigsByLabelAndLocation(final String unitLabel, final String locationId) throws CouldNotPerformException {
        return getUnitConfigsByLocationIdAndUnitLabel(locationId, unitLabel);
    }

    /**
     * Method returns a collection of unit configs which are located within the
     * defined location and match the given unit label.
     * Testing the label is done using {@link LabelProcessor#contains(Label, String)}.
     *
     * @param unitLabel  the label tested
     * @param locationId the id of the location from which units are returned
     *
     * @return a list of unit configs containing the given label withing the given location
     *
     * @throws CouldNotPerformException is thrown if the request fails.
     */
    default List<UnitConfig> getUnitConfigsByLocationIdAndUnitLabel(final String locationId, final String unitLabel) throws CouldNotPerformException {
        return getUnitConfigsByLocationIdAndUnitLabelRecursive(locationId, unitLabel, true);
    }

    /**
     * Method returns a collection of unit configs which are located within the
     * defined location and match the given unit label.
     * Testing the label is done using {@link LabelProcessor#contains(Label, String)}.
     * If the recursive flag is set all units in the location are considered.
     * Else only units directly placed in the location are considered.
     *
     * @param unitLabel  the label tested
     * @param locationId the id of the location from which units are returned
     * @param recursive  flag determining if the whole location tree should be considered
     *
     * @return a list of unit configs containing the given label withing the given location
     *
     * @throws CouldNotPerformException is thrown if the request fails.
     * @deprecated since 2.0 and will be removed in 3.0: please use getUnitConfigsByLocationIdAndUnitLabelRecursive(...) instead.
     */
    @Deprecated
    default List<UnitConfig> getUnitConfigsByLabelAndLocation(final String unitLabel, final String locationId, final boolean recursive) throws CouldNotPerformException {
        return getUnitConfigsByLocationIdAndUnitLabelRecursive(locationId, unitLabel, recursive);
    }

    /**
     * Method returns a collection of unit configs which are located within the
     * defined location and match the given unit label.
     * Testing the label is done using {@link LabelProcessor#contains(Label, String)}.
     * If the recursive flag is set all units in the location are considered.
     * Else only units directly placed in the location are considered.
     *
     * @param locationId the id of the location from which units are returned
     * @param unitLabel  the label tested
     * @param recursive  flag determining if the whole location tree should be considered
     *
     * @return a list of unit configs containing the given label withing the given location
     *
     * @throws CouldNotPerformException is thrown if the request fails.
     */
    default List<UnitConfig> getUnitConfigsByLocationIdAndUnitLabelRecursive(final String locationId, final String unitLabel, final boolean recursive) throws CouldNotPerformException {
        List<UnitConfig> list = new ArrayList<>();
        for (UnitConfig unitConfig : getUnitConfigsByLocationIdRecursive(locationId, recursive)) {
            if (LabelProcessor.contains(unitConfig.getLabel(), unitLabel)) {
                list.add(unitConfig);
            }
        }
        return list;
    }

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
     * @deprecated since 2.0 and will be removed in 3.0: please use getUnitConfigsByLocationAliasAndUnitType(...) instead.
     */
    @Deprecated
    default List<UnitConfig> getUnitConfigsByLocationAlias(final UnitType unitType, final String locationAlias) throws CouldNotPerformException {
        return getUnitConfigsByLocationAliasAndUnitType(locationAlias, unitType);
    }

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
    default List<UnitConfig> getUnitConfigsByLocationAliasAndUnitType(final String locationAlias, final UnitType unitType) throws CouldNotPerformException {
        final HashMap<String, UnitConfig> unitConfigMap = new HashMap<>();
        for (UnitConfig unitConfig : getUnitConfigsByLocationIdAndUnitType(getUnitConfigByAlias(locationAlias).getId(), unitType)) {
            unitConfigMap.put(unitConfig.getId(), unitConfig);
        }
        return new ArrayList<>(unitConfigMap.values());
    }

    /**
     * Method generates a list of service types supported by the given location.
     *
     * @param locationId the location to filter the types.
     *
     * @return a list of supported service types.
     *
     * @throws NotAvailableException is thrown in case the list could not be computed.
     * @deprecated since 2.0 and will be removed in 3.0: please use getServiceTypesByLocationId(...) instead.
     */
    @Deprecated
    default Set<ServiceType> getServiceTypesByLocation(final String locationId) throws CouldNotPerformException {
        return getServiceTypesByLocationId(locationId);
    }

    /**
     * Method generates a list of service types supported by the given location.
     *
     * @param locationId the location to filter the types.
     *
     * @return a list of supported service types.
     *
     * @throws NotAvailableException is thrown in case the list could not be computed.
     */
    default Set<ServiceType> getServiceTypesByLocationId(final String locationId) throws CouldNotPerformException {
        final Set<ServiceType> serviceTypeSet = new HashSet<>();
        for (final UnitConfig unitConfig : getUnitConfigsByLocationId(locationId)) {
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
     *
     * @return A collection of unit configs.
     *
     * @throws CouldNotPerformException is thrown if the request fails.
     * @deprecated since 2.0 and will be removed in 3.0: please use getUnitConfigsByConnectionId(...) instead.
     */
    @Deprecated
    default List<UnitConfig> getUnitConfigsByConnection(final String connectionConfigId) throws CouldNotPerformException {
        return getUnitConfigsByConnectionId(connectionConfigId);
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
    default List<UnitConfig> getUnitConfigsByConnectionId(final String connectionConfigId) throws CouldNotPerformException {
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
     *
     * @return A collection of unit configs.
     *
     * @throws CouldNotPerformException is thrown if the request fails.
     * @throws NotAvailableException
     * @deprecated since 2.0 and will be removed in 3.0: please use getUnitConfigsByConnectionIdAndUnitType(...) instead.
     */
    @Deprecated
    default List<UnitConfig> getUnitConfigsByConnection(final UnitType type, final String connectionConfigId) throws CouldNotPerformException, NotAvailableException {
        return getUnitConfigsByConnectionIdAndUnitType(connectionConfigId, type);
    }

    /**
     * Method returns all unit configurations which are related to the given
     * connection id and an instance of the given unit type.
     *
     * @param connectionConfigId
     * @param unitType
     *
     * @return A collection of unit configs.
     *
     * @throws CouldNotPerformException is thrown if the request fails.
     * @throws NotAvailableException
     */
    default List<UnitConfig> getUnitConfigsByConnectionIdAndUnitType(final String connectionConfigId, final UnitType unitType) throws CouldNotPerformException, NotAvailableException {
        List<UnitConfig> unitConfigList = new ArrayList<>();
        UnitConfig unitConfig;

        for (String unitConfigId : getUnitConfigById(connectionConfigId).getConnectionConfig().getUnitIdList()) {
            try {
                unitConfig = getUnitConfigById(unitConfigId);
                if (unitConfig.getUnitType().equals(unitType) || CachedTemplateRegistryRemote.getRegistry().getSubUnitTypes(unitType).contains(unitConfig.getUnitType())) {
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
     *
     * @return A collection of unit configs.
     *
     * @throws CouldNotPerformException is thrown if the request fails.
     * @throws NotAvailableException
     * @deprecated since 2.0 and will be removed in 3.0: please use getUnitConfigsByConnectionIdAndServiceType(...) instead.
     */
    @Deprecated
    default List<UnitConfig> getUnitConfigsByConnection(final ServiceType type, final String connectionConfigId) throws CouldNotPerformException, NotAvailableException {
        return getUnitConfigsByConnectionIdAndServiceType(connectionConfigId, type);
    }

    /**
     * Method returns all unit configurations which are related to the given
     * connection id and an implement the given service type.
     *
     * @param connectionConfigId related connection.
     * @param serviceType        service type filter.
     *
     * @return A collection of unit configs.
     *
     * @throws CouldNotPerformException is thrown if the request fails.
     * @throws NotAvailableException
     */
    default List<UnitConfig> getUnitConfigsByConnectionIdAndServiceType(final String connectionConfigId, final ServiceType serviceType) throws CouldNotPerformException, NotAvailableException {
        List<UnitConfig> unitConfigList = new ArrayList<>();
        UnitConfig unitConfig;

        for (String unitConfigId : getUnitConfigById(connectionConfigId).getConnectionConfig().getUnitIdList()) {
            try {
                unitConfig = getUnitConfigById(unitConfigId);
                for (ServiceConfig serviceConfig : unitConfig.getServiceConfigList()) {
                    if (serviceConfig.getServiceDescription().getServiceType().equals(serviceType)) {
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
     *
     * @return the list of service configurations.
     *
     * @throws CouldNotPerformException is thrown if the request fails.
     * @throws NotAvailableException    is thrown if the given connection config id
     *                                  is unknown.
     * @deprecated since 2.0 and will be removed in 3.0: please use getServiceConfigsByConnectionId(...) instead.
     */
    @Deprecated
    default List<ServiceConfig> getServiceConfigsByConnection(final String connectionConfigId) throws CouldNotPerformException {
        return getServiceConfigsByConnectionId(connectionConfigId);
    }

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
    default List<ServiceConfig> getServiceConfigsByConnectionId(final String connectionConfigId) throws CouldNotPerformException {
        List<ServiceConfig> serviceConfigList = new ArrayList<>();
        for (UnitConfig unitConfig : getUnitConfigsByConnectionId(connectionConfigId)) {
            serviceConfigList.addAll(unitConfig.getServiceConfigList());
        }
        return serviceConfigList;
    }

    /**
     * Method returns all neighbor tiles for a tile. If the given locationId
     * does not belong to a tile, the could not perform exception is thrown.
     *
     * @param locationId the id of the location which neighbors you want to get
     *
     * @return all neighbor tiles
     *
     * @throws CouldNotPerformException is thrown if the request fails.
     * @deprecated since 2.0 and will be removed in 3.0: please use getNeighborLocationsByLocationId(...) instead.
     */
    @Deprecated
    default List<UnitConfig> getNeighborLocations(final String locationId) throws CouldNotPerformException {
        return getNeighborLocationsByLocationId(locationId);
    }

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
    default List<UnitConfig> getNeighborLocationsByLocationId(final String locationId) throws CouldNotPerformException {
        final UnitConfig locationConfig = getUnitConfigByIdAndUnitType(locationId, UnitType.LOCATION);
        if (locationConfig.getLocationConfig().getLocationType() != LocationType.TILE) {
            throw new CouldNotPerformException("Id[" + locationId + "] does not belong to a tile and therefore its neighbors aren't defined!");
        }

        final Map<String, UnitConfig> neighborMap = new HashMap<>();
        for (UnitConfig connectionConfig : getUnitConfigsByUnitType(UnitType.CONNECTION)) {
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
     * Method returns all agent unit configs which are based on the given agent class.
     *
     * @param agentClass the agent class to identify the units.
     *
     * @return a list of matching agent configs.
     *
     * @throws CouldNotPerformException is thrown in case the list could not be generated.
     */
    default List<UnitConfig> getAgentUnitConfigsByAgentClass(final AgentClass agentClass) throws CouldNotPerformException {
        return getAgentUnitConfigsByAgentClassId(agentClass.getId());
    }

    /**
     * Method returns all agent unit configs which are based on the given agent class.
     *
     * @param agentClassId the if of the agent class to identify the units.
     *
     * @return a list of matching agent configs.
     *
     * @throws CouldNotPerformException is thrown in case the list could not be generated.
     */
    default List<UnitConfig> getAgentUnitConfigsByAgentClassId(final String agentClassId) throws CouldNotPerformException {
        if (!CachedClassRegistryRemote.getRegistry().containsAgentClassById(agentClassId)) {
            throw new NotAvailableException("agentClassId [" + agentClassId + "]");
        }

        List<UnitConfig> agentConfigs = new ArrayList<>();
        for (UnitConfig agentConfig : getUnitConfigsByUnitType(UnitType.AGENT)) {
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
     *
     * @return a list of matching app configs.
     *
     * @throws CouldNotPerformException is thrown in case the list could not be generated.
     */
    default List<UnitConfig> getAppUnitConfigsByAppClass(final AppClass appClass) throws CouldNotPerformException, InterruptedException {
        return getAppUnitConfigsByAppClassId(appClass.getId());
    }

    /**
     * Method returns all app unit configs which are based on the given app class.
     *
     * @param appClassId the if of the app class to identify the units.
     *
     * @return a list of matching app configs.
     *
     * @throws CouldNotPerformException is thrown in case the list could not be generated.
     */
    default List<UnitConfig> getAppUnitConfigsByAppClassId(final String appClassId) throws CouldNotPerformException, InterruptedException {
        if (!CachedClassRegistryRemote.getRegistry().containsAppClassById(appClassId)) {
            throw new NotAvailableException("appClassId [" + appClassId + "]");
        }

        final List<UnitConfig> appConfigs = new ArrayList<>();
        for (UnitConfig appConfig : getUnitConfigsByUnitType(UnitType.APP)) {
            if (appConfig.getAppConfig().getAppClassId().equals(appClassId)) {
                appConfigs.add(appConfig);
            }
        }
        return appConfigs;
    }

    /**
     * Request an authorization token. This methods validates that the user defined in the token has all
     * the permissions necessary to create the token. If the authorizationToken is valid it is encrypted with
     * the service server secret key and encoded as a string using Base64. This string can be distributed to
     * other components which allows them to execute actions in the users name with permissions as defined in the
     * token.
     * <p>
     * NOTE: This method is not annotated as an RPCMethod on purpose. Tokens can only be requested when authenticated.
     * Else the token cannot be verified and encrypted which means everybody listening could use it.
     *
     * @param authorizationToken the authorizationToken which is verified an encrypted
     *
     * @return a future of a task that verifies and encrypts the token
     */
    Future<String> requestAuthorizationToken(final AuthorizationToken authorizationToken);

    /**
     * Request an authorization token while being authenticated. The ticket in the authenticated value is used
     * to authenticate the user and the value in the authenticated value has to be an authorizationToken encrypted with the session key.
     * The user id in the authorization id is verified to be the one of the authenticated user. If the id is not set
     * id will be set to the one of the authenticated user. Afterwards {@link #requestAuthorizationToken(AuthorizationToken)}
     * is called internally to verify the permissions of the token, to encrypt it with the service server secret key
     * and encode it as a string using Base64.
     * The string will then be encrypted again using the session key and set as the value of the AuthenticatedValue.
     *
     * @param authenticatedValue The authenticated value for the request containing a valid ticket and an authorizationToken encrypted
     *                           with the session key.
     *
     * @return The future of a task that creates an authenticated value containing an updated ticket and the token encoded via Base64
     * and encrypted with the session key.
     */
    @RPCMethod
    Future<AuthenticatedValue> requestAuthorizationTokenAuthenticated(final AuthenticatedValue authenticatedValue);

    Future<String> requestAuthenticationToken(final AuthenticationToken authenticationToken);

    @RPCMethod
    Future<AuthenticatedValue> requestAuthenticationTokenAuthenticated(final AuthenticatedValue authenticatedValue);

    Map<String, IdentifiableMessage<String, UnitConfig, Builder>> getAuthorizationGroupMap() throws CouldNotPerformException;

    Map<String, IdentifiableMessage<String, UnitConfig, Builder>> getLocationMap() throws CouldNotPerformException;

//    Not yet implemented so temporally removed from interface
//
//    /**
//     * Method returns a list of probably intersected units by the given 3D ray.
//     * This could for example be useful for selecting units by pointing gestures.
//     *
//     * @param pointingRay3DFloat ray which probably intersects with a specific unit priorized by a given certainty.
//     *
//     * @return a collection of probably intersected units referred by there id.
//     */
//    @RPCMethod
//    Future<UnitProbabilityCollection> computeUnitIntersection(final PointingRay3DFloat pointingRay3DFloat);
//
//    /**
//     * Method returns a list of probably intersected units by the given 3D rays.
//     * This could for example be useful for selecting units by pointing gestures.
//     *
//     * @param pointingRay3DFloatCollection a collection of rays which probably intersects with a specific unit priorized by a given certainty.
//     *
//     * @return a collection of probably intersected units referred by there id.
//     */
//    @RPCMethod
//    Future<UnitProbabilityCollection> computeUnitIntersection(final PointingRay3DFloatCollection pointingRay3DFloatCollection);
}
