package org.openbase.bco.manager.device.test;

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
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.openbase.bco.registry.mock.MockRegistryHolder;
import org.openbase.jps.core.JPService;
import org.openbase.jps.exception.JPServiceException;
import org.openbase.jul.exception.InitializationException;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public class DalRegisterDeviceTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(DalRegisterDeviceTest.class);

    public DalRegisterDeviceTest() {

    }

    @BeforeClass
    public static void setUpClass() throws Throwable {
        try {
            JPService.setupJUnitTestMode();
            MockRegistryHolder.newMockRegistry();
        } catch (Throwable ex) {
            throw ExceptionPrinter.printHistoryAndReturnThrowable(ex, LOGGER);
        }
    }

    @AfterClass
    public static void tearDownClass() throws Throwable {
        try {
            MockRegistryHolder.shutdownMockRegistry();
        } catch (Throwable ex) {
            throw ExceptionPrinter.printHistoryAndReturnThrowable(ex, LOGGER);
        }
    }

    @Before
    public void setUp() throws InitializationException, InstantiationException {
    }

    @After
    public void tearDown() {
    }

    // TODO thuxohl: test get timeouts on older maschines ;)
    // Please try to figure out which part triggers the race condition and remove timeouts later on.
//    @Test (timeout = 10000)
//    public void testRegisterDeviceWhileRunning() throws Exception {
//        System.out.println("testRegisterDeviceWhileRunning");
//        DeviceManagerLauncher instance = new DeviceManagerLauncher();
//        instance.launch();
//
//        Thread.sleep(1000);
//
//        DeviceRegistryRemote remote = new DeviceRegistryRemote();
//        remote.init();
//        remote.activate();
//
//        DeviceClass deviceClass = remote.registerDeviceClass(MockRegistry.getDeviceClass("TestRegisterDeviceWhileRunnint", "DeviceManagerLauncherAndCoKG123456", "DeviceManagerLauncherAndCoKG"));
//        ArrayList<UnitConfig> units = new ArrayList<>();
//        units.add(MockRegistry.getUnitConfig(UnitTemplateType.UnitTemplate.UnitType.AMBIENT_LIGHT, "DeviceManagerLauncherRegisterWhileRunningUnit"));
//        DeviceConfig deviceConfig = remote.registerDeviceConfig(MockRegistry.getDeviceConfig("DeviceManagerLauncherTestRegisterWhileRunningDeviceConfig", "DeviceManagerLauncherTestSerialNumber", deviceClass, units));
//        UnitConfig unit = deviceConfig.getUnitConfig(0);
//        unit = unit.toBuilder().setLabel("ShorterLabel").build();
//        deviceConfig = deviceConfig.toBuilder().clearUnitConfig().addUnitConfig(unit).build();
//        deviceConfig = remote.updateDeviceConfig(deviceConfig);
//
//        Thread.sleep(1000);
//        AmbientLightRemote ambientLightRemote = new AmbientLightRemote();
//        ambientLightRemote.init(deviceConfig.getUnitConfig(0).getScope());
//        ambientLightRemote.activate();
//
//        Thread.sleep(1000);
//        ambientLightRemote.setPower(PowerStateType.PowerState.State.ON);
//        assertTrue(ambientLightRemote.isConnected());
//
////        unit = unit.toBuilder().setMetaConfig(MetaConfigType.MetaConfig.newBuilder().addEntry(EntryType.Entry.newBuilder().setKey("Key").setValue("Value"))).build();
////        deviceConfig = deviceConfig.toBuilder().clearUnitConfig().addUnitConfig(unit).build();
////        deviceConfig = remote.updateDeviceConfig(deviceConfig);
////        Thread.sleep(1000);
////
////        Thread.sleep(1000);
////        assertTrue(ambientLightRemote.isConnected());
//
//        ambientLightRemote.shutdown();
//        remote.shutdown();
//        instance.shutdown();
//    }
//    @Test
//    public void test() throws Exception {
//        assertTrue(true);
//    }
}
