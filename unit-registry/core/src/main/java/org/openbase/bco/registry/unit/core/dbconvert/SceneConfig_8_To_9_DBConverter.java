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
import com.google.gson.stream.JsonReader;
import org.apache.commons.io.FileUtils;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.storage.registry.version.AbstractDBVersionConverter;
import org.openbase.jul.storage.registry.version.DBVersionControl;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.Map;

/**
 * Converter which updates scenes configurations in the database.
 *
 * Responsible actions are moved to optional actions during this update because the scene handling has been changed
 * since scenes are disabled if one required action fails. So we move it to the optional action list to preserve the old behaviour.
 *
 * Additionally, the following refactoring is applied:
 * * service_attribute_type to service_state_class_name
 * * service_attribute to service_state
 *
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class SceneConfig_8_To_9_DBConverter extends AbstractDBVersionConverter {

    private final String SCENE_CONFIG_FIELD = "scene_config";
    private final String REQUIRED_SERVICE_STATE_DESCRIPTION_FIELD = "required_service_state_description";
    private final String OPTIONAL_SERVICE_STATE_DESCRIPTION_FIELD = "optional_service_state_description";
    private final String SERVICE_STATE_FIELD = "service_state";

    public SceneConfig_8_To_9_DBConverter(DBVersionControl versionControl) {
        super(versionControl);
    }

    @Override
    public JsonObject upgrade(final JsonObject outdatedDBEntry, Map<File, JsonObject> dbSnapshot) throws CouldNotPerformException {
        final JsonObject result = outdatedDBEntry;

        if (!result.has(SCENE_CONFIG_FIELD)) {
            return result;
        }

        final JsonObject sceneConfig = result.get(SCENE_CONFIG_FIELD).getAsJsonObject();

        // copy from required to optional
        if (sceneConfig.has(REQUIRED_SERVICE_STATE_DESCRIPTION_FIELD)) {
            upgradeScale(sceneConfig.getAsJsonArray(REQUIRED_SERVICE_STATE_DESCRIPTION_FIELD));
        }

        if(sceneConfig.has(OPTIONAL_SERVICE_STATE_DESCRIPTION_FIELD)) {
            upgradeScale(sceneConfig.getAsJsonArray(OPTIONAL_SERVICE_STATE_DESCRIPTION_FIELD));
        }
        return result;
    }

    private void upgradeScale(final JsonArray serviceStateDescriptionListArray) throws CouldNotPerformException {

        // upgrade service states
        for (final JsonElement serviceStateDescriptionElement : serviceStateDescriptionListArray) {

            final JsonObject serviceStateDescription = serviceStateDescriptionElement.getAsJsonObject();
            final String serviceStateDescriptionString = serviceStateDescription.get(SERVICE_STATE_FIELD).getAsString();


            // deserialize serviceState
            final JsonObject serviceState;
            try {
                JsonReader jsonReader = new JsonReader(new StringReader(serviceStateDescriptionString));

                // needed to handle protobuf generated malformed json code.
                jsonReader.setLenient(true);

                serviceState = new JsonParser().parse(jsonReader).getAsJsonObject();
            } catch (JsonIOException | JsonSyntaxException | IllegalStateException ex) {
                throw new CouldNotPerformException("Could not deserialize service state from String[" + serviceStateDescriptionString + "]!", ex);
            }

            // upgrade color state if needed
            if (serviceState.has("color") && serviceState.get("color").getAsJsonObject().has("hsb_color")) {
                final JsonObject color = serviceState.getAsJsonObject("color").get("hsb_color").getAsJsonObject();
                color.addProperty("saturation", color.getAsJsonPrimitive("saturation").getAsDouble() / 100d);
                color.addProperty("brightness", color.getAsJsonPrimitive("brightness").getAsDouble() / 100d);
            }


            /*...*/

            // write back
            serviceStateDescription.addProperty(SERVICE_STATE_FIELD, serviceState.toString());
        }
    }
}
