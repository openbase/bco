package org.openbase.bco.registry.location.lib.util;

/*
 * #%L
 * REM LocationRegistry Library
 * %%
 * Copyright (C) 2014 - 2016 openbase.org
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-3.0.html>.
 * #L%
 */
import java.util.ArrayList;
import java.util.HashMap;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InvalidStateException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.extension.protobuf.IdentifiableMessage;
import org.openbase.jul.extension.protobuf.container.ProtoBufMessageMapInterface;
import org.openbase.jul.storage.registry.ConsistencyHandler;
import org.openbase.jul.storage.registry.EntryModification;
import rst.spatial.LocationConfigType.LocationConfig;
import rst.spatial.PlacementConfigType;

/**
 *
 * @author * @author <a href="mailto:DivineThreepwood@gmail.com">Divine Threepwood</a>
 */
public class LocationUtils {

    public static void validateRootLocation(final LocationConfig newRootLocation, final ProtoBufMessageMapInterface<String, LocationConfig, LocationConfig.Builder> entryMap, final ConsistencyHandler consistencyHandler) throws CouldNotPerformException, EntryModification {
        try {
            boolean modifiered = false;
            // detect root location
            IdentifiableMessage<String, LocationConfig, LocationConfig.Builder> detectedRootLocationConfigEntry = entryMap.get(newRootLocation.getId());
            LocationConfig.Builder detectedRootLocationConfigBuilder = detectedRootLocationConfigEntry.getMessage().toBuilder();

            // verify if root flag is set.
            if (!detectedRootLocationConfigBuilder.hasRoot() || !detectedRootLocationConfigBuilder.getRoot()) {
                detectedRootLocationConfigBuilder.setRoot(true);
                modifiered = true;
            }

            // verify if placement field is set.
            if (!detectedRootLocationConfigBuilder.hasPlacementConfig()) {
                detectedRootLocationConfigBuilder.setPlacementConfig(PlacementConfigType.PlacementConfig.newBuilder());
                modifiered = true;
            }

            // verify if placement location id is set.
            if (!detectedRootLocationConfigBuilder.getPlacementConfig().hasLocationId() || !detectedRootLocationConfigBuilder.getPlacementConfig().getLocationId().equals(detectedRootLocationConfigBuilder.getId())) {
                detectedRootLocationConfigBuilder.getPlacementConfigBuilder().setLocationId(detectedRootLocationConfigBuilder.getId());
                modifiered = true;
            }

            if (modifiered) {
                throw new EntryModification(detectedRootLocationConfigEntry.setMessage(detectedRootLocationConfigBuilder), consistencyHandler);
            }
        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not validate root location!", ex);
        }
    }

    public static LocationConfig getRootLocation(final ProtoBufMessageMapInterface<String, LocationConfig, LocationConfig.Builder> entryMap) throws NotAvailableException, CouldNotPerformException {
        LocationConfig rootLocation = null;
        try {
            for (LocationConfig locationConfig : entryMap.getMessages()) {
                if (locationConfig.hasRoot() && locationConfig.getRoot()) {
                    if (rootLocation != null) {
                        throw new InvalidStateException("Found more than one [" + rootLocation.getLabel() + "] & [" + locationConfig.getLabel() + "] root locations!");
                    }
                    rootLocation = locationConfig;
                }
            }
        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not lookup root location!", ex);
        }

        if (rootLocation == null) {
            throw new NotAvailableException("root location");
        }
        return rootLocation;
    }

    public static LocationConfig detectRootLocation(final LocationConfig currentLocationConfig, final ProtoBufMessageMapInterface<String, LocationConfig, LocationConfig.Builder> entryMap, final ConsistencyHandler consistencyHandler) throws CouldNotPerformException, EntryModification {
        try {
            try {
                return getRootLocation(entryMap);
            } catch (NotAvailableException ex) {
                LocationConfig newLocationConfig = computeNewRootLocation(currentLocationConfig, entryMap);
                validateRootLocation(newLocationConfig, entryMap, consistencyHandler);
                return newLocationConfig;
            }
        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not detect root location!", ex);
        }
    }

    public static LocationConfig computeNewRootLocation(final LocationConfig currentLocationConfig, ProtoBufMessageMapInterface<String, LocationConfig, LocationConfig.Builder> entryMap) throws CouldNotPerformException {
        try {
            HashMap<String, LocationConfig> rootLocationConfigList = new HashMap<>();
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
                throw new NotAvailableException("root candidate");
            } else if (rootLocationConfigList.size() == 1) {
                return rootLocationConfigList.values().stream().findFirst().get();
            }

            throw new InvalidStateException("To many potential root locations detected!");

        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not compute root location!", ex);
        }
    }
}
