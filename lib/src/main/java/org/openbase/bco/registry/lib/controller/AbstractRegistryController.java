package org.openbase.bco.registry.lib.controller;

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
import org.openbase.jul.storage.file.ProtoBufJSonFileProvider;
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
    private final Class<? extends JPScope> jpScopePropery;

    public AbstractRegistryController(final Class<? extends JPScope> jpScopePropery, MB builder) throws InstantiationException {
        super(builder);
        this.jpScopePropery = jpScopePropery;
        this.registryRemotes = new ArrayList<>();
        this.protoBufJSonFileProvider = new ProtoBufJSonFileProvider();
    }

    @Override
    protected void postInit() throws InitializationException, InterruptedException {
        super.postInit();
        try {
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
        removeDependencies();
    }

    @Override
    public void shutdown() {
        super.shutdown();
        for (final RegistryRemote remote : registryRemotes) {
            remote.shutdown();
        }
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

    protected abstract void activateVersionControl() throws CouldNotPerformException;

    protected abstract void loadRegistries() throws CouldNotPerformException;

    protected abstract void registerConsistencyHandler() throws CouldNotPerformException;
    
    protected abstract void registerRegistryRemotes() throws CouldNotPerformException;

    protected abstract void registerObserver() throws CouldNotPerformException;

    protected abstract void registerDependencies() throws CouldNotPerformException;

    protected abstract void removeDependencies() throws CouldNotPerformException;

    protected abstract void performInitialConsistencyCheck() throws CouldNotPerformException, InterruptedException;
    
    protected abstract void syncDataTypeFlags() throws CouldNotPerformException, InterruptedException;
}
