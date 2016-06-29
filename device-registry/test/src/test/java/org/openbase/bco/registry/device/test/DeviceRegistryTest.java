package org.openbase.bco.registry.device.test;

/*
 * #%L
 * REM DeviceRegistry Test
 * %%
 * Copyright (C) 2014 - 2016 openbase.org
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
import org.openbase.bco.registry.device.core.DeviceRegistryController;
import org.openbase.bco.registry.device.remote.DeviceRegistryRemote;
import org.openbase.jps.core.JPService;
import org.openbase.jps.exception.JPServiceException;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InitializationException;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.exception.InvalidStateException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.extension.rsb.scope.ScopeGenerator;
import org.openbase.bco.registry.location.core.LocationRegistryController;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;
import static junit.framework.TestCase.fail;
import org.openbase.bco.registry.user.core.UserRegistryController;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.pattern.Remote;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rst.authorization.UserConfigType.UserConfig;
import rst.configuration.EntryType;
import rst.configuration.MetaConfigType;
import rst.geometry.PoseType;
import rst.geometry.RotationType;
import rst.geometry.TranslationType;
import rst.homeautomation.binding.BindingConfigType;
import rst.homeautomation.binding.BindingConfigType.BindingConfig;
import rst.homeautomation.binding.BindingTypeHolderType;
import rst.homeautomation.device.DeviceClassType.DeviceClass;
import rst.homeautomation.device.DeviceConfigType.DeviceConfig;
import rst.homeautomation.service.BindingServiceConfigType.BindingServiceConfig;
import rst.homeautomation.service.ServiceConfigType.ServiceConfig;
import rst.homeautomation.service.ServiceTemplateType.ServiceTemplate;
import rst.homeautomation.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import rst.homeautomation.state.EnablingStateType.EnablingState;
import rst.homeautomation.state.InventoryStateType.InventoryState;
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
public class DeviceRegistryTest {

    private static final Logger logger = LoggerFactory.getLogger(DeviceRegistryTest.class);

    public static final String LOCATION_LABEL = "paradise";
    public static LocationConfig LOCATION;

    private static DeviceRegistryController deviceRegistry;
    private static DeviceClass.Builder deviceClass;
    private static DeviceConfig.Builder deviceConfig;

    private static LocationRegistryController locationRegistry;
    private static UserRegistryController userRegistry;

    private static DeviceRegistryRemote deviceRegistryRemote;

    @BeforeClass
    public static void setUpClass() throws InstantiationException, InitializationException, IOException, InvalidStateException, JPServiceException, InterruptedException, CouldNotPerformException, ExecutionException {
        JPService.setupJUnitTestMode();

        deviceRegistry = new DeviceRegistryController();
        locationRegistry = new LocationRegistryController();
        userRegistry = new UserRegistryController();

        deviceRegistry.init();
        locationRegistry.init();
        userRegistry.init();

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

        Thread userRegistryThread = new Thread(new Runnable() {

            @Override
            public void run() {
                try {
                    userRegistry.activate();
                } catch (CouldNotPerformException | InterruptedException ex) {
                    ExceptionPrinter.printHistory(ex, logger);
                }
            }
        });

        deviceRegistryThread.start();
        locationRegistryThread.start();
        userRegistryThread.start();

        deviceRegistryThread.join();
        locationRegistryThread.join();
        userRegistryThread.join();

        deviceClass = DeviceClass.getDefaultInstance().newBuilderForType();
        deviceClass.setLabel("TestDeviceClassLabel");
        deviceClass.setCompany("MyCom");
        deviceClass.setProductNumber("TDCL-001");
        deviceConfig = DeviceConfig.getDefaultInstance().newBuilderForType();
        deviceConfig.setLabel("TestDeviceConfigLabel");
        deviceConfig.setSerialNumber("0001-0004-2245");
        deviceConfig.setDeviceClassId("TestDeviceClassLabel");

        deviceRegistryRemote = new DeviceRegistryRemote();
        deviceRegistryRemote.init();
        deviceRegistryRemote.activate();
        deviceRegistryRemote.waitForData();

        LOCATION = locationRegistry.registerLocationConfig(LocationConfig.newBuilder().setLabel(LOCATION_LABEL).build()).get();
    }

    @AfterClass
    public static void tearDownClass() {
        if (deviceRegistryRemote != null) {
            deviceRegistryRemote.shutdown();
        }
        if (locationRegistry != null) {
            locationRegistry.shutdown();
        }
        if (userRegistry != null) {
            userRegistry.shutdown();
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
    @Test(timeout = 5000)
    public void testRegisterDeviceClass() throws Exception {
        System.out.println("registerDeviceClass");
        deviceRegistry.registerDeviceClass(deviceClass.clone().build()).get();
        assertTrue(deviceRegistry.containsDeviceClass(deviceClass.clone().build()));
//		assertEquals(true, registry.getData().getDeviceClassesBuilderList().contains(deviceClass));
    }

    /**
     * Test of registerDeviceConfig method, of class DeviceRegistryImpl.
     */
//    @Test(timeout = 5000)
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
    @Test(timeout = 5000)
    public void testRegisterDeviceConfigWithUnits() throws Exception {
        String productNumber = "ABCD-4321";
        String serialNumber = "1234-WXYZ";
        String company = "Fibaro";

        String deviceId = company + "_" + productNumber + "_" + serialNumber;
        String deviceLabel = "TestSensor";
        String deviceScope = "/" + LOCATION_LABEL + "/" + "device" + "/" + deviceLabel.toLowerCase() + "/";

        String expectedUnitScope = "/" + LOCATION_LABEL + "/" + UnitType.BATTERY.name().toLowerCase() + "/" + deviceLabel.toLowerCase() + "/";
        String expectedUnitID = expectedUnitScope;

        // units are automatically added when a unit template config in the device class exists
        UnitTemplateConfig unitTemplateConfig = UnitTemplateConfig.newBuilder().setType(UnitType.BATTERY).build();
        DeviceClass motionSensorClass = deviceRegistry.registerDeviceClass(getDeviceClass("F_MotionSensor", productNumber, company).toBuilder().addUnitTemplateConfig(unitTemplateConfig).build()).get();
        DeviceConfig motionSensorConfig = getDeviceConfig(deviceLabel, serialNumber, motionSensorClass, null);
        motionSensorConfig = deviceRegistry.registerDeviceConfig(motionSensorConfig).get();

        assertEquals("Device id is not set properly", deviceId, motionSensorConfig.getId());
        assertEquals("Device scope is not set properly", deviceScope, ScopeGenerator.generateStringRep(motionSensorConfig.getScope()));

        assertEquals("Unit id is not set properly", expectedUnitID, motionSensorConfig.getUnitConfig(0).getId());
        assertEquals("Unit scope is not set properly", expectedUnitScope, ScopeGenerator.generateStringRep(motionSensorConfig.getUnitConfig(0).getScope()));

        assertEquals("Device id is not set in unit", motionSensorConfig.getId(), motionSensorConfig.getUnitConfig(0).getDeviceId());
    }

    /**
     * Test of testRegiseredDeviceConfigWithoutLabel method, of class
     * DeviceRegistryImpl.
     */
    @Test(timeout = 5000)
    public void testRegisteredDeviceConfigWithoutLabel() throws Exception {
        String productNumber = "KNHD-4321";
        String serialNumber = "112358";
        String company = "Company";

        String deviceId = company + "_" + productNumber + "_" + serialNumber;

        DeviceClass clazz = deviceRegistry.registerDeviceClass(getDeviceClass("WithoutLabel", productNumber, company)).get();
        DeviceConfig deviceWithoutLabel = getDeviceConfig("", serialNumber, clazz, new ArrayList<>());
        deviceWithoutLabel = deviceRegistry.registerDeviceConfig(deviceWithoutLabel).get();

        assertEquals("The device label is not set as the id if it is empty!", deviceId, deviceWithoutLabel.getLabel());
    }

    /**
     * Test of testRegisterTwoDevicesWithSameLabel method, of class
     * DeviceRegistryImpl.
     */
//    @Test(timeout = 5000)
    // TODO: fix that the consisteny handling will work after this
    public void testRegisterTwoDevicesWithSameLabel() throws Exception {
        String serialNumber1 = "FIRST_DEV";
        String serialNumber2 = "BAD_DEV";
        String deviceLabel = "SameLabelSameLocation";

        DeviceClass clazz = deviceRegistry.registerDeviceClass(getDeviceClass("WithoutLabel", "xyz", "HuxGMBH")).get();
        DeviceConfig deviceWithLabel1 = getDeviceConfig(deviceLabel, serialNumber1, clazz, new ArrayList<>());
        DeviceConfig deviceWithLabel2 = getDeviceConfig(deviceLabel, serialNumber2, clazz, new ArrayList<>());

        deviceRegistry.registerDeviceConfig(deviceWithLabel1);
        try {
            ExceptionPrinter.setBeQuit(Boolean.TRUE);
            deviceRegistry.registerDeviceConfig(deviceWithLabel2);
            fail("There was no exception thrown even though two devices with the same label [" + deviceLabel + "] where registered in the same location [" + LOCATION_LABEL + "]");
        } catch (Exception ex) {
            assertTrue(true);
        } finally {
            ExceptionPrinter.setBeQuit(Boolean.FALSE);
        }
    }

    @Test(timeout = 5000)
    public void testUnitConfigUnitTemplateConsistencyHandler() throws Exception {
        UnitTemplate unitTemplate = deviceRegistry.updateUnitTemplate(UnitTemplate.newBuilder().setType(UnitType.AMBIENT_LIGHT).addServiceType(ServiceType.BATTERY_PROVIDER).addServiceType(ServiceType.COLOR_SERVICE).build()).get();
        assertTrue(unitTemplate.getServiceTypeList().contains(ServiceType.BATTERY_PROVIDER));
        assertTrue(unitTemplate.getServiceTypeList().contains(ServiceType.COLOR_SERVICE));
        assertTrue(unitTemplate.getType() == UnitType.AMBIENT_LIGHT);

        UnitTemplateConfig unitTemplateConfig = UnitTemplateConfig.newBuilder().setType(unitTemplate.getType()).build();
        String serialNumber1 = "5073";
        String deviceLabel = "thisIsARandomLabel12512";
        BindingConfigType.BindingConfig bindingConfig = BindingConfigType.BindingConfig.newBuilder().setType(BindingTypeHolderType.BindingTypeHolder.BindingType.OPENHAB).build();
        DeviceClass clazz = deviceRegistry.registerDeviceClass(getDeviceClass("unitTest", "423112358", "company").toBuilder().setBindingConfig(bindingConfig).addUnitTemplateConfig(unitTemplateConfig).build()).get();

        MetaConfigType.MetaConfig metaConfig = MetaConfigType.MetaConfig.newBuilder().addEntry(EntryType.Entry.newBuilder().setKey("testKey")).build();
        ServiceConfig serviceConfig1 = getServiceConfig(ServiceType.UNKNOWN);
        ServiceConfig serviceConfig2 = getServiceConfig(ServiceType.BATTERY_PROVIDER).toBuilder().setMetaConfig(metaConfig).build();
        ArrayList<UnitConfig> unitConfigs = new ArrayList<>();
        unitConfigs.add(getUnitConfig(UnitType.AMBIENT_LIGHT, "alsdkhuehlfai").toBuilder().addServiceConfig(serviceConfig1).addServiceConfig(serviceConfig2).build());
        DeviceConfig localDeviceConfig = getDeviceConfig(deviceLabel, serialNumber1, clazz, unitConfigs);

        localDeviceConfig = deviceRegistry.registerDeviceConfig(localDeviceConfig).get();
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

    @Test(timeout = 5000)
    public void testDeviceClassDeviceConfigUnitConsistencyHandler() throws Exception {
        ServiceTemplate serviceTemplate1 = ServiceTemplate.newBuilder().setServiceType(ServiceType.POWER_SERVICE).build();
        UnitTemplateConfig unitTemplateConfig1 = UnitTemplateConfig.newBuilder().setType(UnitType.LIGHT).addServiceTemplate(serviceTemplate1).build();
        ServiceTemplate serviceTemplate2 = ServiceTemplate.newBuilder().setServiceType(ServiceType.BATTERY_PROVIDER).build();
        ServiceTemplate serviceTemplate3 = ServiceTemplate.newBuilder().setServiceType(ServiceType.HANDLE_PROVIDER).build();
        UnitTemplateConfig unitTemplateConfig2 = UnitTemplateConfig.newBuilder().setType(UnitType.HANDLE_SENSOR).addServiceTemplate(serviceTemplate2).addServiceTemplate(serviceTemplate3).build();

        deviceRegistry.updateUnitTemplate(UnitTemplate.newBuilder().setType(UnitType.LIGHT).addServiceType(serviceTemplate1.getServiceType()).build()).get();
        deviceRegistry.updateUnitTemplate(UnitTemplate.newBuilder().setType(UnitType.HANDLE_SENSOR).addServiceType(ServiceType.BATTERY_PROVIDER).addServiceType(ServiceType.HANDLE_PROVIDER).build()).get();

        BindingConfigType.BindingConfig bindingConfig = BindingConfigType.BindingConfig.newBuilder().setType(BindingTypeHolderType.BindingTypeHolder.BindingType.OPENHAB).build();
        DeviceClass clazz = deviceRegistry.registerDeviceClass(getDeviceClass("unittemplateUnitConfigTest", "0149283794283", "company").toBuilder().addUnitTemplateConfig(unitTemplateConfig1).addUnitTemplateConfig(unitTemplateConfig2).setBindingConfig(bindingConfig).build()).get();
        assertTrue(clazz.getUnitTemplateConfigCount() == 2);

        DeviceConfig config = deviceRegistry.registerDeviceConfig(getDeviceConfig("DeviceConfigWhereUnitsShallBeSetViaConsistency", "randomSerial14972", clazz, null)).get();
        assertTrue("Units in device config where not set according to the device classes unit templates", config.getUnitConfigCount() == clazz.getUnitTemplateConfigCount());
        boolean containsLight = false;
        boolean containsHandlseSensor = false;
        for (UnitConfig unit : config.getUnitConfigList()) {
            if (unit.getType().equals(unitTemplateConfig1.getType())) {
                containsLight = true;
                assertEquals("The light unit contains more or less services than the template config", unit.getServiceConfigCount(), unitTemplateConfig1.getServiceTemplateCount());
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

        deviceRegistry.updateUnitTemplate(UnitTemplate.newBuilder().setType(UnitType.BUTTON).addServiceType(ServiceType.BUTTON_PROVIDER).build()).get();

        clazz = deviceRegistry.updateDeviceClass(clazz.toBuilder().addUnitTemplateConfig(unitTemplateConfig3).build()).get();
        config = deviceRegistry.getDeviceConfigById(config.getId());
        assertEquals("Unit configs and templates differ after the update of the device class", config.getUnitConfigCount(), clazz.getUnitTemplateConfigCount());
        assertEquals("Device config does not contain the right unit config", config.getUnitConfig(2).getType(), unitTemplateConfig3.getType());
        assertEquals("Unit config does not contain the right service", config.getUnitConfig(2).getServiceConfig(0).getType(), serviceTemplate4.getServiceType());

        // clearing unit templates because of effects on other tests it might have
        deviceRegistry.updateUnitTemplate(UnitTemplate.newBuilder().setType(UnitType.LIGHT).build());
        deviceRegistry.updateUnitTemplate(UnitTemplate.newBuilder().setType(UnitType.HANDLE_SENSOR).build());
        deviceRegistry.updateUnitTemplate(UnitTemplate.newBuilder().setType(UnitType.BUTTON).build());
    }

    @Test(timeout = 5000)
    public void testBoundToDeviceConsistencyHandler() throws Exception {
        ServiceTemplate serviceTemplate1 = ServiceTemplate.newBuilder().setServiceType(ServiceType.POWER_SERVICE).build();
        UnitTemplateConfig unitTemplateConfig1 = UnitTemplateConfig.newBuilder().setType(UnitType.LIGHT).addServiceTemplate(serviceTemplate1).build();
        deviceRegistry.updateUnitTemplate(UnitTemplate.newBuilder().setType(UnitType.LIGHT).addServiceType(serviceTemplate1.getServiceType()).build());

        BindingConfigType.BindingConfig bindingConfig = BindingConfigType.BindingConfig.newBuilder().setType(BindingTypeHolderType.BindingTypeHolder.BindingType.OPENHAB).build();
        DeviceClass clazz = deviceRegistry.registerDeviceClass(getDeviceClass("BoundToDeviceTest", "boundToDevicePNR", "company").toBuilder().addUnitTemplateConfig(unitTemplateConfig1).setBindingConfig(bindingConfig).build()).get();

        DeviceConfig config = deviceRegistry.registerDeviceConfig(getDeviceConfig("BoundToDeviceTestDevice", "boundToDeviceSNR", clazz, null)).get();
        assertTrue("Unit config has not been added to device config", config.getUnitConfigCount() == 1);
        assertTrue("Unit config has not been set as bound to device", config.getUnitConfig(0).getBoundToDevice());
        assertTrue("Placement config of unit and device do not match although unit is bound to device", config.getUnitConfig(0).getPlacementConfig().equals(config.getPlacementConfig()));

        LocationConfig testLocation = locationRegistry.registerLocationConfig(LocationConfig.newBuilder().setLabel("BoundToDeviceTestLocation").build()).get();
        PlacementConfig placement = config.getUnitConfig(0).getPlacementConfig().toBuilder().setLocationId(testLocation.getId()).build();
        UnitConfig unit = config.getUnitConfig(0).toBuilder().setPlacementConfig(placement).build();
        config = config.toBuilder().clearUnitConfig().addUnitConfig(unit).build();
        assertTrue("Units placement config has not been modified correctly", config.getUnitConfig(0).getPlacementConfig().getLocationId().equals(testLocation.getId()));

        config = deviceRegistry.updateDeviceConfig(config).get();
        assertTrue("Unit is not bound to device anymore", config.getUnitConfig(0).getBoundToDevice());
        assertTrue("Placement config of unit and device do not match although unit is bound to device", config.getUnitConfig(0).getPlacementConfig().equals(config.getPlacementConfig()));
        assertEquals("Location id in placement config of unit does not equals that in device", config.getPlacementConfig().getLocationId(), config.getUnitConfig(0).getPlacementConfig().getLocationId());

        locationRegistry.removeLocationConfig(testLocation).get();
        deviceRegistry.updateUnitTemplate(UnitTemplate.newBuilder().setType(UnitType.LIGHT).build()).get();
    }

    /**
     * Test if the owner of a device is updated correctly.
     *
     * @throws java.lang.Exception
     */
    @Test(timeout = 5000)
    public void testOwnerRemoval() throws Exception {
        UserConfig owner = userRegistry.registerUserConfig(UserConfig.newBuilder().setUserName("owner").setFirstName("Max").setLastName("Mustermann").setEnablingState(EnablingState.newBuilder().setValue(EnablingState.State.ENABLED)).build()).get();

        ArrayList<UnitConfig> units = new ArrayList<>();
        DeviceClass clazz = deviceRegistry.registerDeviceClass(getDeviceClass("OwnerRemovalTest", "194872639127319823", "ServiceGMBH")).get();
        DeviceConfig ownerRemovalDeviceConfig = getDeviceConfig("OwnerRemovalTestDevice", "1249726918723918723", clazz, units);
        ownerRemovalDeviceConfig = ownerRemovalDeviceConfig.toBuilder().setInventoryState(InventoryState.newBuilder().setOwnerId(owner.getId()).setValue(InventoryState.State.IN_STOCK)).build();
        ownerRemovalDeviceConfig = deviceRegistry.registerDeviceConfig(ownerRemovalDeviceConfig).get();

        assertEquals("The device does not have the correct owner id!", owner.getId(), ownerRemovalDeviceConfig.getInventoryState().getOwnerId());

        userRegistry.removeUserConfig(owner).get();
        // wait until device registry consistency check was triggered by user registry and the owner was removed.
        try {
            //deviceRegistryRemote.waitForData(100, TimeUnit.MILLISECONDS);
            deviceRegistry.waitForConsistency();
            deviceRegistryRemote.requestData().get();
        } catch (CouldNotPerformException e) {
            // may data update was received before.
        }

        assertTrue("The owner did not get removed!", !userRegistry.containsUserConfig(owner));
        ownerRemovalDeviceConfig = deviceRegistry.getDeviceConfigById(ownerRemovalDeviceConfig.getId());
        assertEquals("The owner id did not get removed even though the user got removed!", "", ownerRemovalDeviceConfig.getInventoryState().getOwnerId());
    }

    /**
     * Test if the owner of a device is updated correctly.
     *
     * @throws java.lang.Exception
     */
    @Test(timeout = 5000)
    public void testLocationIdInInventoryState() throws Exception {
        ArrayList<UnitConfig> units = new ArrayList<>();
        DeviceClass clazz = deviceRegistry.registerDeviceClass(getDeviceClass("testLocationIdInInventoryState", "103721ggbdk12", "ServiceGMBH")).get();
        DeviceConfig testLocationIdInInventoryStateDevice = getDeviceConfig("testLocationIdInInventoryStateDevice", "103721ggbdk12", clazz, units);
        testLocationIdInInventoryStateDevice = testLocationIdInInventoryStateDevice.toBuilder().setInventoryState(InventoryState.newBuilder().setValue(InventoryState.State.INSTALLED)).build();
        testLocationIdInInventoryStateDevice = deviceRegistry.registerDeviceConfig(testLocationIdInInventoryStateDevice).get();

        assertEquals("The location id in the inventory state has not been set for an installed device!", LOCATION.getId(), testLocationIdInInventoryStateDevice.getInventoryState().getLocationId());
    }

    /**
     * Test if when breaking an existing device the sandbox registers it and
     * does not modify the real registry.
     *
     * @throws java.lang.Exception
     */
    @Test(timeout = 5000)
    public void testSandbox() throws Exception {
        BindingConfig bindingConfig = BindingConfig.newBuilder().setType(BindingTypeHolderType.BindingTypeHolder.BindingType.SINACT).build();
        UnitTemplateConfig unitTemplateConfig = UnitTemplateConfig.newBuilder().setType(UnitType.LIGHT).build();
        DeviceClass clazz = deviceRegistry.registerDeviceClass(getDeviceClass("SandboxTestLabel", "SandboxTestPNR", "SanboxCompany").toBuilder().setBindingConfig(bindingConfig).addUnitTemplateConfig(unitTemplateConfig).build()).get();

        String unitLabel = "SandboxTestUnitLabel";
        ArrayList<UnitConfig> units = new ArrayList<>();
        units.add(getUnitConfig(UnitType.LIGHT, unitLabel));
        units.add(getUnitConfig(UnitType.LIGHT, unitLabel + "2"));

        DeviceConfig deviceConfig = deviceRegistry.registerDeviceConfig(getDeviceConfig("SandboxTestDeviceLabel", "SandboxTestSNR", clazz, units)).get();
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
            ExceptionPrinter.setBeQuit(Boolean.TRUE);
            deviceRegistry.updateDeviceConfig(deviceConfig).get();
            fail("No exception thrown after updating a device with 2 units in the same location with the same label");
        } catch (Exception ex) {
        } finally {
            ExceptionPrinter.setBeQuit(Boolean.FALSE);
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

    private InventoryState getDefaultInventoryState() {
        return InventoryState.newBuilder().setValue(InventoryState.State.IN_STOCK).build();
    }

    private UnitConfig getUnitConfig(UnitType type, String label) {
        return UnitConfig.newBuilder().setPlacementConfig(getDefaultPlacement()).setType(type).setLabel(label).setBoundToDevice(false).build();
    }

    private DeviceConfig getDeviceConfig(String label, String serialNumber, DeviceClass clazz, ArrayList<UnitConfig> units) {
        if (units == null) {
            return DeviceConfig.newBuilder().setPlacementConfig(getDefaultPlacement()).setInventoryState(getDefaultInventoryState()).setLabel(label).setSerialNumber(serialNumber).setDeviceClassId(clazz.getId()).build();
        }
        return DeviceConfig.newBuilder().setPlacementConfig(getDefaultPlacement()).setInventoryState(getDefaultInventoryState()).setLabel(label).setSerialNumber(serialNumber).setDeviceClassId(clazz.getId()).addAllUnitConfig(units).build();
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
        DeviceClass.Builder deviceClassRemoteMessage;
        DeviceConfig.Builder deviceConfigRemoteMessage;
        deviceClassRemoteMessage = DeviceClass.newBuilder().setLabel("RemoteTestDeviceClass").setProductNumber("ABR-132").setCompany("DreamCom");
        deviceClassRemoteMessage = deviceRegistryRemote.registerDeviceClass(deviceClassRemoteMessage.build()).get().toBuilder();
        deviceRegistryRemote.waitForConnectionState(Remote.RemoteConnectionState.CONNECTED);
        while (true) {
            try {
                if (deviceRegistryRemote.containsDeviceClass(deviceClassRemoteMessage.clone().build())) {
                    break;
                }
            } catch (NotAvailableException ex) {
                logger.debug("Not ready yet");
            }
            Thread.yield();
        }
        assertTrue(deviceRegistryRemote.containsDeviceClass(deviceClassRemoteMessage.clone().build()));

        deviceConfigRemoteMessage = getDeviceConfig("RemoteTestDeviceConfig", "1123-5813-2134", deviceClassRemoteMessage.build(), null).toBuilder();
        deviceConfigRemoteMessage = deviceRegistryRemote.registerDeviceConfig(deviceConfigRemoteMessage.build()).get().toBuilder();
        while (true) {
            if (deviceRegistryRemote.containsDeviceConfig(deviceConfigRemoteMessage.clone().build())) {
                break;
            }
            Thread.yield();
        }
        assertTrue(deviceRegistryRemote.containsDeviceConfig(deviceConfigRemoteMessage.clone().build()));
    }

    /**
     * Test of registering a DeviceConfig per remote.
     */
    @Test(timeout = 3000)
    public void testGetReadOnlyFlag() throws Exception {
        System.out.println("testGetReadOnlyFlag");
        System.out.println("remote state: " + deviceRegistryRemote.getConnectionState().name());
        deviceRegistryRemote.waitForConnectionState(Remote.RemoteConnectionState.CONNECTED);
        assertEquals(Boolean.FALSE, deviceRegistryRemote.isDeviceClassRegistryReadOnly());
        assertEquals(Boolean.FALSE, deviceRegistryRemote.isDeviceConfigRegistryReadOnly());
        assertEquals(Boolean.FALSE, deviceRegistryRemote.isUnitTemplateRegistryReadOnly());
    }
}
