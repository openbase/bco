package org.openbase.bco.registry.device.core.plugin;

/*
 * #%L
 * REM DeviceRegistry Core
 * %%
 * Copyright (C) 2014 - 2016 openbase.org
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
import org.openbase.bco.registry.device.core.DeviceRegistryLauncher;
import org.openbase.bco.registry.location.lib.LocationRegistry;
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
import rct.TransformerFactory;
import rst.geometry.PoseType;
import rst.homeautomation.device.DeviceConfigType;
import rst.homeautomation.device.DeviceConfigType.DeviceConfig;
import rst.homeautomation.unit.UnitConfigType;

public class PublishDeviceTransformationRegistryPlugin extends FileRegistryPluginAdapter<String, IdentifiableMessage<String, DeviceConfig, DeviceConfig.Builder>> {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    private Registry<String, IdentifiableMessage<String, DeviceConfig, DeviceConfig.Builder>, ?> registry;
    final LocationRegistry locationRegistry;

    private TransformerFactory transformerFactory;
    private TransformPublisher transformPublisher;

    public PublishDeviceTransformationRegistryPlugin(final LocationRegistry locationRegistry) throws org.openbase.jul.exception.InstantiationException {
        try {
            this.locationRegistry = locationRegistry;
            this.transformerFactory = TransformerFactory.getInstance();
            this.transformPublisher = transformerFactory.createTransformPublisher(JPService.getApplicationName());
        } catch (Exception ex) {
            throw new org.openbase.jul.exception.InstantiationException(this, ex);
        }
    }

    @Override
    public void init(final Registry<String, IdentifiableMessage<String, DeviceConfig, DeviceConfig.Builder>, ?> registry) throws InitializationException, InterruptedException {
        try {
            this.registry = registry;
            for (IdentifiableMessage<String, DeviceConfig, DeviceConfig.Builder> entry : registry.getEntries()) {
                publishTransformation(entry);
            }
        } catch (CouldNotPerformException ex) {
            throw new InitializationException(this, ex);
        }
    }

    public void publishTransformation(IdentifiableMessage<String, DeviceConfigType.DeviceConfig, DeviceConfigType.DeviceConfig.Builder> entry) {
        try {
            DeviceConfigType.DeviceConfig deviceConfig = entry.getMessage();

            if (!deviceConfig.hasId()) {
                throw new NotAvailableException("deviceconfig.id");
            }

            if (!deviceConfig.hasPlacementConfig()) {
                throw new NotAvailableException("deviceconfig.placement");
            }

            if (!deviceConfig.getPlacementConfig().hasPosition()) {
                throw new NotAvailableException("deviceconfig.placement.position");
            }

            if (!deviceConfig.getPlacementConfig().hasTransformationFrameId() || deviceConfig.getPlacementConfig().getTransformationFrameId().isEmpty()) {
                throw new NotAvailableException("deviceconfig.placement.transformationframeid");
            }

            if (!deviceConfig.getPlacementConfig().hasLocationId() || deviceConfig.getPlacementConfig().getLocationId().isEmpty()) {
                throw new NotAvailableException("deviceconfig.placement.locationid");
            }

            Transform transformation;

            // publish device transformation
            if (isTransformationPresent(deviceConfig.getPlacementConfig().getPosition())) {
                logger.info("Publish " + locationRegistry.getLocationConfigById(deviceConfig.getPlacementConfig().getLocationId()).getPlacementConfig().getTransformationFrameId() + " to " + deviceConfig.getPlacementConfig().getTransformationFrameId());
                transformation = PoseTransformer.transform(deviceConfig.getPlacementConfig().getPosition(), locationRegistry.getLocationConfigById(deviceConfig.getPlacementConfig().getLocationId()).getPlacementConfig().getTransformationFrameId(), deviceConfig.getPlacementConfig().getTransformationFrameId());

                try {
                    transformPublisher.sendTransform(transformation, TransformType.STATIC);
                } catch (Exception ex) {
                    ExceptionPrinter.printHistory(new CouldNotPerformException("Could not publish transformation of " + entry + "!", ex), logger, LogLevel.ERROR);
                }
            }

            // publish unit transformation
            for (UnitConfigType.UnitConfig unitConfig : deviceConfig.getUnitConfigList()) {

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

                if (isTransformationPresent(unitConfig.getPlacementConfig().getPosition())) {
                    transformation = PoseTransformer.transform(unitConfig.getPlacementConfig().getPosition(), locationRegistry.getLocationConfigById(unitConfig.getPlacementConfig().getLocationId()).getPlacementConfig().getTransformationFrameId(), unitConfig.getPlacementConfig().getTransformationFrameId());

                    try {
                        transformPublisher.sendTransform(transformation, TransformType.STATIC);
                    } catch (Exception ex) {
                        ExceptionPrinter.printHistory(new CouldNotPerformException("Could not publish transformation of " + entry + "!", ex), logger, LogLevel.ERROR);
                    }
                }
            }
        } catch (Exception ex) {
            ExceptionPrinter.printHistory(new CouldNotPerformException("Could not publish device transformation of " + entry + "!", ex), logger, LogLevel.WARN);
        }
    }

    /**
     * Check if given pose is neutral.
     *
     * @param position
     * @return
     */
    private boolean isTransformationPresent(final PoseType.Pose position) {
        if (!position.hasRotation() && !position.hasTranslation()) {
            return false;
        }

        return !(position.getTranslation().getX() == 0.0
                && position.getTranslation().getY() == 0.0
                && position.getTranslation().getZ() == 0.0
                && position.getRotation().getQw() == 1.0
                && position.getRotation().getQx() == 0.0
                && position.getRotation().getQy() == 0.0
                && position.getRotation().getQx() == 0.0);
    }

    @Override
    public void afterRegister(IdentifiableMessage<String, DeviceConfigType.DeviceConfig, DeviceConfigType.DeviceConfig.Builder> entry) {
        publishTransformation(entry);
    }

    @Override
    public void afterUpdate(IdentifiableMessage<String, DeviceConfigType.DeviceConfig, DeviceConfigType.DeviceConfig.Builder> entry) throws CouldNotPerformException {
        publishTransformation(entry);
    }
}
