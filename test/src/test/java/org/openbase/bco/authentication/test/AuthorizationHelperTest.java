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
import org.openbase.bco.authentication.lib.AuthorizationHelper;
import java.util.HashMap;
import java.util.Map;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import org.openbase.jul.extension.protobuf.IdentifiableMessage;
import rst.domotic.authentication.PermissionConfigType.PermissionConfig;
import rst.domotic.authentication.PermissionType.Permission;
import rst.domotic.unit.authorizationgroup.AuthorizationGroupConfigType.AuthorizationGroupConfig;
import rst.domotic.unit.UnitConfigType.UnitConfig;
import rst.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;
import rst.domotic.unit.location.LocationConfigType.LocationConfig;
import rst.spatial.PlacementConfigType.PlacementConfig;

/**
 *
 * @author <a href="mailto:cromankiewicz@techfak.uni-bielefeld.de">Constantin Romankiewicz</a>
 */
public class AuthorizationHelperTest {

    private static final Permission RWX = Permission.newBuilder().setRead(true).setWrite(true).setAccess(true).build();
    private static final Permission NONE = Permission.newBuilder().setRead(false).setWrite(false).setAccess(false).build();
    private static final Permission READ_ONLY = Permission.newBuilder().setRead(true).setWrite(false).setAccess(false).build();
    private static final String GROUP_1 = "group1";
    private static final String GROUP_2 = "group2";
    private static final String GROUP_CLIENTS = "group_clients";
    private static final String USER_1 = "user1";
    private static final String USER_2 = "user2";
    private static final String USER_3 = "user3";
    private static final String CLIENT_1 = "client1";
    private static final String LOCATION_ROOT = "root";
    private static final String LOCATION_1 = "location1";
    private static final String UNIT_ID = "unit1";

    private final Map<String, IdentifiableMessage<String, UnitConfig, UnitConfig.Builder>> groups;
    private final Map<String, IdentifiableMessage<String, UnitConfig, UnitConfig.Builder>> locations;

    public AuthorizationHelperTest() throws Exception {
        groups = new HashMap<>();
        locations = new HashMap<>();
        AuthorizationGroupConfig group1 = AuthorizationGroupConfig.newBuilder().addMemberId(USER_1).addMemberId(USER_2).build();
        AuthorizationGroupConfig group2 = AuthorizationGroupConfig.newBuilder().addMemberId(USER_1).addMemberId(USER_3).build();
        AuthorizationGroupConfig group3 = AuthorizationGroupConfig.newBuilder().addMemberId(CLIENT_1).build();
        UnitConfig unitConfig1 = UnitConfig.newBuilder().setLabel(GROUP_1).setId(GROUP_1).setType(UnitType.AUTHORIZATION_GROUP).setAuthorizationGroupConfig(group1).build();
        UnitConfig unitConfig2 = UnitConfig.newBuilder().setLabel(GROUP_2).setId(GROUP_2).setType(UnitType.AUTHORIZATION_GROUP).setAuthorizationGroupConfig(group2).build();
        UnitConfig unitConfig3 = UnitConfig.newBuilder().setLabel(GROUP_CLIENTS).setId(GROUP_CLIENTS).setType(UnitType.AUTHORIZATION_GROUP).setAuthorizationGroupConfig(group3).build();
        groups.put(GROUP_1, new IdentifiableMessage<>(unitConfig1));
        groups.put(GROUP_2, new IdentifiableMessage<>(unitConfig2));
        groups.put(GROUP_CLIENTS, new IdentifiableMessage<>(unitConfig3));

        LocationConfig locationRoot = LocationConfig.newBuilder().setRoot(true).build();
        PermissionConfig permissionConfig = PermissionConfig.newBuilder().setOtherPermission(READ_ONLY).build();
        UnitConfig unitConfig4 = UnitConfig.newBuilder()
          .setLabel(LOCATION_ROOT)
          .setId(LOCATION_ROOT)
          .setType(UnitType.LOCATION)
          .setLocationConfig(locationRoot)
          .setPermissionConfig(permissionConfig)
          .build();
        locations.put(LOCATION_ROOT, new IdentifiableMessage<>(unitConfig4));
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
     * @throws java.lang.Exception
     */
    @Test
    public void testOwnerPermissions() throws Exception {
        System.out.println("testOwnerPermissions");

        PlacementConfig.Builder placement = PlacementConfig.newBuilder()
          .setLocationId(LOCATION_ROOT);
        UnitConfig.Builder unitConfigBuilder = UnitConfig.newBuilder().setId(UNIT_ID).setPlacementConfig(placement);
        System.out.println(unitConfigBuilder.build());
        PermissionConfig.Builder configBuilder = unitConfigBuilder.getPermissionConfigBuilder()
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
                    UnitConfig unitConfig = unitConfigBuilder.build();

                    // Test the "owner" permissions.
                    assertEquals(read, AuthorizationHelper.canRead(unitConfig, USER_1, groups, locations));
                    assertEquals(write, AuthorizationHelper.canWrite(unitConfig, USER_1, groups, locations));
                    assertEquals(access, AuthorizationHelper.canAccess(unitConfig, USER_1, groups, locations));

                    // Make sure that a different user didn't gain any permissions.
                    assertEquals(false, AuthorizationHelper.canRead(unitConfig, USER_2, groups, locations));
                    assertEquals(false, AuthorizationHelper.canWrite(unitConfig, USER_2, groups, locations));
                    assertEquals(false, AuthorizationHelper.canAccess(unitConfig, USER_2, groups, locations));
                }
            }
        }
    }

    /**
     * Tests all possible permission combinations for a member of the group,
     * while keeping the "other" permissions false and the "owner" permissions true.
     * @throws java.lang.Exception
     */
    @Test
    public void testGroupPermissions() throws Exception {
        System.out.println("testGroupPermissions");
        PermissionConfig.MapFieldEntry.Builder groupsBuilder = PermissionConfig.MapFieldEntry.newBuilder()
                .setGroupId(GROUP_2).setPermission(NONE);

        PlacementConfig.Builder placement = PlacementConfig.newBuilder()
          .setLocationId(LOCATION_ROOT);
        UnitConfig.Builder unitConfigBuilder = UnitConfig.newBuilder().setId(UNIT_ID).setPlacementConfig(placement);
        PermissionConfig.Builder configBuilder = unitConfigBuilder.getPermissionConfigBuilder()
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
                    configBuilder.setGroupPermission(1, groupsBuilder).build();

                    UnitConfig unitConfig = unitConfigBuilder.build();

                    // Test if group permissions match for the group member.
                    assertEquals(read, AuthorizationHelper.canRead(unitConfig, USER_2, groups, locations));
                    assertEquals(write, AuthorizationHelper.canWrite(unitConfig, USER_2, groups, locations));
                    assertEquals(access, AuthorizationHelper.canAccess(unitConfig, USER_2, groups, locations));

                    // Make sure that the owner didn't lose any permissions.
                    assertEquals(true, AuthorizationHelper.canRead(unitConfig, USER_1, groups, locations));
                    assertEquals(true, AuthorizationHelper.canWrite(unitConfig, USER_1, groups, locations));
                    assertEquals(true, AuthorizationHelper.canAccess(unitConfig, USER_1, groups, locations));

                    // Make sure that a different user didn't gain any permissions.
                    assertEquals(false, AuthorizationHelper.canRead(unitConfig, USER_3, groups, locations));
                    assertEquals(false, AuthorizationHelper.canWrite(unitConfig, USER_3, groups, locations));
                    assertEquals(false, AuthorizationHelper.canAccess(unitConfig, USER_3, groups, locations));
                }
            }
        }
    }

    /**
     * Tests all possible permission combinations for "other", while keeping the owner permissions true.
     * @throws java.lang.Exception
     */
    @Test
    public void testOtherPermissions() throws Exception {
        System.out.println("testOtherPermissions");

        PlacementConfig.Builder placement = PlacementConfig.newBuilder()
          .setLocationId(LOCATION_ROOT);
        UnitConfig.Builder unitConfigBuilder = UnitConfig.newBuilder().setId(UNIT_ID).setPlacementConfig(placement);
        PermissionConfig.Builder configBuilder = unitConfigBuilder.getPermissionConfigBuilder()
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
                    configBuilder.setOtherPermission(permissionBuilder);
                    UnitConfig unitConfig = unitConfigBuilder.build();

                    // Test if "other" permissions match.
                    assertEquals(read, AuthorizationHelper.canRead(unitConfig, USER_2, groups, locations));
                    assertEquals(write, AuthorizationHelper.canWrite(unitConfig, USER_2, groups, locations));
                    assertEquals(access, AuthorizationHelper.canAccess(unitConfig, USER_2, groups, locations));

                    // Make sure that the owner didn't lose any permissions.
                    assertEquals(true, AuthorizationHelper.canRead(unitConfig, USER_1, groups, locations));
                    assertEquals(true, AuthorizationHelper.canWrite(unitConfig, USER_1, groups, locations));
                    assertEquals(true, AuthorizationHelper.canAccess(unitConfig, USER_1, groups, locations));
                }
            }
        }
    }

    /**
     * Tests permissions of a user@client.
     * @throws java.lang.Exception
     */
    @Test
    public void testUserClient() throws Exception {
        System.out.println("testOtherPermissions");

        PlacementConfig.Builder placement = PlacementConfig.newBuilder()
          .setLocationId(LOCATION_ROOT);
        UnitConfig.Builder unitConfigBuilder = UnitConfig.newBuilder().setId(UNIT_ID).setPlacementConfig(placement);
        PermissionConfig.Builder configBuilder = unitConfigBuilder.getPermissionConfigBuilder()
                .setOwnerId(USER_1)
                .setOwnerPermission(RWX)
                .setOtherPermission(NONE);

        String user1 = USER_1 + "@" + CLIENT_1;
        String user2 = USER_2 + "@" + CLIENT_1;

        // No permissions for devices, all permissions for owners: Owner has rights, others don't.
        PermissionConfig.MapFieldEntry.Builder groupsBuilder = PermissionConfig.MapFieldEntry.newBuilder()
                .setGroupId(GROUP_CLIENTS).setPermission(NONE);

        configBuilder.addGroupPermission(0, groupsBuilder);
        UnitConfig unitConfig = unitConfigBuilder.build();
        assertEquals(true, AuthorizationHelper.canRead(unitConfig, user1, groups, locations));
        assertEquals(true, AuthorizationHelper.canWrite(unitConfig, user1, groups, locations));
        assertEquals(true, AuthorizationHelper.canAccess(unitConfig, user1, groups, locations));
        assertEquals(false, AuthorizationHelper.canRead(unitConfig, user2, groups, locations));
        assertEquals(false, AuthorizationHelper.canWrite(unitConfig, user2, groups, locations));
        assertEquals(false, AuthorizationHelper.canAccess(unitConfig, user2, groups, locations));

        // All permissions for devices, all permissions for owners: Owner still has rights, others too.
        groupsBuilder = PermissionConfig.MapFieldEntry.newBuilder()
                .setGroupId(GROUP_CLIENTS).setPermission(RWX);

        configBuilder.setGroupPermission(0, groupsBuilder);
        unitConfig = unitConfigBuilder.build();
        assertEquals(true, AuthorizationHelper.canRead(unitConfig, user1, groups, locations));
        assertEquals(true, AuthorizationHelper.canWrite(unitConfig, user1, groups, locations));
        assertEquals(true, AuthorizationHelper.canAccess(unitConfig, user1, groups, locations));
        System.out.println("-------------------------------------------------------------------------------------------");
        assertEquals(true, AuthorizationHelper.canRead(unitConfig, user2, groups, locations));
        assertEquals(true, AuthorizationHelper.canWrite(unitConfig, user2, groups, locations));
        assertEquals(true, AuthorizationHelper.canAccess(unitConfig, user2, groups, locations));
    }

    @Test
    public void testLocationPermission() throws Exception {
        LocationConfig location1 = LocationConfig.newBuilder().setRoot(false).build();
        PermissionConfig.Builder permissionConfigLocation = PermissionConfig.newBuilder().setOtherPermission(NONE);
        PlacementConfig placementConfigLocation = PlacementConfig.newBuilder().setLocationId(LOCATION_ROOT).build();
        UnitConfig.Builder unitConfigLocationBuilder = UnitConfig.newBuilder()
          .setLabel(LOCATION_1)
          .setId(LOCATION_1)
          .setType(UnitType.LOCATION)
          .setLocationConfig(location1)
          .setPlacementConfig(placementConfigLocation)
          .setPermissionConfig(permissionConfigLocation);
        locations.put(LOCATION_1, new IdentifiableMessage<>(unitConfigLocationBuilder.build()));

        PlacementConfig placementUnit = PlacementConfig.newBuilder().setLocationId(LOCATION_1).build();

        UnitConfig.Builder unitConfigBuilder = UnitConfig.newBuilder().setId(UNIT_ID).setPlacementConfig(placementUnit);
        UnitConfig unitConfig = unitConfigBuilder.build();

        PermissionConfig.Builder permissionConfigBuilder = unitConfigLocationBuilder.getPermissionConfigBuilder();
        Permission.Builder permissionBuilder = permissionConfigBuilder.getOtherPermissionBuilder();

        boolean[] bools = {true, false};
        for (boolean read : bools) {
            permissionBuilder.setRead(read);

            for (boolean write : bools) {
                permissionBuilder.setWrite(write);

                for (boolean access : bools) {
                    permissionBuilder.setAccess(access);
                    locations.put(LOCATION_1, new IdentifiableMessage<>(unitConfigLocationBuilder.build()));

                    // If we have read permission on the location, permissions are inherited from the location.
                    if (read) {
                        assertEquals(read, AuthorizationHelper.canRead(unitConfig, USER_2, groups, locations));
                        assertEquals(write, AuthorizationHelper.canWrite(unitConfig, USER_2, groups, locations));
                        assertEquals(access, AuthorizationHelper.canAccess(unitConfig, USER_2, groups, locations));
                    }
                    // If we have no read permission on the location, we have no permissions at all for the unit..
                    else {
                        assertEquals(false, AuthorizationHelper.canRead(unitConfig, USER_2, groups, locations));
                        assertEquals(false, AuthorizationHelper.canWrite(unitConfig, USER_2, groups, locations));
                        assertEquals(false, AuthorizationHelper.canAccess(unitConfig, USER_2, groups, locations));
                    }
                }
            }

        }
    }
}
