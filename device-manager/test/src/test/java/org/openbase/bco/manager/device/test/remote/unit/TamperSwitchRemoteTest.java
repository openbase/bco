package org.openbase.bco.manager.device.test.remote.unit;

/*
 * #%L
 * COMA DeviceManager Test
 * %%
 * Copyright (C) 2015 - 2016 openbase.org
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
import org.openbase.bco.registry.mock.MockRegistryHolder;
import org.openbase.jps.core.JPService;
import org.openbase.bco.dal.lib.jp.JPHardwareSimulationMode;
import org.openbase.bco.dal.lib.layer.unit.TamperSwitchController;
import org.openbase.bco.dal.remote.unit.TamperSwitchRemote;
import org.openbase.bco.manager.device.core.DeviceManagerLauncher;
import org.openbase.bco.registry.mock.MockRegistry;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InitializationException;
import org.openbase.jul.exception.InvalidStateException;
import org.openbase.jul.pattern.Remote;
import org.junit.After;
import org.junit.AfterClass;
import static org.junit.Assert.assertEquals;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.Ignore;
import org.slf4j.LoggerFactory;
import rst.homeautomation.state.TamperStateType.TamperState;

/**
 *
 * @author thuxohl
 */
public class TamperSwitchRemoteTest {

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(TamperSwitchRemoteTest.class);

    private static TamperSwitchRemote tamperSwitchRemote;
    private static DeviceManagerLauncher deviceManagerLauncher;
    private static MockRegistry registry;
    private static String label;

    public TamperSwitchRemoteTest() {
    }

    @BeforeClass
    public static void setUpClass() throws InitializationException, InvalidStateException, org.openbase.jul.exception.InstantiationException, CouldNotPerformException, InterruptedException {
        JPService.registerProperty(JPHardwareSimulationMode.class, true);
        registry = MockRegistryHolder.newMockRegistry();

        deviceManagerLauncher = new DeviceManagerLauncher();
        deviceManagerLauncher.launch();
        deviceManagerLauncher.getDeviceManager().waitForInit(30, TimeUnit.SECONDS);

        label = MockRegistry.TAMPER_SWITCH_LABEL;

        tamperSwitchRemote = new TamperSwitchRemote();
        tamperSwitchRemote.initByLabel(label);
        tamperSwitchRemote.activate();
        tamperSwitchRemote.waitForConnectionState(Remote.ConnectionState.CONNECTED);
    }

    @AfterClass
    public static void tearDownClass() throws CouldNotPerformException, InterruptedException {
        if (deviceManagerLauncher != null) {
            deviceManagerLauncher.shutdown();
        }
        if (tamperSwitchRemote != null) {
            tamperSwitchRemote.shutdown();
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
    @Test(timeout = 10000)
    public void testGetTamperState() throws Exception {
        System.out.println("getTamperState");
        TamperState tamperState = TamperState.newBuilder().setValue(TamperState.State.TAMPER).build();
        ((TamperSwitchController) deviceManagerLauncher.getDeviceManager().getUnitControllerRegistry().get(tamperSwitchRemote.getId())).updateTamperProvider(tamperState);
        tamperSwitchRemote.requestData().get();
        assertEquals("The getter for the tamper switch state returns the wrong value!", tamperState.getValue(), tamperSwitchRemote.getTamper().getValue());
    }
}
