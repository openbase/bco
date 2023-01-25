package org.openbase.bco.authentication.test

import io.kotest.matchers.string.shouldContain
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import org.openbase.bco.authentication.core.AuthenticationController
import org.openbase.bco.authentication.lib.AuthenticationServerHandler
import org.openbase.bco.authentication.lib.CachedAuthenticationRemote
import org.openbase.bco.authentication.lib.SessionManager
import org.openbase.bco.authentication.lib.jp.JPSessionTimeout
import org.openbase.bco.authentication.mock.MockClientStore
import org.openbase.bco.authentication.mock.MockCredentialStore
import org.openbase.jps.core.JPService
import org.openbase.jul.communication.mqtt.test.MqttIntegrationTest
import org.openbase.jul.exception.initialCauseMessage
import org.openbase.jul.exception.printer.ExceptionPrinter
import java.util.concurrent.ExecutionException

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
 */ /**
 * Test the staying logged in functionality of the session manager.
 * This is not inside the session manager tests because the session timeout is greatly reduced.
 *
 * @author [Tamino Huxohl](mailto:pleminoq@openbase.org)
 */
class StayLoggedInTest : MqttIntegrationTest() {
    override fun setupCustomProperties() {
        // set the session timeout and use the super method for initialization
        JPService.registerProperty(JPSessionTimeout::class.java, SESSION_TIMEOUT)
    }

    /**
     * Test if the staying logged in feature of the session manager works as expected.
     *
     * @throws Exception if something does not work as expected
     */
    @Test
    @Timeout(30)
    @Throws(Exception::class)
    fun testStayingLoggedIn() {
        CachedAuthenticationRemote.prepare()
        val authenticationController =
            AuthenticationController(MockCredentialStore.getInstance(), AuthenticationTest.serviceServerSecretKey)
        authenticationController.init()
        authenticationController.activate()
        authenticationController.waitForActivation()

        // validate that the session timeout has been setup accordingly
        Assertions.assertEquals(
            SESSION_TIMEOUT, JPService.getProperty(
                JPSessionTimeout::class.java
            ).value as Long, "Session timeout has not been initialized correctly"
        )

        // create a new session manager
        val sessionManager = SessionManager()
        // login with stay logged in activated
        sessionManager.loginUser(MockClientStore.ADMIN_ID, MockClientStore.ADMIN_PASSWORD, true)
        // wait longer than the session timeout
        Thread.sleep(SESSION_TIMEOUT + AuthenticationServerHandler.MAX_TIME_DIFF_SERVER_CLIENT)
        // perform a request
        var wrapper = sessionManager.initializeServiceServerRequest()
        CachedAuthenticationRemote.getRemote().validateClientServerTicket(wrapper).get()

        // login as a different user without staying logged in
        sessionManager.loginUser(MockClientStore.USER_ID, MockClientStore.USER_PASSWORD, false)
        // wait longer than the session timeout
        Thread.sleep(SESSION_TIMEOUT + AuthenticationServerHandler.MAX_TIME_DIFF_SERVER_CLIENT)
        // perform a request
        wrapper = sessionManager.initializeServiceServerRequest()
        try {
            ExceptionPrinter.setBeQuit(true)
            CachedAuthenticationRemote.getRemote().validateClientServerTicket(wrapper).get()
            Assertions.fail<Any>("No exception thrown even though the session should have timed out")
        } catch (ex: ExecutionException) {
            ex.initialCauseMessage shouldContain "SessionExpired"
        } finally {
            ExceptionPrinter.setBeQuit(false)
        }

        // shutdown the session manager
        sessionManager.shutdown()
    }

    companion object {
        /**
         * The session timeout for this test.
         */
        private const val SESSION_TIMEOUT: Long = 500
    }
}
