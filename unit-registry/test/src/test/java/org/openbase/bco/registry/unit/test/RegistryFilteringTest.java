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

import org.junit.*;
import org.openbase.bco.authentication.lib.SessionManager;
import org.openbase.bco.registry.mock.MockRegistry;
import org.openbase.bco.registry.mock.MockRegistryHolder;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.bco.registry.unit.core.plugin.UserCreationPlugin;
import org.openbase.bco.registry.unit.lib.UnitRegistry;
import org.openbase.jps.core.JPService;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rst.domotic.unit.UnitConfigType.UnitConfig;
import rst.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;
import rst.domotic.unit.user.UserConfigType.UserConfig;

import java.util.concurrent.ExecutionException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

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

    @Test(timeout = 10000)
    public void testRegisteringWhileLoggedIn() throws Exception {
        System.out.println("testRegisteringWhileLoggedIn");

        UnitConfig.Builder userUnitConfig = UnitConfig.newBuilder();
        UserConfig.Builder userConfig = userUnitConfig.getUserConfigBuilder();
        userUnitConfig.setUnitType(UnitType.USER);
        userUnitConfig.getPermissionConfigBuilder().getOtherPermissionBuilder().setWrite(true).setAccess(true).setRead(true);
        userConfig.setFirstName("Le");
        userConfig.setLastName("Chuck");
        userConfig.setUserName("Admin");

        SessionManager.getInstance().login(Registries.getUnitRegistry().getUnitConfigByAlias(UnitRegistry.ADMIN_USER_ALIAS).getId(), UserCreationPlugin.ADMIN_PASSWORD);
        try {
            Registries.getUnitRegistry().registerUnitConfig(userUnitConfig.build()).get();
        } catch (InterruptedException | ExecutionException | CouldNotPerformException ex) {
            throw ExceptionPrinter.printHistoryAndReturnThrowable(ex, LOGGER);
        }
    }

    /**
     * Test if units are filtered accordingly if a user has no permissions, only read permissions,
     * or read and access permissions.
     *
     * @throws Exception
     */
    @Test(timeout = 10000)
    public void testUnitFiltering() throws Exception {
        System.out.println("testUnitFiltering");

        // get a location with at least one unit and at least one child

        UnitConfig.Builder unitConfig = null;
        for (UnitConfig locationUnitConfig : Registries.getUnitRegistry().getLocationUnitConfigRemoteRegistry().getMessages()) {
            // skip root location
            if (locationUnitConfig.getLocationConfig().getRoot()) {
                continue;
            }

            if (locationUnitConfig.getLocationConfig().getChildIdCount() > 0 && locationUnitConfig.getLocationConfig().getUnitIdCount() > 0) {
                unitConfig = locationUnitConfig.toBuilder();
                break;
            }
        }
        if (unitConfig == null) {
            throw new NotAvailableException("test location");
        }
        LOGGER.info("Found suitable test location[" + unitConfig.getLabel() + "]");
        // remove read and access permissions
        unitConfig.getPermissionConfigBuilder().getOtherPermissionBuilder().setRead(false).setAccess(false);
        Registries.getUnitRegistry().updateUnitConfig(unitConfig.build()).get();

        // test if location does not contain unit ids and child ids anymore anymore
        assertTrue("Unit id list of location has not been filtered without access and read permissions", Registries.getUnitRegistry().getUnitConfigById(unitConfig.getId()).getLocationConfig().getUnitIdList().isEmpty());
        assertTrue("Child id list of location has not been filtered without access and read permissions", Registries.getUnitRegistry().getUnitConfigById(unitConfig.getId()).getLocationConfig().getChildIdList().isEmpty());
        // test if unit configuration cannot be read anymore
        try {
            Registries.getUnitRegistry().getUnitConfigById(unitConfig.getLocationConfig().getUnitId(0));
            assertTrue("Unit configuration can still be seen even though other has no read and access permissions on its location", false);
        } catch (NotAvailableException ex) {
            // this should happen
        }

        // give only read permissions
        unitConfig.getPermissionConfigBuilder().getOtherPermissionBuilder().setAccess(false).setRead(true);
        Registries.getUnitRegistry().updateUnitConfig(unitConfig.build()).get();

        // test if unit ids and child ids are back
        assertEquals("Unit id list of location not available with read permissions",
                unitConfig.getLocationConfig().getUnitIdCount(),
                Registries.getUnitRegistry().getUnitConfigById(unitConfig.getId()).getLocationConfig().getUnitIdCount());
        assertEquals("Child id list of location not available with read permissions",
                unitConfig.getLocationConfig().getChildIdCount(),
                Registries.getUnitRegistry().getUnitConfigById(unitConfig.getId()).getLocationConfig().getChildIdCount());
        // test if unit configurations are still not available
        try {
            Registries.getUnitRegistry().getUnitConfigById(unitConfig.getLocationConfig().getUnitId(0));
        } catch (NotAvailableException ex) {
            assertTrue("Unit configuration cannot be seen even though other has read permissions on its location", false);
        }

        // give only access permissions
        unitConfig.getPermissionConfigBuilder().getOtherPermissionBuilder().setAccess(true).setRead(false);
        Registries.getUnitRegistry().updateUnitConfig(unitConfig.build()).get();

        assertTrue("Location does not provide read permission while providing access permissions", Registries.getUnitRegistry().getUnitConfigById(unitConfig.getId()).getPermissionConfig().getOtherPermission().getRead());

        // test if unit ids and child ids are back
        assertEquals("Unit id list of location not available with access permissions",
                unitConfig.getLocationConfig().getUnitIdCount(),
                Registries.getUnitRegistry().getUnitConfigById(unitConfig.getId()).getLocationConfig().getUnitIdCount());
        assertEquals("Child id list of location not available with access permissions",
                unitConfig.getLocationConfig().getChildIdCount(),
                Registries.getUnitRegistry().getUnitConfigById(unitConfig.getId()).getLocationConfig().getChildIdCount());
        // test if unit configurations are still not available
        try {
            Registries.getUnitRegistry().getUnitConfigById(unitConfig.getLocationConfig().getUnitId(0));
        } catch (NotAvailableException ex) {
            assertTrue("Unit configuration cannot be seen even though other has read and access permissions on its location", false);
        }
    }
}
