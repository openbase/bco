package org.openbase.bco.app.cloud.connector;

/*-
 * #%L
 * BCO Cloud Connector
 * %%
 * Copyright (C) 2018 openbase.org
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

import org.openbase.jps.core.JPService;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InvalidStateException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class CachedCloudConnectorRemote {

    private static final Logger LOGGER = LoggerFactory.getLogger(CachedCloudConnectorRemote.class);

    private static CloudConnectorRemote cloudConnectorRemote;
    private static boolean shutdown = false;

    static {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            shutdown = true;
            shutdown();
        }));
    }

    /**
     * Get a cashed cloud connector remote. The first call to this method will create a new cloud connector remote
     * which is activated which will be returned. This instance is saved and will be returned on all following calls.
     *
     * @return a cashed cloud connector remote
     * @throws NotAvailableException if the cashed instance is not available
     */
    public synchronized static CloudConnectorRemote getRemote() throws NotAvailableException {
        try {
            if (shutdown) {
                throw new InvalidStateException("Remote service is shutting down!");
            }

            if (cloudConnectorRemote == null) {
                try {
                    cloudConnectorRemote = new CloudConnectorRemote();
                    cloudConnectorRemote.init(CloudConnectorLauncher.getCloudConnectorUnitConfig());
                    cloudConnectorRemote.activate();
                } catch (Exception ex) {
                    if (cloudConnectorRemote != null) {
                        cloudConnectorRemote.shutdown();
                        cloudConnectorRemote = null;
                    }
                    throw ExceptionPrinter.printHistoryAndReturnThrowable(new CouldNotPerformException("Could not start cached cloud connector remote!", ex), LOGGER);
                }
            }
            return cloudConnectorRemote;
        } catch (CouldNotPerformException ex) {
            throw new NotAvailableException("CachedCloudConnectorRemote", ex);
        }
    }

    public static void shutdown() {
        // check if externally called.
        if (!shutdown && !JPService.testMode()) {
            LOGGER.warn("This manual cloud connector remote shutdown is only available during unit tests and not allowed during normal operation!");
            return;
        }

        if (cloudConnectorRemote != null) {
            cloudConnectorRemote.shutdown();
            cloudConnectorRemote = null;
        }
    }

}
