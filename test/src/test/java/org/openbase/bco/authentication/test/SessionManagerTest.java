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
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import org.openbase.bco.authentication.core.AuthenticationRegistry;
import org.openbase.bco.authentication.core.AuthenticatorController;
import org.openbase.bco.authentication.lib.SessionManager;
import org.openbase.jps.core.JPService;

/**
 *
 * @author Sebastian Fast <sfast@techfak.uni-bielefeld.de>
 */
public class SessionManagerTest {

    private static AuthenticatorController authenticatorController;
    private static AuthenticationRegistry authenticationRegistry;

    public SessionManagerTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
        JPService.setupJUnitTestMode();

        authenticationRegistry = new MockAuthenticationRegistry();

        authenticatorController = new AuthenticatorController(authenticationRegistry);
        authenticatorController.init();
        authenticatorController.activate();
        authenticatorController.waitForActivation();
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
        if (authenticatorController != null) {
            authenticatorController.shutdown();
        }
    }

    @Before
    public void setUp() throws Exception {

    }

    @After
    public void tearDown() throws Exception {

    }

    /**
     * Test of SessionManager.login() for user.
     *
     * @throws java.lang.Exception
     */
    @Test(timeout = 5000)
    public void testUserLogin() throws Exception {
        String clientId = MockAuthenticationRegistry.USER_ID;
        String password = MockAuthenticationRegistry.USER_PASSWORD;

        SessionManager manager = new SessionManager();
        boolean result = manager.login(clientId, password);

        assertEquals(true, result);
    }

    /**
     * Test of SessionManager.login() for client.
     *
     * @throws java.lang.Exception
     */
    @Test(timeout = 5000)
    public void testClientLogin() throws Exception {
        String clientId = MockAuthenticationRegistry.CLIENT_ID;
        byte[] privateKey = MockAuthenticationRegistry.CLIENT_PRIVATE_KEY;
        byte[] publicKey = MockAuthenticationRegistry.CLIENT_PUBLIC_KEY;

        SessionManager manager = new SessionManager(privateKey, publicKey);
        boolean result = manager.login(clientId);

        assertEquals(true, result);
    }

    /**
     * Test of SessionManager.isLoggedIn().
     *
     * @throws java.lang.Exception
     */
    @Test(timeout = 5000)
    public void testUserIsLoggedIn() throws Exception {
        String clientId = MockAuthenticationRegistry.USER_ID;
        String password = MockAuthenticationRegistry.USER_PASSWORD;
        
        SessionManager manager = new SessionManager();
        
        // user should not be authenticated
        assertEquals(false, manager.isLoggedIn());
        
        boolean result = manager.login(clientId, password);
        
        // should result true
        assertEquals(true, result);
        
        // user should be authenticated
        assertEquals(true, manager.isLoggedIn());
    }

    /**
     * Test of SessionManager.isAuthenticated().
     *
     * @throws java.lang.Exception
     */
    @Test(timeout = 5000)
    public void testUserIsAuthenticated() throws Exception {
        String clientId = MockAuthenticationRegistry.USER_ID;
        String password = MockAuthenticationRegistry.USER_PASSWORD;
        
        SessionManager manager = new SessionManager();
        
        // user should not be authenticated
        assertEquals(false, manager.isAuthenticated());
        
        boolean result = manager.login(clientId, password);
        
        // should result in true
        assertEquals(true, result);
        
        // user should be authenticated
        assertEquals(true, manager.isAuthenticated());
    }

    /**
     * Test of SessionManager.logout().
     *
     * @throws java.lang.Exception
     */
    @Test(timeout = 5000)
    public void testLogout() throws Exception {
        String clientId = MockAuthenticationRegistry.USER_ID;
        String password = MockAuthenticationRegistry.USER_PASSWORD;

        SessionManager manager = new SessionManager();
        boolean result = manager.login(clientId, password);

        assertEquals(true, result);

        manager.logout();
        assertEquals(null, manager.getTicketAuthenticatorWrapper());
        assertArrayEquals(null, manager.getSessionKey());
    }

}
