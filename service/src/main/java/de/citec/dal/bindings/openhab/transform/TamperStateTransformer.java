/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.bindings.openhab.transform;

import org.dc.jul.exception.CouldNotTransformException;
import org.dc.jul.exception.TypeNotSupportedException;
import rst.homeautomation.state.TamperStateType.TamperState;

/**
 *
 * @author thuxohl
 */
public class TamperStateTransformer {

    public static TamperState transform(final double state) throws CouldNotTransformException {
        if (state == 0) {
            return TamperState.newBuilder().setValue(TamperState.State.NO_TAMPER).build();
        } else if (state > 0) {
            //TODO:mpohling/thuxohl adjust the tamper state to also reflect the intensity of the alarm!
            return TamperState.newBuilder().setValue(TamperState.State.TAMPER).build();
        } else {
            throw new CouldNotTransformException("Could not transform " + Double.class.getName() + "! " + Double.class.getSimpleName() + "[" + state + "] is unknown!");
        }
    }

    public static double transform(final TamperState tamperState) throws TypeNotSupportedException, CouldNotTransformException {
        switch (tamperState.getValue()) {
            case NO_TAMPER:
                return 0d;
            case TAMPER:
                return 1d;
            case UNKNOWN:
                throw new TypeNotSupportedException(tamperState, Double.class);
            default:
                throw new CouldNotTransformException("Could not transform " + TamperState.State.class.getName() + "! " + TamperState.State.class.getSimpleName() + "[" + tamperState.getValue().name() + "] is unknown!");
        }
    }
}
