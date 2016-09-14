package org.openbase.bco.registry.location.core.consistency;

/*
 * #%L
 * REM LocationRegistry Core
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
import rst.spatial.ConnectionConfigType;
import rst.spatial.ConnectionConfigType.ConnectionConfig;
import org.openbase.jul.extension.protobuf.container.ProtoBufMessageMap;
import org.openbase.jul.storage.registry.ProtoBufRegistry;

/**
 *
 * @author <a href="mailto:thuxohl@techfak.uni-bielefeld.com">Tamino Huxohl</a>
 */
public class ConnectionLabelConsistencyHandler extends AbstractProtoBufRegistryConsistencyHandler<String, ConnectionConfigType.ConnectionConfig, ConnectionConfigType.ConnectionConfig.Builder> {

    private final Map<String, ConnectionConfig> connectionMap;

    public ConnectionLabelConsistencyHandler() {
        this.connectionMap = new HashMap<>();
    }

    @Override
    public void processData(String id, IdentifiableMessage<String, ConnectionConfigType.ConnectionConfig, ConnectionConfigType.ConnectionConfig.Builder> entry, ProtoBufMessageMap<String, ConnectionConfigType.ConnectionConfig, ConnectionConfigType.ConnectionConfig.Builder> entryMap, ProtoBufRegistry<String, ConnectionConfigType.ConnectionConfig, ConnectionConfigType.ConnectionConfig.Builder> registry) throws CouldNotPerformException, EntryModification {
        ConnectionConfig connection = entry.getMessage();

        if (!connection.hasLabel() || connection.getLabel().isEmpty()) {
            throw new NotAvailableException("connection.label");
        }

        if (!connection.hasPlacementConfig()|| !connection.getPlacementConfig().hasLocationId() || connection.getPlacementConfig().getLocationId().isEmpty()) {
            throw new NotAvailableException("connection.placement.locationId");
        }

        String key = connection.getLabel() + connection.getPlacementConfig().getLocationId();
        if (!connectionMap.containsKey(key)) {
            connectionMap.put(key, connection);
        } else {
            throw new InvalidStateException("Connection [" + connection + "] and connection [" + connectionMap.get(key) + "] are registered with the same label at the same location");
        }
    }

    @Override
    public void reset() {
        connectionMap.clear();
    }
}
