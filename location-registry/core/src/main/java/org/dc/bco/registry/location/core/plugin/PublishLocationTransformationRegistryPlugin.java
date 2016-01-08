/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dc.bco.registry.location.core.plugin;

import org.dc.jul.exception.CouldNotPerformException;
import org.dc.jul.exception.NotAvailableException;
import org.dc.jul.exception.printer.ExceptionPrinter;
import org.dc.jul.exception.printer.LogLevel;
/**
 *
 * @author <a href="mailto:mpohling@cit-ec.uni-bielefeld.de">Divine Threepwood</a>
 */
import org.dc.jul.extension.protobuf.IdentifiableMessage;
import org.dc.jul.extension.rct.transform.PoseTransformer;
import org.dc.jul.storage.registry.Registry;
import org.dc.jul.storage.registry.plugin.FileRegistryPluginAdapter;
import org.dc.bco.registry.location.core.LocationRegistryLauncher;
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

    public PublishLocationTransformationRegistryPlugin() throws org.dc.jul.exception.InstantiationException {
        try {
            this.transformerFactory = TransformerFactory.getInstance();
            this.transformPublisher = transformerFactory.createTransformPublisher(LocationRegistryLauncher.APP_NAME);
        } catch (Exception ex) {
            throw new org.dc.jul.exception.InstantiationException(this, ex);
        }
    }
    
    @Override
    public void init(Registry<String, IdentifiableMessage<String, LocationConfigType.LocationConfig, LocationConfigType.LocationConfig.Builder>, ?> registry) throws CouldNotPerformException {
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

                logger.debug("Publish " + locationConfig.getParentId() + " to " + locationConfig.getId());

                // Create the rct transform object with source and target frames
                Transform transformation = PoseTransformer.transform(locationConfig.getPosition(), locationConfig.getParentId(), locationConfig.getId());

                // Publish the transform object
                transformation.setAuthority(LocationRegistryLauncher.APP_NAME);
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
