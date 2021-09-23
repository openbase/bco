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

import org.openbase.jul.exception.CouldNotTransformException;
import org.openbase.jul.exception.TypeNotSupportedException;
import org.openbase.type.domotic.binding.openhab.OnOffHolderType;
import org.openbase.type.domotic.state.StandbyStateType.StandbyState;
import org.openbase.type.domotic.state.StandbyStateType.StandbyState.State;
import org.openhab.core.library.types.OnOffType;

public class StandbyStateOnOffTypeTransformer implements ServiceStateCommandTransformer<StandbyState, OnOffType> {

    @Override
    public StandbyState transform(OnOffType onOffType) throws CouldNotTransformException {
        switch (onOffType) {
            case OFF:
                return StandbyState.newBuilder().setValue(State.RUNNING).build();
            case ON:
                return StandbyState.newBuilder().setValue(State.STANDBY).build();
            default:
                throw new CouldNotTransformException("Could not transform " + OnOffHolderType.OnOffHolder.OnOff.class.getSimpleName() + "[" + onOffType.name() + "] is unknown!");
        }
    }

    @Override
    public OnOffType transform(StandbyState standbyState) throws TypeNotSupportedException, CouldNotTransformException {
        switch (standbyState.getValue()) {
            case RUNNING:
                return OnOffType.OFF;
            case STANDBY:
                return OnOffType.ON;
            case UNKNOWN:
                throw new TypeNotSupportedException(standbyState, OnOffType.class);
            default:
                throw new CouldNotTransformException("Could not transform " + StandbyState.State.class.getSimpleName() + "[" + standbyState.getValue().name() + "] is unknown!");
        }
    }
}
