package org.openbase.bco.dal.remote;

/*-
 * #%L
 * BCO DAL Remote
 * %%
 * Copyright (C) 2014 - 2017 openbase.org
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-3.0.html>.
 * #L%
 */

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 *
 * @author <a href="mailto:pLeminoq@openbase.org">Tamino Huxohl</a>
 */
public class VoidFuture implements Future<Void> {
    
    private final Future internalFuture;

    public VoidFuture(Future internalFuture) {
        this.internalFuture = internalFuture;
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
    public Void get() throws InterruptedException, ExecutionException {
        internalFuture.get();
        return null;
    }

    @Override
    public Void get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        internalFuture.get(timeout, unit);
        return null;
    }
}
