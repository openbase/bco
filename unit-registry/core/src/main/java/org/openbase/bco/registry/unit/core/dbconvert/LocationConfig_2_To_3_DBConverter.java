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
import com.google.gson.JsonPrimitive;
import java.io.File;
import java.util.Map;
import java.util.UUID;
import org.openbase.jul.storage.registry.version.AbstractDBVersionConverter;
import org.openbase.jul.storage.registry.version.DBVersionControl;

/**
 *
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class LocationConfig_2_To_3_DBConverter extends AbstractDBVersionConverter {

    public LocationConfig_2_To_3_DBConverter(DBVersionControl versionControl) {
        super(versionControl);
    }

    @Override
    public JsonObject upgrade(JsonObject locationConfig, final Map<File, JsonObject> dbSnapshot) {
        String oldId = locationConfig.getAsJsonPrimitive("id").getAsString();
        String newId = UUID.randomUUID().toString();

        locationConfig.remove("id");
        locationConfig.add("id", new JsonPrimitive(newId));

        for (JsonObject location : dbSnapshot.values()) {
            if (location.getAsJsonPrimitive("id").getAsString().equals(oldId)) {
                // set new id here?
                continue;
            }

            // change parent_id in all location if needed to the new id
            if (location.getAsJsonPrimitive("parent_id") != null && location.getAsJsonPrimitive("parent_id").getAsString().equals(oldId)) {
                location.remove("parent_id");
                location.add("parent_id", new JsonPrimitive(newId));
            }

            // adjust the child ids if needed
            if (location.getAsJsonArray("child_id") == null) {
                continue;
            }
            JsonArray childIdArray = new JsonArray();
            for (JsonElement childId : location.getAsJsonArray("child_id")) {
                if (childId.getAsJsonPrimitive().getAsString().equals(oldId)) {
                    childIdArray.add(newId);
                } else {
                    childIdArray.add(childId.getAsJsonPrimitive().getAsString());
                }
            }
            location.remove("child_id");
            location.add("child_id", childIdArray);
        }

        return locationConfig;
    }
}
