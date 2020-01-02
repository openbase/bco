package org.openbase.bco.device.openhab.manager.transform;

/*
 * #%L
 * BCO Openhab Device Manager
 * %%
 * Copyright (C) 2015 - 2020 openbase.org
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
import org.openbase.type.domotic.state.TemperatureStateType.TemperatureState;

/**
 * * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class TemperatureStateDecimalTypeTransformer implements ServiceStateCommandTransformer<TemperatureState, DecimalType> {

    @Override
    public TemperatureState transform(final DecimalType decimalType) {
        return TemperatureState.newBuilder().setTemperature(decimalType.doubleValue()).setTemperatureDataUnit(TemperatureState.DataUnit.CELSIUS).build();
    }

    @Override
    public DecimalType transform(final TemperatureState temperatureState) throws CouldNotTransformException {
        try {
            return new DecimalType(temperatureState.getTemperature());
        } catch (NumberFormatException ex) {
            throw new CouldNotTransformException(TemperatureState.class.getSimpleName(), DecimalType.class.getSimpleName(), ex);
        }
    }
}
