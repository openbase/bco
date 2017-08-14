package org.openbase.bco.registry.unit.test;

/*
 * #%L
 * BCO Registry Unit Test
 * %%
 * Copyright (C) 2014 - 2017 openbase.org
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
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;
import static junit.framework.TestCase.fail;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openbase.bco.registry.device.core.DeviceRegistryController;
import org.openbase.bco.registry.mock.MockRegistry;
import org.openbase.bco.registry.mock.MockRegistryHolder;
import org.openbase.bco.registry.unit.core.UnitRegistryController;
import org.openbase.jps.core.JPService;
import org.openbase.jps.exception.JPServiceException;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InitializationException;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.exception.InvalidStateException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.extension.rsb.scope.ScopeGenerator;
import org.openbase.jul.pattern.Observable;
import org.openbase.jul.pattern.Observer;
import org.openbase.jul.schedule.SyncObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rst.configuration.EntryType;
import rst.configuration.MetaConfigType;
import rst.domotic.binding.BindingConfigType.BindingConfig;
import rst.domotic.service.ServiceConfigType.ServiceConfig;
import rst.domotic.service.ServiceDescriptionType.ServiceDescription;
import rst.domotic.service.ServiceTemplateConfigType.ServiceTemplateConfig;
import rst.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import rst.domotic.state.EnablingStateType.EnablingState;
import rst.domotic.state.InventoryStateType.InventoryState;
import rst.domotic.unit.UnitConfigType.UnitConfig;
import rst.domotic.unit.UnitTemplateConfigType.UnitTemplateConfig;
import rst.domotic.unit.UnitTemplateType.UnitTemplate;
import rst.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;
import rst.domotic.unit.device.DeviceClassType.DeviceClass;
import rst.domotic.unit.device.DeviceConfigType.DeviceConfig;
import rst.domotic.unit.location.LocationConfigType.LocationConfig;
import rst.domotic.unit.user.UserConfigType.UserConfig;
import rst.geometry.AxisAlignedBoundingBox3DFloatType;
import rst.geometry.PoseType;
import rst.geometry.RotationType;
import rst.geometry.TranslationType;
import rst.math.Vec3DDoubleType;
import rst.math.Vec3DDoubleType.Vec3DDouble;
import rst.rsb.ScopeType;
import rst.spatial.PlacementConfigType.PlacementConfig;
import rst.spatial.ShapeType;

/**
 *
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public class DeviceRegistryTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(DeviceRegistryTest.class);

    private static MockRegistry mockRegistry;

    private static UnitRegistryController unitRegistry;
    private static DeviceRegistryController deviceRegistry;

    private static UnitConfig LOCATION;

    @BeforeClass
    public static void setUpClass() throws InstantiationException, InitializationException, IOException, InvalidStateException, JPServiceException, InterruptedException, CouldNotPerformException, ExecutionException {
        JPService.setupJUnitTestMode();
    }

    @AfterClass
    public static void tearDownClass() {
        try {
            MockRegistryHolder.shutdownMockRegistry();
        } catch (Throwable ex) {
            ExceptionPrinter.printHistoryAndReturnThrowable(ex, LOGGER);
        }
    }

    @Before
    public void setUp() throws CouldNotPerformException {
        mockRegistry = MockRegistryHolder.newMockRegistry();

        unitRegistry = (UnitRegistryController) mockRegistry.getUnitRegistry();
        deviceRegistry = (DeviceRegistryController) mockRegistry.getDeviceRegistry();

        LOCATION = mockRegistry.getLocationRegistry().getRootLocationConfig();
    }

    @After
    public void tearDown() throws CouldNotPerformException {
        MockRegistryHolder.shutdownMockRegistry();
//        mockRegistry = MockRegistryHolder.newMockRegistry();
//
//        unitRegistry = (UnitRegistryController) mockRegistry.getUnitRegistry();
//        deviceRegistry = (DeviceRegistryController) mockRegistry.getDeviceRegistry();
//
//        LOCATION = mockRegistry.getLocationRegistry().getRootLocationConfig();
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
        System.out.println("testRegisterDeviceConfigWithUnits");
        String productNumber = "ABCD-4321";
        String serialNumber = "1234-WXYZ";
        String company = "Fibaro";

        String deviceLabel = "TestSensor";
        String deviceScope = "/" + LOCATION.getLabel().toLowerCase() + "/" + "device" + "/" + deviceLabel.toLowerCase() + "/";

        String expectedUnitScope = "/" + LOCATION.getLabel().toLowerCase() + "/" + UnitType.BATTERY.name().toLowerCase() + "/" + deviceLabel.toLowerCase() + "/";

        // units are automatically added when a unit template config in the device class exists
        UnitTemplateConfig unitTemplateConfig = UnitTemplateConfig.newBuilder().setType(UnitType.BATTERY).build();
        unitRegistry.getDeviceRegistryRemote().addDataObserver(notifyChangeObserver);
        DeviceClass motionSensorClass = deviceRegistry.registerDeviceClass(getDeviceClass("F_MotionSensor", productNumber, company).toBuilder().addUnitTemplateConfig(unitTemplateConfig).build()).get();
        waitForDeviceClass(motionSensorClass);
        unitRegistry.getDeviceRegistryRemote().removeDataObserver(notifyChangeObserver);
        UnitConfig motionSensorConfig = getDeviceUnitConfig(deviceLabel, serialNumber, motionSensorClass);
        motionSensorConfig = deviceRegistry.registerDeviceConfig(motionSensorConfig).get();

        assertEquals("Device scope is not set properly", deviceScope, ScopeGenerator.generateStringRep(motionSensorConfig.getScope()));
        assertEquals("Device has not the correct number of units", 1, motionSensorConfig.getDeviceConfig().getUnitIdCount());

        UnitConfig batteryUnit = unitRegistry.getUnitConfigById(motionSensorConfig.getDeviceConfig().getUnitId(0));
        assertEquals("Unit scope is not set properly", expectedUnitScope, ScopeGenerator.generateStringRep(batteryUnit.getScope()));
        assertEquals("Device id is not set in unit", motionSensorConfig.getId(), batteryUnit.getUnitHostId());
    }

    /**
     * Test of testRegiseredDeviceConfigWithoutLabel method, of class
     * DeviceRegistryImpl.
     */
    @Test(timeout = 5000)
    public void testRegisteredDeviceConfigWithoutLabel() throws Exception {
        System.out.println("testRegisteredDeviceConfigWithoutLabel");
        String productNumber = "KNHD-4321";
        String serialNumber = "112358";
        String company = "Company";

        unitRegistry.getDeviceRegistryRemote().addDataObserver(notifyChangeObserver);
        DeviceClass clazz = deviceRegistry.registerDeviceClass(getDeviceClass("WithoutLabel", productNumber, company)).get();
        waitForDeviceClass(clazz);
        unitRegistry.getDeviceRegistryRemote().removeDataObserver(notifyChangeObserver);
        UnitConfig deviceWithoutLabel = getDeviceUnitConfig("", serialNumber, clazz);
        deviceWithoutLabel = deviceRegistry.registerDeviceConfig(deviceWithoutLabel).get();

        assertEquals("The device label is not set as the id if it is empty!", deviceWithoutLabel.getId(), deviceWithoutLabel.getLabel());
    }

    /**
     * Test of testRegisterTwoDevicesWithSameLabel method, of class
     * DeviceRegistryImpl.
     */
    @Test(timeout = 5000)
    public void testRegisterTwoDevicesWithSameLabel() throws Exception {
        System.out.println("testRegisterTwoDevicesWithSameLabel");
        String serialNumber1 = "FIRST_DEV";
        String serialNumber2 = "BAD_DEV";
        String deviceLabel = "SameLabelSameLocation";

        unitRegistry.getDeviceRegistryRemote().addDataObserver(notifyChangeObserver);
        DeviceClass clazz = deviceRegistry.registerDeviceClass(getDeviceClass("WithoutLabel", "xyz", "HuxGMBH")).get();
        waitForDeviceClass(clazz);
        unitRegistry.getDeviceRegistryRemote().removeDataObserver(notifyChangeObserver);
        UnitConfig deviceWithLabel1 = getDeviceUnitConfig(deviceLabel, serialNumber1, clazz);
        UnitConfig deviceWithLabel2 = getDeviceUnitConfig(deviceLabel, serialNumber2, clazz);

        deviceRegistry.registerDeviceConfig(deviceWithLabel1).get();
        try {
            ExceptionPrinter.setBeQuit(Boolean.TRUE);
            deviceRegistry.registerDeviceConfig(deviceWithLabel2).get();
            fail("There was no exception thrown even though two devices with the same label [" + deviceLabel + "] where registered in the same location [" + LOCATION.getLabel() + "]");
        } catch (CouldNotPerformException | InterruptedException | ExecutionException ex) {
            assertTrue(true);
        } finally {
            ExceptionPrinter.setBeQuit(Boolean.FALSE);
        }
    }

    @Test(timeout = 5000)
    public void testUnitConfigUnitTemplateConsistencyHandler() throws Exception {
        System.out.println("testUnitConfigUnitTemplateConsistencyHandler");

        // clearing unit templates because they are already changed by the mock registry
        UnitTemplate unitTemplate = unitRegistry.getUnitTemplateByType(UnitType.COLORABLE_LIGHT).toBuilder().clearServiceDescription().build();
        unitRegistry.updateUnitTemplate(unitTemplate).get();

        ServiceDescription batteryTemplate = ServiceDescription.newBuilder().setType(ServiceType.BATTERY_STATE_SERVICE).build();
        ServiceDescription colorTemplate = ServiceDescription.newBuilder().setType(ServiceType.COLOR_STATE_SERVICE).build();
        unitTemplate = unitRegistry.getUnitTemplateByType(UnitType.COLORABLE_LIGHT);
        unitTemplate = unitTemplate.toBuilder().addServiceDescription(batteryTemplate).addServiceDescription(colorTemplate).build();
        unitTemplate = unitRegistry.updateUnitTemplate(unitTemplate).get();
        assertTrue(unitTemplate.getServiceDescriptionList().get(0).getType() == ServiceType.BATTERY_STATE_SERVICE);
        assertTrue(unitTemplate.getServiceDescriptionList().get(1).getType() == ServiceType.COLOR_STATE_SERVICE);
        assertTrue(unitTemplate.getType() == UnitType.COLORABLE_LIGHT);

        UnitTemplateConfig unitTemplateConfig = UnitTemplateConfig.newBuilder().setType(unitTemplate.getType()).build();
        String serialNumber1 = "5073";
        String deviceLabel = "thisIsARandomLabel12512";
        BindingConfig bindingConfig = BindingConfig.newBuilder().setBindingId("OPENHAB").build();
        unitRegistry.getDeviceRegistryRemote().addDataObserver(notifyChangeObserver);
        DeviceClass clazz = deviceRegistry.registerDeviceClass(getDeviceClass("unitTest", "423112358", "company").toBuilder().setBindingConfig(bindingConfig).addUnitTemplateConfig(unitTemplateConfig).build()).get();
        waitForDeviceClass(clazz);
        unitRegistry.getDeviceRegistryRemote().removeDataObserver(notifyChangeObserver);

        MetaConfigType.MetaConfig metaConfig = MetaConfigType.MetaConfig.newBuilder().addEntry(EntryType.Entry.newBuilder().setKey("testKey")).build();
        UnitConfig localDeviceConfig = getDeviceUnitConfig(deviceLabel, serialNumber1, clazz);

        localDeviceConfig = deviceRegistry.registerDeviceConfig(localDeviceConfig).get();
        assertTrue("DeviceConfig does not contain the correct amount of units", localDeviceConfig.getDeviceConfig().getUnitIdCount() == 1);

        UnitConfig registeredUnit = unitRegistry.getUnitConfigById(localDeviceConfig.getDeviceConfig().getUnitId(0));
        assertTrue("The amount of service configs for the unit is not correct", registeredUnit.getServiceConfigCount() == 2);
        assertTrue(registeredUnit.getServiceConfig(0).getServiceDescription().getType() == ServiceType.BATTERY_STATE_SERVICE || registeredUnit.getServiceConfig(0).getServiceDescription().getType() == ServiceType.COLOR_STATE_SERVICE);
        assertTrue(registeredUnit.getServiceConfig(1).getServiceDescription().getType() == ServiceType.BATTERY_STATE_SERVICE || registeredUnit.getServiceConfig(1).getServiceDescription().getType() == ServiceType.COLOR_STATE_SERVICE);

        ServiceConfig tmpServiceConfig;
        if (registeredUnit.getServiceConfig(0).getServiceDescription().getType() == ServiceType.BATTERY_STATE_SERVICE) {
            tmpServiceConfig = registeredUnit.getServiceConfig(0);
            tmpServiceConfig = tmpServiceConfig.toBuilder().setMetaConfig(metaConfig).build();
            registeredUnit = unitRegistry.updateUnitConfig(registeredUnit.toBuilder().setServiceConfig(0, tmpServiceConfig).build()).get();
            assertEquals(metaConfig, registeredUnit.getServiceConfig(0).getMetaConfig());
        } else if (registeredUnit.getServiceConfig(1).getServiceDescription().getType() == ServiceType.BATTERY_STATE_SERVICE) {
            tmpServiceConfig = registeredUnit.getServiceConfig(1);
            tmpServiceConfig = tmpServiceConfig.toBuilder().setMetaConfig(metaConfig).build();
            registeredUnit = unitRegistry.updateUnitConfig(registeredUnit.toBuilder().setServiceConfig(1, tmpServiceConfig).build()).get();
            assertEquals(metaConfig, registeredUnit.getServiceConfig(1).getMetaConfig());
        }
    }

    @Test(timeout = 5000)
    public void testDeviceClassDeviceConfigUnitConsistencyHandler() throws Exception {
        System.out.println("testDeviceClassDeviceConfigUnitConsistencyHandler");

        // clearing unit templates because they are already changed by the mock registry
        UnitTemplate unitTemplate = unitRegistry.getUnitTemplateByType(UnitType.LIGHT).toBuilder().clearServiceDescription().build();
        unitRegistry.updateUnitTemplate(unitTemplate).get();
        unitTemplate = unitRegistry.getUnitTemplateByType(UnitType.HANDLE).toBuilder().clearServiceDescription().build();
        unitRegistry.updateUnitTemplate(unitTemplate).get();
        unitTemplate = unitRegistry.getUnitTemplateByType(UnitType.BUTTON).toBuilder().clearServiceDescription().build();
        unitRegistry.updateUnitTemplate(unitTemplate).get();

        ServiceTemplateConfig serviceTemplate1 = ServiceTemplateConfig.newBuilder().setServiceType(ServiceType.POWER_STATE_SERVICE).build();
        UnitTemplateConfig unitTemplateConfig1 = UnitTemplateConfig.newBuilder().setType(UnitType.LIGHT).addServiceTemplateConfig(serviceTemplate1).build();
        ServiceTemplateConfig serviceTemplate2 = ServiceTemplateConfig.newBuilder().setServiceType(ServiceType.BATTERY_STATE_SERVICE).build();
        ServiceTemplateConfig serviceTemplate3 = ServiceTemplateConfig.newBuilder().setServiceType(ServiceType.HANDLE_STATE_SERVICE).build();
        UnitTemplateConfig unitTemplateConfig2 = UnitTemplateConfig.newBuilder().setType(UnitType.HANDLE).addServiceTemplateConfig(serviceTemplate2).addServiceTemplateConfig(serviceTemplate3).build();

        unitTemplate = unitRegistry.getUnitTemplateByType(UnitType.LIGHT).toBuilder().addServiceDescription(ServiceDescription.newBuilder().setType(serviceTemplate1.getServiceType())).build();
        unitRegistry.updateUnitTemplate(unitTemplate).get();
        unitTemplate = unitRegistry.getUnitTemplateByType(UnitType.HANDLE).toBuilder().addServiceDescription(ServiceDescription.newBuilder().setType(ServiceType.BATTERY_STATE_SERVICE)).addServiceDescription(ServiceDescription.newBuilder().setType(ServiceType.HANDLE_STATE_SERVICE)).build();
        unitRegistry.updateUnitTemplate(unitTemplate).get();

        unitRegistry.getDeviceRegistryRemote().addDataObserver(notifyChangeObserver);
        BindingConfig bindingConfig = BindingConfig.newBuilder().setBindingId("OPENHAB").build();
        DeviceClass clazz = deviceRegistry.registerDeviceClass(getDeviceClass("unittemplateUnitConfigTest", "0149283794283", "company").toBuilder().addUnitTemplateConfig(unitTemplateConfig1).addUnitTemplateConfig(unitTemplateConfig2).setBindingConfig(bindingConfig).build()).get();
        assertTrue(clazz.getUnitTemplateConfigCount() == 2);
        waitForDeviceClass(clazz);
        unitRegistry.getDeviceRegistryRemote().removeDataObserver(notifyChangeObserver);

        UnitConfig config = deviceRegistry.registerDeviceConfig(getDeviceUnitConfig("DeviceConfigWhereUnitsShallBeSetViaConsistency", "randomSerial14972", clazz)).get();
        assertEquals("Units in device config were not set according to the device classes unit templates", clazz.getUnitTemplateConfigCount(), config.getDeviceConfig().getUnitIdCount());
        boolean containsLight = false;
        boolean containsHandlseSensor = false;
        List<UnitConfig> dalUnitConfigs = new ArrayList<>();
        for (String unitId : config.getDeviceConfig().getUnitIdList()) {
            dalUnitConfigs.add(unitRegistry.getUnitConfigById(unitId));
        }
        for (UnitConfig unit : dalUnitConfigs) {
            if (unit.getType().equals(unitTemplateConfig1.getType())) {
                containsLight = true;
                assertEquals("The light unit contains more or less services than the template config", unit.getServiceConfigCount(), unitTemplateConfig1.getServiceTemplateConfigCount());
                assertTrue("The service type of the light unit does not match", unit.getServiceConfig(0).getServiceDescription().getType().equals(serviceTemplate1.getServiceType()));
            } else if (unit.getType().equals(unitTemplateConfig2.getType())) {
                containsHandlseSensor = true;
                assertTrue("The handle sensor unit contains more or less services than the template config", unit.getServiceConfigCount() == unitTemplateConfig2.getServiceTemplateConfigCount());
                assertEquals("The service type of the handle sensor unit does not match", unit.getServiceConfig(0).getServiceDescription().getType(), serviceTemplate2.getServiceType());
                assertEquals("The service type of the handle sensor unit does not match", unit.getServiceConfig(1).getServiceDescription().getType(), serviceTemplate3.getServiceType());
            }
        }
        assertTrue("The device config does not contain a light unit even though the device class has an according unit template", containsLight);
        assertTrue("The device config does not contain a handle sensor unit even though the device class has an according unit template", containsHandlseSensor);

        ServiceTemplateConfig serviceTemplate4 = ServiceTemplateConfig.newBuilder().setServiceType(ServiceType.BUTTON_STATE_SERVICE).build();
        UnitTemplateConfig unitTemplateConfig3 = UnitTemplateConfig.newBuilder().setType(UnitType.BUTTON).addServiceTemplateConfig(serviceTemplate1).build();

        unitTemplate = unitRegistry.getUnitTemplateByType(UnitType.BUTTON).toBuilder().addServiceDescription(ServiceDescription.newBuilder().setType(ServiceType.BUTTON_STATE_SERVICE)).build();
        unitRegistry.updateUnitTemplate(unitTemplate).get();

        System.out.println("Updating deviceClass...");
        clazz = deviceRegistry.updateDeviceClass(clazz.toBuilder().addUnitTemplateConfig(unitTemplateConfig3).build()).get();
        config = unitRegistry.getUnitConfigById(config.getId());
        // the update is not synced immediatly to the device config, thus this waits and fails if the timeout is exceeded
        while (config.getDeviceConfig().getUnitIdCount() != clazz.getUnitTemplateConfigCount()) {
            Thread.sleep(100);
            config = unitRegistry.getUnitConfigById(config.getId());
        }
        assertEquals("Unit configs and templates differ after the update of the device class", config.getDeviceConfig().getUnitIdCount(), clazz.getUnitTemplateConfigCount());

        dalUnitConfigs.clear();
        for (String unitId : config.getDeviceConfig().getUnitIdList()) {
            dalUnitConfigs.add(unitRegistry.getUnitConfigById(unitId));
        }
        assertEquals("Device config does not contain the right unit config", dalUnitConfigs.get(2).getType(), unitTemplateConfig3.getType());
        assertEquals("Unit config does not contain the right service", dalUnitConfigs.get(2).getServiceConfig(0).getServiceDescription().getType(), serviceTemplate4.getServiceType());

        int sizeBefore = unitRegistry.getDalUnitConfigs().size();
        config = deviceRegistry.updateDeviceConfig(config.toBuilder().setLabel("newDeviceLabel").build()).get();
        assertTrue("More dal units registered after device renaming!", unitRegistry.getDalUnitConfigs().size() == sizeBefore);
        assertTrue("More units in device config after renaming!", config.getDeviceConfig().getUnitIdCount() == 3);

        //test if dal units are also removed when a device is removed
        deviceRegistry.removeDeviceConfig(config).get();
        for (UnitConfig dalUnitConfig : dalUnitConfigs) {
            assertTrue("DalUnit [" + dalUnitConfig.getLabel() + "] still registered even though its device has been removed!", !unitRegistry.containsUnitConfig(dalUnitConfig));
        }

    }
    
    @Test(timeout = 5000)
    public void testBoundingBoxConsistencyHandler() throws Exception {
        // how to get a test unit?
        UnitConfig testUnit = UnitConfig.newBuilder().setType(UnitTemplate.UnitType.COLORABLE_LIGHT).build();
        
       // UnitConfig testUnit = deviceRegistry.getDeviceConfigById("PH_Hue_E27_Device");
        TranslationType.Translation.Builder translationBuilder = TranslationType.Translation.newBuilder().setX(0).setY(0).setZ(0);
        Vec3DDoubleType.Vec3DDouble vector1 = Vec3DDoubleType.Vec3DDouble.newBuilder().setX(0.1).setY(4.2).setZ(0.0).build();
        Vec3DDoubleType.Vec3DDouble vector2 = Vec3DDoubleType.Vec3DDouble.newBuilder().setX(3.6).setY(0.0).setZ(0.0).build();
        Vec3DDoubleType.Vec3DDouble vector3 = Vec3DDoubleType.Vec3DDouble.newBuilder().setX(5.1).setY(2.9).setZ(0.0).build();
        Vec3DDoubleType.Vec3DDouble vector4 = Vec3DDoubleType.Vec3DDouble.newBuilder().setX(2.7).setY(2.1).setZ(0.0).build();
        ArrayList<Vec3DDouble> values = new ArrayList<>(Arrays.asList(vector1, vector2, vector3, vector4)); 
        ShapeType.Shape shapeBuilder =  ShapeType.Shape.newBuilder().addAllFloor(values).build();
        testUnit.toBuilder().getPlacementConfigBuilder().setShape(shapeBuilder);
        
        //deviceRegistry.updateDeviceConfig(testUnit).get(); 
        
        ShapeType.Shape shape = testUnit.getPlacementConfig().getShape();
        
        AxisAlignedBoundingBox3DFloatType.AxisAlignedBoundingBox3DFloat.Builder builder = AxisAlignedBoundingBox3DFloatType.AxisAlignedBoundingBox3DFloat.newBuilder();
        builder.setDepth((float) (4.2));
        builder.setHeight((float) (0.0));
        builder.setWidth((float) (5.0));
        TranslationType.Translation.Builder lfbBuilder = TranslationType.Translation.newBuilder().setX(0.1).setY(0.0).setZ(0.0);
        builder.setLeftFrontBottom(translationBuilder);
        //assertTrue(shape.getBoundingBox().equals(builder.build()));
    }

    @Test(timeout = 5000)
    public void testBoundToDeviceConsistencyHandler() throws Exception {
        System.out.println("testBoundToDeviceConsistencyHandler");
        ServiceTemplateConfig serviceTemplate1 = ServiceTemplateConfig.newBuilder().setServiceType(ServiceType.POWER_STATE_SERVICE).build();
        UnitTemplateConfig unitTemplateConfig1 = UnitTemplateConfig.newBuilder().setType(UnitType.LIGHT).addServiceTemplateConfig(serviceTemplate1).build();
        UnitTemplate unitTemplate = unitRegistry.getUnitTemplateByType(UnitType.LIGHT).toBuilder().addServiceDescription(ServiceDescription.newBuilder().setType(serviceTemplate1.getServiceType())).build();
        unitRegistry.updateUnitTemplate(unitTemplate).get();

        unitRegistry.getDeviceRegistryRemote().addDataObserver(notifyChangeObserver);
        BindingConfig bindingConfig = BindingConfig.newBuilder().setBindingId("OPENHAB").build();
        DeviceClass clazz = deviceRegistry.registerDeviceClass(getDeviceClass("BoundToDeviceTest", "boundToDevicePNR", "company").toBuilder().addUnitTemplateConfig(unitTemplateConfig1).setBindingConfig(bindingConfig).build()).get();
        waitForDeviceClass(clazz);
        unitRegistry.getDeviceRegistryRemote().removeDataObserver(notifyChangeObserver);

        UnitConfig config = deviceRegistry.registerDeviceConfig(getDeviceUnitConfig("BoundToDeviceTestDevice", "boundToDeviceSNR", clazz)).get();
        assertTrue("Unit config has not been added to device config", config.getDeviceConfig().getUnitIdCount() == 1);
        List<UnitConfig> dalUnitConfig = new ArrayList<>();
        for (String unitId : config.getDeviceConfig().getUnitIdList()) {
            dalUnitConfig.add(unitRegistry.getUnitConfigById(unitId));
        }
        assertTrue("Unit config has not been set as bound to device", dalUnitConfig.get(0).getBoundToUnitHost());
        assertTrue("Placement config of unit and device do not match although unit is bound to device", dalUnitConfig.get(0).getPlacementConfig().equals(config.getPlacementConfig()));

        LocationConfig locationConfig = LocationConfig.newBuilder().setType(LocationConfig.LocationType.ZONE).build();
        UnitConfig testLocation = unitRegistry.registerUnitConfig(UnitConfig.newBuilder().setLabel("BoundToDeviceTestLocation").setType(UnitType.LOCATION).setLocationConfig(locationConfig).build()).get();
        PlacementConfig placement = dalUnitConfig.get(0).getPlacementConfig().toBuilder().setLocationId(testLocation.getId()).build();
        UnitConfig unit = dalUnitConfig.get(0).toBuilder().setPlacementConfig(placement).build();

        unit = unitRegistry.updateUnitConfig(unit).get();
        assertTrue("Unit is not bound to device anymore", unit.getBoundToUnitHost());
        assertTrue("Placement config of unit and device do not match although unit is bound to device", unit.getPlacementConfig().equals(config.getPlacementConfig()));
        assertEquals("Location id in placement config of unit does not equals that in device", config.getPlacementConfig().getLocationId(), unit.getPlacementConfig().getLocationId());

        unitRegistry.removeUnitConfig(testLocation).get();
        unitTemplate = unitRegistry.getUnitTemplateByType(UnitType.LIGHT).toBuilder().clearServiceDescription().build();
        unitRegistry.updateUnitTemplate(unitTemplate).get();
    }

    /**
     * Test if the owner of a device is updated correctly.
     *
     * @throws java.lang.Exception
     */
    @Test(timeout = 5000)
    public void testOwnerRemoval() throws Exception {
        System.out.println("testOwnerRemoval");
        UserConfig userConfig = UserConfig.newBuilder().setUserName("owner").setFirstName("Max").setLastName("Mustermann").build();
        UnitConfig owner = unitRegistry.registerUnitConfig(UnitConfig.newBuilder().setType(UnitType.USER).setUserConfig(userConfig).setEnablingState(EnablingState.newBuilder().setValue(EnablingState.State.ENABLED)).build()).get();

        unitRegistry.getDeviceRegistryRemote().addDataObserver(notifyChangeObserver);
        DeviceClass clazz = deviceRegistry.registerDeviceClass(getDeviceClass("OwnerRemovalTest", "194872639127319823", "ServiceGMBH")).get();
        waitForDeviceClass(clazz);
        unitRegistry.getDeviceRegistryRemote().removeDataObserver(notifyChangeObserver);

        UnitConfig ownerRemovalDeviceConfig = getDeviceUnitConfig("OwnerRemovalTestDevice", "1249726918723918723", clazz);
        DeviceConfig tmp = ownerRemovalDeviceConfig.getDeviceConfig().toBuilder().setInventoryState(InventoryState.newBuilder().setOwnerId(owner.getId()).setValue(InventoryState.State.IN_STOCK)).build();
        ownerRemovalDeviceConfig = ownerRemovalDeviceConfig.toBuilder().setDeviceConfig(tmp).build();
        ownerRemovalDeviceConfig = deviceRegistry.registerDeviceConfig(ownerRemovalDeviceConfig).get();

        assertEquals("The device does not have the correct owner id!", owner.getId(), ownerRemovalDeviceConfig.getDeviceConfig().getInventoryState().getOwnerId());

        unitRegistry.removeUnitConfig(owner).get();

        assertTrue("The owner did not get removed!", !unitRegistry.containsUnitConfig(owner));
        ownerRemovalDeviceConfig = unitRegistry.getUnitConfigById(ownerRemovalDeviceConfig.getId());
        while (!ownerRemovalDeviceConfig.getDeviceConfig().getInventoryState().getOwnerId().isEmpty()) {
            Thread.sleep(100);
            ownerRemovalDeviceConfig = deviceRegistry.getDeviceConfigById(ownerRemovalDeviceConfig.getId());
        }
        assertEquals("The owner id did not get removed even though the user got removed!", "", ownerRemovalDeviceConfig.getDeviceConfig().getInventoryState().getOwnerId());
    }

    @Test(timeout = 5000)
    public void testInventoryEnablingStateConnection() throws Exception {
        System.out.println("testInventoryEnablingStateConnection");
        ServiceTemplateConfig serviceTemplate1 = ServiceTemplateConfig.newBuilder().setServiceType(ServiceType.POWER_STATE_SERVICE).build();
        UnitTemplateConfig unitTemplateConfig1 = UnitTemplateConfig.newBuilder().setType(UnitType.LIGHT).addServiceTemplateConfig(serviceTemplate1).build();
        UnitTemplate unitTemplate = unitRegistry.getUnitTemplateByType(UnitType.LIGHT).toBuilder().addServiceDescription(ServiceDescription.newBuilder().setType(serviceTemplate1.getServiceType())).build();
        unitRegistry.updateUnitTemplate(unitTemplate).get();

        unitRegistry.getDeviceRegistryRemote().addDataObserver(notifyChangeObserver);
        DeviceClass clazz = deviceRegistry.registerDeviceClass(getDeviceClass("testInventoryEnablingStateConnection", "1297389612873619", "Inventory").toBuilder().addUnitTemplateConfig(unitTemplateConfig1).build()).get();
        waitForDeviceClass(clazz);
        unitRegistry.getDeviceRegistryRemote().removeDataObserver(notifyChangeObserver);

        String label = "testLabel";
        UnitConfig.Builder device = getDeviceUnitConfig(label, "124972691872s3918723", clazz).toBuilder();
        DeviceConfig.Builder deviceConf = device.getDeviceConfigBuilder();
        InventoryState.Builder inventoryState = deviceConf.getInventoryStateBuilder();
        inventoryState.setValue(InventoryState.State.INSTALLED);

        device = deviceRegistry.registerDeviceConfig(device.build()).get().toBuilder();
        UnitConfig dalUnit = unitRegistry.getUnitConfigById(device.getDeviceConfig().getUnitId(0));
        assertTrue("DeviceUnitConfig is not Enabled", device.getEnablingState().getValue() == EnablingState.State.ENABLED);
        assertTrue("DalUnitConfig is not Enabled", device.getEnablingState().getValue() == dalUnit.getEnablingState().getValue());
        assertEquals("DeviceUnitConfig and DalUnitConfig have different labels", device.getLabel(), dalUnit.getLabel());

        label = label + "-2";
        device = deviceRegistry.updateDeviceConfig(device.setLabel(label).build()).get().toBuilder();
        dalUnit = unitRegistry.getUnitConfigById(device.getDeviceConfig().getUnitId(0));
        assertEquals(device.getLabel(), dalUnit.getLabel());

        deviceConf = device.getDeviceConfigBuilder();
        inventoryState = deviceConf.getInventoryStateBuilder();
        inventoryState.setValue(InventoryState.State.IN_STOCK);
        device = deviceRegistry.updateDeviceConfig(device.build()).get().toBuilder();
        dalUnit = unitRegistry.getUnitConfigById(device.getDeviceConfig().getUnitId(0));
        assertTrue("DeviceUnitConfig inventory state is not IN_STOCK", device.getDeviceConfig().getInventoryState().getValue() == InventoryState.State.IN_STOCK);
        assertTrue("DeviceUnitConfig is not Disabled", device.getEnablingState().getValue() == EnablingState.State.DISABLED);
        assertTrue("DalUnitConfig is not Disabled", device.getEnablingState().getValue() == dalUnit.getEnablingState().getValue());

        deviceConf = device.getDeviceConfigBuilder();
        inventoryState = deviceConf.getInventoryStateBuilder();
        inventoryState.setValue(InventoryState.State.INSTALLED);
        device = deviceRegistry.updateDeviceConfig(device.build()).get().toBuilder();
        dalUnit = unitRegistry.getUnitConfigById(device.getDeviceConfig().getUnitId(0));
        assertTrue("DeviceUnitConfig inventory state is not INSTALLED", device.getDeviceConfig().getInventoryState().getValue() == InventoryState.State.INSTALLED);
        assertTrue("DeviceUnitConfig is not Enabled", device.getEnablingState().getValue() == EnablingState.State.ENABLED);
        assertTrue("DalUnitConfig is not Enabled", device.getEnablingState().getValue() == dalUnit.getEnablingState().getValue());

        unitTemplate = unitRegistry.getUnitTemplateByType(UnitType.LIGHT).toBuilder().clearServiceDescription().build();
        unitRegistry.updateUnitTemplate(unitTemplate).get();
    }

    /**
     * Test if the owner of a device is updated correctly.
     *
     * @throws java.lang.Exception
     */
    @Test(timeout = 5000)
    public void testLocationIdInInventoryState() throws Exception {
        System.out.println("testLocationIdInInventoryState");
        unitRegistry.getDeviceRegistryRemote().addDataObserver(notifyChangeObserver);
        DeviceClass clazz = deviceRegistry.registerDeviceClass(getDeviceClass("testLocationIdInInventoryState", "103721ggbdk12", "ServiceGMBH")).get();
        waitForDeviceClass(clazz);
        unitRegistry.getDeviceRegistryRemote().removeDataObserver(notifyChangeObserver);

        UnitConfig testLocationIdInInventoryStateDevice = getDeviceUnitConfig("testLocationIdInInventoryStateDevice", "103721ggbdk12", clazz);
        DeviceConfig tmp = testLocationIdInInventoryStateDevice.getDeviceConfig().toBuilder().setInventoryState(InventoryState.newBuilder().setValue(InventoryState.State.INSTALLED)).build();
        testLocationIdInInventoryStateDevice = testLocationIdInInventoryStateDevice.toBuilder().setDeviceConfig(tmp).build();
        testLocationIdInInventoryStateDevice = deviceRegistry.registerDeviceConfig(testLocationIdInInventoryStateDevice).get();

        assertEquals("The location id in the inventory state has not been set for an installed device!", LOCATION.getId(), testLocationIdInInventoryStateDevice.getDeviceConfig().getInventoryState().getLocationId());
    }

    //TODO: think of a new way to break the registry to test this... 
    /**
     * Test if when breaking an existing device the sandbox registers it and
     * does not modify the real registry.
     *
     * @throws java.lang.Exception
     */
//    //@Test(timeout = 5000)
//    public void testSandbox() throws Exception {
//        BindingConfig bindingConfig = BindingConfig.newBuilder().setBindingId("SINACT").build();
//        UnitTemplateConfig unitTemplateConfig = UnitTemplateConfig.newBuilder().setType(UnitType.LIGHT).build();
//        DeviceClass clazz = deviceRegistry.registerDeviceClass(getDeviceClass("SandboxTestLabel", "SandboxTestPNR", "SanboxCompany").toBuilder().setBindingConfig(bindingConfig).addUnitTemplateConfig(unitTemplateConfig).build()).get();
//
//        UnitConfig sandboxDeviceConfig = deviceRegistry.registerDeviceConfig(getDeviceUnitConfig("SandboxTestDeviceLabel", "SandboxTestSNR", clazz)).get();
//        assertTrue(deviceRegistry.containsDeviceConfig(sandboxDeviceConfig));
//
//        UnitConfig.Builder sandboxDeviveConfigBuilder = sandboxDeviceConfig.toBuilder();
//        UnitConfig original = UnitConfig.newBuilder(sandboxDeviceConfig).build();
//        List<String> unitIds = new ArrayList(sandboxDeviveConfigBuilder.getDeviceConfig().getUnitIdList());
//        sandboxDeviveConfigBuilder.clearUnitConfig();
//        for (UnitConfig unitConfig : units) {
//            if (!unitConfig.getLabel().equals(unitLabel)) {
//                sandboxDeviveConfigBuilder.addUnitConfig(unitConfig.toBuilder().setLabel(unitLabel));
//            } else {
//                sandboxDeviveConfigBuilder.addUnitConfig(unitConfig);
//            }
//        }
//        sandboxDeviceConfig = sandboxDeviveConfigBuilder.build();
//
//        try {
//            ExceptionPrinter.setBeQuit(Boolean.TRUE);
//            deviceRegistry.updateDeviceConfig(sandboxDeviceConfig).get();
//            fail("No exception thrown after updating a device with 2 units in the same location with the same label");
//        } catch (Exception ex) {
//        } finally {
//            ExceptionPrinter.setBeQuit(Boolean.FALSE); 
//        }
//
//        assertEquals("DeviceConfig has been although the sandbox has rejected an update", original, deviceRegistry.getDeviceConfigById(sandboxDeviceConfig.getId()));
//    }
    private PlacementConfig getDefaultPlacement() {
        RotationType.Rotation rotation = RotationType.Rotation.newBuilder().setQw(1).setQx(0).setQy(0).setQz(0).build();
        TranslationType.Translation translation = TranslationType.Translation.newBuilder().setX(0).setY(0).setZ(0).build();
        PoseType.Pose pose = PoseType.Pose.newBuilder().setRotation(rotation).setTranslation(translation).build();
        ScopeType.Scope.Builder locationScope = ScopeType.Scope.newBuilder().addComponent(LOCATION.getLabel());
        return PlacementConfig.newBuilder().setPosition(pose).setLocationId(LOCATION.getId()).build();
    }

    private InventoryState getDefaultInventoryState() {
        return InventoryState.newBuilder().setValue(InventoryState.State.IN_STOCK).build();
    }

    private UnitConfig getUnitConfig(UnitType type, String label) {
        return UnitConfig.newBuilder().setPlacementConfig(getDefaultPlacement()).setType(type).setLabel(label).setBoundToUnitHost(false).build();
    }

    private UnitConfig getDeviceUnitConfig(String label, String serialNumber, DeviceClass clazz) {
        DeviceConfig tmpDeviceConfig = DeviceConfig.newBuilder().setDeviceClassId(clazz.getId()).setSerialNumber(serialNumber).setInventoryState(getDefaultInventoryState()).build();
        return UnitConfig.newBuilder().setType(UnitType.DEVICE).setPlacementConfig(getDefaultPlacement()).setLabel(label).setDeviceConfig(tmpDeviceConfig).build();
    }

    private DeviceClass getDeviceClass(String label, String productNumber, String company) {
        return DeviceClass.newBuilder().setLabel(label).setProductNumber(productNumber).setCompany(company).build();
    }

    private ServiceConfig getServiceConfig(ServiceType type) {
        return ServiceConfig.newBuilder().setServiceDescription(ServiceDescription.newBuilder().setType(type)).setBindingConfig(BindingConfig.newBuilder().setBindingId("SINACT").build()).build();
    }

    final SyncObject LOCK = new SyncObject("WaitForDeviceClassLock");
    final Observer notifyChangeObserver = new Observer() {

        @Override
        public void update(Observable source, Object data) throws Exception {
            synchronized (LOCK) {
                LOCK.notifyAll();
            }
        }
    };

    /**
     * Wait until the DeviceClassRemoteRegistry of the UnitRegistry contains a
     * DeviceClass.
     *
     * @param deviceClass the DeviceClass tested
     * @throws CouldNotPerformException
     */
    private void waitForDeviceClass(final DeviceClass deviceClass) throws CouldNotPerformException {
//        final SyncObject LOCK = new SyncObject("WaitForDeviceClassLock");
//        final Observer notifyChangeObserver = new Observer() {
//
//            @Override
//            public void update(Observable source, Object data) throws Exception {
//                synchronized (LOCK) {
//                    LOCK.notifyAll();
//                }
//            }
//        };
        synchronized (LOCK) {
//            unitRegistry.getDeviceRegistryRemote().addDataObserver(notifyChangeObserver);
            try {
                while (!unitRegistry.getDeviceRegistryRemote().containsDeviceClass(deviceClass)) {
                    LOCK.wait();
                }
//                System.out.println("Device class [" + deviceClass.getLabel() + "] registered in remote registry!");
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
        }
//        unitRegistry.getDeviceRegistryRemote().removeDataObserver(notifyChangeObserver);
    }
}
