/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.data.transform;

import de.citec.dal.exception.RSBBindingException;
import de.citec.dal.exception.TypeNotSupportedException;
import rst.homeautomation.states.OpenClosedType;
import rst.homeautomation.openhab.OpenClosedHolderType;

/**
 *
 * @author thuxohl
 */
public class OpenClosedStateTransformer {

	public static OpenClosedType.OpenClosed.OpenClosedState transform(OpenClosedHolderType.OpenClosedHolder.OpenClosed openClosedType) throws RSBBindingException {
		switch (openClosedType) {
			case CLOSED:
				return OpenClosedType.OpenClosed.OpenClosedState.CLOSED;
			case OPEN:
				return OpenClosedType.OpenClosed.OpenClosedState.OPEN;
			default:
				throw new RSBBindingException("Could not transform " + OpenClosedHolderType.OpenClosedHolder.OpenClosed.class.getName() + "! " + OpenClosedHolderType.OpenClosedHolder.OpenClosed.class.getSimpleName() + "[" + openClosedType.name() + "] is unknown!");
		}
	}

	public static OpenClosedHolderType.OpenClosedHolder transform(OpenClosedType.OpenClosed.OpenClosedState openClosedState) throws TypeNotSupportedException, RSBBindingException {
		switch (openClosedState) {
			case CLOSED:
				return OpenClosedHolderType.OpenClosedHolder.newBuilder().setState(OpenClosedHolderType.OpenClosedHolder.OpenClosed.CLOSED).build();
			case OPEN:
				return OpenClosedHolderType.OpenClosedHolder.newBuilder().setState(OpenClosedHolderType.OpenClosedHolder.OpenClosed.OPEN).build();
			case UNKNOWN:
				throw new TypeNotSupportedException(openClosedState, OpenClosedHolderType.OpenClosedHolder.OpenClosed.class);
			default:
				throw new RSBBindingException("Could not transform " + OpenClosedType.OpenClosed.OpenClosedState.class.getName() + "! " + OpenClosedType.OpenClosed.OpenClosedState.class.getSimpleName() + "[" + openClosedState.name() + "] is unknown!");
		}
	}
}
