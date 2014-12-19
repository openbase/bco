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
public class UpDownStateTransformer {

	public static Shutter.ShutterState transform(org.openhab.core.library.types.UpDownType upDownType) throws RSBBindingException {
		switch (upDownType) {
			case DOWN:
				return Shutter.ShutterState.DOWN;
			case UP:
				return Shutter.ShutterState.UP;
			default:
				throw new RSBBindingException("Could not transform " + org.openhab.core.library.types.UpDownType.class.getName() + "! " + org.openhab.core.library.types.UpDownType.class.getSimpleName() + "[" + upDownType.name() + "] is unknown!");
		}
	}

	public static org.openhab.core.library.types.UpDownType transform(Shutter.ShutterState shutterState) throws TypeNotSupportedException, RSBBindingException {
		switch (shutterState) {
			case DOWN:
				return org.openhab.core.library.types.UpDownType.DOWN;
			case UP:
				return org.openhab.core.library.types.UpDownType.UP;
			case UNKNOWN:
				throw new TypeNotSupportedException(shutterState, org.openhab.core.library.types.UpDownType.class);
			default:
				throw new RSBBindingException("Could not transform " + Shutter.ShutterState.class.getName() + "! " + Shutter.ShutterState.class.getSimpleName() + "[" + shutterState.name() + "] is unknown!");
		}
	}
}
