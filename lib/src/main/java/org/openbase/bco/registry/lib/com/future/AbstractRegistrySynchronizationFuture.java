/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.openbase.bco.registry.lib.com.future;

/*-
 * #%L
 * BCO Registry Lib
 * %%
 * Copyright (C) 2014 - 2018 openbase.org
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
import org.openbase.jul.schedule.AbstractSynchronizationFuture;
import com.google.protobuf.GeneratedMessage;

import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import org.openbase.bco.registry.lib.com.SynchronizedRemoteRegistry;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.pattern.Observer;
import org.openbase.jul.storage.registry.RegistryRemote;

/**
 * This future is used to synchronize the remote registry in a way that when you call
 * get you can be sure that the change you created has at one time been synchronized to the remote registry.
 *
 * @author pleminoq
 * @param <M>
 */
public abstract class AbstractRegistrySynchronizationFuture<M extends GeneratedMessage> extends AbstractSynchronizationFuture<M, RegistryRemote<?>> {

    private final SynchronizedRemoteRegistry<String, M, ?> remoteRegistry;

    public AbstractRegistrySynchronizationFuture(final Future<M> internalFuture, final SynchronizedRemoteRegistry<String, M, ?> remoteRegistry, final RegistryRemote<?> registryRemote) {
        super(internalFuture, registryRemote);
        this.remoteRegistry = remoteRegistry;
    }

    @Override
    protected boolean check(M message) throws CouldNotPerformException {
        return check(message, remoteRegistry);
    }

    protected abstract boolean check(final M message, final SynchronizedRemoteRegistry<String, M, ?> remoteRegistry) throws CouldNotPerformException;
}
