package org.openbase.bco.authentication.lib;

/*-
 * #%L
 * BCO Authentication Library
 * %%
 * Copyright (C) 2017 - 2018 openbase.org
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

import com.google.protobuf.ProtocolStringList;

import java.util.Map;

import jnr.ffi.annotations.In;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InvalidStateException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.exception.StackTracePrinter;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.exception.printer.LogLevel;
import org.openbase.jul.extension.protobuf.IdentifiableMessage;
import org.openbase.jul.extension.rsb.scope.ScopeGenerator;
import org.openbase.jul.processing.StringProcessor;
import org.slf4j.LoggerFactory;
import rst.domotic.authentication.PermissionConfigType.PermissionConfig;
import rst.domotic.authentication.PermissionConfigType.PermissionConfig.MapFieldEntry;
import rst.domotic.authentication.PermissionType.Permission;
import rst.domotic.unit.UnitConfigType.UnitConfig;
import rst.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;

import javax.xml.stream.events.NotationDeclaration;

/**
 * Helper class to determine the permissions for a given user on a given permission configuration.
 * All methods return the highest permission, i.e. if the permission is true for any level
 * applying to the user, it can't be revoked at any other level and true will be returned.
 *
 * @author <a href="mailto:cromankiewicz@techfak.uni-bielefeld.de">Constantin Romankiewicz</a>
 */
public class AuthorizationHelper {

    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(AuthorizationHelper.class);

    private enum Type {
        READ,
        WRITE,
        ACCESS
    }

    /**
     * Checks whether a user has the permission to read from a permissionConfig,
     * for example to query information about the unit's state who has this permissionConfig.
     *
     * @param unitConfig The unitConfig of the unit the user wants to read.
     * @param userId     ID of the user whose permissions should be checked.
     * @param groups     All available groups in the system, indexed by their group ID.
     * @param locations  All available locations in the system, indexed by their id.
     * @return True if the user can read from the unit, false if not.
     */
    public static boolean canRead(UnitConfig unitConfig, String userId, Map<String, IdentifiableMessage<String, UnitConfig, UnitConfig.Builder>> groups, Map<String, IdentifiableMessage<String, UnitConfig, UnitConfig.Builder>> locations) {
        return canDo(unitConfig, userId, groups, locations, Type.READ);
    }

    /**
     * Checks whether a user has the permission to write to a something with the given permissionConfig,
     * for example to run any action on a unit.
     *
     * @param unitConfig The unitConfig of the unit the user wants to write to.
     * @param userId     ID of the user whose permissions should be checked.
     * @param groups     All available groups in the system, indexed by their group ID.
     * @param locations  All available locations in the system, indexed by their id.
     * @return True if the user can write to the unit, false if not.
     */
    public static boolean canWrite(UnitConfig unitConfig, String userId, Map<String, IdentifiableMessage<String, UnitConfig, UnitConfig.Builder>> groups, Map<String, IdentifiableMessage<String, UnitConfig, UnitConfig.Builder>> locations) {
        return canDo(unitConfig, userId, groups, locations, Type.WRITE);
    }

    /**
     * Checks whether a user has the permission to access a unit with the given permissionConfig.
     *
     * @param unitConfig The unitConfig of the unit the user wants to access.
     * @param userId     ID of the user whose permissions should be checked.
     * @param groups     All available groups in the system, indexed by their group ID.
     * @param locations  All available locations in the system, indexed by their id.
     * @return True if the user can access the unit, false if not.
     */
    public static boolean canAccess(UnitConfig unitConfig, String userId, Map<String, IdentifiableMessage<String, UnitConfig, UnitConfig.Builder>> groups, Map<String, IdentifiableMessage<String, UnitConfig, UnitConfig.Builder>> locations) {
        return canDo(unitConfig, userId, groups, locations, Type.ACCESS);
    }

    /**
     * Checks all permissions for a user.
     *
     * @param unitConfig The unitConfig of the unit for which the permissions apply.
     * @param userId     ID of the user whose permissions should be checked.
     * @param groups     All available groups in the system, indexed by their group ID.
     * @param locations  All available locations in the system, indexed by their id.
     * @return Permission object representing the maximum permissions for the given user on the given unit.
     * @throws CouldNotPerformException If the permissions could not be checked, probably because of invalid location information.
     */
    public static Permission getPermission(UnitConfig unitConfig, String userId, Map<String, IdentifiableMessage<String, UnitConfig, UnitConfig.Builder>> groups, Map<String, IdentifiableMessage<String, UnitConfig, UnitConfig.Builder>> locations) throws CouldNotPerformException {
        return Permission.newBuilder()
                .setAccess(canAccess(unitConfig, userId, groups, locations))
                .setRead(canRead(unitConfig, userId, groups, locations))
                .setWrite(canWrite(unitConfig, userId, groups, locations))
                .build();
    }

    private static boolean canDo(UnitConfig unitConfig, String userId, Map<String, IdentifiableMessage<String, UnitConfig, UnitConfig.Builder>> groups, Map<String, IdentifiableMessage<String, UnitConfig, UnitConfig.Builder>> locations, Type type) {

        // BUG WORKAROUND
        // todo remove me after fixing openbase/bco.authentication#61
        if(unitConfig.getType() == UnitType.UNKNOWN) {
            return true;
        }

        if (!isAuthenticationUnit(unitConfig) && !isRootLocation(unitConfig, locations)) {
            // check if the given user has read permissions for the parent location otherwise skip all further checks
            try {
                if (!canRead(getLocationUnitConfig(unitConfig.getPlacementConfig().getLocationId(), locations), userId, groups, locations)) {
                    return false;
                }
            } catch (NotAvailableException ex) {
                String scope;
                try {
                    scope = ScopeGenerator.generateStringRep(unitConfig.getScope());
                } catch (CouldNotPerformException exx) {
                    scope = "?";
                }
                LOGGER.warn("PermissionConfig of Unit[" + scope + "] is denied!", ex);
                return false;
            }
        }

        try {
            return canDo(getPermissionConfig(unitConfig, locations), userId, groups, locations, type);
        } catch (CouldNotPerformException ex) {
            LOGGER.warn("can not perform the canDo check! Permission will be denied!", ex);
            return false;
        }
    }

    /**
     * Internal helper method to check one of the permissions on a unit.
     *
     * @param permissionConfig The unit for which the permissions apply.
     * @param userId           ID of the user whose permissions should be checked.
     * @param groups           All available groups in the system, indexed by their group ID.
     * @param type             The permission type to check.
     * @return True if the user has the given permission, false if not.
     */
    private static boolean canDo(final PermissionConfig permissionConfig, final String userId, final Map<String, IdentifiableMessage<String, UnitConfig, UnitConfig.Builder>> groups, final Map<String, IdentifiableMessage<String, UnitConfig, UnitConfig.Builder>> locations, Type type) {
        // Other
        if (permitted(permissionConfig.getOtherPermission(), type)) {
            return true;
        }

        // If no user was given, only "other" rights apply.
        if (userId == null) {
            return false;
        }

        // If the given ID has the form user@client, we check both.
        String[] split = userId.split("@", 2);

        if (split.length > 1) {
            return canDo(permissionConfig, split[0], groups, locations, type) || canDo(permissionConfig, split[1], groups, locations, type);
        }

        // Owner
        if (permissionConfig.getOwnerId().equals(userId) && permitted(permissionConfig.getOwnerPermission(), type)) {
            return true;
        }

        // Groups
        if (groups == null) {
            return false;
        }

        ProtocolStringList groupMembers;
        for (final MapFieldEntry entry : permissionConfig.getGroupPermissionList()) {
            if (groups.get(entry.getGroupId()) == null) {
                LOGGER.warn("No Group for id[" + entry.getGroupId() + "] available");
                continue;
            }
            groupMembers = groups.get(entry.getGroupId()).getMessage().getAuthorizationGroupConfig().getMemberIdList();

            // Check if the user belongs to the group.
            if (groupMembers.contains(userId) && permitted(entry.getPermission(), type)) {
                return true;
            }
        }

        return false;
    }

    private static boolean permitted(final Permission permission, final Type type) {
        switch (type) {
            case READ:
                return permission.getRead();
            case WRITE:
                return permission.getWrite();
            case ACCESS:
                return permission.getAccess();
            default:
                return false;
        }
    }

    private static PermissionConfig getPermissionConfig(final UnitConfig unitConfig, final Map<String, IdentifiableMessage<String, UnitConfig, UnitConfig.Builder>> locations) throws NotAvailableException {
        try {
            if (unitConfig == null) {
                throw new NotAvailableException("UnitConfig");
            }

            // the root location should always use its own permissions to terminate the recursive permission resolution.
            if (isRootLocation(unitConfig, locations)) {
                if (!unitConfig.hasPermissionConfig()) {
                    throw new InvalidStateException("The root location does not provide a permission config!");
                }
                return unitConfig.getPermissionConfig();
            }

            // user or authentication group permissions are independent of there location referred location.
            if (isAuthenticationUnit(unitConfig)) {
                if (!unitConfig.hasPermissionConfig()) {
                    throw new InvalidStateException(StringProcessor.transformUpperCaseToCamelCase(unitConfig.getType().name()) + " should always provide a permission config!");
                }
                return unitConfig.getPermissionConfig();
            }

            // verify needed location information
            if (locations == null || locations.isEmpty()) {
                throw new InvalidStateException("No location information available for permission resolution!");
            }

            PermissionConfig unitPermissionConfig;

            // resolve parent permissions
            try {
                final UnitConfig locationUnitConfig = getLocationUnitConfig(unitConfig.getPlacementConfig().getLocationId(), locations);
                unitPermissionConfig = getPermissionConfig(locationUnitConfig, locations);
            } catch (NotAvailableException ex) {
                throw new InvalidStateException("Parent location does not provide a permission config!", ex);
            }

            // resolve unit permissions and merge those with the parent location permissions
            if (unitConfig.hasPermissionConfig()) {
                unitPermissionConfig = mergePermissionConfigs(unitConfig.getPermissionConfig(), unitPermissionConfig);
            }

            return unitPermissionConfig;

        } catch (CouldNotPerformException ex) {
            String scope;
            try {
                scope = ScopeGenerator.generateStringRep(unitConfig.getScope());
            } catch (CouldNotPerformException exx) {
                scope = "?";
            }
            throw new NotAvailableException("PermissionConfig of Unit[" + scope + "]", ex);
        }
    }

    private static boolean isRootLocation(final UnitConfig unitConfig, final Map<String, IdentifiableMessage<String, UnitConfig, UnitConfig.Builder>> locations) {

        // if this unit is not a location it can not be a root location
        if (unitConfig.getType() != UnitType.LOCATION) {
            return false;
        }

        // if no locations are available this location should be the root location
        if (locations.isEmpty()) {
            return true;
        }

        // is this unit a root location?
        return unitConfig.getLocationConfig().hasRoot() && unitConfig.getLocationConfig().getRoot();
    }

    private static UnitConfig getRootLocationUnitConfig(final Map<String, IdentifiableMessage<String, UnitConfig, UnitConfig.Builder>> locations) throws NotAvailableException {
        try {
            for (final IdentifiableMessage<String, UnitConfig, UnitConfig.Builder> locationUnitConfig : locations.values()) {
                if (isRootLocation(locationUnitConfig.getMessage(), locations)) {
                    return locationUnitConfig.getMessage();
                }
            }
            throw new InvalidStateException("Registry does not provide a root location!");
        } catch (CouldNotPerformException ex) {
            throw new NotAvailableException("RootLocation", ex);
        }
    }

    private static boolean isAuthenticationUnit(final UnitConfig unitConfig) {
        switch (unitConfig.getType()) {
            case USER:
            case AUTHORIZATION_GROUP:
                return true;
            default:
                return false;
        }
    }

    private static UnitConfig getLocationUnitConfig(final String locationId, final Map<String, IdentifiableMessage<String, UnitConfig, UnitConfig.Builder>> locations) throws NotAvailableException {
        try {
            if (locationId.isEmpty()) {
                throw new NotAvailableException("locationId");
            }

            if (!locations.containsKey(locationId)) {
                LOGGER.warn("Registry does not contains requested location Entry[" + locationId + "] use root location as fallback to compute permissions.");
                return getRootLocationUnitConfig(locations);
            }
            return locations.get(locationId).getMessage();
        } catch (CouldNotPerformException | NullPointerException ex) {
            // null pointer can occur if the registry is shutting down between the "contains" check and the "get".
            throw new NotAvailableException("LocationConfig[" + locationId + "]", ex);
        }
    }

    private static PermissionConfig mergePermissionConfigs(final PermissionConfig unitPermissionConfig, final PermissionConfig parentLocationPermissionConfig) throws CouldNotPerformException {
        if (unitPermissionConfig == null) {
            throw new NotAvailableException("UserPermissionConfig");
        }

        if (parentLocationPermissionConfig == null) {
            throw new NotAvailableException("ParentLocationPermissionConfig");
        }

        final PermissionConfig.Builder builder = PermissionConfig.newBuilder(unitPermissionConfig);

        // merge other permission
        if (!unitPermissionConfig.hasOtherPermission() || !unitPermissionConfig.getOtherPermission().hasAccess() || !unitPermissionConfig.getOtherPermission().hasRead() || !unitPermissionConfig.getOtherPermission().hasWrite()) {
            builder.setOtherPermission(parentLocationPermissionConfig.getOtherPermission());
        }

        // merge owner permission
        if (!unitPermissionConfig.hasOwnerPermission() || !unitPermissionConfig.getOwnerPermission().hasAccess() || !unitPermissionConfig.getOwnerPermission().hasRead() || !unitPermissionConfig.getOwnerPermission().hasWrite()) {
            builder.setOwnerPermission(parentLocationPermissionConfig.getOwnerPermission());
        }

        boolean found = false;

        // merge group permissions
        for (MapFieldEntry locationEntry : parentLocationPermissionConfig.getGroupPermissionList()) {
            for (MapFieldEntry unitEntry : unitPermissionConfig.getGroupPermissionList()) {
                if (locationEntry.getGroupId().equals(unitEntry.getGroupId())) {
                    found = true;
                }
            }

            if (!found) {
                builder.addGroupPermission(locationEntry);
            }

            found = false;
        }

        return builder.build();
    }
}
