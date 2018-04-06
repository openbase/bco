package org.openbase.bco.authentication.test;

/*-
 * #%L
 * BCO Authentication Test
 * %%
 * Copyright (C) 2017 - 2018 openbase.org
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

import org.junit.*;
import org.openbase.bco.authentication.core.AuthenticatorController;
import org.openbase.bco.authentication.lib.CachedAuthenticationRemote;
import org.openbase.bco.authentication.lib.EncryptionHelper;
import org.openbase.bco.authentication.lib.SessionManager;
import org.openbase.bco.authentication.lib.com.AbstractAuthenticatedCommunicationService;
import org.openbase.bco.authentication.lib.com.AbstractAuthenticatedRemoteService;
import org.openbase.jps.core.JPService;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.extension.protobuf.ClosableDataBuilder;
import org.openbase.jul.extension.rsb.iface.RSBLocalServer;
import org.slf4j.LoggerFactory;
import rsb.Scope;
import rst.domotic.authentication.AuthenticatedValueType.AuthenticatedValue;
import rst.domotic.authentication.LoginCredentialsChangeType.LoginCredentialsChange;
import rst.domotic.authentication.PermissionType.Permission;
import rst.domotic.authentication.TicketAuthenticatorWrapperType.TicketAuthenticatorWrapper;
import rst.domotic.registry.UnitRegistryDataType.UnitRegistryData;
import rst.domotic.registry.UnitRegistryDataType.UnitRegistryData.Builder;
import rst.domotic.unit.UnitConfigType.UnitConfig;
import rst.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;

import static junit.framework.TestCase.assertTrue;

public class AuthenticatedCommunicationTest {

    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(AuthenticatedCommunicationTest.class);

    private static final Scope SCOPE = new Scope("/test/authentication/communication");
    private static AuthenticatorController authenticatorController;

    private static final String USER_ID = "authenticated";
    private static final String USER_PASSWORD = "communication";

    private AuthenticatedCommunicationService communicationService;
    private AuthenticatedRemoteService remoteService;

    public AuthenticatedCommunicationTest() {

    }

    @BeforeClass
    public static void setUpClass() throws Exception {
        JPService.setupJUnitTestMode();

        authenticatorController = new AuthenticatorController();
        authenticatorController.init();
        authenticatorController.activate();
        authenticatorController.waitForActivation();

        Assert.assertTrue("Initial password has not been generated despite an empty registry", AuthenticatorController.getInitialPassword() != null);

        // register a user from which a ticket can be validated
        registerUser();
    }

    private static void registerUser() throws Exception {
        LoginCredentialsChange.Builder loginCredentials = LoginCredentialsChange.newBuilder();
        loginCredentials.setId(USER_ID);
        loginCredentials.setNewCredentials(EncryptionHelper.encryptSymmetric(EncryptionHelper.hash(USER_PASSWORD), EncryptionHelper.hash(AuthenticatorController.getInitialPassword())));
        CachedAuthenticationRemote.getRemote().register(loginCredentials.build()).get();
    }

    @AfterClass
    public static void tearDownClass() {
        CachedAuthenticationRemote.shutdown();
        if (authenticatorController != null) {
            authenticatorController.shutdown();
        }
    }

    @Before
    public void setUp() throws Exception {
        communicationService = new AuthenticatedCommunicationService();
        remoteService = new AuthenticatedRemoteService();

        communicationService.init(SCOPE);
        remoteService.init(SCOPE);

        communicationService.activate();
        remoteService.activate();
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
    @Test(timeout = 5000)
    public void testCommunication() throws Exception {
        UnitConfig.Builder otherAgentConfig = UnitConfig.newBuilder();
        otherAgentConfig.setId("OtherAgent");
        otherAgentConfig.setType(UnitType.AGENT);
        Permission.Builder otherPermission = otherAgentConfig.getPermissionConfigBuilder().getOtherPermissionBuilder();
        otherPermission.setRead(true).setWrite(false).setAccess(true);

        UnitConfig.Builder userAgentConfig = UnitConfig.newBuilder();
        userAgentConfig.setId("UserAgent");
        userAgentConfig.setType(UnitType.AGENT);
        userAgentConfig.getPermissionConfigBuilder().getOtherPermissionBuilder().setRead(false).setAccess(false).setWrite(false);
        userAgentConfig.getPermissionConfigBuilder().getOwnerPermissionBuilder().setRead(true).setAccess(true).setWrite(true);
        userAgentConfig.getPermissionConfigBuilder().setOwnerId(USER_ID);

        LOGGER.info("Start communication test");

        try (ClosableDataBuilder<Builder> dataBuilder = communicationService.getDataBuilder(this, true)) {
            dataBuilder.getInternalBuilder().addAgentUnitConfig(otherAgentConfig);
            dataBuilder.getInternalBuilder().addAgentUnitConfig(userAgentConfig);
        }


        LOGGER.info("Synchronize remote...");
        remoteService.requestData().get();
        LOGGER.info("Synchronizing remote finished!");

        assertTrue(remoteService.getData().getAgentUnitConfigList().contains(otherAgentConfig.build()));
        assertTrue(!remoteService.getData().getAgentUnitConfigList().contains(userAgentConfig.build()));

        LOGGER.info("Login!");
        SessionManager.getInstance().login(USER_ID, USER_PASSWORD);
        LOGGER.info("Synchronize remote...");
        remoteService.requestData().get();
        LOGGER.info("Synchronizing remote finished!");

        assertTrue(remoteService.getData().getAgentUnitConfigList().contains(otherAgentConfig.build()));
        assertTrue(remoteService.getData().getAgentUnitConfigList().contains(userAgentConfig.build()));

        SessionManager.getInstance().logout();
        LOGGER.info("Synchronize remote...");
        remoteService.requestData().get();
        LOGGER.info("Synchronizing remote finished!");

        assertTrue(remoteService.getData().getAgentUnitConfigList().contains(otherAgentConfig.build()));
        assertTrue(!remoteService.getData().getAgentUnitConfigList().contains(userAgentConfig.build()));
    }

    private class AuthenticatedCommunicationService extends AbstractAuthenticatedCommunicationService<UnitRegistryData, UnitRegistryData.Builder> {

        /**
         * Create a communication service.
         *
         * @throws InstantiationException if the creation fails
         */
        public AuthenticatedCommunicationService() throws InstantiationException {
            super(UnitRegistryData.newBuilder());
        }

        @Override
        protected UnitRegistryData filterDataForUser(UnitRegistryData.Builder dataBuilder, String userId) {
            // remove all agent unit configs for which the user does not have direct read permissions
            for (int i = 0; i < dataBuilder.getAgentUnitConfigCount(); i++) {
                if (!canRead(dataBuilder.getAgentUnitConfig(i), userId)) {
                    dataBuilder.removeAgentUnitConfig(i);
                    i--;
                }
            }
            if (userId == null) {
                assertTrue(dataBuilder.build().getAgentUnitConfigCount() < 2);
            } else {
                assertTrue(dataBuilder.build().getAgentUnitConfigCount() == 2);
            }
            return dataBuilder.build();
        }

        private boolean canRead(final UnitConfig unitConfig, final String userId) {
            if (userId == null || !userId.replace("@", "").equals(unitConfig.getPermissionConfig().getOwnerId())) {
                return unitConfig.getPermissionConfig().getOtherPermission().getRead();
            } else {
                return unitConfig.getPermissionConfig().getOwnerPermission().getRead() || unitConfig.getPermissionConfig().getOtherPermission().getRead();
            }
        }

        @Override
        public void registerMethods(RSBLocalServer server) {
            // do nothing because all methods necessary for this test should registered by the super class
        }

        @Override
        public UnitRegistryData requestStatus() throws CouldNotPerformException {
            return super.requestStatus();
        }

        @Override
        public AuthenticatedValue requestDataAuthenticated(TicketAuthenticatorWrapper ticket) throws CouldNotPerformException {
            return super.requestDataAuthenticated(ticket);
        }


    }

    private class AuthenticatedRemoteService extends AbstractAuthenticatedRemoteService<UnitRegistryData> {

        public AuthenticatedRemoteService() {
            super(UnitRegistryData.class);
        }
    }
}
