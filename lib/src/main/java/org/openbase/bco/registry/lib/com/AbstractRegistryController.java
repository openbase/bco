package org.openbase.bco.registry.lib.com;

/*
 * #%L
 * BCO Registry Lib
 * %%
 * Copyright (C) 2014 - 2017 openbase.org
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
import com.google.protobuf.GeneratedMessage;
import java.util.ArrayList;
import java.util.List;
import org.openbase.jps.core.JPService;
import org.openbase.jps.exception.JPNotAvailableException;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.CouldNotTransformException;
import org.openbase.jul.exception.InitializationException;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.exception.printer.LogLevel;
import org.openbase.jul.extension.rsb.com.RSBCommunicationService;
import org.openbase.jul.extension.rsb.scope.ScopeTransformer;
import org.openbase.jul.extension.rsb.scope.jp.JPScope;
import org.openbase.jul.iface.Launchable;
import org.openbase.jul.pattern.Observable;
import org.openbase.jul.schedule.SyncObject;
import org.openbase.jul.storage.file.ProtoBufJSonFileProvider;
import org.openbase.jul.storage.registry.ConsistencyHandler;
import org.openbase.jul.storage.registry.ProtoBufFileSynchronizedRegistry;
import org.openbase.jul.storage.registry.Registry;
import org.openbase.jul.storage.registry.RegistryController;
import org.openbase.jul.storage.registry.RegistryRemote;
import org.openbase.jul.storage.registry.RemoteRegistry;
import static org.openbase.jul.storage.registry.version.DBVersionControl.DB_CONVERTER_PACKAGE_NAME;
import rst.rsb.ScopeType;
import rst.rsb.ScopeType.Scope;

/**
 *
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 * @param <M>
 * @param <MB>
 */
public abstract class AbstractRegistryController<M extends GeneratedMessage, MB extends M.Builder<MB>> extends RSBCommunicationService<M, MB> implements RegistryController<M>, Launchable<Scope> {

    public static final boolean SPARSELY_REGISTRY_DATA_FILTERED = true;
    public static final boolean SPARSELY_REGISTRY_DATA_NOTIFIED = false;

    protected ProtoBufJSonFileProvider protoBufJSonFileProvider = new ProtoBufJSonFileProvider();

    private final SyncObject CHANGE_NOTIFIER = new SyncObject("WaitUntilReadySync");

    /**
     * These are the depending registries where this registry is based on.
     */
    private final List<RegistryRemote> registryRemoteList;
    private final List<RemoteRegistry> remoteRegistryList;
    private final List<ProtoBufFileSynchronizedRegistry> registryList;
    private final Class<? extends JPScope> jpScopePropery;
    private final boolean filterSparselyRegistryData;

    /**
     * Constructor creates a new RegistryController based on the given scope and publishing registry data of the given builder.
     *
     * Node: By default this constructor filters sparsely registry data.
     * If you want to publish data of internal registries even if other internal registries are not ready
     * yet, use can use the other constructor of this class and set the filterSparselyRegistryData flag to false.
     *
     * @param jpScopePropery the scope which is used for registry communication and data publishing.
     * @param builder the builder to build the registry data message.
     * @throws InstantiationException
     */
    public AbstractRegistryController(final Class<? extends JPScope> jpScopePropery, MB builder) throws InstantiationException {
        this(jpScopePropery, builder, SPARSELY_REGISTRY_DATA_FILTERED);
    }

    /**
     * Constructor creates a new RegistryController based on the given scope and publishing registry data of the given builder.
     *
     * @param jpScopePropery the scope which is used for registry communication and data publishing.
     * @param builder the builder to build the registry data message.
     * @param filterSparselyRegistryData if this flag is true the registry data is only published if non of the internal registries is busy.
     * @throws InstantiationException
     */
    public AbstractRegistryController(final Class<? extends JPScope> jpScopePropery, MB builder, final boolean filterSparselyRegistryData) throws InstantiationException {
        super(builder);
        this.filterSparselyRegistryData = filterSparselyRegistryData;
        this.jpScopePropery = jpScopePropery;
        this.registryRemoteList = new ArrayList<>();
        this.registryList = new ArrayList<>();
        this.remoteRegistryList = new ArrayList<>();
        this.protoBufJSonFileProvider = new ProtoBufJSonFileProvider();
    }

    @Override
    public ScopeType.Scope getDefaultConfig() throws NotAvailableException {
        try {
            return ScopeTransformer.transform(JPService.getProperty(jpScopePropery).getValue());
        } catch (JPNotAvailableException | CouldNotTransformException ex) {
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
                registerRegistryRemotes();
            } catch (CouldNotPerformException ex) {
                throw new CouldNotPerformException("Could not register all registry remotes!", ex);
            }

            try {
                registerRemoteRegistries();
            } catch (CouldNotPerformException ex) {
                throw new CouldNotPerformException("Could register all remote registries!", ex);
            }

            try {
                initRemoteRegistries();
            } catch (CouldNotPerformException ex) {
                throw new CouldNotPerformException("Could not init all remote registries!", ex);
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
        } catch (CouldNotPerformException ex) {
            throw new InitializationException(this, ex);
        }
    }

    @Override
    public void activate() throws InterruptedException, CouldNotPerformException {
        try {
            super.activate();
            activateRemoteRegistries();
            activateRegistryRemotes();
            registerDependencies();
            performInitialConsistencyCheck();
        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not activate location registry!", ex);
        }
    }

    @Override
    public void deactivate() throws InterruptedException, CouldNotPerformException {
        super.deactivate();
        deactivateRemoteRegistries();
        deactivateRegistryRemotes();
        removeDependencies();
    }

    @Override
    public void shutdown() {
        super.shutdown();
        registryRemoteList.stream().forEach((remote) -> {
            remote.shutdown();
        });
        remoteRegistryList.stream().forEach((remoteRegistry) -> {
            remoteRegistry.shutdown();
        });
        registryList.stream().forEach((registry) -> {
            registry.shutdown();
        });
    }

    @Override
    public void notifyChange() throws CouldNotPerformException, InterruptedException {
        if (filterSparselyRegistryData) {
            // filter notification if any internal registry is busy to avoid spreading incomplete registry context.
            for (ProtoBufFileSynchronizedRegistry registry : getRegistries()) {
                if (registry.isBusy()) {
                    // skip notification
                    return;
                }
            }
        }

        // continue notification
        syncRegistryFlags();
        super.notifyChange();
        synchronized (CHANGE_NOTIFIER) {
            CHANGE_NOTIFIER.notify();
        }
    }

    private void initRemoteRegistries() throws CouldNotPerformException, InterruptedException {
        for (final RegistryRemote remote : registryRemoteList) {
            remote.init();
        }
    }

    protected void activateRemoteRegistries() throws CouldNotPerformException, InterruptedException {
        for (RemoteRegistry remoteRegistry : remoteRegistryList) {
            if (remoteRegistry instanceof SynchronizedRemoteRegistry) {
                ((SynchronizedRemoteRegistry) remoteRegistry).activate();
            }
        }
    }

    private void activateRegistryRemotes() throws CouldNotPerformException, InterruptedException {
        for (final RegistryRemote remote : registryRemoteList) {
            remote.activate();
        }

        for (final RegistryRemote remote : registryRemoteList) {
            remote.waitForData();
        }
    }

    private void deactivateRemoteRegistries() throws CouldNotPerformException, InterruptedException {
        for (final RemoteRegistry remoteRegistry : remoteRegistryList) {
            if (remoteRegistry instanceof SynchronizedRemoteRegistry) {
                ((SynchronizedRemoteRegistry) remoteRegistry).deactivate();
            }
        }
    }

    protected void deactivateRegistryRemotes() throws CouldNotPerformException, InterruptedException {
        for (final RegistryRemote remote : registryRemoteList) {
            remote.deactivate();
        }
    }

    private void removeDependencies() throws CouldNotPerformException {
        registryList.stream().forEach((registry) -> {
            registry.removeAllDependencies();
        });
    }

    private void registerObserver() throws CouldNotPerformException {
        registryList.stream().forEach((registry) -> {
            registry.addObserver((Observable source, Object data) -> {
                notifyChange();
            });
        });
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
                logger.debug("Trigger inital consistency check of " + registry + " with " + registry.getEntries().size() + " entries.");
                registry.checkConsistency();
            } catch (CouldNotPerformException ex) {
                ExceptionPrinter.printHistory(new CouldNotPerformException("Initial consistency check failed!", ex), logger);
                notifyChange();
            }
        }
    }

    protected void registerDependency(Registry dependency, Class messageClass) throws CouldNotPerformException {
        for (ProtoBufFileSynchronizedRegistry registry : registryList) {
            registry.registerDependency(dependency);
        }
    }

    protected void registerRegistryRemote(final RegistryRemote registry) {
        registryRemoteList.add(registry);
    }

    protected void registerRegistry(final ProtoBufFileSynchronizedRegistry registry) {
        registryList.add(registry);
    }

    protected void registerRemoteRegistry(final RemoteRegistry registry) {
        remoteRegistryList.add(registry);
    }

    protected void registerConsistencyHandler(final ConsistencyHandler consistencyHandler, final Class messageClass) throws CouldNotPerformException {
        for (ProtoBufFileSynchronizedRegistry registry : registryList) {
            if (messageClass.equals(registry.getMessageClass())) {
                registry.registerConsistencyHandler(consistencyHandler);
            } else {
                logger.debug("Register of " + consistencyHandler + " skipped for " + registry + " because " + messageClass.getSimpleName() + " is not compatible.");
            }
        }
    }

    protected List<RegistryRemote> getRegistryRemotes() {
        return registryRemoteList;
    }

    public List<RemoteRegistry> getRemoteRegistries() {
        return remoteRegistryList;
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
                CHANGE_NOTIFIER.wait();
            }
        }
    }

    /**
     * {@inheritDoc}
     *
     * @return {@inheritDoc}
     */
    @Override
    public Boolean isReady() throws InterruptedException {
        for (ProtoBufFileSynchronizedRegistry registry : registryList) {
            if (!registry.isReady()) {
                return false;
            }
        }

        for (RegistryRemote registry : registryRemoteList) {
            if (!registry.isReady()) {
                return false;
            }
        }
        return true;
    }

    protected abstract void registerConsistencyHandler() throws CouldNotPerformException;

    protected abstract void registerPlugins() throws CouldNotPerformException, InterruptedException;

    protected abstract void registerRegistryRemotes() throws CouldNotPerformException;

    protected abstract void registerRegistries() throws CouldNotPerformException;

    protected abstract void registerDependencies() throws CouldNotPerformException;

    protected abstract void syncRegistryFlags() throws CouldNotPerformException, InterruptedException;

    protected abstract void registerRemoteRegistries() throws CouldNotPerformException;
}
