package org.openbase.bco.registry.lib;

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
import org.openbase.jps.exception.JPServiceException;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InitializationException;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.extension.rsb.com.RSBCommunicationService;
import org.openbase.jul.extension.rsb.scope.jp.JPScope;
import org.openbase.jul.pattern.Observable;
import org.openbase.jul.pattern.Observer;
import org.openbase.jul.storage.file.ProtoBufJSonFileProvider;
import org.openbase.jul.storage.registry.ConsistencyHandler;
import org.openbase.jul.storage.registry.ProtoBufFileSynchronizedRegistry;
import org.openbase.jul.storage.registry.Registry;
import org.openbase.jul.storage.registry.RegistryRemote;

/**
 *
 * @author divine
 * @param <M>
 * @param <MB>
 */
public abstract class AbstractRegistryController<M extends GeneratedMessage, MB extends M.Builder<MB>> extends RSBCommunicationService<M, MB> {

    protected ProtoBufJSonFileProvider protoBufJSonFileProvider = new ProtoBufJSonFileProvider();

    private final List<RegistryRemote> registryRemotes;
    private final List<ProtoBufFileSynchronizedRegistry> registries;
    private final Class<? extends JPScope> jpScopePropery;

    public AbstractRegistryController(final Class<? extends JPScope> jpScopePropery, MB builder) throws InstantiationException {
        super(builder);
        this.jpScopePropery = jpScopePropery;
        this.registryRemotes = new ArrayList<>();
        this.registries = new ArrayList<>();
        this.protoBufJSonFileProvider = new ProtoBufJSonFileProvider();
    }

    @Override
    protected void postInit() throws InitializationException, InterruptedException {
        super.postInit();
        try {
            try {
                registerRegistries();
            } catch (CouldNotPerformException ex) {
                throw new CouldNotPerformException("Could not register registries!", ex);
            }
            try {
                registerRegistryRemotes();
            } catch (CouldNotPerformException ex) {
                throw new CouldNotPerformException("Could register registry remotes!", ex);
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
                registerObserver();
            } catch (CouldNotPerformException ex) {
                throw new CouldNotPerformException("Could not register observer for all internal registries!", ex);
            }
        } catch (CouldNotPerformException ex) {
            throw new InitializationException(this, ex);
        }
    }

    public void init() throws InitializationException, InterruptedException {
        try {
            super.init(JPService.getProperty(jpScopePropery).getValue());
            registerRegistryRemotes();
            initRemoteRegistries();
        } catch (JPServiceException | CouldNotPerformException ex) {
            throw new InitializationException(this, ex);
        }
    }

    @Override
    public void activate() throws InterruptedException, CouldNotPerformException {
        try {
            super.activate();
            activateRemoteRegistries();
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
        removeDependencies();
    }

    @Override
    public void shutdown() {
        super.shutdown();
        registryRemotes.stream().forEach((remote) -> {
            remote.shutdown();
        });
    }

    @Override
    public void notifyChange() throws CouldNotPerformException, InterruptedException {
        syncDataTypeFlags();
        super.notifyChange();
    }

    private void initRemoteRegistries() throws CouldNotPerformException, InterruptedException {
        for (final RegistryRemote remote : registryRemotes) {
            remote.init();
        }
    }

    private void activateRemoteRegistries() throws CouldNotPerformException, InterruptedException {
        for (final RegistryRemote remote : registryRemotes) {
            remote.activate();
        }
        for (final RegistryRemote remote : registryRemotes) {
            remote.waitForData();
        }
    }

    private void deactivateRemoteRegistries() throws CouldNotPerformException, InterruptedException {
        for (final RegistryRemote remote : registryRemotes) {
            remote.deactivate();
        }
    }

    protected void registerRegistryRemote(final RegistryRemote registry) {
        registryRemotes.add(registry);
    }

    protected void registerRegistry(final ProtoBufFileSynchronizedRegistry registry) {
        registries.add(registry);
    }

    protected void registerConsistencyHandler(ConsistencyHandler consistencyHandler, Class messageClass) throws CouldNotPerformException {
        for (ProtoBufFileSynchronizedRegistry registry : registries) {
            if (messageClass.equals(registry.getMessageClass())) {
                registry.registerConsistencyHandler(consistencyHandler);
            }
        }
    }

    protected void registerDependency(Registry dependency, Class messageClass) throws CouldNotPerformException {
        registries.stream().filter((registry) -> (messageClass.equals(registry.getMessageClass()))).forEach((registry) -> {
            registry.registerDependency(dependency);
        });
    }

    protected void removeDependencies() throws CouldNotPerformException {
        registries.stream().forEach((registry) -> {
            registry.removeAllDependencies();
        });
    }

    protected void registerObserver() throws CouldNotPerformException {
        registries.stream().forEach((registry) -> {
            registry.addObserver(new Observer() {

                @Override
                public void update(Observable source, Object data) throws Exception {
                    notifyChange();
                }
            });
        });
    }

    protected abstract void activateVersionControl() throws CouldNotPerformException;

    protected abstract void loadRegistries() throws CouldNotPerformException;

    protected abstract void registerConsistencyHandler() throws CouldNotPerformException;

    protected abstract void registerRegistryRemotes() throws CouldNotPerformException;

    protected abstract void registerRegistries() throws CouldNotPerformException;

    protected abstract void registerDependencies() throws CouldNotPerformException;

    protected abstract void performInitialConsistencyCheck() throws CouldNotPerformException, InterruptedException;

    protected abstract void syncDataTypeFlags() throws CouldNotPerformException, InterruptedException;
}
