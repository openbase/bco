package org.openbase.bco.device.openhab.communication

import com.google.gson.*
import jakarta.ws.rs.ProcessingException
import jakarta.ws.rs.client.Client
import jakarta.ws.rs.client.ClientBuilder
import jakarta.ws.rs.client.Entity
import jakarta.ws.rs.client.WebTarget
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import jakarta.ws.rs.sse.InboundSseEvent
import jakarta.ws.rs.sse.SseEventSource
import org.glassfish.jersey.client.oauth2.OAuth2ClientSupport
import org.openbase.bco.device.openhab.jp.JPOpenHABURI
import org.openbase.bco.registry.remote.Registries
import org.openbase.jps.core.JPService
import org.openbase.jps.exception.JPNotAvailableException
import org.openbase.jul.exception.*
import org.openbase.jul.exception.ExceptionProcessor.setInitialCause
import org.openbase.jul.exception.printer.ExceptionPrinter
import org.openbase.jul.exception.printer.LogLevel
import org.openbase.jul.extension.type.processing.LabelProcessor.contains
import org.openbase.jul.extension.type.processing.MetaConfigProcessor
import org.openbase.jul.iface.Shutdownable
import org.openbase.jul.pattern.ObservableImpl
import org.openbase.jul.pattern.Observer
import org.openbase.jul.schedule.GlobalScheduledExecutorService
import org.openbase.jul.schedule.SyncObject
import org.openbase.type.domotic.state.ConnectionStateType
import org.openbase.type.domotic.unit.UnitTemplateType
import org.openbase.type.domotic.unit.gateway.GatewayClassType
import org.openhab.core.types.CommandDescription
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import java.util.function.Consumer

abstract class OpenHABRestConnection : Shutdownable {
    private val topicObservableMapLock = SyncObject("topicObservableMapLock")
    private val connectionStateSyncLock = SyncObject("connectionStateSyncLock")
    private var topicObservableMap: MutableMap<String, ObservableImpl<Any?, JsonObject?>>? = null

    private var restClient: Client? = null
    private var restTarget: WebTarget? = null
    private var sseSource: SseEventSource? = null

    var isShutdownInitiated: Boolean = false
        private set

    @JvmField
    protected var gson: Gson? = null

    private var connectionTask: ScheduledFuture<*>? = null

    var openhabConnectionState: ConnectionStateType.ConnectionState.State =
        ConnectionStateType.ConnectionState.State.DISCONNECTED
        protected set

    private val isTargetReachable: Boolean
        get() {
            try {
                testConnection()
            } catch (e: CouldNotPerformException) {
                if (e.cause is ProcessingException) {
                    return false
                }
            }
            return true
        }

    @Throws(CouldNotPerformException::class)
    protected abstract fun testConnection()

    @Throws(InterruptedException::class)
    fun waitForConnectionState(
        connectionState: ConnectionStateType.ConnectionState.State,
        timeout: Long,
        timeUnit: TimeUnit,
    ) {
        synchronized(connectionStateSyncLock) {
            while (openhabConnectionState != connectionState) {
                (connectionStateSyncLock as Object).wait(timeUnit.toMillis(timeout))
            }
        }
    }

    @Throws(InterruptedException::class)
    fun waitForConnectionState(connectionState: ConnectionStateType.ConnectionState.State) {
        synchronized(connectionStateSyncLock) {
            while (openhabConnectionState != connectionState) {
                (connectionStateSyncLock as Object).wait()
            }
        }
    }

    private fun setConnectState(connectState: ConnectionStateType.ConnectionState.State) {
        synchronized(connectionStateSyncLock) {
            // filter non changing states
            if (connectState == this.openhabConnectionState) {
                return
            }
            LOGGER.trace("Openhab Connection State changed to: $connectState")

            // update state
            this.openhabConnectionState = connectState

            when (connectState) {
                ConnectionStateType.ConnectionState.State.CONNECTING -> {
                    LOGGER.info("Wait for openHAB...")
                    try {
                        connectionTask!!.cancel(true)
                        connectionTask = GlobalScheduledExecutorService.scheduleWithFixedDelay({
                            if (isTargetReachable) {
                                // set connected
                                setConnectState(ConnectionStateType.ConnectionState.State.CONNECTED)

                                // cleanup own task
                                connectionTask!!.cancel(false)
                            }
                        }, 0, 15, TimeUnit.SECONDS)
                    } catch (ex: NotAvailableException) {
                        // if global executor service is not available we have no chance to connect.
                        LOGGER.warn("Wait for openHAB...", ex)
                        setConnectState(ConnectionStateType.ConnectionState.State.DISCONNECTED)
                    } catch (ex: RejectedExecutionException) {
                        LOGGER.warn("Wait for openHAB...", ex)
                        setConnectState(ConnectionStateType.ConnectionState.State.DISCONNECTED)
                    }
                }

                ConnectionStateType.ConnectionState.State.CONNECTED -> {
                    LOGGER.info("Connection to OpenHAB established.")
                    initSSE()
                }

                ConnectionStateType.ConnectionState.State.RECONNECTING -> {
                    LOGGER.warn("Connection to OpenHAB lost!")
                    resetConnection()
                    setConnectState(ConnectionStateType.ConnectionState.State.CONNECTING)
                }

                ConnectionStateType.ConnectionState.State.DISCONNECTED -> {
                    LOGGER.info("Connection to OpenHAB closed.")
                    resetConnection()
                }

                else -> {
                    LOGGER.warn("Unknown connection state: $connectState")
                }
            }
            // notify state change
            (connectionStateSyncLock as Object).notifyAll()
            if (connectState == ConnectionStateType.ConnectionState.State.RECONNECTING) {
                setConnectState(ConnectionStateType.ConnectionState.State.CONNECTING)
            }
        }
    }

    private fun initSSE() {
        // activate sse source if not already done
        if (sseSource != null) {
            LOGGER.warn("SSE already initialized!")
            return
        }

        val webTarget = restTarget!!.path(EVENTS_TARGET)
        sseSource = SseEventSource
            .target(webTarget)
            .reconnectingEvery(15, TimeUnit.SECONDS)
            .build()
            .also { it.open() }

        val evenConsumer = Consumer { inboundSseEvent: InboundSseEvent ->
            // dispatch event
            try {
                val payload = JsonParser.parseString(inboundSseEvent.readData()).asJsonObject
                for ((key, value) in topicObservableMap!!) {
                    try {
                        if (payload[TOPIC_KEY].asString.matches(key.toRegex())) {
                            value.notifyObservers(payload)
                        }
                    } catch (ex: Exception) {
                        ExceptionPrinter.printHistory(
                            CouldNotPerformException(
                                "Could not notify listeners on topic[$key]",
                                ex
                            ), LOGGER
                        )
                    }
                }
            } catch (ex: Exception) {
                ExceptionPrinter.printHistory(CouldNotPerformException("Could not handle SSE payload!", ex), LOGGER)
            }
        }

        val errorHandler = Consumer { ex: Throwable ->
            ExceptionPrinter.printHistory("Openhab connection error detected!", ex, LOGGER, LogLevel.DEBUG)
            checkConnectionState()
        }

        val reconnectHandler = Runnable {
            setConnectState(ConnectionStateType.ConnectionState.State.RECONNECTING)
        }

        sseSource?.register(evenConsumer, errorHandler, reconnectHandler)
    }

    fun checkConnectionState() {
        synchronized(connectionStateSyncLock) {
            // only validate if connected
            if (!isConnected) {
                return
            }

            // if not reachable init a reconnect
            if (!isTargetReachable) {
                setConnectState(ConnectionStateType.ConnectionState.State.RECONNECTING)
            }
        }
    }

    val isConnected: Boolean
        get() = openhabConnectionState == ConnectionStateType.ConnectionState.State.CONNECTED

    @JvmOverloads
    fun addSSEObserver(observer: Observer<Any?, JsonObject?>?, topicRegex: String = "") {
        synchronized(topicObservableMapLock) {
            if (topicObservableMap!!.containsKey(topicRegex)) {
                topicObservableMap!![topicRegex]!!.addObserver(observer)
                return
            }
            val observable = ObservableImpl<Any?, JsonObject?>(this)
            observable.addObserver(observer)
            topicObservableMap!!.put(topicRegex, observable)
        }
    }

    @JvmOverloads
    fun removeSSEObserver(observer: Observer<Any?, JsonObject?>?, topicFilter: String = "") {
        synchronized(topicObservableMapLock) {
            if (topicObservableMap!!.containsKey(topicFilter)) {
                topicObservableMap!![topicFilter]!!.removeObserver(observer)
            }
        }
    }

    private fun resetConnection() {
        // cancel ongoing connection task
        if (!connectionTask!!.isDone) {
            connectionTask!!.cancel(false)
        }

        // close sse
        if (sseSource != null) {
            sseSource!!.close()
            sseSource = null
        }
    }

    @Throws(CouldNotPerformException::class)
    fun validateConnection() {
        if (!isConnected) {
            throw InvalidStateException("Openhab not reachable yet!")
        }
    }

    @Throws(CouldNotPerformException::class, ProcessingException::class)
    private fun validateResponse(response: Response, skipConnectionValidation: Boolean = false): String {
        val result = response.readEntity(String::class.java)

        if (response.status == 200 || response.status == 201 || response.status == 202) {
            return result
        } else if (response.status == 404) {
            if (!skipConnectionValidation) {
                checkConnectionState()
            }
            throw NotAvailableException("URL")
        } else if (response.status == 503) {
            if (!skipConnectionValidation) {
                checkConnectionState()
            }
            // throw a processing exception to indicate that openHAB is still not fully started, this is used to wait for openHAB
            throw ProcessingException("OpenHAB server not ready")
        } else {
            throw CouldNotPerformException("Response returned with ErrorCode[" + response.status + "], Result[" + result + "] and ErrorMessage[" + response.statusInfo.reasonPhrase + "]")
        }
    }

    @Throws(CouldNotPerformException::class)
    protected fun get(target: String, skipValidation: Boolean = false): String {
        try {
            // handle validation

            if (!skipValidation) {
                validateConnection()
            }

            val webTarget = restTarget!!.path(target)
            val response = webTarget.request().get()

            return validateResponse(response, skipValidation)
        } catch (ex: CouldNotPerformException) {
            if (isShutdownInitiated) {
                setInitialCause(ex, ShutdownInProgressException(this))
            }
            throw CouldNotPerformException("Could not get sub-URL[$target]", ex)
        } catch (ex: ProcessingException) {
            if (isShutdownInitiated) {
                setInitialCause(ex, ShutdownInProgressException(this))
            }
            throw CouldNotPerformException("Could not get sub-URL[$target]", ex)
        }
    }

    @Throws(CouldNotPerformException::class)
    protected fun delete(target: String): String {
        try {
            validateConnection()
            val webTarget = restTarget!!.path(target)
            val response = webTarget.request().delete()

            return validateResponse(response)
        } catch (ex: CouldNotPerformException) {
            if (isShutdownInitiated) {
                setInitialCause(ex, ShutdownInProgressException(this))
            }
            throw CouldNotPerformException("Could not delete sub-URL[$target]", ex)
        } catch (ex: ProcessingException) {
            if (isShutdownInitiated) {
                setInitialCause(ex, ShutdownInProgressException(this))
            }
            throw CouldNotPerformException("Could not delete sub-URL[$target]", ex)
        }
    }

    @Throws(CouldNotPerformException::class)
    protected fun putJson(target: String, value: Any?): String {
        return put(target, gson!!.toJson(value), MediaType.APPLICATION_JSON_TYPE)
    }

    @Throws(CouldNotPerformException::class)
    protected fun put(target: String, value: String, mediaType: MediaType?): String {
        try {
            validateConnection()
            val webTarget = restTarget!!.path(target)
            val response = webTarget.request().put(Entity.entity(value, mediaType))

            return validateResponse(response)
        } catch (ex: CouldNotPerformException) {
            if (isShutdownInitiated) {
                setInitialCause(ex, ShutdownInProgressException(this))
            }
            throw CouldNotPerformException("Could not put value[$value] on sub-URL[$target]", ex)
        } catch (ex: ProcessingException) {
            if (isShutdownInitiated) {
                setInitialCause(ex, ShutdownInProgressException(this))
            }
            throw CouldNotPerformException("Could not put value[$value] on sub-URL[$target]", ex)
        }
    }

    @Throws(CouldNotPerformException::class)
    protected fun postJson(target: String, value: Any?): String {
        return post(target, gson!!.toJson(value), MediaType.APPLICATION_JSON_TYPE)
    }

    @Throws(CouldNotPerformException::class)
    protected fun post(target: String, value: String, mediaType: MediaType): String {
        try {
            validateConnection()
            val webTarget = restTarget!!.path(target)
            val response = webTarget.request().post(Entity.entity(value, mediaType))

            return validateResponse(response)
        } catch (ex: CouldNotPerformException) {
            if (isShutdownInitiated) {
                setInitialCause(ex, ShutdownInProgressException(this))
            }
            throw CouldNotPerformException(
                "Could not post Value[$value] of MediaType[$mediaType] on sub-URL[$target]",
                ex
            )
        } catch (ex: ProcessingException) {
            if (isShutdownInitiated) {
                setInitialCause(ex, ShutdownInProgressException(this))
            }
            throw CouldNotPerformException(
                "Could not post Value[$value] of MediaType[$mediaType] on sub-URL[$target]",
                ex
            )
        }
    }

    override fun shutdown() {
        // prepare shutdown

        isShutdownInitiated = true
        setConnectState(ConnectionStateType.ConnectionState.State.DISCONNECTED)

        // stop rest service
        restClient!!.close()

        // stop sse service
        synchronized(topicObservableMapLock) {
            for (jsonObjectObservable in topicObservableMap!!.values) {
                jsonObjectObservable.shutdown()
            }
            topicObservableMap!!.clear()
            resetConnection()
        }
    }


    private val OPENHAB_GATEWAY_CLASS_LABEL = "OpenHAB"
    private val META_CONFIG_TOKEN_KEY = "TOKEN"

    init {
        try {
            this.topicObservableMap = HashMap()
            this.gson = GsonBuilder().setExclusionStrategies(object : ExclusionStrategy {
                override fun shouldSkipField(fieldAttributes: FieldAttributes): Boolean {
                    return false
                }

                override fun shouldSkipClass(aClass: Class<*>): Boolean {
                    // ignore Command Description because its an interface and can not be serialized without any instance creator.
                    if (aClass == CommandDescription::class.java) {
                        return true
                    }
                    return false
                }
            }).create()

            this.restClient = ClientBuilder.newClient()
            try {
                restClient?.register(OAuth2ClientSupport.feature(token))
            } catch (ex: NotAvailableException) {
                LOGGER.warn("Could not retrieve OpenHAB token from gateway config!", ex)
            }
            this.restTarget =
                restClient?.target(JPService.getProperty(JPOpenHABURI::class.java).value.resolve(SEPARATOR + REST_TARGET))
            this.setConnectState(ConnectionStateType.ConnectionState.State.CONNECTING)
        } catch (ex: JPNotAvailableException) {
            throw InstantiationException(this, ex)
        } catch (ex: CouldNotPerformException) {
            throw InstantiationException(this, ex)
        }
    }

    @Throws(CouldNotPerformException::class)
    private fun findOpenHABGatewayClass(): GatewayClassType.GatewayClass {
        for (gatewayClass in Registries.getClassRegistry().gatewayClasses) {
            if (contains(gatewayClass.label, OPENHAB_GATEWAY_CLASS_LABEL)) {
                return gatewayClass
            }
        }

        throw NotAvailableException("OpenHAB Gateway Class")
    }

    @get:Throws(CouldNotPerformException::class)
    private val token: String
        get() {
            val openHABGatewayClass = findOpenHABGatewayClass()

            for (unitConfig in Registries.getUnitRegistry()
                .getUnitConfigsByUnitType(UnitTemplateType.UnitTemplate.UnitType.GATEWAY)) {
                if (unitConfig.gatewayConfig.gatewayClassId == openHABGatewayClass.id) {
                    return MetaConfigProcessor.getValue(unitConfig.metaConfig, META_CONFIG_TOKEN_KEY)
                }
            }
            throw NotAvailableException("token")
        }

    companion object {
        const val SEPARATOR: String = "/"
        const val REST_TARGET: String = "rest"

        const val APPROVE_TARGET: String = "approve"
        const val EVENTS_TARGET: String = "events"

        const val TOPIC_KEY: String = "topic"
        const val TOPIC_SEPARATOR: String = SEPARATOR

        private val LOGGER: Logger = LoggerFactory.getLogger(OpenHABRestConnection::class.java)
    }
}
