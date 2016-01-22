/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dc.bco.registry.device.core.plugin;

import org.dc.bco.registry.device.core.DeviceRegistryLauncher;
import org.dc.bco.registry.location.lib.LocationRegistry;
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

    public PublishDeviceTransformationRegistryPlugin(final LocationRegistry locationRegistry) throws org.dc.jul.exception.InstantiationException {
        try {
            this.locationRegistry = locationRegistry;
            this.transformerFactory = TransformerFactory.getInstance();
            this.transformPublisher = transformerFactory.createTransformPublisher(DeviceRegistryLauncher.APP_NAME);
        } catch (Exception ex) {
            throw new org.dc.jul.exception.InstantiationException(this, ex);
        }
    }

    @Override
    public void init(final Registry<String, IdentifiableMessage<String, DeviceConfig, DeviceConfig.Builder>, ?> registry) throws CouldNotPerformException {
        this.registry = registry;
        for (IdentifiableMessage<String, DeviceConfig, DeviceConfig.Builder> entry : registry.getEntries()) {
            publishTransformation(entry);
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
            ExceptionPrinter.printHistory(new CouldNotPerformException("Could not publish device transformation of " + entry + "!", ex), logger, LogLevel.ERROR);
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
