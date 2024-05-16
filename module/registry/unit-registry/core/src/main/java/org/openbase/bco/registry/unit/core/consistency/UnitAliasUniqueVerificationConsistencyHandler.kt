package org.openbase.bco.registry.unit.core.consistency

import org.openbase.bco.registry.unit.lib.UnitRegistry
import org.openbase.jul.exception.CouldNotPerformException
import org.openbase.jul.exception.RejectedException
import org.openbase.jul.extension.protobuf.IdentifiableMessage
import org.openbase.jul.extension.protobuf.container.ProtoBufMessageMap
import org.openbase.jul.extension.type.processing.ScopeProcessor
import org.openbase.jul.storage.registry.AbstractProtoBufRegistryConsistencyHandler
import org.openbase.jul.storage.registry.EntryModification
import org.openbase.jul.storage.registry.ProtoBufRegistry
import org.openbase.type.domotic.unit.UnitConfigType
import java.util.*

class UnitAliasUniqueVerificationConsistencyHandler(private val unitRegistry: UnitRegistry) :
    AbstractProtoBufRegistryConsistencyHandler<String, UnitConfigType.UnitConfig, UnitConfigType.UnitConfig.Builder>() {
    private var aliasUnitIdMap: MutableMap<String, String> = HashMap()

    @Throws(CouldNotPerformException::class, EntryModification::class)
    override fun processData(
        id: String,
        entry: IdentifiableMessage<String, UnitConfigType.UnitConfig, UnitConfigType.UnitConfig.Builder>,
        entryMap: ProtoBufMessageMap<String, UnitConfigType.UnitConfig, UnitConfigType.UnitConfig.Builder>,
        registry: ProtoBufRegistry<String, UnitConfigType.UnitConfig, UnitConfigType.UnitConfig.Builder>,
    ) {
        val unitConfig = entry.message.toBuilder()

        for (alias in unitConfig.aliasList) {
            if (!aliasUnitIdMap.containsKey(alias.lowercase(Locale.getDefault()))) {
                aliasUnitIdMap[alias.lowercase(Locale.getDefault())] = unitConfig.id
            } else {
                // if already known check if this unit is owning the alias otherwise throw invalid state
                if (aliasUnitIdMap[alias.lowercase(Locale.getDefault())] != unitConfig.id) {
                    throw RejectedException(
                        "Alias[" + alias.lowercase(Locale.getDefault()) + "] of Unit[" + ScopeProcessor.generateStringRep(
                            unitConfig.scope
                        ) + ", " + unitConfig.id + "] is already used by Unit[" + aliasUnitIdMap[alias.lowercase(
                            Locale.getDefault()
                        )] + "]"
                    )
                }
            }
        }
    }

    override fun reset() {
        aliasUnitIdMap = unitRegistry
            .getUnitConfigs(true)
            .flatMap { config -> config.aliasList.map { alias -> alias.lowercase() to config.id } }
            .toMap()
            .toMutableMap()
        super.reset()
    }

    override fun shutdown() {
        aliasUnitIdMap.clear()
        // super call is not performed because those would only call reset() which fails because the unit registry is not responding during shutdown.
    }
}
