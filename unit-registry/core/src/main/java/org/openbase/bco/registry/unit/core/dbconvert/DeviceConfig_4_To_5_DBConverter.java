package org.openbase.bco.registry.unit.core.dbconvert;

/*
 * #%L
 * BCO Registry Unit Core
 * %%
 * Copyright (C) 2014 - 2016 openbase.org
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
import org.openbase.bco.registry.unit.lib.generator.UnitConfigIdGenerator;
import org.openbase.bco.registry.unit.lib.jp.JPDalUnitConfigDatabaseDirectory;
import org.openbase.jps.core.JPService;
import org.openbase.jps.exception.JPNotAvailableException;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InvalidStateException;
import org.openbase.jul.processing.StringProcessor;
import org.openbase.jul.storage.registry.version.GlobalDbVersionConverter;
import rst.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;

/**
 * DBConverter that converts deviceConfigs to the everything is a unit update, replaces the deviceClass IDs
 * with UUIDs and creates dalUnitConfigs.
 *
 *
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class DeviceConfig_4_To_5_DBConverter implements GlobalDbVersionConverter {

    private static final String TYPE_FIELD = "type";
    private static final String ID_FIELD = "id";
    private static final String SERIAL_NUMBER_FIELD = "serial_number";
    private static final String INVENTORY_STATE_FIELD = "inventory_state";
    private static final String DEVICE_CLASS_ID_FIELD = "device_class_id";
    private static final String UNIT_ID_FIELD = "unit_id";
    private static final String UNIT_CONFIG_FIELD = "unit_config";
    private static final String DEVICE_CONFIG_FIELD = "device_config";
    private static final String COMPANY_FIELD = "company";
    private static final String PRODUCT_NUMBER_FIELD = "product_number";

    private static final String SYSTEM_UNIT_ID_FIELD = "system_unit_id";
    private static final String BOUND_TO_SYSTEM_UNIT_FIELD = "bound_to_system_unit";
    private static final String UNIT_HOST_ID_FIELD = "unit_host_id";
    private static final String BOUND_TO_UNIT_HOST_FIELD = "bound_to_unit_host";

    private final UnitConfigIdGenerator idGenerator;
    private final Map<String, String> deviceClassIdMap;

    public DeviceConfig_4_To_5_DBConverter() {
        this.idGenerator = new UnitConfigIdGenerator();
        this.deviceClassIdMap = new HashMap<>();
    }

    @Override
    public JsonObject upgrade(JsonObject deviceUnitConfig, Map<File, JsonObject> dbSnapshot, Map<String, Map<File, JsonObject>> globalDbSnapshots) throws CouldNotPerformException {
        // replace the old id with a UUID
        deviceUnitConfig.remove(ID_FIELD);
        deviceUnitConfig.addProperty(ID_FIELD, idGenerator.generateId(null));

        // add type
        deviceUnitConfig.addProperty(TYPE_FIELD, UnitType.DEVICE.name());

        // add deviceConfig
        JsonObject deviceConfig = new JsonObject();
        // register all dal units from this device config and replace them with their ids in the device config
        if (deviceUnitConfig.has(UNIT_CONFIG_FIELD)) {
            JsonArray unitConfigs = deviceUnitConfig.getAsJsonArray(UNIT_CONFIG_FIELD);
            JsonArray unitIds = new JsonArray();
            for (JsonElement unitConfigElem : unitConfigs) {
                JsonObject unitConfig = unitConfigElem.getAsJsonObject();

                if (unitConfig.has(SYSTEM_UNIT_ID_FIELD)) {
                    unitConfig.remove(SYSTEM_UNIT_ID_FIELD);
                }
                unitConfig.add(UNIT_HOST_ID_FIELD, deviceUnitConfig.get(ID_FIELD));

                if (unitConfig.has(BOUND_TO_SYSTEM_UNIT_FIELD)) {
                    unitConfig.add(BOUND_TO_UNIT_HOST_FIELD, unitConfig.get(BOUND_TO_SYSTEM_UNIT_FIELD));
                    unitConfig.remove(BOUND_TO_SYSTEM_UNIT_FIELD);
                }

                try {
                    File dalUnitConfigDir = JPService.getProperty(JPDalUnitConfigDatabaseDirectory.class).getValue();
                    globalDbSnapshots.get("dal-unit-config").put(new File(dalUnitConfigDir, unitConfig.get(ID_FIELD).getAsString()), unitConfig);
                } catch (JPNotAvailableException ex) {
                    throw new CouldNotPerformException("Could not acces dal unit config database directory!", ex);
                }
                unitIds.add(unitConfig.get(ID_FIELD).getAsString());
            }

            deviceConfig.add(UNIT_ID_FIELD, unitIds);
            deviceUnitConfig.remove(UNIT_CONFIG_FIELD);
        }

        // move all fields special for devices into the deviceConfig
        if (deviceUnitConfig.has(SERIAL_NUMBER_FIELD)) {
            deviceConfig.add(SERIAL_NUMBER_FIELD, deviceUnitConfig.get(SERIAL_NUMBER_FIELD));
            deviceUnitConfig.remove(SERIAL_NUMBER_FIELD);
        }
        if (deviceUnitConfig.has(INVENTORY_STATE_FIELD)) {
            deviceConfig.add(INVENTORY_STATE_FIELD, deviceUnitConfig.get(INVENTORY_STATE_FIELD));
            deviceUnitConfig.remove(INVENTORY_STATE_FIELD);
        }

        //TODO:this step can only be executed if the ids in device classes have been updated which is the version update from 1 to 2
        // update the device class id to its new UUID and put it in the device config
        if (deviceClassIdMap.isEmpty()) {
            try {
                for (JsonObject deviceClass : globalDbSnapshots.get("device-class").values()) {
                    deviceClassIdMap.put(getOldDeviceClassId(deviceClass), deviceClass.get(ID_FIELD).getAsString());
                }
            } catch (CouldNotPerformException ex) {
                deviceClassIdMap.clear();
                throw new CouldNotPerformException("Could not build deviceClass id map!", ex);
            }
        }
        if (deviceUnitConfig.has(DEVICE_CLASS_ID_FIELD)) {
            deviceConfig.addProperty(DEVICE_CLASS_ID_FIELD, deviceClassIdMap.get(deviceUnitConfig.get(DEVICE_CLASS_ID_FIELD).getAsString()));
            deviceUnitConfig.remove(DEVICE_CLASS_ID_FIELD);
        }
        deviceUnitConfig.add(DEVICE_CONFIG_FIELD, deviceConfig);

        return deviceUnitConfig;
    }

    /**
     * Get the id of a deviceClass before the change to UUIDs.
     *
     * @param deviceClass the JSON Object of a deviceClass of which the id is generated
     * @return the old ID for the deviceClass
     * @throws CouldNotPerformException if some fields in the deviceClass which are needed are not set
     */
    private String getOldDeviceClassId(JsonObject deviceClass) throws CouldNotPerformException {
        String id;
        try {
            if (!deviceClass.has(PRODUCT_NUMBER_FIELD)) {
                throw new InvalidStateException("Field [ProductNumber] is missing!");
            }

            if (!deviceClass.has(COMPANY_FIELD)) {
                throw new InvalidStateException("Field [Company] is missing!");
            }

            id = deviceClass.get(COMPANY_FIELD).getAsString();
            id += "_";
            id += deviceClass.get(PRODUCT_NUMBER_FIELD).getAsString();

            return StringProcessor.transformToIdString(id);
        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not generate id!", ex);
        }
    }
}
