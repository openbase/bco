package org.openbase.bco.app.openhab;

import com.google.gson.*;
import org.eclipse.smarthome.config.discovery.dto.DiscoveryResultDTO;
import org.eclipse.smarthome.core.items.dto.ItemDTO;
import org.eclipse.smarthome.core.thing.link.dto.ItemChannelLinkDTO;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.io.rest.core.item.EnrichedItemDTO;
import org.eclipse.smarthome.io.rest.core.thing.EnrichedThingDTO;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.iface.Shutdownable;
import org.openbase.jul.pattern.ObservableImpl;
import org.openbase.jul.pattern.Observer;
import org.openbase.jul.schedule.SyncObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.sse.SseEventSource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OpenHABRestCommunicator implements Shutdownable {

    //TODO: parse from java properties
    public static final String OPENHAB_IP = "localhost";
    public static final String PORT = "8080";

    public static final String SEPARATOR = "/";
    public static final String REST_TARGET = "rest";
    public static final String ITEMS_TARGET = "items";
    public static final String LINKS_TARGET = "links";
    public static final String THINGS_TARGET = "things";
    public static final String INBOX_TARGET = "inbox";
    public static final String APPROVE_TARGET = "approve";

    public static final String TOPIC_KEY = "topic";
    public static final String TOPIC_SEPERATOR = SEPARATOR;

    private static final Logger LOGGER = LoggerFactory.getLogger(OpenHABRestCommunicator.class);

    private static OpenHABRestCommunicator instance = null;

    public static OpenHABRestCommunicator getInstance() {
        if (instance == null) {
            instance = new OpenHABRestCommunicator();
            try {
                Shutdownable.registerShutdownHook(instance);
            } catch (CouldNotPerformException ex) {
                // only thrown if instance would be null
            }
        }

        return instance;
    }

    private final Client client;
    private final WebTarget baseWebTarget;

    private final Gson gson;
    private final JsonParser jsonParser;

    private final Map<String, EventSourceObservableMapping> topicEventSourceMap;
    private final SyncObject eventSourceLock = new SyncObject("EventSourceLock");

    public OpenHABRestCommunicator() {
        this.client = ClientBuilder.newClient();
        this.baseWebTarget = client.target("http://" + OPENHAB_IP + ":" + PORT + SEPARATOR + REST_TARGET);

        this.gson = new GsonBuilder().create();
        this.jsonParser = new JsonParser();

        topicEventSourceMap = new HashMap<>();
    }

    @Override
    public void shutdown() {
        synchronized (eventSourceLock) {
            for (EventSourceObservableMapping mapping : topicEventSourceMap.values()) {
                mapping.getSseEventSource().close();
            }
            topicEventSourceMap.clear();
        }
    }

    public void addSSEObserver(Observer<JsonObject> observer) {
        addSSEObserver(observer, "");
    }

    public void addSSEObserver(Observer<JsonObject> observer, final String topicFilter) {
        synchronized (eventSourceLock) {
            if (topicEventSourceMap.containsKey(topicFilter)) {
                topicEventSourceMap.get(topicFilter).addObserver(observer);
                return;
            }

            final String path = (topicFilter.isEmpty()) ? "" : "?topics=" + topicFilter;
            final WebTarget webTarget = baseWebTarget.path(path);
            final SseEventSource sseEventSource = SseEventSource.target(webTarget).build();
            final ObservableImpl<JsonObject> observable = new ObservableImpl<>();
            topicEventSourceMap.put(topicFilter, new EventSourceObservableMapping(sseEventSource, observable));

            sseEventSource.register(inboundSseEvent -> {
                // parse payload as json
                final JsonObject payload = jsonParser.parse(inboundSseEvent.readData()).getAsJsonObject();
                try {
                    observable.notifyObservers(payload);
                } catch (Exception ex) {
                    ExceptionPrinter.printHistory(new CouldNotPerformException("Could not notify listeners on topic[" + topicFilter + "]", ex), LOGGER);
                }
            });
            sseEventSource.open();
        }
    }

    public void removeSSEObserver(Observer<JsonObject> observer) {
        removeSSEObserver(observer, "");
    }

    public void removeSSEObserver(Observer<JsonObject> observer, final String topicFilter) {
        synchronized (eventSourceLock) {
            if (topicEventSourceMap.containsKey(topicFilter)) {
                final EventSourceObservableMapping mapping = topicEventSourceMap.get(topicFilter);
                mapping.removeObserver(observer);

                if (mapping.getObserverCount() == 0) {
                    topicEventSourceMap.remove(topicFilter);

                    mapping.getSseEventSource().close();
                }
            }
        }
    }

    // ==========================================================================================================================================
    // THINGS
    // ==========================================================================================================================================

    public EnrichedThingDTO registerThing(final EnrichedThingDTO enrichedThingDTO) throws CouldNotPerformException {
        return jsonToClass(jsonParser.parse(postJson(THINGS_TARGET, enrichedThingDTO)), EnrichedThingDTO.class);
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
        return deleteItem(itemDTO.name);
    }

    public ItemDTO deleteItem(final String itemName) throws CouldNotPerformException {
        return jsonToClass(jsonParser.parse(delete(ITEMS_TARGET + SEPARATOR + itemName)), ItemDTO.class);
    }

    public List<EnrichedItemDTO> getItems() throws CouldNotPerformException {
        return jsonElementToTypedList(jsonParser.parse(get(ITEMS_TARGET)), EnrichedItemDTO.class);
    }

    public void postCommand(final String itemName, final Command command) throws CouldNotPerformException {
        postCommand(itemName, command.toString());
    }

    public void postCommand(final String itemName, final String command) throws CouldNotPerformException {
        post(ITEMS_TARGET + SEPARATOR + itemName, command, MediaType.TEXT_PLAIN_TYPE);
    }

    public Map<String, String> getStates() throws CouldNotPerformException {
        final Map<String, String> itemNameStateMap = new HashMap<>();
        for (final EnrichedItemDTO enrichedItemDTO : getItems()) {
            itemNameStateMap.put(enrichedItemDTO.name, enrichedItemDTO.state);
        }
        return itemNameStateMap;
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
        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not get target[" + target + "]", ex);
        }
    }

    private String delete(final String target) throws CouldNotPerformException {
        try {
            final WebTarget webTarget = baseWebTarget.path(target);
            final Response response = webTarget.request().delete();

            return validateResponse(response);
        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not delete target[" + target + "]", ex);
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
        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not put value[" + value + "] on target[" + target + "]", ex);
        }
    }

    private String postJson(final String target, final Object value) throws CouldNotPerformException {
        return post(target, gson.toJson(value), MediaType.APPLICATION_JSON_TYPE);
    }

    private String post(final String target, final String value, final MediaType mediaType) throws CouldNotPerformException {
        try {
            final WebTarget webTarget = baseWebTarget.path(target);
            final Response response = webTarget.request().post(Entity.entity(value, mediaType));

            return validateResponse(response);
        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not post value[" + value + "] on target[" + target + "]", ex);
        }
    }

    private String validateResponse(final Response response) throws CouldNotPerformException {
        final String result = response.readEntity(String.class);

        if (response.getStatus() == 200 || response.getStatus() == 202) {
            return result;
        } else {
            throw new CouldNotPerformException("Response returned with errorCode[" + response.getStatus() + "] and error message[" + result + "]");
        }
    }

    private static class EventSourceObservableMapping {
        private final SseEventSource sseEventSource;
        private final ObservableImpl<JsonObject> observable;
        private int observerCount;

        public EventSourceObservableMapping(final SseEventSource sseEventSource, ObservableImpl<JsonObject> objectObservable) {
            this.sseEventSource = sseEventSource;
            this.observable = objectObservable;
        }

        public void addObserver(final Observer<JsonObject> observer) {
            this.observable.addObserver(observer);
            this.observerCount++;
        }

        public void removeObserver(final Observer<JsonObject> observer) {
            this.observable.removeObserver(observer);
            this.observerCount--;
        }

        public int getObserverCount() {
            return observerCount;
        }

        public SseEventSource getSseEventSource() {
            return sseEventSource;
        }
    }
}
