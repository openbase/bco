package org.openbase.bco.registry.template.core.dbconvert;

/*-
 * #%L
 * BCO Registry Template Core
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

import com.google.gson.JsonObject;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.storage.registry.version.AbstractDBVersionConverter;
import org.openbase.jul.storage.registry.version.DBVersionControl;

import java.io.File;
import java.util.Map;

/**
 * Converter that changes following service types:
 * <ul>
 *     <li>USER_ACTIVITY_STATE_SERVICE to ACTIVITY_MULTI_STATE_SERVICE</li>
 *     <li>USER_PRESENCE_STATE_SERVICE to USER_TRANSIT_STATE_SERVICE</li>
 * </ul>
 */
public class ServiceTemplate_0_To_1_DBConverter extends AbstractDBVersionConverter {

    public static final String TYPE_FIELD_NAME = "type";

    public static final String USER_ACTIVITY_STATE = "USER_ACTIVITY_STATE_SERVICE";
    public static final String ACTIVITY_MULTI_STATE = "ACTIVITY_MULTI_STATE_SERVICE";

    public static final String USER_PRESENCE_STATE = "USER_PRESENCE_STATE_SERVICE";
    public static final String USER_TRANSIT_STATE = "USER_TRANSIT_STATE_SERVICE";

    public ServiceTemplate_0_To_1_DBConverter(DBVersionControl versionControl) {
        super(versionControl);
    }

    @Override
    public JsonObject upgrade(JsonObject outdatedDBEntry, Map<File, JsonObject> dbSnapshot) throws CouldNotPerformException {
        if (outdatedDBEntry.has(TYPE_FIELD_NAME)) {
            String serviceType = outdatedDBEntry.get(TYPE_FIELD_NAME).getAsString();
            if (serviceType.equals(USER_ACTIVITY_STATE)) {
                outdatedDBEntry.addProperty(TYPE_FIELD_NAME, ACTIVITY_MULTI_STATE);
            }

            if(serviceType.equals(USER_PRESENCE_STATE)) {
                outdatedDBEntry.addProperty(TYPE_FIELD_NAME, USER_TRANSIT_STATE);
            }
        }

        return outdatedDBEntry;
    }
}
