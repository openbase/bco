/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.bindings.openhab.transform;

import de.citec.jul.exception.CouldNotPerformException;
import de.citec.jul.exception.CouldNotTransformException;
import de.citec.jul.exception.InvalidStateException;
import de.citec.jul.exception.TypeNotSupportedException;
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
