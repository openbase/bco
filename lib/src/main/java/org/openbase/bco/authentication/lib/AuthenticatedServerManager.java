package org.openbase.bco.authentication.lib;

/*-
 * #%L
 * BCO Authentication Library
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
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutionException;
import javax.crypto.BadPaddingException;
import static org.openbase.bco.authentication.lib.AuthenticationServerHandler.getValidityInterval;
import org.openbase.bco.authentication.lib.jp.JPCredentialsDirectory;
import org.openbase.bco.authentication.lib.jp.JPEnableAuthentication;
import org.openbase.jps.core.JPService;
import org.openbase.jps.exception.JPNotAvailableException;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.RejectedException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.exception.printer.LogLevel;
import org.slf4j.LoggerFactory;
import rst.domotic.authentication.AuthenticatedValueType.AuthenticatedValue;
import rst.domotic.authentication.AuthenticatorType.Authenticator;
import rst.domotic.authentication.TicketAuthenticatorWrapperType.TicketAuthenticatorWrapper;
import rst.domotic.authentication.TicketSessionKeyWrapperType.TicketSessionKeyWrapper;
import rst.domotic.authentication.TicketType.Ticket;

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

    private AuthenticatedServerManager() throws CouldNotPerformException, InterruptedException {
        try {
            if (JPService.getProperty(JPEnableAuthentication.class).getValue()) {
                this.login();
                this.requestServiceServerSecretKey();
            }
        } catch (JPNotAvailableException ex) {
            throw new CouldNotPerformException("Could not check JPEnableAuthentication property", ex);
        }
    }

    public static synchronized AuthenticatedServerManager getInstance() throws CouldNotPerformException, InterruptedException {
        if (instance == null) {
            instance = new AuthenticatedServerManager();
        }

        return instance;
    }

    public static synchronized void shutdown() {
        instance = null;
    }

    /**
     * Validates the ticket from a given TicketAuthenticatorWrapper and returns the client ID,
     * if the ticket is valid.
     *
     * @param wrapper TicketAuthenticatorWrapper holding information about the ticket's validity and the client ID.
     * @return A wrapper containing the client id, the updated ticket for the response and the session key.
     * @throws IOException For I/O errors during the decryption.
     * @throws RejectedException If the ticket is not valid.
     */
    public TicketEvaluationWrapper evaluateClientServerTicket(final TicketAuthenticatorWrapper wrapper) throws IOException, RejectedException {
        try {
            // decrypt ticket and authenticator
            Ticket clientServerTicket = EncryptionHelper.decryptSymmetric(wrapper.getTicket(), serviceServerSecretKey, Ticket.class);
            Authenticator authenticator = EncryptionHelper.decryptSymmetric(wrapper.getAuthenticator(), clientServerTicket.getSessionKeyBytes().toByteArray(), Authenticator.class);

            // compare clientIDs and timestamp to period
            AuthenticationServerHandler.validateTicket(clientServerTicket, authenticator);

            // update period and session key
            clientServerTicket = clientServerTicket.toBuilder().setValidityPeriod(getValidityInterval()).build();
            Authenticator.Builder authenticatorBuilder = authenticator.toBuilder();
            authenticatorBuilder.setTimestamp(authenticator.getTimestamp().toBuilder().setTime(authenticator.getTimestamp().getTime() + 1));

            // update TicketAuthenticatorWrapper
            TicketAuthenticatorWrapper.Builder response = wrapper.toBuilder();
            response.setTicket(EncryptionHelper.encryptSymmetric(clientServerTicket, serviceServerSecretKey));
            response.setAuthenticator(EncryptionHelper.encryptSymmetric(authenticatorBuilder.build(), clientServerTicket.getSessionKeyBytes().toByteArray()));

            return new TicketEvaluationWrapper(authenticator.getClientId(), clientServerTicket.getSessionKeyBytes().toByteArray(), response.build());
        } catch (IOException ex) {
            throw ExceptionPrinter.printHistoryAndReturnThrowable(ex, LOGGER, LogLevel.ERROR);
        } catch (BadPaddingException ex) {
            ExceptionPrinter.printHistory(ex, LOGGER, LogLevel.ERROR);
            throw new RejectedException(ex);
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
            byte[] key;
            File privateKeyFile = new File(JPService.getProperty(JPCredentialsDirectory.class).getValue(), SERVICE_SERVER_PRIVATE_KEY_FILENAME);
            try (FileInputStream inputStream = new FileInputStream(privateKeyFile)) {
                key = new byte[(int) privateKeyFile.length()];
                inputStream.read(key);
            }

            String id = "@" + CredentialStore.SERVICE_SERVER_ID;

            // request TGT
            TicketSessionKeyWrapper ticketSessionKeyWrapper = CachedAuthenticationRemote.getRemote().requestTicketGrantingTicket(id).get();

            // handle KDC response on client side
            List<Object> list = AuthenticationClientHandler.handleKeyDistributionCenterResponse(id, key, false, ticketSessionKeyWrapper);
            TicketAuthenticatorWrapper taw = (TicketAuthenticatorWrapper) list.get(0); // save at somewhere temporarily
            byte[] ticketGrantingServiceSessionKey = (byte[]) list.get(1); // save TGS session key somewhere on client side

            // request CST
            ticketSessionKeyWrapper = CachedAuthenticationRemote.getRemote().requestClientServerTicket(taw).get();

            // handle TGS response on client side
            list = AuthenticationClientHandler.handleTicketGrantingServiceResponse(id, ticketGrantingServiceSessionKey, ticketSessionKeyWrapper);
            this.ticketAuthenticatorWrapper = (TicketAuthenticatorWrapper) list.get(0); // save at somewhere temporarily
            this.sessionKey = (byte[]) list.get(1); // save SS session key somewhere on client side
        } catch (BadPaddingException ex) {
            throw new CouldNotPerformException("The local private key is wrong.");
        } catch (ExecutionException | JPNotAvailableException | CouldNotPerformException | IOException | InterruptedException ex) {
            ExceptionPrinter.printHistory(ex, LOGGER, LogLevel.ERROR);
            throw new CouldNotPerformException("Login failed!", ex);
        }
    }

    /**
     * Requests the service server secret key from the AuthenticationController.
     *
     * @throws CouldNotPerformException
     * @throws InterruptedException
     */
    private void requestServiceServerSecretKey() throws CouldNotPerformException, InterruptedException {
        try {
            ticketAuthenticatorWrapper = AuthenticationClientHandler.initServiceServerRequest(sessionKey, ticketAuthenticatorWrapper);
            AuthenticatedValue value = CachedAuthenticationRemote.getRemote().requestServiceServerSecretKey(ticketAuthenticatorWrapper).get();
            ticketAuthenticatorWrapper = AuthenticationClientHandler.handleServiceServerResponse(sessionKey, ticketAuthenticatorWrapper, value.getTicketAuthenticatorWrapper());

            serviceServerSecretKey = EncryptionHelper.decryptSymmetric(value.getValue(), sessionKey, byte[].class);
        } catch (ExecutionException | RejectedException | IOException | BadPaddingException ex) {
            ExceptionPrinter.printHistory(ex, LOGGER, LogLevel.ERROR);
            throw new CouldNotPerformException("Could not get the service server secret key.", ex);
        }
    }

    /**
     * Should only be used in tests and thus returns null when not in test mode.
     *
     * @return the internal service server secret key
     */
    public byte[] getServiceServerSecretKey() {
        if (JPService.testMode()) {
            return serviceServerSecretKey;
        }
        return null;
    }

    /**
     * Wrapper used to return all values which are needed by an authenticated server after verification.
     */
    public class TicketEvaluationWrapper {

        private final String userId;
        private final byte[] sessionKey;
        private final TicketAuthenticatorWrapper ticketAuthenticatorWrapper;

        /**
         * Create a new wrapper containing the updated ticket, the user id and the session key.
         *
         * @param userId The id of the user whose ticket was evaluated.
         * @param sessionKey The session key of the session of the user whose ticket was evaluated.
         * @param ticketAuthenticatorWrapper The updated ticket which should be send as a response to the client.
         */
        public TicketEvaluationWrapper(final String userId, final byte[] sessionKey, final TicketAuthenticatorWrapper ticketAuthenticatorWrapper) {
            this.userId = userId;
            this.sessionKey = sessionKey;
            this.ticketAuthenticatorWrapper = ticketAuthenticatorWrapper;
        }

        public String getUserId() {
            return userId;
        }

        public byte[] getSessionKey() {
            return sessionKey;
        }

        public TicketAuthenticatorWrapper getTicketAuthenticatorWrapper() {
            return ticketAuthenticatorWrapper;
        }
    }
}
