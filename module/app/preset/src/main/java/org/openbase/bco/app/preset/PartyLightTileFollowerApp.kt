package org.openbase.bco.app.preset

import org.openbase.bco.dal.control.layer.unit.app.AbstractAppController
import org.openbase.bco.dal.lib.layer.unit.Unit
import org.openbase.bco.dal.remote.action.RemoteAction
import org.openbase.bco.dal.remote.layer.unit.Units
import org.openbase.bco.dal.remote.layer.unit.location.LocationRemote
import org.openbase.bco.registry.remote.Registries
import org.openbase.jul.exception.CouldNotPerformException
import org.openbase.jul.exception.ExceptionProcessor.interruptOnShutdown
import org.openbase.jul.exception.ExceptionProcessor.isCausedBySystemShutdown
import org.openbase.jul.exception.InstantiationException
import org.openbase.jul.exception.printer.ExceptionPrinter
import org.openbase.jul.schedule.SyncObject
import org.openbase.type.domotic.action.ActionDescriptionType
import org.openbase.type.domotic.state.ActivationStateType
import org.openbase.type.domotic.unit.UnitTemplateType
import org.openbase.type.domotic.unit.location.LocationConfigType
import org.openbase.type.vision.HSBColorType
import java.util.concurrent.*

/**
 * UnitConfig
 */
class PartyLightTileFollowerApp : AbstractAppController() {
    private var actionLocationMap: MutableMap<Unit<*>?, RemoteAction>? = null
    private var locationRemoteMap: MutableMap<String, LocationRemote>? = null

    private val taskLock = SyncObject("TaskLock")

    override fun shutdown() {
        locationRemoteMap!!.clear()
        super.shutdown()
    }

    private val brightness = 0.50
    private val colors = arrayOf(
        HSBColorType.HSBColor.newBuilder().setHue(0.0).setSaturation(1.0).setBrightness(brightness).build(),
        HSBColorType.HSBColor.newBuilder().setHue(290.0).setSaturation(1.0).setBrightness(brightness).build(),
        HSBColorType.HSBColor.newBuilder().setHue(30.0).setSaturation(1.0).setBrightness(brightness).build(),
    )

    init {
        try {
            Registries.waitForData()
            this.actionLocationMap = HashMap()
            this.locationRemoteMap = HashMap()
        } catch (ex: CouldNotPerformException) {
            throw InstantiationException(this, ex)
        }
    }

    @Throws(CouldNotPerformException::class, InterruptedException::class)
    override fun execute(activationState: ActivationStateType.ActivationState): ActionDescriptionType.ActionDescription? {
        logger.debug("Execute PartyLightTileFollowerApp[$label]")

        // init tile remotes
        locationRemoteMap!!.clear()
        for (locationUnitConfig in Registries.getUnitRegistry(true)
            .getUnitConfigsByUnitType(UnitTemplateType.UnitTemplate.UnitType.LOCATION)) {
            if (locationUnitConfig.locationConfig.locationType != LocationConfigType.LocationConfig.LocationType.TILE) {
                continue
            }
            locationRemoteMap!![locationUnitConfig.id] = Units.getUnit(locationUnitConfig, false, Units.LOCATION)
        }

        TileFollower().call()
        return activationState.responsibleAction
    }

    @Throws(InterruptedException::class, CouldNotPerformException::class)
    override fun stop(activationState: ActivationStateType.ActivationState) {
        val cancelTaskList = ArrayList<Future<ActionDescriptionType.ActionDescription>>()
        for ((_, value) in actionLocationMap!!) {
            cancelTaskList.add(value.cancel())
        }

        for (cancelTask in cancelTaskList) {
            try {
                cancelTask[10, TimeUnit.SECONDS]
            } catch (ex: ExecutionException) {
                if (!isCausedBySystemShutdown(ex)) {
                    ExceptionPrinter.printHistory<Exception>("Could not cancel action!", ex, logger)
                }
            } catch (ex: TimeoutException) {
                if (!isCausedBySystemShutdown(ex)) {
                    ExceptionPrinter.printHistory<Exception>("Could not cancel action!", ex, logger)
                }
            }
        }
        super.stop(activationState)
    }

    inner class TileFollower : Callable<Void?> {
        private val processedLocations: MutableList<String> = ArrayList()

        @Throws(CouldNotPerformException::class, InterruptedException::class)
        override fun call(): Void? {
            if (locationRemoteMap!!.isEmpty()) {
                throw CouldNotPerformException("No Locations found!")
            }

            var locationRemote: LocationRemote?

            var colorIndex = 0
            while (!Thread.currentThread().isInterrupted) {
                Thread.yield()
                try {
                    // apply updates for next iteration
                    colorIndex = ++colorIndex % colors.size
                    processedLocations.clear()

                    // select initial room
                    locationRemote = locationRemoteMap!![config.placementConfig.locationId]

                    processRoom(locationRemote, colors[colorIndex])
                } catch (ex: CouldNotPerformException) {
                    ExceptionPrinter.printHistory(
                        CouldNotPerformException(
                            "Skip animation run!",
                            interruptOnShutdown(ex)
                        ), logger
                    )
                }
            }
            return null
        }

        @Throws(CouldNotPerformException::class, InterruptedException::class)
        private fun processRoom(locationRemote: LocationRemote?, color: HSBColorType.HSBColor) {
            logger.debug("Set $locationRemote to $color...")
            try {
                // skip if no colorable light is present

                if (Registries.getUnitRegistry().getUnitConfigsByLocationIdAndUnitType(
                        locationRemote!!.id,
                        UnitTemplateType.UnitTemplate.UnitType.COLORABLE_LIGHT
                    ).isNotEmpty()
                ) {
                    if (locationRemote.isConnected && locationRemote.isDataAvailable) {
                        val remoteAction = observe(locationRemote.setColor(color, defaultActionParameter))
                        actionLocationMap!![locationRemote] = remoteAction
                        remoteAction.waitForRegistration()
                    }
                    Thread.sleep(1000)
                }

                // mark as processed
                processedLocations.add(locationRemote.id)

                // process neighbors
                var neighborRemote: LocationRemote?
                for (neighborConfig in Registries.getUnitRegistry().getNeighborLocationsByLocationId(
                    locationRemote.id
                )) {
                    // skip if already processed
                    if (processedLocations.contains(neighborConfig.id)) {
                        continue
                    }

                    neighborRemote = locationRemoteMap!![neighborConfig.id]

                    // process remote 
                    processRoom(neighborRemote, color)
                }
            } catch (ex: CouldNotPerformException) {
                throw CouldNotPerformException("Could not process room of $locationRemote", ex)
            }
        }
    }
}
