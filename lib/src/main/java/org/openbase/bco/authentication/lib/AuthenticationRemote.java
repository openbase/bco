package org.openbase.bco.authentication.lib;

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

import org.openbase.bco.authentication.lib.jp.JPAuthenticationScope;
import org.openbase.jps.core.JPService;
import org.openbase.jps.exception.JPNotAvailableException;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InitializationException;
import org.openbase.jul.extension.rsb.com.NotInitializedRSBRemoteServer;
import org.openbase.jul.communication.controller.RPCHelper;
import org.openbase.jul.extension.rsb.com.RSBFactoryImpl;
import org.openbase.jul.extension.rsb.com.RSBSharedConnectionConfig;
import org.openbase.jul.extension.rsb.iface.RSBRemoteServer;
import org.openbase.jul.extension.rsb.scope.ScopeTransformer;
import org.openbase.jul.extension.type.processing.ScopeProcessor;
import org.openbase.jul.iface.Manageable;
import org.openbase.jul.iface.VoidInitializable;
import org.openbase.jul.schedule.WatchDog;
import org.openbase.type.domotic.authentication.AuthenticatedValueType.AuthenticatedValue;
import org.openbase.type.domotic.authentication.TicketAuthenticatorWrapperType.TicketAuthenticatorWrapper;
import org.openbase.type.domotic.authentication.TicketSessionKeyWrapperType.TicketSessionKeyWrapper;
import org.openbase.type.domotic.authentication.UserClientPairType.UserClientPair;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;

import java.util.concurrent.Future;

/**
 * @author <a href="mailto:thuxohl@techfak.uni-bielefeld.de">Tamino Huxohl</a>
 */
public class AuthenticationRemote implements AuthenticationService, Manageable<Void>, VoidInitializable {

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(TicketSessionKeyWrapper.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(TicketAuthenticatorWrapper.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(AuthenticatedValue.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(UserClientPair.getDefaultInstance()));
    }

    private RSBRemoteServer remoteServer;
    private WatchDog serverWatchDog;

    public AuthenticationRemote() {
        this.remoteServer = new NotInitializedRSBRemoteServer();
    }

    @Override
    public void init() throws InitializationException, InterruptedException {
        try {
            remoteServer = RSBFactoryImpl.getInstance().createSynchronizedRemoteServer(
                    ScopeTransformer.transform(JPService.getProperty(JPAuthenticationScope.class).getValue()),
                    RSBSharedConnectionConfig.getParticipantConfig());

            serverWatchDog = new WatchDog(remoteServer, "AuthenticatorWatchDog");
        } catch (JPNotAvailableException | CouldNotPerformException ex) {
            throw new InitializationException(this, ex);
        }
    }

    @Override
    public void activate() throws CouldNotPerformException, InterruptedException {
        serverWatchDog.activate();
    }

    @Override
    public void deactivate() throws CouldNotPerformException, InterruptedException {
        serverWatchDog.deactivate();
    }

    @Override
    public boolean isActive() {
        return remoteServer.isActive();
    }

    public void waitForActivation() throws CouldNotPerformException, InterruptedException {
        try {
            serverWatchDog.waitForServiceActivation();
        } catch (final CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not wait for activation!", ex);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param userClientPair {@inheritDoc}
     *
     * @return {@inheritDoc}
     */
    @Override
    public Future<TicketSessionKeyWrapper> requestTicketGrantingTicket(final UserClientPair userClientPair) {
        return RPCHelper.callRemoteServerMethod(userClientPair, remoteServer, TicketSessionKeyWrapper.class);
    }

    /**
     * {@inheritDoc}
     *
     * @param ticketAuthenticatorWrapper {@inheritDoc}
     *
     * @return {@inheritDoc}
     */
    @Override
    public Future<TicketSessionKeyWrapper> requestClientServerTicket(final TicketAuthenticatorWrapper ticketAuthenticatorWrapper) {
        return RPCHelper.callRemoteServerMethod(ticketAuthenticatorWrapper, remoteServer, TicketSessionKeyWrapper.class);
    }

    /**
     * {@inheritDoc}
     *
     * @param ticketAuthenticatorWrapper {@inheritDoc}
     *
     * @return {@inheritDoc}
     */
    @Override
    public Future<TicketAuthenticatorWrapper> validateClientServerTicket(TicketAuthenticatorWrapper ticketAuthenticatorWrapper) {
        return RPCHelper.callRemoteServerMethod(ticketAuthenticatorWrapper, remoteServer, TicketAuthenticatorWrapper.class);
    }

    /**
     * {@inheritDoc}
     *
     * @param authenticatedValue {@inheritDoc}
     *
     * @return {@inheritDoc}
     */
    @Override
    public Future<AuthenticatedValue> changeCredentials(final AuthenticatedValue authenticatedValue) {
        return RPCHelper.callRemoteServerMethod(authenticatedValue, remoteServer, AuthenticatedValue.class);
    }

    /**
     * {@inheritDoc}
     *
     * @param authenticatedValue {@inheritDoc}
     *
     * @return {@inheritDoc}
     */
    @Override
    public Future<AuthenticatedValue> register(final AuthenticatedValue authenticatedValue) {
        return RPCHelper.callRemoteServerMethod(authenticatedValue, remoteServer, AuthenticatedValue.class);
    }

    /**
     * {@inheritDoc}
     *
     * @param authenticatedValue {@inheritDoc}
     *
     * @return {@inheritDoc}
     */
    @Override
    public Future<AuthenticatedValue> removeUser(final AuthenticatedValue authenticatedValue) {
        return RPCHelper.callRemoteServerMethod(authenticatedValue, remoteServer, AuthenticatedValue.class);
    }

    /**
     * {@inheritDoc}
     *
     * @param AuthenticatedValue {@inheritDoc}
     *
     * @return {@inheritDoc}
     */
    @Override
    public Future<AuthenticatedValue> setAdministrator(final AuthenticatedValue AuthenticatedValue) {
        return RPCHelper.callRemoteServerMethod(AuthenticatedValue, remoteServer, AuthenticatedValue.class);
    }

    /**
     * {@inheritDoc}
     *
     * @param ticketAuthenticatorWrapper {@inheritDoc}
     *
     * @return {@inheritDoc}
     */
    @Override
    public Future<AuthenticatedValue> requestServiceServerSecretKey(final TicketAuthenticatorWrapper ticketAuthenticatorWrapper) {
        return RPCHelper.callRemoteServerMethod(ticketAuthenticatorWrapper, remoteServer, AuthenticatedValue.class);
    }

    /**
     * {@inheritDoc}
     *
     * @param userId {@inheritDoc}
     *
     * @return {@inheritDoc}
     */
    @Override
    public Future<Boolean> isAdmin(final String userId) {
        return RPCHelper.callRemoteServerMethod(userId, remoteServer, Boolean.class);
    }

    /**
     * {@inheritDoc}
     *
     * @param userOrClientId {@inheritDoc}
     *
     * @return {@inheritDoc}
     */
    @Override
    public Future<Boolean> hasUser(final String userOrClientId) {
        return RPCHelper.callRemoteServerMethod(userOrClientId, remoteServer, Boolean.class);
    }
}
