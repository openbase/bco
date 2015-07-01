/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.bindings.openhab.transform;

import de.citec.jul.exception.CouldNotPerformException;
import de.citec.jul.exception.CouldNotTransformException;
import de.citec.jul.exception.InvalidStateException;
import de.citec.jul.exception.TypeNotSupportedException;
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
