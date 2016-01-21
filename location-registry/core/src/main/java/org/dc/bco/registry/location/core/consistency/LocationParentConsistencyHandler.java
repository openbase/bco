/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dc.bco.registry.location.core.consistency;

import java.util.Arrays;
import org.dc.jul.exception.CouldNotPerformException;
import org.dc.jul.exception.NotAvailableException;
import org.dc.jul.extension.protobuf.IdentifiableMessage;
import org.dc.jul.extension.protobuf.container.ProtoBufMessageMapInterface;
import org.dc.jul.storage.registry.AbstractProtoBufRegistryConsistencyHandler;
import org.dc.jul.storage.registry.EntryModification;
import org.dc.jul.storage.registry.ProtoBufRegistryInterface;
import rst.spatial.LocationConfigType;
import rst.spatial.LocationConfigType.LocationConfig;

/**
 *
 * @author mpohling
 */
public class LocationParentConsistencyHandler extends AbstractProtoBufRegistryConsistencyHandler<String, LocationConfig, LocationConfig.Builder> {

    @Override
    public void processData(String id, IdentifiableMessage<String, LocationConfig, LocationConfig.Builder> entry, ProtoBufMessageMapInterface<String, LocationConfig, LocationConfig.Builder> entryMap, ProtoBufRegistryInterface<String, LocationConfig, LocationConfig.Builder> registry) throws CouldNotPerformException, EntryModification {

        LocationConfig.Builder locationConfig = entry.getMessage().toBuilder();

        // check if placement exists.
        if (!locationConfig.hasPlacementConfig()) {
            throw new NotAvailableException("locationconfig.placementconfig");
        }

        // check children consistency
        if (!locationConfig.getPlacementConfig().hasLocationId()) {
            throw new NotAvailableException("locationconfig.placementconfig.locationid");
        }

        // skip root locations
        if(locationConfig.getRoot()) {
            return;
        }

        // check if parent is registered.
        if (!entryMap.containsKey(locationConfig.getPlacementConfig().getLocationId())) {
            logger.warn("Parent[" + locationConfig.getPlacementConfig().getLocationId() + "] of child[" + locationConfig.getId() + "] is unknown! Entry will moved to root location!");
            logger.info("known entries["+Arrays.toString(entryMap.getMessages().toArray())+"]");
            entry.setMessage(locationConfig.setPlacementConfig(locationConfig.getPlacementConfig().toBuilder().clearLocationId()));
//            entry.setMessage(locationConfig.setPlacementConfig(locationConfig.getPlacementConfig().toBuilder().setLocationId(locationConfigRegistry.getRootLocationConfig().getId())));
            throw new EntryModification(entry, this);
        }

        // check if parents knows given child.
        IdentifiableMessage<String, LocationConfigType.LocationConfig, LocationConfig.Builder> parent = registry.get(locationConfig.getPlacementConfig().getLocationId());
        if (parent != null && !parentHasChild(parent.getMessage(), locationConfig.build())) {
             logger.warn("Parent["+parent.getId()+"] does not know Child["+locationConfig.getId()+"]");
            parent.setMessage(parent.getMessage().toBuilder().addChildId(locationConfig.getId()));
            throw new EntryModification(parent, this);
        }
    }

    private boolean parentHasChild(LocationConfig parent, LocationConfig child) {
        return parent.getChildIdList().stream().anyMatch((children) -> (children.equals(child.getId())));
    }
}
