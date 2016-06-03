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
import java.util.List;
import org.dc.jul.exception.CouldNotPerformException;
import org.dc.jul.exception.NotAvailableException;
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

    //TODO tamino: please reimplement more intuitive. May with a treemap where the key is the distance and the value the path.
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
            while (!location.getRoot()) {
                location = locationConfigRegistry.getMessage(location.getPlacementConfig().getLocationId());
                pathToRootLists.get(pathToRootLists.size() - 1).add(0, location);
            }
            if (pathToRootLists.get(pathToRootLists.size() - 1).size() < shortestList) {
                shortestList = pathToRootLists.get(pathToRootLists.size() - 1).size();
                shortestIndex = pathToRootLists.size() - 1;
            }
        }

        if (shortestList == 0) {
            throw new NotAvailableException("LowestCommonParentLocation");
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
