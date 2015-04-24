/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.remote.unit;

import de.citec.dal.DALService;
import de.citec.dal.data.Location;
import de.citec.dal.hal.unit.PowerConsumptionSensorController;
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
public class PowerConsumptionSensorRemoteTest {

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(PowerConsumptionSensorRemoteTest.class);

    private static PowerConsumptionSensorRemote powerConsumptionRemote;
    private static DALService dalService;
    private static MockRegistryHolder registry;
    private static Location locaton;
    private static String label;

    public PowerConsumptionSensorRemoteTest() {
    }

    @BeforeClass
    public static void setUpClass() throws InitializationException, InvalidStateException, de.citec.jul.exception.InstantiationException, CouldNotPerformException {
        JPService.registerProperty(JPHardwareSimulationMode.class, true);
        dalService = new DALService();
        dalService.activate();

        registry = new MockRegistryHolder();
        locaton = new Location(registry.getLocation());
        label = MockRegistryHolder.POWER_CONSUMPTION_LABEL;

        powerConsumptionRemote = new PowerConsumptionSensorRemote();
        powerConsumptionRemote.init(label, locaton);
        powerConsumptionRemote.activate();
    }

    @AfterClass
    public static void tearDownClass() throws CouldNotPerformException, InterruptedException {
        dalService.shutdown();
        try {
            powerConsumptionRemote.deactivate();
        } catch (InterruptedException ex) {
            logger.warn("Could not deactivate power consumption remote: ", ex);
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
     * Test of notifyUpdated method, of class PowerConsumptionSensorRemote.
     */
    @Ignore
    public void testNotifyUpdated() {
    }

    /**
     * Test of getPowerConsumption method, of class
     * PowerConsumptionSensorRemote.
     *
     * @throws java.lang.Exception
     */
    @Test(timeout = 3000)
    public void testGetPowerConsumption() throws Exception {
        System.out.println("getPowerConsumption");
        float consumption = 0.0F;
        ((PowerConsumptionSensorController) dalService.getUnitRegistry().getUnit(label, locaton, PowerConsumptionSensorController.class)).updatePowerConsumption(consumption);
        while (true) {
            try {
                if (powerConsumptionRemote.getPowerConsumption() == consumption) {
                    break;
                }
            } catch (NotAvailableException ex) {
                logger.debug("Not ready yet");
            }
            Thread.yield();
        }
        assertTrue("The getter for the power consumption returns the wrong value!", powerConsumptionRemote.getPowerConsumption() == consumption);
    }
}
