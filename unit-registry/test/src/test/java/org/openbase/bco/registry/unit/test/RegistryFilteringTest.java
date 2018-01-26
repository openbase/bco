package org.openbase.bco.registry.unit.test;

/*-
 * #%L
 * BCO Registry Unit Test
 * %%
 * Copyright (C) 2014 - 2018 openbase.org
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import com.jcraft.jsch.Session;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openbase.bco.authentication.lib.SessionManager;
import org.openbase.bco.registry.mock.MockRegistry;
import org.openbase.bco.registry.mock.MockRegistryHolder;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.jps.core.JPService;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.TimeoutException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.extension.protobuf.processing.ProtoBufFieldProcessor;
import org.openbase.jul.pattern.Observable;
import org.openbase.jul.pattern.Observer;
import org.openbase.jul.schedule.SyncObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rst.domotic.authentication.PermissionConfigType.PermissionConfig;
import rst.domotic.authentication.PermissionType.Permission;
import rst.domotic.unit.UnitConfigType.UnitConfig;
import rst.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;
import rst.domotic.unit.user.UserConfigType.UserConfig;

/**
 * @author <a href="mailto:thuxohl@techfak.uni-bielefeld.de">Tamino Huxohl</a>
 */
public class RegistryFilteringTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(RegistryFilteringTest.class);

    private static MockRegistry mockRegistry;

    @BeforeClass
    public static void setUpClass() throws Exception {
        JPService.setupJUnitTestMode();

        try {
            mockRegistry = MockRegistryHolder.newMockRegistry();
        } catch (org.openbase.jul.exception.InstantiationException ex) {
            throw ExceptionPrinter.printHistoryAndReturnThrowable(ex, LOGGER);
        }
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
        try {
            MockRegistryHolder.shutdownMockRegistry();
        } catch (Exception ex) {
            throw ExceptionPrinter.printHistoryAndReturnThrowable(ex, LOGGER);
        }
    }

    @Before
    public void setUp() {

    }

    @After
    public void tearDown() {
        SessionManager.getInstance().logout();
    }

    /**
     * Test if when access in other permission for a unitConfig is removed. The unit
     * is no longer listed in the remote registry.
     *
     * @throws Exception
     */
    @Test(timeout = 10000)
    public void testWithOtherPermission() throws Exception {
        System.out.println("testWithOtherPermission");

        // size before
        int size = Registries.getUnitRegistry().getDalUnitConfigs().size();

        // remove access rights for one unit
        UnitConfig.Builder unitConfig = Registries.getUnitRegistry().getUnitConfigsByLabelAndUnitType("PH_Hue_E27_Device_Hell", UnitType.COLORABLE_LIGHT).get(0).toBuilder();
        PermissionConfig.Builder permissionConfig = unitConfig.getPermissionConfigBuilder();
        Permission.Builder otherPermission = permissionConfig.getOtherPermissionBuilder();
        otherPermission.setRead(false);
        Registries.getUnitRegistry().updateUnitConfig(unitConfig.build()).get();

        // test if the unitConfig cannot be seen anymore
        assertEquals("Size has not been reduced as a consequence", size - 1, Registries.getUnitRegistry().getDalUnitConfigs().size());
        assertTrue("UnitConfig can still be found in unitRegistry even though the access permission for other has been removed", !Registries.getUnitRegistry().getUnitConfigRemoteRegistry().contains(unitConfig.getId()));
    }

    @Test(timeout = 10000)
    public void testPermissionsOnLogin() throws Exception {
        System.out.println("testPermissionsOnLogin");
        SessionManager.getInstance().login(MockRegistry.admin.getId(), MockRegistry.adminPassword);

        // Register a user
        UnitConfig.Builder userUnitConfig = UnitConfig.newBuilder();
        UserConfig.Builder userConfig = userUnitConfig.getUserConfigBuilder();
        userUnitConfig.setType(UnitType.USER);
        userUnitConfig.getPermissionConfigBuilder().getOtherPermissionBuilder().setRead(true).setAccess(true).setWrite(true);
        userConfig.setFirstName("Guybrush").setLastName("Threepwood").setUserName("Mighty Pirate");
        userUnitConfig = Registries.getUserRegistry().registerUserConfig(userUnitConfig.build()).get().toBuilder();

        // register user in credentials store
        String password = "Elaine";
        SessionManager.getInstance().registerUser(userUnitConfig.getId(), password, false);
        // log admin out because else the following test will not be performed with other permissions
        SessionManager.getInstance().logout();

        // get unitConfig, remove other permission and add user permission
        UnitConfig.Builder unitConfig = Registries.getUnitRegistry().getDalUnitConfigs().get(0).toBuilder();
        PermissionConfig.Builder permissionConfig = unitConfig.getPermissionConfigBuilder();
        Permission.Builder otherPermission = permissionConfig.getOtherPermissionBuilder();
        otherPermission.setAccess(false).setRead(false).setWrite(true);
        Registries.getUnitRegistry().updateUnitConfig(unitConfig.build()).get();
        Permission.Builder ownerPermission = permissionConfig.getOwnerPermissionBuilder();
        ownerPermission.setRead(true);
        permissionConfig.setOwnerId(userUnitConfig.getId());
        Registries.getUnitRegistry().updateUnitConfig(unitConfig.build()).get();

        // unitConfig should be removed with missing all rights and nobody logged in
        assertTrue("UnitConfig has not been removed even though other permissions have been removed", !Registries.getUnitRegistry().containsUnitConfigById(unitConfig.getId()));

        // login
        SessionManager.getInstance().logout();
        SessionManager.getInstance().login(userUnitConfig.getId(), password);

        //TODO: needed since synchronized modification of SessionManager login observable, remove by waiting on a lock
        while(!Registries.getUnitRegistry().containsUnitConfigById(unitConfig.getId())) {
            Thread.sleep(50);
        }

        // unitConfig should be available again
        assertTrue("UnitConfig is not visible even though owner is now logged in", Registries.getUnitRegistry().containsUnitConfigById(unitConfig.getId()));

        // logout to dont interfere with other tests
        SessionManager.getInstance().logout();
    }

    @Test(timeout = 10000)
    public void testWaitForDataOnVirtualRegistryRemote() throws Exception {
        System.out.println("testWaitForDataOnVirtualRegistryRemote");

        Registries.getUserRegistry().waitForData(2, TimeUnit.SECONDS);
        int userCount = Registries.getUserRegistry().getUserConfigs().size();

        UnitConfig.Builder userUnitConfig = Registries.getUserRegistry().getUserConfigs().get(0).toBuilder();
        PermissionConfig.Builder permissionConfig = userUnitConfig.getPermissionConfigBuilder();
        Permission.Builder otherPermission = permissionConfig.getOtherPermissionBuilder();
        otherPermission.setRead(false);

        Registries.getUserRegistry().updateUserConfig(userUnitConfig.build()).get();

        assertTrue("No data available anymore", Registries.getUserRegistry().isDataAvailable());
        assertTrue("User has not been removed", !Registries.getUserRegistry().containsUserConfigById(userUnitConfig.getId()));
        assertEquals("User count does not match", userCount - 1, Registries.getUserRegistry().getUserConfigs().size());
        try {
            Registries.getUserRegistry().waitForData(2, TimeUnit.SECONDS);
        } catch (TimeoutException ex) {
            Assert.fail("WaitForData did not return in time!");
        }
    }

    @Test(timeout = 10000)
    public void testRegisteringWhileLoggedIn() throws Exception {
        System.out.println("testRegisteringWhileLoggedIn");

        UnitConfig.Builder userUnitConfig = UnitConfig.newBuilder();
        UserConfig.Builder userConfig = userUnitConfig.getUserConfigBuilder();
        userUnitConfig.setType(UnitType.USER);
        userUnitConfig.getPermissionConfigBuilder().getOtherPermissionBuilder().setWrite(true).setAccess(true).setRead(true);
        userConfig.setFirstName("Le");
        userConfig.setLastName("Chuck");
        userConfig.setUserName("Admin");

        SessionManager.getInstance().login(MockRegistry.admin.getId(), MockRegistry.adminPassword);

        try {
            Registries.getUnitRegistry().registerUnitConfig(userUnitConfig.build()).get();
        } catch (InterruptedException | ExecutionException | CouldNotPerformException ex) {
            throw ExceptionPrinter.printHistoryAndReturnThrowable(ex, LOGGER);
        }
    }
}
