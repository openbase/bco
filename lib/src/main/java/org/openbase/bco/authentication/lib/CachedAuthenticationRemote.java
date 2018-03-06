package org.openbase.bco.authentication.lib;

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

import org.openbase.jps.core.JPService;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InvalidStateException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author <a href="mailto:thuxohl@techfak.uni-bielefeld.de">Tamino Huxohl</a>
 */
public class CachedAuthenticationRemote {

    private static final Logger LOGGER = LoggerFactory.getLogger(CachedAuthenticationRemote.class);
    
    private static AuthenticationRemote authenticationRemote;
    private static boolean shutdown = false;

    static {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            shutdown = true;
            shutdown();
        }));
    }

    /**
     * Get a cashed authenticator remote. The first call to this method will create a new authenticator remote
     * which is activated which will be returned. This instance is saved and will be returned on all following calls.
     *
     * @return a cashed authenticator remote
     * @throws InterruptedException if in the first call to this methods the creation and activation of the remote is interrupted
     * @throws NotAvailableException if the cashed instance is not available
     */
    public synchronized static AuthenticationRemote getRemote() throws InterruptedException, NotAvailableException {
        try {
            if (shutdown) {
                throw new InvalidStateException("Remote service is shutting down!");
            }

            if (authenticationRemote == null) {
                try {
                    authenticationRemote = new AuthenticationRemote();
                    authenticationRemote.init();
                    authenticationRemote.activate();
                    authenticationRemote.waitForActivation();
                } catch (CouldNotPerformException ex) {
                    if (authenticationRemote != null) {
                        authenticationRemote.shutdown();
                        authenticationRemote = null;
                    }
                    throw ExceptionPrinter.printHistoryAndReturnThrowable(new CouldNotPerformException("Could not start cached authenticator remote!", ex), LOGGER);
                }
            }
            return authenticationRemote;
        } catch (CouldNotPerformException ex) {
            throw new NotAvailableException("CachedAuthenticatorClientRemote", ex);
        }
    }

    public static void shutdown() {
        // check if externally called.
        if (shutdown == false && !JPService.testMode()) {
            LOGGER.warn("This manual authentication remote shutdown is only available during unit tests and not allowed during normal operation!");
            return;
        }
        
        if (authenticationRemote != null) {
            authenticationRemote.shutdown();
            authenticationRemote = null;
        }
    }
}
