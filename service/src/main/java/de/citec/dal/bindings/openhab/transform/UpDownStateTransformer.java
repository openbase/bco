/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.bindings.openhab.transform;

import de.citec.jul.exception.CouldNotTransformException;
import de.citec.jul.exception.TypeNotSupportedException;
import rst.homeautomation.openhab.UpDownHolderType;
import rst.homeautomation.state.ShutterType.Shutter;

/**
 *
 * @author thuxohl
 */
public class UpDownStateTransformer {

	public static Shutter.ShutterState transform(final UpDownHolderType.UpDownHolder.UpDown upDownType) throws CouldNotTransformException {
		switch (upDownType) {
			case DOWN:
				return Shutter.ShutterState.DOWN;
			case UP:
				return Shutter.ShutterState.UP;
			default:
				throw new CouldNotTransformException("Could not transform " + UpDownHolderType.UpDownHolder.UpDown.class.getName() + "! " + UpDownHolderType.UpDownHolder.UpDown.class.getSimpleName() + "[" + upDownType.name() + "] is unknown!");
		}
	}

	public static UpDownHolderType.UpDownHolder transform(final Shutter.ShutterState shutterState) throws TypeNotSupportedException, CouldNotTransformException {
		switch (shutterState) {
			case DOWN:
				return UpDownHolderType.UpDownHolder.newBuilder().setState(UpDownHolderType.UpDownHolder.UpDown.DOWN).build();
			case UP:
				return UpDownHolderType.UpDownHolder.newBuilder().setState(UpDownHolderType.UpDownHolder.UpDown.DOWN).build();
			case UNKNOWN:
				throw new TypeNotSupportedException(shutterState, UpDownHolderType.UpDownHolder.UpDown.class);
			default:
				throw new CouldNotTransformException("Could not transform " + Shutter.ShutterState.class.getName() + "! " + Shutter.ShutterState.class.getSimpleName() + "[" + shutterState.name() + "] is unknown!");
		}
	}
}
