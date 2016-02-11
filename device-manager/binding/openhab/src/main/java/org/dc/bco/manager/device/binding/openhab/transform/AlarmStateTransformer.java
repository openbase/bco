/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dc.bco.manager.device.binding.openhab.transform;

/*
 * #%L
 * COMA DeviceManager Binding OpenHAB
 * %%
 * Copyright (C) 2015 - 2016 DivineCooperation
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

import org.dc.jul.exception.CouldNotPerformException;
import org.dc.jul.exception.CouldNotTransformException;
import org.dc.jul.exception.InvalidStateException;
import org.dc.jul.exception.TypeNotSupportedException;
import rst.homeautomation.state.AlarmStateType.AlarmState;
import rst.homeautomation.state.AlarmStateType.AlarmState.State;

/**
 *
 * @author thuxohl
 */
public class AlarmStateTransformer {

    //TODO: check if the values from openhab match this transoformation
    public static AlarmState transform(final Double decimalType) throws CouldNotTransformException {
        AlarmState.Builder alarmState = AlarmState.newBuilder();
        try {
            if (decimalType.intValue() == 0) {
                alarmState.setValue(State.NO_ALARM);
            } else if (decimalType.intValue() == 1) {
                alarmState.setValue(State.ALARM);
            }
            return alarmState.build();
        } catch (Exception ex) {
            throw new CouldNotTransformException("Could not transform " + Double.class.getName() + "! " + Double.class.getSimpleName() + "[" + decimalType + "] is unknown!", ex);
        }
    }

    public static Double transform(final AlarmState alarmState) throws CouldNotTransformException {
        try {
            switch (alarmState.getValue()) {
                case NO_ALARM:
                    return 0d;
                case ALARM:
                    return 1d;
                case UNKNOWN:
                    throw new InvalidStateException("Unknown state is invalid!");
                default:
                    throw new TypeNotSupportedException(AlarmState.State.class, Double.class);
            }
        } catch (CouldNotPerformException ex) {
            throw new CouldNotTransformException("Could not transform " + AlarmState.State.class.getName() + "!", ex);
        }
    }
}
