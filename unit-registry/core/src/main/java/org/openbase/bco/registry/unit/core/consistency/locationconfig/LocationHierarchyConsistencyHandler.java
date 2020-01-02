package org.openbase.bco.registry.unit.core.consistency.locationconfig;

/*-
 * #%L
 * BCO Registry Unit Core
 * %%
 * Copyright (C) 2014 - 2020 openbase.org
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
import org.openbase.jul.extension.protobuf.container.ProtoBufMessageMap;
import org.openbase.jul.storage.registry.AbstractProtoBufRegistryConsistencyHandler;
import org.openbase.jul.storage.registry.EntryModification;
import org.openbase.jul.storage.registry.ProtoBufRegistry;
import org.openbase.type.domotic.unit.UnitConfigType.UnitConfig;
import org.openbase.type.domotic.unit.location.LocationConfigType.LocationConfig.LocationType;

/**
 *
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class LocationHierarchyConsistencyHandler extends AbstractProtoBufRegistryConsistencyHandler<String, UnitConfig, UnitConfig.Builder> {

    @Override
    public void processData(String id, IdentifiableMessage<String, UnitConfig, UnitConfig.Builder> entry, ProtoBufMessageMap<String, UnitConfig, UnitConfig.Builder> entryMap, ProtoBufRegistry<String, UnitConfig, UnitConfig.Builder> registry) throws CouldNotPerformException, EntryModification {
        UnitConfig.Builder locationUnit = entry.getMessage().toBuilder();

        if (!locationUnit.getLocationConfig().hasLocationType()) {
            throw new NotAvailableException("locationConfig.type");
        }

        if (!locationUnit.hasPlacementConfig()) {
            throw new NotAvailableException("placementConfig");
        }

        if (!locationUnit.getPlacementConfig().hasLocationId() || locationUnit.getPlacementConfig().getLocationId().isEmpty()) {
            throw new NotAvailableException("placementConfig.locationId");
        }

        UnitConfig parent, child;
        switch (locationUnit.getLocationConfig().getLocationType()) {
            case REGION:
                // on top of a region can be a region or a tile
                parent = registry.getMessage(locationUnit.getPlacementConfig().getLocationId());
                if (parent.getLocationConfig().getLocationType() == LocationType.ZONE) {
                    throw new CouldNotPerformException("Parent[" + parent.getLabel() + "] of region[" + locationUnit.getLabel() + "]"
                            + " is a zone which is against the location hierarchy!");
                }
                // below a region can only be other regions
                for (String childId : locationUnit.getLocationConfig().getChildIdList()) {
                    child = registry.getMessage(childId);
                    if (child.getLocationConfig().getLocationType() != LocationType.REGION) {
                        throw new CouldNotPerformException("Child[" + child.getLabel() + "] of region[" + locationUnit.getLabel() + "]"
                                + " is not a region but[" + child.getLocationConfig().getLocationType() + "] which is against the location hierarchy!");
                    }
                }
                break;
            case TILE:
                // on top of a tile can only be a zone
                parent = registry.getMessage(locationUnit.getPlacementConfig().getLocationId());
                if (parent.getLocationConfig().getLocationType() != LocationType.ZONE) {
                    throw new CouldNotPerformException("Parent[" + parent.getLabel() + "] of tile[" + locationUnit.getLabel() + "]"
                            + " is not a zone but[" + parent.getLocationConfig().getLocationType() + "] which is against the location hierarchy!");
                }
                // below a tile can only be regions
                for (String childId : locationUnit.getLocationConfig().getChildIdList()) {
                    child = registry.getMessage(childId);
                    if (child.getLocationConfig().getLocationType() != LocationType.REGION) {
                        throw new CouldNotPerformException("Child[" + child.getLabel() + "] of tile[" + locationUnit.getLabel() + "]"
                                + " is not a region but[" + parent.getLocationConfig().getLocationType() + "] which is against the location hierarchy!");
                    }
                }
                break;
            case ZONE:
                // on top of a zone can only be a zone
                parent = registry.getMessage(locationUnit.getPlacementConfig().getLocationId());
                if (parent.getLocationConfig().getLocationType() != LocationType.ZONE) {
                    throw new CouldNotPerformException("Parent[" + parent.getLabel() + "] of zone[" + locationUnit.getLabel() + "]"
                            + " is not a zone but[" + parent.getLocationConfig().getLocationType() + "] which is against the location hierarchy!");
                }
                // below a zone can only be zones or tiles
                for (String childId : locationUnit.getLocationConfig().getChildIdList()) {
                    child = registry.getMessage(childId);
                    if (child.getLocationConfig().getLocationType() == LocationType.REGION) {
                        throw new CouldNotPerformException("Child[" + child.getLabel() + "] of zone[" + locationUnit.getLabel() + "]"
                                + " is a region which is against the location hierarchy!");
                    }
                }
                break;
        }
    }
}
