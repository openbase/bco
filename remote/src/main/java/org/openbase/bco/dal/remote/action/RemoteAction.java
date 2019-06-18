package org.openbase.bco.dal.remote.action;

/*
 * #%L
 * BCO DAL Remote
 * %%
 * Copyright (C) 2014 - 2019 openbase.org
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
import org.openbase.bco.dal.lib.layer.unit.UnitRemote;
import org.openbase.bco.dal.remote.layer.unit.Units;
import org.openbase.jul.exception.*;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.exception.printer.LogLevel;
import org.openbase.jul.extension.protobuf.processing.ProtoBufFieldProcessor;
import org.openbase.jul.pattern.ObservableImpl;
import org.openbase.jul.pattern.Observer;
import org.openbase.jul.schedule.FutureProcessor;
import org.openbase.jul.schedule.GlobalCachedExecutorService;
import org.openbase.jul.schedule.GlobalScheduledExecutorService;
import org.openbase.jul.schedule.SyncObject;
import org.openbase.type.domotic.action.ActionDescriptionType.ActionDescription;
import org.openbase.type.domotic.action.ActionParameterType.ActionParameter;
import org.openbase.type.domotic.action.ActionReferenceType.ActionReference;
import org.openbase.type.domotic.authentication.AuthTokenType.AuthToken;
import org.openbase.type.domotic.service.ServiceTempusTypeType.ServiceTempusType.ServiceTempus;
import org.openbase.type.domotic.state.ActionStateType.ActionState;
import org.openbase.type.domotic.state.ActionStateType.ActionState.State;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.TimeoutException;

/**
 * * @author Divine <a href="mailto:DivineThreepwood@gmail.com">Divine</a>
 */
public class RemoteAction implements Action {

    public final static long AUTO_EXTENSION_INTERVAL = Action.MAX_EXECUTION_TIME_PERIOD / 2;

    private static final Logger LOGGER = LoggerFactory.getLogger(RemoteAction.class);
    private final SyncObject executionSync = new SyncObject("ExecutionSync");
    private final SyncObject extensionSync = new SyncObject("ExtensionSync");
    private final ActionParameter.Builder actionParameterBuilder;
    private ActionDescription actionDescription;
    private UnitRemote<?> targetUnit;
    private Future<ActionDescription> futureObservationTask;
    private ScheduledFuture<?> autoExtensionTask;
    private final ObservableImpl<RemoteAction, ActionDescription> actionDescriptionObservable;

    private final AuthToken authToken;

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
    private final List<RemoteAction> impactedRemoteActions = new ArrayList<>();

    private Callable<Boolean> autoExtendCheckCallback;


    public RemoteAction(final Future<ActionDescription> actionFuture) {
        this(actionFuture, null, null);
    }

    public RemoteAction(final ActionParameter actionParameter) throws InstantiationException, InterruptedException {
        this(null, actionParameter, null, null);
    }

    public RemoteAction(final Unit<?> executorUnit, final ActionParameter actionParameter) throws InstantiationException, InterruptedException {
        this(executorUnit, actionParameter, null, null);
    }

    public RemoteAction(final ActionDescription actionDescription) throws CouldNotPerformException, InterruptedException {
        this(actionDescription, null, null);
    }

    public RemoteAction(final ActionReference actionReference) {
        this(actionReference, null, null);
    }

    public RemoteAction(final Future<ActionDescription> actionFuture, Callable<Boolean> autoExtendCheckCallback) {
        this(actionFuture, null, autoExtendCheckCallback);
    }

    public RemoteAction(final Unit<?> executorUnit, final ActionParameter actionParameter, Callable<Boolean> autoExtendCheckCallback) throws InstantiationException, InterruptedException {
        this(executorUnit, actionParameter, null, autoExtendCheckCallback);
    }

    public RemoteAction(final ActionDescription actionDescription, Callable<Boolean> autoExtendCheckCallback) {
        this(ActionDescriptionProcessor.generateActionReference(actionDescription), null, autoExtendCheckCallback);
    }

    public RemoteAction(final ActionReference actionReference, Callable<Boolean> autoExtendCheckCallback) {
        this(actionReference, null, autoExtendCheckCallback);
    }

    public RemoteAction(final Future<ActionDescription> actionFuture, final AuthToken authToken) {
        this(actionFuture, authToken, null);
    }

    public RemoteAction(final Unit<?> executorUnit, final ActionParameter actionParameter, final AuthToken authToken) throws InstantiationException, InterruptedException {
        this(executorUnit, actionParameter, authToken, null);
    }

    public RemoteAction(final ActionDescription actionDescription, final AuthToken authToken) {
        this(ActionDescriptionProcessor.generateActionReference(actionDescription), authToken, null);
    }

    public RemoteAction(final ActionReference actionReference, final AuthToken authToken) {
        this(actionReference, authToken, null);
    }

    public RemoteAction(final ActionDescription actionDescription, final AuthToken authToken, Callable<Boolean> autoExtendCheckCallback) throws InstantiationException, InterruptedException {
        try {
            this.actionParameterBuilder = null;
            this.actionDescriptionObservable = new ObservableImpl<>();
            this.autoExtendCheckCallback = autoExtendCheckCallback;
            this.authToken = authToken;
            this.setActionDescriptionAndStartObservation(actionDescription);
        } catch (CouldNotPerformException ex) {
            throw new InstantiationException(this, ex);
        }
    }

    /**
     * Instantiates a remote action with an already executed action referred via an action future.
     *
     * @param actionFuture            the future referring the action.
     * @param authToken               the token used for authentication if for example the action is canceled or extended.
     * @param autoExtendCheckCallback
     */
    public RemoteAction(final Future<ActionDescription> actionFuture, final AuthToken authToken, Callable<Boolean> autoExtendCheckCallback) {
        this.actionParameterBuilder = null;
        this.actionDescriptionObservable = new ObservableImpl<>();
        this.autoExtendCheckCallback = autoExtendCheckCallback;
        this.authToken = authToken;
        this.setActionDescriptionAndStartObservation(actionFuture);
    }

    /**
     * Instantiates a remote action which can be used to execute the referred action.
     *
     * @param initiatingUnit          the unit which initiates the execution. This can be any unit like a user, scene or location.
     * @param actionParameter         the parameter to descripe the action to execute.
     * @param authToken               the token to used for the authorization.
     * @param autoExtendCheckCallback flag defines if the action should be auto extended. This is required to execute an action more than 15 min.
     *
     * @throws InstantiationException is thrown if same information are missing.
     * @throws InterruptedException   is thown if the thread was externally interrupted.
     */
    public RemoteAction(final Unit<?> initiatingUnit, final ActionParameter actionParameter, final AuthToken authToken, Callable<Boolean> autoExtendCheckCallback) throws InstantiationException, InterruptedException {
        try {
            this.actionParameterBuilder = actionParameter.toBuilder();
            this.autoExtendCheckCallback = autoExtendCheckCallback;

            // resolve auth token 1. via Argument 2. via action parameter.
            if (authToken != null) {
                this.authToken = authToken;
            } else if (actionParameter.hasAuthToken()) {
                this.authToken = actionParameter.getAuthToken();
            } else {
                this.authToken = null;
            }
            this.actionDescriptionObservable = new ObservableImpl<>(this);

            // setup initiator
            if (initiatingUnit != null) {
                this.actionParameterBuilder.getActionInitiatorBuilder().setInitiatorId(initiatingUnit.getId());
            }

            // prepare target unit
            this.targetUnit = Units.getUnit(actionParameter.getServiceStateDescription().getUnitId(), false);
        } catch (CouldNotPerformException ex) {
            throw new InstantiationException(this, ex);
        }
    }

    public RemoteAction(final ActionReference actionReference, final AuthToken authToken, Callable<Boolean> autoExtendCheckCallback) {
        this.actionParameterBuilder = null;
        this.actionDescriptionObservable = new ObservableImpl<>();
        this.autoExtendCheckCallback = autoExtendCheckCallback;
        this.authToken = authToken;
        this.setActionReferenceAndStartObservation(actionReference);
    }

    public Future<ActionDescription> execute(final ActionDescription causeActionDescription) {
        try {
            // check if action remote was instantiated via action parameter.
            if (actionParameterBuilder == null) {
                throw new NotAvailableException("ActionParameter");
            }

            if (isRunning()) {
                throw new InvalidStateException("Action is still running and can not be executed twice!");
            }

            synchronized (executionSync) {
                if (causeActionDescription == null) {
                    actionParameterBuilder.clearCause();
                } else {
                    actionParameterBuilder.setCause(causeActionDescription);
                }
                synchronized (executionSync) {
                    return setActionDescriptionAndStartObservation(targetUnit.applyAction(ActionDescriptionProcessor.generateActionDescriptionBuilder(actionParameterBuilder).build()));
                }
            }
        } catch (CouldNotPerformException ex) {
            return FutureProcessor.canceledFuture(new CouldNotPerformException("Could not execute " + this + "!", ex));
        }
    }

    /**
     * {@inheritDoc}
     *
     * @return {@inheritDoc}
     */
    @Override
    public Future<ActionDescription> execute() {
        return execute(null);
    }

    private Future<ActionDescription> setActionDescriptionAndStartObservation(final Future<ActionDescription> future) {
        futureObservationTask = GlobalCachedExecutorService.submit(() -> {
            try {
                return setActionDescriptionAndStartObservation(future.get());
            } catch (InterruptedException ex) {
                // this is useful to cancel actions through cancelling the execute task of a remote actions
                // it allows to easily handle remote actions through the provided future because remote actions pools do not allow to access them
                // it is used to cancel optional actions from scenes
                targetUnit.cancelAction(actionDescription, authToken);
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

    private ActionDescription setActionDescriptionAndStartObservation(final ActionDescription actionDescription) throws CouldNotPerformException, InterruptedException {

        if(actionDescription == null) {
            throw new NotAvailableException("ActionDescription");
        }

        if(!actionDescription.hasId()) {
            throw new InvalidStateException("Given action description seems not to refer to an already executed action!", new NotAvailableException("ActionDescription.id"));
        }

        synchronized (executionSync) {
            RemoteAction.this.actionDescription = actionDescription;

            // configure target unit if needed. This is the case if this remote action was instantiated via a future object.
            if (targetUnit == null) {
                targetUnit = Units.getUnit(actionDescription.getServiceStateDescription().getUnitId(), false);
            }

            if (actionDescription.getIntermediary()) {
                // observe impact actions and register callback if available to support action auto extension.
                for (ActionReference actionReference : actionDescription.getActionImpactList()) {
                    RemoteAction remoteAction = new RemoteAction(actionReference, authToken, autoExtendCheckCallback);
                    remoteAction.addActionDescriptionObserver((source, observable) -> actionDescriptionObservable.notifyObservers(source, observable));
                    impactedRemoteActions.add(remoteAction);
                }
            } else {
                setupActionObservation();
            }
        }
        return actionDescription;
    }

    private Future<ActionDescription> setActionReferenceAndStartObservation(final ActionReference actionReference) {
        futureObservationTask = GlobalCachedExecutorService.submit(() -> {
            synchronized (executionSync) {
                // The following note seems to be outdated, please verify. Since a scene can switch on a location a action reference can point to
                // an intermediate action.
                // Note: this action can never be intermediary because else it cannot be retrieved from an action reference
                // therefore, the difference between this initialization and the one above

                if (targetUnit == null) {
                    targetUnit = Units.getUnit(actionReference.getServiceStateDescription().getUnitId(), true);
                }

                // resolve action description via the action reference.
                for (final ActionDescription actionDescription : targetUnit.getActionList()) {
                    if (actionDescription.getId().equals(actionReference.getActionId())) {
                        RemoteAction.this.actionDescription = actionDescription;
                    }
                }
                return setActionDescriptionAndStartObservation(actionDescription);
            }
        });
        return futureObservationTask;
    }

    private void setupActionObservation() throws CouldNotPerformException {

        // register action update observation
        targetUnit.addDataObserver(ServiceTempus.UNKNOWN, unitObserver);

        synchronized (executionSync) {
            executionSync.notifyAll();
        }

        // because future can already be outdated but the update not received because
        // the action id was not yet available we need to trigger an manual update.
        updateActionDescription(targetUnit.getActionList());

        // setup auto extension if needed
        setupAutoExtension(getActionDescription());
    }

    private void setupAutoExtension(final ActionDescription actionDescription) {

        // check if auto extension was requested, otherwise return.
        if (autoExtendCheckCallback == null) {
            return;
        }

        // check if auto extension is required, otherwise return.
        if (TimeUnit.MICROSECONDS.toMillis(actionDescription.getExecutionTimePeriod()) <= Action.MAX_EXECUTION_TIME_PERIOD) {
            return;
        }

        try {
            assert autoExtensionTask == null;
            autoExtensionTask = GlobalScheduledExecutorService.scheduleAtFixedRate(() -> {
                try {
                    // check if extension is still valid otherwise cancel task.
                    if (!isValid() || !autoExtendCheckCallback.call()) {
                        autoExtensionTask.cancel(false);
                        return;
                    }

                    // request an extension of the action
                    extend().get();
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                } catch (Exception ex) {
                    ExceptionPrinter.printHistory("Could not auto extend " + RemoteAction.this.toString(), ex, LOGGER);
                }
            }, AUTO_EXTENSION_INTERVAL, AUTO_EXTENSION_INTERVAL, TimeUnit.MILLISECONDS);

            final Observer<RemoteAction, ActionDescription> observer = new Observer<RemoteAction, ActionDescription>() {
                @Override
                public void update(RemoteAction remote, ActionDescription description) {
                    if (!remote.isValid()) {
                        RemoteAction.this.removeActionDescriptionObserver(this);
                        cancelAutoExtension();
                    }
                }
            };
            addActionDescriptionObserver(observer);
        } catch (CouldNotPerformException ex) {
            ExceptionPrinter.printHistory("Could not auto extend " + RemoteAction.this.toString(), ex, LOGGER);
        }
    }

    /**
     * Method cancels the auto extension of this action if running.
     */
    public void cancelAutoExtension() {
        if (autoExtensionTask != null && !autoExtensionTask.isDone()) {
            autoExtensionTask.cancel(true);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @return {@inheritDoc}
     */
    @Override
    public ActionDescription getActionDescription() throws NotAvailableException {
        if (actionDescription == null) {
            throw new NotAvailableException(this.getClass().getSimpleName(), "ActionDescription");
        }
        return actionDescription;
    }

    /**
     * {@inheritDoc}
     *
     * @return {@inheritDoc}
     */
    @Override
    public boolean isValid() {
        return (actionParameterBuilder != null || futureObservationTask != null) && actionDescription != null && Action.super.isValid();
    }

    /**
     * {@inheritDoc}
     *
     * @return {@inheritDoc}
     */
    @Override
    public boolean isRunning() {
        if (isValid() && futureObservationTask != null) {
            if (!futureObservationTask.isDone()) {
                return true;
            }

            try {
                if (getActionDescription().getIntermediary()) {
                    for (final RemoteAction impactedRemoteAction : impactedRemoteActions) {
                        if (!impactedRemoteAction.isRunning()) {
                            return false;
                        }
                    }
                    return true;
                } else {
                    return Action.super.isRunning();
                }
            } catch (NotAvailableException ex) {
                return false;
            }
        }

        return false;
    }

    /**
     * {@inheritDoc}
     *
     * @return {@inheritDoc}
     */
    @Override
    public boolean isDone() {
        try {
            if (getActionDescription().getIntermediary()) {
                for (final RemoteAction impactedRemoteAction : impactedRemoteActions) {
                    if (!impactedRemoteAction.isDone()) {
                        return false;
                    }
                }
                return true;
            } else {
                return Action.super.isDone();
            }
        } catch (NotAvailableException ex) {
            return Action.super.isDone();
        }
    }

    /**
     * {@inheritDoc}
     *
     * @return {@inheritDoc}
     */
    @Override
    public Future<ActionDescription> cancel() {
        Future<ActionDescription> future = null;
        try {
            synchronized (executionSync) {
                // make sure an action is not auto extended during the cancellation process.
                cancelAutoExtension();

                if (futureObservationTask == null) {
                    future = FutureProcessor.canceledFuture(ActionDescription.class, new InvalidStateException(this + " has never been executed!"));
                    return future;
                }

                if (!futureObservationTask.isDone()) {
                    future = FutureProcessor.allOf(() -> targetUnit.cancelAction(getActionDescription(), authToken).get() ,futureObservationTask);
                } else {
                    if (getActionDescription().getIntermediary()) {
                        // cancel all impacts of this actions and return the current action description
                        future = FutureProcessor.allOf(impactedRemoteActions, input -> getActionDescription(), remoteAction -> {
                            remoteAction.waitForSubmission();
                            return remoteAction.cancel();
                        });
                    } else {
                        if(isDone()) {
                            // if already done then skip cancellation
                            future = FutureProcessor.completedFuture(getActionDescription());
                        } else {
                            // cancel the action on the controller
                            future = targetUnit.cancelAction(getActionDescription(), authToken);
                        }
                    }
                }
                return future;
            }
        } catch (CouldNotPerformException ex) {
            return FutureProcessor.canceledFuture(ex);
        } finally {
            // cleanup when cancellation is done
            if (future == null || future.isDone()) {
                cleanup();
            } else {
                // otherwise create cleanup task
                final Future<ActionDescription> passthroughFuture = future;
                return GlobalCachedExecutorService.submit(() -> {
                    try {
                        return passthroughFuture.get();
                    } catch (InterruptedException ex) {
                        throw ex;
                    } catch (ExecutionException ex) {
                        throw new CouldNotProcessException("Could not cancel " + RemoteAction.this.toString(), ex);
                    } finally {
                        // final cleanup remote
                        cleanup();
                    }
                });
            }
        }
    }

    private void cleanup() {

        // cleanup synchronisation and observation tasks
        actionDescriptionObservable.shutdown();

        // cancel the auto extension task
        cancelAutoExtension();

        if (targetUnit != null) {
            targetUnit.removeDataObserver(ServiceTempus.UNKNOWN, unitObserver);
        }

        // check if already done, otherwise force cancellation
        if (!isDone()) {
            final ActionDescription.Builder builder;
            if (actionDescription == null) {
                builder = ActionDescription.newBuilder();
            } else {
                builder = actionDescription.toBuilder();
            }
            // force termination
            builder.getActionStateBuilder().setValue(State.UNKNOWN);
            actionDescription = builder.build();
        }
    }


    private void updateActionDescription(final Collection<ActionDescription> actionDescriptions) {
        if (actionDescriptions == null) {
            LOGGER.warn("Update skipped because no action descriptions passed!");
            return;
        }

        if (RemoteAction.this.actionDescription == null) {
            LOGGER.warn("Update skipped because no action description is not available!");
            return;
        }

        // update action description and notify
        for (ActionDescription actionDescription : actionDescriptions) {

            if (actionDescription.getId().equals(RemoteAction.this.actionDescription.getId())) {

                synchronized (executionSync) {
                    final boolean actionExtended = RemoteAction.this.actionDescription.getLastExtensionTimestamp().getTime() < actionDescription.getLastExtensionTimestamp().getTime();
                    RemoteAction.this.actionDescription = actionDescription;

                    // cleanup observation if action is done.
                    if (isDone()) {
                        cleanup();
                    }

                    // notify about update
                    executionSync.notifyAll();

                    // inform about action extension
                    if (actionExtended) {
                        synchronized (extensionSync) {
                            extensionSync.notifyAll();
                        }
                    }
                }

                try {
                    actionDescriptionObservable.notifyObservers(this, actionDescription);
                } catch (CouldNotPerformException ex) {
                    ExceptionPrinter.printHistory("Could not notify all observers!", ex, LOGGER);
                    return;
                }
                return;
            }
        }

        // when action not listed and we are expired anyway, than we can cleanup
        if (isExpired()) {
            cleanup();
        }
    }

    /**
     * {@inheritDoc}
     *
     * @throws CouldNotPerformException {@inheritDoc}
     * @throws InterruptedException     {@inheritDoc}
     */
    @Override
    public void waitUntilDone() throws CouldNotPerformException, InterruptedException {
        waitForSubmission();

        try {
            if (getActionDescription().getIntermediary()) {
                for (final RemoteAction impactedRemoteAction : impactedRemoteActions) {
                    impactedRemoteAction.waitUntilDone();
                }
                return;
            }
        } catch (NotAvailableException ex) {
            // if the action description is not available, than we just continue and wait for it.
        }

        synchronized (executionSync) {
            // wait until done
            while (actionDescription == null || isRunning() || !isDone()) {
                executionSync.wait();
            }
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

        try {
            if (actionDescription.getIntermediary()) {
                for (final RemoteAction impactedRemoteAction : impactedRemoteActions) {
                    impactedRemoteAction.waitForActionState(actionState);
                }
                return;
            }

        } catch (NotAvailableException ex) {
            // if the action description is not available, than we just continue and wait for it.
        }

        synchronized (executionSync) {
            // wait until state is reached
            while (actionDescription == null || actionDescription.getActionState().getValue() != actionState) {
                // Waiting makes no sense if the action is done but the state is still not reached.
                if (actionDescription != null && isDone()) {
                    throw new CouldNotPerformException("Stop waiting because state[" + actionState.name() + "] cannot be reached from state[" + actionDescription.getActionState().getValue().name() + "]");
                }
                executionSync.wait();
            }
        }
    }

    /**
     * This method blocks until the action is executing.
     * Depending of your execution priority and emphasis category the execution can be delayed for a longer term.
     * <p>
     * Note: This can really take some time in case the execution target unit can not directly be allocated.
     *
     * @throws CouldNotPerformException is thrown in case is the action was canceled or rejected before the execution toke place.
     * @throws InterruptedException     is thrown in case the thread was externally interrupted.
     */
    public void waitForExecution() throws CouldNotPerformException, InterruptedException {
        waitForExecution(0L, TimeUnit.MILLISECONDS);
    }

    /**
     * This method blocks until the action is executing.
     * Depending of your execution priority and emphasis category the execution can be delayed for a longer term.
     * <p>
     * Note: This can really take some time in case the execution target unit can not directly be allocated.
     *
     * @param timeout  the maximal time to wait for the execution.
     * @param timeUnit the time unit of the timeout.
     *
     * @throws CouldNotPerformException is thrown in case is the action was canceled or rejected before the execution toke place.
     * @throws TimeoutException         is thrown in case the timeout is reached.
     * @throws InterruptedException     is thrown in case the thread was externally interrupted.
     */
    public void waitForExecution(long timeout, final TimeUnit timeUnit) throws CouldNotPerformException, InterruptedException {
        final long timestamp = System.currentTimeMillis();

        waitForSubmission();
        try {
            if (actionDescription.getIntermediary()) {
                for (final RemoteAction impactedRemoteAction : impactedRemoteActions) {
                    impactedRemoteAction.waitForExecution();
                }
                return;
            }
        } catch (NotAvailableException ex) {
            // if the action description is not available, than we just continue and wait for it.
        }

        // wait on this action
        synchronized (executionSync) {
            // wait until state is reached
            while (!isStateExecuting()) {
                if (isDone()) {
                    throw new CouldNotPerformException("Action is done [" + actionDescription + "] but state was never executed");
                }

                // handle waiting without a timeout.
                if (timeout == 0L) {
                    executionSync.wait(0L);
                    continue;
                }

                // handle waiting with timeout
                final long timeToWait = timeUnit.toMillis(timeout) - (System.currentTimeMillis() - timestamp);
                if (timeToWait < 0) {
                    throw new org.openbase.jul.exception.TimeoutException();
                }
                executionSync.wait(timeToWait);
            }
        }
    }

    private boolean isStateExecuting() throws CouldNotPerformException {
        final Message serviceState = Services.invokeProviderServiceMethod(actionDescription.getServiceStateDescription().getServiceType(), targetUnit);
        final Descriptors.FieldDescriptor descriptor = ProtoBufFieldProcessor.getFieldDescriptor(serviceState, Service.RESPONSIBLE_ACTION_FIELD_NAME);
        final ActionDescription responsibleAction = (ActionDescription) serviceState.getField(descriptor);
        return actionDescription.getId().equals(responsibleAction.getId());
    }

    /**
     * Method blocks until the next successful action extension or the timeout was reached.
     *
     * @param timeout  the maximal time to wait for the extension.
     * @param timeUnit the time unit of the timeout.
     *
     * @throws InterruptedException is thrown if the thread was externally interrupted.
     */
    public void waitForExtension(final long timeout, final TimeUnit timeUnit) throws InterruptedException {
        synchronized (extensionSync) {
            extensionSync.wait(timeUnit.toMillis(timeout));
        }
    }

    /**
     * Method blocks until the next successful action extension.
     *
     * @throws InterruptedException is thrown if the thread was externally interrupted.
     */
    public void waitForExtension() throws InterruptedException {
        synchronized (extensionSync) {
            extensionSync.wait();
        }
    }

    public void waitForSubmission() throws CouldNotPerformException, InterruptedException {
        synchronized (executionSync) {
            if (futureObservationTask == null && !getActionDescription().hasId()) {
                throw new InvalidStateException("Action was never executed!");
            }
        }

        try {
            if (futureObservationTask != null) {
                futureObservationTask.get();
            }

            if (getActionDescription().getIntermediary()) {
                for (final RemoteAction impactedRemoteAction : impactedRemoteActions) {
                    impactedRemoteAction.waitForSubmission();
                }
            }
        } catch (ExecutionException ex) {
            throw new CouldNotPerformException("Could not wait for submission!", ex);
        }
    }

    public void waitForSubmission(long timeout, final TimeUnit timeUnit) throws CouldNotPerformException, InterruptedException, TimeoutException {
        synchronized (executionSync) {
            if (futureObservationTask == null && !getActionDescription().hasId()) {
                throw new InvalidStateException("Action was never executed!");
            }
        }

        try {
            if (futureObservationTask != null) {
                futureObservationTask.get(timeout, timeUnit);
            }

            //TODO: split timeout
            if (getActionDescription().getIntermediary()) {
                for (final RemoteAction impactedRemoteAction : impactedRemoteActions) {
                    impactedRemoteAction.waitForSubmission(timeout, timeUnit);
                }
            }
        } catch (ExecutionException | CancellationException ex) {
            throw new CouldNotPerformException("Could not wait for submission!", ex);
        }
    }

    public boolean isSubmissionDone() {
        if (futureObservationTask != null && futureObservationTask.isDone()) {

            if (actionDescription == null) {
                return false;
            }

            if (actionDescription.getIntermediary()) {
                for (final RemoteAction impactedRemoteAction : impactedRemoteActions) {
                    if (!impactedRemoteAction.isSubmissionDone()) {
                        return false;
                    }
                }
            }
            return actionDescription.hasId();
        }
        return false;
    }

    /**
     * {@inheritDoc}
     *
     * @return {@inheritDoc}
     */
    @Override
    public Future<ActionDescription> extend() {
        try {
            // set extend flag and reapply
            return targetUnit.extendAction(getActionDescription(), authToken);
        } catch (NotAvailableException ex) {
            return FutureProcessor.canceledFuture(ex);
        }
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
    public UnitRemote<?> getTargetUnit() {
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

