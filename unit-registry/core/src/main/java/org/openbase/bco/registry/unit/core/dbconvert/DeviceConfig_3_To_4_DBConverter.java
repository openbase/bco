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
 * DBConverter renaming bound_to_system_unit to bound_to_unit_host and system_unit_id to unit_host_id.
 *
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class DeviceConfig_3_To_4_DBConverter extends AbstractDBVersionConverter {

    private static final String UNIT_CONFIG_FIELD = "unit_config";
    private static final String BOUND_TO_SYSTEM_UNIT_FIELD = "bound_to_system_unit";
    private static final String BOUND_TO_UNIT_HOST_FIELD = "bound_to_unit_host";
    private static final String SYSTEM_UNIT_ID = "system_unit_id";
    private static final String UNIT_HOST_ID = "unit_host_id";

    public DeviceConfig_3_To_4_DBConverter(DBVersionControl versionControl) {
        super(versionControl);
    }

    @Override
    public JsonObject upgrade(JsonObject deviceConfig, final Map<File, JsonObject> dbSnapshot) {
        if (deviceConfig.has(UNIT_CONFIG_FIELD)) {
            JsonArray unitConfigs = deviceConfig.getAsJsonArray(UNIT_CONFIG_FIELD);
            for (JsonElement unitConfig : unitConfigs) {
                JsonObject unit = unitConfig.getAsJsonObject();

                if (unit.has(BOUND_TO_SYSTEM_UNIT_FIELD)) {
                    boolean bound = unit.get(BOUND_TO_SYSTEM_UNIT_FIELD).getAsBoolean();
                    unit.remove(BOUND_TO_SYSTEM_UNIT_FIELD);
                    unit.addProperty(BOUND_TO_UNIT_HOST_FIELD, bound);
                }

                if (unit.has(SYSTEM_UNIT_ID)) {
                    String hostId = unit.get(SYSTEM_UNIT_ID).getAsString();
                    unit.remove(SYSTEM_UNIT_ID);
                    unit.addProperty(UNIT_HOST_ID, hostId);
                }
            }
        }

        return deviceConfig;
    }

}
