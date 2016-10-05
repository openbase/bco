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
public class AppConfig_0_To_1_DBConverter implements DBVersionConverter {

    private static final String TYPE_FIELD = "type";
    private static final String APP_CLASS_ID_FIELD = "app_class_id";
    private static final String APP_CONFIG_FIELD = "app_config";
    private static final String LOCATION_ID_FIELD = "location_id";
    private static final String PLACEMENT_CONFIG_FIELD = "placement_config";

    @Override
    public JsonObject upgrade(JsonObject unitConfig, final Map<File, JsonObject> dbSnapshot) {
        // add type
        unitConfig.addProperty(TYPE_FIELD, UnitType.APP.name());

        // move class to app config
        if (unitConfig.has(APP_CLASS_ID_FIELD)) {
            JsonObject appConfig = new JsonObject();
            appConfig.addProperty(APP_CLASS_ID_FIELD, unitConfig.get(APP_CLASS_ID_FIELD).getAsString());
            unitConfig.remove(APP_CLASS_ID_FIELD);
            unitConfig.add(APP_CONFIG_FIELD, appConfig);
        }

        // move location id to placement config
        if (unitConfig.has(LOCATION_ID_FIELD)) {
            JsonObject placementConfig = new JsonObject();
            placementConfig.addProperty(LOCATION_ID_FIELD, unitConfig.get(LOCATION_ID_FIELD).getAsString());
            unitConfig.remove(LOCATION_ID_FIELD);
            unitConfig.add(PLACEMENT_CONFIG_FIELD, placementConfig);
        }

        return unitConfig;
    }
}
