package bco.openbase.app.preset.agent;

/*-
 * #%L
 * BCO App Preset
 * %%
 * Copyright (C) 2018 openbase.org
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
import org.openbase.jul.extension.rst.processing.LabelProcessor;
import org.openbase.jul.pattern.Observer;
import org.openbase.jul.pattern.trigger.Trigger;
import org.openbase.jul.pattern.trigger.TriggerPool;
import org.openbase.jul.pattern.trigger.TriggerPool.TriggerAggregation;
import org.openbase.jul.schedule.GlobalCachedExecutorService;
import rst.domotic.state.ActivationStateType.ActivationState;

import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;

/**
 *
 * @author <a href="mailto:thuxohl@techfak.uni-bielefeld.de">Tamino Huxohl</a>
 */
public abstract class AbstractTriggerableAgent extends AbstractAgentController {

    private TriggerPool agentTriggerHolder;
    private Observer<Trigger, ActivationState> triggerHolderObserver;

    public AbstractTriggerableAgent(final Class unitClass) throws InstantiationException {
        super(unitClass);
        this.agentTriggerHolder = new TriggerPool();
        this.triggerHolderObserver = (Trigger source, ActivationState data) -> {
            GlobalCachedExecutorService.submit(() -> {
                try {
                    trigger(data);
                } catch (CouldNotPerformException | CancellationException ex) {
                    ExceptionPrinter.printHistory("Could no trigger agent!", ex, logger);
                }
                return null;
            });
        };
    }

    @Override
    protected void postInit() throws InitializationException, InterruptedException {
        super.postInit();
        agentTriggerHolder.addObserver(triggerHolderObserver);
    }

    public void registerTrigger(Trigger trigger, TriggerAggregation aggregation) throws CouldNotPerformException{
        try {
            agentTriggerHolder.addTrigger(trigger, aggregation);
        } catch (CouldNotPerformException ex) {
            throw new InitializationException("Could not add agent to agent pool", ex);
        }
    }

    @Override
    protected void execute(final ActivationState activationState) throws CouldNotPerformException, InterruptedException {
        logger.info("Activating [" + LabelProcessor.getBestMatch(getConfig().getLabel()) + "]");
        
        // do not activate agents that need the resource allocation to work properly if the resource allocation is turned off
        try {
            if (!JPService.getProperty(JPUnitAllocation.class).getValue()) {
                logger.info("Skip activation of agent [" + LabelProcessor.getBestMatch(getConfig().getLabel()) + "] because unit allocation is disabled.");
                return;
            }
        } catch (JPNotAvailableException ex) {
            throw new CouldNotPerformException("Could not access JPResourceAllocation property", ex);
        }
        agentTriggerHolder.activate();
    }

    @Override
    protected void stop(final ActivationState activationState) throws CouldNotPerformException, InterruptedException {
        logger.info("Deactivating [" + LabelProcessor.getBestMatch(getConfig().getLabel()) + "]");
        agentTriggerHolder.deactivate();
    }

    @Override
    public void shutdown() {
        agentTriggerHolder.removeObserver(triggerHolderObserver);
        agentTriggerHolder.shutdown();
        super.shutdown();
    }

    abstract void trigger(final ActivationState activationState) throws CouldNotPerformException, ExecutionException, InterruptedException;
}
