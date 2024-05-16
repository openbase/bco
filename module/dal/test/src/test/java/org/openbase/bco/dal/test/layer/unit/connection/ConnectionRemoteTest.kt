package org.openbase.bco.dal.test.layer.unit.connection

import io.kotest.matchers.comparables.shouldBeEqualComparingTo
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import org.openbase.bco.dal.lib.state.States
import org.openbase.bco.dal.remote.layer.unit.Units
import org.openbase.bco.dal.remote.layer.unit.connection.ConnectionRemote
import org.openbase.bco.dal.test.layer.unit.location.AbstractBCOLocationManagerTest
import org.openbase.bco.registry.mock.MockRegistry
import org.openbase.bco.registry.remote.Registries.getUnitRegistry
import org.openbase.type.domotic.service.ServiceTemplateType.ServiceTemplate
import org.openbase.type.domotic.state.DoorStateType.DoorState
import java.util.concurrent.TimeUnit

/*
 * #%L
 * BCO DAL Test
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
 */ /**
 *
 * @author [Tamino Huxohl](mailto:pleminoq@openbase.org)
 */
class ConnectionRemoteTest : AbstractBCOLocationManagerTest() {
    /**
     * Test if changes in unitControllers are published to a connection remote.
     *
     * @throws Exception
     */
    @Test
    @Timeout(5)
    fun testDoorStateUpdate() {
        println("testDoorStateUpdate")

        val reedContactConfig = getUnitRegistry()
            .getUnitConfigByAlias(MockRegistry.ALIAS_REED_SWITCH_HEAVEN_STAIRWAY_GATE)

        val reedContactController = reedContactConfig
            .let { deviceManagerLauncher.launchable!!.unitControllerRegistry[it.id] }

        val reedContact = reedContactConfig
            .let { Units.getUnit(it, true, Units.REED_CONTACT) }

        val connectionRemote: ConnectionRemote = Units.getUnitByAlias(
            MockRegistry.ALIAS_DOOR_STAIRS_HEAVEN_GATE,
            true,
            ConnectionRemote::class.java
        )

        // set reeds closed
        reedContactController
            .applyServiceState(States.Contact.CLOSED, ServiceTemplate.ServiceType.CONTACT_STATE_SERVICE)
        Thread.sleep(100)

        // sync remotes
        reedContact.requestData().get(5, TimeUnit.SECONDS)
        connectionRemote.requestData().get(5, TimeUnit.SECONDS)

        // check door closed
        connectionRemote.doorState.value shouldBeEqualComparingTo DoorState.State.CLOSED

        // set reeds open
        reedContactController
            .applyServiceState(States.Contact.OPEN, ServiceTemplate.ServiceType.CONTACT_STATE_SERVICE)
        Thread.sleep(100)

        // sync remotes
        reedContact.requestData().get(5, TimeUnit.SECONDS)
        connectionRemote.requestData().get(5, TimeUnit.SECONDS)

        // check door open
        connectionRemote.doorState.value shouldBeEqualComparingTo DoorState.State.OPEN
    }
}
