/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.lm.core.registry.dbconvert;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import de.citec.jul.storage.registry.version.DBVersionConverter;

/**
 *
 * @author <a href="mailto:thuxohl@techfak.uni-bielefeld.com">Tamino Huxohl</a>
 */
public class LocationConfig_1_To_2_DBConverter implements DBVersionConverter {

    @Override
    public JsonObject upgrade(JsonObject locationConfig) {

        // check if child element exists otherwise we are finish
        JsonElement placement = locationConfig.get("placement_config");
        if (placement == null) {
            locationConfig.add("placement_config", getDefaultPlacement());
        }

        return locationConfig;
    }

    private JsonObject getDefaultPlacement() {
        JsonObject translation = new JsonObject();
        translation.add("x", new JsonPrimitive(0));
        translation.add("y", new JsonPrimitive(0));
        translation.add("z", new JsonPrimitive(0));

        JsonObject rotation = new JsonObject();
        rotation.add("qw", new JsonPrimitive(1));
        rotation.add("qx", new JsonPrimitive(0));
        rotation.add("qy", new JsonPrimitive(0));
        rotation.add("qz", new JsonPrimitive(0));

        JsonObject position = new JsonObject();
        position.add("translation", translation);
        position.add("rotation", rotation);

        JsonObject placement = new JsonObject();
        placement.add("position", position);
        return placement;
    }
}
