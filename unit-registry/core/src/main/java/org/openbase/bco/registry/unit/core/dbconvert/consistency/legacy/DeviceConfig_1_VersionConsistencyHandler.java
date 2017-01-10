package org.openbase.bco.registry.unit.core.dbconvert.consistency.legacy;

/*
 * #%L
 * BCO Registry Unit Core
 * %%
 * Copyright (C) 2014 - 2017 openbase.org
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
import java.util.HashMap;
import java.util.Map;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.extension.protobuf.IdentifiableMessage;
import org.openbase.jul.extension.protobuf.container.ProtoBufMessageMap;
import org.openbase.jul.storage.registry.AbstractVersionConsistencyHandler;
import org.openbase.jul.storage.registry.EntryModification;
import org.openbase.jul.storage.registry.version.DBVersionControl;
import rst.domotic.unit.device.DeviceConfigType.DeviceConfig;
import org.openbase.jul.storage.registry.FileSynchronizedRegistry;
import org.openbase.jul.storage.registry.ProtoBufRegistry;

/**
 * Not supported any more because of rst changes.
 *
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class DeviceConfig_1_VersionConsistencyHandler extends AbstractVersionConsistencyHandler<String, DeviceConfig, DeviceConfig.Builder> {

//    private org.openbase.bco.registry.location.lib.LocationRegistry locationRegistry;
    private final Map<String, String> locationLabelIdMap;

    public DeviceConfig_1_VersionConsistencyHandler(final DBVersionControl versionControl, final FileSynchronizedRegistry<String, IdentifiableMessage<String, DeviceConfig, DeviceConfig.Builder>> registry) throws InstantiationException, InterruptedException {
        super(versionControl, registry);
        this.locationLabelIdMap = new HashMap<>();
    }

    @Override
    public void processData(String id, IdentifiableMessage<String, DeviceConfig, DeviceConfig.Builder> entry, ProtoBufMessageMap<String, DeviceConfig, DeviceConfig.Builder> entryMap, ProtoBufRegistry<String, DeviceConfig, DeviceConfig.Builder> registry) throws CouldNotPerformException, EntryModification {
//        if (locationRegistry == null) {
//            logger.info("Connect to location registry....");
//            try {
//                CachedLocationRegistryRemote.waitForData();
//                locationRegistry = CachedLocationRegistryRemote.getRegistry();
//                logger.info("Location registry connected.");
//                String oldID;
//                for (LocationConfig locationConfig : locationRegistry.getLocationConfigs()) {
//                    oldID = oldGenerateId(locationConfig);
//                    if (!locationLabelIdMap.containsKey(oldID)) {
//                        locationLabelIdMap.put(oldID, locationConfig.getId());
//                    }
//                }
//            } catch (InterruptedException ex) {
//                ExceptionPrinter.printHistory(ex, logger, LogLevel.ERROR);
//            }
//        }
//
//        DeviceConfig.Builder deviceConfig = entry.getMessage().toBuilder();
//
//        if (!deviceConfig.hasPlacementConfig()) {
//            throw new NotAvailableException("deviceConfig.placementconfig");
//        }
//
//        if (!deviceConfig.getPlacementConfig().hasLocationId()) {
//            throw new NotAvailableException("deviceConfig.placementconfig.locationid");
//        }
//
//        boolean modification = false;
//        if (locationLabelIdMap.containsKey(deviceConfig.getPlacementConfig().getLocationId())) {
//            logger.info("Update Device[" + deviceConfig.getId() + "] Location id from [" + deviceConfig.getPlacementConfig().getLocationId() + "] to [" + locationLabelIdMap.get(deviceConfig.getPlacementConfig().getLocationId()) + "]");
//            deviceConfig.setPlacementConfig(PlacementConfig.newBuilder(deviceConfig.getPlacementConfig()).setLocationId(locationLabelIdMap.get(deviceConfig.getPlacementConfig().getLocationId())));
//            modification = true;
//        }
//
//        deviceConfig.clearUnitConfig();
//        for (UnitConfig.Builder unitConfig : entry.getMessage().toBuilder().getUnitConfigBuilderList()) {
//
//            // Check if placement is available
//            if (!unitConfig.hasPlacementConfig()) {
//                throw new NotAvailableException("unit.placementconfig");
//            }
//
//            if (!unitConfig.getPlacementConfig().hasLocationId()) {
//                throw new NotAvailableException("unit.placementconfig.locationid");
//            }
//
//            if (locationLabelIdMap.containsKey(unitConfig.getPlacementConfig().getLocationId())) {
//                logger.info("Update Unit[" + unitConfig.getId() + "] Location id from [" + unitConfig.getPlacementConfig().getLocationId() + "] to [" + locationLabelIdMap.get(unitConfig.getPlacementConfig().getLocationId()) + "]");
//                unitConfig.setPlacementConfig(PlacementConfig.newBuilder(unitConfig.getPlacementConfig()).setLocationId(locationLabelIdMap.get(unitConfig.getPlacementConfig().getLocationId())));
//                modification = true;
//            }
//            deviceConfig.addUnitConfig(unitConfig);
//        }
//
//        if (modification) {
//            throw new EntryModification(entry.setMessage(deviceConfig), this);
//        }
    }
//
//    /**
//     * This is the old location id generator used for id recovery.
//     *
//     * @param message
//     * @return
//     * @throws CouldNotPerformException
//     */
//    public String oldGenerateId(LocationConfig message) throws CouldNotPerformException {
//        try {
//            if (!message.hasLabel()) {
//                throw new InvalidStateException("Field [locationConfig.label] is missing!");
//            }
//
//            if (message.getLabel().isEmpty()) {
//                throw new InvalidStateException("Field [Label] is empty!");
//            }
//
//            return StringProcessor.transformToIdString(message.getLabel());
//
//        } catch (CouldNotPerformException ex) {
//            throw new CouldNotPerformException("Could not generate id!", ex);
//        }
//    }

}
