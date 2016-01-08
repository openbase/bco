/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dc.bco.manager.device.test.remote.unit;

import org.dc.bco.dal.lib.data.Location;
import org.dc.bco.dal.lib.layer.unit.BrightnessSensorController;
import org.dc.bco.registry.mock.MockRegistryHolder;
import org.dc.jps.core.JPService;
import org.dc.bco.dal.lib.jp.JPHardwareSimulationMode;
import org.dc.bco.dal.remote.unit.BrightnessSensorRemote;
import org.dc.bco.manager.device.core.DeviceManagerLauncher;
import org.dc.bco.registry.mock.MockRegistry;
import org.dc.jul.exception.CouldNotPerformException;
import org.dc.jul.exception.InitializationException;
import org.dc.jul.exception.InvalidStateException;
import org.junit.After;
import org.junit.AfterClass;
import static org.junit.Assert.assertEquals;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.Ignore;
import org.slf4j.LoggerFactory;

/**
 *
 * @author thuxohl
 */
public class BrightnessSensorRemoteTest {

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(BrightnessSensorRemoteTest.class);

    private static BrightnessSensorRemote brightnessSensorRemote;
    private static DeviceManagerLauncher deviceManagerLauncher;
    private static MockRegistry registry;
    private static Location location;
    private static String label;

    public BrightnessSensorRemoteTest() {
    }

    @BeforeClass
    public static void setUpClass() throws InitializationException, InvalidStateException, org.dc.jul.exception.InstantiationException, CouldNotPerformException, InterruptedException {
        JPService.registerProperty(JPHardwareSimulationMode.class, true);
        registry = MockRegistryHolder.newMockRegistry();

        deviceManagerLauncher = new DeviceManagerLauncher();
        deviceManagerLauncher.launch();
        

        location = new Location(registry.getLocation());
        label = MockRegistry.BRIGHTNESS_SENSOR_LABEL;

        brightnessSensorRemote = new BrightnessSensorRemote();
        brightnessSensorRemote.init(label, location);
        brightnessSensorRemote.activate();
    }

    @AfterClass
    public static void tearDownClass() throws CouldNotPerformException, InterruptedException {
        if (deviceManagerLauncher != null) {
            deviceManagerLauncher.shutdown();
        }
        if (brightnessSensorRemote != null) {
            brightnessSensorRemote.shutdown();
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
     * Test of notifyUpdated method, of class BrightnessSensorRemote.
     */
    @Ignore
    public void testNotifyUpdated() {
    }

    /**
     * Test of getBrightness method, of class BrightnessSensorRemote.
     *
     * @throws java.lang.Exception
     */
    @Test(timeout = 60000)
    public void testGetBrightness() throws Exception {
        System.out.println("getBrightness");
        double brightness = 0.5;
        ((BrightnessSensorController) deviceManagerLauncher.getDeviceManager().getUnitControllerRegistry().get(brightnessSensorRemote.getId())).updateBrightness(brightness);
        brightnessSensorRemote.requestStatus();
        assertEquals("The getter for the brightness returns the wrong value!", brightness, brightnessSensorRemote.getBrightness(), 0.1);
    }
}
