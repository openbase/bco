/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dm.core.registry.dbconvert.consistency;

import de.citec.jul.exception.CouldNotPerformException;
import de.citec.jul.exception.InstantiationException;
import de.citec.jul.exception.NotAvailableException;
import de.citec.jul.exception.printer.ExceptionPrinter;
import de.citec.jul.exception.printer.LogLevel;
import de.citec.jul.extension.protobuf.IdentifiableMessage;
import de.citec.jul.extension.protobuf.container.ProtoBufMessageMapInterface;
import de.citec.jul.storage.registry.EntryModification;
import de.citec.jul.storage.registry.ProtoBufRegistryConsistencyHandler;
import de.citec.jul.storage.registry.ProtoBufRegistryInterface;
import de.citec.lm.remote.LocationRegistryRemote;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rst.homeautomation.device.DeviceConfigType.DeviceConfig;
import rst.homeautomation.unit.UnitConfigType.UnitConfig;
import rst.spatial.LocationConfigType.LocationConfig;
import rst.spatial.PlacementConfigType.PlacementConfig;

/**
 *
 * @author <a href="mailto:thuxohl@techfak.uni-bielefeld.com">Tamino Huxohl</a>
 */
public class DeviceConfig_1_VersionConsistencyHandler implements ProtoBufRegistryConsistencyHandler<String, DeviceConfig, DeviceConfig.Builder> {

    private static final Logger logger = LoggerFactory.getLogger(DeviceConfig_1_VersionConsistencyHandler.class);

    private final LocationRegistryRemote locationRegistryRemote;
    private final Map<String, String> locationLabelIdMap;

    public DeviceConfig_1_VersionConsistencyHandler() throws InstantiationException, InterruptedException {
        try {

            this.locationRegistryRemote = new LocationRegistryRemote();
            this.locationRegistryRemote.init();
            this.locationLabelIdMap = new HashMap<>();

        } catch (CouldNotPerformException ex) {
            throw new InstantiationException(this, ex);
        }
    }

    @Override
    public void processData(String id, IdentifiableMessage<String, DeviceConfig, DeviceConfig.Builder> entry, ProtoBufMessageMapInterface<String, DeviceConfig, DeviceConfig.Builder> entryMap, ProtoBufRegistryInterface<String, DeviceConfig, DeviceConfig.Builder> registry) throws CouldNotPerformException, EntryModification {

        if (!locationRegistryRemote.isActive()) {
            try {
                locationRegistryRemote.activate();
                for (LocationConfig locationConfig : locationRegistryRemote.getLocationConfigs()) {
                    if (!locationLabelIdMap.containsKey(locationConfig.getLabel())) {
                        locationLabelIdMap.put(locationConfig.getLabel(), locationConfig.getId());
                    }
                }
            } catch (InterruptedException ex) {
                throw new RuntimeException(ExceptionPrinter.printHistoryAndReturnThrowable(ex, logger, LogLevel.ERROR));
            }
        }

        DeviceConfig.Builder deviceConfig = entry.getMessage().toBuilder();

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
    }

    //TODO mpohling: mark consistency handler done within the db version if this consistency handler shuts down and the registry is consistent.
    // Blocked by not implemented abstract consistency handler and shutdown method.
}
