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
import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import javax.crypto.BadPaddingException;
import org.openbase.bco.authentication.lib.AuthenticationClientHandler;
import org.openbase.bco.authentication.lib.SessionManager;
import org.openbase.jul.exception.CouldNotPerformException;
import rst.domotic.authentication.TicketAuthenticatorWrapperType.TicketAuthenticatorWrapper;

/**
 * Abstract future that automatically verifies the response from a server.
 *
 * @author <a href="mailto:thuxohl@techfak.uni-bielefeld.de">Tamino Huxohl</a>
 * @param <RETURN>
 * @param <INTERNAL>
 */
public abstract class AuthenticatedFuture<RETURN, INTERNAL> implements Future<RETURN> {

    private final Future<INTERNAL> internalFuture;
    private final SessionManager sessionManager;
    private final Class<RETURN> returnClass;
    private final TicketAuthenticatorWrapper wrapper;

    /**
     * Create an AuthenticatedFuture that uses the SessionManager singleton for the verification.
     *
     * @param internalFuture The internal future whose result is verified.
     * @param returnClass Class of type RETURN.
     * @param wrapper The ticket that was used for the request.
     */
    public AuthenticatedFuture(final Future<INTERNAL> internalFuture, final Class<RETURN> returnClass, final TicketAuthenticatorWrapper wrapper) {
        this(internalFuture, returnClass, wrapper, SessionManager.getInstance());
    }

    /**
     * Create an AuthenticatedFuture.
     *
     * @param internalFuture The internal future whose result is verified.
     * @param returnClass Class of type RETURN.
     * @param wrapper The ticket that was used for the request.
     * @param sessionManager The session manager that is used for the verification.
     */
    public AuthenticatedFuture(final Future<INTERNAL> internalFuture, final Class<RETURN> returnClass, final TicketAuthenticatorWrapper wrapper, final SessionManager sessionManager) {
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
     * @throws ExecutionException If the execution of the internal future failed, the response could not be verified or the conversion to the return type failed.
     */
    @Override
    public RETURN get() throws InterruptedException, ExecutionException {
        INTERNAL internalResult = internalFuture.get();
        try {
            verifyResponse(getTicketFromInternal(internalResult));
            return convertFromInternal(internalResult);
        } catch (CouldNotPerformException ex) {
            throw new ExecutionException("Could not execute authentication", ex);
        }
    }

    /**
     * Call get on the internal future with a given timeout, verifies the ticket and converts the result from
     * the internal future to REPONSE.
     *
     * @return RESPONSE converted from the result of the internal future.
     * @throws InterruptedException If interrupted inside of get of the internal future.
     * @throws ExecutionException If the execution of the internal future failed, the response could not be verified or the conversion to the return type failed.
     * @throws TimeoutException If get on the internal future times out.
     */
    @Override
    public RETURN get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        INTERNAL internalResult = internalFuture.get(timeout, unit);
        try {
            verifyResponse(getTicketFromInternal(internalResult));
            return convertFromInternal(internalResult);
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

            sessionManager.setTicketAuthenticatorWrapper(AuthenticationClientHandler.handleServiceServerResponse(
                    this.sessionManager.getSessionKey(),
                    this.wrapper,
                    ticketAuthenticatorWrapper));
        } catch (IOException | BadPaddingException | CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not verify ServiceServer Response", ex);
        }
    }

    /**
     * Get the return class.
     *
     * @return The return class given in the constructor.
     */
    protected Class<RETURN> getReturnClass() {
        return returnClass;
    }

    /**
     * Get the session manager used by this authenticated future.
     *
     * @return The session manager given in the constructor.
     */
    protected SessionManager getSessionManager() {
        return sessionManager;
    }

    /**
     * Method defining how to get the ticket from the result of the internal future.
     *
     * @param internalType The result from the internal future.
     * @return Ticket extracted from the internal type.
     */
    protected abstract TicketAuthenticatorWrapper getTicketFromInternal(INTERNAL internalType);

    /**
     * Method defining how to get the return value from the result of the internal future.
     *
     * @param internalType The result from the internal future.
     * @return Return value converted from the internal type.
     * @throws CouldNotPerformException If the conversion from the internal type fails.
     */
    protected abstract RETURN convertFromInternal(INTERNAL internalType) throws CouldNotPerformException;
}
