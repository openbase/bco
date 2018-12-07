package org.openbase.bco.dal.lib.action;

/*-
 * #%L
 * BCO DAL Library
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

import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.exception.printer.LogLevel;
import org.openbase.jul.iface.Executable;
import org.openbase.jul.iface.Identifiable;
import org.openbase.jul.processing.StringProcessor;
import org.slf4j.LoggerFactory;
import rst.domotic.action.ActionDescriptionType.ActionDescription;
import rst.domotic.action.ActionEmphasisType.ActionEmphasis.Category;
import rst.domotic.state.ActionStateType.ActionState;
import rst.domotic.state.EmphasisStateType.EmphasisState;

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

    default long getExecutionTimePeriod(final TimeUnit timeUnit) throws NotAvailableException {
        return timeUnit.convert(getActionDescription().getExecutionTimePeriod(), TimeUnit.MICROSECONDS);
    }

    /**
     * Time passed since this action was initialized.
     *
     * @return time in milliseconds.
     */
    default long getLifetime() throws NotAvailableException {
        return System.currentTimeMillis() - getCreationTime();
    }

    /**
     * Time left until the execution time has passed.
     *
     * @return time in milliseconds.
     */
    default long getExecutionTime() throws NotAvailableException {
        return Math.max(getExecutionTimePeriod(TimeUnit.MILLISECONDS) - getLifetime(), 0);
    }

    /**
     * Returns true if there is still some execution time left.
     *
     * @return true execution time is not zero.
     */
    default boolean hasExecutionTimeLeft() {
        try {
            return getExecutionTime() > 0;
        } catch (NotAvailableException ex) {
            ExceptionPrinter.printHistory("Could not check execution time!", ex, LoggerFactory.getLogger(Action.class), LogLevel.WARN);
            return false;
        }
    }

    /**
     * Time when this action was created.
     *
     * @return time in milliseconds.
     */
    default long getCreationTime() throws NotAvailableException {
        return getActionDescription().getTimestamp().getTime();
    }

    /**
     * Check if this action is still valid which means there is still some execution time left or it is valid to execute.
     *
     * @return true if this action is still valid, otherwise false.
     */
    default boolean isValid() {

        // is valid if never executed and no valid
        try {
            if (getExecutionTimePeriod(TimeUnit.MILLISECONDS) == 0 && !isDone()) {
                return true;
            }
        } catch (NotAvailableException e) {
            // not valid because of missing information.
            return false;
        }

        // is valid if some execution time is still left.
        return hasExecutionTimeLeft();
    }

    /**
     * Check if this action has been executed and reached a termination state.
     *
     * @return true if executed and terminated.
     */
    default boolean isDone() {
        try {
            switch (getActionState()) {
                case CANCELED:
                case REJECTED:
                case FINISHED:
                    return true;
            }
        } catch (NotAvailableException ex) {
            // not done if available
        }
        return false;
    }

    /**
     * Check if the action is currently scheduled.
     *
     * @return true if running and false if never started, currently executing or already terminated.
     */
    default boolean isScheduled() {
        try {
            switch (getActionState()) {
                case SCHEDULED:
                    return true;
            }
        } catch (NotAvailableException ex) {
            // not done if available
        }
        return false;
    }

    /**
     * Check if the action is currently executing or scheduled.
     *
     * @return true if running and false if never started or already terminated.
     */
    default boolean isRunning() {
        try {
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
        } catch (NotAvailableException ex) {
            // not done if available
        }
        return false;
    }

    /**
     * Return the current state of this action.
     *
     * @return the action state
     */
    default ActionState.State getActionState() throws NotAvailableException {
        return getActionDescription().getActionState().getValue();
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
                    // because {@code emphasisValue} is max 1.0 we add 10 to force the safety category.
                    emphasisValue = 10;
                    break;
            }
        }
        return emphasisValue;
    }

    static String toString(final Action action) {
        try {
            return action.getClass().getSimpleName() + "[" + action.getId() + "|" + action.getActionDescription().getServiceStateDescription().getServiceType() + "|" + action.getActionDescription().getServiceStateDescription().getServiceAttribute() + "|" + action.getActionDescription().getServiceStateDescription().getUnitId() + "]";
        } catch (NotAvailableException e) {
            try {
                return action.getClass().getSimpleName() + "[" + action.getId() + "]";
            } catch (NotAvailableException ex) {
                return action.getClass() + "[NotInitialized]";
            }
        }
    }
}
