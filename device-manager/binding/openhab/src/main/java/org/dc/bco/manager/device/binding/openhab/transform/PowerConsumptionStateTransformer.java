/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dc.bco.manager.device.binding.openhab.transform;

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
