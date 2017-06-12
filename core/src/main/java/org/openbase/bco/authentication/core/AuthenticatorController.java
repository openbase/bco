package org.openbase.bco.authentication.core;

/*-
 * #%L
 * BCO Authentication Core
 * %%
 * Copyright (C) 2017 openbase.org
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */
import java.io.IOException;
import java.io.StreamCorruptedException;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import org.openbase.bco.authentication.lib.AuthenticationServerHandlerImpl;
import org.openbase.bco.authentication.lib.EncryptionHelper;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InitializationException;
import org.openbase.jul.iface.Launchable;
import org.openbase.jul.iface.VoidInitializable;
import org.openbase.bco.authentication.lib.jp.JPAuthenticationScope;
import org.openbase.jps.core.JPService;
import org.openbase.jps.exception.JPNotAvailableException;
import org.openbase.jul.extension.rsb.com.NotInitializedRSBLocalServer;
import org.openbase.jul.extension.rsb.com.RPCHelper;
import org.openbase.jul.extension.rsb.com.RSBFactoryImpl;
import org.openbase.jul.extension.rsb.com.RSBSharedConnectionConfig;
import org.openbase.jul.extension.rsb.iface.RSBLocalServer;
import org.openbase.jul.schedule.GlobalCachedExecutorService;
import org.openbase.jul.schedule.WatchDog;
import rst.domotic.authentication.TicketAuthenticatorWrapperType.TicketAuthenticatorWrapper;
import rst.domotic.authentication.TicketSessionKeyWrapperType.TicketSessionKeyWrapper;
import org.openbase.bco.authentication.lib.AuthenticationService;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.exception.RejectedException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.exception.printer.LogLevel;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Tamino Huxohl <thuxohl@techfak.uni-bielefel.de>
 */
public class AuthenticatorController implements AuthenticationService, Launchable<Void>, VoidInitializable {

    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(AuthenticatorController.class);

    private RSBLocalServer server;
    private WatchDog serverWatchDog;

    private final byte[] TGSSessionKey;
    private final byte[] TGSPrivateKey;
    private final byte[] SSSessionKey;
    private final byte[] SSPrivateKey;

    private final AuthenticationServerHandlerImpl authenticationHandler;

    public AuthenticatorController() {
        this.server = new NotInitializedRSBLocalServer();

        this.authenticationHandler = new AuthenticationServerHandlerImpl();
        this.TGSSessionKey = EncryptionHelper.generateKey();
        this.TGSPrivateKey = EncryptionHelper.generateKey();
        this.SSSessionKey = EncryptionHelper.generateKey();
        this.SSPrivateKey = EncryptionHelper.generateKey();
    }

    @Override
    public void init() throws InitializationException, InterruptedException {
        try {
            server = RSBFactoryImpl.getInstance().createSynchronizedLocalServer(JPService.getProperty(JPAuthenticationScope.class).getValue(), RSBSharedConnectionConfig.getParticipantConfig());

            // register rpc methods.
            RPCHelper.registerInterface(AuthenticationService.class, this, server);

            serverWatchDog = new WatchDog(server, "AuthenticatorWatchDog");
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
        if (serverWatchDog != null) {
            serverWatchDog.deactivate();
        }
    }

    @Override
    public boolean isActive() {
        if (serverWatchDog != null) {
            return serverWatchDog.isActive();
        } else {
            return false;
        }
    }

    @Override
    public Future<TicketSessionKeyWrapper> requestTGT(String clientId) throws CouldNotPerformException {
        return GlobalCachedExecutorService.submit(new Callable<TicketSessionKeyWrapper>() {
            @Override
            public TicketSessionKeyWrapper call() throws NotAvailableException, CouldNotPerformException {
                try {
                    return authenticationHandler.handleKDCRequest(clientId, "", TGSSessionKey, TGSPrivateKey);
                } catch (NotAvailableException ex) {
                    throw ExceptionPrinter.printHistoryAndReturnThrowable(ex, LOGGER, LogLevel.ERROR);
                } catch (InterruptedException | CouldNotPerformException | IOException ex) {
                    ExceptionPrinter.printHistory(ex, LOGGER, LogLevel.ERROR);
                    throw new CouldNotPerformException("Internal server error. Please try again.");
                }
            }
        });
    }

    @Override
    public Future<TicketSessionKeyWrapper> requestCST(TicketAuthenticatorWrapper ticketAuthenticatorWrapper) throws CouldNotPerformException {
        return GlobalCachedExecutorService.submit(new Callable<TicketSessionKeyWrapper>() {
            @Override
            public TicketSessionKeyWrapper call() throws RejectedException, StreamCorruptedException, CouldNotPerformException {
                try {
                    return authenticationHandler.handleTGSRequest(TGSSessionKey, TGSPrivateKey, SSSessionKey, SSPrivateKey, ticketAuthenticatorWrapper);
                } catch (RejectedException ex) {
                    ExceptionPrinter.printHistory(ex, LOGGER, LogLevel.WARN);
                    throw new RejectedException(ex.getMessage());
                } catch (StreamCorruptedException ex) {
                    ExceptionPrinter.printHistory(ex, LOGGER, LogLevel.WARN);
                    throw new StreamCorruptedException(ex.getMessage());
                } catch (IOException ex) {
                    ExceptionPrinter.printHistory(ex, LOGGER, LogLevel.ERROR);
                    throw new CouldNotPerformException("Internal server error. Please try again.");
                }
            }
        });
    }

    @Override
    public Future<TicketAuthenticatorWrapper> validateCST(TicketAuthenticatorWrapper ticketAuthenticatorWrapper) throws CouldNotPerformException {
        return GlobalCachedExecutorService.submit(new Callable<TicketAuthenticatorWrapper>() {
            @Override
            public TicketAuthenticatorWrapper call() throws RejectedException, StreamCorruptedException, CouldNotPerformException {
                try {
                    return authenticationHandler.handleSSRequest(SSSessionKey, SSPrivateKey, ticketAuthenticatorWrapper);
                } catch (RejectedException ex) {
                    ExceptionPrinter.printHistory(ex, LOGGER, LogLevel.WARN);
                    throw new RejectedException(ex.getMessage());
                } catch (StreamCorruptedException ex) {
                    ExceptionPrinter.printHistory(ex, LOGGER, LogLevel.WARN);
                    throw new StreamCorruptedException(ex.getMessage());
                } catch (IOException ex) {
                    ExceptionPrinter.printHistory(ex, LOGGER, LogLevel.ERROR);
                    throw new CouldNotPerformException("Internal server error. Please try again.");
                }
            }
        });
    }
}
