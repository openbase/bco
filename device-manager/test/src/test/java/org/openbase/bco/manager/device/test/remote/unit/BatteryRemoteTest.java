package org.openbase.bco.manager.device.test.remote.unit;

/*
 * #%L
 * BCO Manager Device Test
 * %%
 * Copyright (C) 2015 - 2017 openbase.org
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
import org.junit.After;
import org.junit.AfterClass;
import static org.junit.Assert.assertEquals;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.openbase.bco.dal.lib.jp.JPHardwareSimulationMode;
import org.openbase.bco.dal.lib.layer.unit.BatteryController;
import org.openbase.bco.dal.remote.unit.BatteryRemote;
import org.openbase.bco.manager.device.core.DeviceManagerLauncher;
import org.openbase.bco.registry.mock.MockRegistry;
import org.openbase.bco.registry.mock.MockRegistryHolder;
import org.openbase.jps.core.JPService;
import org.openbase.jps.exception.JPServiceException;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InitializationException;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.exception.InvalidStateException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.pattern.Remote;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rst.domotic.state.BatteryStateType.BatteryState;

/**
 *
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class BatteryRemoteTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(BatteryRemoteTest.class);

    private static BatteryRemote batteryRemote;
    private static DeviceManagerLauncher deviceManagerLauncher;
    private static MockRegistry registry;
    private static String label;

    public BatteryRemoteTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Throwable {
        try {
            JPService.setupJUnitTestMode();
            JPService.registerProperty(JPHardwareSimulationMode.class, true);
            registry = MockRegistryHolder.newMockRegistry();

            deviceManagerLauncher = new DeviceManagerLauncher();
            deviceManagerLauncher.launch();
            deviceManagerLauncher.getLaunchable().waitForInit(30, TimeUnit.SECONDS);

            label = MockRegistry.BATTERY_LABEL;

            batteryRemote = new BatteryRemote();
            batteryRemote.initByLabel(label);
            batteryRemote.activate();
            batteryRemote.waitForConnectionState(Remote.ConnectionState.CONNECTED);
        } catch (Throwable ex) {
            throw ExceptionPrinter.printHistoryAndReturnThrowable(ex, LOGGER);
        }
    }

    @AfterClass
    public static void tearDownClass() throws Throwable {
        try {
            if (deviceManagerLauncher != null) {
                deviceManagerLauncher.shutdown();
            }
            if (batteryRemote != null) {
                batteryRemote.shutdown();
            }
            MockRegistryHolder.shutdownMockRegistry();
        } catch (Throwable ex) {
            throw ExceptionPrinter.printHistoryAndReturnThrowable(ex, LOGGER);
        }
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    /**
     * Test of notifyUpdated method, of class BatteryRemote.
     */
    @Ignore
    public void testNotifyUpdated() {
    }

    /**
     * Test of getBattaryLevel method, of class BatteryRemote.
     *
     * @throws java.lang.Exception
     */
    @Test(timeout = 10000)
    public void testGetBatteryLevel() throws Exception {
        System.out.println("getBatteryLevel");
        double level = 34.0;
        BatteryState state = BatteryState.newBuilder().setLevel(level).setValue(BatteryState.State.OK).build();
        ((BatteryController) deviceManagerLauncher.getLaunchable().getUnitControllerRegistry().get(batteryRemote.getId())).updateBatteryStateProvider(state);
        batteryRemote.requestData().get();
        assertEquals("The getter for the battery level returns the wrong value!", state, batteryRemote.getBatteryState());
    }
}
