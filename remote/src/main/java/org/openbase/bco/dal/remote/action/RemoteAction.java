package org.openbase.bco.dal.remote.action;

/*
 * #%L
 * BCO DAL Remote
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

import com.google.protobuf.Descriptors;
import com.google.protobuf.Message;
import org.openbase.bco.dal.lib.action.Action;
import org.openbase.bco.dal.lib.action.ActionDescriptionProcessor;
import org.openbase.bco.dal.lib.layer.service.Service;
import org.openbase.bco.dal.lib.layer.service.Services;
import org.openbase.bco.dal.lib.layer.unit.Unit;
import org.openbase.bco.dal.remote.layer.unit.Units;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.exception.InvalidStateException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.exception.printer.LogLevel;
import org.openbase.jul.extension.protobuf.processing.ProtoBufFieldProcessor;
import org.openbase.jul.pattern.ObservableImpl;
import org.openbase.jul.pattern.Observer;
import org.openbase.jul.schedule.FutureProcessor;
import org.openbase.jul.schedule.GlobalCachedExecutorService;
import org.openbase.jul.schedule.SyncObject;
import org.openbase.type.domotic.action.ActionDescriptionType.ActionDescription;
import org.openbase.type.domotic.action.ActionParameterType.ActionParameter;
import org.openbase.type.domotic.state.ActionStateType.ActionState;
import org.openbase.type.domotic.state.ActionStateType.ActionState.State;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.concurrent.*;

/**
 * * @author Divine <a href="mailto:DivineThreepwood@gmail.com">Divine</a>
 */
public class RemoteAction implements Action {

    private static final Logger LOGGER = LoggerFactory.getLogger(RemoteAction.class);
    private final SyncObject executionSync = new SyncObject("ExecutionSync");
    private final ActionParameter.Builder actionParameterBuilder;
    private ActionDescription actionDescription;
    private Unit<?> targetUnit;
    private Future<ActionDescription> futureObservationTask;
    private final ObservableImpl<RemoteAction, ActionDescription> actionDescriptionObservable;
    private final Observer unitObserver = (source, data) -> {
        // check if initial actionDescription is available
        if (RemoteAction.this.actionDescription == null) {
            return;
        }

        try {
            updateActionDescription((Collection<ActionDescription>) ProtoBufFieldProcessor.getRepeatedFieldList("action", (Message) data));
        } catch (NotAvailableException ex) {
            ExceptionPrinter.printHistory("Incoming DataType[" + data.getClass().getSimpleName() + "] does not provide an action list!", ex, LOGGER, LogLevel.WARN);
        }
    };

    public RemoteAction(final Future<ActionDescription> actionFuture) {
        this.actionParameterBuilder = null;
        this.actionDescriptionObservable = new ObservableImpl<>();
        this.initFutureObservationTask(actionFuture);
    }

    public RemoteAction(final Unit<?> executorUnit, final ActionParameter actionParameter) throws InstantiationException, InterruptedException {
        this.actionParameterBuilder = actionParameter.toBuilder();
        this.actionDescriptionObservable = new ObservableImpl<>(this);
        try {
            // setup initiator
            this.actionParameterBuilder.getActionInitiatorBuilder().setInitiatorId(executorUnit.getId());

            // prepare target unit
            this.targetUnit = Units.getUnit(actionParameter.getServiceStateDescription().getUnitId(), false);
        } catch (CouldNotPerformException ex) {
            throw new InstantiationException(this, ex);
        }
    }

    public Future<ActionDescription> execute(final ActionDescription causeActionDescription) {

        // check if action remote was instantiated via task future.
        if (actionParameterBuilder == null) {
            return FutureProcessor.canceledFuture(new NotAvailableException("ActionParameter"));
        }

        if (isRunning()) {
            return FutureProcessor.canceledFuture(new InvalidStateException("Action is still running and can not be executed twice!"));
        }

        synchronized (executionSync) {
            if (causeActionDescription == null) {
                actionParameterBuilder.clearCause();
            } else {
                actionParameterBuilder.setCause(causeActionDescription);
            }
            synchronized (executionSync) {
                try {
                    return initFutureObservationTask(targetUnit.applyAction(ActionDescriptionProcessor.generateActionDescriptionBuilder(actionParameterBuilder).build()));
                } catch (CouldNotPerformException ex) {
                    return FutureProcessor.canceledFuture(ex);
                }
            }
        }
    }

    @Override
    public Future<ActionDescription> execute() {
        return execute(null);
    }

    private Future<ActionDescription> initFutureObservationTask(final Future<ActionDescription> future) {
        futureObservationTask = GlobalCachedExecutorService.submit(() -> {
            try {
                final ActionDescription actionDescription = future.get();
                synchronized (executionSync) {
                    RemoteAction.this.actionDescription = actionDescription;

                    // configure target unit if needed. This is the case if this remote action was instantiated via a future object.
                    if (targetUnit == null) {
                        targetUnit = Units.getUnit(actionDescription.getServiceStateDescription().getUnitId(), false);
                    }

                    // register action update observation
                    targetUnit.addDataObserver(unitObserver);

                    executionSync.notifyAll();

                    // because future can already be outdated but the update not received because
                    // the action id was not yet available we need to trigger an manual update.
                    updateActionDescription(targetUnit.getActionList());
                }
                return actionDescription;
            } catch (InterruptedException ex) {
                throw ex;
            } catch (CancellationException ex) {
                // in case the action is canceled, this is done via the futureObservationTask which than causes this cancellation exception.
                // But in this case we need to cancel the initial future as well.
                future.cancel(true);
                throw new ExecutionException(ex);
            } catch (ExecutionException ex) {
                throw ExceptionPrinter.printHistoryAndReturnThrowable("Could not observe " + this + "!", ex, LOGGER);
            }
        });
        return futureObservationTask;
    }

    /**
     * {@inheritDoc }
     *
     * @return {@inheritDoc }
     */
    @Override
    public ActionDescription getActionDescription() throws NotAvailableException {
        if (actionDescription == null) {
            throw new NotAvailableException(this.getClass().getSimpleName(), "ActionDescription");
        }
        return actionDescription;
    }

    @Override
    public boolean isValid() {
        return (actionParameterBuilder != null || futureObservationTask != null) && Action.super.isValid();
    }

    @Override
    public boolean isRunning() {
        return isValid() && futureObservationTask != null && (!futureObservationTask.isDone() || Action.super.isRunning());
    }

    @Override
    public Future<ActionDescription> cancel() {
        try {
            synchronized (executionSync) {
                if (futureObservationTask == null) {
                    return FutureProcessor.canceledFuture(new InvalidStateException(this + " has never been executed!"));
                }

                if (!futureObservationTask.isDone()) {
                    futureObservationTask.cancel(true);
                }
                return targetUnit.cancelAction(getActionDescription());
            }
        } catch (CouldNotPerformException ex) {
            return FutureProcessor.canceledFuture(ex);
        }
    }

    private void updateActionDescription(final Collection<ActionDescription> actionDescriptions) {

        if (actionDescriptions == null) {
            LOGGER.warn("Update skipped because no action descriptions passed!");
            return;
        }

        // update action description and notify
        for (ActionDescription actionDescription : actionDescriptions) {
            if (actionDescription.getId().equals(RemoteAction.this.actionDescription.getId())) {
                synchronized (executionSync) {
                    RemoteAction.this.actionDescription = actionDescription;

                    // cleanup observation if action is done.
                    if (!isRunning()) {
                        targetUnit.removeDataObserver(unitObserver);
                    }

                    try {
                        actionDescriptionObservable.notifyObservers(this, actionDescription);
                    } catch (CouldNotPerformException ex) {
                        ExceptionPrinter.printHistory("Could not notify all observers!", ex, LOGGER);
                        return;
                    }
                    executionSync.notifyAll();
                }
                return;
            }
        }
    }

    @Override
    public void waitUntilDone() throws CouldNotPerformException, InterruptedException {
        waitForSubmission();
        synchronized (executionSync) {
            // wait until done
            while (actionDescription == null || isRunning() || !isDone()) {
                executionSync.wait();
            }
        }
    }

    public void waitForSubmission() throws CouldNotPerformException, InterruptedException {
        synchronized (executionSync) {
            if (futureObservationTask == null) {
                throw new InvalidStateException("Action was never executed!");
            }
        }

        try {
            futureObservationTask.get();
        } catch (ExecutionException ex) {
            throw new CouldNotPerformException("Could not wait for submission!", ex);
        }
    }

    /**
     * Wait until this action reaches a provided action state. It is only possible to wait for states which will
     * certainly be notified (see {@link #isNotifiedActionState(State)}).
     *
     * @param actionState the state on which is waited.
     *
     * @throws CouldNotPerformException if the action was not yet executed, the provided action state is not certainly notified
     *                                  or the provided state cannot be reached anymore.
     * @throws InterruptedException     if the thread was externally interrupted.
     */
    public void waitForActionState(final ActionState.State actionState) throws CouldNotPerformException, InterruptedException {
        if (!isNotifiedActionState(actionState)) {
            throw new CouldNotPerformException("Cannot wait for state[" + actionState + "] because it is not always notified");
        }
        waitForSubmission();
        synchronized (executionSync) {
            // wait until state is reached
            while (actionDescription == null || actionDescription.getActionState().getValue() != actionState) {
                // test if the state can still be reached
                if (actionDescription != null) {
                    switch (actionState) {
                        case FINISHED:
                        case CANCELED:
                        case REJECTED:
                            // finishing states differs so it cannot be reached anymore
                            throw new CouldNotPerformException("Stop waiting because state[" + actionState.name() + "] cannot be reached from state[" + actionDescription.getActionState().getValue().name() + "]");
                        case SCHEDULED:
                        case EXECUTING:
                            if (isDone()) {
                                throw new CouldNotPerformException("Stop waiting because state[" + actionState.name() + "] cannot be reached from state[" + actionDescription.getActionState().getValue().name() + "]");
                            }
                    }
                }
                executionSync.wait();
            }
        }
    }

    public void waitForExecution() throws CouldNotPerformException, InterruptedException {
        waitForSubmission();
        synchronized (executionSync) {
            // wait until state is reached
            while (!isStateExecuting()) {
                if (isDone()) {
                    throw new CouldNotPerformException("Action is done but state was never executed");
                }

                executionSync.wait();
            }
        }
    }

    private boolean isStateExecuting() throws CouldNotPerformException {
        Message serviceState = Services.invokeProviderServiceMethod(actionDescription.getServiceStateDescription().getServiceType(), targetUnit);
        Descriptors.FieldDescriptor descriptor = ProtoBufFieldProcessor.getFieldDescriptor(serviceState, Service.RESPONSIBLE_ACTION_FIELD_NAME);
        ActionDescription responsibleAction = (ActionDescription) serviceState.getField(descriptor);
        return actionDescription.getId().equals(responsibleAction.getId());
    }

    public void waitForSubmission(long timeout, final TimeUnit timeUnit) throws CouldNotPerformException, InterruptedException, TimeoutException {
        synchronized (executionSync) {
            if (futureObservationTask == null) {
                throw new InvalidStateException("Action was never executed!");
            }
        }

        try {
            futureObservationTask.get(timeout, timeUnit);
        } catch (ExecutionException | CancellationException ex) {
            throw new CouldNotPerformException("Could not wait for submission!", ex);
        }
    }

    public boolean isSubmissionDone() {
        return futureObservationTask != null && futureObservationTask.isDone();
    }

    public void addActionDescriptionObserver(final Observer<RemoteAction, ActionDescription> observer) {
        actionDescriptionObservable.addObserver(observer);
    }

    public void removeActionDescriptionObserver(final Observer<RemoteAction, ActionDescription> observer) {
        actionDescriptionObservable.removeObserver(observer);
    }

    /**
     * Method returns the related unit which will be affected by this action.
     *
     * @return the target unit.
     */
    public Unit<?> getTargetUnit() {
        return targetUnit;
    }

    /**
     * Generates a string representation of this action.
     *
     * @return a description of this unit.
     */
    @Override
    public String toString() {
        return Action.toString(this);
    }
}
