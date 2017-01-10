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
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.extension.protobuf.IdentifiableMessage;
import org.openbase.jul.extension.protobuf.container.ProtoBufMessageMap;
import org.openbase.jul.storage.registry.AbstractProtoBufRegistryConsistencyHandler;
import org.openbase.jul.storage.registry.EntryModification;
import org.openbase.jul.storage.registry.ProtoBufRegistry;
import rst.domotic.unit.UnitConfigType.UnitConfig;
import rst.domotic.unit.location.LocationConfigType.LocationConfig;

/**
 *
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public class LocationChildConsistencyHandler extends AbstractProtoBufRegistryConsistencyHandler<String, UnitConfig, UnitConfig.Builder> {

    @Override
    public void processData(String id, IdentifiableMessage<String, UnitConfig, UnitConfig.Builder> entry, ProtoBufMessageMap<String, UnitConfig, UnitConfig.Builder> entryMap, ProtoBufRegistry<String, UnitConfig, UnitConfig.Builder> registry) throws CouldNotPerformException, EntryModification {
        UnitConfig.Builder locationUnitConfig = entry.getMessage().toBuilder();
        LocationConfig.Builder locationConfig = locationUnitConfig.getLocationConfigBuilder();

        // check if placement exists.
        if (!locationUnitConfig.hasPlacementConfig()) {
            throw new NotAvailableException("locationconfig.placementconfig");
        }

        UnitConfig childLocationConfig;

        // check parent consistency
        for (String childLocationId : new ArrayList<>(locationConfig.getChildIdList())) {

            // check if given child is registered otherwise remove child.
            if (!registry.contains(childLocationId)) {
                List<String> childIds = new ArrayList<>(locationConfig.getChildIdList());
                childIds.remove(childLocationId);
                locationConfig.clearChildId();
                locationConfig.addAllChildId(childIds);
                throw new EntryModification("Child location removed because registered ChildLocation[" + childLocationId + "] for ParentLocation[" + locationUnitConfig.getId() + "] does not exists.", entry.setMessage(locationUnitConfig), this);
            }

            // check if given child is not parent location otherwise remove child.
            if (locationUnitConfig.getPlacementConfig().hasLocationId() && locationUnitConfig.getPlacementConfig().getLocationId().equals(childLocationId)) {
                List<String> childIds = new ArrayList<>(locationConfig.getChildIdList());
                childIds.remove(childLocationId);
                locationConfig.clearChildId().addAllChildId(childIds);
                throw new EntryModification(entry.setMessage(locationUnitConfig), this);
            }

            childLocationConfig = registry.getMessage(childLocationId);
            // check if parent id is valid.
            if (!childLocationConfig.getPlacementConfig().getLocationId().equals(locationUnitConfig.getId())) {
                // remove child.
                List<String> childIds = new ArrayList<>(locationConfig.getChildIdList());
                childIds.remove(childLocationId);
                locationConfig.clearChildId().addAllChildId(childIds);
                throw new EntryModification(entry.setMessage(locationUnitConfig), this);

            }
        }
    }
}
