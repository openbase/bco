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
import org.openbase.type.domotic.action.ActionDescriptionType.ActionDescription;
import org.openbase.type.domotic.state.ActivationStateType.ActivationState;

import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;

/**
 *
 * @author <a href="mailto:thuxohl@techfak.uni-bielefeld.de">Tamino Huxohl</a>
 */
public abstract class AbstractTriggerableAgent extends AbstractAgentController {

    private TriggerPool triggerPool;
    private Observer<Trigger, ActivationState> triggerHolderObserver;

    public AbstractTriggerableAgent() throws InstantiationException {
        this.triggerPool = new TriggerPool();
        this.triggerHolderObserver = (Trigger source, ActivationState data) -> {
            GlobalCachedExecutorService.submit(() -> {
                try {
                    trigger(data);
                } catch (CouldNotPerformException | CancellationException ex) {
                    ExceptionPrinter.printHistory("Could not trigger agent!", ex, logger);
                }
                return null;
            });
        };
    }

    @Override
    protected void postInit() throws InitializationException, InterruptedException {
        super.postInit();
        triggerPool.addObserver(triggerHolderObserver);
    }

    public void registerTrigger(Trigger trigger, TriggerAggregation aggregation) throws CouldNotPerformException{
        try {
            triggerPool.addTrigger(trigger, aggregation);
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
        triggerPool.activate();

        return activationState.getResponsibleAction();
    }

    @Override
    protected void stop(final ActivationState activationState) throws CouldNotPerformException, InterruptedException {
        logger.debug("Deactivating [{}]", LabelProcessor.getBestMatch(getConfig().getLabel()));
        triggerPool.deactivate();
        super.stop(activationState);
    }

    @Override
    public void shutdown() {
        triggerPool.removeObserver(triggerHolderObserver);
        triggerPool.shutdown();
        super.shutdown();
    }

    abstract protected void trigger(final ActivationState activationState) throws CouldNotPerformException, ExecutionException, InterruptedException;
}
