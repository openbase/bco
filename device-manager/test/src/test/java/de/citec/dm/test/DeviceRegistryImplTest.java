/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dm.test;

import de.citec.dm.core.consistency.OpenhabServiceConfigItemIdConsistenyHandler;
import de.citec.dm.core.registry.DeviceRegistryService;
import de.citec.dm.remote.DeviceRegistryRemote;
import de.citec.jp.JPDeviceDatabaseDirectory;
import de.citec.jp.JPDeviceClassDatabaseDirectory;
import de.citec.jp.JPDeviceConfigDatabaseDirectory;
import de.citec.jp.JPDeviceRegistryScope;
import de.citec.jp.JPLocationRegistryScope;
import de.citec.jps.core.JPService;
import de.citec.jps.exception.JPServiceException;
import de.citec.jul.exception.CouldNotPerformException;
import de.citec.jul.exception.ExceptionPrinter;
import de.citec.jul.exception.InitializationException;
import de.citec.jul.exception.InstantiationException;
import de.citec.jul.exception.InvalidStateException;
import de.citec.jul.exception.NotAvailableException;
import de.citec.jul.pattern.Observable;
import de.citec.jul.pattern.Observer;
import de.citec.jul.extension.rsb.scope.ScopeGenerator;
import de.citec.jul.storage.jp.JPInitializeDB;
import de.citec.lm.core.registry.LocationRegistryService;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Level;
import static junit.framework.TestCase.assertEquals;
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
import rst.homeautomation.binding.BindingTypeHolderType;
import rst.homeautomation.device.DeviceClassType.DeviceClass;
import rst.homeautomation.device.DeviceConfigType.DeviceConfig;
import rst.homeautomation.device.DeviceRegistryType;
import rst.homeautomation.service.BindingServiceConfigType.BindingServiceConfig;
import rst.homeautomation.service.ServiceConfigType.ServiceConfig;
import rst.homeautomation.service.ServiceTypeHolderType.ServiceTypeHolder.ServiceType;
import rst.homeautomation.unit.UnitConfigType.UnitConfig;
import rst.homeautomation.unit.UnitTemplateType.UnitTemplate;
import rst.rsb.ScopeType;
import rst.spatial.LocationConfigType.LocationConfig;
import rst.spatial.PlacementConfigType;

/**
 *
 * @author mpohling
 */
public class DeviceRegistryImplTest {

    private static final Logger logger = LoggerFactory.getLogger(DeviceRegistryImplTest.class);

    public static final String LOCATION_LABEL = "paradise";
    public static LocationConfig LOCATION;

    private static DeviceRegistryService deviceRegistry;
    private static DeviceClass.Builder deviceClass;
    private static DeviceConfig.Builder deviceConfig;

    private static LocationRegistryService locationRegistry;

    private static DeviceClass.Builder deviceClassRemoteMessage;
    private static DeviceClass.Builder returnValue;
    private static DeviceConfig.Builder deviceConfigRemoteMessage;
    private static DeviceRegistryRemote remote;

    public DeviceRegistryImplTest() {
    }

    @BeforeClass
    public static void setUpClass() throws InstantiationException, InitializationException, IOException, InvalidStateException, JPServiceException, InterruptedException, CouldNotPerformException {
        JPService.registerProperty(JPInitializeDB.class, true);
        JPService.registerProperty(JPDeviceRegistryScope.class, new Scope("/test/devicemanager/registry/"));
        JPService.registerProperty(JPLocationRegistryScope.class, new Scope("/test/locationmanager/registry/"));
        JPService.registerProperty(JPDeviceDatabaseDirectory.class, new File("/tmp/db/"));
        JPService.registerProperty(JPDeviceConfigDatabaseDirectory.class, new File("device-config"));
        JPService.registerProperty(JPDeviceClassDatabaseDirectory.class, new File("device-classes"));
        JPService.setupJUnitTestMode();

        deviceRegistry = new DeviceRegistryService();
        locationRegistry = new LocationRegistryService();

        deviceRegistry.init();
        locationRegistry.init();

        Thread deviceRegistryThread = new Thread(new Runnable() {

            @Override
            public void run() {
                try {
                    deviceRegistry.activate();
                } catch (CouldNotPerformException | InterruptedException ex) {
                    ExceptionPrinter.printHistory(logger, ex);
                }
            }
        });

        Thread locationRegistryThread = new Thread(new Runnable() {

            @Override
            public void run() {
                try {
                    locationRegistry.activate();
                } catch (CouldNotPerformException | InterruptedException ex) {
                    ExceptionPrinter.printHistory(logger, ex);
                }
            }
        });

        deviceRegistryThread.start();
        locationRegistryThread.start();

        deviceRegistryThread.join();
        locationRegistryThread.join();

        deviceClass = DeviceClass.getDefaultInstance().newBuilderForType();
        deviceClass.setLabel("TestDeviceClassLabel");
        deviceClass.setCompany("MyCom");
        deviceClass.setProductNumber("TDCL-001");
        deviceConfig = DeviceConfig.getDefaultInstance().newBuilderForType();
        deviceConfig.setLabel("TestDeviceConfigLabel");
        deviceConfig.setSerialNumber("0001-0004-2245");
        deviceConfig.setDeviceClass(deviceClass.clone().setId("TestDeviceClassLabel"));

        deviceClassRemoteMessage = DeviceClass.getDefaultInstance().newBuilderForType();
        deviceClassRemoteMessage.setLabel("RemoteTestDeviceClass").setProductNumber("ABR-132").setCompany("DreamCom");
        deviceConfigRemoteMessage = DeviceConfig.getDefaultInstance().newBuilderForType();
        deviceConfigRemoteMessage.setLabel("RemoteTestDeviceConfig").setSerialNumber("1123-5813-2134");
        deviceConfigRemoteMessage.setDeviceClass(deviceClassRemoteMessage.clone().setId("RemoteTestDeviceClass"));

        remote = new DeviceRegistryRemote();
        remote.init();
        remote.activate();

        LOCATION = locationRegistry.registerLocationConfig(LocationConfig.newBuilder().setLabel(LOCATION_LABEL).build());
    }

    @AfterClass
    public static void tearDownClass() {
        remote.shutdown();

        if (locationRegistry != null) {
            locationRegistry.shutdown();
        }
        if (deviceRegistry != null) {
            deviceRegistry.shutdown();
        }
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
        deviceRegistry.registerDeviceClass(deviceClass.clone().build());
        assertTrue(deviceRegistry.containsDeviceClass(deviceClass.clone().build()));
//		assertEquals(true, registry.getData().getDeviceClassesBuilderList().contains(deviceClass));
    }

    /**
     * Test of registerDeviceConfig method, of class DeviceRegistryImpl.
     */
    @Test
    public void testRegisterDeviceConfig() throws Exception {
        System.out.println("registerDeviceConfig");
        deviceRegistry.registerDeviceConfig(deviceConfig.clone().build());
        assertTrue(deviceRegistry.containsDeviceConfig(deviceConfig.clone().build()));
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
        String unitScope = "/" + LOCATION_LABEL + "/" + UnitTemplate.UnitType.BATTERY.name().toLowerCase() + "/" + unitLabel.toLowerCase() + "/";
        String unitID = unitScope;

        ArrayList<UnitConfig> units = new ArrayList<>();
        DeviceClass motionSensorClass = deviceRegistry.registerDeviceClass(getDeviceClass("F_MotionSensor", productNumber, company));
        units.add(getUnitConfig(UnitTemplate.UnitType.BATTERY, unitLabel));
        DeviceConfig motionSensorConfig = getDeviceConfig(deviceLabel, serialNumber, motionSensorClass, units);

        motionSensorConfig = deviceRegistry.registerDeviceConfig(motionSensorConfig);

        assertEquals("Device id is not set properly", deviceId, motionSensorConfig.getId());
        assertEquals("Device scope is not set properly", deviceScope, ScopeGenerator.generateStringRep(motionSensorConfig.getScope()));

        assertEquals("Unit id is not set properly", unitID, motionSensorConfig.getUnitConfig(0).getId());
        assertEquals("Unit scope is not set properly", unitScope, ScopeGenerator.generateStringRep(motionSensorConfig.getUnitConfig(0).getScope()));

        assertEquals("Device id is not set in unit", motionSensorConfig.getId(), motionSensorConfig.getUnitConfig(0).getDeviceId());
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

        DeviceClass clazz = deviceRegistry.registerDeviceClass(getDeviceClass("WithoutLabel", productNumber, company));
        DeviceConfig deviceWithoutLabel = getDeviceConfig("", serialNumber, clazz, new ArrayList<UnitConfig>());
        deviceWithoutLabel = deviceRegistry.registerDeviceConfig(deviceWithoutLabel);

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

        DeviceClass clazz = deviceRegistry.registerDeviceClass(getDeviceClass("WithoutLabel", "xyz", "HuxGMBH"));
        DeviceConfig deviceWithLabel1 = getDeviceConfig(deviceLabel, serialNumber1, clazz, new ArrayList<UnitConfig>());
        DeviceConfig deviceWithLabel2 = getDeviceConfig(deviceLabel, serialNumber2, clazz, new ArrayList<UnitConfig>());

        deviceRegistry.registerDeviceConfig(deviceWithLabel1);
        try {
            deviceRegistry.registerDeviceConfig(deviceWithLabel2);
            fail("There was no exception thrown even though two devices with the same label [" + deviceLabel + "] where registered in the same location [" + LOCATION_LABEL + "]");
        } catch (Exception ex) {
            assertTrue(true);
        }
    }

    /**
     * Test if the unit id of is set in the device service.
     */
    @Test
    public void testServiceConsistencyHandling() throws Exception {
        UnitConfig unitConfig = getUnitConfig(UnitTemplate.UnitType.LIGHT, "ServiceTest");
        BindingServiceConfig bindingConfig = BindingServiceConfig.newBuilder().setType(BindingTypeHolderType.BindingTypeHolder.BindingType.OPENHAB).build();
        ServiceConfig serviceConfig = ServiceConfig.newBuilder().setType(ServiceType.POWER_PROVIDER).setBindingServiceConfig(bindingConfig).build();
        unitConfig = unitConfig.toBuilder().addServiceConfig(serviceConfig).build();
        ArrayList<UnitConfig> units = new ArrayList<>();
        units.add(unitConfig);

        DeviceClass clazz = deviceRegistry.registerDeviceClass(getDeviceClass("ServiceUnitIdTest", "8383838", "ServiceGMBH"));

        DeviceConfig deviceConfig = deviceRegistry.registerDeviceConfig(getDeviceConfig("ServiceTest", "123456", clazz, units));

//        assertTrue("Unit id is not set.", !deviceConfig.getUnitConfig(0).getId().equals(""));
//        assertTrue("Unit id in service config is not set.", !deviceConfig.getUnitConfig(0).getServiceConfig(0).getUnitId().equals(""));
//        assertTrue("Unit id in service config does not match id in unit config.", deviceConfig.getUnitConfig(0).getServiceConfig(0).getUnitId().equals(deviceConfig.getUnitConfig(0).getId()));
        String itemId = OpenhabServiceConfigItemIdConsistenyHandler.generateItemName(deviceConfig, unitConfig, serviceConfig, LOCATION);

//        assertTrue("OpenHAB item id is not set.", itemId.equals(deviceConfig.getUnitConfig(0).getServiceConfig(0).getBindingServiceConfig().getOpenhabBindingServiceConfig().getItemId()));
    }

    private PlacementConfigType.PlacementConfig getDefaultPlacement() {
        RotationType.Rotation rotation = RotationType.Rotation.newBuilder().setQw(1).setQx(0).setQy(0).setQz(0).build();
        TranslationType.Translation translation = TranslationType.Translation.newBuilder().setX(0).setY(0).setZ(0).build();
        PoseType.Pose pose = PoseType.Pose.newBuilder().setRotation(rotation).setTranslation(translation).build();
        ScopeType.Scope.Builder locationScope = ScopeType.Scope.newBuilder().addComponent(LOCATION_LABEL);
        return PlacementConfigType.PlacementConfig.newBuilder().setPosition(pose).setLocationId(LOCATION_LABEL).build();
    }

    private UnitConfig getUnitConfig(UnitTemplate.UnitType type, String label) {
        return UnitConfig.newBuilder().setPlacementConfig(getDefaultPlacement()).setTemplate(UnitTemplate.newBuilder().setType(type).build()).setLabel(label).build();
    }

    private DeviceConfig getDeviceConfig(String label, String serialNumber, DeviceClass clazz, ArrayList<UnitConfig> units) {
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

        returnValue = remote.registerDeviceClass(deviceClassRemoteMessage.clone().build()).toBuilder();
        logger.info("Returned device class id [" + returnValue.getId() + "]");
        deviceClassRemoteMessage.setId(returnValue.getId());

        while (true) {
            try {
                if (remote.getData().getDeviceClassList().contains(deviceClassRemoteMessage.clone().build())) {
                    break;
                }
            } catch (NotAvailableException ex) {
                logger.debug("Not ready yet");
            }
            Thread.yield();
        }
        assertTrue(remote.containsDeviceClass(deviceClassRemoteMessage.clone().build()));
    }

    /**
     * Test of registering a DeviceConfig per remote.
     */
    @Test(timeout = 3000)
    public void testRegisterDeviceConfigPerRemote() throws Exception {
        System.out.println("registerDeviceConfigPerRemote");
        remote.registerDeviceConfig(deviceConfigRemoteMessage.clone().build());
        while (true) {
            if (remote.containsDeviceConfig(deviceConfigRemoteMessage.clone().build())) {
                break;
            }
            Thread.yield();
        }
        assertTrue(remote.containsDeviceConfig(deviceConfigRemoteMessage.clone().build()));
    }
}
