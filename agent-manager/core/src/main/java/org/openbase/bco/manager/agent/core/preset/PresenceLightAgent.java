package org.openbase.bco.manager.agent.core.preset;

/*
 * #%L
 * BCO Manager Agent Core
 * %%
 * Copyright (C) 2015 - 2017 openbase.org
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
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.openbase.bco.dal.remote.unit.Units;
import org.openbase.bco.dal.remote.unit.location.LocationRemote;
import org.openbase.bco.manager.agent.core.AbstractAgentController;
import org.openbase.bco.manager.agent.core.TriggerDAL.AgentTriggerPool;
import org.openbase.bco.manager.agent.core.TriggerJUL.GenericTrigger;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InitializationException;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.pattern.Observable;
import org.openbase.jul.pattern.Observer;
import rst.domotic.action.ActionFutureType.ActionFuture;
import rst.domotic.service.ServiceTemplateType;
import rst.domotic.state.ActivationStateType.ActivationState;
import rst.domotic.state.PowerStateType.PowerState;
import rst.domotic.state.PresenceStateType.PresenceState;
import rst.domotic.unit.UnitConfigType;
import rst.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;
import rst.domotic.unit.location.LocationDataType.LocationData;

/**
 *
 * @author <a href="mailto:tmichalski@techfak.uni-bielefeld.de">Timo Michalski</a>
 */
public class PresenceLightAgent extends AbstractAgentController {

    private LocationRemote locationRemote;
    private Future<ActionFuture> setPowerStateFuture;
    private final PresenceState.State triggerState = PresenceState.State.PRESENT;
    private final Observer<ActivationState> triggerHolderObserver;

    public PresenceLightAgent() throws InstantiationException {
        super(PresenceLightAgent.class);

        triggerHolderObserver = (Observable<ActivationState> source, ActivationState data) -> {
            if (data.getValue().equals(ActivationState.State.ACTIVE)) {
                switchlightsOn();
            } else {
                if (setPowerStateFuture != null) {
                    setPowerStateFuture.cancel(true);
                }
            }
        };
    }

    @Override
    public void init(final UnitConfigType.UnitConfig config) throws InitializationException, InterruptedException {
        super.init(config);

        try {
            locationRemote = Units.getUnit(getConfig().getPlacementConfig().getLocationId(), true, Units.LOCATION);
        } catch (NotAvailableException ex) {
            throw new InitializationException("LocationRemote not available.", ex);
        }

        try {
            GenericTrigger<LocationRemote, LocationData, PresenceState.State> agentTrigger = new GenericTrigger(locationRemote, triggerState, ServiceTemplateType.ServiceTemplate.ServiceType.PRESENCE_STATE_SERVICE);
            agentTriggerHolder.addTrigger(agentTrigger, AgentTriggerPool.TriggerOperation.OR);
        } catch (CouldNotPerformException ex) {
            throw new InitializationException("Could not add agent to agentpool", ex);
        }

        agentTriggerHolder.registerObserver(triggerHolderObserver);
    }

    @Override
    protected void execute() throws CouldNotPerformException, InterruptedException {
        logger.info("Activating [" + getConfig().getLabel() + "]");
        agentTriggerHolder.activate();
    }

    @Override
    protected void stop() throws CouldNotPerformException, InterruptedException {
        logger.info("Deactivating [" + getConfig().getLabel() + "]");
        agentTriggerHolder.deactivate();
    }

    @Override
    public void shutdown() {
        agentTriggerHolder.deregisterObserver(triggerHolderObserver);
        agentTriggerHolder.shutdown();
        super.shutdown();
    }

    private void switchlightsOn() {
        try {
            setPowerStateFuture = locationRemote.setPowerState(PowerState.newBuilder().setValue(PowerState.State.ON).build(), UnitType.LIGHT);
            // TODO: Blocking setPowerState function that is trying to realloc all lights as long as jobs not cancelled. 
            // TODO: Maybe also set Color and Brightness?
        } catch (CouldNotPerformException ex) {
            Logger.getLogger(PresenceLightAgent.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
