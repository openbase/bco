/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dc.bco.registry.location.core.consistency;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import org.dc.jul.exception.CouldNotPerformException;
import org.dc.jul.exception.InvalidStateException;
import org.dc.jul.extension.protobuf.IdentifiableMessage;
import org.dc.jul.extension.protobuf.container.ProtoBufMessageMapInterface;
import org.dc.jul.storage.registry.AbstractProtoBufRegistryConsistencyHandler;
import org.dc.jul.storage.registry.EntryModification;
import org.dc.jul.storage.registry.ProtoBufFileSynchronizedRegistry;
import org.dc.jul.storage.registry.ProtoBufRegistryInterface;
import rst.spatial.ConnectionConfigType.ConnectionConfig;
import rst.spatial.LocationConfigType.LocationConfig;
import rst.spatial.LocationRegistryType.LocationRegistry;

/**
 *
 * @author <a href="mailto:thuxohl@techfak.uni-bielefeld.com">Tamino Huxohl</a>
 */
public class ConnectionTilesConsistencyHandler extends AbstractProtoBufRegistryConsistencyHandler<String, ConnectionConfig, ConnectionConfig.Builder> {

    private final ProtoBufFileSynchronizedRegistry<String, LocationConfig, LocationConfig.Builder, LocationRegistry.Builder> locationConfigRegistry;

    public ConnectionTilesConsistencyHandler(ProtoBufFileSynchronizedRegistry<String, LocationConfig, LocationConfig.Builder, LocationRegistry.Builder> locationConfigRegistry) {
        this.locationConfigRegistry = locationConfigRegistry;
    }

    @Override
    public void processData(String id, IdentifiableMessage<String, ConnectionConfig, ConnectionConfig.Builder> entry, ProtoBufMessageMapInterface<String, ConnectionConfig, ConnectionConfig.Builder> entryMap, ProtoBufRegistryInterface<String, ConnectionConfig, ConnectionConfig.Builder> registry) throws CouldNotPerformException, EntryModification {
        ConnectionConfig.Builder connectionConfig = entry.getMessage().toBuilder();

        if (connectionConfig.getTileIdList().size() < 2) {
            throw new InvalidStateException("Connections must connect at least 2 tiles which is not true for connection [" + connectionConfig + "]");
        }

        boolean modification = false;
        connectionConfig.clearTileId();

        // remove duplicated entries and location ids that are not tiles
        Map<String, String> tileIds = new HashMap<>();
        for (String tileId : entry.getMessage().getTileIdList()) {
            LocationConfig location = null;
            if (locationConfigRegistry.contains(tileId)) {
                location = locationConfigRegistry.get(tileId).getMessage();
            }
            if (location != null && location.hasType() && location.getType() == LocationConfig.LocationType.TILE) {
                tileIds.put(tileId, tileId);
            } else {
                modification = true;
            }
        }
        connectionConfig.addAllTileId(new ArrayList<>(tileIds.keySet()));

        if (modification) {
            throw new EntryModification(entry.setMessage(connectionConfig), this);
        }
    }

    @Override
    public void reset() {
    }
}
