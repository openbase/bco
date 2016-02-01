/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dc.bco.registry.agent.core.consistency;

/*
 * #%L
 * REM AgentRegistry Core
 * %%
 * Copyright (C) 2014 - 2016 DivineCooperation
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

import org.dc.bco.registry.location.remote.LocationRegistryRemote;
import org.dc.jul.exception.CouldNotPerformException;
import org.dc.jul.exception.NotAvailableException;
import org.dc.jul.extension.protobuf.IdentifiableMessage;
import org.dc.jul.extension.protobuf.container.ProtoBufMessageMapInterface;
import org.dc.jul.extension.rsb.scope.ScopeGenerator;
import org.dc.jul.storage.registry.AbstractProtoBufRegistryConsistencyHandler;
import org.dc.jul.storage.registry.EntryModification;
import org.dc.jul.storage.registry.ProtoBufRegistryInterface;
import rst.homeautomation.control.agent.AgentConfigType.AgentConfig;
import rst.rsb.ScopeType.Scope;

/**
 *
 * @author <a href="mailto:thuxohl@techfak.uni-bielefeld.com">Tamino Huxohl</a>
 */
public class ScopeConsistencyHandler extends AbstractProtoBufRegistryConsistencyHandler<String, AgentConfig, AgentConfig.Builder> {

    private final LocationRegistryRemote locationRegistryRemote;

    public ScopeConsistencyHandler(final LocationRegistryRemote locationRegistryRemote) {
        this.locationRegistryRemote = locationRegistryRemote;
    }

    @Override
    public void processData(String id, IdentifiableMessage<String, AgentConfig, AgentConfig.Builder> entry, ProtoBufMessageMapInterface<String, AgentConfig, AgentConfig.Builder> entryMap, ProtoBufRegistryInterface<String, AgentConfig, AgentConfig.Builder> registry) throws CouldNotPerformException, EntryModification {
        AgentConfig agentConfig = entry.getMessage();

        if (!agentConfig.hasLocationId()) {
            throw new NotAvailableException("agentConfig.locationId");
        }
        if (agentConfig.getLocationId().isEmpty()) {
            throw new NotAvailableException("Field agentConfig.locationId is empty");
        }

        Scope newScope = ScopeGenerator.generateAgentScope(agentConfig, locationRegistryRemote.getLocationConfigById(agentConfig.getLocationId()));

        // verify and update scope
        if (!ScopeGenerator.generateStringRep(agentConfig.getScope()).equals(ScopeGenerator.generateStringRep(newScope))) {
            entry.setMessage(agentConfig.toBuilder().setScope(newScope));
            throw new EntryModification(entry, this);
        }
    }

    @Override
    public void reset() {
    }
}
