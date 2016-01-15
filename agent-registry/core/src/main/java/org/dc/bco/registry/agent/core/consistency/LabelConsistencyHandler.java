/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dc.bco.registry.agent.core.consistency;

import java.util.HashMap;
import java.util.Map;
import org.dc.jul.exception.CouldNotPerformException;
import org.dc.jul.exception.InvalidStateException;
import org.dc.jul.exception.NotAvailableException;
import org.dc.jul.extension.protobuf.IdentifiableMessage;
import org.dc.jul.extension.protobuf.container.ProtoBufMessageMapInterface;
import org.dc.jul.storage.registry.AbstractProtoBufRegistryConsistencyHandler;
import org.dc.jul.storage.registry.EntryModification;
import org.dc.jul.storage.registry.ProtoBufRegistryInterface;
import rst.homeautomation.control.agent.AgentConfigType.AgentConfig;

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
    public void processData(String id, IdentifiableMessage<String, AgentConfig, AgentConfig.Builder> entry, ProtoBufMessageMapInterface<String, AgentConfig, AgentConfig.Builder> entryMap, ProtoBufRegistryInterface<String, AgentConfig, AgentConfig.Builder> registry) throws CouldNotPerformException, EntryModification {
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
