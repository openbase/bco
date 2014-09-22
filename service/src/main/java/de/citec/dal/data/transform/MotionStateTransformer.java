/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.data.transform;

import de.citec.dal.exception.RSBBindingException;
import de.citec.dal.exception.TypeNotSupportedException;
import org.openhab.core.library.types.DecimalType;
import rst.homeautomation.states.MotionType;

/**
 *
 * @author thuxohl
 */
public class MotionStateTransformer {

	public static MotionType.Motion.MotionState transform(DecimalType decimalType) throws RSBBindingException {
		switch (decimalType.intValue()) {
			case 0:
				return MotionType.Motion.MotionState.NO_MOVEMENT;
			case 1:
				return MotionType.Motion.MotionState.MOVEMENT;
			default:
				throw new RSBBindingException("Could not transform " + DecimalType.class.getName() + "! " + DecimalType.class.getSimpleName() + "[" + decimalType.intValue() + "] is unknown!");
		}
	}

	public static DecimalType transform(MotionType.Motion.MotionState motionState) throws TypeNotSupportedException, RSBBindingException {
		switch (motionState) {
			case NO_MOVEMENT:
				return new DecimalType(0);
			case MOVEMENT:
				return new DecimalType(1);
			case UNKNOWN:
				throw new TypeNotSupportedException(motionState, DecimalType.class);
			default:
				throw new RSBBindingException("Could not transform " + MotionType.Motion.MotionState.class.getName() + "! " + MotionType.Motion.MotionState.class.getSimpleName() + "[" + motionState.name() + "] is unknown!");
		}
	}
}
