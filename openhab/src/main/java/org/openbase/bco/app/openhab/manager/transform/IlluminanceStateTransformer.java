package org.openbase.bco.app.openhab.manager.transform;

import org.eclipse.smarthome.core.library.types.DecimalType;
import rst.domotic.state.IlluminanceStateType.IlluminanceState;

/*-
 * #%L
 * BCO Openhab App
 * %%
 * Copyright (C) 2018 openbase.org
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

/**
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class IlluminanceStateTransformer {

    /**
     * Transform a number to an illuminationState by setting the number as the illuminance.
     *
     * @param decimalType the brightness value
     * @return the corresponding brightness state
     */
    public static IlluminanceState transform(final DecimalType decimalType) {
        IlluminanceState.Builder state = IlluminanceState.newBuilder();
        state.setIlluminance(decimalType.doubleValue());
        return state.build();
    }

    /**
     * Get the illuminance value.
     *
     * @param illuminanceState the state
     * @return the current illuminance
     */
    public static DecimalType transform(IlluminanceState illuminanceState) {
        return new DecimalType(illuminanceState.getIlluminance());
    }
}
