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
/**
 *
 * @author <a href="mailto:mpohling@cit-ec.uni-bielefeld.de">Divine Threepwood</a>
 */
import de.citec.jul.extension.protobuf.IdentifiableMessage;
import de.citec.jul.extension.rct.transform.PoseTransformer;
import de.citec.jul.storage.registry.RegistryInterface;
import de.citec.jul.storage.registry.plugin.FileRegistryPluginAdapter;
import de.citec.lm.core.LocationManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rct.Transform;
import rct.TransformPublisher;
import rct.TransformType;
import rct.TransformerFactory;
import rst.spatial.LocationConfigType;

public class PublishLocationTransformationRegistryPlugin extends FileRegistryPluginAdapter<String, IdentifiableMessage<String, LocationConfigType.LocationConfig, LocationConfigType.LocationConfig.Builder>> {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    private TransformerFactory transformerFactory;
    private TransformPublisher transformPublisher;

    public PublishLocationTransformationRegistryPlugin() throws de.citec.jul.exception.InstantiationException {
        try {
            this.transformerFactory = TransformerFactory.getInstance();
            this.transformPublisher = transformerFactory.createTransformPublisher(LocationManager.APP_NAME);
        } catch (Exception ex) {
            throw new de.citec.jul.exception.InstantiationException(this, ex);
        }
    }
    
    @Override
    public void init(RegistryInterface<String, IdentifiableMessage<String, LocationConfigType.LocationConfig, LocationConfigType.LocationConfig.Builder>, ?> registry) throws CouldNotPerformException {
        for (IdentifiableMessage<String, LocationConfigType.LocationConfig, LocationConfigType.LocationConfig.Builder> entry : registry.getEntries()) {
            publishtransformation(entry);
        }
    }

    public void publishtransformation(IdentifiableMessage<String, LocationConfigType.LocationConfig, LocationConfigType.LocationConfig.Builder> entry) {
        try {
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
                Transform transformation = PoseTransformer.transform(locationConfig.getPosition(), locationConfig.getParentId(), locationConfig.getId());

                // Publish the transform object
                transformation.setAuthority(LocationManager.APP_NAME);
                transformPublisher.sendTransform(transformation, TransformType.STATIC);
            }
        } catch (Exception ex) {
            ExceptionPrinter.printHistory(new CouldNotPerformException("Could not publish transformation of " + entry + "!", ex), logger, LogLevel.ERROR);
        }
    }
    
    @Override
    public void afterRegister(IdentifiableMessage<String, LocationConfigType.LocationConfig, LocationConfigType.LocationConfig.Builder> entry) {
        publishtransformation(entry);
    }

    @Override
    public void afterUpdate(IdentifiableMessage<String, LocationConfigType.LocationConfig, LocationConfigType.LocationConfig.Builder> entry) throws CouldNotPerformException {
        publishtransformation(entry);
    }
}
