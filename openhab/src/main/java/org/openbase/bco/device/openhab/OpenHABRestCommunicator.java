package org.openbase.bco.device.openhab;

/*-
 * #%L
 * BCO Openhab Device Manager
 * %%
 * Copyright (C) 2015 - 2020 openbase.org
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
import org.eclipse.smarthome.config.discovery.dto.DiscoveryResultDTO;
import org.eclipse.smarthome.core.items.dto.ItemDTO;
import org.eclipse.smarthome.core.thing.dto.ThingDTO;
import org.eclipse.smarthome.core.thing.link.dto.ItemChannelLinkDTO;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.io.rest.core.item.EnrichedItemDTO;
import org.eclipse.smarthome.io.rest.core.thing.EnrichedThingDTO;
import org.openbase.bco.device.openhab.jp.JPOpenHABURI;
import org.openbase.jps.core.JPService;
import org.openbase.jps.exception.JPNotAvailableException;
import org.openbase.jul.exception.*;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.exception.printer.LogLevel;
import org.openbase.jul.iface.Shutdownable;
import org.openbase.jul.pattern.Observable;
import org.openbase.jul.pattern.ObservableImpl;
import org.openbase.jul.pattern.Observer;
import org.openbase.jul.schedule.SyncObject;
import org.openbase.type.domotic.state.ConnectionStateType.ConnectionState;
import org.openbase.type.domotic.state.ConnectionStateType.ConnectionState.State;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.sse.SseEventSource;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class OpenHABRestCommunicator implements Shutdownable {

    public static final String SEPARATOR = "/";
    public static final String REST_TARGET = "rest";
    public static final String ITEMS_TARGET = "items";
    public static final String LINKS_TARGET = "links";
    public static final String THINGS_TARGET = "things";
    public static final String INBOX_TARGET = "inbox";
    public static final String APPROVE_TARGET = "approve";
    public static final String EVENTS_TARGET = "events";

    public static final String TOPIC_KEY = "topic";
    public static final String TOPIC_SEPARATOR = SEPARATOR;

    private static final Logger LOGGER = LoggerFactory.getLogger(OpenHABRestCommunicator.class);

    private static OpenHABRestCommunicator instance = null;

    private boolean shutdownInitiated = false;

    private ConnectionState.State openhabConnectionState = State.DISCONNECTED;

    public static OpenHABRestCommunicator getInstance() {
        if (instance == null) {
            try {
                instance = new OpenHABRestCommunicator();
                Shutdownable.registerShutdownHook(instance);
            } catch (InitializationException ex) {
                ExceptionPrinter.printHistory("Could not create OpenHABRestCommunicator", ex, LOGGER);
            } catch (CouldNotPerformException ex) {
                // only thrown if instance would be null
            }
        }

        return instance;
    }

    private final WebTarget baseWebTarget;

    private final Gson gson;
    private final JsonParser jsonParser;

    private final SyncObject topicObservableMapLock = new SyncObject("topicObservableMapLock");
    private final Map<String, Observable<Object, JsonObject>> topicObservableMap;
    private SseEventSource sseEventSource;

    public OpenHABRestCommunicator() throws InitializationException {
        try {
            final Client client = ClientBuilder.newClient();
            final URI openHABRestURI = JPService.getProperty(JPOpenHABURI.class).getValue().resolve(SEPARATOR + REST_TARGET);
            this.baseWebTarget = client.target(openHABRestURI);

            this.gson = new GsonBuilder().create();
            this.jsonParser = new JsonParser();

            this.topicObservableMap = new HashMap<>();
        } catch (JPNotAvailableException ex) {
            throw new InitializationException(this, ex);
        }
    }

    public boolean isOpenHABOnline() {
        try {
            getDiscoveryResults();
        } catch (CouldNotPerformException e) {
            if (e.getCause() instanceof ProcessingException) {
                return false;
            }
        }
        return true;
    }

    public void waitForOpenHAB() throws InterruptedException {
        while (!isOpenHABOnline()) {
            Thread.sleep(5000);
        }
    }

    private void setConnectState(final ConnectionState.State connectState) {
        this.openhabConnectionState = connectState;
    }

    public boolean isShutdownInitiated() {
        return shutdownInitiated;
    }

    @Override
    public void shutdown() {
        shutdownInitiated = true;
        synchronized (topicObservableMapLock) {
            for (final Observable<Object, JsonObject> jsonObjectObservable : topicObservableMap.values()) {
                jsonObjectObservable.shutdown();
            }
            topicObservableMap.clear();
            if (sseEventSource != null) {
                sseEventSource.close();
            }
        }
    }

    public void addSSEObserver(Observer<Object, JsonObject> observer) {
        addSSEObserver(observer, "");
    }

    public void addSSEObserver(final Observer<Object, JsonObject> observer, final String topicRegex) {
        synchronized (topicObservableMapLock) {
            if (topicObservableMap.containsKey(topicRegex)) {
                topicObservableMap.get(topicRegex).addObserver(observer);
                return;
            }

            if (sseEventSource == null) {
                final WebTarget webTarget = baseWebTarget.path(EVENTS_TARGET);
                sseEventSource = SseEventSource.target(webTarget).reconnectingEvery(30, TimeUnit.SECONDS).build();

                try {
                    LOGGER.info("Wait for openHAB...");
                    setConnectState(State.CONNECTING);
                    waitForOpenHAB();
                    LOGGER.info("OpenHAB is online");
                    // todo release: connection state needs to be updated and a proper reconnect performed in case the connection is lost!
                    setConnectState(State.CONNECTED);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                sseEventSource.open();
            }

            final ObservableImpl<Object, JsonObject> observable = new ObservableImpl<>(this);
            observable.addObserver(observer);
            topicObservableMap.put(topicRegex, observable);
            sseEventSource.register(inboundSseEvent -> {
                try {
                    final JsonObject payload = jsonParser.parse(inboundSseEvent.readData()).getAsJsonObject();
                    if (payload.get(TOPIC_KEY).getAsString().matches(topicRegex)) {
                        observable.notifyObservers(payload);
                    }
                } catch (Exception ex) {
                    ExceptionPrinter.printHistory(new CouldNotPerformException("Could not notify listeners on topic[" + topicRegex + "]", ex), LOGGER);
                }
            }, ex -> {
                ExceptionPrinter.printHistory("Openhab connection error detected!", ex, LOGGER, LogLevel.WARN);
            });
        }
    }

    public void removeSSEObserver(Observer<Object, JsonObject> observer) {
        removeSSEObserver(observer, "");
    }

    public void removeSSEObserver(Observer<Object, JsonObject> observer, final String topicFilter) {
        synchronized (topicObservableMapLock) {
            if (topicObservableMap.containsKey(topicFilter)) {
                topicObservableMap.get(topicFilter).removeObserver(observer);
            }
        }
    }

    // ==========================================================================================================================================
    // THINGS
    // ==========================================================================================================================================

    public EnrichedThingDTO registerThing(final ThingDTO thingDTO) throws CouldNotPerformException {
        return jsonToClass(jsonParser.parse(postJson(THINGS_TARGET, thingDTO)), EnrichedThingDTO.class);
    }

    public EnrichedThingDTO updateThing(final EnrichedThingDTO enrichedThingDTO) throws CouldNotPerformException {
        return jsonToClass(jsonParser.parse(putJson(THINGS_TARGET + SEPARATOR + enrichedThingDTO.UID, enrichedThingDTO)), EnrichedThingDTO.class);
    }

    public EnrichedThingDTO deleteThing(final EnrichedThingDTO enrichedThingDTO) throws CouldNotPerformException {
        return deleteThing(enrichedThingDTO.UID);
    }

    public EnrichedThingDTO deleteThing(final String thingUID) throws CouldNotPerformException {
        return jsonToClass(jsonParser.parse(delete(THINGS_TARGET + SEPARATOR + thingUID)), EnrichedThingDTO.class);
    }

    public EnrichedThingDTO getThing(final String thingUID) throws NotAvailableException {
        try {
            return jsonToClass(jsonParser.parse(get(THINGS_TARGET + SEPARATOR + thingUID)), EnrichedThingDTO.class);
        } catch (CouldNotPerformException ex) {
            throw new NotAvailableException("Thing[" + thingUID + "]");
        }
    }

    public List<EnrichedThingDTO> getThings() throws CouldNotPerformException {
        return jsonElementToTypedList(jsonParser.parse(get(THINGS_TARGET)), EnrichedThingDTO.class);
    }

    // ==========================================================================================================================================
    // ITEMS
    // ==========================================================================================================================================

    public ItemDTO registerItem(final ItemDTO itemDTO) throws CouldNotPerformException {
        final List<ItemDTO> itemDTOList = new ArrayList<>();
        itemDTOList.add(itemDTO);
        return registerItems(itemDTOList).get(0);
    }

    public List<ItemDTO> registerItems(final List<ItemDTO> itemDTOList) throws CouldNotPerformException {
        return jsonElementToTypedList(jsonParser.parse(putJson(ITEMS_TARGET, itemDTOList)), ItemDTO.class);
    }

    public ItemDTO updateItem(final ItemDTO itemDTO) throws CouldNotPerformException {
        return jsonToClass(jsonParser.parse(putJson(ITEMS_TARGET + SEPARATOR + itemDTO.name, itemDTO)), ItemDTO.class);
    }

    public ItemDTO deleteItem(final ItemDTO itemDTO) throws CouldNotPerformException {
        LOGGER.error("Delete item {}", itemDTO.name);
        return deleteItem(itemDTO.name);
    }

    public ItemDTO deleteItem(final String itemName) throws CouldNotPerformException {
        LOGGER.error("Delete item {}", itemName);
        return jsonToClass(jsonParser.parse(delete(ITEMS_TARGET + SEPARATOR + itemName)), ItemDTO.class);
    }

    public List<EnrichedItemDTO> getItems() throws CouldNotPerformException {
        return jsonElementToTypedList(jsonParser.parse(get(ITEMS_TARGET)), EnrichedItemDTO.class);
    }

    public EnrichedItemDTO getItem(final String itemName) throws NotAvailableException {
        try {
            return jsonToClass(jsonParser.parse(get(ITEMS_TARGET + SEPARATOR + itemName)), EnrichedItemDTO.class);
        } catch (CouldNotPerformException ex) {
            throw new NotAvailableException("Item with name[" + itemName + "]");
        }
    }

    public boolean hasItem(final String itemName) {
        try {
            getItem(itemName);
            return true;
        } catch (NotAvailableException ex) {
            return false;
        }
    }

    public void postCommand(final String itemName, final Command command) throws CouldNotPerformException {
        postCommand(itemName, command.toString());
    }

    public void postCommand(final String itemName, final String command) throws CouldNotPerformException {
        post(ITEMS_TARGET + SEPARATOR + itemName, command, MediaType.TEXT_PLAIN_TYPE);
    }

    // ==========================================================================================================================================
    // ITEM_CHANNEL_LINK
    // ==========================================================================================================================================

    public void registerItemChannelLink(final String itemName, final String channelUID) throws CouldNotPerformException {
        registerItemChannelLink(new ItemChannelLinkDTO(itemName, channelUID, new HashMap<>()));
    }

    public void registerItemChannelLink(final ItemChannelLinkDTO itemChannelLinkDTO) throws CouldNotPerformException {
        putJson(LINKS_TARGET + SEPARATOR + itemChannelLinkDTO.itemName + SEPARATOR + itemChannelLinkDTO.channelUID, itemChannelLinkDTO);
    }

    public void deleteItemChannelLink(final ItemChannelLinkDTO itemChannelLinkDTO) throws CouldNotPerformException {
        deleteItemChannelLink(itemChannelLinkDTO.itemName, itemChannelLinkDTO.channelUID);
    }

    public void deleteItemChannelLink(final String itemName, final String channelUID) throws CouldNotPerformException {
        delete(LINKS_TARGET + SEPARATOR + itemName + SEPARATOR + channelUID);
    }

    public List<ItemChannelLinkDTO> getItemChannelLinks() throws CouldNotPerformException {
        return jsonElementToTypedList(jsonParser.parse(get(LINKS_TARGET)), ItemChannelLinkDTO.class);
    }

    // ==========================================================================================================================================
    // DISCOVERY
    // ==========================================================================================================================================

    public void approve(final String thingUID, final String label) throws CouldNotPerformException {
        post(INBOX_TARGET + SEPARATOR + thingUID + SEPARATOR + APPROVE_TARGET, label, MediaType.TEXT_PLAIN_TYPE);
    }

    public List<DiscoveryResultDTO> getDiscoveryResults() throws CouldNotPerformException {
        return jsonElementToTypedList(jsonParser.parse(get(INBOX_TARGET)), DiscoveryResultDTO.class);
    }

    // ==========================================================================================================================================
    // UTIL
    // ==========================================================================================================================================

    private <T> List<T> jsonElementToTypedList(final JsonElement jsonElement, final Class<T> clazz) throws CouldNotPerformException {
        if (jsonElement.isJsonArray()) {
            return jsonArrayToTypedList(jsonElement.getAsJsonArray(), clazz);
        } else {
            throw new CouldNotPerformException("JsonElement is not a JsonArray and thus cannot be converted to a list");
        }
    }

    private <T> List<T> jsonArrayToTypedList(final JsonArray jsonArray, final Class<T> clazz) throws CouldNotPerformException {
        final List<T> result = new ArrayList<T>();

        for (final JsonElement jsonElement : jsonArray) {
            result.add(jsonToClass(jsonElement, clazz));
        }

        return result;
    }

    private <T> T jsonToClass(final JsonElement jsonElement, final Class<T> clazz) throws CouldNotPerformException {
        try {
            return gson.fromJson(jsonElement, clazz);
        } catch (JsonSyntaxException ex) {
            throw new CouldNotPerformException("Could not parse jsonElement into object of class[" + clazz.getSimpleName() + "]", ex);
        }
    }

    private String get(final String target) throws CouldNotPerformException {
        try {
            final WebTarget webTarget = baseWebTarget.path(target);
            final Response response = webTarget.request().get();

            return validateResponse(response);
        } catch (CouldNotPerformException | ProcessingException ex) {
            if(isShutdownInitiated()) {
                ExceptionProcessor.setInitialCause(ex, new ShutdownInProgressException(this));
            }
            throw new CouldNotPerformException("Could not get sub-URL[" + target + "]", ex);
        }
    }

    private String delete(final String target) throws CouldNotPerformException {
        try {
            final WebTarget webTarget = baseWebTarget.path(target);
            final Response response = webTarget.request().delete();

            return validateResponse(response);
        } catch (CouldNotPerformException | ProcessingException ex) {
            if(isShutdownInitiated()) {
                ExceptionProcessor.setInitialCause(ex, new ShutdownInProgressException(this));
            }
            throw new CouldNotPerformException("Could not delete sub-URL[" + target + "]", ex);
        }
    }

    private String putJson(final String target, final Object value) throws CouldNotPerformException {
        return put(target, gson.toJson(value), MediaType.APPLICATION_JSON_TYPE);
    }

    private String put(final String target, final String value, final MediaType mediaType) throws CouldNotPerformException {
        try {
            final WebTarget webTarget = baseWebTarget.path(target);
            final Response response = webTarget.request().put(Entity.entity(value, mediaType));

            return validateResponse(response);
        } catch (CouldNotPerformException | ProcessingException ex) {
            if(isShutdownInitiated()) {
                ExceptionProcessor.setInitialCause(ex, new ShutdownInProgressException(this));
            }
            throw new CouldNotPerformException("Could not put value[" + value + "] on sub-URL[" + target + "]", ex);
        }
    }

    private String postJson(final String target, final Object value) throws CouldNotPerformException {
        return post(target, gson.toJson(value), MediaType.APPLICATION_JSON_TYPE);
    }

    private String post(final String target, final String value, final MediaType mediaType) throws CouldNotPerformException {
        try {

            if(openhabConnectionState != State.CONNECTED) {
                throw new InvalidStateException("Openhab not reachable yet!");
            }

            final WebTarget webTarget = baseWebTarget.path(target);
            final Response response = webTarget.request().post(Entity.entity(value, mediaType));

            return validateResponse(response);
        } catch (CouldNotPerformException | ProcessingException ex) {
            if(isShutdownInitiated()) {
                ExceptionProcessor.setInitialCause(ex, new ShutdownInProgressException(this));
            }
            throw new CouldNotPerformException("Could not post value[" + value + "] on sub-URL[" + target + "]", ex);
        }
    }

    private String validateResponse(final Response response) throws CouldNotPerformException, ProcessingException {
        final String result = response.readEntity(String.class);

        if (response.getStatus() == 200 || response.getStatus() == 201 || response.getStatus() == 202) {
            return result;
        } else if (response.getStatus() == 404) {
            throw new NotAvailableException("URL");
        } else if (response.getStatus() == 503) {
            // throw a processing exception to indicate that openHAB is still not fully started, this is used to wait for openHAB
            throw new ProcessingException("OpenHAB server not ready");
        } else {
            throw new CouldNotPerformException("Response returned with errorCode[" + response.getStatus() + "] and error message[" + result + "]");
        }
    }
}
