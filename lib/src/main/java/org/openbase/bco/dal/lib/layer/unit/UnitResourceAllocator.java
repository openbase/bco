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
import rsb.RSBException;
import rst.communicationpatterns.ResourceAllocationType.ResourceAllocation;
import rst.domotic.action.ActionDescriptionType.ActionDescription;

/**
 *
 * @author <a href="mailto:mpohling@cit-ec.uni-bielefeld.de">Divine Threepwood</a>
 */
public abstract class UnitResourceAllocator<T> {

    private final ResourceAllocation resourceAllocation;
    protected final ExecutableResource.Completion completionType;

    private ExecutableResource<T> executableResource;

    /**
     * Create a UnitResourceAllocator according to the given actionDescription.
     * The resourceAllocation and the executionTimePeriod inside the actionDescription
     * have to be initialized. The resourceAllocation is used for the allocation
     * and the executionTimePeriod is used for the completionType of the ExecutableResource.
     * Zero results in the completionType EXPIRE which will free the resource directly
     * after onAllocation finished. Everything else will result in RETAIN which will
     * block the resource until the slot defined in the ResourceAllocation expires.
     *
     * @param actionDescription
     */
    public UnitResourceAllocator(final ActionDescription actionDescription) {
        this.resourceAllocation = actionDescription.getResourceAllocation();
        if (actionDescription.getExecutionTimePeriod() == 0) {
            this.completionType = ExecutableResource.Completion.EXPIRE;
        } else {
            this.completionType = ExecutableResource.Completion.RETAIN;
        }
    }

    /**
     * Method is called when the resource is allocated.
     *
     * @return
     * @throws ExecutionException
     * @throws InterruptedException
     */
    public abstract T onAllocation() throws ExecutionException, InterruptedException;

    /**
     * Creates and starts an executableResource which will call onAllocation when the resource
     * is allocated.
     *
     * @throws CouldNotPerformException if starting the executableResource fails
     */
    public void allocate() throws CouldNotPerformException {
        executableResource = new ExecutableResource<T>(resourceAllocation, completionType) {

            @Override
            public T execute() throws ExecutionException, InterruptedException {
                return UnitResourceAllocator.this.onAllocation();
            }
        };
        try {
            executableResource.startup();
        } catch (RSBException ex) {
            throw new CouldNotPerformException("Could not allocate resource", ex);
        }
    }

    /**
     * Free the resource by calling shutdown on the executableResource and setting it to null.
     * If allocate has not been called then the executableResource is null and deallocation
     * will do nothing.
     *
     * @throws CouldNotPerformException if the shutdown of the executableResource fails
     */
    public void deallocate() throws CouldNotPerformException {
        try {
            if (executableResource != null) {
                executableResource.shutdown();
            }
            executableResource = null;
        } catch (RSBException ex) {
            throw new CouldNotPerformException("Could not deallocate resource", ex);
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
            if (executableResource != null) {
                executableResource.getFuture().get(timeout, timeUnit);
                return executableResource.getRemote().getCurrentState();
            }
        } catch (CancellationException ex) {
            return executableResource.getRemote().getCurrentState();
        }

        throw new CouldNotPerformException("Execution has not been allocated");
    }

    public ResourceAllocation.State waitForExecution() throws InterruptedException, ExecutionException, CouldNotPerformException {
        try {
            if (executableResource != null) {
                executableResource.getFuture().get();
                return executableResource.getRemote().getCurrentState();
            }
        } catch (CancellationException ex) {
            return executableResource.getRemote().getCurrentState();
        }

        throw new CouldNotPerformException("Execution has not been allocated");
    }

    public ResourceAllocation.State getState() {
        return executableResource.getRemote().getCurrentState();
    }
}
