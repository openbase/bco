package org.openbase.bco.registry.unit.core.plugin;

/*
 * #%L
 * BCO Registry Unit Core
 * %%
 * Copyright (C) 2014 - 2017 openbase.org
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */
import java.util.ConcurrentModificationException;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InitializationException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.exception.printer.LogLevel;
import org.openbase.jul.extension.protobuf.IdentifiableMessage;
import org.openbase.jul.extension.rct.transform.PoseTransformer;
import org.openbase.jul.storage.registry.Registry;
import org.openbase.jul.storage.registry.plugin.FileRegistryPluginAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rct.Transform;
import rct.TransformPublisher;
import rct.TransformType;
import rct.TransformerException;
import rct.TransformerFactory;
import rst.domotic.unit.UnitConfigType.UnitConfig;

public class PublishConnectionTransformationRegistryPlugin extends FileRegistryPluginAdapter<String, IdentifiableMessage<String, UnitConfig, UnitConfig.Builder>> {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    private Registry<String, IdentifiableMessage<String, UnitConfig, UnitConfig.Builder>> registry;
    final Registry<String, IdentifiableMessage<String, UnitConfig, UnitConfig.Builder>> locationRegistry;

    private TransformerFactory transformerFactory;
    private TransformPublisher transformPublisher;

    public PublishConnectionTransformationRegistryPlugin(final Registry<String, IdentifiableMessage<String, UnitConfig, UnitConfig.Builder>> locationRegistry) throws org.openbase.jul.exception.InstantiationException {
        try {
            this.locationRegistry = locationRegistry;
            this.transformerFactory = TransformerFactory.getInstance();
            this.transformPublisher = transformerFactory.createTransformPublisher(getClass().getSimpleName());
        } catch (Exception ex) {
            throw new org.openbase.jul.exception.InstantiationException(this, ex);
        }
    }

    @Override
    public void init(Registry<String, IdentifiableMessage<String, UnitConfig, UnitConfig.Builder>> registry) throws InitializationException, InterruptedException {
        try {
            super.init(registry);
            this.transformPublisher = transformerFactory.createTransformPublisher(registry.getName());
            for (IdentifiableMessage<String, UnitConfig, UnitConfig.Builder> entry : registry.getEntries()) {
                publishTransformation(entry);
            }
        } catch (CouldNotPerformException | TransformerFactory.TransformerFactoryException ex) {
            throw new InitializationException(this, ex);
        }
    }

    public void publishTransformation(IdentifiableMessage<String, UnitConfig, UnitConfig.Builder> entry) {
        try {
            UnitConfig connectionConfig = entry.getMessage();

            if (connectionConfig.hasPlacementConfig() && connectionConfig.getPlacementConfig().hasPosition()) {

                if (!connectionConfig.hasId()) {
                    throw new NotAvailableException("connectionconfig.id");
                }

                if (!connectionConfig.hasPlacementConfig()) {
                    throw new NotAvailableException("connectionconfig.placement");
                }

                if (!connectionConfig.getPlacementConfig().hasPosition()) {
                    throw new NotAvailableException("connectionconfig.placement.position");
                }

                if (!connectionConfig.getPlacementConfig().hasTransformationFrameId() || connectionConfig.getPlacementConfig().getTransformationFrameId().isEmpty()) {
                    throw new NotAvailableException("connectionconfig.placement.transformationframeid");
                }

                if (!connectionConfig.getPlacementConfig().hasLocationId() || connectionConfig.getPlacementConfig().getLocationId().isEmpty()) {
                    throw new NotAvailableException("connectionconfig.placement.locationid");
                }

                logger.info("Publish " + locationRegistry.get(connectionConfig.getPlacementConfig().getLocationId()).getMessage().getPlacementConfig().getTransformationFrameId() + " to " + connectionConfig.getPlacementConfig().getTransformationFrameId());

                // Create the rct transform object with source and target frames
                Transform transformation = PoseTransformer.transform(connectionConfig.getPlacementConfig().getPosition(), locationRegistry.get(connectionConfig.getPlacementConfig().getLocationId()).getMessage().getPlacementConfig().getTransformationFrameId(), connectionConfig.getPlacementConfig().getTransformationFrameId());

                // Publish the transform object
                transformation.setAuthority(getRegistry().getName());
                transformPublisher.sendTransform(transformation, TransformType.STATIC);
            }
        } catch (NotAvailableException ex) {
            ExceptionPrinter.printHistory(new CouldNotPerformException("Could not publish transformation of " + entry + "!", ex), logger, LogLevel.DEBUG);
        } catch (CouldNotPerformException | TransformerException | ConcurrentModificationException | NullPointerException ex) {
            ExceptionPrinter.printHistory(new CouldNotPerformException("Could not publish transformation of " + entry + "!", ex), logger, LogLevel.WARN);
        }
    }

    @Override
    public void afterRegister(IdentifiableMessage<String, UnitConfig, UnitConfig.Builder> entry) {
        publishTransformation(entry);
    }

    @Override
    public void afterUpdate(IdentifiableMessage<String, UnitConfig, UnitConfig.Builder> entry) throws CouldNotPerformException {
        publishTransformation(entry);
    }

    @Override
    public void shutdown() {
        // TODO should be activated after rsb 16 adjustments.
//        if (transformPublisher != null) {
//            transformPublisher.shutdown();
//        }
    }
}
