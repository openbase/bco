package org.openbase.bco.registry.unit.core.plugin;

import org.openbase.jps.core.JPService;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InitializationException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.exception.printer.LogLevel;
import org.openbase.jul.extension.protobuf.IdentifiableMessage;
import org.openbase.jul.extension.rct.transform.PoseTransformer;
import org.openbase.jul.storage.registry.ProtoBufFileSynchronizedRegistry;
import org.openbase.jul.storage.registry.Registry;
import org.openbase.jul.storage.registry.plugin.FileRegistryPluginAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rct.Transform;
import rct.TransformPublisher;
import rct.TransformType;
import rct.TransformerFactory;
import rst.geometry.PoseType;
import rst.homeautomation.unit.UnitConfigType.UnitConfig;
import rst.homeautomation.unit.UnitRegistryDataType.UnitRegistryData;

/**
 *
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class PublishDalUnitTransformationRegistryPlugin extends FileRegistryPluginAdapter<String, IdentifiableMessage<String, UnitConfig, UnitConfig.Builder>> {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    private final ProtoBufFileSynchronizedRegistry<String, UnitConfig, UnitConfig.Builder, UnitRegistryData.Builder> locationRegistry;

    private TransformerFactory transformerFactory;
    private TransformPublisher transformPublisher;

    public PublishDalUnitTransformationRegistryPlugin(final ProtoBufFileSynchronizedRegistry<String, UnitConfig, UnitConfig.Builder, UnitRegistryData.Builder> locationRegistry) throws org.openbase.jul.exception.InstantiationException {
        try {
            this.locationRegistry = locationRegistry;
            this.transformerFactory = TransformerFactory.getInstance();
            this.transformPublisher = transformerFactory.createTransformPublisher(JPService.getApplicationName());
        } catch (Exception ex) {
            throw new org.openbase.jul.exception.InstantiationException(this, ex);
        }
    }

    @Override
    public void init(final Registry<String, IdentifiableMessage<String, UnitConfig, UnitConfig.Builder>> registry) throws InitializationException, InterruptedException {
        try {
            for (IdentifiableMessage<String, UnitConfig, UnitConfig.Builder> entry : registry.getEntries()) {
                publishTransformation(entry);
            }
        } catch (CouldNotPerformException ex) {
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

            if (isTransformationPresent(unitConfig.getPlacementConfig().getPosition())) {
                Transform transformation = PoseTransformer.transform(unitConfig.getPlacementConfig().getPosition(), locationRegistry.getMessage(unitConfig.getPlacementConfig().getLocationId()).getPlacementConfig().getTransformationFrameId(), unitConfig.getPlacementConfig().getTransformationFrameId());

                try {
                    transformPublisher.sendTransform(transformation, TransformType.STATIC);
                } catch (Exception ex) {
                    ExceptionPrinter.printHistory(new CouldNotPerformException("Could not publish transformation of " + entry + "!", ex), logger, LogLevel.ERROR);
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
    public void afterRegister(IdentifiableMessage<String, UnitConfig, UnitConfig.Builder> entry) {
        publishTransformation(entry);
    }

    @Override
    public void afterUpdate(IdentifiableMessage<String, UnitConfig, UnitConfig.Builder> entry) throws CouldNotPerformException {
        publishTransformation(entry);
    }

    @Override
    public void shutdown() {
        //TODO insert rct shutdown after implementation ;)
    }
}
