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
import org.openbase.jps.core.JPService;
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
import rct.TransformerFactory.TransformerFactoryException;
import rst.domotic.unit.UnitConfigType.UnitConfig;

public class PublishLocationTransformationRegistryPlugin extends FileRegistryPluginAdapter<String, IdentifiableMessage<String, UnitConfig, UnitConfig.Builder>> {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    private TransformerFactory transformerFactory;
    private TransformPublisher transformPublisher;

    public PublishLocationTransformationRegistryPlugin() throws org.openbase.jul.exception.InstantiationException {
        try {
            logger.debug("create location transformation publisher");
            this.transformerFactory = TransformerFactory.getInstance();
        } catch (Exception ex) {
            throw new org.openbase.jul.exception.InstantiationException(this, ex);
        }
    }

    @Override
    public void init(final Registry<String, IdentifiableMessage<String, UnitConfig, UnitConfig.Builder>> registry) throws InitializationException, InterruptedException {
        try {
            super.init(registry);
            this.transformPublisher = transformerFactory.createTransformPublisher(registry.getName());
            for (IdentifiableMessage<String, UnitConfig, UnitConfig.Builder> entry : registry.getEntries()) {
                publishtransformation(entry);
            }
        } catch (CouldNotPerformException | TransformerFactoryException ex) {
            throw new InitializationException(this, ex);
        }
    }

    @Override
    public void afterRegister(final IdentifiableMessage<String, UnitConfig, UnitConfig.Builder> entry) {
        publishtransformation(entry);
    }

    @Override
    public void afterUpdate(final IdentifiableMessage<String, UnitConfig, UnitConfig.Builder> entry) throws CouldNotPerformException {
        publishtransformation(entry);
    }

    private synchronized void publishtransformation(final IdentifiableMessage<String, UnitConfig, UnitConfig.Builder> entry) {
        try {
            UnitConfig locationConfig = entry.getMessage();

            // skip root locations
            if (locationConfig.getLocationConfig().getRoot()) {
                return;
            }

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

            if (!JPService.testMode() && JPService.verboseMode()) {
                logger.info("Publish " + getRegistry().get(locationConfig.getPlacementConfig().getLocationId()).getMessage().getPlacementConfig().getTransformationFrameId() + " to " + locationConfig.getPlacementConfig().getTransformationFrameId());
            }

            // Create the rct transform object with source and target frames
            Transform transformation = PoseTransformer.transform(locationConfig.getPlacementConfig().getPosition(), getRegistry().get(locationConfig.getPlacementConfig().getLocationId()).getMessage().getPlacementConfig().getTransformationFrameId(), locationConfig.getPlacementConfig().getTransformationFrameId());

            // Publish the transform object
            transformation.setAuthority(getRegistry().getName());
            transformPublisher.sendTransform(transformation, TransformType.STATIC);
        } catch (NotAvailableException ex) {
            ExceptionPrinter.printHistory(new CouldNotPerformException("Could not publish transformation of " + entry + "!", ex), logger, LogLevel.DEBUG);
        } catch (CouldNotPerformException | TransformerException | ConcurrentModificationException | NullPointerException ex) {
            ExceptionPrinter.printHistory(new CouldNotPerformException("Could not publish transformation of " + entry + "!", ex), logger, LogLevel.WARN);
        }
    }

    @Override
    public void shutdown() {
        if (transformPublisher != null) {
            try {
                transformPublisher.shutdown();
            } catch (Exception ex) {
                logger.warn("Could not shutdown transformation publisher");
            }
        }
    }
}
