package org.openbase.bco.app.cloudconnector;

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
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestMain {

    private static final Logger LOGGER = LoggerFactory.getLogger(TestMain.class);

    public static void main(String[] args) {
        final Gson gson = new GsonBuilder().setPrettyPrinting().create();
        final FulfillmentHandler fulfillmentHandler = new FulfillmentHandler();
        final JsonParser jsonParser = new JsonParser();

        JsonObject a = jsonParser.parse("{\"responseId\":\"1e3d50a1-52c8-49a7-8e47-8689727b8019\",\"queryResult\":{\"queryText\":\"ich koche\",\"parameters\":{\"activity\":\"kochen\"},\"allRequiredParamsPresent\":true,\"fulfillmentMessages\":[{\"text\":{\"text\":[\"\"]}}],\"outputContexts\":[{\"name\":\"projects/actions-bco/agent/sessions/1534795865708/contexts/actions_capability_screen_output\",\"parameters\":{\"activity.original\":\"koche\",\"activity\":\"kochen\"}},{\"name\":\"projects/actions-bco/agent/sessions/1534795865708/contexts/actions_capability_audio_output\",\"parameters\":{\"activity.original\":\"koche\",\"activity\":\"kochen\"}},{\"name\":\"projects/actions-bco/agent/sessions/1534795865708/contexts/google_assistant_input_type_keyboard\",\"parameters\":{\"activity.original\":\"koche\",\"activity\":\"kochen\"}},{\"name\":\"projects/actions-bco/agent/sessions/1534795865708/contexts/actions_capability_web_browser\",\"parameters\":{\"activity.original\":\"koche\",\"activity\":\"kochen\"}},{\"name\":\"projects/actions-bco/agent/sessions/1534795865708/contexts/actions_capability_media_response_audio\",\"parameters\":{\"activity.original\":\"koche\",\"activity\":\"kochen\"}}],\"intent\":{\"name\":\"projects/actions-bco/agent/intents/eaf22c5a-3426-4715-8baf-f0e33ef12f57\",\"displayName\":\"user_activity\"},\"intentDetectionConfidence\":1,\"languageCode\":\"de\"},\"originalDetectIntentRequest\":{\"source\":\"google\",\"version\":\"2\",\"payload\":{\"isInSandbox\":true,\"surface\":{\"capabilities\":[{\"name\":\"actions.capability.WEB_BROWSER\"},{\"name\":\"actions.capability.SCREEN_OUTPUT\"},{\"name\":\"actions.capability.MEDIA_RESPONSE_AUDIO\"},{\"name\":\"actions.capability.AUDIO_OUTPUT\"}]},\"inputs\":[{\"rawInputs\":[{\"query\":\"ich koche\",\"inputType\":\"KEYBOARD\"}],\"arguments\":[{\"rawText\":\"ich koche\",\"textValue\":\"ich koche\",\"name\":\"text\"}],\"intent\":\"actions.intent.TEXT\"}],\"user\":{\"userStorage\":\"{\\\"data\\\":{}}\",\"lastSeen\":\"2018-08-20T20:07:57Z\",\"accessToken\":\"3gG/PDufX1P53AnSU+ZH1Q==\",\"locale\":\"de-DE\",\"userId\":\"ABwppHF9kx1cVAHisoLjrun2mLf1yBLpBTCYFsJCfaGiDSzYWkJBlR0BF5Jc6aOvzLgnddclQqX_99s\"},\"conversation\":{\"conversationId\":\"1534795865708\",\"type\":\"ACTIVE\",\"conversationToken\":\"[]\"},\"availableSurfaces\":[{\"capabilities\":[{\"name\":\"actions.capability.WEB_BROWSER\"},{\"name\":\"actions.capability.SCREEN_OUTPUT\"},{\"name\":\"actions.capability.AUDIO_OUTPUT\"}]}]}},\"session\":\"projects/actions-bco/agent/sessions/1534795865708\"}").getAsJsonObject();
        System.out.println(gson.toJson(a));
//        try {
//            Registries.waitForData();
//            SessionManager.getInstance().login(Registries.getUnitRegistry().getUserUnitIdByUserName("CSRAUser"), "admin");
//            CloudConnectorAppRemote cloudConnectorAppRemote = new CloudConnectorAppRemote();
//            String token = cloudConnectorAppRemote.generateDefaultAuthorizationToken();
//            LOGGER.info("Generated token [" + token + "]");
//            cloudConnectorAppRemote.setAuthorizationToken(token).get();
//            LOGGER.info(cloudConnectorAppRemote.generateDefaultAuthorizationToken());
//            cloudConnectorAppRemote.remove().get();
//        } catch (Exception ex) {
//            ExceptionPrinter.printHistory(ex, LOGGER);
//            System.exit(1);
//        }

        System.exit(0);

//        RegistrationHelper.createRegistrationData("testPwd", "pleminoq@openbase.org");

//        try {
//            JPService.registerProperty(JPCloudServerURI.class, new URI("http://localhost:5000"));
//            CloudConnectorLauncher cloudConnectorLauncher = new CloudConnectorLauncher();
//            LOGGER.info("launch cloud connector...");
//            cloudConnectorLauncher.launch();
//            CloudConnector cloudConnector = cloudConnectorLauncher.getLaunchable();
//            LOGGER.info("Cloud connector is active: " + cloudConnector.isActive());
//            Thread.sleep(1000);
//            cloudConnector.requestSync();
//        } catch (Exception ex) {
//            ExceptionPrinter.printHistory(ex, LOGGER);
//        }

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
//        try {
//            Registries.waitForData();
//            SystemLogin.loginBCOUser();
//        } catch (CouldNotPerformException e) {
//            e.printStackTrace();
//        } catch (InterruptedException e) {
//            e.printStackTrace();
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
//            Registries.waitForData();
//            for (UnitConfig unitConfig : Registries.getUnitRegistry(true).getUnitConfigs(UnitType.POWER_SWITCH)) {
//                JsonObject device = new JsonObject();
//                device.addProperty("id", unitConfig.getId());
//                devices.add(device);
//            }
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
////            JsonObject jsonObject = fulfillmentHandler.handleRequest(request);
////            System.out.println(gson.toJson(jsonObject));
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
