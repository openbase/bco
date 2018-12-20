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
import org.openbase.bco.dal.remote.layer.unit.location.LocationRemote;
import org.openbase.bco.dal.remote.trigger.GenericBCOTrigger;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InitializationException;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.pattern.trigger.TriggerPool.TriggerAggregation;
import rst.domotic.action.ActionDescriptionType.ActionDescription;
import rst.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import rst.domotic.state.ActivationStateType.ActivationState;
import rst.domotic.state.PowerStateType.PowerState;
import rst.domotic.state.PowerStateType.PowerState.State;
import rst.domotic.state.PresenceStateType.PresenceState;
import rst.domotic.unit.UnitConfigType.UnitConfig;
import rst.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;

import java.util.concurrent.ExecutionException;

/**
 * @author <a href="mailto:tmichalski@techfak.uni-bielefeld.de">Timo Michalski</a>
 */
public class AbsenceEnergySavingAgent extends AbstractTriggerableAgent {

    private LocationRemote locationRemote;

    public AbsenceEnergySavingAgent() throws InstantiationException {
        super(AbsenceEnergySavingAgent.class);
    }

    @Override
    public void init(final UnitConfig config) throws InitializationException, InterruptedException {
        super.init(config);
        try {
            locationRemote = Units.getUnit(getConfig().getPlacementConfig().getLocationId(), false, Units.LOCATION);
            registerTrigger(new GenericBCOTrigger(locationRemote, PresenceState.State.ABSENT, ServiceType.PRESENCE_STATE_SERVICE), TriggerAggregation.OR);
        } catch (CouldNotPerformException ex) {
            throw new InitializationException(this, ex);
        }
    }

    private ActionDescription taskActionDescription;

    @Override
    void trigger(ActivationState activationState) throws CouldNotPerformException, ExecutionException, InterruptedException {
        switch (activationState.getValue()) {
            case ACTIVE:
                taskActionDescription = locationRemote.applyAction(generateAction(UnitType.UNKNOWN, ServiceType.POWER_STATE_SERVICE, PowerState.newBuilder().setValue(State.OFF)).setExecutionTimePeriod(Long.MAX_VALUE)).get();
                logger.warn("AbsenceEnergySavingAgent created action with id {}", taskActionDescription.getId());
                break;
            case DEACTIVE:
                if (taskActionDescription != null) {
                    taskActionDescription = locationRemote.cancelAction(taskActionDescription, getDefaultActionParameter().getAuthenticationToken(), null).get();
                    logger.warn("AbsenceEnergySavingAgent cancel action {}", taskActionDescription.getId());
                }
                break;
        }
    }
}
