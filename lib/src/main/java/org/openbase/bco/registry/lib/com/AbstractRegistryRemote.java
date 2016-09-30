package org.openbase.bco.registry.lib.com;

import com.google.protobuf.GeneratedMessage;
import java.util.ArrayList;
import java.util.List;
import org.openbase.jps.core.JPService;
import org.openbase.jps.exception.JPServiceException;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InitializationException;
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
        for (RemoteRegistry remoteRegistry : remoteRegistries) {
            if(remoteRegistry instanceof SynchronizedRemoteRegistry) {
                ((SynchronizedRemoteRegistry) remoteRegistry).activate();
            }
        }
    }

    @Override
    public void deactivate() throws InterruptedException, CouldNotPerformException {
        super.deactivate();
        for (RemoteRegistry remoteRegistry : remoteRegistries) {
            if(remoteRegistry instanceof SynchronizedRemoteRegistry) {
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
            for (RemoteRegistry remoteRegistry : remoteRegistries) {
                remoteRegistry.shutdown();
            }
        } finally {
            super.shutdown();
        }
    }

    protected void registerRemoteRegistry(final RemoteRegistry registry) {
        remoteRegistries.add(registry);
    }

    protected abstract void registerRemoteRegistries() throws CouldNotPerformException;

}
