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
import org.openbase.jul.communication.config.CommunicatorConfig;
import org.openbase.jul.communication.controller.RPCUtils;
import org.openbase.jul.communication.iface.CommunicatorFactory;
import org.openbase.jul.communication.iface.RPCClient;
import org.openbase.jul.communication.mqtt.CommunicatorFactoryImpl;
import org.openbase.jul.communication.mqtt.DefaultCommunicatorConfig;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InitializationException;
import org.openbase.jul.iface.Manageable;
import org.openbase.jul.iface.VoidInitializable;
import org.openbase.jul.schedule.WatchDog;
import org.openbase.type.domotic.authentication.AuthenticatedValueType.AuthenticatedValue;
import org.openbase.type.domotic.authentication.TicketAuthenticatorWrapperType.TicketAuthenticatorWrapper;
import org.openbase.type.domotic.authentication.TicketSessionKeyWrapperType.TicketSessionKeyWrapper;
import org.openbase.type.domotic.authentication.UserClientPairType.UserClientPair;

import java.util.concurrent.Future;

/**
 * @author <a href="mailto:thuxohl@techfak.uni-bielefeld.de">Tamino Huxohl</a>
 */
public class AuthenticationRemote implements AuthenticationService, Manageable<Void>, VoidInitializable {

    private final CommunicatorFactory factory = CommunicatorFactoryImpl.Companion.getInstance();
    private final CommunicatorConfig defaultCommunicatorConfig = DefaultCommunicatorConfig.Companion.getInstance();

    private RPCClient rpcClient;
    private WatchDog serverWatchDog;

    public AuthenticationRemote() {
        this.rpcClient = null;
    }

    @Override
    public void init() throws InitializationException, InterruptedException {
        try {
            rpcClient = factory.createRPCClient(
                JPService.getProperty(JPAuthenticationScope.class).getValue(),
                defaultCommunicatorConfig
            );

            serverWatchDog = new WatchDog(rpcClient, "AuthenticatorWatchDog");
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
        return rpcClient.isActive();
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
        return RPCUtils.callRemoteServerMethod(userClientPair, rpcClient, TicketSessionKeyWrapper.class);
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
        return RPCUtils.callRemoteServerMethod(ticketAuthenticatorWrapper, rpcClient, TicketSessionKeyWrapper.class);
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
        return RPCUtils.callRemoteServerMethod(ticketAuthenticatorWrapper, rpcClient, TicketAuthenticatorWrapper.class);
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
        return RPCUtils.callRemoteServerMethod(authenticatedValue, rpcClient, AuthenticatedValue.class);
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
        return RPCUtils.callRemoteServerMethod(authenticatedValue, rpcClient, AuthenticatedValue.class);
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
        return RPCUtils.callRemoteServerMethod(authenticatedValue, rpcClient, AuthenticatedValue.class);
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
        return RPCUtils.callRemoteServerMethod(AuthenticatedValue, rpcClient, AuthenticatedValue.class);
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
        return RPCUtils.callRemoteServerMethod(ticketAuthenticatorWrapper, rpcClient, AuthenticatedValue.class);
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
        return RPCUtils.callRemoteServerMethod(userId, rpcClient, Boolean.class);
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
        return RPCUtils.callRemoteServerMethod(userOrClientId, rpcClient, Boolean.class);
    }
}
