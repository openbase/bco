package org.openbase.bco.app.preset.agent;

/*-
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
import org.openbase.bco.dal.lib.state.States;
import org.openbase.bco.dal.lib.state.States.Brightness;
import org.openbase.bco.dal.lib.state.States.Power;
import org.openbase.bco.dal.remote.layer.unit.Units;
import org.openbase.bco.dal.remote.layer.unit.location.LocationRemote;
import org.openbase.bco.dal.remote.trigger.GenericServiceStateValueTrigger;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InitializationException;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.pattern.trigger.TriggerPool.TriggerAggregation;
import org.openbase.type.domotic.action.ActionDescriptionType.ActionDescription;
import org.openbase.type.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import org.openbase.type.domotic.state.ActivationStateType.ActivationState;
import org.openbase.type.domotic.state.AlarmStateType.AlarmState;
import org.openbase.type.domotic.state.BlindStateType.BlindState;
import org.openbase.type.domotic.state.BlindStateType.BlindState.State;
import org.openbase.type.domotic.state.PowerStateType.PowerState;
import org.openbase.type.domotic.unit.UnitConfigType.UnitConfig;
import org.openbase.type.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;
import org.openbase.type.vision.ColorType.Color;
import org.openbase.type.vision.HSBColorType.HSBColor;

import java.util.concurrent.ExecutionException;

/**
 *
 * @author <a href="mailto:tmichalski@techfak.uni-bielefeld.de">Timo Michalski</a>
 */
public class FireAlarmAgent extends AbstractTriggerableAgent {

    private LocationRemote locationRemote;
    private final AlarmState.State triggerState = AlarmState.State.ALARM;
    private final HSBColor HSBcolor = HSBColor.newBuilder().setBrightness(1).setSaturation(0).setHue(0).build();
    private final Color color = Color.newBuilder().setType(Color.Type.HSB).setHsbColor(HSBcolor).build();

    public FireAlarmAgent() throws InstantiationException {
        super();
    }

    @Override
    public void init(final UnitConfig config) throws InitializationException, InterruptedException {
        super.init(config);
        try {
            locationRemote = Units.getUnit(getConfig().getPlacementConfig().getLocationId(), false, Units.LOCATION);
            registerActivationTrigger(new GenericServiceStateValueTrigger(locationRemote, triggerState, ServiceType.FIRE_ALARM_STATE_SERVICE), TriggerAggregation.OR);
            registerActivationTrigger(new GenericServiceStateValueTrigger(locationRemote, triggerState, ServiceType.SMOKE_ALARM_STATE_SERVICE), TriggerAggregation.OR);
        } catch (CouldNotPerformException ex) {
            throw new InitializationException(this, ex);
        }
    }

    @Override
    protected void trigger(ActivationState activationState) throws CouldNotPerformException, ExecutionException, InterruptedException {
        switch (activationState.getValue()) {
            case ACTIVE:
                observe(locationRemote.setPowerState(PowerState.State.ON, UnitType.LIGHT, getDefaultActionParameter(Long.MAX_VALUE)));
                observe(locationRemote.setBrightnessState(Brightness.MAX, getDefaultActionParameter(Long.MAX_VALUE)));
                observe(locationRemote.setColorState(States.Color.WHITE, getDefaultActionParameter(Long.MAX_VALUE)));
                observe(locationRemote.setBlindState(BlindState.newBuilder().setOpeningRatio(1d).setValue(State.UP).build(), getDefaultActionParameter(Long.MAX_VALUE)));
                break;
            case DEACTIVE:
                cancelAllObservedActions();
                break;
        }
    }
}
