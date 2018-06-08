package org.openbase.bco.registry.unit.test;

/*
 * #%L
 * BCO Registry Unit Test
 * %%
 * Copyright (C) 2014 - 2018 openbase.org
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

import org.junit.Test;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.extension.rsb.scope.ScopeGenerator;
import org.openbase.jul.extension.rst.processing.LabelProcessor;
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
import rst.domotic.unit.user.UserConfigType.UserConfig;
import rst.geometry.AxisAlignedBoundingBox3DFloatType;
import rst.geometry.TranslationType;
import rst.math.Vec3DDoubleType;
import rst.math.Vec3DDoubleType.Vec3DDouble;
import rst.spatial.ShapeType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

import static junit.framework.TestCase.*;

/**
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public class DeviceRegistryTest extends AbstractBCORegistryTest {

    @Test
    public void integerTest() {
        System.out.println("Max int+1: " + (Integer.MAX_VALUE + 1));
    }

    /**
     * Test of registerDeviceConfigWithUnits method, of class
     * DeviceRegistryImpl.
     * <p>
     * Test if the scope and the id of a device configuration and its units is
     * set when registered.
     *
     * @throws java.lang.Exception
     */
    @Test(timeout = 5000)
    public void testRegisterDeviceConfigWithUnits() throws Exception {
        System.out.println("testRegisterDeviceConfigWithUnits");
        String productNumber = "ABCD-4321";
        String serialNumber = "1234-WXYZ";
        String company = "Fibaro";

        String deviceLabel = "TestSensor";
        String deviceScope = "/" + LabelProcessor.getFirstLabel(unitRegistry.getRootLocationConfig().getLabel()).toLowerCase() + "/" + "device" + "/" + deviceLabel.toLowerCase() + "/";
        String expectedUnitScope = "/" + LabelProcessor.getFirstLabel(unitRegistry.getRootLocationConfig().getLabel()).toLowerCase() + "/" + UnitType.BATTERY.name().toLowerCase() + "/" + deviceLabel.toLowerCase() + "/";

        // units are automatically added when a unit template config in the device class exists
        DeviceClass motionSensorClass = registerDeviceClass(generateDeviceClass("F_MotionSensor", productNumber, company, UnitType.BATTERY));
        UnitConfig motionSensorConfig = unitRegistry.registerUnitConfig(generateDeviceUnitConfig(deviceLabel, serialNumber, motionSensorClass)).get();

        assertEquals("Device scope is not set properly", deviceScope, ScopeGenerator.generateStringRep(motionSensorConfig.getScope()));
        assertEquals("Device has not the correct number of units", 1, motionSensorConfig.getDeviceConfig().getUnitIdCount());

        UnitConfig batteryUnit = unitRegistry.getUnitConfigById(motionSensorConfig.getDeviceConfig().getUnitId(0));
        assertEquals("Unit scope is not set properly", expectedUnitScope, ScopeGenerator.generateStringRep(batteryUnit.getScope()));
        assertEquals("Device id is not set in unit", motionSensorConfig.getId(), batteryUnit.getUnitHostId());
    }

    /**
     * Test of testRegiseredDeviceConfigWithoutLabel method, of class
     * DeviceRegistryImpl.
     *
     * @throws java.lang.Exception
     */
    @Test(timeout = 5000)
    public void testRegisteredDeviceConfigWithoutLabel() throws Exception {
        System.out.println("testRegisteredDeviceConfigWithoutLabel");
        String productNumber = "KNHD-4321";
        String serialNumber = "112358";
        String company = "Company";

        DeviceClass deviceClass = registerDeviceClass(generateDeviceClass("WithoutLabel", productNumber, company));
        UnitConfig deviceWithoutLabel = generateDeviceUnitConfig("", serialNumber, deviceClass).toBuilder().clearLabel().build();
        deviceWithoutLabel = unitRegistry.registerUnitConfig(deviceWithoutLabel).get();

        assertEquals("The device label is not set as the id if it is empty!",
                deviceClass.getCompany() + " " +
                        LabelProcessor.getFirstLabel(deviceClass.getLabel()) + " " +
                        deviceWithoutLabel.getAlias(0),
                LabelProcessor.getFirstLabel(deviceWithoutLabel.getLabel()));
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

        DeviceClass clazz = registerDeviceClass(generateDeviceClass("WithoutLabel", "xyz", "HuxGMBH"));
        UnitConfig deviceWithLabel1 = generateDeviceUnitConfig(deviceLabel, serialNumber1, clazz);
        UnitConfig deviceWithLabel2 = generateDeviceUnitConfig(deviceLabel, serialNumber2, clazz);

        unitRegistry.registerUnitConfig(deviceWithLabel1).get();
        try {
            ExceptionPrinter.setBeQuit(Boolean.TRUE);
            unitRegistry.registerUnitConfig(deviceWithLabel2).get();
            fail("There was no exception thrown even though two devices with the same label [" + deviceLabel + "] where registered in the same location");
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
        UnitTemplate unitTemplate = templateRegistry.getUnitTemplateByType(UnitType.COLORABLE_LIGHT).toBuilder().clearServiceDescription().build();
        templateRegistry.updateUnitTemplate(unitTemplate).get();

        ServiceDescription batteryTemplate = ServiceDescription.newBuilder().setServiceType(ServiceType.BATTERY_STATE_SERVICE).build();
        ServiceDescription colorTemplate = ServiceDescription.newBuilder().setServiceType(ServiceType.COLOR_STATE_SERVICE).build();
        unitTemplate = templateRegistry.getUnitTemplateByType(UnitType.COLORABLE_LIGHT);
        unitTemplate = unitTemplate.toBuilder().addServiceDescription(batteryTemplate).addServiceDescription(colorTemplate).build();
        unitTemplate = templateRegistry.updateUnitTemplate(unitTemplate).get();
        assertTrue(unitTemplate.getServiceDescriptionList().get(0).getServiceType() == ServiceType.BATTERY_STATE_SERVICE);
        assertTrue(unitTemplate.getServiceDescriptionList().get(1).getServiceType() == ServiceType.COLOR_STATE_SERVICE);
        assertTrue(unitTemplate.getType() == UnitType.COLORABLE_LIGHT);

        UnitTemplateConfig unitTemplateConfig = UnitTemplateConfig.newBuilder().setType(unitTemplate.getType()).build();
        String serialNumber1 = "5073";
        String deviceLabel = "thisIsARandomLabel12512";
        BindingConfig bindingConfig = BindingConfig.newBuilder().setBindingId("OPENHAB").build();
        Registries.getClassRegistry().addDataObserver(notifyChangeObserver);
        DeviceClass clazz = classRegistry.registerDeviceClass(generateDeviceClass("unitTest", "423112358", "company").toBuilder().setBindingConfig(bindingConfig).addUnitTemplateConfig(unitTemplateConfig).build()).get();
        waitForDeviceClass(clazz);
        Registries.getClassRegistry().removeDataObserver(notifyChangeObserver);

        MetaConfigType.MetaConfig metaConfig = MetaConfigType.MetaConfig.newBuilder().addEntry(EntryType.Entry.newBuilder().setKey("testKey")).build();
        UnitConfig localDeviceConfig = generateDeviceUnitConfig(deviceLabel, serialNumber1, clazz);

        localDeviceConfig = unitRegistry.registerUnitConfig(localDeviceConfig).get();
        assertTrue("DeviceConfig does not contain the correct amount of units", localDeviceConfig.getDeviceConfig().getUnitIdCount() == 1);

        UnitConfig registeredUnit = unitRegistry.getUnitConfigById(localDeviceConfig.getDeviceConfig().getUnitId(0));
        assertEquals("The amount of service configs for the unit is not correct", 2, registeredUnit.getServiceConfigCount());
        assertTrue(registeredUnit.getServiceConfig(0).getServiceDescription().getServiceType() == ServiceType.BATTERY_STATE_SERVICE || registeredUnit.getServiceConfig(0).getServiceDescription().getServiceType() == ServiceType.COLOR_STATE_SERVICE);
        assertTrue(registeredUnit.getServiceConfig(1).getServiceDescription().getServiceType() == ServiceType.BATTERY_STATE_SERVICE || registeredUnit.getServiceConfig(1).getServiceDescription().getServiceType() == ServiceType.COLOR_STATE_SERVICE);

        ServiceConfig tmpServiceConfig;
        if (registeredUnit.getServiceConfig(0).getServiceDescription().getServiceType() == ServiceType.BATTERY_STATE_SERVICE) {
            tmpServiceConfig = registeredUnit.getServiceConfig(0);
            tmpServiceConfig = tmpServiceConfig.toBuilder().setMetaConfig(metaConfig).build();
            registeredUnit = unitRegistry.updateUnitConfig(registeredUnit.toBuilder().setServiceConfig(0, tmpServiceConfig).build()).get();
            assertEquals(metaConfig, registeredUnit.getServiceConfig(0).getMetaConfig());
        } else if (registeredUnit.getServiceConfig(1).getServiceDescription().getServiceType() == ServiceType.BATTERY_STATE_SERVICE) {
            tmpServiceConfig = registeredUnit.getServiceConfig(1);
            tmpServiceConfig = tmpServiceConfig.toBuilder().setMetaConfig(metaConfig).build();
            registeredUnit = unitRegistry.updateUnitConfig(registeredUnit.toBuilder().setServiceConfig(1, tmpServiceConfig).build()).get();
            assertEquals(metaConfig, registeredUnit.getServiceConfig(1).getMetaConfig());
        }
    }

    @Test(timeout = 10000)
    public void testDeviceClassDeviceConfigUnitConsistencyHandler() throws Exception {
        System.out.println("testDeviceClassDeviceConfigUnitConsistencyHandler");

        // clearing unit templates because they are already changed by the mock registry
        UnitTemplate unitTemplate = templateRegistry.getUnitTemplateByType(UnitType.LIGHT).toBuilder().clearServiceDescription().build();
        templateRegistry.updateUnitTemplate(unitTemplate).get();
        unitTemplate = templateRegistry.getUnitTemplateByType(UnitType.HANDLE).toBuilder().clearServiceDescription().build();
        templateRegistry.updateUnitTemplate(unitTemplate).get();
        unitTemplate = templateRegistry.getUnitTemplateByType(UnitType.BUTTON).toBuilder().clearServiceDescription().build();
        templateRegistry.updateUnitTemplate(unitTemplate).get();

        ServiceTemplateConfig serviceTemplate1 = ServiceTemplateConfig.newBuilder().setServiceType(ServiceType.POWER_STATE_SERVICE).build();
        UnitTemplateConfig unitTemplateConfig1 = UnitTemplateConfig.newBuilder().setType(UnitType.LIGHT).addServiceTemplateConfig(serviceTemplate1).build();
        ServiceTemplateConfig serviceTemplate2 = ServiceTemplateConfig.newBuilder().setServiceType(ServiceType.BATTERY_STATE_SERVICE).build();
        ServiceTemplateConfig serviceTemplate3 = ServiceTemplateConfig.newBuilder().setServiceType(ServiceType.HANDLE_STATE_SERVICE).build();
        UnitTemplateConfig unitTemplateConfig2 = UnitTemplateConfig.newBuilder().setType(UnitType.HANDLE).addServiceTemplateConfig(serviceTemplate2).addServiceTemplateConfig(serviceTemplate3).build();

        unitTemplate = templateRegistry.getUnitTemplateByType(UnitType.LIGHT).toBuilder().addServiceDescription(ServiceDescription.newBuilder().setServiceType(serviceTemplate1.getServiceType())).build();
        templateRegistry.updateUnitTemplate(unitTemplate).get();
        unitTemplate = templateRegistry.getUnitTemplateByType(UnitType.HANDLE).toBuilder().addServiceDescription(ServiceDescription.newBuilder().setServiceType(ServiceType.BATTERY_STATE_SERVICE)).addServiceDescription(ServiceDescription.newBuilder().setServiceType(ServiceType.HANDLE_STATE_SERVICE)).build();
        templateRegistry.updateUnitTemplate(unitTemplate).get();

        Registries.getClassRegistry().addDataObserver(notifyChangeObserver);
        BindingConfig bindingConfig = BindingConfig.newBuilder().setBindingId("OPENHAB").build();
        DeviceClass clazz = classRegistry.registerDeviceClass(generateDeviceClass("unitTemplateUnitConfigTest", "0149283794283", "company").toBuilder().addUnitTemplateConfig(unitTemplateConfig1).addUnitTemplateConfig(unitTemplateConfig2).setBindingConfig(bindingConfig).build()).get();
        assertEquals(2, clazz.getUnitTemplateConfigCount());
        waitForDeviceClass(clazz);
        Registries.getClassRegistry().removeDataObserver(notifyChangeObserver);

        UnitConfig config = unitRegistry.registerUnitConfig(generateDeviceUnitConfig("DeviceConfigWhereUnitsShallBeSetViaConsistency", "randomSerial14972", clazz)).get();
        assertEquals("Units in device config were not set according to the device classes unit templates", clazz.getUnitTemplateConfigCount(), config.getDeviceConfig().getUnitIdCount());
        boolean containsLight = false;
        boolean containsHandlseSensor = false;
        List<UnitConfig> dalUnitConfigs = new ArrayList<>();
        for (String unitId : config.getDeviceConfig().getUnitIdList()) {
            dalUnitConfigs.add(unitRegistry.getUnitConfigById(unitId));
        }
        for (UnitConfig unit : dalUnitConfigs) {
            if (unit.getUnitType().equals(unitTemplateConfig1.getType())) {
                containsLight = true;
                assertEquals("The light unit contains more or less services than the template config", unit.getServiceConfigCount(), unitTemplateConfig1.getServiceTemplateConfigCount());
                assertTrue("The service type of the light unit does not match", unit.getServiceConfig(0).getServiceDescription().getServiceType().equals(serviceTemplate1.getServiceType()));
            } else if (unit.getUnitType().equals(unitTemplateConfig2.getType())) {
                containsHandlseSensor = true;
                assertEquals("The handle sensor unit contains more or less services than the template config", unitTemplateConfig2.getServiceTemplateConfigCount(), unit.getServiceConfigCount());
                assertEquals("The service type of the handle sensor unit does not match", unit.getServiceConfig(0).getServiceDescription().getServiceType(), serviceTemplate2.getServiceType());
                assertEquals("The service type of the handle sensor unit does not match", unit.getServiceConfig(1).getServiceDescription().getServiceType(), serviceTemplate3.getServiceType());
            }
        }
        assertTrue("The device config does not contain a light unit even though the device class has an according unit template", containsLight);
        assertTrue("The device config does not contain a handle sensor unit even though the device class has an according unit template", containsHandlseSensor);

        ServiceTemplateConfig serviceTemplate4 = ServiceTemplateConfig.newBuilder().setServiceType(ServiceType.BUTTON_STATE_SERVICE).build();
        UnitTemplateConfig unitTemplateConfig3 = UnitTemplateConfig.newBuilder().setType(UnitType.BUTTON).addServiceTemplateConfig(serviceTemplate1).build();

        unitTemplate = templateRegistry.getUnitTemplateByType(UnitType.BUTTON).toBuilder().addServiceDescription(ServiceDescription.newBuilder().setServiceType(ServiceType.BUTTON_STATE_SERVICE)).build();
        templateRegistry.updateUnitTemplate(unitTemplate).get();

        System.out.println("Updating deviceClass...");
        clazz = classRegistry.updateDeviceClass(clazz.toBuilder().addUnitTemplateConfig(unitTemplateConfig3).build()).get();
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
        assertEquals("Device config does not contain the right unit config", dalUnitConfigs.get(2).getUnitType(), unitTemplateConfig3.getType());
        assertEquals("Unit config does not contain the right service", dalUnitConfigs.get(2).getServiceConfig(0).getServiceDescription().getServiceType(), serviceTemplate4.getServiceType());

        int sizeBefore = unitRegistry.getDalUnitConfigs().size();
        UnitConfig.Builder configBuilder = config.toBuilder().clearLabel();
        LabelProcessor.addLabel(configBuilder.getLabelBuilder(), Locale.ENGLISH, "newDeviceLabel");
        config = unitRegistry.updateUnitConfig(configBuilder.build()).get();
        assertTrue("More dal units registered after device renaming!", unitRegistry.getDalUnitConfigs().size() == sizeBefore);
        assertTrue("More units in device config after renaming!", config.getDeviceConfig().getUnitIdCount() == 3);

        //test if dal units are also removed when a device is removed
        unitRegistry.removeUnitConfig(config).get();
        for (UnitConfig dalUnitConfig : dalUnitConfigs) {
            assertTrue("DalUnit [" + dalUnitConfig.getLabel() + "] still registered even though its device has been removed!", !unitRegistry.containsUnitConfig(dalUnitConfig));
        }

    }

    @Test(timeout = 5000)
    public void testBoundingBoxConsistencyHandler() throws Exception {
        // how to get a test unit?
        UnitConfig testUnit = UnitConfig.newBuilder().setUnitType(UnitTemplate.UnitType.COLORABLE_LIGHT).build();

        // UnitConfig testUnit = classRegistry.getDeviceConfigById("PH_Hue_E27_Device");
        TranslationType.Translation.Builder translationBuilder = TranslationType.Translation.newBuilder().setX(0).setY(0).setZ(0);
        Vec3DDoubleType.Vec3DDouble vector1 = Vec3DDoubleType.Vec3DDouble.newBuilder().setX(0.1).setY(4.2).setZ(0.0).build();
        Vec3DDoubleType.Vec3DDouble vector2 = Vec3DDoubleType.Vec3DDouble.newBuilder().setX(3.6).setY(0.0).setZ(0.0).build();
        Vec3DDoubleType.Vec3DDouble vector3 = Vec3DDoubleType.Vec3DDouble.newBuilder().setX(5.1).setY(2.9).setZ(0.0).build();
        Vec3DDoubleType.Vec3DDouble vector4 = Vec3DDoubleType.Vec3DDouble.newBuilder().setX(2.7).setY(2.1).setZ(0.0).build();
        ArrayList<Vec3DDouble> values = new ArrayList<>(Arrays.asList(vector1, vector2, vector3, vector4));
        ShapeType.Shape shapeBuilder = ShapeType.Shape.newBuilder().addAllFloor(values).build();
        testUnit.toBuilder().getPlacementConfigBuilder().setShape(shapeBuilder);

        //classRegistry.updateDeviceConfig(testUnit).get();
        ShapeType.Shape shape = testUnit.getPlacementConfig().getShape();

        AxisAlignedBoundingBox3DFloatType.AxisAlignedBoundingBox3DFloat.Builder builder = AxisAlignedBoundingBox3DFloatType.AxisAlignedBoundingBox3DFloat.newBuilder();
        builder.setDepth((float) (4.2));
        builder.setHeight((float) (0.0));
        builder.setWidth((float) (5.0));
        TranslationType.Translation.Builder lfbBuilder = TranslationType.Translation.newBuilder().setX(0.1).setY(0.0).setZ(0.0);
        builder.setLeftFrontBottom(translationBuilder);
        //assertTrue(shape.getBoundingBox().equals(builder.build()));
    }

    /**
     * Test that the owner id for a device can be correctly set and that the owner id is cleared
     * when the user with that id is removed.
     *
     * @throws java.lang.Exception if anything fails
     */
    @Test(timeout = 5000)
    public void testOwnerRemoval() throws Exception {
        System.out.println("testOwnerRemoval");

        // register a user
        UserConfig userConfig = UserConfig.newBuilder().setUserName("owner").setFirstName("Max").setLastName("Mustermann").build();
        UnitConfig.Builder userUnitConfig = UnitConfig.newBuilder().setUnitType(UnitType.USER).setUserConfig(userConfig).setEnablingState(EnablingState.newBuilder().setValue(EnablingState.State.ENABLED));
        userUnitConfig.getPermissionConfigBuilder().getOtherPermissionBuilder().setRead(true).setAccess(true).setWrite(true);
        UnitConfig owner = unitRegistry.registerUnitConfig(userUnitConfig.build()).get();

        // register a device class
        Registries.getClassRegistry().addDataObserver(notifyChangeObserver);
        DeviceClass clazz = classRegistry.registerDeviceClass(generateDeviceClass("OwnerRemovalTest", "194872639127319823", "ServiceGMBH")).get();
        waitForDeviceClass(clazz);
        Registries.getClassRegistry().removeDataObserver(notifyChangeObserver);

        // register a device of the previously registered class with the previously registered owner
        UnitConfig.Builder deviceUnitConfigBuilder = generateDeviceUnitConfig("OwnerRemovalTestDevice", "1249726918723918723", clazz).toBuilder();
        deviceUnitConfigBuilder.getPermissionConfigBuilder().setOwnerId(owner.getId());
        UnitConfig deviceUnitConfig = unitRegistry.registerUnitConfig(deviceUnitConfigBuilder.build()).get();

        // test that owner id is still set after registration

        logger.warn("Before: " + deviceUnitConfigBuilder.build().getPermissionConfig());
        logger.warn("After: " + deviceUnitConfig.getPermissionConfig());
        assertEquals("The device does not have the correct owner id!", owner.getId(), deviceUnitConfig.getPermissionConfig().getOwnerId());

        // remove the owner from the registry
        unitRegistry.removeUnitConfig(owner).get();

        // validate that owner got removed
        assertTrue("The owner did not get removed!", !unitRegistry.containsUnitConfig(owner));

        // validate that owner has been removed from device unit config
        deviceUnitConfig = unitRegistry.getUnitConfigById(deviceUnitConfig.getId());
        while (!deviceUnitConfig.getPermissionConfig().getOwnerId().isEmpty()) {
            Thread.sleep(100);
            deviceUnitConfig = unitRegistry.getUnitConfigById(deviceUnitConfig.getId());
        }
    }

    @Test(timeout = 5000)
    public void testInventoryEnablingStateConnection() throws Exception {
        System.out.println("testInventoryEnablingStateConnection");
        ServiceTemplateConfig serviceTemplate1 = ServiceTemplateConfig.newBuilder().setServiceType(ServiceType.POWER_STATE_SERVICE).build();
        UnitTemplateConfig unitTemplateConfig1 = UnitTemplateConfig.newBuilder().setType(UnitType.LIGHT).addServiceTemplateConfig(serviceTemplate1).build();
        UnitTemplate unitTemplate = templateRegistry.getUnitTemplateByType(UnitType.LIGHT).toBuilder().addServiceDescription(ServiceDescription.newBuilder().setServiceType(serviceTemplate1.getServiceType())).build();
        templateRegistry.updateUnitTemplate(unitTemplate).get();

        Registries.getClassRegistry().addDataObserver(notifyChangeObserver);
        DeviceClass clazz = classRegistry.registerDeviceClass(generateDeviceClass("testInventoryEnablingStateConnection", "1297389612873619", "Inventory").toBuilder().addUnitTemplateConfig(unitTemplateConfig1).build()).get();
        waitForDeviceClass(clazz);
        Registries.getClassRegistry().removeDataObserver(notifyChangeObserver);

        String label = "testLabel";
        UnitConfig.Builder device = generateDeviceUnitConfig(label, "124972691872s3918723", clazz).toBuilder();
        DeviceConfig.Builder deviceConfig = device.getDeviceConfigBuilder();
        InventoryState.Builder inventoryState = deviceConfig.getInventoryStateBuilder();
        inventoryState.setValue(InventoryState.State.INSTALLED);

        device = unitRegistry.registerUnitConfig(device.build()).get().toBuilder();
        UnitConfig dalUnit = unitRegistry.getUnitConfigById(device.getDeviceConfig().getUnitId(0));
        assertTrue("DeviceUnitConfig is not Enabled", device.getEnablingState().getValue() == EnablingState.State.ENABLED);
        assertTrue("DalUnitConfig is not Enabled", device.getEnablingState().getValue() == dalUnit.getEnablingState().getValue());
        assertEquals("DeviceUnitConfig and DalUnitConfig have different labels", device.getLabel(), dalUnit.getLabel());

        label = label + "-2";
        UnitConfig.Builder deviceBuilder = device.clearLabel();
        LabelProcessor.addLabel(deviceBuilder.getLabelBuilder(), Locale.ENGLISH, label);
        device = unitRegistry.updateUnitConfig(deviceBuilder.build()).get().toBuilder();
        dalUnit = unitRegistry.getUnitConfigById(device.getDeviceConfig().getUnitId(0));
        assertEquals(LabelProcessor.getFirstLabel(device.getLabel()), LabelProcessor.getFirstLabel(dalUnit.getLabel()));

        deviceConfig = device.getDeviceConfigBuilder();
        inventoryState = deviceConfig.getInventoryStateBuilder();
        inventoryState.setValue(InventoryState.State.IN_STOCK);
        device = unitRegistry.updateUnitConfig(device.build()).get().toBuilder();
        dalUnit = unitRegistry.getUnitConfigById(device.getDeviceConfig().getUnitId(0));
        assertTrue("DeviceUnitConfig inventory state is not IN_STOCK", device.getDeviceConfig().getInventoryState().getValue() == InventoryState.State.IN_STOCK);
        assertTrue("DeviceUnitConfig is not Disabled", device.getEnablingState().getValue() == EnablingState.State.DISABLED);
        assertTrue("DalUnitConfig is not Disabled", device.getEnablingState().getValue() == dalUnit.getEnablingState().getValue());

        deviceConfig = device.getDeviceConfigBuilder();
        inventoryState = deviceConfig.getInventoryStateBuilder();
        inventoryState.setValue(InventoryState.State.INSTALLED);
        device = unitRegistry.updateUnitConfig(device.build()).get().toBuilder();
        dalUnit = unitRegistry.getUnitConfigById(device.getDeviceConfig().getUnitId(0));
        assertTrue("DeviceUnitConfig inventory state is not INSTALLED", device.getDeviceConfig().getInventoryState().getValue() == InventoryState.State.INSTALLED);
        assertTrue("DeviceUnitConfig is not Enabled", device.getEnablingState().getValue() == EnablingState.State.ENABLED);
        assertTrue("DalUnitConfig is not Enabled", device.getEnablingState().getValue() == dalUnit.getEnablingState().getValue());

        unitTemplate = templateRegistry.getUnitTemplateByType(UnitType.LIGHT).toBuilder().clearServiceDescription().build();
        templateRegistry.updateUnitTemplate(unitTemplate).get();
    }

    /**
     * Test if the owner of a device is updated correctly.
     *
     * @throws java.lang.Exception
     */
    @Test(timeout = 5000)
    public void testLocationIdInInventoryState() throws Exception {
        System.out.println("testLocationIdInInventoryState");
        Registries.getClassRegistry().addDataObserver(notifyChangeObserver);
        DeviceClass clazz = classRegistry.registerDeviceClass(generateDeviceClass("testLocationIdInInventoryState", "103721ggbdk12", "ServiceGMBH")).get();
        waitForDeviceClass(clazz);
        Registries.getClassRegistry().removeDataObserver(notifyChangeObserver);

        UnitConfig testLocationIdInInventoryStateDevice = generateDeviceUnitConfig("testLocationIdInInventoryStateDevice", "103721ggbdk12", clazz);
        DeviceConfig tmp = testLocationIdInInventoryStateDevice.getDeviceConfig().toBuilder().setInventoryState(InventoryState.newBuilder().setValue(InventoryState.State.INSTALLED)).build();
        testLocationIdInInventoryStateDevice = testLocationIdInInventoryStateDevice.toBuilder().setDeviceConfig(tmp).build();
        testLocationIdInInventoryStateDevice = unitRegistry.registerUnitConfig(testLocationIdInInventoryStateDevice).get();

        assertEquals("The location id in the inventory state has not been set for an installed device!", unitRegistry.getRootLocationConfig().getId(), testLocationIdInInventoryStateDevice.getDeviceConfig().getInventoryState().getLocationId());
    }

    /**
     * Test if when breaking an existing device the sandbox registers it and
     * does not modify the real registry.
     *
     * @throws java.lang.Exception
     */
    @Test(timeout = 5000)
    public void testRegistrationErrorHandling() throws Exception {
        System.out.println("testRegistrationErrorHandling");

        // register a device class
        Registries.getClassRegistry().addDataObserver(notifyChangeObserver);
        DeviceClass clazz = classRegistry.registerDeviceClass(generateDeviceClass("testRegistrationErrorHandling", "asdgsdr131423", "ServiceGMBH")).get();
        waitForDeviceClass(clazz);
        Registries.getClassRegistry().removeDataObserver(notifyChangeObserver);

        final String label = "testRegistrationErrorHandlingDevice";
        // register a device
        UnitConfig.Builder deviceUnitConfig = generateDeviceUnitConfig(label, "asdfdsgaer3", clazz).toBuilder();
        deviceUnitConfig.getDeviceConfigBuilder().setInventoryState(InventoryState.newBuilder().setValue(InventoryState.State.INSTALLED));
        unitRegistry.registerUnitConfig(deviceUnitConfig.build()).get();

        // register a second device with a different label
        deviceUnitConfig.clearLabel();
        LabelProcessor.addLabel(deviceUnitConfig.getLabelBuilder(), Locale.ENGLISH, "secondLabel");
        UnitConfig newDeviceUnitConfig = unitRegistry.registerUnitConfig(deviceUnitConfig.build()).get();

        try {
            ExceptionPrinter.setBeQuit(Boolean.TRUE);
            unitRegistry.registerUnitConfig(newDeviceUnitConfig.toBuilder().clearId().build()).get();
            fail("No exception thrown after updating a device with to have the same label as another");
        } catch (Exception ex) {
            assertEquals("DeviceConfig has been changed event though the update has been rejected", newDeviceUnitConfig, unitRegistry.getUnitConfigById(newDeviceUnitConfig.getId()));
        } finally {
            ExceptionPrinter.setBeQuit(Boolean.FALSE);
        }
    }
}
