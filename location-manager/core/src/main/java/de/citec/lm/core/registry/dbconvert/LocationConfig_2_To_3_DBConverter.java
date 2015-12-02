package de.citec.lm.core.registry.dbconvert;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import de.citec.jul.storage.registry.version.DBVersionConverter;
import java.util.UUID;

/**
 *
 * @author <a href="mailto:thuxohl@techfak.uni-bielefeld.com">Tamino Huxohl</a>
 */
public class LocationConfig_2_To_3_DBConverter implements DBVersionConverter {

    @Override
    public JsonObject upgrade(JsonObject locationConfig) {
        locationConfig.remove("id");
        locationConfig.add("id", new JsonPrimitive(UUID.randomUUID().toString()));
        return locationConfig;
    }
}
