package org.openbase.bco.dal.control.layer.unit

import com.google.protobuf.AbstractMessage
import org.openbase.bco.dal.lib.action.ActionDescriptionProcessor
import org.openbase.bco.dal.lib.layer.service.ServiceProvider
import org.openbase.bco.dal.lib.layer.service.operation.ActivationStateOperationService
import org.openbase.bco.dal.lib.layer.service.provider.ActivationStateProviderService
import org.openbase.bco.dal.lib.state.States
import org.openbase.bco.dal.remote.action.RemoteAction
import org.openbase.jul.exception.CouldNotPerformException
import org.openbase.jul.exception.InstantiationException
import org.openbase.jul.exception.printer.ExceptionPrinter
import org.openbase.jul.exception.printer.LogLevel
import org.openbase.jul.exception.tryOrNull
import org.openbase.jul.iface.TimedProcessable
import org.openbase.jul.schedule.FutureProcessor
import org.openbase.type.domotic.action.ActionDescriptionType.ActionDescription
import org.openbase.type.domotic.action.ActionInitiatorType.ActionInitiator.InitiatorType
import org.openbase.type.domotic.action.ActionPriorityType
import org.openbase.type.domotic.service.ServiceTemplateType
import org.openbase.type.domotic.state.ActivationStateType.ActivationState
import org.openbase.type.domotic.unit.UnitConfigType.UnitConfig
import java.io.Serializable
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit

/**
 * @param <D>  the data type of this unit used for the state synchronization.
 * @param <DB> the builder used to build the unit data instance.
 *
 * @author [Divine Threepwood](mailto:divine@openbase.org)
</DB></D> */
abstract class AbstractExecutableBaseUnitController<D, DB : AbstractMessage.Builder<DB>>(builder: DB) :
    AbstractBaseUnitController<D, DB>(builder),
    ActivationStateProviderService where D : AbstractMessage?, D : Serializable? {
    init {
        try {
            registerOperationService(
                ServiceTemplateType.ServiceTemplate.ServiceType.ACTIVATION_STATE_SERVICE,
                ActivationStateOperationServiceImpl(
                    this
                )
            )
        } catch (ex: CouldNotPerformException) {
            throw InstantiationException(this, ex)
        }
    }

    @Throws(CouldNotPerformException::class, InterruptedException::class)
    override fun activate() {
        super.activate()
        handleAutostart()
    }

    @Throws(CouldNotPerformException::class, InterruptedException::class)
    override fun deactivate() {
        cancelAllActions()
        super.deactivate()
    }

    @Throws(CouldNotPerformException::class, InterruptedException::class)
    override fun applyConfigUpdate(config: UnitConfig): UnitConfig {
        getManageWriteLockInterruptible(this).use { _ ->
            val originalConfig = tryOrNull { getConfig() }
            return super.applyConfigUpdate(config).also { updatedConfig ->
                originalConfig?.let {
                    if (isAutostartEnabled(it) != isAutostartEnabled(updatedConfig)) {
                        handleAutostart()
                    }
                }
            }
        }
    }

    private fun handleAutostart() {
        if (!isAutostartEnabled()) {
            return
        }

        try {
            val actionParameter = ActionDescriptionProcessor.generateDefaultActionParameter(
                States.Activation.ACTIVE,
                ServiceTemplateType.ServiceTemplate.ServiceType.ACTIVATION_STATE_SERVICE,
                this
            )
            actionParameter.setInterruptible(true)
            actionParameter.setSchedulable(true)
            actionParameter.setPriority(ActionPriorityType.ActionPriority.Priority.NO)
            actionParameter.actionInitiatorBuilder.setInitiatorType(InitiatorType.SYSTEM)
            actionParameter.setExecutionTimePeriod(TimeUnit.MILLISECONDS.toMicros(TimedProcessable.INFINITY_TIMEOUT))
            RemoteAction(applyAction(actionParameter)) { this.isActive }
        } catch (ex: CouldNotPerformException) {
            ExceptionPrinter.printHistory("Could not autostart $this", ex, logger, LogLevel.ERROR)
        }
    }

    protected abstract fun isAutostartEnabled(config: UnitConfig? = tryOrNull { getConfig() }): Boolean

    @Throws(CouldNotPerformException::class, InterruptedException::class)
    protected abstract fun execute(activationState: ActivationState): ActionDescription?

    @Throws(CouldNotPerformException::class, InterruptedException::class)
    protected abstract fun stop(activationState: ActivationState)

    inner class ActivationStateOperationServiceImpl(private val serviceProvider: ServiceProvider<*>) :
        ActivationStateOperationService {
        override fun setActivationState(activationState: ActivationState): Future<ActionDescription> {
            try {
                when (activationState.value) {
                    ActivationState.State.ACTIVE -> {
                        applyServiceState(
                            activationState,
                            ServiceTemplateType.ServiceTemplate.ServiceType.ACTIVATION_STATE_SERVICE
                        )
                        execute(activationState)
                    }

                    ActivationState.State.INACTIVE, ActivationState.State.UNKNOWN -> {
                        stop(activationState)
                        applyServiceState(
                            activationState,
                            ServiceTemplateType.ServiceTemplate.ServiceType.ACTIVATION_STATE_SERVICE
                        )
                    }

                    else -> {
                        throw CouldNotPerformException("Unsupported activation state: ${activationState.value}")
                    }
                }
                return FutureProcessor.completedFuture(activationState.responsibleAction)
            } catch (ex: CouldNotPerformException) {
                return FutureProcessor.canceledFuture(
                    ActionDescription::class.java, ex
                )
            } catch (ex: InterruptedException) {
                return FutureProcessor.canceledFuture(
                    ActionDescription::class.java, ex
                )
            }
        }

        override fun getServiceProvider(): ServiceProvider<*> {
            return serviceProvider
        }
    }
}
