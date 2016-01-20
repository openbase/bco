/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dc.bco.registry.location.core.consistency;

import org.dc.jul.exception.CouldNotPerformException;
import org.dc.jul.extension.protobuf.IdentifiableMessage;
import org.dc.jul.extension.protobuf.container.ProtoBufMessageMapInterface;
import org.dc.jul.storage.registry.AbstractProtoBufRegistryConsistencyHandler;
import org.dc.jul.storage.registry.EntryModification;
import org.dc.jul.storage.registry.ProtoBufRegistryInterface;
import rst.spatial.LocationConfigType;
import rst.spatial.LocationConfigType.LocationConfig;
import rst.spatial.PlacementConfigType;

/**
 *
 * @author mpohling
 */
public class RootConsistencyHandler extends AbstractProtoBufRegistryConsistencyHandler<String, LocationConfigType.LocationConfig, LocationConfigType.LocationConfig.Builder> {

    @Override
    public void processData(String id, IdentifiableMessage<String, LocationConfig, LocationConfig.Builder> entry, ProtoBufMessageMapInterface<String, LocationConfig, LocationConfig.Builder> entryMap, ProtoBufRegistryInterface<String, LocationConfig, LocationConfig.Builder> registry) throws CouldNotPerformException, EntryModification {
        LocationConfig.Builder locationConfig = entry.getMessage().toBuilder();

        // check if placement exists.
        if (!locationConfig.hasPlacementConfig()) {
            locationConfig.setPlacementConfig(PlacementConfigType.PlacementConfig.newBuilder());
        }

        // check if root flag is set for child node.
        if (locationConfig.getPlacementConfig().hasLocationId() && !locationConfig.getPlacementConfig().getLocationId().equals(locationConfig.getId()) && locationConfig.getRoot()) {
            entry.setMessage(locationConfig.setRoot(false).build());
            throw new EntryModification(entry, this);
        }

        // check if root flag is missing for root node.
        if (locationConfig.getPlacementConfig().hasLocationId() && locationConfig.getPlacementConfig().getLocationId().equals(locationConfig.getId()) && !locationConfig.getRoot()) {
            entry.setMessage(locationConfig.setRoot(true).setPlacementConfig(locationConfig.getPlacementConfig().toBuilder()));
            throw new EntryModification(entry, this);
        }
    }

    @Override
    public void reset() {
    }
}
