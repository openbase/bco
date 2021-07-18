package org.openbase.bco.registry.lib.com;

/*
 * #%L
 * BCO Registry Lib
 * %%
 * Copyright (C) 2014 - 2021 openbase.org
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

import com.google.protobuf.AbstractMessage;
import org.openbase.bco.authentication.lib.com.AbstractAuthenticatedControllerServer;
import org.openbase.jps.core.JPService;
import org.openbase.jps.exception.JPNotAvailableException;
import org.openbase.jul.communication.controller.RPCHelper;
import org.openbase.jul.communication.controller.jp.JPScope;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.exception.*;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.exception.printer.LogLevel;
import org.openbase.jul.extension.rsb.iface.RSBLocalServer;
import org.openbase.jul.iface.Launchable;
import org.openbase.jul.pattern.ChangeListener;
import org.openbase.jul.pattern.Observer;
import org.openbase.jul.schedule.GlobalCachedExecutorService;
import org.openbase.jul.schedule.SyncObject;
import org.openbase.jul.storage.file.ProtoBufJSonFileProvider;
import org.openbase.jul.storage.registry.ConsistencyHandler;
import org.openbase.jul.storage.registry.ProtoBufFileSynchronizedRegistry;
import org.openbase.jul.storage.registry.Registry;
import org.openbase.jul.storage.registry.RegistryController;
import org.openbase.type.communication.ScopeType;
import org.openbase.type.communication.ScopeType.Scope;
import org.openbase.type.domotic.authentication.UserClientPairType.UserClientPair;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Future;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static org.openbase.jul.storage.registry.version.DBVersionControl.DB_CONVERTER_PACKAGE_NAME;

/**
 * @param <M>
 * @param <MB>
 *
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public abstract class AbstractRegistryController<M extends AbstractMessage & Serializable, MB extends M.Builder<MB>> extends AbstractAuthenticatedControllerServer<M, MB> implements RegistryController<M>, Launchable<Scope> {

    public static final boolean SPARSELY_REGISTRY_DATA_FILTERED = true;
    public static final boolean SPARSELY_REGISTRY_DATA_NOTIFIED = false;

    private final SyncObject CHANGE_NOTIFIER = new SyncObject("WaitUntilReadySync");
    private final List<ProtoBufFileSynchronizedRegistry> registryList;
    private final Class<? extends JPScope> jpScopeProperty;
    private final boolean filterSparselyRegistryData;
    private final List<Registry> lockedRegistries;
    private final Random randomJitter;
    private final ReentrantReadWriteLock lock;
    protected ProtoBufJSonFileProvider protoBufJSonFileProvider;
    private Future notifyChangeFuture;
    private ChangeListener transactionListener;
    private final Observer changeObserver;

    /**
     * Constructor creates a new RegistryController based on the given scope and publishing registry data of the given builder.
     * <p>
     * Node: By default this constructor filters sparsely registry data.
     * If you want to publish data of internal registries even if other internal registries are not ready
     * yet, use can use the other constructor of this class and set the filterSparselyRegistryData flag to false.
     *
     * @param jpScopeProperty the scope which is used for registry communication and data publishing.
     * @param builder         the builder to build the registry data message.
     *
     * @throws InstantiationException
     */
    public AbstractRegistryController(final Class<? extends JPScope> jpScopeProperty, MB builder) throws InstantiationException {
        this(jpScopeProperty, builder, SPARSELY_REGISTRY_DATA_FILTERED);
    }

    /**
     * Constructor creates a new RegistryController based on the given scope and publishing registry data of the given builder.
     *
     * @param jpScopeProperty            the scope which is used for registry communication and data publishing.
     * @param builder                    the builder to build the registry data message.
     * @param filterSparselyRegistryData if this flag is true the registry data is only published if non of the internal registries is busy.
     *
     * @throws InstantiationException
     */
    public AbstractRegistryController(final Class<? extends JPScope> jpScopeProperty, MB builder, final boolean filterSparselyRegistryData) throws InstantiationException {
        super(builder);
        this.filterSparselyRegistryData = filterSparselyRegistryData;
        this.jpScopeProperty = jpScopeProperty;

        this.registryList = new ArrayList<>();
        this.protoBufJSonFileProvider = new ProtoBufJSonFileProvider();

        this.randomJitter = new Random();
        this.lockedRegistries = new ArrayList<>();
        this.lock = new ReentrantReadWriteLock();
        this.transactionListener = () -> updateTransactionId();
        this.changeObserver = (source, data) -> notifyChange();
    }

    @Override
    public ScopeType.Scope getDefaultConfig() throws NotAvailableException {
        try {
            return JPService.getProperty(jpScopeProperty).getValue();
        } catch (JPNotAvailableException ex) {
            throw new NotAvailableException("DefaultConfig", ex);
        }
    }

    @Override
    protected void postInit() throws InitializationException, InterruptedException {
        super.postInit();
        try {
            try {
                registerRegistries();
            } catch (CouldNotPerformException ex) {
                throw new CouldNotPerformException("Could not register all internal registries!", ex);
            }

            try {
                registerRemoteRegistries();
            } catch (CouldNotPerformException ex) {
                throw new CouldNotPerformException("Could register all remote registries!", ex);
            }

            try {
                activateVersionControl();
            } catch (CouldNotPerformException ex) {
                throw new CouldNotPerformException("Could not activate version control for all internal registries!", ex);
            }

            try {
                loadRegistries();
            } catch (CouldNotPerformException ex) {
                throw new CouldNotPerformException("Could not load all internal registries!", ex);
            }

            try {
                registerConsistencyHandler();
            } catch (CouldNotPerformException ex) {
                throw new CouldNotPerformException("Could not register consistency handler for all internal registries!", ex);
            }
        } catch (CouldNotPerformException ex) {
            throw new InitializationException(this, ex);
        }
    }

    @Override
    public void activate() throws InterruptedException, CouldNotPerformException {
        try {
            registerDependencies();

            // plugins often are able to register configurations, therefore remote dependencies have to be available for consistency checks
            //todo: waiting within the activation method is a bit critical since in case the middle ware is not available the management lock is hold for a long amount of time.
            waitForRemoteDependencies();

            try {
                registerPlugins();
            } catch (CouldNotPerformException ex) {
                throw new CouldNotPerformException("Could not register plugins for all internal registries!", ex);
            }

            try {
                registerObserver();
            } catch (CouldNotPerformException ex) {
                throw new CouldNotPerformException("Could not register observer for all internal registries!", ex);
            }

            for (ProtoBufFileSynchronizedRegistry registry : registryList) {
                registry.activateDependencies();
            }
            super.activate();

            performInitialConsistencyCheck();
        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not activate "+this+" registry!", ex);
        }
    }

    @Override
    public void deactivate() throws InterruptedException, CouldNotPerformException {
        super.deactivate();
        removeDependencies();
    }

    @Override
    public void shutdown() {
        super.shutdown();
        for (ProtoBufFileSynchronizedRegistry registry : registryList) {
            registry.shutdown();
        }
    }

    @Override
    public void registerMethods(final RSBLocalServer server) throws CouldNotPerformException {
        super.registerMethods(server);
        RPCHelper.registerInterface(RegistryController.class, this, server);
    }

    @Override
    public void notifyChange() throws CouldNotPerformException, InterruptedException {
        try {
            // sync registry flags
            syncRegistryFlags();

            // filter notification in case an internal registry is busy.
            if (filterSparselyRegistryData) {
                // filter notification if any internal registry is busy to avoid spreading incomplete registry context.
                for (ProtoBufFileSynchronizedRegistry registry : getRegistries()) {
                    if (registry.isBusy()) {
                        /*
                         * It can happen that a registry is still busy but it does not produce any
                         * changes. In this case the observer which triggers this notification is not
                         * triggered and thus no notification takes place. So schedule a task which
                         * will notify once all registries are ready.
                         */
                        scheduleNotifyChangeTask();
                        return;
                    }
                }
            }

            // cancel in case the registry which was still busy also notifies
            // this way notifications only take place once
            if (notifyChangeFuture != null && !notifyChangeFuture.isDone()) {
                notifyChangeFuture.cancel(true);
            }

            // lock internal registries so that no changes can take place while notifying
            lockInternalRegistries();
            super.notifyChange();
        } finally {
            // unlock internal registries only if this thread has locked them before
            if (lock.isWriteLockedByCurrentThread()) {
                unlockInternalRegistries();
            }
            synchronized (CHANGE_NOTIFIER) {
                CHANGE_NOTIFIER.notifyAll();
            }
        }
    }

    private void scheduleNotifyChangeTask() {
        if (notifyChangeFuture == null || notifyChangeFuture.isDone()) {
            notifyChangeFuture = GlobalCachedExecutorService.submit(() -> {
                try {
                    waitUntilReady();
                    syncRegistryFlags();
                    try {
                        // lock internal registries so that no changes can take place while notifying
                        lockInternalRegistries();
                        super.notifyChange();
                    } finally {
                        // unlock internal registries only if this thread has locked them before
                        if (lock.isWriteLockedByCurrentThread()) {
                            unlockInternalRegistries();
                        }
                    }
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                } catch (CouldNotPerformException ex) {
                    logger.warn("Could not notify change", ex);
                } finally {
                    synchronized (CHANGE_NOTIFIER) {
                        CHANGE_NOTIFIER.notifyAll();
                    }
                }
            });
        }
    }

    private void lockInternalRegistries() throws InterruptedException {
        boolean success;
        // iterate until thread is interrupted
        while (!Thread.currentThread().isInterrupted()) {
            success = true;

            // try to lock this controller
            if (lock.writeLock().tryLock()) {
                // try to lock all internal registries
                for (Registry registry : getRegistries()) {
                    try {
                        if (registry.tryLockRegistry()) {
                            // if successfully locked add to list
                            lockedRegistries.add(registry);
                        } else {
                            // if one could not be locked break
                            success = false;
                            break;
                        }
                    } catch (RejectedException ex) {
                        // only remote registries throw this exception on trylock so create a fatal implementation error
                        ExceptionPrinter.printHistory(new FatalImplementationErrorException("Internal registry[" + registry + "] not lockable", this), logger);
                    }
                }

                // return if successfully locked all registries
                if (success) {
                    return;
                }
                // unlock all if not successfully locked
                unlockInternalRegistries();
            }

            // sleep for a random time and afterwards try to lock again
            Thread.sleep(20 + randomJitter.nextInt(30));
        }
    }

    private void unlockInternalRegistries() {
        assert lock.isWriteLockedByCurrentThread();

        // unlock all internal registries
        for (Registry registry : lockedRegistries) {
            registry.unlockRegistry();
        }
        // clear list
        lockedRegistries.clear();
        // unlock this controller
        lock.writeLock().unlock();
    }

    private void removeDependencies() throws CouldNotPerformException {
        for (ProtoBufFileSynchronizedRegistry registry : registryList) {
            registry.removeAllDependencies();
        }
    }

    private void registerObserver() throws CouldNotPerformException {
        for (ProtoBufFileSynchronizedRegistry registry : registryList) {
            registry.addObserver(changeObserver);
            registry.addTransactionListener(transactionListener);
        }
    }

    private void waitForRemoteDependencies() throws CouldNotPerformException, InterruptedException {
        for (ProtoBufFileSynchronizedRegistry registry : registryList) {
            registry.waitForRemoteDependencies();
        }
    }

    private void loadRegistries() throws CouldNotPerformException {
        for (final ProtoBufFileSynchronizedRegistry registry : registryList) {
            registry.loadRegistry();
        }
    }

    private void activateVersionControl() throws CouldNotPerformException {
        Package versionConverterPackage;
        for (final ProtoBufFileSynchronizedRegistry registry : registryList) {
            try {
                versionConverterPackage = detectVersionConverterPackage();
            } catch (NotAvailableException ex) {
                ExceptionPrinter.printHistory(new CouldNotPerformException("Skip version control activation for " + registry + "!", ex), logger, LogLevel.WARN);
                continue;
            }
            registry.activateVersionControl(versionConverterPackage);
        }
    }

    private void performInitialConsistencyCheck() throws CouldNotPerformException, InterruptedException {
        for (final ProtoBufFileSynchronizedRegistry registry : registryList) {
            try {
                logger.debug("Trigger initial consistency check of " + registry + " with " + registry.getEntries().size() + " entries.");
                registry.checkConsistency();
            } catch (CouldNotPerformException ex) {
                if (!ExceptionProcessor.isCausedBySystemShutdown(ex)) {
                    ExceptionPrinter.printHistory(new CouldNotPerformException("Initial consistency check failed!", ex), logger);
                }
                notifyChange();
            }
        }
    }

    protected void registerDependency(final Registry dependency, final Class messageClass) throws CouldNotPerformException {
        for (ProtoBufFileSynchronizedRegistry registry : registryList) {
            if (messageClass.equals(registry.getMessageClass())) {
                registry.registerDependency(dependency);
            } else {
                logger.debug("Registration of dependency " + dependency + " skipped for " + registry + " because " + messageClass.getSimpleName() + " is not compatible.");
            }
        }
    }

    protected void registerRegistry(final ProtoBufFileSynchronizedRegistry registry) {
        registryList.add(registry);
    }

    protected void registerConsistencyHandler(final ConsistencyHandler consistencyHandler, final Class messageClass) throws CouldNotPerformException {
        for (ProtoBufFileSynchronizedRegistry registry : registryList) {
            if (messageClass.equals(registry.getMessageClass())) {
                registry.registerConsistencyHandler(consistencyHandler);
            } else {
                logger.debug("Registration of " + consistencyHandler.getClass().getSimpleName() + " skipped for " + registry + " because " + messageClass.getSimpleName() + " is not compatible.");
            }
        }
    }

    protected List<ProtoBufFileSynchronizedRegistry> getRegistries() {
        return registryList;
    }

    private Package detectVersionConverterPackage() throws CouldNotPerformException {
        Package converterPackage;
        try {
            converterPackage = Class.forName((getClass().getPackage().getName() + "." + DB_CONVERTER_PACKAGE_NAME + ".ConverterPackageIdentifier")).getPackage();
        } catch (ClassNotFoundException ex) {
            throw new NotAvailableException("ConverterPackage[" + getClass().getPackage().getName() + "." + DB_CONVERTER_PACKAGE_NAME + ".ConverterPackageIdentifier" + "]", ex);
        }
        return converterPackage;
    }

    /**
     * {@inheritDoc}
     *
     * @throws InterruptedException {@inheritDoc}
     */
    @Override
    public void waitUntilReady() throws InterruptedException {

        while (true) {
            // handle interruption
            if (Thread.interrupted()) {
                throw new InterruptedException();
            }

            synchronized (CHANGE_NOTIFIER) {
                // check ready state
                if (isReady()) {
                    return;
                }
                // sleep until change
                // todo: why is this timeout needed??? Is there any event skipped? Please debug me!
                CHANGE_NOTIFIER.wait(500);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Future<Void> waitUntilReadyFuture() {
        return GlobalCachedExecutorService.submit(() -> {
            waitUntilReady();
            return null;
        });
    }

    /**
     * {@inheritDoc}
     *
     * @return {@inheritDoc}
     */
    @Override
    public Boolean isReady() {
        for (ProtoBufFileSynchronizedRegistry registry : registryList) {
            if (!registry.isReady()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Test if at least one local registry is inconsistent.
     *
     * @return true if all managed registries are consistent and else false
     */
    @Override
    public Boolean isConsistent() {
        for (ProtoBufFileSynchronizedRegistry registry : registryList) {
            if (!registry.isConsistent()) {
                return false;
            }
        }
        return true;
    }

    @Override
    protected M filterDataForUser(MB dataBuilder, final UserClientPair userClientPair) throws CouldNotPerformException {
        return (M) dataBuilder.build();
    }

    protected abstract void registerConsistencyHandler() throws CouldNotPerformException, InterruptedException;

    protected abstract void registerPlugins() throws CouldNotPerformException, InterruptedException;

    protected abstract void registerRegistries() throws CouldNotPerformException;

    protected abstract void registerDependencies() throws CouldNotPerformException;

    protected abstract void syncRegistryFlags() throws CouldNotPerformException, InterruptedException;

    protected abstract void registerRemoteRegistries() throws CouldNotPerformException;
}
