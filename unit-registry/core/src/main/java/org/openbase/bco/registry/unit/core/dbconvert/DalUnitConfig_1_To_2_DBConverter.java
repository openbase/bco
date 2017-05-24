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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.storage.registry.version.AbstractGlobalDBVersionConverter;
import org.openbase.jul.storage.registry.version.DBVersionControl;
import org.openbase.jul.storage.registry.version.DatabaseEntryDescriptor;

/**
 *
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class DalUnitConfig_1_To_2_DBConverter extends AbstractGlobalDBVersionConverter {

    private static final String AGENT_CONFIG_DB_ID = "agent-config-db";
    private static final String APP_CONFIG_DB_ID = "app-config-db";
    private static final String AUTHORIZATION_GROUP_CONFIG_DB_ID = "authorization-group-config-db";
    private static final String CONNECTION_CONFIG_DB_ID = "connection-config-db";
    private static final String DAL_UNIT_CONFIG_DB_ID = "dal-unit-config-db";
    private static final String DEVICE_CONFIG_DB_ID = "device-config-db";
    private static final String LOCATION_CONFIG_DB_ID = "location-config-db";
    private static final String SCENE_CONFIG_DB_ID = "scene-config-db";
    private static final String UNIT_GROUP_DB_ID = "unit-group-db";
    private static final String USER_CONFIG_DB_ID = "user-config-db";
    private static final String UNIT_TEMPLATE_DB_ID = "unit-template-db";

    private static final String SERVICE_CONFIG_FIELD = "service_config";
    private static final String SERVICE_TEMPLATE_FIELD = "service_template";
    private static final String SERVICE_DESCRIPTION_FIELD = "service_description";

    private boolean init;
    private int unitTemplateDBVersion = 0;
    private static final int LEAST_UNIT_TEMPLATE_VERSION = 4;
    private final List<String> unitConfigDbs;

    public DalUnitConfig_1_To_2_DBConverter(DBVersionControl versionControl) {
        super(versionControl);
        init = false;

        unitConfigDbs = new ArrayList<>();
        unitConfigDbs.add(AGENT_CONFIG_DB_ID);
        unitConfigDbs.add(APP_CONFIG_DB_ID);
        unitConfigDbs.add(AUTHORIZATION_GROUP_CONFIG_DB_ID);
        unitConfigDbs.add(CONNECTION_CONFIG_DB_ID);
        unitConfigDbs.add(DAL_UNIT_CONFIG_DB_ID);
        unitConfigDbs.add(DEVICE_CONFIG_DB_ID);
        unitConfigDbs.add(LOCATION_CONFIG_DB_ID);
        unitConfigDbs.add(SCENE_CONFIG_DB_ID);
        unitConfigDbs.add(UNIT_GROUP_DB_ID);
        unitConfigDbs.add(USER_CONFIG_DB_ID);
    }

    @Override
    public JsonObject upgrade(JsonObject outdatedDBEntry, Map<File, JsonObject> dbSnapshot, Map<String, Map<File, DatabaseEntryDescriptor>> globalDbSnapshots) throws CouldNotPerformException {
        // for every unit config (not just dal unit configs) iterate over all service configs and replace service template field as service description
        if (!init) {
            if (unitTemplateDBVersion < LEAST_UNIT_TEMPLATE_VERSION) {
                if (!globalDbSnapshots.get(UNIT_TEMPLATE_DB_ID).isEmpty()) {
                    for (DatabaseEntryDescriptor entry : globalDbSnapshots.get(UNIT_TEMPLATE_DB_ID).values()) {
                        if (entry.getVersion() < LEAST_UNIT_TEMPLATE_VERSION) {
                            throw new CouldNotPerformException("Could not upgrade UnitTemplate DB to version 3! DeviceClass DB version 2 or newer is needed for this upgrade!");
                        }
                    }
                }
                unitTemplateDBVersion = LEAST_UNIT_TEMPLATE_VERSION;
            }

            List<JsonObject> unitConfigs = new ArrayList<>();
            unitConfigDbs.stream().forEach((unitConfigDb) -> {
                globalDbSnapshots.get(unitConfigDb).values().stream().forEach((entry) -> {
                    unitConfigs.add(entry.getEntry());
                });
            });

            unitConfigs.stream().filter((unitConfig) -> (unitConfig.has(SERVICE_CONFIG_FIELD))).map((unitConfig) -> unitConfig.getAsJsonArray(SERVICE_CONFIG_FIELD)).forEach((serviceConfigs) -> {
                for (JsonElement serviceConfigElem : serviceConfigs) {
                    JsonObject serviceConfig = serviceConfigElem.getAsJsonObject();
                    if(serviceConfig.has(SERVICE_TEMPLATE_FIELD)) {
                        JsonObject serviceTemplate = serviceConfig.getAsJsonObject(SERVICE_TEMPLATE_FIELD);
                        serviceConfig.remove(SERVICE_TEMPLATE_FIELD);
                        serviceConfig.add(SERVICE_DESCRIPTION_FIELD, serviceTemplate);
                    }
                }
            });
        }

        init = true;

        return outdatedDBEntry;
    }
}
