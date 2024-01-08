package org.openbase.bco.registry.unit.core.consistency.connectionconfig

import org.openbase.jul.exception.CouldNotPerformException
import org.openbase.jul.exception.printer.ExceptionPrinter
import org.openbase.jul.extension.protobuf.IdentifiableMessage
import org.openbase.jul.extension.protobuf.container.ProtoBufMessageMap
import org.openbase.jul.storage.registry.AbstractProtoBufRegistryConsistencyHandler
import org.openbase.jul.storage.registry.EntryModification
import org.openbase.jul.storage.registry.ProtoBufFileSynchronizedRegistry
import org.openbase.jul.storage.registry.ProtoBufRegistry
import org.openbase.type.domotic.registry.UnitRegistryDataType.UnitRegistryData
import org.openbase.type.domotic.unit.UnitConfigType.UnitConfig

/**
 *
 * @author [Tamino Huxohl](mailto:pleminoq@openbase.org)
 */
class ConnectionLocationConsistencyHandler(
    private val locationRegistry: ProtoBufFileSynchronizedRegistry<String, UnitConfig, UnitConfig.Builder, UnitRegistryData.Builder>,
) :
    AbstractProtoBufRegistryConsistencyHandler<String, UnitConfig, UnitConfig.Builder>() {
    @Throws(CouldNotPerformException::class, EntryModification::class)
    override fun processData(
        id: String,
        entry: IdentifiableMessage<String, UnitConfig, UnitConfig.Builder>,
        entryMap: ProtoBufMessageMap<String, UnitConfig, UnitConfig.Builder>,
        registry: ProtoBufRegistry<String, UnitConfig, UnitConfig.Builder>,
    ) {
        val connectionUnitConfig = entry.message.toBuilder()

        val locationId: String? = try {
            getLowestCommonParentLocation(connectionUnitConfig.connectionConfig.tileIdList, locationRegistry)?.id
        } catch (ex: CouldNotPerformException) {
            ExceptionPrinter.printHistory(
                "Could not find parent location for connection [$connectionUnitConfig]",
                ex,
                logger
            )
            null
        } ?: locationRegistry.messages.firstOrNull { it.locationConfig.root }?.id

        locationId?.let {
            if (locationId != connectionUnitConfig.placementConfig.locationId) {
                val placement = connectionUnitConfig.placementConfig.toBuilder().setLocationId(locationId)
                throw EntryModification(
                    entry.setMessage(connectionUnitConfig.setPlacementConfig(placement), this),
                    this
                )
            }
        }
    }

    companion object {
        fun getLowestCommonParentLocation(
            locationIds: List<String>,
            locationUnitConfigRegistry: ProtoBufFileSynchronizedRegistry<String, UnitConfig, UnitConfig.Builder, UnitRegistryData.Builder>,
        ): UnitConfig? {
            // list containing the paths from root to each location given by locationIds sorted by the lenght of the path, e.g.:
            //      home, apartment, hallway, entrance
            //      home, apartment, outdoor
            val pathsFromRootMap: MutableList<List<UnitConfig>> = ArrayList()

            // fill the list according to the description above
            locationIds.forEach { id ->
                var locationUnitConfig = locationUnitConfigRegistry.getMessage(id)
                val pathFromRootList: MutableList<UnitConfig> = ArrayList()
                pathFromRootList.add(locationUnitConfig)
                while (!locationUnitConfig.locationConfig.root) {
                    locationUnitConfig =
                        locationUnitConfigRegistry.getMessage(locationUnitConfig.placementConfig.locationId)
                    // when adding a location at the front of the list, every entry is moved an index further
                    pathFromRootList.add(0, locationUnitConfig)
                }
                pathsFromRootMap.add(pathFromRootList)
            }

            // sort the list after their sizes:
            //      home, apartment, outdoor
            //      home, apartment, hallway, entrance
            pathsFromRootMap.sortWith { o1: List<UnitConfig>, o2: List<UnitConfig> -> o2.size - o1.size }

            // find the lowest common parent, e.g. for the example above apartment
            // by returning the index before the first elements where the paths differ

            // return null in case connection is not linked to any locations
            if (pathsFromRootMap.isEmpty()) {
                return null;
            }

            val shortestPath = pathsFromRootMap[0].size
            (0 until shortestPath).forEach { i ->
                val currentId = pathsFromRootMap[0][i].id
                (1 until pathsFromRootMap.size).forEach { j ->
                    if (pathsFromRootMap[j][i].id != currentId) {
                        return pathsFromRootMap[0][i - 1]
                    }
                }
            }

            // checking if a lowest common parent exists should not be necessary since a tile cannot be root
            return pathsFromRootMap[0][0]
        }
    }
}
