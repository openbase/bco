package org.openbase.bco.authentication.core;

/*-
 * #%L
 * BCO Authentication Core
 * %%
 * Copyright (C) 2017 - 2018 openbase.org
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.attribute.PosixFilePermission;
import java.security.KeyPair;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Future;
import org.apache.commons.lang.RandomStringUtils;
import javax.crypto.BadPaddingException;
import org.openbase.bco.authentication.lib.AuthenticationServerHandler;
import org.openbase.bco.authentication.lib.EncryptionHelper;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InitializationException;
import org.openbase.jul.iface.Launchable;
import org.openbase.jul.iface.VoidInitializable;
import org.openbase.bco.authentication.lib.jp.JPAuthenticationScope;
import org.openbase.jps.core.JPService;
import org.openbase.jps.exception.JPNotAvailableException;
import org.openbase.jul.extension.rsb.com.NotInitializedRSBLocalServer;
import org.openbase.jul.extension.rsb.com.RPCHelper;
import org.openbase.jul.extension.rsb.com.RSBFactoryImpl;
import org.openbase.jul.extension.rsb.com.RSBSharedConnectionConfig;
import org.openbase.jul.extension.rsb.iface.RSBLocalServer;
import org.openbase.jul.schedule.GlobalCachedExecutorService;
import org.openbase.jul.schedule.WatchDog;
import rst.domotic.authentication.TicketAuthenticatorWrapperType.TicketAuthenticatorWrapper;
import rst.domotic.authentication.TicketSessionKeyWrapperType.TicketSessionKeyWrapper;
import org.openbase.bco.authentication.lib.AuthenticationService;
import org.openbase.bco.authentication.lib.CredentialStore;
import org.openbase.bco.authentication.lib.ExceptionReporter;
import org.openbase.bco.authentication.lib.AuthenticatedServerManager;
import org.openbase.bco.authentication.lib.exception.SessionExpiredException;
import org.openbase.bco.authentication.lib.jp.JPCredentialsDirectory;
import org.openbase.bco.authentication.lib.jp.JPSessionTimeout;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.exception.PermissionDeniedException;
import org.openbase.jul.exception.RejectedException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.exception.printer.LogLevel;
import org.slf4j.LoggerFactory;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rst.domotic.authentication.AuthenticatedValueType.AuthenticatedValue;
import rst.domotic.authentication.AuthenticatorType.Authenticator;
import rst.domotic.authentication.LoginCredentialsChangeType.LoginCredentialsChange;
import rst.domotic.authentication.TicketType.Ticket;

/**
 *
 * @author <a href="mailto:thuxohl@techfak.uni-bielefeld.de">Tamino Huxohl</a>
 */
public class AuthenticatorController implements AuthenticationService, Launchable<Void>, VoidInitializable {

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(TicketSessionKeyWrapper.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(TicketAuthenticatorWrapper.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(LoginCredentialsChange.getDefaultInstance()));
    }

    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(AuthenticatorController.class);
    private static final String STORE_FILENAME = "server_credential_store.json";

    private RSBLocalServer server;
    private WatchDog serverWatchDog;

    private final byte[] ticketGrantingServiceSecretKey;
    private final byte[] serviceServerSecretKey;

    private final CredentialStore store;

    private static String initialPassword;

    private final long ticketValidityTime;

    public AuthenticatorController() throws InitializationException {
        this(new CredentialStore(STORE_FILENAME), EncryptionHelper.generateKey());
    }

    public AuthenticatorController(CredentialStore store) throws InitializationException {
        this(store, EncryptionHelper.generateKey());
    }

    public AuthenticatorController(byte[] serviceServerPrivateKey) throws InitializationException {
        this(new CredentialStore(STORE_FILENAME), serviceServerPrivateKey);
    }

    public AuthenticatorController(CredentialStore store, byte[] serviceServerPrivateKey) throws InitializationException {
        this.server = new NotInitializedRSBLocalServer();

        this.ticketGrantingServiceSecretKey = EncryptionHelper.generateKey();
        this.serviceServerSecretKey = serviceServerPrivateKey;

        this.store = store;

        try {
            this.ticketValidityTime = JPService.getProperty(JPSessionTimeout.class).getValue();
        } catch (JPNotAvailableException ex) {
            throw new InitializationException(AuthenticatorController.class, ex);
        }
    }

    @Override
    public void init() throws InitializationException, InterruptedException {
        try {
            server = RSBFactoryImpl.getInstance().createSynchronizedLocalServer(JPService.getProperty(JPAuthenticationScope.class).getValue(), RSBSharedConnectionConfig.getParticipantConfig());

            // register rpc methods.
            RPCHelper.registerInterface(AuthenticationService.class, this, server);

            serverWatchDog = new WatchDog(server, "AuthenticatorWatchDog");
        } catch (JPNotAvailableException | CouldNotPerformException ex) {
            throw new InitializationException(this, ex);
        }

        store.init();
    }

    @Override
    public void activate() throws CouldNotPerformException, InterruptedException {
        if (store.isEmpty()) {
            // Generate private/public key pair for service servers.
            KeyPair keyPair = EncryptionHelper.generateKeyPair();
            store.addCredentials(CredentialStore.SERVICE_SERVER_ID, keyPair.getPublic().getEncoded(), false);
            try {
                File privateKeyFile = new File(JPService.getProperty(JPCredentialsDirectory.class).getValue(), AuthenticatedServerManager.SERVICE_SERVER_PRIVATE_KEY_FILENAME);
                try (FileOutputStream outputStream = new FileOutputStream(privateKeyFile)) {
                    outputStream.write(keyPair.getPrivate().getEncoded());
                    outputStream.flush();
                }
                CredentialStore.protectFile(privateKeyFile);
            } catch (JPNotAvailableException ex) {
                throw new CouldNotPerformException("Could not load property.", ex);
            } catch (IOException ex) {
                throw new CouldNotPerformException("Could not write private key.", ex);
            }
        }

        if (store.hasOnlyServiceServer() || JPService.testMode()) {
            // Generate initial password.
            initialPassword = RandomStringUtils.randomAlphanumeric(15);
        }

        serverWatchDog.activate();
    }

    @Override
    public void deactivate() throws CouldNotPerformException, InterruptedException {
        if (serverWatchDog != null) {
            serverWatchDog.deactivate();
        }

        store.shutdown();
    }

    @Override
    public boolean isActive() {
        if (serverWatchDog != null) {
            return serverWatchDog.isActive();
        } else {
            return false;
        }
    }

    public void waitForActivation() throws CouldNotPerformException, InterruptedException {
        try {
            serverWatchDog.waitForServiceActivation();
        } catch (final CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not wait for activation!", ex);
        }
    }

    @Override
    public Future<TicketSessionKeyWrapper> requestTicketGrantingTicket(String id) throws CouldNotPerformException {
        return GlobalCachedExecutorService.submit(() -> {
            try {
                String[] split = id.split("@", 2);
                byte[] userKey = null;
                byte[] clientKey = null;
                boolean isUser = split[0].length() > 0;
                boolean isClient = split.length > 1 && split[1].length() > 0;

                if (isUser) {
                    userKey = store.getCredentials(split[0].trim());
                }
                if (isClient) {
                    clientKey = store.getCredentials(split[1].trim());
                }

                return AuthenticationServerHandler.handleKDCRequest(id, userKey, clientKey, "", ticketGrantingServiceSecretKey, ticketValidityTime);
            } catch (NotAvailableException ex) {
                ExceptionPrinter.printHistory(ex, LOGGER, LogLevel.WARN);
                ExceptionReporter.getInstance().report(ex);
                throw new NotAvailableException(id);
            } catch (InterruptedException | CouldNotPerformException | IOException ex) {
                ExceptionPrinter.printHistory(ex, LOGGER, LogLevel.ERROR);
                throw new CouldNotPerformException("Internal server error. Please try again.");
            }
        });
    }

    @Override
    public Future<TicketSessionKeyWrapper> requestClientServerTicket(TicketAuthenticatorWrapper ticketAuthenticatorWrapper) throws CouldNotPerformException {
        return GlobalCachedExecutorService.submit(() -> {
            try {
                return AuthenticationServerHandler.handleTGSRequest(ticketGrantingServiceSecretKey, serviceServerSecretKey, ticketAuthenticatorWrapper, ticketValidityTime);
            } catch (RejectedException | BadPaddingException ex) {
                ExceptionPrinter.printHistory(ex, LOGGER, LogLevel.WARN);
                ExceptionReporter.getInstance().report(ex);
                throw new RejectedException(ex.getMessage());
            } catch (IOException ex) {
                ExceptionPrinter.printHistory(ex, LOGGER, LogLevel.ERROR);
                throw new CouldNotPerformException("Internal server error. Please try again.");
            }
        });
    }

    @Override
    public Future<TicketAuthenticatorWrapper> validateClientServerTicket(TicketAuthenticatorWrapper ticketAuthenticatorWrapper) throws CouldNotPerformException {
        return GlobalCachedExecutorService.submit(() -> {
            try {
                return AuthenticationServerHandler.handleSSRequest(serviceServerSecretKey, ticketAuthenticatorWrapper, ticketValidityTime);
                // TODO: Validate that user/clientId exists in store. Otherwise somebody could still be logged in after being removed from store
            } catch (SessionExpiredException ex) {
                throw ex;
            } catch (RejectedException | BadPaddingException ex) {
                ExceptionPrinter.printHistory(ex, LOGGER, LogLevel.WARN);
                ExceptionReporter.getInstance().report(ex);
                throw new RejectedException(ex.getMessage());
            } catch (IOException ex) {
                ExceptionPrinter.printHistory(ex, LOGGER, LogLevel.ERROR);
                throw new CouldNotPerformException("Internal server error. Please try again.");
            }
        });
    }

    @Override
    public Future<TicketAuthenticatorWrapper> changeCredentials(LoginCredentialsChange loginCredentialsChange) throws CouldNotPerformException, RejectedException, PermissionDeniedException {
        return GlobalCachedExecutorService.submit(() -> {
            try {
                // Validate the given authenticator and ticket.
                TicketAuthenticatorWrapper wrapper = loginCredentialsChange.getTicketAuthenticatorWrapper();
                TicketAuthenticatorWrapper response = AuthenticationServerHandler.handleSSRequest(serviceServerSecretKey, wrapper, ticketValidityTime);

                // Decrypt ticket, authenticator and credentials.
                Ticket clientServerTicket = EncryptionHelper.decryptSymmetric(wrapper.getTicket(), serviceServerSecretKey, Ticket.class);
                byte[] clientServerSessionKey = clientServerTicket.getSessionKeyBytes().toByteArray();
                Authenticator authenticator = EncryptionHelper.decryptSymmetric(wrapper.getAuthenticator(), clientServerSessionKey, Authenticator.class);
                byte[] oldCredentials = EncryptionHelper.decryptSymmetric(loginCredentialsChange.getOldCredentials(), clientServerSessionKey, byte[].class);
                byte[] newCredentials = EncryptionHelper.decryptSymmetric(loginCredentialsChange.getNewCredentials(), clientServerSessionKey, byte[].class);
                String userId = loginCredentialsChange.getId();
                String[] split = authenticator.getClientId().split("@", 2);
                String authenticatorUserId = split[0];

                // Allow users to change their own password and admins to change passwords for other users.
                if (!userId.equals(authenticatorUserId) && !store.isAdmin(authenticatorUserId)) {
                    throw new PermissionDeniedException("You are not permitted to perform this action.");
                }

                // Check if the given credentials correspond to the old one.
                if (!Arrays.equals(oldCredentials, store.getCredentials(userId))) {
                    throw new RejectedException("The old password is wrong.");
                }

                // Update credentials.
                store.setCredentials(userId, newCredentials);

                return response;
            } catch (RejectedException | BadPaddingException ex) {
                ExceptionPrinter.printHistory(ex, LOGGER, LogLevel.WARN);
                ExceptionReporter.getInstance().report(ex);
                throw new RejectedException(ex.getMessage());
            } catch (IOException ex) {
                ExceptionPrinter.printHistory(ex, LOGGER, LogLevel.ERROR);
                throw new CouldNotPerformException("Internal server error. Please try again.");
            }
        });
    }

    @Override
    public Future<TicketAuthenticatorWrapper> register(LoginCredentialsChange loginCredentialsChange) throws CouldNotPerformException, RejectedException, PermissionDeniedException {
        return GlobalCachedExecutorService.submit(() -> {
            try {
                if (initialPassword != null && (store.hasOnlyServiceServer() || JPService.testMode())) {
                    if (!loginCredentialsChange.hasId() || !loginCredentialsChange.hasNewCredentials()) {
                        throw new RejectedException("Cannot register first user, id and/or new credentials empty");
                    }

                    byte[] decryptedCredentials = EncryptionHelper.decryptSymmetric(loginCredentialsChange.getNewCredentials(), EncryptionHelper.hash(initialPassword), byte[].class);

                    this.store.addCredentials(loginCredentialsChange.getId(), decryptedCredentials, true);

                    initialPassword = null;
                    return null;
                }

                // validate the given authenticator and ticket.
                TicketAuthenticatorWrapper wrapper = loginCredentialsChange.getTicketAuthenticatorWrapper();
                TicketAuthenticatorWrapper response = AuthenticationServerHandler.handleSSRequest(serviceServerSecretKey, wrapper, ticketValidityTime);

                // decrypt ticket and authenticator
                Ticket clientServerTicket = EncryptionHelper.decryptSymmetric(wrapper.getTicket(), serviceServerSecretKey, Ticket.class);
                byte[] clientServerSessionKey = clientServerTicket.getSessionKeyBytes().toByteArray();

                Authenticator authenticator = EncryptionHelper.decryptSymmetric(wrapper.getAuthenticator(), clientServerSessionKey, Authenticator.class);
                String authenticatorUserId = authenticator.getClientId().split("@", 2)[0];
                String newId = loginCredentialsChange.getId();

                // check if performing user is admin
                if (!store.isAdmin(authenticatorUserId)) {
                    throw new PermissionDeniedException("You are not permitted to perform this action.");
                }

                // don't allow administrators to overwrite themselves
                // don't allow to overwrite somebody else
                if (newId.equals(authenticatorUserId) || store.hasEntry(newId)) {
                    throw new CouldNotPerformException("You cannot register an existing user.");
                }

                // decrypt key
                byte[] key = EncryptionHelper.decryptSymmetric(loginCredentialsChange.getNewCredentials(), clientServerSessionKey, byte[].class);

                // register
                store.addCredentials(newId, key, loginCredentialsChange.getAdmin());

                return response;
            } catch (RejectedException | BadPaddingException ex) {
                ExceptionPrinter.printHistory(ex, LOGGER, LogLevel.WARN);
                ExceptionReporter.getInstance().report(ex);
                throw new RejectedException(ex.getMessage());
            } catch (IOException ex) {
                ExceptionPrinter.printHistory(ex, LOGGER, LogLevel.ERROR);
                throw new CouldNotPerformException("Internal server error. Please try again.");
            }
        });
    }

    @Override
    public Future<TicketAuthenticatorWrapper> removeUser(LoginCredentialsChange loginCredentialsChange) throws CouldNotPerformException, RejectedException, PermissionDeniedException {
        return GlobalCachedExecutorService.submit(() -> {
            try {
                // validate the given authenticator and ticket.
                TicketAuthenticatorWrapper wrapper = loginCredentialsChange.getTicketAuthenticatorWrapper();
                TicketAuthenticatorWrapper response = AuthenticationServerHandler.handleSSRequest(serviceServerSecretKey, wrapper, ticketValidityTime);

                // decrypt ticket and authenticator
                Ticket clientServerTicket = EncryptionHelper.decryptSymmetric(wrapper.getTicket(), serviceServerSecretKey, Ticket.class);
                byte[] clientServerSessionKey = clientServerTicket.getSessionKeyBytes().toByteArray();

                Authenticator authenticator = EncryptionHelper.decryptSymmetric(wrapper.getAuthenticator(), clientServerSessionKey, Authenticator.class);
                String authenticatorUserId = authenticator.getClientId().split("@", 2)[0];
                String idToRemove = loginCredentialsChange.getId();

                // check if performing user is admin
                if (!this.store.isAdmin(authenticatorUserId)) {
                    throw new PermissionDeniedException("You are not permitted to perform this action.");
                }

                // don't allow administrators to remove themselves
                if (idToRemove.equals(authenticatorUserId)) {
                    throw new CouldNotPerformException("You cannot remove yourself.");
                }

                // don't allow administrators to remove nonexistent entry
                if (!this.store.hasEntry(idToRemove)) {
                    throw new CouldNotPerformException("Given user does not exist.");
                }

                // remove
                this.store.removeEntry(idToRemove);

                return response;
            } catch (RejectedException | BadPaddingException ex) {
                ExceptionPrinter.printHistory(ex, LOGGER, LogLevel.WARN);
                ExceptionReporter.getInstance().report(ex);
                throw new RejectedException(ex.getMessage());
            } catch (IOException ex) {
                ExceptionPrinter.printHistory(ex, LOGGER, LogLevel.ERROR);
                throw new CouldNotPerformException("Internal server error. Please try again.");
            }
        });
    }

    @Override
    public Future<TicketAuthenticatorWrapper> setAdministrator(LoginCredentialsChange loginCredentialsChange) throws CouldNotPerformException, RejectedException, PermissionDeniedException {
        return GlobalCachedExecutorService.submit(() -> {
            try {
                // validate the given authenticator and ticket.
                TicketAuthenticatorWrapper wrapper = loginCredentialsChange.getTicketAuthenticatorWrapper();
                TicketAuthenticatorWrapper response = AuthenticationServerHandler.handleSSRequest(serviceServerSecretKey, wrapper, ticketValidityTime);

                // decrypt ticket and authenticator
                Ticket clientServerTicket = EncryptionHelper.decryptSymmetric(wrapper.getTicket(), serviceServerSecretKey, Ticket.class);
                byte[] clientServerSessionKey = clientServerTicket.getSessionKeyBytes().toByteArray();

                Authenticator authenticator = EncryptionHelper.decryptSymmetric(wrapper.getAuthenticator(), clientServerSessionKey, Authenticator.class);
                String authenticatorUserId = authenticator.getClientId().split("@", 2)[0];
                String newId = loginCredentialsChange.getId();

                // check if performing user is admin
                if (!this.store.isAdmin(authenticatorUserId)) {
                    throw new PermissionDeniedException("You are not permitted to perform this action.");
                }

                // don't allow administrators to change administrator status of themselves
                if (newId.equals(authenticatorUserId)) {
                    throw new CouldNotPerformException("You cannot register an existing user.");
                }

                // don't allow administrators to change administrator status of nonexistent entry
                if (!this.store.hasEntry(newId)) {
                    throw new CouldNotPerformException("Given user does not exist.");
                }

                // register
                this.store.setAdmin(newId, loginCredentialsChange.getAdmin());

                return response;
            } catch (RejectedException | BadPaddingException ex) {
                ExceptionPrinter.printHistory(ex, LOGGER, LogLevel.WARN);
                ExceptionReporter.getInstance().report(ex);
                throw new RejectedException(ex.getMessage());
            } catch (IOException ex) {
                ExceptionPrinter.printHistory(ex, LOGGER, LogLevel.ERROR);
                throw new CouldNotPerformException("Internal server error. Please try again.");
            }
        });
    }

    @Override
    public Future<AuthenticatedValue> requestServiceServerSecretKey(TicketAuthenticatorWrapper ticketAuthenticatorWrapper) throws CouldNotPerformException {
        return GlobalCachedExecutorService.submit(() -> {
            try {
                // Validate the given authenticator and ticket.
                TicketAuthenticatorWrapper response = AuthenticationServerHandler.handleSSRequest(serviceServerSecretKey, ticketAuthenticatorWrapper, ticketValidityTime);

                // Decrypt ticket, authenticator and credentials.
                Ticket clientServerTicket = EncryptionHelper.decryptSymmetric(ticketAuthenticatorWrapper.getTicket(), serviceServerSecretKey, Ticket.class);
                byte[] clientServerSessionKey = clientServerTicket.getSessionKeyBytes().toByteArray();
                Authenticator authenticator = EncryptionHelper.decryptSymmetric(ticketAuthenticatorWrapper.getAuthenticator(), clientServerSessionKey, Authenticator.class);

                if (!authenticator.getClientId().equals("@" + CredentialStore.SERVICE_SERVER_ID)) {
                    throw new RejectedException("Client[" + authenticator.getClientId() + "] is not authorized to request the ServiceServerSecretKey");
                }

                AuthenticatedValue.Builder authenticatedValue = AuthenticatedValue.newBuilder();
                authenticatedValue.setTicketAuthenticatorWrapper(response);
                authenticatedValue.setValue(EncryptionHelper.encryptSymmetric(this.serviceServerSecretKey, clientServerSessionKey));

                return authenticatedValue.build();
            } catch (RejectedException | BadPaddingException ex) {
                ExceptionPrinter.printHistory(ex, LOGGER, LogLevel.WARN);
                ExceptionReporter.getInstance().report(ex);
                throw new RejectedException(ex.getMessage());
            } catch (IOException ex) {
                ExceptionPrinter.printHistory(ex, LOGGER, LogLevel.ERROR);
                throw new CouldNotPerformException("Internal server error. Please try again.");
            }
        });
    }

    @Override
    public Future<Boolean> isAdmin(String userId) throws NotAvailableException {
        return GlobalCachedExecutorService.submit(() -> {
            return store.isAdmin(userId);
        });
    }

    /**
     * Get the initial password which is randomly generated on startup with an empty
     * store. Else it is null and will also be reset to null after registration of the
     * first user.
     *
     * @return the password required for the registration of the initial user
     */
    public static String getInitialPassword() {
        return initialPassword;
    }

    @Override
    public Future<Boolean> hasUser(String userId) {
        return GlobalCachedExecutorService.submit(() -> {
            return store.hasEntry(userId);
        });
    }
}
