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
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.storage.registry.version.AbstractGlobalDBVersionConverter;
import org.openbase.jul.storage.registry.version.DBVersionControl;
import org.openbase.jul.storage.registry.version.DatabaseEntryDescriptor;

/**
 *
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class AgentConfig_2_To_3_DBConverter extends AbstractGlobalDBVersionConverter {

    private static final String DAL_UNIT_CONFIG_DB_ID = "dal-unit-config-db";

    private static final String ID_FIELD = "id";
    private static final String SCOPE_FIELD = "scope";
    private static final String COMPONENT_FIELD = "component";
    private static final String LABEL_FIELD = "label";
    private static final String KEY_FIELD = "key";
    private static final String VALUE_FIELD = "value";
    private static final String ENTRY_FIELD = "entry";
    private static final String META_CONFIG_FIELD = "meta_config";

    private static final String POWER_STATE_SYNCHRONISER_IDENTIFIER = "PowerStateSynchroniser";
    private static final String AMBIENT_COLOR_AGENT_IDE_AGENT_IDENTIFIER = "AmbientColor";

    private final Map<String, String> unitScopeIdMap;

    public AgentConfig_2_To_3_DBConverter(DBVersionControl versionControl) {
        super(versionControl);
        unitScopeIdMap = new HashMap<>();
    }

    @Override
    public JsonObject upgrade(JsonObject agentUnitConfig, Map<File, JsonObject> dbSnapshot, Map<String, Map<File, DatabaseEntryDescriptor>> globalDbSnapshots) throws CouldNotPerformException {
        if (unitScopeIdMap.isEmpty()) {
            globalDbSnapshots.get(DAL_UNIT_CONFIG_DB_ID).values().stream().map((dalUnitConfigDesriptor) -> dalUnitConfigDesriptor.getEntry()).filter((dalUnitConfig) -> !(!dalUnitConfig.has(ID_FIELD) || !dalUnitConfig.has(SCOPE_FIELD))).forEach((dalUnitConfig) -> {
                String id = dalUnitConfig.get(ID_FIELD).getAsString();
                JsonObject scope = dalUnitConfig.getAsJsonObject(SCOPE_FIELD);
                if (!(!scope.has(COMPONENT_FIELD))) {
                    JsonArray scopeComponents = scope.getAsJsonArray(COMPONENT_FIELD);
                    String scopeAsString = "/";
                    for (JsonElement component : scopeComponents) {
                        scopeAsString += component.getAsString() + "/";
                    }

                    unitScopeIdMap.put(scopeAsString, id);
                }
            });
        }

        boolean isPowerStateSynchroniserAgent = false;
        boolean isAmbientColorAgent = false;
        if (agentUnitConfig.has(LABEL_FIELD)) {
            String label = agentUnitConfig.get(LABEL_FIELD).getAsString();
            if (label.contains(POWER_STATE_SYNCHRONISER_IDENTIFIER)) {
                isPowerStateSynchroniserAgent = true;
            } else if (label.contains(AMBIENT_COLOR_AGENT_IDE_AGENT_IDENTIFIER)) {
                isAmbientColorAgent = true;
            }
        }

        if (isPowerStateSynchroniserAgent || isAmbientColorAgent) {
            if (agentUnitConfig.has(META_CONFIG_FIELD)) {
                JsonObject metaConfig = agentUnitConfig.getAsJsonObject(META_CONFIG_FIELD);
                if (metaConfig.has(ENTRY_FIELD)) {
                    JsonArray metaConfigEntries = metaConfig.getAsJsonArray(ENTRY_FIELD);

                    if (isPowerStateSynchroniserAgent) {
                        updatePowerStateSynchroniserAgentMetaConfig(metaConfigEntries, globalDbSnapshots.get(DAL_UNIT_CONFIG_DB_ID));
                    } else if (isAmbientColorAgent) {
                        updateAmbientColorAgentMetaConfig(metaConfigEntries, globalDbSnapshots.get(DAL_UNIT_CONFIG_DB_ID));
                    }
                }
            }
        }

        return agentUnitConfig;
    }

    // PowerStateSynchroniser Meta Config Entries with pattern TARGET_(number) or SOURCE
    private void updatePowerStateSynchroniserAgentMetaConfig(JsonArray metaConfigEntries, Map<File, DatabaseEntryDescriptor> dalUnitDB) throws CouldNotPerformException {
        JsonObject metaConfigEntry;
        String key, unitScope;
        for (JsonElement metaConfigEntryElem : metaConfigEntries) {
            metaConfigEntry = metaConfigEntryElem.getAsJsonObject();

            if (!metaConfigEntry.has(KEY_FIELD) || !metaConfigEntry.has(VALUE_FIELD)) {
                continue;
            }

            key = metaConfigEntry.get(KEY_FIELD).getAsString();
            if (key.matches("TARGET_[0123456789]+") || key.equals("SOURCE")) {
                unitScope = updateScopeToNewUnitTypes(metaConfigEntry.get(VALUE_FIELD).getAsString());
                if (!unitScopeIdMap.containsKey(unitScope)) {
                    throw new CouldNotPerformException("Could not replace scope [" + unitScope + "] with unit id in metaConfigEntry with key [" + key + "] of PowerStateSynchroniser");
                }
                metaConfigEntry.remove(VALUE_FIELD);
                metaConfigEntry.addProperty(VALUE_FIELD, unitScopeIdMap.get(unitScope));
            }
        }
    }

    // AmbientColorAgents MetaConfig entries with pattern UNIT_(number)
    private void updateAmbientColorAgentMetaConfig(JsonArray metaConfigEntries, Map<File, DatabaseEntryDescriptor> dalUnitDB) throws CouldNotPerformException {
        JsonObject metaConfigEntry;
        String key, unitScope;
        for (JsonElement metaConfigEntryElem : metaConfigEntries) {
            metaConfigEntry = metaConfigEntryElem.getAsJsonObject();

            if (!metaConfigEntry.has(KEY_FIELD) || !metaConfigEntry.has(VALUE_FIELD)) {
                continue;
            }

            key = metaConfigEntry.get(KEY_FIELD).getAsString();
            if (key.matches("UNIT_[0123456789]+")) {
                unitScope = updateScopeToNewUnitTypes(metaConfigEntry.get(VALUE_FIELD).getAsString());
                if (!unitScopeIdMap.containsKey(unitScope)) {
                    throw new CouldNotPerformException("Could not replace scope [" + unitScope + "] with unit id in metaConfigEntry with key [" + key + "] of PowerStateSynchroniser");
                }
                metaConfigEntry.remove(VALUE_FIELD);
                metaConfigEntry.addProperty(VALUE_FIELD, unitScopeIdMap.get(unitScope));
            }
        }
    }

    private String updateScopeToNewUnitTypes(String scopeAsString) {
        return scopeAsString.replace("ambientlight", "colorablelight");
    }
}
