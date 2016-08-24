/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.openbase.bco.registry.agent.core.consistency;

/*
 * #%L
 * BCO Registry Agent Core
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

import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.extension.protobuf.IdentifiableMessage;
import org.openbase.jul.extension.protobuf.container.ProtoBufMessageMapInterface;
import org.openbase.jul.storage.registry.AbstractProtoBufRegistryConsistencyHandler;
import org.openbase.jul.storage.registry.EntryModification;
import org.openbase.jul.storage.registry.ProtoBufFileSynchronizedRegistry;
import org.openbase.jul.storage.registry.ProtoBufRegistryInterface;
import rst.homeautomation.control.agent.AgentClassType.AgentClass;
import rst.homeautomation.control.agent.AgentConfigType.AgentConfig;
import rst.homeautomation.control.agent.AgentRegistryDataType.AgentRegistryData;

/**
 *
 * @author <a href="mailto:thuxohl@techfak.uni-bielefeld.com">Tamino Huxohl</a>
 */
public class AgentConfigAgentClassIdConsistencyHandler extends AbstractProtoBufRegistryConsistencyHandler<String, AgentConfig, AgentConfig.Builder> {

    private final ProtoBufFileSynchronizedRegistry<String, AgentClass, AgentClass.Builder, AgentRegistryData.Builder> agentClassRegistry;

    public AgentConfigAgentClassIdConsistencyHandler(final ProtoBufFileSynchronizedRegistry<String, AgentClass, AgentClass.Builder, AgentRegistryData.Builder> agentClassRegistry) {
        this.agentClassRegistry = agentClassRegistry;
    }

    @Override
    public void processData(final String id, final IdentifiableMessage<String, AgentConfig, AgentConfig.Builder> entry, final ProtoBufMessageMapInterface<String, AgentConfig, AgentConfig.Builder> entryMap, final ProtoBufRegistryInterface<String, AgentConfig, AgentConfig.Builder> registry) throws CouldNotPerformException, EntryModification {
        AgentConfig agentConfig = entry.getMessage();

        if (!agentConfig.hasAgentClassId() || agentConfig.getAgentClassId().isEmpty()) {
            throw new NotAvailableException("agentclass.id");
        }

        // get throws a CouldNotPerformException if the agent class with the id does not exists
        agentClassRegistry.get(agentConfig.getAgentClassId());
    }
}
