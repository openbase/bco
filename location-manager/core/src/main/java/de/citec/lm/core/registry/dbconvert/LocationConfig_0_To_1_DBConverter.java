package de.citec.lm.core.registry.dbconvert;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.dc.jul.storage.registry.version.DBVersionConverter;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 *
 * @author * @author <a href="mailto:DivineThreepwood@gmail.com">Divine Threepwood</a>
 */
public class LocationConfig_0_To_1_DBConverter implements DBVersionConverter {

    @Override
    public JsonObject upgrade(final JsonObject locationConfig, final Map<File, JsonObject> dbSnapshot) {


        // check if child element exists otherwise we are finish
        JsonElement childElement = locationConfig.get("child");
        if(childElement == null) {
            return locationConfig;
        }

        // recover child ids
        JsonArray childConfigs = childElement.getAsJsonArray();
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
