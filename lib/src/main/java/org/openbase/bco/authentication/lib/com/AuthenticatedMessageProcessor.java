package org.openbase.bco.authentication.lib.com;

/*-
 * #%L
 * BCO Authentication Library
 * %%
 * Copyright (C) 2017 - 2021 openbase.org
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

import com.google.protobuf.ByteString;
import com.google.protobuf.Message;
import org.openbase.bco.authentication.lib.EncryptionHelper;
import org.openbase.bco.authentication.lib.SessionManager;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.extension.protobuf.processing.SimpleMessageProcessor;
import org.openbase.type.domotic.authentication.AuthenticatedValueType.AuthenticatedValue;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class AuthenticatedMessageProcessor<M extends Message> extends SimpleMessageProcessor<M> {

    public AuthenticatedMessageProcessor(Class<M> dataClass) {
        super(dataClass);
    }

    @Override
    public M process(Message input) throws CouldNotPerformException, InterruptedException {
        if (input instanceof AuthenticatedValue) {
            AuthenticatedValue authenticatedValue = (AuthenticatedValue) input;
            return super.process(getDataFromAuthenticatedValue(authenticatedValue, getDataClass()));
        } else {
            return super.process(input);
        }
    }

    public static <M extends Message> M getDataFromAuthenticatedValue(final AuthenticatedValue authenticatedValue, final Class<M> dataClass) throws CouldNotPerformException {
        return getDataFromAuthenticatedValue(authenticatedValue, SessionManager.getInstance(), dataClass);
    }

    public static <M extends Message> M getDataFromAuthenticatedValue(final AuthenticatedValue authenticatedValue, final SessionManager sessionManager, final Class<M> dataClass) throws CouldNotPerformException {
        if (authenticatedValue.hasTicketAuthenticatorWrapper()) {
            final byte[] sessionKey = sessionManager.getSessionKey();
            if (sessionKey == null) {
                // user has logged out while the request was running
                throw new CouldNotPerformException("Could not decrypt authenticated message");
            }

            return EncryptionHelper.decryptSymmetric(authenticatedValue.getValue(), sessionKey, dataClass);
        } else {
            try {
                Method parseFrom = dataClass.getMethod("parseFrom", ByteString.class);
                return (M) parseFrom.invoke(null, authenticatedValue.getValue());
            } catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
                throw new CouldNotPerformException("Could not invoke parseFrom method on [" + dataClass.getSimpleName() + "]", ex);
            }
        }
    }
}
