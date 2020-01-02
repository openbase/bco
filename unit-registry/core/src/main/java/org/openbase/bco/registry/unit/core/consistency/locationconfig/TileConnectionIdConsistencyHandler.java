package org.openbase.bco.registry.unit.core.consistency.locationconfig;

/*-
 * #%L
 * BCO Registry Unit Core
 * %%
 * Copyright (C) 2014 - 2020 openbase.org
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
import java.util.List;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.extension.protobuf.IdentifiableMessage;
import org.openbase.jul.extension.protobuf.container.ProtoBufMessageMap;
import org.openbase.jul.storage.registry.AbstractProtoBufRegistryConsistencyHandler;
import org.openbase.jul.storage.registry.EntryModification;
import org.openbase.jul.storage.registry.ProtoBufFileSynchronizedRegistry;
import org.openbase.jul.storage.registry.ProtoBufRegistry;
import org.openbase.type.domotic.registry.UnitRegistryDataType.UnitRegistryData;
import org.openbase.type.domotic.unit.UnitConfigType.UnitConfig;
import org.openbase.type.domotic.unit.location.LocationConfigType.LocationConfig;
import org.openbase.type.domotic.unit.location.LocationConfigType.LocationConfig.LocationType;
import org.openbase.type.domotic.unit.location.TileConfigType.TileConfig;

/**
 * ConsistencyHandler that synchronized connection id's into tile configurations.
 *
 * @author <a href="mailto:pLeminoq@openbase.org">Tamino Huxohl</a>
 */
public class TileConnectionIdConsistencyHandler extends AbstractProtoBufRegistryConsistencyHandler<String, UnitConfig, UnitConfig.Builder> {

    private final ProtoBufFileSynchronizedRegistry<String, UnitConfig, UnitConfig.Builder, UnitRegistryData.Builder> connectionRegistry;

    public TileConnectionIdConsistencyHandler(final ProtoBufFileSynchronizedRegistry<String, UnitConfig, UnitConfig.Builder, UnitRegistryData.Builder> connectionRegistry) {
        this.connectionRegistry = connectionRegistry;
    }

    @Override
    public void processData(String id, IdentifiableMessage<String, UnitConfig, UnitConfig.Builder> entry, ProtoBufMessageMap<String, UnitConfig, UnitConfig.Builder> entryMap, ProtoBufRegistry<String, UnitConfig, UnitConfig.Builder> registry) throws CouldNotPerformException, EntryModification {
        // Do nothing if not a tile
        if (entry.getMessage().getLocationConfig().getLocationType() != LocationType.TILE) {
            return;
        }

        UnitConfig.Builder locationUnitConfig = entry.getMessage().toBuilder();
        LocationConfig.Builder locationConfig = locationUnitConfig.getLocationConfigBuilder();
        TileConfig.Builder tileConfig = locationConfig.getTileConfigBuilder();

        boolean modification = false;
        // save old connection ids
        List<String> oldConnectionIdList = new ArrayList<>(tileConfig.getConnectionIdList());
        // clear connection ids
        tileConfig.clearConnectionId();
        for (UnitConfig connectionUnitConfig : connectionRegistry.getMessages()) {
            for (String tileId : connectionUnitConfig.getConnectionConfig().getTileIdList()) {
                if (tileId.equals(locationUnitConfig.getId())) {
                    // add connection ids if they connect this tile
                    tileConfig.addConnectionId(connectionUnitConfig.getId());

                    // test if this connection was alredy in the old connection list
                    if (!modification && !oldConnectionIdList.contains(connectionUnitConfig.getId())) {
                        // if not its a modification
                        modification = true;
                    }
                }
            }
        }

        // test if there are any connections missing from the old list, if yes its a modification
        for (String connectionId : oldConnectionIdList) {
            if (!tileConfig.getConnectionIdList().contains(connectionId)) {
                modification = true;
                break;
            }
        }

        if (modification) {
            throw new EntryModification(entry.setMessage(locationUnitConfig, this), this);
        }
    }
}
