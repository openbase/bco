/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dc.bco.registry.location.core.consistency;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import org.dc.jul.exception.CouldNotPerformException;
import org.dc.jul.exception.InvalidStateException;
import org.dc.jul.exception.NotAvailableException;
import org.dc.jul.extension.protobuf.IdentifiableMessage;
import org.dc.jul.extension.protobuf.container.ProtoBufMessageMapInterface;
import org.dc.jul.storage.registry.AbstractProtoBufRegistryConsistencyHandler;
import org.dc.jul.storage.registry.EntryModification;
import org.dc.jul.storage.registry.ProtoBufRegistryInterface;
import rst.spatial.LocationConfigType;
import rst.spatial.LocationConfigType.LocationConfig;

/**
 *
 * @author mpohling
 */
public class LocationIdConsistencyHandler extends AbstractProtoBufRegistryConsistencyHandler<String, LocationConfigType.LocationConfig, LocationConfigType.LocationConfig.Builder> {

    @Override
    public void processData(String id, IdentifiableMessage<String, LocationConfig, LocationConfig.Builder> entry, ProtoBufMessageMapInterface<String, LocationConfig, LocationConfig.Builder> entryMap, ProtoBufRegistryInterface<String, LocationConfig, LocationConfig.Builder> registry) throws CouldNotPerformException, EntryModification {

        LocationConfig.Builder locationConfig = entry.getMessage().toBuilder();

        if (!locationConfig.hasPlacementConfig()) {
            throw new NotAvailableException("locationconfig.placementconfig");
        }

        // check if location id is setuped.
        if (!locationConfig.getPlacementConfig().hasLocationId()) {

            // detect root location
            LocationConfig.Builder setPlacementConfig = locationConfig.setPlacementConfig(locationConfig.getPlacementConfig().toBuilder().setLocationId(detectRootLocation(locationConfig.build(), entryMap).getId()));
            entry.setMessage(setPlacementConfig);
            throw new EntryModification(entry, this);
        }
    }

    public LocationConfig detectRootLocation(final LocationConfig currentLocationConfig, ProtoBufMessageMapInterface<String, LocationConfig, LocationConfig.Builder> entryMap) throws CouldNotPerformException {
        try {
            logger.info("detectNewRootLocation in registry [" + Arrays.toString(entryMap.getMessages().toArray()) + "]");
            for (LocationConfig locationConfig : entryMap.getMessages()) {
                if (locationConfig.hasRoot() && locationConfig.getRoot()) {
                    return locationConfig;
                }
            }
            return computeNewRootLocation(currentLocationConfig, entryMap);
        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not detect root location!");
        }
    }

    public LocationConfig computeNewRootLocation(final LocationConfig currentLocationConfig, ProtoBufMessageMapInterface<String, LocationConfig, LocationConfig.Builder> entryMap) throws CouldNotPerformException {
        try {
            HashMap<String, LocationConfig> rootLocationConfigList = new HashMap<>();
            logger.info("computeNewRootLocation ");
            for (LocationConfig locationConfig : entryMap.getMessages()) {
                rootLocationConfigList.put(locationConfig.getId(), locationConfig);
            }

            rootLocationConfigList.put(currentLocationConfig.getId(), currentLocationConfig);

            if (rootLocationConfigList.size() == 1) {
                return rootLocationConfigList.values().stream().findFirst().get();
            }

            for (LocationConfig locationConfig : new ArrayList<>(rootLocationConfigList.values())) {
                if (!locationConfig.hasPlacementConfig()) {
                } else if (!locationConfig.getPlacementConfig().hasLocationId()) {
                } else if (locationConfig.getPlacementConfig().getLocationId().isEmpty()) {
                } else if (locationConfig.getPlacementConfig().getLocationId().equals(locationConfig.getId())) {
                    return locationConfig;
                } else {
                    rootLocationConfigList.remove(locationConfig.getId());
                }
            }

            if (rootLocationConfigList.isEmpty()) {
                throw new InvalidStateException("Could not compute root location!");
            } else if (rootLocationConfigList.size() == 1) {
                return rootLocationConfigList.values().stream().findFirst().get();
            }

            throw new InvalidStateException("To many potential root locations detected!");

        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not compute root location!", ex);
        }
    }
}
