/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.data.transform;

import de.citec.dal.exception.RSBBindingException;
import de.citec.dal.exception.TypeNotSupportedException;
import rst.homeautomation.states.ShutterType.Shutter;

/**
 *
 * @author thuxohl
 */
public class StopMoveStateTransformer {

	public static Shutter.ShutterState transform(org.openhab.core.library.types.StopMoveType stopMoveType) throws RSBBindingException {
		switch (stopMoveType) {
			case STOP:
				return Shutter.ShutterState.STOP;
			case MOVE:
				return Shutter.ShutterState.UNKNOWN;
			default:
				throw new RSBBindingException("Could not transform " + org.openhab.core.library.types.StopMoveType.class.getName() + "! " + org.openhab.core.library.types.StopMoveType.class.getSimpleName() + "[" + stopMoveType.name() + "] is unknown!");
		}
	}

	public static org.openhab.core.library.types.StopMoveType transform(Shutter.ShutterState shutterState) throws TypeNotSupportedException, RSBBindingException {
		switch (shutterState) {
			case STOP:
				return org.openhab.core.library.types.StopMoveType.STOP;
			case UP:
				return org.openhab.core.library.types.StopMoveType.MOVE;
            case DOWN:
                return org.openhab.core.library.types.StopMoveType.MOVE;
			case UNKNOWN:
				throw new TypeNotSupportedException(shutterState, org.openhab.core.library.types.StopMoveType.class);
			default:
				throw new RSBBindingException("Could not transform " + Shutter.ShutterState.class.getName() + "! " + Shutter.ShutterState.class.getSimpleName() + "[" + shutterState.name() + "] is unknown!");
		}
	}
}
