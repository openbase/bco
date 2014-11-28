/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.data.transform;

import de.citec.dal.exception.RSBBindingException;
import de.citec.dal.exception.TypeNotSupportedException;
import rst.homeautomation.states.StopMoveType;
import rst.homeautomation.openhab.StopMoveHolderType;

/**
 *
 * @author thuxohl
 */
public class StopMoveStateTransformer {

	public static StopMoveType.StopMove.StopMoveState transform(StopMoveHolderType.StopMoveHolder.StopMove stopMoveType) throws RSBBindingException {
		switch (stopMoveType) {
			case STOP:
				return StopMoveType.StopMove.StopMoveState.STOP;
			case MOVE:
				return StopMoveType.StopMove.StopMoveState.MOVE;
			default:
				throw new RSBBindingException("Could not transform " + StopMoveHolderType.StopMoveHolder.StopMove.class.getName() + "! " + StopMoveHolderType.StopMoveHolder.StopMove.class.getSimpleName() + "[" + stopMoveType.name() + "] is unknown!");
		}
	}

	public static StopMoveHolderType.StopMoveHolder transform(StopMoveType.StopMove.StopMoveState stopMoveState) throws TypeNotSupportedException, RSBBindingException {
		switch (stopMoveState) {
			case STOP:
				return StopMoveHolderType.StopMoveHolder.newBuilder().setState(StopMoveHolderType.StopMoveHolder.StopMove.STOP).build();
			case MOVE:
				return StopMoveHolderType.StopMoveHolder.newBuilder().setState(StopMoveHolderType.StopMoveHolder.StopMove.MOVE).build();
			case UNKNOWN:
				throw new TypeNotSupportedException(stopMoveState, StopMoveHolderType.StopMoveHolder.StopMove.class);
			default:
				throw new RSBBindingException("Could not transform " + StopMoveType.StopMove.StopMoveState.class.getName() + "! " + StopMoveType.StopMove.StopMoveState.class.getSimpleName() + "[" + stopMoveState.name() + "] is unknown!");
		}
	}
}
