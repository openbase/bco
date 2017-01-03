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
import org.junit.After;
import org.junit.AfterClass;
import static org.junit.Assert.assertEquals;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openbase.bco.dal.lib.jp.JPHardwareSimulationMode;
import org.openbase.bco.dal.lib.layer.unit.LightController;
import org.openbase.bco.dal.remote.unit.LightRemote;
import org.openbase.bco.manager.device.core.DeviceManagerLauncher;
import org.openbase.bco.registry.mock.MockRegistry;
import org.openbase.bco.registry.mock.MockRegistryHolder;
import org.openbase.jps.core.JPService;
import org.openbase.jps.exception.JPServiceException;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InitializationException;
import org.openbase.jul.exception.InvalidStateException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.pattern.Remote;
import org.slf4j.LoggerFactory;
import rst.domotic.state.PowerStateType.PowerState;

/**
 *
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class LightRemoteTest {

    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(LightRemoteTest.class);

    private static LightRemote lightRemote;
    private static DeviceManagerLauncher deviceManagerLauncher;
    private static MockRegistry registry;
    private static String label;

    public LightRemoteTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Throwable {
        try {
            JPService.setupJUnitTestMode();
            JPService.registerProperty(JPHardwareSimulationMode.class, true);
            System.out.println("setUpClass thread: " + Thread.currentThread().getName());
            registry = MockRegistryHolder.newMockRegistry();

            deviceManagerLauncher = new DeviceManagerLauncher();
            deviceManagerLauncher.launch();
            deviceManagerLauncher.getLaunchable().waitForInit(30, TimeUnit.SECONDS);

            label = MockRegistry.LIGHT_LABEL;

            lightRemote = new LightRemote();
            lightRemote.initByLabel(label);
            lightRemote.activate();
            System.out.println("#### wait for light remote...");
            lightRemote.waitForConnectionState(Remote.ConnectionState.CONNECTED, 30000);
            System.out.println("#### successfull started!");
        } catch (Throwable ex) {
            ExceptionPrinter.printHistoryAndReturnThrowable(ex, LOGGER);
        }
    }

    @AfterClass
    public static void tearDownClass() throws Throwable {
        try {
            System.out.println("#### tearDownClass");
            if (deviceManagerLauncher != null) {
                deviceManagerLauncher.shutdown();
            }
            if (lightRemote != null) {
                lightRemote.shutdown();
            }
            MockRegistryHolder.shutdownMockRegistry();
            System.out.println("#### finished");
        } catch (Throwable ex) {
            ExceptionPrinter.printHistoryAndReturnThrowable(ex, LOGGER);
        }
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    /**
     * Test of setPowerState method, of class LightRemote.
     *
     * @throws java.lang.Exception
     */
    @Test(timeout = 5000)
    public void testSetPowerState() throws Exception {
        System.out.println("setPowerState");
        PowerState state = PowerState.newBuilder().setValue(PowerState.State.ON).build();
        lightRemote.setPowerState(state).get();
        lightRemote.requestData().get();
        assertEquals("Power has not been set in time!", state, lightRemote.getData().getPowerState());
    }

    /**
     * Test of gsetPowerState method, of class LightRemote.
     *
     * @throws java.lang.Exception
     */
    @Test(timeout = 10000)
    public void testGetPowerState() throws Exception {
        System.out.println("getPowerState");
        PowerState state = PowerState.newBuilder().setValue(PowerState.State.OFF).build();
        ((LightController) deviceManagerLauncher.getLaunchable().getUnitControllerRegistry().get(lightRemote.getId())).updatePowerStateProvider(state);
        lightRemote.requestData().get();
        assertEquals("Light has not been set in time!", state, lightRemote.getPowerState());
    }
}
