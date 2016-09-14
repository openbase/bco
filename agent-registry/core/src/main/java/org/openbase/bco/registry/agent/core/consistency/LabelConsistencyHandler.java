package org.openbase.bco.registry.agent.core.consistency;

/*
 * #%L
 * REM AgentRegistry Core
 * %%
 * Copyright (C) 2014 - 2016 openbase.org
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

import java.util.HashMap;
import java.util.Map;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InvalidStateException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.extension.protobuf.IdentifiableMessage;
import org.openbase.jul.storage.registry.AbstractProtoBufRegistryConsistencyHandler;
import org.openbase.jul.storage.registry.EntryModification;
import rst.homeautomation.control.agent.AgentConfigType.AgentConfig;
import org.openbase.jul.extension.protobuf.container.ProtoBufMessageMap;
import org.openbase.jul.storage.registry.ProtoBufRegistry;

/**
 *
 * @author <a href="mailto:thuxohl@techfak.uni-bielefeld.com">Tamino Huxohl</a>
 */
public class LabelConsistencyHandler extends AbstractProtoBufRegistryConsistencyHandler<String, AgentConfig, AgentConfig.Builder> {

    private final Map<String, AgentConfig> agentMap;

    public LabelConsistencyHandler() {
        this.agentMap = new HashMap<>();
    }

    @Override
    public void processData(String id, IdentifiableMessage<String, AgentConfig, AgentConfig.Builder> entry, ProtoBufMessageMap<String, AgentConfig, AgentConfig.Builder> entryMap, ProtoBufRegistry<String, AgentConfig, AgentConfig.Builder> registry) throws CouldNotPerformException, EntryModification {
        AgentConfig agent = entry.getMessage();

        if (!agent.hasLabel() || agent.getLabel().isEmpty()) {
            throw new NotAvailableException("agent.label");
        }

        if (!agent.hasLocationId() || agent.getLocationId().isEmpty()) {
            throw new NotAvailableException("agent.locationId");
        }

        String key = agent.getLabel() + agent.getLocationId();
        if (!agentMap.containsKey(key)) {
            agentMap.put(key, agent);
        } else {
            throw new InvalidStateException("Agent [" + agent + "] and agent [" + agentMap.get(key) + "] are registered with the same label at the same location");
        }
    }

    @Override
    public void reset() {
        agentMap.clear();
    }
}
