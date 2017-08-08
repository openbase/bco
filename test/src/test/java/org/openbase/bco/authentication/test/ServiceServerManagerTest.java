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

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openbase.bco.authentication.core.AuthenticatorController;
import org.openbase.bco.authentication.lib.AuthenticationClientHandler;
import org.openbase.bco.authentication.lib.CachedAuthenticationRemote;
import org.openbase.bco.authentication.lib.ServiceServerManager;
import org.openbase.bco.authentication.lib.SessionManager;
import org.openbase.bco.authentication.lib.jp.JPInitializeCredentials;
import org.openbase.bco.authentication.lib.mock.MockCredentialStore;
import org.openbase.jps.core.JPService;
import org.slf4j.LoggerFactory;
import rst.domotic.authentication.TicketAuthenticatorWrapperType.TicketAuthenticatorWrapper;

/**
 *
 * @author <a href="mailto:thuxohl@techfak.uni-bielefeld.de">Tamino Huxohl</a>
 */
public class ServiceServerManagerTest {

    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(AuthenticatorControllerTest.class);

    private static AuthenticatorController authenticatorController;

    public ServiceServerManagerTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
        JPService.setupJUnitTestMode();
        JPService.registerProperty(JPInitializeCredentials.class);

        authenticatorController = new AuthenticatorController(new MockCredentialStore());
        authenticatorController.init();
        authenticatorController.activate();
        authenticatorController.waitForActivation();
    }

    @AfterClass
    public static void tearDownClass() {
        CachedAuthenticationRemote.shutdown();
        if (authenticatorController != null) {
            authenticatorController.shutdown();
        }
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    /**
     * Test if the service server can validate client server tickets.
     *
     * @throws java.lang.Exception
     */
    @Test(timeout = 5000)
    public void testServiceServerManagerValidation() throws Exception {
        System.out.println("testServiceServerManagerValidation");

        SessionManager sessionManager = new SessionManager();
        sessionManager.login(MockCredentialStore.USER_ID, MockCredentialStore.USER_PASSWORD);

        TicketAuthenticatorWrapper request = sessionManager.initializeServiceServerRequest();

        ServiceServerManager serviceServerManager = ServiceServerManager.getInstance();
        TicketAuthenticatorWrapper response = serviceServerManager.evaluateClientServerTicket(request).getTicketAuthenticatorWrapper();

        AuthenticationClientHandler.handleServiceServerResponse(sessionManager.getSessionKey(), request, response);
    }
}
