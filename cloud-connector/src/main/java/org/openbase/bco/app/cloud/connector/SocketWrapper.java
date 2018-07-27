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

import com.google.gson.*;
import io.socket.client.Ack;
import io.socket.client.IO;
import io.socket.client.Manager;
import io.socket.client.Socket;
import io.socket.engineio.client.Transport;
import org.openbase.bco.app.cloud.connector.jp.JPCloudServerURI;
import org.openbase.bco.app.cloud.connector.mapping.lib.ErrorCode;
import org.openbase.bco.authentication.lib.TokenStore;
import org.openbase.bco.dal.remote.unit.Units;
import org.openbase.bco.dal.remote.unit.user.UserRemote;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.bco.registry.unit.lib.UnitRegistry;
import org.openbase.jps.core.JPService;
import org.openbase.jps.exception.JPNotAvailableException;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InitializationException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.iface.Launchable;
import org.openbase.jul.iface.VoidInitializable;
import org.openbase.jul.pattern.Observable;
import org.openbase.jul.pattern.Observer;
import org.openbase.jul.schedule.GlobalCachedExecutorService;
import org.openbase.jul.schedule.SyncObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rst.domotic.registry.UnitRegistryDataType.UnitRegistryData;
import rst.domotic.state.UserTransitStateType.UserTransitState;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

/**
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class SocketWrapper implements Launchable<Void>, VoidInitializable {

    private static final String LOGIN_EVENT = "login";
    private static final String REGISTER_EVENT = "register";
    private static final String REQUEST_SYNC_EVENT = "requestSync";
    private static final String USER_TRANSIT_EVENT = "userTransit";

    private static final String TOKEN_KEY = "accessToken";
    private static final String SUCCESS_KEY = "success";
    private static final String ERROR_KEY = "error";

    private static final Logger LOGGER = LoggerFactory.getLogger(SocketWrapper.class);

    private final String userId;
    private final TokenStore tokenStore;
    private JsonObject loginData;
    private Socket socket;
    private boolean active = false;
    private boolean loggedIn = false;
    private String agentUserId;

    private final SyncObject syncRequestLock = new SyncObject("SyncRequestLock");
    private final UnitRegistryObserver unitRegistryObserver;

    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final JsonParser jsonParser = new JsonParser();
    private CompletableFuture<Void> loginFuture;

    public SocketWrapper(final String userId, final TokenStore tokenStore) {
        this(userId, tokenStore, null);
    }

    public SocketWrapper(final String userId, final TokenStore tokenStore, final JsonObject loginData) {
        this.userId = userId;
        this.tokenStore = tokenStore;
        this.loginData = loginData;
        this.unitRegistryObserver = new UnitRegistryObserver();
    }

    @Override
    public void init() throws InitializationException {
        try {
            // validate that the token store contains an authorization token for the given user
            if (!tokenStore.contains(userId + "@BCO")) {
                try {
                    throw new NotAvailableException("Token for user[" + userId + "] for BCO");
                } catch (NotAvailableException ex) {
                    throw new InitializationException(this, ex);
                }
            }

            // validate that either login data is set or the token store contains a token for the cloud
            if (loginData == null && !tokenStore.contains(userId + "@Cloud")) {
                try {
                    throw new NotAvailableException("Login data for user[" + userId + "] for cloud");
                } catch (NotAvailableException ex) {
                    throw new InitializationException(this, ex);
                }
            }

            final String bcoId = Registries.getUnitRegistry().getUnitConfigByAlias(UnitRegistry.BCO_USER_ALIAS).getId();
            agentUserId = userId + "@" + bcoId;

            // create socket
            socket = IO.socket(JPService.getProperty(JPCloudServerURI.class).getValue());
            // add id to header for cloud server
            socket.io().on(Manager.EVENT_TRANSPORT, args -> {
                Transport transport = (Transport) args[0];

                transport.on(Transport.EVENT_REQUEST_HEADERS, args1 -> {
                    try {
                        @SuppressWarnings("unchecked")
                        Map<String, List<String>> headers = (Map<String, List<String>>) args1[0];
                        // combination of bco and user id to header
                        headers.put("id", Collections.singletonList(agentUserId));
                    } catch (Exception ex) {
                        ExceptionPrinter.printHistory(ex, LOGGER);
                    }
                });
            });
            // add listener to socket events
            socket.on(Socket.EVENT_CONNECT, objects -> {
                // when socket is connected
                LOGGER.info("Socket of user[" + userId + "] connected");
                login();
            }).on(Socket.EVENT_MESSAGE, objects -> {
                // received a message
                LOGGER.info("Socket of user[" + userId + "] received a request");
                // handle request
                handleRequest(objects[0], (Ack) objects[objects.length - 1]);
            }).on(Socket.EVENT_DISCONNECT, objects -> {
                // reconnection is automatically done by the socket API, just print that disconnected
                LOGGER.info("Socket of user[" + userId + "] disconnected");
            }).on(USER_TRANSIT_EVENT, objects -> {
                LOGGER.info("Socket of user[" + userId + "] received transit state request");
                handleUserTransitUpdate(objects[0], (Ack) objects[objects.length - 1]);
            });

            // add observer to registry that triggers sync requests on changes
            Registries.getUnitRegistry().addDataObserver(unitRegistryObserver);
        } catch (JPNotAvailableException | CouldNotPerformException ex) {
            throw new InitializationException(this, ex);
        }
    }

    private void handleRequest(final Object request, final Ack acknowledgement) {
        try {
            // parse as json
            final JsonElement parse = jsonParser.parse((String) request);
            LOGGER.info("Request: " + gson.toJson(parse));

            //TODO: fulfillment handlers also need authorization token...
            // handle request and create response
            LOGGER.info("Call handler");
            final JsonObject jsonObject = FulfillmentHandler.handleRequest(parse.getAsJsonObject(), agentUserId, tokenStore.getToken(userId + "@BCO"));
            final String response = gson.toJson(jsonObject);
            LOGGER.info("Handler produced response: " + response);
            // send back response
            acknowledgement.call(response);
        } catch (Exception ex) {
            // send back an error response
            final JsonObject response = new JsonObject();
            final JsonObject payload = new JsonObject();
            response.add(FulfillmentHandler.PAYLOAD_KEY, payload);
            FulfillmentHandler.setError(payload, ex, ErrorCode.UNKNOWN_ERROR);
            acknowledgement.call(gson.toJson(response));
        }
    }

    private Future<Void> register() {
        final CompletableFuture<Void> registrationFuture = new CompletableFuture<>();
        socket.emit(REGISTER_EVENT, gson.toJson(loginData), (Ack) objects -> {
            try {
                final JsonObject response = jsonParser.parse(objects[0].toString()).getAsJsonObject();
                if (response.get(SUCCESS_KEY).getAsBoolean()) {
                    // clear login data
                    loginData = null;
                    // save received token
                    tokenStore.addToken(userId + "@Cloud", response.get(TOKEN_KEY).getAsString());
                    // complete registration future, so that waiting tasks know that is is finished
                    registrationFuture.complete(null);
                } else {
                    LOGGER.error("Could not login user[" + userId + "] at BCO Cloud: " + response.get(ERROR_KEY).getAsString());
                    registrationFuture.completeExceptionally(new CouldNotPerformException("Could not register user"));
                }
            } catch (ArrayIndexOutOfBoundsException | ClassCastException ex) {
                ExceptionPrinter.printHistory("Unexpected response for login request", ex, LOGGER);
                registrationFuture.completeExceptionally(new CouldNotPerformException("Could not register user"));
            }
        });
        return registrationFuture;
    }

    private void login() {
        // this has to be done on another thread because the socket library uses a single event thread
        // so without this it is not possible to wait for the registration to finish
        GlobalCachedExecutorService.submit(() -> {
            if (loginData != null) {
                // register user
                try {
                    register().get(10, TimeUnit.SECONDS);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    ExceptionPrinter.printHistory(ex, LOGGER);
                    loginFuture.completeExceptionally(new CouldNotPerformException("Could not register user[" + userId + "] at BCO Cloud", ex));
                    return;
                } catch (ExecutionException | TimeoutException ex) {
                    ExceptionPrinter.printHistory(ex, LOGGER);
                    loginFuture.completeExceptionally(new CouldNotPerformException("Could not register user[" + userId + "] at BCO Cloud", ex));
                    return;
                }
            }

            final JsonObject loginInfo = new JsonObject();
            try {
                loginInfo.addProperty(TOKEN_KEY, tokenStore.getToken(userId + "@Cloud"));
            } catch (NotAvailableException ex) {
                ExceptionPrinter.printHistory("Could not login user[" + userId + "] at BCO Cloud", ex, LOGGER);
                return;
            }

            LOGGER.info("Send loginInfo [" + gson.toJson(loginInfo) + "]");
            socket.emit(LOGIN_EVENT, gson.toJson(loginInfo), (Ack) objects -> {
                try {
                    final JsonObject response = jsonParser.parse(objects[0].toString()).getAsJsonObject();
                    if (response.get(SUCCESS_KEY).getAsBoolean()) {
                        LOGGER.info("Logged in [" + userId + "] successfully");
                        loginFuture.complete(null);
                    } else {
                        LOGGER.info("Could not login user[" + userId + "] at BCO Cloud: " + response.get(ERROR_KEY));
                        loginFuture.completeExceptionally(new CouldNotPerformException("Could not login user[" + userId + "] at BCO Cloud: " + response.get(ERROR_KEY)));
                    }
                } catch (ArrayIndexOutOfBoundsException | ClassCastException ex) {
                    ExceptionPrinter.printHistory("Unexpected response for login request", ex, LOGGER);
                    loginFuture.completeExceptionally(new CouldNotPerformException("Could not login user[" + userId + "] at BCO Cloud", ex));
                }
            });
        });
    }

    private void handleUserTransitUpdate(final Object object, final Ack acknowledgement) {
        final String transit = (String) object;
        LOGGER.info("Received user transit " + transit);
        try {
            UserTransitState.State state = Enum.valueOf(UserTransitState.State.class, transit);
            final UserRemote userRemote = Units.getUnit(userId, false, UserRemote.class);
            userRemote.setUserTransitState(UserTransitState.newBuilder().setValue(state).build()).get(3, TimeUnit.SECONDS);
            acknowledgement.call("SUCCESS");
        } catch (InterruptedException ex) {
            acknowledgement.call("FAILED");
            Thread.currentThread().interrupt();
            ExceptionPrinter.printHistory(ex, LOGGER);
            // this should not happen since wait for data is not called
        } catch (CouldNotPerformException | ExecutionException ex) {
            acknowledgement.call("FAILED");
            ExceptionPrinter.printHistory(ex, LOGGER);
        } catch (TimeoutException ex) {
            acknowledgement.call("TIMEOUT");
            ExceptionPrinter.printHistory(ex, LOGGER);
        } catch (IllegalArgumentException ex) {
            LOGGER.error("Could not resolve transit as enum value");
            acknowledgement.call("FAILED");
        }
    }

    @Override
    public void activate() throws CouldNotPerformException {
        loginFuture = new CompletableFuture<>();
        if (socket == null) {
            throw new CouldNotPerformException("Cannot activate before initialization");
        }
        active = true;
        socket.connect();
    }

    @Override
    public void deactivate() throws CouldNotPerformException {
        if (socket == null) {
            throw new CouldNotPerformException("Cannot deactivate before initialization");
        }
        socket.disconnect();
        active = false;
        loginFuture = null;
    }

    @Override
    public boolean isActive() {
        return active;
    }

    private boolean isLoggedIn() {
        return loggedIn;
    }

    private class UnitRegistryObserver implements Observer<UnitRegistryData> {
        private JsonObject lastSyncResponse = null;
        private Future syncRequestFuture = null;

        @Override
        public void update(final Observable<UnitRegistryData> source, final UnitRegistryData data) throws Exception {

            // build a response for a sync request
            final JsonObject syncResponse = new JsonObject();
            FulfillmentHandler.handleSync(syncResponse, agentUserId);
            // return if the response to a sync has not changed since the last time
            if (lastSyncResponse != null && lastSyncResponse.hashCode() == syncResponse.hashCode()) {
                return;
            }
            lastSyncResponse = syncResponse;

            if (isLoggedIn()) {
                // trigger sync if socket is connected and logged in
                //TODO: parse response for an error an write a warning
                socket.emit(REQUEST_SYNC_EVENT, (Ack) objects1 -> LOGGER.info("Received response: " + objects1.getClass().getName()));
            } else {
                // not logged or connected so create a task that will trigger the sync when logged in
                if (syncRequestFuture != null && !syncRequestFuture.isDone()) {
                    // task has already been created an is running
                    return;
                }

                // create task
                syncRequestFuture = GlobalCachedExecutorService.submit((Callable<Void>) () -> {
                    System.out.println("Start task to wait for logged in before requesting sync");
                    // wait until logged in
                    synchronized (syncRequestLock) {
                        if (!isLoggedIn()) {
                            syncRequestLock.wait();
                        }
                    }

                    System.out.println("Request sync");
                    // trigger sync
                    //TODO: parse response for an error an write a warning
                    socket.emit("requestSync", (Ack) objects1 -> LOGGER.info("Received response: " + objects1.getClass().getName()));
                    return null;
                });
            }

            // debug print
            JsonObject test = new JsonObject();
            test.addProperty(FulfillmentHandler.REQUEST_ID_KEY, "12345678");
            test.add(FulfillmentHandler.PAYLOAD_KEY, syncResponse);
            LOGGER.info("new sync[" + isLoggedIn() + "]:\n" + gson.toJson(test));
        }
    }

    public Future<Void> getLoginFuture() {
        return loginFuture;
    }
}
