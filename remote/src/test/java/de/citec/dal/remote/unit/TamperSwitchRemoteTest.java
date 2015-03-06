/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.remote.unit;

import de.citec.dal.DALService;
import de.citec.dal.data.Location;
import de.citec.dal.hal.device.fibaro.F_MotionSensorController;
import de.citec.dal.hal.unit.TamperSwitchController;
import de.citec.dal.registry.UnitRegistry;
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
import rst.homeautomation.state.TamperType;

/**
 *
 * @author thuxohl
 */
public class TamperSwitchRemoteTest {

    private static final Location LOCATION = new Location("paradise");
    public static final String LABEL = "Tamper_Switch_Unit_Test";

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(TamperSwitchRemoteTest.class);

    private static TamperSwitchRemote tamperSwitchRemote;
    private static DALService dalService;

    public TamperSwitchRemoteTest() {
    }

    @BeforeClass
    public static void setUpClass() throws InitializationException, InvalidStateException {
        JPService.registerProperty(JPHardwareSimulationMode.class, true);
        dalService = new DALService(new DeviceInitializerImpl());
        dalService.activate();

        tamperSwitchRemote = new TamperSwitchRemote();
        tamperSwitchRemote.init(LABEL, LOCATION);
        tamperSwitchRemote.activate();
    }

    @AfterClass
    public static void tearDownClass() throws CouldNotPerformException {
        dalService.shutdown();
        try {
            tamperSwitchRemote.deactivate();
        } catch (InterruptedException ex) {
            logger.warn("Could not deactivate tamper switch remote: ", ex);
        }
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
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
        ((TamperSwitchController)dalService.getUnitRegistry().getUnit(LABEL, LOCATION, TamperSwitchController.class)).updateTamper(state);
        while (true) {
            try {
                if (tamperSwitchRemote.getTamper().equals(state)) {
                    break;
                }
            } catch (NotAvailableException ex) {
                logger.debug("Not ready yet");
            }
            Thread.yield();
        }
        assertTrue("The getter for the tamper switch state returns the wrong value!", tamperSwitchRemote.getTamper().equals(state));
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
