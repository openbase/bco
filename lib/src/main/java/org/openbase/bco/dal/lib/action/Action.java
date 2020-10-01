package org.openbase.bco.dal.lib.action;

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

import org.openbase.jps.core.JPService;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InvalidStateException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.extension.type.processing.TimestampJavaTimeTransform;
import org.openbase.jul.iface.Executable;
import org.openbase.jul.iface.Identifiable;
import org.openbase.jul.schedule.Timeout;
import org.openbase.type.domotic.action.ActionDescriptionType.ActionDescription;
import org.openbase.type.domotic.action.ActionEmphasisType.ActionEmphasis.Category;
import org.openbase.type.domotic.action.ActionPriorityType.ActionPriority.Priority;
import org.openbase.type.domotic.state.ActionStateType.ActionState;
import org.openbase.type.domotic.state.ActionStateType.ActionState.State;
import org.openbase.type.domotic.state.EmphasisStateType.EmphasisState;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public interface Action extends Executable<ActionDescription>, Identifiable<String> {

    String TYPE_FIELD_NAME_ACTION = "action";

    /**
     * The max execution time of an action until the action finishes if it was never extended.
     * The time unit is milliseconds.
     */
    long MAX_EXECUTION_TIME_PERIOD = (JPService.testMode() ? TimeUnit.SECONDS.toMillis(5) : TimeUnit.MINUTES.toMillis(30));

    /**
     * Returns the id of this action.
     *
     * @return the identifier as string.
     *
     * @throws NotAvailableException is thrown when the action description is not yet available.
     */
    @Override
    default String getId() throws NotAvailableException {
        return getActionDescription().getActionId();
    }

    /**
     * Method returns the action description of this action.
     *
     * @return the action description of this action.
     *
     * @throws NotAvailableException is thrown if the description is not available.
     */
    ActionDescription getActionDescription() throws NotAvailableException;

    /**
     * Method return the time period which this action should be executed.
     * An action is automatically finished when its lifetime reaches its execution time period.
     *
     * @param timeUnit defines the time unit of the returned execution time period to use.
     *
     * @return the execution time period of this action.
     *
     * @throws NotAvailableException is thrown if the execution time period can not be accessed. This can for example happen,
     *                               if a remote action is not yet fully synchronized and the related action description is not available.
     */
    default long getExecutionTimePeriod(final TimeUnit timeUnit) throws NotAvailableException {
        return timeUnit.convert(getActionDescription().getExecutionTimePeriod(), TimeUnit.MICROSECONDS);
    }

    /**
     * Method return the timestamp of the actions last extension.
     * If the action execution period is larger than {@code Action.MAX_EXECUTION_TIME_PERIOD},
     * and it was never extended (indicated by the last extention value), than the action will be finished.
     *
     * @return the last extension of this action it milliseconds
     *
     * @throws NotAvailableException is thrown if the last extension can not be accessed. This can for example happen,
     *                               if a remote action is not yet fully synchronized and the related action description is not available.
     */
    default long getLastExtensionTime() throws NotAvailableException {
        return getLastExtensionTime(TimeUnit.MILLISECONDS);
    }

    /**
     * Method return the timestamp of the actions last extension.
     * If the action execution period is larger than {@code Action.MAX_EXECUTION_TIME_PERIOD},
     * and it was never extended (indicated by the last extention value), than the action will be finished.
     *
     * @param timeUnit the timeunit used for the return value.
     *
     * @return the last extension of this action it the given time unit.
     *
     * @throws NotAvailableException is thrown if the last extension can not be accessed. This can for example happen,
     *                               if a remote action is not yet fully synchronized and the related action description is not available.
     */
    default long getLastExtensionTime(final TimeUnit timeUnit) throws NotAvailableException {

        // the termination action and action with minimal priority that can be replaced any time, such as hardware sync actions that can not be
        // remapped do not need any further extensions and are therefore valid until infinity.
        if(getActionDescription().getPriority() == Priority.TERMINATION
                || (getActionDescription().getPriority() == Priority.NO && !getActionDescription().getInterruptible() && !getActionDescription().getSchedulable()) ) {
            return System.currentTimeMillis() + Timeout.INFINITY_TIMEOUT;
        }

        if (!getActionDescription().hasLastExtensionTimestamp() || !getActionDescription().getLastExtensionTimestamp().hasTime() || getActionDescription().getLastExtensionTimestamp().getTime() == 0) {
            throw new NotAvailableException("LastExtentionTime", new InvalidStateException(this + " has never been extended!"));
        }

        return timeUnit.convert(getActionDescription().getLastExtensionTimestamp().getTime(), TimeUnit.MICROSECONDS);
    }

    /**
     * Method returns the time until this action gets invalid if never extended.
     *
     * @param timeUnit the timeunit used for the return value.
     *
     * @return the time intervall until this action gets invalid.
     *
     * @throws NotAvailableException is thrown if the validation time could not be computed. This can for example happen,
     *                               if a remote action is not yet fully synchronized and the related action description is not available.
     */
    default long getValidityTime(final TimeUnit timeUnit) throws NotAvailableException {

        long timeSinceStartOrLastExtention;
        try {
            timeSinceStartOrLastExtention = getLastExtensionTime();
        } catch (NotAvailableException ex) {
            timeSinceStartOrLastExtention = getCreationTime();
        }

        final long extensionTimeout = Action.MAX_EXECUTION_TIME_PERIOD - (System.currentTimeMillis() - timeSinceStartOrLastExtention);
        return timeUnit.convert(Math.max(0, Math.min(getExecutionTime(), extensionTimeout)), TimeUnit.MILLISECONDS);
    }

    /**
     * Time passed since this action was initialized.
     *
     * @return time in milliseconds.
     *
     * @throws NotAvailableException is thrown when the action state can not be observed yet.
     */
    default long getLifetime() throws NotAvailableException {

        // when done compute time passed between creation and termination.
        if (isDone()) {
            return getTerminationTime() - getCreationTime();
        }

        // otherwise compute time passed since this action was initialized.
        return Math.min(System.currentTimeMillis() - getCreationTime(), getExecutionTimePeriod(TimeUnit.MILLISECONDS));
    }

    /**
     * The timestamp of the termination of this action.
     *
     * @return time in milliseconds.
     *
     * @throws NotAvailableException is thrown when the action was not yet terminated or its state can not be observed.
     */
    default long getTerminationTime() throws NotAvailableException {
        return TimestampJavaTimeTransform.transform(getActionDescription().getTerminationTimestamp());
    }

    /**
     * Time left until the execution time has passed and the action expires.
     *
     * @return time in milliseconds.
     *
     * @throws NotAvailableException is thrown when the action state can not be observed yet.
     */
    default long getExecutionTime() throws NotAvailableException {
        return Math.max(getExecutionTimePeriod(TimeUnit.MILLISECONDS) - getLifetime(), 0);
    }

    /**
     * Returns false if there is still some execution time left.
     *
     * @return true if the execution time is already expired.
     */
    default boolean isExpired() {
        try {
            return getExecutionTime() == 0 || (System.currentTimeMillis() - getLastExtensionTime() > MAX_EXECUTION_TIME_PERIOD);
        } catch (NotAvailableException ex) {
            return false;
        }
    }

    /**
     * Time when this action was created.
     *
     * @return time in milliseconds.
     *
     * @throws NotAvailableException is thrown when the action state can not be observed yet.
     */
    default long getCreationTime() throws NotAvailableException {
        return TimeUnit.MICROSECONDS.toMillis(getActionDescription().getTimestamp().getTime());
    }

    /**
     * Check if this action is still valid which means there is still some execution time left or it is valid to execute.
     *
     * @return true if this action is still valid, otherwise false.
     */
    default boolean isValid() {

        // if done we are not valid to execute anymore.
        if (isDone()) {
            return false;
        }

        // action is not valid when a cancellation was requested.
        if (getActionState() == State.CANCELING) {
            return false;
        }

        // is valid if not expired yet.
        return !isExpired();
    }

    /**
     * Check if this action has been executed and reached a termination state.
     *
     * @return true if executed and terminated.
     */
    default boolean isDone() {
        switch (getActionState()) {
            case CANCELED:
            case REJECTED:
            case FINISHED:
                return true;
        }
        return false;
    }

    /**
     * Check if the action is currently scheduled.
     *
     * @return true if running and false if never started, currently executing or already terminated.
     */
    default boolean isScheduled() {
        switch (getActionState()) {
            case SCHEDULED:
                return true;
        }
        return false;
    }

    /**
     * Check if the action is currently executing or scheduled.
     *
     * @return true if running and false if never started or already terminated.
     */
    default boolean isRunning() {
        switch (getActionState()) {
            case UNKNOWN:
            case INITIALIZED:
            case REJECTED:
            case FINISHED:
            case CANCELED:
            case CANCELING:
                return false;
            case ABORTING:
            case INITIATING:
            case SUBMISSION:
            case SUBMISSION_FAILED:
            case EXECUTING:
            case SCHEDULED:
                return true;
        }
        return false;
    }

    /**
     * Check if the provided action state is definitely notified. ActionState updates are notified in unit data types.
     * In order to reduce the number of updates per unit, only the most important states are definitely notified.
     *
     * @param actionState the state tested.
     *
     * @return if the action state is notified.
     */
    default boolean isNotifiedActionState(final ActionState.State actionState) {
        switch (actionState) {
            case CANCELED:
            case REJECTED:
            case FINISHED:
            case SCHEDULED:
            case EXECUTING:
            case SUBMISSION:
            case SUBMISSION_FAILED:
                return true;
            default:
                return false;
        }
    }

    /**
     * Check if the action is currently processing which means one the way to be executed or currently executing.
     * <p>
     * Note: A action is processing if it is in one of the following states (INITIATING, EXECUTING, SUBMISSION, SUBMISSION_FAILED).
     *
     * @return true if the action is currently processing, otherwise false.
     */
    default boolean isProcessing() {
        return isProcessing(getActionState());
    }

    /**
     * Check if the action is currently processing which means its on the way to be executed or currently executing.
     * <p>
     * Note: A action is processing if it is in one of the following states (INITIATING, EXECUTING, SUBMISSION, SUBMISSION_FAILED).
     *
     * @param actionState the state tested.
     *
     * @return true if the action is currently processing, otherwise false.
     */
    static boolean isProcessing(final ActionState.State actionState) {
        switch (actionState) {
            case INITIATING:
            case EXECUTING:
            case SUBMISSION:
            case SUBMISSION_FAILED:
                return true;
            default:
                return false;
        }
    }

    /**
     * Return the current state of this action.
     *
     * @return the action state
     */
    default ActionState.State getActionState() {
        try {
            return getActionDescription().getActionState().getValue();
        } catch (NotAvailableException e) {
            return State.UNKNOWN;
        }
    }

    Future<ActionDescription> cancel();

    void waitUntilDone() throws CouldNotPerformException, InterruptedException;

    default double getEmphasisValue(final EmphasisState emphasisState) throws NotAvailableException {
        double emphasisValue = 0;
        for (Category category : getActionDescription().getCategoryList()) {
            switch (category) {
                case ECONOMY:
                    emphasisValue = Math.max(emphasisValue, emphasisState.getEconomy());
                    break;
                case COMFORT:
                    emphasisValue = Math.max(emphasisValue, emphasisState.getComfort());
                    break;
                case SECURITY:
                    emphasisValue = Math.max(emphasisValue, emphasisState.getSecurity());
                    break;
                case SAFETY:
                    // because {@code emphasisValue} is max 1.0 we return 10 to force the safety category.
                    return 10;
            }
        }
        return emphasisValue;
    }

    /**
     * Extend this action to run at most {@link Action#MAX_EXECUTION_TIME_PERIOD} milli seconds longer.
     *
     * @return the updated action description when the future was completed.
     */
    Future<ActionDescription> extend();

    /**
     * Method return a string representation of the given action instance.
     *
     * @param action the action to represent as string.
     *
     * @return a string representation of the given action.
     */
    static String toString(final Action action) {
        try {
            return action.getClass().getSimpleName() + "[" + action.getId() + "|" + action.getActionDescription().getServiceStateDescription().getServiceType() + "|" + action.getActionDescription().getServiceStateDescription().getServiceState() + "|" + action.getActionDescription().getServiceStateDescription().getUnitId() + "|" + ActionDescriptionProcessor.getInitialInitiator(action.getActionDescription()).getInitiatorId() + "(" + ActionDescriptionProcessor.getInitialInitiator(action.getActionDescription()).getInitiatorType().name() + ")|" + action.getActionDescription().getActionState().getValue().name() + "]";
        } catch (NotAvailableException e) {
            try {
                return action.getClass().getSimpleName() + "[" + action.getId() + "]";
            } catch (NotAvailableException ex) {
                return action.getClass() + "[NotInitialized]";
            }
        }
    }
}
