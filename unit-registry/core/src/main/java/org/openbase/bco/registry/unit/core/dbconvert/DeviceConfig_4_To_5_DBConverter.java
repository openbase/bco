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

import com.google.gson.JsonObject;
import java.io.File;
import java.util.Map;
import org.openbase.jul.storage.registry.version.DBVersionConverter;
import rst.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;

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
