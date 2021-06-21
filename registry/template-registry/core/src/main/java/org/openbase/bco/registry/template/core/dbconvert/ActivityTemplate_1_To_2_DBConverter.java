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
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.openbase.jul.storage.registry.version.AbstractDBVersionConverter;
import org.openbase.jul.storage.registry.version.DBVersionControl;
import org.openbase.jul.storage.registry.version.GenericRenameFieldDBVersionConverter;

import java.io.File;
import java.util.Map;

/**
 * Converter which updates scenes configurations in the database.
 * <p>
 * Responsible actions are moved to optional actions during this update because the scene handling has been changed
 * since scenes are disabled if one required action fails. So we move it to the optional action list to preserve the old behaviour.
 * <p>
 * Additionally, the following refactoring is applied:
 * * service_attribute_type to service_state_class_name
 * * service_attribute to service_state
 *
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class ActivityTemplate_1_To_2_DBConverter extends GenericRenameFieldDBVersionConverter {

    public ActivityTemplate_1_To_2_DBConverter(final DBVersionControl versionControl) {
        super(versionControl, "activity_type", "type");
    }
}
