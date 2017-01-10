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
import com.google.gson.JsonParser;
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
public class UnitTemplate_0_To_1_DBConverter extends AbstractDBVersionConverter {

    private static final String PROVIDER_PATTERN = "PROVIDER";
    private static final String OPERATION_PATTERN = "OPERATION";

    private static final String ID_FIELD = "id";
    private static final String TYPE_FIELD = "type";
    private static final String INCLUDED_TYPE_FIELD = "included_type";
    private static final String SERVICE_TYPE_FIELD = "service_type";
    private static final String SERVICE_PATTERN_FIELD = "pattern";
    private static final String SERVICE_TEMPLATE_FIELD = "service_template";

    private final Map<String, String> unitTypeMap;
    private final Map<String, String> serviceTypeMap;

    public UnitTemplate_0_To_1_DBConverter(DBVersionControl versionControl) {
        super(versionControl);
        unitTypeMap = new HashMap<>();
        unitTypeMap.put("AGENT", "AGENT");
        unitTypeMap.put("AMBIENT_LIGHT", "COLORABLE_LIGHT");
        unitTypeMap.put("APP", "APP");
        unitTypeMap.put("AUDIO_SINK", "AUDIO_SINK");
        unitTypeMap.put("AUDIO_SOURCE", "AUDIO_SOURCE");
        unitTypeMap.put("BATTERY", "BATTERY");
        unitTypeMap.put("BRIGHTNESS_SENSOR", "BRIGHTNESS_SENSOR");
        unitTypeMap.put("BUTTON", "BUTTON");
        unitTypeMap.put("DEPTH_CAMERA", "VIDEO_DEPTH_SOURCE");
        unitTypeMap.put("DIMMER", "DIMMER");
        unitTypeMap.put("HANDLE_SENSOR", "HANDLE");
        unitTypeMap.put("LIGHT", "LIGHT");
        unitTypeMap.put("MOTION_SENSOR", "MOTION_DETECTOR");
        unitTypeMap.put("POWER_CONSUMPTION_SENSOR", "POWER_CONSUMPTION_SENSOR");
        unitTypeMap.put("POWER_PLUG", "POWER_SWITCH");
        unitTypeMap.put("REED_SWITCH", "REED_CONTACT");
        unitTypeMap.put("RGB_CAMERA", "VIDEO_RGB_SOURCE");
        unitTypeMap.put("ROLLERSHUTTER", "ROLLER_SHUTTER");
        unitTypeMap.put("SCENE", "SCENE");
        unitTypeMap.put("SCREEN", "MONITOR");
        unitTypeMap.put("SMOKE_DETECTOR", "SMOKE_DETECTOR");
        unitTypeMap.put("TAMPER_SWITCH", "TAMPER_DETECTOR");
        unitTypeMap.put("TELEVISION", "TELEVISION");
        unitTypeMap.put("TEMPERATURE_CONTROLLER", "TEMPERATURE_CONTROLLER");
        unitTypeMap.put("TEMPERATURE_SENSOR", "TEMPERATURE_SENSOR");

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
    public JsonObject upgrade(JsonObject unitTemplate, Map<File, JsonObject> dbSnapshot) throws CouldNotPerformException {
        String oldName = unitTemplate.get(TYPE_FIELD).getAsString();
        String newName = unitTypeMap.get(oldName);

        // every type from version 0 should be contained in the map
        // so this is for a debugging purpose
        if (!unitTypeMap.containsKey(oldName)) {
            throw new CouldNotPerformException("unitTypeMap does not contain type [" + oldName + "] and therefore cannot update unit template");
        }

        JsonArray includedTypes = unitTemplate.getAsJsonArray(INCLUDED_TYPE_FIELD);
        JsonArray serviceTypes = unitTemplate.getAsJsonArray(SERVICE_TYPE_FIELD);

        // remove all fields where the values have change
        unitTemplate.remove(ID_FIELD);
        unitTemplate.remove(TYPE_FIELD);
        unitTemplate.remove(INCLUDED_TYPE_FIELD);
        unitTemplate.remove(SERVICE_TYPE_FIELD);

        // id and type have the same values in unit templates
        unitTemplate.addProperty(ID_FIELD, newName);
        unitTemplate.addProperty(TYPE_FIELD, newName);

        // replace included types with the new names
        if (includedTypes != null) {
            JsonParser jsonParser = new JsonParser();
            for (int i = 0; i < includedTypes.size(); ++i) {
                includedTypes.set(i, jsonParser.parse(unitTypeMap.get(includedTypes.get(i).getAsString())));
            }
            unitTemplate.add(INCLUDED_TYPE_FIELD, includedTypes);
        }

        // replace service types with service templates
        if (serviceTypes != null) {
            JsonArray serviceTemplates = new JsonArray();
            for (JsonElement serviceType : serviceTypes) {
                String serviceTypeName = serviceType.getAsString();

                // delete service that does not exist anymore, e.g. multi service, opening ratio service
                if (!serviceTypeMap.containsKey(serviceTypeName)) {
                    continue;
                }

                // new name for the service type
                String newServiceTypeName = serviceTypeMap.get(serviceTypeName);

                // every unit from version 0 has a provider service
                JsonObject serviceTemplate = new JsonObject();
                serviceTemplate.addProperty(TYPE_FIELD, newServiceTypeName);
                serviceTemplate.addProperty(SERVICE_PATTERN_FIELD, PROVIDER_PATTERN);
                serviceTemplates.add(serviceTemplate);

                // only unit with a service type that ends on SERVICE have an operation service of that kind
                if (serviceTypeName.endsWith("SERVICE")) {
                    serviceTemplate = new JsonObject();
                    serviceTemplate.addProperty(TYPE_FIELD, newServiceTypeName);
                    serviceTemplate.addProperty(SERVICE_PATTERN_FIELD, OPERATION_PATTERN);
                    serviceTemplates.add(serviceTemplate);
                }
            }
            unitTemplate.add(SERVICE_TEMPLATE_FIELD, serviceTemplates);
        }

        return unitTemplate;
    }

}
