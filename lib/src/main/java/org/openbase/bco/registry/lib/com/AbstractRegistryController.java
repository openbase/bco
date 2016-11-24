package org.openbase.bco.registry.lib.com;

/*
 * #%L
 * BCO Registry Lib
 * %%
 * Copyright (C) 2014 - 2016 openbase.org
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
import org.openbase.jul.storage.file.ProtoBufJSonFileProvider;
import org.openbase.jul.storage.registry.ConsistencyHandler;
import org.openbase.jul.storage.registry.ProtoBufFileSynchronizedRegistry;
import org.openbase.jul.storage.registry.Registry;
import org.openbase.jul.storage.registry.RegistryController;
import org.openbase.jul.storage.registry.RegistryRemote;
import org.openbase.jul.storage.registry.RegistrySynchronizer;
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

    protected ProtoBufJSonFileProvider protoBufJSonFileProvider = new ProtoBufJSonFileProvider();

    private final List<RegistryRemote> registryRemoteList;
    private final List<RemoteRegistry> remoteRegistrieList;
    private final List<ProtoBufFileSynchronizedRegistry> registrieList;
    private final Class<? extends JPScope> jpScopePropery;

    public AbstractRegistryController(final Class<? extends JPScope> jpScopePropery, MB builder) throws InstantiationException {
        super(builder);
        this.jpScopePropery = jpScopePropery;
        this.registryRemoteList = new ArrayList<>();
        this.registrieList = new ArrayList<>();
        this.remoteRegistrieList = new ArrayList<>();
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
                throw new CouldNotPerformException("Could not activate version control for all internal registries!", ex);
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
        remoteRegistrieList.stream().forEach((remoteRegistry) -> {
            remoteRegistry.shutdown();
        });
        registrieList.stream().forEach((registry) -> {
            registry.shutdown();
        });
    }

    @Override
    public void notifyChange() throws CouldNotPerformException, InterruptedException {
        syncRegistryFlags();
        super.notifyChange();
    }

    private void initRemoteRegistries() throws CouldNotPerformException, InterruptedException {
        for (final RegistryRemote remote : registryRemoteList) {
            remote.init();
        }
    }

    private void activateRemoteRegistries() throws CouldNotPerformException, InterruptedException {
        for (RemoteRegistry remoteRegistry : remoteRegistrieList) {
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
        for (RemoteRegistry remoteRegistry : remoteRegistrieList) {
            if (remoteRegistry instanceof SynchronizedRemoteRegistry) {
                ((SynchronizedRemoteRegistry) remoteRegistry).deactivate();
            }
        }
    }

    private void deactivateRegistryRemotes() throws CouldNotPerformException, InterruptedException {
        for (final RegistryRemote remote : registryRemoteList) {
            remote.deactivate();
        }
    }

    private void removeDependencies() throws CouldNotPerformException {
        registrieList.stream().forEach((registry) -> {
            registry.removeAllDependencies();
        });
    }

    private void registerObserver() throws CouldNotPerformException {
        registrieList.stream().forEach((registry) -> {
            registry.addObserver((Observable source, Object data) -> {
                notifyChange();
            });
        });
    }

    private void loadRegistries() throws CouldNotPerformException {
        for (final ProtoBufFileSynchronizedRegistry registry : registrieList) {
            registry.loadRegistry();
        }
    }

    private void activateVersionControl() throws CouldNotPerformException {
        Package versionConverterPackage;
        for (final ProtoBufFileSynchronizedRegistry registry : registrieList) {
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
        for (final ProtoBufFileSynchronizedRegistry registry : registrieList) {
            try {
                registry.checkConsistency();
            } catch (CouldNotPerformException ex) {
                ExceptionPrinter.printHistory(new CouldNotPerformException("Initial consistency check failed!", ex), logger);
                notifyChange();
            }
        }
    }

    protected void registerDependency(Registry dependency, Class messageClass) throws CouldNotPerformException {
        for (ProtoBufFileSynchronizedRegistry registry : registrieList) {
            registry.registerDependency(dependency);
        }
    }

    protected void registerRegistryRemote(final RegistryRemote registry) {
        registryRemoteList.add(registry);
    }

    protected void registerRegistry(final ProtoBufFileSynchronizedRegistry registry) {
        registrieList.add(registry);
    }

    protected void registerRemoteRegistry(final RemoteRegistry registry) {
        remoteRegistrieList.add(registry);
    }

    protected void registerConsistencyHandler(final ConsistencyHandler consistencyHandler, final Class messageClass) throws CouldNotPerformException {
        for (ProtoBufFileSynchronizedRegistry registry : registrieList) {
            if (messageClass.equals(registry.getMessageClass())) {
                registry.registerConsistencyHandler(consistencyHandler);
            }
        }
    }

    protected List<RegistryRemote> getRegistryRemotes() {
        return registryRemoteList;
    }

    public List<RemoteRegistry> getRemoteRegistries() {
        return remoteRegistrieList;
    }

    protected List<ProtoBufFileSynchronizedRegistry> getRegistries() {
        return registrieList;
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

    protected abstract void registerConsistencyHandler() throws CouldNotPerformException;

    protected abstract void registerPlugins() throws CouldNotPerformException, InterruptedException;

    protected abstract void registerRegistryRemotes() throws CouldNotPerformException;

    protected abstract void registerRegistries() throws CouldNotPerformException;

    protected abstract void registerDependencies() throws CouldNotPerformException;

    protected abstract void syncRegistryFlags() throws CouldNotPerformException, InterruptedException;

    protected abstract void registerRemoteRegistries() throws CouldNotPerformException;
}
