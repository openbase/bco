package org.openbase.bco.authenticator.lib;

import java.util.concurrent.Future;
import org.openbase.bco.authenticator.lib.iface.AuthenticatorInterface;
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

/**
 *
 * @author Tamino Huxohl <thuxohl@techfak.uni-bielefel.de>
 */
public class ClientRemote implements AuthenticatorInterface, Manageable<Void>, VoidInitializable {

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
