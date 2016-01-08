/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dc.bco.manager.device.test.remote.unit;

import org.dc.bco.manager.device.core.DeviceManagerLauncher;
import org.dc.bco.dal.lib.data.Location;
import org.dc.bco.dal.lib.layer.unit.LightController;
import org.dc.bco.registry.mock.MockRegistryHolder;
import org.dc.jps.core.JPService;
import org.dc.bco.dal.lib.jp.JPHardwareSimulationMode;
import org.dc.bco.dal.remote.unit.LightRemote;
import org.dc.bco.registry.mock.MockRegistry;
import org.dc.jul.exception.CouldNotPerformException;
import org.dc.jul.exception.InitializationException;
import org.dc.jul.exception.InvalidStateException;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import static org.junit.Assert.*;
import org.junit.Test;
import org.slf4j.LoggerFactory;
import rst.homeautomation.state.PowerStateType.PowerState;

/**
 *
 * @author thuxohl
 */
public class LightRemoteTest {

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(LightRemoteTest.class);

    private static LightRemote lightRemote;
    private static DeviceManagerLauncher deviceManagerLauncher;
    private static MockRegistry registry;
    private static Location location;
    private static String label;

    public LightRemoteTest() {
    }

    @BeforeClass
    public static void setUpClass() throws InitializationException, InvalidStateException, org.dc.jul.exception.InstantiationException, CouldNotPerformException, InterruptedException {
        JPService.registerProperty(JPHardwareSimulationMode.class, true);
        registry = MockRegistryHolder.newMockRegistry();

        deviceManagerLauncher = new DeviceManagerLauncher();
        deviceManagerLauncher.launch();
        

        location = new Location(registry.getLocation());
        label = MockRegistry.LIGHT_LABEL;

        lightRemote = new LightRemote();
        lightRemote.init(label, location);
        lightRemote.activate();
    }

    @AfterClass
    public static void tearDownClass() throws CouldNotPerformException, InterruptedException {
        if (deviceManagerLauncher != null) {
            deviceManagerLauncher.shutdown();
        }
        if (lightRemote != null) {
            lightRemote.shutdown();
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
     * Test of setPowerState method, of class LightRemote.
     *
     * @throws java.lang.Exception
     */
    @Test(timeout = 60000)
    public void testSetPowerState() throws Exception {
        System.out.println("setPowerState");
        PowerState.State state = PowerState.State.ON;
        lightRemote.setPower(state);
        lightRemote.requestStatus();
        assertEquals("Power has not been set in time!", state, lightRemote.getData().getPowerState().getValue());
    }

    /**
     * Test of gsetPowerState method, of class LightRemote.
     *
     * @throws java.lang.Exception
     */
    @Test(timeout = 60000)
    public void testGetPowerState() throws Exception {
        System.out.println("getPowerState");
        PowerState.State state = PowerState.State.OFF;
        ((LightController) deviceManagerLauncher.getDeviceManager().getUnitControllerRegistry().get(lightRemote.getId())).updatePower(state);
        lightRemote.requestStatus();
        assertEquals("Light has not been set in time!", state, lightRemote.getPower().getValue());
    }
}
