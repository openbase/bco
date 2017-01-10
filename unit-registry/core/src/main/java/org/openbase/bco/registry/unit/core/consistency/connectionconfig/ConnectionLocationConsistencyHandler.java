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
import java.util.Comparator;
import java.util.List;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.extension.protobuf.IdentifiableMessage;
import org.openbase.jul.storage.registry.AbstractProtoBufRegistryConsistencyHandler;
import org.openbase.jul.storage.registry.EntryModification;
import org.openbase.jul.storage.registry.ProtoBufFileSynchronizedRegistry;
import rst.spatial.PlacementConfigType.PlacementConfig;
import org.openbase.jul.extension.protobuf.container.ProtoBufMessageMap;
import org.openbase.jul.storage.registry.ProtoBufRegistry;
import rst.domotic.unit.UnitConfigType.UnitConfig;
import rst.domotic.registry.UnitRegistryDataType.UnitRegistryData;

/**
 *
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class ConnectionLocationConsistencyHandler extends AbstractProtoBufRegistryConsistencyHandler<String, UnitConfig, UnitConfig.Builder> {

    private final ProtoBufFileSynchronizedRegistry<String, UnitConfig, UnitConfig.Builder, UnitRegistryData.Builder> locationRegistry;

    public ConnectionLocationConsistencyHandler(ProtoBufFileSynchronizedRegistry<String, UnitConfig, UnitConfig.Builder, UnitRegistryData.Builder> locationRegistry) {
        this.locationRegistry = locationRegistry;
    }

    @Override
    public void processData(String id, IdentifiableMessage<String, UnitConfig, UnitConfig.Builder> entry, ProtoBufMessageMap<String, UnitConfig, UnitConfig.Builder> entryMap, ProtoBufRegistry<String, UnitConfig, UnitConfig.Builder> registry) throws CouldNotPerformException, EntryModification {
        UnitConfig.Builder connectionUnitConfig = entry.getMessage().toBuilder();

        String locationId;
        try {
            locationId = getLowestCommonParentLocation(connectionUnitConfig.getConnectionConfig().getTileIdList(), locationRegistry).getId();
        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not find parent location for connection [" + connectionUnitConfig + "]", ex);
        }
        if (!locationId.equals(connectionUnitConfig.getPlacementConfig().getLocationId())) {
            PlacementConfig.Builder placement = connectionUnitConfig.getPlacementConfig().toBuilder().setLocationId(locationId);
            throw new EntryModification(entry.setMessage(connectionUnitConfig.setPlacementConfig(placement)), this);
        }
    }

    public static UnitConfig getLowestCommonParentLocation(List<String> locationIds, ProtoBufFileSynchronizedRegistry<String, UnitConfig, UnitConfig.Builder, UnitRegistryData.Builder> locationUnitConfigRegistry) throws CouldNotPerformException {
        // list containing the pathes from root to each location given by locationIds sorted by the lenght of the path, e.g.:
        //      home, apartment, hallway, entrance
        //      home, apartment, outdoor
        final List<List<UnitConfig>> pathesFromRootMap = new ArrayList<>();

        // fill the list according to the description above
        for (String id : locationIds) {
            UnitConfig locationUnitConfig = locationUnitConfigRegistry.getMessage(id);
            final List<UnitConfig> pathFromRootList = new ArrayList<>();
            pathFromRootList.add(locationUnitConfig);
            while (!locationUnitConfig.getLocationConfig().getRoot()) {
                locationUnitConfig = locationUnitConfigRegistry.getMessage(locationUnitConfig.getPlacementConfig().getLocationId());
                // when adding a location at the front of the list, every entry is moved an index further
                pathFromRootList.add(0, locationUnitConfig);
            }
            pathesFromRootMap.add(pathFromRootList);
        }

        // sort the list after their sizes:
        //      home, apartment, outdoor
        //      home, apartment, hallway, entrance
        pathesFromRootMap.sort(new Comparator<List<UnitConfig>>() {

            @Override
            public int compare(List<UnitConfig> o1, List<UnitConfig> o2) {
                return o2.size() - o1.size();
            }
        });

        // find the lowest common parent, e.g. for the example above apartment
        // by returning the index before the first elements where the pathes differ
        int shortestPath = pathesFromRootMap.get(0).size();
        for (int i = 0; i < shortestPath; ++i) {
            String currentId = pathesFromRootMap.get(0).get(i).getId();
            for (int j = 1; j < pathesFromRootMap.size(); ++j) {
                if (!pathesFromRootMap.get(j).get(i).getId().equals(currentId)) {
                    return pathesFromRootMap.get(0).get(i - 1);
                }
            }
        }

        // checking if a lowst common parent exists should not be necessary since a tile cannot be root
        return pathesFromRootMap.get(0).get(0);
    }

    @Override
    public void reset() {
    }
}
