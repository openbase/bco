/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.bindings.openhab.transform;

import org.dc.jul.exception.CouldNotPerformException;
import org.dc.jul.exception.CouldNotTransformException;
import org.dc.jul.exception.InvalidStateException;
import org.dc.jul.exception.TypeNotSupportedException;
import rst.homeautomation.state.MotionStateType.MotionState;
import rst.homeautomation.state.MotionStateType.MotionState.State;
import static rst.homeautomation.state.MotionStateType.MotionState.State.MOVEMENT;
import static rst.homeautomation.state.MotionStateType.MotionState.State.NO_MOVEMENT;

/**
 *
 * @author thuxohl
 */
public class MotionStateTransformer {

    public static MotionState transform(final Double decimalType) throws CouldNotTransformException {
        MotionState.Builder motionState = MotionState.newBuilder();
        try {
            if (decimalType.intValue() == 0) {
                motionState.setValue(State.NO_MOVEMENT);
            } else {
                motionState.setValue(State.MOVEMENT);
            }
            return motionState.build();
        } catch (Exception ex) {
            throw new CouldNotTransformException("Could not transform " + Double.class.getName() + "! " + Double.class.getSimpleName() + "[" + decimalType + "] is unknown!", ex);
        }
    }

    public static Double transform(final MotionState motionState) throws CouldNotTransformException {
        try {
            switch (motionState.getValue()) {
                case NO_MOVEMENT:
                    return 0d;
                case MOVEMENT:
                    return 1d;
                case UNKNOWN:
                    throw new InvalidStateException("Unknown state is invalid!");
                default:
                    throw new TypeNotSupportedException(MotionState.State.class, Double.class);
            }
        } catch (CouldNotPerformException ex) {
            throw new CouldNotTransformException("Could not transform " + MotionState.State.class.getName() + "!", ex);
        }
    }
}
