package org.openbase.bco.registry.unit.core.consistency.connectionconfig

import org.openbase.jul.exception.CouldNotPerformException
import org.openbase.jul.extension.protobuf.IdentifiableMessage
import org.openbase.jul.extension.protobuf.container.ProtoBufMessageMap
import org.openbase.jul.storage.registry.AbstractProtoBufRegistryConsistencyHandler
import org.openbase.jul.storage.registry.EntryModification
import org.openbase.jul.storage.registry.ProtoBufFileSynchronizedRegistry
import org.openbase.jul.storage.registry.ProtoBufRegistry
import org.openbase.type.domotic.registry.UnitRegistryDataType
import org.openbase.type.domotic.unit.UnitConfigType
import org.openbase.type.domotic.unit.location.LocationConfigType.LocationConfig.LocationType

/**
 *
 * @author [Tamino Huxohl](mailto:pleminoq@openbase.org)
 */
class ConnectionTilesConsistencyHandler(private val locationRegistry: ProtoBufFileSynchronizedRegistry<String, UnitConfigType.UnitConfig, UnitConfigType.UnitConfig.Builder, UnitRegistryDataType.UnitRegistryData.Builder>) :
    AbstractProtoBufRegistryConsistencyHandler<String, UnitConfigType.UnitConfig, UnitConfigType.UnitConfig.Builder>() {
    @Throws(CouldNotPerformException::class, EntryModification::class)
    override fun processData(
        id: String,
        entry: IdentifiableMessage<String, UnitConfigType.UnitConfig, UnitConfigType.UnitConfig.Builder>,
        entryMap: ProtoBufMessageMap<String, UnitConfigType.UnitConfig, UnitConfigType.UnitConfig.Builder>,
        registry: ProtoBufRegistry<String, UnitConfigType.UnitConfig, UnitConfigType.UnitConfig.Builder>,
    ) {
        val connectionUnitConfig = entry.message.toBuilder()
        val connectionConfig = connectionUnitConfig.connectionConfigBuilder

        // remove duplicated entries and location ids that are not tiles
        entry.message.connectionConfig.tileIdList
            .distinct()
            .filter { tileId -> locationRegistry[tileId].message?.locationConfig?.locationType == LocationType.TILE }
            .let { tileIds ->
                if (connectionConfig.tileIdList.toList().sorted() != tileIds.sorted()) {
                    connectionConfig.clearTileId()
                    connectionConfig.addAllTileId(tileIds)
                    throw EntryModification(entry.setMessage(connectionUnitConfig, this), this)
                }
            }
    }
}
