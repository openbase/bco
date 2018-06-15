package org.openbase.bco.app.openhab.manager.transform;

/*
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

import org.eclipse.smarthome.core.library.types.UpDownType;
import org.openbase.jul.exception.CouldNotTransformException;
import org.openbase.jul.exception.TypeNotSupportedException;
import rst.domotic.binding.openhab.UpDownHolderType.UpDownHolder;
import rst.domotic.state.BlindStateType.BlindState;

/**
 * * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class UpDownStateTransformer {

    public static BlindState transform(final UpDownType upDownType) throws CouldNotTransformException {
        switch (upDownType) {
            case DOWN:
                return BlindState.newBuilder().setValue(BlindState.State.DOWN).build();
            case UP:
                return BlindState.newBuilder().setValue(BlindState.State.UP).build();
            default:
                throw new CouldNotTransformException("Could not transform " + UpDownHolder.UpDown.class.getSimpleName() + "[" + upDownType.name() + "] is unknown!");
        }
    }

    public static UpDownType transform(final BlindState blindState) throws TypeNotSupportedException, CouldNotTransformException {
        switch (blindState.getValue()) {
            case DOWN:
                return UpDownType.DOWN;
            case UP:
                return UpDownType.UP;
            case UNKNOWN:
                throw new TypeNotSupportedException(blindState, UpDownType.class);
            default:
                throw new CouldNotTransformException("Could not transform " + BlindState.class.getSimpleName() + "[" + blindState.getValue().name() + "] is unknown!");
        }
    }
}
