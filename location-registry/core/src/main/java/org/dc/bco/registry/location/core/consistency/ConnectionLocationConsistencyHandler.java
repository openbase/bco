/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dc.bco.registry.location.core.consistency;

import java.util.ArrayList;
import java.util.List;
import org.dc.jul.exception.CouldNotPerformException;
import org.dc.jul.extension.protobuf.IdentifiableMessage;
import org.dc.jul.extension.protobuf.container.ProtoBufMessageMapInterface;
import org.dc.jul.storage.registry.AbstractProtoBufRegistryConsistencyHandler;
import org.dc.jul.storage.registry.EntryModification;
import org.dc.jul.storage.registry.ProtoBufFileSynchronizedRegistry;
import org.dc.jul.storage.registry.ProtoBufRegistryInterface;
import rst.spatial.ConnectionConfigType.ConnectionConfig;
import rst.spatial.LocationConfigType.LocationConfig;
import rst.spatial.LocationRegistryType.LocationRegistry;
import rst.spatial.PlacementConfigType.PlacementConfig;

/**
 *
 * @author <a href="mailto:thuxohl@techfak.uni-bielefeld.com">Tamino Huxohl</a>
 */
public class ConnectionLocationConsistencyHandler extends AbstractProtoBufRegistryConsistencyHandler<String, ConnectionConfig, ConnectionConfig.Builder> {

    private final ProtoBufFileSynchronizedRegistry<String, LocationConfig, LocationConfig.Builder, LocationRegistry.Builder> locationConfigRegistry;

    public ConnectionLocationConsistencyHandler(ProtoBufFileSynchronizedRegistry<String, LocationConfig, LocationConfig.Builder, LocationRegistry.Builder> locationConfigRegistry) {
        this.locationConfigRegistry = locationConfigRegistry;
    }

    @Override
    public void processData(String id, IdentifiableMessage<String, ConnectionConfig, ConnectionConfig.Builder> entry, ProtoBufMessageMapInterface<String, ConnectionConfig, ConnectionConfig.Builder> entryMap, ProtoBufRegistryInterface<String, ConnectionConfig, ConnectionConfig.Builder> registry) throws CouldNotPerformException, EntryModification {
        ConnectionConfig connection = entry.getMessage();

        String locationId = getLowestCommonParentLocation(connection.getTileIdList(), locationConfigRegistry).getId();
        if (!locationId.equals(connection.getPlacementConfig().getLocationId())) {
            PlacementConfig.Builder placement = connection.getPlacementConfig().toBuilder().setLocationId(locationId);
            throw new EntryModification(entry.setMessage(connection.toBuilder().setPlacementConfig(placement).build()), this);
        }
    }

    private LocationConfig getLowestCommonParentLocation(List<String> locationIds, ProtoBufFileSynchronizedRegistry<String, LocationConfig, LocationConfig.Builder, LocationRegistry.Builder> locationConfigRegistry) throws CouldNotPerformException {
        // list that contains a list of location configs from the root location to one of the given locations in order
        List<List<LocationConfig>> pathToRootLists = new ArrayList<>();
        // the size of the shortest list with a path from root to parent
        int shortestList = Integer.MAX_VALUE;
        // the index of that shortest list
        int shortestIndex = 0;
        for (String id : locationIds) {
            LocationConfig location = locationConfigRegistry.getMessage(id);
            pathToRootLists.add(new ArrayList<>());
            while (!location.getParentId().isEmpty()) {
                location = locationConfigRegistry.getMessage(location.getParentId());
                pathToRootLists.get(pathToRootLists.size() - 1).add(0, location);
            }
            if (pathToRootLists.get(pathToRootLists.size() - 1).size() < shortestList) {
                shortestList = pathToRootLists.get(pathToRootLists.size() - 1).size();
                shortestIndex = pathToRootLists.size() - 1;
            }
        }

        if (shortestList == 0) {
            return null;
        }

        for (int i = 0; i < shortestList; i++) {
            for (int j = 0; j < pathToRootLists.size() - 1; j++) {
                if (!pathToRootLists.get(j).get(i).equals(pathToRootLists.get(j + 1).get(i))) {
                    if (i - 1 < 0) {
                        throw new CouldNotPerformException("");
                    }
                    return pathToRootLists.get(j).get(i - 1);
                }
            }
        }
        return pathToRootLists.get(shortestIndex).get(pathToRootLists.get(shortestIndex).size() - 1);
    }

    @Override
    public void reset() {
    }
}
