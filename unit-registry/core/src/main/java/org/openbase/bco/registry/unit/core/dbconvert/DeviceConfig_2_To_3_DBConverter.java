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
import com.google.gson.JsonPrimitive;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import org.openbase.bco.registry.unit.lib.generator.UnitConfigIdGenerator;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.storage.registry.version.AbstractDBVersionConverter;
import org.openbase.jul.storage.registry.version.DBVersionControl;

/**
 *
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class DeviceConfig_2_To_3_DBConverter extends AbstractDBVersionConverter {

    private static final String ID_FIELD = "id";
    private static final String UNIT_ID_FIELD = "unit_id";
    private static final String UNIT_CONFIG_FIELD = "unit_config";
    private static final String TYPE_FIELD = "type";
    private static final String PATTERN_FIELD = "pattern";
    private static final String UNIT_TEMPLATE_CONFIG_ID_FIELD = "unit_template_config_id";
    private static final String SERVICE_CONFIG_FIELD = "service_config";
    private static final String BINDING_CONFIG_FIELD = "binding_config";
    private static final String BINDING_SERVICE_CONFIG_FIELD = "binding_service_config";
    private static final String BINDING_ID_FIELD = "binding_id";
    private static final String BOUND_TO_SYSTEM_UNIT_FIELD = "bound_to_system_unit";
    private static final String BOUND_TO_DEVICE_FIELD = "bound_to_device";
    private static final String SERVICE_TEMPLATE_FIELD = "service_template";

    private final Map<String, String> unitTypeMap;
    private final Map<String, String> serviceTypeMap;

    private final UnitConfigIdGenerator idGenerator;

    public DeviceConfig_2_To_3_DBConverter(DBVersionControl versionControl) {
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

        idGenerator = new UnitConfigIdGenerator();
    }

    @Override
    public JsonObject upgrade(JsonObject deviceConfig, Map<File, JsonObject> dbSnapshot) throws CouldNotPerformException {
        JsonArray unitConfigs = deviceConfig.getAsJsonArray(UNIT_CONFIG_FIELD);
        if (unitConfigs != null) {
            JsonArray newUnitConfigs = new JsonArray();
            for (JsonElement unitConfigElem : unitConfigs) {
                JsonObject unitConfig = unitConfigElem.getAsJsonObject();

                String newId = idGenerator.generateId(null);
                if (unitConfig.has(ID_FIELD)) {
                    unitConfig.remove(ID_FIELD);
                    unitConfig.addProperty(ID_FIELD, newId);
                }

                String oldType = unitConfig.get(TYPE_FIELD).getAsString();
                String newType = unitTypeMap.get(oldType);
                unitConfig.remove(TYPE_FIELD);
                unitConfig.addProperty(TYPE_FIELD, newType);

                String unitTemplateConfigId = unitConfig.get(UNIT_TEMPLATE_CONFIG_ID_FIELD).getAsString().replace(oldType, newType);
                unitConfig.remove(UNIT_TEMPLATE_CONFIG_ID_FIELD);
                unitConfig.addProperty(UNIT_TEMPLATE_CONFIG_ID_FIELD, unitTemplateConfigId);

                boolean boundToSystemUnit = unitConfig.get(BOUND_TO_DEVICE_FIELD).getAsBoolean();
                unitConfig.remove(BOUND_TO_DEVICE_FIELD);
                unitConfig.addProperty(BOUND_TO_SYSTEM_UNIT_FIELD, boundToSystemUnit);

                JsonArray serviceConfigs = unitConfig.getAsJsonArray(SERVICE_CONFIG_FIELD);
                if (serviceConfigs != null) {
                    JsonArray newServiceConfigs = new JsonArray();
                    for (JsonElement serviceConfigElem : serviceConfigs) {
                        JsonObject serviceConfig = serviceConfigElem.getAsJsonObject();

                        if (serviceConfig.has(UNIT_ID_FIELD)) {
                            serviceConfig.remove(UNIT_ID_FIELD);
                            serviceConfig.addProperty(UNIT_ID_FIELD, newId);
                        }

                        JsonObject bindingConfig = serviceConfig.getAsJsonObject(BINDING_SERVICE_CONFIG_FIELD);
                        if (bindingConfig != null) {
                            JsonPrimitive bindingType = bindingConfig.getAsJsonPrimitive(TYPE_FIELD);
                            if (bindingType != null) {
                                String bindingId = bindingType.getAsString();
                                bindingConfig.remove(TYPE_FIELD);
                                bindingConfig.addProperty(BINDING_ID_FIELD, bindingId);
                            }
                            serviceConfig.remove(BINDING_SERVICE_CONFIG_FIELD);
                            serviceConfig.add(BINDING_CONFIG_FIELD, bindingConfig);
                        }

                        String oldServiceType = serviceConfig.get(TYPE_FIELD).getAsString();
                        String serviceType = serviceTypeMap.get(oldServiceType);
                        if (serviceType == null) {
                            continue;
                        }
                        serviceConfig.remove(TYPE_FIELD);
                        JsonObject serviceTemplate = new JsonObject();
                        serviceTemplate.addProperty(TYPE_FIELD, serviceType);
                        serviceTemplate.addProperty(PATTERN_FIELD, "PROVIDER");
                        serviceConfig.add(SERVICE_TEMPLATE_FIELD, serviceTemplate);
                        newServiceConfigs.add(serviceConfig);

                        if (oldServiceType.endsWith("SERVICE")) {
                            JsonObject operationServiceConfig = new JsonObject();
                            serviceConfig.entrySet().stream().forEach((entry) -> {
                                operationServiceConfig.add(entry.getKey(), entry.getValue());
                            });
                            JsonObject operationServiceTemplate = new JsonObject();
                            operationServiceTemplate.addProperty(TYPE_FIELD, serviceType);
                            operationServiceTemplate.addProperty(PATTERN_FIELD, "OPERATION");
                            operationServiceConfig.remove(SERVICE_TEMPLATE_FIELD);
                            operationServiceConfig.add(SERVICE_TEMPLATE_FIELD, operationServiceTemplate);
                            newServiceConfigs.add(operationServiceConfig);
                        }
                    }
                    unitConfig.remove(SERVICE_CONFIG_FIELD);
                    unitConfig.add(SERVICE_CONFIG_FIELD, newServiceConfigs);
                }
                newUnitConfigs.add(unitConfig);
            }

            deviceConfig.remove(UNIT_CONFIG_FIELD);
            deviceConfig.add(UNIT_CONFIG_FIELD, newUnitConfigs);
        }

        return deviceConfig;
    }

}
