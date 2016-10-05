package org.openbase.bco.registry.unit.core.dbconvert;

import com.google.gson.JsonObject;
import java.io.File;
import java.util.Map;
import org.openbase.jul.storage.registry.version.DBVersionConverter;
import rst.homeautomation.unit.UnitTemplateType.UnitTemplate.UnitType;

/**
 *
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class DeviceConfig_4_To_5_DBConverter implements DBVersionConverter {

    private static final String TYPE_FIELD = "type";
    private static final String SERIAL_NUMBER_FIELD = "serial_number";
    private static final String INVENTORY_STATE_FIELD = "inventory_state";
    private static final String DEVICE_CLASS_ID_FIELD = "device_class_id";
    private static final String UNIT_ID_FIELD = "unit_id";
    private static final String DEVICE_CONFIG_FIELD = "device_config";

    @Override
    public JsonObject upgrade(JsonObject unitConfig, final Map<File, JsonObject> dbSnapshot) {
        // move all fields special for devices into the deviceConfig
        JsonObject deviceConfig = new JsonObject();
        if (unitConfig.has(SERIAL_NUMBER_FIELD)) {
            deviceConfig.add(SERIAL_NUMBER_FIELD, unitConfig.get(SERIAL_NUMBER_FIELD));
            unitConfig.remove(SERIAL_NUMBER_FIELD);
        }
        if (unitConfig.has(INVENTORY_STATE_FIELD)) {
            deviceConfig.add(INVENTORY_STATE_FIELD, unitConfig.get(INVENTORY_STATE_FIELD));
            unitConfig.remove(INVENTORY_STATE_FIELD);
        }
        if (unitConfig.has(DEVICE_CLASS_ID_FIELD)) {
            deviceConfig.add(DEVICE_CLASS_ID_FIELD, unitConfig.get(DEVICE_CLASS_ID_FIELD));
            unitConfig.remove(DEVICE_CLASS_ID_FIELD);
        }
        if (unitConfig.has(UNIT_ID_FIELD)) {
            deviceConfig.add(UNIT_ID_FIELD, unitConfig.get(UNIT_ID_FIELD));
            unitConfig.remove(UNIT_ID_FIELD);
        }
        unitConfig.add(DEVICE_CONFIG_FIELD, deviceConfig);

        // add type
        unitConfig.addProperty(TYPE_FIELD, UnitType.DEVICE.name());

        return unitConfig;
    }
}
