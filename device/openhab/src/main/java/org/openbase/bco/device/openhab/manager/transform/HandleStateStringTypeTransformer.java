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

import org.eclipse.smarthome.core.library.types.StringType;
import org.openbase.jul.exception.CouldNotTransformException;
import org.openbase.jul.processing.StringProcessor;
import org.openbase.type.domotic.state.HandleStateType.HandleState;

/**
 * TODO: rethink handle state: if position makes sense
 *
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public class HandleStateStringTypeTransformer implements ServiceStateCommandTransformer<HandleState, StringType> {

    @Override
    public HandleState transform(final StringType value) throws CouldNotTransformException {
        switch (StringProcessor.transformToUpperCase(value.toString())) {
            case "CLOSED":
                return HandleState.newBuilder().setPosition(0).build();
            case "OPEN":
                return HandleState.newBuilder().setPosition(90).build();
            case "TILTED":
                return HandleState.newBuilder().setPosition(180).build();
            default:
                throw new CouldNotTransformException("Could not transform " + StringType.class.getSimpleName() + "[" + value + "] is not a valid " + HandleState.class.getSimpleName() + "!");
        }
    }

    @Override
    public StringType transform(final HandleState value) throws CouldNotTransformException {
        switch (value.getPosition()) {
            case 0:
                return new StringType("CLOSED");
            case 90:
                return new StringType("OPEN");
            case 180:
                return new StringType("TILTED");
            default:
                throw new CouldNotTransformException("Could not transform " + HandleState.class.getName() + "[" + value + "] to " + StringType.class.getSimpleName() + "!");
        }
    }
}
