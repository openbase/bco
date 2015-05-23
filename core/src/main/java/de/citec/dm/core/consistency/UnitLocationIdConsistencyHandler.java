/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dm.core.consistency;

import de.citec.jul.exception.CouldNotPerformException;
import de.citec.jul.exception.NotAvailableException;
import de.citec.jul.extension.rsb.container.IdentifiableMessage;
import de.citec.jul.extension.rsb.container.ProtoBufMessageMapInterface;
import de.citec.jul.storage.registry.EntryModification;
import de.citec.jul.storage.registry.ProtoBufRegistryConsistencyHandler;
import de.citec.jul.storage.registry.ProtoBufRegistryInterface;
import de.citec.lm.remote.LocationRegistryRemote;
import java.util.List;
import rst.homeautomation.device.DeviceConfigType;
import rst.homeautomation.unit.UnitConfigType;
import rst.spatial.LocationConfigType;
import rst.spatial.PlacementConfigType;

/**
 *
 * @author mpohling
 */
public class UnitLocationIdConsistencyHandler implements ProtoBufRegistryConsistencyHandler<String, DeviceConfigType.DeviceConfig, DeviceConfigType.DeviceConfig.Builder> {

    private final LocationRegistryRemote locationRegistryRemote;

    public UnitLocationIdConsistencyHandler(final LocationRegistryRemote locationRegistryRemote) {
        this.locationRegistryRemote = locationRegistryRemote;
    }

    @Override
    public void processData(String id, IdentifiableMessage<String, DeviceConfigType.DeviceConfig, DeviceConfigType.DeviceConfig.Builder> entry, ProtoBufMessageMapInterface<String, DeviceConfigType.DeviceConfig, DeviceConfigType.DeviceConfig.Builder> entryMap, ProtoBufRegistryInterface<String, DeviceConfigType.DeviceConfig, DeviceConfigType.DeviceConfig.Builder> registry) throws CouldNotPerformException, EntryModification {
        DeviceConfigType.DeviceConfig.Builder deviceConfig = entry.getMessage().toBuilder();

        boolean modification = false;
        for (UnitConfigType.UnitConfig.Builder unitConfig : entry.getMessage().toBuilder().getUnitConfigBuilderList()) {
            if (!unitConfig.hasPlacementConfig()) {
                throw new NotAvailableException("unit.placementconfig");
            }
            
            if(!unitConfig.getPlacementConfig().hasLocationId() || unitConfig.getPlacementConfig().getLocationId().isEmpty()) {
                List<LocationConfigType.LocationConfig> rootLocationConfigs = locationRegistryRemote.getRootLocationConfigs();
                unitConfig.setPlacementConfig(PlacementConfigType.PlacementConfig.newBuilder(unitConfig.getPlacementConfig()).setLocationId(rootLocationConfigs.get(0).getId()));
                modification = true;
            }
            deviceConfig.addUnitConfig(unitConfig);
        }

        if (modification) {
            throw new EntryModification(entry.setMessage(deviceConfig).getMessage(), this);
        }
    }

    @Override
    public void reset() {
    }
}
