package org.openbase.bco.dal.lib.action;

import org.junit.Test;
import org.openbase.jul.extension.rst.processing.TimestampProcessor;
import rst.domotic.action.ActionDescriptionType.ActionDescription;
import rst.domotic.action.ActionEmphasisType.ActionEmphasis.Category;
import rst.domotic.action.ActionInitiatorType.ActionInitiator.InitiatorType;
import rst.domotic.action.ActionPriorityType.ActionPriority.Priority;
import rst.domotic.state.EmphasisStateType.EmphasisState;

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
        public void waitUntilFinish() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Future<ActionDescription> execute() {
            throw new UnsupportedOperationException();
        }
    }
}
