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

import rst.homeautomation.state.PowerConsumptionStateType.PowerConsumptionState;

/**
 *
 * @author thuxohl
 */
public class PowerConsumptionStateTransformer {

    //TODO: has to be part of the unit
    /**
     * Default voltage of a socket in volt.
     */
    public static final Double DEFAULTVOLTAGE = 230d;
    /**
     * Value to calculate from milli to normal or vice versa.
     */
    private static final Double MILLITOAMPERE = 1000d;

    /**
     * OpenHAB receives the value for the current in milli ampere and therefore
     * must be transformed.
     *
     * @param value the new value for the current in mA
     * @return a PowerConsumptionState with the given current and a voltage of
     * 230V
     */
    public static PowerConsumptionState transform(final Double value) {
        PowerConsumptionState.Builder state = PowerConsumptionState.newBuilder();
        state.setCurrent(value / MILLITOAMPERE);
        state.setVoltage(DEFAULTVOLTAGE);
        state.setConsumption(state.getCurrent() * state.getVoltage());
        return state.build();
    }

    /**
     * Get the current in milli ampere from a PowerConsumptionState.
     *
     * @param powerConsumptionState the state
     * @return the current in milli ampere
     */
    public static Double transform(PowerConsumptionState powerConsumptionState) {
        return powerConsumptionState.getCurrent() * MILLITOAMPERE;
    }
}
