package org.openbase.bco.authentication.core;

/*-
 * #%L
 * BCO Authentication Core
 * %%
 * Copyright (C) 2017 - 2019 openbase.org
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

import com.google.protobuf.ByteString;
import org.apache.commons.lang.RandomStringUtils;
import org.openbase.bco.authentication.lib.*;
import org.openbase.bco.authentication.lib.AuthenticatedServiceProcessor.InternalIdentifiedProcessable;
import org.openbase.bco.authentication.lib.AuthenticatedServiceProcessor.TicketValidator;
import org.openbase.bco.authentication.lib.exception.SessionExpiredException;
import org.openbase.bco.authentication.lib.jp.JPAuthenticationScope;
import org.openbase.bco.authentication.lib.jp.JPCredentialsDirectory;
import org.openbase.bco.authentication.lib.jp.JPSessionTimeout;
import org.openbase.jps.core.JPService;
import org.openbase.jps.exception.JPNotAvailableException;
import org.openbase.jul.communication.controller.RPCHelper;
import org.openbase.jul.exception.*;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.exception.printer.LogLevel;
import org.openbase.jul.extension.rsb.com.NotInitializedRSBLocalServer;
import org.openbase.jul.extension.rsb.com.RSBFactoryImpl;
import org.openbase.jul.extension.rsb.com.RSBSharedConnectionConfig;
import org.openbase.jul.extension.rsb.iface.RSBLocalServer;
import org.openbase.jul.extension.rsb.scope.ScopeTransformer;
import org.openbase.jul.iface.Launchable;
import org.openbase.jul.iface.VoidInitializable;
import org.openbase.jul.schedule.GlobalCachedExecutorService;
import org.openbase.jul.schedule.WatchDog;
import org.openbase.type.domotic.authentication.AuthenticatedValueType.AuthenticatedValue;
import org.openbase.type.domotic.authentication.LoginCredentialsChangeType.LoginCredentialsChange;
import org.openbase.type.domotic.authentication.LoginCredentialsType.LoginCredentials;
import org.openbase.type.domotic.authentication.LoginCredentialsType.LoginCredentials.Builder;
import org.openbase.type.domotic.authentication.TicketAuthenticatorWrapperType.TicketAuthenticatorWrapper;
import org.openbase.type.domotic.authentication.TicketSessionKeyWrapperType.TicketSessionKeyWrapper;
import org.openbase.type.domotic.authentication.UserClientPairType.UserClientPair;
import org.slf4j.LoggerFactory;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.security.KeyPair;
import java.util.concurrent.Future;

/**
 * @author <a href="mailto:thuxohl@techfak.uni-bielefeld.de">Tamino Huxohl</a>
 */
public class AuthenticatorController implements AuthenticationService, Launchable<Void>, VoidInitializable {
    // todo release: validate name (AuthenticatorController vs. AuthenticationRemote)
    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(TicketSessionKeyWrapper.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(TicketAuthenticatorWrapper.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(AuthenticatedValue.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(UserClientPair.getDefaultInstance()));
    }

    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(AuthenticatorController.class);
    private static final String STORE_FILENAME = "server_credential_store.json";
    private static final String TICKET_GRANTING_KEY = "ticket_granting_key";
    private static final String SERVICE_SERVER_SECRET_KEY = "service_server_secret_key";

    private RSBLocalServer server;
    private WatchDog serverWatchDog;

    private final CredentialStore credentialStore;

    private static String initialPassword;

    private final long ticketValidityTime;

    private byte[] ticketGrantingServiceSecretKey = null;
    private byte[] serviceServerSecretKey;

    public AuthenticatorController() throws InitializationException {
        this(new CredentialStore(), EncryptionHelper.generateKey());
    }

    public AuthenticatorController(CredentialStore credentialStore) throws InitializationException {
        this(credentialStore, EncryptionHelper.generateKey());
    }

    public AuthenticatorController(byte[] serviceServerPrivateKey) throws InitializationException {
        this(new CredentialStore(), serviceServerPrivateKey);
    }

    public AuthenticatorController(CredentialStore credentialStore, byte[] serviceServerPrivateKey) throws InitializationException {
        this.server = new NotInitializedRSBLocalServer();

        this.credentialStore = credentialStore;
        this.serviceServerSecretKey = serviceServerPrivateKey;

        try {
            this.ticketValidityTime = JPService.getProperty(JPSessionTimeout.class).getValue();
        } catch (JPNotAvailableException ex) {
            throw new InitializationException(AuthenticatorController.class, ex);
        }
    }

    @Override
    public void init() throws InitializationException, InterruptedException {
        try {
            server = RSBFactoryImpl.getInstance().createSynchronizedLocalServer(
                    ScopeTransformer.transform(JPService.getProperty(JPAuthenticationScope.class).getValue()),
                    RSBSharedConnectionConfig.getParticipantConfig());

            // register rpc methods.
            RPCHelper.registerInterface(AuthenticationService.class, this, server);

            serverWatchDog = new WatchDog(server, "AuthenticatorWatchDog");
        } catch (JPNotAvailableException | CouldNotPerformException ex) {
            throw new InitializationException(this, ex);
        }

        credentialStore.init(STORE_FILENAME);

        if (!credentialStore.hasEntry(TICKET_GRANTING_KEY)) {
            credentialStore.addCredentials(TICKET_GRANTING_KEY, EncryptionHelper.generateKey(), false, true);
        }

        if (!credentialStore.hasEntry(SERVICE_SERVER_SECRET_KEY)) {
            if (serviceServerSecretKey != null) {
                credentialStore.addCredentials(SERVICE_SERVER_SECRET_KEY, serviceServerSecretKey, false, true);
            } else {
                credentialStore.addCredentials(SERVICE_SERVER_SECRET_KEY, EncryptionHelper.generateKey(), false, true);
            }
        }

        try {
            ticketGrantingServiceSecretKey = credentialStore.getCredentials(TICKET_GRANTING_KEY).getCredentials().toByteArray();
            serviceServerSecretKey = credentialStore.getCredentials(SERVICE_SERVER_SECRET_KEY).getCredentials().toByteArray();
        } catch (NotAvailableException ex) {
            throw new InitializationException(this, ex);
        }
    }

    @Override
    public void activate() throws CouldNotPerformException, InterruptedException {
        if (!credentialStore.hasEntry(CredentialStore.SERVICE_SERVER_ID) || JPService.testMode()) {
            // Generate private/public key pair for service servers.
            final KeyPair keyPair = EncryptionHelper.generateKeyPair();
            credentialStore.addCredentials(CredentialStore.SERVICE_SERVER_ID, keyPair.getPublic().getEncoded(), false, false);
            try {
                final LoginCredentials loginCredentials = credentialStore.getEntry(CredentialStore.SERVICE_SERVER_ID).toBuilder().setCredentials(ByteString.copyFrom(keyPair.getPrivate().getEncoded())).build();
                File privateKeyFile = new File(JPService.getProperty(JPCredentialsDirectory.class).getValue(), AuthenticatedServerManager.SERVICE_SERVER_PRIVATE_KEY_FILENAME);
                try (FileOutputStream outputStream = new FileOutputStream(privateKeyFile)) {
                    outputStream.write(loginCredentials.toByteArray());
                    outputStream.flush();
                }
                AbstractProtectedStore.protectFile(privateKeyFile);
            } catch (JPNotAvailableException ex) {
                throw new CouldNotPerformException("Could not load property.", ex);
            } catch (IOException ex) {
                throw new CouldNotPerformException("Could not write private key.", ex);
            }
        }

        if (initialPasswordRequired() || JPService.testMode()) {
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

        credentialStore.shutdown();
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

    /**
     * Test if the initial password needs to be generated. This is the case if only three entries are in the credential
     * credentialStore. One for the service server client, one for the ticket granting key and one for the service server secret
     * key.
     *
     * @return if an initial password has to be generated.
     */
    private boolean initialPasswordRequired() {
        return (credentialStore.getSize() == 3 && credentialStore.hasEntry(CredentialStore.SERVICE_SERVER_ID)
                && credentialStore.hasEntry(TICKET_GRANTING_KEY) && credentialStore.hasEntry(SERVICE_SERVER_SECRET_KEY));
    }

    /**
     * {@inheritDoc}
     *
     * @param userClientPair {@inheritDoc}
     *
     * @return {@inheritDoc}
     */
    @Override
    public Future<TicketSessionKeyWrapper> requestTicketGrantingTicket(final UserClientPair userClientPair) {
        return GlobalCachedExecutorService.submit(() -> {
            try {
                // retrieve required credentials from credentialStore
                LoginCredentials userCredentials = null;
                LoginCredentials clientCredentials = null;
                if (!userClientPair.getUserId().isEmpty()) {
                    userCredentials = credentialStore.getCredentials(userClientPair.getUserId());
                }
                if (!userClientPair.getClientId().isEmpty()) {
                    clientCredentials = credentialStore.getCredentials(userClientPair.getClientId());
                }

                // handle request
                return AuthenticationServerHandler.handleKDCRequest(userClientPair, userCredentials, clientCredentials, ticketGrantingServiceSecretKey, ticketValidityTime);
            } catch (NotAvailableException ex) {
                ExceptionPrinter.printHistory(ex, LOGGER, LogLevel.WARN);
                ExceptionReporter.getInstance().report(ex);
                throw new NotAvailableException(ex.getMessage());
            } catch (CouldNotPerformException ex) {
                ExceptionPrinter.printHistory(ex, LOGGER, LogLevel.ERROR);
                throw new CouldNotPerformException("Internal server error. Please try again.");
            }
        });
    }

    /**
     * {@inheritDoc}
     *
     * @param ticketAuthenticatorWrapper {@inheritDoc}
     *
     * @return {@inheritDoc}
     */
    @Override
    public Future<TicketSessionKeyWrapper> requestClientServerTicket(final TicketAuthenticatorWrapper ticketAuthenticatorWrapper) {
        return GlobalCachedExecutorService.submit(() -> {
            try {
                return AuthenticationServerHandler.handleTGSRequest(ticketGrantingServiceSecretKey, serviceServerSecretKey, ticketAuthenticatorWrapper, ticketValidityTime);
            } catch (CouldNotPerformException ex) {
                ExceptionPrinter.printHistory(ex, LOGGER, LogLevel.WARN);
                ExceptionReporter.getInstance().report(ex);
                throw new RejectedException(ex.getMessage());
            }
        });
    }

    /**
     * {@inheritDoc}
     *
     * @param ticketAuthenticatorWrapper {@inheritDoc}
     *
     * @return {@inheritDoc}
     */
    @Override
    public Future<TicketAuthenticatorWrapper> validateClientServerTicket(TicketAuthenticatorWrapper ticketAuthenticatorWrapper) {
        return GlobalCachedExecutorService.submit(() -> {
            try {
                // validate ticket
                final AuthenticationBaseData authenticationBaseData = AuthenticationServerHandler.handleSSRequest(serviceServerSecretKey, ticketAuthenticatorWrapper, ticketValidityTime);
                // validate that user and client still exists so that no user can be logged in after being removed
                final UserClientPair pair = authenticationBaseData.getUserClientPair();
                if (!pair.getUserId().isEmpty() && !credentialStore.hasEntry(pair.getUserId())) {
                    throw new RejectedException("User[" + pair.getUserId() + "] logged in after being removed from authenticator!");

                }
                if (!pair.getClientId().isEmpty() && !credentialStore.hasEntry(pair.getClientId())) {
                    throw new RejectedException("Client[" + pair.getClientId() + "] logged in after being removed from authenticator!");

                }
                // return updated ticket wrapper
                return authenticationBaseData.getTicketAuthenticatorWrapper();
            } catch (SessionExpiredException ex) {
                throw ex;
            } catch (CouldNotPerformException ex) {
                ExceptionPrinter.printHistory(ex, LOGGER, LogLevel.WARN);
                ExceptionReporter.getInstance().report(ex);
                throw new RejectedException(ex.getMessage());
            }
        });
    }

    /**
     * Helper method for utilizing the {@link AuthenticatedServiceProcessor}.
     *
     * @return an object capable of verifying tickets,
     */
    private TicketValidator getTicketValidator() {
        return authenticatedValue -> AuthenticationServerHandler.handleSSRequest(serviceServerSecretKey, authenticatedValue.getTicketAuthenticatorWrapper(), ticketValidityTime);
    }

    /**
     * {@inheritDoc}
     *
     * @param authenticatedValue {@inheritDoc}
     *
     * @return {@inheritDoc}
     */
    @Override
    public Future<AuthenticatedValue> changeCredentials(final AuthenticatedValue authenticatedValue) {
        return GlobalCachedExecutorService.submit(() -> AuthenticatedServiceProcessor.authenticatedAction(authenticatedValue, LoginCredentialsChange.class, getTicketValidator(), (InternalIdentifiedProcessable<LoginCredentialsChange, Serializable>) (loginCredentialsChange, authenticationBaseData) -> {
            final UserClientPair userClientPair = authenticationBaseData.getUserClientPair();
            // validate permissions to change credentials
            boolean isAdmin = credentialStore.isAdmin(userClientPair.getUserId());

            if (!isAdmin) {
                // user is not admin, so verify that new credentials are either for logged in client or user
                boolean userIdMatches = userClientPair.getUserId().equals(loginCredentialsChange.getId()) && !userClientPair.getUserId().isEmpty();
                boolean clientIdMatches = userClientPair.getClientId().equals(loginCredentialsChange.getId()) && !userClientPair.getClientId().isEmpty();
                if (!userIdMatches && !clientIdMatches) {
                    // neither user id nor client id match so reject
                    throw new RejectedException("UserClientPair[" + userClientPair + "] cannot change password of user or client[" + loginCredentialsChange.getId() + "]");
                }
            }

            if (!isAdmin && !credentialStore.getCredentials(loginCredentialsChange.getId()).getCredentials().equals(loginCredentialsChange.getOldCredentials())) {
                throw new RejectedException("Old credentials do not match");
            }

            // update credentials in the credentialStore, this makes sure that a user does not appoint itself an admin
            final Builder newCredentials = credentialStore.getCredentials(loginCredentialsChange.getId()).toBuilder();
            newCredentials.setSymmetric(loginCredentialsChange.getSymmetric());
            newCredentials.setCredentials(loginCredentialsChange.getNewCredentials());
            credentialStore.addEntry(newCredentials.getId(), newCredentials.build());

            // return login credentials
            return loginCredentialsChange;
        }));
    }

    /**
     * {@inheritDoc}
     *
     * @param authenticatedValue {@inheritDoc}
     *
     * @return {@inheritDoc}
     */
    @Override
    public Future<AuthenticatedValue> register(final AuthenticatedValue authenticatedValue) {
        return GlobalCachedExecutorService.submit(() -> AuthenticatedServiceProcessor.authenticatedAction(authenticatedValue, LoginCredentials.class, getTicketValidator(), (InternalIdentifiedProcessable<LoginCredentials, Serializable>) (loginCredentials, authenticationBaseData) -> {
            // test if it is the initial registration
            if (initialPassword != null && (initialPasswordRequired() || JPService.testMode())) {
                // validate credentials
                if (!loginCredentials.hasId() || !loginCredentials.hasCredentials()) {
                    throw new RejectedException("Cannot register first user, id and/or new credentials empty");
                }

                // create credentials for initial user by copying id and symmetric field
                // making sure she/he is an admin and decrypting the credentials with the initial password
                final LoginCredentials adminCredentials = LoginCredentials.newBuilder()
                        .setId(loginCredentials.getId())
                        .setAdmin(true)
                        .setSymmetric(loginCredentials.getSymmetric())
                        .setCredentials(ByteString.copyFrom(EncryptionHelper.decryptSymmetric(loginCredentials.getCredentials(), EncryptionHelper.hash(initialPassword), byte[].class)))
                        .build();
                // save credentials
                credentialStore.addEntry(loginCredentials.getId(), adminCredentials);

                // clear initials password
                initialPassword = null;
                // return credentials
                return adminCredentials;
            }

            // validate that only admins can register new admins
            if (loginCredentials.getAdmin() && !credentialStore.isAdmin(authenticationBaseData.getUserClientPair().getUserId())) {
                throw new PermissionDeniedException("You are not permitted to register an admin.");
            }

            // do not allow overwriting of existing users
            if (credentialStore.hasEntry(loginCredentials.getId())) {
                throw new CouldNotPerformException("You cannot register an existing user.");
            }

            // register
            credentialStore.addEntry(loginCredentials.getId(), loginCredentials);

            // return login credentials
            return loginCredentials;
        }));
    }

    /**
     * {@inheritDoc}
     *
     * @param authenticatedValue {@inheritDoc}
     *
     * @return {@inheritDoc}
     */
    @Override
    public Future<AuthenticatedValue> removeUser(final AuthenticatedValue authenticatedValue) {
        return GlobalCachedExecutorService.submit(() -> AuthenticatedServiceProcessor.authenticatedAction(authenticatedValue, String.class, getTicketValidator(), (InternalIdentifiedProcessable<String, Serializable>) (idToBeRemoved, authenticationBaseData) -> {

            // if the user which is logged in differs from the user to be removed the logged in user has to be an admin
            if (!idToBeRemoved.equals(authenticationBaseData.getUserClientPair().getUserId())) {
                if (!credentialStore.isAdmin(authenticationBaseData.getUserClientPair().getUserId())) {
                    throw new PermissionDeniedException("You are not allowed to perform this action");
                }
            }

            // make sure that if an admin is removed it it not the last one
            if (credentialStore.isAdmin(idToBeRemoved) && credentialStore.getAdminCount() <= 1) {
                throw new PermissionDeniedException("The last admin cannot remove itself");
            }

            // remove user and return id
            credentialStore.removeEntry(idToBeRemoved);
            return idToBeRemoved;
        }));
    }

    /**
     * {@inheritDoc}
     *
     * @param authenticatedValue {@inheritDoc}
     *
     * @return {@inheritDoc}
     */
    @Override
    public Future<AuthenticatedValue> setAdministrator(final AuthenticatedValue authenticatedValue) {
        return GlobalCachedExecutorService.submit(() -> AuthenticatedServiceProcessor.authenticatedAction(authenticatedValue, LoginCredentials.class, getTicketValidator(), (InternalIdentifiedProcessable<LoginCredentials, Serializable>) (loginCredentials, authenticationBaseData) -> {
            // only admins can change admin flags
            if (!credentialStore.isAdmin(authenticationBaseData.getUserClientPair().getUserId())) {
                throw new PermissionDeniedException("You are not permitted to perform this action.");
            }

            // don't allow administrators to change administrator status of themselves
            // this ensures that at least one admin will stay in the system
            if (authenticationBaseData.getUserClientPair().getUserId().equals(loginCredentials.getId())) {
                throw new CouldNotPerformException("Admin status can only be revoked by another admin.");
            }

            // update admin flag
            credentialStore.addEntry(loginCredentials.getId(), credentialStore.getCredentials(loginCredentials.getId()).toBuilder().setAdmin(loginCredentials.getAdmin()).build());

            return loginCredentials;
        }));
    }

    /**
     * {@inheritDoc}
     *
     * @param ticketAuthenticatorWrapper {@inheritDoc}
     *
     * @return {@inheritDoc}
     */
    @Override
    public Future<AuthenticatedValue> requestServiceServerSecretKey(TicketAuthenticatorWrapper ticketAuthenticatorWrapper) {
        return GlobalCachedExecutorService.submit(() -> {
            try {
                final AuthenticationBaseData authenticationBaseData = getTicketValidator().verifyClientServerTicket(AuthenticatedValue.newBuilder().setTicketAuthenticatorWrapper(ticketAuthenticatorWrapper).build());

                if (!authenticationBaseData.getUserClientPair().getClientId().equals(CredentialStore.SERVICE_SERVER_ID)) {
                    throw new RejectedException("Client[" + authenticationBaseData.getUserClientPair().getClientId() + "] is not authorized to request the ServiceServerSecretKey");
                }

                AuthenticatedValue.Builder authenticatedValue = AuthenticatedValue.newBuilder();
                authenticatedValue.setTicketAuthenticatorWrapper(authenticationBaseData.getTicketAuthenticatorWrapper());
                authenticatedValue.setValue(EncryptionHelper.encryptSymmetric(this.serviceServerSecretKey, authenticationBaseData.getSessionKey()));

                return authenticatedValue.build();
            } catch (CouldNotPerformException ex) {
                ExceptionPrinter.printHistory(ex, LOGGER, LogLevel.WARN);
                ExceptionReporter.getInstance().report(ex);
                throw new RejectedException(ex.getMessage());
            }
        });
    }

    /**
     * {@inheritDoc}
     *
     * @param userId {@inheritDoc}
     *
     * @return {@inheritDoc}
     */
    @Override
    public Future<Boolean> isAdmin(final String userId) {
        return GlobalCachedExecutorService.submit(() -> credentialStore.isAdmin(userId));
    }

    /**
     * Get the initial password which is randomly generated on startup with an empty
     * credentialStore. Else it is null and will also be reset to null after registration of the
     * first user.
     *
     * @return the password required for the registration of the initial user
     */
    public static String getInitialPassword() {
        return initialPassword;
    }

    /**
     * {@inheritDoc}
     *
     * @param userOrClientId {@inheritDoc}
     *
     * @return {@inheritDoc}
     */
    @Override
    public Future<Boolean> hasUser(String userOrClientId) {
        return GlobalCachedExecutorService.submit(() -> credentialStore.hasEntry(userOrClientId));
    }
}
