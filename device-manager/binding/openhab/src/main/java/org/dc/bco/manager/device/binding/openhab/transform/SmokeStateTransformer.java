/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dc.bco.manager.device.binding.openhab.transform;

import org.dc.jul.exception.CouldNotTransformException;
import rst.homeautomation.state.SmokeStateType.SmokeState;
import rst.homeautomation.state.SmokeStateType.SmokeState.State;

/**
 *
 * @author thuxohl
 */
public class SmokeStateTransformer {

    //TODO: check if the values from openhab match this transoformation
    public static SmokeState transform(final Double decimalType) throws CouldNotTransformException {
        SmokeState.Builder smokeState = SmokeState.newBuilder();
        try {
            smokeState.setSmokeLevel(decimalType);
            if (decimalType == 0) {
                smokeState.setValue(State.NO_SMOKE);
            } else if (decimalType < 20) {
                smokeState.setValue(State.SOME_SMOKE);
            } else {
                smokeState.setValue(State.SMOKE);
            }
            return smokeState.build();
        } catch (Exception ex) {
            throw new CouldNotTransformException("Could not transform " + Double.class.getName() + "! " + Double.class.getSimpleName() + "[" + decimalType + "] is unknown!", ex);
        }
    }

    public static Double transform(final SmokeState smokeState) {
        return smokeState.getSmokeLevel();
    }
}
