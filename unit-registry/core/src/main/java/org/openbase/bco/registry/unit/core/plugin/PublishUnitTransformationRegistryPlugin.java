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

import org.openbase.jps.core.JPService;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InitializationException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.exception.printer.LogLevel;
import org.openbase.jul.extension.protobuf.IdentifiableMessage;
import org.openbase.jul.extension.rct.GlobalTransformReceiver;
import org.openbase.jul.extension.rct.transform.PoseTransformer;
import org.openbase.jul.storage.registry.ProtoBufFileSynchronizedRegistry;
import org.openbase.jul.storage.registry.ProtoBufRegistry;
import org.openbase.jul.storage.registry.plugin.ProtobufRegistryPluginAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rct.*;
import rst.domotic.registry.UnitRegistryDataType.UnitRegistryData;
import rst.domotic.unit.UnitConfigType.UnitConfig;
import rst.domotic.unit.UnitConfigType.UnitConfig.Builder;

import java.util.ConcurrentModificationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class PublishUnitTransformationRegistryPlugin extends AbstractUnitTransformationRegistryPlugin {

    private final ProtoBufFileSynchronizedRegistry<String, UnitConfig, UnitConfig.Builder, UnitRegistryData.Builder> locationRegistry;

    public PublishUnitTransformationRegistryPlugin(final ProtoBufFileSynchronizedRegistry<String, UnitConfig, UnitConfig.Builder, UnitRegistryData.Builder> locationRegistry) throws org.openbase.jul.exception.InstantiationException {
        super();
        try {
            this.locationRegistry = locationRegistry;
        } catch (Exception ex) {
            throw new org.openbase.jul.exception.InstantiationException(this, ex);
        }
    }

    @Override
    protected void publishTransformation(IdentifiableMessage<String, UnitConfig, UnitConfig.Builder> entry) {
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

            final String parentLocationTransformationFrameId = locationRegistry.getMessage(unitConfig.getPlacementConfig().getLocationId()).getPlacementConfig().getTransformationFrameId();

            // Create the rct transform object with source and target frames
            Transform transformation = PoseTransformer.transform(unitConfig.getPlacementConfig().getPosition(), parentLocationTransformationFrameId, unitConfig.getPlacementConfig().getTransformationFrameId());

            // publish the transform object
            transformation.setAuthority(getRegistry().getName());
            transformPublisher.sendTransform(transformation, TransformType.STATIC);

            // verify transformation
            verifyPublication(transformation, parentLocationTransformationFrameId, unitConfig.getPlacementConfig().getTransformationFrameId());
        } catch (NotAvailableException ex) {
            ExceptionPrinter.printHistory(new CouldNotPerformException("Could not publish transformation of " + entry + "!", ex), logger, LogLevel.DEBUG);
        } catch (CouldNotPerformException | TransformerException | ConcurrentModificationException | NullPointerException ex) {
            ExceptionPrinter.printHistory(new CouldNotPerformException("Could not publish transformation of " + entry + "!", ex), logger, LogLevel.WARN);
        }
    }
}
