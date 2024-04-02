package org.openbase.bco.app.preset

import org.openbase.bco.dal.control.layer.unit.app.AbstractAppController
import org.openbase.bco.dal.lib.layer.unit.Unit
import org.openbase.bco.dal.remote.action.RemoteAction
import org.openbase.bco.dal.remote.layer.unit.Units
import org.openbase.bco.dal.remote.layer.unit.location.LocationRemote
import org.openbase.bco.registry.remote.Registries
import org.openbase.jul.exception.CouldNotPerformException
import org.openbase.jul.exception.InstantiationException
import org.openbase.jul.exception.NotAvailableException
import org.openbase.jul.exception.ShutdownInProgressException
import org.openbase.jul.exception.StackTracePrinter.printStackTrace
import org.openbase.jul.exception.printer.ExceptionPrinter
import org.openbase.jul.iface.Activatable
import org.openbase.jul.pattern.Observer
import org.openbase.jul.pattern.provider.DataProvider
import org.openbase.jul.schedule.SyncObject
import org.openbase.jul.schedule.Timeout
import org.openbase.type.domotic.action.ActionDescriptionType
import org.openbase.type.domotic.service.ServiceTemplateType
import org.openbase.type.domotic.state.ActivationStateType
import org.openbase.type.domotic.state.PowerStateType
import org.openbase.type.domotic.state.PresenceStateType
import org.openbase.type.domotic.unit.UnitConfigType
import org.openbase.type.domotic.unit.UnitTemplateType
import org.openbase.type.domotic.unit.location.LocationConfigType
import org.openbase.type.domotic.unit.location.LocationDataType
import org.openbase.type.vision.HSBColorType
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.concurrent.TimeUnit

/**
 * UnitConfig
 */
class NightLightApp : AbstractAppController() {
    private val locationMapLock = SyncObject("LocationMapLock")

    private val presentsActionLocationMap: MutableMap<Unit<*>?, RemoteAction>
    private val absenceActionLocationMap: MutableMap<Unit<*>?, RemoteAction>
    private val locationMap: MutableMap<LocationRemote, TimedObserver>

    /**
     * @param location    the location to process
     * @param eventSource the source which has triggered the update: location / childlocation / timeout / init (null)
     * @param timeout     the timeout of the location
     */
    private fun update(location: LocationRemote?, eventSource: Any?, timeout: Timeout?) {
        try {
            // skip update when not active

            if (activationState.value != ActivationStateType.ActivationState.State.ACTIVE) {
                if (!executing) {
                    logger.warn("app inactive but still executing! Force stopp...")
                    try {
                        stop(activationState)
                    } catch (e: InterruptedException) {
                        Thread.currentThread().interrupt()
                        return
                    }
                }
                logger.error("Update triggered even when not active!")
                printStackTrace(logger)
                return
            }

            // init present state with main location.
            var presentState = location!!.presenceState.value
            for (neighbor in location.getNeighborLocationList(true)) {
                // break if any present person is detected.

                if (presentState == PresenceStateType.PresenceState.State.PRESENT) {
                    break
                }

                // if not unknown apply state of neighbor
                if (neighbor.presenceState.value != PresenceStateType.PresenceState.State.UNKNOWN) {
                    presentState = neighbor.presenceState.value
                }
            }

            val absenceAction = absenceActionLocationMap[location]
            val presentsAction = presentsActionLocationMap[location]

            when (presentState) {
                PresenceStateType.PresenceState.State.PRESENT -> {
                    if (eventSource === location) {
                        timeout!!.restart(10, TimeUnit.MINUTES)
                    } else {
                        timeout!!.restart(2, TimeUnit.MINUTES)
                    }

                    // if ongoing action skip the update.
                    if (presentsAction != null && !presentsAction.isDone) {
                        return
                    }

                    // System.out.println("Nightmode: switch location " + location.getLabel() + " to orange because of present state].");
                    val availableServiceTypes = location.availableServiceTypes
                    if (availableServiceTypes.contains(ServiceTemplateType.ServiceTemplate.ServiceType.COLOR_STATE_SERVICE)) {
                        presentsActionLocationMap[location] =
                            observe(location.setColor(COLOR_ORANGE, defaultActionParameter))
                    } else if (availableServiceTypes.contains(ServiceTemplateType.ServiceTemplate.ServiceType.BRIGHTNESS_STATE_SERVICE)) {
                        presentsActionLocationMap[location] =
                            observe(location.setBrightness(COLOR_ORANGE.brightness, defaultActionParameter))
                    } else {
                        // location not supported for nightlight
                    }
                }

                PresenceStateType.PresenceState.State.ABSENT -> {
                    // if ongoing action skip the update.
                    if (absenceAction != null && !absenceAction.isDone) {
                        return
                    }

                    if (timeout!!.isExpired || !timeout.isActive) {
                        // System.out.println("Nightmode: switch off " + location.getLabel() + " because of absent state.");
                        absenceActionLocationMap[location] = observe(
                            location.setPowerState(
                                PowerStateType.PowerState.State.OFF,
                                UnitTemplateType.UnitTemplate.UnitType.LIGHT,
                                defaultActionParameter
                            )
                        )

                        // cancel presents actions
                        if (presentsAction != null) {
                            presentsAction.cancel()
                            presentsActionLocationMap.remove(location)
                        }
                    }
                }

                PresenceStateType.PresenceState.State.UNKNOWN -> TODO()
            }
        } catch (ex: ShutdownInProgressException) {
            // skip update when shutdown was initiated.
        } catch (ex: CouldNotPerformException) {
            ExceptionPrinter.printHistory(
                "Could not control light in " + location!!.getLabel("?") + " by night light!",
                ex,
                LOGGER
            )
        }
    }

    @Throws(CouldNotPerformException::class, InterruptedException::class)
    override fun applyConfigUpdate(config: UnitConfigType.UnitConfig): UnitConfigType.UnitConfig {
        var config: UnitConfigType.UnitConfig? = config
        getManageWriteLockInterruptible(this).use { ignored ->
            config = super.applyConfigUpdate(config!!)
            updateLocationMap()
            return config!!
        }
    }

    @Throws(CouldNotPerformException::class)
    private fun updateLocationMap() {
        try {
            synchronized(locationMapLock) {
                // deregister all tile remotes
                locationMap.forEach { (remote: LocationRemote?, observer: TimedObserver) ->
                    observer.deactivate()
                }

                // clear all tile remotes
                locationMap.clear()

                val excludedLocations: MutableCollection<String> = ArrayList()

                // check location exclusion
                try {
                    excludedLocations.addAll(generateVariablePool().getValues(META_CONFIG_KEY_EXCLUDE_LOCATION).values)
                } catch (ex: NotAvailableException) {
                    ExceptionPrinter.printHistory("Could not load variable pool!", ex, LOGGER)
                    // no locations excluded.
                }

                // load parent location
                val parentLocationConfig =
                    Registries.getUnitRegistry().getUnitConfigById(config.placementConfig.locationId)

                // load tile remotes
                remoteLocationLoop@ for (childLocationId in parentLocationConfig.locationConfig.childIdList) {
                    val locationUnitConfig = Registries.getUnitRegistry().getUnitConfigById(childLocationId)

                    // let only tiles pass
                    if (locationUnitConfig.locationConfig.locationType != LocationConfigType.LocationConfig.LocationType.TILE) {
                        continue
                    }

                    // check if location was excluded by id
                    if (excludedLocations.contains(locationUnitConfig.id)) {
                        // System.out.println("exclude locations: " + locationUnitConfig.getLabel());
                        continue@remoteLocationLoop
                    }

                    // check if location was excluded by alias
                    for (alias in locationUnitConfig.aliasList) {
                        if (excludedLocations.contains(alias)) {
                            // System.out.println("exclude locations: " + locationUnitConfig.getLabel());
                            continue@remoteLocationLoop
                        }
                    }

                    val remote = Units.getUnit(locationUnitConfig, false, Units.LOCATION)

                    // skip locations without colorable lights.
                    if (!remote.isServiceAvailable(ServiceTemplateType.ServiceTemplate.ServiceType.COLOR_STATE_SERVICE)) {
                        continue@remoteLocationLoop
                    }

                    locationMap[remote] = TimedObserver(remote)
                }
                if (activationState.value == ActivationStateType.ActivationState.State.ACTIVE) {
                    locationMap.forEach { (remote: LocationRemote?, observer: TimedObserver) ->
                        observer.activate()
                    }
                }
            }
        } catch (ex: InterruptedException) {
            Thread.currentThread().interrupt()
        } catch (ex: CouldNotPerformException) {
            throw CouldNotPerformException("Could not update location map", ex)
        }
    }

    override fun shutdown() {
        super.shutdown()
        synchronized(locationMapLock) {
            locationMap.clear()
        }
    }

    var executing: Boolean = false

    init {
        this.locationMap = HashMap()
        this.presentsActionLocationMap = HashMap()
        this.absenceActionLocationMap = HashMap()
    }

    @Throws(CouldNotPerformException::class, InterruptedException::class)
    override fun execute(activationState: ActivationStateType.ActivationState): ActionDescriptionType.ActionDescription? {
        executing = true
        synchronized(locationMapLock) {
            locationMap.forEach { (remote: LocationRemote?, observer: TimedObserver) ->
                observer.activate()
            }
        }
        return activationState.responsibleAction
    }

    @Throws(InterruptedException::class, CouldNotPerformException::class)
    override fun stop(activationState: ActivationStateType.ActivationState) {
        executing = false
        synchronized(locationMapLock) {
            // remove observer
            locationMap.forEach { (remote: LocationRemote?, observer: TimedObserver) ->
                observer.deactivate()
            }

            // clear actions list
            presentsActionLocationMap.clear()
            absenceActionLocationMap.clear()
        }
        super.stop(activationState)
    }

    internal inner class TimedObserver(remote: LocationRemote) : Activatable {
        var remote: LocationRemote? = null
        var timeout: Timeout? = null
        var internalObserver: Observer<DataProvider<LocationDataType.LocationData?>, LocationDataType.LocationData>? =
            null
        var neighborLocationRemoteList: List<LocationRemote>? = null

        var active: Boolean = false

        init {
            try {
                this.remote = remote
                this.timeout = object : Timeout(1, TimeUnit.MINUTES) {
                    override fun expired() {
                        // skip update when not active
                        if (!active) {
                            logger.error("Update triggered even when not active!")
                            printStackTrace(logger)
                            return
                        }
                        this@NightLightApp.update(remote, this, this)
                    }
                }
                this.neighborLocationRemoteList = remote.getNeighborLocationList(false)
                this.internalObserver =
                    Observer { source: DataProvider<LocationDataType.LocationData?>?, data: LocationDataType.LocationData? ->
                        // skip update when not active
                        if (!active) {
                            logger.error("Update triggered even when not active!")
                            printStackTrace(logger)
                            return@Observer
                        }
                        this@NightLightApp.update(remote, source, timeout)
                    }
            } catch (ex: CouldNotPerformException) {
                throw InstantiationException(this, ex)
            }
        }

        @Synchronized
        override fun activate() {
            active = true

            // register observer
            remote!!.addDataObserver(internalObserver)
            for (neighbor in neighborLocationRemoteList!!) {
                neighbor.addDataObserver(internalObserver)
            }

            this@NightLightApp.update(remote, null, timeout)
        }

        @Synchronized
        override fun deactivate() {
            active = false
            timeout!!.cancel()

            // deregister observer
            remote!!.removeDataObserver(internalObserver)
            for (neighbor in neighborLocationRemoteList!!) {
                neighbor.removeDataObserver(internalObserver)
            }
        }

        override fun isActive(): Boolean {
            return active
        }
    }

    companion object {
        val COLOR_ORANGE: HSBColorType.HSBColor =
            HSBColorType.HSBColor.newBuilder().setHue(15.0).setSaturation(1.0).setBrightness(0.10).build()
        private val LOGGER: Logger = LoggerFactory.getLogger(NightLightApp::class.java)
        private const val META_CONFIG_KEY_EXCLUDE_LOCATION = "EXCLUDE_LOCATION"
    }
}
