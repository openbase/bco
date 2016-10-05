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
public class UserConfig_0_To_1_DBConverter implements DBVersionConverter {

    private static final String TYPE_FIELD = "type";
    private static final String FIRST_NAME_FIELD = "first_name";
    private static final String LAST_NAME_FIELD = "last_name";
    private static final String USER_NAME_FIELD = "user_name";
    private static final String USER_CONFIG_FIELD = "user_config";

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
