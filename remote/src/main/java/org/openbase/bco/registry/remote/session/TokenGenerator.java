package org.openbase.bco.registry.remote.session;

/*-
 * #%L
 * BCO Registry Remote
 * %%
 * Copyright (C) 2014 - 2021 openbase.org
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

import org.openbase.bco.authentication.lib.SessionManager;
import org.openbase.bco.authentication.lib.future.AuthenticatedValueFuture;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.type.domotic.authentication.AuthTokenType.AuthToken;
import org.openbase.type.domotic.authentication.AuthenticatedValueType.AuthenticatedValue;
import org.openbase.type.domotic.authentication.AuthenticationTokenType.AuthenticationToken;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class TokenGenerator {

    /**
     * Method generates a new AuthToken including an authentication token for the user who is currently logged in.
     *
     * @return the token.
     *
     * @throws CouldNotPerformException is thrown if the token could not be generated.
     * @throws InterruptedException     is thrown if the thread was externally interrupted which could indicated an system shutdown.
     */
    public static AuthToken generateAuthToken() throws CouldNotPerformException, InterruptedException {
        return generateAuthToken(SessionManager.getInstance());
    }

    /**
     * Method generates a new AuthToken including an authentication token for the user who is currently logged in.
     *
     * @param timeout  timeout used to fail in case the task takes to long.
     * @param timeUnit the time unit of the timeout.
     *
     * @return the token.
     *
     * @throws CouldNotPerformException is thrown if the token could not be generated.
     * @throws InterruptedException     is thrown if the thread was externally interrupted which could indicated an system shutdown.
     * @throws TimeoutException         is thrown in case a timeout occurs.
     */
    public static AuthToken generateAuthToken(final long timeout, final TimeUnit timeUnit) throws CouldNotPerformException, InterruptedException, TimeoutException {
        return generateAuthToken(SessionManager.getInstance(), timeout, timeUnit);
    }

    /**
     * Method generates a new AuthToken including an authentication token for the user who is currently logged in at the given {@code sessionManager}.
     *
     * @param sessionManager the session manager used to identify the user.
     *
     * @return the token.
     *
     * @throws CouldNotPerformException is thrown if the token could not be generated.
     * @throws InterruptedException     is thrown if the thread was externally interrupted which could indicated an system shutdown.
     */
    public static AuthToken generateAuthToken(final SessionManager sessionManager) throws CouldNotPerformException, InterruptedException {
        try {
            // request authentication token for new user
            AuthenticatedValue authenticatedValue = sessionManager.initializeRequest(AuthenticationToken.newBuilder().setUserId(sessionManager.getUserClientPair().getUserId()).build(), null);
            final String authenticationToken = new AuthenticatedValueFuture<>(
                    Registries.getUnitRegistry().requestAuthenticationTokenAuthenticated(authenticatedValue),
                    String.class,
                    authenticatedValue.getTicketAuthenticatorWrapper(),
                    sessionManager).get();
            return AuthToken.newBuilder().setAuthenticationToken(authenticationToken).build();
        } catch (CouldNotPerformException | ExecutionException ex) {
            throw new CouldNotPerformException("Could not generate token!", ex);
        }
    }

    /**
     * Method generates a new AuthToken including an authentication token for the user who is currently logged in at the given {@code sessionManager}.
     *
     * @param sessionManager the session manager used to identify the user.
     * @param timeout        timeout used to fail in case the task takes to long.
     * @param timeUnit       the time unit of the timeout.
     *
     * @return the token.
     *
     * @throws CouldNotPerformException is thrown if the token could not be generated.
     * @throws InterruptedException     is thrown if the thread was externally interrupted which could indicated an system shutdown.
     * @throws TimeoutException         is thrown in case a timeout occurs.
     */
    public static AuthToken generateAuthToken(final SessionManager sessionManager, final long timeout, final TimeUnit timeUnit) throws CouldNotPerformException, InterruptedException, TimeoutException {
        try {
            // request authentication token for new user
            AuthenticatedValue authenticatedValue = sessionManager.initializeRequest(AuthenticationToken.newBuilder().setUserId(sessionManager.getUserClientPair().getUserId()).build(), null);
            final String authenticationToken = new AuthenticatedValueFuture<>(
                    Registries.getUnitRegistry().requestAuthenticationTokenAuthenticated(authenticatedValue),
                    String.class,
                    authenticatedValue.getTicketAuthenticatorWrapper(),
                    sessionManager).get(timeout, timeUnit);
            return AuthToken.newBuilder().setAuthenticationToken(authenticationToken).build();
        } catch (CouldNotPerformException | ExecutionException ex) {
            throw new CouldNotPerformException("Could not generate token!", ex);
        }
    }
}
