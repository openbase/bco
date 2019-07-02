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
import org.openbase.bco.dal.remote.layer.unit.Units;
import org.openbase.bco.dal.remote.layer.unit.location.LocationRemote;
import org.openbase.bco.dal.remote.trigger.GenericServiceStateValueTrigger;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InitializationException;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.pattern.trigger.TriggerPool;
import org.openbase.type.domotic.action.ActionDescriptionType.ActionDescription;
import org.openbase.type.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import org.openbase.type.domotic.state.ActivationStateType;
import org.openbase.type.domotic.state.PresenceStateType.PresenceState;
import org.openbase.type.domotic.state.TemperatureStateType;
import org.openbase.type.domotic.unit.UnitConfigType;
import org.openbase.type.domotic.unit.UnitTemplateType;

import java.util.concurrent.ExecutionException;

public class HeaterComfortAgent extends AbstractTriggerableAgent{

    private LocationRemote locationRemote;
    private final PresenceState.State triggerState = PresenceState.State.PRESENT;
    private ActionDescription taskActionDescription;
    // TODO: read desired comfortTemperature from config ?
    private final double comfortTemperature = 21.0;

    public HeaterComfortAgent() throws InstantiationException {
        super();
    }

    @Override
    public void init(final UnitConfigType.UnitConfig config) throws InitializationException, InterruptedException {
        super.init(config);
        try {
            locationRemote = Units.getUnit(getConfig().getPlacementConfig().getLocationId(), false, Units.LOCATION);
            registerActivationTrigger(new GenericServiceStateValueTrigger(locationRemote, triggerState, ServiceType.PRESENCE_STATE_SERVICE), TriggerPool.TriggerAggregation.OR);
        } catch (CouldNotPerformException ex) {
            throw new InitializationException(this, ex);
        }
    }

    @Override
    protected void trigger(ActivationStateType.ActivationState activationState) throws CouldNotPerformException, ExecutionException, InterruptedException {
        switch (activationState.getValue()) {
            case ACTIVE:
                taskActionDescription = locationRemote.applyAction(generateAction(UnitTemplateType.UnitTemplate.UnitType.TEMPERATURE_CONTROLLER,
                        ServiceType.TEMPERATURE_STATE_SERVICE,
                        TemperatureStateType.TemperatureState.newBuilder().setTemperature(comfortTemperature)).setExecutionTimePeriod(Long.MAX_VALUE).build()).get();
                break;
            case DEACTIVE:
                if(taskActionDescription != null) {
                    taskActionDescription = locationRemote.cancelAction(taskActionDescription).get();
                }
                break;
        }
    }
}
