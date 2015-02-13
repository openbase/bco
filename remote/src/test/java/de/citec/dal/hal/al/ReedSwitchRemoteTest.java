/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.hal.al;

import de.citec.dal.DALService;
import de.citec.dal.data.Location;
import de.citec.dal.hal.device.homematic.HM_ReedSwitchController;
import de.citec.dal.hal.unit.ReedSwitchController;
import de.citec.dal.util.DALRegistry;
import de.citec.jps.core.JPService;
import de.citec.jps.properties.JPHardwareSimulationMode;
import de.citec.jul.exception.NotAvailableException;
import de.citec.jul.exception.VerificationFailedException;
import org.junit.After;
import org.junit.AfterClass;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.Ignore;
import org.slf4j.LoggerFactory;
import rst.homeautomation.states.OpenClosedType;

/**
 *
 * @author thuxohl
 */
public class ReedSwitchRemoteTest {

    private static final Location LOCATION = new Location("paradise");
    private static final String LABEL = "Reed_Switch_Unit_Test";

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(ReedSwitchRemoteTest.class);

    private ReedSwitchRemote reedSwitchRemote;
    private DALService dalService;

    public ReedSwitchRemoteTest() {
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
        dalService = new DALService(new ReedSwitchRemoteTest.DeviceInitializerImpl());
        dalService.activate();

        reedSwitchRemote = new ReedSwitchRemote();
        reedSwitchRemote.init(LABEL, LOCATION);
        reedSwitchRemote.activate();
    }

    @After
    public void tearDown() {
        dalService.deactivate();
        try {
            reedSwitchRemote.deactivate();
        } catch (InterruptedException ex) {
            logger.warn("Could not deactivate reed switch remote: ", ex);
        }
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
    @Test(timeout = 3000)
    public void testGetReedSwitchState() throws Exception {
        System.out.println("getReedSwitchState");
        OpenClosedType.OpenClosed.OpenClosedState state = OpenClosedType.OpenClosed.OpenClosedState.CLOSED;
        ((ReedSwitchController) dalService.getRegistry().getUnits(ReedSwitchController.class).iterator().next()).updateOpenClosedState(state);
        while (true) {
            try {
                if (reedSwitchRemote.getReedSwitchState().equals(state)) {
                    break;
                }
            } catch (NotAvailableException ex) {
                logger.debug("Not ready yet");
            }
            Thread.yield();
        }
        assertTrue("The getter for the reed switch state returns the wrong value!", reedSwitchRemote.getReedSwitchState().equals(state));
    }

    public static class DeviceInitializerImpl implements de.citec.dal.util.DeviceInitializer {

        @Override
        public void initDevices(final DALRegistry registry) {

            try {
                registry.register(new HM_ReedSwitchController("HM_ReedSwtich_000", LABEL, LOCATION));
            } catch (VerificationFailedException | de.citec.jul.exception.InstantiationException ex) {
                logger.warn("Could not initialize unit test device!", ex);
            }
        }
    }
}
