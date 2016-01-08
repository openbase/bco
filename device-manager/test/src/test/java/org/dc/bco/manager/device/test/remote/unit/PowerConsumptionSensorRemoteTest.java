/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dc.bco.manager.device.test.remote.unit;

import org.dc.bco.dal.lib.data.Location;
import org.dc.bco.dal.lib.layer.unit.PowerConsumptionSensorController;
import org.dc.bco.registry.mock.MockRegistryHolder;
import org.dc.jps.core.JPService;
import org.dc.bco.dal.lib.jp.JPHardwareSimulationMode;
import org.dc.bco.dal.remote.unit.PowerConsumptionSensorRemote;
import org.dc.bco.manager.device.core.DeviceManagerLauncher;
import org.dc.bco.registry.mock.MockRegistry;
import org.dc.jul.exception.CouldNotPerformException;
import org.dc.jul.exception.InitializationException;
import org.dc.jul.exception.InvalidStateException;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.Ignore;
import org.slf4j.LoggerFactory;
import rst.homeautomation.state.PowerConsumptionStateType.PowerConsumptionState;

/**
 *
 * @author thuxohl
 */
public class PowerConsumptionSensorRemoteTest {

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(PowerConsumptionSensorRemoteTest.class);

    private static PowerConsumptionSensorRemote powerConsumptionRemote;
    private static DeviceManagerLauncher deviceManagerLauncher;
    private static MockRegistry registry;
    private static Location locaton;
    private static String label;

    public PowerConsumptionSensorRemoteTest() {
    }

    @BeforeClass
    public static void setUpClass() throws InitializationException, InvalidStateException, org.dc.jul.exception.InstantiationException, CouldNotPerformException, InterruptedException {
        JPService.registerProperty(JPHardwareSimulationMode.class, true);
        registry = MockRegistryHolder.newMockRegistry();

        deviceManagerLauncher = new DeviceManagerLauncher();
        deviceManagerLauncher.launch();
        

        locaton = new Location(registry.getLocation());
        label = MockRegistry.POWER_CONSUMPTION_LABEL;

        powerConsumptionRemote = new PowerConsumptionSensorRemote();
        powerConsumptionRemote.init(label, locaton);
        powerConsumptionRemote.activate();
    }

    @AfterClass
    public static void tearDownClass() throws CouldNotPerformException, InterruptedException {
        if (deviceManagerLauncher != null) {
            deviceManagerLauncher.shutdown();
        }
        if (powerConsumptionRemote != null) {
            powerConsumptionRemote.shutdown();
        }
        if (registry != null) {
            MockRegistryHolder.shutdownMockRegistry();
        }
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
    @Test(timeout = 60000)
    public void testGetPowerConsumption() throws Exception {
        System.out.println("getPowerConsumption");
        double consumption = 200d;
        double voltage = 100d;
        double current = 2d;
        PowerConsumptionState state = PowerConsumptionState.newBuilder().setConsumption(consumption).setCurrent(current).setVoltage(voltage).build();
        ((PowerConsumptionSensorController) deviceManagerLauncher.getDeviceManager().getUnitControllerRegistry().get(powerConsumptionRemote.getId())).updatePowerConsumption(state);
        powerConsumptionRemote.requestStatus();
        Assert.assertEquals("The getter for the power consumption returns the wrong value!", state, powerConsumptionRemote.getPowerConsumption());
    }
}
