/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dc.bco.registry.device.core.consistency;

import org.dc.jul.exception.CouldNotPerformException;
import org.dc.jul.exception.InvalidStateException;
import org.dc.jul.exception.NotAvailableException;
import org.dc.jul.extension.protobuf.IdentifiableMessage;
import org.dc.jul.extension.protobuf.container.ProtoBufMessageMapInterface;
import org.dc.jul.storage.registry.AbstractProtoBufRegistryConsistencyHandler;
import org.dc.jul.storage.registry.EntryModification;
import org.dc.jul.storage.registry.ProtoBufRegistryInterface;
import org.dc.bco.registry.location.remote.LocationRegistryRemote;
import rst.homeautomation.device.DeviceConfigType;
import rst.homeautomation.unit.UnitConfigType;
import rst.spatial.PlacementConfigType;

/**
 *
 * @author mpohling
 */
public class UnitLocationIdConsistencyHandler extends AbstractProtoBufRegistryConsistencyHandler<String, DeviceConfigType.DeviceConfig, DeviceConfigType.DeviceConfig.Builder> {

    private final LocationRegistryRemote locationRegistryRemote;

    public UnitLocationIdConsistencyHandler(final LocationRegistryRemote locationRegistryRemote) {
        this.locationRegistryRemote = locationRegistryRemote;
    }

    @Override
    public void processData(String id, IdentifiableMessage<String, DeviceConfigType.DeviceConfig, DeviceConfigType.DeviceConfig.Builder> entry, ProtoBufMessageMapInterface<String, DeviceConfigType.DeviceConfig, DeviceConfigType.DeviceConfig.Builder> entryMap, ProtoBufRegistryInterface<String, DeviceConfigType.DeviceConfig, DeviceConfigType.DeviceConfig.Builder> registry) throws CouldNotPerformException, EntryModification {
        DeviceConfigType.DeviceConfig.Builder deviceConfig = entry.getMessage().toBuilder();

        boolean modification = false;
        deviceConfig.clearUnitConfig();
        for (UnitConfigType.UnitConfig.Builder unitConfig : entry.getMessage().toBuilder().getUnitConfigBuilderList()) {

            // Check if placement is available
            if (!unitConfig.hasPlacementConfig()) {
                throw new NotAvailableException("unit.placementconfig");
            }

            // Setup device location if unit has no location configured.
            if (!unitConfig.getPlacementConfig().hasLocationId() || unitConfig.getPlacementConfig().getLocationId().isEmpty()) {

                // Check if device placement is available
                if (!deviceConfig.hasPlacementConfig()) {
                    throw new NotAvailableException("device.placementconfig");
                }

                if (!deviceConfig.getPlacementConfig().hasLocationId() || deviceConfig.getPlacementConfig().getLocationId().isEmpty()) {
                    throw new NotAvailableException("device.placementconfig.locationid");
                }

                unitConfig.setPlacementConfig(PlacementConfigType.PlacementConfig.newBuilder(unitConfig.getPlacementConfig()).setLocationId(deviceConfig.getPlacementConfig().getLocationId()));
                modification = true;
            }

            // verify if configured location exists.
            if (!locationRegistryRemote.containsLocationConfigById(unitConfig.getPlacementConfig().getLocationId())) {
                throw new InvalidStateException("The configured Location[" + unitConfig.getPlacementConfig().getLocationId() + "] of Unit[" + unitConfig.getId() + "] is unknown!");
            }

            deviceConfig.addUnitConfig(unitConfig);
        }

        if (modification) {
            throw new EntryModification(entry.setMessage(deviceConfig), this);
        }
    }

    @Override
    public void reset() {
    }
}
