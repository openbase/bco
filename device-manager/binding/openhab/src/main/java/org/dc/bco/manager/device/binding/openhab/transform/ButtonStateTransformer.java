/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dc.bco.manager.device.binding.openhab.transform;

import org.dc.jul.exception.CouldNotTransformException;
import org.dc.jul.exception.TypeNotSupportedException;
import rst.homeautomation.openhab.OnOffHolderType;
import rst.homeautomation.state.ButtonStateType.ButtonState;
import rst.homeautomation.state.ButtonStateType.ButtonState.State;

/**
 *
 * @author mpohling
 */
public class ButtonStateTransformer {

	public static ButtonState transform(OnOffHolderType.OnOffHolder.OnOff onOffType) throws CouldNotTransformException {
		switch (onOffType) {
			case OFF:
				return ButtonState.newBuilder().setValue(State.RELEASED).build();
			case ON:
                return ButtonState.newBuilder().setValue(State.CLICKED).build();
			default:
				throw new CouldNotTransformException("Could not transform " + OnOffHolderType.OnOffHolder.OnOff.class.getName() + "! " + OnOffHolderType.OnOffHolder.OnOff.class.getSimpleName() + "[" + onOffType.name() + "] is unknown!");
		}
	}

	public static OnOffHolderType.OnOffHolder transform(ButtonState buttonState) throws TypeNotSupportedException, CouldNotTransformException {
		switch (buttonState.getValue()) {
			case RELEASED:
				return OnOffHolderType.OnOffHolder.newBuilder().setState(OnOffHolderType.OnOffHolder.OnOff.OFF).build();
			case CLICKED:
				return OnOffHolderType.OnOffHolder.newBuilder().setState(OnOffHolderType.OnOffHolder.OnOff.ON).build();
			case UNKNOWN:
				throw new TypeNotSupportedException(buttonState, OnOffHolderType.OnOffHolder.OnOff.class);
			default:
				throw new CouldNotTransformException("Could not transform " + ButtonState.State.class.getName() + "! " + ButtonState.State.class.getSimpleName() + "[" + buttonState.getValue().name() + "] is unknown!");
		}
	}
}
