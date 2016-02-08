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
import org.dc.jul.exception.CouldNotPerformException;
import org.dc.jul.exception.InvalidStateException;
import org.dc.jul.extension.protobuf.IdentifiableMessage;
import org.dc.jul.extension.protobuf.container.ProtoBufMessageMapInterface;
import org.dc.jul.storage.registry.AbstractProtoBufRegistryConsistencyHandler;
import org.dc.jul.storage.registry.EntryModification;
import org.dc.jul.storage.registry.ProtoBufRegistryInterface;
import java.util.ArrayList;
import java.util.List;
import rst.spatial.LocationConfigType;
import rst.spatial.LocationConfigType.LocationConfig;

/**
 *
 * @author <a href="mailto:mpohling@cit-ec.uni-bielefeld.de">Divine Threepwood</a>
 */
public class LocationLoopConsistencyHandler extends AbstractProtoBufRegistryConsistencyHandler<String, LocationConfig, LocationConfig.Builder> {

    public LocationLoopConsistencyHandler() {
    }

    @Override
    public void processData(String id, IdentifiableMessage<String, LocationConfig, LocationConfig.Builder> entry, ProtoBufMessageMapInterface<String, LocationConfigType.LocationConfig, LocationConfigType.LocationConfig.Builder> entryMap, ProtoBufRegistryInterface<String, LocationConfigType.LocationConfig, LocationConfigType.LocationConfig.Builder> registry) throws CouldNotPerformException, EntryModification {
        loopTestBottomUp(entry.getMessage(), registry);
    }

    private void loopTestBottomUp(final LocationConfig locationConfig, ProtoBufRegistryInterface<String, LocationConfig, LocationConfig.Builder> registry) throws InvalidStateException, CouldNotPerformException {
        loopTestBottomUp(locationConfig, registry, new ArrayList<String>());
    }

    private void loopTestBottomUp(final LocationConfig locationConfig, ProtoBufRegistryInterface<String, LocationConfig, LocationConfig.Builder> registry, List<String> processedLocations) throws InvalidStateException, CouldNotPerformException {

        if (!locationConfig.hasPlacementConfig()) {
            return;
        }

        if (!locationConfig.getPlacementConfig().hasLocationId() && locationConfig.getPlacementConfig().getLocationId().isEmpty()) {
            return;
        }

        markAsProcessed(locationConfig, processedLocations);

        // ignore root notes
        if (locationConfig.getRoot()) {
            return;
        }

        loopTestBottomUp(registry.get(locationConfig.getPlacementConfig().getLocationId()).getMessage(), registry, processedLocations);
    }

    private void loopTestTopDown(final LocationConfig locationConfig, ProtoBufRegistryInterface<String, LocationConfig, LocationConfig.Builder> registry) throws InvalidStateException, CouldNotPerformException {
        loopTestTopDown(locationConfig, registry, new ArrayList<String>());
    }

    private void loopTestTopDown(final LocationConfig locationConfig, ProtoBufRegistryInterface<String, LocationConfig, LocationConfig.Builder> registry, List<String> processedLocations) throws InvalidStateException, CouldNotPerformException {
        markAsProcessed(locationConfig, processedLocations);

        for (String locationId : locationConfig.getChildIdList()) {
            loopTestTopDown(registry.get(locationId).getMessage(), registry, processedLocations);
        }
    }

    private void markAsProcessed(final LocationConfigType.LocationConfig locationConfig, List<String> processedLocations) throws InvalidStateException {
        if (processedLocations.contains(locationConfig.getId())) {
            throw new InvalidStateException("Location loop detected!");
        }
        processedLocations.add(locationConfig.getId());
    }

    @Override
    public void reset() {
    }
}
