/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.bindings.openhab.transform;

import de.citec.jul.exception.CouldNotTransformException;
import de.citec.jul.exception.TypeNotSupportedException;
import rst.homeautomation.openhab.OnOffHolderType;
import rst.homeautomation.state.ButtonStateType.ButtonState;

/**
 *
 * @author mpohling
 */
public class ButtonStateTransformer {

	public static ButtonState.State transform(OnOffHolderType.OnOffHolder.OnOff onOffType) throws CouldNotTransformException {
		switch (onOffType) {
			case OFF:
				return ButtonState.State.RELEASED;
			case ON:
				return ButtonState.State.CLICKED;
			default:
				throw new CouldNotTransformException("Could not transform " + OnOffHolderType.OnOffHolder.OnOff.class.getName() + "! " + OnOffHolderType.OnOffHolder.OnOff.class.getSimpleName() + "[" + onOffType.name() + "] is unknown!");
		}
	}

	public static OnOffHolderType.OnOffHolder transform(ButtonState.State buttonState) throws TypeNotSupportedException, CouldNotTransformException {
		switch (buttonState) {
			case RELEASED:
				return OnOffHolderType.OnOffHolder.newBuilder().setState(OnOffHolderType.OnOffHolder.OnOff.OFF).build();
			case CLICKED:
				return OnOffHolderType.OnOffHolder.newBuilder().setState(OnOffHolderType.OnOffHolder.OnOff.ON).build();
			case UNKNOWN:
				throw new TypeNotSupportedException(buttonState, OnOffHolderType.OnOffHolder.OnOff.class);
			default:
				throw new CouldNotTransformException("Could not transform " + ButtonState.State.class.getName() + "! " + ButtonState.State.class.getSimpleName() + "[" + buttonState.name() + "] is unknown!");
		}
	}
}
