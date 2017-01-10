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
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.File;
import java.util.Map;
import org.openbase.jul.storage.registry.version.AbstractDBVersionConverter;
import org.openbase.jul.storage.registry.version.DBVersionControl;

/**
 *
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class LocationConfig_1_To_2_DBConverter extends AbstractDBVersionConverter {

    public LocationConfig_1_To_2_DBConverter(DBVersionControl versionControl) {
        super(versionControl);
    }

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
