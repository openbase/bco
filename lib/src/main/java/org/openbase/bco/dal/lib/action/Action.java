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
import org.openbase.jul.iface.Executable;
import org.openbase.jul.iface.Identifiable;
import org.openbase.jul.iface.Initializable;
import rst.domotic.action.ActionDescriptionType.ActionDescription;
import rst.domotic.action.ActionFutureType.ActionFuture;
import rst.domotic.action.ActionInitiatorType.ActionInitiator.InitiatorType;
import rst.domotic.state.ActionStateType.ActionState;

import java.io.Serializable;
import java.util.Comparator;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.ToIntFunction;

/**
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public interface Action extends Initializable<ActionDescription>, Executable<ActionFuture>, Identifiable<String>, Comparable<Action> {

    @Override
    default String getId() {
        return getActionDescription().getId();
    }

    /**
     * Method returns the action description of this action.
     *
     * @return the action description of this action.
     */
    ActionDescription getActionDescription();

    default int getRanking() {
        return 0;
    }

    default long getExecutionTimePeriod(final TimeUnit timeUnit) {
        return timeUnit.convert(getActionDescription().getExecutionTimePeriod(), TimeUnit.MICROSECONDS);
    }

    /**
     * Time passed since this action was initialized.
     * @return time in milliseconds.
     */
    default long getLifetime() {
        return System.currentTimeMillis() - getCreationTime();
    }

    /**
     * Time left until the execution time has passed.
     * @return time in milliseconds.
     */
    default long getExecutionTime() {
        return Math.max(getExecutionTimePeriod(TimeUnit.MILLISECONDS) - getLifetime(), 0);
    }

    /**
     * Returns true if there is still some execution time left.
     * @return true execution time is not zero.
     */
    default boolean hasExecutionTimeLeft() {
        return getExecutionTime() > 0;
    }

    /**
     * Time when this action was created.
     * @return time in milliseconds.
     */
    long getCreationTime();

    /**
     * Check if this action is still valid which means there is still some execution time left.
     * @return true if this action is still valid, otherwise false.
     */
    default boolean isValid() {

        // is valid if never executed and no valid
        if (getExecutionTimePeriod(TimeUnit.MILLISECONDS) == 0 && !isDone()) {
            return true;
        }

        // is valid if some execution time is still left.
        return hasExecutionTimeLeft();
    }

    default boolean isDone() {
        switch (getActionState()) {
            case ABORTED:
            case EXECUTION_FAILED:
            case REJECTED:
            case FINISHED:
                return true;
        }
        return false;
    }

    /**
     * Return the current state of this action.
     * @return the action state
     */
    default ActionState.State getActionState() {
        return getActionDescription().getActionState().getValue();
    }

    void cancel();

    void schedule();

    ActionFuture getActionFuture();

    void waitUntilFinish() throws InterruptedException;

    @Override
    default int compareTo(Action o) {
        // compute ranking via priority
        // the priority of this action by subtracting the priority of the given action. If this action is initiated by a human it gets an extra point.
        int priority = getActionDescription().getPriority().getNumber() + ((getActionDescription().getActionInitiator().getInitiatorType() == InitiatorType.HUMAN) ? 1 : 0) - o.getActionDescription().getPriority().getNumber();

        // if no conflict is detected than just return the priority as ranking
//        if (priority != 0) {
            return priority;
//        }

//        // compute ranking by emphasis
//        ActionDescription.
//        return 0;
    }
}
