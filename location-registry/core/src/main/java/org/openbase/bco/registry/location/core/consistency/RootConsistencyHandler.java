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
import org.openbase.bco.registry.location.lib.util.LocationUtils;
import static org.openbase.bco.registry.location.lib.util.LocationUtils.getRootLocation;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.extension.protobuf.IdentifiableMessage;
import org.openbase.jul.extension.protobuf.container.ProtoBufMessageMapInterface;
import org.openbase.jul.storage.registry.AbstractProtoBufRegistryConsistencyHandler;
import org.openbase.jul.storage.registry.EntryModification;
import org.openbase.jul.storage.registry.ProtoBufRegistryInterface;
import rst.spatial.LocationConfigType;
import rst.spatial.LocationConfigType.LocationConfig;

/**
 *
 * @author mpohling
 */
public class RootConsistencyHandler extends AbstractProtoBufRegistryConsistencyHandler<String, LocationConfigType.LocationConfig, LocationConfigType.LocationConfig.Builder> {

    @Override
    public void processData(String id, IdentifiableMessage<String, LocationConfig, LocationConfig.Builder> entry, ProtoBufMessageMapInterface<String, LocationConfig, LocationConfig.Builder> entryMap, ProtoBufRegistryInterface<String, LocationConfig, LocationConfig.Builder> registry) throws CouldNotPerformException, EntryModification {
        LocationConfig.Builder locationConfig = entry.getMessage().toBuilder();

        // check if placement exists.
        if (!locationConfig.hasPlacementConfig()) {
            throw new NotAvailableException("locationconfig.placementconfig");
        }

        // check if root flag is missing for root node.
        if (locationConfig.getPlacementConfig().hasLocationId() && locationConfig.getPlacementConfig().getLocationId().equals(locationConfig.getId()) && !locationConfig.getRoot()) {
            entry.setMessage(locationConfig.setRoot(true).setPlacementConfig(locationConfig.getPlacementConfig().toBuilder()));
            throw new EntryModification(entry, this);
        }

        // check if root flag is set for child node.
        if (locationConfig.getPlacementConfig().hasLocationId() && !locationConfig.getPlacementConfig().getLocationId().equals(locationConfig.getId()) && locationConfig.getRoot()) {
            try {
                // verify that global root node exists which is not this location.
                LocationConfig globalRootLocation = LocationUtils.getRootLocation(entryMap);
                if (globalRootLocation.getId().equals(locationConfig.getId())) {
                    throw new NotAvailableException("valid root node");
                }
            } catch (NotAvailableException ex) {
                // no parent node found or given one is invalid.
                // setup location parent as new root location.
                try {
                    LocationUtils.validateRootLocation(entryMap.getMessage(locationConfig.getPlacementConfig().getLocationId()), entryMap, this);
                } catch (EntryModification exx) {
                    // parent updated
                }
            }

            // delete invalid root flag for current location.
            entry.setMessage(locationConfig.setRoot(false).build());
            throw new EntryModification(entry, this);
        }

        // check if root field is avaible
        if (!locationConfig.hasRoot()) {
            entry.setMessage(locationConfig.setRoot(false));
            throw new EntryModification(entry, this);
        }
    }
}
