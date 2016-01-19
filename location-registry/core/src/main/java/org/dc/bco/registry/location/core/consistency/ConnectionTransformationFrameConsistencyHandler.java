/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dc.bco.registry.location.core.consistency;

import de.citec.jul.extension.rst.storage.registry.consistency.AbstractTransformationFrameConsistencyHandler;
import org.dc.jul.exception.CouldNotPerformException;
import org.dc.jul.extension.protobuf.IdentifiableMessage;
import org.dc.jul.extension.protobuf.container.ProtoBufMessageMapInterface;
import org.dc.jul.storage.registry.EntryModification;
import org.dc.jul.storage.registry.ProtoBufRegistryInterface;
import rst.spatial.ConnectionConfigType.ConnectionConfig;
import rst.spatial.LocationConfigType;
import rst.spatial.PlacementConfigType.PlacementConfig;

/**
 *
 * @author mpohling
 */
public class ConnectionTransformationFrameConsistencyHandler extends AbstractTransformationFrameConsistencyHandler<String, ConnectionConfig, ConnectionConfig.Builder> {

    public ConnectionTransformationFrameConsistencyHandler(final ProtoBufRegistryInterface<String, LocationConfigType.LocationConfig, LocationConfigType.LocationConfig.Builder> locationRegistry) {
        super(locationRegistry);
    }

    @Override
    public void processData(final String id, final IdentifiableMessage<String, ConnectionConfig, ConnectionConfig.Builder> entry, final ProtoBufMessageMapInterface<String, ConnectionConfig, ConnectionConfig.Builder> entryMap, final ProtoBufRegistryInterface<String, ConnectionConfig, ConnectionConfig.Builder> registry) throws CouldNotPerformException, EntryModification {
        ConnectionConfig connectionConfig = entry.getMessage();
        PlacementConfig placementConfig = verifyAndUpdatePlacement(connectionConfig.getLabel(), connectionConfig.getPlacementConfig());

        if(placementConfig != null) {
            entry.setMessage(connectionConfig.toBuilder().setPlacementConfig(placementConfig));
            throw new EntryModification(entry, this);
        }
    }
}
