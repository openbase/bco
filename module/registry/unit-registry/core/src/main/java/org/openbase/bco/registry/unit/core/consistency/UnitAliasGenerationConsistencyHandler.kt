package org.openbase.bco.registry.unit.core.consistency

import org.openbase.bco.registry.unit.lib.UnitRegistry
import org.openbase.jul.storage.registry.AbstractProtoBufRegistryConsistencyHandler
import org.openbase.type.domotic.unit.UnitConfigType.UnitConfig
import org.openbase.type.domotic.unit.UnitTemplateType.UnitTemplate.UnitType
import org.openbase.jul.storage.registry.EntryModification
import org.openbase.jul.storage.registry.ProtoBufRegistry
import org.openbase.jul.exception.CouldNotPerformException
import org.openbase.jul.exception.printer.ExceptionPrinter
import org.openbase.jul.extension.protobuf.IdentifiableMessage
import org.openbase.jul.extension.protobuf.container.ProtoBufMessageMap
import org.openbase.jul.processing.StringProcessor
import org.openbase.type.domotic.unit.UnitConfigType.UnitConfig.Builder
import org.slf4j.LoggerFactory
import java.lang.NumberFormatException
import java.util.ArrayList
import java.util.HashMap
import java.util.stream.Collectors

/*-
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
 */
class UnitAliasGenerationConsistencyHandler (private val unitRegistry: UnitRegistry) :
    AbstractProtoBufRegistryConsistencyHandler<String, UnitConfig, Builder>() {
    private var unitTypeAliasNumberMap: MutableMap<UnitType, Int> = mutableMapOf()
    private var updateNeeded = true

    @Throws(CouldNotPerformException::class, EntryModification::class)
    override fun processData(
        id: String,
        entry: IdentifiableMessage<String, UnitConfig, Builder>,
        entryMap: ProtoBufMessageMap<String, UnitConfig, Builder>,
        registry: ProtoBufRegistry<String, UnitConfig, Builder>
    ) {
        val unitConfig = entry.message.toBuilder()
        val aliasPrefix = generateAliasPrefix(unitConfig.unitType)

        // create comparator that sorts the default alias on top.
        val aliasComparator = Comparator { o1: String, o2: String ->
            val o1StartsWithDefaultPrefix = o1.startsWith(aliasPrefix)
            val o2StartsWithDefaultPrefix = o2.startsWith(aliasPrefix)
            return@Comparator if (o1StartsWithDefaultPrefix && o2StartsWithDefaultPrefix) {
                o1.compareTo(o2)
            } else if (o1StartsWithDefaultPrefix) {
                -1000 + o1.compareTo(o2)
            } else if (o2StartsWithDefaultPrefix) {
                1000 + o1.compareTo(o2)
            } else {
                o1.compareTo(o2)
            }
        }

        if (unitConfig.aliasList.isEmpty() ||
            unitConfig.aliasList.stream().noneMatch { it: String -> it.startsWith(aliasPrefix) }
        ) {
            if (updateNeeded) {
                try {
                    updateUnitTypeAliasNumberMap()
                } catch (ex: CouldNotPerformException) {
                    ExceptionPrinter.printHistory("Could not update internal alias map!", ex, LOGGER)
                }
            }
            val alias = generateAndRegisterAlias(unitConfig.unitType)
            val newAliasList: MutableList<String> = ArrayList(unitConfig.aliasList)
            newAliasList.add(alias)
            newAliasList.sortWith(aliasComparator)
            unitConfig.clearAlias()
            unitConfig.addAllAlias(newAliasList)
            throw EntryModification(entry.setMessage(unitConfig, this), this)
        }

        // make sure default alias is always on top
        val sortedAliasList = ArrayList(unitConfig.aliasList)
            .stream()
            .sorted(aliasComparator)
            .distinct()
            .collect(Collectors.toList())
        if (sortedAliasList != unitConfig.aliasList) {
            unitConfig.clearAlias()
            unitConfig.addAllAlias(sortedAliasList)
            throw EntryModification(entry.setMessage(unitConfig, this), this)
        }
    }

    @Throws(CouldNotPerformException::class)
    private fun updateUnitTypeAliasNumberMap() {
        unitTypeAliasNumberMap.clear()
        for (unitConfig in unitRegistry.getUnitConfigsFiltered(false)) {
            registerAlias(unitConfig)
        }
    }

    private fun generateAndRegisterAlias(unitType: UnitType): String {

        // init if not exist
        if (!unitTypeAliasNumberMap!!.containsKey(unitType)) {
            unitTypeAliasNumberMap!![unitType] = 0
        }

        // generate next number
        val newNumber = unitTypeAliasNumberMap!![unitType]!! + 1

        // register number
        registerNumber(newNumber, unitType)

        // generate and return alias string
        return generateAliasPrefix(unitType) + newNumber
    }

    private fun generateAliasPrefix(unitType: UnitType): String {
        return StringProcessor.transformUpperCaseToPascalCase(unitType.name) + ALIAS_NUMBER_SEPARATOR
    }

    private fun registerAlias(unitConfig: UnitConfig) {
        for (alias in unitConfig.aliasList) {
            registerAlias(alias, unitConfig.unitType)
        }
    }

    private fun registerAlias(alias: String, unitType: UnitType) {
        val split = alias.split(ALIAS_NUMBER_SEPARATOR).toTypedArray()
        if (split.size != 2) {
            return
        }
        if (split[0] != StringProcessor.transformUpperCaseToPascalCase(unitType.name)) {
            return
        }
        try {
            registerNumber(split[1].toInt(), unitType)
        } catch (ex: NumberFormatException) {
            // do nothing since this alias seems not to collide with the default ones.
        }
    }

    private fun registerNumber(number: Int, unitType: UnitType) {
        if (!unitTypeAliasNumberMap!!.containsKey(unitType) || unitTypeAliasNumberMap!![unitType]!! < number) {
            unitTypeAliasNumberMap!![unitType] = number
        }
    }

    override fun reset() {
        updateNeeded = true
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(UnitAliasGenerationConsistencyHandler::class.java)
        const val ALIAS_NUMBER_SEPARATOR = "-"
    }
}
