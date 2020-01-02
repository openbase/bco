/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.openbase.bco.registry.clazz.core.dbconvert;

/*-
 * #%L
 * BCO Registry Class Core
 * %%
 * Copyright (C) 2014 - 2020 openbase.org
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
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.storage.registry.version.AbstractGlobalDBVersionConverter;
import org.openbase.jul.storage.registry.version.DBVersionControl;
import org.openbase.jul.storage.registry.version.DatabaseEntryDescriptor;

import java.io.File;
import java.util.Map;

/**
 *
 * @author pleminoq
 */
public class DeviceClass_2_To_3_DBConverter extends AbstractGlobalDBVersionConverter {

    private static final String UNIT_TEMPLATE_DB_ID = "unit-template-db";

    private static final String LIGHT_SENSOR_TYPE = "LIGHT_SENSOR";
    private static final String BRIGHTNESS_SENSOR_TYPE = "BRIGHTNESS_SENSOR";
    private static final String ILLUMINANCE_STATE_TYPE = "ILLUMINANCE_STATE_SERVICE";

    private static final String ID_FIELD = "id";
    private static final String LABEL_FIELD = "label";
    private static final String TYPE_FIELD = "type";
    private static final String SERVICE_TYPE_FIELD = "service_type";
    private static final String UNIT_TEMPLATE_CONFIG_FIELD = "unit_template_config";
    private static final String SERVICE_TEMPLATE_CONFIG_FIELD = "service_template_config";

    private int unitTemplateDBVersion = 0;
    private static final int LEAST_UNIT_TEMPLATE_DB_VERSION = 2;

    public DeviceClass_2_To_3_DBConverter(DBVersionControl versionControl) {
        super(versionControl);
    }

    @Override
    public JsonObject upgrade(JsonObject outdatedDBEntry, Map<File, JsonObject> dbSnapshot, Map<String, Map<File, DatabaseEntryDescriptor>> globalDbSnapshots) throws CouldNotPerformException {
        // Find all deviceClasses with unitTemplateConfig for unitType BRIGHTNESS_SENSOR and adjust their type and serviceTemplateConfig
        if (unitTemplateDBVersion < LEAST_UNIT_TEMPLATE_DB_VERSION) {
            if (!globalDbSnapshots.get(UNIT_TEMPLATE_DB_ID).isEmpty()) {
                for (DatabaseEntryDescriptor entry : globalDbSnapshots.get(UNIT_TEMPLATE_DB_ID).values()) {
                    if (entry.getVersion() < LEAST_UNIT_TEMPLATE_DB_VERSION) {
                        throw new CouldNotPerformException("Could not upgrade DeviceClass DB to version 3! UnitTemplate DB version 2 or newer is needed for this upgrade!");
                    }
                }
            }
            unitTemplateDBVersion = LEAST_UNIT_TEMPLATE_DB_VERSION;
        }

        JsonObject deviceClass = outdatedDBEntry;
        if (!deviceClass.has(UNIT_TEMPLATE_CONFIG_FIELD)) {
            return deviceClass;
        }

        for (JsonElement unitTemplateConfigElem : deviceClass.getAsJsonArray(UNIT_TEMPLATE_CONFIG_FIELD)) {
            JsonObject unitTemplateConfig = unitTemplateConfigElem.getAsJsonObject();
            if (unitTemplateConfig.get(TYPE_FIELD).getAsString().equals(BRIGHTNESS_SENSOR_TYPE)) {
                String unitTemplateConfigId = unitTemplateConfig.get(ID_FIELD).getAsString();
                unitTemplateConfig.remove(ID_FIELD);
                unitTemplateConfig.addProperty(ID_FIELD, unitTemplateConfigId.replace(BRIGHTNESS_SENSOR_TYPE, LIGHT_SENSOR_TYPE));

                String unitTemplateConfigLabel = unitTemplateConfig.get(LABEL_FIELD).getAsString();
                unitTemplateConfig.remove(LABEL_FIELD);
                unitTemplateConfig.addProperty(LABEL_FIELD, unitTemplateConfigLabel.replace(BRIGHTNESS_SENSOR_TYPE, LIGHT_SENSOR_TYPE));

                unitTemplateConfig.remove(TYPE_FIELD);
                unitTemplateConfig.addProperty(TYPE_FIELD, LIGHT_SENSOR_TYPE);

                JsonObject serviceTemplateConfig = unitTemplateConfig.getAsJsonArray(SERVICE_TEMPLATE_CONFIG_FIELD).get(0).getAsJsonObject();
                serviceTemplateConfig.remove(SERVICE_TYPE_FIELD);
                serviceTemplateConfig.addProperty(SERVICE_TYPE_FIELD, ILLUMINANCE_STATE_TYPE);
            }
        }

        return deviceClass;
    }

}
