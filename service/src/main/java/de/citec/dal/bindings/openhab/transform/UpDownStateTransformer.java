/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.bindings.openhab.transform;

import org.dc.jul.exception.CouldNotTransformException;
import org.dc.jul.exception.TypeNotSupportedException;
import rst.homeautomation.openhab.UpDownHolderType;
import rst.homeautomation.state.ShutterStateType.ShutterState;

/**
 *
 * @author thuxohl
 */
public class UpDownStateTransformer {

	public static ShutterState.State transform(final UpDownHolderType.UpDownHolder.UpDown upDownType) throws CouldNotTransformException {
		switch (upDownType) {
			case DOWN:
				return ShutterState.State.DOWN;
			case UP:
				return ShutterState.State.UP;
			default:
				throw new CouldNotTransformException("Could not transform " + UpDownHolderType.UpDownHolder.UpDown.class.getName() + "! " + UpDownHolderType.UpDownHolder.UpDown.class.getSimpleName() + "[" + upDownType.name() + "] is unknown!");
		}
	}

	public static UpDownHolderType.UpDownHolder transform(final ShutterState.State shutterState) throws TypeNotSupportedException, CouldNotTransformException {
		switch (shutterState) {
			case DOWN:
				return UpDownHolderType.UpDownHolder.newBuilder().setState(UpDownHolderType.UpDownHolder.UpDown.DOWN).build();
			case UP:
				return UpDownHolderType.UpDownHolder.newBuilder().setState(UpDownHolderType.UpDownHolder.UpDown.DOWN).build();
			case UNKNOWN:
				throw new TypeNotSupportedException(shutterState, UpDownHolderType.UpDownHolder.UpDown.class);
			default:
				throw new CouldNotTransformException("Could not transform " + ShutterState.State.class.getName() + "! " + ShutterState.State.class.getSimpleName() + "[" + shutterState.name() + "] is unknown!");
		}
	}
}
