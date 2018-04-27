package org.openbase.bco.app.openhab;

import com.google.gson.*;
import org.eclipse.smarthome.config.discovery.dto.DiscoveryResultDTO;
import org.eclipse.smarthome.core.items.dto.ItemDTO;
import org.eclipse.smarthome.core.thing.link.dto.ItemChannelLinkDTO;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.io.rest.core.item.EnrichedItemDTO;
import org.eclipse.smarthome.io.rest.core.thing.EnrichedThingDTO;
import org.openbase.jul.exception.CouldNotPerformException;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OpenHABRestCommunicator {

    //TODO: parse from java properties
    public static final String OPENHAB_IP = "localhost";
    public static final String PORT = "8080";

    public static final String SEPARATOR = "/";
    public static final String REST_TARGET = "rest";
    public static final String ITEMS_TARGET = "items";
    public static final String LINKS_TARGET = "links";
    public static final String THINGS_TARGET = "things";

    private static OpenHABRestCommunicator instance = null;

    public static OpenHABRestCommunicator getInstance() {
        if (instance == null) {
            instance = new OpenHABRestCommunicator();
        }

        return instance;
    }

    private final Client client;
    private final WebTarget baseWebTarget;

    private final Gson gson;
    private final JsonParser jsonParser;

    public OpenHABRestCommunicator() {
        this.client = ClientBuilder.newClient();
        this.baseWebTarget = client.target("http://" + OPENHAB_IP + ":" + PORT + SEPARATOR + REST_TARGET);

        this.gson = new GsonBuilder().create();
        this.jsonParser = new JsonParser();
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
        try {
            JsonElement itemChannelLinks = jsonParser.parse(get(LINKS_TARGET));
            if (itemChannelLinks.isJsonArray()) {
                return jsonArrayToTypedList(itemChannelLinks.getAsJsonArray(), ItemChannelLinkDTO.class);
            } else {
                throw new CouldNotPerformException("Response for query does not match expected type jsonArray");
            }
        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not request current item channel links from openHAB", ex);
        }
    }

    // ==========================================================================================================================================
    // DISCOVERY
    // ==========================================================================================================================================

    public void approve(final DiscoveryResultDTO discoveryResultDTO) throws CouldNotPerformException {
        approve(discoveryResultDTO.thingUID, discoveryResultDTO.label);
    }

    public void approve(final String thingUID, final String label) throws CouldNotPerformException {
        try {
            final WebTarget webTarget = baseWebTarget.path("inbox/" + thingUID + "/approve");
            final Response response = webTarget.request().post(Entity.entity(label, MediaType.TEXT_PLAIN_TYPE));

            validateResponse(response);
        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not approve thing[" + thingUID + "] with label[" + label + "]", ex);
        }
    }

    public List<DiscoveryResultDTO> getThingsFromInbox() throws CouldNotPerformException {
        try {
            JsonElement things = jsonParser.parse(get("inbox"));
            System.out.println(things.toString());
            if (things.isJsonArray()) {
                return jsonArrayToTypedList(things.getAsJsonArray(), DiscoveryResultDTO.class);
            } else {
                throw new CouldNotPerformException("Response for query does not match expected type jsonArray");
            }
        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not request inbox from openHAB", ex);
        }
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
        post(target, gson.toJson(value), MediaType.APPLICATION_JSON_TYPE);
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
}
