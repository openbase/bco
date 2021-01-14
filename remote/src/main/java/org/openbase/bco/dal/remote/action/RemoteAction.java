package org.openbase.bco.dal.remote.action;

/*
 * #%L
 * BCO DAL Remote
 * %%
 * Copyright (C) 2014 - 2021 openbase.org
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
import org.openbase.bco.dal.lib.action.Action;
import org.openbase.bco.dal.lib.action.ActionDescriptionProcessor;
import org.openbase.bco.dal.lib.layer.service.ServiceStateProcessor;
import org.openbase.bco.dal.lib.layer.unit.Unit;
import org.openbase.bco.dal.lib.layer.unit.UnitRemote;
import org.openbase.bco.dal.remote.layer.unit.Units;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.exception.TimeoutException;
import org.openbase.jul.exception.*;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.exception.printer.LogLevel;
import org.openbase.jul.extension.protobuf.processing.ProtoBufFieldProcessor;
import org.openbase.jul.extension.type.processing.TimestampProcessor;
import org.openbase.jul.pattern.ObservableImpl;
import org.openbase.jul.pattern.Observer;
import org.openbase.jul.schedule.*;
import org.openbase.type.domotic.action.ActionDescriptionType.ActionDescription;
import org.openbase.type.domotic.action.ActionDescriptionType.ActionDescription.Builder;
import org.openbase.type.domotic.action.ActionDescriptionType.ActionDescriptionOrBuilder;
import org.openbase.type.domotic.action.ActionParameterType.ActionParameter;
import org.openbase.type.domotic.action.ActionReferenceType.ActionReference;
import org.openbase.type.domotic.authentication.AuthTokenType.AuthToken;
import org.openbase.type.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import org.openbase.type.domotic.service.ServiceTempusTypeType.ServiceTempusType.ServiceTempus;
import org.openbase.type.domotic.state.ActionStateType.ActionState;
import org.openbase.type.domotic.state.ActionStateType.ActionState.State;
import org.openbase.type.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.*;

/**
 * * @author Divine <a href="mailto:DivineThreepwood@gmail.com">Divine</a>
 */
public class RemoteAction implements Action {

    public final static long AUTO_EXTENSION_INTERVAL = Action.MAX_EXECUTION_TIME_PERIOD / 2;

    private static final Logger LOGGER = LoggerFactory.getLogger(RemoteAction.class);
    private final SyncObject executionSync = new SyncObject("ExecutionSync");
    private final SyncObject extensionSync = new SyncObject("ExtensionSync");
    private final ActionParameter actionParameter;
    private final ObservableImpl<RemoteAction, ActionDescription> actionDescriptionObservable;
    private final AuthToken authToken;
    private final List<RemoteAction> impactedRemoteActions = new ArrayList<>();

    private final Observer unitObserver = (source, data) -> {

        // check if initial actionDescription is available
        if (RemoteAction.this.actionDescription == null) {
            return;
        }

        try {
            updateActionDescription((Collection<ActionDescription>) ProtoBufFieldProcessor.getRepeatedFieldList("action", (Message) data), false);
        } catch (NotAvailableException ex) {
            ExceptionPrinter.printHistory("Incoming DataType[" + data.getClass().getSimpleName() + "] does not provide an action list!", ex, LOGGER, LogLevel.WARN);
        }
    };

    private final Observer impactActionObserver = new Observer<RemoteAction, ActionDescription>() {
        @Override
        public void update(RemoteAction source, ActionDescription observable) throws Exception {
            actionDescriptionObservable.notifyObservers(source, observable);
        }
    };

    private ActionDescription actionDescription;
    private ActionReference actionReference;
    private UnitRemote<?> targetUnit;
    private Future<ActionDescription> futureObservationTask;
    private ScheduledFuture<?> autoExtensionTask;
    private String actionId;
    private ServiceType serviceType;
    private String targetUnitId;
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

    public RemoteAction(final ActionReference actionReference) throws InstantiationException {
        this(actionReference, null, null);
    }

    public RemoteAction(final Future<ActionDescription> actionFuture, Callable<Boolean> autoExtendCheckCallback) {
        this(actionFuture, null, autoExtendCheckCallback);
    }

    public RemoteAction(final Unit<?> executorUnit, final ActionParameter actionParameter, Callable<Boolean> autoExtendCheckCallback) throws InstantiationException, InterruptedException {
        this(executorUnit, actionParameter, null, autoExtendCheckCallback);
    }

    public RemoteAction(final ActionDescription actionDescription, Callable<Boolean> autoExtendCheckCallback) throws InstantiationException, InterruptedException {
        this(actionDescription, null, autoExtendCheckCallback);
    }

    public RemoteAction(final ActionReference actionReference, Callable<Boolean> autoExtendCheckCallback) throws InstantiationException {
        this(actionReference, null, autoExtendCheckCallback);
    }

    public RemoteAction(final Future<ActionDescription> actionFuture, final AuthToken authToken) {
        this(actionFuture, authToken, null);
    }

    public RemoteAction(final Unit<?> executorUnit, final ActionParameter actionParameter, final AuthToken authToken) throws InstantiationException, InterruptedException {
        this(executorUnit, actionParameter, authToken, null);
    }

    public RemoteAction(final ActionDescription actionDescription, final AuthToken authToken) throws InstantiationException, InterruptedException {
        this(actionDescription, authToken, null);
    }

    public RemoteAction(final ActionReference actionReference, final AuthToken authToken) throws InstantiationException {
        this(actionReference, authToken, null);
    }

    public RemoteAction(final ActionDescription actionDescription, final AuthToken authToken, Callable<Boolean> autoExtendCheckCallback) throws InstantiationException, InterruptedException {
        try {
            this.actionParameter = null;
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
        this.actionParameter = null;
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
            // setup initiator
            final ActionParameter.Builder actionParameterBuilder = actionParameter.toBuilder();
            if (initiatingUnit != null) {
                actionParameterBuilder.getActionInitiatorBuilder().setInitiatorId(initiatingUnit.getId());
            }

            this.actionParameter = actionParameterBuilder.build();
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

            // prepare target unit
            this.targetUnit = Units.getUnit(actionParameter.getServiceStateDescription().getUnitId(), false);
        } catch (CouldNotPerformException ex) {
            throw new InstantiationException(this, ex);
        }
    }

    public RemoteAction(final ActionReference actionReference, final AuthToken authToken, Callable<Boolean> autoExtendCheckCallback) throws InstantiationException {
        try {
            this.actionParameter = null;
            this.actionDescriptionObservable = new ObservableImpl<>();
            this.autoExtendCheckCallback = autoExtendCheckCallback;
            this.authToken = authToken;
            this.setActionReferenceAndStartObservation(actionReference);
        } catch (CouldNotPerformException ex) {
            throw new InstantiationException(this, ex);
        }
    }

    public Future<ActionDescription> execute(final ActionDescription causeActionDescription) {
        return execute(causeActionDescription, false);
    }

    /**
     * Executes the action which is defined by the {@code actionParameter} passed via constructor.
     * <p>
     * Note: The execution is canceled in case no {@code actionParameter} are available.
     *
     * @param causeActionDescription
     * @param force                  flag defines if the execution should be forced in case the remote is still executing a previous action of the same kind.
     *
     * @return
     */
    public Future<ActionDescription> execute(final ActionDescriptionOrBuilder causeActionDescription, final boolean force) {
        try {
            // check if action remote was instantiated via action parameter.
            if (actionParameter == null) {
                throw new NotAvailableException("ActionParameter");
            }

            if (force) {
                // we do not need to cancel a running actions because its rejected on the target unit anyway when a the new action is executed.
                reset();
            }

            if (isRunning()) {
                throw new InvalidStateException("Action is still running and can not be executed twice! Use the force flag to continue the execution.");
            }

            final ActionParameter.Builder actionParameterBuilder = actionParameter.toBuilder();
            synchronized (executionSync) {
                if (causeActionDescription != null) {
                    // validate cause
                    if (!causeActionDescription.hasActionId() || causeActionDescription.getActionId().isEmpty()) {
                        throw new InvalidStateException("Given action cause is not initialized!");
                    }
                    // set new cause
                    if (causeActionDescription instanceof ActionDescription) {
                        actionParameterBuilder.setCause((ActionDescription) causeActionDescription);
                    } else if (causeActionDescription instanceof ActionDescription.Builder) {
                        actionParameterBuilder.setCause((ActionDescription.Builder) causeActionDescription);
                    } else {
                        throw new FatalImplementationErrorException(this, new InvalidStateException("ActionDescriptionOrBuilder does not match expected type!"));
                    }
                }

                return FutureProcessor.allOfInclusiveResultFuture(setActionDescriptionAndStartObservation(targetUnit.applyAction(ActionDescriptionProcessor.generateActionDescriptionBuilder(actionParameterBuilder).build())), targetUnit.getDataFuture());
            }
        } catch (CouldNotPerformException ex) {
            return FutureProcessor.canceledFuture(ActionDescription.class, new CouldNotPerformException("Could not execute " + this + "!", ex));
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
                cancel();
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

        if (actionDescription == null) {
            throw new NotAvailableException("ActionDescription");
        }

        if (!actionDescription.hasActionId()) {
            throw new InvalidStateException("Given action description seems not to refer to an already executed action!", new NotAvailableException("ActionDescription.id"));
        }

        this.actionId = actionDescription.getActionId();
        this.serviceType = actionDescription.getServiceStateDescription().getServiceType();
        this.targetUnitId = actionDescription.getServiceStateDescription().getUnitId();

        // configure target unit if needed. This is the case if this remote action was instantiated via a future object.
        if (targetUnit == null) {
            targetUnit = Units.getUnit(targetUnitId, false);
        }

        synchronized (executionSync) {
            RemoteAction.this.actionDescription = actionDescription;

            if (actionDescription.getIntermediary()) {
                // observe impact actions and register callback if available to support action auto extension.
                for (ActionReference actionReference : actionDescription.getActionImpactList()) {
                    RemoteAction remoteAction = new RemoteAction(actionReference, authToken, autoExtendCheckCallback);
                    remoteAction.addActionDescriptionObserver(impactActionObserver);
                    impactedRemoteActions.add(remoteAction);

                    try {
                        actionDescriptionObservable.notifyObservers(remoteAction, remoteAction.getActionDescription());
                    } catch (NotAvailableException ex) {
                        // if the action description is not yet available then the remote action will inform the observer.
                    } catch (CouldNotPerformException ex) {
                        ExceptionPrinter.printHistory("Could not inform observer about action description update!", ex, LOGGER);
                    }
                }
            } else {
                setupActionObservation();
            }
        }
        return actionDescription;
    }

    private Future<ActionDescription> setActionReferenceAndStartObservation(final ActionReference actionReference) throws CouldNotPerformException {

        if (actionReference == null) {
            throw new NotAvailableException("ActionDescription");
        }

        if (!actionReference.hasActionId()) {
            throw new InvalidStateException("Given action description seems not to refer to an already executed action!", new NotAvailableException("ActionDescription.id"));
        }
        this.actionReference = actionReference;
        this.actionId = actionReference.getActionId();
        this.serviceType = actionReference.getServiceStateDescription().getServiceType();
        this.targetUnitId = actionReference.getServiceStateDescription().getUnitId();

        if (isInitializedByIntermediaryActionReference()) {
            futureObservationTask = FutureProcessor.canceledFuture(ActionDescription.class, new InvalidStateException("Intermediary actions initialized by an action reference do not offer an action description!"));
            return futureObservationTask;
        }

        futureObservationTask = GlobalCachedExecutorService.submit(() -> {
            try {

                if (targetUnit == null) {
                    targetUnit = Units.getUnit(actionReference.getServiceStateDescription().getUnitId(), true);
                }

                synchronized (executionSync) {

                    // resolve action description via the action reference if already available.
                    for (final ActionDescription actionDescription : targetUnit.getActionList()) {
                        if (actionDescription.getActionId().equals(actionReference.getActionId())) {
                            RemoteAction.this.actionDescription = actionDescription;
                            break;
                        }
                    }

                    if (actionDescription == null) {
                        throw new InvalidStateException("ActionDescription of unit[" + targetUnit.getLabel(actionReference.getServiceStateDescription().getUnitId()) + "] with action id[" + ActionDescriptionProcessor.toString(actionReference) + "] could not be resolved!");
                    }
                }
            } catch (CouldNotPerformException ex) {
                throw ExceptionPrinter.printHistoryAndReturnThrowable("Future observation task failed!", ex, LOGGER);
            }

            return setActionDescriptionAndStartObservation(actionDescription);

        });
        return futureObservationTask;
    }

    private void setupActionObservation() throws CouldNotPerformException {

        // register action update observation for any events independent of its service tempus.
        targetUnit.addDataObserver(ServiceTempus.UNKNOWN, unitObserver);

        // because future can already be outdated but the update not received because
        // the action id was not yet available we need to trigger an manual update.
        if (targetUnit.isDataAvailable()) {
            updateActionDescription(targetUnit.getActionList(), true);
        }

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
                    extend().get(30, TimeUnit.SECONDS);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                } catch (Exception ex) {
                    if (!ExceptionProcessor.isCausedBySystemShutdown(ex)) {
                        ExceptionPrinter.printHistory("Could not auto extend " + RemoteAction.this.toString(), ex, LOGGER);
                    }
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
            // in case this action was generated by an action reference and the referred action is intermediary we are already done since action references are only delivered through submitted actions.
            if (isInitializedByIntermediaryActionReference()) {
                throw new NotAvailableException(this.getClass().getSimpleName(), "ActionDescription", new InvalidStateException("Intermediary actions initialized by an action reference do not offer an action description!"));
            }
            throw new NotAvailableException(this.getClass().getSimpleName(), "ActionDescription");
        }
        return actionDescription;
    }

    /**
     * This check is useful because when an action remote was initialized by an intermediary action reference,
     * then we can not expect to receive any action description since intermediary actions are not scheduled and therefore no updates published.
     *
     * @return true if this action was initialized by an intermediary action reference, otherwise false.
     */
    private boolean isInitializedByIntermediaryActionReference() {

        if (actionReference == null) {
            return false;
        }

        if (actionReference.getIntermediary()) {
            return true;
        }

        if (targetUnit == null) {
            return false;
        }

        try {
            if (targetUnit.getUnitType() == UnitType.LOCATION
                    || targetUnit.getUnitType() == UnitType.UNIT_GROUP) {
                LOGGER.warn("Intermediary flag not properly synchronized! Recover state for " + targetUnit.getLabel());
                return true;
            }
        } catch (NotAvailableException ex) {
            // no recovery possible
        }

        return false;

    }

    /**
     * Method returns the id of the connected action.
     *
     * @return the action id as string.
     *
     * @throws NotAvailableException if the id is not available.
     */
    public String getActionId() throws NotAvailableException {
        if (actionId == null || actionId.isEmpty()) {
            throw new NotAvailableException("ActionId");
        }
        return actionId;
    }

    /**
     * {@inheritDoc}
     *
     * @return {@inheritDoc}
     */
    @Override
    public boolean isValid() {
        return (actionDescription != null && Action.super.isValid());
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
                        if (impactedRemoteAction.isRunning()) {
                            return true;
                        }
                    }
                    return false;
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
        LOGGER.debug("cancel {}", this);
        Future<ActionDescription> future = null;
        try {
            synchronized (executionSync) {
                // make sure an action is not auto extended during the cancellation process.
                cancelAutoExtension();

                // if future operation is still running then...
                if (futureObservationTask != null && !futureObservationTask.isDone()) {

                    if (targetUnit != null && (actionDescription != null || actionReference != null || (actionId != null && serviceType != null))) {
                        // cancel observation
                        futureObservationTask.cancel(true);
                    } else {
                        try {
                            futureObservationTask.get(2, TimeUnit.SECONDS);
                        } catch (InterruptedException ex) {
                            Thread.currentThread().interrupt();
                        } catch (Exception ex) {
                            // continue with other trails
                        } finally {
                            futureObservationTask.cancel(true);
                        }
                    }
                }

                // if already done then skip cancellation
                if (isDone()) {
                    future = FutureProcessor.completedFuture(getActionDescription());
                }

                // handle intermediary action
                if (actionDescription != null && getActionDescription().getIntermediary()) {
                    LOGGER.debug("Cancel impact of {}", this);
                    // cancel all impacts of this actions and return the current action description
                    future = FutureProcessor.allOf(impactedRemoteActions, (input, time, timeUnit) -> actionDescription, (remoteAction, time, timeUnit) -> remoteAction.cancel());
                    return registerPostActionStateUpdate(future, State.CANCELED);
                }

                // cancel the action via controller if possible.
                if (targetUnit != null) {
                    if (targetUnit.isConnected()) {
                        future = targetUnit.cancelAction(buildBestActionDescriptionToCancelAction(), authToken);
                    } else {
                        future = FutureProcessor.postProcess((data, time, timeUnit) -> {
                            try {
                                return targetUnit.cancelAction(buildBestActionDescriptionToCancelAction(), authToken).get(3, TimeUnit.SECONDS);
                            } catch (InterruptedException ex) {
                                Thread.currentThread().interrupt();
                                throw new CouldNotPerformException("Could not cancel action!", ex);
                            } catch (Exception ex) {
                                throw new CouldNotPerformException("Could not cancel action!", ex);
                            }
                        }, targetUnit.getDataFuture());
                    }
                } else {
                    future = FutureProcessor.canceledFuture(ActionDescription.class, new CouldNotPerformException("Could not cancel action since target unit is not known!"));
                }
                return registerPostActionStateUpdate(future, State.CANCELED);
            }
        } catch (CouldNotPerformException ex) {
            return FutureProcessor.canceledFuture(ActionDescription.class, ex);
        } finally {
            // cleanup when cancellation is done
            if (future == null || future.isDone()) {
                cleanup();
            } else {
                // otherwise create cleanup task
                final Future<ActionDescription> passthroughFuture = future;
                try {
                    return GlobalCachedExecutorService.submit(() -> {
                        try {
                            return passthroughFuture.get(10, TimeUnit.SECONDS);
                        } catch (InterruptedException ex) {
                            throw ex;
                        } catch (ExecutionException | java.util.concurrent.TimeoutException ex) {
                            throw new CouldNotProcessException("Could not cancel " + RemoteAction.this.toString(), ex);
                        } finally {
                            // clear
                            cleanup();
                        }
                    });
                } catch (RejectedExecutionException ex) {
                    return FutureProcessor.canceledFuture(ActionDescription.class, ex);
                }
            }
        }
    }

    private ActionDescription buildBestActionDescriptionToCancelAction() throws NotAvailableException {
        try {
            return getActionDescription();
        } catch (NotAvailableException e) {
            if (actionReference != null) {
                final Builder actionDescriptionBuilder = ActionDescription.newBuilder();
                actionDescriptionBuilder.setActionId(actionReference.getActionId());
                actionDescriptionBuilder.setServiceStateDescription(actionReference.getServiceStateDescription());
                actionDescriptionBuilder.setCancel(true);
                return actionDescriptionBuilder.build();
            } else if (actionId != null && serviceType != null) {
                final Builder actionDescriptionBuilder = ActionDescription.newBuilder();
                actionDescriptionBuilder.setActionId(actionId);
                actionDescriptionBuilder.getServiceStateDescriptionBuilder().setServiceType(serviceType);
                actionDescriptionBuilder.getServiceStateDescriptionBuilder().setUnitId(targetUnit.getId());
                actionDescriptionBuilder.setCancel(true);
                return actionDescriptionBuilder.build();
            } else {
                throw new NotAvailableException("ActionDescription");
            }
        }
    }

    private Future<ActionDescription> registerPostActionStateUpdate(final Future<ActionDescription> future, final ActionState.State actionState) {
        return FutureProcessor.postProcess((result, time, timeUnit) -> {

            // when all sub actions are canceled, than we can mark this intermediary action as canceled as well.
            if (result != null) {
                result = result.toBuilder().setActionState(ActionState.newBuilder().setValue(actionState)).build();
            }

            // update internal description if available
            if (actionDescription != null) {
                actionDescription = result;
            }
            return result;
        }, future);
    }

    /**
     * Method resets the entire action remote. After calling no further action states are available.
     */
    public void reset() {
        cleanup();

        // reset auto extension task
        autoExtensionTask = null;

        // reset observation task
        futureObservationTask = null;

        // reset action state and id
        actionId = null;
        if (actionDescription != null) {
            actionDescription = actionDescription.toBuilder().clearActionId().clearActionState().build();
        }
    }

    /**
     * Method cleans up any resources used by this action like the observation task and the target unit.
     * All information about the action itself will be kept to still enable action state requests.
     */
    private void cleanup() {

        LOGGER.debug("cleanup {}", this);

        // cancel observation task
        if (futureObservationTask != null && !futureObservationTask.isDone()) {
            futureObservationTask.cancel(true);
        }

        // cleanup synchronisation and observation tasks
        actionDescriptionObservable.reset();
        for (RemoteAction impactedRemoteAction : impactedRemoteActions) {
            impactedRemoteAction.removeActionDescriptionObserver(impactActionObserver);
        }
        impactedRemoteActions.clear();

        // cancel the auto extension task
        cancelAutoExtension();

        if (targetUnit != null) {
            targetUnit.removeDataObserver(ServiceTempus.UNKNOWN, unitObserver);
        }

        // check if already done, otherwise set state to unknown since no further state updates will be received.
        if (!isDone()) {
            final ActionDescription.Builder builder;
            if (actionDescription == null) {
                builder = ActionDescription.newBuilder();
            } else {
                builder = actionDescription.toBuilder();
            }
            // set state as unknown since action will no longer be observed.
            builder.getActionStateBuilder().setValue(State.UNKNOWN);
            actionDescription = builder.build();
        }
    }

    private void updateActionDescription(final Collection<ActionDescription> actionDescriptions, final boolean initialSync) {

        if (actionDescriptions == null) {
            LOGGER.warn("Update skipped because no action descriptions passed!");
            return;
        }

        if (RemoteAction.this.actionDescription == null) {
            LOGGER.warn("Update skipped because action description is not available!");
            return;
        }

        // update action description and notify
        for (ActionDescription actionDescription : actionDescriptions) {

            if (actionDescription.getActionId().equals(RemoteAction.this.actionDescription.getActionId())) {

                synchronized (executionSync) {
                    final boolean actionExtended = RemoteAction.this.actionDescription.getLastExtensionTimestamp().getTime() < actionDescription.getLastExtensionTimestamp().getTime();
                    RemoteAction.this.actionDescription = actionDescription;

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
                }

                // cleanup observation if action is done.
                if (isDone()) {
                    cleanup();
                }

                return;
            }
        }

        // if this action is not listed on its target unit and its not just the initial sync where the action id is maybe not yet listed,
        // then we can be sure that this action is an outdated one and the remote action can be cleaned up.
        if (!initialSync) {
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
        waitForRegistration();

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
     * Wait until this action reaches a provided action state.
     *
     * @param actionState the state on which is waited.
     *
     * @throws CouldNotPerformException if the action was not yet executed, the provided action state is not certainly notified
     *                                  or the provided state cannot be reached anymore.
     * @throws InterruptedException     if the thread was externally interrupted.
     */
    public void waitForActionState(final ActionState.State actionState) throws CouldNotPerformException, InterruptedException {
        try {
            waitForActionState(actionState, Timeout.INFINITY_TIMEOUT, TimeUnit.MILLISECONDS);
        } catch (TimeoutException ex) {
            throw new FatalImplementationErrorException("Timeout while wait infinitely.", this);
        }
    }

    /**
     * Wait until this action reaches a provided action state.
     *
     * @param actionState the state on which is waited.
     *
     * @throws CouldNotPerformException if the action was not yet executed, the provided action state is not certainly notified
     *                                  or the provided state cannot be reached anymore.
     * @throws InterruptedException     if the thread was externally interrupted.
     * @throws TimeoutException         is thrown in case the timeout is reached.
     */
    public void waitForActionState(final ActionState.State actionState, final long timeout, final TimeUnit timeUnit) throws CouldNotPerformException, InterruptedException {
        final TimeoutSplitter timeSplit = new TimeoutSplitter(timeout, timeUnit);

        // check not required anymore since state history is observed as well.
//        if (!isNotifiedActionState(actionState)) {
//            throw new CouldNotPerformException("Cannot wait for state[" + actionState + "] because it is not always notified");
//        }

        waitForRegistration(timeSplit.getTime(), TimeUnit.MILLISECONDS);

        try {
            if (actionDescription.getIntermediary()) {
                for (final RemoteAction impactedRemoteAction : impactedRemoteActions) {
                    impactedRemoteAction.waitForActionState(actionState, timeSplit.getTime(), TimeUnit.MILLISECONDS);
                }
                return;
            }

        } catch (NotAvailableException ex) {
            // if the action description is not available, then we just continue and wait for it.
            // sure? this does not work for intermediary actions!
            ExceptionPrinter.printHistory("Could not observe intermediary actions!", ex, LOGGER);
        }

        // wait until unit is ready
        targetUnit.waitForData(timeSplit.getTime(), TimeUnit.MILLISECONDS);


        synchronized (executionSync) {
            // wait until state is reached
            while (actionDescription == null || (actionDescription.getActionState().getValue() != actionState) && !checkIfStateWasPassed(actionState, timeSplit.getTimestamp(), actionDescription)) {
                // Waiting makes no sense if the action is done but the state is still not reached.
                if (actionDescription != null && isDone()) {
                    throw new CouldNotPerformException("Stop waiting because state[" + actionState.name() + "] cannot be reached from state[" + actionDescription.getActionState().getValue().name() + "]");
                }
                executionSync.wait(timeSplit.getTime());
            }
        }
    }

    private static boolean checkIfStateWasPassed(final ActionState.State actionState, final long timestamp, final ActionDescription actionDescription) {
        try {
            return TimestampProcessor.getTimestamp(ServiceStateProcessor.getLatestValueOccurrence(actionState, actionDescription), TimeUnit.MILLISECONDS) > timestamp;
        } catch (NotAvailableException e) {
            return false;
        }
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

    public void waitForRegistration() throws CouldNotPerformException, InterruptedException {

        // in case this action was generated by an action reference and the referred action is intermediary we are already done since action references are only delivered through submitted actions.
        if (isInitializedByIntermediaryActionReference()) {
            return;
        }

        synchronized (executionSync) {
            if (futureObservationTask == null && !getActionDescription().hasActionId()) {
                throw new InvalidStateException("Action was never executed!");
            }
        }

        try {
            if (futureObservationTask != null) {
                try {
                    futureObservationTask.get();
                } catch (CancellationException ex) {
                    // in case the observation task is canceled the action is possibly already done.
                    try {
                        if (!getActionDescription().hasActionId()) {
                            throw new CouldNotPerformException("Registration task was canceled but action id never received!");
                        }
                    } catch (NotAvailableException exx) {
                        throw new CouldNotPerformException("Registration task was canceled but action description never received!");
                    }
                }
            }

            if (getActionDescription().getIntermediary()) {
                for (final RemoteAction impactedRemoteAction : impactedRemoteActions) {
                    impactedRemoteAction.waitForRegistration();
                }
            }
        } catch (ExecutionException ex) {
            throw new CouldNotPerformException("Could not wait for submission!", ex);
        }
    }

    public void waitForRegistration(final long timeout, final TimeUnit timeUnit) throws CouldNotPerformException, InterruptedException {
        final TimeoutSplitter timeSplit = new TimeoutSplitter(timeout, timeUnit);

        // in case this action was generated by an action reference and the referred action is intermediary we are already done since action references are only delivered through submitted actions.
        if (isInitializedByIntermediaryActionReference()) {
            return;
        }

        synchronized (executionSync) {
            if (futureObservationTask == null && (actionId == null || actionId.isEmpty())) {
                throw new InvalidStateException("Action was never executed!");
            }
        }

        try {
            if (futureObservationTask != null) {
                try {
                    if (timeout == 0l || timeUnit.toMillis(timeout) == Timeout.INFINITY_TIMEOUT) {
                        futureObservationTask.get();
                    } else {
                        futureObservationTask.get(timeSplit.getTime(), TimeUnit.MILLISECONDS);
                    }
                } catch (java.util.concurrent.TimeoutException ex) {
                    throw new org.openbase.jul.exception.TimeoutException();
                }
            }

            if (getActionDescription().getIntermediary()) {
                for (final RemoteAction impactedRemoteAction : impactedRemoteActions) {
                    impactedRemoteAction.waitForRegistration(timeSplit.getTime(), TimeUnit.MILLISECONDS);
                }
            }
        } catch (ExecutionException | CancellationException ex) {
            throw new CouldNotPerformException("Could not wait for registration of " + this + "!", ex);
        }
    }

    public boolean isRegistrationDone() {
        if (futureObservationTask != null && futureObservationTask.isDone()) {

            if (actionDescription == null) {
                return false;
            }

            if (actionDescription.getIntermediary()) {
                for (final RemoteAction impactedRemoteAction : impactedRemoteActions) {
                    if (!impactedRemoteAction.isRegistrationDone()) {
                        return false;
                    }
                }
            }
            return actionDescription.hasActionId();
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
            return FutureProcessor.canceledFuture(ActionDescription.class, ex);
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
     *
     * @throws NotAvailableException is thrown if the unit is not loaded yet.
     */
    public UnitRemote<?> getTargetUnit() throws NotAvailableException {

        if (targetUnit == null) {
            throw new NotAvailableException("TargetUnit");
        }
        return targetUnit;
    }

    /**
     * Method returns the action parameter used to execute the remote action in case it was initialized via action parameters.
     *
     * @return the action parameters used to build the action
     *
     * @throws NotAvailableException is thrown in case the action remote was not initialized via the action parameters.
     */
    public ActionParameter getActionParameter() throws NotAvailableException {
        if (actionParameter == null) {
            // can happen in case the remote was not initialized by using the action parameter.
            throw new NotAvailableException("ActionParameter");
        }
        return actionParameter;
    }

    /**
     * Generates a string representation of this action.
     *
     * @return a description of this unit.
     */
    @Override
    public String toString() {

        try {
            // use action description for printing since it offers most detailed information about the action
            return ActionDescriptionProcessor.toString(getActionDescription());
        } catch (NotAvailableException e) {

            // resolve via reference
            if (actionReference != null) {
                return ActionDescriptionProcessor.toString(actionReference);
            }

            // resolve via parameter
            if (actionParameter != null) {
                return ActionDescriptionProcessor.toString(actionParameter);
            }
        }

        // use default as fallback
        return Action.toString(this);
    }
}

