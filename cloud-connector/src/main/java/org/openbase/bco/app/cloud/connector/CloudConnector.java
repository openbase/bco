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
import org.openbase.bco.authentication.lib.jp.JPAuthentication;
import org.openbase.bco.dal.remote.unit.Units;
import org.openbase.bco.dal.remote.unit.user.UserRemote;
import org.openbase.bco.registry.login.SystemLogin;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.bco.registry.unit.lib.UnitRegistry;
import org.openbase.jps.core.JPService;
import org.openbase.jps.exception.JPNotAvailableException;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InitializationException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.extension.rst.processing.MetaConfigPool;
import org.openbase.jul.extension.rst.processing.MetaConfigVariableProvider;
import org.openbase.jul.iface.Launchable;
import org.openbase.jul.iface.VoidInitializable;
import org.openbase.jul.pattern.ObservableImpl;
import org.openbase.jul.schedule.GlobalCachedExecutorService;
import org.openbase.jul.schedule.SyncObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rst.domotic.state.UserTransitStateType.UserTransitState;
import rst.domotic.state.UserTransitStateType.UserTransitState.State;
import rst.domotic.unit.UnitConfigType.UnitConfig;

import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

/**
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class CloudConnector implements Launchable<Void>, VoidInitializable {

    private static final Logger LOGGER = LoggerFactory.getLogger(CloudConnector.class);

    private static final String CLOUD_USERNAME_KEY = "CLOUD_USERNAME";
    private static final String CLOUD_PASSWORD_KEY = "CLOUD_PASSWORD";

    private final FulfillmentHandler fulfillmentHandler;
    private final JsonParser jsonParser;
    private final Gson gson;

    private final SyncObject syncRequestLock = new SyncObject("SyncRequestLock");
    private Future syncRequestFuture = null;

    private Socket socket;
    private boolean active;
    private String accessToken;

    private final ObservableImpl<JsonObject> syncPayloadObservable;

    private boolean loggedIn = false;

    public CloudConnector() {
        this.active = false;
        this.fulfillmentHandler = new FulfillmentHandler();
        this.jsonParser = new JsonParser();
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        this.syncPayloadObservable = new ObservableImpl<>();
    }

    @Override
    public void init() throws InitializationException {
        try {
            // add observer to unit registry that test if the sync request would be answered differently on changes
            try {
                Registries.getUnitRegistry().addDataObserver((observable, unitRegistryData) -> {
                    final JsonObject syncResponsePayload = new JsonObject();
                    fulfillmentHandler.handleSync(syncResponsePayload);
                    syncPayloadObservable.notifyObservers(syncResponsePayload);
                });
            } catch (NotAvailableException ex) {
                throw new InitializationException(this, ex);
            }

            try {
                Registries.getUnitRegistry().waitForData();
            } catch (CouldNotPerformException ex) {
                throw new InitializationException(this, ex);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                throw new InitializationException(this, ex);
            }
            // get cloud uri from property
            final URI cloudURI = JPService.getProperty(JPCloudServerURI.class).getValue();

            // parse bco id, cloud password and cloud username
            final UnitConfig bcoUser = Registries.getUnitRegistry().getUnitConfigByAlias(UnitRegistry.BCO_USER_ALIAS);
            final String bcoId = bcoUser.getId();
            MetaConfigPool metaConfigPool = new MetaConfigPool();
            metaConfigPool.register(new MetaConfigVariableProvider("BCOUserMetaConfig", bcoUser.getMetaConfig()));
            final String cloudUsername = metaConfigPool.getValue(CLOUD_USERNAME_KEY);
            final String cloudPassword = metaConfigPool.getValue(CLOUD_PASSWORD_KEY);

            // create socket
            socket = IO.socket(cloudURI);

            //TODO: send id as a combination of BCO (and logged in user id), and token if available, else handle authentication
            // add id to header for cloud server
            socket.io().on(Manager.EVENT_TRANSPORT, args -> {
                Transport transport = (Transport) args[0];

                transport.on(Transport.EVENT_REQUEST_HEADERS, args1 -> {
                    try {
                        @SuppressWarnings("unchecked")
                        Map<String, List<String>> headers = (Map<String, List<String>>) args1[0];
                        // add bco id to request header
                        headers.put("id", Collections.singletonList(bcoId));
                    } catch (Exception ex) {
                        ExceptionPrinter.printHistory(ex, LOGGER);
                    }
                });
            });

            // trigger syncRequest from google when the payload of a sync request would change
            syncPayloadObservable.addObserver((observable, jsonObject) -> {
                if (isLoggedIn()) {
                    // trigger sync if socket is connected and logged in
                    //TODO: parse response for an error an write a warning
                    socket.emit("requestSync", (Ack) objects1 -> LOGGER.info("Received response: " + objects1.getClass().getName()));
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
                test.add(FulfillmentHandler.PAYLOAD_KEY, jsonObject);
                LOGGER.info("new sync[" + isLoggedIn() + "]:\n" + gson.toJson(test));
            });

            // add listener to socket events
            socket.on(Socket.EVENT_CONNECT, objects -> {
                // when socket is connected
                LOGGER.info("CONNECTED");

                JsonObject loginInfo = new JsonObject();
                loginInfo.addProperty("username", cloudUsername);
                loginInfo.addProperty("password", cloudPassword);
                LOGGER.info("Send loginInfo [" + gson.toJson(loginInfo) + "]");
                socket.emit("login", gson.toJson(loginInfo), (Ack) objects1 -> {
                    try {
                        final String resp = objects1[0].toString();
                        if (resp.startsWith("ERROR")) {
                            // TODO: handle this
                            LOGGER.error("Could not login at cloud server[" + resp + "]");
                            return;
                        }

                        final JsonObject response = jsonParser.parse(resp).getAsJsonObject();
                        if (response.get("success").getAsBoolean()) {
                            LOGGER.info("Authenticated successfully");
                            if (response.has("accessToken")) {
                                setLoggedIn(true);
                                accessToken = response.get("accessToken").getAsString();
                                LOGGER.info("Received accessToken[" + accessToken + "]");
                            }
                        } else {
                            LOGGER.info("Authentication failed");
                        }
                    } catch (ArrayIndexOutOfBoundsException | ClassCastException ex) {
                        ExceptionPrinter.printHistory("Unexpected response for authentication request", ex, LOGGER);
                        deactivate();
                    }
                });
            }).on(Socket.EVENT_MESSAGE, objects -> {
                // received a message
                LOGGER.info("Received request!");
                // get acknowledgement object
                final Ack ack = (Ack) objects[objects.length - 1];

                try {
                    // parse as json
                    final JsonElement parse = jsonParser.parse((String) objects[0]);

                    LOGGER.info("Request: " + gson.toJson(parse));

                    // handle request and create response
                    LOGGER.info("Call handler");
                    final JsonObject jsonObject = fulfillmentHandler.handleRequest(parse.getAsJsonObject());
                    final String response = gson.toJson(jsonObject);
                    LOGGER.info("Handler produced response: " + response);
                    // send back response
                    ack.call(response);
                } catch (Exception ex) {


                    // send back an error response
                    final JsonObject response = new JsonObject();
                    final JsonObject payload = new JsonObject();
                    response.add(FulfillmentHandler.PAYLOAD_KEY, payload);
                    FulfillmentHandler.setError(payload, ex, ErrorCode.UNKNOWN_ERROR);
                    ack.call(gson.toJson(response));
                }
            }).on(Socket.EVENT_DISCONNECT, objects -> {
                // reconnection is automatically done by the socket API, just print that disconnected
                LOGGER.info("Socket disconnected: " + objects[0].toString());
            }).on(Socket.EVENT_ERROR, objects -> {
                LOGGER.info("Received error: " + objects[0]);
            }).on("user transit", objects -> {
                final String transit = (String) objects[0];
                LOGGER.info("Received user transit " + transit);
                final Ack ack = (Ack) objects[objects.length - 1];
                try {
                    State state = Enum.valueOf(State.class, transit);
                    final UserRemote userRemote = Units.getUnit(bcoId, false, UserRemote.class);
                    userRemote.setUserTransitState(UserTransitState.newBuilder().setValue(state).build()).get(3, TimeUnit.SECONDS);
                    ack.call("SUCCESS");
                } catch (InterruptedException ex) {
                    ack.call("FAILED");
                    Thread.currentThread().interrupt();
                    ExceptionPrinter.printHistory(ex, LOGGER);
                    // this should not happen since wait for data is not called
                } catch (CouldNotPerformException | ExecutionException ex) {
                    ack.call("FAILED");
                    ExceptionPrinter.printHistory(ex, LOGGER);
                } catch (TimeoutException ex) {
                    ack.call("TIMEOUT");
                    ExceptionPrinter.printHistory(ex, LOGGER);
                } catch (IllegalArgumentException ex) {
                    LOGGER.error("Could not resolve transit as enum value");
                    ack.call("FAILED");
                }
            });
        } catch (JPNotAvailableException | CouldNotPerformException ex) {
            throw new InitializationException(this, ex);
        }
    }

    @Override
    public void activate() throws CouldNotPerformException {
        //TODO: this is just a workaround for current authentication
        try {
            if(JPService.getProperty(JPAuthentication.class).getValue()) {
                SystemLogin.loginBCOUser();
            }
        } catch (JPNotAvailableException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        if (socket != null) {
            socket.connect();
            active = true;
        } else {
            throw new CouldNotPerformException("Could not activate because not initialized");
        }
    }

    public void requestSync() {
        if (socket != null) {
            socket.emit("requestSync");
        }
    }

    @Override
    public void deactivate() {
        if (socket != null) {
            active = false;
            LOGGER.info("Disconnect from deactivate");
            socket.disconnect();
        }

        if (syncRequestFuture != null && !syncRequestFuture.isDone()) {
            syncRequestFuture.cancel(true);
        }
    }

    @Override
    public boolean isActive() {
        return active;
    }

    private void setLoggedIn(final boolean loggedIn) {
        synchronized (syncRequestLock) {
            this.loggedIn = loggedIn;
            syncRequestLock.notifyAll();
        }
    }

    public boolean isLoggedIn() {
        return loggedIn;
    }
}
