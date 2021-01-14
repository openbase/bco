package org.openbase.bco.registry.clazz.core.dbconvert;

/*-
 * #%L
 * BCO Registry Class Core
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
 * Converter for device classes which updates them to the new label structure.
 * This also updates all internal unit template configs.
 *
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class DeviceClass_3_To_4_DBConverter extends LabelDBConverter {

    public static final String UNIT_TEMPLATE_CONFIG_KEY = "unit_template_config";

    public DeviceClass_3_To_4_DBConverter(DBVersionControl versionControl) {
        super(versionControl);
    }

    @Override
    public JsonObject upgrade(JsonObject outdatedDBEntry, Map<File, JsonObject> dbSnapshot) throws CouldNotPerformException {
        final JsonObject jsonObject = super.upgrade(outdatedDBEntry, dbSnapshot);

        if(jsonObject.has(UNIT_TEMPLATE_CONFIG_KEY)) {
            JsonArray unitTemplateConfigList = jsonObject.getAsJsonArray(UNIT_TEMPLATE_CONFIG_KEY);

            for(final JsonElement unitTemplateConfigElem : unitTemplateConfigList) {
                updateLabel(unitTemplateConfigElem.getAsJsonObject());
            }
        }

        return jsonObject;
    }
}
