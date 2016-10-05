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
public class UnitGroupConfig_1_To_2_DBConverter implements DBVersionConverter {

    private static final String TYPE_FIELD = "type";
    private static final String MEMBER_ID_FIELD = "member_id";
    private static final String UNIT_TYPE_FIELD = "unit_type";
    private static final String SERVICE_TEMPLATE_FIELD = "service_template";
    private static final String UNIT_GROUP_CONFIG_FIELD = "unit_group_config";

    @Override
    public JsonObject upgrade(JsonObject unitConfig, final Map<File, JsonObject> dbSnapshot) {
        // move all fields special for unit groups into the unitGroupConfig
        JsonObject unitGroupConfig = new JsonObject();
        if (unitConfig.has(MEMBER_ID_FIELD)) {
            unitGroupConfig.add(MEMBER_ID_FIELD, unitConfig.get(MEMBER_ID_FIELD));
            unitConfig.remove(MEMBER_ID_FIELD);
        }
        if (unitConfig.has(UNIT_TYPE_FIELD)) {
            unitGroupConfig.add(UNIT_TYPE_FIELD, unitConfig.get(UNIT_TYPE_FIELD));
            unitConfig.remove(UNIT_TYPE_FIELD);
        }
        if (unitConfig.has(SERVICE_TEMPLATE_FIELD)) {
            unitGroupConfig.add(SERVICE_TEMPLATE_FIELD, unitConfig.get(SERVICE_TEMPLATE_FIELD));
            unitConfig.remove(SERVICE_TEMPLATE_FIELD);
        }
        unitConfig.add(UNIT_GROUP_CONFIG_FIELD, unitGroupConfig);

        // add type
        unitConfig.addProperty(TYPE_FIELD, UnitType.UNIT_GROUP.name());

        return unitConfig;
    }

}
