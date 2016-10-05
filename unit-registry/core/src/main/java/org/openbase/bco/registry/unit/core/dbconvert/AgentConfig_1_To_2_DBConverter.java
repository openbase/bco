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
public class AgentConfig_1_To_2_DBConverter implements DBVersionConverter {

    private static final String TYPE_FIELD = "type";
    private static final String AGENT_CLASS_ID_FIELD = "agent_class_id";
    private static final String AGENT_CONFIG_FIELD = "agent_config";
    private static final String LOCATION_ID_FIELD = "location_id";
    private static final String PLACEMENT_CONFIG_FIELD = "placement_config";

    @Override
    public JsonObject upgrade(JsonObject unitConfig, final Map<File, JsonObject> dbSnapshot) {
        // add type
        unitConfig.addProperty(TYPE_FIELD, UnitType.AGENT.name());

        // move class to agent config
        if (unitConfig.has(AGENT_CLASS_ID_FIELD)) {
            JsonObject agentConfig = new JsonObject();
            agentConfig.addProperty(AGENT_CLASS_ID_FIELD, unitConfig.get(AGENT_CLASS_ID_FIELD).getAsString());
            unitConfig.remove(AGENT_CLASS_ID_FIELD);
            unitConfig.add(AGENT_CONFIG_FIELD, agentConfig);
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
