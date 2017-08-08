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
import java.util.Map;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import org.openbase.bco.authentication.lib.AuthorizationHelper;
import org.openbase.jul.extension.protobuf.IdentifiableMessage;
import rst.domotic.authentication.PermissionConfigType.PermissionConfig;
import rst.domotic.authentication.PermissionType.Permission;
import rst.domotic.unit.authorizationgroup.AuthorizationGroupConfigType.AuthorizationGroupConfig;
import rst.domotic.unit.UnitConfigType.UnitConfig;
import rst.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;

/**
 *
 * @author <a href="mailto:cromankiewicz@techfak.uni-bielefeld.de">Constantin Romankiewicz</a>
 */
public class AuthorizationHelperTest {

    private static final Permission RWX = Permission.newBuilder().setRead(true).setWrite(true).setAccess(true).build();
    private static final Permission NONE = Permission.newBuilder().setRead(false).setWrite(false).setAccess(false).build();
    private static final String GROUP_1 = "group1";
    private static final String GROUP_2 = "group2";
    private static final String GROUP_CLIENTS = "group_clients";
    private static final String USER_1 = "user1";
    private static final String USER_2 = "user2";
    private static final String USER_3 = "user3";
    private static final String CLIENT_1 = "client1";

    private final Map<String, IdentifiableMessage<String, UnitConfig, UnitConfig.Builder>> groups;

    public AuthorizationHelperTest() throws Exception {
        groups = new HashMap<>();
        AuthorizationGroupConfig group1 = AuthorizationGroupConfig.newBuilder().addMemberId(USER_1).addMemberId(USER_2).build();
        AuthorizationGroupConfig group2 = AuthorizationGroupConfig.newBuilder().addMemberId(USER_1).addMemberId(USER_3).build();
        AuthorizationGroupConfig group3 = AuthorizationGroupConfig.newBuilder().addMemberId(CLIENT_1).build();
        UnitConfig unitConfig1 = UnitConfig.newBuilder().setLabel(GROUP_1).setId(GROUP_1).setType(UnitType.AUTHORIZATION_GROUP).setAuthorizationGroupConfig(group1).build();
        UnitConfig unitConfig2 = UnitConfig.newBuilder().setLabel(GROUP_2).setId(GROUP_2).setType(UnitType.AUTHORIZATION_GROUP).setAuthorizationGroupConfig(group2).build();
        UnitConfig unitConfig3 = UnitConfig.newBuilder().setLabel(GROUP_CLIENTS).setId(GROUP_CLIENTS).setType(UnitType.AUTHORIZATION_GROUP).setAuthorizationGroupConfig(group3).build();
        groups.put(GROUP_1, new IdentifiableMessage<>(unitConfig1));
        groups.put(GROUP_2, new IdentifiableMessage<>(unitConfig2));
        groups.put(GROUP_CLIENTS, new IdentifiableMessage<>(unitConfig3));
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
        Permission.Builder ownerPermissionBuilder = configBuilder.getOwnerPermissionBuilder();

        boolean[] bools = {true, false};

        for (boolean read : bools) {
            ownerPermissionBuilder.setRead(read);

            for (boolean write : bools) {
                ownerPermissionBuilder.setWrite(write);

                for (boolean access : bools) {
                    ownerPermissionBuilder.setAccess(access);
                    PermissionConfig config = configBuilder.build();

                    // Test the "owner" permissions.
                    assertEquals(AuthorizationHelper.canRead(config, USER_1, groups), read);
                    assertEquals(AuthorizationHelper.canWrite(config, USER_1, groups), write);
                    assertEquals(AuthorizationHelper.canAccess(config, USER_1, groups), access);

                    // Make sure that a different user didn't gain any permissions.
                    assertEquals(AuthorizationHelper.canRead(config, USER_2, groups), false);
                    assertEquals(AuthorizationHelper.canWrite(config, USER_2, groups), false);
                    assertEquals(AuthorizationHelper.canAccess(config, USER_2, groups), false);
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

                    // Change the entry of our group.
                    PermissionConfig config = configBuilder.setGroupPermission(1, groupsBuilder).build();

                    // Test if group permissions match for the group member.
                    assertEquals(AuthorizationHelper.canRead(config, USER_2, groups), read);
                    assertEquals(AuthorizationHelper.canWrite(config, USER_2, groups), write);
                    assertEquals(AuthorizationHelper.canAccess(config, USER_2, groups), access);

                    // Make sure that the owner didn't lose any permissions.
                    assertEquals(AuthorizationHelper.canRead(config, USER_1, groups), true);
                    assertEquals(AuthorizationHelper.canWrite(config, USER_1, groups), true);
                    assertEquals(AuthorizationHelper.canAccess(config, USER_1, groups), true);

                    // Make sure that a different user didn't gain any permissions.
                    assertEquals(AuthorizationHelper.canRead(config, USER_3, groups), false);
                    assertEquals(AuthorizationHelper.canWrite(config, USER_3, groups), false);
                    assertEquals(AuthorizationHelper.canAccess(config, USER_3, groups), false);
                }
            }
        }
    }

    /**
     * Tests all possible permission combinations for "other", while keeping the owner permissions true.
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
                    PermissionConfig config = configBuilder.setOtherPermission(permissionBuilder).build();

                    // Test if "other" permissions match.
                    assertEquals(AuthorizationHelper.canRead(config, USER_2, groups), read);
                    assertEquals(AuthorizationHelper.canWrite(config, USER_2, groups), write);
                    assertEquals(AuthorizationHelper.canAccess(config, USER_2, groups), access);

                    // Make sure that the owner didn't lose any permissions.
                    assertEquals(AuthorizationHelper.canRead(config, USER_1, groups), true);
                    assertEquals(AuthorizationHelper.canWrite(config, USER_1, groups), true);
                    assertEquals(AuthorizationHelper.canAccess(config, USER_1, groups), true);
                }
            }
        }
    }

    /**
     * Tests permissions of a user@client.
     */
    @Test
    public void testUserClient() {
        System.out.println("testOtherPermissions");

        PermissionConfig.Builder configBuilder = PermissionConfig.newBuilder()
                .setOwnerId(USER_1)
                .setOwnerPermission(RWX)
                .setOtherPermission(NONE);

        String user1 = USER_1 + "@" + CLIENT_1;
        String user2 = USER_2 + "@" + CLIENT_1;

        // No permissions for devices, all permissions for owners: Owner has rights, others don't.
        PermissionConfig.MapFieldEntry.Builder groupsBuilder = PermissionConfig.MapFieldEntry.newBuilder()
                .setGroupId(GROUP_CLIENTS).setPermission(NONE);

        configBuilder.addGroupPermission(0, groupsBuilder);
        PermissionConfig config = configBuilder.build();
        assertEquals(AuthorizationHelper.canRead(config, user1, groups), true);
        assertEquals(AuthorizationHelper.canWrite(config, user1, groups), true);
        assertEquals(AuthorizationHelper.canAccess(config, user1, groups), true);
        assertEquals(AuthorizationHelper.canRead(config, user2, groups), false);
        assertEquals(AuthorizationHelper.canWrite(config, user2, groups), false);
        assertEquals(AuthorizationHelper.canAccess(config, user2, groups), false);

        // All permissions for devices, all permissions for owners: Owner still has rights, others too.
        groupsBuilder = PermissionConfig.MapFieldEntry.newBuilder()
                .setGroupId(GROUP_CLIENTS).setPermission(RWX);

        configBuilder.setGroupPermission(0, groupsBuilder);
        config = configBuilder.build();
        assertEquals(AuthorizationHelper.canRead(config, user1, groups), true);
        assertEquals(AuthorizationHelper.canWrite(config, user1, groups), true);
        assertEquals(AuthorizationHelper.canAccess(config, user1, groups), true);
        assertEquals(AuthorizationHelper.canRead(config, user2, groups), true);
        assertEquals(AuthorizationHelper.canWrite(config, user2, groups), true);
        assertEquals(AuthorizationHelper.canAccess(config, user2, groups), true);
    }
}
