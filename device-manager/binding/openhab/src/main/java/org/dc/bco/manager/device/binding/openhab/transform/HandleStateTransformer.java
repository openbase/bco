/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dc.bco.manager.device.binding.openhab.transform;

import org.dc.jul.exception.CouldNotTransformException;
import org.dc.jul.processing.StringProcessor;
import rst.homeautomation.state.HandleStateType.HandleState;

/**
 *
 * @author <a href="mailto:mpohling@cit-ec.uni-bielefeld.de">Divine Threepwood</a>
 */
public class HandleStateTransformer {

    public static HandleState.State transform(final String value) throws CouldNotTransformException {
        try {
            return HandleState.State.valueOf(StringProcessor.transformToUpperCase(value));
        } catch (Exception ex) {
            throw new CouldNotTransformException("Could not transform " + String.class.getName() + "! " + String.class.getSimpleName() + "[" + value + "] is not a valid " + HandleState.State.class.getSimpleName() + "!", ex);
        }
    }

    public static String transform(final HandleState.State value) throws CouldNotTransformException {

        try {
            return StringProcessor.transformToUpperCase(value.name());
        } catch (Exception ex) {
            throw new CouldNotTransformException("Could not transform " + HandleState.State.class.getName() + "[" + value + "] to " + String.class.getSimpleName() + "!", ex);
        }
    }
}
