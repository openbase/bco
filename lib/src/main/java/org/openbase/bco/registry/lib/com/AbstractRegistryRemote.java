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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.openbase.jps.core.JPService;
import org.openbase.jps.exception.JPServiceException;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InitializationException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.extension.rsb.com.RPCHelper;
import org.openbase.jul.extension.rsb.com.RSBRemoteService;
import org.openbase.jul.extension.rsb.scope.jp.JPScope;
import org.openbase.jul.storage.registry.RegistryRemote;
import org.openbase.jul.storage.registry.RemoteRegistry;

/**
 *
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 * @param <M>
 */
public abstract class AbstractRegistryRemote<M extends GeneratedMessage> extends RSBRemoteService<M> implements RegistryRemote<M> {

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
        super.activate();
        for (final RemoteRegistry remoteRegistry : remoteRegistries) {
            if (remoteRegistry instanceof SynchronizedRemoteRegistry) {
                ((SynchronizedRemoteRegistry) remoteRegistry).activate();
            }
        }
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
        try {
            remoteRegistries.stream().forEach((remoteRegistry) -> {
                remoteRegistry.shutdown();
            });
        } finally {
            super.shutdown();
        }
    }

    /**
     * {@inheritDoc}
     *
     * @return {@inheritDoc}
     * @throws InterruptedException {@inheritDoc}
     */
    @Override
    public Boolean isReady() throws InterruptedException {
        try {
            if (!isConnected()) {
                return false;
            }
            return RPCHelper.callRemoteMethod(this, Boolean.class).get(2000, TimeUnit.MILLISECONDS);
        } catch (CouldNotPerformException | ExecutionException | TimeoutException ex) {
            ExceptionPrinter.printHistory("Could not check if registry is ready!", ex, logger);
            return false;
        }
    }

    /**
     * {@inheritDoc}
     *
     * @throws InterruptedException {@inheritDoc}
     * @throws CouldNotPerformException {@inheritDoc}
     */
    @Override
    public void waitUntilReady() throws InterruptedException, CouldNotPerformException {
        try {
            RPCHelper.callRemoteMethod(this);
        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not wait unit registry is ready!", ex);
        }
    }

    protected void registerRemoteRegistry(final RemoteRegistry registry) {
        remoteRegistries.add(registry);
        registry.setRemote(this);
    }

    protected abstract void registerRemoteRegistries() throws CouldNotPerformException;

}
