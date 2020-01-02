package org.openbase.bco.registry.lib.util;

/*
 * #%L
 * BCO Registry Lib
 * %%
 * Copyright (C) 2014 - 2020 openbase.org
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
import java.util.*;

import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InvalidStateException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.extension.protobuf.IdentifiableMessage;
import org.openbase.jul.extension.protobuf.container.ProtoBufMessageMap;
import org.openbase.jul.storage.registry.ConsistencyHandler;
import org.openbase.jul.storage.registry.EntryModification;
import org.openbase.jul.storage.registry.ProtoBufRegistry;
import org.openbase.type.domotic.unit.UnitConfigType.UnitConfig;
import org.openbase.type.domotic.unit.location.LocationConfigType.LocationConfig.LocationType;
import org.openbase.type.spatial.PlacementConfigType;

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
                throw new EntryModification(detectedRootLocationConfigEntry.setMessage(detectedRootLocationConfigBuilder, consistencyHandler), consistencyHandler);
            }
        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not validate root location!", ex);
        }
    }

    public static UnitConfig getRootLocation(final ProtoBufMessageMap<String, UnitConfig, UnitConfig.Builder> entryMap) throws CouldNotPerformException {
        return getRootLocation(entryMap.getMessages());
    }

    public static UnitConfig getRootLocation(final List<UnitConfig> locationUnitConfigList) throws CouldNotPerformException {
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
                for (UnitConfig unitConfig : rootLocationConfigList.values()) {
                    return Optional.of(unitConfig).get();
                }
                return Optional.<UnitConfig>empty().get();
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
                for (UnitConfig unitConfig : rootLocationConfigList.values()) {
                    return Optional.of(unitConfig).get();
                }
                return Optional.<UnitConfig>empty().get();
            }

            throw new InvalidStateException("To many potential root locations detected!");

        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not compute root location!", ex);
        }
    }

    /**
     * Detect the type of a location.
     * Since a location, except the root location, always has a parent the only ambiguous case
     * is when the parent is a zone but no children are defined. Than the location can either be
     * a zone or a tile. In this case an exception is thrown.
     *
     * @param locationUnit the location of which the type is detected
     * @param locationRegistry the location registry
     * @return the type locationUnit should have
     * @throws CouldNotPerformException if the type is ambiguous
     */
    public static LocationType detectLocationType(UnitConfig locationUnit, ProtoBufRegistry<String, UnitConfig, UnitConfig.Builder> locationRegistry) throws CouldNotPerformException {
        try {
            if (!locationUnit.hasPlacementConfig()) {
                throw new NotAvailableException("placementConfig");
            }
            if (!locationUnit.getPlacementConfig().hasLocationId() || locationUnit.getPlacementConfig().getLocationId().isEmpty()) {
                throw new NotAvailableException("placementConfig.locationId");
            }

            if (locationUnit.getLocationConfig().getRoot()) {
                return LocationType.ZONE;
            }

            LocationType parentLocationType = locationRegistry.get(locationUnit.getPlacementConfig().getLocationId()).getMessage().getLocationConfig().getLocationType();
            Set<LocationType> childLocationTypes = new HashSet<>();
            for (String childId : locationUnit.getLocationConfig().getChildIdList()) {
                childLocationTypes.add(locationRegistry.get(childId).getMessage().getLocationConfig().getLocationType());
            }

            return detectLocationType(parentLocationType, childLocationTypes);
        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not detect Type for location [" + locationUnit.getLabel() + "]", ex);
        }
    }

    /**
     * Detect the type of a location.
     * Since a location, except the root location, always has a parent the only ambiguous case
     * is when the parent is a zone but no children are defined. Than the location can either be
     * a zone or a tile. In this case an exception is thrown.
     *
     * @param parentLocationType the type of the parent location of the location whose type is detected
     * @param childLocationTypes a set of all immediate child types of the location whose type is detected
     * @return the type locationUnit should have
     * @throws CouldNotPerformException if the type is ambiguous
     */
    private static LocationType detectLocationType(LocationType parentLocationType, Set<LocationType> childLocationTypes) throws CouldNotPerformException {
        // if the parent is a region or tile than the location has to be a region
        if (parentLocationType == LocationType.REGION || parentLocationType == LocationType.TILE) {
            return LocationType.REGION;
        }

        // if one child is a zone or a tile the location has to be a zone
        if (childLocationTypes.contains(LocationType.ZONE) || childLocationTypes.contains(LocationType.TILE)) {
            return LocationType.ZONE;
        }

        // if the parent is a zone and a child is a region than the location has to be a tile
        if (parentLocationType == LocationType.ZONE && childLocationTypes.contains(LocationType.REGION)) {
            return LocationType.TILE;
        }

        // if the parent type is a zone but no childs are defined the location could be a tile or a zone which leaves the type undefined
        String childTypes = "";
        String acc = childTypes;
        for (LocationType locationType : childLocationTypes) {
            String s = locationType.toString() + " ";
            acc = acc.concat(s);
        }
        childTypes = "[ " + acc + "]";
        throw new CouldNotPerformException("Could not detect locationType from parentType[" + parentLocationType.name() + "] and childTypes" + childTypes);
    }
}
