package org.openbase.bco.device.openhab.manager.transform;

/*
 * #%L
 * BCO Openhab Device Manager
 * %%
 * Copyright (C) 2015 - 2019 openbase.org
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

import org.eclipse.smarthome.core.library.types.DecimalType;
import org.openbase.jul.exception.CouldNotTransformException;
import org.openbase.type.domotic.state.SmokeStateType.SmokeState;
import org.openbase.type.domotic.state.SmokeStateType.SmokeState.State;

/**
 * * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class SmokeStateDecimalTypeTransformer implements ServiceStateCommandTransformer<SmokeState, DecimalType> {

    //TODO: check if the values from openhab match this transformation
    @Override
    public SmokeState transform(final DecimalType decimalType) throws CouldNotTransformException {
        SmokeState.Builder smokeState = SmokeState.newBuilder();
        try {
            smokeState.setSmokeLevel(decimalType.doubleValue() / 100d);
            if (decimalType.intValue() == 0) {
                smokeState.setValue(State.NO_SMOKE);
            } else if (decimalType.intValue() < 20) {
                smokeState.setValue(State.SOME_SMOKE);
            } else {
                smokeState.setValue(State.SMOKE);
            }
            return smokeState.build();
        } catch (Exception ex) {
            throw new CouldNotTransformException("Could not transform " + DecimalType.class.getSimpleName() + "[" + decimalType + "] is unknown!", ex);
        }
    }

    @Override
    public DecimalType transform(final SmokeState smokeState) throws CouldNotTransformException {
        if(!smokeState.isInitialized() || Double.isNaN(smokeState.getSmokeLevel())) {
            throw new CouldNotTransformException("Given smoke state is not initialized!");
        }
        return new DecimalType(smokeState.getSmokeLevel() * 100d);
    }
}
