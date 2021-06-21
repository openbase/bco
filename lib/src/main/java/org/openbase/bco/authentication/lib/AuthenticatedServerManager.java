package org.openbase.bco.authentication.lib;

/*-
 * #%L
 * BCO Authentication Library
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

import org.openbase.bco.authentication.lib.AuthenticationClientHandler.TicketWrapperSessionKeyPair;
import org.openbase.bco.authentication.lib.jp.JPAuthentication;
import org.openbase.bco.authentication.lib.jp.JPCredentialsDirectory;
import org.openbase.bco.authentication.lib.jp.JPSessionTimeout;
import org.openbase.jps.core.JPService;
import org.openbase.jps.exception.JPNotAvailableException;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.exception.RejectedException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.exception.printer.LogLevel;
import org.openbase.type.domotic.authentication.AuthenticatedValueType.AuthenticatedValue;
import org.openbase.type.domotic.authentication.AuthenticationTokenType.AuthenticationToken;
import org.openbase.type.domotic.authentication.AuthenticatorType.Authenticator;
import org.openbase.type.domotic.authentication.AuthorizationTokenType.AuthorizationToken;
import org.openbase.type.domotic.authentication.LoginCredentialsType.LoginCredentials;
import org.openbase.type.domotic.authentication.TicketAuthenticatorWrapperType.TicketAuthenticatorWrapper;
import org.openbase.type.domotic.authentication.TicketSessionKeyWrapperType.TicketSessionKeyWrapper;
import org.openbase.type.domotic.authentication.TicketType.Ticket;
import org.openbase.type.domotic.authentication.UserClientPairType.UserClientPair;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.concurrent.ExecutionException;

/**
 * This class represents a Service Server and provides methods to validate Kerberos client-server-tickets.
 *
 * @author <a href="mailto:cromankiewicz@techfak.uni-bielefeld.de">Constantin Romankiewicz</a>
 */
public class AuthenticatedServerManager {

    public static final String SERVICE_SERVER_PRIVATE_KEY_FILENAME = "service_server_private_key";
    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(AuthenticatedServerManager.class);
    private byte[] serviceServerSecretKey;
    private static AuthenticatedServerManager instance;
    private TicketAuthenticatorWrapper ticketAuthenticatorWrapper;
    private byte[] sessionKey;
    private final long ticketValidityTime;

    private AuthenticatedServerManager() throws CouldNotPerformException {
        try {
            this.ticketValidityTime = JPService.getProperty(JPSessionTimeout.class).getValue();
            if (JPService.getProperty(JPAuthentication.class).getValue()) {
                this.login();
                this.requestServiceServerSecretKey();
            }
        } catch (JPNotAvailableException ex) {
            throw new CouldNotPerformException("Could not check JPProperty", ex);
        }
    }

    public static synchronized AuthenticatedServerManager getInstance() throws CouldNotPerformException {
        if (instance == null) {
            instance = new AuthenticatedServerManager();
        }

        return instance;
    }

    public static synchronized void shutdown() {
        instance = null;
    }

    /**
     * Verifies the ticket from a given AuthenticatedValue and returns authentication base data
     * containing values according to the data in the authenticated value.
     *
     * @param authenticatedValue AuthenticationBaseData holding information about the authorized user and send tokens
     *
     * @return an authentication base data object containing the user id, the session key and an updated ticket and tokens
     *
     * @throws NotAvailableException    if the authenticated value does not contain a ticket
     * @throws CouldNotPerformException on de-/encryption errors
     * @throws RejectedException        If the ticket is not valid.
     */
    public AuthenticationBaseData verifyClientServerTicket(final AuthenticatedValue authenticatedValue) throws CouldNotPerformException, RejectedException {
        if (!authenticatedValue.hasTicketAuthenticatorWrapper()) {
            throw new NotAvailableException("TicketAuthenticatorWrapper");
        }

        // create authentication base data from ticket
        final AuthenticationBaseData authenticationBaseData = verifyClientServerTicket(authenticatedValue.getTicketAuthenticatorWrapper());

        // if authenticated value has this token encrypt it and add it to the return data
        if (authenticatedValue.hasAuthorizationToken()) {
            String tokenString = EncryptionHelper.decryptSymmetric(authenticatedValue.getAuthorizationToken(), authenticationBaseData.getSessionKey(), String.class);
            AuthorizationToken decrypt = EncryptionHelper.decrypt(Base64.getDecoder().decode(tokenString), serviceServerSecretKey, AuthorizationToken.class, true);
            authenticationBaseData.setAuthorizationToken(decrypt);
        }

        // if authenticated value has this token encrypt it and add it to the return data
        if (authenticatedValue.hasAuthenticationToken()) {
            String tokenString = EncryptionHelper.decryptSymmetric(authenticatedValue.getAuthenticationToken(), authenticationBaseData.getSessionKey(), String.class);
            AuthenticationToken decrypt = EncryptionHelper.decrypt(Base64.getDecoder().decode(tokenString), serviceServerSecretKey, AuthenticationToken.class, true);
            authenticationBaseData.setAuthenticationToken(decrypt);
        }

        return authenticationBaseData;
    }

    /**
     * Verifies the ticket from a given TicketAuthenticatorWrapper and returns authentication base data
     * containing values according to the authentication.
     *
     * @param ticketAuthenticatorWrapper TicketAuthenticatorWrapper holding information about the ticket's validity and the client ID.
     *
     * @return an authentication base data object containing the user id, the session key and an updated ticket
     *
     * @throws CouldNotPerformException on de-/encryption errors
     * @throws RejectedException        If the ticket is not valid.
     */
    public AuthenticationBaseData verifyClientServerTicket(final TicketAuthenticatorWrapper ticketAuthenticatorWrapper) throws CouldNotPerformException, RejectedException {
        try {
            // decrypt ticket and authenticator
            Ticket clientServerTicket = EncryptionHelper.decryptSymmetric(ticketAuthenticatorWrapper.getTicket(), serviceServerSecretKey, Ticket.class);
            Authenticator authenticator = EncryptionHelper.decryptSymmetric(ticketAuthenticatorWrapper.getAuthenticator(), clientServerTicket.getSessionKeyBytes().toByteArray(), Authenticator.class);

            // compare clientIDs and timestamp to period
            AuthenticationServerHandler.validateTicket(clientServerTicket, authenticator);

            // update period and session key
            clientServerTicket = clientServerTicket.toBuilder().setValidityPeriod(AuthenticationServerHandler.getValidityInterval(ticketValidityTime)).build();
            Authenticator.Builder authenticatorBuilder = authenticator.toBuilder();
            authenticatorBuilder.setTimestamp(authenticator.getTimestamp().toBuilder().setTime(authenticator.getTimestamp().getTime() + 1));

            // update TicketAuthenticatorWrapper
            TicketAuthenticatorWrapper.Builder response = ticketAuthenticatorWrapper.toBuilder();
            response.setTicket(EncryptionHelper.encryptSymmetric(clientServerTicket, serviceServerSecretKey));
            response.setAuthenticator(EncryptionHelper.encryptSymmetric(authenticatorBuilder.build(), clientServerTicket.getSessionKeyBytes().toByteArray()));

            return new AuthenticationBaseData(authenticator.getUserClientPair(), clientServerTicket.getSessionKeyBytes().toByteArray(), response.build());
        } catch (RejectedException ex) {
            throw ExceptionPrinter.printHistoryAndReturnThrowable(ex, LOGGER, LogLevel.ERROR);
        }
    }

    /**
     * Login method for the service server.
     *
     * @throws CouldNotPerformException If the login fails.
     */
    private void login() throws CouldNotPerformException {
        try {
            // Load private key from file.
            LoginCredentials loginCredentials;
            File privateKeyFile = new File(JPService.getProperty(JPCredentialsDirectory.class).getValue(), SERVICE_SERVER_PRIVATE_KEY_FILENAME);
            try (FileInputStream inputStream = new FileInputStream(privateKeyFile)) {
                loginCredentials = LoginCredentials.parseFrom(inputStream);
            }

            final UserClientPair userClientPair = UserClientPair.newBuilder().setClientId(CredentialStore.SERVICE_SERVER_ID).build();

            // request TGT
            TicketSessionKeyWrapper ticketSessionKeyWrapper = CachedAuthenticationRemote.getRemote().requestTicketGrantingTicket(userClientPair).get();

            // handle KDC response on client side
            TicketWrapperSessionKeyPair ticketWrapperSessionKeyPair = AuthenticationClientHandler.handleKeyDistributionCenterResponse(userClientPair, null, loginCredentials, ticketSessionKeyWrapper);

            // request CST
            ticketSessionKeyWrapper = CachedAuthenticationRemote.getRemote().requestClientServerTicket(ticketWrapperSessionKeyPair.getTicketAuthenticatorWrapper()).get();

            // handle TGS response on client side
            ticketWrapperSessionKeyPair = AuthenticationClientHandler.handleTicketGrantingServiceResponse(userClientPair, ticketWrapperSessionKeyPair.getSessionKey(), ticketSessionKeyWrapper);
            this.ticketAuthenticatorWrapper = ticketWrapperSessionKeyPair.getTicketAuthenticatorWrapper();
            this.sessionKey = ticketWrapperSessionKeyPair.getSessionKey();
        } catch (ExecutionException | JPNotAvailableException | CouldNotPerformException | IOException | InterruptedException ex) {
            ExceptionPrinter.printHistory(ex, LOGGER, LogLevel.ERROR);
            throw new CouldNotPerformException("Login failed!", ex);
        }
    }

    /**
     * Requests the service server secret key from the AuthenticationController.
     * This can only be performed after being {@link #login()} has been called.
     *
     * @throws CouldNotPerformException if the key cannot be requested
     */
    private void requestServiceServerSecretKey() throws CouldNotPerformException {
        try {
            // init ticket for the request
            ticketAuthenticatorWrapper = AuthenticationClientHandler.initServiceServerRequest(sessionKey, ticketAuthenticatorWrapper);
            // perform the request
            final AuthenticatedValue value = CachedAuthenticationRemote.getRemote().requestServiceServerSecretKey(ticketAuthenticatorWrapper).get();
            // validate the response
            ticketAuthenticatorWrapper = AuthenticationClientHandler.handleServiceServerResponse(sessionKey,
                    ticketAuthenticatorWrapper, value.getTicketAuthenticatorWrapper());

            // decrypt and save service server secret key
            serviceServerSecretKey = EncryptionHelper.decryptSymmetric(value.getValue(), sessionKey, byte[].class);
        } catch (ExecutionException | CouldNotPerformException ex) {
            ExceptionPrinter.printHistory(ex, LOGGER, LogLevel.ERROR);
            throw new CouldNotPerformException("Could not get the service server secret key.", ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new CouldNotPerformException("Interrupted while requesting key", ex);
        }
    }

    /**
     * Get the service server secret key.
     *
     * @return the service server secret key
     */
    public byte[] getServiceServerSecretKey() {
        return serviceServerSecretKey;
    }
}
