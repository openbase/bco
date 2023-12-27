package org.openbase.bco.dal.control.message

import com.google.protobuf.Message
import org.openbase.bco.dal.lib.layer.service.Services
import org.openbase.bco.dal.lib.layer.unit.UnitRemote
import org.openbase.bco.dal.remote.layer.unit.Units
import org.openbase.bco.registry.remote.Registries
import org.openbase.jul.exception.CouldNotPerformException
import org.openbase.jul.exception.InitializationException
import org.openbase.jul.iface.Launchable
import org.openbase.jul.iface.VoidInitializable
import org.openbase.jul.pattern.Observer
import org.openbase.jul.pattern.provider.DataProvider
import org.openbase.jul.schedule.RecurrenceEventFilter
import org.openbase.type.domotic.registry.MessageRegistryDataType.MessageRegistryData
import org.slf4j.LoggerFactory
import java.time.Duration
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.write

class MessageManager : Launchable<Void>, VoidInitializable {

    private var logger = LoggerFactory.getLogger(MessageManager::class.java)

    private val unitsOfConditionsLock = ReentrantReadWriteLock()

    private val maxUpdateInterval: Duration = Duration.ofSeconds(1)

    private var active = false

    private val removeOutdatedMessagesTask: RecurrenceEventFilter<Boolean> =
        object : RecurrenceEventFilter<Boolean>(maxUpdateInterval.toMillis()) {
            override fun relay() {
                removeOutdatedMessages()
            }
        }

    fun removeOutdatedMessages() {
        logger.trace("removeOutdatedMessages")
        Registries.getMessageRegistry().userMessages
            .filterNot { message ->
                message.conditionList.any { condition ->
                    Units.getUnit<Message>(condition.unitId, true).let { unit ->
                        Services.equalServiceStates(
                            unit.getServiceState(condition.serviceType),
                            Services.deserializeServiceState(condition)
                        )
                    }
                }
            }
            .forEach { Registries.getMessageRegistry().removeUserMessage(it).get(5, TimeUnit.SECONDS) }
    }

    private var unitsOfConditions: List<UnitRemote<Message>>? = null

    private val conditionObserver: Observer<DataProvider<Message>, Message> =
        Observer { _, _ -> removeOutdatedMessagesTask.trigger() }

    private val messageRegistryChangeObserver: Observer<DataProvider<MessageRegistryData>, MessageRegistryData> =
        Observer { _, data ->
            unitsOfConditionsLock.write {
                unitsOfConditions?.forEach { it.removeDataObserver(conditionObserver) }
                unitsOfConditions = data.userMessageList.let { userMessages ->
                    userMessages.flatMap { it.conditionList }
                        .map { it.unitId }
                        .distinct()
                        .mapNotNull { unitId ->
                            try {
                                Units.getUnit<Message>(unitId, false)
                            } catch (e: CouldNotPerformException) {
                                logger.warn("Could not resolve unit with id $unitId", e)
                                null
                            }
                        }
                }
                unitsOfConditions?.forEach { it.addDataObserver(conditionObserver) }
            }
        }

    @Throws(InitializationException::class, InterruptedException::class)
    override fun init() {
        // this overwrite is needed to overwrite the default implementation!
    }

    override fun activate() {
        active = true
        Registries.getMessageRegistry().addDataObserver(messageRegistryChangeObserver)
    }

    override fun deactivate() {
        Registries.getMessageRegistry().removeDataObserver(messageRegistryChangeObserver)
        unitsOfConditionsLock.write {
            unitsOfConditions?.forEach { it.removeDataObserver(conditionObserver) }
            unitsOfConditions = null
        }
        active = false
    }

    override fun isActive(): Boolean = active
}
