package de.citec.lm.core.registry.dbconvert;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import de.citec.jul.storage.registry.version.DBVersionConverter;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author * @author <a href="mailto:DivineThreepwood@gmail.com">Divine Threepwood</a>
 */
public class LocationConfig_0_To_1_DBConverter implements DBVersionConverter {

    @Override
    public JsonObject upgrade(JsonObject locationConfig) {

        // recover child ids
        JsonArray childConfigs = locationConfig.get("child").getAsJsonArray();
        List<String> childList = new ArrayList<>();
        for (JsonElement childConfigElement : childConfigs) {
            childList.add(childConfigElement.getAsJsonObject().get("id").getAsString());
        }
        // remove outdated child collection
        locationConfig.remove("child");

        // add new entries
        JsonArray child_id_array = new JsonArray();
        for (String child_id : childList) {
            child_id_array.add(child_id);
        }
        locationConfig.add("child_id", child_id_array);

        return locationConfig;
    }
}
