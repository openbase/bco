package org.openbase.bco.api.graphql.subscriptions

import com.google.common.collect.ImmutableList
import org.openbase.bco.api.graphql.error.ServerError
import org.openbase.bco.api.graphql.schema.RegistrySchemaModule
import org.openbase.bco.registry.remote.Registries
import org.openbase.jul.pattern.provider.DataProvider
import org.openbase.type.domotic.registry.UnitRegistryDataType
import org.openbase.type.domotic.unit.UnitConfigType
import org.openbase.type.domotic.unit.UnitFilterType

class UnitRegistrySubscriptionObserver(
    private val unitFilter: UnitFilterType.UnitFilter,
    private val includeDisabledUnits: Boolean,
) : AbstractObserverMapper<DataProvider<UnitRegistryDataType.UnitRegistryData>, UnitRegistryDataType.UnitRegistryData, List<UnitConfigType.UnitConfig>>() {
    private val unitConfigs: MutableList<UnitConfigType.UnitConfig>

    init {
        Registries.getUnitRegistry(
            ServerError.BCO_TIMEOUT_SHORT,
            ServerError.BCO_TIMEOUT_TIME_UNIT
        )
        unitConfigs = ArrayList(
            RegistrySchemaModule.getUnitConfigs(
                unitFilter, includeDisabledUnits
            )
        )
    }

    @Throws(Exception::class)
    override fun update(
        source: DataProvider<UnitRegistryDataType.UnitRegistryData>,
        target: UnitRegistryDataType.UnitRegistryData,
    ) {
        val newUnitConfigs: ImmutableList<UnitConfigType.UnitConfig> =
            RegistrySchemaModule.getUnitConfigs(
                unitFilter, includeDisabledUnits
            )
        if (newUnitConfigs == unitConfigs) {
            // nothing has changed
            return
        }

        // store update
        unitConfigs.clear()
        unitConfigs.addAll(newUnitConfigs)
        super.update(source, target)
    }

    @Throws(Exception::class)
    override fun mapData(
        source: DataProvider<UnitRegistryDataType.UnitRegistryData>,
        data: UnitRegistryDataType.UnitRegistryData,
    ): List<UnitConfigType.UnitConfig> {
        return unitConfigs
    }
}
