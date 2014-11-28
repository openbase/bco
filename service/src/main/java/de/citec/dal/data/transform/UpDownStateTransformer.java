/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.data.transform;

import de.citec.dal.exception.RSBBindingException;
import de.citec.dal.exception.TypeNotSupportedException;
import rst.homeautomation.states.UpDownType;
import rst.homeautomation.openhab.UpDownHolderType;

/**
 *
 * @author thuxohl
 */
public class UpDownStateTransformer {

	public static UpDownType.UpDown.UpDownState transform(UpDownHolderType.UpDownHolder.UpDown upDownType) throws RSBBindingException {
		switch (upDownType) {
			case DOWN:
				return UpDownType.UpDown.UpDownState.DOWN;
			case UP:
				return UpDownType.UpDown.UpDownState.UP;
			default:
				throw new RSBBindingException("Could not transform " + UpDownHolderType.UpDownHolder.UpDown.class.getName() + "! " + UpDownHolderType.UpDownHolder.UpDown.class.getSimpleName() + "[" + upDownType.name() + "] is unknown!");
		}
	}

	public static UpDownHolderType.UpDownHolder transform(UpDownType.UpDown.UpDownState upDownState) throws TypeNotSupportedException, RSBBindingException {
		switch (upDownState) {
			case DOWN:
				return UpDownHolderType.UpDownHolder.newBuilder().setState(UpDownHolderType.UpDownHolder.UpDown.DOWN).build();
			case UP:
				return UpDownHolderType.UpDownHolder.newBuilder().setState(UpDownHolderType.UpDownHolder.UpDown.DOWN).build();
			case UNKNOWN:
				throw new TypeNotSupportedException(upDownState, UpDownHolderType.UpDownHolder.UpDown.class);
			default:
				throw new RSBBindingException("Could not transform " + UpDownType.UpDown.UpDownState.class.getName() + "! " + UpDownType.UpDown.UpDownState.class.getSimpleName() + "[" + upDownState.name() + "] is unknown!");
		}
	}
}
