package org.openbase.bco.authentication.lib;

/*-
 * #%L
 * BCO Authentication Library
 * %%
 * Copyright (C) 2017 - 2019 openbase.org
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
import org.openbase.bco.authentication.lib.AuthenticationClientHandler.TicketWrapperSessionKeyPair;
import org.openbase.bco.authentication.lib.exception.SessionExpiredException;
import org.openbase.bco.authentication.lib.jp.JPAuthentication;
import org.openbase.bco.authentication.lib.jp.JPSessionTimeout;
import org.openbase.jps.core.JPService;
import org.openbase.jps.exception.JPNotAvailableException;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.exception.PermissionDeniedException;
import org.openbase.jul.exception.RejectedException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.exception.printer.LogLevel;
import org.openbase.jul.iface.Shutdownable;
import org.openbase.jul.pattern.ObservableImpl;
import org.openbase.jul.pattern.Observer;
import org.openbase.jul.schedule.GlobalCachedExecutorService;
import org.openbase.jul.schedule.GlobalScheduledExecutorService;
import org.openbase.type.domotic.authentication.AuthTokenType.AuthToken;
import org.openbase.type.domotic.authentication.AuthenticatedValueType.AuthenticatedValue;
import org.openbase.type.domotic.authentication.AuthenticatorType.Authenticator;
import org.openbase.type.domotic.authentication.LoginCredentialsChangeType.LoginCredentialsChange;
import org.openbase.type.domotic.authentication.LoginCredentialsType.LoginCredentials;
import org.openbase.type.domotic.authentication.TicketAuthenticatorWrapperType.TicketAuthenticatorWrapper;
import org.openbase.type.domotic.authentication.TicketSessionKeyWrapperType.TicketSessionKeyWrapper;
import org.openbase.type.domotic.authentication.UserClientPairType.UserClientPair;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author <a href="mailto:sfast@techfak.uni-bielefeld.de">Sebastian Fast</a>
 */
public class SessionManager implements Shutdownable {

    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(SessionManager.class);
    private static final String STORE_FILENAME = "client_credential_store.json";

    private static SessionManager instance;

    /**
     * Get the globally used session manager.
     *
     * @return the global session manager
     */
    public static synchronized SessionManager getInstance() {
        if (instance == null) {
            instance = new SessionManager();
        }

        return instance;
    }

    /**
     * Observable on which it is notified if login or logout is triggered.
     * The user@client id is notified on login in null on logout.
     */
    private final ObservableImpl<SessionManager, UserClientPair> loginObservable;
    /**
     * The ticket and authenticator of the current session.
     */
    private TicketAuthenticatorWrapper ticketAuthenticatorWrapper;
    /**
     * The key of the current session.
     */
    private byte[] sessionKey;
    /**
     * Credential store of the session manager. Storing user password hashes and client private keys.
     */
    private final CredentialStore credentialStore;
    /**
     * Pair describing the currently logged in user and client.
     */
    private final UserClientPair.Builder userClientPair;
    /**
     * Future of a task that renews the ticket of a user if he wants to stay logged in.
     */
    private ScheduledFuture ticketRenewalTask;

    private boolean skipNotification = false;

    /**
     * Create a session manager with the default credential store.
     */
    public SessionManager() {
        this(new CredentialStore());
    }

    /**
     * Crate a session manager using a given credential store.
     *
     * @param credentialStore the credential store used by the session manager.
     */
    public SessionManager(final CredentialStore credentialStore) {
        try {
            // register shutdown hook
            Shutdownable.registerShutdownHook(this);
        } catch (CouldNotPerformException ex) {
            ExceptionPrinter.printHistory("Could not register session manager shutdown hook", ex, LOGGER, LogLevel.WARN);
        }
        this.userClientPair = UserClientPair.newBuilder();
        // create login observable
        this.loginObservable = new ObservableImpl<>(this);
        // add executor service so that it is not waited for notifications and so that they are done in parallel
        this.loginObservable.setExecutorService(GlobalCachedExecutorService.getInstance().getExecutorService());
        // save and init credential store
        this.credentialStore = credentialStore;
        try {
            this.credentialStore.init(STORE_FILENAME);
        } catch (CouldNotPerformException ex) {
            ExceptionPrinter.printHistory("Could not init credential store for session manager", ex, LOGGER, LogLevel.WARN);
        }
    }

    //TODO: test if this method is necessary
    public TicketAuthenticatorWrapper getTicketAuthenticatorWrapper() {
        return ticketAuthenticatorWrapper;
    }

    /**
     * Update the ticket authentication wrapper of the session manager. This method has to be called with the wrapper
     * returned by a server after a request. Otherwise the interval in which the ticket is valid will not be updated
     * and the session will run out.
     * This method will also make sure to keep the current ticket if the given one is older.
     *
     * @param wrapper the new ticket authenticator wrapper
     *
     * @throws CouldNotPerformException if one of the tickets cannot be decrypted using the current session key
     */
    public synchronized void updateTicketAuthenticatorWrapper(final TicketAuthenticatorWrapper wrapper) throws CouldNotPerformException {
        if (ticketAuthenticatorWrapper == null) {
            // the ticket authenticator wrapper can only be null if no one is logged in, then it does not make sense to update it
            throw new CouldNotPerformException("Could not update ticketAuthenticatorWrapper because it was never set");
        }

        try {
            // decrypt current and new authenticators
            Authenticator lastAuthenticator = EncryptionHelper.decryptSymmetric(ticketAuthenticatorWrapper.getAuthenticator(), this.getSessionKey(), Authenticator.class);
            Authenticator currentAuthenticator = EncryptionHelper.decryptSymmetric(wrapper.getAuthenticator(), this.getSessionKey(), Authenticator.class);
            // keep the newer one
            if (currentAuthenticator.getTimestamp().getTime() > lastAuthenticator.getTimestamp().getTime()) {
                ticketAuthenticatorWrapper = wrapper;
            }
        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not update ticket authenticator wrapper", ex);
        }
    }

    /**
     * Get the key of the current session managed by this session manager.
     *
     * @return the current session key
     */
    public byte[] getSessionKey() {
        return sessionKey;
    }

    /**
     * Initialize the current ticket for a request.
     *
     * @return the current ticket initialized for a request
     *
     * @throws RejectedException if the ticket could not be initialized
     */
    public TicketAuthenticatorWrapper initializeServiceServerRequest() throws RejectedException {
        try {
            return AuthenticationClientHandler.initServiceServerRequest(this.getSessionKey(), this.getTicketAuthenticatorWrapper());
        } catch (CouldNotPerformException ex) {
            throw new RejectedException("Initializing request rejected", ex);
        }
    }

    public <VALUE extends Serializable> AuthenticatedValue initializeRequest(final VALUE value, final AuthToken authToken) throws CouldNotPerformException {
        AuthenticatedValue.Builder authenticatedValue = AuthenticatedValue.newBuilder();
        authenticatedValue.setTicketAuthenticatorWrapper(initializeServiceServerRequest());
        authenticatedValue.setValue(EncryptionHelper.encryptSymmetric(value, sessionKey));

        if (authToken != null && authToken.hasAuthenticationToken() && !authToken.getAuthenticationToken().isEmpty()) {
            authenticatedValue.setAuthenticationToken(EncryptionHelper.encryptSymmetric(authToken.getAuthenticationToken(), sessionKey));
        }

        if (authToken != null && authToken.hasAuthorizationToken() && !authToken.getAuthorizationToken().isEmpty()) {
            authenticatedValue.setAuthorizationToken(EncryptionHelper.encryptSymmetric(authToken.getAuthorizationToken(), sessionKey));
        }

        return authenticatedValue.build();
    }

    /**
     * Perform a login for a given userId and password.
     *
     * @param userId   identifier of the user
     * @param password password of the user
     *
     * @throws CouldNotPerformException if the user could not be logged in using the password
     */
    public synchronized void login(final String userId, final String password) throws CouldNotPerformException {
        login(userId, password, false);
    }

    public synchronized void loginClient(final String clientId, final String credentialsUTF8) throws CouldNotPerformException {
        internalLogin(clientId, LoginCredentials.newBuilder().setCredentials(ByteString.copyFrom(credentialsUTF8, StandardCharsets.UTF_8)).setSymmetric(false).build(), true, false);
    }

    /**
     * Perform a login for a given userId and password.
     *
     * @param userId       identifier of the user
     * @param password     password of the user
     * @param stayLoggedIn flag determining if the session should automatically be renewed before it times out
     *
     * @throws CouldNotPerformException if the user could not be logged in with the given password
     */
    public synchronized void login(final String userId, final String password, final boolean stayLoggedIn) throws CouldNotPerformException {
        login(userId, password, stayLoggedIn, false);
    }

    /**
     * Perform a login for a given userId and password.
     *
     * @param userId           identifier of the user
     * @param password         password of the user
     * @param stayLoggedIn     flag determining if the session should automatically be renewed before it times out
     * @param rememberPassword flag determining if the users password should be saved in the credential store. If this is
     *                         done the user can afterwards login just using his id.
     *
     * @throws CouldNotPerformException if the user could not be logged in with the given password
     */
    public synchronized void login(final String userId, final String password, final boolean stayLoggedIn, final boolean rememberPassword) throws CouldNotPerformException {
        final LoginCredentials loginCredentials = LoginCredentials.newBuilder().setId(userId).setCredentials(ByteString.copyFrom(EncryptionHelper.hash(password))).setSymmetric(true).build();
        if (rememberPassword) {
            credentialStore.addEntry(userId, loginCredentials);
        }
        internalLogin(userId, loginCredentials, stayLoggedIn, true);
    }

    /**
     * Perform a login for a given user or client by id.
     *
     * @param clientIdOrUserId identifier of the user or client
     *
     * @throws NotAvailableException    if no entry for an according client or user could be found in the store
     * @throws CouldNotPerformException if logging in failed
     */
    public synchronized void login(final String clientIdOrUserId) throws CouldNotPerformException, NotAvailableException {
        login(clientIdOrUserId, false);
    }

    /**
     * Perform a login for a given user or client by id.
     *
     * @param clientIdOrUserId identifier of the user or client
     * @param stayLoggedIn     flag determining if the session should be automatically renewed. This is only necessary for users
     *                         because its default behaviour for clients.
     *
     * @throws NotAvailableException    if no entry for an according client or user could be found in the store
     * @throws CouldNotPerformException if logging in failed
     */
    public synchronized void login(final String clientIdOrUserId, final boolean stayLoggedIn) throws CouldNotPerformException, NotAvailableException {
        boolean isUser;
        LoginCredentials credentials;
        if (credentialStore.hasEntry(clientIdOrUserId)) {
            isUser = true;
            credentials = credentialStore.getCredentials(clientIdOrUserId);
        } else if (credentialStore.hasEntry("@" + clientIdOrUserId)) {
            isUser = false;
            credentials = credentialStore.getCredentials("@" + clientIdOrUserId);
        } else {
            throw new NotAvailableException("User or client with id[" + clientIdOrUserId + "]");
        }
        this.internalLogin(clientIdOrUserId, credentials, stayLoggedIn, isUser);
    }

    /**
     * Perform a login for a given userId and password.
     *
     * @param id               Identifier of the user or client
     * @param loginCredentials credentials of the user/client to be logged in.
     *
     * @return Returns true if login successful
     *
     * @throws NotAvailableException    If the entered clientId could not be found.
     * @throws CouldNotPerformException In case of a communication error between client and server.
     */
    private synchronized void internalLogin(final String id, final LoginCredentials loginCredentials, final boolean stayLoggedIn, final boolean isUser) throws CouldNotPerformException, NotAvailableException {
        // validate authentication property
        try {
            if (!JPService.getProperty(JPAuthentication.class).getValue()) {
                throw new CouldNotPerformException("Could not login. Authentication is disabled");
            }
        } catch (JPNotAvailableException ex) {
            throw new CouldNotPerformException("Could not check JPEnableAuthenticationProperty", ex);
        }

        // handle cases when somebody is already logged in
        if (this.isLoggedIn()) {
            // do nothing if same user or client is already logged in
            if (id.equals(this.userClientPair.getUserId()) || id.equals(this.userClientPair.getClientId())) {
                return;
            }

            // cancel current ticket renewal task
            if (ticketRenewalTask != null && !ticketRenewalTask.isDone()) {
                ticketRenewalTask.cancel(true);
            }

            // if new client is logged in while a user is logged in the user has to be logged out
            if (!userClientPair.getUserId().isEmpty() && !isUser) {
                this.logout();
            }
        }

        // save the new id
        if (isUser) {
            userClientPair.setUserId(id);
        } else {
            userClientPair.setClientId(id);
        }

        // resolve user at client id and credentials
        LoginCredentials userCredentials = null;
        LoginCredentials clientCredentials = null;
        if (isUser) {
            LOGGER.warn("User {} will be logged in with credentials {}", isUser, loginCredentials);
            // user is logged in so the parameters are his credentials
            userCredentials = loginCredentials;

            // if client was logged in get its credentials from the store
            if (!userClientPair.getClientId().isEmpty()) {
                clientCredentials = credentialStore.getCredentials(userClientPair.getClientId());
            }
        } else {
            // client is logged in so the parameters are his credentials
            clientCredentials = loginCredentials;
        }

        try {
            // request ticket granting ticket
            TicketSessionKeyWrapper ticketSessionKeyWrapper = CachedAuthenticationRemote.getRemote().requestTicketGrantingTicket(getUserClientPair()).get();
            // handle response

            LOGGER.warn("Pair {}", userClientPair);
            LOGGER.warn("Decrypt with user {} and client {}", userCredentials, clientCredentials);
            TicketWrapperSessionKeyPair ticketWrapperSessionKeyPair = AuthenticationClientHandler.handleKeyDistributionCenterResponse(getUserClientPair(), userCredentials, clientCredentials, ticketSessionKeyWrapper);

            // request client server ticket
            ticketSessionKeyWrapper = CachedAuthenticationRemote.getRemote().requestClientServerTicket(ticketWrapperSessionKeyPair.getTicketAuthenticatorWrapper()).get();
            // handle response
            ticketWrapperSessionKeyPair = AuthenticationClientHandler.handleTicketGrantingServiceResponse(getUserClientPair(), ticketWrapperSessionKeyPair.getSessionKey(), ticketSessionKeyWrapper);
            this.ticketAuthenticatorWrapper = ticketWrapperSessionKeyPair.getTicketAuthenticatorWrapper();
            this.sessionKey = ticketWrapperSessionKeyPair.getSessionKey();

            notifyLoginObserver();

            // user wants to stay logged or is a client so trigger a ticket renewal task
            if (stayLoggedIn || !isUser) {
                try {
                    final Long sessionTimeout = JPService.getProperty(JPSessionTimeout.class).getValue();
                    final long delay = (long) ((2 * sessionTimeout) / 4.0d);
                    ticketRenewalTask = GlobalScheduledExecutorService.scheduleWithFixedDelay(() -> {
                        try {
                            renewTicket();
                        } catch (CouldNotPerformException ex) {
                            ExceptionPrinter.printHistory("Could not renew ticket", ex, LOGGER, LogLevel.WARN);
                        }
                    }, delay, delay, TimeUnit.MILLISECONDS);
                } catch (JPNotAvailableException ex) {
                    ExceptionPrinter.printHistory("Could not start ticket renewal task", ex, LOGGER, LogLevel.WARN);
                }
            }
        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not login");
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
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new CouldNotPerformException("Could not login", ex);
        }
    }

    /**
     * Logout by canceling the ticket renewal task and clearing the ticket and session key.
     * If a user is logged in his id will be cleared and if a client was also logged in the client will be logged in again.
     */
    public synchronized void logout() {
        // cancel ticket renewal task
        if (ticketRenewalTask != null && !ticketRenewalTask.isDone()) {
            ticketRenewalTask.cancel(true);
        }

        // clear ticket and session key
        this.ticketAuthenticatorWrapper = null;
        this.sessionKey = null;

        // if a user was logged in clear user id
        if (!userClientPair.getUserId().isEmpty()) {
            userClientPair.clearUserId();

            // if a client was logged in additionally, log him in again
            if (!userClientPair.getClientId().isEmpty()) {
                try {
                    login(userClientPair.getClientId());
                    // return because the login notifies observer already
                    return;
                } catch (CouldNotPerformException ex) {
                    ExceptionPrinter.printHistory("Could not login as client again after user logout", ex, LOGGER, LogLevel.WARN);
                }
            }
        } else if (!userClientPair.getClientId().isEmpty()) {
            // only a client has been logged in so clear its id
            userClientPair.clearClientId();
        }

        // notify observer of logout
        notifyLoginObserver();
    }

    /**
     * The normal logout method logs in a client again if a user and a client were registered.
     * This method will also logout the client. This is mostly necessary for unit tests.
     */
    public synchronized void completeLogout() {
        if (ticketRenewalTask != null && !ticketRenewalTask.isDone()) {
            ticketRenewalTask.cancel(true);
        }
        userClientPair.clearUserId();
        userClientPair.clearClientId();
        sessionKey = null;
        ticketAuthenticatorWrapper = null;
        notifyLoginObserver();
    }

    /**
     * Method notifies login observer with the current user at client id.
     */
    private void notifyLoginObserver() {
        if (skipNotification) {
            return;
        }

        try {
            loginObservable.notifyObservers(getUserClientPair());
        } catch (CouldNotPerformException ex) {
            ExceptionPrinter.printHistory("Could not notify logout to observer", ex, LOGGER, LogLevel.WARN);
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
            return CachedAuthenticationRemote.getRemote().isAdmin(userClientPair.getUserId()).get();
        } catch (InterruptedException | CouldNotPerformException | ExecutionException ex) {
            ExceptionPrinter.printHistory(ex, LOGGER, LogLevel.ERROR);
        }

        return false;
    }

    /**
     * Renew the ticket for the current session by validating it at the authenticator controller.
     * This method is used to keep a user logged in by renewing the ticket before a session runs out.
     *
     * @throws CouldNotPerformException if the ticket could not be renewed
     */
    private synchronized void renewTicket() throws CouldNotPerformException {
        // validate that someone is logged in
        if (!this.isLoggedIn()) {
            throw new CouldNotPerformException("Could not renew ticket because not one is logged in");
        }

        // perform a request with the current ticket
        try {
            // initialize current ticket for a request
            TicketAuthenticatorWrapper request = AuthenticationClientHandler.initServiceServerRequest(this.sessionKey, this.ticketAuthenticatorWrapper);
            // perform the request
            TicketAuthenticatorWrapper response = CachedAuthenticationRemote.getRemote().validateClientServerTicket(request).get();
            // validate response and set as current ticket
            ticketAuthenticatorWrapper = AuthenticationClientHandler.handleServiceServerResponse(this.sessionKey, request, response);
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

            pattern = Pattern.compile("SessionExpiredException: (.*)[\n\r]");
            matcher = pattern.matcher(cause.getMessage());

            if (matcher.find()) {
                throw new SessionExpiredException(matcher.group(1));
            }

            ExceptionPrinter.printHistory(cause, LOGGER, LogLevel.ERROR);
            throw new CouldNotPerformException("Internal server error.", cause);
        }
    }

    /**
     * Logout and re-login what is possible while skipping notifications.
     *
     * @throws CouldNotPerformException if logging in again fails
     */
    public synchronized void reLogin() throws CouldNotPerformException {
        // skip notifications
        skipNotification = true;
        try {
            // save if user stayed logged in
            final boolean stayLoggedIn = ticketRenewalTask != null && !ticketRenewalTask.isDone();
            // save user and client id
            final UserClientPair userClientPair = getUserClientPair();
            // logout);
            logout();
            if (!userClientPair.getUserId().isEmpty() && credentialStore.hasEntry(userClientPair.getUserId())) {
                // if user was save in store log him in again, this is valid because logout will login a client
                // if a user was logged in at a client
                login(userClientPair.getUserId(), stayLoggedIn);
            } else {
                // user was not logged in so login the client again if possible
                if (!userClientPair.getClientId().isEmpty() && credentialStore.hasEntry(userClientPair.getClientId())) {
                    login(userClientPair.getClientId(), stayLoggedIn);
                }
            }
        } catch (CouldNotPerformException ex) {
            throw ex;
        } finally {
            // enable notifications again
            skipNotification = false;
        }

        notifyLoginObserver();
    }

    /**
     * Changes the login credentials for a given user.
     *
     * @param userId         ID of the user / client whose credentials should be changed.
     * @param oldCredentials Old credentials, needed for verification.
     * @param newCredentials New credentials to be set.
     *
     * @throws CouldNotPerformException In case of a communication error between client and server.
     */
    public synchronized void changePassword(final String userId, final String oldCredentials, final String newCredentials) throws CouldNotPerformException {
        if (!this.isLoggedIn()) {
            throw new CouldNotPerformException("Please log in first!");
        }

        try {
            ticketAuthenticatorWrapper = AuthenticationClientHandler.initServiceServerRequest(sessionKey, ticketAuthenticatorWrapper);
            byte[] oldHash = EncryptionHelper.hash(oldCredentials);
            byte[] newHash = EncryptionHelper.hash(newCredentials);

            LoginCredentialsChange loginCredentialsChange = LoginCredentialsChange.newBuilder()
                    .setId(userId)
                    .setOldCredentials(EncryptionHelper.encryptSymmetric(oldHash, sessionKey))
                    .setNewCredentials(EncryptionHelper.encryptSymmetric(newHash, sessionKey))
                    .setSymmetric(true)
                    .build();

            AuthenticatedServiceProcessor.requestAuthenticatedAction(loginCredentialsChange, LoginCredentialsChange.class, this, authenticatedValue -> CachedAuthenticationRemote.getRemote().changeCredentials(authenticatedValue)).get();
        } catch (CouldNotPerformException ex) {
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

    public synchronized String getCredentialHashFromLocalStore(String userId) throws CouldNotPerformException {
        if (!this.isLoggedIn()) {
            throw new CouldNotPerformException("Please log in first!");
        }

        return credentialStore.getEntry(userId).getCredentials().toStringUtf8();
    }

    public synchronized boolean hasCredentials() {
        return !credentialStore.isEmpty();
    }

    /**
     * Registers a client. Automatically generate a key pair and save the private key in the credential store of
     * the session manager.
     *
     * @param clientId the id of the client to be registered with a asymmetric encryption
     *
     * @throws org.openbase.jul.exception.CouldNotPerformException if the client could not registered
     */
    public synchronized void registerClient(final String clientId) throws CouldNotPerformException {
        final KeyPair keyPair = EncryptionHelper.generateKeyPair();
        final LoginCredentials loginCredentials = LoginCredentials.newBuilder().setId(clientId).setCredentials(ByteString.copyFrom(keyPair.getPublic().getEncoded())).setSymmetric(false).setAdmin(false).build();
        this.internalRegister(loginCredentials);
        this.credentialStore.addEntry(clientId, loginCredentials);
    }

    public synchronized boolean hasCredentialsForId(final String id) {
        return this.credentialStore.hasEntry(id) || this.credentialStore.hasEntry("@" + id);
    }

    /**
     * Registers a user.
     *
     * @param userId   the id of the user
     * @param password the password of the user
     * @param isAdmin  flag if user should be an administrator
     *
     * @throws org.openbase.jul.exception.CouldNotPerformException if the user could not be registered
     */
    public synchronized void registerUser(final String userId, final String password, final boolean isAdmin) throws CouldNotPerformException {
        byte[] key = EncryptionHelper.hash(password);
        final LoginCredentials loginCredentials = LoginCredentials.newBuilder().setId(userId).setAdmin(isAdmin).setSymmetric(true).setCredentials(ByteString.copyFrom(key)).build();
        this.internalRegister(loginCredentials);
    }

    /**
     * Registers a user or client.
     * Assumes an administrator who has permissions for this already exists and is logged in with current session manager.
     * Overwrites duplicate entries on client, if entry to be registered does not exist on server.
     * Does not overwrite duplicate entries on client, if entry does exist on server.
     *
     * @param loginCredentials type containing all information for the user/client to be registered.
     *
     * @throws org.openbase.jul.exception.CouldNotPerformException
     */
    private void internalRegister(final LoginCredentials loginCredentials) throws CouldNotPerformException {
        if (!this.isLoggedIn()) {
            throw new CouldNotPerformException("Please log in first!");
        }

        try {
            AuthenticatedServiceProcessor.requestAuthenticatedAction(loginCredentials, LoginCredentials.class, this, authenticatedValue -> CachedAuthenticationRemote.getRemote().register(authenticatedValue)).get();
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
            AuthenticatedServiceProcessor.requestAuthenticatedAction(id, String.class, this, authenticatedValue -> CachedAuthenticationRemote.getRemote().removeUser(authenticatedValue)).get();
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

    public synchronized void setAdministrator(final String id, boolean isAdmin) throws CouldNotPerformException {
        if (!this.isLoggedIn()) {
            throw new CouldNotPerformException("Please log in first!");
        }

        final LoginCredentials loginCredentials = LoginCredentials.newBuilder().setId(id).setAdmin(isAdmin).build();
        try {
            AuthenticatedServiceProcessor.requestAuthenticatedAction(loginCredentials, LoginCredentials.class, this, authenticatedValue -> CachedAuthenticationRemote.getRemote().setAdministrator(authenticatedValue)).get();
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
     * If a user and/or client is logged in, this returns the concatenation of both IDs.
     *
     * @return userId@clientId
     */
    public UserClientPair getUserClientPair() {
        return userClientPair.build();
    }

    public void addLoginObserver(final Observer<SessionManager, UserClientPair> observer) {
        loginObservable.addObserver(observer);
    }

    public void removeLoginObserver(final Observer<SessionManager, UserClientPair> observer) {
        loginObservable.removeObserver(observer);
    }

    @Override
    public void shutdown() {
        completeLogout();
    }
}
