/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.remote.unit;

import de.citec.dal.DALService;
import de.citec.dal.data.Location;
import de.citec.dal.hal.device.fibaro.F_MotionSensorController;
import de.citec.dal.hal.unit.BrightnessSensorController;
import de.citec.dal.registry.DeviceRegistry;
import de.citec.jps.core.JPService;
import de.citec.jps.properties.JPHardwareSimulationMode;
import de.citec.jul.exception.CouldNotPerformException;
import de.citec.jul.exception.InitializationException;
import de.citec.jul.exception.InvalidStateException;
import de.citec.jul.exception.NotAvailableException;
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

    public static final String LABEL = "Brightness_Sensor_Unit_Test";
    private static final Location LOCATION = new Location("paradise");

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(BrightnessSensorRemoteTest.class);

    private static BrightnessSensorRemote brightnessSensorRemote;
    private static DALService dalService;

    public BrightnessSensorRemoteTest() {
    }

    @BeforeClass
    public static void setUpClass()  throws InitializationException, InvalidStateException, de.citec.jul.exception.InstantiationException {
        JPService.registerProperty(JPHardwareSimulationMode.class, true);
        dalService = new DALService(new DeviceInitializerImpl());
        dalService.activate();

        brightnessSensorRemote = new BrightnessSensorRemote();
        brightnessSensorRemote.init(LABEL, LOCATION);
        brightnessSensorRemote.activate();
    }

    @AfterClass
    public static void tearDownClass() throws CouldNotPerformException {
        dalService.shutdown();
        try {
            brightnessSensorRemote.deactivate();
        } catch (InterruptedException ex) {
            logger.warn("Could not deactivate brightness sensor remote: ", ex);
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
     * @throws java.lang.Exception
     */
    @Test(timeout = 3000)
    public void testGetBrightness() throws Exception {
        System.out.println("getBrightness");
        double brightness = 0.5;
        ((BrightnessSensorController) dalService.getUnitRegistry().getUnit(LABEL, LOCATION, BrightnessSensorController.class)).updateBrightness((float) brightness);
        while (true) {
            try {
                if (brightnessSensorRemote.getBrightness() == brightness) {
                    break;
                }
            } catch (NotAvailableException ex) {
                logger.debug("Not ready yet");
            }
            Thread.yield();
        }
        assertTrue("The getter for the brightness returns the wrong value!", brightnessSensorRemote.getBrightness() == brightness);
    }

    public static class DeviceInitializerImpl implements de.citec.dal.util.DeviceInitializer {

        @Override
        public void initDevices(final DeviceRegistry registry) {

            try {
                registry.register(new F_MotionSensorController(LABEL, LOCATION));
            } catch (CouldNotPerformException ex) {
                logger.warn("Could not initialize unit test device!", ex);
            }
        }
    }
}
