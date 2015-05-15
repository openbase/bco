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
import rst.homeautomation.state.MotionStateType.MotionState;

/**
 *
 * @author thuxohl
 */
public class MotionStateTransformer {

    public static MotionState.State transform(final Double decimalType) throws CouldNotTransformException {
        try {
            if (decimalType.intValue() == 0) {
                return MotionState.State.NO_MOVEMENT;
            } else {
                return MotionState.State.MOVEMENT;
            }
        } catch (Exception ex) {
            throw new CouldNotTransformException("Could not transform " + Double.class.getName() + "! " + Double.class.getSimpleName() + "[" + decimalType + "] is unknown!", ex);
        }
    }

    public static Double transform(final MotionState.State motionState) throws CouldNotTransformException {
        try {
            switch (motionState) {
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
