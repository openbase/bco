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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.openbase.jul.storage.registry.version.AbstractDBVersionConverter;
import org.openbase.jul.storage.registry.version.DBVersionControl;

/**
 *
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public class LocationConfig_0_To_1_DBConverter extends AbstractDBVersionConverter {

    public LocationConfig_0_To_1_DBConverter(DBVersionControl versionControl) {
        super(versionControl);
    }

    @Override
    public JsonObject upgrade(final JsonObject locationConfig, final Map<File, JsonObject> dbSnapshot) {

        // check if child element exists otherwise we are finish
        JsonElement childElement = locationConfig.get("child");
        if (childElement == null) {
            return locationConfig;
        }

        // recover child ids
        JsonArray childConfigs = childElement.getAsJsonArray();
        List<String> childList = new ArrayList<>();
        for (JsonElement childConfigElement : childConfigs) {
            childList.add(childConfigElement.getAsJsonObject().get("id").getAsString());
        }
        // remove outdated child collection
        locationConfig.remove("child");

        // add new entries
        JsonArray child_id_array = new JsonArray();
        for (String child_id : childList) {
            child_id_array.add(child_id);
        }
        locationConfig.add("child_id", child_id_array);

        return locationConfig;
    }
}
