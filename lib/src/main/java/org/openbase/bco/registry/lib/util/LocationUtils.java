package org.openbase.bco.registry.lib.util;

/*
 * #%L
 * BCO Registry Lib
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
import java.util.HashMap;
import java.util.List;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InvalidStateException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.extension.protobuf.IdentifiableMessage;
import org.openbase.jul.extension.protobuf.container.ProtoBufMessageMap;
import org.openbase.jul.storage.registry.ConsistencyHandler;
import org.openbase.jul.storage.registry.EntryModification;
import rst.domotic.unit.UnitConfigType.UnitConfig;
import rst.spatial.PlacementConfigType;

/**
 *
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 *
 */
public class LocationUtils {

    public static void validateRootLocation(final UnitConfig newRootLocation, final ProtoBufMessageMap<String, UnitConfig, UnitConfig.Builder> entryMap, final ConsistencyHandler consistencyHandler) throws CouldNotPerformException, EntryModification {
        try {
            boolean modified = false;
            // detect root location
            IdentifiableMessage<String, UnitConfig, UnitConfig.Builder> detectedRootLocationConfigEntry = entryMap.get(newRootLocation.getId());
            UnitConfig.Builder detectedRootLocationConfigBuilder = detectedRootLocationConfigEntry.getMessage().toBuilder();

            // verify if root flag is set.
            if (!detectedRootLocationConfigBuilder.getLocationConfig().hasRoot() || !detectedRootLocationConfigBuilder.getLocationConfig().getRoot()) {
                detectedRootLocationConfigBuilder.getLocationConfigBuilder().setRoot(true);
                modified = true;
            }

            // verify if placement field is set.
            if (!detectedRootLocationConfigBuilder.hasPlacementConfig()) {
                detectedRootLocationConfigBuilder.setPlacementConfig(PlacementConfigType.PlacementConfig.newBuilder());
                modified = true;
            }

            // verify if placement location id is set.
            if (!detectedRootLocationConfigBuilder.getPlacementConfig().hasLocationId() || !detectedRootLocationConfigBuilder.getPlacementConfig().getLocationId().equals(detectedRootLocationConfigBuilder.getId())) {
                detectedRootLocationConfigBuilder.getPlacementConfigBuilder().setLocationId(detectedRootLocationConfigBuilder.getId());
                modified = true;
            }

            if (modified) {
                throw new EntryModification(detectedRootLocationConfigEntry.setMessage(detectedRootLocationConfigBuilder), consistencyHandler);
            }
        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not validate root location!", ex);
        }
    }

    public static UnitConfig getRootLocation(final ProtoBufMessageMap<String, UnitConfig, UnitConfig.Builder> entryMap) throws NotAvailableException, CouldNotPerformException {
        return getRootLocation(entryMap.getMessages());
    }

    public static UnitConfig getRootLocation(final List<UnitConfig> locationUnitConfigList) throws NotAvailableException, CouldNotPerformException {
        UnitConfig rootLocation = null;
        try {
            for (UnitConfig locationConfig : locationUnitConfigList) {
                if (locationConfig.getLocationConfig().hasRoot() && locationConfig.getLocationConfig().getRoot()) {
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

    public static UnitConfig detectRootLocation(final UnitConfig currentLocationConfig, final ProtoBufMessageMap<String, UnitConfig, UnitConfig.Builder> entryMap, final ConsistencyHandler consistencyHandler) throws CouldNotPerformException, EntryModification {
        try {
            try {
                return getRootLocation(entryMap);
            } catch (NotAvailableException ex) {
                UnitConfig newLocationConfig = computeNewRootLocation(currentLocationConfig, entryMap);
                validateRootLocation(newLocationConfig, entryMap, consistencyHandler);
                return newLocationConfig;
            }
        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not detect root location!", ex);
        }
    }

    public static UnitConfig computeNewRootLocation(final UnitConfig currentLocationConfig, ProtoBufMessageMap<String, UnitConfig, UnitConfig.Builder> entryMap) throws CouldNotPerformException {
        try {
            HashMap<String, UnitConfig> rootLocationConfigList = new HashMap<>();
            for (UnitConfig locationConfig : entryMap.getMessages()) {
                rootLocationConfigList.put(locationConfig.getId(), locationConfig);
            }

            rootLocationConfigList.put(currentLocationConfig.getId(), currentLocationConfig);

            if (rootLocationConfigList.size() == 1) {
                return rootLocationConfigList.values().stream().findFirst().get();
            }

            for (UnitConfig locationConfig : new ArrayList<>(rootLocationConfigList.values())) {
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
