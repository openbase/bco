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
import com.google.protobuf.GeneratedMessage;
import java.util.concurrent.Future;
import org.openbase.bco.registry.lib.com.SynchronizedRemoteRegistry;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.storage.registry.RegistryRemote;

/**
 *
 * @author pleminoq
 * @param <M>
 */
public class RemovalFuture<M extends GeneratedMessage> extends AbstractRegistrySynchronizationFuture<M> {

    public RemovalFuture(final Future<M> internalFuture, final SynchronizedRemoteRegistry<String, M, ?> remoteRegistry, final RegistryRemote registryRemote) {
        super(internalFuture, remoteRegistry, registryRemote);
        init();
    }

    @Override
    protected boolean check(M message, final SynchronizedRemoteRegistry<String, M, ?> remoteRegistry) throws CouldNotPerformException {
        return !remoteRegistry.contains(message);
    }
}
