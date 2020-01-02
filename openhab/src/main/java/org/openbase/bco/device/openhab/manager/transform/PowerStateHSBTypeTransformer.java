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
import org.eclipse.smarthome.core.library.types.HSBType;
import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.library.types.PercentType;
import org.openbase.jul.exception.CouldNotTransformException;
import org.openbase.jul.exception.TypeNotSupportedException;
import org.openbase.type.domotic.binding.openhab.OnOffHolderType;
import org.openbase.type.domotic.state.PowerStateType.PowerState;

/**
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public class PowerStateHSBTypeTransformer implements ServiceStateCommandTransformer<PowerState, HSBType> {

    @Override
    public PowerState transform(HSBType hsbType) throws CouldNotTransformException {

        if (hsbType.getBrightness().doubleValue() > 0) {
            return PowerState.newBuilder().setValue(PowerState.State.ON).build();
        } else if (hsbType.getBrightness().doubleValue() == 0) {
            return PowerState.newBuilder().setValue(PowerState.State.OFF).build();
        } else {
            throw new CouldNotTransformException("Could not transform " + HSBType.class.getSimpleName() + "[" + hsbType + "] is unknown!");
        }
    }

    @Override
    public HSBType transform(PowerState powerState) throws TypeNotSupportedException, CouldNotTransformException {
        switch (powerState.getValue()) {
            case OFF:
                return new HSBType(new DecimalType(0), new PercentType(0), new PercentType(0));
            case ON:
                return new HSBType(new DecimalType(0), new PercentType(0), new PercentType(100));
            case UNKNOWN:
                throw new TypeNotSupportedException(powerState, HSBType.class);
            default:
                throw new CouldNotTransformException("Could not transform " + PowerState.State.class.getSimpleName() + "[" + powerState.getValue().name() + "] is unknown!");
        }
    }
}
