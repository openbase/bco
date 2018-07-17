package org.openbase.bco.authentication.lib;

/*-
 * #%L
 * BCO Authentication Library
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
import org.slf4j.LoggerFactory;
import rst.domotic.authentication.AuthenticatorType.Authenticator;
import rst.domotic.authentication.LoginCredentialsChangeType.LoginCredentialsChange;
import rst.domotic.authentication.TicketAuthenticatorWrapperType.TicketAuthenticatorWrapper;
import rst.domotic.authentication.TicketSessionKeyWrapperType.TicketSessionKeyWrapper;

import java.security.KeyPair;
import java.util.List;
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
    private final ObservableImpl<String> loginObservable;
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
     * The id of the client currently logged in.
     */
    private String clientId;
    /**
     * The id of the user currently logged in.
     */
    private String userId;
    /**
     * Future of a task that renews the ticket of a user if he wants to stay logged in.
     */
    private ScheduledFuture ticketRenewalTask;

    /**
     * Create a session manager with the default credential store.
     */
    public SessionManager() {
        this(new CredentialStore(STORE_FILENAME));
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
            LOGGER.warn("Could not register session manager shutdown hook", ex);
        }
        // create login observable
        this.loginObservable = new ObservableImpl<>();
        // add executor service so that it is not waited for notifications and so that they are done in parallel
        this.loginObservable.setExecutorService(GlobalCachedExecutorService.getInstance().getExecutorService());
        // save and init credential store
        this.credentialStore = credentialStore;
        try {
            this.credentialStore.init();
        } catch (CouldNotPerformException ex) {
            LOGGER.warn("Could not init credential store for session manager", ex);
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
     * @throws RejectedException if the ticket could not be initialized
     */
    public TicketAuthenticatorWrapper initializeServiceServerRequest() throws RejectedException {
        try {
            return AuthenticationClientHandler.initServiceServerRequest(this.getSessionKey(), this.getTicketAuthenticatorWrapper());
        } catch (CouldNotPerformException ex) {
            throw new RejectedException("Initializing request rejected", ex);
        }
    }

    /**
     * Perform a login for a given userId and password.
     *
     * @param userId   identifier of the user
     * @param password password of the user
     * @throws CouldNotPerformException if the user could not be logged in using the password
     */
    public synchronized void login(final String userId, final String password) throws CouldNotPerformException {
        login(userId, password, false);
    }

    /**
     * Perform a login for a given userId and password.
     *
     * @param userId       identifier of the user
     * @param password     password of the user
     * @param stayLoggedIn flag determining if the session should automatically be renewed before it times out
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
     * @throws CouldNotPerformException if the user could not be logged in with the given password
     */
    public synchronized void login(final String userId, final String password, final boolean stayLoggedIn, final boolean rememberPassword) throws CouldNotPerformException {
        final byte[] credentials = EncryptionHelper.hash(password);
        if (rememberPassword) {
            credentialStore.setCredentials(userId, credentials);
        }
        internalLogin(userId, credentials, stayLoggedIn, true);
    }

    /**
     * Perform a login for a given user or client by id.
     *
     * @param clientIdOrUserId identifier of the user or client
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
     * @throws NotAvailableException    if no entry for an according client or user could be found in the store
     * @throws CouldNotPerformException if logging in failed
     */
    public synchronized void login(final String clientIdOrUserId, final boolean stayLoggedIn) throws CouldNotPerformException, NotAvailableException {
        boolean isUser;
        byte[] credentials;
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
     * @param id          Identifier of the user or client
     * @param credentials Password or private key of the user or client
     * @return Returns true if login successful
     * @throws NotAvailableException    If the entered clientId could not be found.
     * @throws CouldNotPerformException In case of a communication error between client and server.
     */
    private synchronized void internalLogin(final String id, final byte[] credentials, final boolean stayLoggedIn, final boolean isUser) throws CouldNotPerformException, NotAvailableException {
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
            if (id.equals(this.userId) || id.equals(this.clientId)) {
                return;
            }

            // cancel current ticket renewal task
            if (ticketRenewalTask != null && !ticketRenewalTask.isDone()) {
                ticketRenewalTask.cancel(true);
            }

            // if new client is logged in while a user is logged in the user has to be logged out
            if (userId != null && !isUser) {
                this.logout();
            }
        }

        // save the new id
        if (isUser) {
            this.userId = id;
        } else {
            this.clientId = id;
        }

        // resolve user at client id and credentials
        final String userAtClientId = getUserAtClientId();
        byte[] userKey = null;
        byte[] clientKey = null;
        if (isUser) {
            // user is logged in so the parameters are his credentials
            userKey = credentials;

            // if client was logged in get its credentials from the store
            if (clientId != null) {
                clientKey = credentialStore.getCredentials("@" + clientId);
            }
        } else {
            // client is logged in so the parameters are his credentials
            clientKey = credentials;
        }

        try {
            // request ticket granting ticket
            TicketSessionKeyWrapper ticketSessionKeyWrapper = CachedAuthenticationRemote.getRemote().requestTicketGrantingTicket(userAtClientId).get();
            // handle response
            List<Object> list = AuthenticationClientHandler.handleKeyDistributionCenterResponse(userAtClientId, userKey, clientKey, ticketSessionKeyWrapper);
            final TicketAuthenticatorWrapper wrapper = (TicketAuthenticatorWrapper) list.get(0); // save at somewhere temporarily
            final byte[] ticketGrantingServiceSessionKey = (byte[]) list.get(1); // save TGS session key somewhere on client side

            // request client server ticket
            ticketSessionKeyWrapper = CachedAuthenticationRemote.getRemote().requestClientServerTicket(wrapper).get();
            // handle response
            list = AuthenticationClientHandler.handleTicketGrantingServiceResponse(userAtClientId, ticketGrantingServiceSessionKey, ticketSessionKeyWrapper);
            this.ticketAuthenticatorWrapper = (TicketAuthenticatorWrapper) list.get(0); // save at somewhere temporarily
            this.sessionKey = (byte[]) list.get(1); // save SS session key somewhere on client side

            try {
                loginObservable.notifyObservers(getUserAtClientId());
            } catch (CouldNotPerformException ex) {
                LOGGER.warn("Could not notify login to observer", ex);
            }

            // user wants to stay logged or is a client so trigger a ticket renewal task
            if (stayLoggedIn || !isUser) {
                try {
                    final Long sessionTimeout = JPService.getProperty(JPSessionTimeout.class).getValue();
                    final long delay = (long) ((3 * sessionTimeout) / 4.0d);
                    ticketRenewalTask = GlobalScheduledExecutorService.scheduleWithFixedDelay(() -> {
                        try {
                            renewTicket();
                        } catch (CouldNotPerformException ex) {
                            LOGGER.warn("Could not renew ticket", ex);
                        }
                    }, delay, delay, TimeUnit.MILLISECONDS);
                } catch (JPNotAvailableException ex) {
                    LOGGER.warn("Could not start ticket renewal task", ex);
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
        if (this.userId != null) {
            this.userId = null;

            // if a client was logged in additionally, log him in again
            if (clientId != null) {
                try {
                    login(clientId);
                    // return because the login notifies observer already
                    return;
                } catch (CouldNotPerformException ex) {
                    LOGGER.warn("Could not login as client again after user logout", ex);
                }
            }
        } else if (this.clientId != null) {
            // only a client has been logged in so clear its id
            this.clientId = null;
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
        userId = null;
        clientId = null;
        sessionKey = null;
        ticketAuthenticatorWrapper = null;
        notifyLoginObserver();
    }

    /**
     * Method notifies login observer with the current user at client id.
     */
    private void notifyLoginObserver() {
        try {
            loginObservable.notifyObservers(getUserAtClientId());
        } catch (CouldNotPerformException ex) {
            LOGGER.warn("Could not notify logout to observer", ex);
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
                throw new SessionExpiredException();
            }

            ExceptionPrinter.printHistory(cause, LOGGER, LogLevel.ERROR);
            throw new CouldNotPerformException("Internal server error.", cause);
        }
    }

    /**
     * Changes the login credentials for a given user.
     *
     * @param clientId       ID of the user / client whose credentials should be changed.
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

    /**
     * Registers a client.
     *
     * @param clientId the id of the client
     * @throws org.openbase.jul.exception.CouldNotPerformException if the client could not registered
     */
    public synchronized void registerClient(String clientId) throws CouldNotPerformException {
        KeyPair keyPair = EncryptionHelper.generateKeyPair();
        this.internalRegister(clientId, keyPair.getPublic().getEncoded(), false);
        this.credentialStore.setCredentials("@" + clientId, keyPair.getPrivate().getEncoded());
    }

    public synchronized boolean hasCredentialsForId(final String id) {
        return this.credentialStore.hasEntry(id);
    }

    /**
     * Registers a user.
     *
     * @param userId   the id of the user
     * @param password the password of the user
     * @param isAdmin  flag if user should be an administrator
     * @throws org.openbase.jul.exception.CouldNotPerformException if the user could not be registered
     */
    public synchronized void registerUser(final String userId, final String password, final boolean isAdmin) throws CouldNotPerformException {
        byte[] key = EncryptionHelper.hash(password);
        this.internalRegister(userId, key, isAdmin);
    }

    /**
     * Registers a user or client.
     * Assumes an administrator who has permissions for this already exists and is logged in with current session manager.
     * Overwrites duplicate entries on client, if entry to be registered does not exist on server.
     * Does not overwrite duplicate entries on client, if entry does exist on server.
     *
     * @param id      the id of the user
     * @param key     the password of the user
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

    public void addLoginObserver(Observer<String> observer) {
        loginObservable.addObserver(observer);
    }

    public void removeLoginObserver(Observer<String> observer) {
        loginObservable.removeObserver(observer);
    }

    public String getClientId() {
        return clientId;
    }

    @Override
    public void shutdown() {
        completeLogout();
    }
}
