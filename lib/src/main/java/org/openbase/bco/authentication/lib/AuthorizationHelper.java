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
import java.util.HashMap;
import rst.domotic.authentication.PermissionConfigType.PermissionConfig;
import rst.domotic.authentication.PermissionConfigType.PermissionConfig.MapFieldEntry;
import rst.domotic.authentication.PermissionType.Permission;
import rst.domotic.unit.UnitConfigType.UnitConfig;
import rst.domotic.unit.authorizationgroup.AuthorizationGroupConfigType.AuthorizationGroupConfig;

/**
 * Helper class to determine the permissions for a given user on a given unit.
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
     * Checks whether a user has the permission to read from a unit,
     * for example to query information about the unit's state.
     *
     * @param unitConfig The unit the user wants to read.
     * @param userId ID of the user whose permissions should be checked.
     * @param groups All available groups in the system, indexed by their group ID.
     * @return True if the user can read from the unit, false if not.
     */
    public static boolean canRead(UnitConfig unitConfig, String userId, HashMap<String, AuthorizationGroupConfig> groups) {
        return canDo(unitConfig, userId, groups, Type.READ);
    }

    /**
     * Checks whether a user has the permission to write to a unit,
     * for example to run any action on a unit.
     *
     * @param unitConfig The unit the user wants to write to.
     * @param userId ID of the user whose permissions should be checked.
     * @param groups All available groups in the system, indexed by their group ID.
     * @return True if the user can write to the unit, false if not.
     */
    public static boolean canWrite(UnitConfig unitConfig, String userId, HashMap<String, AuthorizationGroupConfig> groups) {
        return canDo(unitConfig, userId, groups, Type.WRITE);
    }

    /**
     * Checks whether a user has the permission to access a unit.
     *
     * @param unitConfig The unit the user wants to access.
     * @param userId ID of the user whose permissions should be checked.
     * @param groups All available groups in the system, indexed by their group ID.
     * @return True if the user can access the unit, false if not.
     */
    public static boolean canAccess(UnitConfig unitConfig, String userId, HashMap<String, AuthorizationGroupConfig> groups) {
        return canDo(unitConfig, userId, groups, Type.ACCESS);
    }

    /**
     * Checks all permissions for a user.
     *
     * @param unitConfig The unit for which the permissions apply.
     * @param userId ID of the user whose permissions should be checked.
     * @param groups All available groups in the system, indexed by their group ID.
     * @return Permission object representing the maximum permissions for the given user on the given unit.
     */
    public static Permission getPermission(UnitConfig unitConfig, String userId, HashMap<String, AuthorizationGroupConfig> groups) {
        return Permission.newBuilder()
          .setAccess(canAccess(unitConfig, userId, groups))
          .setRead(canRead(unitConfig, userId, groups))
          .setWrite(canWrite(unitConfig, userId, groups))
          .build();
    }

    /**
     * Internal helper method to check one of the permissions on a unit.
     *
     * @param unitConfig The unit for which the permissions apply.
     * @param userId ID of the user whose permissions should be checked.
     * @param groups All available groups in the system, indexed by their group ID.
     * @param type The permission type to check.
     * @return True if the user has the given permission, false if not.
     */
    private static boolean canDo(UnitConfig unitConfig, String userId, HashMap<String, AuthorizationGroupConfig> groups, Type type) {
        PermissionConfig permissionConfig = unitConfig.getPermissionConfig();

        // Other
        if (permitted(permissionConfig.getOtherPermission(), type)) {
            return true;
        }

        // Owner
        if (permissionConfig.getOwnerId().equals(userId) && permitted(permissionConfig.getOwnerPermission(), type)) {
            return true;
        }

        // Groups
        ProtocolStringList groupMembers;

        for (MapFieldEntry entry : permissionConfig.getGroupPermissionList()) {
            groupMembers = groups.get(entry.getGroupId()).getMemberIdList();

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
