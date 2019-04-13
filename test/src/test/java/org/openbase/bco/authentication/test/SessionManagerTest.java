package org.openbase.bco.authentication.test;

/*-
 * #%L
 * BCO Authentication Test
 * %%
 * Copyright (C) 2017 - 2019 openbase.org
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
import org.openbase.bco.authentication.core.AuthenticatorController;
import org.openbase.bco.authentication.lib.CachedAuthenticationRemote;
import org.openbase.bco.authentication.lib.CredentialStore;
import org.openbase.bco.authentication.lib.EncryptionHelper;
import org.openbase.bco.authentication.lib.SessionManager;
import org.openbase.bco.authentication.mock.MockClientStore;
import org.openbase.bco.authentication.mock.MockCredentialStore;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.pattern.Observer;
import org.openbase.jul.schedule.SyncObject;
import org.openbase.type.domotic.authentication.AuthenticatedValueType.AuthenticatedValue;
import org.openbase.type.domotic.authentication.LoginCredentialsType.LoginCredentials;
import org.openbase.type.domotic.authentication.TicketType.Ticket;
import org.openbase.type.domotic.authentication.UserClientPairType.UserClientPair;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutionException;

import static org.junit.Assert.*;

/**
 * @author Sebastian Fast <sfast@techfak.uni-bielefeld.de>
 */
public class SessionManagerTest extends AuthenticationTest {

    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(SessionManagerTest.class);

    private static CredentialStore clientStore;


    public SessionManagerTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
        AuthenticationTest.setUpClass();
        clientStore = new MockClientStore();

        // register an initial user for the authenticator
        try {
            LoginCredentials.Builder loginCredentials = LoginCredentials.newBuilder();
            loginCredentials.setId("InitialUserId");
            loginCredentials.setSymmetric(true);
            loginCredentials.setCredentials(EncryptionHelper.encryptSymmetric(EncryptionHelper.hash("InitialUserPwd"), EncryptionHelper.hash(AuthenticatorController.getInitialPassword())));
            AuthenticatedValue authenticatedValue = AuthenticatedValue.newBuilder().setValue(loginCredentials.build().toByteString()).build();
            CachedAuthenticationRemote.getRemote().register(authenticatedValue).get();
        } catch (InterruptedException | ExecutionException | CouldNotPerformException ex) {
            throw ExceptionPrinter.printHistoryAndReturnThrowable(new CouldNotPerformException("Could not register initial user!"), LOGGER);
        }
    }

    @Before
    public void setUp() throws Exception {
    }

    @After
    public void tearDown() throws Exception {
    }

    /**
     * Test of SessionManager.login() for client.
     *
     * @throws java.lang.Exception
     */
    @Test(timeout = 5000)
    public void registerUser() throws Exception {
        System.out.println("registerUser");
        SessionManager manager = new SessionManager(clientStore);

        // login admin
        manager.loginUser(MockClientStore.ADMIN_ID, MockClientStore.ADMIN_PASSWORD, false);

        // register client
        manager.registerUser("test_user2", "test_password", true).get();

        manager.loginUser("test_user2", "test_password", false);
    }

    /**
     * Test of SessionManager.login() for user.
     *
     * @throws java.lang.Exception
     */
    @Test(timeout = 5000)
    public void loginUser() throws Exception {
        System.out.println("loginUser");
        SessionManager manager = new SessionManager(clientStore);
        manager.loginUser(MockClientStore.ADMIN_ID, MockClientStore.ADMIN_PASSWORD, false);
    }

    /**
     * Test of SessionManager.login() for user.
     *
     * @throws java.lang.Exception
     */
    @Test(timeout = 5000)
    public void loginUserThenOtherUser() throws Exception {
        System.out.println("loginUserThenOtherUser");
        SessionManager manager = new SessionManager(clientStore);
        manager.loginUser(MockClientStore.ADMIN_ID, MockClientStore.ADMIN_PASSWORD, false);
        manager.loginUser(MockClientStore.USER_ID, MockClientStore.USER_PASSWORD, false);
    }

    /**
     * Test of SessionManager.isLoggedIn().
     *
     * @throws java.lang.Exception
     */
    @Test(timeout = 5000)
    public void isLoggedIn() throws Exception {
        System.out.println("isLoggedIn");
        SessionManager manager = new SessionManager(clientStore);
        manager.loginUser(MockClientStore.ADMIN_ID, MockClientStore.ADMIN_PASSWORD, false);

        // user should be authenticated
        assertEquals(true, manager.isLoggedIn());
    }

    /**
     * Test of SessionManager.logout().
     *
     * @throws java.lang.Exception
     */
    @Test(timeout = 5000)
    public void logout() throws Exception {
        System.out.println("logout");
        SessionManager manager = new SessionManager(clientStore);
        manager.loginUser(MockClientStore.ADMIN_ID, MockClientStore.ADMIN_PASSWORD, false);

        manager.logout();
        assertEquals(null, manager.getTicketAuthenticatorWrapper());
        assertArrayEquals(null, manager.getSessionKey());
    }

    /**
     * Test of SessionManager.login() for client.
     *
     * @throws java.lang.Exception
     */
//    @Test(timeout = 5000)
    public void registerClientAndLogin() throws Exception {
        System.out.println("registerClientAndLogin");
        SessionManager manager = new SessionManager(clientStore);

        // login admin
        manager.loginUser(MockClientStore.ADMIN_ID, MockClientStore.ADMIN_PASSWORD, false);

        // register client
        manager.registerClient(MockClientStore.CLIENT_ID).get();

        // login client
        manager.loginClient(MockClientStore.CLIENT_ID, false);

        // login admin
        manager.loginUser(MockClientStore.ADMIN_ID, MockClientStore.ADMIN_PASSWORD, false);

        // register same client should result in Exception
        ExceptionPrinter.setBeQuit(Boolean.TRUE);

        try {
            manager.registerClient(MockClientStore.ADMIN_ID).get();
            fail("You should not be able to register the same client twice.");
        } catch (CouldNotPerformException ex) {
        } finally {
            ExceptionPrinter.setBeQuit(Boolean.FALSE);
        }
    }

    /**
     * Test of SessionManager.login() for client.
     *
     * @throws java.lang.Exception
     */
    @Test(timeout = 5000)
    public void registerClientAndLoginAndLoginUserAndLogout() throws Exception {
        System.out.println("registerClientAndLoginAndLoginUserAndLogout");
        SessionManager manager = new SessionManager(clientStore);
        Ticket ticket;

        // login admin
        manager.loginUser(MockClientStore.ADMIN_ID, MockClientStore.ADMIN_PASSWORD, false);
        ticket = EncryptionHelper.decryptSymmetric(manager.getTicketAuthenticatorWrapper().getTicket(), serviceServerSecretKey, Ticket.class);
        assertEquals(ticket.getUserClientPair().getUserId(), MockClientStore.ADMIN_ID);

        // register client
        manager.registerClient(MockClientStore.CLIENT_ID).get();

        // login client
        manager.loginClient(MockClientStore.CLIENT_ID, false);
        ticket = EncryptionHelper.decryptSymmetric(manager.getTicketAuthenticatorWrapper().getTicket(), serviceServerSecretKey, Ticket.class);
        assertEquals(ticket.getUserClientPair().getClientId(), MockClientStore.CLIENT_ID);

        // login admin (on the client)
        manager.loginUser(MockClientStore.ADMIN_ID, MockClientStore.ADMIN_PASSWORD, false);
        ticket = EncryptionHelper.decryptSymmetric(manager.getTicketAuthenticatorWrapper().getTicket(), serviceServerSecretKey, Ticket.class);
        assertEquals(MockClientStore.ADMIN_ID, ticket.getUserClientPair().getUserId());
        assertEquals(MockClientStore.CLIENT_ID, ticket.getUserClientPair().getClientId());

        // logout admin
        manager.logout();
        assertNotEquals(null, manager.getTicketAuthenticatorWrapper());

        // now client should be logged in again
        ticket = EncryptionHelper.decryptSymmetric(manager.getTicketAuthenticatorWrapper().getTicket(), serviceServerSecretKey, Ticket.class);
        assertEquals(ticket.getUserClientPair().getClientId(), MockClientStore.CLIENT_ID);

        manager.shutdown();
    }

    /**
     * Test if admin can remove himself.
     * Should fail with CouldNotPerformException
     *
     * @throws java.lang.Exception
     */
    @Test(timeout = 5000, expected = CouldNotPerformException.class)
    public void removeAdminHimself() throws Exception {
        System.out.println("removeAdminHimself");
        SessionManager manager = new SessionManager(clientStore);

        // login admin
        manager.loginUser(MockClientStore.ADMIN_ID, MockClientStore.ADMIN_PASSWORD, false);

        try {
            ExceptionPrinter.setBeQuit(Boolean.TRUE);

            // remove himself
            manager.removeUser(MockClientStore.ADMIN_ID).get();
        } finally {
            ExceptionPrinter.setBeQuit(Boolean.FALSE);
        }
    }

    /**
     * Test if an admin can remove another user.
     *
     * @throws java.lang.Exception if something fails.
     */
    @Test(timeout = 5000)
    public void removeAdminOther() throws Exception {
        System.out.println("removeAdminOther");

        final SessionManager manager = new SessionManager(clientStore);

        // login admin
        manager.loginUser(MockClientStore.ADMIN_ID, MockClientStore.ADMIN_PASSWORD, false);

        // remove user
        manager.removeUser(MockClientStore.USER_ID).get();

        // add for test consistency
        manager.registerUser(MockClientStore.USER_ID, MockClientStore.USER_PASSWORD, false).get();
    }

    /**
     * Test of SessionManager.login() for client.
     *
     * @throws java.lang.Exception
     */
    @Test(timeout = 5000, expected = CouldNotPerformException.class)
    public void registerClientAsNonAdmin() throws Exception {
        System.out.println("registerClientAsNonAdmin");
        try {
            ExceptionPrinter.setBeQuit(Boolean.TRUE);
            SessionManager manager = new SessionManager(clientStore);

            // login admin
            manager.loginUser(MockClientStore.USER_ID, MockClientStore.USER_PASSWORD, false);

            // register client
            manager.registerClient(MockClientStore.USER_ID).get();
        } finally {
            ExceptionPrinter.setBeQuit(Boolean.FALSE);
        }
    }

    /**
     * Test of SessionManager.login() for client.
     *
     * @throws java.lang.Exception
     */
    @Test(timeout = 5000)
    public void setAdmin() throws Exception {
        System.out.println("setAdmin");
        SessionManager manager = new SessionManager(clientStore);

        // login admin
        manager.loginUser(MockClientStore.ADMIN_ID, MockClientStore.ADMIN_PASSWORD, false);

        // register client
        manager.registerUser("test_user", "test_password", false).get();
        manager.setAdministrator("test_user", true).get();
        manager.setAdministrator("test_user", false).get();
    }

    /**
     * Test of SessionManager.login() for client.
     *
     * @throws java.lang.Exception
     */
    @Test(timeout = 5000, expected = CouldNotPerformException.class)
    public void setAdminAsNonAdmin() throws Exception {
        System.out.println("setAdminAsNonAdmin");
        try {
            ExceptionPrinter.setBeQuit(Boolean.TRUE);
            SessionManager manager = new SessionManager(clientStore);

            // login admin
            manager.loginUser(MockClientStore.USER_ID, MockClientStore.USER_PASSWORD, false);

            // register client
            manager.setAdministrator(MockClientStore.USER_ID, true).get();
        } finally {
            ExceptionPrinter.setBeQuit(Boolean.FALSE);
        }
    }

    /**
     * Test of method isAdmin
     *
     * @throws Exception
     */
    @Test(timeout = 5000)
    public void isAdmin() throws Exception {
        System.out.println("isAdmin");
        SessionManager manager = new SessionManager(clientStore);

        // login admin
        manager.loginUser(MockClientStore.ADMIN_ID, MockClientStore.ADMIN_PASSWORD, false);

        assertEquals(manager.isAdmin(), true);
    }

    private int notificationCounter = 0;

    /**
     * Test if the login observable notifies correctly.
     *
     * @throws Exception
     */
    @Test(timeout = 5000)
    public void loginObservableTest() throws Exception {
        System.out.println("loginObservableTest");

        final SyncObject loginSyncObject = new SyncObject("LoginSyncObject");
        final long maxWaitTime = 1000;

        Observer<SessionManager, UserClientPair> loginObserver = (SessionManager source, UserClientPair data) -> {
            synchronized (loginSyncObject) {
                notificationCounter++;
                loginSyncObject.notifyAll();
            }
        };

        SessionManager sessionManager = new SessionManager();
        sessionManager.addLoginObserver(loginObserver);

        sessionManager.loginUser(MockCredentialStore.ADMIN_ID, MockCredentialStore.ADMIN_PASSWORD, false);
        synchronized (loginSyncObject) {
            loginSyncObject.wait(maxWaitTime);
        }
        assertEquals("Notification counter should be 1 after the first login", 1, notificationCounter);
        synchronized (loginSyncObject) {
            loginSyncObject.wait(maxWaitTime);
        }
        sessionManager.loginUser(MockCredentialStore.USER_ID, MockCredentialStore.USER_PASSWORD, false);
        synchronized (loginSyncObject) {
            loginSyncObject.wait(maxWaitTime);
        }
        assertEquals("Notification counter should be 2 after logging in another user", 2, notificationCounter);
        sessionManager.logout();
        synchronized (loginSyncObject) {
            loginSyncObject.wait(maxWaitTime);
        }
        assertEquals("Notification counter should be 3 after logout", 3, notificationCounter);

        sessionManager.removeLoginObserver(loginObserver);
    }
}
