package org.openbase.bco.registry.unit.core.plugin;

/*
 * #%L
 * BCO Registry Unit Core
 * %%
 * Copyright (C) 2014 - 2020 openbase.org
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
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.exception.printer.LogLevel;
import org.openbase.jul.extension.protobuf.IdentifiableMessage;
import org.openbase.rct.Transform;
import org.openbase.rct.TransformType;
import org.openbase.rct.type.PoseTransformer;
import org.openbase.type.domotic.unit.UnitConfigType.UnitConfig;

import java.util.ConcurrentModificationException;

public class PublishLocationTransformationRegistryPlugin extends AbstractUnitTransformationRegistryPlugin {

    public PublishLocationTransformationRegistryPlugin() throws InstantiationException {
        super();
    }

    @Override
    protected synchronized void publishTransformation(final IdentifiableMessage<String, UnitConfig, UnitConfig.Builder> entry) {
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

            if (!locationConfig.getPlacementConfig().hasPose()) {
                throw new NotAvailableException("locationconfig.placementconfig.position");
            }

            if (!locationConfig.getPlacementConfig().hasTransformationFrameId() || locationConfig.getPlacementConfig().getTransformationFrameId().isEmpty()) {
                throw new NotAvailableException("locationconfig.placementconfig.transformationframeid");
            }

            if (!locationConfig.getPlacementConfig().hasLocationId() || locationConfig.getPlacementConfig().getLocationId().isEmpty()) {
                throw new NotAvailableException("locationconfig.placementconfig.locationid");
            }

            final String parentLocationTransformationFrameId = getRegistry().get(locationConfig.getPlacementConfig().getLocationId()).getMessage().getPlacementConfig().getTransformationFrameId();

            // Create the rct transform object with source and target frames
            Transform transformation = PoseTransformer.transform(locationConfig.getPlacementConfig().getPose(), parentLocationTransformationFrameId, locationConfig.getPlacementConfig().getTransformationFrameId());

            // Publish the transform object
            transformation.setAuthority(getRegistry().getName());
            transformPublisher.sendTransform(transformation, TransformType.STATIC);

            // verify transformation
            verifyPublication(transformation, parentLocationTransformationFrameId, locationConfig.getPlacementConfig().getTransformationFrameId());

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
