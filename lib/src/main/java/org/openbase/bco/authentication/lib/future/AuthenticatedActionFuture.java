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
 *
 * @author <a href="mailto:thuxohl@techfak.uni-bielefeld.de">Tamino Huxohl</a>
 */
public class AuthenticatedActionFuture extends AuthenticatedFuture<ActionFuture, ActionFuture> {

    public AuthenticatedActionFuture(final Future<ActionFuture> applyActionFuture, final TicketAuthenticatorWrapper ticketAuthenticatorWrapper) {
        super(applyActionFuture, ActionFuture.class, ticketAuthenticatorWrapper);
    }
    
    public AuthenticatedActionFuture(final Future<ActionFuture> applyActionFuture, final TicketAuthenticatorWrapper ticketAuthenticatorWrapper, final SessionManager sessionManager) {
        super(applyActionFuture, ActionFuture.class, ticketAuthenticatorWrapper, sessionManager);
    }

    @Override
    protected TicketAuthenticatorWrapper getWrapperFromInternal(ActionFuture internalType) {
        return internalType.getTicketAuthenticatorWrapper();
    }

    @Override
    protected ActionFuture convertFromInternal(ActionFuture internalType) throws CouldNotPerformException {
        return internalType;
    }
}
