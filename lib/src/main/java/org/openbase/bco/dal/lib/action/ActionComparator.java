package org.openbase.bco.dal.lib.action;

/*-
 * #%L
 * BCO DAL Library
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

import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.pattern.provider.Provider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.openbase.type.domotic.action.ActionInitiatorType.ActionInitiator.InitiatorType;
import org.openbase.type.domotic.state.EmphasisStateType.EmphasisState;

import java.util.Comparator;

/**
 * Comparator can be used to sort action by there ranking. It makes sure that actions with higher priorities are
 * at the beginning of the list after sorting.
 * <p>
 * Actions are compared by:
 * <p>
 * 1.Priority
 * 2.EmphasisCategory
 * 3.IndividualRanking
 */
public class ActionComparator implements Comparator<Action> {

    /**
     * The class logger.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(ActionComparator.class);

    /**
     * The emphasis state offered by the location of the both actions.
     */
    private final Provider<EmphasisState> emphasisStateProvider;

    /**
     * Creates a new action comparator.
     *
     * @param emphasisStateProvider
     */
    public ActionComparator(final Provider<EmphasisState> emphasisStateProvider) {
        this.emphasisStateProvider = emphasisStateProvider;
    }

    /**
     * Compares two actions by its 1.Priority 2.EmphasisCategory and 3.IndividualRanking.
     *
     * @param action1 the first action to compare.
     * @param action2 the second action to compare with.
     *
     * @return a int value between [-11 vv 11]
     */
    @Override
    public int compare(final Action action1, final Action action2) {
        try {
            // make sure that finished actions always end up at the end of the list
            // this is required because actions are not immediately removed when they are done
            // they stay for a while to make sure their finishing state is synced to all remotes
            if (action1.isDone()) {
                if (action2.isDone()) {
                    return 0;
                } else {
                    return 1;
                }
            } else {
                if (action2.isDone()) {
                    return -1;
                }
            }

            // resolve original initiators of actions
            final InitiatorType initiatorType1 = ActionDescriptionProcessor.getInitialInitiator(action1.getActionDescription()).getInitiatorType();
            final InitiatorType initiatorType2 = ActionDescriptionProcessor.getInitialInitiator(action2.getActionDescription()).getInitiatorType();

            // compute ranking via priority
            // the priority of this action by subtracting the priority of the given action. If this action is initiated by a human it gets an extra half-point.
            // it does not get a full extra point so that there are no conflicts between human and system
            double priority1 = action1.getActionDescription().getPriority().getNumber() + ((initiatorType1 == InitiatorType.HUMAN) ? 0.5 : 0);
            double priority2 = action2.getActionDescription().getPriority().getNumber() + ((initiatorType2 == InitiatorType.HUMAN) ? 0.5 : 0);

            // make sure that diff is -1, 0 or 1
            int priority = (int) Math.signum(priority2 - priority1);

            // if no conflict is detected than just return the priority as ranking
            if (priority != 0) {
                return priority;
            }

            // conflict can only exist if both are human or system
            if (initiatorType1 == InitiatorType.SYSTEM) {
                // if both actions are initiated by system try to resolve by emphasis

                EmphasisState emphasisState;

                try {
                    emphasisState = emphasisStateProvider.get();
                } catch (CouldNotPerformException | InterruptedException ex) {

                    if (ex instanceof InterruptedException) {
                        Thread.currentThread().interrupt();
                    }
                    // create mock-up to guarantee safety operations
                    emphasisState = EmphasisState.getDefaultInstance();
                }

                // make sure that diff is -1, 0 or 1
                int emphasis = (int) Math.signum(action2.getEmphasisValue(emphasisState) - action1.getEmphasisValue(emphasisState));

                // if no conflict is detected than just return the emphasis as ranking
                if (emphasis != 0) {
                    return emphasis;
                }

                //TODO: resolve conflict between agents
            }

            // resolve conflicts between humans or systems by prioritizing newer actions
            return (int) Math.signum(action2.getCreationTime() - action1.getCreationTime());
        } catch (CouldNotPerformException ex) {
            ExceptionPrinter.printHistory("Could not compare actions!", ex, LOGGER);
        }
        return -1;
    }
}
