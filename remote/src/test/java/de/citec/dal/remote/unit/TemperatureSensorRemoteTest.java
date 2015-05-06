/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.remote.unit;

import de.citec.dal.registry.MockRegistry;
import de.citec.dal.DALService;
import de.citec.dal.data.Location;
import de.citec.dal.hal.unit.TemperatureSensorController;
import de.citec.dal.registry.MockFactory;
import de.citec.jps.core.JPService;
import de.citec.jps.properties.JPHardwareSimulationMode;
import de.citec.jul.exception.CouldNotPerformException;
import de.citec.jul.exception.InitializationException;
import de.citec.jul.exception.InvalidStateException;
import de.citec.jul.exception.InstantiationException;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.Ignore;
import org.slf4j.LoggerFactory;

/**
 *
 * @author thuxohl
 */
public class TemperatureSensorRemoteTest {

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(TemperatureSensorRemoteTest.class);

    private static TemperatureSensorRemote temperatureSensorRemote;
    private static DALService dalService;
    private static MockRegistry registry;
    private static Location location;
    private static String label;

    public TemperatureSensorRemoteTest() {
    }

    @BeforeClass
    public static void setUpClass() throws InitializationException, InvalidStateException, InstantiationException, CouldNotPerformException {
        JPService.registerProperty(JPHardwareSimulationMode.class, true);
        registry = MockFactory.newMockRegistry();

        dalService = new DALService();
        dalService.init();
        dalService.activate();

        location = new Location(registry.getLocation());
        label = MockRegistry.TEMPERATURE_SENSOR_LABEL;

        temperatureSensorRemote = new TemperatureSensorRemote();
        temperatureSensorRemote.init(label, location);
        temperatureSensorRemote.activate();
    }

    @AfterClass
    public static void tearDownClass() throws CouldNotPerformException {
        if (dalService != null) {
            dalService.shutdown();
        }
        if (temperatureSensorRemote != null) {
            temperatureSensorRemote.shutdown();
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
     * Test of notifyUpdated method, of class TemperatureSensorRemote.
     */
    @Ignore
    public void testNotifyUpdated() {
    }

    /**
     * Test of getTemperature method, of class TemperatureSensorRemote.
     *
     * @throws java.lang.Exception
     */
    @Test
    public void testGetTemperature() throws Exception {
        System.out.println("getTemperature");
        float temperature = 37.0F;
        ((TemperatureSensorController) dalService.getUnitRegistry().get(temperatureSensorRemote.getId())).updateTemperature(temperature);
        temperatureSensorRemote.requestStatus();
        Assert.assertEquals("The getter for the tamper switch state returns the wrong value!", temperature, temperatureSensorRemote.getTemperature(), 0.1);
    }
}
