package org.openbase.bco.registry.location.remote;

/*
 * #%L
 * BCO Registry Location Remote
 * %%
 * Copyright (C) 2014 - 2018 openbase.org
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
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.openbase.jps.core.JPService;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.FatalImplementationErrorException;
import org.openbase.jul.exception.InvalidStateException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.schedule.SyncObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public class CachedLocationRegistryRemote {

    private static final Logger LOGGER = LoggerFactory.getLogger(CachedLocationRegistryRemote.class);

    private static final SyncObject REMOTE_LOCK = new SyncObject("CachedLocationRegistryRemoteLock");

    private static LocationRegistryRemote registryRemote;
    private static boolean shutdown = false;

    static {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            shutdown = true;
            shutdown();
        }));
    }

    public synchronized static void reinitialize() throws InterruptedException, CouldNotPerformException {
        try {
            getRegistry().reinit(REMOTE_LOCK);
            getRegistry().requestData().get(10, TimeUnit.SECONDS);
        } catch (ExecutionException | TimeoutException | CouldNotPerformException | CancellationException ex) {
            throw new CouldNotPerformException("Could not reinitialize " + CachedLocationRegistryRemote.class.getSimpleName() + "!", ex);
        }
    }

    /**
     *
     * @return @throws InterruptedException
     * @throws NotAvailableException
     */
    public synchronized static LocationRegistryRemote getRegistry() throws InterruptedException, NotAvailableException {
        try {
            if (shutdown) {
                throw new InvalidStateException("Remote service is shutting down!");
            }

            if (registryRemote == null) {
                try {
                    registryRemote = new LocationRegistryRemote();
                    registryRemote.init();
                    registryRemote.activate();
                    registryRemote.lock(REMOTE_LOCK);
                } catch (Exception ex) {
                    if (registryRemote != null) {
                        registryRemote.unlock(REMOTE_LOCK);
                        registryRemote.shutdown();
                        registryRemote = null;
                    }
                    throw ExceptionPrinter.printHistoryAndReturnThrowable(new CouldNotPerformException("Could not start cached location registry remote!", ex), LOGGER);
                }
            }
            return registryRemote;
        } catch (CouldNotPerformException ex) {
            throw new NotAvailableException("cached location registry", ex);
        }
    }

    public static void waitForData() throws InterruptedException, CouldNotPerformException {
        getRegistry().waitForData();
    }

    public static void waitForData(long timeout, TimeUnit timeUnit) throws CouldNotPerformException, InterruptedException {
        getRegistry().waitForData(timeout, timeUnit);
    }

    /**
     * Method blocks until the registry is not handling any tasks and is currently consistent.
     *
     * Note: If you have just modified the registry this method can maybe return immediately if the task is not yet received by the registry controller. So you should prefer the futures of the modification methods for synchronization tasks.
     *
     * @throws InterruptedException is thrown in case the thread was externally interrupted.
     * @throws org.openbase.jul.exception.CouldNotPerformException is thrown if the wait could not be performed.
     */
    public static void waitUntilReady() throws InterruptedException, CouldNotPerformException {
        getRegistry().waitUntilReady();
    }

    /**
     * Method shutdown the cached registry instances.
     *
     * Please use method with care!
     * Make sure no other instances are using the cached remote instances before shutdown.
     *
     * Note: This method takes only effect in unit tests, otherwise this call is ignored. During normal operation there is not need for a manual registry shutdown because each registry takes care of its shutdown.
     */
    public static void shutdown() {

        // check if externally called.
        if (shutdown == false && !JPService.testMode()) {
            LOGGER.warn("This manual registry shutdown is only available during unit tests and not allowed during normal operation!");
            return;
        }

        synchronized (REMOTE_LOCK) {
            if (registryRemote != null) {
                try {
                    registryRemote.unlock(REMOTE_LOCK);
                } catch (final CouldNotPerformException ex) {
                    ExceptionPrinter.printHistory(new FatalImplementationErrorException("Internal remote was locked by an external instance!", CachedLocationRegistryRemote.class, ex), LOGGER);
                }
                registryRemote.shutdown();
                registryRemote = null;
            }
        }
    }
}
