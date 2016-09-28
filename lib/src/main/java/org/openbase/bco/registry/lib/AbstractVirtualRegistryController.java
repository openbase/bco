package org.openbase.bco.registry.lib;

import com.google.protobuf.GeneratedMessage;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.extension.rsb.scope.jp.JPScope;

/**
 *
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 * @param <M>
 * @param <MB>
 */
public abstract class AbstractVirtualRegistryController<M extends GeneratedMessage, MB extends M.Builder<MB>> extends AbstractRegistryController<M, MB>{

    public AbstractVirtualRegistryController(Class<? extends JPScope> jpScopePropery, MB builder) throws InstantiationException {
        super(jpScopePropery, builder);
    }
    
    @Override
    protected void activateVersionControl() throws CouldNotPerformException {
        // not needed for virtual registries.
    }

    @Override
    protected void loadRegistries() throws CouldNotPerformException {
        // not needed for virtual registries.
    }

    @Override
    protected void registerConsistencyHandler() throws CouldNotPerformException {
        // not needed for virtual registries.
    }

    @Override
    protected void registerObserver() throws CouldNotPerformException {
        // not needed for virtual registries.
    }

    @Override
    protected void registerDependencies() throws CouldNotPerformException {
        // not needed for virtual registries.
    }

    @Override
    protected void removeDependencies() throws CouldNotPerformException {
        // not needed for virtual registries.
    }

    @Override
    protected void performInitialConsistencyCheck() throws CouldNotPerformException, InterruptedException {
        // not needed for virtual registries.
    }
}
