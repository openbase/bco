/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.lm.core.consistency;

import de.citec.jul.exception.CouldNotPerformException;
import de.citec.jul.extension.protobuf.IdentifiableMessage;
import de.citec.jul.extension.protobuf.container.ProtoBufMessageMapInterface;
import de.citec.jul.storage.registry.EntryModification;
import de.citec.jul.storage.registry.ProtoBufRegistryConsistencyHandler;
import de.citec.jul.storage.registry.ProtoBufRegistryInterface;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rst.spatial.LocationConfigType;
import rst.spatial.LocationConfigType.LocationConfig;

/**
 *
 * @author mpohling
 */
public class ParentChildConsistencyHandler implements ProtoBufRegistryConsistencyHandler<String, LocationConfig, LocationConfig.Builder> {

    private static final Logger logger = LoggerFactory.getLogger(ParentChildConsistencyHandler.class);

    @Override
    public void processData(String id, IdentifiableMessage<String, LocationConfig, LocationConfig.Builder> entry, ProtoBufMessageMapInterface<String, LocationConfig, LocationConfig.Builder> entryMap, ProtoBufRegistryInterface<String, LocationConfig, LocationConfig.Builder> registry) throws CouldNotPerformException, EntryModification {

        LocationConfig locationConfig = entry.getMessage();

        // check if parents know their children.
        if (locationConfig.hasParentId()) {

            // check if parent is registered.
            if (!entryMap.containsKey(locationConfig.getParentId())) {
                logger.warn("Parent[" + locationConfig.getParentId() + "] of child[" + locationConfig.getId() + "] is unknown! Parent entry will be erased!");
                entry.setMessage(locationConfig.toBuilder().clearParentId().setRoot(true).build());
                throw new EntryModification(entry, this);
            }

            // check if parents knows given child.
            IdentifiableMessage<String, LocationConfigType.LocationConfig, LocationConfig.Builder> parent = registry.get(locationConfig.getParentId());
            if (parent != null && !parentHasChild(parent.getMessage(), locationConfig)) {
                parent.setMessage(parent.getMessage().toBuilder().addChildId(locationConfig.getId()).build());
                throw new EntryModification(entry, this);
            }
        }
        // check if children know their parent.
        for (String childLocationId : new ArrayList<>(locationConfig.getChildIdList())) {
            LocationConfig childLocationConfig;

            // check if given child is registered otherwise register.
            if (!registry.contains(childLocationId)) {
                logger.warn("registered child[" + childLocationId + "] for parent[" + locationConfig + "] does not exists.");
                List<String> childIds = new ArrayList<>(locationConfig.getChildIdList());
                childIds.remove(childLocationId);
                throw new EntryModification(entry.setMessage(locationConfig.toBuilder().clearChildId().addAllChildId(childIds).build()), this);
            } else {
                childLocationConfig = registry.getMessage(childLocationId);
            }

            IdentifiableMessage<String, LocationConfig, LocationConfig.Builder> child = entryMap.get(childLocationConfig, registry.getIdGenerator());
            // check if parent id is registered
            if (!childLocationConfig.hasParentId()) {
                throw new EntryModification(child.setMessage(child.getMessage().toBuilder().setParentId(locationConfig.getId())), this);
            }

            // check if parent id is valid.
            if (!childLocationConfig.getParentId().equals(locationConfig.getId())) {
                throw new EntryModification(child.setMessage(child.getMessage().toBuilder().setParentId(locationConfig.getId())), this);
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
