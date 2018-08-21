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

import org.openbase.bco.authentication.lib.SessionManager;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.ExceptionProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.BadPaddingException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class ReLoginFuture<V> implements Future<V> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReLoginFuture.class);

    private final Future<V> internalFuture;
    private final SessionManager sessionManager;

    public ReLoginFuture(final Future<V> internalFuture, final SessionManager sessionManager) {
        this.internalFuture = internalFuture;
        this.sessionManager = sessionManager;
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
    public V get() throws InterruptedException, ExecutionException {
        try {
            return internalFuture.get();
        } catch (ExecutionException ex) {
            throw handleLoginError(ex);
        }
    }

    @Override
    public V get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        try {
            return internalFuture.get(timeout, unit);
        } catch (ExecutionException ex) {
            throw handleLoginError(ex);
        }
    }

    private ExecutionException handleLoginError(ExecutionException ex) {
        try {
            if (ExceptionProcessor.getInitialCause(ex) instanceof BadPaddingException) {
                // authenticator could not decrypt ticket so re-login or logout
                sessionManager.reLogin();
            }
        } catch (CouldNotPerformException exx) {
            return new ExecutionException("Could not re login", exx);
        }
        // was no login error so just return the exception
        return ex;
    }
}
