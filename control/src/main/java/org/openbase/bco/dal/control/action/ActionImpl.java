package org.openbase.bco.dal.control.action;

/*
 * #%L
 * BCO DAL Control
 * %%
 * Copyright (C) 2014 - 2020 openbase.org
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

import com.google.protobuf.Message;
import org.openbase.bco.dal.control.layer.unit.AbstractUnitController;
import org.openbase.bco.dal.lib.action.Action;
import org.openbase.bco.dal.lib.action.ActionDescriptionProcessor;
import org.openbase.bco.dal.lib.action.SchedulableAction;
import org.openbase.bco.dal.lib.jp.JPProviderControlMode;
import org.openbase.bco.dal.lib.layer.service.ServiceStateProcessor;
import org.openbase.bco.dal.lib.layer.service.Services;
import org.openbase.jps.core.JPService;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.exception.*;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.exception.printer.LogLevel;
import org.openbase.jul.extension.protobuf.ClosableDataBuilder;
import org.openbase.jul.extension.type.processing.MultiLanguageTextProcessor;
import org.openbase.jul.extension.type.processing.TimestampProcessor;
import org.openbase.jul.schedule.*;
import org.openbase.type.domotic.action.ActionDescriptionType.ActionDescription;
import org.openbase.type.domotic.action.ActionInitiatorType.ActionInitiator.InitiatorType;
import org.openbase.type.domotic.action.ActionPriorityType.ActionPriority.Priority;
import org.openbase.type.domotic.service.ServiceDescriptionType.ServiceDescription;
import org.openbase.type.domotic.service.ServiceTemplateType.ServiceTemplate.ServicePattern;
import org.openbase.type.domotic.service.ServiceTempusTypeType.ServiceTempusType.ServiceTempus;
import org.openbase.type.domotic.state.ActionStateType.ActionState;
import org.openbase.type.domotic.state.ActionStateType.ActionState.State;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * @author Divine <a href="mailto:DivineThreepwood@gmail.com">Divine</a>
 */
public class ActionImpl implements SchedulableAction {

    /**
     * Timeout how long it is waited on execution failure until a rescheduling process is triggered.
     */
    private static final long EXECUTION_FAILURE_TIMEOUT = TimeUnit.SECONDS.toMillis(15);

    private static final Logger LOGGER = LoggerFactory.getLogger(ActionImpl.class);

    protected final AbstractUnitController<?, ?> unit;
    private final SyncObject executionStateChangeSync = new SyncObject("ExecutionStateChangeSync");
    private final SyncObject actionTaskLock = new SyncObject("ActionTaskLock");
    private final ReentrantReadWriteLock actionDescriptionBuilderLock = new ReentrantReadWriteLock();
    private ActionDescription.Builder actionDescriptionBuilder;
    private Message serviceState;
    private ServiceDescription serviceDescription;
    private Future<ActionDescription> actionTask;

    /**
     * Constructor creates a new action object which helps to manage its execution during the scheduling process.
     * <p>
     * Note: This constructor always aspects a new action which will be prepared during construction.
     *
     * @param actionDescription the description of the action.
     * @param unit              the target unit which needs to be allocated.
     *
     * @throws InstantiationException is throw in case the given action description is invalid. Checkout the exception cause chain for more details.
     */
    public ActionImpl(final ActionDescription actionDescription, final AbstractUnitController<?, ?> unit) throws InstantiationException {
        try {
            this.unit = unit;
            this.init(actionDescription);
        } catch (CouldNotPerformException ex) {
            throw new InstantiationException(this, ex);
        }
    }

    /**
     * Constructor creates a new action object which is already executing.
     * The action description is fully generated based on the responsible action of the service state.
     *
     * @param serviceState describing the ongoing action.
     * @param unit         the target unit which needs to be allocated.
     *
     * @throws InstantiationException is throw in case the given action description is invalid. Checkout the exception cause chain for more details.
     */
    public ActionImpl(final Message serviceState, final AbstractUnitController<?, ?> unit) throws InstantiationException {
        try {
            this.unit = unit;
            this.init(serviceState);
        } catch (CouldNotPerformException ex) {
            throw new InstantiationException(this, ex);
        }
    }


    @Override
    public void init(final ActionDescription actionDescription) throws InitializationException {
        init(actionDescription, true);
    }

    private void init(final ActionDescription actionDescription, final boolean prepare) throws InitializationException {
        LOGGER.trace("================================================================================");
        actionDescriptionBuilderLock.writeLock().lock();
        try {
            actionDescriptionBuilder = actionDescription.toBuilder();

            // verify and prepare action description
            serviceState = ActionDescriptionProcessor.verifyActionDescription(actionDescriptionBuilder, unit, prepare).build();

            if (!Services.hasResponsibleAction(serviceState)) {
                StackTracePrinter.printStackTrace(LOGGER);
            }

            // initially set last extension to creation time
            actionDescriptionBuilder.setLastExtensionTimestamp(actionDescriptionBuilder.getTimestamp());

            // since its an action it has to be an operation service pattern
            serviceDescription = ServiceDescription.newBuilder().setServiceType(actionDescriptionBuilder.getServiceStateDescription().getServiceType()).setPattern(ServicePattern.OPERATION).build();

            // mark new action as initialized.
            if (prepare) {
                updateActionState(State.INITIALIZED);
            }
        } catch (CouldNotPerformException ex) {
            throw new InitializationException(this, ex);
        } finally {
            actionDescriptionBuilderLock.writeLock().unlock();
        }
    }

    private void init(final Message serviceState) throws InitializationException {
        LOGGER.trace("================================================================================");
        actionDescriptionBuilderLock.writeLock().lock();
        try {

            this.serviceState = serviceState;

            actionDescriptionBuilder = Services.getResponsibleAction(serviceState).toBuilder();

            // verify and prepare action description
            ActionDescriptionProcessor.verifyActionDescription(actionDescriptionBuilder, unit, false).build();

            // initially set last extension to creation time
            actionDescriptionBuilder.setLastExtensionTimestamp(actionDescriptionBuilder.getTimestamp());

            // since its an action it has to be an operation service pattern
            serviceDescription = ServiceDescription.newBuilder().setServiceType(actionDescriptionBuilder.getServiceStateDescription().getServiceType()).setPattern(ServicePattern.OPERATION).build();

            // mark new action as initialized.
            updateActionState(State.EXECUTING);
        } catch (CouldNotPerformException ex) {
            throw new InitializationException(this, ex);
        } finally {
            actionDescriptionBuilderLock.writeLock().unlock();
        }
    }

    /**
     * checks if the execution task is finished.
     *
     * @return true if the execution task is finish otherwise true.
     */
    private boolean isActionTaskFinish() {
        synchronized (actionTaskLock) {
            // when the action task finishes it will finally reset the action task to null.
            // actionTask.isDone(); is not a solution at all because when the action task is
            // canceled then the method already returns true but the task is maybe still running.
            return actionTask == null;
        }
    }

    @Override
    public boolean isProcessing() {
        return !isActionTaskFinish() || SchedulableAction.super.isProcessing();
    }

    /**
     * {@inheritDoc}
     *
     * @return {@inheritDoc}
     */
    @Override
    public Future<ActionDescription> execute() {

        // builder lock has to be locked first before locking the action task lock to avoid deadlocks
        try {
            actionDescriptionBuilderLock.writeLock().lockInterruptibly();
        } catch (InterruptedException ex) {
            return FutureProcessor.canceledFuture(ActionDescription.class, ex);
        }
        try {
            synchronized (actionTaskLock) {

                // avoid execution in case unit is shutting down
                if (unit.isShutdownInProgress()) {
                    return FutureProcessor.canceledFuture(ActionDescription.class, new ShutdownInProgressException(unit));
                }

                // validate if action task is already executing
                if (isProcessing()) {
                    return FutureProcessor.canceledFuture(ActionDescription.class, new InvalidStateException("Can not execute an already processing action!"));
                }

                // handle abortion or cancellation before the execution has started.
                if (actionDescriptionBuilder.getActionState().getValue() == State.CANCELING) {
                    return FutureProcessor.canceledFuture(ActionDescription.class, new InvalidStateException("Action was canceled before the execution has started."));
                } else if (actionDescriptionBuilder.getActionState().getValue() == State.ABORTING) {
                    return FutureProcessor.canceledFuture(ActionDescription.class, new InvalidStateException("Action was aborted before the execution has started."));
                }

                // Initiate
                updateActionState(ActionState.State.INITIATING);

                actionTask = GlobalCachedExecutorService.submit(() -> {
                    try {

                        // validate operation service
                        boolean hasOperationService = false;
                        for (ServiceDescription description : unit.getUnitTemplate().getServiceDescriptionList()) {
                            if (description.getServiceType() == serviceDescription.getServiceType() && description.getPattern() == ServicePattern.OPERATION) {
                                hasOperationService = true;
                                break;
                            }
                        }

                        // check is implicitly used make sure actionTask variable is not null since it will be synchronized during call.
                        if (isActionTaskFinish()) {
                            new FatalImplementationErrorException("Action task is marked as finished even when the task is still running!", this);
                        }

                        // loop as long as task is not canceled.
                        while (!Thread.interrupted() && (actionTask == null || !actionTask.isCancelled())) {

                            try {
                                // validate action state
                                if (isDone()) {
                                    LOGGER.error(ActionImpl.this + " was done before executed!");
                                    break;
                                }

                                if (!isValid()) {
                                    LOGGER.debug(ActionImpl.this + " no longer valid and will be rejected!");
                                    updateActionStateIfNotCanceled(State.REJECTED);
                                    break;
                                }

                                // Submission
                                updateActionStateIfNotCanceled(State.SUBMISSION);

                                // only update requested state if it is an operation state, else throw an exception if not in provider control mode
                                if (!hasOperationService) {
                                    if (!JPService.getValue(JPProviderControlMode.class, false)) {
                                        throw new NotAvailableException("Operation service " + serviceDescription.getServiceType().name() + " of unit " + unit);
                                    }
                                } else {
                                    setRequestedState();
                                }

                                unit.performOperationService(serviceState, serviceDescription.getServiceType()).get(EXECUTION_FAILURE_TIMEOUT, TimeUnit.SECONDS);
                                updateActionStateIfNotCanceled(State.EXECUTING);

                                // action can be finished if not done yet and time has expired or execution time was never required.
                                if (!isDone() && (isExpired() || getExecutionTimePeriod(TimeUnit.MICROSECONDS) == 0)) {
                                    updateActionStateIfNotCanceled(State.FINISHED);
                                }
                                break;
                            } catch (CouldNotPerformException | ExecutionException | RuntimeException ex) {
                                // avoid execution in case unit is shutting down
                                if (unit.isShutdownInProgress() || ExceptionProcessor.isCausedBySystemShutdown(ex)) {
                                    updateActionStateIfNotCanceled(State.REJECTED);
                                    break;
                                }

                                if (!isDone()) {
                                    updateActionStateIfNotCanceled(State.SUBMISSION_FAILED);
                                }
                                ExceptionPrinter.printHistory("Action execution failed", ex, LOGGER, LogLevel.WARN);
                                Thread.sleep(EXECUTION_FAILURE_TIMEOUT);
                            }
                        }
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                        throw ex;
                    } catch (Exception ex) {
                        ExceptionPrinter.printHistory("Action task crashed!", new FatalImplementationErrorException(this, ex), LOGGER);
                        if (!isDone()) {
                            updateActionState(State.REJECTED);
                        }
                    } finally {
                        synchronized (actionTaskLock) {
                            actionTask = null;
                            actionTaskLock.notifyAll();
                        }
                    }
                    return getActionDescription();
                });

                return actionTask;
            }
        } finally {
            actionDescriptionBuilderLock.writeLock().unlock();
        }
    }

    /**
     * Method blocks until the action reaches a terminated state.
     *
     * @throws InterruptedException is thrown if the thread was externally interrupted.
     */
    public void waitUntilDone() throws InterruptedException {
        synchronized (executionStateChangeSync) {
            while (!isDone()) {
                executionStateChangeSync.wait();
            }
        }
    }

    /**
     * Method blocks until the action reaches a terminated state or the timeout is reached.
     *
     * @throws InterruptedException is thrown if the thread was externally interrupted.
     */
    public void waitUntilDone(final long timeout, final TimeUnit timeUnit) throws InterruptedException, TimeoutException {
        final TimeoutSplitter timeoutSplitter = new TimeoutSplitter(timeout, timeUnit);
        synchronized (executionStateChangeSync) {
            while (!isDone()) {
                executionStateChangeSync.wait(timeoutSplitter.getTime());
            }
        }
    }

    private void waitForActionTaskFinalization(final long timeout, final TimeUnit timeUnit) throws InterruptedException, TimeoutException {
        final TimeoutSplitter timeoutSplitter = new TimeoutSplitter(timeout, timeUnit);
        synchronized (actionTaskLock) {
            while (!isActionTaskFinish()) {
                actionTaskLock.wait(timeoutSplitter.getTime());
            }
        }
    }

    private void setRequestedState() throws CouldNotPerformException, InterruptedException {
        try (ClosableDataBuilder dataBuilder = unit.getDataBuilderInterruptible(this)) {

            if (!Services.hasResponsibleAction(serviceState)) {
                StackTracePrinter.printStackTrace(LOGGER);
            }

            // set the new service attribute as requested state in the unit data builder
            Services.invokeServiceMethod(serviceDescription.getServiceType(), serviceDescription.getPattern(), ServiceTempus.REQUESTED, dataBuilder.getInternalBuilder(), serviceState);
        }
    }

    /**
     * {@inheritDoc }
     *
     * @return {@inheritDoc }
     */
    @Override
    public ActionDescription getActionDescription() {
        actionDescriptionBuilderLock.readLock().lock();
        try {
            return actionDescriptionBuilder.build();
        } finally {
            actionDescriptionBuilderLock.readLock().unlock();
        }
    }

    @Override
    public void autoExtendWithLowPriority() throws VerificationFailedException {

        // auto extend
        actionDescriptionBuilderLock.writeLock().lock();
        try {

            // validate if action is extendable
            if (!isAutoContinueWithLowPriorityIntended()) {
                throw new VerificationFailedException(this + "is not compatible to be automatically extended because flag is not set!");
            } else if (ActionDescriptionProcessor.getInitialInitiator(actionDescriptionBuilder).getInitiatorType() != InitiatorType.HUMAN) {
                throw new VerificationFailedException(this + "is not compatible to be automatically extended because it was not initiated by a human!");
            } else if (isDone()) {
                throw new VerificationFailedException(this + "is not compatible to be automatically extended because it is already done!");
            }

            // extend
            actionDescriptionBuilder.setPriority(Priority.LOW);
            actionDescriptionBuilder.setInterruptible(false);
            actionDescriptionBuilder.setSchedulable(false);
            actionDescriptionBuilder.setExecutionTimePeriod(Timeout.getInfinityTimeout(TimeUnit.MICROSECONDS));

            // has to be reset because otherwise the action would still be invalid.
            actionDescriptionBuilder.setLastExtensionTimestamp(TimestampProcessor.getCurrentTimestamp());
        } finally {
            actionDescriptionBuilderLock.writeLock().unlock();
        }

        // validate that action is valid after extension
        if (!isValid()) {
            throw new VerificationFailedException(this + "is not valid after extension!");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Future<ActionDescription> cancel() {

        try {
            actionDescriptionBuilderLock.writeLock().lockInterruptibly();
        } catch (InterruptedException ex) {
            return FutureProcessor.canceledFuture(ActionDescription.class, ex);
        }
        try {
            // if action is not executing, set to canceled if not already done and finish
            if (!isProcessing()) {
                if (!isDone()) {
                    updateActionState(State.CANCELED);
                }

                // we need to update the transaction id to inform the remote that the action was successful even when already canceled.
                try {
                    unit.updateTransactionId();
                } catch (CouldNotPerformException ex) {
                    ExceptionPrinter.printHistory("Could not update transaction id", ex, LOGGER);
                }

                // notify transaction id change
                try {
                    if(!unit.isDataBuilderWriteLockedByCurrentThread()) {
                        unit.notifyChange();
                    }
                } catch (CouldNotPerformException ex) {
                    ExceptionPrinter.printHistory("Could not notify transaction id update", ex, LOGGER);
                } catch (InterruptedException ex) {
                    return FutureProcessor.canceledFuture(ActionDescription.class, ex);
                }

                return FutureProcessor.completedFuture(getActionDescription());
            }

            // action is currently executing, so set to canceling, wait till its done, set to canceled and trigger reschedule
            updateActionState(State.CANCELING);
            try {
                return GlobalCachedExecutorService.submit(() -> {

                    // cancel action task
                    cancelActionTask();

                    if (!isDone()) {
                        updateActionState(State.CANCELED);
                    }

                    // trigger reschedule because any next action can be executed.
                    try {
                        unit.reschedule();
                    } catch (CouldNotPerformException ex) {
                        // if the reschedule is not possible because of an system shutdown everything is fine, otherwise its a controller error and there is no need to inform the remote about any error if the cancellation was successful.
                        if (!ExceptionProcessor.isCausedBySystemShutdown(ex)) {
                            ExceptionPrinter.printHistory("Reschedule of " + unit + " failed after action cancellation!", ex, LOGGER);
                        }
                    }
                    return getActionDescription();

                });
            } catch (RejectedExecutionException ex) {
                return FutureProcessor.canceledFuture(ActionDescription.class, new CouldNotPerformException("Could not cancel " + this, ex));
            }
        } finally {
            actionDescriptionBuilderLock.writeLock().unlock();
        }
    }

    @Override
    public Future<ActionDescription> abort(boolean forceReject) {
        try {
            actionDescriptionBuilderLock.writeLock().lockInterruptibly();
        } catch (InterruptedException ex) {
            return FutureProcessor.canceledFuture(ActionDescription.class, ex);
        }
        try {
            if (!isProcessing()) {
                // this should never happen since a task should be executing before it is aborted
                LOGGER.error("Aborted action was not executing before");
                return FutureProcessor.completedFuture(getActionDescription());
            }

            updateActionState(State.ABORTING);
            return GlobalCachedExecutorService.submit(() -> {

                // cancel action task
                cancelActionTask();

                // if not done yet
                if (!isDone()) {
                    // if action is interruptible it can be scheduled otherwise it is rejected
                    if (!forceReject && getActionDescription().getInterruptible() && getActionDescription().getSchedulable()) {
                        updateActionState(State.SCHEDULED);
                    } else {
                        updateActionState(State.REJECTED);
                    }
                }

                // rescheduling is not necessary because aborting is only done when rescheduling
                return getActionDescription();
            });
        } finally {
            actionDescriptionBuilderLock.writeLock().unlock();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void schedule() {

        // cancel action task
        cancelActionTask();

        actionDescriptionBuilderLock.writeLock().lock();
        try {

            if (isProcessing()) {
                final Future<ActionDescription> abortionTask = abort(true);

                // apply error handling
                if (abortionTask.isCancelled()) {
                    try {
                        abortionTask.get();
                    } catch (Exception ex) {
                        // in case of internal interruption just interrupt thread and return
                        // handle recursive interruption as well because interruption can be encapsulated in ExecutionException.
                        if(ExceptionProcessor.isCausedByInterruption(ex)) {
                            Thread.currentThread().interrupt();
                            return;
                        }
                        ExceptionPrinter.printHistory("Could not abort " + this + " in order to schedule it.", ex, LOGGER);
                    }
                }
            }

            // if not already finished then we force the state.
            if (!isDone() && getActionState() != State.CANCELING) {
                updateActionState(State.SCHEDULED);
            }
        } finally {
            actionDescriptionBuilderLock.writeLock().unlock();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void reject() {

        // cancel action task
        cancelActionTask();

        actionDescriptionBuilderLock.writeLock().lock();
        try {

            if (isProcessing()) {
                final Future<ActionDescription> abortionTask = abort(true);

                // apply error handling
                if (abortionTask.isCancelled()) {
                    try {
                        abortionTask.get();
                    } catch (Exception ex) {
                        // in case of internal interruption just interrupt thread and return
                        // handle recursive interruption as well because interruption can be encapsulated in ExecutionException.
                        if(ExceptionProcessor.isCausedByInterruption(ex)) {
                            Thread.currentThread().interrupt();
                            return;
                        }
                        ExceptionPrinter.printHistory("Could not abort " + this + " in order to reject it.", ex, LOGGER);
                    }
                }
            }

            // if not already finished then we force the state.
            if (!isDone() && getActionState() != State.CANCELING) {
                updateActionState(State.REJECTED);
            }
        } finally {
            actionDescriptionBuilderLock.writeLock().unlock();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void finish() {

        // cancel action task
        cancelActionTask();

        actionDescriptionBuilderLock.writeLock().lock();
        try {

            // if not already finished then we force the state.
            if (!isDone() && getActionState() != State.CANCELING) {
                updateActionState(State.FINISHED);
            }
        } finally {
            actionDescriptionBuilderLock.writeLock().unlock();
        }
    }

    private void cancelActionTask() {

        if (actionDescriptionBuilderLock.isWriteLockedByCurrentThread()) {
            new FatalImplementationErrorException("Any thead that cancels the action task should not hold the builder lock, because the lock is required to guarantee a proper task shutdown.", this);
        }

        // finalize if still running
        if (!isActionTaskFinish()) {
            // try to speedup cancellation by forcing the thread by interruption if this is working, cleanup the following disabled code:
            //            // try a smooth finishing if not already failed.
            //            actionTask.cancel(getActionState() == State.SUBMISSION_FAILED);
            //
            //            try {
            //                waitForActionTaskFinalization(1000);
            //            } catch (InterruptedException e) {
            //                Thread.currentThread().interrupt();
            //            }
            //
            //            // check if finished yet
            //            if (!isActionTaskFinish()) {
            //                LOGGER.warn("Execution of " + this + " can not be finished smoothly! Force finalization...");
            actionTask.cancel(true);
            //            }

            try {
                waitForActionTaskFinalization(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            } catch (TimeoutException ex) {
                // timeout
            }

            // check if finished after force
            if (!isActionTaskFinish()) {
                LOGGER.error("Can not finalize " + this + " it seems the execution has stuck.");
                StackTracePrinter.printStackTrace(LOGGER, LogLevel.WARN);
            }
        }
    }

    private void updateActionStateIfNotCanceled(final ActionState.State state) throws InterruptedException {
        synchronized (actionTaskLock) {
            if (actionTask.isCancelled()) {
                throw new InterruptedException();
            }
        }
        updateActionState(state);
    }

    private void updateActionState(final ActionState.State state) {

        actionDescriptionBuilderLock.writeLock().lock();
        try {

            // duplicated state confirmation should be ok to simplify the code, but than skip the update.
            if (getActionState() == state) {
                return;
            }

            // check duplicated termination
            if (isDone()) {
                LOGGER.warn("Can not change the state to {} of an already {} action!", state.name(), actionDescriptionBuilder.getActionState().getValue().name().toLowerCase());
                StackTracePrinter.printStackTrace(LOGGER, LogLevel.WARN);
                return;
            }

            // validate transition
            try {
                validateStateTransition(state);
            } catch (InvalidStateException ex) {
                StackTracePrinter.printStackTrace(LOGGER);
                if (JPService.testMode()) {
                    new FatalImplementationErrorException("Found illegal state transition!", this, ex);
                } else {
                    ExceptionPrinter.printHistory("Found illegal state transition!", ex, LOGGER, LogLevel.ERROR);
                }
            }

            // handle some special cases
            switch (state) {
                case EXECUTING:
                    // inform about execution
                    LOGGER.info(MultiLanguageTextProcessor.getBestMatch(actionDescriptionBuilder.getDescription(), this + " State[" + state.name() + "]"));
                    break;

                case ABORTING:
                case CANCELING:
                    // mark action task already as canceled, to make sure the task is not
                    // updating any further action states which would otherwise introduce invalid state transitions.
                    synchronized (actionTaskLock) {
                        if (!isActionTaskFinish()) {
                            actionTask.cancel(false);
                        }
                    }
                    break;
            }

            // print update in debug mode
            if (JPService.debugMode()) {
                LOGGER.info("State[" + state.name() + "] " + this);
                //StackTracePrinter.printStackTrace(LOGGER, LogLevel.INFO);
            }

            // perform the update
            actionDescriptionBuilder.getActionStateBuilder().setValue(state);
            try {
                ServiceStateProcessor.updateLatestValueOccurrence(state.getValueDescriptor(), TimestampProcessor.getCurrentTimestamp(), actionDescriptionBuilder.getActionStateBuilder());
            } catch (CouldNotPerformException ex) {
                ExceptionPrinter.printHistory(ex, LOGGER);
            }

            // setup termination time if needed
            if (isDone()) {
                actionDescriptionBuilder.setTerminationTimestamp(TimestampProcessor.getCurrentTimestamp());
            }

        } finally {
            actionDescriptionBuilderLock.writeLock().unlock();
        }

        // notify about state change to wakeup all wait methods.
        synchronized (executionStateChangeSync) {
            executionStateChangeSync.notifyAll();
        }

        // make sure that state changes to finishing states, scheduled and executing always trigger a notification
        if (isNotifiedActionState(state)) {
            unit.notifyScheduledActionList();
        }
    }

    private void validateStateTransition(final ActionState.State state) throws InvalidStateException {
        // validate state transition
        switch (getActionState()) {
            case INITIALIZED:
                switch (state) {
                    case SCHEDULED:
                    case INITIATING:
                    case REJECTED: // this is a special case were the action is rejected before it is initiating. This is common during system shutdown and a valid transition because the initialization would not notified anyway.
                        return;
                    default:
                        throw new InvalidStateException("State transition " + getActionState().name() + " -> " + state.name() + " is invalid!");
                }
            case SCHEDULED:
                switch (state) {
                    case INITIATING:
                    case CANCELED:
                    case REJECTED:
                        return;
                    default:
                        throw new InvalidStateException("State transition " + getActionState().name() + " -> " + state.name() + " is invalid!");
                }
            case INITIATING:
                switch (state) {
                    case SUBMISSION:
                    case REJECTED:
                    case ABORTING:
                    case CANCELED:
                        return;
                    default:
                        throw new InvalidStateException("State transition " + getActionState().name() + " -> " + state.name() + " is invalid!");
                }
            case EXECUTING:
                switch (state) {
                    case ABORTING:
                    case CANCELING:
                    case FINISHED:
                        return;
                    default:
                        throw new InvalidStateException("State transition " + getActionState().name() + " -> " + state.name() + " is invalid!");
                }
            case SUBMISSION_FAILED:
                switch (state) {
                    case SUBMISSION:
                    case ABORTING:
                    case CANCELING:
                    case REJECTED:
                        return;
                    default:
                        throw new InvalidStateException("State transition " + getActionState().name() + " -> " + state.name() + " is invalid!");
                }
            case SUBMISSION:
                switch (state) {
                    case SUBMISSION_FAILED:
                    case EXECUTING:
                    case CANCELING:
                    case ABORTING:
                        return;
                    default:
                        throw new InvalidStateException("State transition " + getActionState().name() + " -> " + state.name() + " is invalid!");
                }
            case ABORTING:
                switch (state) {
                    case SCHEDULED:
                    case REJECTED:
                        return;
                    default:
                        throw new InvalidStateException("State transition " + getActionState().name() + " -> " + state.name() + " is invalid!");
                }
            case CANCELING:
                switch (state) {
                    case CANCELED:
                        return;
                    default:
                        throw new InvalidStateException("State transition " + getActionState().name() + " -> " + state.name() + " is invalid!");
                }
            case UNKNOWN:
                switch (state) {
                    case INITIALIZED:
                    case EXECUTING: // required when initializing an already executing action
                        return;
                    default:
                        throw new InvalidStateException("State transition " + getActionState().name() + " -> " + state.name() + " is invalid!");
                }
        }
    }

    @Override
    public Future<ActionDescription> extend() {
        try {
            actionDescriptionBuilderLock.writeLock().lockInterruptibly();
        } catch (InterruptedException ex) {
            return FutureProcessor.canceledFuture(ActionDescription.class, ex);
        }
        try {
            actionDescriptionBuilder.setLastExtensionTimestamp(TimestampProcessor.getCurrentTimestamp());
            return FutureProcessor.completedFuture(actionDescriptionBuilder.build());
        } finally {
            actionDescriptionBuilderLock.writeLock().unlock();
        }
    }

    @Override
    public String toString() {
        return Action.toString(this);
    }
}
