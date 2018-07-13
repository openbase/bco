package org.openbase.bco.authentication.lib.future;

/*-
 * #%L
 * BCO Authentication Library
 * %%
 * Copyright (C) 2017 - 2018 openbase.org
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
import org.openbase.bco.authentication.lib.AuthenticationClientHandler;
import org.openbase.bco.authentication.lib.EncryptionHelper;
import org.openbase.bco.authentication.lib.SessionManager;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.NotAvailableException;
import rst.domotic.authentication.AuthenticatedValueType.AuthenticatedValue;
import rst.domotic.authentication.TicketAuthenticatorWrapperType.TicketAuthenticatorWrapper;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class AuthenticationFuture<RETURN> implements Future<RETURN> {

    private final Future<AuthenticatedValue> internalFuture;
    private final SessionManager sessionManager;
    private final Class<RETURN> returnClass;
    private final TicketAuthenticatorWrapper wrapper;

    private AuthenticatedValue authenticatedValue = null;

    /**
     * Create an AuthenticatedFuture that uses the SessionManager singleton for the verification.
     *
     * @param internalFuture The internal future whose result is verified.
     * @param returnClass    Class of type RETURN.
     * @param wrapper        The ticket that was used for the request.
     */
    public AuthenticationFuture(final Future<AuthenticatedValue> internalFuture, final Class<RETURN> returnClass, final TicketAuthenticatorWrapper wrapper) {
        this(internalFuture, returnClass, wrapper, SessionManager.getInstance());
    }

    /**
     * Create an AuthenticatedFuture.
     *
     * @param internalFuture The internal future whose result is verified.
     * @param returnClass    Class of type RETURN.
     * @param wrapper        The ticket that was used for the request.
     * @param sessionManager The session manager that is used for the verification.
     */
    public AuthenticationFuture(final Future<AuthenticatedValue> internalFuture, final Class<RETURN> returnClass, final TicketAuthenticatorWrapper wrapper, final SessionManager sessionManager) {
        this.internalFuture = internalFuture;
        this.returnClass = returnClass;
        this.sessionManager = sessionManager;
        this.wrapper = wrapper;
    }

    /**
     * Cancel the internal future.
     * {@inheritDoc}
     *
     * @param mayInterruptIfRunning {@inheritDoc}
     * @return {@inheritDoc}
     */
    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        return internalFuture.cancel(mayInterruptIfRunning);
    }

    /**
     * Return if the internal future has been canceled.
     * {@inheritDoc}
     *
     * @return {@inheritDoc}
     */
    @Override
    public boolean isCancelled() {
        return internalFuture.isCancelled();
    }

    /**
     * Return it the internal future is done.
     * {@inheritDoc}
     *
     * @return {@inheritDoc}
     */
    @Override
    public boolean isDone() {
        return internalFuture.isDone();
    }

    /**
     * Call get on the internal future, verifies the ticket and converts the result from
     * the internal future to REPONSE.
     *
     * @return RESPONSE converted from the result of the internal future.
     * @throws InterruptedException If interrupted inside of get of the internal future.
     * @throws ExecutionException   If the execution of the internal future failed, the response could not be verified or the conversion to the return type failed.
     */
    @Override
    public RETURN get() throws InterruptedException, ExecutionException {
        AuthenticatedValue authenticatedValue = internalFuture.get();
        this.authenticatedValue = authenticatedValue;
        try {
            verifyResponse(authenticatedValue.getTicketAuthenticatorWrapper());
            return convertFromInternal(authenticatedValue);
        } catch (CouldNotPerformException ex) {
            throw new ExecutionException("Could not execute authentication", ex);
        }
    }

    /**
     * Call get on the internal future with a given timeout, verifies the ticket and converts the result from
     * the internal future to REPONSE.
     *
     * @return RESPONSE converted from the result of the internal future.
     * @throws InterruptedException                  If interrupted inside of get of the internal future.
     * @throws ExecutionException                    If the execution of the internal future failed, the response could not be verified or the conversion to the return type failed.
     * @throws java.util.concurrent.TimeoutException If get on the internal future times out.
     */
    @Override
    public RETURN get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        AuthenticatedValue authenticatedValue = internalFuture.get(timeout, unit);
        this.authenticatedValue = authenticatedValue;
        try {
            verifyResponse(authenticatedValue.getTicketAuthenticatorWrapper());
            return convertFromInternal(authenticatedValue);
        } catch (CouldNotPerformException ex) {
            throw new ExecutionException("Could not execute authentication", ex);
        }
    }

    /**
     * If the a user is logged in with the given SessionManager verify the response.
     *
     * @param ticketAuthenticatorWrapper The ticket coming with the response from the server.
     * @throws CouldNotPerformException If the verification cannot be performed.
     */
    private void verifyResponse(TicketAuthenticatorWrapper ticketAuthenticatorWrapper) throws CouldNotPerformException {
        try {
            // only verify if logged in
            if (!this.sessionManager.isLoggedIn()) {
                return;
            }

            sessionManager.updateTicketAuthenticatorWrapper(AuthenticationClientHandler.handleServiceServerResponse(
                    this.sessionManager.getSessionKey(),
                    this.wrapper,
                    ticketAuthenticatorWrapper));
        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not verify ServiceServer Response", ex);
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
    private RETURN convertFromInternal(AuthenticatedValue authenticatedValue) throws CouldNotPerformException {
        try {
            if (sessionManager.isLoggedIn()) {
                return EncryptionHelper.decryptSymmetric(authenticatedValue.getValue(), sessionManager.getSessionKey(), returnClass);
            } else {
                try {
                    if (authenticatedValue.hasValue() && !authenticatedValue.getValue().isEmpty()) {
                        if (!GeneratedMessage.class.isAssignableFrom(returnClass)) {
                            throw new CouldNotPerformException("AuthenticatedValue has a value but the client method did not expect one");
                        }

                        Method parseFrom = returnClass.getMethod("parseFrom", ByteString.class);
                        return (RETURN) parseFrom.invoke(null, authenticatedValue.getValue());
                    }
                    return null;
                } catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
                    throw new CouldNotPerformException("Could not invoke parseFrom method on [" + returnClass.getSimpleName() + "]", ex);
                }
            }
        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("COuld not get return value from internal value", ex);
        }
    }

    public AuthenticatedValue getAuthenticatedValue() throws NotAvailableException {
        if (authenticatedValue == null) {
            throw new NotAvailableException("authenticated value");
        }

        return authenticatedValue;
    }
}
