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
import java.util.concurrent.Future;
import org.openbase.bco.authentication.lib.SessionManager;
import org.openbase.jul.exception.CouldNotPerformException;
import rst.domotic.action.ActionFutureType.ActionFuture;
import rst.domotic.authentication.TicketAuthenticatorWrapperType.TicketAuthenticatorWrapper;

/**
 * AuthenticatedFuture which returns an ActionFuture and its internal Future also is of type ActionFuture.
 *
 * @author <a href="mailto:thuxohl@techfak.uni-bielefeld.de">Tamino Huxohl</a>
 */
public class AuthenticatedActionFuture extends AuthenticatedFuture<ActionFuture, ActionFuture> {

    /**
     * Create a new AuthenticatedActionFuture which uses the default session manager.
     *
     * @param applyActionFuture The internal future.
     * @param ticketAuthenticatorWrapper The ticket which was used for the request.
     */
    public AuthenticatedActionFuture(final Future<ActionFuture> applyActionFuture, final TicketAuthenticatorWrapper ticketAuthenticatorWrapper) {
        super(applyActionFuture, ActionFuture.class, ticketAuthenticatorWrapper);
    }

    /**
     * Create a new AuthenticatedActionFuture.
     *
     * @param applyActionFuture The internal future.
     * @param ticketAuthenticatorWrapper The ticket which was used for the request.
     * @param sessionManager The session manager used to verify the response.
     */
    public AuthenticatedActionFuture(final Future<ActionFuture> applyActionFuture, final TicketAuthenticatorWrapper ticketAuthenticatorWrapper, final SessionManager sessionManager) {
        super(applyActionFuture, ActionFuture.class, ticketAuthenticatorWrapper, sessionManager);
    }

    /**
     * {@inheritDoc}
     *
     * @param internalType {@inheritDoc}
     * @return The ticket inside the action future.
     */
    @Override
    protected TicketAuthenticatorWrapper getTicketFromInternal(ActionFuture internalType) {
        return internalType.getTicketAuthenticatorWrapper();
    }

    /**
     * {@inheritDoc}
     * 
     * @param internalType {@inheritDoc}
     * @return The internal type.
     * @throws CouldNotPerformException {@inheritDoc} 
     */
    @Override
    protected ActionFuture convertFromInternal(ActionFuture internalType) throws CouldNotPerformException {
        return internalType;
    }
}
