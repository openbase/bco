package org.dc.bco.registry.device.core.dbconvert;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.dc.jul.storage.registry.version.DBVersionConverter;
import java.io.File;
import java.util.Map;

/**
 *
 * @author * @author <a href="mailto:DivineThreepwood@gmail.com">Divine Threepwood</a>
 */
public class DeviceConfig_0_To_1_DBConverter implements DBVersionConverter {

    @Override
    public JsonObject upgrade(JsonObject deviceConfig, final Map<File, JsonObject> dbSnapshot) {
        // recover and setup device class id
        JsonObject deviceClass = deviceConfig.get("device_class").getAsJsonObject();
        String deviceClassID = deviceClass.get("id").getAsString();
        deviceConfig.remove("device_class");
        deviceConfig.addProperty("device_class_id", deviceClassID);


        // recover unit config
        JsonArray unitConfigs = deviceConfig.get("unit_config").getAsJsonArray();
        for (JsonElement unitConfigElement : unitConfigs) {
            JsonObject unitConfig = unitConfigElement.getAsJsonObject();
            
            // remove service config
            unitConfig.remove("service_config");
            
            // reconstruct unit type and remove template
            String unitType = unitConfig.get("template").getAsJsonObject().get("type").getAsString();
            unitConfig.remove("template");
            unitConfig.addProperty("type", unitType);
        }
        return deviceConfig;
    }
}
