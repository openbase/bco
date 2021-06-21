package org.openbase.bco.registry.unit.core.consistency.locationconfig;

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
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.extension.protobuf.IdentifiableMessage;
import org.openbase.jul.extension.protobuf.container.ProtoBufMessageMap;
import org.openbase.jul.storage.registry.AbstractProtoBufRegistryConsistencyHandler;
import org.openbase.jul.storage.registry.EntryModification;
import org.openbase.jul.storage.registry.ProtoBufRegistry;
import org.openbase.type.domotic.unit.UnitConfigType.UnitConfig;
import org.openbase.type.domotic.unit.location.LocationConfigType.LocationConfig;

/**
 *
 @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public class RootConsistencyHandler extends AbstractProtoBufRegistryConsistencyHandler<String, UnitConfig, UnitConfig.Builder> {

    @Override
    public void processData(String id, IdentifiableMessage<String, UnitConfig, UnitConfig.Builder> entry, ProtoBufMessageMap<String, UnitConfig, UnitConfig.Builder> entryMap, ProtoBufRegistry<String, UnitConfig, UnitConfig.Builder> registry) throws CouldNotPerformException, EntryModification {
        UnitConfig.Builder locationUnitConfig = entry.getMessage().toBuilder();
        LocationConfig.Builder locationConfig = locationUnitConfig.getLocationConfigBuilder();

        // check if placement exists.
        if (!locationUnitConfig.hasPlacementConfig()) {
            throw new NotAvailableException("locationconfig.placementconfig");
        }

        // check if root flag is missing for root node.
        if (locationUnitConfig.getPlacementConfig().hasLocationId() && locationUnitConfig.getPlacementConfig().getLocationId().equals(locationUnitConfig.getId()) && !locationConfig.getRoot()) {
            locationConfig.setRoot(true);
            entry.setMessage(locationUnitConfig.setPlacementConfig(locationUnitConfig.getPlacementConfig().toBuilder()), this);
            throw new EntryModification(entry, this);
        }

        // check if root flag is set for child node.
        if (locationUnitConfig.getPlacementConfig().hasLocationId() && !locationUnitConfig.getPlacementConfig().getLocationId().equals(locationUnitConfig.getId()) && locationConfig.getRoot()) {
            try {
                // verify that global root node exists which is not this location.
                UnitConfig globalRootLocation = LocationUtils.getRootLocation(entryMap);
                if (globalRootLocation.getId().equals(locationUnitConfig.getId())) {
                    throw new NotAvailableException("valid root location");
                }
            } catch (NotAvailableException ex) {
                // no parent node found or given one is invalid.
                // setup location parent as new root location.
                try {
                    LocationUtils.validateRootLocation(entryMap.getMessage(locationUnitConfig.getPlacementConfig().getLocationId()), entryMap, this);
                } catch (EntryModification exx) {
                    // parent updated
                }
            }

            // delete invalid root flag for current location.
            locationConfig.setRoot(false);
            entry.setMessage(locationUnitConfig, this);
            throw new EntryModification(entry, this);
        }

        // check if root field is available
        if (!locationConfig.hasRoot()) {
            locationConfig.setRoot(false);
            throw new EntryModification(entry.setMessage(locationUnitConfig, this), this);
        }
    }
}
