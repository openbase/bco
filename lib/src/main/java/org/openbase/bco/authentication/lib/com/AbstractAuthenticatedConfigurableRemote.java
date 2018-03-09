package org.openbase.bco.authentication.lib.com;

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

import com.google.protobuf.GeneratedMessage;
import org.openbase.bco.authentication.lib.SessionManager;
import org.openbase.bco.authentication.lib.iface.AuthenticatedRequestable;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.extension.rsb.com.AbstractConfigurableRemote;
import org.openbase.jul.pattern.Observer;
import rsb.Event;
import rsb.Handler;
import rst.domotic.authentication.TicketAuthenticatorWrapperType.TicketAuthenticatorWrapper;

import java.util.concurrent.Future;

public class AbstractAuthenticatedConfigurableRemote<M extends GeneratedMessage, CONFIG extends GeneratedMessage> extends AbstractConfigurableRemote<M, CONFIG> {

    private final Observer<String> loginObserver;

    public AbstractAuthenticatedConfigurableRemote(final Class<M> dataClass, final Class<CONFIG> configClass) {
        super(dataClass, configClass);
        this.setMessageProcessor(new AuthenticatedMessageProcessor<>(dataClass));

        this.loginObserver = (source, data) -> requestData();
        SessionManager.getInstance().addLoginObserver(this.loginObserver);
    }

    @Override
    protected Handler generateHandler() {
        return new AuthenticatedUpdateHandler();
    }

    @Override
    protected Future<Event> internalRequestStatus() throws CouldNotPerformException {
        if (SessionManager.getInstance().isLoggedIn()) {
            Event event = new Event(TicketAuthenticatorWrapper.class, SessionManager.getInstance().initializeServiceServerRequest());
            return getRemoteServer().callAsync(AuthenticatedRequestable.REQUEST_DATA_AUTHENTICATED_METHOD, event);
        } else {
            return super.internalRequestStatus();
        }
    }

    private class AuthenticatedUpdateHandler implements Handler {

        @Override
        public void internalNotify(Event event) {
            try {
                logger.debug("Internal notification while logged in[" + SessionManager.getInstance().isLoggedIn() + "]");
                if (event.getData() != null && SessionManager.getInstance().isLoggedIn()) {
                    requestData();
                } else {
                    applyEventUpdate(event);
                }
            } catch (Exception ex) {
                ExceptionPrinter.printHistory(new CouldNotPerformException("Internal notification failed!", ex), logger);
            }
        }
    }
}
