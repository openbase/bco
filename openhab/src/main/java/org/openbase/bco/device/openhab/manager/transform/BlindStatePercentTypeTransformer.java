package org.openbase.bco.device.openhab.manager.transform;

/*-
 * #%L
 * BCO Openhab Device Manager
 * %%
 * Copyright (C) 2015 - 2019 openbase.org
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

import org.eclipse.smarthome.core.library.types.PercentType;
import org.openbase.type.domotic.state.BlindStateType.BlindState;

import java.math.BigDecimal;

public class BlindStatePercentTypeTransformer implements ServiceStateCommandTransformer<BlindState, PercentType> {

    @Override
    public BlindState transform(final PercentType decimalType) {
        return BlindState.newBuilder().setOpeningRatio(decimalType.doubleValue() / 100d).build();
    }

    @Override
    public PercentType transform(final BlindState blindState) {
        return new PercentType(BigDecimal.valueOf(blindState.getOpeningRatio() * 100d));
    }
}
