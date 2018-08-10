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

        JsonObject a = jsonParser.parse("{\"responses\":[],\"expectUserResponse\":true,\"digested\":false,\"_responded\":false,\"request\":{\"isInSandbox\":true,\"surface\":{\"capabilities\":[{\"name\":\"actions.capability.WEB_BROWSER\"},{\"name\":\"actions.capability.MEDIA_RESPONSE_AUDIO\"},{\"name\":\"actions.capability.SCREEN_OUTPUT\"},{\"name\":\"actions.capability.AUDIO_OUTPUT\"}]},\"inputs\":[{\"rawInputs\":[{\"query\":\"ich koche\",\"inputType\":\"KEYBOARD\"}],\"arguments\":[{\"rawText\":\"ich koche\",\"textValue\":\"ich koche\",\"name\":\"text\"}],\"intent\":\"actions.intent.TEXT\"}],\"user\":{\"userStorage\":\"{\\\"data\\\":{}}\",\"lastSeen\":\"2018-08-02T14:07:53Z\",\"accessToken\":\"EI8wRkkEG1Pek8msvZZo5Q==\",\"locale\":\"de-DE\",\"userId\":\"ABwppHF9kx1cVAHisoLjrun2mLf1yBLpBTCYFsJCfaGiDSzYWkJBlR0BF5Jc6aOvzLgnddclQqX_99s\"},\"conversation\":{\"conversationId\":\"1533219692859\",\"type\":\"ACTIVE\",\"conversationToken\":\"[]\"},\"availableSurfaces\":[{\"capabilities\":[{\"name\":\"actions.capability.WEB_BROWSER\"},{\"name\":\"actions.capability.SCREEN_OUTPUT\"},{\"name\":\"actions.capability.AUDIO_OUTPUT\"}]}]},\"headers\":{\"host\":\"bco-cloud.herokuapp.com\",\"connection\":\"close\",\"accept\":\"text/plain, */*\",\"content-type\":\"application/json; charset=UTF-8\",\"accept-charset\":\"big5, big5-hkscs, cesu-8, euc-jp, euc-kr, gb18030, gb2312, gbk, ibm-thai, ibm00858, ibm01140, ibm01141, ibm01142, ibm01143, ibm01144, ibm01145, ibm01146, ibm01147, ibm01148, ibm01149, ibm037, ibm1026, ibm1047, ibm273, ibm277, ibm278, ibm280, ibm284, ibm285, ibm290, ibm297, ibm420, ibm424, ibm437, ibm500, ibm775, ibm850, ibm852, ibm855, ibm857, ibm860, ibm861, ibm862, ibm863, ibm864, ibm865, ibm866, ibm868, ibm869, ibm870, ibm871, ibm918, iso-2022-cn, iso-2022-jp, iso-2022-jp-2, iso-2022-kr, iso-8859-1, iso-8859-13, iso-8859-15, iso-8859-2, iso-8859-3, iso-8859-4, iso-8859-5, iso-8859-6, iso-8859-7, iso-8859-8, iso-8859-9, jis_x0201, jis_x0212-1990, koi8-r, koi8-u, shift_jis, tis-620, us-ascii, utf-16, utf-16be, utf-16le, utf-32, utf-32be, utf-32le, utf-8, windows-1250, windows-1251, windows-1252, windows-1253, windows-1254, windows-1255, windows-1256, windows-1257, windows-1258, windows-31j, x-big5-hkscs-2001, x-big5-solaris, x-compound_text, x-euc-jp-linux, x-euc-tw, x-eucjp-open, x-ibm1006, x-ibm1025, x-ibm1046, x-ibm1097, x-ibm1098, x-ibm1112, x-ibm1122, x-ibm1123, x-ibm1124, x-ibm1166, x-ibm1364, x-ibm1381, x-ibm1383, x-ibm300, x-ibm33722, x-ibm737, x-ibm833, x-ibm834, x-ibm856, x-ibm874, x-ibm875, x-ibm921, x-ibm922, x-ibm930, x-ibm933, x-ibm935, x-ibm937, x-ibm939, x-ibm942, x-ibm942c, x-ibm943, x-ibm943c, x-ibm948, x-ibm949, x-ibm949c, x-ibm950, x-ibm964, x-ibm970, x-iscii91, x-iso-2022-cn-cns, x-iso-2022-cn-gb, x-iso-8859-11, x-jis0208, x-jisautodetect, x-johab, x-macarabic, x-maccentraleurope, x-maccroatian, x-maccyrillic, x-macdingbat, x-macgreek, x-machebrew, x-maciceland, x-macroman, x-macromania, x-macsymbol, x-macthai, x-macturkish, x-macukraine, x-ms932_0213, x-ms950-hkscs, x-ms950-hkscs-xp, x-mswin-936, x-pck, x-sjis_0213, x-utf-16le-bom, x-utf-32be-bom, x-utf-32le-bom, x-windows-50220, x-windows-50221, x-windows-874, x-windows-949, x-windows-950, x-windows-iso2022jp\",\"user-agent\":\"Apache-HttpClient/4.5.4 (Java/1.8.0_171)\",\"accept-encoding\":\"gzip,deflate\",\"x-request-id\":\"5f92805c-e453-4aad-9533-1b72e0609db6\",\"x-forwarded-for\":\"104.198.35.201\",\"x-forwarded-proto\":\"https\",\"x-forwarded-port\":\"443\",\"via\":\"1.1 vegur\",\"connect-time\":\"1\",\"x-request-start\":\"1533219698670\",\"total-route-time\":\"0\",\"content-length\":\"3105\",\"authorization\":\"Bearer EI8wRkkEG1Pek8msvZZo5Q==\"},\"sandbox\":true,\"input\":{\"raw\":\"ich koche\",\"type\":\"KEYBOARD\"},\"surface\":{\"capabilities\":{\"list\":[{\"name\":\"actions.capability.WEB_BROWSER\"},{\"name\":\"actions.capability.MEDIA_RESPONSE_AUDIO\"},{\"name\":\"actions.capability.SCREEN_OUTPUT\"},{\"name\":\"actions.capability.AUDIO_OUTPUT\"}]}},\"available\":{\"surfaces\":{\"list\":[{\"capabilities\":{\"list\":[{\"name\":\"actions.capability.WEB_BROWSER\"},{\"name\":\"actions.capability.SCREEN_OUTPUT\"},{\"name\":\"actions.capability.AUDIO_OUTPUT\"}]}}],\"capabilities\":{\"surfaces\":[{\"capabilities\":{\"list\":[{\"name\":\"actions.capability.WEB_BROWSER\"},{\"name\":\"actions.capability.SCREEN_OUTPUT\"},{\"name\":\"actions.capability.AUDIO_OUTPUT\"}]}}]}}},\"user\":{\"raw\":{\"userStorage\":\"{\\\"data\\\":{}}\",\"lastSeen\":\"2018-08-02T14:07:53Z\",\"accessToken\":\"EI8wRkkEG1Pek8msvZZo5Q==\",\"locale\":\"de-DE\",\"userId\":\"ABwppHF9kx1cVAHisoLjrun2mLf1yBLpBTCYFsJCfaGiDSzYWkJBlR0BF5Jc6aOvzLgnddclQqX_99s\"},\"storage\":{},\"_id\":\"ABwppHF9kx1cVAHisoLjrun2mLf1yBLpBTCYFsJCfaGiDSzYWkJBlR0BF5Jc6aOvzLgnddclQqX_99s\",\"locale\":\"de-DE\",\"permissions\":[],\"last\":{\"seen\":\"2018-08-02T14:07:53.000Z\"},\"name\":{},\"entitlements\":[],\"access\":{\"token\":\"EI8wRkkEG1Pek8msvZZo5Q==\"},\"profile\":{}},\"arguments\":{\"parsed\":{\"input\":{\"text\":\"ich koche\"},\"list\":[\"ich koche\"]},\"status\":{\"input\":{},\"list\":[null]},\"raw\":{\"list\":[{\"rawText\":\"ich koche\",\"textValue\":\"ich koche\",\"name\":\"text\"}],\"input\":{\"text\":{\"rawText\":\"ich koche\",\"textValue\":\"ich koche\",\"name\":\"text\"}}}},\"device\":{},\"id\":\"1533219692859\",\"type\":\"ACTIVE\",\"screen\":true,\"body\":{\"responseId\":\"f3f4ca07-711d-402b-8805-1b40d3e773ae\",\"queryResult\":{\"queryText\":\"ich koche\",\"parameters\":{\"activity\":\"koche\"},\"allRequiredParamsPresent\":true,\"fulfillmentMessages\":[{\"text\":{\"text\":[\"\"]}}],\"outputContexts\":[{\"name\":\"projects/actions-bco/agent/sessions/1533219692859/contexts/actions_capability_screen_output\",\"parameters\":{\"activity.original\":\"koche\",\"activity\":\"koche\"}},{\"name\":\"projects/actions-bco/agent/sessions/1533219692859/contexts/actions_capability_audio_output\",\"parameters\":{\"activity.original\":\"koche\",\"activity\":\"koche\"}},{\"name\":\"projects/actions-bco/agent/sessions/1533219692859/contexts/google_assistant_input_type_keyboard\",\"parameters\":{\"activity.original\":\"koche\",\"activity\":\"koche\"}},{\"name\":\"projects/actions-bco/agent/sessions/1533219692859/contexts/actions_capability_web_browser\",\"parameters\":{\"activity.original\":\"koche\",\"activity\":\"koche\"}},{\"name\":\"projects/actions-bco/agent/sessions/1533219692859/contexts/actions_capability_media_response_audio\",\"parameters\":{\"activity.original\":\"koche\",\"activity\":\"koche\"}}],\"intent\":{\"name\":\"projects/actions-bco/agent/intents/eaf22c5a-3426-4715-8baf-f0e33ef12f57\",\"displayName\":\"user_activity\"},\"intentDetectionConfidence\":1,\"languageCode\":\"de\"},\"originalDetectIntentRequest\":{\"source\":\"google\",\"version\":\"2\",\"payload\":{\"isInSandbox\":true,\"surface\":{\"capabilities\":[{\"name\":\"actions.capability.WEB_BROWSER\"},{\"name\":\"actions.capability.MEDIA_RESPONSE_AUDIO\"},{\"name\":\"actions.capability.SCREEN_OUTPUT\"},{\"name\":\"actions.capability.AUDIO_OUTPUT\"}]},\"inputs\":[{\"rawInputs\":[{\"query\":\"ich koche\",\"inputType\":\"KEYBOARD\"}],\"arguments\":[{\"rawText\":\"ich koche\",\"textValue\":\"ich koche\",\"name\":\"text\"}],\"intent\":\"actions.intent.TEXT\"}],\"user\":{\"userStorage\":\"{\\\"data\\\":{}}\",\"lastSeen\":\"2018-08-02T14:07:53Z\",\"accessToken\":\"EI8wRkkEG1Pek8msvZZo5Q==\",\"locale\":\"de-DE\",\"userId\":\"ABwppHF9kx1cVAHisoLjrun2mLf1yBLpBTCYFsJCfaGiDSzYWkJBlR0BF5Jc6aOvzLgnddclQqX_99s\"},\"conversation\":{\"conversationId\":\"1533219692859\",\"type\":\"ACTIVE\",\"conversationToken\":\"[]\"},\"availableSurfaces\":[{\"capabilities\":[{\"name\":\"actions.capability.WEB_BROWSER\"},{\"name\":\"actions.capability.SCREEN_OUTPUT\"},{\"name\":\"actions.capability.AUDIO_OUTPUT\"}]}]}},\"session\":\"projects/actions-bco/agent/sessions/1533219692859\"},\"version\":2,\"action\":\"\",\"intent\":\"user_activity\",\"parameters\":{\"activity\":\"koche\"},\"contexts\":{\"_session\":\"projects/actions-bco/agent/sessions/1533219692859\",\"input\":{\"actions_capability_screen_output\":{\"name\":\"projects/actions-bco/agent/sessions/1533219692859/contexts/actions_capability_screen_output\",\"parameters\":{\"activity.original\":\"koche\",\"activity\":\"koche\"}},\"actions_capability_audio_output\":{\"name\":\"projects/actions-bco/agent/sessions/1533219692859/contexts/actions_capability_audio_output\",\"parameters\":{\"activity.original\":\"koche\",\"activity\":\"koche\"}},\"google_assistant_input_type_keyboard\":{\"name\":\"projects/actions-bco/agent/sessions/1533219692859/contexts/google_assistant_input_type_keyboard\",\"parameters\":{\"activity.original\":\"koche\",\"activity\":\"koche\"}},\"actions_capability_web_browser\":{\"name\":\"projects/actions-bco/agent/sessions/1533219692859/contexts/actions_capability_web_browser\",\"parameters\":{\"activity.original\":\"koche\",\"activity\":\"koche\"}},\"actions_capability_media_response_audio\":{\"name\":\"projects/actions-bco/agent/sessions/1533219692859/contexts/actions_capability_media_response_audio\",\"parameters\":{\"activity.original\":\"koche\",\"activity\":\"koche\"}}},\"output\":{}},\"incoming\":{\"parsed\":[\"\"]},\"query\":\"ich koche\",\"data\":{}}").getAsJsonObject();
        System.out.println(gson.toJson(a));
//        try {
//            Registries.waitForData();
//            SessionManager.getInstance().login(Registries.getUnitRegistry().getUserUnitIdByUserName("admin"), "admin");
//            CachedCloudConnectorRemote.getRemote().waitForActivation();
//            CachedCloudConnectorRemote.getRemote().connect(true).get();
//        } catch (Exception ex) {
//            ExceptionPrinter.printHistory(ex, LOGGER);
//            System.exit(1);
//        }

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
