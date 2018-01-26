package org.openbase.bco.registry.unit.core.plugin;

/*
 * #%L
 * BCO Registry Unit Core
 * %%
 * Copyright (C) 2014 - 2018 openbase.org
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
import org.openbase.jul.storage.registry.FileSynchronizedRegistry;
import org.openbase.jul.storage.registry.ProtoBufFileSynchronizedRegistry;
import org.openbase.jul.storage.registry.ProtoBufRegistry;
import org.openbase.jul.storage.registry.Registry;
import org.openbase.jul.storage.registry.plugin.FileRegistryPluginAdapter;
import org.openbase.jul.storage.registry.plugin.ProtobufRegistryPluginAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rct.Transform;
import rct.TransformPublisher;
import rct.TransformType;
import rct.TransformerException;
import rct.TransformerFactory;
import rst.domotic.registry.UnitRegistryDataType.UnitRegistryData;
import rst.domotic.unit.UnitConfigType.UnitConfig;
import rst.domotic.unit.UnitConfigType.UnitConfig.Builder;

/**
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class PublishUnitTransformationRegistryPlugin extends ProtobufRegistryPluginAdapter<String, UnitConfig, Builder> {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    private final ProtoBufFileSynchronizedRegistry<String, UnitConfig, UnitConfig.Builder, UnitRegistryData.Builder> locationRegistry;

    private TransformerFactory transformerFactory;
    private TransformPublisher transformPublisher;

    public PublishUnitTransformationRegistryPlugin(final ProtoBufFileSynchronizedRegistry<String, UnitConfig, UnitConfig.Builder, UnitRegistryData.Builder> locationRegistry) throws org.openbase.jul.exception.InstantiationException {
        try {
            this.locationRegistry = locationRegistry;
            this.transformerFactory = TransformerFactory.getInstance();
        } catch (Exception ex) {
            throw new org.openbase.jul.exception.InstantiationException(this, ex);
        }
    }

    @Override
    public void init(final ProtoBufRegistry<String, UnitConfig, Builder> registry) throws InitializationException, InterruptedException {
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
            UnitConfig unitConfig = entry.getMessage();

            if (!unitConfig.hasPlacementConfig()) {
                throw new NotAvailableException("unitconfig.placementconfig");
            }

            if (!unitConfig.getPlacementConfig().hasPosition()) {
                throw new NotAvailableException("unitconfig.placementconfig.position");
            }

            if (!unitConfig.getPlacementConfig().hasTransformationFrameId() || unitConfig.getPlacementConfig().getTransformationFrameId().isEmpty()) {
                throw new NotAvailableException("unitconfig.placementconfig.transformationframeid");
            }

            if (!unitConfig.getPlacementConfig().hasLocationId() || unitConfig.getPlacementConfig().getLocationId().isEmpty()) {
                throw new NotAvailableException("unitconfig.placementconfig.locationid");
            }

            Transform transformation = PoseTransformer.transform(unitConfig.getPlacementConfig().getPosition(), locationRegistry.getMessage(unitConfig.getPlacementConfig().getLocationId()).getPlacementConfig().getTransformationFrameId(), unitConfig.getPlacementConfig().getTransformationFrameId());
            transformation.setAuthority(getRegistry().getName());

            if (!JPService.testMode() && JPService.verboseMode()) {
                logger.info("Publish " + locationRegistry.get(unitConfig.getPlacementConfig().getLocationId()).getMessage().getPlacementConfig().getTransformationFrameId() + " to " + unitConfig.getPlacementConfig().getTransformationFrameId());
            }
            transformPublisher.sendTransform(transformation, TransformType.STATIC);
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
        if (transformPublisher != null) {
            try {
                transformPublisher.shutdown();
            } catch (Exception ex) {
                logger.warn("Could not shutdown transformation publisher");
            }
        }
    }
}
