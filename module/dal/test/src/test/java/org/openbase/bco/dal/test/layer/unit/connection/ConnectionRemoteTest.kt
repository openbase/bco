package org.openbase.bco.dal.test.layer.unit.connection

import io.kotest.matchers.comparables.shouldBeEqualComparingTo
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import org.openbase.bco.dal.control.layer.unit.ReedContactController
import org.openbase.bco.dal.lib.state.States
import org.openbase.bco.dal.remote.layer.unit.Units
import org.openbase.bco.dal.remote.layer.unit.connection.ConnectionRemote
import org.openbase.bco.dal.test.layer.unit.location.AbstractBCOLocationManagerTest
import org.openbase.bco.dal.test.layer.unit.location.LocationRemoteTest
import org.openbase.bco.registry.remote.Registries
import org.openbase.bco.registry.remote.Registries.getUnitRegistry
import org.openbase.bco.registry.unit.lib.UnitRegistry
import org.openbase.jul.exception.printer.ExceptionPrinter
import org.openbase.jul.extension.type.processing.ScopeProcessor
import org.openbase.type.domotic.service.ServiceTemplateType.ServiceTemplate
import org.openbase.type.domotic.state.ConnectionStateType
import org.openbase.type.domotic.state.ContactStateType.ContactState
import org.openbase.type.domotic.state.DoorStateType.DoorState
import org.openbase.type.domotic.unit.UnitTemplateType.UnitTemplate.UnitType
import org.slf4j.LoggerFactory
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
    // todo: test still a bit unstable if repeated 100 times.
    fun testDoorStateUpdate() {
        println("testDoorStateUpdate")

        val reedContactConfigs = getUnitRegistry()
            .getUnitConfigsByUnitType(UnitType.REED_CONTACT)

        val reedContactControllers = reedContactConfigs
            .map { deviceManagerLauncher.launchable.unitControllerRegistry[it.id] }

        val reedContacts = reedContactConfigs
            .map { Units.getUnit(it, true, Units.REED_CONTACT) }

        val connectionRemote: ConnectionRemote = Units.getUnit(
            getUnitRegistry().getUnitConfigsByUnitType(UnitType.CONNECTION)[0],
            true,
            ConnectionRemote::class.java
        )

        // set reeds closed
        reedContactControllers.forEach {
            it.applyServiceState(States.Contact.CLOSED, ServiceTemplate.ServiceType.CONTACT_STATE_SERVICE)
        }
        Thread.sleep(100)

        // sync remotes
        reedContacts
            .plus(connectionRemote)
            .map { it.requestData() }
            .forEach { it.get(5, TimeUnit.SECONDS) }


        // check door closed
        connectionRemote.doorState.value shouldBeEqualComparingTo DoorState.State.CLOSED

        // set reeds open
        reedContactControllers.forEach {
            it.applyServiceState(States.Contact.OPEN, ServiceTemplate.ServiceType.CONTACT_STATE_SERVICE)
        }
        Thread.sleep(100)

        // sync remotes
        reedContacts
            .plus(connectionRemote)
            .map { it.requestData() }
            .forEach { it.get(5, TimeUnit.SECONDS) }

        // check door open
        connectionRemote.doorState.value shouldBeEqualComparingTo DoorState.State.OPEN
    }
}
