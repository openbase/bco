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
import rst.domotic.state.AlarmStateType.AlarmState;
import rst.domotic.state.AlarmStateType.AlarmState.State;

/**
 * * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class AlarmStateTransformer {

    //TODO: check if the values from openhab match this transformation
    public static AlarmState transform(final DecimalType decimalType) throws CouldNotTransformException {
        switch (decimalType.intValue()) {
            case 0:
                return AlarmState.newBuilder().setValue(State.NO_ALARM).build();
            case 1:
                return AlarmState.newBuilder().setValue(State.ALARM).build();
            default:
                throw new CouldNotTransformException("Could not transform " + DecimalType.class.getSimpleName() + "[" + decimalType + "] is unknown!");
        }
    }

    public static DecimalType transform(final AlarmState alarmState) throws TypeNotSupportedException, CouldNotTransformException {
        switch (alarmState.getValue()) {
            case NO_ALARM:
                return new DecimalType(0);
            case ALARM:
                return new DecimalType(1);
            case UNKNOWN:
                throw new TypeNotSupportedException(alarmState, DecimalType.class);
            default:
                throw new CouldNotTransformException("Could not transform " + AlarmState.class.getSimpleName() + "[" + alarmState.getValue().name() + "] is unknown!");
        }
    }
}
