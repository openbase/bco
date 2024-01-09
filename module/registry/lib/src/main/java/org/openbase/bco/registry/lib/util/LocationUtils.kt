package org.openbase.bco.registry.lib.util

import org.openbase.jul.exception.CouldNotPerformException
import org.openbase.jul.exception.InvalidStateException
import org.openbase.jul.exception.NotAvailableException
import org.openbase.jul.extension.protobuf.container.ProtoBufMessageMap
import org.openbase.jul.storage.registry.ConsistencyHandler
import org.openbase.jul.storage.registry.EntryModification
import org.openbase.jul.storage.registry.ProtoBufRegistry
import org.openbase.type.domotic.unit.UnitConfigType
import org.openbase.type.domotic.unit.location.LocationConfigType.LocationConfig.LocationType
import org.openbase.type.spatial.PlacementConfigType
import java.util.*

/**
 *
 * @author [Divine Threepwood](mailto:divine@openbase.org)
 */
object LocationUtils {
    @JvmStatic
    @Throws(CouldNotPerformException::class, EntryModification::class)
    fun validateRootLocation(
        newRootLocation: UnitConfigType.UnitConfig,
        entryMap: ProtoBufMessageMap<String, UnitConfigType.UnitConfig, UnitConfigType.UnitConfig.Builder>,
        consistencyHandler: ConsistencyHandler<*, *, *, *>,
    ) {
        try {
            var modified = false
            // detect root location
            val detectedRootLocationConfigEntry = entryMap[newRootLocation.id]
            val detectedRootLocationConfigBuilder = detectedRootLocationConfigEntry.message.toBuilder()

            // verify if root flag is set.
            if (!detectedRootLocationConfigBuilder.locationConfig.hasRoot() || !detectedRootLocationConfigBuilder.locationConfig.root) {
                detectedRootLocationConfigBuilder.locationConfigBuilder.setRoot(true)
                modified = true
            }

            // verify if placement field is set.
            if (!detectedRootLocationConfigBuilder.hasPlacementConfig()) {
                detectedRootLocationConfigBuilder.setPlacementConfig(PlacementConfigType.PlacementConfig.newBuilder())
                modified = true
            }

            // verify if placement location id is set.
            if (!detectedRootLocationConfigBuilder.placementConfig.hasLocationId() || detectedRootLocationConfigBuilder.placementConfig.locationId != detectedRootLocationConfigBuilder.id) {
                detectedRootLocationConfigBuilder.placementConfigBuilder.setLocationId(detectedRootLocationConfigBuilder.id)
                modified = true
            }

            if (modified) {
                throw EntryModification(
                    detectedRootLocationConfigEntry.setMessage(
                        detectedRootLocationConfigBuilder,
                        consistencyHandler
                    ), consistencyHandler
                )
            }
        } catch (ex: CouldNotPerformException) {
            throw CouldNotPerformException("Could not validate root location!", ex)
        }
    }

    @JvmStatic
    @Throws(CouldNotPerformException::class)
    fun getRootLocation(entryMap: ProtoBufMessageMap<String, UnitConfigType.UnitConfig, UnitConfigType.UnitConfig.Builder>): UnitConfigType.UnitConfig {
        return getRootLocation(entryMap.getMessages())
    }

    @JvmStatic
    @Throws(CouldNotPerformException::class)
    fun getRootLocation(locationUnitConfigList: List<UnitConfigType.UnitConfig>): UnitConfigType.UnitConfig {
        var rootLocation: UnitConfigType.UnitConfig? = null
        try {
            for (locationConfig in locationUnitConfigList) {
                if (locationConfig.locationConfig.hasRoot() && locationConfig.locationConfig.root) {
                    if (rootLocation != null) {
                        throw InvalidStateException("Found more than one [" + rootLocation.label + "] & [" + locationConfig.label + "] root locations!")
                    }
                    rootLocation = locationConfig
                }
            }
        } catch (ex: CouldNotPerformException) {
            throw CouldNotPerformException("Could not lookup root location!", ex)
        }

        if (rootLocation == null) {
            throw NotAvailableException("root location")
        }
        return rootLocation
    }

    @JvmStatic
    @Throws(CouldNotPerformException::class, EntryModification::class)
    fun detectRootLocation(
        currentLocationConfig: UnitConfigType.UnitConfig,
        entryMap: ProtoBufMessageMap<String, UnitConfigType.UnitConfig, UnitConfigType.UnitConfig.Builder>,
        consistencyHandler: ConsistencyHandler<*, *, *, *>,
    ): UnitConfigType.UnitConfig {
        try {
            try {
                return getRootLocation(entryMap)
            } catch (ex: NotAvailableException) {
                val newLocationConfig = computeNewRootLocation(currentLocationConfig, entryMap)
                validateRootLocation(newLocationConfig, entryMap, consistencyHandler)
                return newLocationConfig
            }
        } catch (ex: CouldNotPerformException) {
            throw CouldNotPerformException("Could not detect root location!", ex)
        }
    }

    @JvmStatic
    @Throws(CouldNotPerformException::class)
    fun computeNewRootLocation(
        currentLocationConfig: UnitConfigType.UnitConfig,
        entryMap: ProtoBufMessageMap<String, UnitConfigType.UnitConfig, UnitConfigType.UnitConfig.Builder>,
    ): UnitConfigType.UnitConfig {
        try {
            val rootLocationConfigList = HashMap<String, UnitConfigType.UnitConfig>()
            for (locationConfig in entryMap.getMessages()) {
                rootLocationConfigList[locationConfig.id] = locationConfig
            }

            rootLocationConfigList[currentLocationConfig.id] = currentLocationConfig

            if (rootLocationConfigList.size == 1) {
                for (unitConfig in rootLocationConfigList.values) {
                    return Optional.of(unitConfig).get()
                }
                return Optional.empty<UnitConfigType.UnitConfig>().get()
            }

            for (locationConfig in ArrayList(rootLocationConfigList.values)) {
                if (!locationConfig.hasPlacementConfig()) {
                } else if (!locationConfig.placementConfig.hasLocationId()) {
                } else if (locationConfig.placementConfig.locationId.isEmpty()) {
                } else if (locationConfig.placementConfig.locationId == locationConfig.id) {
                    return locationConfig
                } else {
                    rootLocationConfigList.remove(locationConfig.id)
                }
            }

            if (rootLocationConfigList.isEmpty()) {
                throw NotAvailableException("root candidate")
            } else if (rootLocationConfigList.size == 1) {
                for (unitConfig in rootLocationConfigList.values) {
                    return Optional.of(unitConfig).get()
                }
                return Optional.empty<UnitConfigType.UnitConfig>().get()
            }

            throw InvalidStateException("To many potential root locations detected!")
        } catch (ex: CouldNotPerformException) {
            throw CouldNotPerformException("Could not compute root location!", ex)
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
    @JvmStatic
    @Throws(CouldNotPerformException::class)
    fun detectLocationType(
        locationUnit: UnitConfigType.UnitConfig,
        locationRegistry: ProtoBufRegistry<String, UnitConfigType.UnitConfig, UnitConfigType.UnitConfig.Builder>,
    ): LocationType {
        try {
            if (!locationUnit.hasPlacementConfig()) {
                throw NotAvailableException("placementConfig")
            }
            if (!locationUnit.placementConfig.hasLocationId() || locationUnit.placementConfig.locationId.isEmpty()) {
                throw NotAvailableException("placementConfig.locationId")
            }

            if (locationUnit.locationConfig.root) {
                return LocationType.ZONE
            }

            val parentLocationType =
                locationRegistry[locationUnit.placementConfig.locationId].message.locationConfig.locationType

            val childLocationTypes: List<LocationType> = locationUnit.locationConfig.childIdList
                .map { childId -> locationRegistry[childId].message.locationConfig.locationType }
                .distinct()
                .filter { it != LocationType.UNKNOWN }

            return detectLocationType(parentLocationType, childLocationTypes)
        } catch (ex: CouldNotPerformException) {
            throw CouldNotPerformException("Could not detect Type for location [" + locationUnit.label + "]", ex)
        }
    }

    /**
     * Detect the type of a location.
     * Since a location, except the root location, always has a parent the only ambiguous case
     * is when the parent is a zone but no children are defined. Then the location can either be
     * a zone or a tile. In this case an exception is thrown.
     *
     * @param parentLocationType the type of the parent location of the location whose type is detected
     * @param childLocationTypes a set of all immediate child types of the location whose type is detected
     * @return the type locationUnit should have
     * @throws CouldNotPerformException if the type is ambiguous
     */
    @JvmStatic
    @Throws(CouldNotPerformException::class)
    private fun detectLocationType(
        parentLocationType: LocationType,
        childLocationTypes: List<LocationType>,
    ): LocationType {

        when (parentLocationType) {
            // if the parent is a region or tile then the location has to be a region
            LocationType.REGION, LocationType.TILE -> {
                return LocationType.REGION
            }

            LocationType.ZONE -> {

                // if the parent is a zone and has no children then it has to be
                // a tile since each branch could contain exactly one tile
                if (childLocationTypes.isEmpty()) {
                    return LocationType.TILE
                }

                // if one child is a zone or a tile the location has to be a zone
                if (childLocationTypes.contains(LocationType.ZONE) || childLocationTypes.contains(LocationType.TILE)) {
                    return LocationType.ZONE
                }

                // if the parent is a zone and a child is a region than the location has to be a tile
                if (childLocationTypes.contains(LocationType.REGION)) {
                    return LocationType.TILE
                }
            }

            LocationType.UNKNOWN -> {} // skip detection
        }
        throw CouldNotPerformException("Could not detect locationType from parentType[${parentLocationType.name}] and childTypes ${childLocationTypes.map { it.name }}")
    }
}
