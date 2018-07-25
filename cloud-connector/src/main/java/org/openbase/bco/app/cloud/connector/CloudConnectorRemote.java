package org.openbase.bco.app.cloud.connector;

import com.google.gson.JsonObject;
import org.openbase.bco.authentication.lib.SessionManager;
import org.openbase.bco.authentication.lib.future.AuthenticatedValueFuture;
import org.openbase.bco.authentication.lib.jp.JPAuthenticationScope;
import org.openbase.jps.core.JPService;
import org.openbase.jps.exception.JPNotAvailableException;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InitializationException;
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

import java.util.concurrent.Future;

/**
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class CloudConnectorRemote implements CloudConnectorInterface, Manageable<Void>, VoidInitializable {

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(AuthenticatedValue.getDefaultInstance()));
    }

    private RSBRemoteServer remoteServer;
    private WatchDog serverWatchDog;

    public CloudConnectorRemote() {
        this.remoteServer = new NotInitializedRSBRemoteServer();
    }

    @Override
    public void init() throws InitializationException {
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

    public Future<String> connect(final boolean connect) throws CouldNotPerformException {
        final JsonObject params = new JsonObject();
        params.addProperty("connect", connect);
        final AuthenticatedValue authenticatedValue = SessionManager.getInstance().initializeRequest(params.toString(), null, null);
        final Future<AuthenticatedValue> internalFuture = connect(authenticatedValue);
        return new AuthenticatedValueFuture<>(internalFuture, String.class, authenticatedValue.getTicketAuthenticatorWrapper(), SessionManager.getInstance());
    }

    public Future<String> connect(final String password) throws CouldNotPerformException {
        return connect(password, true);
    }

    public Future<String> connect(final String password, final boolean autoStart) throws CouldNotPerformException {
        // request authorization token for current user -> how to determine full rights?
        final String authorizationToken = "";
        final String params = RegistrationHelper.createRegistrationData(password, authorizationToken, autoStart);
        final AuthenticatedValue authenticatedValue = SessionManager.getInstance().initializeRequest(params, null, null);
        final Future<AuthenticatedValue> internalFuture = connect(authenticatedValue);
        return new AuthenticatedValueFuture<>(internalFuture, String.class, authenticatedValue.getTicketAuthenticatorWrapper(), SessionManager.getInstance());
    }

    @Override
    public Future<AuthenticatedValue> connect(final AuthenticatedValue authenticatedValue) throws CouldNotPerformException {
        return RPCHelper.callRemoteServerMethod(authenticatedValue, remoteServer, AuthenticatedValue.class);
    }
}
