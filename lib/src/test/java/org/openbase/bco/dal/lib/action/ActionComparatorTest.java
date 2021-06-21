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

import org.junit.Test;
import org.openbase.jul.extension.type.processing.TimestampProcessor;
import org.openbase.type.domotic.action.ActionDescriptionType.ActionDescription;
import org.openbase.type.domotic.action.ActionEmphasisType.ActionEmphasis.Category;
import org.openbase.type.domotic.action.ActionInitiatorType.ActionInitiator.InitiatorType;
import org.openbase.type.domotic.action.ActionPriorityType.ActionPriority.Priority;
import org.openbase.type.domotic.state.EmphasisStateType.EmphasisState;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

/**
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class ActionComparatorTest {

    /**
     * Test the action ranking by creating a number of actions, adding them to a list and validating the order after sorting.
     */
    @Test
    public void testActionComparison() {
        final EmphasisState emphasisState = EmphasisState.newBuilder().setEconomy(0.6).setComfort(0.3).setSecurity(0.1).build();
        final ActionComparator actionComparator = new ActionComparator(() -> emphasisState);

        // create actions
        final ActionMockUp humanNormal = new ActionMockUp(InitiatorType.HUMAN, Priority.NORMAL);
        final ActionMockUp humanNormal2 = new ActionMockUp(InitiatorType.HUMAN, Priority.NORMAL);
        final ActionMockUp systemNormal = new ActionMockUp(InitiatorType.SYSTEM, Priority.NORMAL, Category.ECONOMY);
        final ActionMockUp systemNormal2 = new ActionMockUp(InitiatorType.SYSTEM, Priority.NORMAL, Category.COMFORT);

        // test symmetry property of comparison
        assertEquals(Math.signum(actionComparator.compare(systemNormal, humanNormal)), -Math.signum(actionComparator.compare(humanNormal, systemNormal)), 0.1);

        // add actions to list
        final List<ActionMockUp> actionList = new ArrayList<>();
        actionList.add(systemNormal);
        actionList.add(humanNormal2);
        actionList.add(humanNormal);
        actionList.add(systemNormal2);

        // sort list
        actionList.sort(actionComparator);

        // check if the sort order is as expected
        assertSame(actionList.get(0), humanNormal2);
        assertSame(actionList.get(1), humanNormal);
        assertSame(actionList.get(2), systemNormal);
        assertSame(actionList.get(3), systemNormal2);
    }

    /**
     * Mock-up action which allows to create an action description with fields required for comparison.
     */
    private class ActionMockUp implements Action {

        private final ActionDescription actionDescription;

        private ActionMockUp(final InitiatorType initiatorType, final Priority priority) {
            this(initiatorType, priority, Category.ECONOMY);
        }

        private ActionMockUp(final InitiatorType initiatorType, final Priority priority, final Category category) {
            final ActionDescription.Builder builder = ActionDescription.newBuilder();
            builder.setTimestamp(TimestampProcessor.getCurrentTimestamp());
            builder.getActionInitiatorBuilder().setInitiatorType(initiatorType);
            builder.setPriority(priority);
            builder.addCategory(category);

            this.actionDescription = builder.build();
        }

        @Override
        public ActionDescription getActionDescription() {
            return actionDescription;
        }

        @Override
        public Future<ActionDescription> cancel() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void waitUntilDone() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Future<ActionDescription> execute() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Future<ActionDescription> extend() {
            throw new UnsupportedOperationException();
        }
    }
}
