package org.openbase.bco.manager.device.binding.openhab.transform;

/*
 * #%L
 * COMA DeviceManager Binding OpenHAB
 * %%
 * Copyright (C) 2015 - 2016 openbase.org
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

import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.CouldNotTransformException;
import org.openbase.jul.exception.InvalidStateException;
import org.openbase.jul.exception.TypeNotSupportedException;
import rst.homeautomation.state.ReedSwitchStateType.ReedSwitchState;
import rst.homeautomation.openhab.OpenClosedHolderType;

/**
 *
 * @author thuxohl
 */
public class OpenClosedStateTransformer {

	public static ReedSwitchState.State transform(OpenClosedHolderType.OpenClosedHolder.OpenClosed openClosedType) throws CouldNotTransformException {
		switch (openClosedType) {
			case CLOSED:
				return ReedSwitchState.State.CLOSED;
			case OPEN:
				return ReedSwitchState.State.OPEN;
			default:
				throw new CouldNotTransformException("Could not transform " + OpenClosedHolderType.OpenClosedHolder.OpenClosed.class.getName() + "! " + OpenClosedHolderType.OpenClosedHolder.OpenClosed.class.getSimpleName() + "[" + openClosedType.name() + "] is unknown!");
		}
	}

	public static OpenClosedHolderType.OpenClosedHolder transform(ReedSwitchState.State reedSwitchState) throws CouldNotTransformException {
		try {
			switch (reedSwitchState) {
				case CLOSED:
					return OpenClosedHolderType.OpenClosedHolder.newBuilder().setState(OpenClosedHolderType.OpenClosedHolder.OpenClosed.CLOSED).build();
				case OPEN:
					return OpenClosedHolderType.OpenClosedHolder.newBuilder().setState(OpenClosedHolderType.OpenClosedHolder.OpenClosed.OPEN).build();
				case UNKNOWN:
					throw new InvalidStateException("Unknown state is invalid!");
				default:
					throw new TypeNotSupportedException(reedSwitchState, OpenClosedHolderType.OpenClosedHolder.class);
			}
		} catch (CouldNotPerformException ex) {
			throw new CouldNotTransformException("Could not transform " + ReedSwitchState.State.class.getName() + "!", ex);
		}

	}
}
