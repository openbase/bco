package org.openbase.bco.registry.unit.core.dbconvert;

import com.google.gson.JsonObject;
import java.io.File;
import java.util.Map;
import org.openbase.jul.storage.registry.version.DBVersionConverter;
import rst.homeautomation.unit.UnitTemplateType.UnitTemplate.UnitType;

/**
 *
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class ConnectionConfig_0_To_1_DBConverter implements DBVersionConverter {

    private static final String TYPE_FIELD = "type";
    private static final String TILE_ID_FIELD = "tile_id";
    private static final String UNIT_ID_FIELD = "unit_id";
    private static final String CONNECTION_CONFIG_FIELD = "connection_config";

    @Override
    public JsonObject upgrade(JsonObject unitConfig, final Map<File, JsonObject> dbSnapshot) {
        // move all fields special for connections into the connection config
        JsonObject connectionConfig = new JsonObject();
        if (unitConfig.has(TYPE_FIELD)) {
            connectionConfig.add(TYPE_FIELD, unitConfig.get(TYPE_FIELD));
            unitConfig.remove(TYPE_FIELD);
        }
        if (unitConfig.has(TILE_ID_FIELD)) {
            connectionConfig.add(TILE_ID_FIELD, unitConfig.get(TILE_ID_FIELD));
            unitConfig.remove(TILE_ID_FIELD);
        }
        if (unitConfig.has(UNIT_ID_FIELD)) {
            connectionConfig.add(UNIT_ID_FIELD, unitConfig.get(UNIT_ID_FIELD));
            unitConfig.remove(UNIT_ID_FIELD);
        }
        unitConfig.add(CONNECTION_CONFIG_FIELD, connectionConfig);

        // add type
        unitConfig.addProperty(TYPE_FIELD, UnitType.CONNECTION.name());

        return unitConfig;
    }
}
