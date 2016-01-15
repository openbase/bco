/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dc.bco.registry.location.core.consistency;

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
import rst.spatial.ConnectionConfigType;
import rst.spatial.ConnectionConfigType.ConnectionConfig;

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
    public void processData(String id, IdentifiableMessage<String, ConnectionConfigType.ConnectionConfig, ConnectionConfigType.ConnectionConfig.Builder> entry, ProtoBufMessageMapInterface<String, ConnectionConfigType.ConnectionConfig, ConnectionConfigType.ConnectionConfig.Builder> entryMap, ProtoBufRegistryInterface<String, ConnectionConfigType.ConnectionConfig, ConnectionConfigType.ConnectionConfig.Builder> registry) throws CouldNotPerformException, EntryModification {
        ConnectionConfig connection = entry.getMessage();

        if (!connection.hasLabel() || connection.getLabel().isEmpty()) {
            throw new NotAvailableException("connection.label");
        }

        if (!connection.hasPlacement() || !connection.getPlacement().hasLocationId() || connection.getPlacement().getLocationId().isEmpty()) {
            throw new NotAvailableException("connection.placement.locationId");
        }

        String key = connection.getLabel() + connection.getPlacement().getLocationId();
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
