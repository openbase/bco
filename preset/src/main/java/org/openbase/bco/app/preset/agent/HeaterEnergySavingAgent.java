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

import java.util.concurrent.ExecutionException;

/**
 *
 * @author <a href="mailto:tmichalski@techfak.uni-bielefeld.de">Timo Michalski</a>
 */
public class HeaterEnergySavingAgent extends AbstractTriggerableAgent {

    private LocationRemote locationRemote;
    private final WindowState.State triggerState = WindowState.State.OPEN;
    private ActionDescription taskActionDescription;
    // TODO: read desired energySavingTemperature from config ?
    private final double energySavingTemperature = 13.0;


    public HeaterEnergySavingAgent() throws InstantiationException {
        super(HeaterEnergySavingAgent.class);
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
            throw new InitializationException(this, ex);
        }
    }

    @Override
    void trigger(ActivationState activationState) throws CouldNotPerformException, ExecutionException, InterruptedException {
        switch (activationState.getValue()) {
            case ACTIVE:
                taskActionDescription = locationRemote.applyAction(generateAction(UnitType.TEMPERATURE_CONTROLLER,
                        ServiceType.TEMPERATURE_STATE_SERVICE,
                        TemperatureState.newBuilder().setTemperature(energySavingTemperature)).toBuilder().setExecutionTimePeriod(Long.MAX_VALUE).build()).get();
                break;
            case DEACTIVE:
                if(taskActionDescription != null) {
                    taskActionDescription = locationRemote.cancelAction(taskActionDescription).get();
                }
                break;
        }
    }
}
