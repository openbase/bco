/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dc.bco.manager.device.binding.openhab.transform;

/*
 * #%L
 * COMA DeviceManager Binding OpenHAB
 * %%
 * Copyright (C) 2015 - 2016 DivineCooperation
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

import org.dc.jul.exception.CouldNotTransformException;
import org.dc.jul.exception.TypeNotSupportedException;
import rst.homeautomation.openhab.StopMoveHolderType;
import static rst.homeautomation.openhab.StopMoveHolderType.StopMoveHolder.StopMove.STOP;
import rst.homeautomation.state.ShutterStateType.ShutterState;

/**
 *
 * @author thuxohl
 */
public class StopMoveStateTransformer {

    public static ShutterState.State transform(final StopMoveHolderType.StopMoveHolder.StopMove stopMoveType) throws CouldNotTransformException {
        switch (stopMoveType) {
            case STOP:
                return ShutterState.State.STOP;
            case MOVE:
                return ShutterState.State.UNKNOWN;
            default:
                throw new CouldNotTransformException("Could not transform " + StopMoveHolderType.StopMoveHolder.StopMove.class.getName() + "! " + StopMoveHolderType.StopMoveHolder.StopMove.class.getSimpleName() + "[" + stopMoveType.name() + "] is unknown!");
        }
    }

    public static StopMoveHolderType.StopMoveHolder transform(ShutterState.State shutterState) throws TypeNotSupportedException, CouldNotTransformException {
        switch (shutterState) {
            case STOP:
                return StopMoveHolderType.StopMoveHolder.newBuilder().setState(StopMoveHolderType.StopMoveHolder.StopMove.STOP).build();
            case UP:
                return StopMoveHolderType.StopMoveHolder.newBuilder().setState(StopMoveHolderType.StopMoveHolder.StopMove.MOVE).build();
            case DOWN:
                return StopMoveHolderType.StopMoveHolder.newBuilder().setState(StopMoveHolderType.StopMoveHolder.StopMove.MOVE).build();
            case UNKNOWN:
                throw new TypeNotSupportedException(shutterState, StopMoveHolderType.StopMoveHolder.StopMove.class);
            default:
                throw new CouldNotTransformException("Could not transform " + ShutterState.State.class.getName() + "! " + ShutterState.State.class.getSimpleName() + "[" + shutterState.name() + "] is unknown!");
        }
    }
}
