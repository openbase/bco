package org.openbase.bco.app.cloudconnector;

/*-
 * #%L
 * BCO Cloud Connector
 * %%
 * Copyright (C) 2018 - 2019 openbase.org
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
import org.openbase.bco.app.cloudconnector.jp.JPCloudServerURI;
import org.openbase.bco.app.cloudconnector.mapping.lib.ErrorCode;
import org.openbase.bco.authentication.lib.SessionManager;
import org.openbase.bco.authentication.lib.future.AuthenticatedValueFuture;
import org.openbase.bco.dal.lib.action.ActionDescriptionProcessor;
import org.openbase.bco.dal.remote.layer.unit.Units;
import org.openbase.bco.dal.remote.layer.unit.location.LocationRemote;
import org.openbase.bco.dal.remote.layer.unit.user.UserRemote;
import org.openbase.bco.registry.lib.util.UnitConfigProcessor;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.bco.registry.unit.lib.UnitRegistry;
import org.openbase.jps.core.JPService;
import org.openbase.jps.exception.JPNotAvailableException;
import org.openbase.jul.exception.*;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.extension.type.processing.LabelProcessor;
import org.openbase.jul.iface.Launchable;
import org.openbase.jul.iface.VoidInitializable;
import org.openbase.jul.pattern.Observer;
import org.openbase.jul.pattern.provider.DataProvider;
import org.openbase.jul.schedule.GlobalCachedExecutorService;
import org.openbase.type.domotic.action.ActionDescriptionType.ActionDescription;
import org.openbase.type.domotic.activity.ActivityConfigType.ActivityConfig;
import org.openbase.type.domotic.authentication.AuthTokenType.AuthToken;
import org.openbase.type.domotic.authentication.AuthenticatedValueType.AuthenticatedValue;
import org.openbase.type.domotic.registry.UnitRegistryDataType.UnitRegistryData;
import org.openbase.type.domotic.service.ServiceStateDescriptionType.ServiceStateDescription;
import org.openbase.type.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import org.openbase.type.domotic.state.ActivityMultiStateType.ActivityMultiState;
import org.openbase.type.domotic.state.EnablingStateType.EnablingState.State;
import org.openbase.type.domotic.state.LocalPositionStateType.LocalPositionState;
import org.openbase.type.domotic.state.UserTransitStateType.UserTransitState;
import org.openbase.type.domotic.unit.UnitConfigType.UnitConfig;
import org.openbase.type.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;
import org.openbase.type.domotic.unit.scene.SceneConfigType.SceneConfig;
import org.openbase.type.language.LabelType.Label;
import org.openbase.type.language.LabelType.Label.MapFieldEntry;
import org.openbase.type.language.LabelType.LabelOrBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.TimeoutException;
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

    private static final String RESPONSE_GENERIC_ERROR = "Entschuldige, es ist ein Fehler aufgetreten.";
//    private static final String

    private static final Logger LOGGER = LoggerFactory.getLogger(SocketWrapper.class);

    private final String userId;
    private final CloudConnectorTokenStore tokenStore;

    private JsonObject loginData;
    private Socket socket;
    private boolean active, loggedIn;

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


            // create socket
            socket = IO.socket(JPService.getProperty(JPCloudServerURI.class).getValue());
            // add id to header for cloud server
            socket.io().on(Manager.EVENT_TRANSPORT, args -> {
                Transport transport = (Transport) args[0];

                transport.on(Transport.EVENT_REQUEST_HEADERS, args1 -> {
                    try {
                        final String bcoId = Registries.getUnitRegistry().getUnitConfigByAlias(UnitRegistry.BCO_USER_ALIAS).getId();
                        final String agentUserId = userId + "@" + bcoId;
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
                // handle request
                handleRequest(objects[0], (Ack) objects[objects.length - 1]);
            }).on(Socket.EVENT_DISCONNECT, objects -> {
                // reconnection is automatically done by the socket API, just print that disconnected
                LOGGER.info("Socket of user[" + userId + "] disconnected");
            }).on(INTENT_USER_TRANSIT, objects -> {
                handleUserTransitUpdate(objects[0], (Ack) objects[objects.length - 1]);
            }).on(INTENT_USER_ACTIVITY, objects -> {
                handleActivity(objects[0], (Ack) objects[objects.length - 1]);
            }).on(INTENT_REGISTER_SCENE, objects -> {
                handleSceneRegistration(objects[0], (Ack) objects[objects.length - 1]);
            }).on(INTENT_USER_ACTIVITY_CANCELLATION, objects -> {
                handleActivityCancellation(objects[0], (Ack) objects[objects.length - 1]);
            }).on(INTENT_RELOCATE, objects -> {
                handleRelocating(objects[0], (Ack) objects[objects.length - 1]);
            }).on(INTENT_RENAMING, objects -> {
                handleRenaming(objects[0], (Ack) objects[objects.length - 1]);
            }).on(Socket.EVENT_RECONNECT_ATTEMPT, objects -> {
                LOGGER.debug("Attempt to reconnect socket of user {}", userId);
            }).on(Socket.EVENT_RECONNECT_ERROR, objects -> {
                LOGGER.debug("Reconnection error for socket of user {} because {}", userId, objects.length > 0 ? objects[0] : 1);
            }).on(Socket.EVENT_RECONNECT_FAILED, objects -> {
                LOGGER.debug("Reconnection failed for socket of user {} because {}", userId, objects.length > 0 ? objects[0] : 1);
            }).on(Socket.EVENT_RECONNECTING, objects -> {
                LOGGER.info("Reconnection event for user {}", userId);
            }).on(Socket.EVENT_RECONNECT, objects -> {
                LOGGER.info("Socket of user {} reconnected!", userId);
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
            LOGGER.trace("Request: {}", gson.toJson(parse));

            // handle request and create response
            final JsonObject jsonObject = FulfillmentHandler.handleRequest(parse.getAsJsonObject(), userId, tokenStore.getCloudConnectorToken(), tokenStore.getBCOToken(userId));
            final String response = gson.toJson(jsonObject);
            LOGGER.trace("Handler produced response: {}", response);
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

            LOGGER.trace("Send login info [" + gson.toJson(loginInfo) + "]");
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
                        LOGGER.warn("Could not login user[" + userId + "] at BCO Cloud: " + response.get(ERROR_KEY));
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
        LOGGER.trace("Received relocation request:\n {}", gson.toJson(data));

        if (!data.has(CURRENT_LABEL_KEY)) {
            respond(ack, "Welches Gerät soll verschoben werden? Sage zum Beispiel: Der Deckenfluter steht im Wohnzimmer.", true);
            return;
        }
        final String currentLabel = data.get(CURRENT_LABEL_KEY).getAsString().trim();

        if (!data.has(NEW_LOCATION_KEY)) {
            respond(ack, "Wo ist das Gerät " + currentLabel + "? Sage zum Beispiel: " + currentLabel + " ist jetzt im Flur.", true);
            return;
        }
        final String newLocationLabel = data.get(NEW_LOCATION_KEY).getAsString().trim();

        String response;
        try {
            UnitConfig.Builder currentUnit;
            if (data.has(CURRENT_LOCATION_KEY)) {
                final String currentLocationLabel = data.get(CURRENT_LOCATION_KEY).getAsString().trim();
                try {
                    currentUnit = getUnitByLabelAndLocation(currentLabel, currentLocationLabel).toBuilder();
                } catch (NotAvailableException ex) {
                    respond(ack, "Ich kann das Gerät " + currentLabel + " in dem Ort " + currentLocationLabel + " nicht finden.", true);
                    return;
                }
            } else {
                try {
                    currentUnit = getUnitByLabel(currentLabel).toBuilder();
                } catch (NotAvailableException ex) {
                    respond(ack, "Ich kann das Gerät " + currentLabel + " nicht finden.", true);
                    return;
                }
            }
            final List<UnitConfig> locations = Registries.getUnitRegistry().getUnitConfigsByLabelAndUnitType(newLocationLabel, UnitType.LOCATION);
            if (locations.isEmpty()) {
                respond(ack, "Ich kann den Ort " + newLocationLabel + " nicht finden.", true);
                return;
            }

            response = currentLabel + " wurde in den Ort " + newLocationLabel + " verschoben.";
            currentUnit.getPlacementConfigBuilder().setLocationId(locations.get(0).getId());
            currentUnit.getEnablingStateBuilder().setValue(State.DISABLED);
            try {
                //TODO: validate that this is really necessary
                final int currentRequestNumber = requestNumber;
                // Note: this is a hack because google will not update locations, this the unit is disabled when moved to a new location and then again enabled
                // this triggers two synchronizations with google where the unit is removed and then added again which causes the location to update
                AuthenticatedValue authenticatedValue = SessionManager.getInstance().initializeRequest(currentUnit.build(), AuthToken.newBuilder().setAuthenticationToken(tokenStore.getCloudConnectorToken()).setAuthorizationToken(tokenStore.getBCOToken(userId)).build());
                LOGGER.debug("Update unit location and set disabled");
                Future<UnitConfig> updateFuture = new AuthenticatedValueFuture<>(Registries.getUnitRegistry().updateUnitConfigAuthenticated(authenticatedValue), UnitConfig.class, authenticatedValue.getTicketAuthenticatorWrapper(), SessionManager.getInstance());
                try {
                    updateFuture.get(3, TimeUnit.SECONDS).toBuilder();
                } catch (TimeoutException ex) {
                    respond(ack, response.replace("wurde", "wird"));
                } finally {
                    enableAgain(updateFuture, currentRequestNumber);
                }
            } catch (ExecutionException ex) {
                if (ExceptionProcessor.getInitialCause(ex) instanceof PermissionDeniedException) {
                    respond(ack, "Du besitzt nicht die benötigten Rechte um das Gerät " + currentLabel + " in den Ort " + newLocationLabel + " zu verschieben.");
                    return;
                }
                respond(ack, RESPONSE_GENERIC_ERROR, true);
                return;
            }
            respond(ack, response);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            respond(ack, RESPONSE_GENERIC_ERROR, true);
            ExceptionPrinter.printHistory(ex, LOGGER);
        } catch (CouldNotPerformException ex) {
            respond(ack, RESPONSE_GENERIC_ERROR, true);
            ExceptionPrinter.printHistory(ex, LOGGER);
        }
    }

    private void enableAgain(final Future<UnitConfig> unitConfigFuture, final int currentRequestNumber) {
        LOGGER.trace("Trigger enable task");
        GlobalCachedExecutorService.submit(() -> {
            try {
                final UnitConfig.Builder currentUnit = unitConfigFuture.get().toBuilder();
                LOGGER.debug("Disabled and relocated unit. Now enable again");
                currentUnit.getEnablingStateBuilder().setValue(State.ENABLED);
                final AuthenticatedValue authenticatedValue = SessionManager.getInstance().initializeRequest(currentUnit.build(), AuthToken.newBuilder().setAuthenticationToken(tokenStore.getCloudConnectorToken()).setAuthorizationToken(tokenStore.getBCOToken(userId)).build());
                Registries.getUnitRegistry().updateUnitConfigAuthenticated(authenticatedValue).get();
                LOGGER.debug("RequestCounters {}, {}", requestNumber, currentRequestNumber);
                if (requestNumber < currentRequestNumber + 2) {
                    LOGGER.info("Trigger request sync after enabling because only {} changes occurred", requestNumber - currentRequestNumber);
                    requestSync();
                }
            } catch (Exception ex) {
                ExceptionPrinter.printHistory("Could not enable unit again", ex, LOGGER);
            }
            return null;
        });
    }

    private UnitConfig getUnitByLabel(final String label) throws CouldNotPerformException {
        return fromList(Registries.getUnitRegistry().getUnitConfigsByLabel(label));
    }

    private UnitConfig getUnitByLabelAndLocation(final String label, final String locationLabel) throws CouldNotPerformException {
        final List<UnitConfig> locationList = Registries.getUnitRegistry().getUnitConfigsByLabelAndUnitType(locationLabel, UnitType.LOCATION);
        if (locationList.isEmpty()) {
            return getUnitByLabel(label);
        }
        return fromList(Registries.getUnitRegistry().getUnitConfigsByLocationIdAndUnitLabel(locationList.get(0).getId(), label));
    }


    private UnitConfig fromList(final List<UnitConfig> unitConfigList) throws CouldNotPerformException {
        if (unitConfigList.isEmpty()) {
            throw new NotAvailableException("unit");
        }

        final UnitConfig unitConfig = unitConfigList.get(0);
        if (!UnitConfigProcessor.isHostUnitAvailable(unitConfig) || !unitConfig.getBoundToUnitHost()) {
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
        LOGGER.trace("Received renaming request:\n {}", gson.toJson(data));

        if (!data.has(CURRENT_LABEL_KEY)) {
            respond(ack, "Welches Gerät soll ich umbenennen? Sage zum Beispiel die Deckenlampe soll jetzt Deckenlicht heißen.", true);
            return;
        }
        final String currentLabel = data.get(CURRENT_LABEL_KEY).getAsString().trim();

        if (!data.has(NEW_LABEL_KEY)) {
            String newName;
            if (currentLabel.equalsIgnoreCase("Deckenlicht")) {
                newName = "Deckenlicht";
            } else {
                newName = "Deckenlampe";
            }
            respond(ack, "Welchen neuen Namen soll das Gerät " + currentLabel + " bekommen. Sage zum Beispiel nenne " + currentLabel + " in " + newName + " um", true);
            return;
        }
        final String newLabel = data.get(NEW_LABEL_KEY).getAsString().trim();


        String response = "";
        try {
            UnitConfig.Builder currentUnit;
            if (data.has(CURRENT_LOCATION_KEY)) {
                final String currentLocationLabel = data.get(CURRENT_LOCATION_KEY).getAsString();
                try {
                    currentUnit = getUnitByLabelAndLocation(currentLabel, currentLocationLabel).toBuilder();
                } catch (NotAvailableException ex) {
                    respond(ack, "Ich kann das Gerät " + currentLabel + " in dem Ort " + currentLocationLabel + " nicht finden", true);
                    return;
                }
            } else {
                try {
                    currentUnit = getUnitByLabel(currentLabel).toBuilder();
                } catch (NotAvailableException ex) {
                    respond(ack, "Ich kann das Gerät " + currentLabel + " nicht finden.", true);
                    return;
                }
            }

            LabelProcessor.replace(currentUnit.getLabelBuilder(), currentLabel, newLabel);
            response = currentLabel + " wurde zu " + newLabel + " umbenannt.";
            try {
                final AuthenticatedValue authenticatedValue = SessionManager.getInstance().initializeRequest(currentUnit.build(), AuthToken.newBuilder().setAuthenticationToken(tokenStore.getCloudConnectorToken()).setAuthorizationToken(tokenStore.getBCOToken(userId)).build());
                Registries.getUnitRegistry().updateUnitConfigAuthenticated(authenticatedValue).get(3, TimeUnit.SECONDS);
            } catch (ExecutionException ex) {
                if (ExceptionProcessor.getInitialCause(ex) instanceof PermissionDeniedException) {
                    respond(ack, "Du besitzt nicht die benötigten Rechte um das Gerät " + currentLabel + " in " + newLabel + " umzubenennen.");
                    return;
                }
                respond(ack, RESPONSE_GENERIC_ERROR, true);
                return;
            }
            respond(ack, response);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            respond(ack, RESPONSE_GENERIC_ERROR, true);
            ExceptionPrinter.printHistory(ex, LOGGER);
        } catch (CouldNotPerformException ex) {
            respond(ack, RESPONSE_GENERIC_ERROR, true);
            ExceptionPrinter.printHistory(ex, LOGGER);
        } catch (TimeoutException ex) {
            respond(ack, response.replace("wurde", "wird"));
        }

    }

    private void handleUserTransitUpdate(final Object object, final Ack acknowledgement) {
        final JsonObject data = jsonParser.parse(object.toString()).getAsJsonObject();
        LOGGER.trace("User [{}] received user transit request: {}", userId, gson.toJson(data));
        try {
            if (!data.has("userTransit")) {
                throw new NotAvailableException("UserTransitState");
            }
            final String userTransitState = data.get("userTransit").getAsString();
            final UserTransitState.State state = Enum.valueOf(UserTransitState.State.class, userTransitState);
            final UserTransitState serviceState = UserTransitState.newBuilder().setValue(state).build();
            final UserRemote userRemote = Units.getUnit(userId, false, UserRemote.class);

            final ActionDescription actionDescription = ActionDescriptionProcessor.generateActionDescriptionBuilder(serviceState, ServiceType.USER_TRANSIT_STATE_SERVICE, userRemote).build();
            final AuthenticatedValue authenticatedValue = SessionManager.getInstance().initializeRequest(actionDescription, AuthToken.newBuilder().setAuthenticationToken(tokenStore.getCloudConnectorToken()).setAuthorizationToken(tokenStore.getBCOToken(userId)).build());

            userRemote.applyActionAuthenticated(authenticatedValue).get(3, TimeUnit.SECONDS);
            respond(acknowledgement, "Alles klar");
        } catch (InterruptedException ex) {
            respond(acknowledgement, RESPONSE_GENERIC_ERROR, true);
            ExceptionPrinter.printHistory(ex, LOGGER);
            Thread.currentThread().interrupt();
            // this should not happen since wait for data is not called
        } catch (CouldNotPerformException | TimeoutException | IllegalArgumentException | ExecutionException ex) {
            try {
                if (ExceptionProcessor.getInitialCause(ex) instanceof PermissionDeniedException) {
                    // note: this should not happen since the registry should guarantee that users always have permissions for themselves
                    respond(acknowledgement, "Du hast keine Rechte den Status von Nutzer " + Registries.getUnitRegistry().getUnitConfigById(userId).getUserConfig().getUserName() + " zu beeinflussen.");
                    return;
                }
            } catch (CouldNotPerformException exx) {
                // return error as below
            }
            respond(acknowledgement, RESPONSE_GENERIC_ERROR, true);
            ExceptionPrinter.printHistory(ex, LOGGER);
        }
    }

    private void handleActivity(final Object object, final Ack acknowledgement) {
        final JsonObject params = jsonParser.parse(object.toString()).getAsJsonObject();
        LOGGER.trace("User [{}] received set activities request: {}", userId, gson.toJson(params));
        try {
            LocalPositionState localPositionState = null;
            String errorResponse = "";
            final UserRemote userRemote = Units.getUnit(userId, false, UserRemote.class);
            if (params.has("location")) {
                final String locationLabel = params.get("location").getAsString();
                UnitConfig location = null;

                for (UnitConfig unitConfig : Registries.getUnitRegistry().getUnitConfigsByUnitType(UnitType.LOCATION)) {
                    for (MapFieldEntry entry : unitConfig.getLabel().getEntryList()) {
                        for (String label : entry.getValueList()) {
                            if (locationLabel.equalsIgnoreCase(label)) {
                                location = unitConfig;
                            }
                        }
                    }
                }

                if (location == null) {
                    errorResponse += "Der Ort " + locationLabel + " ist nicht verfügbar.";
                } else {
                    localPositionState = LocalPositionState.newBuilder().addLocationId(location.getId()).build();

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

            if (!errorResponse.isEmpty()) {
                respond(acknowledgement, errorResponse, true);
                return;
            }
            String response = "Okay, deine ";
            if (builder.getActivityIdCount() == 1) {
                response += "Aktivität ist jetzt " + getLabelForUser(Registries.getActivityRegistry().getActivityConfigById(builder.getActivityId(0)).getLabel());
            } else {
                response += "Aktivitäten sind nun ";
                for (int i = 0; i < builder.getActivityIdCount(); i++) {
                    if (i == builder.getActivityIdCount() - 1) {
                        response += " und ";
                    } else {
                        response += ", ";
                    }
                    response += getLabelForUser(Registries.getActivityRegistry().getActivityConfigById(builder.getActivityId(i)).getLabel());
                }
            }

            ActionDescription actionDescription;
            AuthenticatedValue authenticatedValue;

            if (localPositionState != null) {
                actionDescription = ActionDescriptionProcessor.generateActionDescriptionBuilder(localPositionState, ServiceType.LOCAL_POSITION_STATE_SERVICE, userRemote).build();
                authenticatedValue = SessionManager.getInstance().initializeRequest(actionDescription, AuthToken.newBuilder().setAuthenticationToken(tokenStore.getCloudConnectorToken()).setAuthorizationToken(tokenStore.getBCOToken(userId)).build());

                try {
                    userRemote.applyActionAuthenticated(authenticatedValue).get(3, TimeUnit.SECONDS);
                } catch (TimeoutException ex) {
                    final String locationLabel = getLabelForUser(Registries.getUnitRegistry().getUnitConfigById(localPositionState.getLocationId(0)).getLabel());
                    response = "Dein Aufenthaltsort wird auf " + locationLabel + " gesetzt. Danach werden deine Aktivitäten berarbeitet.";
                    respond(acknowledgement, response);
                    return;
                }
            }

            actionDescription = ActionDescriptionProcessor.generateActionDescriptionBuilder(builder.build(), ServiceType.ACTIVITY_MULTI_STATE_SERVICE, userRemote).build();
            authenticatedValue = SessionManager.getInstance().initializeRequest(actionDescription, AuthToken.newBuilder().setAuthenticationToken(tokenStore.getCloudConnectorToken()).setAuthorizationToken(tokenStore.getBCOToken(userId)).build());

            try {
                userRemote.applyActionAuthenticated(authenticatedValue).get(3, TimeUnit.SECONDS);
            } catch (TimeoutException ex) {
                response = response.replace("ist jetzt", "wird auf").replace("sind nun", "werden auf") + " gesetzt.";
                respond(acknowledgement, response);
                return;
            }

            response += ".";
            respond(acknowledgement, response);
        } catch (CouldNotPerformException | InterruptedException | ExecutionException ex) {
            respond(acknowledgement, RESPONSE_GENERIC_ERROR, true);
            ExceptionPrinter.printHistory(ex, LOGGER);
            if (ex instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private String getLabelForUser(final LabelOrBuilder labelOrBuilder) throws CouldNotPerformException {
        final String language = Registries.getUnitRegistry().getUnitConfigById(userId).getUserConfig().getLanguage();
        return LabelProcessor.getBestMatch(new Locale(language), labelOrBuilder);
    }

    private void handleActivityCancellation(final Object object, final Ack acknowledgement) {
        final JsonObject params = jsonParser.parse(object.toString()).getAsJsonObject();
        LOGGER.trace("User [{}] received cancel activities request: {}", userId, gson.toJson(params));
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
                final ActionDescription actionDescription = ActionDescriptionProcessor.generateActionDescriptionBuilder(builder.build(), ServiceType.ACTIVITY_MULTI_STATE_SERVICE, userRemote).build();
                final AuthenticatedValue authenticatedValue = SessionManager.getInstance().initializeRequest(actionDescription, AuthToken.newBuilder().setAuthenticationToken(tokenStore.getCloudConnectorToken()).setAuthorizationToken(tokenStore.getBCOToken(userId)).build());
                userRemote.applyActionAuthenticated(authenticatedValue).get(2, TimeUnit.SECONDS);
            }

            if (!errorResponse.isEmpty()) {
                respond(acknowledgement, errorResponse, true);
            } else {
                respond(acknowledgement, "Okay");
            }
        } catch (CouldNotPerformException | InterruptedException | ExecutionException ex) {
            respond(acknowledgement, RESPONSE_GENERIC_ERROR, true);
            ExceptionPrinter.printHistory(ex, LOGGER);
            if (ex instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
        } catch (TimeoutException ex) {
            respond(acknowledgement, "Deine Anfrage wird bearbeitet.");
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
        LOGGER.trace("User [{}] received scene registration request: {}", userId, gson.toJson(data));
        final UnitConfig.Builder sceneUnitConfig = UnitConfig.newBuilder().setUnitType(UnitType.SCENE);

        UnitConfig location;
        String label = null;
        try {
            location = Registries.getUnitRegistry().getRootLocationConfig();
            if (data.has("location")) {
                final String locationLabel = data.get("location").getAsString();
                List<UnitConfig> locations = Registries.getUnitRegistry().getUnitConfigsByLabelAndUnitType(locationLabel, UnitType.LOCATION);
                if (locations.size() == 0) {
                    respond(acknowledgement, "Ich kann den Ort " + locationLabel + " nicht finden", true);
                } else {
                    location = locations.get(0);
                }
            }

            if (data.has("label")) {
                label = data.get("label").getAsString();
                final Label.MapFieldEntry.Builder entry = sceneUnitConfig.getLabelBuilder().addEntryBuilder();
                entry.setKey(Locale.GERMAN.getLanguage());
                entry.addValue(label);

                // make sure label is available for this location
                for (UnitConfig unitConfig : Registries.getUnitRegistry().getUnitConfigsByLocationIdAndUnitLabel(location.getId(), label)) {
                    if (unitConfig.getUnitType() == UnitType.SCENE) {
                        respond(acknowledgement, "Es existiert bereits eine Szene mit dem Name " + label + " in dem Ort " + LabelProcessor.getBestMatch(Locale.GERMAN, location.getLabel()), true);
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
                respond(acknowledgement, RESPONSE_GENERIC_ERROR, true);
                ExceptionPrinter.printHistory(ex, LOGGER);
                return;
            }

            try {
                final AuthenticatedValue authenticatedValue = SessionManager.getInstance().initializeRequest(sceneUnitConfig.build(), AuthToken.newBuilder().setAuthenticationToken(tokenStore.getCloudConnectorToken()).setAuthorizationToken(tokenStore.getBCOToken(userId)).build());
                UnitConfig unitConfig = new AuthenticatedValueFuture<>(Registries.getUnitRegistry().registerUnitConfigAuthenticated(authenticatedValue), UnitConfig.class, authenticatedValue.getTicketAuthenticatorWrapper(), SessionManager.getInstance()).get(5, TimeUnit.SECONDS);
                try {
                    label = LabelProcessor.getLabelByLanguage(Locale.GERMAN, unitConfig.getLabel());
                } catch (NotAvailableException ex) {
                    label = LabelProcessor.getBestMatch(unitConfig.getLabel());
                }
                respond(acknowledgement, "Die Szene " + label + " wurde erfolgreich registriert.");
            } catch (ExecutionException ex) {
                if (ExceptionProcessor.getInitialCause(ex) instanceof PermissionDeniedException) {
                    respond(acknowledgement, "Du besitzt nicht die Rechte eine Szene zu registrieren.");
                    return;
                }
                respond(acknowledgement, RESPONSE_GENERIC_ERROR, true);
                ExceptionPrinter.printHistory(ex, LOGGER);
            } catch (TimeoutException e) {
                if (label != null) {
                    respond(acknowledgement, "Das registrieren der Szene " + label + " dauert noch ein wenig.");
                } else {
                    respond(acknowledgement, "Das erstellen einer Szene von dem Ort " + LabelProcessor.getLabelByLanguage(Locale.GERMAN, location.getLabel()) + " dauert etwas länger.");
                }
            }
        } catch (InterruptedException ex) {
            respond(acknowledgement, RESPONSE_GENERIC_ERROR, true);
            Thread.currentThread().interrupt();
            ExceptionPrinter.printHistory(ex, LOGGER);
        } catch (CouldNotPerformException ex) {
            respond(acknowledgement, RESPONSE_GENERIC_ERROR, true);
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

    private class UnitRegistryObserver implements Observer<DataProvider<UnitRegistryData>, UnitRegistryData> {
        private JsonObject lastSyncResponse = null;

        @Override
        public void update(final DataProvider<UnitRegistryData> source, final UnitRegistryData data) throws Exception {

            // build a response for a sync request
            final JsonObject syncResponse = new JsonObject();
            FulfillmentHandler.handleSync(syncResponse, userId);
            // return if the response to a sync has not changed since the last time
            if (lastSyncResponse != null && lastSyncResponse.hashCode() == syncResponse.hashCode()) {
                return;
            }
            lastSyncResponse = syncResponse;

            // trigger sync if socket is connected and logged in
            // if the user is not logged the login process will trigger an update anyway
            if (isLoggedIn()) {
                requestSync();
            }
        }
    }

    int requestNumber = 0;

    private void requestSync() {
        requestNumber++;
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

    private void respond(final Ack acknowledgement, final String text) {
        respond(acknowledgement, text, false);
    }

    private void respond(final Ack acknowledgement, final String text, final boolean error) {
        final JsonObject response = new JsonObject();
        response.addProperty("text", text);
        response.addProperty("error", error);
        acknowledgement.call(gson.toJson(response));
    }

    public Future<Void> getLoginFuture() {
        return loginFuture;
    }
}
