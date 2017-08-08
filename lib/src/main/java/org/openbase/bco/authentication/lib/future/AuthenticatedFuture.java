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
import org.openbase.bco.authentication.lib.EncryptionHelper;
import org.openbase.bco.authentication.lib.SessionManager;
import org.openbase.jul.exception.CouldNotPerformException;
import rst.domotic.authentication.AuthenticatorType.Authenticator;
import rst.domotic.authentication.TicketAuthenticatorWrapperType.TicketAuthenticatorWrapper;

/**
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

    public AuthenticatedFuture(final Future<INTERNAL> internalFuture, final Class<RETURN> returnClass, final TicketAuthenticatorWrapper wrapper) {
        this(internalFuture, returnClass, wrapper, SessionManager.getInstance());
    }

    public AuthenticatedFuture(final Future<INTERNAL> internalFuture, final Class<RETURN> returnClass, final TicketAuthenticatorWrapper wrapper, final SessionManager sessionManager) {
        this.internalFuture = internalFuture;
        this.returnClass = returnClass;
        this.sessionManager = sessionManager;
        this.wrapper = wrapper;
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        return internalFuture.cancel(mayInterruptIfRunning);
    }

    @Override
    public boolean isCancelled() {
        return internalFuture.isCancelled();
    }

    @Override
    public boolean isDone() {
        return internalFuture.isDone();
    }

    @Override
    public RETURN get() throws InterruptedException, ExecutionException {
        INTERNAL internalResult = internalFuture.get();
        try {
            verifyResponse(getWrapperFromInternal(internalResult));
            return convertFromInternal(internalResult);
        } catch (CouldNotPerformException ex) {
            throw new ExecutionException("Could not execute authentication", ex);
        }
    }

    @Override
    public RETURN get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        INTERNAL internalResult = internalFuture.get(timeout, unit);
        try {
            verifyResponse(getWrapperFromInternal(internalResult));
            return convertFromInternal(internalResult);
        } catch (CouldNotPerformException ex) {
            throw new ExecutionException("Could not execute authentication", ex);
        }
    }

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

    protected Class<RETURN> getReturnClass() {
        return returnClass;
    }

    protected SessionManager getSessionManager() {
        return sessionManager;
    }

    protected abstract TicketAuthenticatorWrapper getWrapperFromInternal(INTERNAL internalType);

    protected abstract RETURN convertFromInternal(INTERNAL internalType) throws CouldNotPerformException;
}
