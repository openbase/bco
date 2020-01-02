package org.openbase.bco.registry.activity.core.dbconvert;

/*-
 * #%L
 * BCO Registry Activity Core
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
 * With the registry simplification ActivityClass became ActivityTemplate.
 * Thus the id field in ActivityConfig (which is now ActivityConfig) has been renamed.
 * This converter updates the id field accordingly.
 *
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class ActivityConfig_0_To_1_DBConverter extends AbstractDBVersionConverter {

    public static final String USER_ACTIVITY_CLASS_ID_FIELD = "user_activity_class_id";
    public static final String ACTIVITY_TEMPLATE_ID_FIELD = "activity_template_id";

    /**
     * {@inheritDoc}
     *
     * @param versionControl {@inheritDoc}
     */
    public ActivityConfig_0_To_1_DBConverter(DBVersionControl versionControl) {
        super(versionControl);
    }

    /**
     * {@inheritDoc}
     *
     * @param outdatedDBEntry {@inheritDoc}
     * @param dbSnapshot      {@inheritDoc}
     * @return {@inheritDoc}
     * @throws CouldNotPerformException {@inheritDoc}
     */
    @Override
    public JsonObject upgrade(JsonObject outdatedDBEntry, Map<File, JsonObject> dbSnapshot) throws CouldNotPerformException {
        if (outdatedDBEntry.has(USER_ACTIVITY_CLASS_ID_FIELD)) {
            final String activityTemplateId = outdatedDBEntry.get(USER_ACTIVITY_CLASS_ID_FIELD).getAsString();
            outdatedDBEntry.remove(USER_ACTIVITY_CLASS_ID_FIELD);
            outdatedDBEntry.addProperty(ACTIVITY_TEMPLATE_ID_FIELD, activityTemplateId);
        }

        return outdatedDBEntry;
    }
}
