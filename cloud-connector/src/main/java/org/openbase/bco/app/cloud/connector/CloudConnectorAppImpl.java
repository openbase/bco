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
import org.openbase.bco.authentication.lib.SessionManager;
import org.openbase.bco.authentication.lib.TokenStore;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.bco.registry.unit.core.plugin.UnitUserCreationPlugin;
import org.openbase.jps.core.JPService;
import org.openbase.jps.exception.JPNotAvailableException;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InitializationException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.extension.rsb.com.RPCHelper;
import org.openbase.jul.extension.rsb.com.RSBFactoryImpl;
import org.openbase.jul.extension.rsb.com.RSBSharedConnectionConfig;
import org.openbase.jul.extension.rsb.iface.RSBLocalServer;
import org.openbase.jul.extension.rst.processing.LabelProcessor;
import org.openbase.jul.iface.Launchable;
import org.openbase.jul.iface.VoidInitializable;
import org.openbase.jul.schedule.GlobalCachedExecutorService;
import org.openbase.jul.schedule.WatchDog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rsb.Scope;
import rst.domotic.authentication.AuthenticatedValueType.AuthenticatedValue;
import rst.domotic.unit.UnitConfigType.UnitConfig;
import rst.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;
import rst.domotic.unit.app.AppClassType.AppClass;
import rst.domotic.unit.user.UserConfigType.UserConfig;

import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class CloudConnectorAppImpl implements Launchable<Void>, VoidInitializable, CloudConnectorApp {

    private static final Logger LOGGER = LoggerFactory.getLogger(CloudConnectorAppImpl.class);

    private final TokenStore tokenStore;
    private final JsonParser jsonParser;
    private final Map<String, SocketWrapper> userIdSocketMap;

    private WatchDog serverWatchDog;

    public CloudConnectorAppImpl() {
        this.userIdSocketMap = new HashMap<>();
        this.tokenStore = new TokenStore("cloud_connector_token_store.json");
        this.jsonParser = new JsonParser();
    }

    @Override
    public void init() throws InitializationException {
        try {
            tokenStore.init();

            final Scope scope = JPService.getProperty(JPCloudConnectorScope.class).getValue();
            final RSBLocalServer server = RSBFactoryImpl.getInstance().createSynchronizedLocalServer(scope, RSBSharedConnectionConfig.getParticipantConfig());

            // register rpc methods.
            RPCHelper.registerInterface(CloudConnectorApp.class, this, server);

            serverWatchDog = new WatchDog(server, "AuthenticatorWatchDog");
        } catch (CouldNotPerformException | JPNotAvailableException ex) {
            throw new InitializationException(this, ex);
        }
    }

    @Override
    public void activate() throws CouldNotPerformException, InterruptedException {
        login();
        //TODO: cloud connector should be an app and thus logged in by an app manager, if the token store
        // does not contain an authentication token for the cloud it should be generated here
        serverWatchDog.activate();

        // start socket connection for all users which are already registered
        final Set<String> userIds = new HashSet<>();
        for (final Entry<String, String> entry : tokenStore.getEntryMap().entrySet()) {
            final String userId = entry.getKey().split("@")[0];
            if (userIds.contains(userId)) {
                continue;
            }
            userIds.add(userId);
            SocketWrapper socketWrapper = new SocketWrapper(userId, tokenStore);
            userIdSocketMap.put(userId, socketWrapper);
            socketWrapper.init();
            socketWrapper.activate();
        }
    }

    public static final String CLOUD_CONNECTOR_APP_CLASS_LABEL = "Cloud Connector";

    private void login() throws CouldNotPerformException, InterruptedException {
        try {
            Registries.waitForData();
            String appClassId = "";
            for (AppClass appClass : Registries.getClassRegistry().getAppClasses()) {
                if (LabelProcessor.getLabelListByLanguage(Locale.ENGLISH, appClass.getLabel()).contains(CLOUD_CONNECTOR_APP_CLASS_LABEL)) {
                    appClassId = appClass.getId();
                    break;
                }
            }

            if (appClassId.isEmpty()) {
                throw new NotAvailableException("Cloud Connector App Class");
            }

            String appConfigId = "";
            for (UnitConfig unitConfig : Registries.getUnitRegistry().getUnitConfigs(UnitType.APP)) {
                if (unitConfig.getAppConfig().getAppClassId().equals(appClassId)) {
                    appConfigId = unitConfig.getId();
                    break;
                }
            }

            if (appConfigId.isEmpty()) {
                throw new NotAvailableException("Cloud Connector App Config");
            }

            SessionManager.getInstance().login(UnitUserCreationPlugin.findUser(appConfigId, Registries.getUnitRegistry().getUserUnitConfigRemoteRegistry()).getId());
        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not login Cloud Connector", ex);
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
                final SocketWrapper socketWrapper = new SocketWrapper(userId, tokenStore);
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

            SocketWrapper socketWrapper = new SocketWrapper(userId, tokenStore, params);
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
    public Future<AuthenticatedValue> connect(AuthenticatedValue authenticatedValue) {
        return GlobalCachedExecutorService.submit(() ->
                AuthenticatedServiceProcessor.authenticatedAction(authenticatedValue, String.class, this::connect));
    }
}
