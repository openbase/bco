package org.openbase.bco.registry.unit.core.dbconvert;

/*
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
import com.google.gson.JsonObject;
import java.io.File;
import java.util.Map;
import org.openbase.jul.storage.registry.version.AbstractDBVersionConverter;
import org.openbase.jul.storage.registry.version.DBVersionControl;
import rst.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;

/**
 *
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class UserConfig_0_To_1_DBConverter extends AbstractDBVersionConverter {

    private static final String TYPE_FIELD = "type";
    private static final String FIRST_NAME_FIELD = "first_name";
    private static final String LAST_NAME_FIELD = "last_name";
    private static final String USER_NAME_FIELD = "user_name";
    private static final String USER_CONFIG_FIELD = "user_config";

    public UserConfig_0_To_1_DBConverter(DBVersionControl versionControl) {
        super(versionControl);
    }

    @Override
    public JsonObject upgrade(JsonObject unitConfig, final Map<File, JsonObject> dbSnapshot) {
        // move all fields special for users into the userConfig
        JsonObject userConfig = new JsonObject();
        if (unitConfig.has(FIRST_NAME_FIELD)) {
            userConfig.add(FIRST_NAME_FIELD, unitConfig.get(FIRST_NAME_FIELD));
            unitConfig.remove(FIRST_NAME_FIELD);
        }
        if (unitConfig.has(LAST_NAME_FIELD)) {
            userConfig.add(LAST_NAME_FIELD, unitConfig.get(LAST_NAME_FIELD));
            unitConfig.remove(LAST_NAME_FIELD);
        }
        if (unitConfig.has(USER_NAME_FIELD)) {
            userConfig.add(USER_NAME_FIELD, unitConfig.get(USER_NAME_FIELD));
            unitConfig.remove(USER_NAME_FIELD);
        }
        unitConfig.add(USER_CONFIG_FIELD, userConfig);

        // add type
        unitConfig.addProperty(TYPE_FIELD, UnitType.USER.name());

        return unitConfig;
    }
}
