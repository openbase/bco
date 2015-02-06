/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.hal.al;

import de.citec.dal.DALService;
import de.citec.dal.data.Location;
import de.citec.dal.hal.device.fibaro.F_MotionSensorController;
import de.citec.dal.hal.unit.TamperSwitchController;
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
import rst.homeautomation.states.TamperType;

/**
 *
 * @author thuxohl
 */
public class TamperSwtichRemoteTest {

    private static final Location LOCATION = new Location("paradise");
    private static final String LABEL = "Tamper_Switch_Unit_Test";

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(TamperSwtichRemoteTest.class);

    private TamperSwtichRemote tamperSwitchRemote;
    private DALService dalService;

    public TamperSwtichRemoteTest() {
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
        dalService = new DALService(new TamperSwtichRemoteTest.DeviceInitializerImpl());
        dalService.activate();

        tamperSwitchRemote = new TamperSwtichRemote();
        tamperSwitchRemote.init(LABEL, LOCATION);
        tamperSwitchRemote.activate();
    }

    @After
    public void tearDown() {
        dalService.deactivate();
        try {
            tamperSwitchRemote.deactivate();
        } catch (InterruptedException ex) {
            logger.warn("Could not deactivate tamper switch remote: ", ex);
        }
    }

    /**
     * Test of notifyUpdated method, of class TamperSwtichRemote.
     */
    @Ignore
    public void testNotifyUpdated() {
    }

    /**
     * Test of getTamperState method, of class TamperSwtichRemote.
     *
     * @throws java.lang.Exception
     */
    @Test(timeout = 3000)
    public void testGetTamperState() throws Exception {
        System.out.println("getTamperState");
        TamperType.Tamper.TamperState state = TamperType.Tamper.TamperState.TAMPER;
        ((TamperSwitchController) dalService.getRegistry().getUnits(TamperSwitchController.class).iterator().next()).updateTamperState(state);
        while (!tamperSwitchRemote.getTamperState().equals(state)) {
            Thread.yield();
        }
        assertTrue("The getter for the tamper switch state returns the wrong value!", tamperSwitchRemote.getTamperState().equals(state));
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
