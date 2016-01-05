/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal;

import org.dc.bco.registry.device.core.mock.MockRegistry;
import de.citec.dal.remote.unit.AmbientLightRemote;
import org.dc.bco.registry.device.remote.DeviceRegistryRemote;
import org.dc.jps.exception.JPServiceException;
import org.dc.jul.exception.InitializationException;
import org.dc.jul.exception.InstantiationException;
import java.util.ArrayList;
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

    @Test
    public void testRegisterDeviceWhileRunning() throws Exception {
        System.out.println("testRegisterDeviceWhileRunning");
        DALService instance = new DALService();
        instance.init();

        Thread.sleep(1000);

        DeviceRegistryRemote remote = new DeviceRegistryRemote();
        remote.init();
        remote.activate();

        DeviceClass deviceClass = remote.registerDeviceClass(MockRegistry.getDeviceClass("TestRegisterDeviceWhileRunnint", "DALServiceAndCoKG123456", "DALServiceAndCoKG"));
        ArrayList<UnitConfig> units = new ArrayList<>();
        units.add(MockRegistry.getUnitConfig(UnitTemplateType.UnitTemplate.UnitType.AMBIENT_LIGHT, "DALServiceRegisterWhileRunningUnit"));
        DeviceConfig deviceConfig = remote.registerDeviceConfig(MockRegistry.getDeviceConfig("DALServiceTestRegisterWhileRunningDeviceConfig", "DalServiceTestSerialNumber", deviceClass, units));
        UnitConfig unit = deviceConfig.getUnitConfig(0);
        unit = unit.toBuilder().setLabel("ShorterLabel").build();
        deviceConfig = deviceConfig.toBuilder().clearUnitConfig().addUnitConfig(unit).build();
        deviceConfig = remote.updateDeviceConfig(deviceConfig);

        Thread.sleep(1000);
        AmbientLightRemote ambientLightRemote = new AmbientLightRemote();
        ambientLightRemote.init(deviceConfig.getUnitConfig(0).getScope());
        ambientLightRemote.activate();

        Thread.sleep(1000);
        ambientLightRemote.setPower(PowerStateType.PowerState.State.ON);
        assertTrue(ambientLightRemote.isConnected());
        
//        unit = unit.toBuilder().setMetaConfig(MetaConfigType.MetaConfig.newBuilder().addEntry(EntryType.Entry.newBuilder().setKey("Key").setValue("Value"))).build();
//        deviceConfig = deviceConfig.toBuilder().clearUnitConfig().addUnitConfig(unit).build();
//        deviceConfig = remote.updateDeviceConfig(deviceConfig);
//        Thread.sleep(1000);
//        
//        Thread.sleep(1000);
//        assertTrue(ambientLightRemote.isConnected());

        ambientLightRemote.shutdown();
        remote.shutdown();
        instance.shutdown();
    }
}
