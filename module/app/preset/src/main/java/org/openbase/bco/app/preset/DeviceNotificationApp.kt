package org.openbase.bco.app.preset

import org.openbase.bco.dal.control.layer.unit.app.AbstractAppController
import org.openbase.bco.dal.lib.layer.service.ServiceStateProcessor
import org.openbase.bco.dal.remote.layer.unit.BatteryRemote
import org.openbase.bco.dal.remote.layer.unit.CustomUnitPool
import org.openbase.bco.registry.message.remote.registerUserMessageAuthenticated
import org.openbase.bco.registry.remote.Registries
import org.openbase.jul.exception.CouldNotPerformException
import org.openbase.jul.exception.printer.ExceptionPrinter
import org.openbase.jul.extension.type.processing.LabelProcessor
import org.openbase.jul.extension.type.processing.MultiLanguageTextProcessor
import org.openbase.jul.extension.type.processing.TimestampProcessor
import org.openbase.jul.schedule.GlobalScheduledExecutorService
import org.openbase.type.domotic.action.ActionDescriptionType.ActionDescription
import org.openbase.type.domotic.communication.UserMessageType.UserMessage
import org.openbase.type.domotic.service.ServiceStateDescriptionType.ServiceStateDescription
import org.openbase.type.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType
import org.openbase.type.domotic.state.ActivationStateType.ActivationState
import org.openbase.type.domotic.state.BatteryStateType.BatteryState
import org.openbase.type.domotic.unit.UnitConfigType.UnitConfig
import org.openbase.type.domotic.unit.UnitFilterType.UnitFilter
import org.openbase.type.domotic.unit.UnitTemplateType.UnitTemplate.UnitType
import org.openbase.type.domotic.unit.dal.BatteryDataType.BatteryData
import org.openbase.type.language.MultiLanguageTextType.MultiLanguageText
import org.slf4j.LoggerFactory
import java.time.Duration
import java.util.*
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

/**
 * An app that notifies users about devices with empty batteries or offline states.
 */
class DeviceNotificationApp : AbstractAppController() {

    private val batteryPool = CustomUnitPool<BatteryData, BatteryRemote>()
    private var task: ScheduledFuture<*>? = null

    init {
        batteryPool.init(
            UnitFilter.newBuilder().setProperties(UnitConfig.newBuilder().setUnitType(UnitType.BATTERY)).build()
        )
    }

    @Throws(CouldNotPerformException::class, InterruptedException::class)
    override fun execute(activationState: ActivationState): ActionDescription =
        activationState.responsibleAction.also {
            batteryPool.activate()
            task?.cancel(false)
            task = GlobalScheduledExecutorService.scheduleWithFixedDelay(
                ::checkDevices,
                INITIAL_VALIDATION_DELAY.toMillis(),
                VALIDATION_PERIOD.toMillis(),
                TimeUnit.MILLISECONDS
            )
            LOGGER.trace(getLabel() + " is running.")
        }

    @Throws(InterruptedException::class, CouldNotPerformException::class)
    override fun stop(activationState: ActivationState) =
        super.stop(activationState).also {
            batteryPool.deactivate()
            task?.cancel(true)
            LOGGER.trace(getLabel() + " has been stopped.")
        }

    fun checkDevices() {
        try {
            batteryPool.internalUnitList
                .onEach { remote -> remote.waitForData() }
                .filter { remote ->
                    when (remote.data.batteryState.value) {
                        BatteryState.State.LOW, BatteryState.State.CRITICAL, BatteryState.State.UNKNOWN -> true
                        else -> false
                    }
                }
                .map { remote ->
                    val messageBuilder: UserMessage.Builder = UserMessage.newBuilder()
                    val textBuilder: MultiLanguageText.Builder = MultiLanguageText.newBuilder()

                    MultiLanguageTextProcessor.addMultiLanguageText(
                        textBuilder,
                        Locale.ENGLISH,
                        "Battery level of ${
                            LabelProcessor.getBestMatch(
                                Locale.ENGLISH,
                                remote.config.label
                            )
                        } is ${remote.data.batteryState.value}"
                    )

                    MultiLanguageTextProcessor.addMultiLanguageText(
                        textBuilder,
                        Locale.GERMAN,
                        "Batterieladung von ${
                            LabelProcessor.getBestMatch(
                                Locale.GERMAN,
                                remote.config.label
                            )
                        } ist ${remote.data.batteryState.value}"
                    )

                    messageBuilder.id = UUID.randomUUID().toString()
                    messageBuilder.messageType = UserMessage.MessageType.WARNING
                    messageBuilder.timestamp = TimestampProcessor.getCurrentTimestamp()
                    messageBuilder.text = textBuilder.build()
                    messageBuilder.senderId = userConfig.id
                    messageBuilder.addCondition(
                        with(ServiceStateDescription.newBuilder()) {
                            setUnitId(remote.id)
                            setServiceType(ServiceType.BATTERY_STATE_SERVICE)
                            setServiceStateClassName(BatteryState::class.java.name)
                            setServiceState(
                                ServiceStateProcessor.serializeServiceState(
                                    BatteryState.newBuilder().setValue(remote.data.batteryState.value).build(),
                                    false,
                                )
                            )
                        }.also { ServiceStateProcessor.deserializeServiceState(it) }
                    )

                    // validate if message already exist
                    messageBuilder.build() to Registries.getMessageRegistry()
                        .getUserMessagesByText(
                            MultiLanguageTextProcessor.getBestMatch(
                                Locale.ENGLISH, messageBuilder.text
                            ), Locale.ENGLISH
                        ).isNotEmpty()
                }
                .filterNot { (_, exist) -> exist }
                .forEach { (message, _) ->
                    Registries.getMessageRegistry()
                        .registerUserMessageAuthenticated(message, token)
                        .get(5, TimeUnit.SECONDS)
                }
        } catch (e: CouldNotPerformException) {
            ExceptionPrinter.printHistory("Could not check device states!", e, logger)
        }
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(TemplateApp::class.java)
        
        private val VALIDATION_PERIOD: Duration = Duration.ofHours(24)
        private val INITIAL_VALIDATION_DELAY: Duration = Duration.ofHours(1)
    }
}
