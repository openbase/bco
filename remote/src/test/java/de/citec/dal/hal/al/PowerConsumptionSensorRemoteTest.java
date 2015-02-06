/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.hal.al;

import de.citec.dal.DALService;
import de.citec.dal.data.Location;
import de.citec.dal.hal.device.fibaro.F_MotionSensorController;
import de.citec.dal.hal.device.plugwise.PW_PowerPlugController;
import de.citec.dal.hal.unit.PowerConsumptionSensorController;
import de.citec.dal.util.DALRegistry;
import de.citec.jps.core.JPService;
import de.citec.jps.properties.JPHardwareSimulationMode;
import de.citec.jul.exception.VerificationFailedException;
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

    private static final Location LOCATION = new Location("paradise");
    private static final String LABEL = "Power_Consumption_Unit_Test";

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(PowerConsumptionSensorRemoteTest.class);

    private PowerConsumptionSensorRemote powerConsumptionRemote;
    private DALService dalService;

    public PowerConsumptionSensorRemoteTest() {
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
        dalService = new DALService(new PowerConsumptionSensorRemoteTest.DeviceInitializerImpl());
        dalService.activate();

        powerConsumptionRemote = new PowerConsumptionSensorRemote();
        powerConsumptionRemote.init(LABEL, LOCATION);
        powerConsumptionRemote.activate();
    }

    @After
    public void tearDown() {
        dalService.deactivate();
        try {
            powerConsumptionRemote.deactivate();
        } catch (InterruptedException ex) {
            logger.warn("Could not deactivate power consumption remote: ", ex);
        }
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
    @Test
    public void testGetPowerConsumption() throws Exception {
        System.out.println("getPowerConsumption");
        float consumption = 0.0F;
        ((PowerConsumptionSensorController) dalService.getRegistry().getUnits(PowerConsumptionSensorController.class).iterator().next()).updatePowerConsumption(consumption);
        while (!(powerConsumptionRemote.getPowerConsumption() == consumption)) {
            Thread.yield();
        }
        assertTrue("The getter for the power consumption returns the wrong value!", powerConsumptionRemote.getPowerConsumption() == consumption);
    }

    public static class DeviceInitializerImpl implements de.citec.dal.util.DeviceInitializer {

        @Override
        public void initDevices(final DALRegistry registry) {

            try {
                registry.register(new PW_PowerPlugController("PW_PowerPlug_000", LABEL, LOCATION));
            } catch (de.citec.jul.exception.InstantiationException | VerificationFailedException ex) {
                logger.warn("Could not initialize unit test device!", ex);
            }
        }
    }
}
