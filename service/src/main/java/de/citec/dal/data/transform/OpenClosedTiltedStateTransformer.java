/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.data.transform;

import de.citec.dal.exception.RSBBindingException;
import de.citec.jul.exception.TypeNotSupportedException;
import rst.homeautomation.states.OpenClosedTiltedType;

/**
 *
 * @author thuxohl
 */
public class OpenClosedTiltedStateTransformer {

    public static OpenClosedTiltedType.OpenClosedTilted.OpenClosedTiltedState transform(final String stringType) throws RSBBindingException {
        switch (stringType) {
            case "CLOSED":
                return OpenClosedTiltedType.OpenClosedTilted.OpenClosedTiltedState.CLOSED;
            case "OPEN":
                return OpenClosedTiltedType.OpenClosedTilted.OpenClosedTiltedState.OPEN;
            case "TILTED":
                return OpenClosedTiltedType.OpenClosedTilted.OpenClosedTiltedState.TILTED;
            default:
                throw new RSBBindingException("Could not transform " + String.class.getName() + "! " + String.class.getSimpleName() + "[" + stringType+ "] is unknown!");
        }
    }

    public static String transform(final OpenClosedTiltedType.OpenClosedTilted.OpenClosedTiltedState openClosedTiltedState) throws TypeNotSupportedException, RSBBindingException {
        switch (openClosedTiltedState) {
            case CLOSED:
                return "CLOSED";
            case OPEN:
                return "OPEN";
            case TILTED:
                return "TILTED";
            case UNKNOWN:
                throw new TypeNotSupportedException(openClosedTiltedState, String.class);
            default:
                throw new RSBBindingException("Could not transform " + OpenClosedTiltedType.OpenClosedTilted.OpenClosedTiltedState.class.getName() + "! " + OpenClosedTiltedType.OpenClosedTilted.OpenClosedTiltedState.class.getSimpleName() + "[" + openClosedTiltedState.name() + "] is unknown!");
        }
    }
}
