/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.hal.al;

import de.citec.dal.DALService;
import de.citec.dal.data.Location;
import de.citec.dal.hal.device.fibaro.F_MotionSensorController;
import de.citec.dal.hal.unit.BatteryController;
import de.citec.dal.util.DALRegistry;
import de.citec.jps.core.JPService;
import de.citec.jps.properties.JPHardwareSimulationMode;
import de.citec.jul.exception.NotAvailableException;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.Ignore;
import org.slf4j.LoggerFactory;

/**
 *
 * @author thuxohl
 */
public class BatteryRemoteTest {

    private static final Location LOCATION = new Location("paradise");
    private static final String LABEL = "Battery_Unit_Test";

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(BatteryRemoteTest.class);

    private static BatteryRemote batteryRemote;
    private static DALService dalService;

    public BatteryRemoteTest() {
    }

    @BeforeClass
    public static void setUpClass() {
        JPService.registerProperty(JPHardwareSimulationMode.class, true);
        dalService = new DALService(new BatteryRemoteTest.DeviceInitializerImpl());
        dalService.activate();

        batteryRemote = new BatteryRemote();
        batteryRemote.init(LABEL, LOCATION);
        batteryRemote.activate();
    }

    @AfterClass
    public static void tearDownClass() {
        dalService.deactivate();
        try {
            batteryRemote.deactivate();
        } catch (InterruptedException ex) {
            logger.warn("Could not deactivate battery remote: ", ex);
        }
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    /**
     * Test of notifyUpdated method, of class BatteryRemote.
     */
    @Ignore
    public void testNotifyUpdated() {
    }

    /**
     * Test of getBattaryLevel method, of class BatteryRemote.
     *
     * @throws java.lang.Exception
     */
    @Test(timeout = 3000)
    public void testGetBatteryLevel() throws Exception {
        System.out.println("getBatteryLevel");
        double level = 34.0;
        ((BatteryController) dalService.getRegistry().getUnits(BatteryController.class).iterator().next()).updateBattery(level);
        while (true) {
            try {
                if (batteryRemote.getBattery() == level) {
                    break;
                }
            } catch (NotAvailableException ex) {
                logger.debug("Not ready yet");
            }
            Thread.yield();
        }
        assertTrue("The getter for the battery level returns the wrong value!", batteryRemote.getBattery() == level);
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
