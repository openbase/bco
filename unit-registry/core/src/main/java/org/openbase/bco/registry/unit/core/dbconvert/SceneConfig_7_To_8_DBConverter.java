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
import org.openbase.jul.storage.registry.version.AbstractDBVersionConverter;
import org.openbase.jul.storage.registry.version.DBVersionControl;

import java.io.File;
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
public class SceneConfig_7_To_8_DBConverter extends AbstractDBVersionConverter {

    private final String SCENE_CONFIG_FIELD = "scene_config";
    private final String REQUIRED_SERVICE_STATE_DESCRIPTION_FIELD = "required_service_state_description";
    private final String OPTIONAL_SERVICE_STATE_DESCRIPTION_FIELD = "optional_service_state_description";
    private final String SERVICE_ATTRIBUTE_TYPE_FIELD_OLD = "service_attribute_type";
    private final String SERVICE_STATE_CLASS_NAME_FIELD_NEW = "service_state_class_name";
    private final String SERVICE_ATTRIBUTE_FIELD_OLD = "service_attribute";
    private final String SERVICE_STATE_FIELD_NEW = "service_state";

    public SceneConfig_7_To_8_DBConverter(DBVersionControl versionControl) {
        super(versionControl);
    }

    @Override
    public JsonObject upgrade(final JsonObject outdatedDBEntry, Map<File, JsonObject> dbSnapshot) {
        final JsonObject result = outdatedDBEntry;

        if (!result.has(SCENE_CONFIG_FIELD)) {
            return result;
        }

        final JsonObject sceneConfig = result.get(SCENE_CONFIG_FIELD).getAsJsonObject();

        // copy from required to optional
        if (sceneConfig.has(REQUIRED_SERVICE_STATE_DESCRIPTION_FIELD)) {
            final JsonArray requiredServiceStateDescriptions = sceneConfig.get(REQUIRED_SERVICE_STATE_DESCRIPTION_FIELD).getAsJsonArray();
            final JsonArray optionalServiceStateDescriptions;

            // load or create new optional action list
            if(sceneConfig.has(OPTIONAL_SERVICE_STATE_DESCRIPTION_FIELD)) {
                optionalServiceStateDescriptions = sceneConfig.get(OPTIONAL_SERVICE_STATE_DESCRIPTION_FIELD).getAsJsonArray();
            } else {
                optionalServiceStateDescriptions = new JsonArray();
                sceneConfig.add(OPTIONAL_SERVICE_STATE_DESCRIPTION_FIELD, optionalServiceStateDescriptions);
            }

            // copy
            for (final JsonElement serviceStateDescriptionElement : requiredServiceStateDescriptions) {
                optionalServiceStateDescriptions.add(serviceStateDescriptionElement);
            }

            // clear required actions
            sceneConfig.remove(REQUIRED_SERVICE_STATE_DESCRIPTION_FIELD);
        }

        // apply refactoring
        // because required actions are emty after step 1. we only need to process the optional list
        if(sceneConfig.has(OPTIONAL_SERVICE_STATE_DESCRIPTION_FIELD)) {
            final JsonArray optionalServiceStateDescriptions;
            optionalServiceStateDescriptions = sceneConfig.get(OPTIONAL_SERVICE_STATE_DESCRIPTION_FIELD).getAsJsonArray();
            // refactor
            for (final JsonElement serviceStateDescriptionElement : optionalServiceStateDescriptions) {
                final JsonObject serviceStateDescription = serviceStateDescriptionElement.getAsJsonObject();
                serviceStateDescription.add(SERVICE_STATE_CLASS_NAME_FIELD_NEW, serviceStateDescription.remove(SERVICE_ATTRIBUTE_TYPE_FIELD_OLD));
                serviceStateDescription.add(SERVICE_STATE_FIELD_NEW, serviceStateDescription.remove(SERVICE_ATTRIBUTE_FIELD_OLD));
            }
        }
        return result;
    }
}
