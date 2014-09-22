/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.data.transform;

import de.citec.dal.exception.RSBBindingException;
import de.citec.dal.exception.TypeNotSupportedException;
import rst.homeautomation.states.OpenClosedType;

/**
 *
 * @author thuxohl
 */
public class OpenClosedStateTransformer {

	public static OpenClosedType.OpenClosed.OpenClosedState transform(org.openhab.core.library.types.OpenClosedType openClosedType) throws RSBBindingException {
		switch (openClosedType) {
			case CLOSED:
				return OpenClosedType.OpenClosed.OpenClosedState.CLOSED;
			case OPEN:
				return OpenClosedType.OpenClosed.OpenClosedState.OPEN;
			default:
				throw new RSBBindingException("Could not transform " + org.openhab.core.library.types.OpenClosedType.class.getName() + "! " + org.openhab.core.library.types.OpenClosedType.class.getSimpleName() + "[" + openClosedType.name() + "] is unknown!");
		}
	}

	public static org.openhab.core.library.types.OpenClosedType transform(OpenClosedType.OpenClosed.OpenClosedState openClosedState) throws TypeNotSupportedException, RSBBindingException {
		switch (openClosedState) {
			case CLOSED:
				return org.openhab.core.library.types.OpenClosedType.CLOSED;
			case OPEN:
				return org.openhab.core.library.types.OpenClosedType.OPEN;
			case UNKNOWN:
				throw new TypeNotSupportedException(openClosedState, org.openhab.core.library.types.OpenClosedType.class);
			default:
				throw new RSBBindingException("Could not transform " + OpenClosedType.OpenClosed.OpenClosedState.class.getName() + "! " + OpenClosedType.OpenClosed.OpenClosedState.class.getSimpleName() + "[" + openClosedState.name() + "] is unknown!");
		}
	}
}
