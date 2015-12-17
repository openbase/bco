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
import de.citec.jul.storage.registry.AbstractProtoBufRegistryConsistencyHandler;
import de.citec.jul.storage.registry.EntryModification;
import de.citec.jul.storage.registry.ProtoBufRegistryInterface;
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
        loopTest(entry.getMessage(), registry);
    }

    private void loopTest(final LocationConfig locationConfig, ProtoBufRegistryInterface<String, LocationConfig, LocationConfig.Builder> registry) throws InvalidStateException, CouldNotPerformException {
        loopTest(locationConfig, registry, new ArrayList<String>());
    }
    private void loopTest(final LocationConfig locationConfig, ProtoBufRegistryInterface<String, LocationConfig, LocationConfig.Builder> registry, List<String> processedLocations) throws InvalidStateException, CouldNotPerformException {
        markAsProcessed(locationConfig, processedLocations);

        for (String locationId : locationConfig.getChildIdList()) {
            loopTest(registry.get(locationId).getMessage(), registry, processedLocations);
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
