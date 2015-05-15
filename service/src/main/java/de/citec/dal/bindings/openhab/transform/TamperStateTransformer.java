/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.bindings.openhab.transform;

import de.citec.jul.exception.CouldNotTransformException;
import de.citec.jul.exception.TypeNotSupportedException;
import rst.homeautomation.state.TamperStateType.TamperState;

/**
 *
 * @author thuxohl
 */
public class TamperStateTransformer {

	public static TamperState.State transform(final double state) throws CouldNotTransformException {
		switch ((int) state) {
			case 0:
				return TamperState.State.NO_TAMPER;
			case 255:
				return TamperState.State.TAMPER;
			default:
				throw new CouldNotTransformException("Could not transform " + Double.class.getName() + "! " + Double.class.getSimpleName() + "[" + state + "] is unknown!");
		}
	}

	public static double transform(final TamperState.State tamperState) throws TypeNotSupportedException, CouldNotTransformException {
		switch (tamperState) {
			case NO_TAMPER:
				return 0d;
			case TAMPER:
				return 255d;
			case UNKNOWN:
				throw new TypeNotSupportedException(tamperState, Double.class);
			default:
				throw new CouldNotTransformException("Could not transform " + TamperState.State.class.getName() + "! " + TamperState.State.class.getSimpleName() + "[" + tamperState.name() + "] is unknown!");
		}
	}
}
