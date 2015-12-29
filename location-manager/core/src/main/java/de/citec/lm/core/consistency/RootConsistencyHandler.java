/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.lm.core.consistency;

import org.dc.jul.exception.CouldNotPerformException;
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
public class RootConsistencyHandler extends AbstractProtoBufRegistryConsistencyHandler<String, LocationConfigType.LocationConfig, LocationConfigType.LocationConfig.Builder> {

    @Override
    public void processData(String id, IdentifiableMessage<String, LocationConfig, LocationConfig.Builder> entry, ProtoBufMessageMapInterface<String, LocationConfig, LocationConfig.Builder> entryMap, ProtoBufRegistryInterface<String, LocationConfig, LocationConfig.Builder> registry) throws CouldNotPerformException, EntryModification {
        LocationConfigType.LocationConfig locationConfig = entry.getMessage();

        // check if root flag is set for child node.
        if (locationConfig.hasParentId() &&  locationConfig.getRoot()) {
            entry.setMessage(locationConfig.toBuilder().setRoot(false).build());
            throw new EntryModification(entry, this);
        }

        // check if root flag is missing for root node.
        if (!locationConfig.hasParentId() && !locationConfig.getRoot()) {
            entry.setMessage(locationConfig.toBuilder().setRoot(true).build());
            throw new EntryModification(entry, this);
        }
    }

    @Override
    public void reset() {
    }
}
