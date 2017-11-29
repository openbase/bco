package org.openbase.bco.registry.lib.com;

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
import org.openbase.jul.storage.registry.RegistryRemote;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author <a href="mailto:pLeminoq@openbase.org">Tamino Huxohl</a>
 */
public abstract class CachedRegistryRemote {
    public static final Logger LOGGER = LoggerFactory.getLogger(CachedRegistryRemote.class);

    private static final SyncObject REMOTE_LOCK = new SyncObject("CachedRegistryRemoteLock");

    private static RegistryRemote registryRemote;
    private static boolean shutdown = false;

    static {
        Runtime.getRuntime().addShutdownHook(new Thread() {

            @Override
            public void run() {
                shutdown = true;
                shutdown();
            }
        });
    }

    public synchronized static void reinitialize() throws InterruptedException, CouldNotPerformException {
        try {
            if (shutdown) {
                throw new InvalidStateException("Remote service is shutting down!");
            }
//            getRegistry().unlock(REMOTE_LOCK);
//            getRegistry().init();
            getRegistry().reinit();
//            getRegistry().lock(REMOTE_LOCK);
            getRegistry().requestData().get(10, TimeUnit.SECONDS);
        } catch (ExecutionException | TimeoutException | CouldNotPerformException | CancellationException ex) {
            throw new CouldNotPerformException("Could not reinitialize " + getName() + "!", ex);
        }
    }

    /**
     *
     * @return @throws InterruptedException
     * @throws NotAvailableException
     */
    protected synchronized static RegistryRemote getRegistry() throws InterruptedException, NotAvailableException {
        try {
            if (shutdown) {
                throw new InvalidStateException("Remote service is shutting down!");
            }

            if (registryRemote == null) {
                try {
                    
                    registryRemote = getRegistryRemoteClass().getConstructor().newInstance();
                    registryRemote.init();
                    registryRemote.activate();
                    registryRemote.lock(REMOTE_LOCK);
                } catch (Exception ex) {
                    if (registryRemote != null) {
                        registryRemote.unlock(REMOTE_LOCK);
                        registryRemote.shutdown();
                        registryRemote = null;
                    }
                    throw ExceptionPrinter.printHistoryAndReturnThrowable(new CouldNotPerformException("Could not start cached app registry remote!", ex), LOGGER);
                }
            }
            return registryRemote;
        } catch (CouldNotPerformException ex) {
            throw new NotAvailableException("cached app registry", ex);
        }
    }

    public static void waitForData() throws InterruptedException, CouldNotPerformException {
        if (registryRemote == null) {
            getRegistry();
        }
        registryRemote.waitForData();
    }

    public static void waitForData(long timeout, TimeUnit timeUnit) throws CouldNotPerformException, InterruptedException {
        if (registryRemote == null) {
            getRegistry();
        }
        registryRemote.waitForData(timeout, timeUnit);
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
                    ExceptionPrinter.printHistory(new FatalImplementationErrorException("Internal remote was locked by an external instance!", CachedRegistryRemote.class, ex), LOGGER);
                }
                registryRemote.shutdown();
                registryRemote = null;
            }
        }
    }
    
    public static final String getName() {
        return CachedRegistryRemote.class.getSimpleName();
    }
    
    public static final <R extends RegistryRemote> Class<R> getRegistryRemoteClass() {
        return null;
    }
}
