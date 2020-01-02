package org.openbase.bco.authentication.lib.com;

/*-
 * #%L
 * BCO Authentication Library
 * %%
 * Copyright (C) 2017 - 2020 openbase.org
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

import com.google.protobuf.Message;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InitializationException;
import org.openbase.jul.extension.protobuf.processing.GenericMessageProcessor;
import org.openbase.type.domotic.authentication.AuthenticatedValueType.AuthenticatedValue;

public class AuthenticatedGenericMessageProcessor<M extends Message> extends GenericMessageProcessor<M> {

    public AuthenticatedGenericMessageProcessor(Class<M> dataClass) throws InitializationException {
        super(dataClass);
    }

    @Override
    public M process(Message input) throws CouldNotPerformException, InterruptedException {
        if (input instanceof AuthenticatedValue) {
            AuthenticatedValue authenticatedValue = (AuthenticatedValue) input;
            return super.process(AuthenticatedMessageProcessor.getDataFromAuthenticatedValue(authenticatedValue, getDataClass()));
        } else {
            return super.process(input);
        }
    }
}
