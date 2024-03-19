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
import org.openbase.type.domotic.state.ConnectionStateType
import org.openbase.type.domotic.unit.UnitTemplateType.UnitTemplate.UnitType
import org.openbase.type.domotic.unit.gateway.GatewayClassType
import org.openhab.core.types.CommandDescription
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.net.ConnectException
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock
import java.util.function.Consumer
import kotlin.concurrent.withLock

abstract class OpenHABRestConnection : Shutdownable {
    private val topicObservableMapLock = ReentrantLock()
    private val connectionStateLock = ReentrantLock()
    private val connectionStateCondition = connectionStateLock.newCondition()
    private var topicObservableMap: MutableMap<String, ObservableImpl<Any, JsonObject>>

    private var restClient: Client
    private var restTarget: WebTarget
    private var sseSource: SseEventSource? = null

    var isShutdownInitiated: Boolean = false
        private set

    @JvmField
    protected var gson: Gson

    private var connectionTask: ScheduledFuture<*>? = null

    var openhabConnectionState: ConnectionStateType.ConnectionState.State =
        ConnectionStateType.ConnectionState.State.DISCONNECTED
        protected set

    init {
        try {
            this.topicObservableMap = HashMap()
            this.gson = GsonBuilder().setExclusionStrategies(object : ExclusionStrategy {
                override fun shouldSkipField(fieldAttributes: FieldAttributes): Boolean {
                    return false
                }

                override fun shouldSkipClass(aClass: Class<*>): Boolean {
                    // ignore Command Description because its an interface and can not be serialized without any instance creator.
                    return aClass == CommandDescription::class.java
                }
            }).create()

            this.restClient = ClientBuilder.newClient()
            try {
                restClient.register(OAuth2ClientSupport.feature(token))
            } catch (ex: NotAvailableException) {
                LOGGER.warn("Could not retrieve OpenHAB token from gateway config!", ex)
            }
            this.restTarget =
                restClient.target(JPService.getProperty(JPOpenHABURI::class.java).value.resolve(SEPARATOR + REST_TARGET))
            this.setConnectState(ConnectionStateType.ConnectionState.State.CONNECTING)
        } catch (ex: JPNotAvailableException) {
            throw InstantiationException(this, ex)
        } catch (ex: CouldNotPerformException) {
            throw InstantiationException(this, ex)
        }
    }

    private val isTargetReachable: Boolean
        get() = runCatching { testConnection() }.isSuccess

    @Throws(CouldNotPerformException::class)
    protected abstract fun testConnection()

    @Throws(InterruptedException::class)
    fun waitForConnectionState(
        connectionState: ConnectionStateType.ConnectionState.State,
        timeout: Long,
        timeUnit: TimeUnit,
    ) {
        connectionStateLock.withLock {
            while (openhabConnectionState != connectionState) {
                connectionStateCondition.await(timeout, timeUnit)
            }
        }
    }

    @Throws(InterruptedException::class)
    fun waitForConnectionState(connectionState: ConnectionStateType.ConnectionState.State) {
        connectionStateLock.withLock {
            while (openhabConnectionState != connectionState) {
                connectionStateCondition.await()
            }
        }
    }

    private fun setConnectState(connectState: ConnectionStateType.ConnectionState.State) {
        connectionStateLock.withLock {
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
                        connectionTask?.cancel(true)
                        connectionTask = GlobalScheduledExecutorService.scheduleWithFixedDelay({
                            if (isTargetReachable) {
                                // set connected
                                setConnectState(ConnectionStateType.ConnectionState.State.CONNECTED)

                                // cleanup own task
                                connectionTask?.cancel(false)
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
            connectionStateCondition.signalAll()
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

        sseSource = SseEventSource
            .target(restTarget.path(EVENTS_TARGET))
            .reconnectingEvery(15, TimeUnit.SECONDS)
            .build()
            .also { it.open() }


        val evenConsumer = Consumer { inboundSseEvent: InboundSseEvent ->
            // dispatch event
            try {
                val payload = JsonParser.parseString(inboundSseEvent.readData()).asJsonObject
                for ((key, value) in topicObservableMap) {
                    try {
                        payload[TOPIC_KEY]
                            ?.asString
                            ?.takeIf { it.matches(key.toRegex()) }
                            ?.run { value.notifyObservers(payload) }
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

    private fun checkConnectionState() {
        connectionStateLock.withLock {
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
    fun addSSEObserver(observer: Observer<Any, JsonObject>, topicRegex: String = "") {
        topicObservableMapLock.withLock {
            topicObservableMap
                .getOrPut(topicRegex) { ObservableImpl<Any, JsonObject>(this) }
                .addObserver(observer)
        }
    }

    @JvmOverloads
    fun removeSSEObserver(observer: Observer<Any, JsonObject>, topicFilter: String = "") {
        topicObservableMapLock.withLock {
            topicObservableMap[topicFilter]?.removeObserver(observer)
        }
    }

    private fun resetConnection() {
        // cancel ongoing connection task
        connectionTask
            ?.takeIf { !it.isDone }
            ?.cancel(false)

        // close sse
        sseSource
            ?.close()
            ?.also { sseSource = null }
    }

    @Throws(CouldNotPerformException::class)
    fun validateConnection() {
        if (!isConnected) {
            throw InvalidStateException("Openhab not reachable yet!")
        }
    }

    @Throws(CouldNotPerformException::class, ProcessingException::class)
    private fun validateResponse(response: Response, skipConnectionValidation: Boolean = false): String =
        response.readEntity(String::class.java).let { result ->
            when (response.status) {
                200, 201, 202 -> {
                    result
                }

                404 -> {
                    if (!skipConnectionValidation) {
                        checkConnectionState()
                    }
                    throw NotAvailableException("URL")
                }

                503 -> {
                    if (!skipConnectionValidation) {
                        checkConnectionState()
                    }
                    // throw a processing exception to indicate that openHAB is still not fully started, this is used to wait for openHAB
                    throw ProcessingException("OpenHAB server not ready")
                }

                else -> {
                    throw CouldNotPerformException("Response returned with ErrorCode[" + response.status + "], Result[" + result + "] and ErrorMessage[" + response.statusInfo.reasonPhrase + "]")
                }
            }
        }

    @Throws(CouldNotPerformException::class)
    protected fun get(target: String, skipValidation: Boolean = false): String {
        try {
            // handle validation

            if (!skipValidation) {
                validateConnection()
            }

            val webTarget = restTarget.path(target)
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
            val webTarget = restTarget.path(target)
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
        return put(target, gson.toJson(value), MediaType.APPLICATION_JSON_TYPE)
    }

    @Throws(CouldNotPerformException::class)
    protected fun put(target: String, value: String, mediaType: MediaType?): String {
        try {
            validateConnection()
            val webTarget = restTarget.path(target)
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
        } catch (ex: ConnectException) {
            if (isShutdownInitiated) {
                setInitialCause(ex, ShutdownInProgressException(this))
            }
            throw CouldNotPerformException("Could not put value[$value] on sub-URL[$target]", ex)
        }
    }

    @Throws(CouldNotPerformException::class)
    protected fun postJson(target: String, value: Any?): String {
        return post(target, gson.toJson(value), MediaType.APPLICATION_JSON_TYPE)
    }

    @Throws(CouldNotPerformException::class)
    protected fun post(target: String, value: String, mediaType: MediaType): String {
        try {
            validateConnection()
            val webTarget = restTarget.path(target)
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
        restClient.close()

        // stop sse service
        topicObservableMapLock.withLock {
            topicObservableMap.values.forEach { jsonObjectObservable ->
                jsonObjectObservable.shutdown()
            }
            topicObservableMap.clear()
            resetConnection()
        }
    }

    @Throws(CouldNotPerformException::class)
    private fun findOpenHABGatewayClass(): GatewayClassType.GatewayClass? =
        Registries.getClassRegistry().gatewayClasses.find { contains(it.label, OPENHAB_GATEWAY_CLASS_LABEL) }

    private val token: String?
        get() = findOpenHABGatewayClass()?.let { openHABGatewayClass ->
            Registries.getUnitRegistry()
                .getUnitConfigsByUnitType(UnitType.GATEWAY)
                .find { it.gatewayConfig.gatewayClassId == openHABGatewayClass.id }
                ?.let { MetaConfigProcessor.getValue(it.metaConfig, META_CONFIG_TOKEN_KEY) }
        }

    companion object {

        private const val OPENHAB_GATEWAY_CLASS_LABEL = "OpenHAB"
        private const val META_CONFIG_TOKEN_KEY = "TOKEN"

        const val SEPARATOR: String = "/"
        const val REST_TARGET: String = "rest"

        const val APPROVE_TARGET: String = "approve"
        const val EVENTS_TARGET: String = "events"

        const val TOPIC_KEY: String = "topic"
        const val TOPIC_SEPARATOR: String = SEPARATOR

        private val LOGGER: Logger = LoggerFactory.getLogger(OpenHABRestConnection::class.java)
    }
}
