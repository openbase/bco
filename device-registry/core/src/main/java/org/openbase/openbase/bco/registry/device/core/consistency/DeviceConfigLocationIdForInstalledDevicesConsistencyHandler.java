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
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.extension.protobuf.IdentifiableMessage;
import org.openbase.jul.extension.protobuf.container.ProtoBufMessageMapInterface;
import org.openbase.jul.storage.registry.AbstractProtoBufRegistryConsistencyHandler;
import org.openbase.jul.storage.registry.EntryModification;
import org.openbase.jul.storage.registry.ProtoBufRegistryInterface;
import rst.homeautomation.device.DeviceConfigType.DeviceConfig;
import rst.homeautomation.state.InventoryStateType.InventoryState;

/**
 *
 * @author <a href="mailto:thuxohl@techfak.uni-bielefeld.com">Tamino Huxohl</a>
 */
public class DeviceConfigLocationIdForInstalledDevicesConsistencyHandler extends AbstractProtoBufRegistryConsistencyHandler<String, DeviceConfig, DeviceConfig.Builder> {

    @Override
    public void processData(String id, IdentifiableMessage<String, DeviceConfig, DeviceConfig.Builder> entry, ProtoBufMessageMapInterface<String, DeviceConfig, DeviceConfig.Builder> entryMap, ProtoBufRegistryInterface<String, DeviceConfig, DeviceConfig.Builder> registry) throws CouldNotPerformException, EntryModification {
        DeviceConfig.Builder deviceConfig = entry.getMessage().toBuilder();

        if (!deviceConfig.hasInventoryState()) {
            throw new NotAvailableException("inventoryState");
        }

        if (!deviceConfig.hasPlacementConfig()) {
            throw new NotAvailableException("placementConfig");
        }

        if (!deviceConfig.getPlacementConfig().hasLocationId() || deviceConfig.getPlacementConfig().getLocationId().isEmpty()) {
            throw new NotAvailableException("placementConfig.locationId");
        }

        if (deviceConfig.getInventoryState().getValue() == InventoryState.State.INSTALLED && !deviceConfig.getInventoryState().getLocationId().equals(deviceConfig.getPlacementConfig().getLocationId())) {
            deviceConfig.setInventoryState(deviceConfig.getInventoryStateBuilder().setLocationId(deviceConfig.getPlacementConfig().getLocationId()));
            throw new EntryModification(entry.setMessage(deviceConfig), this);
        }
    }

    @Override
    public void reset() {
    }
}
