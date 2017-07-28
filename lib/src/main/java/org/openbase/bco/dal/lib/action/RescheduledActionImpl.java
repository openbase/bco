package org.openbase.bco.dal.lib.action;

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
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.Future;
import org.openbase.bco.dal.lib.layer.unit.AbstractUnitController;
import org.openbase.bco.dal.lib.layer.unit.UnitAllocation;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.extension.rsb.scope.ScopeGenerator;
import org.openbase.jul.extension.rst.processing.ActionDescriptionProcessor;
import org.openbase.jul.schedule.GlobalCachedExecutorService;
import rst.domotic.action.ActionFutureType.ActionFuture;

/**
 *
 * @author <a href="mailto:thuxohl@techfak.uni-bielefeld.de">Tamino Huxohl</a>
 */
public class RescheduledActionImpl extends ActionImpl {

    public RescheduledActionImpl(AbstractUnitController unit) {
        super(unit);
    }

    @Override
    public Future<ActionFuture> execute() throws CouldNotPerformException {
        return GlobalCachedExecutorService.submit(new Callable<ActionFuture>() {
            @Override
            public ActionFuture call() throws Exception {
                UnitAllocation unitAllocation = RescheduledActionImpl.super.internalExecute();
                try {
                    unitAllocation.getTaskExecutor().getFuture().get();
                } catch (CancellationException ex) {
                    System.out.println("Canceled! Scope: " + ScopeGenerator.generateStringRep(unit.getScope()));
                    // do nothing because cancellation only occurs if resource state is one which is handled below
                }

                System.out.println("Executor Allocation State - " + unitAllocation.getTaskExecutor().getRemote().getCurrentState());
                switch (unitAllocation.getTaskExecutor().getRemote().getCurrentState()) {
                    case REJECTED:
                        // rejected because the resource is blocked by someone else
                        // TODO: is it possible to get information about the allocaiton that currently blocks this
                        Thread.sleep(1000);
                    case ABORTED:
                    case CANCELLED:
                        if (actionDescriptionBuilder.getExecutionTimePeriod() != 0 && actionDescriptionBuilder.getExecutionValidity().getMillisecondsSinceEpoch() > System.currentTimeMillis()) {
                            System.out.println("Inside boundaries");
                            ActionDescriptionProcessor.updateResourceAllocationId(actionDescriptionBuilder);
                            ActionDescriptionProcessor.updateResourceAllocationSlot(actionDescriptionBuilder);
                            return call();
                        }

                }
                System.out.println("Execution finished");
                if (unitAllocation.getTaskExecutor().getFuture().isCancelled()) {
                    return null;
                }
                return unitAllocation.getTaskExecutor().getFuture().get();
            }
        });
    }
}
