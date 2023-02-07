package org.openbase.bco.app.preset.agent

import com.google.protobuf.Message
import org.openbase.bco.dal.control.layer.unit.agent.AbstractAgentController
import org.openbase.bco.dal.lib.layer.service.ServiceStateProvider
import org.openbase.jul.exception.CouldNotPerformException
import org.openbase.jul.exception.ExceptionProcessor.isCausedBySystemShutdown
import org.openbase.jul.exception.InitializationException
import org.openbase.jul.exception.printer.ExceptionPrinter
import org.openbase.jul.exception.printer.LogLevel
import org.openbase.jul.extension.type.processing.LabelProcessor.getBestMatch
import org.openbase.jul.pattern.Observer
import org.openbase.jul.pattern.trigger.Trigger
import org.openbase.jul.pattern.trigger.TriggerPool
import org.openbase.jul.pattern.trigger.TriggerPool.TriggerAggregation
import org.openbase.jul.pattern.trigger.TriggerPriority
import org.openbase.jul.schedule.GlobalCachedExecutorService
import org.openbase.jul.schedule.RecurrenceEventFilter
import org.openbase.jul.schedule.SyncObject
import org.openbase.type.domotic.action.ActionDescriptionType
import org.openbase.type.domotic.service.ServiceTemplateType
import org.openbase.type.domotic.service.ServiceTempusTypeType
import org.openbase.type.domotic.state.ActivationStateType.ActivationState
import java.util.concurrent.CancellationException
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

/*-
 * #%L
 * BCO App Preset
 * %%
 * Copyright (C) 2018 - 2021 openbase.org
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */ /**
 * @author [Tamino Huxohl](mailto:thuxohl@techfak.uni-bielefeld.de)
 */
abstract class AbstractTriggerableAgent : AbstractAgentController() {
    private val activationTriggerPool: TriggerPool
    private val deactivationTriggerPool: TriggerPool
    private val activationTriggerPoolObserver: Observer<Trigger, ActivationState>
    private val deactivationTriggerPoolObserver: Observer<Trigger, ActivationState>
    private val triggerSync = SyncObject("TriggerSync")
    private var currentTriggerActivationState: ActivationState.State
    private val parentLocationEmphasisRescheduleEventFilter: RecurrenceEventFilter<Void>
    private val emphasisStateObserver: Observer<ServiceStateProvider<Message?>, Message>

    init {
        currentTriggerActivationState = ActivationState.State.UNKNOWN
        activationTriggerPool = TriggerPool()
        deactivationTriggerPool = TriggerPool()

        // used to make sure reschedule is triggered when the emphasis state of the parent location has been changed.
        parentLocationEmphasisRescheduleEventFilter =
            object : RecurrenceEventFilter<Void>(TimeUnit.SECONDS.toMillis(5)) {
                override fun relay() {
                    try {
                        activationTriggerPool.forceNotification()
                    } catch (ex: CouldNotPerformException) {
                        ExceptionPrinter.printHistory("Could not notify agent about emphasis state change.", ex, logger)
                    }
                }
            }
        emphasisStateObserver =
            Observer { source: ServiceStateProvider<Message?>?, data: Message? -> parentLocationEmphasisRescheduleEventFilter.trigger() }
        activationTriggerPoolObserver = Observer { source: Trigger?, data: ActivationState ->
            logger.debug("activationTriggerPoolObserver current " + currentTriggerActivationState.name + " trigger: " + data.value.name)
            synchronized(triggerSync) {
                try {
                    //triggerInternal(data);
                    when (currentTriggerActivationState) {
                        ActivationState.State.ACTIVE -> {

                            // do not handle activation update when deactivation trigger are registered.
                            if (!deactivationTriggerPool.isEmpty) {
                                return@Observer
                            }
                            if (data.value == ActivationState.State.INACTIVE) {
                                triggerInternal(data)
                            }
                        }

                        ActivationState.State.INACTIVE -> if (data.value == ActivationState.State.ACTIVE) {
                            triggerInternal(data)
                        }

                        ActivationState.State.UNKNOWN -> triggerInternal(data)
                        else -> {}
                    }
                } catch (ex: CancellationException) {
                    ExceptionPrinter.printHistory("Could not trigger agent!", ex, logger)
                }
            }
        }
        deactivationTriggerPoolObserver = Observer { source: Trigger?, data: ActivationState ->
            logger.info("deactivationTriggerPoolObserver current " + currentTriggerActivationState.name + " trigger: " + data.value.name)
            synchronized(triggerSync) {
                try {
                    // deactivate agent if agent is active and deactivation pool is triggering an active state.
                    when (currentTriggerActivationState) {
                        ActivationState.State.ACTIVE, ActivationState.State.UNKNOWN ->                             // if the deactivation pool is active we need to send a deactivation trigger
                            if (data.value == ActivationState.State.ACTIVE) {
                                triggerInternal(
                                    data.toBuilder().setValue(ActivationState.State.INACTIVE)
                                        .build()
                                )
                            }

                        else -> {}
                    }
                } catch (ex: CancellationException) {
                    ExceptionPrinter.printHistory("Could not trigger agent!", ex, logger)
                }
            }
        }
    }

    @Throws(InitializationException::class, InterruptedException::class)
    override fun postInit() {
        super.postInit()
        activationTriggerPool.addObserver(activationTriggerPoolObserver)
        deactivationTriggerPool.addObserver(deactivationTriggerPoolObserver)
    }

    /**
     * Method registers a new trigger which can activate the agent.
     * In case no deactivation trigger are registered, the activation trigger can also cause a deactivation of the agent.
     *
     * @param trigger     the trigger to register.
     * @param aggregation used to
     *
     * @throws CouldNotPerformException
     */
    @Throws(CouldNotPerformException::class)
    fun registerActivationTrigger(trigger: Trigger, aggregation: TriggerAggregation?) {
        try {
            /*
             * We need to take care that agents that schedule new actions are prioritized
             * in contrast to the one which cancel their actions.
             */
            trigger.priority = TriggerPriority.HIGH
            activationTriggerPool.addTrigger(trigger, aggregation!!)
        } catch (ex: CouldNotPerformException) {
            throw InitializationException("Could not add agent to agent pool", ex)
        }
    }

    @Throws(CouldNotPerformException::class)
    fun registerDeactivationTrigger(trigger: Trigger, aggregation: TriggerAggregation?) {
        try {
            /*
             * We need to take care that agents that cancel their actions are handled with low priority
             * to avoid termination actions to get accidentally activated.
             */
            trigger.priority = TriggerPriority.LOW
            deactivationTriggerPool.addTrigger(trigger, aggregation!!)
        } catch (ex: CouldNotPerformException) {
            throw InitializationException("Could not add agent to agent pool", ex)
        }
    }

    @Throws(CouldNotPerformException::class, InterruptedException::class)
    override fun execute(activationState: ActivationState): ActionDescriptionType.ActionDescription {
        logger.debug("Activating [{}]", getBestMatch(config.label))
        activationTriggerPool.activate()
        deactivationTriggerPool.activate()

        // register emphasis state observer on location
        getParentLocationRemote(false).addServiceStateObserver(
            ServiceTempusTypeType.ServiceTempusType.ServiceTempus.CURRENT,
            ServiceTemplateType.ServiceTemplate.ServiceType.EMPHASIS_STATE_SERVICE,
            emphasisStateObserver
        )
        return activationState.responsibleAction
    }

    @Throws(CouldNotPerformException::class, InterruptedException::class)
    override fun stop(activationState: ActivationState) {
        logger.debug("Deactivating [{}]", getBestMatch(config.label))
        activationTriggerPool.deactivate()
        deactivationTriggerPool.deactivate()

        // deregister emphasis state observer
        try {
            getParentLocationRemote(false).removeServiceStateObserver(
                ServiceTempusTypeType.ServiceTempusType.ServiceTempus.CURRENT,
                ServiceTemplateType.ServiceTemplate.ServiceType.EMPHASIS_STATE_SERVICE,
                emphasisStateObserver
            )
        } catch (ex: CouldNotPerformException) {
            if (!isCausedBySystemShutdown(ex)) {
                ExceptionPrinter.printHistory(
                    "Could not deregister parent location observation!",
                    ex,
                    logger,
                    LogLevel.WARN
                )
            }
        }
        super.stop(activationState)
    }

    override fun shutdown() {
        activationTriggerPool.removeObserver(activationTriggerPoolObserver)
        deactivationTriggerPool.removeObserver(deactivationTriggerPoolObserver)
        activationTriggerPool.shutdown()
        deactivationTriggerPool.shutdown()
        super.shutdown()
    }

    private fun triggerInternal(activationState: ActivationState) {
        currentTriggerActivationState = activationState.value
        GlobalCachedExecutorService.submit<Any> {
            synchronized(triggerSync) {
                trigger(activationState)
                return@submit null
            }
        }
    }

    @Throws(
        CouldNotPerformException::class,
        ExecutionException::class,
        InterruptedException::class,
        TimeoutException::class
    )
    protected abstract fun trigger(activationState: ActivationState)
}
