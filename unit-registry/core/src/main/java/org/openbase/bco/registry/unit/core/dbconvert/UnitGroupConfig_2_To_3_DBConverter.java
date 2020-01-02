package org.openbase.bco.registry.unit.core.dbconvert;

/*-
 * #%L
 * BCO Registry Unit Core
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
import java.io.File;
import java.util.Map;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.storage.registry.version.DBVersionControl;
import org.openbase.jul.storage.registry.version.DatabaseEntryDescriptor;

/**
 *
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class UnitGroupConfig_2_To_3_DBConverter extends ServiceTemplateToServiceDescriptionDbConverter {

    private static final String UNIT_GROUP_CONFIG_FIELD = "unit_group_config";
    
    public UnitGroupConfig_2_To_3_DBConverter(DBVersionControl versionControl) {
        super(versionControl);
    }
    
    @Override
    public JsonObject upgrade(JsonObject outdatedDBEntry, Map<File, JsonObject> dbSnapshot, Map<String, Map<File, DatabaseEntryDescriptor>> globalDbSnapshots) throws CouldNotPerformException {
        if(outdatedDBEntry.has(UNIT_GROUP_CONFIG_FIELD)) {
            JsonObject unitGroupConfig = outdatedDBEntry.getAsJsonObject(UNIT_GROUP_CONFIG_FIELD);
            if(unitGroupConfig.has(SERVICE_TEMPLATE_FIELD)) {
                JsonElement serviceTemplate = unitGroupConfig.get(SERVICE_TEMPLATE_FIELD);
                unitGroupConfig.remove(SERVICE_TEMPLATE_FIELD);
                unitGroupConfig.add(SERVICE_DESCRIPTION_FIELD, serviceTemplate);
            }
        }
        
        return super.upgrade(outdatedDBEntry, dbSnapshot, globalDbSnapshots);
    }
    
}
