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
import java.util.List;
import org.dc.jul.exception.CouldNotPerformException;
import org.dc.jul.exception.NotAvailableException;
import org.dc.jul.extension.protobuf.IdentifiableMessage;
import org.dc.jul.extension.protobuf.container.ProtoBufMessageMapInterface;
import org.dc.jul.storage.registry.AbstractProtoBufRegistryConsistencyHandler;
import org.dc.jul.storage.registry.EntryModification;
import org.dc.jul.storage.registry.ProtoBufRegistryInterface;
import rst.spatial.LocationConfigType.LocationConfig;

/**
 *
 * @author mpohling
 */
public class LocationChildConsistencyHandler extends AbstractProtoBufRegistryConsistencyHandler<String, LocationConfig, LocationConfig.Builder> {

    @Override
    public void processData(String id, IdentifiableMessage<String, LocationConfig, LocationConfig.Builder> entry, ProtoBufMessageMapInterface<String, LocationConfig, LocationConfig.Builder> entryMap, ProtoBufRegistryInterface<String, LocationConfig, LocationConfig.Builder> registry) throws CouldNotPerformException, EntryModification {

        LocationConfig.Builder locationConfig = entry.getMessage().toBuilder();

        // check if placement exists.
        if (!locationConfig.hasPlacementConfig()) {
            throw new NotAvailableException("locationconfig.placementconfig");
        }

        LocationConfig childLocationConfig;

        // check parent consistency
        for (String childLocationId : new ArrayList<>(locationConfig.getChildIdList())) {

            // check if given child is registered otherwise remove child.
            if (!registry.contains(childLocationId)) {
                logger.warn("Registered ChildLocation[" + childLocationId + "] for ParentLocation[" + locationConfig.getId() + "] does not exists.");
                List<String> childIds = new ArrayList<>(locationConfig.getChildIdList());
                childIds.remove(childLocationId);
                throw new EntryModification(entry.setMessage(locationConfig.clearChildId().addAllChildId(childIds).build()), this);
            }

            // check if given child is not parent location otherwise remove child.
            if (locationConfig.getPlacementConfig().hasLocationId() && locationConfig.getPlacementConfig().getLocationId().equals(childLocationId)) {
                List<String> childIds = new ArrayList<>(locationConfig.getChildIdList());
                childIds.remove(childLocationId);
                throw new EntryModification(entry.setMessage(locationConfig.clearChildId().addAllChildId(childIds).build()), this);
            }


//            // check if parent id is registered
//            if (!childLocationConfig.hasPlacementConfig() || locationConfig.getPlacementConfig().hasLocationId()) {
//                throw new EntryModification(child.setMessage(child.getMessage().toBuilder().setPlacementConfig(locationConfig.getPlacementConfig().toBuilder().setLocationId(locationConfig.getId()))), this);
//            }

            childLocationConfig = registry.getMessage(childLocationId);

            // check if parent id is valid.
            if (!childLocationConfig.getPlacementConfig().getLocationId().equals(locationConfig.getId())) {
//                IdentifiableMessage<String, LocationConfig, LocationConfig.Builder> child = entryMap.get(childLocationConfig.getId());
//                throw new EntryModification(child.setMessage(child.getMessage().toBuilder().setPlacementConfig(childLocationConfig.getPlacementConfig().toBuilder().setLocationId(locationConfig.getId()).build())), this);

                // remove child.
                List<String> childIds = new ArrayList<>(locationConfig.getChildIdList());
                childIds.remove(childLocationId);
                throw new EntryModification(entry.setMessage(locationConfig.clearChildId().addAllChildId(childIds).build()), this);

            }
        }
    }
}
