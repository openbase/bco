package org.openbase.bco.manager.device.binding.openhab.transform;

/*
 * #%L
 * COMA DeviceManager Binding OpenHAB
 * %%
 * Copyright (C) 2015 - 2016 openbase.org
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

import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.CouldNotTransformException;
import org.openbase.jul.exception.InvalidStateException;
import org.openbase.jul.exception.TypeNotSupportedException;
import rst.homeautomation.state.HandleStateType.HandleState;

/**
 *
 * @author mpohling
 */
public class OpenClosedTiltedStateTransformer {

    public static HandleState.State transform(final String stringType) throws CouldNotTransformException {
        try {
            return HandleState.State.valueOf(stringType);
        } catch (Exception ex) {
            throw new CouldNotTransformException("Could not transform " + String.class.getName() + "! " + String.class.getSimpleName() + "[" + stringType + "] is unknown!", ex);
        }
    }

    public static String transform(final HandleState.State handleState) throws CouldNotTransformException {
        try {
            switch (handleState) {
                case CLOSED:
                    return "CLOSED";
                case OPEN:
                    return "OPEN";
                case TILTED:
                    return "TILTED";
                case UNKNOWN:
                    throw new InvalidStateException("Unknown state is invalid!");
                default:
                    throw new TypeNotSupportedException(handleState, String.class);
            }
        } catch (CouldNotPerformException ex) {
            throw new CouldNotTransformException("Could not transform " + HandleState.State.class.getName() + "!", ex);
        }
    }
}
