package org.openbase.bco.registry.unit.core.dbconvert;

/*-
 * #%L
 * BCO Registry Unit Core
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
import java.io.File;
import java.util.Map;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.storage.registry.version.AbstractDBVersionConverter;
import org.openbase.jul.storage.registry.version.DBVersionControl;

/**
 *
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class SceneConfig_3_To_4_DBConverter extends AbstractDBVersionConverter {

    private final String SCENE_CONFIG_FIELD = "scene_config";
    private final String ACTION_CONFIG_FIELD = "action_config";
    private final String REQUIRED_SERVICE_STATE_DESCRIPTION_FIELD = "required_service_state_description";

    private final String UNIT_ID_FIELD = "unit_id";
    private final String SERVICE_TYPE_FIELD = "service_type";
    private final String UNIT_TYPE_FIELD = "unit_type";
    private final String SERVICE_ATTRIBUTE_TYPE_FIELD = "service_attribute_type";
    private final String SERVICE_ATTRIBUTE_TYPE = "service_attribute";

    public SceneConfig_3_To_4_DBConverter(DBVersionControl versionControl) {
        super(versionControl);
    }
    
    @Override
    public JsonObject upgrade(JsonObject sceneUnitConfig, Map<File, JsonObject> dbSnapshot) throws CouldNotPerformException {
        JsonObject sceneConfig = sceneUnitConfig.getAsJsonObject(SCENE_CONFIG_FIELD);

        JsonArray serviceStateDescriptions = new JsonArray();
        JsonArray actionConfigsJsonArray = sceneConfig.getAsJsonArray(ACTION_CONFIG_FIELD);
        
        if(actionConfigsJsonArray == null)  {
            return sceneUnitConfig;
        }
        
        for (JsonElement actionConfigElem : actionConfigsJsonArray) {
            JsonObject actionConfig = actionConfigElem.getAsJsonObject();
            JsonObject serviceStateDescription = new JsonObject();

            if (actionConfig.has(UNIT_ID_FIELD)) {
                serviceStateDescription.addProperty(UNIT_ID_FIELD, actionConfig.get(UNIT_ID_FIELD).getAsString());
            }
            if (actionConfig.has(SERVICE_TYPE_FIELD)) {
                serviceStateDescription.addProperty(SERVICE_TYPE_FIELD, actionConfig.get(SERVICE_TYPE_FIELD).getAsString());
            }
            if (actionConfig.has(SERVICE_ATTRIBUTE_TYPE_FIELD)) {
                serviceStateDescription.addProperty(SERVICE_ATTRIBUTE_TYPE_FIELD, actionConfig.get(SERVICE_ATTRIBUTE_TYPE_FIELD).getAsString());
            }
            if (actionConfig.has(SERVICE_ATTRIBUTE_TYPE)) {
                serviceStateDescription.addProperty(SERVICE_ATTRIBUTE_TYPE, actionConfig.get(SERVICE_ATTRIBUTE_TYPE).getAsString());
            }

            serviceStateDescription.addProperty(UNIT_TYPE_FIELD, "UNKNOWN");
            serviceStateDescriptions.add(serviceStateDescription);
        }
        sceneConfig.remove(ACTION_CONFIG_FIELD);
        sceneConfig.add(REQUIRED_SERVICE_STATE_DESCRIPTION_FIELD, serviceStateDescriptions);

        return sceneUnitConfig;
    }
}
