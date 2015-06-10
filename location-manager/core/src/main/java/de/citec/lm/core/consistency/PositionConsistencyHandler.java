/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.lm.core.consistency;

import de.citec.jul.exception.CouldNotPerformException;
import de.citec.jul.extension.rsb.container.IdentifiableMessage;
import de.citec.jul.extension.rsb.container.ProtoBufMessageMapInterface;
import de.citec.jul.storage.registry.EntryModification;
import de.citec.jul.storage.registry.ProtoBufRegistryConsistencyHandler;
import de.citec.jul.storage.registry.ProtoBufRegistryInterface;
import rst.geometry.PoseType;
import rst.geometry.PoseType.Pose;
import rst.geometry.RotationType.Rotation;
import rst.geometry.TranslationType.Translation;
import rst.spatial.LocationConfigType;
import rst.spatial.LocationConfigType.LocationConfig;

/**
 *
 * @author mpohling
 */
public class PositionConsistencyHandler implements ProtoBufRegistryConsistencyHandler<String, LocationConfig, LocationConfig.Builder> {

    @Override
    public void processData(String id, IdentifiableMessage<String, LocationConfig, LocationConfig.Builder> entry, ProtoBufMessageMapInterface<String, LocationConfig, LocationConfig.Builder> entryMap, ProtoBufRegistryInterface<String, LocationConfig, LocationConfig.Builder> registry) throws CouldNotPerformException, EntryModification {
        LocationConfigType.LocationConfig locationConfig = entry.getMessage();

        // setup position
        if (!locationConfig.hasPosition()) {
            entry.setMessage(locationConfig.toBuilder().setPosition(generateDefaultInstance()));
            throw new EntryModification(entry, this);
        }
    }

    private Pose generateDefaultInstance() {
        return Pose.newBuilder()
                .setRotation(Rotation.newBuilder().setQw(1).setQx(0).setQy(0).setQz(0))
                .setTranslation(Translation.newBuilder().setX(0).setY(0).setZ(0)).build();
    }

    @Override
    public void reset() {
    }
}
