package org.openbase.bco.registry.unit.test;

/*-
 * #%L
 * BCO Registry Unit Test
 * %%
 * Copyright (C) 2014 - 2017 openbase.org
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

import org.junit.After;
import org.junit.AfterClass;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openbase.bco.authentication.core.AuthenticatorController;
import org.openbase.bco.authentication.core.mock.MockCredentialStore;
import org.openbase.bco.authentication.lib.EncryptionHelper;
import org.openbase.bco.authentication.lib.SessionManager;
import org.openbase.bco.registry.mock.MockRegistry;
import org.openbase.bco.registry.mock.MockRegistryHolder;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.jps.core.JPService;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rst.domotic.authentication.PermissionConfigType.PermissionConfig;
import rst.domotic.authentication.PermissionType.Permission;
import rst.domotic.unit.UnitConfigType.UnitConfig;
import rst.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;
import rst.domotic.unit.user.UserConfigType.UserConfig;

/**
 *
 * @author <a href="mailto:thuxohl@techfak.uni-bielefeld.de">Tamino Huxohl</a>
 */
public class RegistryFilteringTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(DeviceRegistryTest.class);

    private static MockRegistry mockRegistry;
    
    private static AuthenticatorController authenticatorController;
    private static MockCredentialStore mockCredentialStore;

    @BeforeClass
    public static void setUpClass() throws Exception {
        JPService.setupJUnitTestMode();

        try {
            mockRegistry = MockRegistryHolder.newMockRegistry();

            mockCredentialStore = new MockCredentialStore();
            authenticatorController = new AuthenticatorController(mockCredentialStore);
            authenticatorController.init();
            authenticatorController.activate();
            authenticatorController.waitForActivation();
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
    public void setUp() throws CouldNotPerformException {

    }

    @After
    public void tearDown() throws CouldNotPerformException {
    }

    /**
     * The if when access in other permission for a unitConfig is removed. The unit
     * is no longer listed in the remote registry.
     *
     * @throws Exception
     */
    @Test(timeout = 10000)
    public void testWithOtherPermission() throws Exception {
        System.out.println("testWithOtherPermission");

        // size before
        int size = Registries.getUnitRegistry().getDalUnitConfigs().size();

        // remove acces rights for one unit
        UnitConfig.Builder unitConfig = Registries.getUnitRegistry().getUnitConfigsByLabel("PH_Hue_E27_Device_Hell").get(0).toBuilder();
        for (UnitConfig unit : Registries.getUnitRegistry().getUnitConfigsByLabel("PH_Hue_E27_Device_Hell")) {
            if (unit.getType() == UnitType.COLORABLE_LIGHT) {
                unitConfig = unit.toBuilder();
            }
        }
        PermissionConfig.Builder permissionConfig = unitConfig.getPermissionConfigBuilder();
        Permission.Builder otherPermission = permissionConfig.getOtherPermissionBuilder();
        otherPermission.setAccess(false);
        Registries.getUnitRegistry().updateUnitConfig(unitConfig.build()).get();

        // test if the unitConfig cannot be seen anymore
        assertEquals("Size has not been reduced as a consequence", size - 1, Registries.getUnitRegistry().getDalUnitConfigs().size());
        assertTrue("UnitConfig can still be found in unitRegistry even though the access permission for other has been removed", !Registries.getUnitRegistry().getUnitConfigRemoteRegistry().contains(unitConfig.getId()));
    }
    
    @Test(timeout = 10000)
    public void testPermissionsOnLogin() throws Exception {
        System.out.println("testPermissionsOnLogin");
        
        // Register a user
        UnitConfig.Builder userUnitConfig = UnitConfig.newBuilder();
        UserConfig.Builder userConfig = userUnitConfig.getUserConfigBuilder();
        userUnitConfig.setType(UnitType.USER);
        userConfig.setFirstName("Gubrush").setLastName("Threepwood").setUserName("Mighty Pirate");
        userUnitConfig = Registries.getUserRegistry().registerUserConfig(userUnitConfig.build()).get().toBuilder();
        
        // register user in credentials store
        String password = "Elaine";
        mockCredentialStore.addCredentials(userUnitConfig.getId(), EncryptionHelper.hash(password), true);
        
        // get unitConfig, remove other permission and add user permission
        UnitConfig.Builder unitConfig = Registries.getUnitRegistry().getDalUnitConfigs().get(0).toBuilder();
        PermissionConfig.Builder permissionConfig = unitConfig.getPermissionConfigBuilder();
        Permission.Builder otherPermission = permissionConfig.getOtherPermissionBuilder();
        otherPermission.setAccess(false);
        Registries.getUnitRegistry().updateUnitConfig(unitConfig.build()).get();
        Permission.Builder ownerPermission = permissionConfig.getOwnerPermissionBuilder();
        ownerPermission.setAccess(true);
        permissionConfig.setOwnerId(userUnitConfig.getId());
        Registries.getUnitRegistry().updateUnitConfig(unitConfig.build()).get();
        
        // unitConfig should be removed with missing all rights and nobody logged in
        assertTrue("UnitConfig has not been removed event though other permissions have been removed", !Registries.getUnitRegistry().containsUnitConfigById(unitConfig.getId()));
        
        // logint
        SessionManager.getInstance().login(userUnitConfig.getId(), password);
        
        // unitConfig should be available again
        assertTrue("UnitConfig is not visible even though owner is now loggen in", Registries.getUnitRegistry().containsUnitConfigById(unitConfig.getId()));
        
        // logout to dont interfere with other tests
        SessionManager.getInstance().logout();
    }
}
