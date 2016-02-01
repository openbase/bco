/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dc.bco.registry.location.core.consistency;

/*
 * #%L
 * REM LocationRegistry Core
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import org.dc.jul.exception.CouldNotPerformException;
import org.dc.jul.exception.InvalidStateException;
import org.dc.jul.exception.NotAvailableException;
import org.dc.jul.extension.protobuf.IdentifiableMessage;
import org.dc.jul.extension.protobuf.container.ProtoBufMessageMapInterface;
import org.dc.jul.storage.registry.AbstractProtoBufRegistryConsistencyHandler;
import org.dc.jul.storage.registry.EntryModification;
import org.dc.jul.storage.registry.ProtoBufRegistryInterface;
import rst.spatial.LocationConfigType;
import rst.spatial.LocationConfigType.LocationConfig;

/**
 *
 * @author mpohling
 */
public class LocationIdConsistencyHandler extends AbstractProtoBufRegistryConsistencyHandler<String, LocationConfigType.LocationConfig, LocationConfigType.LocationConfig.Builder> {

    @Override
    public void processData(String id, IdentifiableMessage<String, LocationConfig, LocationConfig.Builder> entry, ProtoBufMessageMapInterface<String, LocationConfig, LocationConfig.Builder> entryMap, ProtoBufRegistryInterface<String, LocationConfig, LocationConfig.Builder> registry) throws CouldNotPerformException, EntryModification {

        LocationConfig.Builder locationConfig = entry.getMessage().toBuilder();

        if (!locationConfig.hasPlacementConfig()) {
            throw new NotAvailableException("locationconfig.placementconfig");
        }

        // check if location id is setuped.
        if (!locationConfig.getPlacementConfig().hasLocationId()) {

            // detect root location
            LocationConfig.Builder setPlacementConfig = locationConfig.setPlacementConfig(locationConfig.getPlacementConfig().toBuilder().setLocationId(detectRootLocation(locationConfig.build(), entryMap).getId()));
            entry.setMessage(setPlacementConfig);
            throw new EntryModification(entry, this);
        }
    }

    public LocationConfig detectRootLocation(final LocationConfig currentLocationConfig, ProtoBufMessageMapInterface<String, LocationConfig, LocationConfig.Builder> entryMap) throws CouldNotPerformException {
        try {
            logger.info("detectNewRootLocation in registry [" + Arrays.toString(entryMap.getMessages().toArray()) + "]");
            for (LocationConfig locationConfig : entryMap.getMessages()) {
                if (locationConfig.hasRoot() && locationConfig.getRoot()) {
                    return locationConfig;
                }
            }
            return computeNewRootLocation(currentLocationConfig, entryMap);
        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not detect root location!");
        }
    }

    public LocationConfig computeNewRootLocation(final LocationConfig currentLocationConfig, ProtoBufMessageMapInterface<String, LocationConfig, LocationConfig.Builder> entryMap) throws CouldNotPerformException {
        try {
            HashMap<String, LocationConfig> rootLocationConfigList = new HashMap<>();
            logger.info("computeNewRootLocation ");
            for (LocationConfig locationConfig : entryMap.getMessages()) {
                rootLocationConfigList.put(locationConfig.getId(), locationConfig);
            }

            rootLocationConfigList.put(currentLocationConfig.getId(), currentLocationConfig);

            if (rootLocationConfigList.size() == 1) {
                return rootLocationConfigList.values().stream().findFirst().get();
            }

            for (LocationConfig locationConfig : new ArrayList<>(rootLocationConfigList.values())) {
                if (!locationConfig.hasPlacementConfig()) {
                } else if (!locationConfig.getPlacementConfig().hasLocationId()) {
                } else if (locationConfig.getPlacementConfig().getLocationId().isEmpty()) {
                } else if (locationConfig.getPlacementConfig().getLocationId().equals(locationConfig.getId())) {
                    return locationConfig;
                } else {
                    rootLocationConfigList.remove(locationConfig.getId());
                }
            }

            if (rootLocationConfigList.isEmpty()) {
                throw new InvalidStateException("Could not compute root location!");
            } else if (rootLocationConfigList.size() == 1) {
                return rootLocationConfigList.values().stream().findFirst().get();
            }

            throw new InvalidStateException("To many potential root locations detected!");

        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not compute root location!", ex);
        }
    }
}
