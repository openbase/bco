/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.remote.unit;

import de.citec.dal.DALService;
import de.citec.dal.data.Location;
import de.citec.dal.hal.device.fibaro.F_FGS221Controller;
import de.citec.dal.hal.unit.LightController;
import de.citec.dal.registry.DeviceRegistry;
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
import rst.homeautomation.state.PowerType;

/**
 *
 * @author thuxohl
 */
public class LightRemoteTest {

    public static final String LABEL = "Light_Unit_Test";
    public static final String[] UNITS = {"Light_1", "Light_2", "testButton_1", "testButton_2"};
    public static final Location LOCATION = new Location("paradise");

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(LightRemoteTest.class);

    private static LightRemote lightRemote;
    private static DALService dalService;

    public LightRemoteTest() {
    }

    @BeforeClass
    public static void setUpClass() throws InitializationException, InvalidStateException, de.citec.jul.exception.InstantiationException {
        JPService.registerProperty(JPHardwareSimulationMode.class, true);
        dalService = new DALService(new DeviceInitializerImpl());
        dalService.activate();

        lightRemote = new LightRemote();
        lightRemote.init(UNITS[0], LOCATION);
        lightRemote.activate();
    }

    @AfterClass
    public static void tearDownClass() throws CouldNotPerformException {
        dalService.shutdown();
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
        assertTrue("Power has not been set in time!", lightRemote.getData().getPowerState().getState().equals(state));
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
        ((LightController) dalService.getUnitRegistry().getUnit(UNITS[0], LOCATION, LightController.class)).updatePower(state);
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
        assertTrue("Light has not been set in time!", lightRemote.getPower().equals(state));
    }

    /**
     * Test of notifyUpdated method, of class LightRemote.
     */
    @Ignore
    public void testNotifyUpdated() {
    }

    public static class DeviceInitializerImpl implements de.citec.dal.util.DeviceInitializer {

        @Override
        public void initDevices(final DeviceRegistry registry) {

            try {
                registry.register(new F_FGS221Controller(LABEL, UNITS, LOCATION));
            } catch (CouldNotPerformException ex) {
                logger.warn("Could not initialize unit test device!", ex);
            }
        }
    }
}
