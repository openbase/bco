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

import org.eclipse.smarthome.core.library.types.OpenClosedType;
import org.openbase.jul.exception.CouldNotTransformException;
import org.openbase.jul.exception.TypeNotSupportedException;
import rst.domotic.state.ContactStateType.ContactState;

/**
 * * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class OpenClosedStateTransformer {

    public static ContactState transform(OpenClosedType openClosedType) throws CouldNotTransformException {
        switch (openClosedType) {
            case CLOSED:
                return ContactState.newBuilder().setValue(ContactState.State.CLOSED).build();
            case OPEN:
                return ContactState.newBuilder().setValue(ContactState.State.OPEN).build();
            default:
                throw new CouldNotTransformException("Could not transform " + OpenClosedType.class.getSimpleName() + "[" + openClosedType.name() + "] is unknown!");
        }
    }

    public static OpenClosedType transform(ContactState contactState) throws CouldNotTransformException, TypeNotSupportedException {
        switch (contactState.getValue()) {
            case CLOSED:
                return OpenClosedType.CLOSED;
            case OPEN:
                return OpenClosedType.OPEN;
            case UNKNOWN:
                throw new TypeNotSupportedException(contactState, OpenClosedType.class);
            default:
                throw new CouldNotTransformException("Could not transform " + ContactState.class.getSimpleName() + "[" + contactState.getValue().name() + "] is unknown!");
        }
    }
}
