package org.openbase.bco.authentication.lib;

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
import org.openbase.jul.exception.RejectedException;
import rst.domotic.action.ActionFutureType.ActionFuture;
import rst.domotic.authentication.TicketAuthenticatorWrapperType.TicketAuthenticatorWrapper;

/**
 *
 * @author <a href="mailto:thuxohl@techfak.uni-bielefeld.de">Tamino Huxohl</a>
 */
public class AuthenticationFuture implements Future<ActionFuture> {

    private final Future<ActionFuture> applyActionFuture;
    private final SessionManager sessionManager;

    public AuthenticationFuture(final Future<ActionFuture> applyActionFuture, final SessionManager sessionManager) {
        this.applyActionFuture = applyActionFuture;
        this.sessionManager = sessionManager;
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        return this.applyActionFuture.cancel(mayInterruptIfRunning);
    }

    @Override
    public boolean isCancelled() {
        return this.applyActionFuture.isCancelled();
    }

    @Override
    public boolean isDone() {
        return this.applyActionFuture.isDone();
    }

    @Override
    public ActionFuture get() throws InterruptedException, ExecutionException {
        ActionFuture actionFuture = this.applyActionFuture.get();
        return verifyResponse(actionFuture);
    }

    @Override
    public ActionFuture get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        ActionFuture actionFuture = this.applyActionFuture.get(timeout, unit);
        return verifyResponse(actionFuture);
    }

    private ActionFuture verifyResponse(ActionFuture actionFuture) throws ExecutionException {
        try {
            TicketAuthenticatorWrapper wrapper = AuthenticationClientHandler.handleServiceServerResponse(
                    this.sessionManager.getSessionKey(),
                    this.sessionManager.getTicketAuthenticatorWrapper(),
                    actionFuture.getTicketAuthenticatorWrapper());

            return actionFuture.toBuilder().setTicketAuthenticatorWrapper(wrapper).build();
        } catch (IOException | RejectedException ex) {
            throw new ExecutionException("Could not verify ServiceServer Response", ex);
        }
    }
}
