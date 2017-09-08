/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.openbase.bco.registry.lib.com;

/*-
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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.openbase.jul.exception.CouldNotPerformException;

/**
 *
 * @author pleminoq
 * @param <M>
 */
public class RegistrySynchronizationFuture<M extends GeneratedMessage> implements Future<M> {

    private final Future<M> internalFuture;
    private final AbstractRegistryRemote registryRemote;

    public RegistrySynchronizationFuture(final Future<M> internalFuture, final AbstractRegistryRemote registryRemote) {
        this.internalFuture = internalFuture;
        this.registryRemote = registryRemote;
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        return internalFuture.cancel(mayInterruptIfRunning);
    }

    @Override
    public boolean isCancelled() {
        return internalFuture.isCancelled();
    }

    @Override
    public boolean isDone() {
        return internalFuture.isDone();
    }

    @Override
    public M get() throws InterruptedException, ExecutionException {
        M result = internalFuture.get();
        try {
            registryRemote.waitUntilReady();
            registryRemote.requestData().get();
        } catch (CouldNotPerformException ex) {
            throw new ExecutionException("Waiting for registry remote data failed", ex);
        }
        return result;
    }

    @Override
    public M get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        long current = System.currentTimeMillis();
        M result = internalFuture.get(timeout, unit);
        long remainingTime = Math.max(1, unit.convert(System.currentTimeMillis() - current, TimeUnit.MILLISECONDS));
        try {
            current = System.currentTimeMillis();
            registryRemote.waitUntilReadyFuture().get(remainingTime, unit);
            remainingTime = Math.max(1, unit.convert(System.currentTimeMillis() - current, TimeUnit.MILLISECONDS));
            registryRemote.requestData().get(remainingTime, unit);
        } catch (CouldNotPerformException ex) {
            throw new ExecutionException("Waiting for registry remote data failed", ex);
        }
        return result;
    }

}
