package org.openbase.bco.app.influxdbconnector

import com.google.protobuf.Descriptors
import com.google.protobuf.Message
import com.influxdb.client.InfluxDBClient
import com.influxdb.client.InfluxDBClientFactory
import com.influxdb.client.WriteApi
import com.influxdb.client.WriteOptions
import com.influxdb.client.domain.Bucket
import com.influxdb.client.domain.WritePrecision
import com.influxdb.client.write.Point
import com.influxdb.client.write.events.WriteErrorEvent
import com.influxdb.client.write.events.WriteSuccessEvent
import org.openbase.bco.dal.control.layer.unit.InfluxDbProcessor
import org.openbase.bco.dal.control.layer.unit.app.AbstractAppController
import org.openbase.bco.dal.lib.layer.service.ServiceStateProvider
import org.openbase.bco.dal.lib.layer.service.Services
import org.openbase.bco.dal.lib.layer.unit.UnitRemote
import org.openbase.bco.dal.remote.layer.unit.CustomUnitPool
import org.openbase.bco.dal.remote.layer.unit.Units
import org.openbase.bco.registry.remote.Registries
import org.openbase.jul.exception.*
import org.openbase.jul.exception.printer.ExceptionPrinter
import org.openbase.jul.exception.printer.LogLevel
import org.openbase.jul.extension.type.processing.LabelProcessor.getBestMatch
import org.openbase.jul.extension.type.processing.TimestampProcessor
import org.openbase.jul.pattern.Observer
import org.openbase.jul.schedule.GlobalCachedExecutorService
import org.openbase.jul.schedule.GlobalScheduledExecutorService
import org.openbase.type.domotic.action.ActionDescriptionType
import org.openbase.type.domotic.service.ServiceTemplateType
import org.openbase.type.domotic.service.ServiceTemplateType.ServiceTemplate.ServicePattern
import org.openbase.type.domotic.service.ServiceTempusTypeType.ServiceTempusType.ServiceTempus
import org.openbase.type.domotic.state.ActivationStateType
import org.openbase.type.domotic.unit.UnitConfigType.UnitConfig
import java.util.*
import java.util.concurrent.*
import java.util.concurrent.TimeoutException


class InfluxDbconnectorApp : AbstractAppController() {
    private var writeApi: WriteApi? = null
    private var databaseTimeout: Int = InfluxDbProcessor.DATABASE_TIMEOUT_DEFAULT
    private var bucket: Bucket? = null
    private var token: CharArray? = null
    private var task: Future<*>? = null
    private var heartbeat: Future<*>? = null
    private var databaseUrl: String? = InfluxDbProcessor.INFLUXDB_URL_DEFAULT
    private var bucketName: String = InfluxDbProcessor.INFLUXDB_BUCKET_DEFAULT
    private var influxDBClient: InfluxDBClient? = null
    private var batchTime: Int = InfluxDbProcessor.INFLUXDB_BATCH_TIME_DEFAULT
    private var batchLimit: Int = InfluxDbProcessor.INFLUXDB_BATCH_LIMIT_DEFAULT
    private val customUnitPool: CustomUnitPool<*, *> = CustomUnitPool<Message, UnitRemote<Message>>()
    private val unitStateObserver: Observer<ServiceStateProvider<Message>, Message>
    private var org: String = InfluxDbProcessor.INFLUXDB_ORG_DEFAULT

    init {
        this.unitStateObserver =
            Observer<ServiceStateProvider<Message>, Message> { source: ServiceStateProvider<Message>, data: Message ->
                storeServiceState(
                    source.serviceProvider as org.openbase.bco.dal.lib.layer.unit.Unit<*>,
                    source.serviceType,
                    false
                )
            }
    }

    @Throws(CouldNotPerformException::class, InterruptedException::class)
    override fun applyConfigUpdate(config: UnitConfig): UnitConfig {
        getManageWriteLockInterruptible(this).use { _ ->
            return super.applyConfigUpdate(config).also {
                bucketName = tryOrNull {
                    generateVariablePool().getValue(InfluxDbProcessor.INFLUXDB_BUCKET)
                } ?: bucketName

                batchTime = tryOrNull {
                    generateVariablePool().getValue(InfluxDbProcessor.INFLUXDB_BATCH_TIME).toInt()
                } ?: batchTime

                batchLimit = tryOrNull {
                    generateVariablePool().getValue(InfluxDbProcessor.INFLUXDB_BATCH_LIMIT).toInt()
                } ?: batchLimit

                databaseUrl = tryOrNull {
                    generateVariablePool().getValue(InfluxDbProcessor.INFLUXDB_URL)
                } ?: databaseUrl

                org = tryOrNull {
                    generateVariablePool().getValue(InfluxDbProcessor.INFLUXDB_ORG)
                } ?: org

                token = generateVariablePool().getValue(InfluxDbProcessor.INFLUXDB_TOKEN).toCharArray()
            }
        }
    }

    override fun execute(activationState: ActivationStateType.ActivationState): ActionDescriptionType.ActionDescription? {
        task = GlobalCachedExecutorService.submit<Any?> {
            try {
                logger.debug("Execute influx db connector")

                // connect to db
                connectToDatabase()
                while (!task!!.isCancelled) {
                    try {
                        verifyConnection()
                        break
                    } catch (ex: CouldNotPerformException) {
                        ExceptionPrinter.printHistory<CouldNotPerformException>(
                            "Could not reach influxdb server at " + databaseUrl + ". Try again in " + databaseTimeout / 1000 + " seconds!",
                            ex,
                            logger,
                            LogLevel.WARN
                        )
                        Thread.sleep(databaseTimeout.toLong())
                        if (databaseTimeout < InfluxDbProcessor.MAX_TIMEOUT) {
                            databaseTimeout += InfluxDbProcessor.ADDITIONAL_TIMEOUT
                        }
                    }
                }

                // lookup bucked
                while (!task!!.isCancelled) {
                    try {
                        // check if bucked found
                        databaseBucket
                        break
                    } catch (ex: NotAvailableException) {
                        logger.warn("Could not get bucket. Try again in " + databaseTimeout / 1000 + " seconds!")

                        ExceptionPrinter.printHistory(ex, logger)
                        Thread.sleep(databaseTimeout.toLong())
                    }
                }

                // start observation
                try {
                    startObservation()
                } catch (ex: InitializationException) {
                    ExceptionPrinter.printHistory(ex, logger)
                }
            } catch (ex: InterruptedException) {
                // finish task because its canceled.
                Thread.currentThread().interrupt()
                return@submit
            }
            if (!task!!.isCancelled && isConnected) {
                try {
                    // write initial heartbeat
                    logger.debug("initial heartbeat")
                    writeApi!!.writePoint(
                        bucketName, org, Point.measurement(InfluxDbProcessor.HEARTBEAT_MEASUREMENT)
                            .addField(InfluxDbProcessor.HEARTBEAT_FIELD, InfluxDbProcessor.HEARTBEAT_OFFLINE_VALUE)
                            .time(System.currentTimeMillis() - 1, WritePrecision.MS)
                    )
                    writeApi?.writePoint(
                        bucketName, org, Point.measurement(InfluxDbProcessor.HEARTBEAT_MEASUREMENT)
                            .addField(InfluxDbProcessor.HEARTBEAT_FIELD, InfluxDbProcessor.HEARTBEAT_ONLINE_VALUE)
                            .time(System.currentTimeMillis(), WritePrecision.MS)
                    )

                    heartbeat = GlobalScheduledExecutorService.scheduleAtFixedRate(
                        {
                            logger.debug("write heartbeat")
                            writeApi?.writePoint(
                                bucketName, org, Point.measurement(InfluxDbProcessor.HEARTBEAT_MEASUREMENT)
                                    .addField(
                                        InfluxDbProcessor.HEARTBEAT_FIELD,
                                        InfluxDbProcessor.HEARTBEAT_ONLINE_VALUE
                                    )
                                    .time(System.currentTimeMillis(), WritePrecision.MS)
                            )
                        },
                        InfluxDbProcessor.HEARTBEAT_INITIAL_DELAY.toLong(),
                        InfluxDbProcessor.HEARTBEAT_PERIOD,
                        TimeUnit.MILLISECONDS
                    )
                } catch (ex: NotAvailableException) {
                    ExceptionPrinter.printHistory(
                        "Could not write heartbeat!",
                        ex,
                        logger,
                        LogLevel.WARN
                    )
                }
            }
            null
        }
        return activationState.responsibleAction
    }

    @Throws(CouldNotPerformException::class, InterruptedException::class)
    override fun stop(activationState: ActivationStateType.ActivationState) {
        // finish task

        logger.debug("finish task")
        if (task != null && !task!!.isDone) {
            task!!.cancel(true)
            try {
                task!![5, TimeUnit.SECONDS]
            } catch (ex: CancellationException) {
                // that's what we are waiting for.
            } catch (ex: Exception) {
                ExceptionPrinter.printHistory(ex, logger)
            }
        }

        logger.debug("finish heartbeat")
        if (heartbeat != null && !heartbeat!!.isDone) {
            heartbeat?.cancel(true)
            try {
                heartbeat!![5, TimeUnit.SECONDS]
            } catch (ex: CancellationException) {
                // that's what we are waiting for.
            } catch (ex: Exception) {
                ExceptionPrinter.printHistory(ex, logger)
            }
        }

        if (isConnected) {
            // write final heartbeat if connection is still established.
            writeApi?.writePoint(
                bucketName, org, Point.measurement(InfluxDbProcessor.HEARTBEAT_MEASUREMENT)
                    .addField(InfluxDbProcessor.HEARTBEAT_FIELD, InfluxDbProcessor.HEARTBEAT_ONLINE_VALUE)
                    .time(System.currentTimeMillis() - 1, WritePrecision.MS)
            )
            writeApi?.writePoint(
                bucketName, org, Point.measurement(InfluxDbProcessor.HEARTBEAT_MEASUREMENT)
                    .addField(InfluxDbProcessor.HEARTBEAT_FIELD, InfluxDbProcessor.HEARTBEAT_OFFLINE_VALUE)
                    .time(System.currentTimeMillis(), WritePrecision.MS)
            )
            writeApi?.flush()
        }

        // deregister
        customUnitPool.removeServiceStateObserver(unitStateObserver)
        customUnitPool.deactivate()
        disconnectDatabase()

        super.stop(activationState)
    }

    @Throws(InitializationException::class, InterruptedException::class)
    fun startObservation() {
        try {
            // setup pool
            customUnitPool.addServiceStateObserver(unitStateObserver)
            customUnitPool.activate()

            for (unitConfig in Registries.getUnitRegistry(true).unitConfigs) {
                val unit: UnitRemote<*>
                try {
                    unit = Units.getFutureUnit(unitConfig, true)
                        .get(InfluxDbProcessor.MAX_INITIAL_STORAGE_TIMEOUT, TimeUnit.MILLISECONDS)
                } catch (ex: ExecutionException) {
                    ExceptionPrinter.printHistory<Exception>(
                        "Could not reach Unit " + getBestMatch(unitConfig.label) + "! Skip initial service state synchronisation because unit will be synchronized anyway when it connection is established.",
                        ex,
                        logger,
                        LogLevel.DEBUG
                    )
                    continue
                } catch (ex: TimeoutException) {
                    ExceptionPrinter.printHistory<Exception>(
                        "Could not reach Unit " + getBestMatch(unitConfig.label) + "! Skip initial service state synchronisation because unit will be synchronized anyway when it connection is established.",
                        ex,
                        logger,
                        LogLevel.DEBUG
                    )
                    continue
                }
                try {
                    for (serviceDescription in unit.unitTemplate.serviceDescriptionList) {
                        if (serviceDescription.pattern != ServicePattern.PROVIDER) {
                            continue
                        }
                        storeServiceState(unit, serviceDescription.serviceType, true)
                    }
                } catch (ex: CouldNotPerformException) {
                    ExceptionPrinter.printHistory("Could not store service state $unit", ex, logger)
                }
            }
        } catch (ex: CouldNotPerformException) {
            throw InitializationException(this, ex)
        }
    }

    @Throws(CouldNotPerformException::class)
    private fun storeServiceState(
        unit: org.openbase.bco.dal.lib.layer.unit.Unit<*>,
        serviceType: ServiceTemplateType.ServiceTemplate.ServiceType,
        initialSync: Boolean,
    ) {
        val currentServiceState: Message
        var lastServiceState: Message? = null
        try {
            currentServiceState = Services.invokeProviderServiceMethod(
                serviceType,
                ServiceTempus.CURRENT,
                unit.data
            )
        } catch (ex: NotAvailableException) {
            // if the current state is not available, we just try to at least store the last known service state if available.

            try {
                lastServiceState = Services.invokeProviderServiceMethod(
                    serviceType,
                    ServiceTempus.LAST,
                    unit.data
                )
                storeServiceState(unit, serviceType, lastServiceState)
            } catch (exx: CouldNotPerformException) {
                // we don't care if the last service state is not available
                // which can be the case for an initial sync
                // or any states which got only one state update since system startup.
            }
            return
        }

        // store t - 1 entry
        try {
            lastServiceState = Services.invokeProviderServiceMethod(
                serviceType,
                ServiceTempus.LAST,
                unit.data
            )
            val serviceStateTimestamp = TimestampProcessor.getTimestamp(currentServiceState, TimeUnit.MILLISECONDS) - 1L
            lastServiceState =
                TimestampProcessor.updateTimestamp(serviceStateTimestamp, lastServiceState, TimeUnit.MILLISECONDS)
            storeServiceState(unit, serviceType, lastServiceState)
        } catch (ex: CouldNotPerformException) {
            // we don't care if the last service state is not available
            // which can be the case for an initial sync
            // or any states which got only one state update since system startup.
        }

        try {
            storeServiceState(unit, serviceType, currentServiceState)
        } catch (ex: CouldNotPerformException) {
            // filter log if initial timestamps are missing

            if (initialSync) {
                return
            }

            ExceptionPrinter.printHistory(
                "Could not store service state change into db! " +
                        "UnitType[" + unit.unitType + "] " +
                        "ServiceType[" + serviceType + "] " +
                        "CurrentServiceState[" + currentServiceState + "] " +
                        "LastServiceState[" + lastServiceState + "]",
                ex, logger, LogLevel.DEBUG
            )
        }
    }

    @Throws(InvalidStateException::class)
    private fun storeServiceState(
        unit: org.openbase.bco.dal.lib.layer.unit.Unit<*>,
        serviceType: ServiceTemplateType.ServiceTemplate.ServiceType,
        serviceState: Message,
    ) {
        val timestamp = TimestampProcessor.getTimestamp(serviceState, TimeUnit.MILLISECONDS)
        try {
            val initiator = runCatching {
                Services.getResponsibleAction(serviceState).actionInitiator.initiatorType.name.lowercase()
            }.getOrElse { "system" }

            val stateValuesMap = resolveStateValueToMap(serviceState)
            val point = Point.measurement(serviceType.name.lowercase(Locale.getDefault()))
                .addTag("alias", unit.config.getAlias(0))
                .addTag("initiator", initiator)
                .addTag("unit_id", unit.id)
                .addTag("unit_type", unit.unitType.name.lowercase(Locale.getDefault()))
                .addTag("location_id", unit.parentLocationConfig.id)
                .addTag("location_alias", unit.parentLocationConfig.getAlias(0))
                .time(timestamp, WritePrecision.MS)

            var values = 0
            for ((key, value) in stateValuesMap) {
                // detect numbers with regex
                if (value.matches("-?\\d+(\\.\\d+)?".toRegex())) {
                    values++
                    point.addField(key, value.toDouble())
                } else {
                    point.addTag(key, value)
                }
            }

            unit.config.label.entryList
                .filter { it.valueList.isNotEmpty() }
                .forEach { point.addTag("label_" + it.key, it.getValue(0)) }

            if (values > 0) {
                writeApi!!.writePoint(bucketName!!, org!!, point)
            }
        } catch (ex: CouldNotPerformException) {
            ExceptionPrinter.printHistory(
                "Could not store service state " + serviceType.name + " of " + unit,
                ex,
                logger
            )
        }
    }


    @Throws(CouldNotPerformException::class)
    fun resolveStateValueToMap(serviceState: Message): Map<String, String> {
        val stateValues: MutableMap<String, String> = HashMap()
        for (fieldDescriptor in serviceState.descriptorForType.fields) {
            val stateName = fieldDescriptor.name
            var stateType = fieldDescriptor.type.toString().lowercase()

            // filter invalid states
            if (stateName == null) {
                logger.warn("Could not detect datatype of $stateType")
            }

            when (stateName) {
                "aggregated_value_coverage", "last_value_occurrence", "timestamp", "responsible_action", "type", "rgb_color", "frame_id" -> continue
            }
            // filter data units
            if (stateName.endsWith("data_unit")) {
                continue
            }

            var stateValue = serviceState.getField(fieldDescriptor).toString()

            when (stateValue) {
                "", "NaN" -> continue
                else -> {}
            }

            try {
                if (fieldDescriptor.type == Descriptors.FieldDescriptor.Type.MESSAGE) {
                    if (fieldDescriptor.isRepeated) {
                        val types: MutableList<String> = ArrayList()

                        for (i in 0 until serviceState.getRepeatedFieldCount(fieldDescriptor)) {
                            val repeatedFieldEntry = serviceState.getRepeatedField(fieldDescriptor, i)
                            if (repeatedFieldEntry is Message) {
                                types.add(
                                    "[" + Services.resolveStateValue(
                                        repeatedFieldEntry
                                    ).toString() + "]"
                                )
                            }
                            types.add(repeatedFieldEntry.toString())
                        }
                        stateType = types.toString().lowercase(Locale.getDefault())
                    } else {
                        stateValue = Services.resolveStateValue(
                            serviceState.getField(fieldDescriptor) as Message
                        ).toString()
                    }
                }
            } catch (ex: InvalidStateException) {
                logger.warn("Could not process value of " + fieldDescriptor.name)
                continue
            }

            if (fieldDescriptor.javaType == Descriptors.FieldDescriptor.JavaType.ENUM) {
                val finalStateValue = stateValue
                stateValue = fieldDescriptor.enumType.values.stream()
                    .filter { `val`: Descriptors.EnumValueDescriptor -> `val`.name == finalStateValue }
                    .findFirst().get().number.toString()
            }

            stateValues[fieldDescriptor.name] = stateValue.lowercase()
        }
        return stateValues
    }

    private val isConnected: Boolean
        /**
         * Method checks if the connection is established.
         *
         * @return true if the connection to influx db is established, otherwise false.
         */
        get() {
            try {
                verifyConnection()
            } catch (ex: VerificationFailedException) {
                return false
            }
            return true
        }

    /**
     * Method verifies the connection state.
     *
     * @throws VerificationFailedException is thrown if the connection is not established.
     */
    @Throws(VerificationFailedException::class)
    private fun verifyConnection() {
        if (influxDBClient == null) {
            throw VerificationFailedException("Influx db connection has never been initiated.")
        }

        if (influxDBClient?.ping() != true) {
            throw VerificationFailedException("Could not connect to database server at $databaseUrl!")
        }

        // initiate WriteApi
        val writeoptions = WriteOptions.builder().batchSize(batchLimit).flushInterval(batchTime).build()
        writeApi = influxDBClient?.makeWriteApi(writeoptions)
        writeApi?.listenEvents(WriteSuccessEvent::class.java) {
            logger.debug("Successfully wrote data into db")
        }
        writeApi?.listenEvents(WriteErrorEvent::class.java) { event: WriteErrorEvent ->
            val exception = event.throwable
            logger.warn(exception.message)
        }
        logger.debug("Connected to Influxdb at $databaseUrl")
    }

    private fun connectToDatabase() {
        try {
            influxDBClient?.close()
        } catch (ex: Exception) {
            ExceptionPrinter.printHistory("Could not shutdown database connection!", ex, logger)
        }
        logger.debug(" Try to connect to influxDB at $databaseUrl")

        token?.let {
            influxDBClient = InfluxDBClientFactory.create(
                databaseUrl + "?readTimeout=" + InfluxDbProcessor.READ_TIMEOUT + "&connectTimeout=" + InfluxDbProcessor.CONNECT_TIMOUT + "&writeTimeout=" + InfluxDbProcessor.WRITE_TIMEOUT + "&logLevel=BASIC",
                it
            )
        }
    }

    private fun disconnectDatabase() {
        try {
            writeApi?.flush()
            influxDBClient?.close()
            writeApi = null
        } catch (ex: Exception) {
            ExceptionPrinter.printHistory("Could not shutdown database connection!", ex, logger)
        }
    }

    @get:Throws(NotAvailableException::class)
    private val databaseBucket: Unit
        get() {
            logger.debug("Get bucket $bucketName")
            bucket = influxDBClient?.bucketsApi?.findBucketByName(bucketName)
            if (bucket == null) {
                throw NotAvailableException("bucket", bucketName)
            }
        }
}
