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
import com.google.gson.GsonBuilder;
import org.openbase.bco.app.cloud.connector.jp.JPCloudServerURI;
import org.openbase.jps.core.JPService;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;

public class TestMain {

    private static final Logger LOGGER = LoggerFactory.getLogger(TestMain.class);

    public static void main(String[] args) {
        final Gson gson = new GsonBuilder().setPrettyPrinting().create();
        final FulfillmentHandler fulfillmentHandler = new FulfillmentHandler();

        try {
            JPService.registerProperty(JPCloudServerURI.class, new URI("http://localhost:5000"));
            CloudConnectorLauncher cloudConnectorLauncher = new CloudConnectorLauncher();
            LOGGER.info("launch cloud connector...");
            cloudConnectorLauncher.launch();
            CloudConnector cloudConnector = cloudConnectorLauncher.getLaunchable();
            LOGGER.info("Cloud connector is active: " + cloudConnector.isActive());
            Thread.sleep(1000);
            cloudConnector.requestSync();
        } catch (Exception ex) {
            ExceptionPrinter.printHistory(ex, LOGGER);
        }

//        try {
//            JPService.registerProperty(JPCloudServerURI.class);
////            String[] testArgs = {"--cloud-server", "localhost"};
//            String[] testArgs = {};
//            JPService.parse(testArgs);
//            URI uri = JPService.getProperty(JPCloudServerURI.class).getValue();
//            LOGGER.info(uri.getPath());
//            LOGGER.info(String.valueOf(uri.getPort()));
//            LOGGER.info(uri.getUserInfo());
//            LOGGER.info(uri.getAuthority());
//            LOGGER.info(uri.getHost());
//            LOGGER.info(uri.getScheme());
//        } catch (JPServiceException ex) {
//            LOGGER.info("", ex);
//        }
//        LOGGER.info(new CouldNotPerformException("Could not do this").toString());

//         test sync
//        JsonObject syncRequest = new JsonObject();
//        syncRequest.addProperty(FulfillmentHandler.REQUEST_ID_KEY, /*UUID.randomUUID().toString()*/"id");
//        JsonArray inputs = new JsonArray();
//        JsonObject input = new JsonObject();
//        inputs.add(input);
//        input.addProperty("intent", "action.devices.SYNC");
//        syncRequest.add("inputs", inputs);
//        System.out.println(gson.toJson(syncRequest).replace("\"", "\\\""));
//
//        JsonObject jsonObject = fulfillmentHandler.handleRequest(syncRequest);
//        System.out.println(gson.toJson(jsonObject));

        // test query
//        JsonObject query = new JsonObject();
//        query.addProperty(FulfillmentHandler.REQUEST_ID_KEY, UUID.randomUUID().toString());
//        JsonArray inputs = new JsonArray();
//        query.add("inputs", inputs);
//        JsonObject input = new JsonObject();
//        inputs.add(input);
//        input.addProperty("intent", "action.devices.QUERY");
//        JsonObject payload = new JsonObject();
//        input.add("payload", payload);
//        JsonArray devices = new JsonArray();
//        payload.add("devices", devices);
//        try {
//            for (UnitConfig unitConfig : Registries.getUnitRegistry(true).getUnitConfigs(UnitType.LIGHT)) {
//                JsonObject device = new JsonObject();
//                device.addProperty("id", unitConfig.getId());
//                devices.add(device);
//            }
//
//            System.out.println(gson.toJson(query));
//
//            JsonObject jsonObject = fulfillmentHandler.handleRequest(query);
//            System.out.println(gson.toJson(jsonObject));
//        } catch (Exception ex) {
//            ExceptionPrinter.printHistory(ex, LoggerFactory.getLogger(TestMain.class));
//            System.exit(1);
//        }

        // test execution
//        JsonObject request = new JsonObject();
//        request.addProperty(FulfillmentHandler.REQUEST_ID_KEY, UUID.randomUUID().toString());
//        JsonArray inputs = new JsonArray();
//        request.add("inputs", inputs);
//        JsonObject input = new JsonObject();
//        inputs.add(input);
//        input.addProperty("intent", "action.devices.EXECUTE");
//        JsonObject payload = new JsonObject();
//        input.add("payload", payload);
//        JsonArray commands = new JsonArray();
//        payload.add("commands", commands);
//        JsonObject command = new JsonObject();
//        commands.add(command);
//        JsonArray devices = new JsonArray();
//        command.add("devices", devices);
//        try {
//            for (UnitConfig unitConfig : Registries.getUnitRegistry(true).getUnitConfigs(UnitType.LIGHT)) {
//                JsonObject device = new JsonObject();
//                device.addProperty("id", unitConfig.getId());
//                devices.add(device);
//            }
//
//            JsonArray execution = new JsonArray();
//            command.add("execution", execution);
//            JsonObject execute = new JsonObject();
//            execution.add(execute);
//            execute.addProperty("command", "action.devices.commands.OnOff");
//            JsonObject params = new JsonObject();
//            execute.add("params", params);
//            params.addProperty("on", true);
//
//            System.out.println(gson.toJson(request));
//
//            JsonObject jsonObject = fulfillmentHandler.handleRequest(request);
//            System.out.println(gson.toJson(jsonObject));
//        } catch (Exception ex) {
//            StackTracePrinter.printStackTrace(ex.getStackTrace(), LoggerFactory.getLogger(TestMain.class), LogLevel.INFO);
//            ExceptionPrinter.printHistory(ex, LoggerFactory.getLogger(TestMain.class));
//            System.exit(1);
//        }
//        System.exit(0);


//        try {
//            Socket socket = IO.socket("http://localhost:5000");
//            Socket socket = IO.socket("https://bco-cloud.herokuapp.com/");
//            socket.on(Socket.EVENT_CONNECT, objects -> {
//                LOGGER.info("CONNECTED");
//
//                LOGGER.info("Authenticate...");
//                final String pwd = "DevelopmentAccess!";
//                socket.emit("authenticate", pwd, (Ack) objects1 -> LOGGER.info("Authentication: " + objects1[0].toString()));
//
////                socket.disconnect();
//            }).on(Socket.EVENT_MESSAGE, objects -> {
//                LOGGER.info("Received message: " + objects[0]);
//                JsonParser jsonParser = new JsonParser();
//                JsonElement parse = jsonParser.parse((String) objects[0]);
//
//                try {
//                    LOGGER.info("Call handler");
//                    JsonObject jsonObject = fulfillmentHandler.handleRequest(parse.getAsJsonObject());
//                    final Ack ack = (Ack) objects[objects.length - 1];
//                    String response = gson.toJson(jsonObject);
//                    LOGGER.info("Handler produced response: " + response);
//                    ack.call(response);
//                } catch (CouldNotPerformException ex) {
//                    // todo handle error
//                }
//            }).on(Socket.EVENT_DISCONNECT, objects -> {
//                LOGGER.info("Socket disconnected: " + objects[0].toString());
//            });
//
//            LOGGER.info("Try to connect to socket[" + socket.id() + "]");
//            socket.connect();
//            LOGGER.info("Connected to socket[" + socket + "]");
//
////            socket.emit("test", "hello", new Ack() {
////                @Override
////                public void call(Object... objects) {
////                    System.out.println("Server received message: ");
////                    for (Object object : objects) {
////                        System.out.println(object);
////                    }
////                }
////            });
//            while (!Thread.currentThread().isInterrupted()) {
//                Thread.sleep(5000);
//            }
//
//
//            LOGGER.info("Disconnect socket[" + socket + "]");
//            socket.disconnect();
//            LOGGER.info("Disconnected socket[" + socket + "]");
//        } catch (Exception ex) {
//            ExceptionPrinter.printHistory(new CouldNotPerformException("Could not create socket client", ex), LOGGER);
//        }
    }
}
