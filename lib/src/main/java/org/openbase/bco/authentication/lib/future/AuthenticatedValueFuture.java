package org.openbase.bco.authentication.lib.future;

/*-
 * #%L
 * BCO Authentication Library
 * %%
 * Copyright (C) 2017 - 2019 openbase.org
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
import org.openbase.jul.exception.InvalidStateException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.type.domotic.authentication.AuthenticatedValueType.AuthenticatedValue;
import org.openbase.type.domotic.authentication.TicketAuthenticatorWrapperType.TicketAuthenticatorWrapper;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.Future;

/**
 * Implementation of the abstract authentication future for the AuthenticatedValue type.
 *
 * @param <RETURN> The type of value this future returns.
 * @author <a href="mailto:thuxohl@techfak.uni-bielefeld.de">Tamino Huxohl</a>
 */
public class AuthenticatedValueFuture<RETURN> extends AbstractAuthenticationFuture<RETURN, AuthenticatedValue> {

    /**
     * Create an AuthenticationFutureImpl.
     *
     * @param internalFuture The internal future whose result is verified.
     * @param returnClass    Class of type RETURN.
     * @param wrapper        The ticket that was used for the request.
     * @param sessionManager The session manager that is used for the verification.
     */
    public AuthenticatedValueFuture(final Future<AuthenticatedValue> internalFuture, final Class<RETURN> returnClass, final TicketAuthenticatorWrapper wrapper, final SessionManager sessionManager) {
        super(internalFuture, returnClass, wrapper, sessionManager);
    }

    @Override
    protected TicketAuthenticatorWrapper getTicketFromInternal(final AuthenticatedValue authenticatedValue) throws NotAvailableException {
        try {
            if(authenticatedValue == null) {
                throw new NotAvailableException("AuthenticatedValue");
            }
            return authenticatedValue.getTicketAuthenticatorWrapper();
        } catch (CouldNotPerformException ex) {
            throw new NotAvailableException("Ticket", ex);
        }
    }

    /**
     * Decrypt the value inside the authenticated value or parse from byte string if no one is logged in.
     * {@inheritDoc}
     *
     * @param authenticatedValue The result from the internal future.
     * @return The encrypted or parsed value inside the authenticated value.
     * @throws CouldNotPerformException If the decryption failed or the message cannot be parsed from a byte string.
     */
    @Override
    protected RETURN convertFromInternal(AuthenticatedValue authenticatedValue) throws CouldNotPerformException {
        try {
            if (!authenticatedValue.hasValue()) {
                return null;
            }

            if (getSessionManager().isLoggedIn()) {
                return EncryptionHelper.decryptSymmetric(authenticatedValue.getValue(), getSessionManager().getSessionKey(), getReturnClass());
            } else {
                try {
                    if (!Message.class.isAssignableFrom(getReturnClass())) {
                        throw new CouldNotPerformException("AuthenticatedValue has a value but the client method did not expect one");
                    }

                    Method parseFrom = getReturnClass().getMethod("parseFrom", ByteString.class);
                    return (RETURN) parseFrom.invoke(null, authenticatedValue.getValue());
                } catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
                    throw new CouldNotPerformException("Could not invoke parseFrom method on [" + getReturnClass().getSimpleName() + "]", ex);
                }
            }
        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not get return value from internal value", ex);
        }
    }
}
