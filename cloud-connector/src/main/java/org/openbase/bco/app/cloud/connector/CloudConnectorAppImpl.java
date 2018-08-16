package org.openbase.bco.app.cloud.connector;

/*-
 * #%L
 * BCO Cloud Connector
 * %%
 * Copyright (C) 2018 openbase.org
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
import org.openbase.bco.manager.app.core.AbstractAppController;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InitializationException;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.extension.rsb.com.RPCHelper;
import org.openbase.jul.extension.rsb.iface.RSBLocalServer;
import org.openbase.jul.extension.rst.processing.MetaConfigPool;
import org.openbase.jul.extension.rst.processing.MetaConfigVariableProvider;
import org.openbase.jul.schedule.GlobalCachedExecutorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rst.domotic.authentication.AuthenticatedValueType.AuthenticatedValue;
import rst.domotic.authentication.AuthenticationTokenType.AuthenticationToken;
import rst.domotic.unit.UnitConfigType.UnitConfig;
import rst.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;
import rst.domotic.unit.user.UserConfigType.UserConfig;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class CloudConnectorAppImpl extends AbstractAppController implements CloudConnectorApp {

    private static final Logger LOGGER = LoggerFactory.getLogger(CloudConnectorAppImpl.class);

    private final CloudConnectorTokenStore tokenStore;
    private final JsonParser jsonParser;
    private final Map<String, SocketWrapper> userIdSocketMap;

    public CloudConnectorAppImpl() throws InstantiationException {
        super(CloudConnectorAppImpl.class);
        this.userIdSocketMap = new HashMap<>();
        this.tokenStore = new CloudConnectorTokenStore();
        this.jsonParser = new JsonParser();
    }

    @Override
    protected void postInit() throws InitializationException, InterruptedException {
        super.postInit();

        tokenStore.init(CloudConnectorTokenStore.DEFAULT_TOKEN_STORE_FILENAME);
    }

    @Override
    public void registerMethods(RSBLocalServer server) throws CouldNotPerformException {
        super.registerMethods(server);

        // additionally register method for connecting to cloud
        RPCHelper.registerInterface(CloudConnectorApp.class, this, server);
    }

    private void createAuthenticationToken() throws CouldNotPerformException, InterruptedException {
        try {
            UnitConfig cloudConnectorUser = null;
            for (final UnitConfig unitConfig : Registries.getUnitRegistry().getUnitConfigs(UnitType.USER)) {
                MetaConfigPool metaConfigPool = new MetaConfigPool();
                metaConfigPool.register(new MetaConfigVariableProvider(unitConfig.getAlias(0), unitConfig.getMetaConfig()));
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
            sessionManager.login(cloudConnectorUser.getId());
            final AuthenticatedValue authenticatedValue = sessionManager.initializeRequest(authenticationToken, null, null);
            final String token = new AuthenticatedValueFuture<>(
                    Registries.getUnitRegistry().requestAuthenticationTokenAuthenticated(authenticatedValue),
                    String.class,
                    authenticatedValue.getTicketAuthenticatorWrapper(),
                    sessionManager).get();
            tokenStore.addToken(getId(), token);
        } catch (CouldNotPerformException | ExecutionException ex) {
            throw new CouldNotPerformException("Could not create authentication token for cloud connector", ex);
        }
    }

    @Override
    protected void execute() throws CouldNotPerformException, InterruptedException {
        logger.info("Execute Cloud Connector");
        if (!tokenStore.hasEntry(getId())) {
            createAuthenticationToken();
        }

        // start socket connection for all users which are already registered
        final Set<String> userIds = new HashSet<>();
        for (final Entry<String, String> entry : tokenStore.getEntryMap().entrySet()) {
            // ignore authentication token
            if (entry.getKey().equals(getId())) {
                continue;
            }

            final String userId = entry.getKey().split("@")[0];
            if (userIds.contains(userId)) {
                continue;
            }
            userIds.add(userId);
            SocketWrapper socketWrapper = new SocketWrapper(userId, tokenStore, tokenStore.getToken(getId()));
            userIdSocketMap.put(userId, socketWrapper);
            socketWrapper.init();
            socketWrapper.activate();
        }
    }

    @Override
    protected void stop() throws CouldNotPerformException {
        logger.info("Stop Cloud Connector");
        for (SocketWrapper socketWrapper : userIdSocketMap.values()) {
            socketWrapper.deactivate();
        }
        tokenStore.shutdown();
    }

    private String retrieveAuthenticatedUserId(final AuthenticationBaseData authenticationBaseData) {
        if (authenticationBaseData.getAuthenticationToken() != null) {
            return authenticationBaseData.getAuthenticationToken().getUserId();
        } else {
            return authenticationBaseData.getUserId().split("@")[0];
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param authenticatedValue {@inheritDoc}
     * @return {@inheritDoc}
     */
    @Override
    public Future<AuthenticatedValue> connect(final AuthenticatedValue authenticatedValue) {
        return GlobalCachedExecutorService.submit(() -> AuthenticatedServiceProcessor.authenticatedAction(authenticatedValue, Boolean.class, this::connect));
    }

    private Boolean connect(final Boolean connect, final AuthenticationBaseData authenticationBaseData) throws CouldNotPerformException {
        final String userId = retrieveAuthenticatedUserId(authenticationBaseData);
        LOGGER.info("User[" + authenticationBaseData.getUserId() + "] connects[" + connect + "]...");

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
                    socketWrapper = new SocketWrapper(userId, tokenStore, tokenStore.getToken(getId()));
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

        return null;
    }

    @Override
    public Future<AuthenticatedValue> register(AuthenticatedValue authenticatedValue) {
        return GlobalCachedExecutorService.submit(() -> AuthenticatedServiceProcessor.authenticatedAction(authenticatedValue, String.class, this::register));
    }

    private String register(final String jsonString, final AuthenticationBaseData authenticationBaseData) throws CouldNotPerformException {
        final String userId = retrieveAuthenticatedUserId(authenticationBaseData);
        LOGGER.info("Register user[" + userId + "]...");

        if (tokenStore.hasCloudToken(userId)) {
            throw new CouldNotPerformException("User[" + userId + "] is already registered");
        }

        try {
            final JsonObject params = jsonParser.parse(jsonString).getAsJsonObject();

            if (!params.has(RegistrationHelper.PASSWORD_HASH_KEY)) {
                throw new NotAvailableException(RegistrationHelper.PASSWORD_HASH_KEY);
            }

            if (!params.has(RegistrationHelper.PASSWORD_SALT_KEY)) {
                throw new NotAvailableException(RegistrationHelper.PASSWORD_SALT_KEY);
            }

            if (params.has(RegistrationHelper.AUTHORIZATION_TOKEN_KEY)) {
                tokenStore.addBCOToken(userId, params.get(RegistrationHelper.AUTHORIZATION_TOKEN_KEY).getAsString());
                params.remove(RegistrationHelper.AUTHORIZATION_TOKEN_KEY);
            } else {
                if (!tokenStore.hasBCOToken(userId)) {
                    throw new NotAvailableException(RegistrationHelper.AUTHORIZATION_TOKEN_KEY);
                }
            }

            //TODO where to store this ?
            boolean autostart = true;
            if (params.has("auto_start")) {
                autostart = params.get("auto_start").getAsBoolean();
                params.remove("auto_start");
            }

            final UserConfig userConfig = Registries.getUnitRegistry().getUnitConfigById(userId).getUserConfig();
            params.addProperty("username", userConfig.getUserName());
            params.addProperty(RegistrationHelper.EMAIL_HASH_KEY, RegistrationHelper.hash(userConfig.getEmail()));

            SocketWrapper socketWrapper = new SocketWrapper(userId, tokenStore, tokenStore.getToken(getId()), params);
            userIdSocketMap.put(userId, socketWrapper);
            socketWrapper.init();
            socketWrapper.activate();
            socketWrapper.getLoginFuture().get(10, TimeUnit.SECONDS);
            return null;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new CouldNotPerformException("Could not connect to BCO Cloud for user[" + userId + "]", ex);
        } catch (ExecutionException | TimeoutException ex) {
            throw new CouldNotPerformException("Could not connect to BCO Cloud for user[" + userId + "]", ex);
        }
    }

    @Override
    public Future<AuthenticatedValue> remove(AuthenticatedValue authenticatedValue) {
        return null;
    }

    @Override
    public Future<AuthenticatedValue> setAuthorizationToken(AuthenticatedValue authenticatedValue) {
        return GlobalCachedExecutorService.submit(() -> AuthenticatedServiceProcessor.authenticatedAction(authenticatedValue, String.class, this::setAuthorizationToken));
    }

    private String setAuthorizationToken(final String authorizationToken, final AuthenticationBaseData authenticationBaseData) {
        final String userId = retrieveAuthenticatedUserId(authenticationBaseData);
        LOGGER.info("Set authorization token for user[" + userId + "]...");

        tokenStore.addBCOToken(userId, authorizationToken);
        return null;
    }

    @Override
    public Future<AuthenticatedValue> setAutoStart(AuthenticatedValue authenticatedValue) {
        return null;
    }
}
