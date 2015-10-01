/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.lm.core.plugin;

import de.citec.jul.exception.CouldNotPerformException;
import de.citec.jul.exception.NotAvailableException;
import de.citec.jul.exception.printer.ExceptionPrinter;
import de.citec.jul.exception.printer.LogLevel;
import de.citec.jul.extension.protobuf.IdentifiableMessage;
import de.citec.jul.storage.registry.RegistryInterface;
import de.citec.jul.storage.registry.plugin.RegistryPlugin;
import de.citec.jul.storage.registry.plugin.RegistryPluginAdapter;
import de.citec.lm.core.LocationManager;
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

/**
 *
 * @author <a href="mailto:mpohling@cit-ec.uni-bielefeld.de">Divine Threepwood</a>
 */
public class RCTRegistryPlugin extends RegistryPluginAdapter<String, IdentifiableMessage<String, LocationConfigType.LocationConfig, LocationConfigType.LocationConfig.Builder>> {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    private TransformerFactory transformerFactory;
    private TransformPublisher transformPublisher;

    public RCTRegistryPlugin() throws de.citec.jul.exception.InstantiationException {
        try {
            this.transformerFactory = TransformerFactory.getInstance();
            this.transformPublisher = transformerFactory.createTransformPublisher(LocationManager.APP_NAME);
        } catch (Exception ex) {
            throw new de.citec.jul.exception.InstantiationException(this, ex);
        }
    }

    public void processData(IdentifiableMessage<String, LocationConfigType.LocationConfig, LocationConfigType.LocationConfig.Builder> entry) throws CouldNotPerformException {
        LocationConfigType.LocationConfig locationConfig = entry.getMessage();

        if (!locationConfig.getRoot() && locationConfig.hasPosition()) {

            if (!locationConfig.hasId()) {
                throw new NotAvailableException("locationconfig.id");
            }

            if (!locationConfig.hasParentId()) {
                throw new NotAvailableException("locationconfig.parentid");
            }

            logger.info("Publish " + locationConfig.getParentId() + " to " + locationConfig.getId());

            // Create the rct transform object with source and target frames
            Transform transformation = transform(locationConfig.getPosition(), locationConfig.getParentId(), locationConfig.getId());

            try {
                // Publish the transform object
                transformation.setAuthority(LocationManager.APP_NAME);
                transformPublisher.sendTransform(transformation, TransformType.STATIC);
            } catch (Exception ex) {
                ExceptionPrinter.printHistoryAndReturnThrowable(logger, new CouldNotPerformException("Could not publish transformation of " + entry + "!", ex));
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
    public void init(RegistryInterface<String, IdentifiableMessage<String, LocationConfigType.LocationConfig, LocationConfigType.LocationConfig.Builder>, ?> registry) throws CouldNotPerformException {
    }

    @Override
    public void beforeRegister(IdentifiableMessage<String, LocationConfigType.LocationConfig, LocationConfigType.LocationConfig.Builder> entry) {
    }

    @Override
    public void afterRegister(IdentifiableMessage<String, LocationConfigType.LocationConfig, LocationConfigType.LocationConfig.Builder> entry) {
        try {
            processData(entry);
        } catch (CouldNotPerformException ex) {
            ExceptionPrinter.printHistory(ex, logger, LogLevel.ERROR);
        }
    }


}
