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
import org.openbase.bco.authentication.lib.TokenStore;
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

    private final TokenStore tokenStore;
    private final JsonParser jsonParser;
    private final Map<String, SocketWrapper> userIdSocketMap;

    public CloudConnectorAppImpl() throws InstantiationException {
        super(CloudConnectorAppImpl.class);
        this.userIdSocketMap = new HashMap<>();
        this.tokenStore = new TokenStore("cloud_connector_token_store.json");
        this.jsonParser = new JsonParser();
    }

    @Override
    protected void postInit() throws InitializationException, InterruptedException {
        super.postInit();

        tokenStore.init();
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
        if (!tokenStore.contains(getId())) {
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
    protected void stop() throws CouldNotPerformException, InterruptedException {
        logger.info("Stop Cloud Connector");
        for (SocketWrapper socketWrapper : userIdSocketMap.values()) {
            socketWrapper.deactivate();
        }
        tokenStore.shutdown();
    }

    private String connect(final String jsonObject, final AuthenticationBaseData authenticationBaseData) throws CouldNotPerformException {
        LOGGER.info("User[" + authenticationBaseData.getUserId() + "] connects..");

        final String userId;
        if (authenticationBaseData.getAuthenticationToken() != null) {
            userId = authenticationBaseData.getAuthenticationToken().getUserId();
        } else {
            userId = authenticationBaseData.getUserId().split("@")[0];
        }

        try {
            final JsonObject params = jsonParser.parse(jsonObject).getAsJsonObject();

            if (userIdSocketMap.containsKey(userId)) {
                if (!params.has("connect")) {
                    throw new NotAvailableException("connect parameter for registered user[" + userId + "]");
                }

                final boolean connect = params.get("connect").getAsBoolean();
                final SocketWrapper socketWrapper = userIdSocketMap.get(userId);
                if (connect && !socketWrapper.isActive()) {
                    socketWrapper.activate();
                    socketWrapper.getLoginFuture().get(10, TimeUnit.SECONDS);
                } else if (!connect && socketWrapper.isActive()) {
                    socketWrapper.deactivate();
                }
                return "Success";
            }

            if (tokenStore.contains(userId + "@BCO")) {
                final SocketWrapper socketWrapper = new SocketWrapper(userId, tokenStore, tokenStore.getToken(getId()));
                userIdSocketMap.put(userId, socketWrapper);
                socketWrapper.init();
                socketWrapper.activate();
                socketWrapper.getLoginFuture().get(10, TimeUnit.SECONDS);
                return "Success";
            }

            if (!params.has(RegistrationHelper.AUTHORIZATION_TOKEN_KEY)) {
                throw new NotAvailableException(RegistrationHelper.AUTHORIZATION_TOKEN_KEY);
            }

            if (!params.has(RegistrationHelper.PASSWORD_HASH_KEY)) {
                throw new NotAvailableException(RegistrationHelper.PASSWORD_HASH_KEY);
            }

            if (!params.has(RegistrationHelper.PASSWORD_SALT_KEY)) {
                throw new NotAvailableException(RegistrationHelper.PASSWORD_SALT_KEY);
            }

            tokenStore.addToken(userId + "@BCO", params.get(RegistrationHelper.AUTHORIZATION_TOKEN_KEY).getAsString());
            params.remove(RegistrationHelper.AUTHORIZATION_TOKEN_KEY);

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
            return "Success";
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new CouldNotPerformException("Could not connect to BCO Cloud for user[" + userId + "]", ex);
        } catch (ExecutionException | TimeoutException ex) {
            throw new CouldNotPerformException("Could not connect to BCO Cloud for user[" + userId + "]", ex);
        }
    }

    @Override
    public Future<AuthenticatedValue> connect(final AuthenticatedValue authenticatedValue) {
        return GlobalCachedExecutorService.submit(() ->
                AuthenticatedServiceProcessor.authenticatedAction(authenticatedValue, String.class, this::connect));
    }
}
