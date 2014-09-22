/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.data.transform;

import de.citec.dal.exception.RSBBindingException;
import de.citec.dal.exception.TypeNotSupportedException;
import org.openhab.core.library.types.StringType;
import rst.homeautomation.states.OpenClosedTiltedType;

/**
 *
 * @author thuxohl
 */
public class OpenClosedTiltedStateTransformer {

	public static OpenClosedTiltedType.OpenClosedTilted.OpenClosedTiltedState transform(StringType stringType) throws RSBBindingException {
		switch (stringType.toString()) {
			case "CLOSED":
				return OpenClosedTiltedType.OpenClosedTilted.OpenClosedTiltedState.CLOSED;
			case "OPEN":
				return OpenClosedTiltedType.OpenClosedTilted.OpenClosedTiltedState.OPEN;
            case "TILTED":
                return OpenClosedTiltedType.OpenClosedTilted.OpenClosedTiltedState.TILTED;
			default:
				throw new RSBBindingException("Could not transform " + StringType.class.getName() + "! " + StringType.class.getSimpleName() + "[" + stringType.toString() + "] is unknown!");
		}
	}

	public static StringType transform(OpenClosedTiltedType.OpenClosedTilted.OpenClosedTiltedState openClosedTiltedState) throws TypeNotSupportedException, RSBBindingException {
		switch (openClosedTiltedState) {
			case CLOSED:
				return new StringType("CLOSED");
			case OPEN:
				return new StringType("OPEN");
            case TILTED:
                return new StringType("TILTED");
			case UNKNOWN:
				throw new TypeNotSupportedException(openClosedTiltedState, StringType.class);
			default:
				throw new RSBBindingException("Could not transform " + OpenClosedTiltedType.OpenClosedTilted.OpenClosedTiltedState.class.getName() + "! " + OpenClosedTiltedType.OpenClosedTilted.OpenClosedTiltedState.class.getSimpleName() + "[" + openClosedTiltedState.name() + "] is unknown!");
		}
	}
}
