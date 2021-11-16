package org.openbase.bco.authentication.test;

/*-
 * #%L
 * BCO Authentication Test
 * %%
 * Copyright (C) 2017 - 2021 openbase.org
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

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openbase.bco.authentication.core.AuthenticationController;
import org.openbase.bco.authentication.lib.CachedAuthenticationRemote;
import org.openbase.bco.authentication.lib.EncryptionHelper;
import org.openbase.bco.authentication.lib.SessionManager;
import org.openbase.bco.authentication.lib.com.AbstractAuthenticatedControllerServer;
import org.openbase.bco.authentication.lib.com.AbstractAuthenticatedRemoteClient;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.extension.protobuf.ClosableDataBuilder;
import org.openbase.type.domotic.authentication.AuthenticatedValueType.AuthenticatedValue;
import org.openbase.type.domotic.authentication.LoginCredentialsType;
import org.openbase.type.domotic.authentication.PermissionType.Permission;
import org.openbase.type.domotic.authentication.UserClientPairType.UserClientPair;
import org.openbase.type.domotic.registry.UnitRegistryDataType.UnitRegistryData;
import org.openbase.type.domotic.registry.UnitRegistryDataType.UnitRegistryData.Builder;
import org.openbase.type.domotic.unit.UnitConfigType.UnitConfig;
import org.openbase.type.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;
import org.slf4j.LoggerFactory;

import java.util.List;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;

public class AuthenticatedCommunicationTest extends AuthenticationTest {

    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(AuthenticatedCommunicationTest.class);

    private static final String SCOPE = "/test/authentication/communication";

    private static final String USER_ID = "authenticated";
    private static final String USER_PASSWORD = "communication";

    private AuthenticatedControllerServerTestImpl communicationService;
    private AuthenticatedRemoteClientTestImpl remoteService;

    public AuthenticatedCommunicationTest() {

    }

    @BeforeClass
    public static void setUpClass() throws Exception {
        AuthenticationTest.setUpClass();

        // register a user from which a ticket can be validated
        registerUser();
    }

    private static void registerUser() throws Exception {
        LoginCredentialsType.LoginCredentials.Builder loginCredentials = LoginCredentialsType.LoginCredentials.newBuilder();
        loginCredentials.setId(USER_ID);
        loginCredentials.setSymmetric(true);
        loginCredentials.setCredentials(EncryptionHelper.encryptSymmetric(EncryptionHelper.hash(USER_PASSWORD), EncryptionHelper.hash(AuthenticationController.getInitialPassword())));
        AuthenticatedValue authenticatedValue = AuthenticatedValue.newBuilder().setValue(loginCredentials.build().toByteString()).build();
        CachedAuthenticationRemote.getRemote().register(authenticatedValue).get();
    }

    @Before
    public void setUp() throws Exception {
        communicationService = new AuthenticatedControllerServerTestImpl();
        remoteService = new AuthenticatedRemoteClientTestImpl();

        communicationService.init(SCOPE);
        remoteService.init(SCOPE);

        communicationService.activate();
        remoteService.activate();

        // wait for initial synchronization
        // otherwise, it can finish sync tasks after updating the controller in tests
        remoteService.requestData().get();
    }

    @After
    public void tearDown() {
        remoteService.shutdown();
        communicationService.shutdown();
    }

    /**
     * Test of communication between authenticated communication and remote service.
     *
     * @throws java.lang.Exception
     */
    @Test(timeout = 10000)
    public void testCommunication() throws Exception {
        LOGGER.info("Start communication test");

        UnitConfig.Builder otherAgentConfig = UnitConfig.newBuilder();
        otherAgentConfig.setId("OtherAgent");
        otherAgentConfig.setUnitType(UnitType.AGENT);
        Permission.Builder otherPermission = otherAgentConfig.getPermissionConfigBuilder().getOtherPermissionBuilder();
        otherPermission.setRead(true).setWrite(false).setAccess(true);

        UnitConfig.Builder userAgentConfig = UnitConfig.newBuilder();
        userAgentConfig.setId("UserAgent");
        userAgentConfig.setUnitType(UnitType.AGENT);
        userAgentConfig.getPermissionConfigBuilder().getOtherPermissionBuilder().setRead(false).setAccess(false).setWrite(false);
        userAgentConfig.getPermissionConfigBuilder().getOwnerPermissionBuilder().setRead(true).setAccess(true).setWrite(true);
        userAgentConfig.getPermissionConfigBuilder().setOwnerId(USER_ID);

        final List<UnitConfig> expectedAgentsLoggedOut = List.of(otherAgentConfig.build());
        final List<UnitConfig> expectedAgentsLoggedIn = List.of(otherAgentConfig.build(), userAgentConfig.build());

        LOGGER.info("Register agents");
        try (ClosableDataBuilder<Builder> dataBuilder = communicationService.getDataBuilderInterruptible(this)) {
            dataBuilder.getInternalBuilder().addAgentUnitConfig(otherAgentConfig);
            dataBuilder.getInternalBuilder().addAgentUnitConfig(userAgentConfig);
        }

        LOGGER.info("Synchronize remote...");
        UnitRegistryData data = remoteService.requestData().get();
        LOGGER.info("Synchronizing remote finished! " + data.getAgentUnitConfigCount() + " agents");

        assertEquals(
                "Without being logged in only the 'OtherAgent' should be visible by the remote.",
                expectedAgentsLoggedOut,
                data.getAgentUnitConfigList()
        );

        LOGGER.info("Login!");
        SessionManager.getInstance().loginUser(USER_ID, USER_PASSWORD, false);
        LOGGER.info("Synchronize remote...");
        data = remoteService.requestData().get();
        LOGGER.info("Synchronizing remote finished! " + data.getAgentUnitConfigCount() + " agents");

        assertEquals(
                "Being logged in both agens should be visible by the remote.",
                expectedAgentsLoggedIn,
                data.getAgentUnitConfigList()
        );
        //expectedAgents.add(userAgentConfig.build());
        assertTrue(remoteService.getData().getAgentUnitConfigList().contains(otherAgentConfig.build()));
        assertTrue(remoteService.getData().getAgentUnitConfigList().contains(userAgentConfig.build()));

        SessionManager.getInstance().logout();
        LOGGER.info("Synchronize remote...");
        data = remoteService.requestData().get();
        LOGGER.info("Synchronizing remote finished! " + data.getAgentUnitConfigCount() + " agents");

        assertEquals(
                "Only 'OtherAgent' should be visible again after logging out.",
                expectedAgentsLoggedOut,
                data.getAgentUnitConfigList()
        );
        assertTrue(remoteService.getData().getAgentUnitConfigList().contains(otherAgentConfig.build()));
        assertTrue(!remoteService.getData().getAgentUnitConfigList().contains(userAgentConfig.build()));
    }

    private static class AuthenticatedControllerServerTestImpl extends AbstractAuthenticatedControllerServer<UnitRegistryData, Builder> {

        /**
         * Create a communication service.
         *
         * @throws InstantiationException if the creation fails
         */
        public AuthenticatedControllerServerTestImpl() throws InstantiationException {
            super(UnitRegistryData.newBuilder());
        }

        @Override
        protected UnitRegistryData filterDataForUser(UnitRegistryData.Builder dataBuilder, UserClientPair userClientPair) {
            // remove all agent unit configs for which the user does not have direct read permissions
            for (int i = 0; i < dataBuilder.getAgentUnitConfigCount(); i++) {
                if (!canRead(dataBuilder.getAgentUnitConfig(i), userClientPair)) {
                    dataBuilder.removeAgentUnitConfig(i);
                    i--;
                }
            }
            if (userClientPair.getClientId().isEmpty() && userClientPair.getUserId().isEmpty()) {
                assertTrue("Other permissions should only show the OtherAgent", dataBuilder.build().getAgentUnitConfigCount() < 2);
            } else {
                assertEquals("For a logged in user both agents should be visible", 2, dataBuilder.build().getAgentUnitConfigCount());
            }
            return dataBuilder.build();
        }

        private boolean canRead(final UnitConfig unitConfig, final UserClientPair userClientPair) {
            final boolean matchesUser = !userClientPair.getUserId().isEmpty() && userClientPair.getUserId().equals(unitConfig.getPermissionConfig().getOwnerId());
            final boolean matchesClient = !userClientPair.getClientId().isEmpty() && userClientPair.getClientId().equals(unitConfig.getPermissionConfig().getOwnerId());
            if (matchesUser || matchesClient) {
                return unitConfig.getPermissionConfig().getOwnerPermission().getRead() || unitConfig.getPermissionConfig().getOtherPermission().getRead();
            }
            return unitConfig.getPermissionConfig().getOtherPermission().getRead();
        }

    }

    private static class AuthenticatedRemoteClientTestImpl extends AbstractAuthenticatedRemoteClient<UnitRegistryData> {

        public AuthenticatedRemoteClientTestImpl() {
            super(UnitRegistryData.class);
        }
    }
}
