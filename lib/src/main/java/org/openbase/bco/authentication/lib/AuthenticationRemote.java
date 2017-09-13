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
import java.util.concurrent.Future;
import org.openbase.bco.authentication.lib.jp.JPAuthenticationScope;
import org.openbase.jps.core.JPService;
import org.openbase.jps.exception.JPNotAvailableException;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InitializationException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.extension.rsb.com.NotInitializedRSBRemoteServer;
import org.openbase.jul.extension.rsb.com.RPCHelper;
import org.openbase.jul.extension.rsb.com.RSBFactoryImpl;
import org.openbase.jul.extension.rsb.com.RSBSharedConnectionConfig;
import org.openbase.jul.extension.rsb.iface.RSBRemoteServer;
import org.openbase.jul.iface.Manageable;
import org.openbase.jul.iface.VoidInitializable;
import org.openbase.jul.schedule.WatchDog;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rst.domotic.authentication.AuthenticatedValueType.AuthenticatedValue;
import rst.domotic.authentication.LoginCredentialsChangeType.LoginCredentialsChange;
import rst.domotic.authentication.TicketAuthenticatorWrapperType.TicketAuthenticatorWrapper;
import rst.domotic.authentication.TicketSessionKeyWrapperType.TicketSessionKeyWrapper;

/**
 *
 * @author <a href="mailto:thuxohl@techfak.uni-bielefeld.de">Tamino Huxohl</a>
 */
public class AuthenticationRemote implements AuthenticationService, Manageable<Void>, VoidInitializable {

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(TicketSessionKeyWrapper.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(TicketAuthenticatorWrapper.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(LoginCredentialsChange.getDefaultInstance()));
    }
    
    private RSBRemoteServer remoteServer;
    private WatchDog serverWatchDog;

    public AuthenticationRemote() {
        this.remoteServer = new NotInitializedRSBRemoteServer();
    }

    @Override
    public void init() throws InitializationException, InterruptedException {
        try {
            remoteServer = RSBFactoryImpl.getInstance().createSynchronizedRemoteServer(JPService.getProperty(JPAuthenticationScope.class).getValue(), RSBSharedConnectionConfig.getParticipantConfig());

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

    @Override
    public Future<TicketSessionKeyWrapper> requestTicketGrantingTicket(String clientId) throws CouldNotPerformException {
        return RPCHelper.callRemoteServerMethod(clientId, remoteServer, TicketSessionKeyWrapper.class);
    }

    @Override
    public Future<TicketSessionKeyWrapper> requestClientServerTicket(TicketAuthenticatorWrapper ticketAuthenticatorWrapper) throws CouldNotPerformException {
        return RPCHelper.callRemoteServerMethod(ticketAuthenticatorWrapper, remoteServer, TicketSessionKeyWrapper.class);
    }

    @Override
    public Future<TicketAuthenticatorWrapper> validateClientServerTicket(TicketAuthenticatorWrapper ticketAuthenticatorWrapper) throws CouldNotPerformException {
        return RPCHelper.callRemoteServerMethod(ticketAuthenticatorWrapper, remoteServer, TicketAuthenticatorWrapper.class);
    }

    @Override
    public Future<TicketAuthenticatorWrapper> changeCredentials(LoginCredentialsChange loginCredentialsChange) throws CouldNotPerformException {
        return RPCHelper.callRemoteServerMethod(loginCredentialsChange, remoteServer, TicketAuthenticatorWrapper.class);
    }

    @Override
    public Future<TicketAuthenticatorWrapper> register(LoginCredentialsChange loginCredentialsChange) throws CouldNotPerformException {
        return RPCHelper.callRemoteServerMethod(loginCredentialsChange, remoteServer, TicketAuthenticatorWrapper.class);
    }

    @Override
    public Future<TicketAuthenticatorWrapper> removeUser(LoginCredentialsChange loginCredentialsChange) throws CouldNotPerformException {
        return RPCHelper.callRemoteServerMethod(loginCredentialsChange, remoteServer, TicketAuthenticatorWrapper.class);
    }

    @Override
    public Future<TicketAuthenticatorWrapper> setAdministrator(LoginCredentialsChange loginCredentialsChange) throws CouldNotPerformException {
        return RPCHelper.callRemoteServerMethod(loginCredentialsChange, remoteServer, TicketAuthenticatorWrapper.class);
    }

    @Override
    public Future<AuthenticatedValue> requestServiceServerSecretKey(TicketAuthenticatorWrapper ticketAuthenticatorWrapper) throws CouldNotPerformException {
        return RPCHelper.callRemoteServerMethod(ticketAuthenticatorWrapper, remoteServer, AuthenticatedValue.class);
    }

    @Override
    public Future<Boolean> isAdmin(String userId) throws NotAvailableException, CouldNotPerformException {
        return RPCHelper.callRemoteServerMethod(userId, remoteServer, Boolean.class);
    }

    @Override
    public Future<Boolean> hasUser(String userId) throws CouldNotPerformException {
        return RPCHelper.callRemoteServerMethod(userId, remoteServer, Boolean.class);
    }
}
