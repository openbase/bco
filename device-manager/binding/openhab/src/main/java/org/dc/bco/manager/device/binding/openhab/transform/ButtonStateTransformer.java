package org.dc.bco.manager.device.binding.openhab.transform;

/*
 * #%L
 * COMA DeviceManager Binding OpenHAB
 * %%
 * Copyright (C) 2015 - 2016 DivineCooperation
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */

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
