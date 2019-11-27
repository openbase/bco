package org.openbase.bco.registry.unit.remote;

/*
 * #%L
 * BCO Registry Unit Remote
 * %%
 * Copyright (C) 2014 - 2019 openbase.org
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

import org.openbase.bco.registry.unit.lib.UnitRegistry;
import org.openbase.jps.core.JPService;
import org.openbase.jul.exception.*;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.iface.Shutdownable;
import org.openbase.jul.iface.Shutdownable.ShutdownDaemon;
import org.openbase.jul.schedule.SyncObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public class CachedUnitRegistryRemote {

    private static final Logger LOGGER = LoggerFactory.getLogger(CachedUnitRegistryRemote.class);

    private static final SyncObject REMOTE_LOCK = new SyncObject("CachedUnitRegistryRemoteLock");

    private static UnitRegistryRemote registryRemote;
    private transient static boolean shutdown = false;

    /**
     * Setup shutdown hook
     */
    static {
        try {
            Shutdownable.registerShutdownHook(() -> {
                shutdown = true;
                shutdown();
            }, 4000);
        } catch (CouldNotPerformException ex) {
            if (!ExceptionProcessor.isCausedBySystemShutdown(ex)) {
                ExceptionPrinter.printHistory("Could not register shutdown hook!", ex, LOGGER);
            }
        }
    }

    /**
     * Reinitialize the internal registry remote and request data to be synchronized again.
     *
     * @throws InterruptedException     is thrown in case the thread was externally interrupted.
     * @throws CouldNotPerformException is thrown if the reinitialization could not be performed.
     */
    public synchronized static void reinitialize() throws InterruptedException, CouldNotPerformException {
        try {
            // only call re-init if the registry was activated and initialized in the first place
            if (registryRemote != null) {
                getRegistry().reinit(REMOTE_LOCK);
            }
            getRegistry().requestData().get(30, TimeUnit.SECONDS);
        } catch (ExecutionException | TimeoutException | CouldNotPerformException | CancellationException ex) {
            throw new CouldNotPerformException("Could not reinitialize " + CachedUnitRegistryRemote.class.getSimpleName() + "!", ex);
        }
    }

    /**
     * Get a cached UnitRegistryRemote.
     *
     * @param waitForData defines if the method should block until the data is available.
     *
     * @return a cached UnitRegistryRemote
     *
     * @throws NotAvailableException if the initial startup of the UnitRegistryRemote fails
     * @throws InterruptedException  is thrown if the thread is externally interrupted.
     */
    public synchronized static UnitRegistryRemote getRegistry(final boolean waitForData) throws CouldNotPerformException, InterruptedException {
        waitForData();
        return getRegistry();
    }

    /**
     * Get a cached UnitRegistryRemote.
     *
     * @return a cached UnitRegistryRemote
     *
     * @throws NotAvailableException if the initial startup of the UnitRegistryRemote fails
     */
    public synchronized static UnitRegistryRemote getRegistry() throws NotAvailableException {
        try {
            if (shutdown) {
                throw new ShutdownInProgressException(UnitRegistry.class);
            }

            if (registryRemote == null) {
                try {
                    registryRemote = new UnitRegistryRemote();
                    registryRemote.init();
                    registryRemote.activate();
                    registryRemote.lock(REMOTE_LOCK);
                } catch (Exception ex) {
                    if (registryRemote != null) {
                        registryRemote.unlock(REMOTE_LOCK);
                        registryRemote.shutdown();
                        registryRemote = null;
                    }
                    throw ExceptionPrinter.printHistoryAndReturnThrowable(new CouldNotPerformException("Could not start cached unit registry remote!", ex), LOGGER);
                }
            }
            return registryRemote;
        } catch (CouldNotPerformException ex) {
            throw new NotAvailableException("Cached unit registry is not available!", ex);
        }
    }

    /**
     * Wait for data on the internal registry remote.
     *
     * @throws InterruptedException     is thrown in case the thread was externally interrupted.
     * @throws CouldNotPerformException is thrown if the wait could not be performed.
     */
    public static void waitForData() throws InterruptedException, CouldNotPerformException {
        getRegistry().waitForData();
    }

    /**
     * Wait for data on the internal registry remote and define a timeout.
     *
     * @throws InterruptedException     is thrown in case the thread was externally interrupted.
     * @throws CouldNotPerformException is thrown if the wait could not be performed.
     */
    public static void waitForData(long timeout, TimeUnit timeUnit) throws CouldNotPerformException, InterruptedException {
        getRegistry().waitForData(timeout, timeUnit);
    }

    /**
     * Method blocks until the registry is not handling any tasks and is currently consistent.
     * <p>
     * Note: If you have just modified the registry this method can maybe return immediately if the task is not yet received by the registry controller. So you should prefer the futures of the modification methods for synchronization tasks.
     *
     * @throws InterruptedException                                is thrown in case the thread was externally interrupted.
     * @throws org.openbase.jul.exception.CouldNotPerformException is thrown if the wait could not be performed.
     */
    public static void waitUntilReady() throws InterruptedException, CouldNotPerformException {
        getRegistry().waitUntilReady();
    }

    public static void prepare() throws CouldNotPerformException {
        synchronized (REMOTE_LOCK) {
            // check if externally called.
            if (registryRemote != null || !JPService.testMode()) {
                LOGGER.warn("This manual registry preparation is only available during unit tests and not allowed during normal operation!");
                return;
            }

            shutdown = false;
            getRegistry();
        }
    }

    /**
     * Shutdown the cached registry instances.
     * <p> <b>
     * Please use method with care!
     * Make sure no other instances are using the cached remote instances before shutdown.
     * </b> </p>
     * Note: This method takes only effect in unit tests, otherwise this call is ignored. During normal operation there is not need for a manual registry shutdown because each registry takes care of its shutdown.
     */
    public static void shutdown() {

        // check if externally called.
        if (shutdown == false && !JPService.testMode()) {
            LOGGER.warn("This manual registry shutdown is only available during unit tests and not allowed during normal operation!");
            return;
        }

        synchronized (REMOTE_LOCK) {

            // set flag again for the unit test case
            shutdown = true;

            if (registryRemote != null) {
                try {
                    registryRemote.unlock(REMOTE_LOCK);
                } catch (final CouldNotPerformException ex) {
                    ExceptionPrinter.printHistory(new FatalImplementationErrorException("Internal remote was locked by an external instance!", CachedUnitRegistryRemote.class, ex), LOGGER);
                }
                registryRemote.shutdown();
                registryRemote = null;
            }
        }
    }
}
