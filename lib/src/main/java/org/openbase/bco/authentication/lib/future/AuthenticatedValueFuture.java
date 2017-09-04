package org.openbase.bco.authentication.lib.future;

/*-
 * #%L
 * BCO Authentication Library
 * %%
 * Copyright (C) 2017 openbase.org
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
import com.google.protobuf.GeneratedMessage;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.Future;
import javax.crypto.BadPaddingException;
import org.openbase.bco.authentication.lib.EncryptionHelper;
import org.openbase.bco.authentication.lib.SessionManager;
import org.openbase.jul.exception.CouldNotPerformException;
import rst.domotic.authentication.AuthenticatedValueType.AuthenticatedValue;
import rst.domotic.authentication.TicketAuthenticatorWrapperType.TicketAuthenticatorWrapper;

/**
 * An AuthenticatedFuture which has an internal Future with an authenticated value and returns a generated message.
 *
 * @author <a href="mailto:thuxohl@techfak.uni-bielefeld.de">Tamino Huxohl</a>
 * @param <RETURN> The type of the generated message which is returned.
 */
public class AuthenticatedValueFuture<RETURN extends GeneratedMessage> extends AuthenticatedFuture<RETURN, AuthenticatedValue> {

    /**
     * Create a new AuthenticatedValueFuture with the default session manager.
     *
     * @param internalFuture The internal future.
     * @param returnValueClass Class of type RETURN.
     * @param ticketAuthenticatorWrapper The ticket used for the request.
     */
    public AuthenticatedValueFuture(final Future<AuthenticatedValue> internalFuture, final Class<RETURN> returnValueClass, final TicketAuthenticatorWrapper ticketAuthenticatorWrapper) {
        super(internalFuture, returnValueClass, ticketAuthenticatorWrapper);
    }

    /**
     * Create a new AuthenticatedValueFuture
     *
     * @param internalFuture The internal future.
     * @param returnValueClass Class of type RETURN.
     * @param ticketAuthenticatorWrapper The ticket used for the request.
     * @param sessionManager Session manager that is used to verify the response.
     */
    public AuthenticatedValueFuture(final Future<AuthenticatedValue> internalFuture, final Class<RETURN> returnValueClass, final TicketAuthenticatorWrapper ticketAuthenticatorWrapper, final SessionManager sessionManager) {
        super(internalFuture, returnValueClass, ticketAuthenticatorWrapper, sessionManager);
    }

    /**
     * {@inheritDoc}
     *
     * @param internalType {@inheritDoc}
     * @return The ticket inside the authenticated value.
     */
    @Override
    protected TicketAuthenticatorWrapper getTicketFromInternal(AuthenticatedValue internalType) {
        return internalType.getTicketAuthenticatorWrapper();
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
            if (getSessionManager().isLoggedIn()) {
                try {
                    return EncryptionHelper.decryptSymmetric(authenticatedValue.getValue(), getSessionManager().getSessionKey(), getReturnClass());
                } catch (BadPaddingException | IOException ex) {
                    throw new CouldNotPerformException("Decrypting result of internal future failed!", ex);
                }
            } else {
                try {
                    Method parseFrom = getReturnClass().getMethod("parseFrom", ByteString.class);
                    return (RETURN) parseFrom.invoke(null, authenticatedValue.getValue());
                } catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
                    throw new CouldNotPerformException("Could not invoke parseFrom method on [" + getReturnClass().getSimpleName() + "]", ex);
                }
            }
        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("COuld not get return value from internal value", ex);
        }
    }
}
