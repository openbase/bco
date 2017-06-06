package org.openbase.bco.authenticator.lib;

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
import org.openbase.bco.authenticator.lib.jp.JPAuthentificationScope;
import org.openbase.jps.core.JPService;
import org.openbase.jps.exception.JPNotAvailableException;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InitializationException;
import org.openbase.jul.extension.rsb.com.NotInitializedRSBRemoteServer;
import org.openbase.jul.extension.rsb.com.RSBFactoryImpl;
import org.openbase.jul.extension.rsb.com.RSBSharedConnectionConfig;
import org.openbase.jul.extension.rsb.iface.RSBRemoteServer;
import org.openbase.jul.iface.Manageable;
import org.openbase.jul.iface.VoidInitializable;
import org.openbase.jul.schedule.WatchDog;
import rst.domotic.authentification.AuthenticatorTicketType.AuthenticatorTicket;
import rst.domotic.authentification.LoginResponseType.LoginResponse;
import org.openbase.bco.authenticator.lib.iface.Authenticator;

/**
 *
 * @author Tamino Huxohl <thuxohl@techfak.uni-bielefel.de>
 */
public class ClientRemote implements Authenticator, Manageable<Void>, VoidInitializable {

    private RSBRemoteServer remoteServer;
    private WatchDog serverWatchDog;

    public ClientRemote() {
        this.remoteServer = new NotInitializedRSBRemoteServer();
    }

    @Override
    public void init() throws InitializationException, InterruptedException {
        try {
            remoteServer = RSBFactoryImpl.getInstance().createSynchronizedRemoteServer(JPService.getProperty(JPAuthentificationScope.class).getValue(), RSBSharedConnectionConfig.getParticipantConfig());

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

    @Override
    public Future<LoginResponse> requestTGT(String clientId) throws CouldNotPerformException {
        return remoteServer.callAsync("requestTGT", clientId);
    }

    @Override
    public Future<LoginResponse> requestCST(AuthenticatorTicket authenticatorTicket) throws CouldNotPerformException {
        return remoteServer.callAsync("requestCST", authenticatorTicket);
    }

    @Override
    public Future<AuthenticatorTicket> validateCST(AuthenticatorTicket authenticatorTicket) throws CouldNotPerformException {
        return remoteServer.callAsync("validateCST", authenticatorTicket);
    }

}
