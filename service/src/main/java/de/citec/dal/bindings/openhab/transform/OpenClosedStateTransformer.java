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
import rst.homeautomation.state.OpenClosedType;
import rst.homeautomation.openhab.OpenClosedHolderType;

/**
 *
 * @author thuxohl
 */
public class OpenClosedStateTransformer {

	public static OpenClosedType.OpenClosed.OpenClosedState transform(OpenClosedHolderType.OpenClosedHolder.OpenClosed openClosedType) throws CouldNotTransformException {
		switch (openClosedType) {
			case CLOSED:
				return OpenClosedType.OpenClosed.OpenClosedState.CLOSED;
			case OPEN:
				return OpenClosedType.OpenClosed.OpenClosedState.OPEN;
			default:
				throw new CouldNotTransformException("Could not transform " + OpenClosedHolderType.OpenClosedHolder.OpenClosed.class.getName() + "! " + OpenClosedHolderType.OpenClosedHolder.OpenClosed.class.getSimpleName() + "[" + openClosedType.name() + "] is unknown!");
		}
	}

	public static OpenClosedHolderType.OpenClosedHolder transform(OpenClosedType.OpenClosed.OpenClosedState openClosedState) throws CouldNotTransformException {
		try {
			switch (openClosedState) {
				case CLOSED:
					return OpenClosedHolderType.OpenClosedHolder.newBuilder().setState(OpenClosedHolderType.OpenClosedHolder.OpenClosed.CLOSED).build();
				case OPEN:
					return OpenClosedHolderType.OpenClosedHolder.newBuilder().setState(OpenClosedHolderType.OpenClosedHolder.OpenClosed.OPEN).build();
				case UNKNOWN:
					throw new InvalidStateException("Unknown state is invalid!");
				default:
					throw new TypeNotSupportedException(openClosedState, OpenClosedHolderType.OpenClosedHolder.class);
			}
		} catch (CouldNotPerformException ex) {
			throw new CouldNotTransformException("Could not transform " + OpenClosedHolderType.OpenClosedHolder.OpenClosed.class.getName() + "!", ex);
		}

	}
}
