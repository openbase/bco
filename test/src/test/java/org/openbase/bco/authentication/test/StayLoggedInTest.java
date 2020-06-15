package org.openbase.bco.authentication.test;

/*-
 * #%L
 * BCO Authentication Test
 * %%
 * Copyright (C) 2017 - 2020 openbase.org
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

import org.junit.BeforeClass;
import org.junit.Test;
import org.openbase.bco.authentication.lib.AuthenticationServerHandler;
import org.openbase.bco.authentication.lib.CachedAuthenticationRemote;
import org.openbase.bco.authentication.lib.SessionManager;
import org.openbase.bco.authentication.lib.exception.SessionExpiredException;
import org.openbase.bco.authentication.lib.jp.JPSessionTimeout;
import org.openbase.bco.authentication.mock.MockClientStore;
import org.openbase.jps.core.JPService;
import org.openbase.jul.exception.ExceptionProcessor;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.type.domotic.authentication.TicketAuthenticatorWrapperType.TicketAuthenticatorWrapper;

import java.util.concurrent.ExecutionException;

import static org.junit.Assert.*;

/**
 * Test the staying logged in functionality of the session manager.
 * This is not inside the session manager tests because the session timeout is greatly reduced.
 *
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class StayLoggedInTest extends AuthenticationTest {

    /**
     * The session timeout for this test.
     */
    private static final long SESSION_TIMEOUT = 500;

    @BeforeClass
    public static void setUpClass() throws Exception {
        // set the session timeout and use the super method for initialization
        JPService.registerProperty(JPSessionTimeout.class, SESSION_TIMEOUT);
        AuthenticationTest.setUpClass();
    }

    /**
     * Test if the staying logged in feature of the session manager works as expected.
     *
     * @throws Exception if something does not work as expected
     */
    @Test(timeout = 20000)
    public void testStayingLoggedIn() throws Exception {
        // validate that the session timeout has been setup accordingly
        assertEquals("Session timeout has not been initialized correctly", SESSION_TIMEOUT, (long) JPService.getProperty(JPSessionTimeout.class).getValue());

        // create a new session manager
        SessionManager sessionManager = new SessionManager();
        // login with stay logged in activated
        sessionManager.loginUser(MockClientStore.ADMIN_ID, MockClientStore.ADMIN_PASSWORD, true);
        // wait longer than the session timeout
        Thread.sleep(SESSION_TIMEOUT + AuthenticationServerHandler.MAX_TIME_DIFF_SERVER_CLIENT + 200);
        // perform a request
        TicketAuthenticatorWrapper wrapper = sessionManager.initializeServiceServerRequest();
        CachedAuthenticationRemote.getRemote().validateClientServerTicket(wrapper).get();

        // login as a different user without staying logged in
        sessionManager.loginUser(MockClientStore.USER_ID, MockClientStore.USER_PASSWORD, false);
        // wait longer than the session timeout
        Thread.sleep(SESSION_TIMEOUT + AuthenticationServerHandler.MAX_TIME_DIFF_SERVER_CLIENT + 200);
        // perform a request
        wrapper = sessionManager.initializeServiceServerRequest();
        try {
            ExceptionPrinter.setBeQuit(true);
            CachedAuthenticationRemote.getRemote().validateClientServerTicket(wrapper).get();
            fail("No exception thrown even though the session should have timed out");
        } catch (ExecutionException ex) {
            assertTrue("Task did not fail because of session expired exception", ExceptionProcessor.getInitialCause(ex) instanceof SessionExpiredException);
        } finally {
            ExceptionPrinter.setBeQuit(false);
        }

        // shutdown the session manager
        sessionManager.shutdown();
    }
}
