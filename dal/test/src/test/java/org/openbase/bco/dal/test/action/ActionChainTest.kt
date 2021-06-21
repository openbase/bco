package org.openbase.bco.dal.test.action

import lombok.extern.slf4j.Slf4j
import org.junit.Assert
import org.junit.BeforeClass
import org.junit.Test
import org.openbase.bco.dal.remote.layer.unit.Units
import org.openbase.bco.dal.test.layer.unit.location.AbstractBCOLocationManagerTest
import org.openbase.bco.dal.test.layer.unit.location.LocationRemoteTest
import org.openbase.bco.registry.remote.Registries
import org.openbase.jul.exception.printer.ExceptionPrinter
import org.openbase.type.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType.*
import org.openbase.type.domotic.state.PowerStateType.PowerState
import org.openbase.type.domotic.unit.UnitTemplateType.UnitTemplate.UnitType.*
import org.slf4j.LoggerFactory

@Slf4j
class ActionChainTest : AbstractBCOLocationManagerTest() {

    @Test(timeout = 5000)
    @Throws(Exception::class)
    fun `when a location is turned of its units that support the power service are registered as impact`() {
        val rootLocationRemote =
            Units.getUnit(Registries.getUnitRegistry().rootLocationConfig, true, Units.LOCATION)

        rootLocationRemote.waitForData()

        val remoteAction =
            waitForExecution(rootLocationRemote.setPowerState(PowerState.State.ON))

        // resolve all power state units
        val powerStateUnits = rootLocationRemote.getUnitMap(true)
            .flatMap { it.value  }
            .filter { unit ->
                unit.config.serviceConfigList.any {
                    it.serviceDescription.serviceType == POWER_STATE_SERVICE
                }}

        remoteAction.actionDescription.actionImpactList
            .map { it.serviceStateDescription.unitId }
            .forEach { impactedUnitId ->
                Assert.assertTrue(powerStateUnits.any { it.id == impactedUnitId })
            }
    }

    @Test(timeout = 5000)
    @Throws(Exception::class)
    fun `when a light is switched on the locations the light is a part of are registered as impact`() {
        val rootLocationRemote =
            Units.getUnit(Registries.getUnitRegistry().rootLocationConfig, true, Units.LOCATION)

        val impact = rootLocationRemote.getUnits(COLORABLE_LIGHT, true, Units.COLORABLE_LIGHT)
            .map { light -> waitForExecution(light.setPowerState(PowerState.State.ON)) }
            .map { action -> action.actionDescription.actionImpactList }

        impact.forEach { Assert.assertFalse(it.isEmpty()) }
        impact.forEach { impactedUnits ->
                Assert.assertTrue(impactedUnits.any {
                    it.serviceStateDescription.unitId == rootLocationRemote.id
                })
            }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(LocationRemoteTest::class.java)

        @BeforeClass
        @Throws(Throwable::class)
        fun setUpClass() {
            try {
                AbstractBCOLocationManagerTest.setUpClass()
            } catch (ex: Throwable) {
                throw ExceptionPrinter.printHistoryAndReturnThrowable(ex, logger)
            }
        }
    }
}
