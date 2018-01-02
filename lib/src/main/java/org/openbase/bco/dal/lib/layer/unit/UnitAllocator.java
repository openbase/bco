package org.openbase.bco.dal.lib.layer.unit;

/*-
 * #%L
 * BCO DAL Library
 * %%
 * Copyright (C) 2014 - 2018 openbase.org
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
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import org.openbase.jul.exception.CouldNotPerformException;
import rst.domotic.action.ActionDescriptionType.ActionDescription;
import rst.domotic.action.ActionFutureType.ActionFuture;

/**
 *
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public class UnitAllocator {

    /**
     * Create a UnitAllocation according to the given actionDescriptionBuilder.
     *
     * The resourceAllocation and the executionTimePeriod inside the actionDescription have to be initialized. The executionTimePeriod is used for the completionType of the internal ExecutableResource.
     * Zero results in the completionType EXPIRE which will free the resource directly after onAllocation finished. Everything else will result in RETAIN which will
     * block the resource until the slot defined in the ResourceAllocation expires.
     *
     * @param actionDescriptionBuilder
     * @param task
     * @return
     * @throws CouldNotPerformException if starting the executableResource fails
     */
    public static UnitAllocation allocate(final ActionDescription.Builder actionDescriptionBuilder, final Callable<ActionFuture> task) throws CouldNotPerformException {
        try {

            // detect completion type
            final ExecutableResource.Completion completionType;
            if (actionDescriptionBuilder.getExecutionTimePeriod() == 0) {
                completionType = ExecutableResource.Completion.EXPIRE;
            } else {
                completionType = ExecutableResource.Completion.RETAIN;
            }

            // allocate
            return new UnitAllocation(new ExecutableResource(actionDescriptionBuilder.getResourceAllocation(), completionType) {

                @Override
                public ActionFuture execute() throws ExecutionException, InterruptedException {
                    try {
                        return task.call();
                    } catch (final InterruptedException ex) {
                        throw ex;
                    } catch (final Exception ex) {
                        throw new ExecutionException(ex);
                    }
                }
            });
        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not allocate unit!", ex);
        }
    }

    /**
     * Free the {@code UnitAllocation}.
     * If the {@code UnitAllocation} was never allocated this method has no effect.
     *
     * @param unitAllocation
     * @throws CouldNotPerformException if the shutdown of the executableResource fails
     */
    public static void deallocate(final UnitAllocation unitAllocation) throws CouldNotPerformException {
        try {
            unitAllocation.free();
        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not deallocate resource", ex);
        }
    }
}
