package org.openbase.bco.dal.test.layer.unit.user;

/*
 * #%L
 * BCO DAL Test
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

import org.junit.BeforeClass;
import org.junit.Test;
import org.openbase.bco.dal.remote.action.Actions;
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

import static org.junit.Assert.*;

/**
 * Integration test of controlling a user controller using a user remote.
 *
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class UserRemoteTest extends AbstractBCOUserManagerTest {

    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(UserRemoteTest.class);

    private static UserRemote userRemote;

    @BeforeClass
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
     * @throws java.lang.Exception
     */
    @Test(timeout = 10000)
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
    @Test(timeout = 10000)
    public void testSetPresenceState() throws Exception {
        System.out.println("testSetPresenceState");

        waitForExecution(userRemote.setPresenceState(State.PRESENT));
        assertEquals("User presence state has not updated as expected", State.PRESENT, userRemote.getPresenceState().getValue());
        assertTrue("Local position state has not updated as expected",
                userRemote.getLocalPositionState().getLocationIdList().contains(Registries.getUnitRegistry().getRootLocationConfig().getId()));

        waitForExecution(userRemote.setPresenceState(State.ABSENT));
        assertEquals("User presence state has not updated as expected", State.ABSENT, userRemote.getPresenceState().getValue());
        assertTrue("Local position state has not updated as expected", userRemote.getLocalPositionState().getLocationIdList().isEmpty());
    }

    /**
     * Test setting the multi activity state of a user.
     *
     * @throws Exception if an error occurs
     */
    @Test(timeout = 10000)
    public void testSetMultiActivityState() throws Exception {
        System.out.println("testSetMultiActivityState");

        final String activityId = MockRegistry.TEST_ACTIVITY_ID;
        final ActivityMultiState activityMultiState = ActivityMultiState.newBuilder().addActivityId(activityId).build();

        // test setting an activity
        waitForExecution(userRemote.setActivityMultiState(activityMultiState));
        assertTrue("Activity multi state does not contain the expected activity id", userRemote.getActivityMultiState().getActivityIdList().contains(activityId));
        assertEquals("User performs an unexpected number of activities", 1, userRemote.getActivityMultiState().getActivityIdCount());

        // test if duplicates will be removed, so nothing should change doing this
        waitForExecution(userRemote.addActivityState(activityId));
        assertTrue("Activity multi state does not contain the expected activity id", userRemote.getActivityMultiState().getActivityIdList().contains(activityId));
        assertEquals("User performs an unexpected number of activities", 1, userRemote.getActivityMultiState().getActivityIdCount());

        // test removing the activity
        waitForExecution(userRemote.removeActivityState(activityId));
        assertFalse("Activity multi state does contains an unexpected activity id", userRemote.getActivityMultiState().getActivityIdList().contains(activityId));
        assertEquals("User performs more activities than expected", 0, userRemote.getActivityMultiState().getActivityIdCount());
    }

    /**
     * Test setting the user transit state and validate that depending states are updated accordingly.
     *
     * @throws Exception if an error occurs
     */
    @Test(timeout = 10000)
    public void testUserTransitState() throws Exception {
        System.out.println("testUserTransitState");

        waitForExecution(userRemote.setUserTransitState(UserTransitState.State.LONG_TERM_ABSENT));
        assertEquals("User transit state has not updated as expected", UserTransitState.State.LONG_TERM_ABSENT, userRemote.getUserTransitState().getValue());
        assertEquals("User presence state has not updated as expected", State.ABSENT, userRemote.getPresenceState().getValue());
        assertTrue("Local position state has not updated as expected", userRemote.getLocalPositionState().getLocationIdList().isEmpty());

        waitForExecution(userRemote.setUserTransitState(UserTransitState.State.SOON_ABSENT));
        assertEquals("User transit state has not updated as expected", UserTransitState.State.SOON_ABSENT, userRemote.getUserTransitState().getValue());
        assertEquals("User presence state has not updated as expected", State.PRESENT, userRemote.getPresenceState().getValue());
        assertTrue("Local position state has not updated as expected",
                userRemote.getLocalPositionState().getLocationIdList().contains(Registries.getUnitRegistry().getRootLocationConfig().getId()));
    }

    /**
     * Test setting the local position state and validate that depending states are updated accordingly.
     *
     * @throws Exception if an error occurs
     */
    @Test(timeout = 10000)
    public void testLocalPositionState() throws Exception {
        System.out.println("testLocalPositionState");

        LocalPositionState localPositionState;
        // create state with random location
        localPositionState = LocalPositionState.newBuilder().addLocationId(Registries.getUnitRegistry().getUnitConfigsByUnitType(UnitType.LOCATION).get(0).getId()).build();
        waitForExecution(userRemote.setLocalPositionState(localPositionState));
        assertTrue("Local position state has not updated as expected",
                userRemote.getLocalPositionState().getLocationIdList().contains(localPositionState.getLocationId(0)));
        assertEquals("User presence state has not updated as expected", State.PRESENT, userRemote.getPresenceState().getValue());

        // create state without location
        localPositionState = localPositionState.toBuilder().clearLocationId().build();
        waitForExecution(userRemote.setLocalPositionState(localPositionState));
        assertTrue("Local position state has not updated as expected", userRemote.getLocalPositionState().getLocationIdList().isEmpty());
        assertEquals("User presence state has not updated as expected", State.ABSENT, userRemote.getPresenceState().getValue());
    }
}
