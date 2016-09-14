package org.openbase.bco.registry.location.core.consistency;

/*
 * #%L
 * REM LocationRegistry Core
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
import org.openbase.bco.registry.device.remote.DeviceRegistryRemote;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.extension.protobuf.IdentifiableMessage;
import org.openbase.jul.storage.registry.AbstractProtoBufRegistryConsistencyHandler;
import org.openbase.jul.storage.registry.EntryModification;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import rst.homeautomation.device.DeviceConfigType;
import rst.homeautomation.state.InventoryStateType;
import rst.homeautomation.unit.UnitConfigType;
import rst.spatial.LocationConfigType.LocationConfig;
import org.openbase.jul.extension.protobuf.container.ProtoBufMessageMap;
import org.openbase.jul.storage.registry.ProtoBufRegistry;

/**
 *
 * @author mpohling
 */
public class LocationUnitIdConsistencyHandler extends AbstractProtoBufRegistryConsistencyHandler<String, LocationConfig, LocationConfig.Builder> {

    private final DeviceRegistryRemote deviceRegistryRemote;

    public LocationUnitIdConsistencyHandler(final DeviceRegistryRemote deviceRegistryRemote) {
        this.deviceRegistryRemote = deviceRegistryRemote;
    }

    @Override
    public void processData(String id, IdentifiableMessage<String, LocationConfig, LocationConfig.Builder> entry, ProtoBufMessageMap<String, LocationConfig, LocationConfig.Builder> entryMap, ProtoBufRegistry<String, LocationConfig, LocationConfig.Builder> registry) throws CouldNotPerformException, EntryModification {
        LocationConfig locationConfig = entry.getMessage();

        Collection<String> lookupUnitIds;

        try {
            lookupUnitIds = lookupUnitIds(locationConfig, registry);
        } catch (NotAvailableException ex) {
            lookupUnitIds = new ArrayList<>();
        }

        // verify and update unit ids
        if (generateListHash(locationConfig.getUnitIdList()) != generateListHash(lookupUnitIds)) {
            entry.setMessage(locationConfig.toBuilder().clearUnitId().addAllUnitId(lookupUnitIds));
            throw new EntryModification(entry, this);
        }
    }

    private Collection<String> lookupUnitIds(final LocationConfig locationConfig, ProtoBufRegistry<String, LocationConfig, LocationConfig.Builder> registry) throws CouldNotPerformException {
        try {

            if (!locationConfig.hasId() || locationConfig.getId().isEmpty()) {
                throw new NotAvailableException("location id");
            }

            Set<String> unitIdList = new HashSet<>();
            try {
                for (UnitConfigType.UnitConfig unitConfig : deviceRegistryRemote.getUnitConfigs()) {

                    // skip units with no placenment config
                    if (!unitConfig.hasPlacementConfig()) {
                        continue;
                    } else if (!unitConfig.getPlacementConfig().hasLocationId()) {
                        continue;
                    } else if (unitConfig.getPlacementConfig().getLocationId().isEmpty()) {
                        continue;
                    }

                    // filter units which are currently not installed
                    DeviceConfigType.DeviceConfig deviceConfig = deviceRegistryRemote.getDeviceConfigById(unitConfig.getSystemUnitId());
                    if (deviceConfig.getInventoryState().getValue() != InventoryStateType.InventoryState.State.INSTALLED) {
                        continue;
                    }

                    // add direct assigned units
                    if (unitConfig.getPlacementConfig().getLocationId().equals(locationConfig.getId())) {
                        unitIdList.add(unitConfig.getId());
                    }

                    // add child units
                    for (String childId : locationConfig.getChildIdList()) {
                        unitIdList.addAll(registry.get(childId).getMessage().getUnitIdList());
                    }
                }
            } catch (NotAvailableException ex) {
                // no units available for this location...
            }
            return unitIdList;
        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Unit id loockup failed!", ex);
        }
    }

    private int generateListHash(Collection<String> list) {
        int hash = 0;
        for (String entry : list) {
            hash += entry.hashCode();
        }
        return hash;
    }

    @Override
    public void reset() {
    }
}
