/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.openbase.bco.registry.unit.core.dbconvert;

/*-
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
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.File;
import java.util.Map;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.storage.registry.version.AbstractGlobalDBVersionConverter;
import org.openbase.jul.storage.registry.version.DBVersionControl;
import org.openbase.jul.storage.registry.version.DatabaseEntryDescriptor;

/**
 *
 * @author pleminoq
 */
public class DalUnitConfig_0_To_1_DBConverter extends AbstractGlobalDBVersionConverter {

    private static final String DEVICE_CLASS_DB_ID = "device-class-db";
    private static final String UNIT_TEMPLATE_DB_ID = "unit-template-db";

    private static final String LIGHT_SENSOR_TYPE = "LIGHT_SENSOR";
    private static final String DIMMER_TYPE = "DIMMER";
    private static final String INTENSITY_STATE_SERVICE = "INTENSITY_STATE_SERVICE";
    private static final String BRIGHTNESS_STATE_SERVICE = "BRIGHTNESS_STATE_SERVICE";
    private static final String BRIGHTNESS_SENSOR_TYPE = "BRIGHTNESS_SENSOR";
    private static final String ILLUMINANCE_STATE_TYPE = "ILLUMINANCE_STATE_SERVICE";
    private static final String PROVIDER_PATTERN = "PROVIDER";

    private static final String TYPE_FIELD = "type";
    private static final String ID_FIELD = "id";
    private static final String PATTERN_FIELD = "pattern";
    private static final String SERVICE_TEMPLATE_FIELD = "service_template";
    private static final String SERVICE_CONFIG_FIELD = "service_config";
    private static final String UNIT_TEMPLATE_CONFIG_ID_FIELD = "unit_template_config_id";

    private boolean init;
    private int deviceClassDBVersion = 0;
    private int unitTemplateClassDBVersion = 0;
    private static final int LEAST_DEVICE_CLASS_VERSION = 2;
    private static final int LEAST_UNIT_TEMPLATE_VERSION = 3;

    public DalUnitConfig_0_To_1_DBConverter(DBVersionControl versionControl) {
        super(versionControl);
        init = false;
    }

    @Override
    public JsonObject upgrade(JsonObject outdatedDBEntry, Map<File, JsonObject> dbSnapshot, Map<String, Map<File, DatabaseEntryDescriptor>> globalDbSnapshots) throws CouldNotPerformException {
        if (deviceClassDBVersion < LEAST_DEVICE_CLASS_VERSION) {
            if (!globalDbSnapshots.get(DEVICE_CLASS_DB_ID).isEmpty()) {
                for (DatabaseEntryDescriptor entry : globalDbSnapshots.get(DEVICE_CLASS_DB_ID).values()) {
                    if (entry.getVersion() < LEAST_DEVICE_CLASS_VERSION) {
                        throw new CouldNotPerformException("Could not upgrade UnitTemplate DB to version 3! DeviceClass DB version 2 or newer is needed for this upgrade!");
                    }
                }
            }
            deviceClassDBVersion = LEAST_DEVICE_CLASS_VERSION;
        }

        if (unitTemplateClassDBVersion < LEAST_UNIT_TEMPLATE_VERSION) {
            if (!globalDbSnapshots.get(UNIT_TEMPLATE_DB_ID).isEmpty()) {
                for (DatabaseEntryDescriptor entry : globalDbSnapshots.get(UNIT_TEMPLATE_DB_ID).values()) {
                    if (entry.getVersion() < LEAST_UNIT_TEMPLATE_VERSION) {
                        throw new CouldNotPerformException("Could not upgrade DalUnit DB to version 1! UnitTemplate DB version 3 or newer is needed for this upgrade!");
                    }
                }
            }
            unitTemplateClassDBVersion = LEAST_UNIT_TEMPLATE_VERSION;
        }

        // Rename BRIGHTNESS_SENSOR into LIGHT_SENSOR
        // Replace BRIGHTNESS_SENSOR_TYPE by ILLUMINANCE_STATE_TYPE for all LIGHT_SENSOR_TYPE
        if (outdatedDBEntry.get(TYPE_FIELD).getAsString().equals(BRIGHTNESS_SENSOR_TYPE)) {
            outdatedDBEntry.remove(TYPE_FIELD);
            outdatedDBEntry.addProperty(TYPE_FIELD, LIGHT_SENSOR_TYPE);

            String unitTemplateConfigId = outdatedDBEntry.get(UNIT_TEMPLATE_CONFIG_ID_FIELD).getAsString();
            outdatedDBEntry.remove(UNIT_TEMPLATE_CONFIG_ID_FIELD);
            outdatedDBEntry.addProperty(UNIT_TEMPLATE_CONFIG_ID_FIELD, unitTemplateConfigId.replace(BRIGHTNESS_SENSOR_TYPE, LIGHT_SENSOR_TYPE));

            JsonObject serviceConfig = outdatedDBEntry.getAsJsonArray(SERVICE_CONFIG_FIELD).get(0).getAsJsonObject();
            JsonObject serviceTemplate2 = serviceConfig.getAsJsonObject(SERVICE_TEMPLATE_FIELD);
            serviceTemplate2.remove(TYPE_FIELD);
            serviceTemplate2.addProperty(TYPE_FIELD, ILLUMINANCE_STATE_TYPE);
            outdatedDBEntry = outdatedDBEntry;
        }

        // Replace INTENSITY_STATE_SERVICE by BRIGHTNESS_STATE_SERVICE for all DIMMER
        if (outdatedDBEntry.get(TYPE_FIELD).getAsString().equals(DIMMER_TYPE)) {
            for (JsonElement serviceConfig : outdatedDBEntry.getAsJsonArray(SERVICE_CONFIG_FIELD)) {
                JsonObject serviceTemplate2 = serviceConfig.getAsJsonObject().getAsJsonObject(SERVICE_TEMPLATE_FIELD);
                if (serviceTemplate2.get(TYPE_FIELD).getAsString().equals(INTENSITY_STATE_SERVICE)) {
                    serviceTemplate2.remove(TYPE_FIELD);
                    serviceTemplate2.addProperty(TYPE_FIELD, BRIGHTNESS_STATE_SERVICE);
                }
            }
        }
        return outdatedDBEntry;
    }
}
