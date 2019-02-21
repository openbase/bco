package org.openbase.bco.dal.lib.action;

/*-
 * #%L
 * BCO DAL Library
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

import org.openbase.bco.dal.lib.layer.service.provider.EmphasisStateProviderService;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.exception.printer.LogLevel;
import org.openbase.jul.iface.Executable;
import org.openbase.jul.iface.Identifiable;
import org.openbase.type.domotic.action.ActionDescriptionType.ActionDescription;
import org.openbase.type.domotic.action.ActionEmphasisType.ActionEmphasis.Category;
import org.openbase.type.domotic.state.ActionStateType.ActionState;
import org.openbase.type.domotic.state.ActionStateType.ActionState.State;
import org.openbase.type.domotic.state.EmphasisStateType.EmphasisState;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public interface Action extends Executable<ActionDescription>, Identifiable<String> {

    String TYPE_FIELD_NAME_ACTION = "action";

    @Override
    default String getId() throws NotAvailableException {
        return getActionDescription().getId();
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
     * Time passed since this action was initialized.
     *
     * @return time in milliseconds.
     */
    default long getLifetime() throws NotAvailableException {
        return Math.min(System.currentTimeMillis() - getCreationTime(), getExecutionTimePeriod(TimeUnit.MILLISECONDS));
    }

    /**
     * Time left until the execution time has passed and the action expires.
     *
     * @return time in milliseconds.
     */
    default long getExecutionTime() throws NotAvailableException {
        return Math.max(getExecutionTimePeriod(TimeUnit.MILLISECONDS) - getLifetime(), 0);
    }

    /**
     * Returns false if there is still some execution time left.
     * <p>
     * Note: An action without zero execution time period will at least executed once. Such an action has no execution time but is still not expired as long as it has been executed.
     *
     * @return true if the execution time is already expired.
     */
    default boolean isExpired() {
        try {
            // make sure action is at least executed once if no execution time is offered.
            if (getExecutionTimePeriod(TimeUnit.MILLISECONDS) == 0) {
                switch (getActionState()) {
                    case UNKNOWN:
                    case INITIALIZED:
                    case INITIATING:
                    case EXECUTING:
                    case EXECUTION_FAILED:
                        return false;
                    default:
                        return true;
                }

            }
            return getExecutionTime() == 0;
        } catch (NotAvailableException ex) {
            return false;
        }
    }

    /**
     * Time when this action was created.
     *
     * @return time in milliseconds.
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
            case ABORTED:
            case EXECUTION_FAILED:
            case REJECTED:
            case FINISHED:
                return false;
            case ABORTING:
            case INITIATING:
            case EXECUTING:
            case SCHEDULED:
            case ABORTING_FAILED:
            case FINISHING:
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
            case EXECUTION_FAILED:
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
     * Method return a string representation of the given action instance.
     *
     * @param action the action to represent as string.
     *
     * @return a string representation of the given action.
     */
    static String toString(final Action action) {
        try {
            return action.getClass().getSimpleName() + "[" + action.getId() + "|" + action.getActionDescription().getServiceStateDescription().getServiceType() + "|" + action.getActionDescription().getServiceStateDescription().getServiceState() + "|" + action.getActionDescription().getServiceStateDescription().getUnitId() + "|" + ActionDescriptionProcessor.getInitialInitiator(action.getActionDescription()).getInitiatorId() + "]";
        } catch (NotAvailableException e) {
            try {
                return action.getClass().getSimpleName() + "[" + action.getId() + "]";
            } catch (NotAvailableException ex) {
                return action.getClass() + "[NotInitialized]";
            }
        }
    }
}
