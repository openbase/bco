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
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.io.File;
import java.util.Map;
import org.openbase.bco.registry.unit.lib.generator.UnitConfigIdGenerator;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.storage.registry.version.AbstractGlobalDBVersionConverter;
import org.openbase.jul.storage.registry.version.DBVersionControl;
import org.openbase.jul.storage.registry.version.DatabaseEntryDescriptor;

/**
 *
 * @author pleminoq
 */
public class UnitTemplate_2_To_3_DBConverter extends AbstractGlobalDBVersionConverter {

    private static final String DEVICE_CLASS_DB_ID = "device-class-db";
    private static final String DAL_UNIT_CONFIG_DB_ID = "dal-unit-config-db";

    private static final String LIGHT_SENSOR_TYPE = "LIGHT_SENSOR";
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
    private final UnitConfigIdGenerator idGenerator;
    private int deviceClassDBVersion = 0;
    private static final int LEAST_DEVICE_CLASS_VERSION = 2;

    public UnitTemplate_2_To_3_DBConverter(DBVersionControl versionControl) {
        super(versionControl);
        init = false;
        idGenerator = new UnitConfigIdGenerator();
    }

    @Override
    public JsonObject upgrade(JsonObject outdatedDBEntry, Map<File, JsonObject> dbSnapshot, Map<String, Map<File, DatabaseEntryDescriptor>> globalDbSnapshots) throws CouldNotPerformException {
        if (!init) {
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

            // Add a unitTemplate with unitType LIGHT_SENSOR and serviceTemplate ILLUMINANCE_STATE_SERVICE, PROVIDER
            JsonObject unitTemplate = new JsonObject();
            String id = idGenerator.generateId(null);
            unitTemplate.addProperty(ID_FIELD, id);
            unitTemplate.addProperty(TYPE_FIELD, LIGHT_SENSOR_TYPE);

            JsonArray serviceTemplates = new JsonArray();
            JsonObject serviceTemplate = new JsonObject();
            serviceTemplate.addProperty(TYPE_FIELD, ILLUMINANCE_STATE_TYPE);
            serviceTemplate.addProperty(PATTERN_FIELD, PROVIDER_PATTERN);
            serviceTemplates.add(serviceTemplate);
            unitTemplate.add(SERVICE_TEMPLATE_FIELD, serviceTemplates);
            init = true;
        }

        return outdatedDBEntry;
    }
}
