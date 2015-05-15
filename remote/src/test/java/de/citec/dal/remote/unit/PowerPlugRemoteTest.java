/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.remote.unit;

import de.citec.dal.registry.MockRegistry;
import de.citec.dal.DALService;
import de.citec.dal.data.Location;
import de.citec.dal.hal.unit.PowerPlugController;
import de.citec.dal.registry.MockFactory;
import de.citec.jps.core.JPService;
import de.citec.jps.properties.JPHardwareSimulationMode;
import de.citec.jul.exception.CouldNotPerformException;
import de.citec.jul.exception.InitializationException;
import de.citec.jul.exception.InstantiationException;
import de.citec.jul.exception.InvalidStateException;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.Ignore;
import org.slf4j.LoggerFactory;
import rst.homeautomation.state.PowerStateType.PowerState;

/**
 *
 * @author thuxohl
 */
public class PowerPlugRemoteTest {

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(PowerPlugRemoteTest.class);

    private static PowerPlugRemote powerPlugRemote;
    private static DALService dalService;
    private static MockRegistry registry;
    private static Location locaton;
    private static String label;

    public PowerPlugRemoteTest() {
    }

    @BeforeClass
    public static void setUpClass() throws InitializationException, InvalidStateException, InstantiationException, CouldNotPerformException {
        JPService.registerProperty(JPHardwareSimulationMode.class, true);
        registry = MockFactory.newMockRegistry();

        dalService = new DALService();
        dalService.init();
        dalService.activate();

        locaton = new Location(registry.getLocation());
        label = MockRegistry.POWER_PLUG_LABEL;

        powerPlugRemote = new PowerPlugRemote();
        powerPlugRemote.init(label, locaton);
        powerPlugRemote.activate();
    }

    @AfterClass
    public static void tearDownClass() throws CouldNotPerformException, InterruptedException {
        if (dalService != null) {
            dalService.shutdown();
        }
        if (powerPlugRemote != null) {
            powerPlugRemote.shutdown();
        }
        if (registry != null) {
            MockFactory.shutdownMockRegistry();
        }
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    /**
     * Test of setPowerState method, of class PowerPlugRemote.
     *
     * @throws java.lang.Exception
     */
    @Test
    public void testSetPowerState() throws Exception {
        System.out.println("setPowerState");
        PowerState.State state = PowerState.State.ON;
        powerPlugRemote.setPower(state);
        powerPlugRemote.requestStatus();
        assertEquals("Power state has not been set in time!", state, powerPlugRemote.getData().getPowerState().getValue());
    }

    /**
     * Test of getPowerState method, of class PowerPlugRemote.
     *
     * @throws java.lang.Exception
     */
    @Test
    public void testGetPowerState() throws Exception {
        System.out.println("getPowerState");
        PowerState.State state = PowerState.State.OFF;
        ((PowerPlugController) dalService.getUnitRegistry().get(powerPlugRemote.getId())).updatePower(state);
        powerPlugRemote.requestStatus();
        assertEquals("The getter for the power state returns the wrong value!", state, powerPlugRemote.getPower().getValue());
    }

    /**
     * Test of notifyUpdated method, of class PowerPlugRemote.
     */
    @Ignore
    public void testNotifyUpdated() {
    }
}
