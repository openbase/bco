/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.remote.unit;

import de.citec.dal.DALService;
import de.citec.dal.data.Location;
import de.citec.dal.hal.device.homematic.HM_ReedSwitchController;
import de.citec.dal.hal.unit.ReedSwitchController;
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
import rst.homeautomation.state.OpenClosedType;

/**
 *
 * @author thuxohl
 */
public class ReedSwitchRemoteTest {

    private static final Location LOCATION = new Location("paradise");
    public static final String LABEL = "Reed_Switch_Unit_Test";

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(ReedSwitchRemoteTest.class);

    private static ReedSwitchRemote reedSwitchRemote;
    private static DALService dalService;

    public ReedSwitchRemoteTest() {
    }

    @BeforeClass
    public static void setUpClass() throws InitializationException, InvalidStateException, de.citec.jul.exception.InstantiationException {
        JPService.registerProperty(JPHardwareSimulationMode.class, true);
        dalService = new DALService(new DeviceInitializerImpl());
        dalService.activate();

        reedSwitchRemote = new ReedSwitchRemote();
        reedSwitchRemote.init(LABEL, LOCATION);
        reedSwitchRemote.activate();
    }

    @AfterClass
    public static void tearDownClass() throws CouldNotPerformException {
        dalService.shutdown();
        try {
            reedSwitchRemote.deactivate();
        } catch (InterruptedException ex) {
            logger.warn("Could not deactivate reed switch remote: ", ex);
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
    @Test(timeout = 3000)
    public void testGetReedSwitchState() throws Exception {
        System.out.println("getReedSwitchState");
        OpenClosedType.OpenClosed.OpenClosedState state = OpenClosedType.OpenClosed.OpenClosedState.CLOSED;
        ((ReedSwitchController) dalService.getUnitRegistry().getUnit(LABEL, LOCATION, ReedSwitchController.class)).updateReedSwitch(state);
        while (true) {
            try {
                if (reedSwitchRemote.getReedSwitch().equals(state)) {
                    break;
                }
            } catch (NotAvailableException ex) {
                logger.debug("Not ready yet");
            }
            Thread.yield();
        }
        assertTrue("The getter for the reed switch state returns the wrong value!", reedSwitchRemote.getReedSwitch().equals(state));
    }

    public static class DeviceInitializerImpl implements de.citec.dal.util.DeviceInitializer {

        @Override
        public void initDevices(final DeviceRegistry registry) {

            try {
                registry.register(new HM_ReedSwitchController(LABEL, LOCATION));
            } catch (CouldNotPerformException ex) {
                logger.warn("Could not initialize unit test device!", ex);
            }
        }
    }
}
