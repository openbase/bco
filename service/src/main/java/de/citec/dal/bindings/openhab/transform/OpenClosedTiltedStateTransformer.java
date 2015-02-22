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
import rst.homeautomation.state.OpenClosedTiltedType;

/**
 *
 * @author mpohling
 */
public class OpenClosedTiltedStateTransformer {

	public static OpenClosedTiltedType.OpenClosedTilted.OpenClosedTiltedState transform(final String stringType) throws CouldNotTransformException {
		try {
			return OpenClosedTiltedType.OpenClosedTilted.OpenClosedTiltedState.valueOf(stringType);
		} catch (Exception ex) {
			throw new CouldNotTransformException("Could not transform " + String.class.getName() + "! " + String.class.getSimpleName() + "[" + stringType + "] is unknown!", ex);
		}
	}

	public static String transform(final OpenClosedTiltedType.OpenClosedTilted.OpenClosedTiltedState openClosedTiltedState) throws CouldNotTransformException {
		try {
			switch (openClosedTiltedState) {
				case CLOSED:
					return "CLOSED";
				case OPEN:
					return "OPEN";
				case TILTED:
					return "TILTED";
				case UNKNOWN:
					throw new InvalidStateException("Unknown state is invalid!");
				default:
					throw new TypeNotSupportedException(openClosedTiltedState, String.class);
			}
		} catch (CouldNotPerformException ex) {
			throw new CouldNotTransformException("Could not transform " + OpenClosedTiltedType.OpenClosedTilted.OpenClosedTiltedState.class.getName() + "!", ex);
		}
	}
}
