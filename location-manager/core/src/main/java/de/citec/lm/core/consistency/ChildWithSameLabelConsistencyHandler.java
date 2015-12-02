/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.lm.core.consistency;

import de.citec.jul.exception.CouldNotPerformException;
import de.citec.jul.exception.InvalidStateException;
import de.citec.jul.extension.protobuf.IdentifiableMessage;
import de.citec.jul.extension.protobuf.container.ProtoBufMessageMapInterface;
import de.citec.jul.extension.rsb.scope.ScopeGenerator;
import de.citec.jul.storage.registry.EntryModification;
import de.citec.jul.storage.registry.ProtoBufRegistryConsistencyHandler;
import de.citec.jul.storage.registry.ProtoBufRegistryInterface;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rst.spatial.LocationConfigType.LocationConfig;

/**
 *
 * @author mpohling
 */
public class ChildWithSameLabelConsistencyHandler implements ProtoBufRegistryConsistencyHandler<String, LocationConfig, LocationConfig.Builder> {

    private static final Logger logger = LoggerFactory.getLogger(ChildWithSameLabelConsistencyHandler.class);

    private final Map<String, String> labelConsistencyMap;

    public ChildWithSameLabelConsistencyHandler() {
        labelConsistencyMap = new HashMap<>();
    }

    @Override
    public void processData(String id, IdentifiableMessage<String, LocationConfig, LocationConfig.Builder> entry, ProtoBufMessageMapInterface<String, LocationConfig, LocationConfig.Builder> entryMap, ProtoBufRegistryInterface<String, LocationConfig, LocationConfig.Builder> registry) throws CouldNotPerformException, EntryModification {
        LocationConfig locationConfig = entry.getMessage();

        for (String childLocationId : new ArrayList<>(locationConfig.getChildIdList())) {
            LocationConfig childLocationConfig = registry.getMessage(childLocationId);

            if (labelConsistencyMap.containsKey(childLocationConfig.getLabel()) && !labelConsistencyMap.get(childLocationConfig.getLabel()).equals(childLocationId)) {
                throw new InvalidStateException("Location [" + locationConfig.getId() + "," + locationConfig.getLabel() + "] has more than on child with the same label [" + childLocationConfig.getLabel() + "]");
            } else {
                labelConsistencyMap.put(childLocationConfig.getLabel(), childLocationId);
            }
        }
    }

    @Override
    public void reset() {
        labelConsistencyMap.clear();
    }
}
