/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.remote.unit;

import de.citec.dal.DALService;
import de.citec.dal.hal.unit.LightController;
import de.citec.jps.core.JPService;
import de.citec.jps.properties.JPHardwareSimulationMode;
import de.citec.jul.exception.CouldNotPerformException;
import de.citec.jul.exception.InitializationException;
import de.citec.jul.exception.InvalidStateException;
import de.citec.jul.exception.NotAvailableException;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import static org.junit.Assert.*;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.LoggerFactory;
import rst.homeautomation.states.PowerType;

/**
 *
 * @author thuxohl
 */
public class LightRemoteTest {

    public static final String LABEL = "Light_Unit_Test";
    public static final String[] UNITS = {"Light_1", "Light_2","testButton_1","testButton_2"};

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(DALService.class);

    private static LightRemote lightRemote;
    private static DALService dalService;

    public LightRemoteTest() {
    }

    @BeforeClass
    public static void setUpClass() throws InitializationException, InvalidStateException {
        JPService.registerProperty(JPHardwareSimulationMode.class, true);
        dalService = new DALService(new TestConfiguration());
        dalService.activate();

        lightRemote = new LightRemote();
        lightRemote.init(UNITS[0], TestConfiguration.LOCATION);
        lightRemote.activate();
    }

    @AfterClass
    public static void tearDownClass() throws CouldNotPerformException {
        dalService.deactivate();
        try {
            lightRemote.deactivate();
        } catch (InterruptedException ex) {
            logger.warn("Could not deactivate light remote: ", ex);
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
    @Test(timeout = 3000)
    public void testSetPowerState() throws Exception {
        System.out.println("setPowerState");
        PowerType.Power.PowerState state = PowerType.Power.PowerState.ON;
        lightRemote.setPower(state);
        while (true) {
            try {
                if (lightRemote.getData().getPowerState().getState().equals(state)) {
                    break;
                }
            } catch (NotAvailableException ex) {
                logger.debug("Not ready yet");
            }
            Thread.yield();
        }
        assertTrue("Color has not been set in time!", lightRemote.getData().getPowerState().getState().equals(state));
    }

    /**
     * Test of gsetPowerState method, of class LightRemote.
     *
     * @throws java.lang.Exception
     */
    @Test(timeout = 3000)
    public void testGetPowerState() throws Exception {
        System.out.println("getPowerState");
        PowerType.Power.PowerState state = PowerType.Power.PowerState.OFF;
        LightController test = ((LightController) dalService.getRegistry().getUnit(UNITS[0], TestConfiguration.LOCATION, LightController.class));
        test.updatePower(state);
        System.out.println(test.getScope() + "," + test.getLable());
        while (true) {
            try {
                if (lightRemote.getPower().equals(state)) {
                    break;
                }
            } catch (NotAvailableException ex) {
                logger.debug("Not ready yet");
            }
            Thread.yield();
        }
        assertTrue("Color has not been set in time!", lightRemote.getPower().equals(state));
    }

    /**
     * Test of notifyUpdated method, of class LightRemote.
     */
    @Ignore
    public void testNotifyUpdated() {
    }
}
