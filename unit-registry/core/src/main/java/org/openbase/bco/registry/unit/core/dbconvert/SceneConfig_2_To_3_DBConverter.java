package org.openbase.bco.registry.unit.core.dbconvert;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.File;
import java.util.Map;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.storage.registry.version.AbstractDBVersionConverter;
import org.openbase.jul.storage.registry.version.DBVersionControl;

/**
 *
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class SceneConfig_2_To_3_DBConverter extends AbstractDBVersionConverter {

    private final String SCENE_CONFIG_FIELD = "scene_config";
    private final String ACTION_CONFIG_FIELD = "action_config";
    private final String REQUIRED_SERVICE_STATE_DESCRIPTION_FIELD = "required_service_state_description";

    private final String UNIT_ID_FIELD = "unit_id";
    private final String SERVICE_TYPE_FIELD = "service_type";
    private final String UNIT_TYPE_FIELD = "unit_type";
    private final String SERVICE_ATTRIBUTE_TYPE_FIELD = "service_attribute_type";
    private final String SERVICE_ATTRIBUTE_TYPE = "service_attribute";

    public SceneConfig_2_To_3_DBConverter(DBVersionControl versionControl) {
        super(versionControl);
    }

    @Override
    public JsonObject upgrade(JsonObject sceneUnitConfig, Map<File, JsonObject> dbSnapshot) throws CouldNotPerformException {
        JsonObject sceneConfig = sceneUnitConfig.getAsJsonObject(SCENE_CONFIG_FIELD);

        JsonArray serviceStateDescriptions = new JsonArray();
        for (JsonElement actionConfigElem : sceneConfig.getAsJsonArray(ACTION_CONFIG_FIELD)) {
            JsonObject actionConfig = actionConfigElem.getAsJsonObject();
            JsonObject serviceStateDescription = new JsonObject();

            if (actionConfig.has(UNIT_ID_FIELD)) {
                serviceStateDescription.addProperty(UNIT_ID_FIELD, actionConfig.get(UNIT_ID_FIELD).getAsString());
            }
            if (actionConfig.has(SERVICE_TYPE_FIELD)) {
                serviceStateDescription.addProperty(SERVICE_TYPE_FIELD, actionConfig.get(SERVICE_TYPE_FIELD).getAsString());
            }
            if (actionConfig.has(SERVICE_ATTRIBUTE_TYPE_FIELD)) {
                serviceStateDescription.addProperty(SERVICE_ATTRIBUTE_TYPE_FIELD, actionConfig.get(SERVICE_ATTRIBUTE_TYPE_FIELD).getAsString());
            }
            if (actionConfig.has(SERVICE_ATTRIBUTE_TYPE)) {
                serviceStateDescription.addProperty(SERVICE_ATTRIBUTE_TYPE, actionConfig.get(SERVICE_ATTRIBUTE_TYPE).getAsString());
            }

            serviceStateDescription.addProperty(UNIT_TYPE_FIELD, "UNKNOWN");
            serviceStateDescriptions.add(serviceStateDescription);
        }
        sceneConfig.remove(ACTION_CONFIG_FIELD);
        sceneConfig.add(REQUIRED_SERVICE_STATE_DESCRIPTION_FIELD, serviceStateDescriptions);

        return sceneUnitConfig;
    }
}
