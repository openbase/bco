package org.openbase.bco.registry.lib.com;

/*
 * #%L
 * BCO Registry Lib
 * %%
 * Copyright (C) 2014 - 2019 openbase.org
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

import com.google.protobuf.Message;
import org.openbase.bco.authentication.lib.com.AbstractAuthenticatedRemoteClient;
import org.openbase.jps.core.JPService;
import org.openbase.jps.exception.JPServiceException;
import org.openbase.jul.communication.controller.jp.JPScope;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InitializationException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.communication.controller.RPCHelper;
import org.openbase.jul.schedule.GlobalCachedExecutorService;
import org.openbase.jul.storage.registry.RegistryRemote;
import org.openbase.jul.storage.registry.RemoteRegistry;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

/**
 * @param <M>
 *
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public abstract class AbstractRegistryRemote<M extends Message> extends AbstractAuthenticatedRemoteClient<M> implements RegistryRemote<M> {

    private final Class<? extends JPScope> jpScopePropery;
    private final List<RemoteRegistry> remoteRegistries;

    public AbstractRegistryRemote(final Class<? extends JPScope> jpScopePropery, final Class<M> dataClass) {
        super(dataClass);
        this.jpScopePropery = jpScopePropery;
        this.remoteRegistries = new ArrayList<>();
    }

    @Override
    public void init() throws InitializationException, InterruptedException {
        try {
            super.init(JPService.getProperty(jpScopePropery).getValue());
        } catch (JPServiceException | CouldNotPerformException ex) {
            throw new InitializationException(this, ex);
        }
    }

    @Override
    protected void postInit() throws InitializationException, InterruptedException {
        super.postInit();
        try {
            try {
                remoteRegistries.clear();
                registerRemoteRegistries();
            } catch (CouldNotPerformException ex) {
                throw new CouldNotPerformException("Could not activate version control for all internal registries!", ex);
            }
        } catch (CouldNotPerformException ex) {
            throw new InitializationException(this, ex);
        }
    }

    @Override
    public void activate() throws InterruptedException, CouldNotPerformException {
        for (final RemoteRegistry remoteRegistry : remoteRegistries) {
            if (remoteRegistry instanceof SynchronizedRemoteRegistry) {
                ((SynchronizedRemoteRegistry) remoteRegistry).activate();
            }
        }
        super.activate();
    }

    @Override
    public void deactivate() throws InterruptedException, CouldNotPerformException {
        super.deactivate();
        for (final RemoteRegistry remoteRegistry : remoteRegistries) {
            if (remoteRegistry instanceof SynchronizedRemoteRegistry) {
                ((SynchronizedRemoteRegistry) remoteRegistry).deactivate();
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void shutdown() {
        super.shutdown();
        for (RemoteRegistry remoteRegistry : remoteRegistries) {
            remoteRegistry.shutdown();
        }
    }

    /**
     * {@inheritDoc}
     *
     * @return {@inheritDoc}
     */
    @Override
    public Boolean isReady() {
        try {
            if (!isConnected()) {
                return false;
            }
            ping().get(1000, TimeUnit.MILLISECONDS);
            return RPCHelper.callRemoteMethod(this, Boolean.class).get(1000, TimeUnit.MILLISECONDS);
        } catch (ExecutionException | TimeoutException ex) {
            ExceptionPrinter.printHistory("Could not check if registry is ready!", ex, logger);
            return false;
        } catch (InterruptedException ex) {
            return false;
        }
    }

    /**
     * {@inheritDoc}
     *
     * @throws InterruptedException     {@inheritDoc}
     * @throws CouldNotPerformException {@inheritDoc}
     */
    @Override
    public void waitUntilReady() throws InterruptedException, CouldNotPerformException {
        try {
            waitForData();
            RPCHelper.callRemoteMethod(this, Void.class).get();
        } catch (final CouldNotPerformException | ExecutionException | CancellationException ex) {
            throw new CouldNotPerformException("Could not wait until " + this + " is ready!", ex);
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
     * This method registers this registry remote as a registry proxy for the registry controller where the given remote registry is based on.
     * This object than can be used for registry state checks and synchronization issues.
     * <p>
     * ATTENTION: the order here is important, if somebody registers an observer
     * on one of these remote registries and tries to get values from other remote registries
     * which are registered later than these are not synced yet
     *
     * @param registry a remote registry which must not be compatible with the Message type {@code M} declared for this registry remote.
     */
    protected void registerRemoteRegistry(final RemoteRegistry<?, ?, ?> registry) {
        remoteRegistries.add(registry);
        registry.setRegistryRemote(this);
    }

    protected abstract void registerRemoteRegistries() throws CouldNotPerformException;

    protected List<RemoteRegistry> getRemoteRegistries() {
        return remoteRegistries;
    }
}
