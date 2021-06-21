package org.openbase.bco.dal.task;

/*-
 * #%L
 * BCO DAL Task
 * %%
 * Copyright (C) 2014 - 2021 openbase.org
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
import de.citec.csra.task.TaskProxy;
import de.citec.csra.task.srv.*;

import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import org.openbase.bco.dal.remote.layer.unit.Units;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InitializationException;
import org.openbase.jul.exception.InvalidStateException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.extension.rsb.com.exception.RSBResolvedException;
import org.openbase.jul.schedule.GlobalCachedExecutorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
//import org.openbase.type.communicationpatterns.TaskStateType.TaskState;
import org.openbase.type.domotic.action.ActionDescriptionType.ActionDescription;
import rsb.Event;
import rsb.Informer;
import rsb.RSBException;
import rst.communicationpatterns.TaskStateType.TaskState;

/**
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public class BCOTaskServerImpl implements BCOTaskServer {

    protected static final Logger LOGGER = LoggerFactory.getLogger(BCOTaskServerController.class);
    public static final String TASK_HANDLER_SCOPE = "/bco/task";

    private final BCOTaskFactory taskFactory;
    private final TaskServer taskServer;
    private Future listenerTask;

    public BCOTaskServerImpl() throws org.openbase.jul.exception.InstantiationException, InterruptedException {
        try {
            this.taskFactory = new BCOTaskFactory();
            this.taskServer = new TaskServer(TASK_HANDLER_SCOPE, new BCOTaskHandler());
        } catch (RSBException ex) {
            throw new org.openbase.jul.exception.InstantiationException(this, new RSBResolvedException(ex));
        }
    }

    @Override
    public void init() throws InitializationException {
//        try {
//
//        } catch (CouldNotPerformException ex) {
//            throw new InitializationException(this, ex);
//        }
    }

    @Override
    public synchronized void activate() throws CouldNotPerformException {
        try {
            taskServer.activate();
            listenerTask = GlobalCachedExecutorService.submit(new Callable<Void>() {
                @Override
                public Void call() throws Exception {
                    taskServer.listen();
                    return null;
                }
            });
        } catch (RSBException ex) {
            throw new CouldNotPerformException("Could not activate " + this, new RSBResolvedException(ex));
        }
    }

    @Override
    public boolean isActive() {
        return listenerTask != null;
    }

    @Override
    public void deactivate() throws CouldNotPerformException, InterruptedException {
        try {
            if (isActive()) {
                listenerTask.cancel(true);
                listenerTask = null;
                taskServer.deactivate();
            }
        } catch (NullPointerException ex) {
            throw new CouldNotPerformException("Could not deactivate " + this, ex);
        } catch (RSBException ex) {
            throw new CouldNotPerformException("Could not deactivate " + this, new RSBResolvedException(ex));
        }
    }

    @Override
    public void shutdown() {
        try {
            deactivate();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        } catch (CouldNotPerformException ex) {
            ExceptionPrinter.printHistory("Could not shutdown " + this, ex, LOGGER);
        }
    }

    @Override
    public String toString() {
        return BCOTaskServer.class.getSimpleName() + "[" + TASK_HANDLER_SCOPE + "]";
    }

    public class BCOTaskHandler implements TaskHandler {

        @Override
        public void handle(final TaskState taskState, final Event event, final Informer informer) throws Exception {
            if (!isActive()) {
                throw new InvalidStateException(BCOTaskServerImpl.this + " is not active!");
            }

            TaskProxy proxy = new TaskProxy(taskState, event, informer);
            TaskExecutionMonitor monitor = new TaskExecutionMonitor(proxy, taskFactory);
            GlobalCachedExecutorService.submit(monitor);
        }
    }

    public class BCOTaskFactory implements LocalTaskFactory {

        @Override
        public LocalTask newLocalTask(Object taskDescription) throws IllegalArgumentException {

            // read action config
            if (!(taskDescription instanceof ActionDescription)) {
                throw new IllegalArgumentException("Unknown DataType[" + taskDescription.getClass() + "]!");
            }

            final ActionDescription actionDescription = (ActionDescription) taskDescription;

            return () -> {
                try {
                    Units.getUnit(actionDescription.getServiceStateDescription().getUnitId(), true).applyAction(actionDescription);
                } catch (CouldNotPerformException ex) {
                    throw ExceptionPrinter.printHistoryAndReturnThrowable(new CouldNotPerformException("Could not execute task!", ex), LOGGER);
                }
                return null;
            };
        }
    }
}
