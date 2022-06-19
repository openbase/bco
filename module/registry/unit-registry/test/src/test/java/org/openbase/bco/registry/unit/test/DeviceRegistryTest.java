package org.openbase.bco.registry.unit.test;

/*
 * #%L
 * BCO Registry Unit Test
 * %%
 * Copyright (C) 2014 - 2021 openbase.org
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

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.openbase.bco.registry.mock.MockRegistry;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.extension.type.processing.ScopeProcessor;
import org.openbase.jul.extension.type.processing.LabelProcessor;
import org.openbase.type.configuration.EntryType;
import org.openbase.type.configuration.MetaConfigType;
import org.openbase.type.domotic.binding.BindingConfigType.BindingConfig;
import org.openbase.type.domotic.service.ServiceConfigType.ServiceConfig;
import org.openbase.type.domotic.service.ServiceDescriptionType.ServiceDescription;
import org.openbase.type.domotic.service.ServiceTemplateConfigType.ServiceTemplateConfig;
import org.openbase.type.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import org.openbase.type.domotic.state.EnablingStateType.EnablingState;
import org.openbase.type.domotic.state.EnablingStateType.EnablingState.State;
import org.openbase.type.domotic.state.InventoryStateType.InventoryState;
import org.openbase.type.domotic.unit.UnitConfigType.UnitConfig;
import org.openbase.type.domotic.unit.UnitTemplateConfigType.UnitTemplateConfig;
import org.openbase.type.domotic.unit.UnitTemplateType.UnitTemplate;
import org.openbase.type.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;
import org.openbase.type.domotic.unit.device.DeviceClassType.DeviceClass;
import org.openbase.type.domotic.unit.device.DeviceConfigType.DeviceConfig;
import org.openbase.type.domotic.unit.user.UserConfigType.UserConfig;
import org.openbase.type.geometry.AxisAlignedBoundingBox3DFloatType.AxisAlignedBoundingBox3DFloat;
import org.openbase.type.geometry.TranslationType.Translation;
import org.openbase.type.math.Vec3DDoubleType.Vec3DDouble;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public class DeviceRegistryTest extends AbstractBCORegistryTest {

    /**
     * Test of registerUnitConfigWithUnits method, of class
     * DeviceRegistryImpl.
     * <p>
     * Test if the scope and the id of a device configuration and its units is
     * set when registered.
     *
     * @throws java.lang.Exception
     */
    @Test
    @Timeout(5)
    public void testRegisterUnitConfigWithUnits() throws Exception {
        System.out.println("testRegisterUnitConfigWithUnits");
        String productNumber = "ABCD-4321";
        String serialNumber = "1234-WXYZ";
        String company = "Fibaro";

        String deviceLabel = "TestSensor";

        // units are automatically added when a unit template config in the device class exists
        DeviceClass motionSensorClass = Registries.getClassRegistry().registerDeviceClass(generateDeviceClass("F_MotionSensor", productNumber, company, UnitType.BATTERY)).get();
        UnitConfig motionSensorConfig = Registries.getUnitRegistry().registerUnitConfig(generateDeviceUnitConfig(deviceLabel, serialNumber, motionSensorClass)).get();

        // lookup battery unit
        UnitConfig batteryUnitConfig = null;
        for (String unitId : motionSensorConfig.getDeviceConfig().getUnitIdList()) {
            final UnitConfig unitConfig = Registries.getUnitRegistry().getUnitConfigById(unitId);
            if (unitConfig.getUnitType() == UnitType.BATTERY) {
                batteryUnitConfig = unitConfig;
            }
        }

        String deviceScope = "/" + Registries.getUnitRegistry().getRootLocationConfig().getAlias(0).toLowerCase() + "/" + "device" + "/"+motionSensorConfig.getAlias(0).toLowerCase();
        String expectedUnitScope = "/" + Registries.getUnitRegistry().getRootLocationConfig().getAlias(0).toLowerCase() + "/" + UnitType.BATTERY.name().toLowerCase() + "/" + batteryUnitConfig.getAlias(0).toLowerCase();

        assertEquals(deviceScope, ScopeProcessor.generateStringRep(motionSensorConfig.getScope()), "Device scope is not set properly");
        assertEquals(1, motionSensorConfig.getDeviceConfig().getUnitIdCount(), "Device has not the correct number of units");

        UnitConfig batteryUnit = Registries.getUnitRegistry().getUnitConfigById(motionSensorConfig.getDeviceConfig().getUnitId(0));
        assertEquals(expectedUnitScope, ScopeProcessor.generateStringRep(batteryUnit.getScope()), "Unit scope is not set properly");
        assertEquals(motionSensorConfig.getId(), batteryUnit.getUnitHostId(), "Device id is not set in unit");
    }

    /**
     * Test of testRegiseredDeviceConfigWithoutLabel method, of class
     * DeviceRegistryImpl.
     *
     * @throws java.lang.Exception
     */
    @Test
    @Timeout(5)
    public void testRegisteredDeviceConfigWithoutLabel() throws Exception {
        System.out.println("testRegisteredDeviceConfigWithoutLabel");
        String productNumber = "KNHD-4321";
        String serialNumber = "112358";
        String company = "Company";

        DeviceClass deviceClass = Registries.getClassRegistry().registerDeviceClass(generateDeviceClass("WithoutLabel", productNumber, company)).get();
        UnitConfig deviceWithoutLabel = generateDeviceUnitConfig("", serialNumber, deviceClass).toBuilder().clearLabel().build();
        deviceWithoutLabel = Registries.getUnitRegistry().registerUnitConfig(deviceWithoutLabel).get();

        assertEquals(LabelProcessor.format(deviceClass.getCompany() + " " + LabelProcessor.getBestMatch(deviceClass.getLabel()) + " " + deviceWithoutLabel.getAlias(0)),
                LabelProcessor.getBestMatch(deviceWithoutLabel.getLabel()),
                "The device label is not set as the id if it is empty!");
    }

    @Test
    @Timeout(15)
    public void testUnitConfigUnitTemplateConsistencyHandler() throws Exception {
        System.out.println("testUnitConfigUnitTemplateConsistencyHandler");

        // clearing unit templates because they are already changed by the mock registry
        UnitTemplate unitTemplate = Registries.getTemplateRegistry().getUnitTemplateByType(UnitType.COLORABLE_LIGHT).toBuilder().clearServiceDescription().build();
        Registries.getTemplateRegistry().updateUnitTemplate(unitTemplate).get();

        ServiceDescription batteryTemplate = ServiceDescription.newBuilder().setServiceType(ServiceType.BATTERY_STATE_SERVICE).build();
        ServiceDescription colorTemplate = ServiceDescription.newBuilder().setServiceType(ServiceType.COLOR_STATE_SERVICE).build();
        unitTemplate = Registries.getTemplateRegistry().getUnitTemplateByType(UnitType.COLORABLE_LIGHT);
        unitTemplate = unitTemplate.toBuilder().addServiceDescription(batteryTemplate).addServiceDescription(colorTemplate).build();
        unitTemplate = Registries.getTemplateRegistry().updateUnitTemplate(unitTemplate).get();
        assertSame(unitTemplate.getServiceDescriptionList().get(0).getServiceType(), ServiceType.BATTERY_STATE_SERVICE);
        assertSame(unitTemplate.getServiceDescriptionList().get(1).getServiceType(), ServiceType.COLOR_STATE_SERVICE);
        assertSame(unitTemplate.getUnitType(), UnitType.COLORABLE_LIGHT);

        UnitTemplateConfig unitTemplateConfig = UnitTemplateConfig.newBuilder().setUnitType(unitTemplate.getUnitType()).build();
        String serialNumber1 = "5073";
        String deviceLabel = "thisIsARandomLabel12512";
        BindingConfig bindingConfig = BindingConfig.newBuilder().setBindingId("OPENHAB").build();
        DeviceClass clazz = Registries.getClassRegistry().registerDeviceClass(generateDeviceClass("unitTest", "423112358", "company").toBuilder().setBindingConfig(bindingConfig).addUnitTemplateConfig(unitTemplateConfig).build()).get();
        logger.info(Registries.getClassRegistry().getDeviceClassById(clazz.getId()).getUnitTemplateConfig(0).toString());
        logger.info(Registries.getTemplateRegistry().getUnitTemplateByType(UnitType.COLORABLE_LIGHT).toString());

        MetaConfigType.MetaConfig metaConfig = MetaConfigType.MetaConfig.newBuilder().addEntry(EntryType.Entry.newBuilder().setKey("testKey")).build();
        UnitConfig localDeviceConfig = generateDeviceUnitConfig(deviceLabel, serialNumber1, clazz);

        localDeviceConfig = Registries.getUnitRegistry().registerUnitConfig(localDeviceConfig).get();
        assertTrue(localDeviceConfig.getDeviceConfig().getUnitIdCount() == 1, "DeviceConfig does not contain the correct amount of units");

        UnitConfig registeredUnit = Registries.getUnitRegistry().getUnitConfigById(localDeviceConfig.getDeviceConfig().getUnitId(0));
        assertEquals(2, registeredUnit.getServiceConfigCount(), "The amount of service configs for the unit is not correct");
        assertTrue(registeredUnit.getServiceConfig(0).getServiceDescription().getServiceType() == ServiceType.BATTERY_STATE_SERVICE || registeredUnit.getServiceConfig(0).getServiceDescription().getServiceType() == ServiceType.COLOR_STATE_SERVICE);
        assertTrue(registeredUnit.getServiceConfig(1).getServiceDescription().getServiceType() == ServiceType.BATTERY_STATE_SERVICE || registeredUnit.getServiceConfig(1).getServiceDescription().getServiceType() == ServiceType.COLOR_STATE_SERVICE);

        ServiceConfig tmpServiceConfig;
        if (registeredUnit.getServiceConfig(0).getServiceDescription().getServiceType() == ServiceType.BATTERY_STATE_SERVICE) {
            tmpServiceConfig = registeredUnit.getServiceConfig(0);
            tmpServiceConfig = tmpServiceConfig.toBuilder().setMetaConfig(metaConfig).build();
            registeredUnit = Registries.getUnitRegistry().updateUnitConfig(registeredUnit.toBuilder().setServiceConfig(0, tmpServiceConfig).build()).get();
            assertEquals(metaConfig, registeredUnit.getServiceConfig(0).getMetaConfig());
        } else if (registeredUnit.getServiceConfig(1).getServiceDescription().getServiceType() == ServiceType.BATTERY_STATE_SERVICE) {
            tmpServiceConfig = registeredUnit.getServiceConfig(1);
            tmpServiceConfig = tmpServiceConfig.toBuilder().setMetaConfig(metaConfig).build();
            registeredUnit = Registries.getUnitRegistry().updateUnitConfig(registeredUnit.toBuilder().setServiceConfig(1, tmpServiceConfig).build()).get();
            assertEquals(metaConfig, registeredUnit.getServiceConfig(1).getMetaConfig());
        }
    }

    @Test
    @Timeout(10)
    public void testDeviceClassDeviceConfigUnitConsistencyHandler() throws Exception {
        System.out.println("testDeviceClassDeviceConfigUnitConsistencyHandler");

        // clearing unit templates because they are already changed by the mock registry
        UnitTemplate unitTemplate = Registries.getTemplateRegistry().getUnitTemplateByType(UnitType.LIGHT).toBuilder().clearServiceDescription().build();
        Registries.getTemplateRegistry().updateUnitTemplate(unitTemplate).get();
        unitTemplate = Registries.getTemplateRegistry().getUnitTemplateByType(UnitType.HANDLE).toBuilder().clearServiceDescription().build();
        Registries.getTemplateRegistry().updateUnitTemplate(unitTemplate).get();
        unitTemplate = Registries.getTemplateRegistry().getUnitTemplateByType(UnitType.BUTTON).toBuilder().clearServiceDescription().build();
        Registries.getTemplateRegistry().updateUnitTemplate(unitTemplate).get();

        ServiceTemplateConfig serviceTemplate1 = ServiceTemplateConfig.newBuilder().setServiceType(ServiceType.POWER_STATE_SERVICE).build();
        UnitTemplateConfig unitTemplateConfig1 = UnitTemplateConfig.newBuilder().setUnitType(UnitType.LIGHT).addServiceTemplateConfig(serviceTemplate1).build();
        ServiceTemplateConfig serviceTemplate2 = ServiceTemplateConfig.newBuilder().setServiceType(ServiceType.BATTERY_STATE_SERVICE).build();
        ServiceTemplateConfig serviceTemplate3 = ServiceTemplateConfig.newBuilder().setServiceType(ServiceType.HANDLE_STATE_SERVICE).build();
        UnitTemplateConfig unitTemplateConfig2 = UnitTemplateConfig.newBuilder().setUnitType(UnitType.HANDLE).addServiceTemplateConfig(serviceTemplate2).addServiceTemplateConfig(serviceTemplate3).build();

        unitTemplate = Registries.getTemplateRegistry().getUnitTemplateByType(UnitType.LIGHT).toBuilder().addServiceDescription(ServiceDescription.newBuilder().setServiceType(serviceTemplate1.getServiceType())).build();
        Registries.getTemplateRegistry().updateUnitTemplate(unitTemplate).get();
        unitTemplate = Registries.getTemplateRegistry().getUnitTemplateByType(UnitType.HANDLE).toBuilder().addServiceDescription(ServiceDescription.newBuilder().setServiceType(ServiceType.BATTERY_STATE_SERVICE)).addServiceDescription(ServiceDescription.newBuilder().setServiceType(ServiceType.HANDLE_STATE_SERVICE)).build();
        Registries.getTemplateRegistry().updateUnitTemplate(unitTemplate).get();

        BindingConfig bindingConfig = BindingConfig.newBuilder().setBindingId("OPENHAB").build();
        DeviceClass clazz = Registries.getClassRegistry().registerDeviceClass(generateDeviceClass("unitTemplateUnitConfigTest", "0149283794283", "company").toBuilder().addUnitTemplateConfig(unitTemplateConfig1).addUnitTemplateConfig(unitTemplateConfig2).setBindingConfig(bindingConfig).build()).get();
        assertEquals(2, clazz.getUnitTemplateConfigCount());
        List<UnitTemplateConfig> unitTemplateConfigList = Registries.getClassRegistry().getDeviceClassById(clazz.getId()).getUnitTemplateConfigList();
        logger.info(unitTemplateConfigList.get(0) + ", " + unitTemplateConfigList.get(1));

        UnitConfig config = Registries.getUnitRegistry().registerUnitConfig(generateDeviceUnitConfig("DeviceConfigWhereUnitsShallBeSetViaConsistency", "randomSerial14972", clazz)).get();
        assertEquals(clazz.getUnitTemplateConfigCount(), config.getDeviceConfig().getUnitIdCount(), "Units in device config were not set according to the device classes unit templates");
        boolean containsLight = false;
        boolean containsHandlseSensor = false;
        List<UnitConfig> dalUnitConfigs = new ArrayList<>();
        for (String unitId : config.getDeviceConfig().getUnitIdList()) {
            dalUnitConfigs.add(Registries.getUnitRegistry().getUnitConfigById(unitId));
        }
        for (UnitConfig unit : dalUnitConfigs) {
            if (unit.getUnitType().equals(unitTemplateConfig1.getUnitType())) {
                containsLight = true;
                assertEquals(unit.getServiceConfigCount(), unitTemplateConfig1.getServiceTemplateConfigCount(), "The light unit contains more or less services than the template config");
                assertTrue(unit.getServiceConfig(0).getServiceDescription().getServiceType().equals(serviceTemplate1.getServiceType()), "The service type of the light unit does not match");
            } else if (unit.getUnitType().equals(unitTemplateConfig2.getUnitType())) {
                containsHandlseSensor = true;
                assertEquals(unitTemplateConfig2.getServiceTemplateConfigCount(), unit.getServiceConfigCount(), "The handle sensor unit contains more or less services than the template config");
                assertEquals(unit.getServiceConfig(0).getServiceDescription().getServiceType(), serviceTemplate2.getServiceType(), "The service type of the handle sensor unit does not match");
                assertEquals(unit.getServiceConfig(1).getServiceDescription().getServiceType(), serviceTemplate3.getServiceType(), "The service type of the handle sensor unit does not match");
            }
        }
        assertTrue(containsLight, "The device config does not contain a light unit even though the device class has an according unit template");
        assertTrue(containsHandlseSensor, "The device config does not contain a handle sensor unit even though the device class has an according unit template");

        ServiceTemplateConfig serviceTemplate4 = ServiceTemplateConfig.newBuilder().setServiceType(ServiceType.BUTTON_STATE_SERVICE).build();
        UnitTemplateConfig unitTemplateConfig3 = UnitTemplateConfig.newBuilder().setUnitType(UnitType.BUTTON).addServiceTemplateConfig(serviceTemplate1).build();

        unitTemplate = Registries.getTemplateRegistry().getUnitTemplateByType(UnitType.BUTTON).toBuilder().addServiceDescription(ServiceDescription.newBuilder().setServiceType(ServiceType.BUTTON_STATE_SERVICE)).build();
        Registries.getTemplateRegistry().updateUnitTemplate(unitTemplate).get();

        System.out.println("Updating deviceClass...");
        clazz = Registries.getClassRegistry().updateDeviceClass(clazz.toBuilder().addUnitTemplateConfig(unitTemplateConfig3).build()).get();

        Registries.waitUntilReady();
        Registries.requestData();

        config = Registries.getUnitRegistry().getUnitConfigById(config.getId());


        assertEquals(config.getDeviceConfig().getUnitIdCount(), clazz.getUnitTemplateConfigCount(), "Unit configs and templates differ after the update of the device class");
        Registries.waitUntilReady();
        Registries.requestData();

        dalUnitConfigs.clear();
        for (String unitId : config.getDeviceConfig().getUnitIdList()) {
            dalUnitConfigs.add(Registries.getUnitRegistry().getUnitConfigById(unitId));
        }
        assertEquals(dalUnitConfigs.get(2).getUnitType(), unitTemplateConfig3.getUnitType(), "Device config does not contain the right unit config");
        assertEquals(dalUnitConfigs.get(2).getServiceConfig(0).getServiceDescription().getServiceType(), serviceTemplate4.getServiceType(), "Unit config does not contain the right service");

        int sizeBefore = Registries.getUnitRegistry().getDalUnitConfigs().size();
        UnitConfig.Builder configBuilder = config.toBuilder().clearLabel();
        LabelProcessor.addLabel(configBuilder.getLabelBuilder(), Locale.ENGLISH, "newDeviceLabel");
        config = Registries.getUnitRegistry().updateUnitConfig(configBuilder.build()).get();
        assertTrue(Registries.getUnitRegistry().getDalUnitConfigs().size() == sizeBefore, "More dal units registered after device renaming!");
        assertTrue(config.getDeviceConfig().getUnitIdCount() == 3, "More units in device config after renaming!");

        //test if dal units are also removed when a device is removed
        Registries.getUnitRegistry().removeUnitConfig(config).get();
        for (UnitConfig dalUnitConfig : dalUnitConfigs) {
            assertTrue(!Registries.getUnitRegistry().containsUnitConfig(dalUnitConfig), "DalUnit [" + dalUnitConfig.getLabel() + "] still registered even though its device has been removed!");
        }

    }

    @Test
    @Timeout(5)
    public void testBoundingBoxConsistencyHandler() throws Exception {

        // request a unit
        final UnitConfig.Builder testUnit = Registries.getUnitRegistry().getUnitConfigByAlias(MockRegistry.getUnitAlias(UnitType.POWER_SWITCH)).toBuilder();

        // setup shape
        final Vec3DDouble vector1 = Vec3DDouble.newBuilder().setX(0.1).setY(4.2).setZ(0.0).build();
        final Vec3DDouble vector2 = Vec3DDouble.newBuilder().setX(3.6).setY(0.0).setZ(0.0).build();
        final Vec3DDouble vector3 = Vec3DDouble.newBuilder().setX(5.1).setY(2.9).setZ(0.0).build();
        final Vec3DDouble vector4 = Vec3DDouble.newBuilder().setX(2.7).setY(2.1).setZ(0.0).build();
        final ArrayList<Vec3DDouble> values = new ArrayList<>(Arrays.asList(vector1, vector2, vector3, vector4));
        testUnit.getPlacementConfigBuilder().getShapeBuilder().addAllFloor(values);

        // update in registry
        final UnitConfig updatedUnit = Registries.getUnitRegistry().updateUnitConfig(testUnit.build()).get();

        // verify
        final AxisAlignedBoundingBox3DFloat.Builder builder = AxisAlignedBoundingBox3DFloat.newBuilder();
        builder.setDepth((float) (4.2));
        builder.setHeight((float) (0.0));
        builder.setWidth((float) (5.0));
        builder.setLeftFrontBottom(Translation.newBuilder().setX(0.1).setY(0).setZ(0));
        assertEquals(builder.build(), updatedUnit.getPlacementConfig().getShape().getBoundingBox(), "Bounding box has not been updated!");
    }

    /**
     * Test that the owner id for a device can be correctly set and that the owner id is cleared
     * when the user with that id is removed.
     *
     * @throws java.lang.Exception if anything fails
     */
    @Test
    @Timeout(15)
    public void testOwnerRemoval() throws Exception {
        System.out.println("testOwnerRemoval");

        // register a user
        UserConfig userConfig = UserConfig.newBuilder().setUserName("owner").setFirstName("Max").setLastName("Mustermann").build();
        UnitConfig.Builder userUnitConfig = UnitConfig.newBuilder().setUnitType(UnitType.USER).setUserConfig(userConfig).setEnablingState(EnablingState.newBuilder().setValue(EnablingState.State.ENABLED));
        userUnitConfig.getPermissionConfigBuilder().getOtherPermissionBuilder().setRead(true).setAccess(true).setWrite(true);
        UnitConfig owner = Registries.getUnitRegistry().registerUnitConfig(userUnitConfig.build()).get();

        // register a device class
        DeviceClass clazz = Registries.getClassRegistry().registerDeviceClass(generateDeviceClass("OwnerRemovalTest", "194872639127319823", "ServiceGMBH")).get();

        // register a device of the previously registered class with the previously registered owner
        UnitConfig.Builder deviceUnitConfigBuilder = generateDeviceUnitConfig("OwnerRemovalTestDevice", "1249726918723918723", clazz).toBuilder();
        deviceUnitConfigBuilder.getPermissionConfigBuilder().setOwnerId(owner.getId());
        UnitConfig deviceUnitConfig = Registries.getUnitRegistry().registerUnitConfig(deviceUnitConfigBuilder.build()).get();

        // test that owner id is still set after registration
        assertEquals(owner.getId(), deviceUnitConfig.getPermissionConfig().getOwnerId(), "The device does not have the correct owner id!");

        // remove the owner from the registry
        Registries.getUnitRegistry().removeUnitConfig(owner).get();

        // validate that owner got removed
        assertFalse(Registries.getUnitRegistry().containsUnitConfig(owner), "The owner did not get removed!");

        // validate that owner has been removed from device unit config
        deviceUnitConfig = Registries.getUnitRegistry().getUnitConfigById(deviceUnitConfig.getId());
        while (!deviceUnitConfig.getPermissionConfig().getOwnerId().isEmpty()) {
            Thread.sleep(100);
            deviceUnitConfig = Registries.getUnitRegistry().getUnitConfigById(deviceUnitConfig.getId());
        }
    }

    @Test
    @Timeout(15)
    public void testInventoryEnablingStateConnection() throws Exception {
        System.out.println("testInventoryEnablingStateConnection");
        ServiceTemplateConfig serviceTemplate1 = ServiceTemplateConfig.newBuilder().setServiceType(ServiceType.POWER_STATE_SERVICE).build();
        UnitTemplateConfig unitTemplateConfig1 = UnitTemplateConfig.newBuilder().setUnitType(UnitType.LIGHT).addServiceTemplateConfig(serviceTemplate1).build();
        UnitTemplate unitTemplate = Registries.getTemplateRegistry().getUnitTemplateByType(UnitType.LIGHT).toBuilder().addServiceDescription(ServiceDescription.newBuilder().setServiceType(serviceTemplate1.getServiceType())).build();
        Registries.getTemplateRegistry().updateUnitTemplate(unitTemplate).get();

        DeviceClass clazz = Registries.getClassRegistry().registerDeviceClass(generateDeviceClass("testInventoryEnablingStateConnection", "1297389612873619", "Inventory").toBuilder().addUnitTemplateConfig(unitTemplateConfig1).build()).get();

        String label = "testLabel";
        UnitConfig.Builder device = generateDeviceUnitConfig(label, "124972691872s3918723", clazz).toBuilder();
        DeviceConfig.Builder deviceConfig = device.getDeviceConfigBuilder();
        InventoryState.Builder inventoryState = deviceConfig.getInventoryStateBuilder();
        inventoryState.setValue(InventoryState.State.INSTALLED);

        device = Registries.getUnitRegistry().registerUnitConfig(device.build()).get().toBuilder();
        UnitConfig dalUnit = Registries.getUnitRegistry().getUnitConfigById(device.getDeviceConfig().getUnitId(0));
        assertSame(device.getEnablingState().getValue(), State.ENABLED, "DeviceUnitConfig is not Enabled");
        assertSame(device.getEnablingState().getValue(), dalUnit.getEnablingState().getValue(), "DalUnitConfig is not Enabled");
        assertEquals(device.getLabel(), dalUnit.getLabel(), "DeviceUnitConfig and DalUnitConfig have different labels");

        label = label + "-2";
        UnitConfig.Builder deviceBuilder = device.clearLabel();
        LabelProcessor.addLabel(deviceBuilder.getLabelBuilder(), Locale.ENGLISH, label);
        device = Registries.getUnitRegistry().updateUnitConfig(deviceBuilder.build()).get().toBuilder();
        dalUnit = Registries.getUnitRegistry().getUnitConfigById(device.getDeviceConfig().getUnitId(0));
        assertEquals(LabelProcessor.getBestMatch(device.getLabel()), LabelProcessor.getBestMatch(dalUnit.getLabel()));

        deviceConfig = device.getDeviceConfigBuilder();
        inventoryState = deviceConfig.getInventoryStateBuilder();
        inventoryState.setValue(InventoryState.State.IN_STOCK);
        device = Registries.getUnitRegistry().updateUnitConfig(device.build()).get().toBuilder();
        dalUnit = Registries.getUnitRegistry().getUnitConfigById(device.getDeviceConfig().getUnitId(0));
        assertSame(device.getDeviceConfig().getInventoryState().getValue(), InventoryState.State.IN_STOCK, "DeviceUnitConfig inventory state is not IN_STOCK");
        assertSame(device.getEnablingState().getValue(), State.DISABLED, "DeviceUnitConfig is not Disabled");
        assertSame(device.getEnablingState().getValue(), dalUnit.getEnablingState().getValue(), "DalUnitConfig is not Disabled");

        deviceConfig = device.getDeviceConfigBuilder();
        inventoryState = deviceConfig.getInventoryStateBuilder();
        inventoryState.setValue(InventoryState.State.INSTALLED);
        device = Registries.getUnitRegistry().updateUnitConfig(device.build()).get().toBuilder();
        dalUnit = Registries.getUnitRegistry().getUnitConfigById(device.getDeviceConfig().getUnitId(0));
        assertSame(device.getDeviceConfig().getInventoryState().getValue(), InventoryState.State.INSTALLED, "DeviceUnitConfig inventory state is not INSTALLED");
        assertSame(device.getEnablingState().getValue(), State.ENABLED, "DeviceUnitConfig is not Enabled");
        assertSame(device.getEnablingState().getValue(), dalUnit.getEnablingState().getValue(), "DalUnitConfig is not Enabled");

        unitTemplate = Registries.getTemplateRegistry().getUnitTemplateByType(UnitType.LIGHT).toBuilder().clearServiceDescription().build();
        Registries.getTemplateRegistry().updateUnitTemplate(unitTemplate).get();
    }

    /**
     * Test if the owner of a device is updated correctly.
     *
     * @throws java.lang.Exception
     */
    @Test
    @Timeout(5)
    public void testLocationIdInInventoryState() throws Exception {
        System.out.println("testLocationIdInInventoryState");
        DeviceClass clazz = Registries.getClassRegistry().registerDeviceClass(generateDeviceClass("testLocationIdInInventoryState", "103721ggbdk12", "ServiceGMBH")).get();

        UnitConfig testLocationIdInInventoryStateDevice = generateDeviceUnitConfig("testLocationIdInInventoryStateDevice", "103721ggbdk12", clazz);
        DeviceConfig tmp = testLocationIdInInventoryStateDevice.getDeviceConfig().toBuilder().setInventoryState(InventoryState.newBuilder().setValue(InventoryState.State.INSTALLED)).build();
        testLocationIdInInventoryStateDevice = testLocationIdInInventoryStateDevice.toBuilder().setDeviceConfig(tmp).build();
        testLocationIdInInventoryStateDevice = Registries.getUnitRegistry().registerUnitConfig(testLocationIdInInventoryStateDevice).get();

        assertEquals(Registries.getUnitRegistry().getRootLocationConfig().getId(), testLocationIdInInventoryStateDevice.getDeviceConfig().getInventoryState().getLocationId(), "The location id in the inventory state has not been set for an installed device!");
    }

    /**
     * Test if when breaking an existing device the sandbox registers it and
     * does not modify the real registry.
     *
     * @throws java.lang.Exception
     */
    @Test
    @Timeout(5)
    public void testRegistrationErrorHandling() throws Exception {
        System.out.println("testRegistrationErrorHandling");

        // register a device class
        DeviceClass clazz = Registries.getClassRegistry().registerDeviceClass(generateDeviceClass("testRegistrationErrorHandling", "asdgsdr131423", "ServiceGMBH")).get();

        final String label = "testRegistrationErrorHandlingDevice";
        // register a device
        UnitConfig.Builder deviceUnitConfig = generateDeviceUnitConfig(label, "asdfdsgaer3", clazz).toBuilder();
        deviceUnitConfig.getDeviceConfigBuilder().setInventoryState(InventoryState.newBuilder().setValue(InventoryState.State.INSTALLED));
        Registries.getUnitRegistry().registerUnitConfig(deviceUnitConfig.build()).get();

        // register a second device with a different label
        deviceUnitConfig.clearLabel();
        LabelProcessor.addLabel(deviceUnitConfig.getLabelBuilder(), Locale.ENGLISH, "secondLabel");
        UnitConfig newDeviceUnitConfig = Registries.getUnitRegistry().registerUnitConfig(deviceUnitConfig.build()).get();

        try {
            ExceptionPrinter.setBeQuit(Boolean.TRUE);
            Registries.getUnitRegistry().registerUnitConfig(newDeviceUnitConfig.toBuilder().clearId().build()).get();
            fail("No exception thrown after updating a device with to have the same label as another");
        } catch (Exception ex) {
            assertEquals(newDeviceUnitConfig, Registries.getUnitRegistry().getUnitConfigById(newDeviceUnitConfig.getId()), "DeviceConfig has been changed event though the update has been rejected");
        } finally {
            ExceptionPrinter.setBeQuit(Boolean.FALSE);
        }
    }
}
