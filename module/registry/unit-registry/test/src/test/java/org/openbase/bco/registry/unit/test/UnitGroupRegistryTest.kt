package org.openbase.bco.registry.unit.test

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import org.openbase.bco.registry.mock.MockRegistry
import org.openbase.bco.registry.remote.Registries
import org.openbase.jul.extension.type.processing.LabelProcessor
import org.openbase.type.domotic.unit.UnitConfigType.UnitConfig
import org.openbase.type.domotic.unit.UnitTemplateType.UnitTemplate.UnitType
import java.util.*
import java.util.concurrent.ExecutionException

/*-
 * #%L
 * BCO Registry Unit Test
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
 */ /**
 * @author [Tamino Huxohl](mailto:thuxohl@techfak.uni-bielefeld.de)
 */
class UnitGroupRegistryTest : AbstractBCORegistryTest() {
    /**
     * Test if changing the placement of a unit group works.
     */
    @Test
    @Timeout(10)
    fun testPlacementChange() {
        val unitConfig = UnitConfig.newBuilder()
        LabelProcessor.addLabel(unitConfig.labelBuilder, Locale.ENGLISH, "PlacementChangeGroup")
        unitConfig.unitType = UnitType.UNIT_GROUP
        val placement = unitConfig.placementConfigBuilder
        placement.locationId = Registries.getUnitRegistry().rootLocationConfig.id
        val unitGroupConfig = unitConfig.unitGroupConfigBuilder
        unitGroupConfig.unitType = UnitType.COLORABLE_LIGHT
        unitGroupConfig.addMemberId(
            Registries.getUnitRegistry().getUnitConfigsByUnitType(UnitType.COLORABLE_LIGHT)[0].id
        )
        val registeredGroup = Registries.getUnitRegistry().registerUnitConfig(unitConfig.build()).get()

        registeredGroup.placementConfig.shape.boundingBox shouldBe MockRegistry.DEFAULT_BOUNDING_BOX
    }

    /**
     * Test if it is possible to register unit groups with recursive references.
     */
    @Test
    @Timeout(5)
    fun `test unit group recursion`() {
        val colorableLightIds = Registries.getUnitRegistry().getUnitConfigsByUnitType(UnitType.COLORABLE_LIGHT)
            .map { unitConfig -> unitConfig.id }

        var unitGroup1 = registerUnitGroup(UnitType.COLORABLE_LIGHT, colorableLightIds).toBuilder()
        var unitGroup2 = registerUnitGroup(UnitType.COLORABLE_LIGHT, colorableLightIds).toBuilder()
        var unitGroup3 = registerUnitGroup(UnitType.COLORABLE_LIGHT, colorableLightIds).toBuilder()

        unitGroup1.unitGroupConfigBuilder.addMemberId(unitGroup2.id)
        unitGroup1.unitGroupConfig.memberIdList shouldContain unitGroup2.id
        unitGroup1 = Registries.getUnitRegistry().updateUnitConfig(unitGroup1.build()).get().toBuilder()
        unitGroup1.unitGroupConfig.memberIdList shouldContain unitGroup2.id

        unitGroup2.unitGroupConfigBuilder.addMemberId(unitGroup3.id)
        unitGroup2 = Registries.getUnitRegistry().updateUnitConfig(unitGroup2.build()).get().toBuilder()
        unitGroup2.unitGroupConfig.memberIdList shouldContain unitGroup3.id

        unitGroup3.unitGroupConfigBuilder.addMemberId(unitGroup3.id)
        val updateFuture = Registries.getUnitRegistry().updateUnitConfig(unitGroup3.build())
        shouldThrow<ExecutionException> { updateFuture.get() }
    }

    private fun registerUnitGroup(unitGroupType: UnitType, memberIds: List<String> = emptyList()): UnitConfig {
        val unitGroup = UnitConfig.newBuilder()
        unitGroup.unitType = UnitType.UNIT_GROUP
        unitGroup.unitGroupConfigBuilder.setUnitType(unitGroupType).addAllMemberId(memberIds)
        return Registries.getUnitRegistry().registerUnitConfig(unitGroup.build()).get()
    }
}
