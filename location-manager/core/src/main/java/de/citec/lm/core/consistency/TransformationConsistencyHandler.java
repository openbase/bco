/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.lm.core.consistency;

import de.citec.lm.core.LocationManager;
import de.citec.jul.exception.CouldNotPerformException;
import de.citec.jul.exception.ExceptionPrinter;
import de.citec.jul.exception.InstantiationException;
import de.citec.jul.exception.NotAvailableException;
import de.citec.jul.extension.rsb.container.IdentifiableMessage;
import de.citec.jul.extension.rsb.container.ProtoBufMessageMapInterface;
import de.citec.jul.storage.registry.EntryModification;
import de.citec.jul.storage.registry.ProtoBufRegistryConsistencyHandler;
import de.citec.jul.storage.registry.ProtoBufRegistryInterface;
import javax.media.j3d.Transform3D;
import javax.vecmath.Quat4d;
import javax.vecmath.Vector3d;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rct.Transform;
import rct.TransformPublisher;
import rct.TransformType;
import rct.TransformerFactory;
import rst.geometry.PoseType;
import rst.geometry.RotationType;
import rst.geometry.TranslationType;
import rst.spatial.LocationConfigType;
import rst.spatial.LocationConfigType.LocationConfig;

/**
 *
 * @author mpohling
 */
public class TransformationConsistencyHandler implements ProtoBufRegistryConsistencyHandler<String, LocationConfig, LocationConfig.Builder> {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    private TransformerFactory transformerFactory;
    private TransformPublisher transformPublisher;

    public TransformationConsistencyHandler() throws InstantiationException {
        try {
            this.transformerFactory = TransformerFactory.getInstance();
            this.transformPublisher = transformerFactory.createTransformPublisher(LocationManager.APP_NAME);
        } catch (Exception ex) {
            throw new de.citec.jul.exception.InstantiationException(this, ex);
        }
    }

    @Override
    public void processData(String id, IdentifiableMessage<String, LocationConfig, LocationConfig.Builder> entry, ProtoBufMessageMapInterface<String, LocationConfig, LocationConfig.Builder> entryMap, ProtoBufRegistryInterface<String, LocationConfig, LocationConfig.Builder> registry) throws CouldNotPerformException, EntryModification {
        LocationConfigType.LocationConfig locationConfig = entry.getMessage();

        if (!locationConfig.getRoot() && locationConfig.hasPosition()) {

            if (!locationConfig.hasId()) {
                throw new NotAvailableException("locationconfig.id");
            }

            if (!locationConfig.hasParentId()) {
                throw new NotAvailableException("locationconfig.parentid");
            }

            // Create the rct transform object with source and target frames
            Transform transformation = transform(locationConfig.getPosition(), locationConfig.getParentId(), locationConfig.getId());

            try {
                // Publish the transform object
                transformation.setAuthority(LocationManager.APP_NAME);
                transformPublisher.sendTransform(transformation, TransformType.STATIC);
            } catch (Exception ex) {
                ExceptionPrinter.printHistory(logger, new CouldNotPerformException("Could not publish transformation of " + entry + "!", ex));
            }
        }
    }

    //TODO mpohling: Should be moved into own jul transformer lib.
    public Transform transform(final PoseType.Pose position, String frameParent, String frameChild) {
        RotationType.Rotation pRotation = position.getRotation();
        TranslationType.Translation pTranslation = position.getTranslation();
        Quat4d jRotation = new Quat4d(pRotation.getQx(), pRotation.getQy(), pRotation.getQz(), pRotation.getQw());
        Vector3d jTranslation = new Vector3d(pTranslation.getX(), pTranslation.getY(), pTranslation.getZ());
        Transform3D transform = new Transform3D(jRotation, jTranslation, 1.0);
        return new Transform(transform, frameParent, frameChild, System.currentTimeMillis());
    }

    @Override
    public void reset() {
    }
}
