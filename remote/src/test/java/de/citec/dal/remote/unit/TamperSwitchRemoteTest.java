/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.remote.unit;

import de.citec.dal.registry.MockRegistry;
import de.citec.dal.DALService;
import de.citec.dal.data.Location;
import de.citec.dal.hal.unit.TamperSwitchController;
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

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(TamperSwitchRemoteTest.class);

    private static TamperSwitchRemote tamperSwitchRemote;
    private static DALService dalService;
    private static MockRegistry registry;
    private static Location location;
    private static String label;

    public TamperSwitchRemoteTest() {
    }

    @BeforeClass
    public static void setUpClass() throws InitializationException, InvalidStateException, de.citec.jul.exception.InstantiationException, CouldNotPerformException {
        JPService.registerProperty(JPHardwareSimulationMode.class, true);
        registry = new MockRegistry();
        
        dalService = new DALService();
        dalService.init();
        dalService.activate();

        location = new Location(registry.getLocation());
        label = MockRegistry.TAMPER_SWITCH_LABEL;

        tamperSwitchRemote = new TamperSwitchRemote();
        tamperSwitchRemote.init(label, location);
        tamperSwitchRemote.activate();
    }

    @AfterClass
    public static void tearDownClass() throws CouldNotPerformException, InterruptedException {
        dalService.shutdown();
        try {
            tamperSwitchRemote.deactivate();
        } catch (InterruptedException ex) {
            logger.warn("Could not deactivate tamper switch remote: ", ex);
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
        ((TamperSwitchController) dalService.getUnitRegistry().get(tamperSwitchRemote.getId())).updateTamper(state);
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
}
