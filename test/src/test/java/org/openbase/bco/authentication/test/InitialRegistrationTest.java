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

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openbase.bco.authentication.lib.CachedAuthenticationRemote;
import org.openbase.bco.authentication.lib.EncryptionHelper;
import org.openbase.bco.authentication.lib.SessionManager;
import org.slf4j.LoggerFactory;
import rst.domotic.authentication.LoginCredentialsChangeType.LoginCredentialsChange;

import static org.junit.Assert.assertTrue;

/**
 * @author <a href="mailto:thuxohl@techfak.uni-bielefeld.de">Tamino Huxohl</a>
 */
public class InitialRegistrationTest extends AuthenticationTest {

    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(AuthenticatorControllerTest.class);

    public InitialRegistrationTest() {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    /**
     * @throws java.lang.Exception
     */
    @Test(timeout = 5000)
    public void initialRegistrationTest() throws Exception {
        LOGGER.info("initialRegistrationTest");

        // register the initial user via session manager
        String userId = "First";
        String password = "random";
        LoginCredentialsChange.Builder loginCredentials = LoginCredentialsChange.newBuilder();
        loginCredentials.setId(userId);
        loginCredentials.setNewCredentials(EncryptionHelper.encryptSymmetric(EncryptionHelper.hash(password), EncryptionHelper.hash(authenticatorController.getInitialPassword())));
        CachedAuthenticationRemote.getRemote().register(loginCredentials.build()).get();

        // test if login works afterwards
        SessionManager.getInstance().login(userId, password);
        assertTrue("User is not logged in", SessionManager.getInstance().isLoggedIn());
        assertTrue("User cannot be authenticated", SessionManager.getInstance().isAuthenticated());
    }
}
