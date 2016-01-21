/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dc.bco.registry.location.core.consistency;

import java.util.ArrayList;
import java.util.List;
import org.dc.jul.exception.CouldNotPerformException;
import org.dc.jul.exception.NotAvailableException;
import org.dc.jul.extension.protobuf.IdentifiableMessage;
import org.dc.jul.extension.protobuf.container.ProtoBufMessageMapInterface;
import org.dc.jul.storage.registry.AbstractProtoBufRegistryConsistencyHandler;
import org.dc.jul.storage.registry.EntryModification;
import org.dc.jul.storage.registry.ProtoBufRegistryInterface;
import rst.spatial.LocationConfigType.LocationConfig;

/**
 *
 * @author mpohling
 */
public class LocationChildConsistencyHandler extends AbstractProtoBufRegistryConsistencyHandler<String, LocationConfig, LocationConfig.Builder> {

    @Override
    public void processData(String id, IdentifiableMessage<String, LocationConfig, LocationConfig.Builder> entry, ProtoBufMessageMapInterface<String, LocationConfig, LocationConfig.Builder> entryMap, ProtoBufRegistryInterface<String, LocationConfig, LocationConfig.Builder> registry) throws CouldNotPerformException, EntryModification {

        LocationConfig.Builder locationConfig = entry.getMessage().toBuilder();

        // check if placement exists.
        if (!locationConfig.hasPlacementConfig()) {
            throw new NotAvailableException("locationconfig.placementconfig");
        }

        LocationConfig childLocationConfig;

        // check parent consistency
        for (String childLocationId : new ArrayList<>(locationConfig.getChildIdList())) {

            // check if given child is registered otherwise remove child.
            if (!registry.contains(childLocationId)) {
                logger.warn("Registered ChildLocation[" + childLocationId + "] for ParentLocation[" + locationConfig.getId() + "] does not exists.");
                List<String> childIds = new ArrayList<>(locationConfig.getChildIdList());
                childIds.remove(childLocationId);
                throw new EntryModification(entry.setMessage(locationConfig.clearChildId().addAllChildId(childIds).build()), this);
            }

//            // check if parent id is registered
//            if (!childLocationConfig.hasPlacementConfig() || locationConfig.getPlacementConfig().hasLocationId()) {
//                throw new EntryModification(child.setMessage(child.getMessage().toBuilder().setPlacementConfig(locationConfig.getPlacementConfig().toBuilder().setLocationId(locationConfig.getId()))), this);
//            }

            childLocationConfig = registry.getMessage(childLocationId);

            // check if parent id is valid.
            if (!childLocationConfig.getPlacementConfig().getLocationId().equals(locationConfig.getId())) {
                IdentifiableMessage<String, LocationConfig, LocationConfig.Builder> child = entryMap.get(childLocationConfig.getId());
                throw new EntryModification(child.setMessage(child.getMessage().toBuilder().setPlacementConfig(childLocationConfig.getPlacementConfig().toBuilder().setLocationId(locationConfig.getId()).build())), this);
            }
        }
    }
}
