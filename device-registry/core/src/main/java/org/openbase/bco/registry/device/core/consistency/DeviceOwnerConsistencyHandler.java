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
import org.openbase.bco.registry.user.remote.UserRegistryRemote;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.extension.protobuf.IdentifiableMessage;
import org.openbase.jul.storage.registry.AbstractProtoBufRegistryConsistencyHandler;
import org.openbase.jul.storage.registry.EntryModification;
import rst.homeautomation.device.DeviceConfigType;
import rst.homeautomation.state.InventoryStateType.InventoryState;
import org.openbase.jul.extension.protobuf.container.ProtoBufMessageMap;
import org.openbase.jul.storage.registry.ProtoBufRegistry;

/**
 *
 * @author <a href="mailto:thuxohl@techfak.uni-bielefeld.com">Tamino Huxohl</a>
 */
public class DeviceOwnerConsistencyHandler extends AbstractProtoBufRegistryConsistencyHandler<String, DeviceConfigType.DeviceConfig, DeviceConfigType.DeviceConfig.Builder> {

    private final UserRegistryRemote userRegistryRemote;

    public DeviceOwnerConsistencyHandler(final UserRegistryRemote userRegistryRemote) {
        this.userRegistryRemote = userRegistryRemote;
    }

    @Override
    public void processData(String id, IdentifiableMessage<String, DeviceConfigType.DeviceConfig, DeviceConfigType.DeviceConfig.Builder> entry, ProtoBufMessageMap<String, DeviceConfigType.DeviceConfig, DeviceConfigType.DeviceConfig.Builder> entryMap, ProtoBufRegistry<String, DeviceConfigType.DeviceConfig, DeviceConfigType.DeviceConfig.Builder> registry) throws CouldNotPerformException, EntryModification {
        DeviceConfigType.DeviceConfig.Builder deviceConfig = entry.getMessage().toBuilder();

        // verify if configured owner exists.
        if (deviceConfig.hasInventoryState() && deviceConfig.getInventoryState().hasOwnerId() && !deviceConfig.getInventoryState().getOwnerId().isEmpty() && !userRegistryRemote.containsUserConfigById(deviceConfig.getInventoryState().getOwnerId())) {
            // remove unknown owner
            deviceConfig.setInventoryState(InventoryState.newBuilder(deviceConfig.getInventoryState()).setOwnerId(""));
            throw new EntryModification(entry.setMessage(deviceConfig), this);
        }
    }
}
