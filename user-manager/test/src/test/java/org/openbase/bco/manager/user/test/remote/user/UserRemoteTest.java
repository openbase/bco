package org.openbase.bco.manager.user.test.remote.user;

/*
 * #%L
 * BCO Manager User Test
 * %%
 * Copyright (C) 2015 - 2017 openbase.org
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
import org.openbase.jps.core.JPService;
import org.openbase.bco.dal.lib.jp.JPHardwareSimulationMode;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InitializationException;
import org.openbase.jul.exception.InvalidStateException;
import org.openbase.bco.manager.user.core.UserManagerLauncher;
import org.openbase.bco.dal.remote.unit.user.UserRemote;
import org.openbase.bco.registry.mock.MockRegistry;
import org.openbase.bco.registry.mock.MockRegistryHolder;
import org.junit.After;
import org.junit.AfterClass;
import static org.junit.Assert.assertEquals;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.slf4j.LoggerFactory;
import rst.domotic.state.UserActivityStateType.UserActivityState;
import rst.domotic.state.UserActivityStateType.UserActivityState.Activity;
import rst.domotic.state.UserPresenceStateType.UserPresenceState;
import rst.domotic.unit.UnitConfigType.UnitConfig;

/**
 *
 * * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class UserRemoteTest {

    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(UserRemoteTest.class);

    private static UserManagerLauncher userManagerLauncher;
    private static UserRemote userRemote;
    private static MockRegistry registry;

    public UserRemoteTest() {

    }

    @BeforeClass
    public static void setUpClass() throws Throwable {
        try {
            JPService.setupJUnitTestMode();
            JPService.registerProperty(JPHardwareSimulationMode.class, true);
            registry = MockRegistryHolder.newMockRegistry();

            userManagerLauncher = new UserManagerLauncher();
            userManagerLauncher.launch();

            UnitConfig unitConfig = MockRegistry.testUser;
            userRemote = new UserRemote();
            userRemote.init(unitConfig);
            userRemote.activate();
        } catch (Throwable ex) {
            throw ExceptionPrinter.printHistoryAndReturnThrowable(ex, LOGGER);
        }
    }

    @AfterClass
    public static void tearDownClass() throws Throwable {
        try {
            if (userManagerLauncher != null) {
                userManagerLauncher.shutdown();
            }
            if (userRemote != null) {
                userRemote.shutdown();
            }
            MockRegistryHolder.shutdownMockRegistry();
        } catch (Throwable ex) {
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

        UserActivityState activity = UserActivityState.newBuilder().setCurrentActivity(Activity.EATING).setLastActivity(Activity.COOKING).setNextActivity(Activity.RELAXING).build();
        UserPresenceState presenceState = UserPresenceState.newBuilder().setValue(UserPresenceState.State.AT_HOME).build();

        userRemote.setUserActivityState(activity).get();
        userRemote.setUserPresenceState(presenceState).get();

        userRemote.requestData().get();

        assertEquals("UserActivityState has not been set!", activity, userRemote.getUserActivityState());
        assertEquals("UserPresenceState has not been set!", presenceState, userRemote.getUserPresenceState());
    }
}
