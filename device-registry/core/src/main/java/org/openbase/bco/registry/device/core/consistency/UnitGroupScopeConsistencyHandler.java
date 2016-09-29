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
import rst.homeautomation.unit.UnitGroupConfigType.UnitGroupConfig;
import rst.rsb.ScopeType;
import rst.spatial.LocationConfigType.LocationConfig;
import org.openbase.jul.extension.protobuf.container.ProtoBufMessageMap;
import org.openbase.jul.storage.registry.ProtoBufRegistry;

/**
 *
 @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public class UnitGroupScopeConsistencyHandler extends AbstractProtoBufRegistryConsistencyHandler<String, UnitGroupConfig, UnitGroupConfig.Builder> {

    private final LocationRegistryRemote locationRegistryRemote;

    public UnitGroupScopeConsistencyHandler(final LocationRegistryRemote locationRegistryRemote) {
        this.locationRegistryRemote = locationRegistryRemote;
    }

    @Override
    public void processData(String id, IdentifiableMessage<String, UnitGroupConfig, UnitGroupConfig.Builder> entry, ProtoBufMessageMap<String, UnitGroupConfig, UnitGroupConfig.Builder> entryMap, ProtoBufRegistry<String, UnitGroupConfig, UnitGroupConfig.Builder> registry) throws CouldNotPerformException, EntryModification {
        UnitGroupConfig unitGroupConfig = entry.getMessage();

        if (!unitGroupConfig.hasPlacementConfig()) {
            throw new NotAvailableException("unitGroupConfig.placementconfig");
        }
        if (!unitGroupConfig.getPlacementConfig().hasLocationId() || unitGroupConfig.getPlacementConfig().getLocationId().isEmpty()) {
            throw new NotAvailableException("unitGroupConfig.placementconfig.locationid");
        }

        LocationConfig locationConfig = locationRegistryRemote.getLocationConfigById(unitGroupConfig.getPlacementConfig().getLocationId());
        ScopeType.Scope newScope = ScopeGenerator.generateUnitGroupScope(unitGroupConfig, locationConfig);

        // verify and update scope
        if (!ScopeGenerator.generateStringRep(unitGroupConfig.getScope()).equals(ScopeGenerator.generateStringRep(newScope))) {
            entry.setMessage(unitGroupConfig.toBuilder().setScope(newScope));
            throw new EntryModification(entry, this);
        }
    }
}
