/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.data.transform;

import de.citec.dal.exception.RSBBindingException;
import de.citec.dal.exception.TypeNotSupportedException;
import rst.homeautomation.states.StopMoveType;
import rst.homeautomation.states.UpDownType;

/**
 *
 * @author thuxohl
 */
public class StopMoveStateTransformer {

	public static StopMoveType.StopMove.StopMoveState transform(org.openhab.core.library.types.StopMoveType stopMoveType) throws RSBBindingException {
		switch (stopMoveType) {
			case STOP:
				return StopMoveType.StopMove.StopMoveState.STOP;
			case MOVE:
				return StopMoveType.StopMove.StopMoveState.MOVE;
			default:
				throw new RSBBindingException("Could not transform " + org.openhab.core.library.types.StopMoveType.class.getName() + "! " + org.openhab.core.library.types.StopMoveType.class.getSimpleName() + "[" + stopMoveType.name() + "] is unknown!");
		}
	}

	public static org.openhab.core.library.types.StopMoveType transform(StopMoveType.StopMove.StopMoveState stopMoveState) throws TypeNotSupportedException, RSBBindingException {
		switch (stopMoveState) {
			case STOP:
				return org.openhab.core.library.types.StopMoveType.STOP;
			case MOVE:
				return org.openhab.core.library.types.StopMoveType.MOVE;
			case UNKNOWN:
				throw new TypeNotSupportedException(stopMoveState, org.openhab.core.library.types.StopMoveType.class);
			default:
				throw new RSBBindingException("Could not transform " + StopMoveType.StopMove.StopMoveState.class.getName() + "! " + StopMoveType.StopMove.StopMoveState.class.getSimpleName() + "[" + stopMoveState.name() + "] is unknown!");
		}
	}
}
