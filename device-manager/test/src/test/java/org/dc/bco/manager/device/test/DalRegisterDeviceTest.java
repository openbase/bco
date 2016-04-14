/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dc.bco.manager.device.test;

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

import org.dc.bco.dal.remote.unit.AmbientLightRemote;
import org.dc.bco.registry.device.remote.DeviceRegistryRemote;
import org.dc.jps.exception.JPServiceException;
import org.dc.jul.exception.InitializationException;
import org.dc.jul.exception.InstantiationException;
import java.util.ArrayList;
import org.dc.bco.manager.device.core.DeviceManagerLauncher;
import org.dc.bco.registry.mock.MockRegistry;
import org.junit.After;
import org.junit.AfterClass;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rst.homeautomation.device.DeviceClassType.DeviceClass;
import rst.homeautomation.device.DeviceConfigType.DeviceConfig;
import rst.homeautomation.state.PowerStateType;
import rst.homeautomation.unit.UnitConfigType.UnitConfig;
import rst.homeautomation.unit.UnitTemplateType;

/**
 *
 * @author mpohling
 */
public class DalRegisterDeviceTest {

    private static final Logger logger = LoggerFactory.getLogger(DalRegisterDeviceTest.class);

    private static MockRegistry registry;

    public DalRegisterDeviceTest() {

    }

    @BeforeClass
    public static void setUpClass() throws InstantiationException, JPServiceException {
        registry = new MockRegistry();
    }

    @AfterClass
    public static void tearDownClass() {
        if (registry != null) {
            registry.shutdown();
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
