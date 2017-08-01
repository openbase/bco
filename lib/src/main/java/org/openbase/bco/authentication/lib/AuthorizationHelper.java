package org.openbase.bco.authentication.lib;

/*-
 * #%L
 * BCO Authentication Library
 * %%
 * Copyright (C) 2017 openbase.org
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
import org.openbase.jul.extension.protobuf.IdentifiableMessage;
import rst.domotic.authentication.PermissionConfigType.PermissionConfig;
import rst.domotic.authentication.PermissionConfigType.PermissionConfig.MapFieldEntry;
import rst.domotic.authentication.PermissionType.Permission;
import rst.domotic.unit.UnitConfigType.UnitConfig;

/**
 * Helper class to determine the permissions for a given user on a given permission configuration.
 * All methods return the highest permission, i.e. if the permission is true for any level
 * applying to the user, it can't be revoked at any other level and true will be returned.
 *
 * @author <a href="mailto:cromankiewicz@techfak.uni-bielefeld.de">Constantin Romankiewicz</a>
 */
public class AuthorizationHelper {
    private enum Type {
        READ,
        WRITE,
        ACCESS
    }

    /**
     * Checks whether a user has the permission to read from a permissionConfig,
     * for example to query information about the unit's state who has this permissionConfig.
     *
     * @param permissionConfig The permissionConfig the user wants to read.
     * @param userId ID of the user whose permissions should be checked.
     * @param groups All available groups in the system, indexed by their group ID.
     * @return True if the user can read from the unit, false if not.
     */
    public static boolean canRead(PermissionConfig permissionConfig, String userId, Map<String, IdentifiableMessage<String, UnitConfig, UnitConfig.Builder>> groups) {
        return canDo(permissionConfig, userId, groups, Type.READ);
    }

    /**
     * Checks whether a user has the permission to write to a something with the given permissionConfig,
     * for example to run any action on a unit.
     *
     * @param permissionConfig The permissionConfig of the unit the user wants to write to.
     * @param userId ID of the user whose permissions should be checked.
     * @param groups All available groups in the system, indexed by their group ID.
     * @return True if the user can write to the unit, false if not.
     */
    public static boolean canWrite(PermissionConfig permissionConfig, String userId, Map<String, IdentifiableMessage<String, UnitConfig, UnitConfig.Builder>> groups) {
        return canDo(permissionConfig, userId, groups, Type.WRITE);
    }

    /**
     * Checks whether a user has the permission to access a unit with the given permissionConfig.
     *
     * @param permissionConfig The permissionConfig of the unit the user wants to access.
     * @param userId ID of the user whose permissions should be checked.
     * @param groups All available groups in the system, indexed by their group ID.
     * @return True if the user can access the unit, false if not.
     */
    public static boolean canAccess(PermissionConfig permissionConfig, String userId, Map<String, IdentifiableMessage<String, UnitConfig, UnitConfig.Builder>> groups) {
        return canDo(permissionConfig, userId, groups, Type.ACCESS);
    }

    /**
     * Checks all permissions for a user.
     *
     * @param permissionConfig The permissionConfig of the unit for which the permissions apply.
     * @param userId ID of the user whose permissions should be checked.
     * @param groups All available groups in the system, indexed by their group ID.
     * @return Permission object representing the maximum permissions for the given user on the given unit.
     */
    public static Permission getPermission(PermissionConfig permissionConfig, String userId, Map<String, IdentifiableMessage<String, UnitConfig, UnitConfig.Builder>> groups) {
        return Permission.newBuilder()
          .setAccess(canAccess(permissionConfig, userId, groups))
          .setRead(canRead(permissionConfig, userId, groups))
          .setWrite(canWrite(permissionConfig, userId, groups))
          .build();
    }

    /**
     * Internal helper method to check one of the permissions on a unit.
     *
     * @param permissionConfig The unit for which the permissions apply.
     * @param userId ID of the user whose permissions should be checked.
     * @param groups All available groups in the system, indexed by their group ID.
     * @param type The permission type to check.
     * @return True if the user has the given permission, false if not.
     */
    private static boolean canDo(PermissionConfig permissionConfig, String userId, Map<String, IdentifiableMessage<String, UnitConfig, UnitConfig.Builder>> groups, Type type) {
        // Other
        if (permitted(permissionConfig.getOtherPermission(), type)) {
            return true;
        }

        // If no user was given, only "other" rights apply.
        if (userId == null) {
            return false;
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

        for (MapFieldEntry entry : permissionConfig.getGroupPermissionList()) {
            groupMembers = groups.get(entry.getGroupId()).getMessage().getAuthorizationGroupConfig().getMemberIdList();

            // Check if the user belongs to the group.
            if (groupMembers.contains(userId) && permitted(entry.getPermission(), type)) {
                return true;
            }
        }

        return false;
    }

    private static boolean permitted(Permission permission, Type type) {
        switch(type) {
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
}
