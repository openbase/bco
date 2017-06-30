package org.openbase.bco.dal.lib.layer.unit;

/*-
 * #%L
 * BCO DAL Library
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
import de.citec.csra.allocation.cli.ExecutableResource;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InstantiationException;
import rsb.RSBException;
import rst.communicationpatterns.ResourceAllocationType;
import rst.communicationpatterns.ResourceAllocationType.ResourceAllocation;
import rst.domotic.action.ActionFutureType.ActionFuture;

/**
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public class UnitAllocation {

    private ExecutableResource<ActionFuture> taskExecutor;

    public UnitAllocation(ExecutableResource<ActionFuture> taskExecutor) throws org.openbase.jul.exception.InstantiationException {
        try {
            this.taskExecutor = taskExecutor;
            try {
                taskExecutor.startup();
            } catch (RSBException ex) {
                throw new CouldNotPerformException("Could not schedule allocation!", ex);
            }
        } catch (CouldNotPerformException ex) {
            throw new InstantiationException(this, ex);
        }
    }

    public ExecutableResource<ActionFuture> getTaskExecutor() {
        return taskExecutor;
    }

    /**
     * Free this {@code UnitAllocation}.
     * If the {@code UnitAllocation} was never allocated or is already deallocated this method has no effect.
     *
     * @throws CouldNotPerformException if the shutdown of the {@code taskExecutor} fails.
     */
    public void free() throws CouldNotPerformException {
        try {
            if (taskExecutor != null) {
                taskExecutor.shutdown();
                taskExecutor = null;
            }
        } catch (RSBException ex) {
            throw new CouldNotPerformException("Could not free unit allocation!");
        }
    }

    /**
     *
     * @param timeout
     * @param timeUnit
     * @return
     * @throws InterruptedException
     * @throws ExecutionException
     * @throws TimeoutException
     * @throws CouldNotPerformException
     */
    public ResourceAllocation.State waitForExecution(final long timeout, final TimeUnit timeUnit) throws InterruptedException, ExecutionException, TimeoutException, CouldNotPerformException {
        try {
            if (taskExecutor != null) {
                taskExecutor.getFuture().get(timeout, timeUnit);
                return taskExecutor.getRemote().getCurrentState();
            }
        } catch (CancellationException ex) {
            return taskExecutor.getRemote().getCurrentState();
        }

        throw new CouldNotPerformException("Execution has not been allocated");
    }

    public ResourceAllocation.State waitForExecution() throws InterruptedException, ExecutionException, CouldNotPerformException {
        try {
            if (taskExecutor != null) {
                taskExecutor.getFuture().get();
                return taskExecutor.getRemote().getCurrentState();
            }
        } catch (CancellationException ex) {
            return taskExecutor.getRemote().getCurrentState();
        }

        throw new CouldNotPerformException("Execution has not been allocated");
    }

    public ResourceAllocationType.ResourceAllocation.State getState() {
        return taskExecutor.getRemote().getCurrentState();
    }
}
