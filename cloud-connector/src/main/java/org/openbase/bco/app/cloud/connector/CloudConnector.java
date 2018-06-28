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
import org.openbase.jul.pattern.ObservableImpl;
import org.openbase.jul.schedule.GlobalCachedExecutorService;
import org.openbase.jul.schedule.SyncObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

/**
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class CloudConnector implements Launchable<Void>, VoidInitializable {

    private static final Logger LOGGER = LoggerFactory.getLogger(CloudConnector.class);

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
            try {
                Registries.getUnitRegistry().waitForData();
            } catch (CouldNotPerformException ex) {
                throw new InitializationException(this, ex);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                throw new InitializationException(this, ex);
            }
            // get cloud uri from property
            URI cloudURI = JPService.getProperty(JPCloudServerURI.class).getValue();

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
                        headers.put("id", Collections.singletonList(Registries.getUnitRegistry().getUnitConfigByAlias(UnitRegistry.BCO_USER_ALIAS).getId()));
                    } catch (Exception ex) {
                        ExceptionPrinter.printHistory(ex, LOGGER);
                    }
                });
            });

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

            // trigger syncRequest from google when the payload of a sync request would change
            syncPayloadObservable.addObserver((observable, jsonObject) -> {
                if (isLoggedIn()) {
                    // trigger sync if socket is connected and logged in
                    socket.emit("requestSync");
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
                        socket.emit("requestSync");
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
                loginInfo.addProperty("username", "BCOUser");
                loginInfo.addProperty("password", "password_bco");
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

                //TODO: what when server down? try to reconnect every 30 seconds or shutdown?
//                if (active) {
//                    socket.connect();
//                }
                for (Object object : objects) {
                    LOGGER.info(object.toString());
                }
                // when disconnected
                LOGGER.info("Socket disconnected: " + objects[0].toString());
            }).on(Socket.EVENT_ERROR, objects -> {
                LOGGER.info("Received error: " + objects[0]);
            });
        } catch (JPNotAvailableException ex) {
            throw new InitializationException(this, ex);
        }
    }

    @Override
    public void activate() throws CouldNotPerformException {
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
