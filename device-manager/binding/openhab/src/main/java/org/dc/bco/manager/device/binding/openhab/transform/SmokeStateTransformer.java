package org.dc.bco.manager.device.binding.openhab.transform;

/*
 * #%L
 * COMA DeviceManager Binding OpenHAB
 * %%
 * Copyright (C) 2015 - 2016 DivineCooperation
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */

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
