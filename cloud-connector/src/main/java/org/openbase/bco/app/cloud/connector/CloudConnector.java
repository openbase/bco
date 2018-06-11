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

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.socket.client.Ack;
import io.socket.client.IO;
import io.socket.client.Socket;
import org.openbase.bco.app.cloud.connector.jp.JPCloudServerURI;
import org.openbase.jps.core.JPService;
import org.openbase.jps.exception.JPNotAvailableException;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InitializationException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.iface.Launchable;
import org.openbase.jul.iface.VoidInitializable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;

/**
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class CloudConnector implements Launchable<Void>, VoidInitializable {

    private final static Logger LOGGER = LoggerFactory.getLogger(CloudConnector.class);

    private final FulfillmentHandler fulfillmentHandler;
    private final JsonParser jsonParser;
    private final Gson gson;

    private Socket socket;
    private boolean active;
    private URI cloudURI;

    public CloudConnector() {
        this.active = false;
        this.fulfillmentHandler = new FulfillmentHandler();
        this.jsonParser = new JsonParser();
        this.gson = new Gson();
    }

    @Override
    public void init() throws InitializationException {
        try {
            // get cloud uri from property
            cloudURI = JPService.getProperty(JPCloudServerURI.class).getValue();

            // create socket
            socket = IO.socket(cloudURI);

            // add listener to socket events
            socket.on(Socket.EVENT_CONNECT, objects -> {
                // when socket is connected
                LOGGER.info("CONNECTED");

                // authenticate
                LOGGER.info("Authenticate...");
//                final String pwd = "DevelopmentAccess!";
                final String pwd = "socketPassword";
                socket.emit("authenticate", pwd, (Ack) objects1 -> {
                    try {
                        final String response = (String) objects1[0];
                        if (response.contains("failed")) {
                            LOGGER.error("Authentication with cloud[" + cloudURI.toString() + "] failed");
                            shutdown();
                        } else {
                            LOGGER.info("Authenticated successfully");
                        }
                    } catch (ArrayIndexOutOfBoundsException | ClassCastException ex) {
                        ExceptionPrinter.printHistory("Unexpected response for authentication request", ex, LOGGER);
                        deactivate();
                    }
                });
            }).on(Socket.EVENT_MESSAGE, objects -> {
                // received a message
                LOGGER.info("Received message: " + objects[0]);
                // parse as json
                final JsonElement parse = jsonParser.parse((String) objects[0]);

                // handle request and create response
                LOGGER.info("Call handler");
                final JsonObject jsonObject = fulfillmentHandler.handleRequest(parse.getAsJsonObject());
                final String response = gson.toJson(jsonObject);

                // get acknowledgement object
                final Ack ack = (Ack) objects[objects.length - 1];
                LOGGER.info("Handler produced response: " + response);
                // send back response
                ack.call(response);
            }).on(Socket.EVENT_DISCONNECT, objects -> {

                //TODO: what when server down? try to reconnect every 30 seconds or so?
//                if (active) {
//                    socket.connect();
//                }
                for (Object object : objects) {
                    LOGGER.info(object.toString());
                }
                // when disconnected
                LOGGER.info("Socket disconnected: " + objects[0].toString());
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
    }

    @Override
    public boolean isActive() {
        return active;
    }
}
