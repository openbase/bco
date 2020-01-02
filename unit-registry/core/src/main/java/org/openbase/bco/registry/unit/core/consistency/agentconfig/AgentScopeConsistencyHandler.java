package org.openbase.bco.registry.unit.core.consistency.agentconfig;

/*
 * #%L
 * BCO Registry Unit Core
 * %%
 * Copyright (C) 2014 - 2020 openbase.org
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

import org.openbase.bco.registry.clazz.remote.CachedClassRegistryRemote;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InvalidStateException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.extension.protobuf.IdentifiableMessage;
import org.openbase.jul.extension.protobuf.container.ProtoBufMessageMap;
import org.openbase.bco.registry.lib.generator.ScopeGenerator;
import org.openbase.jul.extension.type.processing.ScopeProcessor;
import org.openbase.jul.storage.registry.AbstractProtoBufRegistryConsistencyHandler;
import org.openbase.jul.storage.registry.EntryModification;
import org.openbase.jul.storage.registry.ProtoBufFileSynchronizedRegistry;
import org.openbase.jul.storage.registry.ProtoBufRegistry;
import org.openbase.type.domotic.unit.UnitConfigType.UnitConfig;
import org.openbase.type.domotic.registry.UnitRegistryDataType.UnitRegistryData;
import org.openbase.type.domotic.unit.agent.AgentClassType.AgentClass;
import org.openbase.type.communication.ScopeType.Scope;

/**
 *
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class AgentScopeConsistencyHandler extends AbstractProtoBufRegistryConsistencyHandler<String, UnitConfig, UnitConfig.Builder> {

    private final ProtoBufFileSynchronizedRegistry<String, UnitConfig, UnitConfig.Builder, UnitRegistryData.Builder> locationRegistry;
    private final Map<String, UnitConfig> agentMap;

    public AgentScopeConsistencyHandler(final ProtoBufFileSynchronizedRegistry<String, UnitConfig, UnitConfig.Builder, UnitRegistryData.Builder> locationRegistry) {
        this.locationRegistry = locationRegistry;
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
        final AgentClass agentClassById = CachedClassRegistryRemote.getRegistry().getAgentClassById(agentUnitConfig.getAgentConfig().getAgentClassId());
        final Scope newScope = ScopeGenerator.generateAgentScope(agentUnitConfig, agentClassById, locationRegistry.getMessage(agentUnitConfig.getPlacementConfig().getLocationId()));

        // verify and update scope
        if (!ScopeProcessor.generateStringRep(agentUnitConfig.getScope()).equals(ScopeProcessor.generateStringRep(newScope))) {
            if (agentMap.containsKey(ScopeProcessor.generateStringRep(newScope))) {
                throw new InvalidStateException("Two agents [" + agentUnitConfig + "][" + agentMap.get(ScopeProcessor.generateStringRep(newScope)) + "] are registered with the same label and location");
            } else {
                agentMap.put(ScopeProcessor.generateStringRep(newScope), agentUnitConfig);
                entry.setMessage(agentUnitConfig.toBuilder().setScope(newScope), this);
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
