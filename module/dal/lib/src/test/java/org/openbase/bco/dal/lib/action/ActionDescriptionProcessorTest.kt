package org.openbase.bco.dal.lib.action

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import org.openbase.jul.extension.type.processing.LabelProcessor
import org.openbase.type.domotic.action.ActionDescriptionType.ActionDescription
import org.openbase.type.domotic.service.ServiceTemplateType
import org.openbase.type.domotic.state.PowerStateType
import org.openbase.type.domotic.unit.UnitConfigType
import java.util.*

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

class ActionDescriptionProcessorTest {

    @Timeout(10)
    @Test
    fun unitChainSuffixForNonReplaceableAction() {
        val buttonActionDescription = ActionDescription.newBuilder()
        val sceneChillActionDescription = ActionDescription.newBuilder()
        val sceneMoodLightActionDescription = ActionDescription.newBuilder()
        val locationActionDescription = ActionDescription.newBuilder()
        val lightActionDescription = ActionDescription.newBuilder()

        buttonActionDescription.serviceStateDescriptionBuilder.unitId = "Button"
        sceneChillActionDescription.serviceStateDescriptionBuilder.unitId = "SceneChill"
        sceneMoodLightActionDescription.serviceStateDescriptionBuilder.unitId = "SceneMoodLight"
        locationActionDescription.serviceStateDescriptionBuilder.unitId = "Location"
        lightActionDescription.serviceStateDescriptionBuilder.unitId = "Light"
        sceneChillActionDescription.replaceable = false
        sceneMoodLightActionDescription.replaceable = false

        ActionDescriptionProcessor.updateActionCause(sceneChillActionDescription, buttonActionDescription)
        ActionDescriptionProcessor.updateActionCause(sceneMoodLightActionDescription, sceneChillActionDescription)
        ActionDescriptionProcessor.updateActionCause(locationActionDescription, sceneMoodLightActionDescription)
        ActionDescriptionProcessor.updateActionCause(lightActionDescription, locationActionDescription)

        Assertions.assertEquals(true, buttonActionDescription.replaceable)
        Assertions.assertEquals(false, sceneChillActionDescription.replaceable)
        Assertions.assertEquals(false, sceneMoodLightActionDescription.replaceable)
        Assertions.assertEquals(true, locationActionDescription.replaceable)
        Assertions.assertEquals(true, lightActionDescription.replaceable)
        Assertions.assertEquals(
            "Button",
            ActionDescriptionProcessor.getUnitChainSuffixForNonReplaceableAction(buttonActionDescription.build()),
            "Chain suffix does not match!"
        )
        Assertions.assertEquals(
            "SceneChill",
            ActionDescriptionProcessor.getUnitChainSuffixForNonReplaceableAction(sceneChillActionDescription.build()),
            "Chain suffix does not match!"
        )
        Assertions.assertEquals(
            "SceneMoodLight",
            ActionDescriptionProcessor.getUnitChainSuffixForNonReplaceableAction(sceneMoodLightActionDescription.build()),
            "Chain suffix does not match!"
        )
        Assertions.assertEquals(
            "SceneMoodLight_Location",
            ActionDescriptionProcessor.getUnitChainSuffixForNonReplaceableAction(locationActionDescription.build()),
            "Chain suffix does not match!"
        )
        Assertions.assertEquals(
            "SceneMoodLight_Location_Light",
            ActionDescriptionProcessor.getUnitChainSuffixForNonReplaceableAction(lightActionDescription.build()),
            "Chain suffix does not match!"
        )
    }

    /**
     * Make sure that it is not possible to add two entries with the same language to
     * the description.
     */
    @Test
    fun `test description generation`() {
        val actionDescription = ActionDescription.newBuilder()
        actionDescription.serviceStateDescriptionBuilder.serviceType =
            ServiceTemplateType.ServiceTemplate.ServiceType.POWER_STATE_SERVICE

        val serviceState = PowerStateType.PowerState.newBuilder()
        serviceState.value = PowerStateType.PowerState.State.ON

        val unitConfig = UnitConfigType.UnitConfig.newBuilder()
        LabelProcessor.addLabel(unitConfig.labelBuilder, Locale.ENGLISH, "Mocked Lamp")

        ActionDescriptionProcessor.generateDescription(actionDescription, serviceState, unitConfig.build())
        Assertions.assertEquals(
            ActionDescriptionProcessor.GENERIC_ACTION_DESCRIPTION_MAP.size,
            actionDescription.description.entryCount
        )

        ActionDescriptionProcessor.generateDescription(actionDescription, serviceState, unitConfig.build())
        Assertions.assertEquals(
            ActionDescriptionProcessor.GENERIC_ACTION_DESCRIPTION_MAP.size,
            actionDescription.description.entryCount
        )
    }
}
