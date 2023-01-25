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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.openbase.bco.authentication.core.AuthenticationController;
import org.openbase.bco.authentication.lib.AuthenticatedServerManager;
import org.openbase.bco.authentication.lib.CachedAuthenticationRemote;
import org.openbase.bco.authentication.lib.EncryptionHelper;
import org.openbase.bco.authentication.lib.SessionManager;
import org.openbase.bco.authentication.mock.MockCredentialStore;
import org.openbase.jps.core.JPService;
import org.openbase.jul.communication.mqtt.test.MqttIntegrationTest;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class AuthenticationTest extends MqttIntegrationTest {

    public static AuthenticationController authenticationController;
    public static byte[] serviceServerSecretKey = EncryptionHelper.generateKey();

    @BeforeEach
    public void setupAuthentication() throws Throwable {
        JPService.setupJUnitTestMode();
        CachedAuthenticationRemote.prepare();
        authenticationController = new AuthenticationController(MockCredentialStore.getInstance(), serviceServerSecretKey);
        authenticationController.init();
        authenticationController.activate();
        authenticationController.waitForActivation();
        assertNotNull(AuthenticationController.getInitialPassword(), "Initial password has not been generated despite an empty registry");
    }

    @AfterEach
    public void tearDownAuthentication() {
        // reset credential store because it could have been changed in a test
        MockCredentialStore.getInstance().reset();
        CachedAuthenticationRemote.shutdown();
        if (authenticationController != null) {
            authenticationController.shutdown();
        }
        AuthenticatedServerManager.shutdown();
        SessionManager.getInstance().shutdown();
        authenticationController = null;
    }
}
