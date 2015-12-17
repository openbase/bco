/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dm.core.consistency;

import de.citec.jul.exception.CouldNotPerformException;
import de.citec.jul.exception.InvalidStateException;
import de.citec.jul.exception.NotAvailableException;
import de.citec.jul.extension.protobuf.IdentifiableMessage;
import de.citec.jul.extension.protobuf.container.ProtoBufMessageMapInterface;
import de.citec.jul.storage.registry.AbstractProtoBufRegistryConsistencyHandler;
import de.citec.jul.storage.registry.EntryModification;
import de.citec.jul.storage.registry.ProtoBufRegistryInterface;
import de.citec.lm.remote.LocationRegistryRemote;
import rst.homeautomation.device.DeviceConfigType;
import rst.spatial.PlacementConfigType;

/**
 *
 * @author mpohling
 */
public class DeviceLocationIdConsistencyHandler extends AbstractProtoBufRegistryConsistencyHandler<String, DeviceConfigType.DeviceConfig, DeviceConfigType.DeviceConfig.Builder> {

    private final LocationRegistryRemote locationRegistryRemote;

    public DeviceLocationIdConsistencyHandler(final LocationRegistryRemote locationRegistryRemote) {
        this.locationRegistryRemote = locationRegistryRemote;
    }

    @Override
    public void processData(String id, IdentifiableMessage<String, DeviceConfigType.DeviceConfig, DeviceConfigType.DeviceConfig.Builder> entry, ProtoBufMessageMapInterface<String, DeviceConfigType.DeviceConfig, DeviceConfigType.DeviceConfig.Builder> entryMap, ProtoBufRegistryInterface<String, DeviceConfigType.DeviceConfig, DeviceConfigType.DeviceConfig.Builder> registry) throws CouldNotPerformException, EntryModification {
        DeviceConfigType.DeviceConfig.Builder deviceConfig = entry.getMessage().toBuilder();

        // check if placementconfig is available
        if (!deviceConfig.hasPlacementConfig()) {
            throw new NotAvailableException("device.placementconfig");
        }

        // setup base location of device has no location configured.
        if (!deviceConfig.getPlacementConfig().hasLocationId() || deviceConfig.getPlacementConfig().getLocationId().isEmpty()) {
            deviceConfig.setPlacementConfig(PlacementConfigType.PlacementConfig.newBuilder(deviceConfig.getPlacementConfig()).setLocationId(locationRegistryRemote.getRootLocationConfig().getId()));
            throw new EntryModification(entry.setMessage(deviceConfig), this);
        }

        // verify if configured location exists.
        if (!locationRegistryRemote.containsLocationConfigById(deviceConfig.getPlacementConfig().getLocationId())) {
            throw new InvalidStateException("The configured Location[" + deviceConfig.getPlacementConfig().getLocationId() + "] of Device[" + deviceConfig.getId() + "] is unknown!");
        }
    }

    @Override
    public void reset() {
    }
}
