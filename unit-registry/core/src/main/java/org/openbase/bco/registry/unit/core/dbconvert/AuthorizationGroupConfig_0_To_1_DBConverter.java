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
public class AuthorizationGroupConfig_0_To_1_DBConverter extends AbstractDBVersionConverter {

    private static final String TYPE_FIELD = "type";
    private static final String MEMBER_ID_FIELD = "member_id";
    private static final String AUTHORIZATION_GROUP_CONFIG_FIELD = "authorization_group_config";

    public AuthorizationGroupConfig_0_To_1_DBConverter(DBVersionControl versionControl) {
        super(versionControl);
    }

    @Override
    public JsonObject upgrade(JsonObject unitConfig, final Map<File, JsonObject> dbSnapshot) {
        // move all fields special for authorization groups into the authorizationGroupConfig
        JsonObject authorizationGroupConfig = new JsonObject();
        if (unitConfig.has(MEMBER_ID_FIELD)) {
            authorizationGroupConfig.add(MEMBER_ID_FIELD, unitConfig.get(MEMBER_ID_FIELD));
            unitConfig.remove(MEMBER_ID_FIELD);
        }
        unitConfig.add(AUTHORIZATION_GROUP_CONFIG_FIELD, authorizationGroupConfig);

        // add type
        unitConfig.addProperty(TYPE_FIELD, UnitType.AUTHORIZATION_GROUP.name());

        return unitConfig;
    }
}
