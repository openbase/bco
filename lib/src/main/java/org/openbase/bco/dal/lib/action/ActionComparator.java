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
import org.openbase.jul.pattern.provider.Provider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rst.domotic.action.ActionInitiatorType.ActionInitiator.InitiatorType;
import rst.domotic.state.EmphasisStateType.EmphasisState;

import java.util.Comparator;

/**
 * Comparator can be used to sort action by there ranking.
 *
 * Actions are compared by:
 *
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
     * Compares to actions by its 1.Priority 2.EmphasisCategory and 3.IndividualRanking
     *
     * @param targetAction    the action to rank.
     * @param referenceAction the action to compare with.
     *
     * @return a int value between [-11 vv 11]
     */
    @Override
    public int compare(final Action targetAction, final Action referenceAction) {
        // compute ranking via priority
        // the priority of this action by subtracting the priority of the given action. If this action is initiated by a human it gets an extra point.
        int priority = targetAction.getActionDescription().getPriority().getNumber() + ((targetAction.getActionDescription().getActionInitiator().getInitiatorType() == InitiatorType.HUMAN) ? 1 : 0) - referenceAction.getActionDescription().getPriority().getNumber();

        // if no conflict is detected than just return the priority as ranking
        if (priority != 0) {
            return priority;
        }

        EmphasisState emphasisState;

        try {
            emphasisState = emphasisStateProvider.get();
        } catch (CouldNotPerformException | InterruptedException ex) {

            if (ex instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            // create mockup to guarantee safety operations
            emphasisState = EmphasisState.newBuilder().setEconomy(1d / 3d).setSecurity(1d / 3d).setComfort(1d / 3d).build();
        }

        double emphasis = targetAction.getEmphasisValue(emphasisState) - referenceAction.getEmphasisValue(emphasisState);

        // if no conflict is detected than just return the emphasis as ranking
        if (emphasis != 0) {
            // multiply with 100 to project 0-1 scale to valid in value
            return (int) (emphasis * 100);
        }

        // todo implement individual action scheduling to avoid conflicts.

        LOGGER.warn("Conflict between {} and {} detected!", targetAction, referenceAction);

        return 0;
    }
}
