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
import org.openbase.type.domotic.state.PowerConsumptionStateType.PowerConsumptionState;

/**
 * * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class PowerConsumptionStateDecimalTypeTransformer implements ServiceStateCommandTransformer<PowerConsumptionState, DecimalType> {

    //TODO: has to be part of the unit
    /**
     * Default voltage of a socket in volt.
     */
    public static final Double DEFAULT_VOLTAGE = 230d;
    /**
     * Value to calculate from milli to normal or vice versa.
     */
    private static final Double MILLI_TO_AMPERE = 1000d;

    /**
     * OpenHAB receives the value for the current in milli ampere and therefore
     * must be transformed.
     *
     * @param decimalType the new value for the current in mA
     *
     * @return a PowerConsumptionState with the given current and a voltage of
     * 230V
     */
    @Override
    public PowerConsumptionState transform(final DecimalType decimalType) {
        PowerConsumptionState.Builder state = PowerConsumptionState.newBuilder();
        state.setCurrent(decimalType.doubleValue() / MILLI_TO_AMPERE);
        state.setVoltage(DEFAULT_VOLTAGE);
        state.setConsumption(state.getCurrent() * state.getVoltage());
        return state.build();
    }

    /**
     * Get the current in milli ampere from a PowerConsumptionState.
     *
     * @param powerConsumptionState the state
     *
     * @return the current in milli ampere
     */
    @Override
    public DecimalType transform(PowerConsumptionState powerConsumptionState) {
        return new DecimalType(powerConsumptionState.getCurrent() * MILLI_TO_AMPERE);
    }
}
