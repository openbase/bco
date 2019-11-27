package org.openbase.bco.authentication.lib;

/*-
 * #%L
 * BCO Authentication Library
 * %%
 * Copyright (C) 2017 - 2019 openbase.org
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
import org.openbase.jul.exception.*;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.iface.Shutdownable;
import org.openbase.jul.schedule.SyncObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author <a href="mailto:thuxohl@techfak.uni-bielefeld.de">Tamino Huxohl</a>
 */
public class CachedAuthenticationRemote {

    private static final Logger LOGGER = LoggerFactory.getLogger(CachedAuthenticationRemote.class);

    private static AuthenticationRemote authenticationRemote;
    private static transient boolean shutdown = false;
    private static final SyncObject REMOTE_LOCK = new SyncObject("CachedAuthenticationRemote");

    /**
     * Setup shutdown hook
     */
    static {
        try {
            Shutdownable.registerShutdownHook(() -> {
                shutdown = true;
                shutdown();
            });
        } catch (CouldNotPerformException ex) {
            if (!ExceptionProcessor.isCausedBySystemShutdown(ex)) {
                ExceptionPrinter.printHistory("Could not register shutdown hook!", ex, LOGGER);
            }
        }
    }

    /**
     * Get a cashed authenticator remote. The first call to this method will create a new authenticator remote
     * which is activated which will be returned. This instance is saved and will be returned on all following calls.
     *
     * @return a cashed authenticator remote
     *
     * @throws NotAvailableException if the cashed instance is not available
     */
    public synchronized static AuthenticationRemote getRemote() throws NotAvailableException {
        try {
            if (shutdown) {
                throw new ShutdownInProgressException("AuthenticationRemote");
            }

            if (authenticationRemote == null) {
                try {
                    authenticationRemote = new AuthenticationRemote();
                    authenticationRemote.init();
                    authenticationRemote.activate();
                    authenticationRemote.waitForActivation();
                } catch (Exception ex) {
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

    public static void prepare() throws CouldNotPerformException {
        synchronized (REMOTE_LOCK) {
            // check if externally called.
            if (authenticationRemote != null || !JPService.testMode()) {
                LOGGER.warn("This manual registry preparation is only available during unit tests and not allowed during normal operation!");
                return;
            }

            shutdown = false;

            getRemote();
        }
    }

    public static void shutdown() {
        // check if externally called.
        if (shutdown == false && !JPService.testMode()) {
            LOGGER.warn("This manual authentication remote shutdown is only available during unit tests and not allowed during normal operation!");
            return;
        }

        synchronized (REMOTE_LOCK) {

            // set flag again for the unit test case
            shutdown = true;

            if (authenticationRemote != null) {
                authenticationRemote.shutdown();
                authenticationRemote = null;
            }
        }
    }
}
