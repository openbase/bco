package org.openbase.bco.app.preset.agent;

/*
 * #%L
 * BCO App Preset
 * %%
 * Copyright (C) 2018 - 2020 openbase.org
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

import org.openbase.bco.dal.remote.action.RemoteAction;
import org.openbase.bco.dal.remote.layer.unit.LightRemote;
import org.openbase.bco.dal.remote.layer.unit.Units;
import org.openbase.bco.dal.remote.layer.unit.location.LocationRemote;
import org.openbase.bco.dal.remote.trigger.GenericServiceStateValueTrigger;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InitializationException;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.pattern.trigger.TriggerPool.TriggerAggregation;
import org.openbase.type.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import org.openbase.type.domotic.state.ActivationStateType.ActivationState;
import org.openbase.type.domotic.state.PowerStateType.PowerState.State;
import org.openbase.type.domotic.state.PresenceStateType.PresenceState;
import org.openbase.type.domotic.unit.UnitConfigType.UnitConfig;
import org.openbase.type.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * @author <a href="mailto:tmichalski@techfak.uni-bielefeld.de">Timo Michalski</a>
 */
public class AbsenceEnergySavingAgent extends AbstractDelayedTriggerableAgent {

    /**
     * 30 minutes max absence timeout
     */
    public static final long MAX_TIMEOUT = TimeUnit.MINUTES.toMillis(30);

    private LocationRemote locationRemote;
    private RemoteAction lastAction;

    public AbsenceEnergySavingAgent() throws InstantiationException {
        super(DelayMode.DELAY_ACTIVATION, MAX_TIMEOUT);
    }

    @Override
    public void init(final UnitConfig config) throws InitializationException, InterruptedException {
        super.init(config);
        try {
            locationRemote = Units.getUnit(getConfig().getPlacementConfig().getLocationId(), false, Units.LOCATION);
            registerActivationTrigger(new GenericServiceStateValueTrigger(locationRemote, PresenceState.State.ABSENT, ServiceType.PRESENCE_STATE_SERVICE), TriggerAggregation.OR);
        } catch (CouldNotPerformException ex) {
            throw new InitializationException(this, ex);
        }
    }

    @Override
    protected void delayedTrigger(ActivationState activationState) throws CouldNotPerformException, ExecutionException, InterruptedException {
        switch (activationState.getValue()) {
            case ACTIVE:
                lastAction = observe(locationRemote.setPowerState(State.OFF, getDefaultActionParameter(Long.MAX_VALUE)));
                break;
            case DEACTIVE:
                cancelAllObservedActions();
                break;
        }
    }

    @Override
    protected double getDelayScaleFactor() throws NotAvailableException {
        return locationRemote.getComfortToEconomyRatio();
    }
}
