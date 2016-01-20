/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dc.bco.registry.location.core.consistency;

import java.util.ArrayList;
import java.util.List;
import org.dc.bco.registry.location.lib.LocationRegistry;
import org.dc.jul.exception.CouldNotPerformException;
import org.dc.jul.extension.protobuf.IdentifiableMessage;
import org.dc.jul.extension.protobuf.container.ProtoBufMessageMapInterface;
import org.dc.jul.storage.registry.AbstractProtoBufRegistryConsistencyHandler;
import org.dc.jul.storage.registry.EntryModification;
import org.dc.jul.storage.registry.ProtoBufRegistryInterface;
import rst.spatial.LocationConfigType;
import rst.spatial.LocationConfigType.LocationConfig;
import rst.spatial.PlacementConfigType;

/**
 *
 * @author mpohling
 */
public class ParentChildConsistencyHandler extends AbstractProtoBufRegistryConsistencyHandler<String, LocationConfig, LocationConfig.Builder> {

    private final LocationRegistry locationConfigRegistry;

    public ParentChildConsistencyHandler(final LocationRegistry locationConfigRegistry) {
        this.locationConfigRegistry = locationConfigRegistry;
    }

    @Override
    public void processData(String id, IdentifiableMessage<String, LocationConfig, LocationConfig.Builder> entry, ProtoBufMessageMapInterface<String, LocationConfig, LocationConfig.Builder> entryMap, ProtoBufRegistryInterface<String, LocationConfig, LocationConfig.Builder> registry) throws CouldNotPerformException, EntryModification {

        LocationConfig.Builder locationConfig = entry.getMessage().toBuilder();

        // check if placement exists.
        if (!locationConfig.hasPlacementConfig()) {
            locationConfig.setPlacementConfig(PlacementConfigType.PlacementConfig.newBuilder());
        }

        // check children consistency
        if (locationConfig.getPlacementConfig().hasLocationId()) {

            // check if parent is registered.

            // check if parent is registered.
            if (!entryMap.containsKey(locationConfig.getPlacementConfig().getLocationId())) {
                logger.warn("Parent[" + locationConfig.getPlacementConfig().getLocationId() + "] of child[" + locationConfig.getId() + "] is unknown! Entry will moved to root location!");
                entry.setMessage(locationConfig.setPlacementConfig(locationConfig.getPlacementConfig().toBuilder().setLocationId(locationConfigRegistry.getRootLocationConfig().getId()))); //clear !!!
                throw new EntryModification(entry, this);
            }

            // check if parents knows given child.
            IdentifiableMessage<String, LocationConfigType.LocationConfig, LocationConfig.Builder> parent = registry.get(locationConfig.getPlacementConfig().getLocationId());
            if (parent != null && !parentHasChild(parent.getMessage(), locationConfig.build())) {
                parent.setMessage(parent.getMessage().toBuilder().addChildId(locationConfig.getId()));
                throw new EntryModification(entry, this);
            }
        }

        // check parent consistency
        for (String childLocationId : new ArrayList<>(locationConfig.getChildIdList())) {

            // check if given child is registered otherwise remove child.
            if (!registry.contains(childLocationId)) {
                logger.warn("Registered ChildLocation[" + childLocationId + "] for ParentLocation[" + locationConfig.getId() + "] does not exists.");
                List<String> childIds = new ArrayList<>(locationConfig.getChildIdList());
                childIds.remove(childLocationId);
                throw new EntryModification(entry.setMessage(locationConfig.clearChildId().addAllChildId(childIds).build()), this);
            }

            final LocationConfig childLocationConfig = registry.getMessage(childLocationId);

            IdentifiableMessage<String, LocationConfig, LocationConfig.Builder> child = entryMap.get(childLocationConfig.getId());

            // check if parent id is registered
            if (!childLocationConfig.hasPlacementConfig() || locationConfig.getPlacementConfig().hasLocationId()) {
                throw new EntryModification(child.setMessage(child.getMessage().toBuilder().setPlacementConfig(locationConfig.getPlacementConfig().toBuilder().setLocationId(locationConfig.getId()))), this);
            }

            // check if parent id is valid.
            if (!childLocationConfig.getPlacementConfig().getLocationId().equals(locationConfig.getId())) {
                throw new EntryModification(child.setMessage(child.getMessage().toBuilder().setPlacementConfig(locationConfig.getPlacementConfig().toBuilder().setLocationId(locationConfig.getId()).build())), this);
            }
        }
    }

    private boolean parentHasChild(LocationConfig parent, LocationConfig child) {
        for (String children : parent.getChildIdList()) {
            if (children.equals(child.getId())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void reset() {
    }
}
