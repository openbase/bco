/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dc.bco.registry.device.core.consistency;

import de.citec.jul.extension.rst.storage.registry.consistency.AbstractTransformationFrameConsistencyHandler;
import org.dc.jul.exception.CouldNotPerformException;
import org.dc.jul.extension.protobuf.IdentifiableMessage;
import org.dc.jul.extension.protobuf.container.ProtoBufMessageMapInterface;
import org.dc.jul.storage.registry.EntryModification;
import org.dc.jul.storage.registry.ProtoBufRegistryInterface;
import rst.homeautomation.device.DeviceConfigType.DeviceConfig;
import rst.homeautomation.unit.UnitConfigType;
import rst.spatial.LocationConfigType;
import rst.spatial.PlacementConfigType.PlacementConfig;

/**
 *
 * @author mpohling
 */
public class UnitTransformationFrameConsistencyHandler extends AbstractTransformationFrameConsistencyHandler<String, DeviceConfig, DeviceConfig.Builder> {

    public UnitTransformationFrameConsistencyHandler(final ProtoBufRegistryInterface<String, LocationConfigType.LocationConfig, LocationConfigType.LocationConfig.Builder> locationRegistry) {
        super(locationRegistry);
    }

    @Override
    public void processData(final String id, final IdentifiableMessage<String, DeviceConfig, DeviceConfig.Builder> entry, final ProtoBufMessageMapInterface<String, DeviceConfig, DeviceConfig.Builder> entryMap, final ProtoBufRegistryInterface<String, DeviceConfig, DeviceConfig.Builder> registry) throws CouldNotPerformException, EntryModification {

        DeviceConfig.Builder deviceConfigBuilder = entry.getMessage().toBuilder();
        deviceConfigBuilder.clearUnitConfig();
        boolean modification = false;
        PlacementConfig placementConfig;

        for (UnitConfigType.UnitConfig.Builder unitConfigBuilder : entry.getMessage().toBuilder().getUnitConfigBuilderList()) {

            placementConfig = verifyAndUpdatePlacement(unitConfigBuilder.getLabel(), unitConfigBuilder.getPlacementConfig());

            if (placementConfig != null) {
                unitConfigBuilder.setPlacementConfig(placementConfig);
                logger.debug("UnitTransformationFrameConsistencyHandler Upgrade Unit["+unitConfigBuilder.getId()+"] frame to "+placementConfig.getTransformationFrameId());
                modification = true;
            }

            deviceConfigBuilder.addUnitConfig(unitConfigBuilder);
        }

        if (modification) {
            logger.debug("UnitTransformationFrameConsistencyHandler Publish Device["+deviceConfigBuilder.getId()+"]!");
            throw new EntryModification(entry.setMessage(deviceConfigBuilder), this);
        }
    }
}
