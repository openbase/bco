package org.openbase.bco.authentication.lib;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.StreamCorruptedException;
import java.util.List;
import java.util.concurrent.ExecutionException;
import javax.crypto.BadPaddingException;
import static org.openbase.bco.authentication.lib.AuthenticationServerHandler.getValidityInterval;
import org.openbase.bco.authentication.lib.jp.JPCredentialsDirectory;
import org.openbase.jps.core.JPService;
import org.openbase.jps.exception.JPNotAvailableException;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.FatalImplementationErrorException;
import org.openbase.jul.exception.NotAvailableException;
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
public class ServiceServerManager {
    public static final String SERVICE_SERVER_PRIVATE_KEY_FILENAME = "service_server_private_key";
    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(ServiceServerManager.class);
    private byte [] serviceServerSecretKey;
    private ServiceServerManager instance;
    private TicketAuthenticatorWrapper ticketAuthenticatorWrapper;
    private byte [] sessionKey;

    private ServiceServerManager() throws CouldNotPerformException, InterruptedException {
        this.login();
        this.requestServiceServerSecretKey();
    }

    public synchronized ServiceServerManager getInstance() throws CouldNotPerformException, InterruptedException {
        if (instance == null) {
            instance = new ServiceServerManager();
        }

        return instance;
    }

    /**
     * Validates the ticket from a given TicketAuthenticatorWrapper and returns the client ID,
     * if the ticket is valid.
     *
     * @param wrapper TicketAuthenticatorWrapper holding information about the ticket's validity and the client ID.
     * @return Client ID.
     * @throws IOException For I/O errors during the decryption.
     * @throws RejectedException If the ticket is not valid.
     */
    public String evaluateClientServerTicket(TicketAuthenticatorWrapper wrapper) throws IOException, RejectedException {
        try {
            // decrypt ticket and authenticator
            Ticket clientServerTicket = EncryptionHelper.decryptSymmetric(wrapper.getTicket(), serviceServerSecretKey, Ticket.class);
            Authenticator authenticator = EncryptionHelper.decryptSymmetric(wrapper.getAuthenticator(), clientServerTicket.getSessionKeyBytes().toByteArray(), Authenticator.class);

            // compare clientIDs and timestamp to period
            AuthenticationServerHandler.validateTicket(clientServerTicket, authenticator);

            // update period and session key
            Ticket.Builder cstb = clientServerTicket.toBuilder();
            cstb.setValidityPeriod(getValidityInterval());

            // update TicketAuthenticatorWrapper
            TicketAuthenticatorWrapper.Builder newWrapper = wrapper.toBuilder();
            newWrapper.setTicket(EncryptionHelper.encryptSymmetric(clientServerTicket, serviceServerSecretKey));

            wrapper = newWrapper.build();
            return authenticator.getClientId();
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
            File privateKeyFile = new File(JPService.getProperty(JPCredentialsDirectory.class).getValue(), SERVICE_SERVER_PRIVATE_KEY_FILENAME);
            byte[] key;
            try (FileInputStream inputStream = new FileInputStream(privateKeyFile)) {
                key = new byte[(int)privateKeyFile.length()];
                inputStream.read(key);
            }

            // request TGT
            TicketSessionKeyWrapper ticketSessionKeyWrapper = CachedAuthenticationRemote.getRemote().requestTicketGrantingTicket(CredentialStore.SERVICE_SERVER_ID).get();

            // handle KDC response on client side
            List<Object> list = AuthenticationClientHandler.handleKeyDistributionCenterResponse(CredentialStore.SERVICE_SERVER_ID, key, false, ticketSessionKeyWrapper);
            TicketAuthenticatorWrapper taw = (TicketAuthenticatorWrapper) list.get(0); // save at somewhere temporarily
            byte[] ticketGrantingServiceSessionKey = (byte[]) list.get(1); // save TGS session key somewhere on client side

            // request CST
            ticketSessionKeyWrapper = CachedAuthenticationRemote.getRemote().requestClientServerTicket(taw).get();

            // handle TGS response on client side
            list = AuthenticationClientHandler.handleTicketGrantingServiceResponse(CredentialStore.SERVICE_SERVER_ID, ticketGrantingServiceSessionKey, ticketSessionKeyWrapper);
            this.ticketAuthenticatorWrapper = (TicketAuthenticatorWrapper) list.get(0); // save at somewhere temporarily
            this.sessionKey = (byte[]) list.get(1); // save SS session key somewhere on client side
        } catch (BadPaddingException ex) {
            throw new CouldNotPerformException("The local private key is wrong.");
        } catch (ExecutionException ex) {
            Throwable cause = ex.getCause();

            if (cause instanceof NotAvailableException) {
                throw ExceptionPrinter.printHistoryAndReturnThrowable((NotAvailableException) cause, LOGGER, LogLevel.ERROR);
            }

            if (cause instanceof StreamCorruptedException) {
                ExceptionPrinter.printHistory(cause, LOGGER, LogLevel.ERROR);
                throw new CouldNotPerformException("Decryption failed. Please login again.", cause);
            }

            // RejectedException is thrown if the timestamp in the Authenticator does not fit to time period in TGT
            // or, if the clientID in Authenticator does not match the clientID in the TGT.
            // This should never occur, as Authenticator and Ticket are generated by the client handler for the same user.
            if (cause instanceof RejectedException) {
                throw new FatalImplementationErrorException(this, cause);
            }

            ExceptionPrinter.printHistory(cause, LOGGER, LogLevel.ERROR);
            throw new CouldNotPerformException("Internal server error.", cause);
        } catch (JPNotAvailableException | CouldNotPerformException | IOException | InterruptedException ex) {
            ExceptionPrinter.printHistory(ex, LOGGER, LogLevel.ERROR);
            throw new CouldNotPerformException("Login failed! Please try again.", ex);
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
            AuthenticatedValue value = CachedAuthenticationRemote.getRemote().requestServiceServerSecretKey(ticketAuthenticatorWrapper).get();
            ticketAuthenticatorWrapper = AuthenticationClientHandler.handleServiceServerResponse(sessionKey, ticketAuthenticatorWrapper, value.getTicketAuthenticatorWrapper());

            serviceServerSecretKey = EncryptionHelper.decryptSymmetric(value.getValue(), sessionKey, byte[].class);
        } catch (ExecutionException | RejectedException | IOException | BadPaddingException ex) {
            ExceptionPrinter.printHistory(ex, LOGGER, LogLevel.ERROR);
            throw new CouldNotPerformException("Could not get the service server secret key.", ex);
        }
    }
}
