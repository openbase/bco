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

import com.google.protobuf.ByteString;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openbase.bco.authentication.lib.AuthenticationClientHandler;
import org.openbase.bco.authentication.lib.AuthenticationClientHandler.TicketWrapperSessionKeyPair;
import org.openbase.bco.authentication.lib.CachedAuthenticationRemote;
import org.openbase.bco.authentication.lib.EncryptionHelper;
import org.openbase.bco.authentication.mock.MockClientStore;
import org.openbase.bco.authentication.mock.MockCredentialStore;
import org.openbase.jul.communication.exception.RPCException;
import org.openbase.jul.communication.exception.RPCResolvedException;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.NotSupportedException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.exception.printer.LogLevel;
import org.openbase.type.domotic.authentication.AuthenticatedValueType.AuthenticatedValue;
import org.openbase.type.domotic.authentication.AuthenticatorType;
import org.openbase.type.domotic.authentication.LoginCredentialsChangeType.LoginCredentialsChange;
import org.openbase.type.domotic.authentication.LoginCredentialsType.LoginCredentials;
import org.openbase.type.domotic.authentication.TicketAuthenticatorWrapperType.TicketAuthenticatorWrapper;
import org.openbase.type.domotic.authentication.TicketSessionKeyWrapperType.TicketSessionKeyWrapper;
import org.openbase.type.domotic.authentication.UserClientPairType.UserClientPair;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutionException;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author <a href="mailto:thuxohl@techfak.uni-bielefeld.de">Tamino Huxohl</a>
 */
public class AuthenticatorControllerTest extends AuthenticationTest {

    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(AuthenticatorControllerTest.class);

    public AuthenticatorControllerTest() {
    }

    /**
     * Test of communication between ClientRemote and AuthenticatorController.
     *
     * @throws java.lang.Exception
     */
    @Test(timeout = 5000)
    public void testCommunication() throws Exception {
        System.out.println("testCommunication");

        final UserClientPair userClientPair = UserClientPair.newBuilder().setUserId(MockCredentialStore.USER_ID).build();
        final LoginCredentials loginCredentials = MockCredentialStore.getInstance().getCredentials(MockCredentialStore.USER_ID);

        // handle KDC request on server side
        TicketSessionKeyWrapper ticketSessionKeyWrapper = CachedAuthenticationRemote.getRemote().requestTicketGrantingTicket(userClientPair).get();

        // handle KDC response on client side
        TicketWrapperSessionKeyPair ticketWrapperSessionKeyPair = AuthenticationClientHandler.handleKeyDistributionCenterResponse(userClientPair, loginCredentials, null, ticketSessionKeyWrapper);

        // handle TGS request on server side
        ticketSessionKeyWrapper = CachedAuthenticationRemote.getRemote().requestClientServerTicket(ticketWrapperSessionKeyPair.getTicketAuthenticatorWrapper()).get();

        // handle TGS response on client side
        ticketWrapperSessionKeyPair = AuthenticationClientHandler.handleTicketGrantingServiceResponse(userClientPair, ticketWrapperSessionKeyPair.getSessionKey(), ticketSessionKeyWrapper);

        // init SS request on client side
        TicketAuthenticatorWrapper clientTicketAuthenticatorWrapper = AuthenticationClientHandler.initServiceServerRequest(ticketWrapperSessionKeyPair.getSessionKey(), ticketWrapperSessionKeyPair.getTicketAuthenticatorWrapper());

        // handle SS request on server side
        TicketAuthenticatorWrapper serverTicketAuthenticatorWrapper = CachedAuthenticationRemote.getRemote().validateClientServerTicket(clientTicketAuthenticatorWrapper).get();

        // handle SS response on client side
        AuthenticationClientHandler.handleServiceServerResponse(ticketWrapperSessionKeyPair.getSessionKey(), clientTicketAuthenticatorWrapper, serverTicketAuthenticatorWrapper);
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

            UserClientPair nonExistentUserId = UserClientPair.newBuilder().setUserId("12abc-15123").build();

            CachedAuthenticationRemote.getRemote().requestTicketGrantingTicket(nonExistentUserId).get();
        } finally {
            ExceptionPrinter.setBeQuit(Boolean.FALSE);
        }
    }

    /**
     * Test if the correct exception is thrown if the wrong password for a user is used.
     *
     * @throws Exception
     */
    @Test(timeout = 5000, expected = CouldNotPerformException.class)
    public void testAuthenticationWithIncorrectPassword() throws Exception {
        System.out.println("testAuthenticationWithIncorrectPassword");

        final UserClientPair userClientPair = UserClientPair.newBuilder().setUserId(MockCredentialStore.USER_ID).build();
        final LoginCredentials wrongLoginCredentials = LoginCredentials.newBuilder().setSymmetric(true).setCredentials(ByteString.copyFrom(EncryptionHelper.hash("wrong_password"))).build();

        TicketSessionKeyWrapper ticketSessionKeyWrapper = CachedAuthenticationRemote.getRemote().requestTicketGrantingTicket(userClientPair).get();
        AuthenticationClientHandler.handleKeyDistributionCenterResponse(userClientPair, wrongLoginCredentials, null, ticketSessionKeyWrapper);
    }

    @Test(timeout = 5000)
    public void testChangeCredentials() throws Exception {
        System.out.println("testChangeCredentials");

        final UserClientPair userClientPair = UserClientPair.newBuilder().setUserId(MockCredentialStore.USER_ID).build();
        final LoginCredentials currentCredentials = MockCredentialStore.getInstance().getCredentials(MockCredentialStore.USER_ID);
        final LoginCredentials newCredentials = currentCredentials.toBuilder().setCredentials(ByteString.copyFrom(EncryptionHelper.hash("newPassword"))).build();

        // handle KDC request on server side
        TicketSessionKeyWrapper ticketSessionKeyWrapper = CachedAuthenticationRemote.getRemote().requestTicketGrantingTicket(userClientPair).get();

        // handle KDC response on client side
        TicketWrapperSessionKeyPair ticketWrapperSessionKeyPair = AuthenticationClientHandler.handleKeyDistributionCenterResponse(userClientPair, currentCredentials, null, ticketSessionKeyWrapper);

        // handle TGS request on server side
        ticketSessionKeyWrapper = CachedAuthenticationRemote.getRemote().requestClientServerTicket(ticketWrapperSessionKeyPair.getTicketAuthenticatorWrapper()).get();

        // handle TGS response on client side
        ticketWrapperSessionKeyPair = AuthenticationClientHandler.handleTicketGrantingServiceResponse(userClientPair, ticketWrapperSessionKeyPair.getSessionKey(), ticketSessionKeyWrapper);

        // init SS request on client side
        TicketAuthenticatorWrapper clientTicketAuthenticatorWrapper = AuthenticationClientHandler.initServiceServerRequest(ticketWrapperSessionKeyPair.getSessionKey(), ticketWrapperSessionKeyPair.getTicketAuthenticatorWrapper());

        LoginCredentialsChange loginCredentialsChange = LoginCredentialsChange.newBuilder()
                .setId(MockClientStore.USER_ID)
                .setOldCredentials(currentCredentials.getCredentials())
                .setNewCredentials(newCredentials.getCredentials())
                .setSymmetric(newCredentials.getSymmetric())
                .build();

        AuthenticatedValue authenticatedValue = AuthenticatedValue.newBuilder().setValue(EncryptionHelper.encryptSymmetric(loginCredentialsChange, ticketWrapperSessionKeyPair.getSessionKey()))
                .setTicketAuthenticatorWrapper(clientTicketAuthenticatorWrapper).build();
        CachedAuthenticationRemote.getRemote().changeCredentials(authenticatedValue).get();
    }

    /**
     * Tests the feature of changing passwords for other users.
     * Only admins should be allowed to do this.
     *
     * @throws Exception
     */
    @Test(timeout = 5000)
    public void testChangeOthersCredentials() throws Exception {
        System.out.println("testChangeOthersCredentials");

        // Trying to change the admin's password with a normal user. This should not work.
        UserClientPair userClientPair = UserClientPair.newBuilder().setUserId(MockCredentialStore.USER_ID).build();
        final LoginCredentials userCredentials = MockCredentialStore.getInstance().getCredentials(MockCredentialStore.USER_ID);

        // handle KDC request on server side
        TicketSessionKeyWrapper ticketSessionKeyWrapper = CachedAuthenticationRemote.getRemote().requestTicketGrantingTicket(userClientPair).get();

        // handle KDC response on client side
        TicketWrapperSessionKeyPair ticketWrapperSessionKeyPair = AuthenticationClientHandler.handleKeyDistributionCenterResponse(userClientPair, userCredentials, null, ticketSessionKeyWrapper);

        // handle TGS request on server side
        ticketSessionKeyWrapper = CachedAuthenticationRemote.getRemote().requestClientServerTicket(ticketWrapperSessionKeyPair.getTicketAuthenticatorWrapper()).get();

        // handle TGS response on client side
        ticketWrapperSessionKeyPair = AuthenticationClientHandler.handleTicketGrantingServiceResponse(userClientPair, ticketWrapperSessionKeyPair.getSessionKey(), ticketSessionKeyWrapper);

        // init SS request on client side
        TicketAuthenticatorWrapper clientTicketAuthenticatorWrapper = AuthenticationClientHandler.initServiceServerRequest(ticketWrapperSessionKeyPair);

        LoginCredentialsChange loginCredentialsChange = LoginCredentialsChange.newBuilder()
                .setId(MockClientStore.ADMIN_ID)
                .setOldCredentials(ByteString.copyFrom(MockCredentialStore.ADMIN_PASSWORD_HASH))
                .setNewCredentials(ByteString.copyFrom(MockCredentialStore.USER_PASSWORD_HASH))
                .setSymmetric(true)
                .build();
        AuthenticatedValue authenticatedValue = AuthenticatedValue.newBuilder()
                .setTicketAuthenticatorWrapper(clientTicketAuthenticatorWrapper)
                .setValue(EncryptionHelper.encryptSymmetric(loginCredentialsChange, ticketWrapperSessionKeyPair.getSessionKey()))
                .build();
        try {
            ExceptionPrinter.setBeQuit(Boolean.TRUE);
            CachedAuthenticationRemote.getRemote().changeCredentials(authenticatedValue).get();
            fail("A user should not be able to change the password of another user.");
        } catch (CouldNotPerformException | ExecutionException ex) {
        } finally {
            ExceptionPrinter.setBeQuit(Boolean.FALSE);
        }

        // Trying to change the user's password with an admin account. This should work.
        userClientPair = UserClientPair.newBuilder().setUserId(MockCredentialStore.ADMIN_ID).build();
        final LoginCredentials adminCredentials = LoginCredentials.newBuilder().setSymmetric(true).setCredentials(ByteString.copyFrom(MockCredentialStore.ADMIN_PASSWORD_HASH)).build();

        // handle KDC request on server side
        ticketSessionKeyWrapper = CachedAuthenticationRemote.getRemote().requestTicketGrantingTicket(userClientPair).get();

        // handle KDC response on client side
        ticketWrapperSessionKeyPair = AuthenticationClientHandler.handleKeyDistributionCenterResponse(userClientPair, adminCredentials, null, ticketSessionKeyWrapper);

        // handle TGS request on server side
        ticketSessionKeyWrapper = CachedAuthenticationRemote.getRemote().requestClientServerTicket(ticketWrapperSessionKeyPair.getTicketAuthenticatorWrapper()).get();

        // handle TGS response on client side
        ticketWrapperSessionKeyPair = AuthenticationClientHandler.handleTicketGrantingServiceResponse(userClientPair, ticketWrapperSessionKeyPair.getSessionKey(), ticketSessionKeyWrapper);

        // init SS request on client side
        clientTicketAuthenticatorWrapper = AuthenticationClientHandler.initServiceServerRequest(ticketWrapperSessionKeyPair);

        loginCredentialsChange = LoginCredentialsChange.newBuilder()
                .setId(MockClientStore.USER_ID)
                .setOldCredentials(userCredentials.getCredentials())
                .setNewCredentials(ByteString.copyFrom(EncryptionHelper.hash("newClientPassword")))
                .setSymmetric(true)
                .build();
        authenticatedValue = AuthenticatedValue.newBuilder()
                .setTicketAuthenticatorWrapper(clientTicketAuthenticatorWrapper)
                .setValue(EncryptionHelper.encryptSymmetric(loginCredentialsChange, ticketWrapperSessionKeyPair.getSessionKey()))
                .build();

        CachedAuthenticationRemote.getRemote().changeCredentials(authenticatedValue).get();
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

        final UserClientPair userClientPair = UserClientPair.newBuilder().setUserId(MockCredentialStore.USER_ID).build();
        final LoginCredentials loginCredentials = LoginCredentials.newBuilder().setCredentials(ByteString.copyFrom(MockCredentialStore.USER_PASSWORD_HASH)).setSymmetric(true).build();

        // handle KDC request on server side
        TicketSessionKeyWrapper ticketSessionKeyWrapper = CachedAuthenticationRemote.getRemote().requestTicketGrantingTicket(userClientPair).get();

        // handle KDC response on client side
        TicketWrapperSessionKeyPair ticketWrapperSessionKeyPair = AuthenticationClientHandler.handleKeyDistributionCenterResponse(userClientPair, loginCredentials, null, ticketSessionKeyWrapper);

        // handle TGS request on server side
        ticketSessionKeyWrapper = CachedAuthenticationRemote.getRemote().requestClientServerTicket(ticketWrapperSessionKeyPair.getTicketAuthenticatorWrapper()).get();

        // handle TGS response on client side
        ticketWrapperSessionKeyPair = AuthenticationClientHandler.handleTicketGrantingServiceResponse(userClientPair, ticketWrapperSessionKeyPair.getSessionKey(), ticketSessionKeyWrapper);

        // init SS request on client side
        TicketAuthenticatorWrapper request1 = AuthenticationClientHandler.initServiceServerRequest(ticketWrapperSessionKeyPair.getSessionKey(), ticketWrapperSessionKeyPair.getTicketAuthenticatorWrapper());

        Thread.sleep(100);

        // init SS request on client side
        TicketAuthenticatorWrapper request2 = AuthenticationClientHandler.initServiceServerRequest(ticketWrapperSessionKeyPair);

        AuthenticatorType.Authenticator request2auth = EncryptionHelper.decryptSymmetric(request2.getAuthenticator(), ticketWrapperSessionKeyPair.getSessionKey(), AuthenticatorType.Authenticator.class);
        AuthenticatorType.Authenticator request1auth = EncryptionHelper.decryptSymmetric(request1.getAuthenticator(), ticketWrapperSessionKeyPair.getSessionKey(), AuthenticatorType.Authenticator.class);

        System.err.println(request2auth.getTimestamp().getTime());
        System.err.println(request1auth.getTimestamp().getTime());

        // handle SS request on server side
        TicketAuthenticatorWrapper response2 = CachedAuthenticationRemote.getRemote().validateClientServerTicket(request2).get();

        // handle SS response on client side
        AuthenticationClientHandler.handleServiceServerResponse(ticketWrapperSessionKeyPair.getSessionKey(), request2, response2);

        // handle SS request on server side
        TicketAuthenticatorWrapper response1 = CachedAuthenticationRemote.getRemote().validateClientServerTicket(request1).get();

        // handle SS response on client side
        AuthenticationClientHandler.handleServiceServerResponse(ticketWrapperSessionKeyPair.getSessionKey(), request1, response1);
    }

    @Test(timeout = 5000)
    public void testLoginCombinations() throws Exception {
        final UserClientPair clientSymmetricUserSymmetric = UserClientPair.newBuilder()
                .setClientId(MockCredentialStore.CLIENT_SYMMETRIC_ID)
                .setUserId(MockCredentialStore.USER_SYMMETRIC_ID)
                .build();
        CachedAuthenticationRemote.getRemote().requestTicketGrantingTicket(clientSymmetricUserSymmetric).get();

        final UserClientPair clientAsymmetricUserSymmetric = UserClientPair.newBuilder()
                .setClientId(MockCredentialStore.CLIENT_ASYMMETRIC_ID)
                .setUserId(MockCredentialStore.USER_SYMMETRIC_ID)
                .build();
        CachedAuthenticationRemote.getRemote().requestTicketGrantingTicket(clientAsymmetricUserSymmetric).get();

        final UserClientPair clientSymmetricUserAsymmetric = UserClientPair.newBuilder()
                .setClientId(MockCredentialStore.CLIENT_SYMMETRIC_ID)
                .setUserId(MockCredentialStore.USER_ASYMMETRIC_ID)
                .build();
        CachedAuthenticationRemote.getRemote().requestTicketGrantingTicket(clientSymmetricUserAsymmetric).get();

        final UserClientPair clientAsymmetricUserAsymmetric = UserClientPair.newBuilder()
                .setClientId(MockCredentialStore.CLIENT_ASYMMETRIC_ID)
                .setUserId(MockCredentialStore.USER_ASYMMETRIC_ID)
                .build();
        try {
            ExceptionPrinter.setBeQuit(true);
            CachedAuthenticationRemote.getRemote().requestTicketGrantingTicket(clientAsymmetricUserAsymmetric).get();
            fail("No exception throw even when authentication method is not supported.");
        } catch (ExecutionException ex) {
            //TODO: wasnt this done automatically before?
            final Exception exception = RPCResolvedException.resolveRPCException((RPCException) ex.getCause());
            assertTrue(exception.getMessage().contains("NotSupportedException"));
        } finally {
            ExceptionPrinter.setBeQuit(false);
        }
    }
}
