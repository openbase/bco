/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.data.transform;

import de.citec.dal.exception.RSBBindingException;
import de.citec.dal.exception.TypeNotSupportedException;
import rst.homeautomation.states.UpDownType;

/**
 *
 * @author thuxohl
 */
public class UpDownStateTransformer {

	public static UpDownType.UpDown.UpDownState transform(org.openhab.core.library.types.UpDownType upDownType) throws RSBBindingException {
		switch (upDownType) {
			case DOWN:
				return UpDownType.UpDown.UpDownState.DOWN;
			case UP:
				return UpDownType.UpDown.UpDownState.UP;
			default:
				throw new RSBBindingException("Could not transform " + org.openhab.core.library.types.UpDownType.class.getName() + "! " + org.openhab.core.library.types.UpDownType.class.getSimpleName() + "[" + upDownType.name() + "] is unknown!");
		}
	}

	public static org.openhab.core.library.types.UpDownType transform(UpDownType.UpDown.UpDownState upDownState) throws TypeNotSupportedException, RSBBindingException {
		switch (upDownState) {
			case DOWN:
				return org.openhab.core.library.types.UpDownType.DOWN;
			case UP:
				return org.openhab.core.library.types.UpDownType.UP;
			case UNKNOWN:
				throw new TypeNotSupportedException(upDownState, org.openhab.core.library.types.UpDownType.class);
			default:
				throw new RSBBindingException("Could not transform " + UpDownType.UpDown.UpDownState.class.getName() + "! " + UpDownType.UpDown.UpDownState.class.getSimpleName() + "[" + upDownState.name() + "] is unknown!");
		}
	}
}
