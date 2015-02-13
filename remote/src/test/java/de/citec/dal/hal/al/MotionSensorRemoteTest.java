/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.hal.al;

import de.citec.dal.DALService;
import de.citec.dal.data.Location;
import de.citec.dal.hal.device.fibaro.F_MotionSensorController;
import de.citec.dal.hal.unit.MotionSensorController;
import de.citec.dal.util.DALRegistry;
import de.citec.jps.core.JPService;
import de.citec.jps.properties.JPHardwareSimulationMode;
import de.citec.jul.exception.NotAvailableException;
import org.junit.After;
import org.junit.AfterClass;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.Ignore;
import org.slf4j.LoggerFactory;
import rst.homeautomation.states.MotionType;

/**
 *
 * @author thuxohl
 */
public class MotionSensorRemoteTest {

    private static final Location LOCATION = new Location("paradise");
    private static final String LABEL = "Motion_Sensor_Unit_Test";

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(MotionSensorRemoteTest.class);

    private static MotionSensorRemote motionSensorRemote;
    private static DALService dalService;

    public MotionSensorRemoteTest() {
    }

    @BeforeClass
    public static void setUpClass() {
        JPService.registerProperty(JPHardwareSimulationMode.class, true);
        dalService = new DALService(new MotionSensorRemoteTest.DeviceInitializerImpl());
        dalService.activate();

        motionSensorRemote = new MotionSensorRemote();
        motionSensorRemote.init(LABEL, LOCATION);
        motionSensorRemote.activate();
    }

    @AfterClass
    public static void tearDownClass() {
        dalService.deactivate();
        try {
            motionSensorRemote.deactivate();
        } catch (InterruptedException ex) {
            logger.warn("Could not deactivate motion sensor remote: ", ex);
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
    @Test(timeout = 3000)
    public void testGetMotionState() throws Exception {
        System.out.println("getMotionState");
        MotionType.Motion.MotionState state = MotionType.Motion.MotionState.MOVEMENT;
        ((MotionSensorController) dalService.getRegistry().getUnits(MotionSensorController.class).iterator().next()).updateMotion(state);
        while (true) {
            try {
                if (motionSensorRemote.getMotion().equals(state)) {
                    break;
                }
            } catch (NotAvailableException ex) {
                logger.debug("Not ready yet");
            }
            Thread.yield();
        }
        assertTrue("The getter for the motion state returns the wrong value!", motionSensorRemote.getMotion().equals(state));
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
