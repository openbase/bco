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
import org.dc.bco.manager.device.core.DeviceManagerLauncher;
import org.dc.bco.dal.lib.data.Location;
import org.dc.bco.dal.lib.layer.unit.MotionSensorController;
import org.dc.bco.registry.mock.MockRegistryHolder;
import org.dc.jps.core.JPService;
import org.dc.bco.dal.lib.jp.JPHardwareSimulationMode;
import org.dc.bco.dal.remote.unit.MotionSensorRemote;
import org.dc.bco.registry.mock.MockRegistry;
import org.dc.jul.exception.CouldNotPerformException;
import org.dc.jul.exception.InitializationException;
import org.dc.jul.exception.InvalidStateException;
import org.dc.jul.pattern.Remote;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.Ignore;
import org.slf4j.LoggerFactory;
import rst.homeautomation.state.MotionStateType.MotionState;

/**
 *
 * @author thuxohl
 */
public class MotionSensorRemoteTest {

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(MotionSensorRemoteTest.class);

    private static MotionSensorRemote motionSensorRemote;
    private static DeviceManagerLauncher deviceManagerLauncher;
    private static MockRegistry registry;
    private static Location location;
    private static String label;

    public MotionSensorRemoteTest() {
    }

    @BeforeClass
    public static void setUpClass() throws InitializationException, InvalidStateException, org.dc.jul.exception.InstantiationException, CouldNotPerformException, InterruptedException {
        JPService.registerProperty(JPHardwareSimulationMode.class, true);
        registry = MockRegistryHolder.newMockRegistry();

        deviceManagerLauncher = new DeviceManagerLauncher();
        deviceManagerLauncher.launch();

        location = new Location(registry.getLocation());
        label = MockRegistry.MOTION_SENSOR_LABEL;

        motionSensorRemote = new MotionSensorRemote();
        motionSensorRemote.init(label, location);
        motionSensorRemote.activate();
    }

    @AfterClass
    public static void tearDownClass() throws CouldNotPerformException, InterruptedException {
        if (deviceManagerLauncher != null) {
            deviceManagerLauncher.shutdown();
        }
        if (motionSensorRemote != null) {
            motionSensorRemote.shutdown();
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
     * Test of notifyUpdated method, of class MotionSenorRemote.
     */
    @Ignore
    public void testNotifyUpdated() {
    }

    /**
     * Test of getMotionState method, of class MotionSenorRemote.
     *
     * @throws java.lang.Exception
     */
    @Test(timeout = 60000)
    public void testGetMotionState() throws Exception {
        System.out.println("getMotionState");
        motionSensorRemote.waitForConnectionState(Remote.RemoteConnectionState.CONNECTED);
        MotionState motion = MotionState.newBuilder().setValue(MotionState.State.MOVEMENT).build();
        ((MotionSensorController) deviceManagerLauncher.getDeviceManager().getUnitControllerRegistry().get(motionSensorRemote.getId())).updateMotion(motion);
        motionSensorRemote.requestData().get();
        Assert.assertEquals("The getter for the motion state returns the wrong value!", motion.getValue(), motionSensorRemote.getMotion().getValue());
    }
}
