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
import java.io.IOException;
import java.security.KeyPair;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.openbase.bco.authentication.lib.mock.MockClientStore;
import org.openbase.bco.authentication.lib.jp.JPAuthenticationSimulationMode;
import org.openbase.jps.core.JPService;
import org.openbase.jps.exception.JPNotAvailableException;
import javax.crypto.BadPaddingException;
import org.openbase.bco.authentication.lib.jp.JPEnableAuthentication;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InitializationException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.exception.PermissionDeniedException;
import org.openbase.jul.exception.RejectedException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.exception.printer.LogLevel;
import org.openbase.jul.pattern.Observable;
import org.openbase.jul.pattern.ObservableImpl;
import org.openbase.jul.pattern.Observer;
import org.slf4j.LoggerFactory;
import rst.domotic.authentication.AuthenticatorType;
import rst.domotic.authentication.LoginCredentialsChangeType.LoginCredentialsChange;
import rst.domotic.authentication.TicketAuthenticatorWrapperType.TicketAuthenticatorWrapper;
import rst.domotic.authentication.TicketSessionKeyWrapperType.TicketSessionKeyWrapper;

/**
 *
 * @author <a href="mailto:sfast@techfak.uni-bielefeld.de">Sebastian Fast</a>
 */
public class SessionManager {

    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(SessionManager.class);
    private static final String STORE_FILENAME = "client_credential_store.json";

    private static SessionManager instance;

    private TicketAuthenticatorWrapper ticketAuthenticatorWrapper;
    private byte[] sessionKey;

    private CredentialStore store;

    // remember id of client that is currently logged in
    private String clientId;

    // remember id of client during session
    private String previousClientId;

    // remember user id during session
    private String userId;
    private String userPassword;

    /**
     * Observable on which it will be notified if login or logout is triggered.
     */
    private final ObservableImpl<Boolean> loginObervable;

    public static synchronized SessionManager getInstance() {
        if (instance == null) {
            instance = new SessionManager();
        }

        return instance;
    }

    public SessionManager() {
        this(null, null);
    }

    public SessionManager(byte[] sessionKey) {
        this(null, sessionKey);
    }

    public SessionManager(CredentialStore userStore) {
        this(userStore, null);
    }

    public SessionManager(CredentialStore userStore, byte[] sessionKey) {
        // load registry
        this.loginObervable = new ObservableImpl<>();
        boolean simulation = false;
        try {
            simulation = JPService.getProperty(JPAuthenticationSimulationMode.class).getValue();
        } catch (JPNotAvailableException ex) {
            LOGGER.warn("Could not check simulation property. Starting in normal mode.", ex);
        }
        if (simulation) {
            this.store = new MockClientStore(STORE_FILENAME);
        } else {
            this.store = userStore;
        }
        if (sessionKey != null) {
            this.sessionKey = sessionKey;
        }
    }

    public void initStore() throws InitializationException {
        if (this.store == null) {
            this.store = new CredentialStore(STORE_FILENAME);
        }
        this.store.init();
    }

    public TicketAuthenticatorWrapper getTicketAuthenticatorWrapper() {
        return ticketAuthenticatorWrapper;
    }

    public synchronized void setTicketAuthenticatorWrapper(TicketAuthenticatorWrapper ticketAuthenticatorWrapper) throws IOException, BadPaddingException {
        if (this.ticketAuthenticatorWrapper == null) {
            this.ticketAuthenticatorWrapper = ticketAuthenticatorWrapper;
        } else {
            AuthenticatorType.Authenticator lastAuthenticator = EncryptionHelper.decryptSymmetric(
                    this.ticketAuthenticatorWrapper.getAuthenticator(), this.getSessionKey(), AuthenticatorType.Authenticator.class);
            AuthenticatorType.Authenticator currentAuthenticator = EncryptionHelper.decryptSymmetric(
                    ticketAuthenticatorWrapper.getAuthenticator(), this.getSessionKey(), AuthenticatorType.Authenticator.class);
            if (currentAuthenticator.getTimestamp().getTime() > lastAuthenticator.getTimestamp().getTime()) {
                this.ticketAuthenticatorWrapper = ticketAuthenticatorWrapper;
            }
        }
    }

    public byte[] getSessionKey() {
        return sessionKey;
    }

    public TicketAuthenticatorWrapper initializeServiceServerRequest() throws RejectedException {
        try {
            return AuthenticationClientHandler.initServiceServerRequest(this.getSessionKey(), this.getTicketAuthenticatorWrapper());
        } catch (IOException | BadPaddingException ex) {
            throw new RejectedException("Initializing request rejected", ex);
        }
    }

    /**
     * TODO: Save Login data, if keepUserLoggedIn set to true
     * Perform a login for a given userId and password.
     *
     * @param userId Identifier of the user
     * @param userPassword Password of the user
     * @return Returns Returns an TicketAuthenticatorWrapperWrapper containing
     * both the ClientServerTicket and Authenticator
     * @throws NotAvailableException If the entered clientId could not be found.
     * @throws CouldNotPerformException In case of a communication error between client and server.
     */
    public synchronized boolean login(String userId, String userPassword) throws CouldNotPerformException, NotAvailableException {
        return this.login(userId, userPassword, false);
    }

    public synchronized boolean login(String userId, String userPassword, boolean rememberPassword) throws CouldNotPerformException, NotAvailableException {
        byte[] clientPasswordHash = EncryptionHelper.hash(userPassword);
        if (rememberPassword) {
            this.userPassword = userPassword;
        }
        return this.internalLogin(userId, clientPasswordHash, true);
    }

    /**
     * Perform a login for a given clientId.
     *
     * @param clientId Identifier of the user
     * @return Returns Returns an TicketAuthenticatorWrapperWrapper containing
     * both the ClientServerTicket and Authenticator
     * @throws NotAvailableException If the entered clientId could not be found.
     * @throws CouldNotPerformException In case of a communication error between client and server.
     */
    public synchronized boolean login(String clientId) throws CouldNotPerformException, NotAvailableException {
        byte[] key = getCredentials(clientId);
        return this.internalLogin(clientId, key, false);
    }

    /**
     * Perform a relog for the client registered in the store.
     *
     * @return Returns true if relog successful, appropriate exception otherwise
     * @throws NotAvailableException If the entered clientId could not be found. Or if the clientId was not set in the beginning.
     * @throws CouldNotPerformException In case of a communication error between client and server.
     */
    public synchronized boolean relog() throws CouldNotPerformException, NotAvailableException {
        this.ticketAuthenticatorWrapper = null;
        this.sessionKey = null;

        // if user is logged in and can login again
        if (this.canUserLoginAgain()) {
            return this.login(this.userId, this.userPassword, true);
        }

        // if user is not logged in and the client can login again
        if (this.canClientLoginAgain()) {
            return this.login(this.previousClientId);
        }

        // if neither user or client can login again
        this.logout();
        throw new CouldNotPerformException("Your session has expired. You have been logged out for security reasons. Please log in again.");
    }

    /**
     * Perform a login for a given userId and password.
     *
     * @param id Identifier of the user or client
     * @param key Password or private key of the user or client
     * @return Returns true if login successful
     * @throws NotAvailableException If the entered clientId could not be found.
     * @throws CouldNotPerformException In case of a communication error between client and server.
     */
    private boolean internalLogin(String id, byte[] key, boolean isUser) throws CouldNotPerformException, NotAvailableException {
        try {
            if (!JPService.getProperty(JPEnableAuthentication.class).getValue()) {
                return false;
            }
        } catch (JPNotAvailableException ex) {
            throw new CouldNotPerformException("Could not check JPEnableAuthenticationProperty", ex);
        }

        // temporary wrapper and session key. Incase a new user/client wants to login but failed then
        // reset these in order to reset the session to previous user/client
        TicketAuthenticatorWrapper tmpTicketAuthenticatorWrapper = null;
        byte[] tmpSessionKey = null;
        String tmpUserId = null;
        String tmpUserPassword = null;

        // becomes true if login was successful
        boolean result = false;

        if (this.isLoggedIn()) {
            // if same user or client is already looged in
            if (id.equals(this.userId) || id.equals(this.clientId)) {
                return true;
            }
            // if other user or client wants to login
            // then logout user or client 
            // and log them in again in case of login failure
            tmpTicketAuthenticatorWrapper = this.ticketAuthenticatorWrapper;
            tmpSessionKey = this.sessionKey;
            tmpUserId = this.userId;
            tmpUserPassword = this.userPassword;
            this.logout();
        }

        if (isUser) {
            this.userId = id;
        } else {
            this.clientId = id;
            this.previousClientId = id;
        }

        try {
            // prepend clientId to userId for TicketGrantingTicket request
            String userIdAtClientId = "@";
            byte[] userKey = null;
            byte[] clientKey = null;

            if (this.previousClientId != null) {
                userIdAtClientId = userIdAtClientId + this.previousClientId;
                clientKey = getCredentials(this.previousClientId);
            }

            if (isUser) {
                userIdAtClientId = id + userIdAtClientId;
                userKey = key;
            } else {
                clientKey = key;
            }

            // request TGT
            TicketSessionKeyWrapper ticketSessionKeyWrapper = CachedAuthenticationRemote.getRemote().requestTicketGrantingTicket(userIdAtClientId).get();

            // handle KDC response on client side
            List<Object> list = AuthenticationClientHandler.handleKeyDistributionCenterResponse(userIdAtClientId, userKey, clientKey, ticketSessionKeyWrapper);
            TicketAuthenticatorWrapper taw = (TicketAuthenticatorWrapper) list.get(0); // save at somewhere temporarily
            byte[] ticketGrantingServiceSessionKey = (byte[]) list.get(1); // save TGS session key somewhere on client side

            // request CST
            ticketSessionKeyWrapper = CachedAuthenticationRemote.getRemote().requestClientServerTicket(taw).get();

            // handle TGS response on client side
            list = AuthenticationClientHandler.handleTicketGrantingServiceResponse(userIdAtClientId, ticketGrantingServiceSessionKey, ticketSessionKeyWrapper);
            this.ticketAuthenticatorWrapper = (TicketAuthenticatorWrapper) list.get(0); // save at somewhere temporarily
            this.sessionKey = (byte[]) list.get(1); // save SS session key somewhere on client side

            try {
                loginObervable.notifyObservers(true);
            } catch (CouldNotPerformException ex) {
                LOGGER.warn("Could not notify login to observer", ex);
            }

            result = true;
            return result;
        } catch (BadPaddingException ex) {
            throw new CouldNotPerformException("The password you have entered was wrong. Please try again!");
        } catch (ExecutionException ex) {
            Throwable cause = ex.getCause();

            // Ugly workaround, as RSB wraps the stacktrace into the message string.
            Pattern pattern = Pattern.compile("NotAvailableException: (.*)[\n\r]");
            Matcher matcher = pattern.matcher(cause.getMessage());

            if (matcher.find()) {
                ExceptionPrinter.printHistory(cause, LOGGER, LogLevel.ERROR);
                throw new NotAvailableException(matcher.group(1));
            }

            ExceptionPrinter.printHistory(cause, LOGGER, LogLevel.ERROR);
            throw new CouldNotPerformException("Internal server error.", cause);
        } catch (CouldNotPerformException | IOException | InterruptedException ex) {
            throw new CouldNotPerformException("Login failed! Please try again.", ex);
        } finally {
            if (!result) {
                // reset incase of login failure
                this.ticketAuthenticatorWrapper = tmpTicketAuthenticatorWrapper;
                this.sessionKey = tmpSessionKey;
                this.userId = tmpUserId;
                this.userPassword = tmpUserPassword;
            }
        }
    }

    /**
     * Logs a user out by setting CST and session key to null
     */
    public synchronized void logout() {
        // if a user was logged in, login client, if a client was already logged in
        if (this.userId != null) {
            this.userId = null;
            this.userPassword = null;
            if (this.previousClientId != null) {
                try {
                    this.login(this.previousClientId);
                } catch (CouldNotPerformException ex) {
                    ExceptionPrinter.printHistory(ex, LOGGER, LogLevel.ERROR);
                }
                return;
            }
        }
        if (this.clientId != null) {
            this.clientId = null;
        }
        this.ticketAuthenticatorWrapper = null;
        this.sessionKey = null;
        try {
            loginObervable.notifyObservers(false);
        } catch (CouldNotPerformException ex) {
            LOGGER.warn("Could not notify logout to observer", ex);
        }
    }

    /**
     * This method performs a complete logout by also clearing the previous client id.
     * If the normal logout is used and a new user logs in he will be logged in at the last
     * client every time. This method is mostly necessary for unit tests.
     */
    public synchronized void completeLogout() {
        this.userId = null;
        this.userPassword = null;
        this.previousClientId = null;
        this.clientId = null;
        this.sessionKey = null;
        this.ticketAuthenticatorWrapper = null;
        try {
            loginObervable.notifyObservers(false);
        } catch (CouldNotPerformException ex) {
            LOGGER.warn("Could not notify complete logout to observer", ex);
        }
    }

    /**
     * determines if a user is logged in.
     * does not validate ClientServerTicket and SessionKey
     *
     * @return Returns true if logged in otherwise false
     */
    public boolean isLoggedIn() {
        return this.ticketAuthenticatorWrapper != null && this.sessionKey != null;
    }

    /**
     * Determines whether the currently logged in user is an admin.
     *
     * @return True if the user is an admin, false if not.
     */
    public synchronized boolean isAdmin() {
        if (!this.isLoggedIn()) {
            return false;
        }

        try {
            return CachedAuthenticationRemote.getRemote().isAdmin(userId).get();
        } catch (InterruptedException | CouldNotPerformException | ExecutionException ex) {
            ExceptionPrinter.printHistory(ex, LOGGER, LogLevel.ERROR);
        }

        return false;
    }

    /**
     * determines if a user can login again.
     *
     * @return true if so, false otherwise
     */
    public boolean canUserLoginAgain() {
        if (this.userId != null) {
            return this.userPassword != null;
        }
        return false;
    }

    /**
     * determines if a client can login again.
     *
     * @return true if so, false otherwise
     */
    public boolean canClientLoginAgain() {
        return this.previousClientId != null && this.store != null && this.store.hasEntry(this.previousClientId);
    }

    /**
     * determines if a user is authenticated.
     * does validate ClientServerTicket and SessionKey
     *
     * @return Returns true if authenticated otherwise appropriate exception
     * @throws org.openbase.jul.exception.CouldNotPerformException In case of a communication error between client and server.
     */
    public synchronized boolean isAuthenticated() throws CouldNotPerformException {
        if (!this.isLoggedIn()) {
            return false;
        }

        try {
            Observer<Boolean> observer = (Observable<Boolean> source, Boolean data) -> {
                LOGGER.warn("Login state change while in isAuthenticated to [" + data + "]" + sessionKey);
            };
            this.loginObervable.addObserver(observer);
            byte[] before = this.sessionKey;
            TicketAuthenticatorWrapper request = AuthenticationClientHandler.initServiceServerRequest(this.sessionKey, this.ticketAuthenticatorWrapper);
            byte[] init = this.sessionKey;
            TicketAuthenticatorWrapper response = CachedAuthenticationRemote.getRemote().validateClientServerTicket(request).get();
            byte[] after = this.sessionKey;
            if (this.sessionKey == null) {
                this.loginObervable.removeObserver(observer);
                throw new CouldNotPerformException("Why is this happening?[" + before + ", " + init + ", " + after + "]");
            }
            response = AuthenticationClientHandler.handleServiceServerResponse(this.sessionKey, request, response);
            this.ticketAuthenticatorWrapper = response;
            this.loginObervable.removeObserver(observer);
            return true;
        } catch (IOException | BadPaddingException ex) {
            this.logout();
            ExceptionPrinter.printHistory(ex, LOGGER, LogLevel.ERROR);
            throw new CouldNotPerformException("Decryption failed. You have been logged out for security reasons. Please log in again.");
        } catch (InterruptedException ex) {
            // keep the interruption
            Thread.currentThread().interrupt();
            throw new CouldNotPerformException("Action was interrupted.", ex);
        } catch (ExecutionException ex) {
            Throwable cause = ex.getCause();

            Pattern pattern = Pattern.compile("RejectedException: (.*)[\n\r]");
            Matcher matcher = pattern.matcher(cause.getMessage());

            if (matcher.find()) {
                ExceptionPrinter.printHistory(cause, LOGGER, LogLevel.ERROR);
                throw new RejectedException(matcher.group(1));
            }

            pattern = Pattern.compile("PermissionDeniedException: (.*)[\n\r]");
            matcher = pattern.matcher(cause.getMessage());

            if (matcher.find()) {
                ExceptionPrinter.printHistory(cause, LOGGER, LogLevel.ERROR);
                throw new PermissionDeniedException(matcher.group(1));
            }

            ExceptionPrinter.printHistory(cause, LOGGER, LogLevel.ERROR);
            throw new CouldNotPerformException("Internal server error.", cause);
        }
    }

    /**
     * Changes the login credentials for a given user.
     *
     * @param clientId ID of the user / client whose credentials should be changed.
     * @param oldCredentials Old credentials, needed for verification.
     * @param newCredentials New credentials to be set.
     * @throws CouldNotPerformException In case of a communication error between client and server.
     */
    public synchronized void changeCredentials(String clientId, String oldCredentials, String newCredentials) throws CouldNotPerformException {
        if (!this.isLoggedIn()) {
            throw new CouldNotPerformException("Please log in first!");
        }

        if (clientId == null) {
            clientId = userId;
        }

        try {
            ticketAuthenticatorWrapper = AuthenticationClientHandler.initServiceServerRequest(sessionKey, ticketAuthenticatorWrapper);
            byte[] oldHash = EncryptionHelper.hash(oldCredentials);
            byte[] newHash = EncryptionHelper.hash(newCredentials);

            LoginCredentialsChange loginCredentialsChange = LoginCredentialsChange.newBuilder()
                    .setId(clientId)
                    .setOldCredentials(EncryptionHelper.encryptSymmetric(oldHash, sessionKey))
                    .setNewCredentials(EncryptionHelper.encryptSymmetric(newHash, sessionKey))
                    .setTicketAuthenticatorWrapper(ticketAuthenticatorWrapper)
                    .build();

            TicketAuthenticatorWrapper newTicketAuthenticatorWrapper = CachedAuthenticationRemote.getRemote().changeCredentials(loginCredentialsChange).get();
            ticketAuthenticatorWrapper = AuthenticationClientHandler.handleServiceServerResponse(sessionKey, ticketAuthenticatorWrapper, newTicketAuthenticatorWrapper);
        } catch (IOException | BadPaddingException ex) {
            this.logout();
            ExceptionPrinter.printHistory(ex, LOGGER, LogLevel.ERROR);
            throw new CouldNotPerformException("Decryption failed. You have been logged out for security reasons. Please log in again.");
        } catch (RejectedException | NotAvailableException ex) {
            throw ExceptionPrinter.printHistoryAndReturnThrowable(ex, LOGGER, LogLevel.ERROR);
        } catch (InterruptedException ex) {
            ExceptionPrinter.printHistory(ex, LOGGER, LogLevel.ERROR);
            throw new CouldNotPerformException("Action was interrupted.", ex);
        } catch (ExecutionException ex) {
            Throwable cause = ex.getCause();

            Pattern pattern = Pattern.compile("RejectedException: (.*)[\n\r]");
            Matcher matcher = pattern.matcher(cause.getMessage());

            if (matcher.find()) {
                ExceptionPrinter.printHistory(cause, LOGGER, LogLevel.ERROR);
                throw new RejectedException(matcher.group(1));
            }

            pattern = Pattern.compile("PermissionDeniedException: (.*)[\n\r]");
            matcher = pattern.matcher(cause.getMessage());

            if (matcher.find()) {
                ExceptionPrinter.printHistory(cause, LOGGER, LogLevel.ERROR);
                throw new PermissionDeniedException(matcher.group(1));
            }

            ExceptionPrinter.printHistory(cause, LOGGER, LogLevel.ERROR);
            throw new CouldNotPerformException("Internal server error.", cause);
        }
    }

    /**
     * Registers a client.
     *
     * @param clientId the id of the client
     * @throws org.openbase.jul.exception.CouldNotPerformException
     */
    public synchronized void registerClient(String clientId) throws CouldNotPerformException {
        if (this.store == null) {
            try {
                this.initStore();
            } catch (InitializationException ex) {
                throw new NotAvailableException(ex);
            }
        }

        KeyPair keyPair = EncryptionHelper.generateKeyPair();
        this.internalRegister(clientId, keyPair.getPublic().getEncoded(), false);
        this.store.setCredentials(clientId, keyPair.getPrivate().getEncoded());
    }

    /**
     * Registers a user.
     *
     * @param userId the id of the user
     * @param password the password of the user
     * @param isAdmin flag if user should be an administrator
     * @throws org.openbase.jul.exception.CouldNotPerformException
     */
    public synchronized void registerUser(String userId, String password, boolean isAdmin) throws CouldNotPerformException {
        byte[] key = EncryptionHelper.hash(password);
        this.internalRegister(userId, key, isAdmin);
    }

    /**
     * Registers a user or client.
     * Assumes an administrator who has permissions for this already exists and is logged in with current session manager.
     * Overwrites duplicate entries on client, if entry to be registered does not exist on server.
     * Does not overwrite duplicate entries on client, if entry does exist on server.
     *
     * @param userId the id of the user
     * @param password the password of the user
     * @param isAdmin flag if user should be an administrator
     * @throws org.openbase.jul.exception.CouldNotPerformException
     */
    private void internalRegister(String id, byte[] key, boolean isAdmin) throws CouldNotPerformException {
        if (!this.isLoggedIn()) {
            throw new CouldNotPerformException("Please log in first!");
        }

        try {
            ticketAuthenticatorWrapper = AuthenticationClientHandler.initServiceServerRequest(this.sessionKey, this.ticketAuthenticatorWrapper);

            LoginCredentialsChange loginCredentialsChange = LoginCredentialsChange.newBuilder()
                    .setId(id)
                    .setNewCredentials(EncryptionHelper.encryptSymmetric(key, this.sessionKey))
                    .setTicketAuthenticatorWrapper(this.ticketAuthenticatorWrapper)
                    .setAdmin(isAdmin)
                    .build();

            TicketAuthenticatorWrapper wrapper = CachedAuthenticationRemote.getRemote().register(loginCredentialsChange).get();
            ticketAuthenticatorWrapper = AuthenticationClientHandler.handleServiceServerResponse(this.sessionKey, this.ticketAuthenticatorWrapper, wrapper);
        } catch (IOException | BadPaddingException ex) {
            this.logout();
            ExceptionPrinter.printHistory(ex, LOGGER, LogLevel.ERROR);
            throw new CouldNotPerformException("Decryption failed. You have been logged out for security reasons. Please log in again.");
        } catch (RejectedException | NotAvailableException ex) {
            throw ExceptionPrinter.printHistoryAndReturnThrowable(ex, LOGGER, LogLevel.ERROR);
        } catch (InterruptedException ex) {
            ExceptionPrinter.printHistory(ex, LOGGER, LogLevel.ERROR);
            throw new CouldNotPerformException("Action was interrupted.", ex);
        } catch (ExecutionException ex) {
            Throwable cause = ex.getCause();

            Pattern pattern = Pattern.compile("RejectedException: (.*)[\n\r]");
            Matcher matcher = pattern.matcher(cause.getMessage());

            if (matcher.find()) {
                ExceptionPrinter.printHistory(cause, LOGGER, LogLevel.ERROR);
                throw new RejectedException(matcher.group(1));
            }

            pattern = Pattern.compile("PermissionDeniedException: (.*)[\n\r]");
            matcher = pattern.matcher(cause.getMessage());

            if (matcher.find()) {
                ExceptionPrinter.printHistory(cause, LOGGER, LogLevel.ERROR);
                throw new PermissionDeniedException(matcher.group(1));
            }

            ExceptionPrinter.printHistory(cause, LOGGER, LogLevel.ERROR);
            throw new CouldNotPerformException("Internal server error.", cause);
        }
    }

    public synchronized void removeUser(String id) throws CouldNotPerformException {
        if (!this.isAdmin()) {
            throw new CouldNotPerformException("You have to be an admin to perform this action");
        }

        try {
            ticketAuthenticatorWrapper = AuthenticationClientHandler.initServiceServerRequest(this.sessionKey, this.ticketAuthenticatorWrapper);

            LoginCredentialsChange loginCredentialsChange = LoginCredentialsChange.newBuilder()
                    .setId(id)
                    .setTicketAuthenticatorWrapper(this.ticketAuthenticatorWrapper)
                    .build();

            TicketAuthenticatorWrapper wrapper = CachedAuthenticationRemote.getRemote().removeUser(loginCredentialsChange).get();
            ticketAuthenticatorWrapper = AuthenticationClientHandler.handleServiceServerResponse(this.sessionKey, this.ticketAuthenticatorWrapper, wrapper);
        } catch (IOException | BadPaddingException ex) {
            this.logout();
            ExceptionPrinter.printHistory(ex, LOGGER, LogLevel.ERROR);
            throw new CouldNotPerformException("Decryption failed. You have been logged out for security reasons. Please log in again.");
        } catch (RejectedException | NotAvailableException ex) {
            throw ExceptionPrinter.printHistoryAndReturnThrowable(ex, LOGGER, LogLevel.ERROR);
        } catch (InterruptedException ex) {
            ExceptionPrinter.printHistory(ex, LOGGER, LogLevel.ERROR);
            throw new CouldNotPerformException("Action was interrupted.", ex);
        } catch (ExecutionException ex) {
            Throwable cause = ex.getCause();

            Pattern pattern = Pattern.compile("RejectedException: (.*)[\n\r]");
            Matcher matcher = pattern.matcher(cause.getMessage());

            if (matcher.find()) {
                ExceptionPrinter.printHistory(cause, LOGGER, LogLevel.ERROR);
                throw new RejectedException(matcher.group(1));
            }

            pattern = Pattern.compile("PermissionDeniedException: (.*)[\n\r]");
            matcher = pattern.matcher(cause.getMessage());

            if (matcher.find()) {
                ExceptionPrinter.printHistory(cause, LOGGER, LogLevel.ERROR);
                throw new PermissionDeniedException(matcher.group(1));
            }

            ExceptionPrinter.printHistory(cause, LOGGER, LogLevel.ERROR);
            throw new CouldNotPerformException("Internal server error.", cause);
        }
    }

    public synchronized void setAdministrator(String id, boolean isAdmin) throws CouldNotPerformException {
        if (!this.isLoggedIn()) {
            throw new CouldNotPerformException("Please log in first!");
        }

        try {
            ticketAuthenticatorWrapper = AuthenticationClientHandler.initServiceServerRequest(this.sessionKey, this.ticketAuthenticatorWrapper);

            LoginCredentialsChange loginCredentialsChange = LoginCredentialsChange.newBuilder()
                    .setId(id)
                    .setTicketAuthenticatorWrapper(this.ticketAuthenticatorWrapper)
                    .setAdmin(isAdmin)
                    .build();

            TicketAuthenticatorWrapper wrapper = CachedAuthenticationRemote.getRemote().setAdministrator(loginCredentialsChange).get();
            ticketAuthenticatorWrapper = AuthenticationClientHandler.handleServiceServerResponse(this.sessionKey, this.ticketAuthenticatorWrapper, wrapper);
        } catch (IOException | BadPaddingException ex) {
            this.logout();
            ExceptionPrinter.printHistory(ex, LOGGER, LogLevel.ERROR);
            throw new CouldNotPerformException("Decryption failed. You have been logged out for security reasons. Please log in again.");
        } catch (RejectedException | NotAvailableException ex) {
            throw ExceptionPrinter.printHistoryAndReturnThrowable(ex, LOGGER, LogLevel.ERROR);
        } catch (InterruptedException ex) {
            ExceptionPrinter.printHistory(ex, LOGGER, LogLevel.ERROR);
            throw new CouldNotPerformException("Action was interrupted.", ex);
        } catch (ExecutionException ex) {
            Throwable cause = ex.getCause();

            Pattern pattern = Pattern.compile("RejectedException: (.*)[\n\r]");
            Matcher matcher = pattern.matcher(cause.getMessage());

            if (matcher.find()) {
                ExceptionPrinter.printHistory(cause, LOGGER, LogLevel.ERROR);
                throw new RejectedException(matcher.group(1));
            }

            pattern = Pattern.compile("PermissionDeniedException: (.*)[\n\r]");
            matcher = pattern.matcher(cause.getMessage());

            if (matcher.find()) {
                ExceptionPrinter.printHistory(cause, LOGGER, LogLevel.ERROR);
                throw new PermissionDeniedException(matcher.group(1));
            }

            ExceptionPrinter.printHistory(cause, LOGGER, LogLevel.ERROR);
            throw new CouldNotPerformException("Internal server error.", cause);
        }
    }

    /**
     * Retrieves the credentials for a given client ID from the local store.
     *
     * @param clientId
     * @return Credentials, if they could be found.
     * @throws NotAvailableException
     */
    private byte[] getCredentials(String clientId) throws NotAvailableException {
        if (this.store == null) {
            try {
                this.initStore();
            } catch (InitializationException ex) {
                throw new NotAvailableException(ex);
            }
        }
        byte[] key = this.store.getCredentials(clientId);
        return key;
    }

    /**
     * If a user and/or client is logged in, this returns the concatenation of both IDs.
     *
     * @return userId@clientId
     */
    public String getUserAtClientId() {
        String userAtClient = "";

        if (userId != null) {
            userAtClient += userId;
        }

        userAtClient += "@";

        if (clientId != null) {
            userAtClient += clientId;
        }

        return userAtClient;
    }

    public String getUserId() {
        return userId;
    }

    public Observable<Boolean> getLoginObervable() {
        return loginObervable;
    }

    public String getClientId() {
        return clientId;
    }
}
