package org.openbase.bco.registry.template.core.dbconvert;

/*-
 * #%L
 * BCO Registry Template Core
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
import com.google.gson.JsonObject;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.storage.registry.version.AbstractGlobalDBVersionConverter;
import org.openbase.jul.storage.registry.version.DBVersionControl;
import org.openbase.jul.storage.registry.version.DatabaseEntryDescriptor;

import java.io.File;
import java.util.Map;

/**
 *
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class UnitTemplate_3_To_4_DBConverter extends AbstractGlobalDBVersionConverter {

    private static final String SERVICE_TEMPLATE_FIELD = "service_template";
    private static final String SERVICE_DESCRIPTION_FIELD = "service_description";

    public UnitTemplate_3_To_4_DBConverter(DBVersionControl versionControl) {
        super(versionControl);
    }

    @Override
    public JsonObject upgrade(JsonObject outdatedDBEntry, Map<File, JsonObject> dbSnapshot, Map<String, Map<File, DatabaseEntryDescriptor>> globalDbSnapshots) throws CouldNotPerformException {
        if(outdatedDBEntry.has(SERVICE_TEMPLATE_FIELD)) {
            JsonArray serviceTemplate = outdatedDBEntry.getAsJsonArray(SERVICE_TEMPLATE_FIELD);
            outdatedDBEntry.remove(SERVICE_TEMPLATE_FIELD);
            outdatedDBEntry.add(SERVICE_DESCRIPTION_FIELD, serviceTemplate);
        }

        return outdatedDBEntry;
    }
}
