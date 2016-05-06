package org.dc.bco.manager.user.test.remote.user;

/*
 * #%L
 * COMA DeviceManager Test
 * %%
 * Copyright (C) 2015 - 2016 DivineCooperation
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
import org.dc.jps.core.JPService;
import org.dc.jps.exception.JPServiceException;
import org.dc.bco.dal.lib.jp.JPHardwareSimulationMode;
import org.dc.jul.exception.CouldNotPerformException;
import org.dc.jul.exception.InitializationException;
import org.dc.jul.exception.InstantiationException;
import org.dc.jul.exception.InvalidStateException;
import org.dc.bco.manager.user.core.UserManagerLauncher;
import org.dc.bco.manager.user.remote.UserRemote;
import org.dc.bco.registry.mock.MockRegistry;
import org.dc.bco.registry.mock.MockRegistryHolder;
import org.junit.After;
import org.junit.AfterClass;
import static org.junit.Assert.assertEquals;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.LoggerFactory;
import rst.authorization.UserConfigType.UserConfig;

/**
 *
 * @author thuxohl
 */
public class UserRemoteTest {

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(UserRemoteTest.class);

    private static UserManagerLauncher userManagerLauncher;
    private static UserRemote userRemote;
    private static MockRegistry registry;

    public UserRemoteTest() {
    }

    @BeforeClass
    public static void setUpClass() throws InitializationException, InvalidStateException, InstantiationException, CouldNotPerformException, JPServiceException, InterruptedException {
        JPService.registerProperty(JPHardwareSimulationMode.class, true);
        registry = MockRegistryHolder.newMockRegistry();

        userManagerLauncher = new UserManagerLauncher();
        userManagerLauncher.launch();

        UserConfig userConfig = MockRegistry.testUser;
        userRemote = new UserRemote();
        userRemote.init(userConfig);
        userRemote.activate();
    }

    @AfterClass
    public static void tearDownClass() throws CouldNotPerformException, InterruptedException {
        if (userManagerLauncher != null) {
            userManagerLauncher.shutdown();
        }
        if (userRemote != null) {
            userRemote.shutdown();
        }
        if (registry != null) {
            MockRegistryHolder.shutdownMockRegistry();
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
    @Test
    public void testGetUserName() throws Exception {
        userRemote.requestData().get();
        assertEquals("The user created int he manager has a different user name than the one registered!", MockRegistry.USER_NAME, userRemote.getData().getUserName());
        logger.info("User activity [" + userRemote.getUserActivity() + "]");
        logger.info("User presence [" + userRemote.getUserPresenceState() + "]");
    }
}
