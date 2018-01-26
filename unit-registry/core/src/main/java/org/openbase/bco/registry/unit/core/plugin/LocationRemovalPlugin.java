package org.openbase.bco.registry.unit.core.plugin;

/*-
 * #%L
 * BCO Registry Unit Core
 * %%
 * Copyright (C) 2014 - 2018 openbase.org
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.RejectedException;
import org.openbase.jul.extension.protobuf.IdentifiableMessage;
import org.openbase.jul.storage.registry.ProtoBufFileSynchronizedRegistry;
import org.openbase.jul.storage.registry.plugin.FileRegistryPluginAdapter;
import org.openbase.jul.storage.registry.plugin.ProtobufRegistryPluginAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rst.domotic.registry.UnitRegistryDataType.UnitRegistryData;
import rst.domotic.unit.UnitConfigType.UnitConfig;
import rst.domotic.unit.UnitConfigType.UnitConfig.Builder;
import rst.domotic.unit.location.LocationConfigType.LocationConfig.LocationType;
import rst.domotic.unit.location.TileConfigType.TileConfig;
import rst.spatial.PlacementConfigType.PlacementConfig;

/**
 * A plugin that handles changes that have to be done before removing a location.
 *
 * @author <a href="mailto:thuxohl@techfak.uni-bielefeld.de">Tamino Huxohl</a>
 */
public class LocationRemovalPlugin extends ProtobufRegistryPluginAdapter<String, UnitConfig, Builder> {

    private static final Logger LOGGER = LoggerFactory.getLogger(LocationRemovalPlugin.class);

    private final List<ProtoBufFileSynchronizedRegistry<String, UnitConfig, UnitConfig.Builder, UnitRegistryData.Builder>> unitConfigRegistryList;
    private final ProtoBufFileSynchronizedRegistry<String, UnitConfig, UnitConfig.Builder, UnitRegistryData.Builder> locationUnitConfigRegistry;
    private final ProtoBufFileSynchronizedRegistry<String, UnitConfig, UnitConfig.Builder, UnitRegistryData.Builder> connectionUnitConfigRegistry;

    public LocationRemovalPlugin(
            final List<ProtoBufFileSynchronizedRegistry<String, UnitConfig, UnitConfig.Builder, UnitRegistryData.Builder>> unitConfigRegistryList,
            final ProtoBufFileSynchronizedRegistry<String, UnitConfig, UnitConfig.Builder, UnitRegistryData.Builder> locationUnitConfigRegistry,
            final ProtoBufFileSynchronizedRegistry<String, UnitConfig, UnitConfig.Builder, UnitRegistryData.Builder> connectionUnitConfigRegistry) {
        this.unitConfigRegistryList = unitConfigRegistryList;
        this.locationUnitConfigRegistry = locationUnitConfigRegistry;
        this.connectionUnitConfigRegistry = connectionUnitConfigRegistry;
    }

    /**
     * Handle things which have to be done before a location is removed.
     * This mainly involves to move all units to at the location to its parent.
     * Additionally if the removed location is a tile, all its regions and connections to it are removed.
     * Furthermore removing the roo location is rejected.
     *
     * @param entry The location which will be removed.
     * @throws RejectedException If the entry is the root location or the process of changes needed to remove the location fails.
     */
    @Override
    public void beforeRemove(IdentifiableMessage<String, UnitConfig, UnitConfig.Builder> entry) throws RejectedException {
        // the root location cannot be removed
        if (entry.getMessage().getLocationConfig().getRoot()) {
            throw new RejectedException("The root location cannot be removed. If you want to remove this location introduce another root first.");
        }

        try {
            // handle special case that the location to be removed is a tile
            if (entry.getMessage().getLocationConfig().getType() == LocationType.TILE) {
                // remove all regions which are placed in this tile
                try {
                    for (UnitConfig unitConfig : getChildLocationSet(entry.getMessage())) {
                        locationUnitConfigRegistry.remove(unitConfig);
                    }
                } catch (CouldNotPerformException ex) {
                    throw new RejectedException("Could not remove all regions of tile[" + entry.getMessage().getLabel() + "] which have to be removed");
                }

                // remove connections which now do not work anymore
                try {
                    for (UnitConfig unitConfig : getConnectionsToRemove(entry.getMessage().getLocationConfig().getTileConfig())) {
                        connectionUnitConfigRegistry.remove(unitConfig);
                    }
                } catch (CouldNotPerformException ex) {
                    throw new RejectedException("Could not remove all connection of tile[" + entry.getMessage().getLabel() + "] which have to be removed");
                }

            }

            // place every unit at the parent of the removed location
            try {
                String id = entry.getMessage().getId();
                String parentId = entry.getMessage().getPlacementConfig().getLocationId();
                for (ProtoBufFileSynchronizedRegistry<String, UnitConfig, UnitConfig.Builder, UnitRegistryData.Builder> unitConfigRegistry : unitConfigRegistryList) {
                    for (UnitConfig unitConfig : unitConfigRegistry.getMessages()) {
                        if (unitConfig.getPlacementConfig().getLocationId().equals(id)) {
                            UnitConfig.Builder unitConfigBuilder = unitConfig.toBuilder();
                            PlacementConfig.Builder placementConfigBuilder = unitConfigBuilder.getPlacementConfigBuilder();

                            placementConfigBuilder.setLocationId(parentId);

                            unitConfigRegistry.update(unitConfigBuilder.build());
                        }
                    }
                }
            } catch (CouldNotPerformException ex) {
                throw new RejectedException("Unable to transfer all units registerd at[" + entry.getMessage().getLabel() + "] to its parent", ex);
            }
        } catch (CouldNotPerformException ex) {
            throw new RejectedException("Unable to transfer all units registerd at[" + entry.getMessage().getLabel() + "] to its parent", ex);
        }
    }

    /**
     * Get a set of child locations of the given location.
     * They are resolved recursively.
     *
     * @param locationUnitConfig The location of whom the child set is generated.
     * @return A set of all children of the given location.
     * @throws CouldNotPerformException If a child id of the given location cannot be resolved.
     */
    private Set<UnitConfig> getChildLocationSet(final UnitConfig locationUnitConfig) throws CouldNotPerformException {
        final Set<UnitConfig> childLocationSet = new HashSet<>();

        for (String childId : locationUnitConfig.getLocationConfig().getChildIdList()) {
            UnitConfig child = locationUnitConfigRegistry.getMessage(childId);
            childLocationSet.add(child);
            childLocationSet.addAll(getChildLocationSet(child));
        }

        return childLocationSet;
    }

    /**
     * Get a set of all connections which have to be removed when the location
     * with the given TileConfig is removed.
     *
     * @param tileConfig The TileConfig of the location to be removed.
     * @return A set of all connections that have to be removed. These are connections connecting the given tile and and only one other tile.
     * @throws CouldNotPerformException If a connection id of the given tile cannot be resolved.
     */
    private Set<UnitConfig> getConnectionsToRemove(final TileConfig tileConfig) throws CouldNotPerformException {
        final Set<UnitConfig> connectionsToRemove = new HashSet<>();

        for (String connectionId : tileConfig.getConnectionIdList()) {
            UnitConfig connectionUnitConfig = connectionUnitConfigRegistry.getMessage(connectionId);
            if (connectionUnitConfig.getConnectionConfig().getTileIdCount() < 3) {
                connectionsToRemove.add(connectionUnitConfig);
            }
        }

        return connectionsToRemove;
    }
}
