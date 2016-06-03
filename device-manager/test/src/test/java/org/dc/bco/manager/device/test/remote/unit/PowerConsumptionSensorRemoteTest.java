package org.dc.bco.manager.device.test.remote.unit;

/*
 * #%L
 * COMA DeviceManager Test
 * %%
 * Copyright (C) 2015 - 2016 DivineCooperation
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */
import java.util.concurrent.TimeUnit;
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
import org.dc.jul.pattern.Remote;
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
        deviceManagerLauncher.getDeviceManager().waitForInit(30, TimeUnit.SECONDS);

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
        MockRegistryHolder.shutdownMockRegistry();
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
    @Test(timeout = 10000)
    public void testGetPowerConsumption() throws Exception {
        System.out.println("getPowerConsumption");
        double consumption = 200d;
        double voltage = 100d;
        double current = 2d;
        powerConsumptionRemote.waitForConnectionState(Remote.RemoteConnectionState.CONNECTED);
        PowerConsumptionState state = PowerConsumptionState.newBuilder().setConsumption(consumption).setCurrent(current).setVoltage(voltage).build();
        ((PowerConsumptionSensorController) deviceManagerLauncher.getDeviceManager().getUnitControllerRegistry().get(powerConsumptionRemote.getId())).updatePowerConsumptionProvider(state);
        powerConsumptionRemote.requestData().get();
        Assert.assertEquals("The getter for the power consumption returns the wrong value!", state, powerConsumptionRemote.getPowerConsumption());
    }
}
