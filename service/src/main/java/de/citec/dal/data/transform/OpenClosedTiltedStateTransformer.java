/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.data.transform;

import de.citec.dal.exception.RSBBindingException;
import de.citec.dal.exception.TypeNotSupportedException;
import rst.homeautomation.states.OpenClosedTiltedType;
import rst.homeautomation.openhab.StringType;

/**
 *
 * @author thuxohl
 */
public class OpenClosedTiltedStateTransformer {

    public static OpenClosedTiltedType.OpenClosedTilted.OpenClosedTiltedState transform(StringType.String stringType) throws RSBBindingException {
        switch (stringType.getValue()) {
            case "CLOSED":
                return OpenClosedTiltedType.OpenClosedTilted.OpenClosedTiltedState.CLOSED;
            case "OPEN":
                return OpenClosedTiltedType.OpenClosedTilted.OpenClosedTiltedState.OPEN;
            case "TILTED":
                return OpenClosedTiltedType.OpenClosedTilted.OpenClosedTiltedState.TILTED;
            default:
                throw new RSBBindingException("Could not transform " + StringType.String.class.getName() + "! " + StringType.String.class.getSimpleName() + "[" + stringType.toString() + "] is unknown!");
        }
    }

    public static StringType.String transform(OpenClosedTiltedType.OpenClosedTilted.OpenClosedTiltedState openClosedTiltedState) throws TypeNotSupportedException, RSBBindingException {
        switch (openClosedTiltedState) {
            case CLOSED:
                return StringType.String.newBuilder().setValue("CLOSED").build();
            case OPEN:
                return StringType.String.newBuilder().setValue("OPEN").build();
            case TILTED:
                return StringType.String.newBuilder().setValue("TILTED").build();
            case UNKNOWN:
                throw new TypeNotSupportedException(openClosedTiltedState, StringType.String.class);
            default:
                throw new RSBBindingException("Could not transform " + OpenClosedTiltedType.OpenClosedTilted.OpenClosedTiltedState.class.getName() + "! " + OpenClosedTiltedType.OpenClosedTilted.OpenClosedTiltedState.class.getSimpleName() + "[" + openClosedTiltedState.name() + "] is unknown!");
        }
    }
}
