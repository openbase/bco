/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.remote.unit;

import de.citec.dal.DALService;
import de.citec.dal.data.Location;
import de.citec.dal.hal.unit.MotionSensorController;
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
import rst.homeautomation.state.MotionType;

/**
 *
 * @author thuxohl
 */
public class MotionSensorRemoteTest {

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(MotionSensorRemoteTest.class);

    private static MotionSensorRemote motionSensorRemote;
    private static DALService dalService;
    private static MockRegistryHolder registry;
    private static Location location;
    private static String label;

    public MotionSensorRemoteTest() {
    }

    @BeforeClass
    public static void setUpClass() throws InitializationException, InvalidStateException, de.citec.jul.exception.InstantiationException, CouldNotPerformException {
        JPService.registerProperty(JPHardwareSimulationMode.class, true);
        registry = new MockRegistryHolder();
        
        dalService = new DALService();
        dalService.activate();

        location = new Location(registry.getLocation());
        label = MockRegistryHolder.MOTION_SENSOR_LABEL;

        motionSensorRemote = new MotionSensorRemote();
        motionSensorRemote.init(label, location);
        motionSensorRemote.activate();
    }

    @AfterClass
    public static void tearDownClass() throws CouldNotPerformException, InterruptedException {
        dalService.shutdown();
        try {
            motionSensorRemote.deactivate();
        } catch (InterruptedException ex) {
            logger.warn("Could not deactivate motion sensor remote: ", ex);
        }
        registry.shutdown();
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
        ((MotionSensorController) dalService.getUnitRegistry().getUnit(label, location, MotionSensorController.class)).updateMotion(state);
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
}
