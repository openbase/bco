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
import org.openbase.bco.authentication.lib.SessionManager;
import org.openbase.bco.authentication.lib.future.ReLoginFuture;
import org.openbase.bco.authentication.lib.iface.AuthenticatedRequestable;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.ExceptionProcessor;
import org.openbase.jul.exception.RejectedException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.communication.controller.AbstractConfigurableRemote;
import org.openbase.jul.pattern.Observer;
import org.openbase.jul.schedule.FutureProcessor;
import org.openbase.jul.schedule.GlobalCachedExecutorService;
import org.openbase.type.domotic.authentication.TicketAuthenticatorWrapperType.TicketAuthenticatorWrapper;
import org.openbase.type.domotic.authentication.UserClientPairType.UserClientPair;
import rsb.Event;
import rsb.Handler;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;

import java.util.concurrent.*;

public class AbstractAuthenticatedConfigurableRemote<M extends Message, CONFIG extends Message> extends AbstractConfigurableRemote<M, CONFIG> {

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(TicketAuthenticatorWrapper.getDefaultInstance()));
    }

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
            if (isSyncRunning()) {
                // if a sync task is still running restart it
                restartSyncTask();
            } else {
                // trigger a new data request to update data for the user
                requestData();
            }
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
    protected Handler generateHandler() {
        return new AuthenticatedUpdateHandler();
    }

    @Override
    protected Future<Event> internalRequestStatus() {
        try {
            if (SessionManager.getInstance().isLoggedIn()) {
                final Event event = new Event(TicketAuthenticatorWrapper.class, SessionManager.getInstance().initializeServiceServerRequest());
                return new ReLoginFuture<>(getRemoteServer().callAsync(AuthenticatedRequestable.REQUEST_DATA_AUTHENTICATED_METHOD, event), SessionManager.getInstance());
            } else {
                return super.internalRequestStatus();
            }
        } catch (RejectedException ex) {
            return FutureProcessor.canceledFuture(Event.class, ex);
        }
    }

    private class AuthenticatedUpdateHandler implements Handler {

        @Override
        public void internalNotify(Event event) {
            try {
                if (event.getData() != null) {
                    otherData = (M) event.getData();
                    if (SessionManager.getInstance().isLoggedIn()) {
                        // received a new data event from the controller which is filtered for other permissions, so trigger an authenticated request
                        GlobalCachedExecutorService.submit((Callable<Void>) () -> {
                            if (isSyncRunning()) {
                                // a sync task is currently running so wait for it to finish and trigger a new one
                                // to make sure that the latest data update is received
                                try {
                                    requestData().get(10, TimeUnit.SECONDS);
                                } catch (InterruptedException ex) {
                                    Thread.currentThread().interrupt();
                                    return null;
                                } catch (ExecutionException | TimeoutException ex) {
                                    throw new CouldNotPerformException("Could not wait for running sync task", ex);
                                } catch (CancellationException ex) {
                                    logger.error("Cancellation exception", ex);
                                    // request data was cancelled and is most likely done again by the login observer
                                    return null;
                                }
                            }
                            requestData();
                            return null;
                        });
                    } else {
                        applyEventUpdate(event);
                    }
                } else {
                    applyEventUpdate(event);
                }
            } catch (Exception ex) {
                if(!ExceptionProcessor.isCausedBySystemShutdown(ex)) {
                    ExceptionPrinter.printHistory(new CouldNotPerformException("Internal notification failed!", ex), logger);
                }
            }
        }
    }
}
