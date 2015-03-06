/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.csra.dm.registry;

import de.citec.jp.JPDatabaseDirectory;
import de.citec.jp.JPDeviceClassDatabaseDirectory;
import de.citec.jp.JPDeviceConfigDatabaseDirectory;
import de.citec.jps.core.JPService;
import de.citec.jul.exception.InitializationException;
import de.citec.jul.exception.InstantiationException;
import de.citec.jul.rsb.RSBInformerInterface;
import java.io.File;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import rst.homeautomation.device.DeviceClassType;
import rst.homeautomation.device.DeviceClassType.DeviceClass;
import rst.homeautomation.device.DeviceConfigType;
import rst.homeautomation.device.DeviceConfigType.DeviceConfig;

/**
 *
 * @author mpohling
 */
public class DeviceRegistryImplTest {

    private static DeviceRegistryImpl registry;
    private static DeviceClassType.DeviceClass.Builder deviceClass;
    private static DeviceConfigType.DeviceConfig.Builder deviceConfig;

    public DeviceRegistryImplTest() {
    }

    @BeforeClass
    public static void setUpClass() throws InstantiationException, InitializationException {
        File dbFile = new File("/tmp/db/");
        File dbDeviceClasses = new File("/tmp/db/device-classes");
        File dbDeviceConfig = new File("/tmp/db/device-config");
        
        dbFile.mkdir();
        
        dbDeviceClasses.delete();
        dbDeviceConfig.delete();
        dbDeviceClasses.mkdir();
        dbDeviceConfig.mkdir();
                
        JPService.registerProperty(JPDatabaseDirectory.class, dbFile);
        JPService.registerProperty(JPDeviceConfigDatabaseDirectory.class, dbDeviceConfig);
        JPService.registerProperty(JPDeviceClassDatabaseDirectory.class, dbDeviceClasses);
        
        registry = new DeviceRegistryImpl();
        registry.init(RSBInformerInterface.InformerType.Single);
        registry.activate();

        deviceClass = DeviceClass.getDefaultInstance().newBuilderForType();
        deviceClass.setLabel("TestDeviceClassLabel");
        deviceConfig = DeviceConfig.getDefaultInstance().newBuilderForType();
        deviceConfig.setLabel("TestDeviceConfigLabel");
        deviceConfig.setSerialNumber("0001-0004-2245");
        deviceConfig.setDeviceClass(deviceClass.clone().setId("TestDeviceClassLabel"));
    }

    @AfterClass
    public static void tearDownClass() throws InterruptedException {
        registry.deactivate();
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    /**
     * Test of registerDeviceClass method, of class DeviceRegistryImpl.
     */
    @Test
    public void testRegisterDeviceClass() throws Exception {
        System.out.println("registerDeviceClass");
        registry.registerDeviceClass(deviceClass.clone().build());
        registry.getData().getDeviceClassesBuilderList().contains(deviceClass);
    }

    /**
     * Test of registerDeviceConfig method, of class DeviceRegistryImpl.
     */
    @Test
    public void testRegisterDeviceConfig() throws Exception {
        System.out.println("registerDeviceConfig");
        registry.registerDeviceConfig(deviceConfig.clone().build());
        registry.getData().getDeviceConfigsBuilderList().contains(deviceConfig);
    }

    /**
     * Test of updateDeviceClass method, of class DeviceRegistryImpl.
     */
    @Test
    public void testUpdateDeviceClass() throws Exception {
//        System.out.println("updateDeviceClass");
//        DeviceClassType.DeviceClass deviceClass = null;
//        DeviceRegistryImpl instance = new DeviceRegistryImpl();
//        instance.updateDeviceClass(deviceClass);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
    }

    /**
     * Test of updateDeviceConfig method, of class DeviceRegistryImpl.
     */
    @Test
    public void testUpdateDeviceConfig() throws Exception {
//        System.out.println("updateDeviceConfig");
//        DeviceConfigType.DeviceConfig deviceConfig = null;
//        DeviceRegistryImpl instance = new DeviceRegistryImpl();
//        instance.updateDeviceConfig(deviceConfig);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
    }

    /**
     * Test of removeDeviceConfig method, of class DeviceRegistryImpl.
     */
    @Test
    public void testRemoveDeviceConfig() throws Exception {
//        System.out.println("removeDeviceConfig");
//        DeviceConfigType.DeviceConfig deviceConfig = null;
//        DeviceRegistryImpl instance = new DeviceRegistryImpl();
//        instance.removeDeviceConfig(deviceConfig);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
    }

    /**
     * Test of removeDeviceClass method, of class DeviceRegistryImpl.
     */
    @Test
    public void testRemoveDeviceClass() throws Exception {
//        System.out.println("removeDeviceClass");
//        DeviceClassType.DeviceClass deviceClass = null;
//        DeviceRegistryImpl instance = new DeviceRegistryImpl();
//        instance.removeDeviceClass(deviceClass);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
    }

}
