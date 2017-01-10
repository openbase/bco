package org.openbase.bco.registry.unit.core.consistency.agentconfig;

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
import java.util.Map;
import java.util.TreeMap;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InvalidStateException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.extension.protobuf.IdentifiableMessage;
import org.openbase.jul.extension.protobuf.container.ProtoBufMessageMap;
import org.openbase.jul.extension.rsb.scope.ScopeGenerator;
import org.openbase.jul.storage.registry.AbstractProtoBufRegistryConsistencyHandler;
import org.openbase.jul.storage.registry.EntryModification;
import org.openbase.jul.storage.registry.ProtoBufFileSynchronizedRegistry;
import org.openbase.jul.storage.registry.ProtoBufRegistry;
import org.openbase.jul.storage.registry.Registry;
import rst.domotic.unit.UnitConfigType.UnitConfig;
import rst.domotic.registry.UnitRegistryDataType.UnitRegistryData;
import rst.domotic.unit.agent.AgentClassType.AgentClass;
import rst.rsb.ScopeType.Scope;

/**
 *
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class AgentScopeConsistencyHandler extends AbstractProtoBufRegistryConsistencyHandler<String, UnitConfig, UnitConfig.Builder> {

    private final ProtoBufFileSynchronizedRegistry<String, UnitConfig, UnitConfig.Builder, UnitRegistryData.Builder> locationRegistry;
    private final Registry<String, IdentifiableMessage<String, AgentClass, AgentClass.Builder>> agentClassRegistry;
    private final Map<String, UnitConfig> agentMap;

    public AgentScopeConsistencyHandler(final ProtoBufFileSynchronizedRegistry<String, UnitConfig, UnitConfig.Builder, UnitRegistryData.Builder> locationRegistry, final Registry<String, IdentifiableMessage<String, AgentClass, AgentClass.Builder>> agentClassRegistry) {
        this.locationRegistry = locationRegistry;
        this.agentClassRegistry = agentClassRegistry;
        this.agentMap = new TreeMap<>();
    }

    @Override
    public void processData(String id, IdentifiableMessage<String, UnitConfig, UnitConfig.Builder> entry, ProtoBufMessageMap<String, UnitConfig, UnitConfig.Builder> entryMap, ProtoBufRegistry<String, UnitConfig, UnitConfig.Builder> registry) throws CouldNotPerformException, EntryModification {
        UnitConfig agentUnitConfig = entry.getMessage();

        if (!agentUnitConfig.hasPlacementConfig()) {
            throw new NotAvailableException("agent.placementConfig");
        }

        if (!agentUnitConfig.getPlacementConfig().hasLocationId() || agentUnitConfig.getPlacementConfig().getLocationId().isEmpty()) {
            throw new NotAvailableException("agent.placementConfig.locationId");
        }
        
        if (!agentUnitConfig.getAgentConfig().hasAgentClassId() || agentUnitConfig.getAgentConfig().getAgentClassId().isEmpty()) {
            throw new NotAvailableException("agent.agentClassId");
        }

        Scope newScope = ScopeGenerator.generateAgentScope(agentUnitConfig, agentClassRegistry.get(agentUnitConfig.getAgentConfig().getAgentClassId()).getMessage(), locationRegistry.getMessage(agentUnitConfig.getPlacementConfig().getLocationId()));

        // verify and update scope
        if (!ScopeGenerator.generateStringRep(agentUnitConfig.getScope()).equals(ScopeGenerator.generateStringRep(newScope))) {
            if (agentMap.containsKey(ScopeGenerator.generateStringRep(newScope))) {
                throw new InvalidStateException("Two agents [" + agentUnitConfig + "][" + agentMap.get(ScopeGenerator.generateStringRep(newScope)) + "] are registered with the same label and location");
            } else {
                agentMap.put(ScopeGenerator.generateStringRep(newScope), agentUnitConfig);
                entry.setMessage(agentUnitConfig.toBuilder().setScope(newScope));
                throw new EntryModification(entry, this);
            }
        }
    }

    @Override
    public void reset() {
        agentMap.clear();
        super.reset();
    }
}
