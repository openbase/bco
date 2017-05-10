package org.openbase.bco.dal.lib.simulation.unit;

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
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Future;
import org.openbase.bco.dal.lib.layer.unit.UnitController;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.iface.Activatable;
import org.slf4j.LoggerFactory;

/**
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 *
 * This class can be extended to implement any unit simulator.
 *
 * @param <UC> the type of unit controller which is used for the simulation.
 */
public abstract class AbstractUnitSimulator<UC extends UnitController> implements Activatable {

    protected final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(getClass());

    private boolean active = false;
    private final UC unitController;
    private final List<Future> runningTaskList;

    /**
     * Constructor creates a new unit simulator.
     *
     * @param unitController this controller must be linked to the unit which should be simulated.
     */
    public AbstractUnitSimulator(final UC unitController) {
        this.unitController = unitController;
        this.runningTaskList = new ArrayList<>();
    }

    /**
     * Method activates this unit simulator.
     *
     * @throws CouldNotPerformException is thrown in case the simulator could not be started.
     * @throws InterruptedException is thrown if the current thread is externally interrupted.
     */
    @Override
    public void activate() throws CouldNotPerformException, InterruptedException {
        active = true;
        runningTaskList.addAll(executeSimulationTasks());
    }

    /**
     * Method deactivates this simulator by canceling all running simulation tasks.
     *
     * @throws CouldNotPerformException
     * @throws InterruptedException is thrown if the current thread is externally interrupted.
     */
    @Override
    public void deactivate() throws CouldNotPerformException, InterruptedException {
        active = false;

        // stop all running tasks
        runningTaskList.forEach((final Future taskFuture) -> {
            if (!taskFuture.isDone()) {
                taskFuture.cancel(true);
            }
        });
        runningTaskList.clear();
    }

    /**
     * Method returns true if this simulator is currently active, otherwise false.
     *
     * @return true if active.
     */
    @Override
    public boolean isActive() {
        return active;
    }

    /**
     * Method returns the unit controller to apply any service state modification.
     *
     * @return the unit controller representing the unit to simulate.
     */
    public UC getUnitController() {
        return unitController;
    }

    /**
     * Method prints an simulator description.
     *
     * @return an description as string.
     */
    @Override
    public String toString() {
        return getClass().getSimpleName() + "[" + unitController != null ? unitController.toString() : "?" + "]";
    }

    /**
     * Method should return a list of futures of unit task which are used for the unit simulator.
     * Please make sure the tasks are already running and assigned to any global executor service.
     *
     * Note: In case this simulator is deactivated all running tasks are canceled via the given future list.
     *
     * @return a collection of task futures.
     */
    protected abstract Collection<Future> executeSimulationTasks();
}
