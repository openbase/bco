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
import org.openbase.bco.app.cloud.connector.jp.JPCloudConnectorScope;
import org.openbase.bco.authentication.lib.AuthorizationHelper;
import org.openbase.bco.authentication.lib.SessionManager;
import org.openbase.bco.authentication.lib.future.AuthenticatedValueFuture;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.jps.core.JPService;
import org.openbase.jps.exception.JPNotAvailableException;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InitializationException;
import org.openbase.jul.extension.rsb.com.NotInitializedRSBRemoteServer;
import org.openbase.jul.extension.rsb.com.RPCHelper;
import org.openbase.jul.extension.rsb.com.RSBFactoryImpl;
import org.openbase.jul.extension.rsb.com.RSBSharedConnectionConfig;
import org.openbase.jul.extension.rsb.iface.RSBRemoteServer;
import org.openbase.jul.iface.Manageable;
import org.openbase.jul.iface.VoidInitializable;
import org.openbase.jul.schedule.WatchDog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rst.domotic.authentication.AuthenticatedValueType.AuthenticatedValue;
import rst.domotic.authentication.AuthorizationTokenType.AuthorizationToken;
import rst.domotic.authentication.AuthorizationTokenType.AuthorizationToken.MapFieldEntry.Builder;
import rst.domotic.unit.UnitConfigType.UnitConfig;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class CloudConnectorRemote implements CloudConnectorInterface, Manageable<Void>, VoidInitializable {

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(AuthenticatedValue.getDefaultInstance()));
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(CloudConnectorRemote.class);

    private RSBRemoteServer remoteServer;
    private WatchDog serverWatchDog;

    public CloudConnectorRemote() {
        this.remoteServer = new NotInitializedRSBRemoteServer();
    }

    @Override
    public void init() throws InitializationException {
        try {
            remoteServer = RSBFactoryImpl.getInstance().createSynchronizedRemoteServer(JPService.getProperty(JPCloudConnectorScope.class).getValue(), RSBSharedConnectionConfig.getParticipantConfig());

            serverWatchDog = new WatchDog(remoteServer, "AuthenticatorWatchDog");
        } catch (JPNotAvailableException | CouldNotPerformException ex) {
            throw new InitializationException(this, ex);
        }
    }

    @Override
    public void activate() throws CouldNotPerformException, InterruptedException {
        serverWatchDog.activate();
    }

    @Override
    public void deactivate() throws CouldNotPerformException, InterruptedException {
        serverWatchDog.deactivate();
    }

    @Override
    public boolean isActive() {
        return remoteServer.isActive();
    }

    public void waitForActivation() throws CouldNotPerformException, InterruptedException {
        try {
            serverWatchDog.waitForServiceActivation();
        } catch (final CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not wait for activation!", ex);
        }
    }

    public Future<String> connect(final boolean connect) throws CouldNotPerformException {
        final JsonObject params = new JsonObject();
        params.addProperty("connect", connect);
        final AuthenticatedValue authenticatedValue = SessionManager.getInstance().initializeRequest(params.toString(), null, null);
        final Future<AuthenticatedValue> internalFuture = connect(authenticatedValue);
        return new AuthenticatedValueFuture<>(internalFuture, String.class, authenticatedValue.getTicketAuthenticatorWrapper(), SessionManager.getInstance());
    }

    public Future<String> connect(final String password) throws CouldNotPerformException {
        return connect(password, true);
    }

    public Future<String> connect(final String password, final boolean autoStart) throws CouldNotPerformException {
        // request authorization token for current user -> how to determine full rights?
        final AuthorizationToken token = getToken();
        LOGGER.info("Created authorization token:\n" + token);
        String authorizationToken;
        try {
            authorizationToken = Registries.getUnitRegistry().requestAuthorizationToken(token).get();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new CouldNotPerformException("Interrupted while requesting authorization token", ex);
        } catch (ExecutionException ex) {
            throw new CouldNotPerformException("Could not request authorization token", ex);
        }
        final String params = RegistrationHelper.createRegistrationData(password, authorizationToken, autoStart);
        LOGGER.info("Send params [" + params + "]");
        final AuthenticatedValue authenticatedValue = SessionManager.getInstance().initializeRequest(params, null, null);
        final Future<AuthenticatedValue> internalFuture = connect(authenticatedValue);
        return new AuthenticatedValueFuture<>(internalFuture, String.class, authenticatedValue.getTicketAuthenticatorWrapper(), SessionManager.getInstance());
    }

    @Override
    public Future<AuthenticatedValue> connect(final AuthenticatedValue authenticatedValue) throws CouldNotPerformException {
        return RPCHelper.callRemoteServerMethod(authenticatedValue, remoteServer, AuthenticatedValue.class);
    }

    private AuthorizationToken getToken() throws CouldNotPerformException {
        final AuthorizationToken.Builder authorizationToken = AuthorizationToken.newBuilder().setUserId(SessionManager.getInstance().getUserId());
        final Set<String> accessPermissionSet = new HashSet<>();
        addPermissionRules(accessPermissionSet, Registries.getUnitRegistry().getRootLocationConfig());
        for (final String unitId : accessPermissionSet) {
            Builder builder = authorizationToken.addPermissionRuleBuilder();
            builder.setKey(unitId);
            builder.getValueBuilder().setAccess(true).setRead(false).setWrite(false);
        }
        return authorizationToken.build();
    }

    private void addPermissionRules(final Set<String> accessPermissionSet, final UnitConfig location) throws CouldNotPerformException {
        if (AuthorizationHelper.canAccess(location, SessionManager.getInstance().getUserId(), Registries.getUnitRegistry().getAuthorizationGroupMap(), Registries.getUnitRegistry().getLocationMap())) {
            accessPermissionSet.add(location.getId());
        } else {
            if (!location.getLocationConfig().getChildIdList().isEmpty()) {
                for (final String childId : location.getLocationConfig().getChildIdList()) {
                    addPermissionRules(accessPermissionSet, Registries.getUnitRegistry().getUnitConfigById(childId));
                }
            } else {
                for (final String unitId : location.getLocationConfig().getUnitIdList()) {
                    if (!accessPermissionSet.contains(unitId)) {
                        final UnitConfig unitConfig = Registries.getUnitRegistry().getUnitConfigById(unitId);
                        if (AuthorizationHelper.canAccess(unitConfig, SessionManager.getInstance().getUserId(), Registries.getUnitRegistry().getAuthorizationGroupMap(), Registries.getUnitRegistry().getLocationMap())) {
                            accessPermissionSet.add(unitId);
                        }
                    }
                }
            }
        }
    }
}
