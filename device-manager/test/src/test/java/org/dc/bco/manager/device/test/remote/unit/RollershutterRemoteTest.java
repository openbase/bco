package org.dc.bco.manager.device.test.remote.unit;

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
import org.dc.bco.dal.lib.data.Location;
import org.dc.bco.dal.lib.layer.unit.RollershutterController;
import org.dc.bco.registry.mock.MockRegistryHolder;
import org.dc.jps.core.JPService;
import org.dc.bco.dal.lib.jp.JPHardwareSimulationMode;
import org.dc.bco.dal.remote.unit.RollershutterRemote;
import org.dc.bco.manager.device.core.DeviceManagerLauncher;
import org.dc.bco.registry.mock.MockRegistry;
import org.dc.jul.exception.CouldNotPerformException;
import org.dc.jul.exception.InitializationException;
import org.dc.jul.exception.InvalidStateException;
import org.dc.jul.pattern.Remote;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.Ignore;
import org.slf4j.LoggerFactory;
import rst.homeautomation.state.ShutterStateType.ShutterState;

/**
 *
 * @author thuxohl
 */
public class RollershutterRemoteTest {

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(RollershutterRemoteTest.class);

    private static RollershutterRemote rollershutterRemote;
    private static DeviceManagerLauncher deviceManagerLauncher;
    private static MockRegistry registry;
    private static Location location;
    private static String label;

    public RollershutterRemoteTest() {
    }

    @BeforeClass
    public static void setUpClass() throws InitializationException, InvalidStateException, org.dc.jul.exception.InstantiationException, CouldNotPerformException, InterruptedException {
        JPService.registerProperty(JPHardwareSimulationMode.class, true);
        registry = MockRegistryHolder.newMockRegistry();

        deviceManagerLauncher = new DeviceManagerLauncher();
        deviceManagerLauncher.launch();

        location = new Location(registry.getLocation());
        label = MockRegistry.ROLLERSHUTTER_LABEL;

        rollershutterRemote = new RollershutterRemote();
        rollershutterRemote.init(label, location);
        rollershutterRemote.activate();
    }

    @AfterClass
    public static void tearDownClass() throws CouldNotPerformException, InterruptedException {
        if (deviceManagerLauncher != null) {
            deviceManagerLauncher.shutdown();
        }
        if (rollershutterRemote != null) {
            rollershutterRemote.shutdown();
        }
        if (registry != null) {
            MockRegistryHolder.shutdownMockRegistry();
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
    @Test(timeout = 60000)
    public void testSetShutterState() throws Exception {
        System.out.println("setShutterState");
        ShutterState state = ShutterState.newBuilder().setValue(ShutterState.State.DOWN).build();
        rollershutterRemote.setShutter(state).get();
        rollershutterRemote.requestData().get();
        assertEquals("Shutter has not been set in time!", state, rollershutterRemote.getData().getShutterState());
    }

    /**
     * Test of getShutterState method, of class RollershutterRemote.
     *
     * @throws java.lang.Exception
     */
    @Test(timeout = 60000)
    public void testGetShutterState() throws Exception {
        System.out.println("getShutterState");
        ShutterState state = ShutterState.newBuilder().setValue(ShutterState.State.STOP).build();
        rollershutterRemote.waitForConnectionState(Remote.RemoteConnectionState.CONNECTED);
        ((RollershutterController) deviceManagerLauncher.getDeviceManager().getUnitControllerRegistry().get(rollershutterRemote.getId())).updateShutter(state);
        rollershutterRemote.requestData().get();
        assertEquals("Shutter has not been set in time!", rollershutterRemote.getShutter(), state);
    }

    /**
     * Test of setOpeningRatio method, of class RollershutterRemote.
     *
     * @throws java.lang.Exception
     */
    @Test(timeout = 60000)
    public void testSetOpeningRatio() throws Exception {
        System.out.println("setOpeningRatio");
        double openingRatio = 34.0D;
        rollershutterRemote.setOpeningRatio(openingRatio).get();
        rollershutterRemote.requestData().get();
        assertEquals("Opening ration has not been set in time!", openingRatio, rollershutterRemote.getData().getOpeningRatio(), 0.1);
    }

    /**
     * Test of setOpeningRatio method, of class RollershutterRemote.
     *
     * @throws java.lang.Exception
     */
    @Test(timeout = 60000)
    public void testGetOpeningRatio() throws Exception {
        System.out.println("getOpeningRatio");
        Double openingRatio = 70.0D;
        rollershutterRemote.waitForConnectionState(Remote.RemoteConnectionState.CONNECTED);
        ((RollershutterController) deviceManagerLauncher.getDeviceManager().getUnitControllerRegistry().get(rollershutterRemote.getId())).updateOpeningRatio(openingRatio);
        rollershutterRemote.requestData().get();
        assertEquals("Opening ration has not been set in time!", openingRatio, rollershutterRemote.getOpeningRatio());
    }

    /**
     * Test of notifyUpdated method, of class RollershutterRemote.
     */
    @Ignore
    public void testNotifyUpdated() {
    }
}
