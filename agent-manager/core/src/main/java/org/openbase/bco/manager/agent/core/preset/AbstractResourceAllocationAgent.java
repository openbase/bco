package org.openbase.bco.manager.agent.core.preset;

/*-
 * #%L
 * BCO Manager Agent Core
 * %%
 * Copyright (C) 2015 - 2018 openbase.org
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

import org.openbase.bco.dal.lib.jp.JPResourceAllocation;
import org.openbase.bco.manager.agent.core.AbstractAgentController;
import org.openbase.bco.dal.remote.action.ActionRescheduler;
import org.openbase.jps.core.JPService;
import org.openbase.jps.exception.JPNotAvailableException;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InitializationException;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.pattern.Observer;
import rst.domotic.state.ActivationStateType.ActivationState;

/**
 *
 * @author <a href="mailto:thuxohl@techfak.uni-bielefeld.de">Tamino Huxohl</a>
 */
public abstract class AbstractResourceAllocationAgent extends AbstractAgentController {

    protected ActionRescheduler actionRescheduleHelper;
    protected Observer<ActivationState> triggerHolderObserver;

    public AbstractResourceAllocationAgent(final Class unitClass) throws InstantiationException {
        super(unitClass);
    }

    @Override
    protected void postInit() throws InitializationException, InterruptedException {
        super.postInit();
        
        agentTriggerHolder.registerObserver(triggerHolderObserver);
    }

    @Override
    protected void execute() throws CouldNotPerformException, InterruptedException {
        logger.info("Activating [" + getConfig().getLabel() + "]");
        
        // do not activate agents that need the resource allocation to work properly if the resource allocation is turned off
        try {
            if (!JPService.getProperty(JPResourceAllocation.class).getValue()) {
                logger.info("Skip activatio of agent [" + getConfig().getLabel() + "] because resource allocation is disabled");
                return;
            }
        } catch (JPNotAvailableException ex) {
            throw new CouldNotPerformException("Could not access JPResourceAllocation property", ex);
        }
        
        agentTriggerHolder.activate();
    }

    @Override
    protected void stop() throws CouldNotPerformException, InterruptedException {
        logger.info("Deactivating [" + getConfig().getLabel() + "]");
        actionRescheduleHelper.stopExecution();
        agentTriggerHolder.deactivate();
    }

    @Override
    public void shutdown() {
        actionRescheduleHelper.stopExecution();
        agentTriggerHolder.deregisterObserver(triggerHolderObserver);
        agentTriggerHolder.shutdown();
        super.shutdown();
    }
}
