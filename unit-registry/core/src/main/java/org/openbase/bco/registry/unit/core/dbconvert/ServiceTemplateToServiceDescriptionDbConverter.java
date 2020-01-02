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
import org.openbase.jul.storage.registry.version.AbstractGlobalDBVersionConverter;
import org.openbase.jul.storage.registry.version.DBVersionControl;
import org.openbase.jul.storage.registry.version.DatabaseEntryDescriptor;

/**
 *
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class ServiceTemplateToServiceDescriptionDbConverter extends AbstractGlobalDBVersionConverter {

    protected static final String SERVICE_CONFIG_FIELD = "service_config";
    protected static final String SERVICE_TEMPLATE_FIELD = "service_template";
    protected static final String SERVICE_DESCRIPTION_FIELD = "service_description";

    public ServiceTemplateToServiceDescriptionDbConverter(DBVersionControl versionControl) {
        super(versionControl);
    }

    @Override
    public JsonObject upgrade(JsonObject outdatedDBEntry, Map<File, JsonObject> dbSnapshot, Map<String, Map<File, DatabaseEntryDescriptor>> globalDbSnapshots) throws CouldNotPerformException {
        if (outdatedDBEntry.has(SERVICE_CONFIG_FIELD)) {
            for (JsonElement serviceConfigElem : outdatedDBEntry.getAsJsonArray(SERVICE_CONFIG_FIELD)) {
                JsonObject serviceConfig = serviceConfigElem.getAsJsonObject();
                if (serviceConfig.has(SERVICE_TEMPLATE_FIELD)) {
                    JsonObject serviceTemplate = serviceConfig.getAsJsonObject(SERVICE_TEMPLATE_FIELD);
                    serviceConfig.remove(SERVICE_TEMPLATE_FIELD);
                    serviceConfig.add(SERVICE_DESCRIPTION_FIELD, serviceTemplate);
                }
            }
        }

        return outdatedDBEntry;
    }
}
