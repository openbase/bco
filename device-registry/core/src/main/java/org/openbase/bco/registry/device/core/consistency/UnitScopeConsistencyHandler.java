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
import java.util.Map;
import java.util.TreeMap;
import org.openbase.bco.registry.location.remote.LocationRegistryRemote;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InvalidStateException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.extension.protobuf.IdentifiableMessage;
import org.openbase.jul.extension.protobuf.container.ProtoBufMessageMap;
import org.openbase.jul.extension.rsb.scope.ScopeGenerator;
import org.openbase.jul.storage.registry.AbstractProtoBufRegistryConsistencyHandler;
import org.openbase.jul.storage.registry.EntryModification;
import org.openbase.jul.storage.registry.ProtoBufRegistry;
import rst.homeautomation.device.DeviceConfigType;
import rst.homeautomation.device.DeviceConfigType.DeviceConfig;
import rst.homeautomation.unit.UnitConfigType.UnitConfig;
import rst.rsb.ScopeType;

/**
 *
 @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public class UnitScopeConsistencyHandler extends AbstractProtoBufRegistryConsistencyHandler<String, DeviceConfig, DeviceConfig.Builder> {

    private final LocationRegistryRemote locationRegistryRemote;
    private final Map<String, UnitConfig> unitScopeMap;

    public UnitScopeConsistencyHandler(final LocationRegistryRemote locationRegistryRemote) {
        this.locationRegistryRemote = locationRegistryRemote;
        this.unitScopeMap = new TreeMap<>();
    }

    @Override
    public void processData(String id, IdentifiableMessage<String, DeviceConfig, DeviceConfig.Builder> entry, ProtoBufMessageMap<String, DeviceConfig, DeviceConfig.Builder> entryMap, ProtoBufRegistry<String, DeviceConfig, DeviceConfig.Builder> registry) throws CouldNotPerformException, EntryModification {
        DeviceConfigType.DeviceConfig.Builder deviceConfig = entry.getMessage().toBuilder();

        deviceConfig.clearUnitConfig();
        boolean modification = false;
        for (UnitConfig.Builder unitConfig : entry.getMessage().toBuilder().getUnitConfigBuilderList()) {
            UnitConfig unitConfigClone = UnitConfig.newBuilder(unitConfig.build()).build();

            if (!unitConfigClone.hasPlacementConfig()) {
                throw new NotAvailableException("placementconfig");
            }

            if (!unitConfigClone.getPlacementConfig().hasLocationId() || unitConfigClone.getPlacementConfig().getLocationId().isEmpty()) {
                throw new NotAvailableException("placementconfig.locationid");
            }

            ScopeType.Scope newScope = ScopeGenerator.generateUnitScope(unitConfigClone, locationRegistryRemote.getLocationConfigById(unitConfigClone.getPlacementConfig().getLocationId()));

            // verify and update scope
            if (!ScopeGenerator.generateStringRep(unitConfig.getScope()).equals(ScopeGenerator.generateStringRep(newScope))) {
                unitConfig.setScope(newScope);
                modification = true;
            }

            if (unitScopeMap.containsKey(ScopeGenerator.generateStringRep(unitConfig.getScope()))) {
                throw new InvalidStateException("Two units with same scope[" + ScopeGenerator.generateStringRep(unitConfig.getScope()) + "] detected provided by Device[" + deviceConfig.getId() + "] and Device[" + unitScopeMap.get(ScopeGenerator.generateStringRep(unitConfig.getScope())).getUnitHostId() + "]!");
            }
            unitScopeMap.put(ScopeGenerator.generateStringRep(unitConfig.getScope()), unitConfig.build());

            deviceConfig.addUnitConfig(unitConfig);
        }

        if (modification) {
            throw new EntryModification(entry.setMessage(deviceConfig), this);
        }
    }

    @Override
    public void reset() {
        unitScopeMap.clear();
    }
}
