/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.hal.al;

import de.citec.dal.DALService;
import de.citec.dal.data.Location;
import de.citec.dal.hal.device.fibaro.F_MotionSensorController;
import de.citec.dal.hal.unit.BrightnessSensorController;
import de.citec.dal.util.DALRegistry;
import de.citec.jps.core.JPService;
import de.citec.jps.properties.JPHardwareSimulationMode;
import org.junit.After;
import org.junit.AfterClass;
import static org.junit.Assert.assertTrue;
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

    private static final Location LOCATION = new Location("paradise");
    private static final String LABEL = "Brightness_Sensor_Unit_Test";

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(BrightnessSensorRemoteTest.class);

    private BrightnessSensorRemote brightnessSensorRemote;
    private DALService dalService;

    public BrightnessSensorRemoteTest() {
    }

    @BeforeClass
    public static void setUpClass() {
    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Before
    public void setUp() {
        JPService.registerProperty(JPHardwareSimulationMode.class, true);
        dalService = new DALService(new BrightnessSensorRemoteTest.DeviceInitializerImpl());
        dalService.activate();

        brightnessSensorRemote = new BrightnessSensorRemote();
        brightnessSensorRemote.init(LABEL, LOCATION);
        brightnessSensorRemote.activate();
    }

    @After
    public void tearDown() {
        dalService.deactivate();
        try {
            brightnessSensorRemote.deactivate();
        } catch (InterruptedException ex) {
            logger.warn("Could not deactivate brightness sensor remote: ", ex);
        }
    }

    /**
     * Test of notifyUpdated method, of class BrightnessSensorRemote.
     */
    @Ignore
    public void testNotifyUpdated() {
    }

    /**
     * Test of getBrightness method, of class BrightnessSensorRemote.
     */
    @Test(timeout = 3000)
    public void testGetBrightness() throws Exception {
        System.out.println("getBrightness");
        double brightness = 0.5;
        ((BrightnessSensorController) dalService.getRegistry().getUnits(BrightnessSensorController.class).iterator().next()).updateBrightness((float) brightness);
        while (!(brightnessSensorRemote.getBrightness() == brightness)) {
            Thread.yield();
        }
        assertTrue("The getter for the brightness returns the wrong value!", brightnessSensorRemote.getBrightness() == brightness);
    }

    public static class DeviceInitializerImpl implements de.citec.dal.util.DeviceInitializer {

        @Override
        public void initDevices(final DALRegistry registry) {

            try {
                registry.register(new F_MotionSensorController("F_MotionSensor_000", LABEL, LOCATION));
            } catch (de.citec.jul.exception.InstantiationException ex) {
                logger.warn("Could not initialize unit test device!", ex);
            }
        }
    }
}
