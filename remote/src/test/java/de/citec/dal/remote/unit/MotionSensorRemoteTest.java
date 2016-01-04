/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.remote.unit;

import org.dc.bco.registry.device.core.mock.MockRegistry;
import de.citec.dal.DALService;
import de.citec.dal.data.Location;
import de.citec.dal.hal.unit.MotionSensorController;
import org.dc.bco.registry.mock.MockRegistryHolder;
import org.dc.jps.core.JPService;
import de.citec.jps.properties.JPHardwareSimulationMode;
import org.dc.jul.exception.CouldNotPerformException;
import org.dc.jul.exception.InitializationException;
import org.dc.jul.exception.InvalidStateException;
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
    private static DALService dalService;
    private static MockRegistry registry;
    private static Location location;
    private static String label;

    public MotionSensorRemoteTest() {
    }

    @BeforeClass
    public static void setUpClass() throws InitializationException, InvalidStateException, org.dc.jul.exception.InstantiationException, CouldNotPerformException, InterruptedException {
        JPService.registerProperty(JPHardwareSimulationMode.class, true);
        registry = MockRegistryHolder.newMockRegistry();

        dalService = new DALService();
        dalService.init();
        

        location = new Location(registry.getLocation());
        label = MockRegistry.MOTION_SENSOR_LABEL;

        motionSensorRemote = new MotionSensorRemote();
        motionSensorRemote.init(label, location);
        motionSensorRemote.activate();
    }

    @AfterClass
    public static void tearDownClass() throws CouldNotPerformException, InterruptedException {
        if (dalService != null) {
            dalService.shutdown();
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
        MotionState motion = MotionState.newBuilder().setValue(MotionState.State.MOVEMENT).build();
        ((MotionSensorController) dalService.getUnitRegistry().get(motionSensorRemote.getId())).updateMotion(motion);
        Assert.assertEquals("The getter for the motion state returns the wrong value!", motion.getValue(), motionSensorRemote.requestStatus().getMotionState().getValue());
    }
}
