/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.openbase.bco.registry.device.core.dbconvert.deviceclass;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.processing.StringProcessor;
import org.openbase.jul.storage.registry.version.DBVersionConverter;

/**
 *
 * @author <a href="mailto:thuxohl@techfak.uni-bielefeld.com">Tamino Huxohl</a>
 */
public class DeviceClass_0_To_1_DBConverter implements DBVersionConverter {

    private static final String BINDING_CONFIG_FIELD = "binding_config";
    private static final String TYPE_FIELD = "type";
    private static final String BINDING_ID_FIELD = "binding_id";
    private static final String UNIT_TEMPLATE_CONFIG_FIELD = "unit_template_config";
    private static final String SERVICE_TEMPLATE_FIELD = "service_template";
    private static final String SERVICE_TEMPLATE_CONFIG_FIELD = "service_template_config";
    private static final String SERVICE_TYPE_FIELD = "service_type";

    private final Map<String, String> unitTypeMap;
    private final Map<String, String> serviceTypeMap;

    public DeviceClass_0_To_1_DBConverter() {
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
    public JsonObject upgrade(JsonObject deviceClass, Map<File, JsonObject> dbSnapshot) throws CouldNotPerformException {
        // replace binding type with binding id
        JsonObject bindingConfig = deviceClass.getAsJsonObject(BINDING_CONFIG_FIELD);
        deviceClass.remove(BINDING_CONFIG_FIELD);
        String bindingId = StringProcessor.transformUpperCaseToCamelCase(bindingConfig.getAsJsonPrimitive(TYPE_FIELD).getAsString());
        bindingConfig.addProperty(BINDING_ID_FIELD, bindingId);
        deviceClass.add(BINDING_CONFIG_FIELD, bindingConfig);

        JsonArray unitTemplateConfigs = deviceClass.getAsJsonArray(UNIT_TEMPLATE_CONFIG_FIELD);
        for (JsonElement unitTemplateConfigElem : unitTemplateConfigs) {
            JsonObject unitTemplateConfig = unitTemplateConfigElem.getAsJsonObject();
            String newType = unitTypeMap.get(unitTemplateConfig.get(TYPE_FIELD).getAsString());
            unitTemplateConfig.remove(TYPE_FIELD);
            unitTemplateConfig.addProperty(TYPE_FIELD, newType);

            //TODO: also adjust if and label of unit template config or let the consistency handler handle this?
            JsonArray serviceTemplates = unitTemplateConfig.getAsJsonArray(SERVICE_TEMPLATE_FIELD);
            for (JsonElement serviceTemplateElem : serviceTemplates) {
                JsonObject serviceTemplate = serviceTemplateElem.getAsJsonObject();
                String newServiceType = serviceTypeMap.get(serviceTemplate.get(SERVICE_TYPE_FIELD).getAsString());
                serviceTemplate.remove(SERVICE_TYPE_FIELD);
                serviceTemplate.addProperty(SERVICE_TYPE_FIELD, newServiceType);
            }
            unitTemplateConfig.remove(SERVICE_TEMPLATE_FIELD);
            unitTemplateConfig.add(SERVICE_TEMPLATE_CONFIG_FIELD, serviceTemplates);

            deviceClass.remove(UNIT_TEMPLATE_CONFIG_FIELD);
            deviceClass.add(UNIT_TEMPLATE_CONFIG_FIELD, unitTemplateConfigs);
        }

        return deviceClass;
    }
}
