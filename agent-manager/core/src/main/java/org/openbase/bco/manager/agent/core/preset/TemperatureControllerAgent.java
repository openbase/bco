package org.openbase.bco.manager.agent.core.preset;

/*-
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
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Future;
import org.openbase.bco.dal.remote.unit.TemperatureControllerRemote;
import org.openbase.bco.dal.remote.unit.Units;
import org.openbase.bco.dal.remote.unit.location.LocationRemote;
import org.openbase.bco.manager.agent.core.AbstractAgentController;
import org.openbase.bco.manager.agent.core.TriggerDAL.AgentTriggerPool;
import org.openbase.bco.manager.agent.core.TriggerJUL.GenericTrigger;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InitializationException;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.extension.rst.processing.TimestampProcessor;
import org.openbase.jul.pattern.Observable;
import org.openbase.jul.pattern.Observer;
import rst.domotic.action.ActionFutureType.ActionFuture;
import rst.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import rst.domotic.state.ActivationStateType.ActivationState;
import rst.domotic.state.PresenceStateType.PresenceState;
import rst.domotic.state.TemperatureStateType.TemperatureState;
import rst.domotic.unit.UnitConfigType;
import rst.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;
import rst.domotic.unit.location.LocationDataType;

/**
 *
 * @author <a href="mailto:tmichalski@techfak.uni-bielefeld.de">Timo Michalski</a>
 */
public class TemperatureControllerAgent extends AbstractAgentController {

    private LocationRemote locationRemote;
    private Future<ActionFuture> regulateTemperatureFuture;
    private final Map<TemperatureControllerRemote, TemperatureState> previousTemperatureState;
    private final PresenceState.State triggerState = PresenceState.State.ABSENT;
    private final Observer<ActivationState> triggerHolderObserver;

    public TemperatureControllerAgent() throws InstantiationException {
        super(TemperatureControllerAgent.class);

        previousTemperatureState = new HashMap();

        triggerHolderObserver = (Observable<ActivationState> source, ActivationState data) -> {
            if (data.getValue().equals(ActivationState.State.ACTIVE)) {
                regulateTemperature();
            } else if (regulateTemperatureFuture != null) {
                regulateTemperatureFuture.cancel(true);
                restoreTemperature();
            }
        };
    }

    @Override
    public void init(final UnitConfigType.UnitConfig config) throws InitializationException, InterruptedException {
        try {
            super.init(config);

            try {
                locationRemote = Units.getUnit(getConfig().getPlacementConfig().getLocationId(), true, Units.LOCATION);
            } catch (NotAvailableException ex) {
                throw new InitializationException("LocationRemote not available.", ex);
            }

            try {
                GenericTrigger<LocationRemote, LocationDataType.LocationData, PresenceState.State> agentTrigger = new GenericTrigger(locationRemote, triggerState, ServiceType.PRESENCE_STATE_SERVICE);
                agentTriggerHolder.addTrigger(agentTrigger, AgentTriggerPool.TriggerOperation.OR);
            } catch (CouldNotPerformException ex) {
                throw new InitializationException("Could not add agent to agentpool", ex);
            }

            agentTriggerHolder.registerObserver(triggerHolderObserver);
        } catch (CouldNotPerformException ex) {
            throw new InitializationException("Could not initialize Agent.", ex);
        }
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

    private void regulateTemperature() {
        previousTemperatureState.clear();
        try {
            for (TemperatureControllerRemote remote : locationRemote.getUnits(UnitType.TEMPERATURE_CONTROLLER, true, Units.TEMPERATURE_CONTROLLER)) {
                previousTemperatureState.put(remote, remote.getTargetTemperatureState());

            }
        } catch (CouldNotPerformException | InterruptedException ex) {
            logger.error("Could not get all TemperatureControllerRemotes.", ex);
        }

        TemperatureState temperatureState;
        try {
            temperatureState = TimestampProcessor.updateTimestampWithCurrentTime(TemperatureState.newBuilder().setTemperature(13.0).build());
            regulateTemperatureFuture = locationRemote.setTargetTemperatureState(temperatureState, UnitType.TEMPERATURE_CONTROLLER);
        } catch (CouldNotPerformException ex) {
            logger.error("Could not set targetTemperatureState.", ex);
        }
    }

    private void restoreTemperature() {
        if (regulateTemperatureFuture != null && !regulateTemperatureFuture.isDone()) {
            regulateTemperatureFuture.cancel(true);
        }

        if (previousTemperatureState == null | previousTemperatureState.isEmpty()) {
            return;
        }

        previousTemperatureState.forEach((remote, temperatureState) -> {
            try {
                remote.setTargetTemperatureState(TimestampProcessor.updateTimestampWithCurrentTime(temperatureState));
            } catch (CouldNotPerformException ex) {
                logger.error("Could not restore targetTemperatureState.", ex);
            }
        });
        previousTemperatureState.clear();
    }
}
