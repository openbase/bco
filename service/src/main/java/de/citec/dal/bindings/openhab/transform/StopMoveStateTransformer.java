/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.bindings.openhab.transform;

import de.citec.jul.exception.CouldNotTransformException;
import de.citec.jul.exception.TypeNotSupportedException;
import rst.homeautomation.openhab.StopMoveHolderType;
import static rst.homeautomation.openhab.StopMoveHolderType.StopMoveHolder.StopMove.STOP;
import rst.homeautomation.states.ShutterType.Shutter;

/**
 *
 * @author thuxohl
 */
public class StopMoveStateTransformer {


	public static Shutter.ShutterState transform(final StopMoveHolderType.StopMoveHolder.StopMove stopMoveType) throws CouldNotTransformException {
		switch (stopMoveType) {
			case STOP:
				return Shutter.ShutterState.STOP;
			case MOVE:
				return Shutter.ShutterState.UNKNOWN;
			default:
				throw new CouldNotTransformException("Could not transform " + StopMoveHolderType.StopMoveHolder.StopMove.class.getName() + "! " + StopMoveHolderType.StopMoveHolder.StopMove.class.getSimpleName() + "[" + stopMoveType.name() + "] is unknown!");
		}
	}


	public static StopMoveHolderType.StopMoveHolder transform(Shutter.ShutterState shutterState) throws TypeNotSupportedException, CouldNotTransformException {
		switch (shutterState) {
			case STOP:
				return StopMoveHolderType.StopMoveHolder.newBuilder().setState(StopMoveHolderType.StopMoveHolder.StopMove.STOP).build();
			case UP:
				return StopMoveHolderType.StopMoveHolder.newBuilder().setState(StopMoveHolderType.StopMoveHolder.StopMove.MOVE).build();
            case DOWN:
                return StopMoveHolderType.StopMoveHolder.newBuilder().setState(StopMoveHolderType.StopMoveHolder.StopMove.MOVE).build();
			case UNKNOWN:
				throw new TypeNotSupportedException(shutterState, StopMoveHolderType.StopMoveHolder.StopMove.class);
			default:
				throw new CouldNotTransformException("Could not transform " + Shutter.ShutterState.class.getName() + "! " + Shutter.ShutterState.class.getSimpleName() + "[" + shutterState.name() + "] is unknown!");
		}
	}
}
