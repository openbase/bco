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

import org.eclipse.smarthome.core.library.types.OnOffType;
import org.openbase.jul.exception.CouldNotTransformException;
import org.openbase.jul.exception.TypeNotSupportedException;
import org.openbase.type.domotic.state.PresenceStateType.PresenceState;
import org.openbase.type.domotic.state.PresenceStateType.PresenceState.State;

/**
 * * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class PresenceStateOnOffTypeTransformer implements ServiceStateCommandTransformer<PresenceState, OnOffType> {

    @Override
    public PresenceState transform(final OnOffType onOffType) throws CouldNotTransformException {
        switch (onOffType) {
            case OFF:
                return PresenceState.newBuilder().setValue(State.ABSENT).build();
            case ON:
                return PresenceState.newBuilder().setValue(State.PRESENT).build();
            default:
                throw new CouldNotTransformException("Could not transform " + OnOffType.class.getSimpleName() + "[" + onOffType.name() + "] is unknown!");
        }
    }

    @Override
    public OnOffType transform(final PresenceState presenceState) throws TypeNotSupportedException, CouldNotTransformException {
        switch (presenceState.getValue()) {
            case ABSENT:
                return OnOffType.OFF;
            case PRESENT:
                return OnOffType.ON;
            case UNKNOWN:
                throw new TypeNotSupportedException(presenceState, OnOffType.class);
            default:
                throw new CouldNotTransformException("Could not transform " + PresenceState.class.getSimpleName() + "[" + presenceState.getValue().name() + "] is unknown!");
        }
    }
}
