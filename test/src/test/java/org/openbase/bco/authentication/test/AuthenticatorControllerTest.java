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
import org.openbase.bco.authentication.core.mock.MockAuthenticationRegistry;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutionException;
import org.junit.After;
import org.junit.AfterClass;
import static org.junit.Assert.fail;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openbase.bco.authentication.core.AuthenticatorController;
import org.openbase.bco.authentication.lib.AuthenticationClientHandler;
import org.openbase.bco.authentication.lib.ClientRemote;
import org.openbase.bco.authentication.lib.EncryptionHelper;
import org.openbase.bco.authentication.lib.jp.JPAuthenticationSimulationMode;
import org.openbase.jps.core.JPService;
import org.slf4j.LoggerFactory;
import rst.domotic.authentication.TicketAuthenticatorWrapperType.TicketAuthenticatorWrapper;
import rst.domotic.authentication.TicketSessionKeyWrapperType.TicketSessionKeyWrapper;

/**
 *
 * @author <a href="mailto:thuxohl@techfak.uni-bielefeld.de">Tamino Huxohl</a>
 */
public class AuthenticatorControllerTest {

    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(AuthenticatorControllerTest.class);

    private static AuthenticatorController authenticatorController;

    private static ClientRemote clientRemote;

    public AuthenticatorControllerTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
        JPService.setupJUnitTestMode();

        authenticatorController = new AuthenticatorController(new MockAuthenticationRegistry());
        authenticatorController.init();
        authenticatorController.activate();

        clientRemote = new ClientRemote();
        clientRemote.init();
        clientRemote.activate();
        
        Thread.sleep(100);
    }

    @AfterClass
    public static void tearDownClass() {
        if (clientRemote != null) {
            clientRemote.shutdown();
        }
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
     * Test of communication between ClientRemote and AuthenticatorController.
     *
     * @throws java.lang.Exception
     */
    @Test(timeout = 5000)
    public void testCommunication() throws Exception {
        System.out.println("testCommunication");

        String clientId = MockAuthenticationRegistry.CLIENT_ID;
        byte[] clientPasswordHash = MockAuthenticationRegistry.PASSWORD_HASH;

        // handle KDC request on server side
        TicketSessionKeyWrapper ticketSessionKeyWrapper = clientRemote.requestTicketGrantingTicket(clientId).get();

        // handle KDC response on client side
        List<Object> list = AuthenticationClientHandler.handleKDCResponse(clientId, clientPasswordHash, ticketSessionKeyWrapper);
        TicketAuthenticatorWrapper clientTicketAuthenticatorWrapper = (TicketAuthenticatorWrapper) list.get(0); // save at somewhere temporarily
        byte[] clientTGSSessionKey = (byte[]) list.get(1); // save TGS session key somewhere on client side

        // handle TGS request on server side
        ticketSessionKeyWrapper = clientRemote.requestClientServerTicket(clientTicketAuthenticatorWrapper).get();

        // handle TGS response on client side
        list = AuthenticationClientHandler.handleTGSResponse(clientId, clientTGSSessionKey, ticketSessionKeyWrapper);
        clientTicketAuthenticatorWrapper = (TicketAuthenticatorWrapper) list.get(0); // save at somewhere temporarily
        byte[] clientSSSessionKey = (byte[]) list.get(1); // save SS session key somewhere on client side

        // init SS request on client side
        clientTicketAuthenticatorWrapper = AuthenticationClientHandler.initSSRequest(clientSSSessionKey, clientTicketAuthenticatorWrapper);

        // handle SS request on server side
        TicketAuthenticatorWrapper serverTicketAuthenticatorWrapper = clientRemote.validateClientServerTicket(clientTicketAuthenticatorWrapper).get();

        // handle SS response on client side
        AuthenticationClientHandler.handleSSResponse(clientSSSessionKey, clientTicketAuthenticatorWrapper, serverTicketAuthenticatorWrapper);
    }

    /**
     * Test if an exception is correctly thrown if a user requests a ticket granting ticket with
     * a wrong client id.
     *
     * @throws Exception
     */
    @Test(timeout = 5000)
    public void testAuthenticationWithNonExistentUser() throws Exception {
        System.out.println("testAuthenticationWithNonExistentUser");

        String nonExistentClientId = "12abc-15123";

        try {
            clientRemote.requestTicketGrantingTicket(nonExistentClientId).get();
        } catch (ExecutionException ex) {
            // test successful
            return;
        }
        fail("Exception has not been thrown even though there should be no user[" + nonExistentClientId + "] in the database!");
    }

    /**
     * Test if the correct exception is thrown if the wrong password for a user is used.
     *
     * @throws Exception
     */
    @Test(timeout = 5000)
    public void testAuthenticationWithIncorrectPassword() throws Exception {
        System.out.println("testAuthenticationWithIncorrectPassword");

        String clientId = MockAuthenticationRegistry.CLIENT_ID;
        String password = "wrongpassword";
        byte[] passwordHash = EncryptionHelper.hash(password);

        TicketSessionKeyWrapper ticketSessionKeyWrapper = clientRemote.requestTicketGrantingTicket(clientId).get();
        try {
            AuthenticationClientHandler.handleKDCResponse(clientId, passwordHash, ticketSessionKeyWrapper);
        } catch (IOException ex) {
            return;
        }
        fail("Exception has not been thrown even though user[" + clientId + "] does not use the password[" + password + "]!");
    }
}
