package org.openbase.bco.api.graphql.batchloader

import org.dataloader.BatchLoader
import org.openbase.bco.registry.unit.lib.UnitRegistry
import org.openbase.jul.exception.CouldNotPerformException
import org.openbase.jul.exception.printer.ExceptionPrinter
import org.openbase.type.domotic.unit.UnitConfigType
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionStage

/*-
 * #%L
 * BCO GraphQL API
 * %%
 * Copyright (C) 2020 openbase.org
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
 */@Component
class BCOUnitBatchLoader(private val unitRegistry: UnitRegistry) : BatchLoader<String, UnitConfigType.UnitConfig> {
    private val log = LoggerFactory.getLogger(javaClass)
    override fun load(ids: List<String>): CompletionStage<List<UnitConfigType.UnitConfig>> {
        val unitConfigList: MutableList<UnitConfigType.UnitConfig> = ArrayList()
        for (id in ids) {
            try {
                unitConfigList.add(unitRegistry.getUnitConfigById(id))
            } catch (ex: CouldNotPerformException) {
                ExceptionPrinter.printHistory("Could not resolve all unit config by id!", ex, log)
            }
        }
        return CompletableFuture.completedFuture(unitConfigList)
    }
}
