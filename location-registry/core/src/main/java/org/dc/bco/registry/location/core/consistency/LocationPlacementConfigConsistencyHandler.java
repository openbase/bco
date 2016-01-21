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
public class LocationPlacementConfigConsistencyHandler extends AbstractProtoBufRegistryConsistencyHandler<String, LocationConfigType.LocationConfig, LocationConfigType.LocationConfig.Builder> {

    @Override
    public void processData(String id, IdentifiableMessage<String, LocationConfig, LocationConfig.Builder> entry, ProtoBufMessageMapInterface<String, LocationConfig, LocationConfig.Builder> entryMap, ProtoBufRegistryInterface<String, LocationConfig, LocationConfig.Builder> registry) throws CouldNotPerformException, EntryModification {
        LocationConfig.Builder locationConfig = entry.getMessage().toBuilder();

        // check if placement exists, otherwise create.
        if (!locationConfig.hasPlacementConfig()) {
            throw new EntryModification(entry.setMessage(locationConfig.setPlacementConfig(PlacementConfigType.PlacementConfig.newBuilder())), this);
        }
    }
}
