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
import java.io.File;
import java.util.Map;
import org.openbase.jul.storage.registry.version.AbstractDBVersionConverter;
import org.openbase.jul.storage.registry.version.DBVersionControl;
import rst.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;

/**
 *
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class SceneConfig_1_To_2_DBConverter extends AbstractDBVersionConverter {

    private static final String TYPE_FIELD = "type";
    private static final String ACTION_CONFIG_FIELD = "action_config";
    private static final String SCENE_CONFIG_FIELD = "scene_config";
    private static final String LOCATION_ID_FIELD = "location_id";
    private static final String PLACEMENT_CONFIG_FIELD = "placement_config";

    public SceneConfig_1_To_2_DBConverter(DBVersionControl versionControl) {
        super(versionControl);
    }

    @Override
    public JsonObject upgrade(JsonObject unitConfig, final Map<File, JsonObject> dbSnapshot) {
        // add type
        unitConfig.addProperty(TYPE_FIELD, UnitType.SCENE.name());

        // move action configs into scene config
        if (unitConfig.has(ACTION_CONFIG_FIELD)) {
            JsonObject sceneConfig = new JsonObject();
            sceneConfig.add(ACTION_CONFIG_FIELD, unitConfig.get(ACTION_CONFIG_FIELD));
            unitConfig.remove(ACTION_CONFIG_FIELD);
            unitConfig.add(SCENE_CONFIG_FIELD, sceneConfig);
        }

        // move location id to placement config
        if (unitConfig.has(LOCATION_ID_FIELD)) {
            JsonObject placementConfig = new JsonObject();
            placementConfig.addProperty(LOCATION_ID_FIELD, unitConfig.get(LOCATION_ID_FIELD).getAsString());
            unitConfig.remove(LOCATION_ID_FIELD);
            unitConfig.add(PLACEMENT_CONFIG_FIELD, placementConfig);
        }

        return unitConfig;
    }

}
