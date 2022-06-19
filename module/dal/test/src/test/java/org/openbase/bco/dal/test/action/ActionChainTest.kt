package org.openbase.bco.dal.test.action

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import org.openbase.bco.dal.remote.layer.unit.Units
import org.openbase.bco.dal.test.layer.unit.location.AbstractBCOLocationManagerTest
import org.openbase.bco.dal.test.layer.unit.location.LocationRemoteTest
import org.openbase.bco.registry.remote.Registries
import org.openbase.jul.exception.printer.ExceptionPrinter
import org.openbase.type.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType.POWER_STATE_SERVICE
import org.openbase.type.domotic.state.PowerStateType.PowerState
import org.openbase.type.domotic.unit.UnitTemplateType.UnitTemplate.UnitType.COLORABLE_LIGHT
import org.slf4j.LoggerFactory

class ActionChainTest : AbstractBCOLocationManagerTest() {

    private val log = LoggerFactory.getLogger(javaClass)

    @Test
    @Timeout(15)
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
                assertTrue(powerStateUnits.any { it.id == impactedUnitId })
            }
    }

    @Test
    @Timeout(15)
    @Throws(Exception::class)
    fun `when a light is switched on the locations the light is a part of are registered as impact`() {
        val rootLocationRemote =
            Units.getUnit(Registries.getUnitRegistry().rootLocationConfig, true, Units.LOCATION)

        val impact = rootLocationRemote.getUnits(COLORABLE_LIGHT, true, Units.COLORABLE_LIGHT)
            .map { light -> waitForExecution(light.setPowerState(PowerState.State.ON)) }
            .map { action -> action.actionDescription.actionImpactList }

        impact.forEach { assertFalse(it.isEmpty()) }
        impact.forEach { impactedUnits ->
                assertTrue(impactedUnits.any {
                    it.serviceStateDescription.unitId == rootLocationRemote.id
                })
            }
    }
}
