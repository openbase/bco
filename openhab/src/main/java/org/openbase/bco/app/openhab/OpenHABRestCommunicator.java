package org.openbase.bco.app.openhab;

import com.google.gson.*;
import org.eclipse.smarthome.config.discovery.dto.DiscoveryResultDTO;
import org.eclipse.smarthome.io.rest.core.thing.EnrichedThingDTO;
import org.openbase.jul.exception.CouldNotPerformException;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;

public class OpenHABRestCommunicator {

    //TODO: parse from java property
    public static String openhabIp = "localhost";

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
        this.baseWebTarget = client.target("http://" + openhabIp + ":8080/rest");

        this.gson = new GsonBuilder().create();
        this.jsonParser = new JsonParser();
    }

    public EnrichedThingDTO registerThing(final EnrichedThingDTO enrichedThingDTO) throws CouldNotPerformException {
        return parse(jsonParser.parse(post("things", enrichedThingDTO)), EnrichedThingDTO.class);
    }

    public EnrichedThingDTO updateThing(final EnrichedThingDTO enrichedThingDTO) throws CouldNotPerformException {
        return parse(jsonParser.parse(put("things/" + enrichedThingDTO.UID, enrichedThingDTO)), EnrichedThingDTO.class);
    }

    public EnrichedThingDTO deleteThing(final EnrichedThingDTO enrichedThingDTO) throws CouldNotPerformException {
        return deleteThing(enrichedThingDTO.UID);
    }

    public EnrichedThingDTO deleteThing(final String thingUID) throws CouldNotPerformException {
        return parse(jsonParser.parse(delete("things/" + thingUID)), EnrichedThingDTO.class);
    }

    public List<EnrichedThingDTO> getThings() throws CouldNotPerformException {
        try {
            JsonElement things = jsonParser.parse(get("things"));
            if (things.isJsonArray()) {
                return jsonArrayToTypesList(things.getAsJsonArray(), EnrichedThingDTO.class);
            } else {
                throw new CouldNotPerformException("Response for query does not match expected type jsonArray");
            }
        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not request current things from openHAB", ex);
        }
    }

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
                return jsonArrayToTypesList(things.getAsJsonArray(), DiscoveryResultDTO.class);
            } else {
                throw new CouldNotPerformException("Response for query does not match expected type jsonArray");
            }
        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not request inbox from openHAB", ex);
        }
    }

    private <T> List<T> jsonArrayToTypesList(final JsonArray jsonArray, final Class<T> clazz) throws CouldNotPerformException {
        final List<T> result = new ArrayList<T>();

        for (final JsonElement jsonElement : jsonArray) {
            result.add(parse(jsonElement, clazz));
        }

        return result;
    }

    private <T> T parse(final JsonElement jsonElement, final Class<T> clazz) throws CouldNotPerformException {
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

    private String put(final String target, final Object value) throws CouldNotPerformException {
        try {
            final WebTarget webTarget = baseWebTarget.path(target);
            final Response response = webTarget.request().put(Entity.entity(gson.toJson(value), MediaType.APPLICATION_JSON_TYPE));

            return validateResponse(response);
        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not put value[" + value + "] on target[" + target + "]", ex);
        }
    }

    private String post(final String target, final Object value) throws CouldNotPerformException {
        try {
            final WebTarget webTarget = baseWebTarget.path(target);
            final Response response = webTarget.request().post(Entity.entity(gson.toJson(value), MediaType.APPLICATION_JSON_TYPE));

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
