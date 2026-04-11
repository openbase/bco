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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.openbase.bco.authentication.core.AuthenticationController;
import org.openbase.bco.authentication.lib.*;
import org.openbase.type.domotic.authentication.AuthenticatedValueType.AuthenticatedValue;
import org.openbase.type.domotic.authentication.LoginCredentialsType.LoginCredentials;
import org.slf4j.LoggerFactory;
import org.openbase.type.domotic.authentication.TicketAuthenticatorWrapperType.TicketAuthenticatorWrapper;

/**
 *
 * @author <a href="mailto:thuxohl@techfak.uni-bielefeld.de">Tamino Huxohl</a>
 */
public class ServiceServerManagerTest extends AuthenticationTest {

    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(AuthenticatorControllerTest.class);

    public ServiceServerManagerTest() {
    }

    /**
     * Test if the service server can validate client server tickets.
     *
     * @throws java.lang.Exception
     */
    @Test
    @Timeout(15)
    public void testServiceServerManagerValidation() throws Exception {
        System.out.println("testServiceServerManagerValidation");

        // register a user from which a ticket can be validated
        String userId = "ServiceServerManagerUser";
        String password = "Security";
        LoginCredentials.Builder loginCredentials = LoginCredentials.newBuilder();
        loginCredentials.setSymmetric(true);
        loginCredentials.setId(userId);
        loginCredentials.setCredentials(EncryptionHelper.encryptSymmetric(EncryptionHelper.hash(password), EncryptionHelper.hash(AuthenticationController.getInitialPassword())));
        final AuthenticatedValue authenticatedValue = AuthenticatedValue.newBuilder().setValue(loginCredentials.build().toByteString()).build();
        CachedAuthenticationRemote.getRemote().register(authenticatedValue).get();


        SessionManager.getInstance().loginUser(userId, password, false);
        
        TicketAuthenticatorWrapper request = SessionManager.getInstance().initializeServiceServerRequest();
        TicketAuthenticatorWrapper response = AuthenticatedServerManager.getInstance().verifyClientServerTicket(request).getTicketAuthenticatorWrapper();

        AuthenticationClientHandler.handleServiceServerResponse(SessionManager.getInstance().getSessionKey(), request, response);
    }
}
