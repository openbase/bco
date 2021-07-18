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

import com.google.protobuf.Message;
import org.eclipse.smarthome.core.types.Command;
import org.openbase.jul.exception.CouldNotTransformException;
import org.openbase.jul.exception.TypeNotSupportedException;

/**
 * @param <S> the service state that can be transformed.
 * @param <C> the command type that can be transformed
 *
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public interface ServiceStateCommandTransformer<S extends Message, C extends Command> {

    /**
     * Transform a command of type C to a service state of type S.
     *
     * @param command the command which will be transformed.
     *
     * @return the transformation of the command into the service state.
     *
     * @throws CouldNotTransformException if the transformation fails.
     * @throws TypeNotSupportedException  if the value of the command is not supported.
     */
    S transform(final C command) throws CouldNotTransformException, TypeNotSupportedException;

    /**
     * Transform a service state into a command.
     *
     * @param serviceState the service state to be transformed.
     *
     * @return the transformation of the state into the command.
     *
     * @throws CouldNotTransformException if the transformation fails.
     * @throws TypeNotSupportedException  if the value of the command is not supported.
     */
    C transform(final S serviceState) throws CouldNotTransformException, TypeNotSupportedException;
}
