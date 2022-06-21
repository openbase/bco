package org.openbase.bco.authentication.lib.com;

/*-
 * #%L
 * BCO Authentication Library
 * %%
 * Copyright (C) 2017 - 2021 openbase.org
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

import com.google.protobuf.Message;
import kotlin.Unit;
import kotlin.jvm.functions.Function1;
import org.jetbrains.annotations.NotNull;
import org.openbase.bco.authentication.lib.SessionManager;
import org.openbase.bco.authentication.lib.future.AuthenticatedValueFuture;
import org.openbase.bco.authentication.lib.future.ReLoginFuture;
import org.openbase.bco.authentication.lib.iface.AuthenticatedRequestable;
import org.openbase.jul.communication.data.RPCResponse;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.ExceptionProcessor;
import org.openbase.jul.exception.RejectedException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.communication.controller.AbstractConfigurableRemote;
import org.openbase.jul.pattern.Observer;
import org.openbase.jul.schedule.FutureProcessor;
import org.openbase.type.communication.EventType.Event;
import org.openbase.type.communication.mqtt.ResponseType;
import org.openbase.type.domotic.authentication.AuthenticatedValueType.AuthenticatedValue;
import org.openbase.type.domotic.authentication.TicketAuthenticatorWrapperType.TicketAuthenticatorWrapper;
import org.openbase.type.domotic.authentication.UserClientPairType.UserClientPair;

import java.util.concurrent.*;

public class AbstractAuthenticatedConfigurableRemote<M extends Message, CONFIG extends Message> extends AbstractConfigurableRemote<M, CONFIG> {

    private final Observer<SessionManager, UserClientPair> loginObserver;
    /**
     * Data object for other permissions;
     */
    private M otherData;

    public AbstractAuthenticatedConfigurableRemote(final Class<M> dataClass, final Class<CONFIG> configClass) {
        super(dataClass, configClass);
        this.setMessageProcessor(new AuthenticatedMessageProcessor<>(dataClass));

        this.loginObserver = (source, data) -> {
            // somebody new logged in
            if (otherData != null) {
                setData(otherData);
            }
            restartSyncTask();
        };
    }

    @Override
    public void activate() throws InterruptedException, CouldNotPerformException {
        super.activate();
        SessionManager.getInstance().addLoginObserver(loginObserver);
    }

    @Override
    public void deactivate() throws InterruptedException, CouldNotPerformException {
        SessionManager.getInstance().removeLoginObserver(loginObserver);
        super.deactivate();
    }

    @Override
    protected Function1<Event, Unit> generateHandler() {
        return new AuthenticatedUpdateHandler();
    }

    @Override
    protected Future<RPCResponse<M>> internalRequestStatus() {
        try {
            final SessionManager sessionManager = SessionManager.getInstance();
            if (sessionManager.isLoggedIn()) {
                final TicketAuthenticatorWrapper ticketAuthenticatorWrapper = sessionManager.initializeServiceServerRequest();
                final Future<RPCResponse<AuthenticatedValue>> requestFuture = getRpcClient().callMethod(
                        AuthenticatedRequestable.REQUEST_DATA_AUTHENTICATED_METHOD,
                        AuthenticatedValue.class,
                        ticketAuthenticatorWrapper
                );
                final Future<AuthenticatedValue> authenticatedValueFuture =
                        FutureProcessor.postProcess(
                            (input, timeout, timeUnit) -> input.getResponse(),
                            requestFuture
                        );
                final ReLoginFuture<AuthenticatedValue> reloginFuture = new ReLoginFuture<>(
                        authenticatedValueFuture,
                        sessionManager
                );
                final AuthenticatedValueFuture authenticationFuture =  new AuthenticatedValueFuture<>(reloginFuture,
                        getDataClass(),
                        ticketAuthenticatorWrapper,
                        sessionManager
                );
                return new AuthenticatedRPCResponseFuture<>(requestFuture, authenticationFuture);
            } else {
                return super.internalRequestStatus();
            }
        } catch (RejectedException ex) {
            return FutureProcessor.canceledFuture(null, ex);
        }
    }

    private class AuthenticatedUpdateHandler implements Function1<Event, Unit> {

        @Override
        public Unit invoke(Event event) {
            try {

                if (!SessionManager.getInstance().isLoggedIn() || !event.hasPayload()) {
                    applyEventUpdate(event);
                    return null;
                }

                // cache data object with other permissions.
                otherData = event.getPayload().unpack(getDataClass());

                restartSyncTask();

            } catch (Exception ex) {
                if(!ExceptionProcessor.isCausedBySystemShutdown(ex)) {
                    ExceptionPrinter.printHistory(new CouldNotPerformException("Internal notification failed!", ex), logger);
                }
            }
            return null;
        }
    }
}
