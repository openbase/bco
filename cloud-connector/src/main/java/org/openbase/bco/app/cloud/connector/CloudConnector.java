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
import org.openbase.bco.app.cloud.connector.jp.JPCloudConnectorScope;
import org.openbase.bco.authentication.lib.AuthenticatedServiceProcessor;
import org.openbase.bco.authentication.lib.AuthenticationBaseData;
import org.openbase.bco.authentication.lib.TokenStore;
import org.openbase.bco.authentication.lib.jp.JPAuthentication;
import org.openbase.bco.registry.login.SystemLogin;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.jps.core.JPService;
import org.openbase.jps.exception.JPNotAvailableException;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InitializationException;
import org.openbase.jul.extension.rsb.com.RSBFactoryImpl;
import org.openbase.jul.extension.rsb.com.RSBSharedConnectionConfig;
import org.openbase.jul.extension.rsb.iface.RSBLocalServer;
import org.openbase.jul.iface.Launchable;
import org.openbase.jul.iface.VoidInitializable;
import org.openbase.jul.schedule.GlobalCachedExecutorService;
import org.openbase.jul.schedule.WatchDog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rsb.Scope;
import rst.domotic.authentication.AuthenticatedValueType.AuthenticatedValue;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.Future;

/**
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class CloudConnector implements Launchable<Void>, VoidInitializable {

    private static final Logger LOGGER = LoggerFactory.getLogger(CloudConnector.class);

    private final TokenStore tokenStore;
    private final JsonParser jsonParser;
    private final Map<String, SocketWrapper> userIdSocketMap;

    private RSBLocalServer server;
    private WatchDog serverWatchDog;

    public CloudConnector() {
        this.userIdSocketMap = new HashMap<>();
        this.tokenStore = new TokenStore("cloud_connector_token_store.json");
        this.jsonParser = new JsonParser();
    }

    @Override
    public void init() throws InitializationException {
        try {
            tokenStore.init();

            final Scope scope = JPService.getProperty(JPCloudConnectorScope.class).getValue();
            server = RSBFactoryImpl.getInstance().createSynchronizedLocalServer(scope, RSBSharedConnectionConfig.getParticipantConfig());

            //TODO register methods, what about a remote?
            // register rpc methods.
//            RPCHelper.registerInterface(AuthenticationService.class, this, server);

            serverWatchDog = new WatchDog(server, "AuthenticatorWatchDog");

            //TODO: cloud connector itself needs to be logged in: how to handle the initIal process?
        } catch (CouldNotPerformException | JPNotAvailableException ex) {
            throw new InitializationException(this, ex);
        }
    }

    private String internalActivate(final String jsonObject, AuthenticationBaseData authenticationBaseData) throws CouldNotPerformException {
        final String userId = authenticationBaseData.getUserId();
        if (userIdSocketMap.containsKey(userId) && userIdSocketMap.get(userId).isActive()) {
            if (userIdSocketMap.get(userId).isActive()) {
                return "Already active";
            } else {
                userIdSocketMap.get(userId).activate();
                return "Activated";
            }
        }

        if (jsonObject.isEmpty()) {
            SocketWrapper socketWrapper = new SocketWrapper(userId, tokenStore);
            userIdSocketMap.put(userId, socketWrapper);
            socketWrapper.activate();
        } else {
            // TODO: token should also be contained or  not?
            JsonObject asJsonObject = jsonParser.parse(jsonObject).getAsJsonObject();
            tokenStore.addToken(userId + "@BCO", asJsonObject.get(RegistrationHelper.AUTHORIZATION_TOKEN_KEY).getAsString());
            if (asJsonObject.has("username")) {
                final String username = Registries.getUnitRegistry().getUnitConfigById(authenticationBaseData.getUserId()).getUserConfig().getUserName();
                asJsonObject.addProperty("username", username);
            }
            SocketWrapper socketWrapper = new SocketWrapper(userId, tokenStore, asJsonObject);
            userIdSocketMap.put(userId, socketWrapper);
            socketWrapper.activate();
        }

        return "Success";
    }

    public Future<AuthenticatedValue> activateForUser(final AuthenticatedValue authenticatedValue) {
        return GlobalCachedExecutorService.submit(() -> AuthenticatedServiceProcessor.authenticatedAction(authenticatedValue, String.class, this::internalActivate));
    }

    @Override
    public void activate() throws CouldNotPerformException, InterruptedException {
        //TODO: this is just a workaround for current authentication
        try {
            if (JPService.getProperty(JPAuthentication.class).getValue()) {
                SystemLogin.loginBCOUser();
            }
        } catch (JPNotAvailableException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        serverWatchDog.activate();
        final Set<String> userIds = new HashSet<>();
        for (final Entry<String, String> entry : tokenStore.getEntryMap().entrySet()) {
            final String userId = entry.getKey().split("@")[0];
            if (userIds.contains(userId)) {
                continue;
            }
            userIds.add(userId);
            SocketWrapper socketWrapper = new SocketWrapper(userId, tokenStore);
            userIdSocketMap.put(userId, socketWrapper);
            socketWrapper.activate();
        }
    }

    @Override
    public void deactivate() throws CouldNotPerformException, InterruptedException {
        if (serverWatchDog != null) {
            serverWatchDog.deactivate();
        }
        for (SocketWrapper socketWrapper : userIdSocketMap.values()) {
            socketWrapper.deactivate();
        }
        tokenStore.shutdown();
    }

    @Override
    public boolean isActive() {
        if (serverWatchDog != null) {
            return serverWatchDog.isActive();
        }
        return false;
    }
}
