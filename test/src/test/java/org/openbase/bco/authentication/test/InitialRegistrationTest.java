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
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openbase.bco.authentication.core.AuthenticatorController;
import org.openbase.bco.authentication.lib.CachedAuthenticationRemote;
import org.openbase.bco.authentication.lib.EncryptionHelper;
import org.openbase.bco.authentication.lib.SessionManager;
import org.openbase.jps.core.JPService;
import org.slf4j.LoggerFactory;
import rst.domotic.authentication.LoginCredentialsChangeType.LoginCredentialsChange;

/**
 *
 * @author <a href="mailto:thuxohl@techfak.uni-bielefeld.de">Tamino Huxohl</a>
 */
public class InitialRegistrationTest {

    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(AuthenticatorControllerTest.class);

    public InitialRegistrationTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
        JPService.setupJUnitTestMode();
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    /**
     *
     * @throws java.lang.Exception
     */
    @Test(timeout = 5000)
    public void initialRegistrationTest() throws Exception {
        LOGGER.info("initialRegistrationTest");

        // start an authenticator with an empty registry
        AuthenticatorController authenticator = new AuthenticatorController();
        authenticator.init();
        authenticator.activate();
        authenticator.waitForActivation();

        assertTrue("Initial password has not been generated despite an empty registry", AuthenticatorController.getInitialPassword() != null);

        // register the initial user via session manager
        String userId = "First";
        String password = "random";
        LoginCredentialsChange.Builder loginCredentials = LoginCredentialsChange.newBuilder();
        loginCredentials.setId(userId);
        loginCredentials.setNewCredentials(EncryptionHelper.encryptSymmetric(EncryptionHelper.hash(password), EncryptionHelper.hash(authenticator.getInitialPassword())));
        CachedAuthenticationRemote.getRemote().register(loginCredentials.build()).get();

        // test if login works afterwards
        SessionManager.getInstance().login(userId, password);
        assertTrue("User is not logged in", SessionManager.getInstance().isLoggedIn());
        assertTrue("User cannot be authenticated", SessionManager.getInstance().isAuthenticated());

        // logout and shutdown authenticator to not interfere with other tests
        SessionManager.getInstance().logout();
        authenticator.shutdown();
    }
}
