package org.openbase.bco.registry.unit.core.dbconvert;

/*-
 * #%L
 * BCO Registry Unit Core
 * %%
 * Copyright (C) 2014 - 2021 openbase.org
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

import com.google.gson.*;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.storage.registry.version.DBVersionControl;

import java.io.File;
import java.util.Map;

/**
 * Converter which updates scenes to the new label structure.
 * Additionally responsible actions and timestamps are cleared from serialized service attributes.
 * This is required because the action description changed and thus these service attributes can no longer
 * be de-serialized. These values should not have been serialized to begin with.
 *
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class SceneConfig_4_To_5_DBConverter extends LabelAndTypeDBConverter {

    private final String SCENE_CONFIG_FIELD = "scene_config";
    private final String REQUIRED_SERVICE_STATE_DESCRIPTION_FIELD = "required_service_state_description";
    private final String SERVICE_ATTRIBUTE_FIELD = "service_attribute";
    private final String TIMESTAMP_FIELD = "timestamp";
    private final String RESPONSIBLE_ACTION_FIELD = "responsible_action";

    private final JsonParser jsonParser;
    private final Gson gson;

    public SceneConfig_4_To_5_DBConverter(DBVersionControl versionControl) {
        super(versionControl);
        this.jsonParser = new JsonParser();
        this.gson = new Gson();
    }

    @Override
    public JsonObject upgrade(JsonObject outdatedDBEntry, Map<File, JsonObject> dbSnapshot) throws CouldNotPerformException {
        final JsonObject result = super.upgrade(outdatedDBEntry, dbSnapshot);

        if (!result.has(SCENE_CONFIG_FIELD)) {
            return result;
        }

        final JsonObject sceneConfig = result.get(SCENE_CONFIG_FIELD).getAsJsonObject();

        if (!sceneConfig.has(REQUIRED_SERVICE_STATE_DESCRIPTION_FIELD)) {
            return result;
        }

        final JsonArray serviceStateDescriptions = sceneConfig.get(REQUIRED_SERVICE_STATE_DESCRIPTION_FIELD).getAsJsonArray();
        for (final JsonElement serviceStateDescriptionElem : serviceStateDescriptions) {
            final JsonObject serviceStateDescription = serviceStateDescriptionElem.getAsJsonObject();

            if (!serviceStateDescription.has(SERVICE_ATTRIBUTE_FIELD)) {
                continue;
            }

            final String serviceAttributeString = serviceStateDescription.get(SERVICE_ATTRIBUTE_FIELD).getAsString();
            final JsonObject serviceAttribute = jsonParser.parse(serviceAttributeString).getAsJsonObject();

            if (serviceAttribute.has(TIMESTAMP_FIELD)) {
                serviceAttribute.remove(TIMESTAMP_FIELD);
            }

            if (serviceAttribute.has(RESPONSIBLE_ACTION_FIELD)) {
                serviceAttribute.remove(RESPONSIBLE_ACTION_FIELD);
            }

            serviceStateDescription.addProperty(SERVICE_ATTRIBUTE_FIELD, gson.toJson(serviceAttribute));
        }

        return result;
    }
}
