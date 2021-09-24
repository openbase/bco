package org.openbase.bco.device.openhab.manager.transform;

/*-
 * #%L
 * BCO Openhab Device Manager
 * %%
 * Copyright (C) 2015 - 2021 openbase.org
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

import org.openbase.type.domotic.state.BrightnessStateType.BrightnessState;
import org.openbase.type.domotic.state.BrightnessStateType.BrightnessState.Builder;
import org.openhab.core.library.types.PercentType;

import java.math.BigDecimal;

/**
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class BrightnessStatePercentTypeTransformer implements ServiceStateCommandTransformer<BrightnessState, PercentType> {

    /**
     * Transform a number to a brightness state by setting the number as the brightness value.
     *
     * @param percentType the brightness value
     * @return the corresponding brightness state
     */
    @Override
    public BrightnessState transform(final PercentType percentType) {
        Builder state = BrightnessState.newBuilder();
        state.setBrightness(percentType.doubleValue() / 100d);
        return state.build();
    }

    /**
     * Get the brightness value.
     *
     * @param brightnessState the state
     * @return the current brightness value
     */
    @Override
    public PercentType transform(BrightnessState brightnessState) {
        return new PercentType(BigDecimal.valueOf(brightnessState.getBrightness() * 100d));
    }
}
