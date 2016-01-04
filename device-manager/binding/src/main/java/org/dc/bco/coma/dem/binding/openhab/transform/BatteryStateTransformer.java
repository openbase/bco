/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dc.bco.coma.dem.binding.openhab.transform;

import rst.homeautomation.state.BatteryStateType.BatteryState;

/**
 *
 * @author thuxohl
 */
public class BatteryStateTransformer {

    /**
     * Transform a number to a battery state. The number is set as the level and
     * should be between 0 and 100. If the level is higher than 15 the battery
     * state is set as okay, higher than 3 means critical and between 0 and 3
     * means insufficient. If the value is smaller than 0 unknown is set as the
     * battery state.
     *
     * @param value the battery level between 0 and 100
     * @return the corresponding battery state
     */
    public static BatteryState transform(final Double value) {
        BatteryState.Builder state = BatteryState.newBuilder();
        state.setLevel(value);
        if (value > 15) {
            state.setValue(BatteryState.State.OK);
        } else if (value > 3) {
            state.setValue(BatteryState.State.CRITICAL);
        } else if (value >= 0) {
            state.setValue(BatteryState.State.INSUFFICIENT);
        } else {
            state.setValue(BatteryState.State.UNKNOWN);
        }
        return state.build();
    }

    /**
     * Get the battery level between 0 and 100.
     *
     * @param batteryState the state
     * @return the current battery level
     */
    public static Double transform(BatteryState batteryState) {
        return batteryState.getLevel();
    }
}
