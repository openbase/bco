package org.openbase.bco.device.openhab.manager.transform;

/*
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

import org.eclipse.smarthome.core.library.types.StopMoveType;
import org.openbase.jul.exception.CouldNotTransformException;
import org.openbase.jul.exception.TypeNotSupportedException;
import org.openbase.type.domotic.binding.openhab.StopMoveHolderType.StopMoveHolder;
import org.openbase.type.domotic.state.BlindStateType.BlindState;

/**
 * * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class BlindStateStopMoveTypeTransformer implements ServiceStateCommandTransformer<BlindState, StopMoveType> {

    @Override
    public BlindState transform(final StopMoveType stopMoveType) throws CouldNotTransformException {
        switch (stopMoveType) {
            case STOP:
                return BlindState.newBuilder().setValue(BlindState.State.STOP).build();
            case MOVE:
                return BlindState.newBuilder().setValue(BlindState.State.UNKNOWN).build();
            default:
                throw new CouldNotTransformException("Could not transform " + StopMoveHolder.StopMove.class.getSimpleName() + "[" + stopMoveType.name() + "] is unknown!");
        }
    }

    @Override
    public StopMoveType transform(BlindState blindState) throws TypeNotSupportedException, CouldNotTransformException {
        switch (blindState.getValue()) {
            case STOP:
                return StopMoveType.STOP;
            case UP:
            case DOWN:
                return StopMoveType.MOVE;
            case UNKNOWN:
                throw new TypeNotSupportedException(blindState, StopMoveType.class);
            default:
                throw new CouldNotTransformException("Could not transform " + BlindState.class.getSimpleName() + "[" + blindState.getValue().name() + "] is unknown!");
        }
    }
}
