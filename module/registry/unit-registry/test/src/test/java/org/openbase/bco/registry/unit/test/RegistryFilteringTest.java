package org.openbase.bco.registry.unit.test;

/*-
 * #%L
 * BCO Registry Unit Test
 * %%
 * Copyright (C) 2014 - 2021 openbase.org
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

import com.google.protobuf.ByteString;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.openbase.bco.authentication.lib.AuthenticatedServerManager;
import org.openbase.bco.authentication.lib.EncryptionHelper;
import org.openbase.bco.authentication.lib.SessionManager;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.bco.registry.unit.core.plugin.UserCreationPlugin;
import org.openbase.bco.registry.unit.lib.UnitRegistry;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.extension.type.processing.LabelProcessor;
import org.openbase.type.domotic.authentication.AuthorizationTokenType.AuthorizationToken;
import org.openbase.type.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import org.openbase.type.domotic.unit.UnitConfigType.UnitConfig;
import org.openbase.type.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;
import org.openbase.type.domotic.unit.user.UserConfigType.UserConfig;

import java.util.Base64;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author <a href="mailto:thuxohl@techfak.uni-bielefeld.de">Tamino Huxohl</a>
 */
public class RegistryFilteringTest extends AbstractBCORegistryTest {

    @AfterEach
    @Timeout(30)
    public void tearDown() throws Exception {
        SessionManager.getInstance().completeLogout();
    }

    @Test
    @Timeout(10)
    public void testRegisteringWhileLoggedIn() throws Exception {
        System.out.println("testRegisteringWhileLoggedIn");

        UnitConfig.Builder userUnitConfig = UnitConfig.newBuilder();
        UserConfig.Builder userConfig = userUnitConfig.getUserConfigBuilder();
        userUnitConfig.setUnitType(UnitType.USER);
        userUnitConfig.getPermissionConfigBuilder().getOtherPermissionBuilder().setWrite(true).setAccess(true).setRead(true);
        userConfig.setFirstName("Tamino");
        userConfig.setLastName("Huxohl");
        userConfig.setUserName("LeChuck");

        SessionManager.getInstance().loginUser(Registries.getUnitRegistry().getUnitConfigByAlias(UnitRegistry.ADMIN_USER_ALIAS).getId(), UserCreationPlugin.ADMIN_PASSWORD, false);
        try {
            Registries.getUnitRegistry().registerUnitConfig(userUnitConfig.build()).get();
        } catch (InterruptedException | ExecutionException | CouldNotPerformException ex) {
            throw ExceptionPrinter.printHistoryAndReturnThrowable(ex, logger);
        }
    }

    /**
     * Test if units are filtered accordingly if a user has no permissions, only read permissions,
     * or read and access permissions.
     *
     * @throws Exception
     */
    @Test
    @Timeout(10)
    public void testUnitFiltering() throws Exception {
        System.out.println("testUnitFiltering");

        // get a location with at least one unit and at least one child

        UnitConfig.Builder unitConfig = null;
        for (UnitConfig locationUnitConfig : Registries.getUnitRegistry(true).getLocationUnitConfigRemoteRegistry(true).getMessages()) {
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
        logger.info("Found suitable test location[" + LabelProcessor.getBestMatch(unitConfig.getLabel()) + "]");
        // remove read and access permissions
        unitConfig.getPermissionConfigBuilder().getOtherPermissionBuilder().setRead(false).setAccess(false);
        Registries.getUnitRegistry().updateUnitConfig(unitConfig.build()).get();

        // test if location does not contain unit ids and child ids anymore anymore
        assertTrue(Registries.getUnitRegistry().getUnitConfigById(unitConfig.getId()).getLocationConfig().getUnitIdList().isEmpty(), "Unit id list of location has not been filtered without access and read permissions");
        assertTrue(Registries.getUnitRegistry().getUnitConfigById(unitConfig.getId()).getLocationConfig().getChildIdList().isEmpty(), "Child id list of location has not been filtered without access and read permissions");
        // test if unit configuration cannot be read anymore
        try {
            Registries.getUnitRegistry().getUnitConfigById(unitConfig.getLocationConfig().getUnitId(0));
            fail("Unit configuration can still be seen even though other has no read and access permissions on its location");
        } catch (NotAvailableException ex) {
            // this should happen
        }

        // give only read permissions
        unitConfig.getPermissionConfigBuilder().getOtherPermissionBuilder().setAccess(false).setRead(true);
        Registries.getUnitRegistry().updateUnitConfig(unitConfig.build()).get();

        // test if unit ids and child ids are back
        assertEquals(
                unitConfig.getLocationConfig().getUnitIdCount(),
                Registries.getUnitRegistry().getUnitConfigById(unitConfig.getId()).getLocationConfig().getUnitIdCount(),
                "Unit id list of location not available with read permissions");
        assertEquals(
                unitConfig.getLocationConfig().getChildIdCount(),
                Registries.getUnitRegistry().getUnitConfigById(unitConfig.getId()).getLocationConfig().getChildIdCount(),
                "Child id list of location not available with read permissions");
        // test if unit configurations are still not available
        try {
            Registries.getUnitRegistry().getUnitConfigById(unitConfig.getLocationConfig().getUnitId(0));
        } catch (NotAvailableException ex) {
            assertTrue(false, "Unit configuration cannot be seen even though other has read permissions on its location");
        }

        // give only access permissions
        unitConfig.getPermissionConfigBuilder().getOtherPermissionBuilder().setAccess(true).setRead(false);
        Registries.getUnitRegistry().updateUnitConfig(unitConfig.build()).get();

        assertTrue(Registries.getUnitRegistry().getUnitConfigById(unitConfig.getId()).getPermissionConfig().getOtherPermission().getRead(), "Location does not provide read permission while providing access permissions");

        // test if unit ids and child ids are back
        assertEquals(
                unitConfig.getLocationConfig().getUnitIdCount(),
                Registries.getUnitRegistry().getUnitConfigById(unitConfig.getId()).getLocationConfig().getUnitIdCount(),
                "Unit id list of location not available with access permissions");
        assertEquals(
                unitConfig.getLocationConfig().getChildIdCount(),
                Registries.getUnitRegistry().getUnitConfigById(unitConfig.getId()).getLocationConfig().getChildIdCount(),
                "Child id list of location not available with access permissions");
        // test if unit configurations are still not available
        try {
            Registries.getUnitRegistry().getUnitConfigById(unitConfig.getLocationConfig().getUnitId(0));
        } catch (NotAvailableException ex) {
            assertTrue(false, "Unit configuration cannot be seen even though other has read and access permissions on its location");
        }
    }

    @Test
    @Timeout(10)
    public void testRequestingAuthorizationToken() throws Exception {
        final String adminUserId = Registries.getUnitRegistry().getUnitConfigByAlias(UnitRegistry.ADMIN_USER_ALIAS).getId();
        SessionManager.getInstance().loginUser(adminUserId, UserCreationPlugin.ADMIN_PASSWORD, false);

        final AuthorizationToken.Builder authorizationToken = AuthorizationToken.newBuilder();
        authorizationToken.setUserId(adminUserId);
        AuthorizationToken.PermissionRule.Builder locationEntry = authorizationToken.addPermissionRuleBuilder();
        locationEntry.setUnitId(Registries.getUnitRegistry().getRootLocationConfig().getId());
        locationEntry.getPermissionBuilder().setRead(true).setAccess(true).setWrite(false);
        locationEntry.setServiceTemplateId(Registries.getTemplateRegistry().getServiceTemplateByType(ServiceType.POWER_STATE_SERVICE).getId());

        final String token = Registries.getUnitRegistry().requestAuthorizationToken(authorizationToken.build()).get();
        final AuthorizationToken decrypted = EncryptionHelper.decryptSymmetric(
                ByteString.copyFrom(Base64.getDecoder().decode(token)), AuthenticatedServerManager.getInstance().getServiceServerSecretKey(), AuthorizationToken.class);

        assertEquals(authorizationToken.build(), decrypted, "Returned authorization token does not match");
    }
}
