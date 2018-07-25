package org.openbase.bco.app.cloud.connector;

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
                    cloudConnectorRemote.init();
                    cloudConnectorRemote.activate();
                    cloudConnectorRemote.waitForActivation();
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
        if (shutdown == false && !JPService.testMode()) {
            LOGGER.warn("This manual cloud connector remote shutdown is only available during unit tests and not allowed during normal operation!");
            return;
        }

        if (cloudConnectorRemote != null) {
            cloudConnectorRemote.shutdown();
            cloudConnectorRemote = null;
        }
    }

}
