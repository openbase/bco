package org.openbase.bco.dal.lib.simulation.service;

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
import com.google.protobuf.GeneratedMessage;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import org.openbase.bco.dal.lib.layer.unit.UnitController;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.extension.rst.processing.TimestampProcessor;
import org.openbase.jul.schedule.GlobalScheduledExecutorService;
import rst.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;

/**
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 *
 * @param <SERVICE_STATE> the type of the service states used for the simulation.
 */
public abstract class AbstractScheduledServiceSimulator<SERVICE_STATE extends GeneratedMessage> extends AbstractServiceSimulator {

    private final static long DEFAULT_STARTUP_DELAY = 0;
    public static final long DEFAULT_CHANGE_RATE = 30000;

    protected final static Random RANDOM = new Random(System.currentTimeMillis());

    private final Runnable simulationTask;
    private Future simulationTaskFuture;
    private final long changeRate;
    private final UnitController unitController;

    /**
     * Creates a new scheduled {@code serviceType} simulator to simulate the given {@code @unitController}.
     *
     * @param unitController the unit to simulate.
     * @param serviceType the service type to simulate.
     */
    public AbstractScheduledServiceSimulator(final UnitController unitController, final ServiceType serviceType) {
        this(unitController, serviceType, DEFAULT_CHANGE_RATE);
    }

    /**
     * Creates a new scheduled {@code serviceType} simulator to simulate the given {@code @unitController}.
     *
     * @param unitController the unit to simulate.
     * @param serviceType the service type to simulate.
     * @param changeRate the simulation update speed in milliseconds.
     */
    public AbstractScheduledServiceSimulator(final UnitController unitController, final ServiceType serviceType, final long changeRate) {
        this.changeRate = changeRate;
        this.unitController = unitController;
        this.simulationTask = () -> {

            final SERVICE_STATE serviceState;
            try {
                serviceState = getNextServiceState();
            } catch (NotAvailableException ex) {
                LOGGER.warn("No more further service states are available. Simulation task will be terminated.");
                if (simulationTaskFuture != null) {
                    simulationTaskFuture.cancel(true);
                }
                return;
            }

            // apply random service manipulation
            try {
                // randomly select one of the registered service states, update the service state timestamp and apply the state update on unit controller.
                unitController.applyDataUpdate(serviceType, TimestampProcessor.updateTimestampWithCurrentTime(serviceState));
            } catch (CouldNotPerformException ex) {
                ExceptionPrinter.printHistory("Could not apply service modification!", ex, LOGGER);
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
            ExceptionPrinter.printHistory("Coult not execute random service simulation of " + unitController + "!", ex, LOGGER);
        }
        return futureList;
    }

    /**
     * Method should return the next service state which is applied during next service state simulation.
     *
     * @return an appropriated value state.
     * @throws org.openbase.jul.exception.NotAvailableException can be thrown in case no more service states are available. Be informed that in this case the simulation task will be terminated.
     */
    protected abstract SERVICE_STATE getNextServiceState() throws NotAvailableException;
}
