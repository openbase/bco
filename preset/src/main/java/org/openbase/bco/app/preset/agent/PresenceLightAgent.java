package org.openbase.bco.app.preset.agent;

/*
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

import org.openbase.bco.dal.lib.state.States;
import org.openbase.bco.dal.lib.state.States.Brightness;
import org.openbase.bco.dal.remote.action.RemoteAction;
import org.openbase.bco.dal.remote.layer.unit.DimmableLightRemote;
import org.openbase.bco.dal.remote.layer.unit.LightRemote;
import org.openbase.bco.dal.remote.layer.unit.Units;
import org.openbase.bco.dal.remote.layer.unit.location.LocationRemote;
import org.openbase.bco.dal.remote.trigger.GenericBoundedDoubleValueTrigger;
import org.openbase.bco.dal.remote.trigger.GenericBoundedDoubleValueTrigger.TriggerOperation;
import org.openbase.bco.dal.remote.trigger.GenericServiceStateValueTrigger;
import org.openbase.jul.exception.*;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.pattern.trigger.TriggerPool.TriggerAggregation;
import org.openbase.type.domotic.service.ServiceTemplateType;
import org.openbase.type.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import org.openbase.type.domotic.state.ActivationStateType.ActivationState;
import org.openbase.type.domotic.state.IlluminanceStateType.IlluminanceState;
import org.openbase.type.domotic.state.PowerStateType.PowerState.State;
import org.openbase.type.domotic.state.PresenceStateType.PresenceState;
import org.openbase.type.domotic.unit.UnitConfigType.UnitConfig;
import org.openbase.type.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * @author <a href="mailto:tmichalski@techfak.uni-bielefeld.de">Timo Michalski</a>
 */
public class PresenceLightAgent extends AbstractDelayedTriggerableAgent {

    /**
     * 30 minutes max presence timeout
     */
    public static final long MAX_TIMEOUT = TimeUnit.MINUTES.toMillis(30);

    public static final double MIN_ILLUMINANCE_UNTIL_TRIGGER = 100d;

    private LocationRemote locationRemote;

    public PresenceLightAgent() throws InstantiationException {
        super(DelayMode.DELAY_DEACTIVATION, MAX_TIMEOUT);
    }

    @Override
    public void init(final UnitConfig config) throws InitializationException, InterruptedException {
        super.init(config);
        try {
            locationRemote = Units.getUnit(getConfig().getPlacementConfig().getLocationId(), false, Units.LOCATION);

            // activation trigger
            registerActivationTrigger(new GenericServiceStateValueTrigger(locationRemote, PresenceState.State.PRESENT, ServiceTemplateType.ServiceTemplate.ServiceType.PRESENCE_STATE_SERVICE), TriggerAggregation.AND);
            registerActivationTrigger(new GenericBoundedDoubleValueTrigger<>(locationRemote, MIN_ILLUMINANCE_UNTIL_TRIGGER, TriggerOperation.LOW_ACTIVE, ServiceType.ILLUMINANCE_STATE_SERVICE, "getIlluminance"), TriggerAggregation.AND);

            // deactivation trigger
            registerDeactivationTrigger(new GenericServiceStateValueTrigger(locationRemote, PresenceState.State.ABSENT, ServiceTemplateType.ServiceTemplate.ServiceType.PRESENCE_STATE_SERVICE), TriggerAggregation.OR);
        } catch (CouldNotPerformException ex) {
            throw new InitializationException(this, ex);
        }
    }

    @Override
    protected void delayedTrigger(final ActivationState activationState) throws CouldNotPerformException, ExecutionException, InterruptedException, TimeoutException {
        switch (activationState.getValue()) {
            case ACTIVE:
                // handle colorable lights
                observe(locationRemote.setNeutralWhite(getDefaultActionParameter(Long.MAX_VALUE)));

                // handle dimmable lights
                for (DimmableLightRemote light : locationRemote.getUnits(UnitType.DIMMABLE_LIGHT, false, Units.DIMMABLE_LIGHT)) {
                    // make sure colorable lights are filtered
                    if(light.getUnitType() == UnitType.DIMMABLE_LIGHT) {
                        observe(light.setBrightnessState(Brightness.MAX, getDefaultActionParameter(Long.MAX_VALUE)));
                    }
                }

                // handle lights
                for (LightRemote light : locationRemote.getUnits(UnitType.LIGHT, false, Units.LIGHT)) {
                    // make sure dimmable and colorable lights are filtered
                    if(light.getUnitType() == UnitType.LIGHT) {
                        observe(light.setPowerState(State.ON, getDefaultActionParameter(Long.MAX_VALUE)));
                    }
                }
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
