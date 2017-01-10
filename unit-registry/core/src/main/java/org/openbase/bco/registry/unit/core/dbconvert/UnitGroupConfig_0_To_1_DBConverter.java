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
public class UnitGroupConfig_0_To_1_DBConverter extends AbstractDBVersionConverter {

    private static final String UNIT_TYPE_FIELD = "unit_type";
    private static final String SERVICE_TEMPLATE_FIELD = "service_template";
    private static final String SERVICE_TYPE_FIELD = "service_type";
    private static final String TYPE_FIELD = "type";
    private static final String PATTERN_FIELD = "pattern";
    private static final String META_CONFIG_FIELD = "meta_config";

    private static final String PROVIDER_PATTERN = "PROVIDER";
    private static final String OPERATION_PATTERN = "OPERATION";

    private final Map<String, String> unitTypeMap;
    private final Map<String, String> serviceTypeMap;

    public UnitGroupConfig_0_To_1_DBConverter(DBVersionControl versionControl) {
        super(versionControl);
        unitTypeMap = new HashMap<>();
        unitTypeMap.put("AMBIENT_LIGHT", "COLORABLE_LIGHT");
        unitTypeMap.put("LIGHT", "LIGHT");
        unitTypeMap.put("DIMMER", "DIMMER");
        unitTypeMap.put("POWER_PLUG", "POWER_SWITCH");
        unitTypeMap.put("TEMPERATURE_CONTROLLER", "TEMPERATURE_CONTROLLER");
        unitTypeMap.put("ROLLERSHUTTER", "ROLLER_SHUTTER");
        unitTypeMap.put("SCREEN", "MONITOR");
        unitTypeMap.put("TELEVISION", "TELEVISION");
        unitTypeMap.put("BUTTON", "BUTTON");
        unitTypeMap.put("REED_SWITCH", "REED_CONTACT");
        unitTypeMap.put("HANDLE_SENSOR", "HANDLE");
        unitTypeMap.put("BATTERY", "BATTERY");
        unitTypeMap.put("MOTION_SENSOR", "MOTION_DETECTOR");
        unitTypeMap.put("SMOKE_DETECTOR", "SMOKE_DETECTOR");
        unitTypeMap.put("TAMPER_SWITCH", "TAMPER_DETECTOR");
        unitTypeMap.put("POWER_CONSUMPTION_SENSOR", "POWER_CONSUMPTION_SENSOR");
        unitTypeMap.put("BRIGHTNESS_SENSOR", "BRIGHTNESS_SENSOR");
        unitTypeMap.put("TEMPERATURE_SENSOR", "TEMPERATURE_SENSOR");
        unitTypeMap.put("AUDIO_SOURCE", "AUDIO_SOURCE");
        unitTypeMap.put("AUDIO_SINK", "AUDIO_SINK");
        unitTypeMap.put("DEPTH_CAMERA", "VIDEO_DEPTH_SOURCE");
        unitTypeMap.put("RGB_CAMERA", "VIDEO_RGB_SOURCE");

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
    public JsonObject upgrade(JsonObject unitGroup, Map<File, JsonObject> dbSnapshot) throws CouldNotPerformException {
        String newUnitType = unitTypeMap.get(unitGroup.get(UNIT_TYPE_FIELD).getAsString());
        unitGroup.remove(UNIT_TYPE_FIELD);
        unitGroup.addProperty(UNIT_TYPE_FIELD, newUnitType);

        JsonArray serviceTemplates = unitGroup.getAsJsonArray(SERVICE_TEMPLATE_FIELD);
        if (serviceTemplates != null) {
            JsonArray newServiceTemplates = new JsonArray();
            for (JsonElement serviceTemplateElem : serviceTemplates) {
                JsonObject serviceTemplate = serviceTemplateElem.getAsJsonObject();

                String oldServiceType = serviceTemplate.get(SERVICE_TYPE_FIELD).getAsString();
                String newServiceType = serviceTypeMap.get(oldServiceType);
                serviceTemplate.remove(SERVICE_TYPE_FIELD);
                serviceTemplate.addProperty(TYPE_FIELD, newServiceType);
                serviceTemplate.addProperty(PATTERN_FIELD, PROVIDER_PATTERN);
                newServiceTemplates.add(serviceTemplate);

                if (oldServiceType.endsWith("SERVICE")) {
                    JsonObject operationServiceTemplate = new JsonObject();
                    operationServiceTemplate.addProperty(TYPE_FIELD, newServiceType);
                    operationServiceTemplate.addProperty(PATTERN_FIELD, OPERATION_PATTERN);
                    operationServiceTemplate.add(META_CONFIG_FIELD, serviceTemplate.get(META_CONFIG_FIELD));
                    newServiceTemplates.add(operationServiceTemplate);
                }
            }
            unitGroup.remove(SERVICE_TEMPLATE_FIELD);
            unitGroup.add(SERVICE_TEMPLATE_FIELD, newServiceTemplates);
        }

        return unitGroup;
    }
}
