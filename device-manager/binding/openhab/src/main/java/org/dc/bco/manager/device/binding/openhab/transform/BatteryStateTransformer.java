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
