package org.openbase.bco.manager.device.binding.openhab.transform;

/*
 * #%L
 * COMA DeviceManager Binding OpenHAB
 * %%
 * Copyright (C) 2015 - 2016 openbase.org
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

import org.openbase.jul.exception.CouldNotTransformException;
import org.openbase.jul.exception.TypeNotSupportedException;
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
