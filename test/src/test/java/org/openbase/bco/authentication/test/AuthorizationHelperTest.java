package org.openbase.bco.authentication.test;

/*-
 * #%L
 * BCO Authentication Test
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

import java.util.HashMap;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import org.openbase.bco.authentication.lib.AuthorizationHelper;
import rst.domotic.authentication.PermissionConfigType.PermissionConfig;
import rst.domotic.authentication.PermissionType.Permission;
import rst.domotic.unit.UnitConfigType.UnitConfig;

/**
 *
 * @author <a href="mailto:cromankiewicz@techfak.uni-bielefeld.de">Constantin Romankiewicz</a>
 */
import rst.domotic.unit.authorizationgroup.AuthorizationGroupConfigType.AuthorizationGroupConfig;
public class AuthorizationHelperTest {
    private static final Permission RWX = Permission.newBuilder().setRead(true).setWrite(true).setAccess(true).build();
    private static final Permission NONE = Permission.newBuilder().setRead(false).setWrite(false).setAccess(false).build();
    private HashMap<String, AuthorizationGroupConfig> groups;
    private static final String GROUP_1 = "group1";
    private static final String GROUP_2 = "group2";
    private static final String USER_1 = "user1";
    private static final String USER_2 = "user2";
    private static final String USER_3 = "user3";


    public AuthorizationHelperTest() {
        groups = new HashMap<>();
        AuthorizationGroupConfig group1 = AuthorizationGroupConfig.newBuilder()
          .addMemberId(USER_1).addMemberId(USER_2).build();
        AuthorizationGroupConfig group2 = AuthorizationGroupConfig.newBuilder()
          .addMemberId(USER_1).addMemberId(USER_3).build();
        groups.put(GROUP_1, group1);
        groups.put(GROUP_2, group2);
    }

    @BeforeClass
    public static void setUpClass() {
    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    /**
     * Tests all possible permission combinations for the owner, while keeping the "other" permissions false.
     */
    @Test
    public void testOwnerPermissions() {
        System.out.println("testOwnerPermissions");

        PermissionConfig.Builder configBuilder = PermissionConfig.newBuilder()
          .setOwnerId(USER_1)
          .setOtherPermission(NONE);
        Permission.Builder permissionBuilder = Permission.newBuilder();

        boolean[] bools = {true, false};

        for (boolean read : bools) {
            permissionBuilder.setRead(read);

            for (boolean write : bools) {
                permissionBuilder.setWrite(write);

                for (boolean access : bools) {
                    permissionBuilder.setAccess(access);
                    UnitConfig unitConfig = UnitConfig.newBuilder()
                      .setPermissionConfig(configBuilder.setOwnerPermission(permissionBuilder))
                      .build();

                    // Test the "owner" permissions.
                    assertEquals(AuthorizationHelper.canRead(unitConfig, USER_1, groups), read);
                    assertEquals(AuthorizationHelper.canWrite(unitConfig, USER_1, groups), write);
                    assertEquals(AuthorizationHelper.canAccess(unitConfig, USER_1, groups), access);

                    // Make sure that a different user didn't gain any permissions.
                    assertEquals(AuthorizationHelper.canRead(unitConfig, USER_2, groups), false);
                    assertEquals(AuthorizationHelper.canWrite(unitConfig, USER_2, groups), false);
                    assertEquals(AuthorizationHelper.canAccess(unitConfig, USER_2, groups), false);
                }
            }
        }
    }

    /**
     * Tests all possible permission combinations for a member of the group, 
     * while keeping the "other" permissions false and the "owner" permissions true.
     */
    @Test
    public void testGroupPermissions() {
        System.out.println("testGroupPermissions");
        PermissionConfig.MapFieldEntry.Builder groupsBuilder = PermissionConfig.MapFieldEntry.newBuilder()
          .setGroupId(GROUP_2).setPermission(NONE);

        PermissionConfig.Builder configBuilder = PermissionConfig.newBuilder()
          .setOwnerId(USER_1)
          .setOwnerPermission(RWX)
          .setOtherPermission(NONE)
          .addGroupPermission(0, groupsBuilder);
        Permission.Builder permissionBuilder = Permission.newBuilder();

        boolean[] bools = {true, false};

        // Add our group to the list, initially with no permissions.
        groupsBuilder.setGroupId(GROUP_1)
          .setPermission(NONE);
        configBuilder.addGroupPermission(1, groupsBuilder);

        for (boolean read : bools) {
            permissionBuilder.setRead(read);

            for (boolean write : bools) {
                permissionBuilder.setWrite(write);

                for (boolean access : bools) {
                    permissionBuilder.setAccess(access);

                    // Change the permission for our group.
                    groupsBuilder.setPermission(permissionBuilder);

                    UnitConfig unitConfig = UnitConfig.newBuilder()
                      // Change the entry of our group.
                      .setPermissionConfig(configBuilder.setGroupPermission(1, groupsBuilder))
                      .build();

                    // Test if group permissions match for the group member.
                    assertEquals(AuthorizationHelper.canRead(unitConfig, USER_2, groups), read);
                    assertEquals(AuthorizationHelper.canWrite(unitConfig, USER_2, groups), write);
                    assertEquals(AuthorizationHelper.canAccess(unitConfig, USER_2, groups), access);

                    // Make sure that the owner didn't lose any permissions.
                    assertEquals(AuthorizationHelper.canRead(unitConfig, USER_1, groups), true);
                    assertEquals(AuthorizationHelper.canWrite(unitConfig, USER_1, groups), true);
                    assertEquals(AuthorizationHelper.canAccess(unitConfig, USER_1, groups), true);

                    // Make sure that a different user didn't gain any permissions.
                    assertEquals(AuthorizationHelper.canRead(unitConfig, USER_3, groups), false);
                    assertEquals(AuthorizationHelper.canWrite(unitConfig, USER_3, groups), false);
                    assertEquals(AuthorizationHelper.canAccess(unitConfig, USER_3, groups), false);
                }
            }
        }
    }

    /**
     * Tests all possible permission combinations for the owner, while keeping the "owner" permissions false.
     */
    @Test
    public void testOtherPermissions() {
        System.out.println("testOtherPermissions");

        PermissionConfig.Builder configBuilder = PermissionConfig.newBuilder()
          .setOwnerId(USER_1)
          .setOwnerPermission(RWX);
        Permission.Builder permissionBuilder = Permission.newBuilder();

        boolean[] bools = {true, false};

        for (boolean read : bools) {
            permissionBuilder.setRead(read);

            for (boolean write : bools) {
                permissionBuilder.setWrite(write);

                for (boolean access : bools) {
                    permissionBuilder.setAccess(access);
                    UnitConfig unitConfig = UnitConfig.newBuilder()
                      .setPermissionConfig(configBuilder.setOtherPermission(permissionBuilder))
                      .build();

                    // Test if "other" permissions match.
                    assertEquals(AuthorizationHelper.canRead(unitConfig, USER_2, groups), read);
                    assertEquals(AuthorizationHelper.canWrite(unitConfig, USER_2, groups), write);
                    assertEquals(AuthorizationHelper.canAccess(unitConfig, USER_2, groups), access);

                    // Make sure that the owner didn't lose any permissions.
                    assertEquals(AuthorizationHelper.canRead(unitConfig, USER_1, groups), true);
                    assertEquals(AuthorizationHelper.canWrite(unitConfig, USER_1, groups), true);
                    assertEquals(AuthorizationHelper.canAccess(unitConfig, USER_1, groups), true);
                }
            }
        }
    }

}
