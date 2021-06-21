package org.openbase.bco.registry.unit.core.dbconvert;

/*-
 * #%L
 * BCO Registry Unit Core
 * %%
 * Copyright (C) 2014 - 2021 openbase.org
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
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.storage.registry.version.DBVersionControl;

import java.io.File;
import java.util.Map;

/**
 * Converter which updates unit groups to the new label structure.
 *
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class UnitGroupConfig_3_To_4_DBConverter extends LabelAndTypeDBConverter {


    public static final String TYPE_KEY = "type";
    public static final String SERVICE_TYPE_KEY = "service_type";
    public static final String SERVICE_DESCRIPTION_KEY = "service_description";

    public UnitGroupConfig_3_To_4_DBConverter(DBVersionControl versionControl) {
        super(versionControl);
    }

    @Override
    public JsonObject upgrade(JsonObject outdatedDBEntry, Map<File, JsonObject> dbSnapshot) throws CouldNotPerformException {

        // convert labels
        final JsonObject jsonObject = super.upgrade(outdatedDBEntry, dbSnapshot);
        final JsonObject unitGroupConfig = jsonObject.getAsJsonObject("unit_group_config");

        // updated service_descriptions
        if (unitGroupConfig.has(SERVICE_DESCRIPTION_KEY)) {
            final JsonArray serviceDescriptionList = unitGroupConfig.getAsJsonArray(SERVICE_DESCRIPTION_KEY);

            for (final JsonElement arrayEntry : serviceDescriptionList) {
                final JsonObject serviceDescription = arrayEntry.getAsJsonObject();

                if (serviceDescription.has(TYPE_KEY)) {
                    final String serviceType = serviceDescription.get(TYPE_KEY).getAsString();
                    serviceDescription.remove(TYPE_KEY);
                    serviceDescription.addProperty(SERVICE_TYPE_KEY, serviceType);
                }
            }
        }

        return jsonObject;
    }
}
