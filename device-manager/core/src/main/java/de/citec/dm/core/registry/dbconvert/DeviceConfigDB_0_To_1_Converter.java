package de.citec.dm.core.registry.dbconvert;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import de.citec.dm.core.registry.DBVersionConverter;

/**
 *
 * @author * @author <a href="mailto:DivineThreepwood@gmail.com">Divine Threepwood</a>
 */
public class DeviceConfigDB_0_To_1_Converter implements DBVersionConverter {

    @Override
    public JsonObject upgrade(JsonObject deviceConfig) {
        // recover and setup device class id
        JsonObject deviceClass = deviceConfig.get("device_class").getAsJsonObject();
        String deviceClassID = deviceClass.get("id").getAsString();
        deviceConfig.addProperty("device_class_id", deviceClassID);
        deviceConfig.remove("device_class");

        // remove service config
        JsonArray unitConfigs = deviceConfig.get("unit_config").getAsJsonArray();
        for (JsonElement unitConfig : unitConfigs) {
            unitConfig.getAsJsonObject().remove("service_config");
        }
        return deviceConfig;
    }
}
