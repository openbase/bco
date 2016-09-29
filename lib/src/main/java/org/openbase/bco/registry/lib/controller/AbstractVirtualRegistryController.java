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

    @Override
    protected void syncDataTypeFlags() throws CouldNotPerformException, InterruptedException {
        // not needed for virtual registries.
    }
}
