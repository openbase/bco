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
import java.util.HashMap;
import java.util.Map;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.storage.registry.version.AbstractDBVersionConverter;
import org.openbase.jul.storage.registry.version.DBVersionControl;

/**
 *
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class SceneConfig_0_To_1_DBConverter extends AbstractDBVersionConverter {

    private static final String ACTION_CONFIG_FIELD = "action_config";
    private static final String SERVICE_HOLDER_FIELD = "service_holder";
    private static final String ACTION_STATE_FIELD = "action_state";
    private static final String SERVICE_TYPE_FIELD = "service_type";
    private static final String UNIT_ID_FIELD = "unit_id";

    private final Map<String, String> serviceTypeMap;

    public SceneConfig_0_To_1_DBConverter(DBVersionControl versionControl) {
        super(versionControl);
        serviceTypeMap = new HashMap<>();
        serviceTypeMap.put("BATTERY_PROVIDER", "BATTERY_STATE_SERVICE");
        serviceTypeMap.put("BRIGHTNESS_PROVIDER", "BRIGHTNESS_STATE_SERVICE");
        serviceTypeMap.put("BRIGHTNESS_SERVICE", "BRIGHTNESS_STATE_SERVICE");
        serviceTypeMap.put("DIM_SERVICE", "INTENSITY_STATE_SERVICE");
        serviceTypeMap.put("DIM_PROVIDER", "INTENSITY_STATE_SERVICE");
        serviceTypeMap.put("BUTTON_PROVIDER", "BUTTON_STATE_SERVICE");
        serviceTypeMap.put("COLOR_PROVIDER", "COLOR_STATE_SERVICE");
        serviceTypeMap.put("COLOR_SERVICE", "COLOR_STATE_SERVICE");
        serviceTypeMap.put("HANDLE_PROVIDER", "HANDLE_STATE_SERVICE");
        serviceTypeMap.put("MOTION_PROVIDER", "MOTION_STATE_SERVICE");
        serviceTypeMap.put("POWER_CONSUMPTION_PROVIDER", "POWER_CONSUMPTION_STATE_SERVICE");
        serviceTypeMap.put("POWER_PROVIDER", "POWER_STATE_SERVICE");
        serviceTypeMap.put("POWER_SERVICE", "POWER_STATE_SERVICE");
        serviceTypeMap.put("REED_SWITCH_PROVIDER", "CONTACT_STATE_SERVICE");
        serviceTypeMap.put("SHUTTER_PROVIDER", "BLIND_STATE_SERVICE");
        serviceTypeMap.put("SHUTTER_SERVICE", "BLIND_STATE_SERVICE");
        serviceTypeMap.put("TAMPER_PROVIDER", "TAMPER_STATE_SERVICE");
        serviceTypeMap.put("TARGET_TEMPERATURE_PROVIDER", "TARGET_TEMPERATURE_STATE_SERVICE");
        serviceTypeMap.put("TARGET_TEMPERATURE_SERVICE", "TARGET_TEMPERATURE_STATE_SERVICE");
        serviceTypeMap.put("TEMPERATURE_PROVIDER", "TEMPERATURE_STATE_SERVICE");
        serviceTypeMap.put("STANDBY_PROVIDER", "STANDBY_STATE_SERVICE");
        serviceTypeMap.put("STANDBY_SERVICE", "STANDBY_STATE_SERVICE");
        serviceTypeMap.put("SMOKE_STATE_PROVIDER", "SMOKE_STATE_SERVICE");
        serviceTypeMap.put("SMOKE_ALARM_STATE_PROVIDER", "SMOKE_ALARM_STATE_SERVICE");
        serviceTypeMap.put("TEMPERATURE_ALARM_STATE_PROVIDER", "TEMPERATURE_ALARM_STATE_SERVICE");
    }

    @Override
    public JsonObject upgrade(JsonObject sceneConfig, Map<File, JsonObject> dbSnapshot) throws CouldNotPerformException {
        JsonArray actionConfigs = sceneConfig.getAsJsonArray(ACTION_CONFIG_FIELD);
        for (JsonElement actionConfigElem : actionConfigs) {
            JsonObject actionConfig = actionConfigElem.getAsJsonObject();

            String newServiceType = serviceTypeMap.get(actionConfig.get(SERVICE_TYPE_FIELD).getAsString());
            actionConfig.remove(SERVICE_TYPE_FIELD);
            actionConfig.addProperty(SERVICE_TYPE_FIELD, newServiceType);

            String unitId = actionConfig.get(SERVICE_HOLDER_FIELD).getAsString();
            actionConfig.remove(SERVICE_HOLDER_FIELD);
            actionConfig.addProperty(UNIT_ID_FIELD, unitId);

            actionConfig.remove(ACTION_STATE_FIELD);
        }
        sceneConfig.remove(ACTION_CONFIG_FIELD);
        sceneConfig.add(ACTION_CONFIG_FIELD, actionConfigs);
        return sceneConfig;
    }

}
