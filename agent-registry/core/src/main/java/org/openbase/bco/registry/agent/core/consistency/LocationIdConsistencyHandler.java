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

import org.openbase.bco.registry.location.remote.LocationRegistryRemote;
import org.openbase.jps.core.JPService;
import org.openbase.jps.exception.JPServiceException;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InvalidStateException;
import org.openbase.jul.extension.protobuf.IdentifiableMessage;
import org.openbase.jul.storage.registry.AbstractProtoBufRegistryConsistencyHandler;
import org.openbase.jul.storage.registry.EntryModification;
import org.openbase.jul.storage.registry.jp.JPRecoverDB;
import rst.homeautomation.control.agent.AgentConfigType.AgentConfig;
import org.openbase.jul.extension.protobuf.container.ProtoBufMessageMap;
import org.openbase.jul.storage.registry.ProtoBufRegistry;

/**
 *
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class LocationIdConsistencyHandler extends AbstractProtoBufRegistryConsistencyHandler<String, AgentConfig, AgentConfig.Builder> {

    final LocationRegistryRemote locationRegistryRemote;

    public LocationIdConsistencyHandler(final LocationRegistryRemote locationRegistryRemote) {
        this.locationRegistryRemote = locationRegistryRemote;
    }

    @Override
    public void processData(String id, IdentifiableMessage<String, AgentConfig, AgentConfig.Builder> entry, ProtoBufMessageMap<String, AgentConfig, AgentConfig.Builder> entryMap, ProtoBufRegistry<String, AgentConfig, AgentConfig.Builder> registry) throws CouldNotPerformException, EntryModification {
        AgentConfig agentConfig = entry.getMessage();

        if (!agentConfig.hasLocationId() || agentConfig.getLocationId().isEmpty()) {
            entry.setMessage(agentConfig.toBuilder().setLocationId(locationRegistryRemote.getRootLocationConfig().getId()));
            throw new EntryModification(entry, this);
        }

        // verify if configured location exists.
        if (!locationRegistryRemote.containsLocationConfigById(agentConfig.getLocationId())) {
            try {
                if (!JPService.getProperty(JPRecoverDB.class).getValue()) {
                    throw new InvalidStateException("The configured Location[" + agentConfig.getLocationId() + "] of Device[" + agentConfig.getId() + "] is unknown!");
                }
            } catch (JPServiceException ex) {
                throw new InvalidStateException("The configured Location[" + agentConfig.getLocationId() + "] of Device[" + agentConfig.getId() + "] is unknown and can not be recovered!", ex);
            }
            // recover agent location with root location.
            entry.setMessage(agentConfig.toBuilder().setLocationId(locationRegistryRemote.getRootLocationConfig().getId()));
            throw new EntryModification(entry, this);
        }
    }
}