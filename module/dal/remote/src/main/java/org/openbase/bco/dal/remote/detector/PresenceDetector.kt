package org.openbase.bco.dal.remote.detector

import com.google.protobuf.Message
import org.openbase.bco.dal.lib.layer.service.ServiceStateProvider
import org.openbase.bco.dal.lib.layer.unit.UnitRemote
import org.openbase.bco.dal.lib.layer.unit.location.Location
import org.openbase.bco.dal.remote.layer.unit.CustomUnitPool
import org.openbase.jps.core.JPService
import org.openbase.jul.exception.*
import org.openbase.jul.exception.printer.ExceptionPrinter
import org.openbase.jul.exception.printer.LogLevel
import org.openbase.jul.extension.type.processing.TimestampProcessor
import org.openbase.jul.iface.Manageable
import org.openbase.jul.pattern.Filter
import org.openbase.jul.pattern.ObservableImpl
import org.openbase.jul.pattern.Observer
import org.openbase.jul.pattern.provider.DataProvider
import org.openbase.jul.schedule.GlobalCachedExecutorService
import org.openbase.jul.schedule.Timeout
import org.openbase.type.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType
import org.openbase.type.domotic.state.*
import org.openbase.type.domotic.state.PresenceStateType.PresenceState
import org.openbase.type.domotic.state.PresenceStateType.PresenceStateOrBuilder
import org.openbase.type.domotic.state.WindowStateType.WindowState
import org.openbase.type.domotic.unit.UnitConfigType.UnitConfig
import org.openbase.type.domotic.unit.UnitTemplateType.UnitTemplate.UnitType
import org.openbase.type.domotic.unit.connection.ConnectionConfigType
import org.openbase.type.domotic.unit.location.LocationConfigType.LocationConfig.LocationType
import org.openbase.type.domotic.unit.location.LocationDataType.LocationData
import org.openbase.type.domotic.unit.location.TileConfigType
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit
import kotlin.math.max

/**
 * @author [Divine Threepwood](mailto:divine@openbase.org)
 */
class PresenceDetector : Manageable<Location>, DataProvider<PresenceState> {
    private val logger: Logger = LoggerFactory.getLogger(javaClass)

    private val presenceStateBuilder: PresenceState.Builder = PresenceState.newBuilder()
    private var presenceTimeout: Timeout? = null
    private val locationDataObserver: Observer<DataProvider<LocationData>, LocationData>
    private var location: Location? = null
    private val presenceStateObservable = ObservableImpl<DataProvider<PresenceState>, PresenceState>(this)
    private val buttonUnitPool: CustomUnitPool<Message, UnitRemote<Message>>
    private val connectionUnitPool: CustomUnitPool<Message, UnitRemote<Message>>

    private var active = false
    private var isShutdownInitiated: Boolean = false
        private set

    init {
        this.presenceTimeout = object : Timeout(PRESENCE_TIMEOUT) {
            override fun expired() {

                if (location == null) {
                    return
                }

                try {
                    // if motion is still detected just restart the timeout.
                    if (location!!.data.motionState.value == MotionStateType.MotionState.State.MOTION) {
                        GlobalCachedExecutorService.submit {
                            try {
                                presenceTimeout?.restart()
                            } catch (ex: CouldNotPerformException) {
                                ExceptionPrinter.printHistory("Could not setup presence timeout!", ex, logger)
                            }
                        }
                        return
                    }
                    updatePresenceState(
                        PresenceState.newBuilder()
                            .setValue(PresenceState.State.ABSENT)
                    )
                } catch (ex: ShutdownInProgressException) {
                    // skip update on shutdown
                } catch (ex: CouldNotPerformException) {
                    ExceptionPrinter.printHistory(
                        CouldNotPerformException("Could not notify absent by timer!", ex),
                        logger
                    )
                }
            }
        }

        locationDataObserver = Observer { _: DataProvider<LocationData>, data: LocationData ->
            updateMotionState(data.motionState)
        }

        this.buttonUnitPool = CustomUnitPool()
        this.connectionUnitPool = CustomUnitPool()

        buttonUnitPool.addServiceStateObserver { _: ServiceStateProvider<Message>, data: Message ->
            try {
                this@PresenceDetector.updateButtonState(data as ButtonStateType.ButtonState)
            } catch (ex: ClassCastException) {
                ExceptionPrinter.printHistory("ButtonPool entail incompatible units!", ex, logger)
            }
        }

        connectionUnitPool.addServiceStateObserver { source: ServiceStateProvider<Message>, data: Message ->
            when (source.serviceType) {
                ServiceType.WINDOW_STATE_SERVICE -> updateWindowState(data as WindowState)
                ServiceType.DOOR_STATE_SERVICE -> updateDoorState(data as DoorStateType.DoorState)
                ServiceType.PASSAGE_STATE_SERVICE -> {}
                else -> logger.warn("Invalid connection service update received: " + source.serviceType.name + " from " + source + " pool:" + connectionUnitPool.isActive())
            }
        }
    }

    @Throws(InitializationException::class, InterruptedException::class)
    override fun init(location: Location) {
        try {
            this.location = location
            buttonUnitPool.init(
                Filter<UnitConfig> { unitConfig: UnitConfig -> unitConfig.unitType == UnitType.BUTTON },
                Filter<UnitConfig> { unitConfig: UnitConfig ->
                    try {
                        unitConfig.placementConfig.locationId == location.id
                    } catch (ex: NotAvailableException) {
                        ExceptionPrinter.printHistory(
                            "Could not resolve location id within button filter operation.",
                            ex,
                            logger
                        )
                        true
                    }
                })


            if ((location.config.locationConfig.locationType == LocationType.TILE)) {
                connectionUnitPool.init(
                    Filter<UnitConfig> { unitConfig: UnitConfig -> unitConfig.unitType == UnitType.CONNECTION },
                    Filter<UnitConfig> { unitConfig: UnitConfig ->
                        try {
                            unitConfig.connectionConfig.tileIdList.contains(location.id)
                        } catch (ex: NotAvailableException) {
                            ExceptionPrinter.printHistory(
                                "Could not resolve location id within connection filter operation.",
                                ex,
                                logger
                            )
                            false
                        }
                    },
                    Filter<UnitConfig> { unitConfig: UnitConfig ->
                        try {
                            location.config.locationConfig.tileConfig.tileType != TileConfigType.TileConfig.TileType.OUTDOOR || unitConfig.connectionConfig.connectionType != ConnectionConfigType.ConnectionConfig.ConnectionType.WINDOW
                        } catch (ex: NotAvailableException) {
                            ExceptionPrinter.printHistory(
                                "Could not resolve location id within connection filter operation.",
                                ex,
                                logger
                            )
                            false
                        }
                    })
            }
        } catch (ex: CouldNotPerformException) {
            throw InitializationException(this, ex)
        }
    }

    @Throws(InitializationException::class, InterruptedException::class)
    fun init(location: Location, motionTimeout: Long) {
        init(location)
        presenceTimeout?.defaultWaitTime = motionTimeout
    }

    @Throws(CouldNotPerformException::class, InterruptedException::class)
    override fun activate() {
        active = true
        location!!.addDataObserver(locationDataObserver)

        buttonUnitPool.activate()

        if ((location!!.config.locationConfig.locationType == LocationType.TILE)) {
            connectionUnitPool.activate()
        }

        // start initial timeout
        presenceTimeout?.start()
        updateMotionState(location!!.data.motionState)
    }


    @Throws(CouldNotPerformException::class, InterruptedException::class)
    override fun deactivate() {
        active = false
        presenceTimeout?.cancel()
        if (location != null) {
            // can be null if never initialized or initialization failed
            location!!.removeDataObserver(locationDataObserver)
        }
        buttonUnitPool.deactivate()
        if ((location!!.config.locationConfig.locationType == LocationType.TILE)) {
            connectionUnitPool.deactivate()
        }
    }

    override fun isActive(): Boolean {
        return active
    }

    @Throws(InvalidStateException::class)
    override fun validateData() {
        if (isShutdownInitiated) {
            throw InvalidStateException(ShutdownInProgressException(this))
        }

        if (isDataAvailable) {
            throw InvalidStateException(NotAvailableException("Data"))
        }
    }

    override fun shutdown() {
        try {
            isShutdownInitiated = true
            deactivate()
        } catch (ex: CouldNotPerformException) {
            ExceptionPrinter.printHistory<Exception>(ex, logger)
        } catch (ex: InterruptedException) {
            ExceptionPrinter.printHistory<Exception>(ex, logger)
        }
        buttonUnitPool.shutdown()
        connectionUnitPool.shutdown()
    }

    @Synchronized
    @Throws(CouldNotPerformException::class)
    private fun updatePresenceState(presenceState: PresenceStateOrBuilder) {
        // update timestamp and reset timer

        if (presenceState.value == PresenceState.State.PRESENT && presenceStateBuilder.timestamp.time != presenceState.timestamp.time) {
            presenceTimeout?.restart()
            presenceStateBuilder.timestampBuilder.setTime(
                max(presenceStateBuilder.timestamp.time.toDouble(), presenceState.timestamp.time.toDouble())
                    .toLong()
            )
        }

        // filter non state changes
        if (presenceStateBuilder.value == presenceState.value) {
            return
        }

        // update value
        TimestampProcessor.updateTimestampWithCurrentTime(presenceStateBuilder, logger)
        presenceStateBuilder.setValue(presenceState.value)

        // notify
        try {
            presenceStateObservable.notifyObservers(presenceStateBuilder.build())
        } catch (ex: CouldNotPerformException) {
            ExceptionPrinter.printHistory(
                CouldNotPerformException("Could not update MotionState!", ex),
                logger,
                LogLevel.ERROR
            )
        }
    }

    @Throws(CouldNotPerformException::class)
    private fun updateMotionState(motionState: MotionStateType.MotionStateOrBuilder) {
        // Filter rush motion predictions.

        if (motionState.value == MotionStateType.MotionState.State.NO_MOTION) {
            return
        }

        if (motionState.value == MotionStateType.MotionState.State.MOTION) {
            updatePresenceState(
                TimestampProcessor.updateTimestampWithCurrentTime(
                    PresenceState.newBuilder().setValue(
                        PresenceState.State.PRESENT
                    ).setResponsibleAction(motionState.responsibleAction).build()
                )
            )
        }
    }

    @Throws(CouldNotPerformException::class)
    private fun updateButtonState(buttonState: ButtonStateType.ButtonStateOrBuilder) {
        when (buttonState.value) {
            ButtonStateType.ButtonState.State.PRESSED, ButtonStateType.ButtonState.State.RELEASED, ButtonStateType.ButtonState.State.DOUBLE_PRESSED, ButtonStateType.ButtonState.State.UNKNOWN ->                 // ignore non presence prove
                return

            else ->
                return
        }
    }

    @Throws(CouldNotPerformException::class)
    private fun updateDoorState(doorState: DoorStateType.DoorState) {
        when (doorState.value) {
            DoorStateType.DoorState.State.OPEN -> {
                updatePresenceState(
                    TimestampProcessor.updateTimestampWithCurrentTime(
                        PresenceState.newBuilder().setValue(
                            PresenceState.State.PRESENT
                        ).setResponsibleAction(doorState.responsibleAction).build()
                    )
                )
                // ignore non presence prove
                return
            }

            DoorStateType.DoorState.State.CLOSED, DoorStateType.DoorState.State.UNKNOWN ->
                return

            else ->
                return
        }
    }

    @Throws(CouldNotPerformException::class)
    private fun updateWindowState(windowState: WindowState) {
        when (windowState.value) {
            WindowState.State.OPEN, WindowState.State.TILTED, WindowState.State.CLOSED -> {
                updatePresenceState(
                    TimestampProcessor.updateTimestampWithCurrentTime(
                        PresenceState.newBuilder().setValue(
                            PresenceState.State.PRESENT
                        ).setResponsibleAction(windowState.responsibleAction).build()
                    )
                )
                // ignore non presence prove
                return
            }

            WindowState.State.UNKNOWN ->
                return

            else ->
                return
        }
    }

    override fun isDataAvailable(): Boolean {
        return presenceStateObservable.isValueAvailable
    }

    override fun getDataClass(): Class<PresenceState> {
        return PresenceState::class.java
    }

    @Throws(NotAvailableException::class)
    override fun getData(): PresenceState {
        return presenceStateObservable.value
    }

    override fun getDataFuture(): Future<PresenceState> {
        return presenceStateObservable.valueFuture
    }

    override fun addDataObserver(observer: Observer<DataProvider<PresenceState>, PresenceState>) {
        presenceStateObservable.addObserver(observer)
    }

    override fun removeDataObserver(observer: Observer<DataProvider<PresenceState>, PresenceState>) {
        presenceStateObservable.removeObserver(observer)
    }

    @Throws(CouldNotPerformException::class, InterruptedException::class)
    override fun waitForData() {
        presenceStateObservable.waitForValue()
    }

    @Throws(CouldNotPerformException::class, InterruptedException::class)
    override fun waitForData(timeout: Long, timeUnit: TimeUnit) {
        presenceStateObservable.waitForValue(timeout, timeUnit)
    }

    companion object {
        /**
         * Default 3 minute window of no movement unit the state switches to
         * NO_MOTION.
         */
        val PRESENCE_TIMEOUT: Long = (if (JPService.testMode()) 50 else 60000).toLong()
    }
}
