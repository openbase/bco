package org.dc.bco.registry.location.core.consistency;

/*
 * #%L
 * REM LocationRegistry Core
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
