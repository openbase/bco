/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.remote.unit;

import de.citec.dal.registry.MockRegistry;
import de.citec.dal.DALService;
import de.citec.dal.data.Location;
import de.citec.dal.hal.unit.BrightnessSensorController;
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

/**
 *
 * @author thuxohl
 */
public class BrightnessSensorRemoteTest {

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(BrightnessSensorRemoteTest.class);

    private static BrightnessSensorRemote brightnessSensorRemote;
    private static DALService dalService;
    private static MockRegistry registry;
    private static Location location;
    private static String label;

    public BrightnessSensorRemoteTest() {
    }

    @BeforeClass
    public static void setUpClass() throws InitializationException, InvalidStateException, de.citec.jul.exception.InstantiationException, CouldNotPerformException {
        JPService.registerProperty(JPHardwareSimulationMode.class, true);
        registry = new MockRegistry();
        
        dalService = new DALService();
        dalService.activate();

        location = new Location(registry.getLocation());
        label = MockRegistry.BRIGHTNESS_SENSOR_LABEL;

        brightnessSensorRemote = new BrightnessSensorRemote();
        brightnessSensorRemote.init(label, location);
        brightnessSensorRemote.activate();
    }

    @AfterClass
    public static void tearDownClass() throws CouldNotPerformException, InterruptedException {
        dalService.shutdown();
        try {
            brightnessSensorRemote.deactivate();
        } catch (InterruptedException ex) {
            logger.warn("Could not deactivate brightness sensor remote: ", ex);
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
     * Test of notifyUpdated method, of class BrightnessSensorRemote.
     */
    @Ignore
    public void testNotifyUpdated() {
    }

    /**
     * Test of getBrightness method, of class BrightnessSensorRemote.
     *
     * @throws java.lang.Exception
     */
    @Test(timeout = 3000)
    public void testGetBrightness() throws Exception {
        System.out.println("getBrightness");
        double brightness = 0.5;
        ((BrightnessSensorController) dalService.getUnitRegistry().getUnit(label, location, BrightnessSensorController.class)).updateBrightness((float) brightness);
        while (true) {
            try {
                if (brightnessSensorRemote.getBrightness() == brightness) {
                    break;
                }
            } catch (NotAvailableException ex) {
                logger.debug("Not ready yet");
            }
            Thread.yield();
        }
        assertTrue("The getter for the brightness returns the wrong value!", brightnessSensorRemote.getBrightness() == brightness);
    }
}
