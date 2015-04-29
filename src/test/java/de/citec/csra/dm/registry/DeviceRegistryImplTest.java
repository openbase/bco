/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.csra.dm.registry;

import de.citec.csra.dm.remote.DeviceRegistryRemote;
import de.citec.jp.JPDeviceDatabaseDirectory;
import de.citec.jp.JPDeviceClassDatabaseDirectory;
import de.citec.jp.JPDeviceConfigDatabaseDirectory;
import de.citec.jp.JPDeviceRegistryScope;
import de.citec.jps.core.JPService;
import de.citec.jps.exception.JPServiceException;
import de.citec.jul.exception.InitializationException;
import de.citec.jul.exception.InstantiationException;
import de.citec.jul.exception.InvalidStateException;
import de.citec.jul.exception.NotAvailableException;
import de.citec.jul.pattern.Observable;
import de.citec.jul.pattern.Observer;
import de.citec.jul.rsb.com.RSBInformerInterface;
import de.citec.jul.rsb.scope.ScopeGenerator;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import static junit.framework.TestCase.assertEquals;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import static junit.framework.TestCase.assertTrue;
import static junit.framework.TestCase.fail;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rsb.Scope;
import rst.geometry.PoseType;
import rst.geometry.RotationType;
import rst.geometry.TranslationType;
import rst.homeautomation.device.DeviceClassType;
import rst.homeautomation.device.DeviceClassType.DeviceClass;
import rst.homeautomation.device.DeviceConfigType;
import rst.homeautomation.device.DeviceConfigType.DeviceConfig;
import rst.homeautomation.device.DeviceRegistryType;
import rst.homeautomation.unit.UnitConfigType;
import rst.homeautomation.unit.UnitTemplateType;
import rst.rsb.ScopeType;
import rst.spatial.LocationConfigType;
import rst.spatial.PlacementConfigType;

/**
 *
 * @author mpohling
 */
public class DeviceRegistryImplTest {

    private static final Logger logger = LoggerFactory.getLogger(DeviceRegistryImplTest.class);

    public static final String LOCATION_LABEL = "paradise";
    
    private static DeviceRegistryService registry;
    private static DeviceClassType.DeviceClass.Builder deviceClass;
    private static DeviceConfigType.DeviceConfig.Builder deviceConfig;

    private static DeviceClassType.DeviceClass.Builder deviceClassRemote;
    private static DeviceClassType.DeviceClass.Builder returnValue;
    private static DeviceConfigType.DeviceConfig.Builder deviceConfigRemote;
    private static DeviceRegistryRemote remote;

    public DeviceRegistryImplTest() {
    }

    @BeforeClass
    public static void setUpClass() throws InstantiationException, InitializationException, IOException, InvalidStateException, JPServiceException {
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
        JPService.registerProperty(JPDeviceDatabaseDirectory.class, dbFile);
        JPService.registerProperty(JPDeviceConfigDatabaseDirectory.class, dbDeviceConfig);
        JPService.registerProperty(JPDeviceClassDatabaseDirectory.class, dbDeviceClasses);
        JPService.setupJUnitTestMode();

        registry = new DeviceRegistryService();
        registry.init(RSBInformerInterface.InformerType.Single);
        registry.activate();

        deviceClass = DeviceClass.getDefaultInstance().newBuilderForType();
        deviceClass.setLabel("TestDeviceClassLabel");
        deviceClass.setCompany("MyCom");
        deviceClass.setProductNumber("TDCL-001");
        deviceConfig = DeviceConfig.getDefaultInstance().newBuilderForType();
        deviceConfig.setLabel("TestDeviceConfigLabel");
        deviceConfig.setSerialNumber("0001-0004-2245");
        deviceConfig.setDeviceClass(deviceClass.clone().setId("TestDeviceClassLabel"));

        deviceClassRemote = DeviceClass.getDefaultInstance().newBuilderForType();
        deviceClassRemote.setLabel("RemoteTestDeviceClass").setProductNumber("ABR-132").setCompany("DreamCom");
        deviceConfigRemote = DeviceConfig.getDefaultInstance().newBuilderForType();
        deviceConfigRemote.setLabel("RemoteTestDeviceConfig").setSerialNumber("1123-5813-2134");
        deviceConfigRemote.setDeviceClass(deviceClassRemote.clone().setId("RemoteTestDeviceClass"));

        remote = new DeviceRegistryRemote();
        remote.init(JPService.getProperty(JPDeviceRegistryScope.class).getValue());
        remote.activate();
    }

    @AfterClass
    public static void tearDownClass() {
        remote.shutdown();
        registry.shutdown();
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
     * Test of registerDeviceConfigWithUnits method, of class
     * DeviceRegistryImpl.
     *
     * Test if the scope and the id of a device configuration and its units is
     * set when registered.
     */
    @Test
    public void testRegisterDeviceConfigWithUnits() throws Exception {
        String productNumber = "ABCD-4321";
        String serialNumber = "1234-WXYZ";
        String company = "Fibaro";

        String deviceId = company + "_" + productNumber + "_" + serialNumber;
        String deviceLabel = "TestSensor";
        String deviceScope = "/" + LOCATION_LABEL + "/" + deviceLabel.toLowerCase() + "/";

        String unitLabel = "Battery";
        String unitScope = "/" + LOCATION_LABEL + "/" + unitLabel.toLowerCase() + "/";
        String unitID = unitScope;

        ArrayList<UnitConfigType.UnitConfig> units = new ArrayList<>();
        DeviceClass motionSensorClass = registry.registerDeviceClass(getDeviceClass("F_MotionSensor", productNumber, company));
        units.add(getUnitConfig(UnitTemplateType.UnitTemplate.UnitType.BATTERY, unitLabel));
        DeviceConfig motionSensorConfig = getDeviceConfig(deviceLabel, serialNumber, motionSensorClass, units);

        motionSensorConfig = registry.registerDeviceConfig(motionSensorConfig);

        assertEquals("Device id is not set properly", deviceId, motionSensorConfig.getId());
        assertEquals("Device scope is not set properly", deviceScope, ScopeGenerator.generateStringRep(motionSensorConfig.getScope()));

        assertEquals("Unit id is not set properly", unitID, motionSensorConfig.getUnitConfig(0).getId());
        assertEquals("Unit scope is not set properly", unitScope, ScopeGenerator.generateStringRep(motionSensorConfig.getUnitConfig(0).getScope()));
    }

    /**
     * Test of testRegiseredDeviceConfigWithoutLabel method, of class
     * DeviceRegistryImpl.
     */
    @Test
    public void testRegisteredDeviceConfigWithoutLabel() throws Exception {
        String productNumber = "KNHD-4321";
        String serialNumber = "112358";
        String company = "Company";

        String deviceId = company + "_" + productNumber + "_" + serialNumber;

        DeviceClass clazz = registry.registerDeviceClass(getDeviceClass("WithoutLabel", productNumber, company));
        DeviceConfig deviceWithoutLabel = getDeviceConfig("", serialNumber, clazz, new ArrayList<UnitConfigType.UnitConfig>());
        deviceWithoutLabel = registry.registerDeviceConfig(deviceWithoutLabel);

        assertEquals("The device label is not set as the id if it is empty!", deviceId, deviceWithoutLabel.getLabel());
    }

    /**
     * Test of testRegisterTwoDevicesWithSameLabel method, of class
     * DeviceRegistryImpl.
     */
    @Test
    public void testRegisterTwoDevicesWithSameLabel() throws Exception {
        String serialNumber1 = "FIRST_DEV";
        String serialNumber2 = "BAD_DEV";
        String deviceLabel = "SameLabelSameLocation";
        
        DeviceClass clazz = registry.registerDeviceClass(getDeviceClass("WithoutLabel", "xyz", "HuxGMBH"));
        DeviceConfig deviceWithLabel1 = getDeviceConfig(deviceLabel, serialNumber1, clazz, new ArrayList<UnitConfigType.UnitConfig>());
        DeviceConfig deviceWithLabel2 = getDeviceConfig(deviceLabel, serialNumber2, clazz, new ArrayList<UnitConfigType.UnitConfig>());

        registry.registerDeviceConfig(deviceWithLabel1);
        try {
            registry.registerDeviceConfig(deviceWithLabel2);
            fail("There was no exception thrown even though two devices with the same label [" + deviceLabel + "] where registered in the same location [" + LOCATION_LABEL + "]");
        } catch (Exception ex) {
            assertTrue(true);
        }
    }

    private PlacementConfigType.PlacementConfig getDefaultPlacement() {
        RotationType.Rotation rotation = RotationType.Rotation.newBuilder().setQw(1).setQx(0).setQy(0).setQz(0).build();
        TranslationType.Translation translation = TranslationType.Translation.newBuilder().setX(0).setY(0).setZ(0).build();
        PoseType.Pose pose = PoseType.Pose.newBuilder().setRotation(rotation).setTranslation(translation).build();
        ScopeType.Scope.Builder locationScope = ScopeType.Scope.newBuilder().addComponent(LOCATION_LABEL);
        return PlacementConfigType.PlacementConfig.newBuilder().setPosition(pose).setLocationConfig(LocationConfigType.LocationConfig.newBuilder().setLabel(LOCATION_LABEL).setScope(locationScope).setId(LOCATION_LABEL).build()).build();
    }

    private UnitConfigType.UnitConfig getUnitConfig(UnitTemplateType.UnitTemplate.UnitType type, String label) {
        return UnitConfigType.UnitConfig.newBuilder().setPlacementConfig(getDefaultPlacement()).setTemplate(UnitTemplateType.UnitTemplate.newBuilder().setType(type).build()).setLabel(label).build();
    }

    private DeviceConfig getDeviceConfig(String label, String serialNumber, DeviceClass clazz, ArrayList<UnitConfigType.UnitConfig> units) {
        return DeviceConfig.newBuilder().setPlacementConfig(getDefaultPlacement()).setLabel(label).setSerialNumber(serialNumber).setDeviceClass(clazz).addAllUnitConfig(units).build();
    }

    private DeviceClass getDeviceClass(String label, String productNumber, String company) {
        return DeviceClass.newBuilder().setLabel(label).setProductNumber(productNumber).setCompany(company).build();

    }

    /**
     * Test of registering a DeviceClass per remote.
     */
    @Test(timeout = 5000)
    public void testRegisterDeviceClassPerRemote() throws Exception {
        System.out.println("registerDeviceClassPerRemote");

        remote.addObserver(new Observer<DeviceRegistryType.DeviceRegistry>() {

            @Override
            public void update(Observable<DeviceRegistryType.DeviceRegistry> source, DeviceRegistryType.DeviceRegistry data) throws Exception {
                if (data != null) {
                    logger.info("Got empty data!");
                } else {
                    logger.info("Got data update: " + data);
                }
            }
        });

        returnValue = remote.registerDeviceClass(deviceClassRemote.clone().build()).toBuilder();
        logger.info("Returned device class id [" + returnValue.getId() + "]");
        deviceClassRemote.setId(returnValue.getId());

        while (true) {
            try {
                if (remote.getData().getDeviceClassList().contains(deviceClassRemote.clone().build())) {
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
}
