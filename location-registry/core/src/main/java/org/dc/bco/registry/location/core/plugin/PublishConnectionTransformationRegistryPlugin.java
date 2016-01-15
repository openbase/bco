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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rct.Transform;
import rct.TransformPublisher;
import rct.TransformType;
import rct.TransformerException;
import rct.TransformerFactory;
import rst.spatial.ConnectionConfigType.ConnectionConfig;

public class PublishConnectionTransformationRegistryPlugin extends FileRegistryPluginAdapter<String, IdentifiableMessage<String, ConnectionConfig, ConnectionConfig.Builder>> {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    private TransformerFactory transformerFactory;
    private TransformPublisher transformPublisher;

    public PublishConnectionTransformationRegistryPlugin() throws org.dc.jul.exception.InstantiationException {
        try {
            this.transformerFactory = TransformerFactory.getInstance();
            this.transformPublisher = transformerFactory.createTransformPublisher(PublishConnectionTransformationRegistryPlugin.class.getSimpleName());
        } catch (Exception ex) {
            throw new org.dc.jul.exception.InstantiationException(this, ex);
        }
    }
    
    @Override
    public void init(Registry<String, IdentifiableMessage<String, ConnectionConfig, ConnectionConfig.Builder>, ?> registry) throws CouldNotPerformException {
        for (IdentifiableMessage<String, ConnectionConfig, ConnectionConfig.Builder> entry : registry.getEntries()) {
            publishtransformation(entry);
        }
    }

    public void publishtransformation(IdentifiableMessage<String, ConnectionConfig, ConnectionConfig.Builder> entry) {
        try {
            ConnectionConfig connectionConfig = entry.getMessage();

            if (connectionConfig.hasPlacement() && connectionConfig.getPlacement().hasPosition()) {

                if (!connectionConfig.hasId()) {
                    throw new NotAvailableException("connectionconfig.id");
                }

                if (!connectionConfig.getPlacement().hasLocationId()) {
                    throw new NotAvailableException("connectionconfig.placement.locationid");
                }

                logger.debug("Publish " + connectionConfig.getPlacement().getLocationId()+ " to " + connectionConfig.getId());

                // Create the rct transform object with source and target frames
                Transform transformation = PoseTransformer.transform(connectionConfig.getPlacement().getPosition(), connectionConfig.getPlacement().getLocationId(), connectionConfig.getId());

                // Publish the transform object
                transformation.setAuthority(PublishConnectionTransformationRegistryPlugin.class.getSimpleName());
                transformPublisher.sendTransform(transformation, TransformType.STATIC);
            }
        } catch (NotAvailableException | TransformerException ex) {
            ExceptionPrinter.printHistory(new CouldNotPerformException("Could not publish transformation of " + entry + "!", ex), logger, LogLevel.ERROR);
        }
    }
    
    @Override
    public void afterRegister(IdentifiableMessage<String, ConnectionConfig, ConnectionConfig.Builder> entry) {
        publishtransformation(entry);
    }

    @Override
    public void afterUpdate(IdentifiableMessage<String, ConnectionConfig, ConnectionConfig.Builder> entry) throws CouldNotPerformException {
        publishtransformation(entry);
    }
}
