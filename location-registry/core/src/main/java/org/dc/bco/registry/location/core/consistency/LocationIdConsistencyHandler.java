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
import org.dc.jul.exception.InvalidStateException;
import org.dc.jul.exception.NotAvailableException;
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
public class LocationIdConsistencyHandler extends AbstractProtoBufRegistryConsistencyHandler<String, LocationConfigType.LocationConfig, LocationConfigType.LocationConfig.Builder> {

    private final LocationRegistry locationConfigRegistry;

    public LocationIdConsistencyHandler(final LocationRegistry locationConfigRegistry) {
        this.locationConfigRegistry = locationConfigRegistry;
    }

    @Override
    public void processData(String id, IdentifiableMessage<String, LocationConfig, LocationConfig.Builder> entry, ProtoBufMessageMapInterface<String, LocationConfig, LocationConfig.Builder> entryMap, ProtoBufRegistryInterface<String, LocationConfig, LocationConfig.Builder> registry) throws CouldNotPerformException, EntryModification {

//        try {
        logger.info("processing: " + entry.getMessage());

        LocationConfig.Builder locationConfig = entry.getMessage().toBuilder();

        // check if placement exists.
        if (!locationConfig.hasPlacementConfig()) {
            locationConfig.setPlacementConfig(PlacementConfigType.PlacementConfig.newBuilder());
        }

        // check if location id is setuped.
        if (!locationConfig.getPlacementConfig().hasLocationId()) {

            // detect root location
            LocationConfig rootLocationConfig;
            rootLocationConfig = computeNewRootLocation(locationConfig.build());
            logger.info("Missing location id detected for [" + locationConfig.getId() + "]!");
            LocationConfig.Builder setPlacementConfig = locationConfig.setPlacementConfig(locationConfig.getPlacementConfig().toBuilder().setLocationId(rootLocationConfig.getId()));
            logger.info("Updated to: [" + setPlacementConfig.getPlacementConfig().getLocationId() + "]");
            entry.setMessage(setPlacementConfig);
            logger.info("setup: " + entry.getMessage());
            throw new EntryModification(entry, this);
        }
//        } catch (CouldNotPerformException ex) {
//            throw ExceptionPrinter.printHistoryAndReturnThrowable(ex, logger);
//        }
    }

    public LocationConfig computeNewRootLocation(final LocationConfig currentLocationConfig) throws CouldNotPerformException {
        try {
            try {
                return locationConfigRegistry.getRootLocationConfig();
            } catch (NotAvailableException ex) {
                // compute ...
            }

            List<LocationConfig> locationConfigs = locationConfigRegistry.getLocationConfigs();

            if (!locationConfigs.contains(currentLocationConfig)) {
                locationConfigs.add(currentLocationConfig);
            }

            if (locationConfigs.size() == 1) {
                return locationConfigs.get(0);
            }

            for (LocationConfig locationConfig : new ArrayList<>(locationConfigs)) {
                if (!locationConfig.hasPlacementConfig()) {
                } else if (!locationConfig.getPlacementConfig().hasLocationId()) {
                } else if (locationConfig.getPlacementConfig().getLocationId().isEmpty()) {
                } else if (locationConfig.getPlacementConfig().getLocationId().equals(locationConfig.getId())) {
                    return locationConfig;
                } else {
                    locationConfigs.remove(locationConfig);
                }
            }

            if (locationConfigs.isEmpty()) {
                throw new InvalidStateException("Could not compute root location!");
            } else if (locationConfigs.size() == 1) {
                return locationConfigs.get(0);
            }

            throw new InvalidStateException("To many potential root locations detected!");

        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not compute root location!", ex);
        }
    }
}
