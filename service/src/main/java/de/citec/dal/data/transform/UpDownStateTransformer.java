/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.data.transform;

import de.citec.dal.exception.RSBBindingException;
import de.citec.dal.exception.TypeNotSupportedException;
import rst.homeautomation.openhab.UpDownHolderType;
import rst.homeautomation.states.ShutterType.Shutter;

/**
 *
 * @author thuxohl
 */
public class UpDownStateTransformer {

	public static Shutter.ShutterState transform(final UpDownHolderType.UpDownHolder.UpDown upDownType) throws RSBBindingException {
		switch (upDownType) {
			case DOWN:
				return Shutter.ShutterState.DOWN;
			case UP:
				return Shutter.ShutterState.UP;
			default:
				throw new RSBBindingException("Could not transform " + UpDownHolderType.UpDownHolder.UpDown.class.getName() + "! " + UpDownHolderType.UpDownHolder.UpDown.class.getSimpleName() + "[" + upDownType.name() + "] is unknown!");
		}
	}

	public static UpDownHolderType.UpDownHolder transform(final Shutter.ShutterState shutterState) throws TypeNotSupportedException, RSBBindingException {
		switch (shutterState) {
			case DOWN:
				return UpDownHolderType.UpDownHolder.newBuilder().setState(UpDownHolderType.UpDownHolder.UpDown.DOWN).build();
			case UP:
				return UpDownHolderType.UpDownHolder.newBuilder().setState(UpDownHolderType.UpDownHolder.UpDown.DOWN).build();
			case UNKNOWN:
				throw new TypeNotSupportedException(shutterState, UpDownHolderType.UpDownHolder.UpDown.class);
			default:
				throw new RSBBindingException("Could not transform " + Shutter.ShutterState.class.getName() + "! " + Shutter.ShutterState.class.getSimpleName() + "[" + shutterState.name() + "] is unknown!");
		}
	}
}
