package org.openbase.bco.app.openhab.manager.transform;

/*-
 * #%L
 * JUL Extension OpenHAB
 * %%
 * Copyright (C) 2015 - 2018 openbase.org
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 *
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-3.0.html>.
 * #L%
 */

import org.eclipse.smarthome.core.library.types.PercentType;
import rst.domotic.state.BrightnessStateType.BrightnessState;
import rst.domotic.state.BrightnessStateType.BrightnessState.Builder;

/**
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class BrightnessStateTransformer {

    /**
     * Transform a number to a brightness state by setting the number as the brightness value.
     *
     * @param percentType the brightness value
     * @return the corresponding brightness state
     */
    public static BrightnessState transform(final PercentType percentType) {
        Builder state = BrightnessState.newBuilder();
        state.setBrightness(percentType.doubleValue());
        return state.build();
    }

    /**
     * Get the brightness value.
     *
     * @param brightnessState the state
     * @return the current brightness value
     */
    public static PercentType transform(BrightnessState brightnessState) {
        return new PercentType((int) brightnessState.getBrightness());
    }
}
