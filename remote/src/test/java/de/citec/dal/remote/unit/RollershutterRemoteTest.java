/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.remote.unit;

import de.citec.dal.DALService;
import de.citec.dal.data.Location;
import de.citec.dal.hal.unit.RollershutterController;
import de.citec.jps.core.JPService;
import de.citec.jps.preset.JPDebugMode;
import de.citec.jps.properties.JPHardwareSimulationMode;
import de.citec.jul.exception.CouldNotPerformException;
import de.citec.jul.exception.InitializationException;
import de.citec.jul.exception.InvalidStateException;
import de.citec.jul.exception.NotAvailableException;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.Ignore;
import org.slf4j.LoggerFactory;
import rst.homeautomation.state.ShutterType;

/**
 *
 * @author thuxohl
 */
public class RollershutterRemoteTest {

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(RollershutterRemoteTest.class);

    private static RollershutterRemote rollershutterRemote;
    private static DALService dalService;
    private static MockRegistryHolder registry;
    private static Location location;
    private static String label;

    public RollershutterRemoteTest() {
    }

    @BeforeClass
    public static void setUpClass() throws InitializationException, InvalidStateException, de.citec.jul.exception.InstantiationException, CouldNotPerformException {
        JPService.registerProperty(JPHardwareSimulationMode.class, true);
        JPService.registerProperty(JPDebugMode.class, true);
        dalService = new DALService();
        dalService.activate();

        registry = new MockRegistryHolder();
        location = new Location(registry.getLocation());
        label = MockRegistryHolder.ROLLERSHUTTER_LABEL;

        rollershutterRemote = new RollershutterRemote();
        rollershutterRemote.init(label, location);
        rollershutterRemote.activate();
    }

    @AfterClass
    public static void tearDownClass() throws CouldNotPerformException, InterruptedException {
        dalService.shutdown();
        try {
            rollershutterRemote.deactivate();
        } catch (InterruptedException ex) {
            logger.warn("Could not deactivate rollershutter remote: ", ex);
        }
        registry.shutdown();
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    /**
     * Test of setShutterState method, of class RollershutterRemote.
     *
     * @throws java.lang.Exception
     */
    @Test(timeout = 3000)
    public void testSetShutterState() throws Exception {
        System.out.println("setShutterState");
        ShutterType.Shutter.ShutterState state = ShutterType.Shutter.ShutterState.DOWN;
        rollershutterRemote.setShutter(state);
        while (true) {
            try {
                if (rollershutterRemote.getData().getShutterState().getState().equals(state)) {
                    break;
                }
            } catch (NotAvailableException ex) {
                logger.debug("Not ready yet");
            }
            Thread.yield();
        }
        assertTrue("Shutter has not been set in time!", rollershutterRemote.getData().getShutterState().getState().equals(state));
    }

    /**
     * Test of getShutterState method, of class RollershutterRemote.
     *
     * @throws java.lang.Exception
     */
    @Test(timeout = 3000)
    public void testGetShutterState() throws Exception {
        System.out.println("getShutterState");
        ShutterType.Shutter.ShutterState state = ShutterType.Shutter.ShutterState.STOP;
        ((RollershutterController) dalService.getUnitRegistry().getUnit(label, location, RollershutterController.class)).updateShutter(state);
        while (true) {
            try {
                if (rollershutterRemote.getShutter().equals(state)) {
                    break;
                }
            } catch (NotAvailableException ex) {
                logger.debug("Not ready yet");
            }
            Thread.yield();
        }
        assertTrue("Shutter has not been set in time!", rollershutterRemote.getShutter().equals(state));
    }

    /**
     * Test of setOpeningRatio method, of class RollershutterRemote.
     *
     * @throws java.lang.Exception
     */
    @Test(timeout = 3000)
    public void testSetOpeningRatio() throws Exception {
        System.out.println("setOpeningRatio");
        double openingRatio = 34.0D;
        rollershutterRemote.setOpeningRatio(openingRatio);
        while (true) {
            try {
                if (rollershutterRemote.getData().getOpeningRatio() == openingRatio) {
                    break;
                }
            } catch (NotAvailableException ex) {
                logger.debug("Not ready yet");
            }
            Thread.yield();
        }
        assertTrue("Opening ration has not been set in time!", rollershutterRemote.getData().getOpeningRatio() == openingRatio);
    }

    /**
     * Test of setOpeningRatio method, of class RollershutterRemote.
     *
     * @throws java.lang.Exception
     */
    @Test(timeout = 3000)
    public void testGetOpeningRatio() throws Exception {
        System.out.println("getOpeningRatio");
        Double openingRatio = 70.0D;
        ((RollershutterController) dalService.getUnitRegistry().getUnit(label, location, RollershutterController.class)).updateOpeningRatio(openingRatio);
        while (true) {
            try {
                if (rollershutterRemote.getOpeningRatio().equals(openingRatio)) {
                    break;
                }
            } catch (NotAvailableException ex) {
                logger.debug("Not ready yet");
            }
            Thread.yield();
        }
        assertTrue("Opening ration has not been set in time!", rollershutterRemote.getOpeningRatio().equals(openingRatio));
    }

    /**
     * Test of notifyUpdated method, of class RollershutterRemote.
     */
    @Ignore
    public void testNotifyUpdated() {
    }
}
