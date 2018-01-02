package org.openbase.bco.manager.user.test.remote.user;

/*
 * #%L
 * BCO Manager User Test
 * %%
 * Copyright (C) 2015 - 2018 openbase.org
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */
import org.junit.After;
import static org.junit.Assert.assertEquals;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openbase.bco.dal.remote.unit.Units;
import org.openbase.bco.dal.remote.unit.user.UserRemote;
import org.openbase.bco.registry.mock.MockRegistry;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InitializationException;
import org.openbase.jul.exception.InvalidStateException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.slf4j.LoggerFactory;
import rst.domotic.state.UserActivityStateType.UserActivityState;
import rst.domotic.state.UserPresenceStateType.UserPresenceState;

/**
 *
 * * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class UserRemoteTest extends AbstractBCOUserManagerTest {

    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(UserRemoteTest.class);

    private static UserRemote userRemote;

    public UserRemoteTest() {

    }

    @BeforeClass
    public static void setUpClass() throws Throwable {
        try {
            AbstractBCOUserManagerTest.setUpClass();

            userRemote = Units.getUnit(MockRegistry.testUser, true, UserRemote.class);
        } catch (CouldNotPerformException | InterruptedException ex) {
            throw ExceptionPrinter.printHistoryAndReturnThrowable(ex, LOGGER);
        }
    }

    @Before
    public void setUp() throws InitializationException, InvalidStateException {

    }

    @After
    public void tearDown() throws CouldNotPerformException {

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
        assertEquals("The user created in the manager has a different user name than the one registered!", MockRegistry.USER_NAME, userRemote.getData().getUserName());
    }

    @Test(timeout = 10000)
    public void testSetUserValues() throws Exception {
        System.out.println("testSetUserValues");

        //TODO: this has to be changed to use real ids
        String activityId = "cooking";
        UserActivityState activity = UserActivityState.newBuilder().setActivityId(activityId).build();
        UserPresenceState presenceState = UserPresenceState.newBuilder().setValue(UserPresenceState.State.AT_HOME).build();

        userRemote.setUserActivityState(activity).get();
        userRemote.setUserPresenceState(presenceState).get();

        userRemote.requestData().get();

        assertEquals("UserActivityState has not been set!", activityId, userRemote.getUserActivityState().getActivityId());
        assertEquals("UserPresenceState has not been set!", presenceState.getValue(), userRemote.getUserPresenceState().getValue());
    }
}
