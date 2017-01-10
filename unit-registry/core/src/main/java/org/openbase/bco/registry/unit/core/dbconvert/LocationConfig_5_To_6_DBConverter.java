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
public class LocationConfig_5_To_6_DBConverter extends AbstractDBVersionConverter {

    private static final String TYPE_FIELD = "type";
    private static final String ROOT_FIELD = "root";
    private static final String CHILD_ID_FIELD = "child_id";
    private static final String UNIT_ID_FIELD = "unit_id";
    private static final String TILE_CONFIG_FIELD = "tile_config";
    private static final String LOCATION_CONFIG_FIELD = "location_config";

    public LocationConfig_5_To_6_DBConverter(DBVersionControl versionControl) {
        super(versionControl);
    }

    @Override
    public JsonObject upgrade(JsonObject unitConfig, final Map<File, JsonObject> dbSnapshot) {
        // move all fields special for locations into the location config
        JsonObject locationConfig = new JsonObject();
        if (unitConfig.has(TYPE_FIELD)) {
            locationConfig.add(TYPE_FIELD, unitConfig.get(TYPE_FIELD));
            unitConfig.remove(TYPE_FIELD);
        }
        if (unitConfig.has(ROOT_FIELD)) {
            locationConfig.add(ROOT_FIELD, unitConfig.get(ROOT_FIELD));
            unitConfig.remove(ROOT_FIELD);
        }
        if (unitConfig.has(CHILD_ID_FIELD)) {
            locationConfig.add(CHILD_ID_FIELD, unitConfig.get(CHILD_ID_FIELD));
            unitConfig.remove(CHILD_ID_FIELD);
        }
        if (unitConfig.has(UNIT_ID_FIELD)) {
            locationConfig.add(UNIT_ID_FIELD, unitConfig.get(UNIT_ID_FIELD));
            unitConfig.remove(UNIT_ID_FIELD);
        }
        if (unitConfig.has(TILE_CONFIG_FIELD)) {
            locationConfig.add(TILE_CONFIG_FIELD, unitConfig.get(TILE_CONFIG_FIELD));
            unitConfig.remove(TILE_CONFIG_FIELD);
        }
        unitConfig.add(LOCATION_CONFIG_FIELD, locationConfig);

        // add type
        unitConfig.addProperty(TYPE_FIELD, UnitType.LOCATION.name());

        return unitConfig;
    }

}
