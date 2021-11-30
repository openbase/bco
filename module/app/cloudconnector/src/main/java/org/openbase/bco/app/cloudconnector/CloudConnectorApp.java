package org.openbase.bco.app.cloudconnector;

/*-
 * #%L
 * BCO Cloud Connector
 * %%
 * Copyright (C) 2018 - 2021 openbase.org
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

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.openbase.bco.authentication.lib.AuthenticatedServiceProcessor;
import org.openbase.bco.authentication.lib.AuthenticationBaseData;
import org.openbase.bco.authentication.lib.SessionManager;
import org.openbase.bco.authentication.lib.future.AuthenticatedValueFuture;
import org.openbase.bco.dal.control.layer.unit.app.AbstractAppController;
import org.openbase.bco.registry.lib.util.UnitConfigProcessor;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.jul.communication.iface.RPCServer;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InitializationException;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.extension.type.processing.MetaConfigPool;
import org.openbase.jul.extension.type.processing.MetaConfigVariableProvider;
import org.openbase.jul.schedule.GlobalCachedExecutorService;
import org.openbase.type.domotic.action.ActionDescriptionType.ActionDescription;
import org.openbase.type.domotic.authentication.AuthenticatedValueType.AuthenticatedValue;
import org.openbase.type.domotic.authentication.AuthenticationTokenType.AuthenticationToken;
import org.openbase.type.domotic.state.ActivationStateType.ActivationState;
import org.openbase.type.domotic.unit.UnitConfigType.UnitConfig;
import org.openbase.type.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;
import org.openbase.type.domotic.unit.user.UserConfigType.UserConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class CloudConnectorApp extends AbstractAppController implements CloudConnector {

    private static final Logger LOGGER = LoggerFactory.getLogger(CloudConnectorApp.class);

    private final CloudConnectorTokenStore tokenStore;
    private final JsonParser jsonParser;
    private final Map<String, SocketWrapper> userIdSocketMap;

    public CloudConnectorApp() throws InstantiationException {
        super();
        this.userIdSocketMap = new HashMap<>();
        this.tokenStore = new CloudConnectorTokenStore();
        this.jsonParser = new JsonParser();
    }

    @Override
    protected void postInit() throws InitializationException, InterruptedException {
        super.postInit();

        tokenStore.init(CloudConnectorTokenStore.DEFAULT_TOKEN_STORE_FILENAME);
    }

    /**
     * {@inheritDoc}
     *
     * @param server {@inheritDoc}
     *
     * @throws CouldNotPerformException {@inheritDoc}
     */
    @Override
    public void registerMethods(RPCServer server) throws CouldNotPerformException {
        super.registerMethods(server);

        // additionally, register method for connecting to cloud
        server.registerMethods(CloudConnector.class, this);
    }

    private void createAuthenticationToken() throws CouldNotPerformException, InterruptedException {
        try {
            UnitConfig cloudConnectorUser = null;
            for (final UnitConfig unitConfig : Registries.getUnitRegistry().getUnitConfigsByUnitType(UnitType.USER)) {
                MetaConfigPool metaConfigPool = new MetaConfigPool();
                metaConfigPool.register(new MetaConfigVariableProvider(UnitConfigProcessor.getDefaultAlias(unitConfig, "?"), unitConfig.getMetaConfig()));
                try {
                    String unitId = metaConfigPool.getValue("UNIT_ID");
                    if (unitId.equalsIgnoreCase(getId())) {
                        cloudConnectorUser = unitConfig;
                        break;
                    }
                } catch (NotAvailableException ex) {
                    // do nothing
                }
            }

            if (cloudConnectorUser == null) {
                throw new NotAvailableException("Cloud Connector App User");
            }

            final AuthenticationToken authenticationToken = AuthenticationToken.newBuilder().setUserId(cloudConnectorUser.getId()).build();
            final SessionManager sessionManager = new SessionManager();
            sessionManager.loginUser(cloudConnectorUser.getId(), false);
            final AuthenticatedValue authenticatedValue = sessionManager.initializeRequest(authenticationToken, null);
            final String token = new AuthenticatedValueFuture<>(
                    Registries.getUnitRegistry().requestAuthenticationTokenAuthenticated(authenticatedValue),
                    String.class,
                    authenticatedValue.getTicketAuthenticatorWrapper(),
                    sessionManager).get();
            tokenStore.addCloudConnectorToken(token);
        } catch (CouldNotPerformException | ExecutionException ex) {
            throw new CouldNotPerformException("Could not create authentication token for cloud connector", ex);
        }
    }

    /**
     * Execute the cloud connector app by activating socket connections for all registered users.
     * Additionally the cloud connector creates an authentication token for itself when first executed.
     *
     * @throws CouldNotPerformException if a socket connection could not be established
     * @throws InterruptedException     if the activation is interrupted
     */
    @Override
    protected ActionDescription execute(final ActivationState activationState) throws CouldNotPerformException, InterruptedException {

        if (!tokenStore.hasCloudConnectorToken()) {
            createAuthenticationToken();
        }

        // start socket connection for all users which are already registered
        for (final Entry<String, String> entry : tokenStore.getCloudEntries().entrySet()) {
            final String userId = entry.getKey();
            final SocketWrapper socketWrapper = new SocketWrapper(userId, tokenStore);
            socketWrapper.init();
            socketWrapper.activate();
            userIdSocketMap.put(userId, socketWrapper);
        }
        return activationState.getResponsibleAction();
    }

    /**
     * Stop the cloud connector by deactivating all socket connections.
     *
     * @throws CouldNotPerformException if a socket connections could not be deactivated
     */
    @Override
    protected void stop(final ActivationState activationState) throws CouldNotPerformException, InterruptedException {
        for (SocketWrapper socketWrapper : userIdSocketMap.values()) {
            socketWrapper.deactivate();
        }
        super.stop(activationState);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void shutdown() {
        super.shutdown();
        tokenStore.shutdown();
    }

    /**
     * Retrieve the id of the authenticated user from authentication base data. If an authentication token is available
     * the user id it contains is returned. Else the client part of the authenticated user id is removed.
     *
     * @param authenticationBaseData the data from which the user id is retrieved
     *
     * @return the id of the authenticated user
     *
     * @throws CouldNotPerformException if only a client is logged in but no user
     */
    private String retrieveAuthenticatedUserId(final AuthenticationBaseData authenticationBaseData) throws CouldNotPerformException {
        if (authenticationBaseData.getAuthenticationToken() != null) {
            return authenticationBaseData.getAuthenticationToken().getUserId();
        } else {
            if (authenticationBaseData.getUserClientPair().getUserId().isEmpty()) {
                throw new CouldNotPerformException("Could not retrieve authenticated user because only client[" + authenticationBaseData.getUserClientPair().getClientId() + "] is logged in");
            }
            return authenticationBaseData.getUserClientPair().getUserId();
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param authenticatedValue {@inheritDoc}
     *
     * @return {@inheritDoc}
     */
    @Override
    public Future<AuthenticatedValue> connect(final AuthenticatedValue authenticatedValue) {
        return GlobalCachedExecutorService.submit(() -> AuthenticatedServiceProcessor.authenticatedAction(authenticatedValue, Boolean.class,
                (connect, authenticationBaseData) -> {
                    final String userId = retrieveAuthenticatedUserId(authenticationBaseData);
                    LOGGER.info("User[" + authenticationBaseData.getUserClientPair().getUserId() + "] connects[" + connect + "]...");

                    if (connect) {
                        try {
                            final SocketWrapper socketWrapper;
                            if (userIdSocketMap.containsKey(userId)) {
                                // get existing socket
                                socketWrapper = userIdSocketMap.get(userId);
                            } else {
                                // only create new socket if user is already registered
                                if (!tokenStore.hasBCOToken(userId) || !tokenStore.hasCloudToken(userId)) {
                                    throw new CouldNotPerformException("User[" + userId + "] is not yet registered");
                                }
                                // create new socket
                                socketWrapper = new SocketWrapper(userId, tokenStore);
                            }

                            // if socket is not yet active activate and wait for login
                            if (!socketWrapper.isActive()) {
                                socketWrapper.activate();
                                socketWrapper.getLoginFuture().get(10, TimeUnit.SECONDS);
                            }
                        } catch (CouldNotPerformException | ExecutionException | InterruptedException | TimeoutException ex) {
                            if (ex instanceof InterruptedException) {
                                Thread.currentThread().interrupt();
                            }
                            throw new CouldNotPerformException("Could not connect socket for user[" + userId + "]", ex);
                        }
                    } else {
                        // only disconnected if socket exists and is active
                        if (userIdSocketMap.containsKey(userId) && userIdSocketMap.get(userId).isActive()) {
                            try {
                                userIdSocketMap.get(userId).deactivate();
                            } catch (CouldNotPerformException ex) {
                                throw new CouldNotPerformException("Could not disconnect socket for user[" + userId + "]", ex);
                            }
                        }
                    }

                    // return null because internal value in the authenticated value send back is irrelevant for the user
                    return null;
                })
        );
    }

    /**
     * {@inheritDoc}
     *
     * @param authenticatedValue {@inheritDoc}
     *
     * @return {@inheritDoc}
     */
    @Override
    public Future<AuthenticatedValue> register(AuthenticatedValue authenticatedValue) {
        return GlobalCachedExecutorService.submit(() -> AuthenticatedServiceProcessor.authenticatedAction(authenticatedValue, String.class,
                (jsonString, authenticationBaseData) -> {
                    final String userId = retrieveAuthenticatedUserId(authenticationBaseData);

                    // validate that not already registered
                    if (tokenStore.hasCloudToken(userId)) {
                        throw new CouldNotPerformException("User[" + userId + "] is already registered");
                    }

                    try {
                        // parse string as json
                        final JsonObject params = jsonParser.parse(jsonString).getAsJsonObject();

                        // validate that password hash is available
                        if (!params.has(PASSWORD_HASH_KEY)) {
                            throw new NotAvailableException(PASSWORD_HASH_KEY);
                        }

                        // validate that password salt is available
                        if (!params.has(PASSWORD_SALT_KEY)) {
                            throw new NotAvailableException(PASSWORD_SALT_KEY);
                        }

                        // validate that authorization token is available
                        if (!params.has(AUTHORIZATION_TOKEN_KEY)) {
                            throw new NotAvailableException(AUTHORIZATION_TOKEN_KEY);
                        }

                        // save authorization token and remove it because the json object will be used to register at the cloud
                        tokenStore.addBCOToken(userId, params.get(AUTHORIZATION_TOKEN_KEY).getAsString());
                        params.remove(AUTHORIZATION_TOKEN_KEY);

                        // add username and email for the authenticated user
                        final UserConfig userConfig = Registries.getUnitRegistry().getUnitConfigById(userId).getUserConfig();
                        params.addProperty(USERNAME_KEY, userConfig.getUserName());
                        params.addProperty(EMAIL_HASH_KEY, CloudConnector.hash(userConfig.getEmail()));

                        // create a socket wrapper for the user with the given params
                        // this will automatically register and login the user
                        SocketWrapper socketWrapper = new SocketWrapper(userId, tokenStore, params);
                        userIdSocketMap.put(userId, socketWrapper);
                        socketWrapper.init();
                        socketWrapper.activate();
                        // wait for the socket wrapper to login to inform the user of possible problems
                        socketWrapper.getLoginFuture().get(10, TimeUnit.SECONDS);
                        // return null because internal value in the authenticated value send back is irrelevant for the user
                        return null;
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                        throw new CouldNotPerformException("Could not connect to BCO Cloud for user[" + userId + "]", ex);
                    } catch (ExecutionException | TimeoutException ex) {
                        throw new CouldNotPerformException("Could not connect to BCO Cloud for user[" + userId + "]", ex);
                    }
                })
        );
    }

    /**
     * {@inheritDoc}
     *
     * @param authenticatedValue {@inheritDoc}
     *
     * @return {@inheritDoc}
     */
    @Override
    public Future<AuthenticatedValue> remove(final AuthenticatedValue authenticatedValue) {
        return GlobalCachedExecutorService.submit(() -> AuthenticatedServiceProcessor.authenticatedAction(authenticatedValue, String.class,
                (value, authenticationBaseData) -> {
                    final String userId = retrieveAuthenticatedUserId(authenticationBaseData);

                    // do nothing if not registered
                    if (!tokenStore.hasCloudToken(userId)) {
                        return null;
                    }

                    try {
                        // retrieve socket wrapper or create one
                        SocketWrapper socketWrapper;
                        if (userIdSocketMap.containsKey(userId)) {
                            socketWrapper = userIdSocketMap.get(userId);
                        } else {
                            socketWrapper = new SocketWrapper(userId, tokenStore);
                            socketWrapper.init();
                        }

                        // make sure socket connection is established and logged in
                        if (!socketWrapper.isActive()) {
                            socketWrapper.activate();
                        }
                        socketWrapper.getLoginFuture().get(10, TimeUnit.SECONDS);

                        // remove Cloud account for user
                        socketWrapper.remove().get(10, TimeUnit.SECONDS);
                        // deactivate socket connection
                        socketWrapper.deactivate();
                    } catch (CouldNotPerformException | ExecutionException | TimeoutException ex) {
                        throw new CouldNotPerformException("Could not remove user[" + userId + "]", ex);
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                        throw new CouldNotPerformException("Could not remove user[" + userId + "]", ex);
                    }
                    // remove socket wrapper and tokens from store
                    userIdSocketMap.remove(userId);
                    tokenStore.removeBCOToken(userId);
                    tokenStore.removeCloudToken(userId);

                    // return null because internal value in the authenticated value send back is irrelevant for the user
                    return null;
                })
        );
    }

    /**
     * {@inheritDoc}
     *
     * @param authenticatedValue {@inheritDoc}
     *
     * @return {@inheritDoc}
     */
    @Override
    public Future<AuthenticatedValue> setAuthorizationToken(final AuthenticatedValue authenticatedValue) {
        return GlobalCachedExecutorService.submit(() -> AuthenticatedServiceProcessor.authenticatedAction(authenticatedValue, String.class,
                (authorizationToken, authenticationBaseData) -> {
                    final String userId = retrieveAuthenticatedUserId(authenticationBaseData);

                    // set authorization token in store for user
                    tokenStore.addBCOToken(userId, authorizationToken);

                    // return null because internal value in the authenticated value send back is irrelevant for the user
                    return null;
                })
        );
    }
}
