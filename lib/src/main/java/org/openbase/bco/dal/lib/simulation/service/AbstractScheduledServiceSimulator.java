package org.openbase.bco.dal.lib.simulation.service;

/*-
 * #%L
 * BCO DAL Library
 * %%
 * Copyright (C) 2014 - 2020 openbase.org
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

import com.google.protobuf.Message;
import org.openbase.bco.dal.lib.action.ActionDescriptionProcessor;
import org.openbase.bco.dal.lib.jp.JPBenchmarkMode;
import org.openbase.bco.dal.lib.layer.unit.UnitController;
import org.openbase.jps.core.JPService;
import org.openbase.jps.exception.JPNotAvailableException;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.ExceptionProcessor;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.extension.type.processing.TimestampProcessor;
import org.openbase.jul.schedule.GlobalScheduledExecutorService;
import org.openbase.jul.schedule.SyncObject;
import org.openbase.type.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * @param <SERVICE_STATE> the type of the service states used for the simulation.
 *
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public abstract class AbstractScheduledServiceSimulator<SERVICE_STATE extends Message> extends AbstractServiceSimulator {

    private final static long DEFAULT_STARTUP_DELAY = 15000;
    public static final long DEFAULT_CHANGE_RATE = TimeUnit.MINUTES.toMillis(3);
    public static final long BENCHMARK_CHANGE_RATE = 1000;

    protected final static Random RANDOM = new Random(System.currentTimeMillis());

    private final Runnable simulationTask;
    private Future simulationTaskFuture;
    private final long changeRate;
    protected final UnitController unitController;

    // Debug code that prints how many simulations occur
    private static final long START = System.currentTimeMillis();
    private static int simulationCount = 0;
    private static final SyncObject SIM_COUNT_SYNC = new SyncObject("SimulationCountSync");
    private static final Logger DEBUG_LOGGER = LoggerFactory.getLogger(AbstractScheduledServiceSimulator.class);

    private static void increaseSimCount() {
        synchronized (SIM_COUNT_SYNC) {
            simulationCount = simulationCount + 1;
            if (simulationCount % 100 == 0) {
                long time = TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - START);
                DEBUG_LOGGER.warn("Simulated services [" + simulationCount + "] in [" + time + "]s which means [" + simulationCount / time + "] simulations/s!");
            }
        }
    }

    /**
     * Creates a new scheduled {@code serviceType} simulator to simulate the given {@code @unitController}.
     *
     * @param unitController the unit to simulate.
     * @param serviceType    the service type to simulate.
     */
    public AbstractScheduledServiceSimulator(final UnitController unitController, final ServiceType serviceType) {
        this(unitController, serviceType, (isBenchmarkDetected() ? BENCHMARK_CHANGE_RATE : DEFAULT_CHANGE_RATE));
    }

    /**
     * Creates a new scheduled {@code serviceType} simulator to simulate the given {@code @unitController}.
     *
     * @param unitController the unit to simulate.
     * @param serviceType    the service type to simulate.
     * @param changeRate     the simulation update speed in milliseconds.
     */
    public AbstractScheduledServiceSimulator(final UnitController unitController, final ServiceType serviceType, final long changeRate) {
        this.changeRate = (isBenchmarkDetected() ? BENCHMARK_CHANGE_RATE : changeRate);
        this.unitController = unitController;
        this.simulationTask = () -> {
            final SERVICE_STATE.Builder serviceStateBuilder;
            try {
                serviceStateBuilder = getNextServiceState().toBuilder();
            } catch (NotAvailableException ex) {
                LOGGER.warn("No further service states available for service {} of unit {}. Simulation task will be terminated.",
                        serviceType.name(), unitController);
                if (simulationTaskFuture != null) {
                    simulationTaskFuture.cancel(false);
                }
                return;
            }

            // apply random service manipulation
            try {
                // generate responsible action
                ActionDescriptionProcessor.generateAndSetResponsibleAction(serviceStateBuilder, serviceType, unitController, 3, TimeUnit.MINUTES);

                // randomly select one of the registered service states, update the service state timestamp and apply the state update on unit controller.
                unitController.applyDataUpdate(TimestampProcessor.updateTimestampWithCurrentTime(serviceStateBuilder), serviceType);
                increaseSimCount();
            } catch (CouldNotPerformException ex) {
                if (!ExceptionProcessor.isCausedBySystemShutdown(ex)) {
                    ExceptionPrinter.printHistory("Could not apply service modification!", ex, LOGGER);
                }
            }
        };
    }

    /**
     * Method submits all simulation tasks to the global executor service and returns the task future list.
     *
     * @return a future list of the executed tasks.
     */
    @Override
    public Collection<Future> executeSimulationTasks() {
        final List<Future> futureList = new ArrayList<>();
        try {
            simulationTaskFuture = GlobalScheduledExecutorService.scheduleAtFixedRate(simulationTask, RANDOM.nextInt((int) changeRate) + DEFAULT_STARTUP_DELAY, changeRate, TimeUnit.MILLISECONDS);
            futureList.add(simulationTaskFuture);
        } catch (IllegalArgumentException | RejectedExecutionException | NotAvailableException ex) {
            if (!ExceptionProcessor.isCausedBySystemShutdown(ex)) {
                ExceptionPrinter.printHistory("Could not execute random service simulation of " + unitController + "!", ex, LOGGER);
            }
        }
        return futureList;
    }

    private static boolean isBenchmarkDetected() {
        try {
            return JPService.getProperty(JPBenchmarkMode.class).getValue();
        } catch (JPNotAvailableException ex) {
            return false;
        }
    }

    /**
     * Method should return the next service state which is applied during next service state simulation.
     *
     * @return an appropriated value state.
     *
     * @throws org.openbase.jul.exception.NotAvailableException can be thrown in case no more service states are available. Be informed that in this case the simulation task will be terminated.
     */
    protected abstract SERVICE_STATE getNextServiceState() throws NotAvailableException;
}
