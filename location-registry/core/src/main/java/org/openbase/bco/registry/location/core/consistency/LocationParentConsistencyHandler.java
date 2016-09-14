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
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.extension.protobuf.IdentifiableMessage;
import org.openbase.jul.storage.registry.AbstractProtoBufRegistryConsistencyHandler;
import org.openbase.jul.storage.registry.EntryModification;
import rst.spatial.LocationConfigType;
import rst.spatial.LocationConfigType.LocationConfig;
import org.openbase.jul.extension.protobuf.container.ProtoBufMessageMap;
import org.openbase.jul.storage.registry.ProtoBufRegistry;

/**
 *
 * @author mpohling
 */
public class LocationParentConsistencyHandler extends AbstractProtoBufRegistryConsistencyHandler<String, LocationConfig, LocationConfig.Builder> {

    @Override
    public void processData(String id, IdentifiableMessage<String, LocationConfig, LocationConfig.Builder> entry, ProtoBufMessageMap<String, LocationConfig, LocationConfig.Builder> entryMap, ProtoBufRegistry<String, LocationConfig, LocationConfig.Builder> registry) throws CouldNotPerformException, EntryModification {

        LocationConfig.Builder locationConfig = entry.getMessage().toBuilder();

        // check if placement exists.
        if (!locationConfig.hasPlacementConfig()) {
            throw new NotAvailableException("locationconfig.placementconfig");
        }

        // check children consistency
        if (!locationConfig.getPlacementConfig().hasLocationId()) {
            throw new NotAvailableException("locationconfig.placementconfig.locationid");
        }

        // skip root locations
        if (locationConfig.getRoot()) {
            return;
        }

        // check if parent is registered.
        if (!entryMap.containsKey(locationConfig.getPlacementConfig().getLocationId())) {
            entry.setMessage(locationConfig.setPlacementConfig(locationConfig.getPlacementConfig().toBuilder().clearLocationId()));
            throw new EntryModification("Parent[" + locationConfig.getPlacementConfig().getLocationId() + "] of child[" + locationConfig.getId() + "] is unknown! Entry will moved to root location!", entry, this);
        }

        // check if parents knows given child.
        IdentifiableMessage<String, LocationConfigType.LocationConfig, LocationConfig.Builder> parent = registry.get(locationConfig.getPlacementConfig().getLocationId());
        if (parent != null && !parentHasChild(parent.getMessage(), locationConfig.build()) && !parent.getMessage().getPlacementConfig().getLocationId().equals(locationConfig.getId())) {
            parent.setMessage(parent.getMessage().toBuilder().addChildId(locationConfig.getId()));
            throw new EntryModification("Parent[" + parent.getId() + "] does not know Child[" + locationConfig.getId() + "]", parent, this);
        }
    }

    private boolean parentHasChild(LocationConfig parent, LocationConfig child) {
        return parent.getChildIdList().stream().anyMatch((children) -> (children.equals(child.getId())));
    }
}
