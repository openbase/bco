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
public class AuthorizationGroupConfig_0_To_1_DBConverter implements DBVersionConverter {

    private static final String TYPE_FIELD = "type";
    private static final String MEMBER_ID_FIELD = "member_id";
    private static final String AUTHORIZATION_GROUP_CONFIG_FIELD = "authorization_group_config";

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
