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
    private ActionDescription taskActionDescriptionLightPower;
    private ActionDescription taskActionDescriptionLightBrightness;
    private ActionDescription taskActionDescriptionLightColor;
    private ActionDescription taskActionDescriptionBlinds;

    public FireAlarmAgent() throws InstantiationException {
        super();
    }

    @Override
    public void init(final UnitConfig config) throws InitializationException, InterruptedException {
        super.init(config);
        try {
            locationRemote = Units.getUnit(getConfig().getPlacementConfig().getLocationId(), false, Units.LOCATION);
            // Todo: Add trigger for FireAlarm as soon as it is supported
//            registerActivationTrigger(new GenericServiceStateValueTrigger(locationRemote, triggerState, ServiceType.FIRE_ALARM_STATE_SERVICE), TriggerAggregation.OR);
            registerActivationTrigger(new GenericServiceStateValueTrigger(locationRemote, triggerState, ServiceType.SMOKE_ALARM_STATE_SERVICE), TriggerAggregation.OR);
        } catch (CouldNotPerformException ex) {
            throw new InitializationException(this, ex);
        }
    }

    private void alarmRoutine() throws CouldNotPerformException, ExecutionException, InterruptedException{
        taskActionDescriptionLightPower =  locationRemote.applyAction(generateAction(UnitType.LIGHT,
                ServiceType.POWER_STATE_SERVICE,
                PowerState.newBuilder().setValue(PowerState.State.ON)).setExecutionTimePeriod(Long.MAX_VALUE).build()).get();
        // TODO: Set brightnessState as soon as it is supported
//        taskActionDescriptionLightBrightness =  locationRemote.applyAction(generateAction(UnitType.UNKNOWN,
//                ServiceType.BRIGHTNESS_STATE_SERVICE,
//                BrightnessState.newBuilder().setBrightness(1)).toBuilder().setExecutionTimePeriod(Long.MAX_VALUE).build()).get();
        // TODO: Set color to white
//        taskActionDescriptionLightColor =  locationRemote.applyAction(generateAction(UnitType.UNKNOWN,
//                ServiceType.COLOR_STATE_SERVICE,
//                ColorState.newBuilder().setColor(color)).toBuilder().setExecutionTimePeriod(Long.MAX_VALUE).build()).get();
        taskActionDescriptionBlinds =  locationRemote.applyAction(generateAction(UnitType.UNKNOWN,
                ServiceType.BLIND_STATE_SERVICE,
                BlindState.newBuilder().setOpeningRatio(1d).setValue(State.UP)).setExecutionTimePeriod(Long.MAX_VALUE).build()).get();
    }

    @Override
    protected void trigger(ActivationState activationState) throws CouldNotPerformException, ExecutionException, InterruptedException {
        switch (activationState.getValue()) {
            case ACTIVE:
                alarmRoutine();
                break;
            case DEACTIVE:
                if(taskActionDescriptionLightPower != null) {
                    taskActionDescriptionLightPower = locationRemote.cancelAction(taskActionDescriptionLightPower).get();
                }
                if(taskActionDescriptionLightBrightness != null) {
                    taskActionDescriptionLightBrightness = locationRemote.cancelAction(taskActionDescriptionLightBrightness).get();
                }
                if(taskActionDescriptionLightColor != null) {
                    taskActionDescriptionLightColor = locationRemote.cancelAction(taskActionDescriptionLightColor).get();
                }

                if(taskActionDescriptionBlinds != null) {
                    taskActionDescriptionBlinds = locationRemote.cancelAction(taskActionDescriptionBlinds).get();
                }
                break;
        }
    }
}
