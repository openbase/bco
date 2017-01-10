package org.openbase.bco.registry.unit.core.consistency.connectionconfig;

/*
 * #%L
 * BCO Registry Unit Core
 * %%
 * Copyright (C) 2014 - 2017 openbase.org
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
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InvalidStateException;
import org.openbase.jul.extension.protobuf.IdentifiableMessage;
import org.openbase.jul.extension.protobuf.container.ProtoBufMessageMap;
import org.openbase.jul.storage.registry.AbstractProtoBufRegistryConsistencyHandler;
import org.openbase.jul.storage.registry.EntryModification;
import org.openbase.jul.storage.registry.ProtoBufFileSynchronizedRegistry;
import org.openbase.jul.storage.registry.ProtoBufRegistry;
import rst.domotic.unit.UnitConfigType.UnitConfig;
import rst.domotic.registry.UnitRegistryDataType.UnitRegistryData;
import rst.domotic.unit.connection.ConnectionConfigType.ConnectionConfig;
import rst.domotic.unit.location.LocationConfigType.LocationConfig;

/**
 *
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class ConnectionTilesConsistencyHandler extends AbstractProtoBufRegistryConsistencyHandler<String, UnitConfig, UnitConfig.Builder> {

    private final ProtoBufFileSynchronizedRegistry<String, UnitConfig, UnitConfig.Builder, UnitRegistryData.Builder> locationRegistry;

    public ConnectionTilesConsistencyHandler(ProtoBufFileSynchronizedRegistry<String, UnitConfig, UnitConfig.Builder, UnitRegistryData.Builder> locationRegistry) {
        this.locationRegistry = locationRegistry;
    }

    @Override
    public void processData(String id, IdentifiableMessage<String, UnitConfig, UnitConfig.Builder> entry, ProtoBufMessageMap<String, UnitConfig, UnitConfig.Builder> entryMap, ProtoBufRegistry<String, UnitConfig, UnitConfig.Builder> registry) throws CouldNotPerformException, EntryModification {
        UnitConfig.Builder connectionUnitConfig = entry.getMessage().toBuilder();
        ConnectionConfig.Builder connectionConfig = connectionUnitConfig.getConnectionConfigBuilder();

        if (connectionConfig.getTileIdList().size() < 2) {
            throw new InvalidStateException("Connections must connect at least 2 tiles which is not true for connection [" + entry.getMessage() + "]");
        }

        boolean modification = false;
        connectionConfig.clearTileId();
        // remove duplicated entries and location ids that are not tiles
        Map<String, String> tileIds = new HashMap<>();
        for (String tileId : entry.getMessage().getConnectionConfig().getTileIdList()) {
            UnitConfig location = null;
            if (locationRegistry.contains(tileId)) {
                location = locationRegistry.get(tileId).getMessage();
            }
            if (location != null && location.getLocationConfig().hasType() && location.getLocationConfig().getType() == LocationConfig.LocationType.TILE) {
                tileIds.put(tileId, tileId);
            } else {
                modification = true;
            }
        }
        connectionConfig.addAllTileId(new ArrayList<>(tileIds.keySet()));

        if (modification) {
            throw new EntryModification(entry.setMessage(connectionUnitConfig), this);
        }
    }
}
