package org.openbase.bco.registry.unit.core.plugin;

/*
 * #%L
 * BCO Registry Unit Core
 * %%
 * Copyright (C) 2014 - 2021 openbase.org
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

import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.ExceptionProcessor;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.exception.printer.LogLevel;
import org.openbase.jul.extension.protobuf.IdentifiableMessage;
import org.openbase.jul.storage.registry.ProtoBufFileSynchronizedRegistry;
import org.openbase.rct.Transform;
import org.openbase.rct.TransformType;
import org.openbase.rct.type.PoseTransformer;
import org.openbase.type.domotic.registry.UnitRegistryDataType.UnitRegistryData;
import org.openbase.type.domotic.unit.UnitConfigType.UnitConfig;

import java.util.ConcurrentModificationException;

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

            if (!unitConfig.getPlacementConfig().hasPose()) {
                throw new NotAvailableException("unitconfig.placementconfig.pose");
            }

            if (!unitConfig.getPlacementConfig().hasTransformationFrameId() || unitConfig.getPlacementConfig().getTransformationFrameId().isEmpty()) {
                throw new NotAvailableException("unitconfig.placementconfig.transformationframeid");
            }

            if (!unitConfig.getPlacementConfig().hasLocationId() || unitConfig.getPlacementConfig().getLocationId().isEmpty()) {
                throw new NotAvailableException("unitconfig.placementconfig.locationid");
            }

            final String parentLocationTransformationFrameId = locationRegistry.getMessage(unitConfig.getPlacementConfig().getLocationId()).getPlacementConfig().getTransformationFrameId();

            // Create the rct transform object with source and target frames
            Transform transformation = PoseTransformer.Companion.transform(
                    unitConfig.getPlacementConfig().getPose(),
                    parentLocationTransformationFrameId,
                    unitConfig.getPlacementConfig().getTransformationFrameId(),
                    getRegistry().getName()
                );

            // publish the transform object
            transformPublisher.sendTransform(transformation, TransformType.STATIC);

            // verify transformation
            verifyPublication(transformation, parentLocationTransformationFrameId, unitConfig.getPlacementConfig().getTransformationFrameId());
        } catch (NotAvailableException ex) {
            if (!ExceptionProcessor.isCausedBySystemShutdown(ex)) {
                ExceptionPrinter.printHistory(new CouldNotPerformException("Could not publish transformation of " + entry + "!", ex), logger, LogLevel.DEBUG);
            }
        } catch (CouldNotPerformException | ConcurrentModificationException | NullPointerException ex) {
            if (!ExceptionProcessor.isCausedBySystemShutdown(ex)) {
                ExceptionPrinter.printHistory(new CouldNotPerformException("Could not publish transformation of " + entry + "!", ex), logger, LogLevel.WARN);
            }
        }
    }
}
