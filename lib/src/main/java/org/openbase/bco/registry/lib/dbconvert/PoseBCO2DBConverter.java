package org.openbase.bco.registry.lib.dbconvert;

/*-
 * #%L
 * BCO Registry Lib
 * %%
 * Copyright (C) 2014 - 2020 openbase.org
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
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.storage.registry.version.AbstractDBVersionConverter;
import org.openbase.jul.storage.registry.version.DBVersionControl;

import java.io.File;
import java.util.Locale;
import java.util.Map;

/**
 * Converter which is able to convert json object from the position string
 * to the new pose data type.
 *
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public class PoseBCO2DBConverter extends AbstractDBVersionConverter {

    public static final String POSITION_KEY = "position";
    public static final String POSE_KEY = "pose";
    public static final String PLACEMENT_CONFIG_KEY = "placement_config";

    public PoseBCO2DBConverter(DBVersionControl versionControl) {
        super(versionControl);
    }

    @Override
    public JsonObject upgrade(JsonObject outdatedDBEntry, Map<File, JsonObject> dbSnapshot) throws CouldNotPerformException {
        return updatePose(outdatedDBEntry);
    }

    protected JsonObject updatePose(final JsonObject jsonObject) {

        // skip if no placement config exist.
        if (!jsonObject.has(PLACEMENT_CONFIG_KEY)) {
            return jsonObject;
        }

        final JsonObject placementConfigKey = jsonObject.get(PLACEMENT_CONFIG_KEY).getAsJsonObject();

        // skip if no position field exist.
        if (!placementConfigKey.has(POSITION_KEY)) {
            return jsonObject;
        }

        placementConfigKey.add(POSE_KEY, placementConfigKey.get(POSITION_KEY));
        placementConfigKey.remove(POSITION_KEY);
        return jsonObject;
    }
}
