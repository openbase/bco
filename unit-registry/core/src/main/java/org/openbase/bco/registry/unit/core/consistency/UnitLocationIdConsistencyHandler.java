package org.openbase.bco.registry.device.core.consistency;

/*
 * #%L
 * REM DeviceRegistry Core
 * %%
 * Copyright (C) 2014 - 2016 openbase.org
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
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InvalidStateException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.extension.protobuf.IdentifiableMessage;
import org.openbase.jul.storage.registry.AbstractProtoBufRegistryConsistencyHandler;
import org.openbase.jul.storage.registry.EntryModification;
import org.openbase.bco.registry.location.remote.LocationRegistryRemote;
import org.openbase.jps.core.JPService;
import org.openbase.jps.exception.JPServiceException;
import org.openbase.jul.storage.registry.jp.JPRecoverDB;
import rst.homeautomation.device.DeviceConfigType;
import rst.homeautomation.unit.UnitConfigType;
import rst.spatial.PlacementConfigType;
import org.openbase.jul.extension.protobuf.container.ProtoBufMessageMap;
import org.openbase.jul.storage.registry.ProtoBufRegistry;

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
    public void processData(String id, IdentifiableMessage<String, DeviceConfigType.DeviceConfig, DeviceConfigType.DeviceConfig.Builder> entry, ProtoBufMessageMap<String, DeviceConfigType.DeviceConfig, DeviceConfigType.DeviceConfig.Builder> entryMap, ProtoBufRegistry<String, DeviceConfigType.DeviceConfig, DeviceConfigType.DeviceConfig.Builder> registry) throws CouldNotPerformException, EntryModification {
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
                try {
                    if (!JPService.getProperty(JPRecoverDB.class).getValue()) {
                        throw new InvalidStateException("The configured Location[" + unitConfig.getPlacementConfig().getLocationId() + "] of Unit[" + unitConfig.getId() + "] is unknown!");
                    }
                } catch (JPServiceException ex) {
                    throw new InvalidStateException("The configured Location[" + unitConfig.getPlacementConfig().getLocationId() + "] of Unit[" + unitConfig.getId() + "] is unknown and can not be recovered!", ex);
                }
                // recover unit location with device location.
                unitConfig.setPlacementConfig(PlacementConfigType.PlacementConfig.newBuilder(unitConfig.getPlacementConfig()).setLocationId(deviceConfig.getPlacementConfig().getLocationId()));
                modification = true;
            }

            deviceConfig.addUnitConfig(unitConfig);
        }

        if (modification) {
            throw new EntryModification(entry.setMessage(deviceConfig), this);
        }
    }
}
