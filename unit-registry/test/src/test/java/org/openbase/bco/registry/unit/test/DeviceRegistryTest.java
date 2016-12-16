package org.openbase.bco.registry.unit.test;

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
import java.io.IOException;
import java.util.ArrayList;
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
import org.openbase.bco.registry.agent.core.AgentRegistryController;
import org.openbase.bco.registry.app.core.AppRegistryController;
import org.openbase.bco.registry.device.core.DeviceRegistryController;
import org.openbase.bco.registry.device.remote.DeviceRegistryRemote;
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
import rst.domotic.service.ServiceTemplateConfigType.ServiceTemplateConfig;
import rst.domotic.service.ServiceTemplateType.ServiceTemplate;
import rst.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import rst.domotic.state.EnablingStateType.EnablingState;
import rst.domotic.state.InventoryStateType.InventoryState;
import rst.domotic.unit.UnitConfigType.UnitConfig;
import rst.domotic.unit.UnitTemplateConfigType.UnitTemplateConfig;
import rst.domotic.unit.UnitTemplateType.UnitTemplate;
import rst.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;
import rst.domotic.unit.device.DeviceClassType.DeviceClass;
import rst.domotic.unit.device.DeviceConfigType.DeviceConfig;
import rst.domotic.unit.user.UserConfigType.UserConfig;
import rst.geometry.PoseType;
import rst.geometry.RotationType;
import rst.geometry.TranslationType;
import rst.rsb.ScopeType;
import rst.spatial.PlacementConfigType.PlacementConfig;

/**
 *
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public class DeviceRegistryTest {

    private static final Logger logger = LoggerFactory.getLogger(DeviceRegistryTest.class);

    public static final String LOCATION_LABEL = "paradise";
    public static UnitConfig LOCATION;

    private static DeviceRegistryController deviceRegistry;
    private static UnitRegistryController unitRegistry;
    private static AppRegistryController appRegistry;
    private static AgentRegistryController agentRegistry;

    private static DeviceClass.Builder deviceClass;
    private static DeviceConfig.Builder deviceConfig;
    private static UnitConfig.Builder deviceUnitConfig;

//    private static DeviceRegistryRemote deviceRegistryRemote;
    @BeforeClass
    public static void setUpClass() throws InstantiationException, InitializationException, IOException, InvalidStateException, JPServiceException, InterruptedException, CouldNotPerformException, ExecutionException {
        JPService.setupJUnitTestMode();

        deviceRegistry = new DeviceRegistryController();
        unitRegistry = new UnitRegistryController();
        appRegistry = new AppRegistryController();
        agentRegistry = new AgentRegistryController();

        deviceRegistry.init();
        unitRegistry.init();
        appRegistry.init();
        agentRegistry.init();

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

        Thread unitRegistryThread = new Thread(new Runnable() {

            @Override
            public void run() {
                try {
                    unitRegistry.activate();
                } catch (CouldNotPerformException | InterruptedException ex) {
                    ExceptionPrinter.printHistory(ex, logger);
                }
            }
        });

        Thread appRegistryThread = new Thread(new Runnable() {

            @Override
            public void run() {
                try {
                    appRegistry.activate();
                } catch (CouldNotPerformException | InterruptedException ex) {
                    ExceptionPrinter.printHistory(ex, logger);
                }
            }
        });

        Thread agentRegistryThread = new Thread(new Runnable() {

            @Override
            public void run() {
                try {
                    agentRegistry.activate();
                } catch (CouldNotPerformException | InterruptedException ex) {
                    ExceptionPrinter.printHistory(ex, logger);
                }
            }
        });

        deviceRegistryThread.start();
        unitRegistryThread.start();
        appRegistryThread.start();
        agentRegistryThread.start();

        deviceRegistryThread.join();
        unitRegistryThread.join();
        appRegistryThread.join();
        agentRegistryThread.join();

        deviceClass = DeviceClass.getDefaultInstance().newBuilderForType();
        deviceClass.setLabel("TestDeviceClassLabel");
        deviceClass.setCompany("MyCom");
        deviceClass.setProductNumber("TDCL-001");
        deviceConfig = DeviceConfig.getDefaultInstance().newBuilderForType();
        deviceConfig.setSerialNumber("0001-0004-2245");
        deviceConfig.setDeviceClassId("TestDeviceClassLabel");
        deviceUnitConfig = UnitConfig.newBuilder().setLabel("TestDeviceConfigLabel").setType(UnitType.DEVICE).setDeviceConfig(deviceConfig);

        LOCATION = unitRegistry.registerUnitConfig(UnitConfig.newBuilder().setLabel(LOCATION_LABEL).setType(UnitType.LOCATION).build()).get();
    }

    @AfterClass
    public static void tearDownClass() {
        if (unitRegistry != null) {
            unitRegistry.shutdown();
        }
        if (deviceRegistry != null) {
            deviceRegistry.shutdown();
        }
        if (appRegistry != null) {
            appRegistry.shutdown();
        }
        if (agentRegistry != null) {
            agentRegistry.shutdown();
        }
    }

    @Before
    public void setUp() throws CouldNotPerformException {
        unitRegistry.getDalUnitConfigRegistry().clear();
        unitRegistry.getDeviceUnitConfigRegistry().clear();
        deviceRegistry.getDeviceClassRegistry().clear();
    }

    @After
    public void tearDown() {
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

        String deviceLabel = "TestSensor";
        String deviceScope = "/" + LOCATION_LABEL + "/" + "device" + "/" + deviceLabel.toLowerCase() + "/";

        String expectedUnitScope = "/" + LOCATION_LABEL + "/" + UnitType.BATTERY.name().toLowerCase() + "/" + deviceLabel.toLowerCase() + "/";

        // units are automatically added when a unit template config in the device class exists
        UnitTemplateConfig unitTemplateConfig = UnitTemplateConfig.newBuilder().setType(UnitType.BATTERY).build();
        DeviceClass motionSensorClass = deviceRegistry.registerDeviceClass(getDeviceClass("F_MotionSensor", productNumber, company).toBuilder().addUnitTemplateConfig(unitTemplateConfig).build()).get();
        waitForDeviceClass(motionSensorClass);
        UnitConfig motionSensorConfig = getDeviceUnitConfig(deviceLabel, serialNumber, motionSensorClass);
        motionSensorConfig = deviceRegistry.registerDeviceConfig(motionSensorConfig).get();
        System.out.println("Config:" + motionSensorConfig);

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
        String productNumber = "KNHD-4321";
        String serialNumber = "112358";
        String company = "Company";

        DeviceClass clazz = deviceRegistry.registerDeviceClass(getDeviceClass("WithoutLabel", productNumber, company)).get();
        waitForDeviceClass(clazz);
        UnitConfig deviceWithoutLabel = getDeviceUnitConfig("", serialNumber, clazz);
        deviceWithoutLabel = deviceRegistry.registerDeviceConfig(deviceWithoutLabel).get();

        assertEquals("The device label is not set as the id if it is empty!", deviceWithoutLabel.getId(), deviceWithoutLabel.getLabel());
    }

    /**
     * Test of testRegisterTwoDevicesWithSameLabel method, of class
     * DeviceRegistryImpl.
     */
    // TODO: fix that the consisteny handling will work after this
    @Test(timeout = 5000)
    public void testRegisterTwoDevicesWithSameLabel() throws Exception {
        String serialNumber1 = "FIRST_DEV";
        String serialNumber2 = "BAD_DEV";
        String deviceLabel = "SameLabelSameLocation";

        DeviceClass clazz = deviceRegistry.registerDeviceClass(getDeviceClass("WithoutLabel", "xyz", "HuxGMBH")).get();
        waitForDeviceClass(clazz);
        UnitConfig deviceWithLabel1 = getDeviceUnitConfig(deviceLabel, serialNumber1, clazz);
        UnitConfig deviceWithLabel2 = getDeviceUnitConfig(deviceLabel, serialNumber2, clazz);

        deviceRegistry.registerDeviceConfig(deviceWithLabel1).get();
        try {
            ExceptionPrinter.setBeQuit(Boolean.TRUE);
            deviceRegistry.registerDeviceConfig(deviceWithLabel2).get();
            fail("There was no exception thrown even though two devices with the same label [" + deviceLabel + "] where registered in the same location [" + LOCATION_LABEL + "]");
        } catch (CouldNotPerformException | InterruptedException | ExecutionException ex) {
            assertTrue(true);
        } finally {
            ExceptionPrinter.setBeQuit(Boolean.FALSE);
        }
    }

    @Test(timeout = 5000)
    public void testUnitConfigUnitTemplateConsistencyHandler() throws Exception {
        ServiceTemplate batteryTemplate = ServiceTemplate.newBuilder().setType(ServiceType.BATTERY_STATE_SERVICE).build();
        ServiceTemplate colorTemplate = ServiceTemplate.newBuilder().setType(ServiceType.COLOR_STATE_SERVICE).build();
        UnitTemplate unitTemplate = unitRegistry.getUnitTemplateByType(UnitType.COLORABLE_LIGHT);
        unitTemplate = unitTemplate.toBuilder().addServiceTemplate(batteryTemplate).addServiceTemplate(colorTemplate).build();
        unitTemplate = unitRegistry.updateUnitTemplate(unitTemplate).get();
        assertTrue(unitTemplate.getServiceTemplateList().contains(batteryTemplate));
        assertTrue(unitTemplate.getServiceTemplateList().contains(colorTemplate));
        assertTrue(unitTemplate.getType() == UnitType.COLORABLE_LIGHT);

        UnitTemplateConfig unitTemplateConfig = UnitTemplateConfig.newBuilder().setType(unitTemplate.getType()).build();
        String serialNumber1 = "5073";
        String deviceLabel = "thisIsARandomLabel12512";
        BindingConfig bindingConfig = BindingConfig.newBuilder().setBindingId("OPENHAB").build();
        DeviceClass clazz = deviceRegistry.registerDeviceClass(getDeviceClass("unitTest", "423112358", "company").toBuilder().setBindingConfig(bindingConfig).addUnitTemplateConfig(unitTemplateConfig).build()).get();
        waitForDeviceClass(clazz);

        MetaConfigType.MetaConfig metaConfig = MetaConfigType.MetaConfig.newBuilder().addEntry(EntryType.Entry.newBuilder().setKey("testKey")).build();
        UnitConfig localDeviceConfig = getDeviceUnitConfig(deviceLabel, serialNumber1, clazz);

        localDeviceConfig = deviceRegistry.registerDeviceConfig(localDeviceConfig).get();
        assertTrue(localDeviceConfig.getDeviceConfig().getUnitIdCount() == 1);

        UnitConfig registeredUnit = unitRegistry.getUnitConfigById(localDeviceConfig.getDeviceConfig().getUnitId(0));
        assertTrue(registeredUnit.getServiceConfigCount() == 2);
        assertTrue(registeredUnit.getServiceConfig(0).getServiceTemplate().getType() == ServiceType.BATTERY_STATE_SERVICE || registeredUnit.getServiceConfig(0).getServiceTemplate().getType() == ServiceType.COLOR_STATE_SERVICE);
        assertTrue(registeredUnit.getServiceConfig(1).getServiceTemplate().getType() == ServiceType.BATTERY_STATE_SERVICE || registeredUnit.getServiceConfig(1).getServiceTemplate().getType() == ServiceType.COLOR_STATE_SERVICE);

        ServiceConfig tmpServiceConfig;
        if (registeredUnit.getServiceConfig(0).getServiceTemplate().getType() == ServiceType.BATTERY_STATE_SERVICE) {
            tmpServiceConfig = registeredUnit.getServiceConfig(0);
            tmpServiceConfig = tmpServiceConfig.toBuilder().setMetaConfig(metaConfig).build();
            registeredUnit = unitRegistry.updateUnitConfig(registeredUnit.toBuilder().setServiceConfig(0, tmpServiceConfig).build()).get();
            assertEquals(metaConfig, registeredUnit.getServiceConfig(0).getMetaConfig());
        } else if (registeredUnit.getServiceConfig(1).getServiceTemplate().getType() == ServiceType.BATTERY_STATE_SERVICE) {
            tmpServiceConfig = registeredUnit.getServiceConfig(1);
            tmpServiceConfig = tmpServiceConfig.toBuilder().setMetaConfig(metaConfig).build();
            registeredUnit = unitRegistry.updateUnitConfig(registeredUnit.toBuilder().setServiceConfig(1, tmpServiceConfig).build()).get();
            assertEquals(metaConfig, registeredUnit.getServiceConfig(1).getMetaConfig());
        }
    }

    @Test//(timeout = 5000)
    public void testDeviceClassDeviceConfigUnitConsistencyHandler() throws Exception {
        ServiceTemplateConfig serviceTemplate1 = ServiceTemplateConfig.newBuilder().setServiceType(ServiceType.POWER_STATE_SERVICE).build();
        UnitTemplateConfig unitTemplateConfig1 = UnitTemplateConfig.newBuilder().setType(UnitType.LIGHT).addServiceTemplateConfig(serviceTemplate1).build();
        ServiceTemplateConfig serviceTemplate2 = ServiceTemplateConfig.newBuilder().setServiceType(ServiceType.BATTERY_STATE_SERVICE).build();
        ServiceTemplateConfig serviceTemplate3 = ServiceTemplateConfig.newBuilder().setServiceType(ServiceType.HANDLE_STATE_SERVICE).build();
        UnitTemplateConfig unitTemplateConfig2 = UnitTemplateConfig.newBuilder().setType(UnitType.HANDLE).addServiceTemplateConfig(serviceTemplate2).addServiceTemplateConfig(serviceTemplate3).build();

        UnitTemplate unitTemplate = unitRegistry.getUnitTemplateByType(UnitType.LIGHT).toBuilder().addServiceTemplate(ServiceTemplate.newBuilder().setType(serviceTemplate1.getServiceType())).build();
        unitRegistry.updateUnitTemplate(unitTemplate).get();
        unitTemplate = unitRegistry.getUnitTemplateByType(UnitType.HANDLE).toBuilder().addServiceTemplate(ServiceTemplate.newBuilder().setType(ServiceType.BATTERY_STATE_SERVICE)).addServiceTemplate(ServiceTemplate.newBuilder().setType(ServiceType.HANDLE_STATE_SERVICE)).build();
        unitRegistry.updateUnitTemplate(unitTemplate).get();

        BindingConfig bindingConfig = BindingConfig.newBuilder().setBindingId("OPENHAB").build();
        DeviceClass clazz = deviceRegistry.registerDeviceClass(getDeviceClass("unittemplateUnitConfigTest", "0149283794283", "company").toBuilder().addUnitTemplateConfig(unitTemplateConfig1).addUnitTemplateConfig(unitTemplateConfig2).setBindingConfig(bindingConfig).build()).get();
        assertTrue(clazz.getUnitTemplateConfigCount() == 2);
        waitForDeviceClass(clazz);

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
                assertTrue("The service type of the light unit does not match", unit.getServiceConfig(0).getServiceTemplate().getType().equals(serviceTemplate1.getServiceType()));
            } else if (unit.getType().equals(unitTemplateConfig2.getType())) {
                containsHandlseSensor = true;
                assertTrue("The handle sensor unit contains more or less services than the template config", unit.getServiceConfigCount() == unitTemplateConfig2.getServiceTemplateConfigCount());
                assertEquals("The service type of the handle sensor unit does not match", unit.getServiceConfig(0).getServiceTemplate().getType(), serviceTemplate2.getServiceType());
                assertEquals("The service type of the handle sensor unit does not match", unit.getServiceConfig(1).getServiceTemplate().getType(), serviceTemplate3.getServiceType());
            }
        }
        assertTrue("The device config does not contain a light unit even though the device class has an according unit template", containsLight);
        assertTrue("The device config does not contain a handle sensor unit even though the device class has an according unit template", containsHandlseSensor);

        ServiceTemplateConfig serviceTemplate4 = ServiceTemplateConfig.newBuilder().setServiceType(ServiceType.BUTTON_STATE_SERVICE).build();
        UnitTemplateConfig unitTemplateConfig3 = UnitTemplateConfig.newBuilder().setType(UnitType.BUTTON).addServiceTemplateConfig(serviceTemplate1).build();

        unitTemplate = unitRegistry.getUnitTemplateByType(UnitType.BUTTON).toBuilder().addServiceTemplate(ServiceTemplate.newBuilder().setType(ServiceType.BUTTON_STATE_SERVICE)).build();
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
        assertEquals("Unit config does not contain the right service", dalUnitConfigs.get(2).getServiceConfig(0).getServiceTemplate().getType(), serviceTemplate4.getServiceType());

        config = deviceRegistry.updateDeviceConfig(config.toBuilder().setLabel("newDeviceLabel").build()).get();
        assertTrue("More dal units registered after device renaming!", unitRegistry.getDalUnitConfigs().size() == 3);
        assertTrue("More units in device config after renaming!", config.getDeviceConfig().getUnitIdCount() == 3);

        //test if dal units are also removed when a device is removed
        deviceRegistry.removeDeviceConfig(config).get();
        for (UnitConfig dalUnitConfig : dalUnitConfigs) {
            assertTrue("DalUnit [" + dalUnitConfig.getLabel() + "] still registered even though its device has been removed!", !unitRegistry.containsUnitConfig(dalUnitConfig));
        }

        // clearing unit templates because of effects on other tests it might have
        unitTemplate = unitRegistry.getUnitTemplateByType(UnitType.LIGHT).toBuilder().clearServiceTemplate().build();
        unitRegistry.updateUnitTemplate(unitTemplate);
        unitTemplate = unitRegistry.getUnitTemplateByType(UnitType.HANDLE).toBuilder().clearServiceTemplate().build();
        unitRegistry.updateUnitTemplate(unitTemplate);
        unitTemplate = unitRegistry.getUnitTemplateByType(UnitType.BUTTON).toBuilder().clearServiceTemplate().build();
        unitRegistry.updateUnitTemplate(unitTemplate).get();
    }

    @Test(timeout = 5000)
    public void testBoundToDeviceConsistencyHandler() throws Exception {
        ServiceTemplateConfig serviceTemplate1 = ServiceTemplateConfig.newBuilder().setServiceType(ServiceType.POWER_STATE_SERVICE).build();
        UnitTemplateConfig unitTemplateConfig1 = UnitTemplateConfig.newBuilder().setType(UnitType.LIGHT).addServiceTemplateConfig(serviceTemplate1).build();
        UnitTemplate unitTemplate = unitRegistry.getUnitTemplateByType(UnitType.LIGHT).toBuilder().addServiceTemplate(ServiceTemplate.newBuilder().setType(serviceTemplate1.getServiceType())).build();
        unitRegistry.updateUnitTemplate(unitTemplate).get();

        BindingConfig bindingConfig = BindingConfig.newBuilder().setBindingId("OPENHAB").build();
        DeviceClass clazz = deviceRegistry.registerDeviceClass(getDeviceClass("BoundToDeviceTest", "boundToDevicePNR", "company").toBuilder().addUnitTemplateConfig(unitTemplateConfig1).setBindingConfig(bindingConfig).build()).get();
        waitForDeviceClass(clazz);

        UnitConfig config = deviceRegistry.registerDeviceConfig(getDeviceUnitConfig("BoundToDeviceTestDevice", "boundToDeviceSNR", clazz)).get();
        assertTrue("Unit config has not been added to device config", config.getDeviceConfig().getUnitIdCount() == 1);
        List<UnitConfig> dalUnitConfig = new ArrayList<>();
        for (String unitId : config.getDeviceConfig().getUnitIdList()) {
            dalUnitConfig.add(unitRegistry.getUnitConfigById(unitId));
        }
        assertTrue("Unit config has not been set as bound to device", dalUnitConfig.get(0).getBoundToUnitHost());
        assertTrue("Placement config of unit and device do not match although unit is bound to device", dalUnitConfig.get(0).getPlacementConfig().equals(config.getPlacementConfig()));

        UnitConfig testLocation = unitRegistry.registerUnitConfig(UnitConfig.newBuilder().setLabel("BoundToDeviceTestLocation").setType(UnitType.LOCATION).build()).get();
        PlacementConfig placement = dalUnitConfig.get(0).getPlacementConfig().toBuilder().setLocationId(testLocation.getId()).build();
        UnitConfig unit = dalUnitConfig.get(0).toBuilder().setPlacementConfig(placement).build();

        unit = unitRegistry.updateUnitConfig(unit).get();
        assertTrue("Unit is not bound to device anymore", unit.getBoundToUnitHost());
        assertTrue("Placement config of unit and device do not match although unit is bound to device", unit.getPlacementConfig().equals(config.getPlacementConfig()));
        assertEquals("Location id in placement config of unit does not equals that in device", config.getPlacementConfig().getLocationId(), unit.getPlacementConfig().getLocationId());

        unitRegistry.removeUnitConfig(testLocation).get();
        unitTemplate = unitRegistry.getUnitTemplateByType(UnitType.LIGHT).toBuilder().clearServiceTemplate().build();
        unitRegistry.updateUnitTemplate(unitTemplate).get();
    }

    /**
     * Test if the owner of a device is updated correctly.
     *
     * @throws java.lang.Exception
     */
    @Test(timeout = 5000)
    public void testOwnerRemoval() throws Exception {
        UserConfig userConfig = UserConfig.newBuilder().setUserName("owner").setFirstName("Max").setLastName("Mustermann").build();
        UnitConfig owner = unitRegistry.registerUnitConfig(UnitConfig.newBuilder().setType(UnitType.USER).setUserConfig(userConfig).setEnablingState(EnablingState.newBuilder().setValue(EnablingState.State.ENABLED)).build()).get();

        DeviceClass clazz = deviceRegistry.registerDeviceClass(getDeviceClass("OwnerRemovalTest", "194872639127319823", "ServiceGMBH")).get();
        waitForDeviceClass(clazz);

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

    /**
     * Test if the owner of a device is updated correctly.
     *
     * @throws java.lang.Exception
     */
    @Test(timeout = 5000)
    public void testLocationIdInInventoryState() throws Exception {
        DeviceClass clazz = deviceRegistry.registerDeviceClass(getDeviceClass("testLocationIdInInventoryState", "103721ggbdk12", "ServiceGMBH")).get();
        waitForDeviceClass(clazz);

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
        ScopeType.Scope.Builder locationScope = ScopeType.Scope.newBuilder().addComponent(LOCATION_LABEL);
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
        return ServiceConfig.newBuilder().setServiceTemplate(ServiceTemplate.newBuilder().setType(type)).setBindingConfig(BindingConfig.newBuilder().setBindingId("SINACT").build()).build();
    }

    /**
     * Wait until the DeviceClassRemoteEegistry of the UnitRegistry contains a DeviceClass.
     *
     * @param deviceClass the DeviceClass tested
     * @throws CouldNotPerformException
     */
    private void waitForDeviceClass(final DeviceClass deviceClass) throws CouldNotPerformException {
        DeviceRegistryRemote deviceRegistryRemote = unitRegistry.getDeviceRegistryRemote();
        final SyncObject LOCK = new SyncObject("WaitForDeviceClassLock");
        final Observer notifyChangeObserver = new Observer() {

            @Override
            public void update(Observable source, Object data) throws Exception {
                synchronized (LOCK) {
                    LOCK.notifyAll();
                }
            }
        };
        synchronized (LOCK) {
            deviceRegistryRemote.addDataObserver(notifyChangeObserver);
            try {
                while (!deviceRegistryRemote.containsDeviceClass(deviceClass)) {
                    LOCK.wait();
                }
                System.out.println("Device class [" + deviceClass.getLabel() + "] registered in remote registry!");
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
        }
        deviceRegistryRemote.removeDataObserver(notifyChangeObserver);
    }
}
