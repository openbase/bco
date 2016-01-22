/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dc.bco.registry.location.core.dbconvert;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import java.io.File;
import java.util.Map;
import org.dc.jul.storage.registry.version.DBVersionConverter;

/**
 *
 * @author <a href="mailto:thuxohl@techfak.uni-bielefeld.com">Tamino Huxohl</a>
 */
public class LocationConfig_3_To_4_DBConverter implements DBVersionConverter {

    @Override
    public JsonObject upgrade(JsonObject locationConfig, final Map<File, JsonObject> dbSnapshot) {
        // remove position
        if (locationConfig.getAsJsonObject("position") != null) {
            locationConfig.remove("position");
        }

        if (locationConfig.getAsJsonPrimitive("parent_id") == null) {
            return locationConfig;
        }

        String parentId = locationConfig.getAsJsonPrimitive("parent_id").getAsString();
        JsonObject placement;
        if (locationConfig.getAsJsonObject("placement_config") == null) {
            placement = new JsonObject();
        } else {
            placement = locationConfig.getAsJsonObject("placement_config");
        }

        for (JsonObject location : dbSnapshot.values()) {
            if (location.getAsJsonPrimitive("id").getAsString().equals(parentId)) {
                //parent exists
                placement.remove("location_id");
                placement.add("location_id", new JsonPrimitive(parentId));
            }
        }
        locationConfig.remove("parent_id");
        locationConfig.remove("placement_config");
        locationConfig.add("placement_config", placement);

        return locationConfig;
    }
}
