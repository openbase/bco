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
import rst.spatial.LocationConfigType;
import rst.spatial.PlacementConfigType.PlacementConfig;

/**
 *
 * @author mpohling
 */
public class DeviceTransformationFrameConsistencyHandler extends AbstractTransformationFrameConsistencyHandler<String, DeviceConfig, DeviceConfig.Builder> {

    public DeviceTransformationFrameConsistencyHandler(final ProtoBufRegistryInterface<String, LocationConfigType.LocationConfig, LocationConfigType.LocationConfig.Builder> locationRegistry) {
        super(locationRegistry);
    }

    @Override
    public void processData(final String id, final IdentifiableMessage<String, DeviceConfig, DeviceConfig.Builder> entry, final ProtoBufMessageMapInterface<String, DeviceConfig, DeviceConfig.Builder> entryMap, final ProtoBufRegistryInterface<String, DeviceConfig, DeviceConfig.Builder> registry) throws CouldNotPerformException, EntryModification {
        DeviceConfig.Builder deviceConfigBuilder = entry.getMessage().toBuilder();
        PlacementConfig placementConfig = verifyAndUpdatePlacement(deviceConfigBuilder.getLabel(), deviceConfigBuilder.getPlacementConfig());

        if (placementConfig != null) {
            entry.setMessage(deviceConfigBuilder.setPlacementConfig(placementConfig));
            throw new EntryModification(entry, this);
        }
    }
}
