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
import org.openbase.bco.dal.remote.unit.Units;
import org.openbase.bco.dal.remote.unit.location.LocationRemote;
import org.openbase.bco.dal.remote.unit.user.UserRemote;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.bco.registry.unit.lib.UnitRegistry;
import org.openbase.jps.core.JPService;
import org.openbase.jps.exception.JPNotAvailableException;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InitializationException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.extension.rst.processing.LabelProcessor;
import org.openbase.jul.iface.Launchable;
import org.openbase.jul.iface.VoidInitializable;
import org.openbase.jul.pattern.Observable;
import org.openbase.jul.pattern.Observer;
import org.openbase.jul.schedule.GlobalCachedExecutorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rst.configuration.LabelType.Label;
import rst.configuration.LabelType.Label.MapFieldEntry;
import rst.domotic.activity.ActivityConfigType.ActivityConfig;
import rst.domotic.registry.UnitRegistryDataType.UnitRegistryData;
import rst.domotic.service.ServiceStateDescriptionType.ServiceStateDescription;
import rst.domotic.state.ActivityMultiStateType.ActivityMultiState;
import rst.domotic.state.LocalPositionStateType.LocalPositionState;
import rst.domotic.state.UserTransitStateType.UserTransitState;
import rst.domotic.unit.UnitConfigType.UnitConfig;
import rst.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;
import rst.domotic.unit.scene.SceneConfigType.SceneConfig;

import java.util.*;
import java.util.concurrent.*;

/**
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class SocketWrapper implements Launchable<Void>, VoidInitializable {

    private static final String LOGIN_EVENT = "login";
    private static final String REGISTER_EVENT = "register";
    private static final String REMOVE_EVENT = "remove";
    private static final String REQUEST_SYNC_EVENT = "requestSync";

    private static final String INTENT_REGISTER_SCENE = "register_scene";
    private static final String INTENT_RENAMING = "rename";
    private static final String INTENT_RELOCATE = "relocate";
    private static final String INTENT_USER_ACTIVITY = "user_activity";
    private static final String INTENT_USER_ACTIVITY_CANCELLATION = "user_activity_cancellation";
    private static final String INTENT_USER_TRANSIT = "user_transit";

    private static final String ID_KEY = "id";
    private static final String TOKEN_KEY = "accessToken";
    private static final String SUCCESS_KEY = "success";
    private static final String ERROR_KEY = "error";

    private static final Logger LOGGER = LoggerFactory.getLogger(SocketWrapper.class);

    private final String userId;
    private final CloudConnectorTokenStore tokenStore;

    private JsonObject loginData;
    private Socket socket;
    private boolean active, loggedIn;
    private String agentUserId;

    private final UnitRegistryObserver unitRegistryObserver;

    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final JsonParser jsonParser = new JsonParser();
    private CompletableFuture<Void> loginFuture;

    public SocketWrapper(final String userId, final CloudConnectorTokenStore tokenStore) {
        this(userId, tokenStore, null);
    }

    public SocketWrapper(final String userId, final CloudConnectorTokenStore tokenStore, final JsonObject loginData) {
        this.userId = userId;
        this.tokenStore = tokenStore;
        this.loginData = loginData;
        this.unitRegistryObserver = new UnitRegistryObserver();
        this.active = false;
    }

    @Override
    public void init() throws InitializationException {
        try {
            // validate that the token store contains an authorization token for the given user
            tokenStore.getBCOToken(userId);

            // validate that either login data is set or the token store contains a token for the cloud
            if (loginData == null && !tokenStore.hasCloudToken(userId)) {
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
                        headers.put(ID_KEY, Collections.singletonList(agentUserId));
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
            }).on(INTENT_USER_TRANSIT, objects -> {
                LOGGER.info("Socket of user[" + userId + "] received transit state request");
                handleUserTransitUpdate(objects[0], (Ack) objects[objects.length - 1]);
            }).on(INTENT_USER_ACTIVITY, objects -> {
                LOGGER.info("Socket of user[" + userId + "] received activity request");
                handleActivity(objects[0], (Ack) objects[objects.length - 1]);
            }).on(INTENT_REGISTER_SCENE, objects -> {
                LOGGER.info("Socket of user[" + userId + "] received register scene request");
                handleSceneRegistration(objects[0], (Ack) objects[objects.length - 1]);
            }).on(INTENT_USER_ACTIVITY_CANCELLATION, objects -> {
                LOGGER.info("Socket of user[" + userId + "] received activity cancellation request");
                handleActivityCancellation(objects[0], (Ack) objects[objects.length - 1]);
            }).on(INTENT_RELOCATE, objects -> {
                LOGGER.info("Socket of user[" + userId + "] received relocate request");
                handleRelocating(objects[0], (Ack) objects[objects.length - 1]);
            }).on(INTENT_RENAMING, objects -> {
                LOGGER.info("Socket of user[" + userId + "] received rename request");
                handleRenaming(objects[0], (Ack) objects[objects.length - 1]);
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

            // handle request and create response
            LOGGER.info("Call handler with authentication token[" + tokenStore.getCloudConnectorToken() + "]");
            final JsonObject jsonObject = FulfillmentHandler.handleRequest(parse.getAsJsonObject(), agentUserId, tokenStore.getCloudConnectorToken(), tokenStore.getBCOToken(userId));
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
                    tokenStore.addCloudToken(userId, response.get(TOKEN_KEY).getAsString());
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
                loginInfo.addProperty(TOKEN_KEY, tokenStore.getCloudToken(userId));
            } catch (NotAvailableException ex) {
                ExceptionPrinter.printHistory("Could not login user[" + userId + "] at BCO Cloud", ex, LOGGER);
                return;
            }

            LOGGER.info("Send loginInfo [" + gson.toJson(loginInfo) + "]");
            socket.emit(LOGIN_EVENT, gson.toJson(loginInfo), (Ack) objects -> {
                try {
                    final JsonObject response = jsonParser.parse(objects[0].toString()).getAsJsonObject();
                    if (response.get(SUCCESS_KEY).getAsBoolean()) {
                        loggedIn = true;
                        LOGGER.info("Logged in [" + userId + "] successfully");
                        loginFuture.complete(null);
                        // trigger initial database sync
                        requestSync();
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

    public Future<Void> remove() {
        final CompletableFuture<Void> future = new CompletableFuture<>();
        socket.emit(REMOVE_EVENT, (Ack) objects -> {
            try {
                final JsonObject response = jsonParser.parse(objects[0].toString()).getAsJsonObject();
                if (response.get(SUCCESS_KEY).getAsBoolean()) {
                    future.complete(null);
                } else {
                    future.completeExceptionally(new CouldNotPerformException("Could not remove user account: " + response.get(ERROR_KEY)));
                }
            } catch (ArrayIndexOutOfBoundsException | ClassCastException ex) {
                final CouldNotPerformException couldNotPerformException = new CouldNotPerformException("Unexpected response for remove request", ex);
                ExceptionPrinter.printHistory(couldNotPerformException, LOGGER);
                future.completeExceptionally(couldNotPerformException);
            }
        });
        return future;
    }

    private static final String CURRENT_LABEL_KEY = "labelCurrent";
    private static final String NEW_LABEL_KEY = "labelNew";
    private static final String CURRENT_LOCATION_KEY = "locationCurrent";
    private static final String NEW_LOCATION_KEY = "locationNew";

    private void handleRelocating(final Object object, final Ack ack) {
        final JsonObject data = jsonParser.parse(object.toString()).getAsJsonObject();
        LOGGER.info("Received relocation data:\n" + gson.toJson(data));

        String response = "";
        try {
            UnitConfig.Builder currentUnit;
            final String currentLabel = data.get(CURRENT_LABEL_KEY).getAsString();
            if (data.has(CURRENT_LOCATION_KEY)) {
                final String currentLocationLabel = data.get(CURRENT_LABEL_KEY).getAsString();
                try {
                    currentUnit = getUnitByLabelAndLocation(currentLabel, currentLocationLabel).toBuilder();
                } catch (NotAvailableException ex) {
                    ack.call("Ich kann die Unit " + currentLabel + " in der Location " + currentLocationLabel + " nicht finden.");
                    return;
                }
            } else {
                try {
                    currentUnit = getUnitByLabelE(currentLabel).toBuilder();
                } catch (NotAvailableException ex) {
                    ack.call("Ich kann die Unit " + currentLabel + " nicht finden.");
                    return;
                }
            }
            final String newLocationLabel = data.get(NEW_LOCATION_KEY).getAsString().replace(" ", "");
            final List<UnitConfig> locations = Registries.getUnitRegistry().getUnitConfigsByLabelAndUnitType(newLocationLabel, UnitType.LOCATION);
            if (locations.isEmpty()) {
                ack.call("Ich kann die Location " + newLocationLabel + " nicht finden.");
                return;
            }

            response = currentLabel + " wurde in die Location " + newLocationLabel + " verschoben.";
            currentUnit.getPlacementConfigBuilder().setLocationId(locations.get(0).getId());
            Registries.getUnitRegistry().updateUnitConfig(currentUnit.build()).get(3, TimeUnit.SECONDS);
            ack.call(response);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            ack.call("Entschuldige. Es ist ein Fehler aufgetreten.");
            ExceptionPrinter.printHistory(ex, LOGGER);
        } catch (ExecutionException | CouldNotPerformException ex) {
            ack.call("Entschuldige. Es ist ein Fehler aufgetreten.");
            ExceptionPrinter.printHistory(ex, LOGGER);
        } catch (TimeoutException ex) {
            ack.call(response.replace("wurde", "wird"));
        }
    }

    private UnitConfig getUnitByLabelE(final String label) throws CouldNotPerformException {
        return fromList(Registries.getUnitRegistry().getUnitConfigsByLabel(label));
    }

    private UnitConfig getUnitByLabelAndLocation(final String label, final String locationLabel) throws CouldNotPerformException {
        final List<UnitConfig> locationList = Registries.getUnitRegistry().getUnitConfigsByLabelAndUnitType(locationLabel, UnitType.LOCATION);
        if (locationList.isEmpty()) {
            return getUnitByLabelE(label);
        }
        return fromList(Registries.getUnitRegistry().getUnitConfigsByLabelAndLocation(label, locationList.get(0).getId()));
    }

    private UnitConfig fromList(final List<UnitConfig> unitConfigList) throws CouldNotPerformException {
        if (unitConfigList.isEmpty()) {
            throw new NotAvailableException("unit");
        }

        final UnitConfig unitConfig = unitConfigList.get(0);
        if (unitConfig.getUnitHostId().isEmpty() || !unitConfig.getBoundToUnitHost()) {
            return unitConfig;
        }

        final UnitConfig hostUnit = Registries.getUnitRegistry().getUnitConfigById(unitConfig.getUnitHostId());
        if (hostUnit.getUnitType() == UnitType.DEVICE && hostUnit.getLabel().equals(unitConfig.getLabel())) {
            return hostUnit;
        } else {
            return unitConfig;
        }
    }


    private void handleRenaming(final Object object, final Ack ack) {
        final JsonObject data = jsonParser.parse(object.toString()).getAsJsonObject();
        LOGGER.info("Received renaming data:\n" + gson.toJson(data));

        String response = "";
        try {
            UnitConfig.Builder currentUnit;
            final String currentLabel = data.get(CURRENT_LABEL_KEY).getAsString();
            if (data.has(CURRENT_LOCATION_KEY)) {
                final String currentLocationLabel = data.get(CURRENT_LABEL_KEY).getAsString();
                try {
                    currentUnit = getUnitByLabelAndLocation(currentLabel, currentLocationLabel).toBuilder();
                } catch (NotAvailableException ex) {
                    ack.call("Ich kann die Unit " + currentLabel + " in der Location " + currentLocationLabel + " nicht finden.");
                    return;
                }
            } else {
                try {
                    currentUnit = getUnitByLabelE(currentLabel).toBuilder();
                } catch (NotAvailableException ex) {
                    ack.call("Ich kann die Unit " + currentLabel + " nicht finden.");
                    return;
                }
            }

            final String newLabel = data.get(NEW_LABEL_KEY).getAsString().replace(" ", "");
            LabelProcessor.replace(currentUnit.getLabelBuilder(), currentLabel, newLabel);

            response = currentLabel + " wurde zu " + newLabel + " umbenannt.";
            Registries.getUnitRegistry().updateUnitConfig(currentUnit.build()).get(3, TimeUnit.SECONDS);
            ack.call(response);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            ack.call("Entschuldige. Es ist ein Fehler aufgetreten.");
            ExceptionPrinter.printHistory(ex, LOGGER);
        } catch (ExecutionException | CouldNotPerformException ex) {
            ack.call("Entschuldige. Es ist ein Fehler aufgetreten.");
            ExceptionPrinter.printHistory(ex, LOGGER);
        } catch (TimeoutException ex) {
            ack.call(response.replace("wurde", "wird"));
        }

    }

    private void handleUserTransitUpdate(final Object object, final Ack acknowledgement) {
        final String transit = (String) object;
        LOGGER.info("Received user transit " + transit);
        try {
            final UserTransitState.State state = Enum.valueOf(UserTransitState.State.class, transit);
            final UserRemote userRemote = Units.getUnit(userId, false, UserRemote.class);
            userRemote.setUserTransitState(UserTransitState.newBuilder().setValue(state).build()).get(3, TimeUnit.SECONDS);
            acknowledgement.call("Alles klar");
        } catch (InterruptedException ex) {
            acknowledgement.call("Entschuldige. Es ist ein Fehler aufgetreten.");
            ExceptionPrinter.printHistory(ex, LOGGER);
            Thread.currentThread().interrupt();
            // this should not happen since wait for data is not called
        } catch (CouldNotPerformException | ExecutionException | TimeoutException | IllegalArgumentException ex) {
            acknowledgement.call("Entschuldige. Es ist ein Fehler aufgetreten.");
            ExceptionPrinter.printHistory(ex, LOGGER);
        }
    }

    private void handleActivity(final Object object, final Ack acknowledgement) {
        final JsonObject params = jsonParser.parse(object.toString()).getAsJsonObject();
        LOGGER.info("Set activities: " + gson.toJson(params));
        try {
            String errorResponse = "";
            final UserRemote userRemote = Units.getUnit(userId, false, UserRemote.class);
            if (params.has("location")) {
                final String locationLabel = params.get("location").getAsString();
                LOGGER.info("Found location " + locationLabel);
                UnitConfig location = null;

                for (UnitConfig unitConfig : Registries.getUnitRegistry().getUnitConfigs(UnitType.LOCATION)) {
                    for (MapFieldEntry entry : unitConfig.getLabel().getEntryList()) {
                        for (String label : entry.getValueList()) {
                            if (locationLabel.equalsIgnoreCase(label)) {
                                location = unitConfig;
                            }
                        }
                    }
                }

                if (location == null) {
                    errorResponse += "Die Location " + locationLabel + " ist nicht verfügbar.";
                } else {
                    userRemote.setLocalPositionState(LocalPositionState.newBuilder().addLocationId(location.getId()).build()).get(3, TimeUnit.SECONDS);
                }
            }

            if (!userRemote.isDataAvailable()) {
                userRemote.waitForData(3, TimeUnit.SECONDS);
            }

            final JsonArray activities = params.get("activity").getAsJsonArray();
            final ActivityMultiState.Builder builder = userRemote.getActivityMultiState().toBuilder().clearActivityId();

            if (activities.size() == 0) {
                throw new NotAvailableException("activities");
            } else {
                final List<ActivityConfig> activityList = new ArrayList<>();
                final List<String> unavailableActivities = new ArrayList<>();
                for (final JsonElement activityElem : activities) {
                    boolean found = false;
                    final String activityRepresentation = activityElem.getAsString();
                    configLoop:
                    for (final ActivityConfig activityConfig : Registries.getActivityRegistry().getActivityConfigs()) {
                        for (Label.MapFieldEntry entry : activityConfig.getLabel().getEntryList()) {
                            for (String label : entry.getValueList()) {
                                if (label.toLowerCase().contains(activityRepresentation)) {
                                    activityList.add(activityConfig);
                                    builder.addActivityId(activityConfig.getId());
                                    found = true;
                                    break configLoop;
                                }
                            }
                        }
                    }

                    if (!found) {
                        unavailableActivities.add(activityRepresentation);
                    }
                }
//                    response += "Deine ";
//                    if (activityList.size() == 1) {
//                        response += "Aktivität ist jetzt " + LabelProcessor.getBestMatch(activityList.get(0).getLabel()) + ".";
//                    } else {
//                        response += "Aktivitäten sind jetzt ";
//                        for (int i = 0; i < activityList.size(); i++) {
//                            final String label = LabelProcessor.getBestMatch(activityList.get(i).getLabel());
//                            if (i == activityList.size() - 1) {
//                                response += "und " + label;
//                            } else if (i == activityList.size() - 2) {
//                                response += label + " ";
//                            } else {
//                                response += label + ", ";
//                            }
//                        }
//                        response += ".";
//                    }
//                }

                final String unavailable = buildUnavailableActivityResponse(errorResponse, unavailableActivities);
                if (!unavailable.isEmpty()) {
                    if (!errorResponse.isEmpty()) {
                        errorResponse += " Außerdem kann ich die ";
                    } else {
                        errorResponse += "Ich kann die ";
                    }
                    errorResponse += unavailable;
                }
            }

            if (builder.getActivityIdCount() > 0) {
                userRemote.setActivityMultiState(builder.build()).get(3, TimeUnit.SECONDS);
            }

            if (!errorResponse.isEmpty()) {
                acknowledgement.call(errorResponse);
            } else {
                acknowledgement.call("Okay");
            }
        } catch (CouldNotPerformException | InterruptedException | ExecutionException ex) {
            acknowledgement.call("Entschuldige. Es ist ein Fehler aufgetreten.");
            ExceptionPrinter.printHistory(ex, LOGGER);
            if (ex instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
        } catch (TimeoutException ex) {
            acknowledgement.call("Deine Anfrage wird bearbeitet.");
        }
    }

    private void handleActivityCancellation(final Object object, final Ack acknowledgement) {
        final JsonObject params = jsonParser.parse(object.toString()).getAsJsonObject();
        LOGGER.info("Cancel activities: " + gson.toJson(params));
        try {
            final JsonArray activities = params.get("activity").getAsJsonArray();

            final UserRemote userRemote = Units.getUnit(userId, false, UserRemote.class);
            userRemote.waitForData(3, TimeUnit.SECONDS);
            final ActivityMultiState.Builder builder = userRemote.getActivityMultiState().toBuilder();
            int initialCount = builder.getActivityIdCount();

            String errorResponse = "";
            if (activities.size() == 0) {
                builder.clearActivityId();
            } else {
                final List<ActivityConfig> activityList = new ArrayList<>();
                final List<String> unavailableActivities = new ArrayList<>();
                for (final JsonElement activityElem : activities) {
                    boolean found = false;
                    final String activityRepresentation = activityElem.getAsString();
                    configLoop:
                    for (final ActivityConfig activityConfig : Registries.getActivityRegistry().getActivityConfigs()) {
                        for (Label.MapFieldEntry entry : activityConfig.getLabel().getEntryList()) {
                            for (String label : entry.getValueList()) {
                                if (label.toLowerCase().contains(activityRepresentation)) {
                                    activityList.add(activityConfig);
                                    found = true;
                                    break configLoop;
                                }
                            }
                        }
                    }

                    if (!found) {
                        unavailableActivities.add(activityRepresentation);
                    }
                }

//                if (activityList.size() > 0) {
//                    response += "Deine ";
//                    if (activityList.size() == 1) {
//                        response += "Aktivität " + LabelProcessor.getBestMatch(activityList.get(0).getLabel()) + " wurde beendet.";
//                    } else {
//                        response += "Aktivitäten ";
//                        for (int i = 0; i < activityList.size(); i++) {
//                            final String label = LabelProcessor.getBestMatch(activityList.get(i).getLabel());
//                            if (i == activityList.size() - 1) {
//                                response += "und " + label;
//                            } else if (i == activityList.size() - 2) {
//                                response += label + " ";
//                            } else {
//                                response += label + ", ";
//                            }
//                        }
//                        response += " wurden beendet.";
//                    }
//                }
                final ArrayList<String> idList = new ArrayList<>(builder.getActivityIdList());
                builder.clearActivityId();
                outer:
                for (String id : idList) {
                    for (ActivityConfig activityConfig : activityList) {
                        if (activityConfig.getId().equals(id)) {
                            continue outer;
                        }
                    }
                    builder.addActivityId(id);
                }


                final String unavailable = buildUnavailableActivityResponse(errorResponse, unavailableActivities);
                if (!unavailable.isEmpty()) {
                    errorResponse += "Ich kann die " + unavailable;
                }
            }

            if (builder.getActivityIdCount() != initialCount) {
                userRemote.setActivityMultiState(builder.build()).get(3, TimeUnit.SECONDS);
            }

            if (!errorResponse.isEmpty()) {
                acknowledgement.call(errorResponse);
            } else {
                acknowledgement.call("Okay");
            }
        } catch (CouldNotPerformException | InterruptedException | ExecutionException ex) {
            acknowledgement.call("Entschuldige. Es ist ein Fehler aufgetreten.");
            ExceptionPrinter.printHistory(ex, LOGGER);
            if (ex instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
        } catch (TimeoutException ex) {
            acknowledgement.call("Deine Anfrage wird bearbeitet.");
        }
    }

    private String buildUnavailableActivityResponse(String response, final List<String> unavailableActivities) {
        String res = "";
        if (unavailableActivities.size() > 0) {
            if (unavailableActivities.size() == 1) {
                res += "Aktivität " + unavailableActivities.get(0) + " nicht finden";
            } else {
                res += "Aktivitäten ";
                for (int i = 0; i < unavailableActivities.size(); i++) {
                    if (i == unavailableActivities.size() - 1) {
                        res += "und " + unavailableActivities.get(i);
                    } else if (i == unavailableActivities.size() - 2) {
                        res += unavailableActivities.get(i) + " ";
                    } else {
                        res += unavailableActivities + ", ";
                    }
                }
                res += " nicht finden.";
            }
        }
        return res;
    }

    private void handleSceneRegistration(final Object object, final Ack acknowledgement) {
        //TODO: if taking to long give feedback that process is still running?
//        final long MAX_WAIING_TIME = 5000;
//        long startingTime = System.currentTimeMillis();

        final JsonObject data = jsonParser.parse(object.toString()).getAsJsonObject();
        LOGGER.info("Received scene registration data:\n" + gson.toJson(data));
        final UnitConfig.Builder sceneUnitConfig = UnitConfig.newBuilder().setUnitType(UnitType.SCENE);

        try {
            UnitConfig location = Registries.getUnitRegistry().getRootLocationConfig();
            if (data.has("location")) {
                final String locationLabel = data.get("location").getAsString();
                List<UnitConfig> locations = Registries.getUnitRegistry().getUnitConfigsByLabelAndUnitType(locationLabel, UnitType.LOCATION);
                if (locations.size() == 0) {
                    acknowledgement.call("Ich kann die location " + locationLabel + " nicht finden");
                } else {
                    location = locations.get(0);
                }
            }

            if (data.has("label")) {
                final String label = data.get("label").getAsString();
                final Label.MapFieldEntry.Builder entry = sceneUnitConfig.getLabelBuilder().addEntryBuilder();
                entry.setKey(Locale.GERMAN.getLanguage());
                entry.addValue(label);

                // make sure label is available for this location
                for (UnitConfig unitConfig : Registries.getUnitRegistry().getUnitConfigsByLabelAndLocation(label, location.getId())) {
                    if (unitConfig.getUnitType() == UnitType.SCENE) {
                        acknowledgement.call("");
                    }
                }
            }

            final SceneConfig.Builder sceneConfig = sceneUnitConfig.getSceneConfigBuilder();
            final LocationRemote locationRemote = Units.getUnit(location, false, LocationRemote.class);
            locationRemote.waitForData(5, TimeUnit.SECONDS);
            try {
                for (final ServiceStateDescription serviceStateDescription : locationRemote.recordSnapshot().get(5, TimeUnit.SECONDS).getServiceStateDescriptionList()) {
                    sceneConfig.addRequiredServiceStateDescription(serviceStateDescription);
                }
            } catch (ExecutionException | TimeoutException ex) {
                acknowledgement.call("Entschuldige. Es ist ein Fehler aufgetreten");
                ExceptionPrinter.printHistory(ex, LOGGER);
                return;
            }

            try {
                UnitConfig unitConfig = Registries.getUnitRegistry().registerUnitConfig(sceneUnitConfig.build()).get(1, TimeUnit.SECONDS);
                String label;
                try {
                    label = LabelProcessor.getLabelByLanguage(Locale.GERMAN, unitConfig.getLabel());
                } catch (NotAvailableException ex) {
                    label = LabelProcessor.getBestMatch(unitConfig.getLabel());
                }
                acknowledgement.call("Die Szene " + label + " wurde erfolgreich registriert.");
            } catch (ExecutionException ex) {
                acknowledgement.call("Entschuldige. Es ist ein Fehler aufgetreten");
                ExceptionPrinter.printHistory(ex, LOGGER);
            } catch (TimeoutException e) {
                acknowledgement.call("Die Szene wird gerade registriert");
            }
        } catch (InterruptedException ex) {
            acknowledgement.call("Entschuldige. Es ist ein Fehler aufgetreten");
            Thread.currentThread().interrupt();
            ExceptionPrinter.printHistory(ex, LOGGER);
        } catch (CouldNotPerformException ex) {
            acknowledgement.call("Entschuldige. Es ist ein Fehler aufgetreten");
            ExceptionPrinter.printHistory(ex, LOGGER);
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

            // trigger sync if socket is connected and logged in
            // if the user is not logged the login process will trigger an update anyway
            if (isLoggedIn()) {
                requestSync();

                // debug print
//                JsonObject test = new JsonObject();
//                test.addProperty(FulfillmentHandler.REQUEST_ID_KEY, "12345678");
//                test.add(FulfillmentHandler.PAYLOAD_KEY, syncResponse);
//                LOGGER.debug("new sync[" + isLoggedIn() + "]:\n" + gson.toJson(test));
            }
        }
    }

    private void requestSync() {
        socket.emit(REQUEST_SYNC_EVENT, (Ack) objects -> {
            final JsonObject response = jsonParser.parse(objects[0].toString()).getAsJsonObject();
            if (response.has(SUCCESS_KEY)) {
                final boolean success = response.get(SUCCESS_KEY).getAsBoolean();
                if (success) {
                    LOGGER.info("Successfully performed sync request for user[" + userId + "]");
                } else {
                    LOGGER.warn("Could not perform sync for user[" + userId + "]: " + response.get(ERROR_KEY));
                }
            }
        });
    }

    public Future<Void> getLoginFuture() {
        return loginFuture;
    }
}
