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
import org.openbase.bco.registry.lib.com.SynchronizedRemoteRegistry;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.pattern.Observable;
import org.openbase.jul.pattern.Observer;
import org.openbase.jul.schedule.GlobalCachedExecutorService;
import org.openbase.jul.schedule.SyncObject;

/**
 * This future is used to synchronize the remote registry in a way that when you call
 * get you can be sure that the change you created has at one time been synchronized to the remote registry.
 *
 * @author pleminoq
 * @param <M>
 */
public abstract class AbstractRegistrySynchronizationFuture<M extends GeneratedMessage> implements Future<M> {

    final SyncObject CHECK_LOCK = new SyncObject("WaitForMessageLock");
    final SyncObject SYNCHRONISTION_LOCK = new SyncObject("SynchronisationLock");
    final Observer notifyChangeObserver = (Observer) (Observable source, Object data) -> {
        synchronized (CHECK_LOCK) {
            CHECK_LOCK.notifyAll();
        }
    };
    boolean synchronisationComplete = false;

    private final Future<M> internalFuture;
    private final Future synchronisationFuture;
    private final SynchronizedRemoteRegistry<String, M, ?> remoteRegistry;

    public AbstractRegistrySynchronizationFuture(final Future<M> internalFuture, final SynchronizedRemoteRegistry<String, M, ?> remoteRegistry) {
        this.internalFuture = internalFuture;
        this.remoteRegistry = remoteRegistry;

        // create a synchronisation task which makes sure that the change requested by
        // the internal future has at one time been synchronized to the remote registry
        synchronisationFuture = GlobalCachedExecutorService.submit(() -> {
            remoteRegistry.addObserver(notifyChangeObserver);
            try {
                M restult = internalFuture.get();
                waitForRemoteRegistrySynchronisation(restult);
                synchronized (SYNCHRONISTION_LOCK) {
                    synchronisationComplete = true;
                    SYNCHRONISTION_LOCK.notifyAll();
                }
            } catch (InterruptedException ex) {
                // restore interrput
                Thread.currentThread().interrupt();
            } catch (ExecutionException ex) {
                // can only happen if the internal future failed so do nothing
                // because errors on the internal future are received by calling
                // get on this future anyways
            } catch (CouldNotPerformException ex) {
                // can only happen if waitForRemoteRegistrySynchronisation failed
                // so throw the excepion so that it is cleared that this task failed
                throw ex;
            } finally {
                remoteRegistry.removeObserver(notifyChangeObserver);
                synchronized (SYNCHRONISTION_LOCK) {
                    SYNCHRONISTION_LOCK.notifyAll();
                }
            }
            return null;
        });
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        return internalFuture.cancel(mayInterruptIfRunning) && synchronisationFuture.cancel(mayInterruptIfRunning);
    }

    @Override
    public boolean isCancelled() {
        return internalFuture.isCancelled() && synchronisationFuture.isCancelled();
    }

    @Override
    public boolean isDone() {
        return internalFuture.isDone() && synchronisationFuture.isDone();
    }

    @Override
    public M get() throws InterruptedException, ExecutionException {
        M result = internalFuture.get();

        synchronized (SYNCHRONISTION_LOCK) {
            if (!synchronisationComplete && !synchronisationFuture.isDone()) {
                SYNCHRONISTION_LOCK.wait();
                if (!synchronisationComplete) {
                    // synchronisation future was canceled or failed but the internal future not...
                }
            } else {
                // synchronisation future was canceled or failed but the internal future not...
            }
        }

        return result;
    }

    @Override
    public M get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        M result = internalFuture.get(timeout, unit);

        synchronized (SYNCHRONISTION_LOCK) {
            if (!synchronisationComplete && !synchronisationFuture.isDone()) {
                SYNCHRONISTION_LOCK.wait(TimeUnit.MILLISECONDS.convert(timeout, unit));
                if (!synchronisationComplete && !synchronisationFuture.isDone()) {
                    throw new TimeoutException();
                } else if (!synchronisationComplete) {
                    // synchronisation future was canceled or failed but the internal future not...
                }
            } else {
                // synchronisation future was canceled or failed but the internal future not...
            }
        }

        return result;
    }

    public Future<M> getInternalFuture() {
        return internalFuture;
    }

    private void waitForRemoteRegistrySynchronisation(final M message) throws CouldNotPerformException {
        try {
            remoteRegistry.waitForData();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
        synchronized (CHECK_LOCK) {
            try {
                while (!check(message, remoteRegistry)) {
                    CHECK_LOCK.wait();
                }
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
        }
    }

    protected abstract boolean check(final M message, final SynchronizedRemoteRegistry<String, M, ?> remoteRegistry) throws CouldNotPerformException;
}
