/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dm.test;

import de.citec.dm.core.consistency.OpenhabServiceConfigItemIdConsistencyHandler;
import de.citec.dm.core.registry.DeviceRegistryService;
import de.citec.dm.remote.DeviceRegistryRemote;
import de.citec.jp.JPDeviceClassDatabaseDirectory;
import de.citec.jp.JPDeviceConfigDatabaseDirectory;
import de.citec.jp.JPDeviceRegistryScope;
import de.citec.jp.JPLocationRegistryScope;
import org.dc.jps.core.JPService;
import org.dc.jps.exception.JPServiceException;
import de.citec.jul.exception.CouldNotPerformException;
import de.citec.jul.exception.InitializationException;
import de.citec.jul.exception.InstantiationException;
import de.citec.jul.exception.InvalidStateException;
import de.citec.jul.exception.NotAvailableException;
import de.citec.jul.exception.printer.ExceptionPrinter;
import de.citec.jul.extension.rsb.scope.ScopeGenerator;
import de.citec.jul.pattern.Observable;
import de.citec.jul.pattern.Observer;
import de.citec.jul.storage.registry.jp.JPDatabaseDirectory;
import de.citec.jul.storage.registry.jp.JPInitializeDB;
import de.citec.lm.core.registry.LocationRegistryService;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;
import static junit.framework.TestCase.fail;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rsb.Scope;
import rst.configuration.EntryType;
import rst.configuration.MetaConfigType;
import rst.geometry.PoseType;
import rst.geometry.RotationType;
import rst.geometry.TranslationType;
import rst.homeautomation.binding.BindingConfigType;
import rst.homeautomation.binding.BindingTypeHolderType;
import rst.homeautomation.device.DeviceClassType.DeviceClass;
import rst.homeautomation.device.DeviceConfigType.DeviceConfig;
import rst.homeautomation.device.DeviceRegistryType;
import rst.homeautomation.service.BindingServiceConfigType.BindingServiceConfig;
import rst.homeautomation.service.ServiceConfigType.ServiceConfig;
import rst.homeautomation.service.ServiceTemplateType.ServiceTemplate;
import rst.homeautomation.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import rst.homeautomation.unit.UnitConfigType.UnitConfig;
import rst.homeautomation.unit.UnitTemplateConfigType.UnitTemplateConfig;
import rst.homeautomation.unit.UnitTemplateType.UnitTemplate;
import rst.homeautomation.unit.UnitTemplateType.UnitTemplate.UnitType;
import rst.rsb.ScopeType;
import rst.spatial.LocationConfigType.LocationConfig;
import rst.spatial.PlacementConfigType.PlacementConfig;

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
        JPService.registerProperty(JPDatabaseDirectory.class, new File("/tmp/" + System.getProperty("user.name") + "/db/"));
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
                    ExceptionPrinter.printHistory(ex, logger);
                }
            }
        });

        Thread locationRegistryThread = new Thread(new Runnable() {

            @Override
            public void run() {
                try {
                    locationRegistry.activate();
                } catch (CouldNotPerformException | InterruptedException ex) {
                    ExceptionPrinter.printHistory(ex, logger);
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
        deviceConfig.setDeviceClassId("TestDeviceClassLabel");

        deviceClassRemoteMessage = DeviceClass.getDefaultInstance().newBuilderForType();
        deviceClassRemoteMessage.setLabel("RemoteTestDeviceClass").setProductNumber("ABR-132").setCompany("DreamCom");
        deviceConfigRemoteMessage = DeviceConfig.getDefaultInstance().newBuilderForType();
        deviceConfigRemoteMessage.setLabel("RemoteTestDeviceConfig").setSerialNumber("1123-5813-2134");
        deviceConfigRemoteMessage.setDeviceClassId("RemoteTestDeviceClass");

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
//    @Test
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
        String unitScope = "/" + LOCATION_LABEL + "/" + UnitType.BATTERY.name().toLowerCase() + "/" + unitLabel.toLowerCase() + "/";
        String unitID = unitScope;

        ArrayList<UnitConfig> units = new ArrayList<>();
        DeviceClass motionSensorClass = deviceRegistry.registerDeviceClass(getDeviceClass("F_MotionSensor", productNumber, company));
        units.add(getUnitConfig(UnitType.BATTERY, unitLabel));
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
//    @Test
    // TODO: fix that the consisteny handling will work after this
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

    @Test
    public void testUnitConfigUnitTemplateConsistencyHandler() throws Exception {
        UnitTemplate unitTemplate = deviceRegistry.updateUnitTemplate(UnitTemplate.newBuilder().setType(UnitType.AMBIENT_LIGHT).addServiceType(ServiceType.BATTERY_PROVIDER).addServiceType(ServiceType.COLOR_SERVICE).build());
        assertTrue(unitTemplate.getServiceTypeList().contains(ServiceType.BATTERY_PROVIDER));
        assertTrue(unitTemplate.getServiceTypeList().contains(ServiceType.COLOR_SERVICE));
        assertTrue(unitTemplate.getType() == UnitType.AMBIENT_LIGHT);

        String serialNumber1 = "5073";
        String deviceLabel = "thisIsARandomLabel12512";
        BindingConfigType.BindingConfig bindingConfig = BindingConfigType.BindingConfig.newBuilder().setType(BindingTypeHolderType.BindingTypeHolder.BindingType.OPENHAB).build();
        DeviceClass clazz = deviceRegistry.registerDeviceClass(getDeviceClass("unitTest", "423112358", "company").toBuilder().setBindingConfig(bindingConfig).build());

        MetaConfigType.MetaConfig metaConfig = MetaConfigType.MetaConfig.newBuilder().addEntry(EntryType.Entry.newBuilder().setKey("testKey")).build();
        ServiceConfig serviceConfig1 = getServiceConfig(ServiceType.UNKNOWN);
        ServiceConfig serviceConfig2 = getServiceConfig(ServiceType.BATTERY_PROVIDER).toBuilder().setMetaConfig(metaConfig).build();
        ArrayList<UnitConfig> unitConfigs = new ArrayList<>();
        unitConfigs.add(getUnitConfig(UnitType.AMBIENT_LIGHT, "alsdkhuehlfai").toBuilder().addServiceConfig(serviceConfig1).addServiceConfig(serviceConfig2).build());
        DeviceConfig localDeviceConfig = getDeviceConfig(deviceLabel, serialNumber1, clazz, unitConfigs);

        localDeviceConfig = deviceRegistry.registerDeviceConfig(localDeviceConfig);
        assertTrue(localDeviceConfig.getUnitConfigCount() == 1);
        assertTrue(localDeviceConfig.getUnitConfig(0).getServiceConfigCount() == 2);
        UnitConfig unit = localDeviceConfig.getUnitConfig(0);
        assertTrue(unit.getServiceConfig(0).getType() == ServiceType.BATTERY_PROVIDER || unit.getServiceConfig(0).getType() == ServiceType.COLOR_SERVICE);
        assertTrue(unit.getServiceConfig(1).getType() == ServiceType.BATTERY_PROVIDER || unit.getServiceConfig(1).getType() == ServiceType.COLOR_SERVICE);
        if (unit.getServiceConfig(0).getType() == ServiceType.BATTERY_PROVIDER) {
            assertEquals(metaConfig, unit.getServiceConfig(0).getMetaConfig());
        } else if (unit.getServiceConfig(1).getType() == ServiceType.BATTERY_PROVIDER) {
            assertEquals(metaConfig, unit.getServiceConfig(1).getMetaConfig());
        }
    }

    @Test
    public void testDeviceClassDeviceConfigUnitConsistencyHandler() throws Exception {
        ServiceTemplate serviceTemplate1 = ServiceTemplate.newBuilder().setServiceType(ServiceType.POWER_SERVICE).build();
        UnitTemplateConfig unitTemplateConfig1 = UnitTemplateConfig.newBuilder().setType(UnitType.LIGHT).addServiceTemplate(serviceTemplate1).build();
        ServiceTemplate serviceTemplate2 = ServiceTemplate.newBuilder().setServiceType(ServiceType.BATTERY_PROVIDER).build();
        ServiceTemplate serviceTemplate3 = ServiceTemplate.newBuilder().setServiceType(ServiceType.HANDLE_PROVIDER).build();
        UnitTemplateConfig unitTemplateConfig2 = UnitTemplateConfig.newBuilder().setType(UnitType.HANDLE_SENSOR).addServiceTemplate(serviceTemplate2).addServiceTemplate(serviceTemplate3).build();

        deviceRegistry.updateUnitTemplate(UnitTemplate.newBuilder().setType(UnitType.LIGHT).addServiceType(serviceTemplate1.getServiceType()).build());
        deviceRegistry.updateUnitTemplate(UnitTemplate.newBuilder().setType(UnitType.HANDLE_SENSOR).addServiceType(ServiceType.BATTERY_PROVIDER).addServiceType(ServiceType.HANDLE_PROVIDER).build());

        BindingConfigType.BindingConfig bindingConfig = BindingConfigType.BindingConfig.newBuilder().setType(BindingTypeHolderType.BindingTypeHolder.BindingType.OPENHAB).build();
        DeviceClass clazz = deviceRegistry.registerDeviceClass(getDeviceClass("unittemplateUnitConfigTest", "0149283794283", "company").toBuilder().addUnitTemplateConfig(unitTemplateConfig1).addUnitTemplateConfig(unitTemplateConfig2).setBindingConfig(bindingConfig).build());
        assertTrue(clazz.getUnitTemplateConfigCount() == 2);

        DeviceConfig config = deviceRegistry.registerDeviceConfig(getDeviceConfig("DeviceConfigWhereUnitsShallBeSetViaConsistency", "randomSerial14972", clazz, null));
        assertTrue("Units in device config where not set according to the device classes unit templates", config.getUnitConfigCount() == clazz.getUnitTemplateConfigCount());
        boolean containsLight = false;
        boolean containsHandlseSensor = false;
        for (UnitConfig unit : config.getUnitConfigList()) {
            if (unit.getType().equals(unitTemplateConfig1.getType())) {
                containsLight = true;
                assertTrue("The light unit contains more or less services than the template config", unit.getServiceConfigCount() == unitTemplateConfig1.getServiceTemplateCount());
                assertTrue("The service type of the light unit does not match", unit.getServiceConfig(0).getType().equals(serviceTemplate1.getServiceType()));
            } else if (unit.getType().equals(unitTemplateConfig2.getType())) {
                containsHandlseSensor = true;
                assertTrue("The handle sensor unit contains more or less services than the template config", unit.getServiceConfigCount() == unitTemplateConfig2.getServiceTemplateCount());
                assertEquals("The service type of the handle sensor unit does not match", unit.getServiceConfig(0).getType(), serviceTemplate2.getServiceType());
                assertEquals("The service type of the handle sensor unit does not match", unit.getServiceConfig(1).getType(), serviceTemplate3.getServiceType());
            }
        }
        assertTrue("The device config does not contain a light unit even though the device class has an according unit template", containsLight);
        assertTrue("The device config does not contain a handle sensor unit even though the device class has an according unit template", containsHandlseSensor);

        ServiceTemplate serviceTemplate4 = ServiceTemplate.newBuilder().setServiceType(ServiceType.BUTTON_PROVIDER).build();
        UnitTemplateConfig unitTemplateConfig3 = UnitTemplateConfig.newBuilder().setType(UnitType.BUTTON).addServiceTemplate(serviceTemplate1).build();

        deviceRegistry.updateUnitTemplate(UnitTemplate.newBuilder().setType(UnitType.BUTTON).addServiceType(ServiceType.BUTTON_PROVIDER).build());

        clazz = deviceRegistry.updateDeviceClass(clazz.toBuilder().addUnitTemplateConfig(unitTemplateConfig3).build());
        config = deviceRegistry.getDeviceConfigById(config.getId());
        assertTrue("Unit configs and templates differ after the update of the device class", config.getUnitConfigCount() == clazz.getUnitTemplateConfigCount());
        assertEquals("Device config does not contain the right unit config", config.getUnitConfig(2).getType(), unitTemplateConfig3.getType());
        assertEquals("Unit config does not contain the right service", config.getUnitConfig(2).getServiceConfig(0).getType(), serviceTemplate4.getServiceType());

        // clearing unit templates because of effects on other tests it might have
        deviceRegistry.updateUnitTemplate(UnitTemplate.newBuilder().setType(UnitType.LIGHT).build());
        deviceRegistry.updateUnitTemplate(UnitTemplate.newBuilder().setType(UnitType.HANDLE_SENSOR).build());
        deviceRegistry.updateUnitTemplate(UnitTemplate.newBuilder().setType(UnitType.BUTTON).build());
    }

    @Test
    public void testBoundToDeviceConsistencyHandler() throws Exception {
        ServiceTemplate serviceTemplate1 = ServiceTemplate.newBuilder().setServiceType(ServiceType.POWER_SERVICE).build();
        UnitTemplateConfig unitTemplateConfig1 = UnitTemplateConfig.newBuilder().setType(UnitType.LIGHT).addServiceTemplate(serviceTemplate1).build();
        deviceRegistry.updateUnitTemplate(UnitTemplate.newBuilder().setType(UnitType.LIGHT).addServiceType(serviceTemplate1.getServiceType()).build());

        BindingConfigType.BindingConfig bindingConfig = BindingConfigType.BindingConfig.newBuilder().setType(BindingTypeHolderType.BindingTypeHolder.BindingType.OPENHAB).build();
        DeviceClass clazz = deviceRegistry.registerDeviceClass(getDeviceClass("BoundToDeviceTest", "boundToDevicePNR", "company").toBuilder().addUnitTemplateConfig(unitTemplateConfig1).setBindingConfig(bindingConfig).build());

        DeviceConfig config = deviceRegistry.registerDeviceConfig(getDeviceConfig("BoundToDeviceTestDevice", "boundToDeviceSNR", clazz, null));
        assertTrue("Unit config has not been added to device config", config.getUnitConfigCount() == 1);
        assertTrue("Unit config has not been set as bound to device", config.getUnitConfig(0).getBoundToDevice());
        assertTrue("Placement config of unit and device do not match although unit is bound to device", config.getUnitConfig(0).getPlacementConfig().equals(config.getPlacementConfig()));

        LocationConfig testLocation = locationRegistry.registerLocationConfig(LocationConfig.newBuilder().setLabel("BoundToDeviceTestLocation").build());
        PlacementConfig placement = config.getUnitConfig(0).getPlacementConfig().toBuilder().setLocationId(testLocation.getId()).build();
        UnitConfig unit = config.getUnitConfig(0).toBuilder().setPlacementConfig(placement).build();
        config = config.toBuilder().clearUnitConfig().addUnitConfig(unit).build();
        assertTrue("Units placement config has not been modified correctly", config.getUnitConfig(0).getPlacementConfig().getLocationId().equals(testLocation.getId()));

        config = deviceRegistry.updateDeviceConfig(config);
        assertTrue("Unit is not bound to device anymore", config.getUnitConfig(0).getBoundToDevice());
        assertTrue("Placement config of unit and device do not match although unit is bound to device", config.getUnitConfig(0).getPlacementConfig().equals(config.getPlacementConfig()));
        assertEquals("Location id in placement config of unit does not equals that in device", config.getPlacementConfig().getLocationId(), config.getUnitConfig(0).getPlacementConfig().getLocationId());

        locationRegistry.removeLocationConfig(testLocation);
        deviceRegistry.updateUnitTemplate(UnitTemplate.newBuilder().setType(UnitType.LIGHT).build());
    }

    /**
     * Test if the unit id of is set in the device service.
     */
    @Test
    public void testServiceConsistencyHandling() throws Exception {
        UnitConfig unitConfig = getUnitConfig(UnitType.LIGHT, "ServiceTest");
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
        String itemId = OpenhabServiceConfigItemIdConsistencyHandler.generateItemName(deviceConfig, clazz.getLabel(), unitConfig, serviceConfig, LOCATION);

//        assertTrue("OpenHAB item id is not set.", itemId.equals(deviceConfig.getUnitConfig(0).getServiceConfig(0).getBindingServiceConfig().getMetaConfig().getEntry(0).getValue()));
    }

    /**
     * Test if when breaking an existing device the sandbox registers it and
     * does not modify the real registry.
     *
     * @throws exception
     */
    @Test
    public void testSandbox() throws Exception {
        DeviceClass clazz = deviceRegistry.registerDeviceClass(getDeviceClass("SandboxTestLabel", "SandboxTestPNR", "SanboxCompany"));

        String unitLabel = "SandboxTestUnitLabel";
        ArrayList<UnitConfig> units = new ArrayList<>();
        units.add(getUnitConfig(UnitType.LIGHT, unitLabel));
        units.add(getUnitConfig(UnitType.LIGHT, unitLabel + "2"));

        DeviceConfig deviceConfig = deviceRegistry.registerDeviceConfig(getDeviceConfig("SandboxTestDeviceLabel", "SandboxTestSNR", clazz, units));
        assertTrue(deviceRegistry.containsDeviceConfig(deviceConfig));

        DeviceConfig.Builder deviveConfigBuilder = deviceConfig.toBuilder();
        DeviceConfig original = DeviceConfig.newBuilder(deviceConfig).build();
        units = new ArrayList(deviveConfigBuilder.getUnitConfigList());
        deviveConfigBuilder.clearUnitConfig();
        for (UnitConfig unitConfig : units) {
            if (!unitConfig.getLabel().equals(unitLabel)) {
                deviveConfigBuilder.addUnitConfig(unitConfig.toBuilder().setLabel(unitLabel));
            } else {
                deviveConfigBuilder.addUnitConfig(unitConfig);
            }
        }
        deviceConfig = deviveConfigBuilder.build();

        try {
            deviceRegistry.updateDeviceConfig(deviceConfig);
            fail("No exception thrown after updating a device with 2 units in the same location with the same label");
        } catch (Exception ex) {
        }

        assertEquals("DeviceConfig has been although the sandbox has rejected an update", original, deviceRegistry.getDeviceConfigById(deviceConfig.getId()));
    }

    private PlacementConfig getDefaultPlacement() {
        RotationType.Rotation rotation = RotationType.Rotation.newBuilder().setQw(1).setQx(0).setQy(0).setQz(0).build();
        TranslationType.Translation translation = TranslationType.Translation.newBuilder().setX(0).setY(0).setZ(0).build();
        PoseType.Pose pose = PoseType.Pose.newBuilder().setRotation(rotation).setTranslation(translation).build();
        ScopeType.Scope.Builder locationScope = ScopeType.Scope.newBuilder().addComponent(LOCATION_LABEL);
        return PlacementConfig.newBuilder().setPosition(pose).setLocationId(LOCATION.getId()).build();
    }

    private UnitConfig getUnitConfig(UnitType type, String label) {
        return UnitConfig.newBuilder().setPlacementConfig(getDefaultPlacement()).setType(type).setLabel(label).setBoundToDevice(false).build();
    }

    private DeviceConfig getDeviceConfig(String label, String serialNumber, DeviceClass clazz, ArrayList<UnitConfig> units) {
        if (units == null) {
            return DeviceConfig.newBuilder().setPlacementConfig(getDefaultPlacement()).setLabel(label).setSerialNumber(serialNumber).setDeviceClassId(clazz.getId()).build();
        }
        return DeviceConfig.newBuilder().setPlacementConfig(getDefaultPlacement()).setLabel(label).setSerialNumber(serialNumber).setDeviceClassId(clazz.getId()).addAllUnitConfig(units).build();
    }

    private DeviceClass getDeviceClass(String label, String productNumber, String company) {
        return DeviceClass.newBuilder().setLabel(label).setProductNumber(productNumber).setCompany(company).build();
    }

    private ServiceConfig getServiceConfig(ServiceType type) {
        return ServiceConfig.newBuilder().setType(type).setBindingServiceConfig(BindingServiceConfig.newBuilder().setType(BindingTypeHolderType.BindingTypeHolder.BindingType.SINACT).build()).build();
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
//    @Test(timeout = 3000)
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

    /**
     * Test of registering a DeviceConfig per remote.
     */
    @Test(timeout = 3000)
    public void testGetReadOnlyFlag() throws Exception {
        System.out.println("registerDeviceConfigPerRemote");
        assertEquals(Boolean.FALSE, remote.isDeviceClassRegistryReadOnly().get());
        assertEquals(Boolean.FALSE, remote.isDeviceConfigRegistryReadOnly().get());
        assertEquals(Boolean.FALSE, remote.isUnitTemplateRegistryReadOnly().get());
    }
}
