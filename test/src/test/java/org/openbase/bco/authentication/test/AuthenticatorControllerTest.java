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
import org.openbase.bco.authentication.lib.AuthenticationClientHandler;
import org.openbase.bco.authentication.lib.CachedAuthenticationRemote;
import org.openbase.bco.authentication.lib.EncryptionHelper;
import org.openbase.bco.authentication.lib.SessionManager;
import org.openbase.bco.authentication.mock.MockClientStore;
import org.openbase.bco.authentication.mock.MockCredentialStore;
import org.openbase.jps.core.JPService;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.slf4j.LoggerFactory;
import rst.domotic.authentication.AuthenticatorType;
import rst.domotic.authentication.LoginCredentialsChangeType.LoginCredentialsChange;
import rst.domotic.authentication.TicketAuthenticatorWrapperType.TicketAuthenticatorWrapper;
import rst.domotic.authentication.TicketSessionKeyWrapperType.TicketSessionKeyWrapper;

import javax.crypto.BadPaddingException;
import java.util.List;
import java.util.concurrent.ExecutionException;

import static org.junit.Assert.fail;

/**
 *
 * @author <a href="mailto:thuxohl@techfak.uni-bielefeld.de">Tamino Huxohl</a>
 */
public class AuthenticatorControllerTest {

    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(AuthenticatorControllerTest.class);

    private static AuthenticatorController authenticatorController;

    public AuthenticatorControllerTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
        JPService.setupJUnitTestMode();

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
     * Test of communication between ClientRemote and AuthenticatorController.
     *
     * @throws java.lang.Exception
     */
    @Test(timeout = 5000)
    public void testCommunication() throws Exception {
        System.out.println("testCommunication");

        String userId = MockCredentialStore.USER_ID + "@";
        byte[] userPasswordHash = MockCredentialStore.USER_PASSWORD_HASH;

        // handle KDC request on server side
        TicketSessionKeyWrapper ticketSessionKeyWrapper = CachedAuthenticationRemote.getRemote().requestTicketGrantingTicket(userId).get();

        // handle KDC response on client side
        List<Object> list = AuthenticationClientHandler.handleKeyDistributionCenterResponse(userId, userPasswordHash, true, ticketSessionKeyWrapper);
        TicketAuthenticatorWrapper clientTicketAuthenticatorWrapper = (TicketAuthenticatorWrapper) list.get(0); // save at somewhere temporarily
        byte[] clientTGSSessionKey = (byte[]) list.get(1); // save TGS session key somewhere on client side

        // handle TGS request on server side
        ticketSessionKeyWrapper = CachedAuthenticationRemote.getRemote().requestClientServerTicket(clientTicketAuthenticatorWrapper).get();

        // handle TGS response on client side
        list = AuthenticationClientHandler.handleTicketGrantingServiceResponse(userId, clientTGSSessionKey, ticketSessionKeyWrapper);
        clientTicketAuthenticatorWrapper = (TicketAuthenticatorWrapper) list.get(0); // save at somewhere temporarily
        byte[] clientSSSessionKey = (byte[]) list.get(1); // save SS session key somewhere on client side

        // init SS request on client side
        clientTicketAuthenticatorWrapper = AuthenticationClientHandler.initServiceServerRequest(clientSSSessionKey, clientTicketAuthenticatorWrapper);

        // handle SS request on server side
        TicketAuthenticatorWrapper serverTicketAuthenticatorWrapper = CachedAuthenticationRemote.getRemote().validateClientServerTicket(clientTicketAuthenticatorWrapper).get();

        // handle SS response on client side
        AuthenticationClientHandler.handleServiceServerResponse(clientSSSessionKey, clientTicketAuthenticatorWrapper, serverTicketAuthenticatorWrapper);
    }

    /**
     * Test if an exception is correctly thrown if a user requests a ticket granting ticket with
     * a wrong client id.
     *
     * @throws Exception
     */
    @Test(timeout = 5000, expected = ExecutionException.class)
    public void testAuthenticationWithNonExistentUser() throws Exception {
        System.out.println("testAuthenticationWithNonExistentUser");
        try {
            ExceptionPrinter.setBeQuit(Boolean.TRUE);

            String nonExistentUserId = "12abc-15123";

            CachedAuthenticationRemote.getRemote().requestTicketGrantingTicket(nonExistentUserId + "@").get();
        } finally {
            ExceptionPrinter.setBeQuit(Boolean.FALSE);
        }
    }

    /**
     * Test if the correct exception is thrown if the wrong password for a user is used.
     *
     * @throws Exception
     */
    @Test(timeout = 5000, expected = BadPaddingException.class)
    public void testAuthenticationWithIncorrectPassword() throws Exception {
        System.out.println("testAuthenticationWithIncorrectPassword");

        String userId = MockCredentialStore.USER_ID + "@";
        String password = "wrongpassword";
        byte[] passwordHash = EncryptionHelper.hash(password);

        TicketSessionKeyWrapper ticketSessionKeyWrapper = CachedAuthenticationRemote.getRemote().requestTicketGrantingTicket(userId).get();
        AuthenticationClientHandler.handleKeyDistributionCenterResponse(userId, passwordHash, true, ticketSessionKeyWrapper);
    }

    @Test(timeout = 5000)
    public void testChangeCredentials() throws Exception {
        System.out.println("testChangeCredentials");

        String userId = MockCredentialStore.USER_ID + "@";
        byte[] userPasswordHash = MockCredentialStore.USER_PASSWORD_HASH;
        byte[] newPasswordHash = MockCredentialStore.USER_PASSWORD_HASH;

        // handle KDC request on server side
        TicketSessionKeyWrapper ticketSessionKeyWrapper = CachedAuthenticationRemote.getRemote().requestTicketGrantingTicket(userId).get();

        // handle KDC response on client side
        List<Object> list = AuthenticationClientHandler.handleKeyDistributionCenterResponse(userId, userPasswordHash, true, ticketSessionKeyWrapper);
        TicketAuthenticatorWrapper clientTicketAuthenticatorWrapper = (TicketAuthenticatorWrapper) list.get(0); // save at somewhere temporarily
        byte[] clientTGSSessionKey = (byte[]) list.get(1); // save TGS session key somewhere on client side

        // handle TGS request on server side
        ticketSessionKeyWrapper = CachedAuthenticationRemote.getRemote().requestClientServerTicket(clientTicketAuthenticatorWrapper).get();

        // handle TGS response on client side
        list = AuthenticationClientHandler.handleTicketGrantingServiceResponse(userId, clientTGSSessionKey, ticketSessionKeyWrapper);
        clientTicketAuthenticatorWrapper = (TicketAuthenticatorWrapper) list.get(0); // save at somewhere temporarily
        byte[] clientSSSessionKey = (byte[]) list.get(1); // save SS session key somewhere on client side

        // init SS request on client side
        clientTicketAuthenticatorWrapper = AuthenticationClientHandler.initServiceServerRequest(clientSSSessionKey, clientTicketAuthenticatorWrapper);

        LoginCredentialsChange loginCredentialsChange = LoginCredentialsChange.newBuilder()
                .setId(MockClientStore.USER_ID)
                .setOldCredentials(EncryptionHelper.encryptSymmetric(userPasswordHash, clientSSSessionKey))
                .setNewCredentials(EncryptionHelper.encryptSymmetric(newPasswordHash, clientSSSessionKey))
                .setTicketAuthenticatorWrapper(clientTicketAuthenticatorWrapper)
                .build();

        CachedAuthenticationRemote.getRemote().changeCredentials(loginCredentialsChange).get();
    }

    /**
     * Tests the feature of changing passwords for other users.
     * Only admins should be allowed to do this.
     * @throws Exception
     */
    @Test(timeout = 5000)
    public void testChangeOthersCredentials() throws Exception {
        System.out.println("testChangeOthersCredentials");

        // Trying to change the admin's password with a normal user. This should not work.
        String userId = MockCredentialStore.USER_ID + "@";
        byte[] userPasswordHash = MockCredentialStore.USER_PASSWORD_HASH;
        byte[] adminPasswordHash = MockCredentialStore.ADMIN_PASSWORD_HASH;
        byte[] newPasswordHash = MockCredentialStore.USER_PASSWORD_HASH;

        // handle KDC request on server side
        TicketSessionKeyWrapper ticketSessionKeyWrapper = CachedAuthenticationRemote.getRemote().requestTicketGrantingTicket(userId).get();

        // handle KDC response on client side
        List<Object> list = AuthenticationClientHandler.handleKeyDistributionCenterResponse(userId, userPasswordHash, true, ticketSessionKeyWrapper);
        TicketAuthenticatorWrapper clientTicketAuthenticatorWrapper = (TicketAuthenticatorWrapper) list.get(0); // save at somewhere temporarily
        byte[] clientTGSSessionKey = (byte[]) list.get(1); // save TGS session key somewhere on client side

        // handle TGS request on server side
        ticketSessionKeyWrapper = CachedAuthenticationRemote.getRemote().requestClientServerTicket(clientTicketAuthenticatorWrapper).get();

        // handle TGS response on client side
        list = AuthenticationClientHandler.handleTicketGrantingServiceResponse(userId, clientTGSSessionKey, ticketSessionKeyWrapper);
        clientTicketAuthenticatorWrapper = (TicketAuthenticatorWrapper) list.get(0); // save at somewhere temporarily
        byte[] clientSSSessionKey = (byte[]) list.get(1); // save SS session key somewhere on client side

        // init SS request on client side
        clientTicketAuthenticatorWrapper = AuthenticationClientHandler.initServiceServerRequest(clientSSSessionKey, clientTicketAuthenticatorWrapper);

        LoginCredentialsChange loginCredentialsChange = LoginCredentialsChange.newBuilder()
                .setId(MockClientStore.ADMIN_ID)
                .setOldCredentials(EncryptionHelper.encryptSymmetric(adminPasswordHash, clientSSSessionKey))
                .setNewCredentials(EncryptionHelper.encryptSymmetric(newPasswordHash, clientSSSessionKey))
                .setTicketAuthenticatorWrapper(clientTicketAuthenticatorWrapper)
                .build();

        try {
            ExceptionPrinter.setBeQuit(Boolean.TRUE);
            CachedAuthenticationRemote.getRemote().changeCredentials(loginCredentialsChange).get();
            fail("A user should not be able to change the password of another user.");
        } catch (CouldNotPerformException | ExecutionException ex) {
        } finally {
            ExceptionPrinter.setBeQuit(Boolean.FALSE);
        }

        // Trying to change the user's password with an admin account. This should work.
        String adminId = MockCredentialStore.ADMIN_ID + "@";

        // handle KDC request on server side
        ticketSessionKeyWrapper = CachedAuthenticationRemote.getRemote().requestTicketGrantingTicket(adminId).get();

        // handle KDC response on client side
        list = AuthenticationClientHandler.handleKeyDistributionCenterResponse(adminId, adminPasswordHash, true, ticketSessionKeyWrapper);
        clientTicketAuthenticatorWrapper = (TicketAuthenticatorWrapper) list.get(0); // save at somewhere temporarily
        clientTGSSessionKey = (byte[]) list.get(1); // save TGS session key somewhere on client side

        // handle TGS request on server side
        ticketSessionKeyWrapper = CachedAuthenticationRemote.getRemote().requestClientServerTicket(clientTicketAuthenticatorWrapper).get();

        // handle TGS response on client side
        list = AuthenticationClientHandler.handleTicketGrantingServiceResponse(adminId, clientTGSSessionKey, ticketSessionKeyWrapper);
        clientTicketAuthenticatorWrapper = (TicketAuthenticatorWrapper) list.get(0); // save at somewhere temporarily
        clientSSSessionKey = (byte[]) list.get(1); // save SS session key somewhere on client side

        // init SS request on client side
        clientTicketAuthenticatorWrapper = AuthenticationClientHandler.initServiceServerRequest(clientSSSessionKey, clientTicketAuthenticatorWrapper);

        loginCredentialsChange = LoginCredentialsChange.newBuilder()
                .setId(MockClientStore.USER_ID)
                .setOldCredentials(EncryptionHelper.encryptSymmetric(userPasswordHash, clientSSSessionKey))
                .setNewCredentials(EncryptionHelper.encryptSymmetric(newPasswordHash, clientSSSessionKey))
                .setTicketAuthenticatorWrapper(clientTicketAuthenticatorWrapper)
                .build();

        CachedAuthenticationRemote.getRemote().changeCredentials(loginCredentialsChange).get();
    }

    /**
     * Test of async communication between ClientRemote and AuthenticatorController.
     * After login two requests will be sent asynchronously. 
     * The first request will return later than the second request. 
     * This should work and only the newest ticket should then be used for further requests.
     *
     * @throws java.lang.Exception
     */
    @Test(timeout = 5000)
    public void testAsyncCommunication() throws Exception {
        System.out.println("testAsyncCommunication");

        String userId = MockCredentialStore.USER_ID + "@";
        byte[] userPasswordHash = MockCredentialStore.USER_PASSWORD_HASH;

        // handle KDC request on server side
        TicketSessionKeyWrapper ticketSessionKeyWrapper = CachedAuthenticationRemote.getRemote().requestTicketGrantingTicket(userId).get();

        // handle KDC response on client side
        List<Object> list = AuthenticationClientHandler.handleKeyDistributionCenterResponse(userId, userPasswordHash, true, ticketSessionKeyWrapper);
        TicketAuthenticatorWrapper clientTicketAuthenticatorWrapper = (TicketAuthenticatorWrapper) list.get(0); // save at somewhere temporarily
        byte[] clientTGSSessionKey = (byte[]) list.get(1); // save TGS session key somewhere on client side

        // handle TGS request on server side
        ticketSessionKeyWrapper = CachedAuthenticationRemote.getRemote().requestClientServerTicket(clientTicketAuthenticatorWrapper).get();

        // handle TGS response on client side
        list = AuthenticationClientHandler.handleTicketGrantingServiceResponse(userId, clientTGSSessionKey, ticketSessionKeyWrapper);
        clientTicketAuthenticatorWrapper = (TicketAuthenticatorWrapper) list.get(0); // save at somewhere temporarily
        byte[] clientSSSessionKey = (byte[]) list.get(1); // save SS session key somewhere on client side

        // init SS request on client side
        TicketAuthenticatorWrapper request1 = AuthenticationClientHandler.initServiceServerRequest(clientSSSessionKey, clientTicketAuthenticatorWrapper);
        
        Thread.sleep(100);
        
        // init SS request on client side
        TicketAuthenticatorWrapper request2 = AuthenticationClientHandler.initServiceServerRequest(clientSSSessionKey, clientTicketAuthenticatorWrapper);
        
        AuthenticatorType.Authenticator request2auth = EncryptionHelper.decryptSymmetric(request2.getAuthenticator(), clientSSSessionKey, AuthenticatorType.Authenticator.class);
        AuthenticatorType.Authenticator request1auth = EncryptionHelper.decryptSymmetric(request1.getAuthenticator(), clientSSSessionKey, AuthenticatorType.Authenticator.class);
        
        System.err.println(request2auth.getTimestamp().getTime());
        System.err.println(request1auth.getTimestamp().getTime());  

        // handle SS request on server side
        TicketAuthenticatorWrapper response2 = CachedAuthenticationRemote.getRemote().validateClientServerTicket(request2).get();

        // handle SS response on client side
        AuthenticationClientHandler.handleServiceServerResponse(clientSSSessionKey, request2, response2);

        // handle SS request on server side
        TicketAuthenticatorWrapper response1 = CachedAuthenticationRemote.getRemote().validateClientServerTicket(request1).get();

        // handle SS response on client side
        AuthenticationClientHandler.handleServiceServerResponse(clientSSSessionKey, request1, response1);
        
        SessionManager manager = new SessionManager(clientSSSessionKey);
        manager.setTicketAuthenticatorWrapper(response2);
        manager.setTicketAuthenticatorWrapper(response1);
        
        AuthenticatorType.Authenticator response2auth = EncryptionHelper.decryptSymmetric(response2.getAuthenticator(), clientSSSessionKey, AuthenticatorType.Authenticator.class);
        AuthenticatorType.Authenticator sessionManagerAuth = EncryptionHelper.decryptSymmetric(manager.getTicketAuthenticatorWrapper().getAuthenticator(), clientSSSessionKey, AuthenticatorType.Authenticator.class);
        
        System.err.println(response2auth.getTimestamp().getTime());
        System.err.println(sessionManagerAuth.getTimestamp().getTime());  
        
        Assert.assertTrue(response2auth.getTimestamp().getTime() == sessionManagerAuth.getTimestamp().getTime());
    }
}
