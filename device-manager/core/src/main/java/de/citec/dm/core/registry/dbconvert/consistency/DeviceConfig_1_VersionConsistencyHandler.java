/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dm.core.registry.dbconvert.consistency;

import de.citec.jul.exception.CouldNotPerformException;
import de.citec.jul.exception.NotAvailableException;
import de.citec.jul.extension.protobuf.IdentifiableMessage;
import de.citec.jul.extension.protobuf.container.ProtoBufMessageMapInterface;
import de.citec.jul.storage.registry.EntryModification;
import de.citec.jul.storage.registry.ProtoBufRegistryConsistencyHandler;
import de.citec.jul.storage.registry.ProtoBufRegistryInterface;
import de.citec.lm.remote.LocationRegistryRemote;
import java.util.HashMap;
import java.util.Map;
import rst.homeautomation.device.DeviceConfigType.DeviceConfig;
import rst.homeautomation.unit.UnitConfigType.UnitConfig;
import rst.spatial.LocationConfigType.LocationConfig;
import rst.spatial.PlacementConfigType.PlacementConfig;

/**
 *
 * @author <a href="mailto:thuxohl@techfak.uni-bielefeld.com">Tamino Huxohl</a>
 */
public class DeviceConfig_1_VersionConsistencyHandler implements ProtoBufRegistryConsistencyHandler<String, DeviceConfig, DeviceConfig.Builder> {

    private final LocationRegistryRemote locationRegistryRemote;
    private final Map<String, String> locationLabelIdMap;

    public DeviceConfig_1_VersionConsistencyHandler(final LocationRegistryRemote locationRegistryRemote) {
        this.locationRegistryRemote = locationRegistryRemote;
        this.locationLabelIdMap = new HashMap<>();
    }

    @Override
    public void processData(String id, IdentifiableMessage<String, DeviceConfig, DeviceConfig.Builder> entry, ProtoBufMessageMapInterface<String, DeviceConfig, DeviceConfig.Builder> entryMap, ProtoBufRegistryInterface<String, DeviceConfig, DeviceConfig.Builder> registry) throws CouldNotPerformException, EntryModification {
        DeviceConfig.Builder deviceConfig = entry.getMessage().toBuilder();

        for (LocationConfig locationConfig : locationRegistryRemote.getLocationConfigs()) {
            locationLabelIdMap.put(locationConfig.getLabel(), locationConfig.getId());
        }

        if (!deviceConfig.hasPlacementConfig()) {
            throw new NotAvailableException("deviceConfig.placementconfig");
        }

        if (!deviceConfig.getPlacementConfig().hasLocationId()) {
            throw new NotAvailableException("deviceConfig.placementconfig.locationid");
        }

        boolean modification = false;
        if (locationLabelIdMap.containsKey(deviceConfig.getPlacementConfig().getLocationId())) {
            deviceConfig.setPlacementConfig(PlacementConfig.newBuilder(deviceConfig.getPlacementConfig()).setLocationId(locationLabelIdMap.get(deviceConfig.getPlacementConfig().getLocationId())));
            modification = true;
        }

        deviceConfig.clearUnitConfig();
        for (UnitConfig.Builder unitConfig : entry.getMessage().toBuilder().getUnitConfigBuilderList()) {

            // Check if placement is available
            if (!unitConfig.hasPlacementConfig()) {
                throw new NotAvailableException("unit.placementconfig");
            }

            if (!unitConfig.getPlacementConfig().hasLocationId()) {
                throw new NotAvailableException("unit.placementconfig.locationid");
            }

            if (locationLabelIdMap.containsKey(unitConfig.getPlacementConfig().getLocationId())) {
                unitConfig.setPlacementConfig(PlacementConfig.newBuilder(unitConfig.getPlacementConfig()).setLocationId(locationLabelIdMap.get(unitConfig.getPlacementConfig().getLocationId())));
                modification = true;
            }
            deviceConfig.addUnitConfig(unitConfig);
        }

        if (modification) {
            throw new EntryModification(entry.setMessage(deviceConfig), this);
        }
    }

    @Override
    public void reset() {
        locationLabelIdMap.clear();
    }
}
