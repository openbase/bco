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
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.extension.protobuf.ClosableDataBuilder;
import org.openbase.jul.extension.rsb.scope.jp.JPScope;
import org.openbase.jul.pattern.Observable;
import org.openbase.jul.pattern.Observer;

/**
 *
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 * @param <M> The virtual registry message type.
 * @param <MB> The virtual registry message builder.
 * @param <RM> The message type of the real registry which is mirrored by this virtual registry.
 */
public abstract class AbstractVirtualRegistryController<M extends GeneratedMessage, MB extends M.Builder<MB>, RM> extends AbstractRegistryController<M, MB> {

    private final VirtualRegistrySynchronizer virtualRegistrySynchronizer;

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
    public AbstractVirtualRegistryController(Class<? extends JPScope> jpScopePropery, MB builder) throws InstantiationException {
        super(jpScopePropery, builder);
        this.virtualRegistrySynchronizer = new VirtualRegistrySynchronizer();
    }

    /**
     * Constructor creates a new RegistryController based on the given scope and publishing registry data of the given builder.
     *
     * @param jpScopePropery the scope which is used for registry communication and data publishing.
     * @param builder the builder to build the registry data message.
     * @param filterSparselyRegistryData if this flag is true the registry data is only published if non of the internal registries is busy.
     * @throws InstantiationException
     */
    public AbstractVirtualRegistryController(final Class<? extends JPScope> jpScopePropery, MB builder, final boolean filterSparselyRegistryData) throws InstantiationException {
        super(jpScopePropery, builder, filterSparselyRegistryData);
        this.virtualRegistrySynchronizer = new VirtualRegistrySynchronizer();
    }

    @Override
    protected void registerConsistencyHandler() throws CouldNotPerformException {
        // not needed for virtual registries.
    }

    @Override
    protected void registerDependencies() throws CouldNotPerformException {
        // not needed for virtual registries.
    }

    @Override
    protected void syncRegistryFlags() throws CouldNotPerformException, InterruptedException {
        // not needed for virtual registries.
    }

    @Override
    protected void registerRegistries() throws CouldNotPerformException {
        // not needed for virtual registries.
    }

    @Override
    protected void registerPlugins() throws CouldNotPerformException, InterruptedException {
        // not needed for virtual registries.
    }

    @Override
    protected void activateRemoteRegistries() throws CouldNotPerformException, InterruptedException {
        /* The order here is important!
         * If the VirtualRegistrySynchronizer would be registered first the data type would be notified while the remote registries
         * have not synced. This way getter for the VirtualRegistryController would not contain the updated values when called within an observer. */
        super.activateRemoteRegistries();
        getRegistryRemotes().forEach((registryRemote) -> {
            registryRemote.addDataObserver(virtualRegistrySynchronizer);
        });
    }

    @Override
    protected void deactivateRegistryRemotes() throws CouldNotPerformException, InterruptedException {
        getRegistryRemotes().forEach((registryRemote) -> {
            registryRemote.removeDataObserver(virtualRegistrySynchronizer);
        });
        super.deactivateRegistryRemotes();
    }

    protected abstract void syncVirtualRegistryFields(final MB virtualDataBuilder, final RM realData) throws CouldNotPerformException;

    class VirtualRegistrySynchronizer implements Observer<RM> {

        @Override
        public void update(Observable<RM> source, RM realData) throws Exception {
            try {
                try (ClosableDataBuilder<MB> dataBuilder = getDataBuilder(this)) {
                    syncVirtualRegistryFields(dataBuilder.getInternalBuilder(), realData);
                } catch (Exception ex) {
                    throw new CouldNotPerformException("Could not apply data change!", ex);
                }
            } catch (CouldNotPerformException ex) {
                ExceptionPrinter.printHistory("Could not sync virtual registry!", ex, logger);
            }
        }
    }
}
