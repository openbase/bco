/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dc.bco.registry.device.core.consistency;

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
import org.dc.jul.exception.InvalidStateException;
import org.dc.jul.exception.NotAvailableException;
import org.dc.jul.extension.protobuf.IdentifiableMessage;
import org.dc.jul.extension.protobuf.container.ProtoBufMessageMapInterface;
import org.dc.jul.storage.registry.AbstractProtoBufRegistryConsistencyHandler;
import org.dc.jul.storage.registry.EntryModification;
import org.dc.jul.storage.registry.ProtoBufRegistryInterface;
import org.dc.bco.registry.location.remote.LocationRegistryRemote;
import org.dc.jps.core.JPService;
import org.dc.jps.exception.JPServiceException;
import org.dc.jul.storage.registry.jp.JPRecoverDB;
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

    @Override
    public void reset() {
    }
}
