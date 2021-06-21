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
import org.openbase.type.domotic.state.TamperStateType.TamperState;
import org.openbase.type.domotic.state.TamperStateType.TamperState.State;

/**
 * * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class TamperStateOnOffTypeTransformer implements ServiceStateCommandTransformer<TamperState, OnOffType> {

    @Override
    public TamperState transform(final OnOffType onOffType) throws CouldNotTransformException {
        switch (onOffType) {
            case OFF:
                return TamperState.newBuilder().setValue(State.NO_TAMPER).build();
            case ON:
                return TamperState.newBuilder().setValue(State.TAMPER).build();
            default:
                throw new CouldNotTransformException("Could not transform " + OnOffType.class.getSimpleName() + "[" + onOffType.name() + "] is unknown!");
        }
    }

    @Override
    public OnOffType transform(final TamperState tamperState) throws TypeNotSupportedException, CouldNotTransformException {
        switch (tamperState.getValue()) {
            case NO_TAMPER:
                return OnOffType.OFF;
            case TAMPER:
                return OnOffType.ON;
            case UNKNOWN:
                throw new TypeNotSupportedException(tamperState, OnOffType.class);
            default:
                throw new CouldNotTransformException("Could not transform " + TamperState.class.getSimpleName() + "[" + tamperState.getValue().name() + "] is unknown!");
        }
    }
}
