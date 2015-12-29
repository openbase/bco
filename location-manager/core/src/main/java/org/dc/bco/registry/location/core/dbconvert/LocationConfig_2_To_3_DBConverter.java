package org.dc.bco.registry.location.core.dbconvert;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import org.dc.jul.storage.registry.version.DBVersionConverter;
import java.io.File;
import java.util.Map;
import java.util.UUID;

/**
 *
 * @author <a href="mailto:thuxohl@techfak.uni-bielefeld.com">Tamino Huxohl</a>
 */
public class LocationConfig_2_To_3_DBConverter implements DBVersionConverter {

    @Override
    public JsonObject upgrade(JsonObject locationConfig, final Map<File, JsonObject> dbSnapshot) {
        String oldId = locationConfig.getAsJsonPrimitive("id").getAsString();
        String newId = UUID.randomUUID().toString();

        locationConfig.remove("id");
        locationConfig.add("id", new JsonPrimitive(newId));

        for (JsonObject location : dbSnapshot.values()) {
            if (location.getAsJsonPrimitive("id").getAsString().equals(oldId)) {
                // set new id here?
                continue;
            }

            // change parent_id in all location if needed to the new id
            if (location.getAsJsonPrimitive("parent_id") != null && location.getAsJsonPrimitive("parent_id").getAsString().equals(oldId)) {
                location.remove("parent_id");
                location.add("parent_id", new JsonPrimitive(newId));
            }

            // adjust the child ids if needed
            if (location.getAsJsonArray("child_id") == null) {
                continue;
            }
            JsonArray childIdArray = new JsonArray();
            for (JsonElement childId : location.getAsJsonArray("child_id")) {
                if (childId.getAsJsonPrimitive().getAsString().equals(oldId)) {
                    childIdArray.add(newId);
                } else {
                    childIdArray.add(childId.getAsJsonPrimitive().getAsString());
                }
            }
            location.remove("child_id");
            location.add("child_id", childIdArray);
        }

        return locationConfig;
    }
}
