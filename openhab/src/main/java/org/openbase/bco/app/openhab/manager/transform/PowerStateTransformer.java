package org.openbase.bco.app.openhab.manager.transform;

/*
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

import org.eclipse.smarthome.core.library.types.OnOffType;
import org.openbase.jul.exception.CouldNotTransformException;
import org.openbase.jul.exception.TypeNotSupportedException;
import rst.domotic.binding.openhab.OnOffHolderType;
import rst.domotic.state.PowerStateType.PowerState;

/**
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public class PowerStateTransformer {

    public static PowerState transform(OnOffType onOffType) throws CouldNotTransformException {
        switch (onOffType) {
            case OFF:
                return PowerState.newBuilder().setValue(PowerState.State.OFF).build();
            case ON:
                return PowerState.newBuilder().setValue(PowerState.State.ON).build();
            default:
                throw new CouldNotTransformException("Could not transform " + OnOffHolderType.OnOffHolder.OnOff.class.getSimpleName() + "[" + onOffType.name() + "] is unknown!");
        }
    }

    public static OnOffType transform(PowerState powerState) throws TypeNotSupportedException, CouldNotTransformException {
        switch (powerState.getValue()) {
            case OFF:
                return OnOffType.OFF;
            case ON:
                return OnOffType.ON;
            case UNKNOWN:
                throw new TypeNotSupportedException(powerState, OnOffType.class);
            default:
                throw new CouldNotTransformException("Could not transform " + PowerState.State.class.getSimpleName() + "[" + powerState.getValue().name() + "] is unknown!");
        }
    }
}
