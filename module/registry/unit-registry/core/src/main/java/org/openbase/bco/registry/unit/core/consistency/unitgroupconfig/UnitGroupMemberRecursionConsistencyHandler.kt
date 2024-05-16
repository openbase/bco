package org.openbase.bco.registry.unit.core.consistency.unitgroupconfig

import org.openbase.jul.exception.CouldNotPerformException
import org.openbase.jul.exception.InvalidStateException
import org.openbase.jul.extension.protobuf.IdentifiableMessage
import org.openbase.jul.extension.protobuf.container.ProtoBufMessageMap
import org.openbase.jul.extension.type.processing.LabelProcessor
import org.openbase.jul.storage.registry.AbstractProtoBufRegistryConsistencyHandler
import org.openbase.jul.storage.registry.EntryModification
import org.openbase.jul.storage.registry.ProtoBufRegistry
import org.openbase.type.domotic.unit.UnitConfigType.UnitConfig

/*
 * #%L
 * BCO Registry Unit Core
 * %%
 * Copyright (C) 2014 - 2021 openbase.org
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
 */ /** @author [Tamino Huxohl](mailto:pleminoq@openbase.org) */
class UnitGroupMemberRecursionConsistencyHandler
    : AbstractProtoBufRegistryConsistencyHandler<String, UnitConfig, UnitConfig.Builder>() {

    @Throws(CouldNotPerformException::class, EntryModification::class)
    override fun processData(
        id: String,
        entry: IdentifiableMessage<String, UnitConfig, UnitConfig.Builder>,
        entryMap: ProtoBufMessageMap<String, UnitConfig, UnitConfig.Builder>,
        registry: ProtoBufRegistry<String, UnitConfig, UnitConfig.Builder>,
    ) {
        val unitGroupUnitConfig = entry.message ?: return
        val memberIds = unitGroupUnitConfig.unitGroupConfig.memberIdList.toList()

        unitGroupUnitConfig
            .takeIf { memberIds.containsUnit(it, registry) }
            ?.let {
                throw InvalidStateException(
                    "UnitGroup[${LabelProcessor.getBestMatch(unitGroupUnitConfig.label)}] " +
                            "refers itself as member!"
                )
            }
    }

    private fun List<String>.containsUnit(
        unit: UnitConfig,
        registry: ProtoBufRegistry<String, UnitConfig, UnitConfig.Builder>,
    ): Boolean = this
        .filter { registry.contains(it) }
        .mapNotNull { registry.get(it).message }
        .any { it.id == unit.id || it.unitGroupConfig.memberIdList.toList().containsUnit(unit, registry) }
}
