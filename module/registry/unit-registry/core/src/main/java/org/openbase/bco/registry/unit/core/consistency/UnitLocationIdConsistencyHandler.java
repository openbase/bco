package org.openbase.bco.registry.unit.core.consistency;

/*
 * #%L
 * BCO Registry Unit Core
 * %%
 * Copyright (C) 2014 - 2021 openbase.org
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

import org.openbase.bco.registry.lib.util.LocationUtils;
import org.openbase.bco.registry.lib.util.UnitConfigProcessor;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.extension.protobuf.IdentifiableMessage;
import org.openbase.jul.extension.protobuf.container.ProtoBufMessageMap;
import org.openbase.jul.storage.registry.AbstractProtoBufRegistryConsistencyHandler;
import org.openbase.jul.storage.registry.EntryModification;
import org.openbase.jul.storage.registry.ProtoBufRegistry;
import org.openbase.type.domotic.unit.UnitConfigType.UnitConfig;
import org.openbase.type.spatial.PlacementConfigType.PlacementConfig;

/**
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public class UnitLocationIdConsistencyHandler extends AbstractProtoBufRegistryConsistencyHandler<String, UnitConfig, UnitConfig.Builder> {

    private final ProtoBufRegistry<String, UnitConfig, UnitConfig.Builder> locationRegistry;
    private final ProtoBufRegistry<String, UnitConfig, UnitConfig.Builder> dalUnitRegistry;


    public UnitLocationIdConsistencyHandler(final ProtoBufRegistry<String, UnitConfig, UnitConfig.Builder> locationRegistry, final ProtoBufRegistry<String, UnitConfig, UnitConfig.Builder> dalUnitRegistry) {
        this.locationRegistry = locationRegistry;
        this.dalUnitRegistry = dalUnitRegistry;
    }

    @Override
    public void processData(String id, IdentifiableMessage<String, UnitConfig, UnitConfig.Builder> entry, ProtoBufMessageMap<String, UnitConfig, UnitConfig.Builder> entryMap, ProtoBufRegistry<String, UnitConfig, UnitConfig.Builder> registry) throws CouldNotPerformException, EntryModification {
        UnitConfig.Builder unitConfig = entry.getMessage().toBuilder();

        if (!unitConfig.hasPlacementConfig()) {
            throw new NotAvailableException("unitconfig.placementconfig");
        }

        // setup location id if missing.
        if (!unitConfig.hasPlacementConfig() || !unitConfig.getPlacementConfig().hasLocationId() || unitConfig.getPlacementConfig().getLocationId().isEmpty()) {

            String rootLocationId;
            try {
                // resolution via more frequently updated entryMap via consistency checks if this entryMap contains the locations.
                rootLocationId = LocationUtils.getRootLocation(entryMap).getId();
            } catch (CouldNotPerformException ex) {
                try {
                    // resolution via the location registry.
                    rootLocationId = LocationUtils.getRootLocation(locationRegistry.getMessages()).getId();
                } catch (CouldNotPerformException exx) {
                    // if the root location could not be detected this consistency check is not needed.
                    return;
                }
            }

            PlacementConfig rootPlacement = PlacementConfig.newBuilder().setLocationId(rootLocationId).build();
            throw new EntryModification(entry.setMessage(unitConfig.setPlacementConfig(rootPlacement), this), this);
        }

        // verify if configured location exists.
        if (!locationRegistry.contains(unitConfig.getPlacementConfig().getLocationId())) {
            String locationId = null;

            // for dal units recover location from host unit location if available
            if (UnitConfigProcessor.isDalUnit(unitConfig) && UnitConfigProcessor.isHostUnitAvailable(unitConfig)) {
                outer:
                for (UnitConfig hostUnitConfig : dalUnitRegistry.getMessages()) {
                    if (hostUnitConfig.getId().equals(unitConfig.getUnitHostId())) {

                        // recover location from host unit if it is at a valid location
                        if (hostUnitConfig.getPlacementConfig().hasLocationId() && locationRegistry.contains(hostUnitConfig.getPlacementConfig().getLocationId())) {
                            locationId = hostUnitConfig.getPlacementConfig().getLocationId();
                        }
                        break outer;
                    }
                }
            }

            if (locationId == null) {
                // set root location as default location.
                try {
                    locationId = LocationUtils.getRootLocation(locationRegistry.getMessages()).getId();
                } catch (CouldNotPerformException ex) {
                    // if root location is not yet available we skip this consistency handling.
                    return;
                }
            }

            final PlacementConfig placementConfig = PlacementConfig.newBuilder().setLocationId(locationId).build();
            throw new EntryModification(entry.setMessage(unitConfig.setPlacementConfig(placementConfig), this), this);
        }
    }
}
