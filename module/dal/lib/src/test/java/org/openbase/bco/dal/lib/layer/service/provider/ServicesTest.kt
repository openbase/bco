package org.openbase.bco.dal.lib.layer.service.provider

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import org.openbase.bco.dal.lib.action.Action
import org.openbase.bco.dal.lib.layer.service.Services
import org.openbase.bco.dal.lib.state.States
import org.openbase.bco.registry.remote.Registries
import org.openbase.bco.registry.unit.test.AbstractBCORegistryTest
import org.openbase.type.domotic.service.ServiceStateDescriptionType.ServiceStateDescription
import org.openbase.type.domotic.service.ServiceTemplateType.ServiceTemplate
import java.util.function.Consumer

/*-
 * #%L
 * BCO DAL Library
 * %%
 * Copyright (C) 2014 - 2021 openbase.org
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 *
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-3.0.html>.
 * #L%
 */
class ServicesTest : AbstractBCORegistryTest() {

    @Test
    @Timeout(value = 30)
    fun testComputeActionImpact() {
        val unitRegistry = Registries.getUnitRegistry(true)
        val serviceState = ServiceStateDescription.newBuilder().apply {
            unitId = unitRegistry.rootLocationConfig.id
            serviceState = Services.serializeServiceState(
                States.Power.ON,
                true
            )
            serviceType = ServiceTemplate.ServiceType.POWER_STATE_SERVICE
        }.build()

        val impact = Services.computeActionImpact(serviceState)
            .onEach { it.actionId shouldBe Action.PRECOMPUTED_ACTION_ID }

        val impactedUnitIdList = impact
            .map { it.serviceStateDescription.unitId }
            .distinct()
            .sorted()

        val affectedUnitsUnsorted = unitRegistry.getUnitConfigsByLocationIdAndServiceType(
            unitRegistry.rootLocationConfig.id,
            serviceState.serviceType
        )
            .map { it.id }
            .plus(unitRegistry.rootLocationConfig.id) // by design the action impacts also contain the location itself
            .distinct()
            .sorted()

        impactedUnitIdList shouldBe affectedUnitsUnsorted
    }
}
