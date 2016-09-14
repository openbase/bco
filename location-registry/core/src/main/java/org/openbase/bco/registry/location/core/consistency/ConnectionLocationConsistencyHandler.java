package org.openbase.bco.registry.location.core.consistency;

/*
 * #%L
 * REM LocationRegistryData Core
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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.extension.protobuf.IdentifiableMessage;
import org.openbase.jul.extension.protobuf.container.ProtoBufMessageMapInterface;
import org.openbase.jul.storage.registry.AbstractProtoBufRegistryConsistencyHandler;
import org.openbase.jul.storage.registry.EntryModification;
import org.openbase.jul.storage.registry.ProtoBufFileSynchronizedRegistry;
import org.openbase.jul.storage.registry.ProtoBufRegistryInterface;
import rst.spatial.ConnectionConfigType.ConnectionConfig;
import rst.spatial.LocationConfigType.LocationConfig;
import rst.spatial.LocationRegistryDataType.LocationRegistryData;
import rst.spatial.PlacementConfigType.PlacementConfig;

/**
 *
 * @author <a href="mailto:thuxohl@techfak.uni-bielefeld.com">Tamino Huxohl</a>
 */
public class ConnectionLocationConsistencyHandler extends AbstractProtoBufRegistryConsistencyHandler<String, ConnectionConfig, ConnectionConfig.Builder> {

    private final ProtoBufFileSynchronizedRegistry<String, LocationConfig, LocationConfig.Builder, LocationRegistryData.Builder> locationConfigRegistry;

    public ConnectionLocationConsistencyHandler(ProtoBufFileSynchronizedRegistry<String, LocationConfig, LocationConfig.Builder, LocationRegistryData.Builder> locationConfigRegistry) {
        this.locationConfigRegistry = locationConfigRegistry;
    }

    @Override
    public void processData(String id, IdentifiableMessage<String, ConnectionConfig, ConnectionConfig.Builder> entry, ProtoBufMessageMapInterface<String, ConnectionConfig, ConnectionConfig.Builder> entryMap, ProtoBufRegistryInterface<String, ConnectionConfig, ConnectionConfig.Builder> registry) throws CouldNotPerformException, EntryModification {
        ConnectionConfig connection = entry.getMessage();

        String locationId;
        try {
            locationId = getLowestCommonParentLocation(connection.getTileIdList(), locationConfigRegistry).getId();
        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not find parent location for connection [" + connection + "]", ex);
        }
        if (!locationId.equals(connection.getPlacementConfig().getLocationId())) {
            PlacementConfig.Builder placement = connection.getPlacementConfig().toBuilder().setLocationId(locationId);
            throw new EntryModification(entry.setMessage(connection.toBuilder().setPlacementConfig(placement).build()), this);
        }
    }

    public static LocationConfig getLowestCommonParentLocation(List<String> locationIds, ProtoBufFileSynchronizedRegistry<String, LocationConfig, LocationConfig.Builder, LocationRegistryData.Builder> locationConfigRegistry) throws CouldNotPerformException {
        // list containing the pathes from root to each location given by locationIds sorted by the lenght of the path, e.g.:
        //      home, apartment, hallway, entrance
        //      home, apartment, outdoor
        List<List<LocationConfig>> pathesFromRootMap = new ArrayList<>();

        // fill the list according to the description above
        for (String id : locationIds) {
            LocationConfig location = locationConfigRegistry.getMessage(id);
            List<LocationConfig> pathFromRootList = new ArrayList<>();
            pathFromRootList.add(location);
            while (!location.getRoot()) {
                location = locationConfigRegistry.getMessage(location.getPlacementConfig().getLocationId());
                // when adding a location at the front of the list, every entry is moved an index further
                pathFromRootList.add(0, location);
            }
            pathesFromRootMap.add(pathFromRootList);
        }

        // sort the list after their sizes:
        //      home, apartment, outdoor
        //      home, apartment, hallway, entrance
        pathesFromRootMap.sort(new Comparator<List<LocationConfig>>() {

            @Override
            public int compare(List<LocationConfig> o1, List<LocationConfig> o2) {
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
