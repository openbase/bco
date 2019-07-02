package org.openbase.bco.app.preset.agent;

/*-
 * #%L
 * BCO App Preset
 * %%
 * Copyright (C) 2018 - 2019 openbase.org
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
 */

import org.openbase.bco.dal.lib.jp.JPUnitAllocation;
import org.openbase.bco.dal.control.layer.unit.agent.AbstractAgentController;
import org.openbase.jps.core.JPService;
import org.openbase.jps.exception.JPNotAvailableException;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InitializationException;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.extension.type.processing.LabelProcessor;
import org.openbase.jul.pattern.Observer;
import org.openbase.jul.pattern.trigger.Trigger;
import org.openbase.jul.pattern.trigger.TriggerPool;
import org.openbase.jul.pattern.trigger.TriggerPool.TriggerAggregation;
import org.openbase.jul.schedule.GlobalCachedExecutorService;
import org.openbase.jul.schedule.SyncObject;
import org.openbase.type.domotic.action.ActionDescriptionType.ActionDescription;
import org.openbase.type.domotic.state.ActivationStateType.ActivationState;
import org.openbase.type.domotic.state.ActivationStateType.ActivationState.State;

import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;

/**
 * @author <a href="mailto:thuxohl@techfak.uni-bielefeld.de">Tamino Huxohl</a>
 */
public abstract class AbstractTriggerableAgent extends AbstractAgentController {

    private final TriggerPool activationTriggerPool;
    private final TriggerPool deactivationTriggerPool;

    private final Observer<Trigger, ActivationState> activationTriggerPoolObserver;
    private final Observer<Trigger, ActivationState> deactivationTriggerPoolObserver;

    private final SyncObject triggerSync = new SyncObject("TriggerSync");

    private ActivationState.State currentTriggerActivationState;

    public AbstractTriggerableAgent() throws InstantiationException {

        this.currentTriggerActivationState = State.UNKNOWN;

        this.activationTriggerPool = new TriggerPool();
        this.deactivationTriggerPool = new TriggerPool();

        this.activationTriggerPoolObserver = (Trigger source, ActivationState data) -> {
            System.out.println("activationTriggerPoolObserver current " + currentTriggerActivationState.name() + " trigger: " + data.getValue().name());
            synchronized (triggerSync) {
                try {
                    //triggerInternal(data);
                    switch (currentTriggerActivationState) {
                        // if agent is active and deactivate agent if agent is active and deactivation pool is triggering an active state.
                        case ACTIVE:

                            // do not handle activation update when deactivation trigger are registered.
                            if (!deactivationTriggerPool.isEmpty()) {
                                return;
                            }

                            if (data.getValue() == State.DEACTIVE) {
                                triggerInternal(data);
                            }
                            break;

                        case DEACTIVE:
                            if (data.getValue() == State.ACTIVE) {
                                triggerInternal(data);
                            }
                            break;

                        case UNKNOWN:
                            triggerInternal(data);
                            break;

                        default:
                            // do nothing
                    }
                } catch (CancellationException ex) {
                    ExceptionPrinter.printHistory("Could not trigger agent!", ex, logger);
                }
            }
        };
        this.deactivationTriggerPoolObserver = (Trigger source, ActivationState data) -> {
            System.out.println("deactivationTriggerPoolObserver current " + currentTriggerActivationState.name() + " trigger: " + data.getValue().name());
            synchronized (triggerSync) {
                try {
                    // deactivate agent if agent is active and deactivation pool is triggering an active state.
                    switch (currentTriggerActivationState) {
                        case ACTIVE:
                        case UNKNOWN:
                            // if the deactivation pool is active we need to send a deactivation trigger
                            if (data.getValue() == State.ACTIVE) {
                                triggerInternal(data.toBuilder().setValue(State.DEACTIVE).build());
                            }
                            break;
                        default:
                            // do nothing
                    }
                } catch (CancellationException ex) {
                    ExceptionPrinter.printHistory("Could not trigger agent!", ex, logger);
                }
            }
        };
    }

    @Override
    protected void postInit() throws InitializationException, InterruptedException {
        super.postInit();
        activationTriggerPool.addObserver(activationTriggerPoolObserver);
        deactivationTriggerPool.addObserver(deactivationTriggerPoolObserver);
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
    public void registerActivationTrigger(Trigger trigger, TriggerAggregation aggregation) throws CouldNotPerformException {
        try {
            activationTriggerPool.addTrigger(trigger, aggregation);
        } catch (CouldNotPerformException ex) {
            throw new InitializationException("Could not add agent to agent pool", ex);
        }
    }

    public void registerDeactivationTrigger(Trigger trigger, TriggerAggregation aggregation) throws CouldNotPerformException {
        try {
            deactivationTriggerPool.addTrigger(trigger, aggregation);
        } catch (CouldNotPerformException ex) {
            throw new InitializationException("Could not add agent to agent pool", ex);
        }
    }

    @Override
    protected ActionDescription execute(final ActivationState activationState) throws CouldNotPerformException, InterruptedException {
        logger.debug("Activating [{}]", LabelProcessor.getBestMatch(getConfig().getLabel()));

        // do not activate agents that need the resource allocation to work properly if the resource allocation is turned off
        try {
            if (!JPService.getProperty(JPUnitAllocation.class).getValue()) {
                logger.warn("Skip activation of agent [" + LabelProcessor.getBestMatch(getConfig().getLabel()) + "] because unit allocation is disabled.");
                return activationState.getResponsibleAction();
            }
        } catch (JPNotAvailableException ex) {
            throw new CouldNotPerformException("Could not access JPResourceAllocation property", ex);
        }
        activationTriggerPool.activate();
        deactivationTriggerPool.activate();

        return activationState.getResponsibleAction();
    }

    @Override
    protected void stop(final ActivationState activationState) throws CouldNotPerformException, InterruptedException {
        logger.debug("Deactivating [{}]", LabelProcessor.getBestMatch(getConfig().getLabel()));
        activationTriggerPool.deactivate();
        deactivationTriggerPool.deactivate();
        super.stop(activationState);
    }

    @Override
    public void shutdown() {
        activationTriggerPool.removeObserver(activationTriggerPoolObserver);
        deactivationTriggerPool.removeObserver(deactivationTriggerPoolObserver);
        activationTriggerPool.shutdown();
        deactivationTriggerPool.shutdown();
        super.shutdown();
    }

    private void triggerInternal(final ActivationState activationState) {
        currentTriggerActivationState = activationState.getValue();
        GlobalCachedExecutorService.submit(() -> {
            synchronized (triggerSync) {
                trigger(activationState);
                return null;
            }
        });
    }

    abstract protected void trigger(final ActivationState activationState) throws CouldNotPerformException, ExecutionException, InterruptedException;
}
