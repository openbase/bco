/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dc.bco.registry.location.core.plugin;

import org.dc.bco.registry.location.core.LocationRegistryLauncher;
import org.dc.jul.exception.CouldNotPerformException;
import org.dc.jul.exception.NotAvailableException;
import org.dc.jul.exception.printer.ExceptionPrinter;
import org.dc.jul.exception.printer.LogLevel;
import org.dc.jul.extension.protobuf.IdentifiableMessage;
import org.dc.jul.extension.rct.transform.PoseTransformer;
import org.dc.jul.storage.registry.Registry;
import org.dc.jul.storage.registry.plugin.FileRegistryPluginAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rct.Transform;
import rct.TransformPublisher;
import rct.TransformType;
import rct.TransformerException;
import rct.TransformerFactory;
import rst.spatial.LocationConfigType.LocationConfig;

public class PublishLocationTransformationRegistryPlugin extends FileRegistryPluginAdapter<String, IdentifiableMessage<String, LocationConfig, LocationConfig.Builder>> {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    private TransformerFactory transformerFactory;
    private TransformPublisher transformPublisher;

    private Registry<String, IdentifiableMessage<String, LocationConfig, LocationConfig.Builder>, ?> registry;

    public PublishLocationTransformationRegistryPlugin() throws org.dc.jul.exception.InstantiationException {
        try {
            this.transformerFactory = TransformerFactory.getInstance();
            this.transformPublisher = transformerFactory.createTransformPublisher(LocationRegistryLauncher.APP_NAME);
        } catch (Exception ex) {
            throw new org.dc.jul.exception.InstantiationException(this, ex);
        }
    }

    @Override
    public void init(final Registry<String, IdentifiableMessage<String, LocationConfig, LocationConfig.Builder>, ?> registry) throws CouldNotPerformException {
        this.registry = registry;
        for (IdentifiableMessage<String, LocationConfig, LocationConfig.Builder> entry : registry.getEntries()) {
            publishtransformation(entry);
        }
    }

    public void publishtransformation(final IdentifiableMessage<String, LocationConfig, LocationConfig.Builder> entry) {
        try {
            LocationConfig locationConfig = entry.getMessage();

            if (!locationConfig.getRoot() && locationConfig.hasPosition()) {

                if (!locationConfig.hasId()) {
                    throw new NotAvailableException("locationconfig.id");
                }

                if (!locationConfig.hasPlacementConfig()) {
                    throw new NotAvailableException("locationconfig.placementconfig");
                }

                if (!locationConfig.getPlacementConfig().hasPosition()) {
                    throw new NotAvailableException("locationconfig.placementconfig.position");
                }

                if (!locationConfig.getPlacementConfig().hasTransformationFrameId() || locationConfig.getPlacementConfig().getTransformationFrameId().isEmpty()) {
                    throw new NotAvailableException("locationconfig.placementconfig.transformationframeid");
                }

                if (!locationConfig.getPlacementConfig().hasLocationId() || locationConfig.getPlacementConfig().getLocationId().isEmpty()) {
                    throw new NotAvailableException("locationconfig.placementconfig.locationid");
                }

                logger.debug("Publish " + locationConfig.getPlacementConfig().getLocationId() + " to " + locationConfig.getId());

                // Create the rct transform object with source and target frames
                Transform transformation = PoseTransformer.transform(locationConfig.getPosition(), registry.get(locationConfig.getPlacementConfig().getLocationId()).getMessage().getPlacementConfig().getTransformationFrameId(), locationConfig.getPlacementConfig().getTransformationFrameId());

                // Publish the transform object
                transformation.setAuthority(LocationRegistryLauncher.APP_NAME);
                transformPublisher.sendTransform(transformation, TransformType.STATIC);
            }
        } catch (CouldNotPerformException | TransformerException ex) {
            ExceptionPrinter.printHistory(new CouldNotPerformException("Could not publish transformation of " + entry + "!", ex), logger, LogLevel.ERROR);
        }
    }

    @Override
    public void afterRegister(final IdentifiableMessage<String, LocationConfig, LocationConfig.Builder> entry) {
        publishtransformation(entry);
    }

    @Override
    public void afterUpdate(final IdentifiableMessage<String, LocationConfig, LocationConfig.Builder> entry) throws CouldNotPerformException {
        publishtransformation(entry);
    }
}
