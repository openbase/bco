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
