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
import org.openbase.jul.extension.rsb.scope.ScopeGenerator;
import org.openbase.jul.storage.registry.AbstractProtoBufRegistryConsistencyHandler;
import org.openbase.jul.storage.registry.EntryModification;
import org.openbase.bco.registry.location.remote.LocationRegistryRemote;
import rst.homeautomation.device.DeviceConfigType;
import rst.homeautomation.device.DeviceConfigType.DeviceConfig;
import rst.rsb.ScopeType;
import rst.spatial.LocationConfigType.LocationConfig;
import org.openbase.jul.extension.protobuf.container.ProtoBufMessageMap;
import org.openbase.jul.storage.registry.ProtoBufRegistry;

/**
 *
 * @author mpohling
 */
public class DeviceScopeConsistencyHandler extends AbstractProtoBufRegistryConsistencyHandler<String, DeviceConfig, DeviceConfig.Builder> {

    private final LocationRegistryRemote locationRegistryRemote;

    public DeviceScopeConsistencyHandler(final LocationRegistryRemote locationRegistryRemote) {
        this.locationRegistryRemote = locationRegistryRemote;
    }

    @Override
    public void processData(String id, IdentifiableMessage<String, DeviceConfig, DeviceConfig.Builder> entry, ProtoBufMessageMap<String, DeviceConfig, DeviceConfig.Builder> entryMap, ProtoBufRegistry<String, DeviceConfig, DeviceConfig.Builder> registry) throws CouldNotPerformException, EntryModification {
        DeviceConfigType.DeviceConfig deviceConfig = entry.getMessage();

        if (!deviceConfig.hasPlacementConfig()) {
            throw new NotAvailableException("deviceconfig.placementconfig");
        }
        if (!deviceConfig.getPlacementConfig().hasLocationId() || deviceConfig.getPlacementConfig().getLocationId().isEmpty()) {
            throw new NotAvailableException("deviceconfig.placementconfig.locationid");
        }

        LocationConfig locationConfig = locationRegistryRemote.getLocationConfigById(deviceConfig.getPlacementConfig().getLocationId());
        ScopeType.Scope newScope = ScopeGenerator.generateDeviceScope(deviceConfig, locationConfig);

        // verify and update scope
        if (!ScopeGenerator.generateStringRep(deviceConfig.getScope()).equals(ScopeGenerator.generateStringRep(newScope))) {
            entry.setMessage(deviceConfig.toBuilder().setScope(newScope));
            throw new EntryModification(entry, this);
        }
    }
}
