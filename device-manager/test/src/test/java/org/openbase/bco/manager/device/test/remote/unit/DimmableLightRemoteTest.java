//package org.openbase.bco.manager.device.test.remote.unit;
//
///*
// * #%L
// * COMA DeviceManager Test
// * %%
// * Copyright (C) 2015 - 2016 openbase.org
// * %%
// * This program is free software: you can redistribute it and/or modify
// * it under the terms of the GNU General Public License as
// * published by the Free Software Foundation, either version 3 of the
// * License, or (at your option) any later version.
// * 
// * This program is distributed in the hope that it will be useful,
// * but WITHOUT ANY WARRANTY; without even the implied warranty of
// * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// * GNU General Public License for more details.
// * 
// * You should have received a copy of the GNU General Public
// * License along with this program.  If not, see
// * <http://www.gnu.org/licenses/gpl-3.0.html>.
// * #L%
// */
//import java.util.concurrent.TimeUnit;
//import org.openbase.bco.dal.lib.layer.unit.DimmableLightController;
//import org.openbase.bco.registry.mock.MockRegistryHolder;
//import org.openbase.jps.core.JPService;
//import org.openbase.bco.dal.lib.jp.JPHardwareSimulationMode;
//import org.openbase.bco.dal.remote.unit.DimmableLightRemote;
//import org.openbase.bco.manager.device.core.DeviceManagerLauncher;
//import org.openbase.bco.registry.mock.MockRegistry;
//import org.openbase.jul.exception.CouldNotPerformException;
//import org.openbase.jul.exception.InitializationException;
//import org.openbase.jul.exception.InvalidStateException;
//import org.openbase.jul.pattern.Remote;
//import org.junit.After;
//import org.junit.AfterClass;
//import org.junit.Before;
//import org.junit.BeforeClass;
//import org.junit.Test;
//import static org.junit.Assert.*;
//import org.junit.Ignore;
//import org.slf4j.LoggerFactory;
//import rst.domotic.state.BrightnessStateType.BrightnessState;
//import rst.domotic.state.PowerStateType.PowerState;
//
///**
// * TODO reactivate this test.
// * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
// */
//public class DimmableLightRemoteTest {
//
//    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(DimmableLightRemoteTest.class);
//
//    private static DimmableLightRemote dimmableLightRemote;
//    private static DeviceManagerLauncher deviceManagerLauncher;
//    private static MockRegistry registry;
//
//    public DimmableLightRemoteTest() {
//    }
//
//    @BeforeClass
//    public static void setUpClass() throws InitializationException, InvalidStateException, org.openbase.jul.exception.InstantiationException, CouldNotPerformException, InterruptedException {
//        JPService.setupJUnitTestMode();
//        JPService.registerProperty(JPHardwareSimulationMode.class, true);
//        MockRegistryHolder.newMockRegistry();
//
//        deviceManagerLauncher = new DeviceManagerLauncher();
//        deviceManagerLauncher.launch();
//        deviceManagerLauncher.getLaunchable().waitForInit(30, TimeUnit.SECONDS);
//
//        dimmableLightRemote = new DimmableLightRemote();
//        dimmableLightRemote.initByLabel(MockRegistry.DIMMABLE_LIGHT_LABEL);
//        dimmableLightRemote.activate();
//        dimmableLightRemote.waitForConnectionState(Remote.ConnectionState.CONNECTED);
//    }
//
//    @AfterClass
//    public static void tearDownClass() throws Throwable {
//        if (deviceManagerLauncher != null) {
//            deviceManagerLauncher.shutdown();
//        }
//        if (dimmableLightRemote != null) {
//            dimmableLightRemote.shutdown();
//        }
//        MockRegistryHolder.shutdownMockRegistry();
//    }
//
//    @Before
//    public void setUp() {
//    }
//
//    @After
//    public void tearDown() {
//    }
//
//    /**
//     * Test of notifyUpdated method, of class DimmerRemote.
//     */
//    @Ignore
//    public void testNotifyUpdated() {
//    }
//
//    /**
//     * Test of setPower method, of class DimmerRemote.
//     *
//     * @throws java.lang.Exception
//     */
//    @Test(timeout = 10000)
//    public void testSetPower() throws Exception {
//        System.out.println("setPowerState");
//        PowerState state = PowerState.newBuilder().setValue(PowerState.State.ON).build();
//        dimmableLightRemote.setPowerState(state).get();
//        dimmableLightRemote.requestData().get();
//        assertEquals("Power has not been set in time!", state, dimmableLightRemote.getData().getPowerState());
//    }
//
//    /**
//     * Test of getPower method, of class DimmerRemote.
//     */
//    @Test(timeout = 10000)
//    public void testGetPower() throws Exception {
//        System.out.println("getPowerState");
//        PowerState state = PowerState.newBuilder().setValue(PowerState.State.OFF).build();
//        ((DimmableLightController) deviceManagerLauncher.getLaunchable().getUnitControllerRegistry().get(dimmableLightRemote.getId())).updatePowerStateProvider(state);
//        dimmableLightRemote.requestData().get();
//        assertEquals("Power has not been set in time!", state, dimmableLightRemote.getPowerState());
//    }
//
//    /**
//     * Test of setDimm method, of class DimmerRemote.
//     */
//    @Test(timeout = 10000)
//    public void testSetBrightness() throws Exception {
//        System.out.println("setBrightness");
//        Double brightness = 66d;
//        BrightnessState brightnessState = BrightnessState.newBuilder().setBrightness(brightness).build();
//        dimmableLightRemote.setBrightnessState(brightnessState).get();
//        dimmableLightRemote.requestData().get();
//        assertEquals("Dimm has not been set in time!", brightness, dimmableLightRemote.getBrightnessState().getBrightness(), 0.1);
//    }
//
//    /**
//     * Test of getDimm method, of class DimmerRemote.
//     */
//    @Test(timeout = 10000)
//    public void testGetBrightness() throws Exception {
//        System.out.println("getBrightness");
//        Double brightness = 70.0d;
//        BrightnessState brightnessState = BrightnessState.newBuilder().setBrightness(brightness).build();
//        ((DimmableLightController) deviceManagerLauncher.getLaunchable().getUnitControllerRegistry().get(dimmableLightRemote.getId())).updateBrightnessStateProvider(brightnessState);
//        dimmableLightRemote.requestData().get();
//        assertEquals("Dimm has not been set in time!", brightnessState, dimmableLightRemote.getBrightnessState());
//    }
//}
