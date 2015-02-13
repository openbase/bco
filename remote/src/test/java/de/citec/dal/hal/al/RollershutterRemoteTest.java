/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.hal.al;

import de.citec.dal.DALService;
import de.citec.dal.data.Location;
import de.citec.dal.hal.device.hager.HA_TYA628CController;
import de.citec.dal.util.DALRegistry;
import de.citec.jps.core.JPService;
import de.citec.jps.properties.JPHardwareSimulationMode;
import de.citec.jul.exception.InstantiationException;
import de.citec.jul.exception.NotAvailableException;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.Ignore;
import org.slf4j.LoggerFactory;
import rst.homeautomation.states.ShutterType;

/**
 *
 * @author thuxohl
 */
public class RollershutterRemoteTest {

    private static final Location LOCATION = new Location("paradise");
    private static final String LABEL = "Rollershutter_Unit_Test";
    private static final String[] ROLLERSHUTTER = {"Rollershutter_1", "Rollershutter_2", "Rollershutter_3", "Rollershutter_4", "Rollershutter_5", "Rollershutter_6", "Rollershutter_7", "Rollershutter_8"};

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(RollershutterRemoteTest.class);

    private RollershutterRemote rollershutterRemote;
    private DALService dalService;

    public RollershutterRemoteTest() {
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
        dalService = new DALService(new RollershutterRemoteTest.DeviceInitializerImpl());
        dalService.activate();

        rollershutterRemote = new RollershutterRemote();
        rollershutterRemote.init(ROLLERSHUTTER[0], LOCATION);
        rollershutterRemote.activate();
    }

    @After
    public void tearDown() {
        dalService.deactivate();
        try {
            rollershutterRemote.deactivate();
        } catch (InterruptedException ex) {
            logger.warn("Could not deactivate rollershutter remote: ", ex);
        }
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
        rollershutterRemote.setShutterState(state);
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
        assertTrue("Color has not been set in time!", rollershutterRemote.getData().getShutterState().getState().equals(state));
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
        rollershutterRemote.setShutterState(state);
        while (true) {
            try {
                if (rollershutterRemote.getShutterState().equals(state)) {
                    break;
                }
            } catch (NotAvailableException ex) {
                logger.debug("Not ready yet");
            }
            Thread.yield();
        }
        assertTrue("Color has not been set in time!", rollershutterRemote.getShutterState().equals(state));
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
        assertTrue("Color has not been set in time!", rollershutterRemote.getData().getOpeningRatio() == openingRatio);
    }
    
    /**
     * Test of setOpeningRatio method, of class RollershutterRemote.
     *
     * @throws java.lang.Exception
     */
    @Test(timeout = 3000)
    public void testGetOpeningRatio() throws Exception {
        System.out.println("getOpeningRatio");
        double openingRatio = 70.0D;
        rollershutterRemote.setOpeningRatio(openingRatio);
        while (true) {
            try {
                if (rollershutterRemote.getOpeningRatio() == openingRatio) {
                    break;
                }
            } catch (NotAvailableException ex) {
                logger.debug("Not ready yet");
            }
            Thread.yield();
        }
        assertTrue("Color has not been set in time!", rollershutterRemote.getOpeningRatio() == openingRatio);
    }

    /**
     * Test of notifyUpdated method, of class RollershutterRemote.
     */
    @Ignore
    public void testNotifyUpdated() {
    }

    public static class DeviceInitializerImpl implements de.citec.dal.util.DeviceInitializer {

        @Override
        public void initDevices(final DALRegistry registry) {

            try {
                registry.register(new HA_TYA628CController("HA_TYA628C_000", LABEL, ROLLERSHUTTER, LOCATION));
            } catch (InstantiationException ex) {
                logger.warn("Could not initialize unit test device!", ex);
            }
        }
    }
}
