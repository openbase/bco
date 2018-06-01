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

import org.eclipse.smarthome.core.library.types.DecimalType;
import org.openbase.jul.exception.CouldNotTransformException;
import org.openbase.jul.exception.TypeNotSupportedException;
import rst.domotic.state.MotionStateType.MotionState;
import rst.domotic.state.MotionStateType.MotionState.State;

/**
 * * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class MotionStateTransformer {

    public static MotionState transform(final DecimalType decimalType) throws CouldNotTransformException {
        MotionState.Builder motionState = MotionState.newBuilder();
        try {
            if (decimalType.intValue() == 0) {
                motionState.setValue(State.NO_MOTION);
            } else {
                motionState.setValue(State.MOTION);
            }
            return motionState.build();
        } catch (Exception ex) {
            throw new CouldNotTransformException("Could not transform " + DecimalType.class.getSimpleName() + "[" + decimalType + "] is unknown!", ex);
        }
    }

    public static DecimalType transform(final MotionState motionState) throws CouldNotTransformException, TypeNotSupportedException {
        switch (motionState.getValue()) {
            case NO_MOTION:
                return new DecimalType(0d);
            case MOTION:
                return new DecimalType(1d);
            case UNKNOWN:
                throw new TypeNotSupportedException(motionState, DecimalType.class);
            default:
                throw new CouldNotTransformException("Could not transform " + MotionState.class.getSimpleName() + "[" + motionState.getValue().name() + "] is unknown!");
        }
    }
}
