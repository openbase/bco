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
import org.openbase.type.domotic.state.BatteryStateType.BatteryState;
import org.openbase.type.domotic.state.BatteryStateType.BatteryState.State;

/**
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class BatteryStateDecimalTypeTransformer implements ServiceStateCommandTransformer<BatteryState, DecimalType> {

    /**
     * Transform a number to a battery state. The number is set as the level and
     * should be between 0 and 1.
     *
     * @param decimalType the battery level between 0 and 1
     *
     * @return the corresponding battery state
     */
    @Override
    public BatteryState transform(final DecimalType decimalType) {
        BatteryState.Builder state = BatteryState.newBuilder();
        state.setLevel(decimalType.doubleValue() / 100d);

        return state.build();
    }

    /**
     * Get the battery level between 0 and 1.
     *
     * @param batteryState the state
     *
     * @return the current battery level
     */
    @Override
    public DecimalType transform(BatteryState batteryState) {
        return new DecimalType(batteryState.getLevel() * 100d);
    }
}
