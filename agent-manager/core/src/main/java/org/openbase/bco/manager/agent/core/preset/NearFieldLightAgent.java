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
import java.util.List;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.openbase.bco.dal.remote.unit.Units;
import org.openbase.bco.dal.remote.unit.connection.ConnectionRemote;
import org.openbase.bco.dal.remote.unit.location.LocationRemote;
import org.openbase.bco.manager.agent.core.AbstractAgentController;
import org.openbase.bco.manager.agent.core.TriggerDAL.NeighborConnectionPresenceTrigger;
import org.openbase.bco.manager.agent.core.TriggerJUL.GenericTrigger;
import org.openbase.bco.manager.agent.core.TriggerJUL.TriggerPool;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InitializationException;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.pattern.Observable;
import org.openbase.jul.pattern.Observer;
import rst.domotic.action.ActionFutureType.ActionFuture;
import rst.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import rst.domotic.state.ActivationStateType.ActivationState;
import rst.domotic.state.BrightnessStateType.BrightnessState;
import rst.domotic.state.PresenceStateType.PresenceState;
import rst.domotic.unit.UnitConfigType;
import rst.domotic.unit.connection.ConnectionConfigType.ConnectionConfig.ConnectionType;
import rst.domotic.unit.location.LocationDataType.LocationData;

/**
 *
 * @author <a href="mailto:tmichalski@techfak.uni-bielefeld.de">Timo Michalski</a>
 */
public class NearFieldLightAgent extends AbstractAgentController {

    private LocationRemote locationRemote;
    private Future<ActionFuture> setBrightnessStateFuture;
    private boolean isDimmed = false;
    private List<LocationRemote> neighborRemotes;
    private final Observer<ActivationState> triggerHolderObserver;

    public NearFieldLightAgent() throws InstantiationException {
        super(NearFieldLightAgent.class);

        triggerHolderObserver = (Observable<ActivationState> source, ActivationState data) -> {
            if (data.getValue().equals(ActivationState.State.ACTIVE)) {
                dimmLights();
            } else if (setBrightnessStateFuture != null) {
                setBrightnessStateFuture.cancel(true);
                isDimmed = false;
            }
        };
    }

    @Override
    public void init(final UnitConfigType.UnitConfig config) throws InitializationException, InterruptedException {
        try {
            super.init(config);
            locationRemote = Units.getUnit(getConfig().getPlacementConfig().getLocationId(), true, Units.LOCATION);
            neighborRemotes = locationRemote.getNeighborLocationList(true);
        } catch (CouldNotPerformException ex) {
            throw new InitializationException("LocationRemote not available", ex);
        }

        try {
            for (LocationRemote neigborRemote : neighborRemotes) {
                if (locationRemote.hasDirectConnection(neigborRemote.getId(), ConnectionType.PASSAGE, true)) {
                    GenericTrigger<LocationRemote, LocationData, PresenceState.State> trigger = new GenericTrigger<>(neigborRemote, PresenceState.State.PRESENT, ServiceType.PRESENCE_STATE_SERVICE);
                    agentTriggerHolder.addTrigger(trigger, TriggerPool.TriggerOperation.OR);
                } else {
                    for (ConnectionRemote relatedConnection : locationRemote.getDirectConnectionList(neigborRemote.getId(), true)) {
                        NeighborConnectionPresenceTrigger trigger = new NeighborConnectionPresenceTrigger(neigborRemote, relatedConnection);
                        agentTriggerHolder.addTrigger(trigger, TriggerPool.TriggerOperation.OR);
                    }
                }
            }
        } catch (CouldNotPerformException ex) {
            throw new InitializationException("Could not initialize trigger", ex);
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

    private void dimmLights() {
        if (isDimmed) {
            return;
        }
        try {
            // Blocking and trying to realloc all lights
            setBrightnessStateFuture = locationRemote.setBrightnessState(BrightnessState.newBuilder().setBrightness(0.5).build());
        } catch (CouldNotPerformException ex) {
            Logger.getLogger(NearFieldLightAgent.class.getName()).log(Level.SEVERE, null, ex);
        }
        isDimmed = true;
    }
}
