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
import org.openbase.bco.registry.unit.lib.generator.UnitConfigIdGenerator;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.storage.registry.version.AbstractDBVersionConverter;
import org.openbase.jul.storage.registry.version.DBVersionControl;
import rst.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;

/**
 *
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class AgentConfig_1_To_2_DBConverter extends AbstractDBVersionConverter {

    private static final String TYPE_FIELD = "type";
    private static final String ID_FIELD = "id";
    private static final String AGENT_CLASS_ID_FIELD = "agent_class_id";
    private static final String AGENT_CONFIG_FIELD = "agent_config";
    private static final String LOCATION_ID_FIELD = "location_id";
    private static final String PLACEMENT_CONFIG_FIELD = "placement_config";

    private final UnitConfigIdGenerator idGenerator;

    public AgentConfig_1_To_2_DBConverter(DBVersionControl versionControl) {
        super(versionControl);
        this.idGenerator = new UnitConfigIdGenerator();
    }

    @Override
    public JsonObject upgrade(JsonObject agentUnitConfig, final Map<File, JsonObject> dbSnapshot) throws CouldNotPerformException {
        // add type
        agentUnitConfig.addProperty(TYPE_FIELD, UnitType.AGENT.name());

        // move class to agent config
        if (agentUnitConfig.has(AGENT_CLASS_ID_FIELD)) {
            JsonObject agentConfig = new JsonObject();
            agentConfig.addProperty(AGENT_CLASS_ID_FIELD, agentUnitConfig.get(AGENT_CLASS_ID_FIELD).getAsString());
            agentUnitConfig.remove(AGENT_CLASS_ID_FIELD);
            agentUnitConfig.add(AGENT_CONFIG_FIELD, agentConfig);
        }

        // move location id to placement config
        if (agentUnitConfig.has(LOCATION_ID_FIELD)) {
            JsonObject placementConfig = new JsonObject();
            placementConfig.addProperty(LOCATION_ID_FIELD, agentUnitConfig.get(LOCATION_ID_FIELD).getAsString());
            agentUnitConfig.remove(LOCATION_ID_FIELD);
            agentUnitConfig.add(PLACEMENT_CONFIG_FIELD, placementConfig);
        }

        agentUnitConfig.remove(ID_FIELD);
        agentUnitConfig.addProperty(ID_FIELD, idGenerator.generateId(null));

        return agentUnitConfig;
    }

}
