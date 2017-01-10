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
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class LocationConfig_3_To_4_DBConverter extends AbstractDBVersionConverter {

    public LocationConfig_3_To_4_DBConverter(DBVersionControl versionControl) {
        super(versionControl);
    }

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
