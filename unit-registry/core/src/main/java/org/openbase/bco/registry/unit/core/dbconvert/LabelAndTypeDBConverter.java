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
import org.openbase.bco.registry.lib.dbconvert.LabelDBConverter;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.storage.registry.version.DBVersionControl;

import java.io.File;
import java.util.Map;

/**
 * Converter which updates the old label string to the new label type and
 * changes the name of the type field in unitConfig to unitType and in serviceDescription
 * to serviceType.
 *
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class LabelAndTypeDBConverter extends LabelDBConverter {

    public static final String TYPE_KEY = "type";
    public static final String UNIT_TYPE_KEY = "unit_type";
    public static final String SERVICE_TYPE_KEY = "service_type";
    public static final String SERVICE_CONFIG_KEY = "service_type";
    public static final String SERVICE_DESCRIPTION_KEY = "service_description";

    public LabelAndTypeDBConverter(DBVersionControl versionControl) {
        super(versionControl);
    }

    @Override
    public JsonObject upgrade(JsonObject outdatedDBEntry, Map<File, JsonObject> dbSnapshot) throws CouldNotPerformException {
        // update label
        final JsonObject jsonObject = super.upgrade(outdatedDBEntry, dbSnapshot);

        // update unit type field
        if (jsonObject.has(TYPE_KEY)) {
            final String unitType = jsonObject.get(TYPE_KEY).getAsString();
            jsonObject.remove(TYPE_KEY);
            jsonObject.addProperty(UNIT_TYPE_KEY, unitType);
        }

        // updated service type field in every service config
        if (jsonObject.has(SERVICE_CONFIG_KEY)) {
            final JsonArray serviceConfigList = jsonObject.getAsJsonArray(SERVICE_CONFIG_KEY);

            for (final JsonElement serviceConfigElem : serviceConfigList) {
                final JsonObject serviceConfig = serviceConfigElem.getAsJsonObject();

                if(serviceConfig.has(SERVICE_DESCRIPTION_KEY)) {
                    final JsonObject serviceDescription = serviceConfig.getAsJsonObject(SERVICE_DESCRIPTION_KEY);

                    if(serviceDescription.has(TYPE_KEY)) {
                        final String serviceType = serviceDescription.get(TYPE_KEY).getAsString();
                        serviceDescription.remove(TYPE_KEY);
                        serviceDescription.addProperty(SERVICE_TYPE_KEY, serviceType);
                    }
                }
            }
        }

        return jsonObject;
    }
}
