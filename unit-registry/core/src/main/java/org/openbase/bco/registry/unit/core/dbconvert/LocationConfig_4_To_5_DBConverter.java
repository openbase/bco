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
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import java.io.File;
import java.util.Map;
import org.openbase.jul.storage.registry.version.AbstractDBVersionConverter;
import org.openbase.jul.storage.registry.version.DBVersionControl;

/**
 *
 */
public class LocationConfig_4_To_5_DBConverter extends AbstractDBVersionConverter {

    public LocationConfig_4_To_5_DBConverter(DBVersionControl versionControl) {
        super(versionControl);
    }

    @Override
    public JsonObject upgrade(JsonObject locationConfig, final Map<File, JsonObject> dbSnapshot) {

        // check if location is a tile
        if (locationConfig.getAsJsonPrimitive("type").getAsString().equalsIgnoreCase("tile")) {
            JsonObject tileConfig = locationConfig.getAsJsonObject("tile_config");
            if (tileConfig != null && tileConfig.getAsJsonPrimitive("type") != null && tileConfig.getAsJsonPrimitive("type").getAsString().equals("ROOM")) {
                tileConfig.remove("type");
                tileConfig.add("type", new JsonPrimitive("INDOOR"));
            }
        }
        return locationConfig;
    }
}
