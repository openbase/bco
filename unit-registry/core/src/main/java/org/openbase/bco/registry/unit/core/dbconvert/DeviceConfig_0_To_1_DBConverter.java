package org.openbase.bco.registry.unit.core.dbconvert;

/*
 * #%L
 * BCO Registry Unit Core
 * %%
 * Copyright (C) 2014 - 2017 openbase.org
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.File;
import java.util.Map;
import org.openbase.jul.storage.registry.version.AbstractDBVersionConverter;
import org.openbase.jul.storage.registry.version.DBVersionControl;

/**
 *
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public class DeviceConfig_0_To_1_DBConverter extends AbstractDBVersionConverter {

    public DeviceConfig_0_To_1_DBConverter(DBVersionControl versionControl) {
        super(versionControl);
    }

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
