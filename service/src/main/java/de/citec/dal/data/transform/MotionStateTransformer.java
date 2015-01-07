/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.data.transform;

import de.citec.dal.exception.RSBBindingException;
import de.citec.dal.exception.TypeNotSupportedException;
import rst.homeautomation.states.MotionType;

/**
 *
 * @author thuxohl
 */
public class MotionStateTransformer {

	public static MotionType.Motion.MotionState transform(final double decimalType) throws RSBBindingException {
		switch ((int) decimalType) {
			case 0:
				return MotionType.Motion.MotionState.NO_MOVEMENT;
			case 1:
				return MotionType.Motion.MotionState.MOVEMENT;
			default:
				throw new RSBBindingException("Could not transform " + DecimalType.Decimal.class.getName() + "! " + DecimalType.Decimal.class.getSimpleName() + "[" + decimalType + "] is unknown!");
		}
	}

	public static DecimalType.Decimal transform(final MotionType.Motion.MotionState motionState) throws TypeNotSupportedException, RSBBindingException {
		switch (motionState) {
			case NO_MOVEMENT:
				return DecimalType.Decimal.newBuilder().setValue(0).build();
			case MOVEMENT:
				return DecimalType.Decimal.newBuilder().setValue(1).build();
			case UNKNOWN:
				throw new TypeNotSupportedException(motionState, DecimalType.Decimal.class);
			default:
				throw new RSBBindingException("Could not transform " + MotionType.Motion.MotionState.class.getName() + "! " + MotionType.Motion.MotionState.class.getSimpleName() + "[" + motionState.name() + "] is unknown!");
		}
	}
}
