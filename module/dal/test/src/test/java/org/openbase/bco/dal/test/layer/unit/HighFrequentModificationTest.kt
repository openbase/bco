package org.openbase.bco.dal.test.layer.unit

import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import org.openbase.bco.dal.lib.state.States
import org.openbase.bco.dal.remote.layer.unit.LightRemote
import org.openbase.bco.dal.remote.layer.unit.Units
import org.openbase.bco.dal.test.AbstractBCODeviceManagerTest
import org.openbase.bco.registry.mock.MockRegistry
import org.openbase.bco.registry.remote.Registries
import org.openbase.jul.extension.type.processing.LabelProcessor
import org.openbase.type.domotic.unit.UnitTemplateType.UnitTemplate
import java.util.logging.Level
import java.util.logging.Logger

/**
 * @author [Tamino Huxohl](mailto:pleminoq@openbase.org)
 */
class HighFrequentModificationTest : AbstractBCODeviceManagerTest() {

    var lightRemote: LightRemote? = null

    @BeforeAll
    @Timeout(30)
    @Throws(Throwable::class)
    fun loadUnits() {
        lightRemote = Units.getUnitByAlias<LightRemote?>(
            MockRegistry.getUnitAlias(UnitTemplate.UnitType.LIGHT),
            true,
            LightRemote::class.java
        )
    }

    /**
     * Test to simulate race conditions by modifying the Light with 4 threads in parallel.
     *
     * This test creates four threads that perform concurrent modifications on the LightRemote instance:
     * - Two threads toggle the PowerState randomly between ON and OFF.
     * - Two threads modify the UnitConfig (label and location) of the light.
     *
     * This test aims to expose potential race conditions in the LightRemote implementation.
     *
     * @throws Exception if any thread encounters an error during execution.
     */
    @RepeatedTest(10)
    @Timeout(30)
    @Throws(Exception::class)
    fun `high frequent config and service modification should should be possible`() {
        println("testRaceCondition")

        val numberOfInterations = 10

        // Thread 1: Randomly toggle PowerState ON/OFF
        val powerStateThread1 = Thread(Runnable {
            try {
                for (i in 0..numberOfInterations) {
                    lightRemote!!.setPowerState(if (Math.random() > 0.5) States.Power.ON else States.Power.OFF)
                }
            } catch (e: Exception) {
                LOGGER.log(Level.SEVERE, "Error in powerStateThread1", e)
            }
        })

        // Thread 2: Randomly toggle PowerState ON/OFF
        val powerStateThread2 = Thread(Runnable {
            try {
                for (i in 0..numberOfInterations) {
                    lightRemote!!.setPowerState(if (Math.random() > 0.5) States.Power.ON else States.Power.OFF).get()
                }
            } catch (e: Exception) {
                LOGGER.log(Level.SEVERE, "Error in powerStateThread2", e)
            }
        })

        // Thread 3: Modify the label of the unit
        val unitConfigThread1 = Thread(Runnable {
            try {
                for (i in 0..numberOfInterations) {
                    val newLabel = LabelProcessor.buildLabel("Label$i")
                    Registries.getUnitRegistry().getUnitConfigById(lightRemote!!.getId())
                        .toBuilder()
                        .apply { setLabel(newLabel) }
                        .build()
                        .also { Registries.getUnitRegistry().updateUnitConfig(it) }
                }
            } catch (e: Exception) {
                LOGGER.log(Level.SEVERE, "Error in unitConfigThread1", e)
            }
        })

        // Thread 4: Modify the location of the unit
        val unitConfigThread2 = Thread(Runnable {
            try {
                for (i in 0..numberOfInterations) {
                    val newLocationId = Registries.getUnitRegistry().getUnitConfigsByUnitType(UnitTemplate.UnitType.LOCATION)
                        .shuffled()
                        .first()
                        .id

                    Registries.getUnitRegistry().getUnitConfigById(lightRemote!!.getId())
                        .toBuilder()
                        .apply { placementConfigBuilder.setLocationId(newLocationId) }
                        .build()
                        .also { Registries.getUnitRegistry().updateUnitConfig(it) }
                }
            } catch (e: Exception) {
                LOGGER.log(Level.SEVERE, "Error in unitConfigThread2", e)
            }
        })

        // Start all threads
        powerStateThread1.start()
        powerStateThread2.start()
        unitConfigThread1.start()
        unitConfigThread2.start()

        // Wait for all threads to complete
        powerStateThread1.join()
        powerStateThread2.join()
        unitConfigThread1.join()
        unitConfigThread2.join()

        println("Race condition test completed.")
    }

    companion object {
        private val LOGGER: Logger = Logger.getLogger(HighFrequentModificationTest::class.java.getName())
    }
}
