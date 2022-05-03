package org.openbase.bco.dal.test.layer.unit.user;

/*
 * #%L
 * BCO DAL Test
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

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.openbase.bco.dal.remote.layer.unit.Units;
import org.openbase.bco.dal.remote.layer.unit.user.UserRemote;
import org.openbase.bco.registry.mock.MockRegistry;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.type.domotic.state.ActivityMultiStateType.ActivityMultiState;
import org.openbase.type.domotic.state.LocalPositionStateType.LocalPositionState;
import org.openbase.type.domotic.state.PresenceStateType.PresenceState.State;
import org.openbase.type.domotic.state.UserTransitStateType.UserTransitState;
import org.openbase.type.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;
import org.slf4j.LoggerFactory;

/**
 * Integration test of controlling a user controller using a user remote.
 *
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class UserRemoteTest extends AbstractBCOUserManagerTest {

    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(UserRemoteTest.class);

    private static UserRemote userRemote;

    @BeforeAll
    public static void setUpClass() throws Throwable {
        try {
            AbstractBCOUserManagerTest.setUpClass();

            userRemote = Units.getUnit(MockRegistry.testUser, true, UserRemote.class);
        } catch (CouldNotPerformException | InterruptedException ex) {
            throw ExceptionPrinter.printHistoryAndReturnThrowable(ex, LOGGER);
        }
    }

    /**
     * Test of getUsername method, of class UserRemote.
     *
     */
    @Test
    @Timeout(10)
    public void testGetUserName() throws Exception {
        System.out.println("testGetUserName");
        userRemote.requestData().get();
        assertEquals("The user created in the test has a different user name than the one registered!", MockRegistry.USER_NAME, userRemote.getUserName());
    }

    /**
     * Test setting the presence state of a user and validate that depending states are updated accordingly.
     *
     * @throws Exception if an error occurs
     */
    @Test
    @Timeout(10)
    public void testSetPresenceState() throws Exception {
        System.out.println("testSetPresenceState");

        waitForExecution(userRemote.setPresenceState(State.PRESENT));
        assertEquals(State.PRESENT, userRemote.getPresenceState().getValue(), "User presence state has not updated as expected");
        assertTrue(userRemote.getLocalPositionState().getLocationIdList().contains(Registries.getUnitRegistry().getRootLocationConfig().getId()),
                "Local position state has not updated as expected");

        waitForExecution(userRemote.setPresenceState(State.ABSENT));
        assertEquals(State.ABSENT, userRemote.getPresenceState().getValue(), "User presence state has not updated as expected");
        assertTrue(userRemote.getLocalPositionState().getLocationIdList().isEmpty(), "Local position state has not updated as expected");
    }

    /**
     * Test setting the multi activity state of a user.
     *
     * @throws Exception if an error occurs
     */
    @Test
    @Timeout(10)
    public void testSetMultiActivityState() throws Exception {
        System.out.println("testSetMultiActivityState");

        final String activityId = MockRegistry.TEST_ACTIVITY_ID;
        final ActivityMultiState activityMultiState = ActivityMultiState.newBuilder().addActivityId(activityId).build();

        // test setting an activity
        waitForExecution(userRemote.setActivityMultiState(activityMultiState));
        assertTrue(userRemote.getActivityMultiState().getActivityIdList().contains(activityId), "Activity multi state does not contain the expected activity id");
        assertEquals(1, userRemote.getActivityMultiState().getActivityIdCount(), "User performs an unexpected number of activities");

        // test if duplicates will be removed, so nothing should change doing this
        waitForExecution(userRemote.addActivityState(activityId));
        assertTrue(userRemote.getActivityMultiState().getActivityIdList().contains(activityId), "Activity multi state does not contain the expected activity id");
        assertEquals(1, userRemote.getActivityMultiState().getActivityIdCount(), "User performs an unexpected number of activities");

        // test removing the activity
        waitForExecution(userRemote.removeActivityState(activityId));
        assertFalse(userRemote.getActivityMultiState().getActivityIdList().contains(activityId), "Activity multi state does contains an unexpected activity id");
        assertEquals(0, userRemote.getActivityMultiState().getActivityIdCount(), "User performs more activities than expected");
    }

    /**
     * Test setting the user transit state and validate that depending states are updated accordingly.
     *
     * @throws Exception if an error occurs
     */
    @Test
    @Timeout(10)
    public void testUserTransitState() throws Exception {
        System.out.println("testUserTransitState");

        waitForExecution(userRemote.setUserTransitState(UserTransitState.State.LONG_TERM_ABSENT));
        assertEquals(UserTransitState.State.LONG_TERM_ABSENT, userRemote.getUserTransitState().getValue(), "User transit state has not updated as expected");
        assertEquals(State.ABSENT, userRemote.getPresenceState().getValue(), "User presence state has not updated as expected");
        assertTrue(userRemote.getLocalPositionState().getLocationIdList().isEmpty(), "Local position state has not updated as expected");

        waitForExecution(userRemote.setUserTransitState(UserTransitState.State.SOON_ABSENT));
        assertEquals(UserTransitState.State.SOON_ABSENT, userRemote.getUserTransitState().getValue(), "User transit state has not updated as expected");
        assertEquals(State.PRESENT, userRemote.getPresenceState().getValue(), "User presence state has not updated as expected");
        assertTrue(userRemote.getLocalPositionState().getLocationIdList().contains(Registries.getUnitRegistry().getRootLocationConfig().getId()),
                "Local position state has not updated as expected");
    }

    /**
     * Test setting the local position state and validate that depending states are updated accordingly.
     *
     * @throws Exception if an error occurs
     */
    @Test
    @Timeout(10)
    public void testLocalPositionState() throws Exception {
        System.out.println("testLocalPositionState");

        LocalPositionState localPositionState;
        // create state with random location
        localPositionState = LocalPositionState.newBuilder().addLocationId(Registries.getUnitRegistry().getUnitConfigsByUnitType(UnitType.LOCATION).get(0).getId()).build();
        waitForExecution(userRemote.setLocalPositionState(localPositionState));
        assertTrue(userRemote.getLocalPositionState().getLocationIdList().contains(localPositionState.getLocationId(0)),
                "Local position state has not updated as expected");
        assertEquals(State.PRESENT, userRemote.getPresenceState().getValue(), "User presence state has not updated as expected");

        // create state without location
        localPositionState = localPositionState.toBuilder().clearLocationId().build();
        waitForExecution(userRemote.setLocalPositionState(localPositionState));
        assertTrue(userRemote.getLocalPositionState().getLocationIdList().isEmpty(), "Local position state has not updated as expected");
        assertEquals(State.ABSENT, userRemote.getPresenceState().getValue(), "User presence state has not updated as expected");
    }
}
