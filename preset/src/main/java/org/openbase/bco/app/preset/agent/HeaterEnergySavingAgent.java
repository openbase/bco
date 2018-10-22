package org.openbase.bco.app.preset.agent;

/*
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
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import org.openbase.bco.dal.remote.layer.unit.TemperatureControllerRemote;
import org.openbase.bco.dal.remote.layer.unit.Units;
import org.openbase.bco.dal.remote.layer.unit.connection.ConnectionRemote;
import org.openbase.bco.dal.remote.layer.unit.location.LocationRemote;
import org.openbase.bco.dal.remote.trigger.GenericBCOTrigger;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InitializationException;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.pattern.trigger.TriggerPool.TriggerAggregation;
import rst.domotic.action.ActionDescriptionType.ActionDescription;
import rst.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import rst.domotic.state.ActivationStateType.ActivationState;
import rst.domotic.state.TemperatureStateType.TemperatureState;
import rst.domotic.state.WindowStateType.WindowState;
import rst.domotic.unit.UnitConfigType.UnitConfig;
import rst.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;
import rst.domotic.unit.connection.ConnectionConfigType.ConnectionConfig.ConnectionType;

/**
 *
 * @author <a href="mailto:tmichalski@techfak.uni-bielefeld.de">Timo Michalski</a>
 */
public class HeaterEnergySavingAgent extends AbstractTriggerableAgent {

    private LocationRemote locationRemote;
    private final Map<TemperatureControllerRemote, TemperatureState> previousTemperatureState;
    private final WindowState.State triggerState = WindowState.State.OPEN;
    private ActionDescription taskActionDescription;

    public HeaterEnergySavingAgent() throws InstantiationException {
        super(HeaterEnergySavingAgent.class);

        previousTemperatureState = new HashMap();
    }

    @Override
    public void init(final UnitConfig config) throws InitializationException, InterruptedException {
        super.init(config);
        try {
            try {
                locationRemote = Units.getUnit(getConfig().getPlacementConfig().getLocationId(), false, Units.LOCATION);
            } catch (NotAvailableException ex) {
                throw new InitializationException("LocationRemote not available.", ex);
            }
            for (ConnectionRemote connectionRemote : locationRemote.getConnectionList(false)) {
                if (connectionRemote.getConfig().getConnectionConfig().getType().equals(ConnectionType.WINDOW)) {
                    try {
                        registerTrigger(new GenericBCOTrigger(connectionRemote, triggerState, ServiceType.WINDOW_STATE_SERVICE), TriggerAggregation.OR);
                    } catch (CouldNotPerformException ex) {
                        throw new InitializationException("Could not add agent to agentpool", ex);
                    }
                }
            }
        } catch (CouldNotPerformException ex) {
            throw new InitializationException("Could not initialize Agent.", ex);
        }
    }

    /**
     * For all TemperatureControllerRemotes available in location, save the current targetTemperatureState for later restore.
     * Set the targetTemperature for whole location to 13.0.
     *
     * @throws CouldNotPerformException
     * @throws ExecutionException
     * @throws InterruptedException
     */
    private void regulateHeater() throws CouldNotPerformException, ExecutionException, InterruptedException{
        previousTemperatureState.clear();
        try {
            for (TemperatureControllerRemote remote : locationRemote.getUnits(UnitType.TEMPERATURE_CONTROLLER, true, Units.TEMPERATURE_CONTROLLER)) {
                previousTemperatureState.put(remote, remote.getTargetTemperatureState());
            }
        } catch (CouldNotPerformException | InterruptedException ex) {
            logger.error("Could not get all TemperatureControllerRemotes.", ex);
        }
        taskActionDescription = locationRemote.applyAction(generateAction(UnitType.TEMPERATURE_CONTROLLER, ServiceType.TEMPERATURE_STATE_SERVICE, TemperatureState.newBuilder().setTemperature(13.0)).toBuilder().setExecutionTimePeriod(Long.MAX_VALUE).build()).get();
    }

    /**
     * Try to restore all the saved targetTemperature values.
     * It will only try to restore it once if possible, without placing a schedulable interruptible action.
     *
     * @throws CouldNotPerformException
     * @throws ExecutionException
     * @throws InterruptedException
     */
    private void restoreTemperatureState() throws CouldNotPerformException, ExecutionException, InterruptedException{
        if(taskActionDescription != null) {
            taskActionDescription = locationRemote.cancelAction(taskActionDescription).get();
        }
        if (previousTemperatureState == null | previousTemperatureState.isEmpty()) {
            return;
        }
        previousTemperatureState.forEach((remote, temperatureState) -> {
            try {
                ActionDescription actionDescription = generateAction(UnitType.TEMPERATURE_CONTROLLER, ServiceType.TEMPERATURE_STATE_SERVICE, TemperatureState.newBuilder().setTemperature(temperatureState.getTemperature()));
                remote.applyAction(actionDescription.toBuilder().setInterruptible(false).setSchedulable(false).setExecutionTimePeriod(0).build()).get();
            } catch (CouldNotPerformException | InterruptedException | ExecutionException ex) {
                logger.error("Could not restore targetTemperatureState.", ex);
            }
        });
        previousTemperatureState.clear();
    }

    @Override
    void trigger(ActivationState activationState) throws CouldNotPerformException, ExecutionException, InterruptedException {
        switch (activationState.getValue()) {
            case ACTIVE:
                regulateHeater();
                break;
            case DEACTIVE:
                restoreTemperatureState();
                break;
        }
    }
}
