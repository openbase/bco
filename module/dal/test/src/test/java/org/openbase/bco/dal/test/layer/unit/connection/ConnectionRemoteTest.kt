package org.openbase.bco.dal.test.layer.unit.connection

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import org.openbase.bco.dal.control.layer.unit.ReedContactController
import org.openbase.bco.dal.remote.layer.unit.Units
import org.openbase.bco.dal.remote.layer.unit.connection.ConnectionRemote
import org.openbase.bco.dal.test.layer.unit.location.AbstractBCOLocationManagerTest
import org.openbase.bco.dal.test.layer.unit.location.LocationRemoteTest
import org.openbase.bco.registry.remote.Registries
import org.openbase.jul.exception.printer.ExceptionPrinter
import org.openbase.jul.extension.type.processing.ScopeProcessor
import org.openbase.type.domotic.service.ServiceTemplateType.ServiceTemplate
import org.openbase.type.domotic.state.ConnectionStateType
import org.openbase.type.domotic.state.ContactStateType.ContactState
import org.openbase.type.domotic.state.DoorStateType.DoorState
import org.openbase.type.domotic.unit.UnitTemplateType.UnitTemplate.UnitType
import org.slf4j.LoggerFactory

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
    @Timeout(15)
    @Throws(Exception::class)
    fun testDoorStateUpdate() {
        println("testDoorStateUpdate")
        val reedContactControllerList: MutableList<ReedContactController> = ArrayList()
        for (dalUnitConfig in Registries.getUnitRegistry().dalUnitConfigs) {
            val unitController = deviceManagerLauncher.launchable.unitControllerRegistry[dalUnitConfig.id]
            if (unitController is ReedContactController) {
                reedContactControllerList.add(unitController)
            }
        }
        val closedState = ContactState.newBuilder().setValue(ContactState.State.CLOSED).build()
        for (reedContact in reedContactControllerList) {
            reedContact.applyDataUpdate(closedState, ServiceTemplate.ServiceType.CONTACT_STATE_SERVICE)
        }
        println("ping")
        connectionRemote!!.ping().get()
        println("ping done")
        println("request data of " + ScopeProcessor.generateStringRep(connectionRemote!!.scope))
        println("got data: " + connectionRemote!!.requestData().get().doorState.value)
        while (connectionRemote!!.doorState.value != DoorState.State.CLOSED) {
            println("current state: " + connectionRemote!!.doorState.value + " waiting for: " + DoorState.State.CLOSED)
            Thread.sleep(10)
        }
        Assertions.assertEquals(
            DoorState.State.CLOSED,
            connectionRemote!!.doorState.value,
            "Doorstate of the connection has not been updated!"
        )
        val openState = ContactState.newBuilder().setValue(ContactState.State.OPEN).build()
        for (reedContact in reedContactControllerList) {
            reedContact.applyDataUpdate(openState, ServiceTemplate.ServiceType.CONTACT_STATE_SERVICE)
        }
        println("ping")
        connectionRemote!!.ping().get()
        println("ping done")
        println("request data of " + ScopeProcessor.generateStringRep(connectionRemote!!.scope))
        println("got data: " + connectionRemote!!.requestData().get().doorState.value)
        while (connectionRemote!!.doorState.value != DoorState.State.OPEN) {
            println("current state: " + connectionRemote!!.doorState.value + " waiting for: " + DoorState.State.OPEN)
            Thread.sleep(10)
        }
        Assertions.assertEquals(
            DoorState.State.OPEN,
            connectionRemote!!.doorState.value,
            "Doorstate of the connection has not been updated!"
        )
    }

    companion object {
        private val logger = LoggerFactory.getLogger(LocationRemoteTest::class.java)
        private var connectionRemote: ConnectionRemote? = null
        @BeforeAll
        @Throws(Throwable::class)
        fun loadUnits() {
            try {
                connectionRemote = Units.getUnit(
                    Registries.getUnitRegistry().getUnitConfigsByUnitType(UnitType.CONNECTION)[0],
                    true,
                    ConnectionRemote::class.java
                )
                connectionRemote.waitForConnectionState(ConnectionStateType.ConnectionState.State.CONNECTED)
            } catch (ex: Throwable) {
                throw ExceptionPrinter.printHistoryAndReturnThrowable(ex, logger)
            }
        }
    }
}
