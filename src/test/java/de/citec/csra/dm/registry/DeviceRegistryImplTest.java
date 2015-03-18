/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.csra.dm.registry;

import de.citec.csra.dm.remote.DeviceRegistryRemote;
import de.citec.jp.JPDatabaseDirectory;
import de.citec.jp.JPDeviceClassDatabaseDirectory;
import de.citec.jp.JPDeviceConfigDatabaseDirectory;
import de.citec.jp.JPDeviceRegistryScope;
import de.citec.jps.core.JPService;
import de.citec.jul.exception.CouldNotPerformException;
import de.citec.jul.exception.InitializationException;
import de.citec.jul.exception.InstantiationException;
import de.citec.jul.exception.InvalidStateException;
import de.citec.jul.exception.NotAvailableException;
import de.citec.jul.pattern.Observable;
import de.citec.jul.pattern.Observer;
import de.citec.jul.rsb.RSBInformerInterface;
import java.io.File;
import java.io.IOException;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import static junit.framework.TestCase.assertTrue;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rsb.Scope;
import rst.homeautomation.device.DeviceClassType;
import rst.homeautomation.device.DeviceClassType.DeviceClass;
import rst.homeautomation.device.DeviceConfigType;
import rst.homeautomation.device.DeviceConfigType.DeviceConfig;
import rst.homeautomation.registry.DeviceRegistryType;

/**
 *
 * @author mpohling
 */
public class DeviceRegistryImplTest {

    private static final Logger logger = LoggerFactory.getLogger(DeviceRegistryImplTest.class);

    private static DeviceRegistryImpl registry;
    private static DeviceClassType.DeviceClass.Builder deviceClass;
    private static DeviceConfigType.DeviceConfig.Builder deviceConfig;

    private static DeviceClassType.DeviceClass.Builder deviceClassRemote;
    private static DeviceClassType.DeviceClass.Builder returnValue;
    private static DeviceConfigType.DeviceConfig.Builder deviceConfigRemote;
    private static DeviceRegistryRemote remote;

    public DeviceRegistryImplTest() {
    }

    @BeforeClass
    public static void setUpClass() throws InstantiationException, InitializationException, IOException, InvalidStateException {
        File dbFile = new File("/tmp/db/");
        File dbDeviceClasses = new File("/tmp/db/device-classes");
        File dbDeviceConfig = new File("/tmp/db/device-config");
        Scope scope = new Scope("/test/registry/devicemanager");

        dbFile.mkdir();

        FileUtils.deleteDirectory(dbDeviceClasses);
        FileUtils.deleteDirectory(dbDeviceConfig);
        dbDeviceClasses.mkdir();
        dbDeviceConfig.mkdir();

        JPService.registerProperty(JPDeviceRegistryScope.class, scope);
        JPService.registerProperty(JPDatabaseDirectory.class, dbFile);
        JPService.registerProperty(JPDeviceConfigDatabaseDirectory.class, dbDeviceConfig);
        JPService.registerProperty(JPDeviceClassDatabaseDirectory.class, dbDeviceClasses);

        registry = new DeviceRegistryImpl();
        registry.init(RSBInformerInterface.InformerType.Single);
        registry.activate();

        deviceClass = DeviceClass.getDefaultInstance().newBuilderForType();
        deviceClass.setLabel("TestDeviceClassLabel");
        deviceClass.setProductNumber("TDCL-001");
        deviceConfig = DeviceConfig.getDefaultInstance().newBuilderForType();
        deviceConfig.setLabel("TestDeviceConfigLabel");
        deviceConfig.setSerialNumber("0001-0004-2245");
        deviceConfig.setDeviceClass(deviceClass.clone().setId("TestDeviceClassLabel"));

        deviceClassRemote = DeviceClass.getDefaultInstance().newBuilderForType();
        deviceClassRemote.setLabel("RemoteTestDeviceClass").setProductNumber("ABR-132");
        deviceConfigRemote = DeviceConfig.getDefaultInstance().newBuilderForType();
        deviceConfigRemote.setLabel("RemoteTestDeviceConfig").setSerialNumber("1123-5813-2134");
        deviceConfigRemote.setDeviceClass(deviceClassRemote.clone().setId("RemoteTestDeviceClass"));

        remote = new DeviceRegistryRemote();
        remote.init(JPService.getProperty(JPDeviceRegistryScope.class).getValue());
        remote.activate();
    }

    @AfterClass
    public static void tearDownClass() throws InterruptedException, CouldNotPerformException {
        remote.deactivate();
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
        assertTrue(registry.containsDeviceClass(deviceClass.clone().build()));
//		assertEquals(true, registry.getData().getDeviceClassesBuilderList().contains(deviceClass));
    }

    /**
     * Test of registerDeviceConfig method, of class DeviceRegistryImpl.
     */
    @Test
    public void testRegisterDeviceConfig() throws Exception {
        System.out.println("registerDeviceConfig");
        registry.registerDeviceConfig(deviceConfig.clone().build());
        assertTrue(registry.containsDeviceConfig(deviceConfig.clone().build()));
    }

    /**
     * Test of registering a DeviceClass per remote.
     */
    @Test(timeout = 3000)
    public void testRegisterDeviceClassPerRemote() throws Exception {
        System.out.println("registerDeviceClassPerRemote");
        
        remote.addObserver(new Observer<DeviceRegistryType.DeviceRegistry>() {

            @Override
            public void update(Observable<DeviceRegistryType.DeviceRegistry> source, DeviceRegistryType.DeviceRegistry data) throws Exception {
                if(!data.hasLabel()) {
                    logger.info("Got empty data!");
                } else {
                    logger.info("Got data update: "+data);
                }
            }
        });
        
        returnValue = remote.registerDeviceClass(deviceClassRemote.clone().build()).toBuilder();
        logger.info("Returned device class id ["+returnValue.getId()+"]");
        deviceClassRemote.setId("RemoteTestDeviceClass");
                
        while (true) {
            try {
                if (remote.getData().getDeviceClassesList().contains(deviceClassRemote.clone().build())) {
                    break;
                }
            } catch (NotAvailableException ex) {
                logger.debug("Not ready yet");
            }
            Thread.yield();
        }
        assertTrue(remote.containsDeviceClass(deviceClassRemote.clone().build()));
    }

    /**
     * Test of registering a DeviceConfig per remote.
     */
    @Test(timeout = 3000)
    public void testRegisterDeviceConfigPerRemote() throws Exception {
        System.out.println("registerDeviceConfigPerRemote");
        remote.registerDeviceConfig(deviceConfigRemote.clone().build());
        while (true) {
            if (remote.containsDeviceConfig(deviceConfigRemote.clone().build())) {
                break;
            }
            Thread.yield();
        }
        assertTrue(remote.containsDeviceConfig(deviceConfigRemote.clone().build()));
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
