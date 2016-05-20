package org.dc.bco.registry.device.core.dbconvert.consistency;

/*
 * #%L
 * REM DeviceRegistry Core
 * %%
 * Copyright (C) 2014 - 2016 DivineCooperation
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
import org.dc.jul.exception.CouldNotPerformException;
import org.dc.jul.exception.InstantiationException;
import org.dc.jul.exception.InvalidStateException;
import org.dc.jul.exception.NotAvailableException;
import org.dc.jul.exception.printer.ExceptionPrinter;
import org.dc.jul.exception.printer.LogLevel;
import org.dc.jul.extension.protobuf.IdentifiableMessage;
import org.dc.jul.extension.protobuf.container.ProtoBufMessageMapInterface;
import org.dc.jul.processing.StringProcessor;
import org.dc.jul.storage.registry.AbstractVersionConsistencyHandler;
import org.dc.jul.storage.registry.EntryModification;
import org.dc.jul.storage.registry.FileSynchronizedRegistryInterface;
import org.dc.jul.storage.registry.ProtoBufRegistryInterface;
import org.dc.jul.storage.registry.version.DBVersionControl;
import java.util.HashMap;
import java.util.Map;
import org.dc.bco.registry.location.remote.CachedLocationRegistryRemote;
import rst.homeautomation.device.DeviceConfigType.DeviceConfig;
import rst.homeautomation.unit.UnitConfigType.UnitConfig;
import rst.spatial.LocationConfigType.LocationConfig;
import rst.spatial.PlacementConfigType.PlacementConfig;

/**
 *
 * @author <a href="mailto:thuxohl@techfak.uni-bielefeld.com">Tamino Huxohl</a>
 */
public class DeviceConfig_1_VersionConsistencyHandler extends AbstractVersionConsistencyHandler<String, DeviceConfig, DeviceConfig.Builder> {

    private org.dc.bco.registry.location.lib.LocationRegistry locationRegistry;
    private final Map<String, String> locationLabelIdMap;

    public DeviceConfig_1_VersionConsistencyHandler(final DBVersionControl versionControl, final FileSynchronizedRegistryInterface<String, IdentifiableMessage<String, DeviceConfig, DeviceConfig.Builder>, ProtoBufRegistryInterface<String, DeviceConfig, DeviceConfig.Builder>> registry) throws InstantiationException, InterruptedException {
        super(versionControl, registry);
        this.locationLabelIdMap = new HashMap<>();
    }

    @Override
    public void processData(String id, IdentifiableMessage<String, DeviceConfig, DeviceConfig.Builder> entry, ProtoBufMessageMapInterface<String, DeviceConfig, DeviceConfig.Builder> entryMap, ProtoBufRegistryInterface<String, DeviceConfig, DeviceConfig.Builder> registry) throws CouldNotPerformException, EntryModification {

        if (locationRegistry == null) {
            logger.info("Connect to location registry....");
            try {
                locationRegistry = CachedLocationRegistryRemote.getLocationRegistry();
                logger.info("Location registry connected.");
                String oldID;
                for (LocationConfig locationConfig : locationRegistry.getLocationConfigs()) {
                    oldID = oldGenerateId(locationConfig);
                    if (!locationLabelIdMap.containsKey(oldID)) {
                        locationLabelIdMap.put(oldID, locationConfig.getId());
                        System.out.println("register mapping old[" + oldID + "] = new[" + locationConfig.getId() + "]");
                    }
                }
            } catch (InterruptedException ex) {
                ExceptionPrinter.printHistory(ex, logger, LogLevel.ERROR);
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
            logger.info("Update Device[" + deviceConfig.getId() + "] Location id from [" + deviceConfig.getPlacementConfig().getLocationId() + "] to [" + locationLabelIdMap.get(deviceConfig.getPlacementConfig().getLocationId()) + "]");
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
                logger.info("Update Unit[" + unitConfig.getId() + "] Location id from [" + unitConfig.getPlacementConfig().getLocationId() + "] to [" + locationLabelIdMap.get(unitConfig.getPlacementConfig().getLocationId()) + "]");
                unitConfig.setPlacementConfig(PlacementConfig.newBuilder(unitConfig.getPlacementConfig()).setLocationId(locationLabelIdMap.get(unitConfig.getPlacementConfig().getLocationId())));
                modification = true;
            }
            deviceConfig.addUnitConfig(unitConfig);
        }

        if (modification) {
            throw new EntryModification(entry.setMessage(deviceConfig), this);
        }
    }

    /**
     * This is the old location id generator used for id recovery.
     *
     * @param message
     * @return
     * @throws CouldNotPerformException
     */
    public String oldGenerateId(LocationConfig message) throws CouldNotPerformException {
        try {
            if (!message.hasLabel()) {
                throw new InvalidStateException("Field [locationConfig.label] is missing!");
            }

            if (message.getLabel().isEmpty()) {
                throw new InvalidStateException("Field [Label] is empty!");
            }

            return StringProcessor.transformToIdString(message.getLabel());

        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not generate id!", ex);
        }
    }

}
