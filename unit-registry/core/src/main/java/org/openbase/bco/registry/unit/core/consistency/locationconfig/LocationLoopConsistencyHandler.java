package org.openbase.bco.registry.unit.core.consistency.locationconfig;

/*
 * #%L
 * BCO Registry Unit Core
 * %%
 * Copyright (C) 2014 - 2017 openbase.org
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
import java.util.List;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InvalidStateException;
import org.openbase.jul.extension.protobuf.IdentifiableMessage;
import org.openbase.jul.extension.protobuf.container.ProtoBufMessageMap;
import org.openbase.jul.storage.registry.AbstractProtoBufRegistryConsistencyHandler;
import org.openbase.jul.storage.registry.EntryModification;
import org.openbase.jul.storage.registry.ProtoBufRegistry;
import rst.domotic.unit.UnitConfigType.UnitConfig;

/**
 *
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public class LocationLoopConsistencyHandler extends AbstractProtoBufRegistryConsistencyHandler<String, UnitConfig, UnitConfig.Builder> {

    public LocationLoopConsistencyHandler() {
    }

    @Override
    public void processData(String id, IdentifiableMessage<String, UnitConfig, UnitConfig.Builder> entry, ProtoBufMessageMap<String, UnitConfig, UnitConfig.Builder> entryMap, ProtoBufRegistry<String, UnitConfig, UnitConfig.Builder> registry) throws CouldNotPerformException, EntryModification {
        loopTestBottomUp(entry.getMessage(), registry);
    }

    private void loopTestBottomUp(final UnitConfig locationConfig, ProtoBufRegistry<String, UnitConfig, UnitConfig.Builder> registry) throws InvalidStateException, CouldNotPerformException {
        loopTestBottomUp(locationConfig, registry, new ArrayList<>());
    }

    private void loopTestBottomUp(final UnitConfig locationConfig, ProtoBufRegistry<String, UnitConfig, UnitConfig.Builder> registry, List<String> processedLocations) throws InvalidStateException, CouldNotPerformException {

        if (!locationConfig.hasPlacementConfig()) {
            return;
        }

        if (!locationConfig.getPlacementConfig().hasLocationId() && locationConfig.getPlacementConfig().getLocationId().isEmpty()) {
            return;
        }

        markAsProcessed(locationConfig, processedLocations);

        // ignore root notes
        if (locationConfig.getLocationConfig().getRoot()) {
            return;
        }

        loopTestBottomUp(registry.get(locationConfig.getPlacementConfig().getLocationId()).getMessage(), registry, processedLocations);
    }

    private void loopTestTopDown(final UnitConfig locationConfig, ProtoBufRegistry<String, UnitConfig, UnitConfig.Builder> registry) throws InvalidStateException, CouldNotPerformException {
        loopTestTopDown(locationConfig, registry, new ArrayList<>());
    }

    private void loopTestTopDown(final UnitConfig locationConfig, ProtoBufRegistry<String, UnitConfig, UnitConfig.Builder> registry, List<String> processedLocations) throws InvalidStateException, CouldNotPerformException {
        markAsProcessed(locationConfig, processedLocations);

        for (String locationId : locationConfig.getLocationConfig().getChildIdList()) {
            loopTestTopDown(registry.get(locationId).getMessage(), registry, processedLocations);
        }
    }

    private void markAsProcessed(final UnitConfig locationConfig, List<String> processedLocations) throws InvalidStateException {
        if (processedLocations.contains(locationConfig.getId())) {
            throw new InvalidStateException("Location loop detected!");
        }
        processedLocations.add(locationConfig.getId());
    }
}
