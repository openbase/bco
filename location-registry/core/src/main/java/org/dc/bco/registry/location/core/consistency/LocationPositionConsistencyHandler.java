/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dc.bco.registry.location.core.consistency;

import org.dc.jul.exception.CouldNotPerformException;
import org.dc.jul.exception.NotAvailableException;
import org.dc.jul.extension.protobuf.IdentifiableMessage;
import org.dc.jul.extension.protobuf.container.ProtoBufMessageMapInterface;
import org.dc.jul.storage.registry.AbstractProtoBufRegistryConsistencyHandler;
import org.dc.jul.storage.registry.EntryModification;
import org.dc.jul.storage.registry.ProtoBufRegistryInterface;
import rst.geometry.PoseType.Pose;
import rst.geometry.RotationType.Rotation;
import rst.geometry.TranslationType.Translation;
import rst.spatial.LocationConfigType;
import rst.spatial.LocationConfigType.LocationConfig;

/**
 *
 * @author mpohling
 */
public class LocationPositionConsistencyHandler extends AbstractProtoBufRegistryConsistencyHandler<String, LocationConfig, LocationConfig.Builder> {

    @Override
    public void processData(String id, IdentifiableMessage<String, LocationConfig, LocationConfig.Builder> entry, ProtoBufMessageMapInterface<String, LocationConfig, LocationConfig.Builder> entryMap, ProtoBufRegistryInterface<String, LocationConfig, LocationConfig.Builder> registry) throws CouldNotPerformException, EntryModification {
        LocationConfigType.LocationConfig locationConfig = entry.getMessage();

        if(!locationConfig.hasPlacementConfig()) {
            throw new NotAvailableException("locationconfig.placementconfig");
        }

        // setup position
        if (!locationConfig.getPlacementConfig().hasPosition()) {
            entry.setMessage(locationConfig.toBuilder().setPlacementConfig(locationConfig.getPlacementConfig().toBuilder().setPosition(generateDefaultInstance())));
            throw new EntryModification(entry, this);
        }
    }

    private Pose generateDefaultInstance() {
        return Pose.newBuilder()
                .setRotation(Rotation.newBuilder().setQw(1).setQx(0).setQy(0).setQz(0))
                .setTranslation(Translation.newBuilder().setX(0).setY(0).setZ(0)).build();
    }
}
