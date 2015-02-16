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
import rst.homeautomation.states.MotionType;

/**
 *
 * @author thuxohl
 */
public class MotionStateTransformer {

    public static MotionType.Motion.MotionState transform(final Double decimalType) throws CouldNotTransformException {
        try {
            if (decimalType.intValue() == 0) {
                return MotionType.Motion.MotionState.NO_MOVEMENT;
            } else {
                return MotionType.Motion.MotionState.MOVEMENT;
            }
        } catch (Exception ex) {
            throw new CouldNotTransformException("Could not transform " + Double.class.getName() + "! " + Double.class.getSimpleName() + "[" + decimalType + "] is unknown!", ex);
        }
    }

    public static Double transform(final MotionType.Motion.MotionState motionState) throws CouldNotTransformException {
        try {
            switch (motionState) {
                case NO_MOVEMENT:
                    return 0d;
                case MOVEMENT:
                    return 1d;
                case UNKNOWN:
                    throw new InvalidStateException("Unknown state is invalid!");
                default:
                    throw new TypeNotSupportedException(MotionType.Motion.MotionState.class, Double.class);
            }
        } catch (CouldNotPerformException ex) {
            throw new CouldNotTransformException("Could not transform " + MotionType.Motion.MotionState.class.getName() + "!", ex);
        }
    }
}
