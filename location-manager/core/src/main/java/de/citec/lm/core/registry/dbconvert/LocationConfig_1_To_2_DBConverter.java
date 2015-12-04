/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.lm.core.registry.dbconvert;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import de.citec.jul.storage.registry.version.DBVersionConverter;
import java.io.File;
import java.util.Map;

/**
 *
 * @author <a href="mailto:thuxohl@techfak.uni-bielefeld.com">Tamino Huxohl</a>
 */
public class LocationConfig_1_To_2_DBConverter implements DBVersionConverter {

    @Override
    public JsonObject upgrade(JsonObject locationConfig, final Map<File, JsonObject> dbSnapshot) {

        // check if child element exists otherwise we are finish
        JsonElement placement = locationConfig.get("placement_config");
        if (placement == null) {
            locationConfig.add("placement_config", copyPlacement(locationConfig));
        }
        return locationConfig;
    }

    private JsonObject copyPlacement(JsonObject locationConfig) {
        final JsonObject placement = new JsonObject();
        placement.add("position", locationConfig.get("position"));
        return placement;
    }
}
