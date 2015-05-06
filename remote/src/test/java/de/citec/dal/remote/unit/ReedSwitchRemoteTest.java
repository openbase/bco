/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.remote.unit;

import de.citec.dal.registry.MockRegistry;
import de.citec.dal.DALService;
import de.citec.dal.data.Location;
import de.citec.dal.hal.unit.ReedSwitchController;
import de.citec.dal.registry.MockFactory;
import de.citec.jps.core.JPService;
import de.citec.jps.properties.JPHardwareSimulationMode;
import de.citec.jul.exception.CouldNotPerformException;
import de.citec.jul.exception.InitializationException;
import de.citec.jul.exception.InvalidStateException;
import de.citec.jul.exception.NotAvailableException;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.Ignore;
import org.slf4j.LoggerFactory;
import rst.homeautomation.state.OpenClosedType;

/**
 *
 * @author thuxohl
 */
public class ReedSwitchRemoteTest {

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(ReedSwitchRemoteTest.class);

    private static ReedSwitchRemote reedSwitchRemote;
    private static DALService dalService;
    private static MockRegistry registry;
    private static Location location;
    private static String label;

    public ReedSwitchRemoteTest() {
    }

    @BeforeClass
    public static void setUpClass() throws InitializationException, InvalidStateException, de.citec.jul.exception.InstantiationException, CouldNotPerformException {
        JPService.registerProperty(JPHardwareSimulationMode.class, true);
        registry = MockFactory.newMockRegistry();

        dalService = new DALService();
        dalService.init();
        dalService.activate();

        location = new Location(registry.getLocation());
        label = MockRegistry.REED_SWITCH_LABEL;

        reedSwitchRemote = new ReedSwitchRemote();
        reedSwitchRemote.init(label, location);
        reedSwitchRemote.activate();
    }

    @AfterClass
    public static void tearDownClass() throws CouldNotPerformException, InterruptedException {
        if (dalService != null) {
            dalService.shutdown();
        }
        if (reedSwitchRemote != null) {
            reedSwitchRemote.shutdown();
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
     * Test of notifyUpdated method, of class ReedSwitchRemote.
     */
    @Ignore
    public void testNotifyUpdated() {
    }

    /**
     * Test of getReedSwitchState method, of class ReedSwitchRemote.
     *
     * @throws java.lang.Exception
     */
    @Test
    public void testGetReedSwitchState() throws Exception {
        System.out.println("getReedSwitchState");
        OpenClosedType.OpenClosed.OpenClosedState state = OpenClosedType.OpenClosed.OpenClosedState.CLOSED;
        ((ReedSwitchController) dalService.getUnitRegistry().get(reedSwitchRemote.getId())).updateReedSwitch(state);
        reedSwitchRemote.requestStatus();
        Assert.assertEquals("The getter for the reed switch state returns the wrong value!", state, reedSwitchRemote.getReedSwitch());
    }
}
