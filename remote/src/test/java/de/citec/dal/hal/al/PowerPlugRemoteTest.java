/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.hal.al;

import de.citec.dal.DALService;
import de.citec.dal.data.Location;
import de.citec.dal.hal.device.plugwise.PW_PowerPlugController;
import de.citec.dal.hal.unit.PowerPlugController;
import de.citec.dal.util.DALRegistry;
import de.citec.jps.core.JPService;
import de.citec.jps.properties.JPHardwareSimulationMode;
import de.citec.jul.exception.VerificationFailedException;
import de.citec.jul.exception.InstantiationException;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.Ignore;
import org.slf4j.LoggerFactory;
import rst.homeautomation.states.PowerType;

/**
 *
 * @author thuxohl
 */
public class PowerPlugRemoteTest {

    private static final Location LOCATION = new Location("paradise");
    private static final String LABEL = "Power_Plug_Unit_Test";

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(PowerPlugRemoteTest.class);

    private PowerPlugRemote powerPlugRemote;
    private DALService dalService;

    public PowerPlugRemoteTest() {
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
        dalService = new DALService(new PowerPlugRemoteTest.DeviceInitializerImpl());
        dalService = new DALService();
        dalService.activate();

        powerPlugRemote = new PowerPlugRemote();
        powerPlugRemote.init(LABEL, LOCATION);
        powerPlugRemote.activate();
    }

    @After
    public void tearDown() {
        dalService.deactivate();
        try {
            powerPlugRemote.deactivate();
        } catch (InterruptedException ex) {
            logger.warn("Could not deactivate power plug remote: ", ex);
        }
    }

    /**
     * Test of setPowerState method, of class PowerPlugRemote.
     *
     * @throws java.lang.Exception
     */
    @Test(timeout = 3000)
    public void testSetPowerState() throws Exception {
        System.out.println("setPowerState");
        PowerType.Power.PowerState state = PowerType.Power.PowerState.ON;
        powerPlugRemote.setPowerState(state);
        while (!powerPlugRemote.getData().getPowerState().getState().equals(state)) {
            Thread.yield();
        }
        assertTrue("Power state has not been set in time!", powerPlugRemote.getData().getPowerState().getState().equals(state));
    }

    /**
     * Test of getPowerState method, of class PowerPlugRemote.
     *
     * @throws java.lang.Exception
     */
    @Test(timeout = 3000)
    public void testGetPowerState() throws Exception {
        System.out.println("getPowerState");
        PowerType.Power.PowerState state = PowerType.Power.PowerState.OFF;
        ((PowerPlugController) dalService.getRegistry().getUnits(PowerPlugController.class).iterator().next()).setPowerState(state);
        while (!powerPlugRemote.getPowerState().equals(state)) {
            Thread.yield();
        }
        assertTrue("The getter for the power state returns the wrong value!", powerPlugRemote.getPowerState().equals(state));
    }

    /**
     * Test of notifyUpdated method, of class PowerPlugRemote.
     */
    @Ignore
    public void testNotifyUpdated() {
    }

    public static class DeviceInitializerImpl implements de.citec.dal.util.DeviceInitializer {

        @Override
        public void initDevices(final DALRegistry registry) {

            try {
                registry.register(new PW_PowerPlugController("PW_PowerPlug_000", LABEL, LOCATION));
            } catch (VerificationFailedException | InstantiationException ex) {
                logger.warn("Could not initialize unit test device!", ex);
            }
        }
    }
}
