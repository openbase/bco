/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dc.bco.manager.device.binding.openhab.transform;

import org.dc.jul.exception.CouldNotPerformException;
import org.dc.jul.exception.CouldNotTransformException;
import org.dc.jul.exception.InvalidStateException;
import org.dc.jul.exception.TypeNotSupportedException;
import rst.homeautomation.state.HandleStateType.HandleState;

/**
 *
 * @author mpohling
 */
public class OpenClosedTiltedStateTransformer {

    public static HandleState.State transform(final String stringType) throws CouldNotTransformException {
        try {
            return HandleState.State.valueOf(stringType);
        } catch (Exception ex) {
            throw new CouldNotTransformException("Could not transform " + String.class.getName() + "! " + String.class.getSimpleName() + "[" + stringType + "] is unknown!", ex);
        }
    }

    public static String transform(final HandleState.State handleState) throws CouldNotTransformException {
        try {
            switch (handleState) {
                case CLOSED:
                    return "CLOSED";
                case OPEN:
                    return "OPEN";
                case TILTED:
                    return "TILTED";
                case UNKNOWN:
                    throw new InvalidStateException("Unknown state is invalid!");
                default:
                    throw new TypeNotSupportedException(handleState, String.class);
            }
        } catch (CouldNotPerformException ex) {
            throw new CouldNotTransformException("Could not transform " + HandleState.State.class.getName() + "!", ex);
        }
    }
}
