package org.openbase.bco.manager.device.test.remote.unit;

/*
 * #%L
 * BCO Manager Device Test
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
import java.util.concurrent.TimeUnit;
import org.junit.After;
import org.junit.AfterClass;
import static org.junit.Assert.assertEquals;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.openbase.bco.dal.lib.jp.JPHardwareSimulationMode;
import org.openbase.bco.dal.lib.layer.unit.RollerShutterController;
import org.openbase.bco.dal.remote.unit.RollerShutterRemote;
import org.openbase.bco.manager.device.core.DeviceManagerLauncher;
import org.openbase.bco.registry.mock.MockRegistry;
import org.openbase.bco.registry.mock.MockRegistryHolder;
import org.openbase.jps.core.JPService;
import org.openbase.jps.exception.JPServiceException;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InitializationException;
import org.openbase.jul.exception.InvalidStateException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.pattern.Remote;
import org.slf4j.LoggerFactory;
import rst.domotic.state.BlindStateType.BlindState;

/**
 *
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class RollerShutterRemoteTest {

    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(RollerShutterRemoteTest.class);

    private static RollerShutterRemote rollerShutterRemote;
    private static DeviceManagerLauncher deviceManagerLauncher;
    private static MockRegistry registry;
    private static String label;

    public RollerShutterRemoteTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Throwable {
        try {
            JPService.setupJUnitTestMode();
            JPService.registerProperty(JPHardwareSimulationMode.class, true);
            registry = MockRegistryHolder.newMockRegistry();

            deviceManagerLauncher = new DeviceManagerLauncher();
            deviceManagerLauncher.launch();
            deviceManagerLauncher.getLaunchable().waitForInit(30, TimeUnit.SECONDS);

            label = MockRegistry.ROLLER_SHUTTER_LABEL;

            rollerShutterRemote = new RollerShutterRemote();
            rollerShutterRemote.initByLabel(label);
            rollerShutterRemote.activate();
            rollerShutterRemote.waitForConnectionState(Remote.ConnectionState.CONNECTED);
        } catch (Throwable ex) {
            ExceptionPrinter.printHistoryAndReturnThrowable(ex, LOGGER);
        }
    }

    @AfterClass
    public static void tearDownClass() throws Throwable {
        try {
            if (deviceManagerLauncher != null) {
                deviceManagerLauncher.shutdown();
            }
            if (rollerShutterRemote != null) {
                rollerShutterRemote.shutdown();
            }
            MockRegistryHolder.shutdownMockRegistry();
        } catch (Throwable ex) {
            ExceptionPrinter.printHistoryAndReturnThrowable(ex, LOGGER);
        }
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    /**
     * Test of setShutterState method, of class RollershutterRemote.
     *
     * @throws java.lang.Exception
     */
    @Test(timeout = 10000)
    public void testSetShutterState() throws Exception {
        System.out.println("setShutterState");
        BlindState state = BlindState.newBuilder().setMovementState(BlindState.MovementState.DOWN).build();
        rollerShutterRemote.setBlindState(state).get();
        rollerShutterRemote.requestData().get();
        assertEquals("Shutter has not been set in time!", state, rollerShutterRemote.getData().getBlindState());
    }

    /**
     * Test of getShutterState method, of class RollershutterRemote.
     *
     * @throws java.lang.Exception
     */
    @Test(timeout = 10000)
    public void testGetShutterState() throws Exception {
        System.out.println("getShutterState");
        BlindState state = BlindState.newBuilder().setMovementState(BlindState.MovementState.UP).build();
        ((RollerShutterController) deviceManagerLauncher.getLaunchable().getUnitControllerRegistry().get(rollerShutterRemote.getId())).updateBlindStateProvider(state);
        rollerShutterRemote.requestData().get();
        assertEquals("Shutter has not been set in time!", rollerShutterRemote.getBlindState(), state);
    }

    /**
     * Test of notifyUpdated method, of class RollershutterRemote.
     */
    @Ignore
    public void testNotifyUpdated() {
    }
}
