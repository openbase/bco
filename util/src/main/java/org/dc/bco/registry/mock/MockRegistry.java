package org.dc.bco.registry.mock;

/*
 * #%L
 * REM Utility
 * %%
 * Copyright (C) 2014 - 2016 DivineCooperation
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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.dc.bco.registry.agent.core.AgentRegistryLauncher;
import org.dc.bco.registry.agent.lib.AgentRegistry;
import org.dc.bco.registry.app.core.AppRegistryLauncher;
import org.dc.bco.registry.app.lib.AppRegistry;
import org.dc.bco.registry.device.core.DeviceRegistryLauncher;
import org.dc.bco.registry.device.lib.DeviceRegistry;
import org.dc.bco.registry.device.remote.CachedDeviceRegistryRemote;
import org.dc.bco.registry.location.core.LocationRegistryLauncher;
import org.dc.bco.registry.location.lib.LocationRegistry;
import org.dc.bco.registry.location.remote.CachedLocationRegistryRemote;
import org.dc.bco.registry.scene.core.SceneRegistryLauncher;
import org.dc.bco.registry.scene.lib.SceneRegistry;
import org.dc.bco.registry.user.core.UserRegistryLauncher;
import org.dc.bco.registry.user.lib.UserRegistry;
import org.dc.jps.core.JPService;
import org.dc.jul.exception.CouldNotPerformException;
import org.dc.jul.exception.InstantiationException;
import org.dc.jul.exception.printer.ExceptionPrinter;
import org.dc.jul.schedule.GlobalExecuterService;
import org.slf4j.LoggerFactory;
import rst.authorization.UserConfigType.UserConfig;
import rst.geometry.PoseType.Pose;
import rst.geometry.RotationType.Rotation;
import rst.geometry.TranslationType.Translation;
import rst.homeautomation.binding.BindingConfigType.BindingConfig;
import rst.homeautomation.binding.BindingTypeHolderType;
import rst.homeautomation.device.DeviceClassType.DeviceClass;
import rst.homeautomation.device.DeviceConfigType.DeviceConfig;
import rst.homeautomation.service.BindingServiceConfigType;
import rst.homeautomation.service.ServiceConfigType;
import rst.homeautomation.service.ServiceConfigType.ServiceConfig;
import rst.homeautomation.service.ServiceTemplateType.ServiceTemplate;
import rst.homeautomation.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import rst.homeautomation.state.EnablingStateType.EnablingState;
import rst.homeautomation.state.InventoryStateType;
import rst.homeautomation.unit.UnitConfigType.UnitConfig;
import rst.homeautomation.unit.UnitTemplateConfigType.UnitTemplateConfig;
import rst.homeautomation.unit.UnitTemplateType.UnitTemplate;
import rst.homeautomation.unit.UnitTemplateType.UnitTemplate.UnitType;
import rst.spatial.LocationConfigType.LocationConfig;
import rst.spatial.PlacementConfigType.PlacementConfig;

/**
 *
 * @author thuxohl
 */
public class MockRegistry {

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(MockRegistry.class);

    public static final String USER_NAME = "uSeRnAmE";
    public static UserConfig testUser;

    public static final String AMBIENT_LIGHT_LABEL = "Ambient_Light_Unit_Test";
    public static final String BATTERY_LABEL = "Battery_Unit_Test";
    public static final String BRIGHTNESS_SENSOR_LABEL = "Brightness_Sensor_Unit_Test";
    public static final String BUTTON_LABEL = "Button_Unit_Test";
    public static final String DIMMER_LABEL = "Dimmer_Unit_Test";
    public static final String HANDLE_SENSOR_LABEL = "Handle_Sensor_Unit_Test";
    public static final String LIGHT_LABEL = "Light_Unit_Test";
    public static final String MOTION_SENSOR_LABEL = "Motion_Sensor_Unit_Test";
    public static final String POWER_CONSUMPTION_LABEL = "Power_Consumption_Sensor_Unit_Test";
    public static final String POWER_PLUG_LABEL = "Power_Plug_Unit_Test";
    public static final String REED_SWITCH_LABEL = "Reed_Switch_Unit_Test";
    public static final String ROLLERSHUTTER_LABEL = "Rollershutter_Unit_Test";
    public static final String TAMPER_SWITCH_LABEL = "Tamper_Switch_Unit_Test";
    public static final String TEMPERATURE_SENSOR_LABEL = "Temperature_Sensor_Unit_Test";
    public static final String TEMPERATURE_CONTROLLER_LABEL = "Temperature_Controller_Unit_Test";
    public static final String SMOKE_DETECTOR_LABEL = "Smoke_Detector_Unit_Test";
    private final String serialNumber = "1234-5678-9100";

    private static DeviceRegistryLauncher deviceRegistryLauncher;
    private static LocationRegistryLauncher locationRegistryLauncher;
    private static AgentRegistryLauncher agentRegistryLauncher;
    private static AppRegistryLauncher appRegistryLauncher;
    private static SceneRegistryLauncher sceneRegistryLauncher;
    private static UserRegistryLauncher userRegistryLauncher;

    private static DeviceRegistry deviceRegistry;
    private static LocationRegistry locationRegistry;
    private static AgentRegistry agentRegistry;
    private static AppRegistry appRegistry;
    private static SceneRegistry sceneRegistry;
    private static UserRegistry userRegisty;

//    private final DeviceRegistryRemote deviceRegistry;
//    private final LocationRegistryRemote locationRemote;
//    private final UserRegistryRemote userRemote;
    private static LocationConfig paradise;

    public enum MockUnitTemplate {

        AMBIENT_LIGHT(UnitType.AMBIENT_LIGHT, ServiceType.COLOR_SERVICE, ServiceType.POWER_SERVICE, ServiceType.BRIGHTNESS_SERVICE),
        LIGHT(UnitType.LIGHT, ServiceType.POWER_SERVICE),
        MOTION_SENSOR(UnitType.MOTION_SENSOR, ServiceType.MOTION_PROVIDER),
        BRIGHTNESS_SENSOR(UnitType.BRIGHTNESS_SENSOR, ServiceType.BRIGHTNESS_PROVIDER),
        BUTTON(UnitType.BUTTON, ServiceType.BUTTON_PROVIDER),
        DIMMER(UnitType.DIMMER, ServiceType.BRIGHTNESS_SERVICE, ServiceType.POWER_SERVICE),
        HANDLE_SENSOR(UnitType.HANDLE_SENSOR, ServiceType.HANDLE_PROVIDER),
        POWER_CONSUMPTION_SENSOR(UnitType.POWER_CONSUMPTION_SENSOR, ServiceType.POWER_CONSUMPTION_PROVIDER),
        POWER_PLUG(UnitType.POWER_PLUG, ServiceType.POWER_SERVICE),
        REED_SWITCH(UnitType.REED_SWITCH, ServiceType.REED_SWITCH_PROVIDER),
        ROLLERSHUTTER(UnitType.ROLLERSHUTTER, ServiceType.SHUTTER_SERVICE, ServiceType.OPENING_RATIO_SERVICE),
        TAMPER_SWITCH(UnitType.TAMPER_SWITCH, ServiceType.TAMPER_PROVIDER),
        TEMPERATURE_CONTROLLER(UnitType.TEMPERATURE_CONTROLLER, ServiceType.TARGET_TEMPERATURE_SERVICE, ServiceType.TEMPERATURE_PROVIDER),
        SMOKE_DETECTOR_CONTROLLER(UnitType.SMOKE_DETECTOR, ServiceType.SMOKE_STATE_PROVIDER, ServiceType.SMOKE_ALARM_STATE_PROVIDER),
        TEMPERATURE_SENSOR(UnitType.TEMPERATURE_SENSOR, ServiceType.TEMPERATURE_PROVIDER), // TODO mpohling: whats about temperature service?
        BATTERY(UnitType.BATTERY, ServiceType.BATTERY_PROVIDER);

        private final UnitTemplate template;

        MockUnitTemplate(UnitTemplate.UnitType type, ServiceType... serviceTypes) {
            UnitTemplate.Builder templateBuilder = UnitTemplate.newBuilder();
            templateBuilder.setType(type);
            for (ServiceType serviceType : serviceTypes) {
                templateBuilder.addServiceType(serviceType);
            }
            this.template = templateBuilder.build();
        }

        public UnitTemplate getTemplate() {
            return template;
        }

        public static UnitTemplate getTemplate(UnitType type) throws CouldNotPerformException {
            for (MockUnitTemplate templateType : values()) {
                if (templateType.getTemplate().getType() == type) {
                    return templateType.getTemplate();
                }
            }
            throw new CouldNotPerformException("Could not find template for " + type + "!");
        }
    }

    public MockRegistry() throws InstantiationException {
        try {
            JPService.setupJUnitTestMode();
            List<Future<Void>> registryStartupTasks = new ArrayList<>();
            registryStartupTasks.add(GlobalExecuterService.submit(new Callable<Void>() {

                @Override
                public Void call() throws Exception {
                    try {
                        userRegistryLauncher = new UserRegistryLauncher();
                        userRegisty = userRegistryLauncher.getUserRegistry();
                        registerUser();
                    } catch (CouldNotPerformException | InterruptedException ex) {
                        ExceptionPrinter.printHistory(ex, logger, org.dc.jul.exception.printer.LogLevel.ERROR);
                    }
                    return null;
                }
            }));

            // wait for initialization
            for (Future<Void> task : registryStartupTasks) {
                task.get();
            }
            logger.info("User registry started!");

            registryStartupTasks.add(GlobalExecuterService.submit(new Callable<Void>() {

                @Override
                public Void call() throws Exception {
                    try {
                        locationRegistryLauncher = new LocationRegistryLauncher();
                        locationRegistry = locationRegistryLauncher.getLocationRegistry();
                    } catch (CouldNotPerformException | InterruptedException ex) {
                        ExceptionPrinter.printHistory(ex, logger, org.dc.jul.exception.printer.LogLevel.ERROR);
                    }
                    return null;
                }
            }));

            registryStartupTasks.add(GlobalExecuterService.submit(new Callable<Void>() {

                @Override
                public Void call() throws Exception {
                    try {
                        deviceRegistryLauncher = new DeviceRegistryLauncher();
                        deviceRegistry = deviceRegistryLauncher.getDeviceRegistry();
                        // load templates
                        for (MockUnitTemplate template : MockUnitTemplate.values()) {
                            deviceRegistry.updateUnitTemplate(template.getTemplate()).get();
                        }
                    } catch (CouldNotPerformException | InterruptedException ex) {
                        ExceptionPrinter.printHistory(ex, logger, org.dc.jul.exception.printer.LogLevel.ERROR);
                    }
                    return null;
                }
            }));

            // wait for initialization
            for (Future<Void> task : registryStartupTasks) {
                task.get();
            }
            logger.info("Location & Device registry started!");

            registryStartupTasks.add(GlobalExecuterService.submit(new Callable<Void>() {

                @Override
                public Void call() throws Exception {
                    try {
                        logger.info("Register locations...");
                        registerLocations();
                        // TODO need to be implemented.
                        // locationRegistry.waitForConsistency();
                        logger.info("Register devices...");
                        registerDevices();
                        logger.info("Wait for consistency");
                        deviceRegistry.waitForConsistency();
                    } catch (CouldNotPerformException | InterruptedException ex) {
                        ExceptionPrinter.printHistory(ex, logger, org.dc.jul.exception.printer.LogLevel.ERROR);
                    }
                    return null;
                }
            }));

            // wait for initialization
            for (Future<Void> task : registryStartupTasks) {
                logger.info("Wait for device & location registration...");
                task.get();
            }
            logger.info("Devices & Location registered!");

            registryStartupTasks.add(GlobalExecuterService.submit(new Callable<Void>() {

                @Override
                public Void call() throws Exception {
                    try {
                        agentRegistryLauncher = new AgentRegistryLauncher();
                        agentRegistry = agentRegistryLauncher.getAgentRegistry();
                    } catch (CouldNotPerformException | InterruptedException ex) {
                        ExceptionPrinter.printHistory(ex, logger, org.dc.jul.exception.printer.LogLevel.ERROR);
                    }
                    return null;
                }
            }));
            registryStartupTasks.add(GlobalExecuterService.submit(new Callable<Void>() {

                @Override
                public Void call() throws Exception {
                    try {
                        appRegistryLauncher = new AppRegistryLauncher();
                        appRegistry = appRegistryLauncher.getAppRegistry();
                    } catch (CouldNotPerformException | InterruptedException ex) {
                        ExceptionPrinter.printHistory(ex, logger, org.dc.jul.exception.printer.LogLevel.ERROR);
                    }
                    return null;
                }
            }));
            registryStartupTasks.add(GlobalExecuterService.submit(new Callable<Void>() {

                @Override
                public Void call() throws Exception {
                    try {
                        sceneRegistryLauncher = new SceneRegistryLauncher();
                        sceneRegistry = sceneRegistryLauncher.getSceneRegistry();
                    } catch (CouldNotPerformException | InterruptedException ex) {
                        ExceptionPrinter.printHistory(ex, logger, org.dc.jul.exception.printer.LogLevel.ERROR);
                    }
                    return null;
                }
            }));

            // wait for initialization
            for (Future<Void> task : registryStartupTasks) {
                task.get();
            }
            CachedDeviceRegistryRemote.reinitialize();
            CachedLocationRegistryRemote.reinitialize();
        } catch (Exception ex) {
            throw new InstantiationException(this, ex);
        }
    }

    public void shutdown() {
        System.out.println("Shutdown MockRegistry");
        deviceRegistryLauncher.shutdown();
        locationRegistryLauncher.shutdown();
        agentRegistryLauncher.shutdown();
        appRegistryLauncher.shutdown();
        sceneRegistryLauncher.shutdown();
        userRegistryLauncher.shutdown();
        CachedDeviceRegistryRemote.shutdown();
        CachedLocationRegistryRemote.shutdown();
        System.out.println("Mockregistry shutdown successfully!");
    }

    private void registerLocations() throws CouldNotPerformException, InterruptedException {
        try {
            paradise = locationRegistry.registerLocationConfig(LocationConfig.newBuilder().setLabel("Paradise").build()).get();
        } catch (ExecutionException ex) {
            throw new CouldNotPerformException(ex);
        }
    }

    private void registerUser() throws CouldNotPerformException, InterruptedException {
        UserConfig.Builder config = UserConfig.newBuilder().setFirstName("Max").setLastName("Mustermann").setUserName(USER_NAME);
        config.setEnablingState(EnablingState.newBuilder().setValue(EnablingState.State.ENABLED));
        try {
            testUser = userRegisty.registerUserConfig(config.build()).get();
        } catch (ExecutionException ex) {
            throw new CouldNotPerformException(ex);
        }
    }

    private void registerDevices() throws CouldNotPerformException, InterruptedException {
        ArrayList<UnitConfig> units = new ArrayList<>();

        try {
            // ambient light
            DeviceClass ambientLightClass = deviceRegistry.registerDeviceClass(getDeviceClass("Philips_Hue_E27", "KV01_18U", "Philips", UnitType.AMBIENT_LIGHT)).get();
            units.add(getUnitConfig(UnitType.AMBIENT_LIGHT, AMBIENT_LIGHT_LABEL));
            deviceRegistry.registerDeviceConfig(getDeviceConfig("PH_Hue_E27_Device", serialNumber, ambientLightClass, units)).get();

            units.clear();
            // battery, brightnessSensor, motionSensor, tamperSwitch, temperatureSensor
            DeviceClass motionSensorClass = deviceRegistry.registerDeviceClass(getDeviceClass("Fibaro_MotionSensor", "FGMS_001", "Fibaro", UnitType.MOTION_SENSOR, UnitType.BATTERY, UnitType.BRIGHTNESS_SENSOR, UnitType.TEMPERATURE_SENSOR, UnitType.TAMPER_SWITCH)).get();
            units.add(getUnitConfig(UnitType.MOTION_SENSOR, MOTION_SENSOR_LABEL));
            units.add(getUnitConfig(UnitType.BATTERY, BATTERY_LABEL));
            units.add(getUnitConfig(UnitType.BRIGHTNESS_SENSOR, BRIGHTNESS_SENSOR_LABEL));
            units.add(getUnitConfig(UnitType.TEMPERATURE_SENSOR, TEMPERATURE_SENSOR_LABEL));
            units.add(getUnitConfig(UnitType.TAMPER_SWITCH, TAMPER_SWITCH_LABEL));
            deviceRegistry.registerDeviceConfig(getDeviceConfig("F_MotionSensor_Device", serialNumber, motionSensorClass, units)).get();

            units.clear();
            // button
            DeviceClass buttonClass = deviceRegistry.registerDeviceClass(getDeviceClass("Gira_429496730210000", "429496730210000", "Gira", UnitType.BUTTON)).get();
            units.add(getUnitConfig(UnitTemplate.UnitType.BUTTON, BUTTON_LABEL));
            deviceRegistry.registerDeviceConfig(getDeviceConfig("GI_429496730210000_Device", serialNumber, buttonClass, units)).get();

            units.clear();
            // dimmer
            DeviceClass dimmerClass = deviceRegistry.registerDeviceClass(getDeviceClass("Hager_TYA663A", "TYA663A", "Hager", UnitType.DIMMER)).get();
            units.add(getUnitConfig(UnitTemplate.UnitType.DIMMER, DIMMER_LABEL));
            deviceRegistry.registerDeviceConfig(getDeviceConfig("HA_TYA663A_Device", serialNumber, dimmerClass, units)).get();

            units.clear();
            // handle
            DeviceClass handleClass = deviceRegistry.registerDeviceClass(getDeviceClass("Homematic_RotaryHandleSensor", "Sec_RHS", "Homematic", UnitType.HANDLE_SENSOR)).get();
            units.add(getUnitConfig(UnitTemplate.UnitType.HANDLE_SENSOR, HANDLE_SENSOR_LABEL));
            deviceRegistry.registerDeviceConfig(getDeviceConfig("HM_RotaryHandleSensor_Device", serialNumber, handleClass, units)).get();
            units.clear();
            // light
            DeviceClass lightClass = deviceRegistry.registerDeviceClass(getDeviceClass("Fibaro_FGS_221", "FGS_221", "Fibaro", UnitType.LIGHT)).get();
            units.add(getUnitConfig(UnitTemplate.UnitType.LIGHT, LIGHT_LABEL));
            deviceRegistry.registerDeviceConfig(getDeviceConfig("F_FGS221_Device", serialNumber, lightClass, units)).get();

            units.clear();
            // powerConsumptionSensor, powerPlug
            DeviceClass powerPlugClass = deviceRegistry.registerDeviceClass(getDeviceClass("Plugwise_PowerPlug", "070140", "Plugwise", UnitType.POWER_PLUG, UnitType.POWER_CONSUMPTION_SENSOR)).get();
            units.add(getUnitConfig(UnitTemplate.UnitType.POWER_PLUG, POWER_PLUG_LABEL));
            units.add(getUnitConfig(UnitTemplate.UnitType.POWER_CONSUMPTION_SENSOR, POWER_CONSUMPTION_LABEL));
            deviceRegistry.registerDeviceConfig(getDeviceConfig("PW_PowerPlug_Device", serialNumber, powerPlugClass, units)).get();

            units.clear();
            // reedSwitch
            DeviceClass reedSwitchClass = deviceRegistry.registerDeviceClass(getDeviceClass("Homematic_ReedSwitch", "Sec_SC_2", "Homematic", UnitType.REED_SWITCH)).get();
            units.add(getUnitConfig(UnitTemplate.UnitType.REED_SWITCH, REED_SWITCH_LABEL));
            deviceRegistry.registerDeviceConfig(getDeviceConfig("HM_ReedSwitch_Device", serialNumber, reedSwitchClass, units)).get();

            units.clear();
            // rollershutter
            DeviceClass rollershutterClass = deviceRegistry.registerDeviceClass(getDeviceClass("Hager_TYA628C", "TYA628C", "Hager", UnitType.ROLLERSHUTTER)).get();
            units.add(getUnitConfig(UnitTemplate.UnitType.ROLLERSHUTTER, ROLLERSHUTTER_LABEL));
            deviceRegistry.registerDeviceConfig(getDeviceConfig("HA_TYA628C_Device", serialNumber, rollershutterClass, units)).get();

            units.clear();
            // smoke detector
            DeviceClass smokeDetector = deviceRegistry.registerDeviceClass(getDeviceClass("Fibaro_FGSS_001", "FGSS_001", "Fibaro", UnitType.SMOKE_DETECTOR)).get();
            units.add(getUnitConfig(UnitTemplate.UnitType.SMOKE_DETECTOR, SMOKE_DETECTOR_LABEL));
            deviceRegistry.registerDeviceConfig(getDeviceConfig("Fibaro_SmokeDetector_Device", serialNumber, smokeDetector, units)).get();

            units.clear();
            // temperature controller
            DeviceClass temperatureControllerClass = deviceRegistry.registerDeviceClass(getDeviceClass("Gira_429496730250000", "429496730250000", "Gira", UnitType.TEMPERATURE_CONTROLLER)).get();
            units.add(getUnitConfig(UnitTemplate.UnitType.TEMPERATURE_CONTROLLER, TEMPERATURE_CONTROLLER_LABEL));
            deviceRegistry.registerDeviceConfig(getDeviceConfig("Gire_TemperatureController_Device", serialNumber, temperatureControllerClass, units)).get();
        } catch (ExecutionException ex) {
            throw new CouldNotPerformException(ex);
        }
    }

    public static PlacementConfig getDefaultPlacement() {
        Rotation rotation = Rotation.newBuilder().setQw(1).setQx(0).setQy(0).setQz(0).build();
        Translation translation = Translation.newBuilder().setX(0).setY(0).setZ(0).build();
        Pose pose = Pose.newBuilder().setRotation(rotation).setTranslation(translation).build();
        return PlacementConfig.newBuilder().setPosition(pose).setLocationId(getLocation().getId()).build();
    }

    public static Iterable<ServiceConfigType.ServiceConfig> getServiceConfig(final UnitTemplate template) {
        List<ServiceConfigType.ServiceConfig> serviceConfigList = new ArrayList<>();
        for (ServiceType type : template.getServiceTypeList()) {
            BindingServiceConfigType.BindingServiceConfig bindingServiceConfig = BindingServiceConfigType.BindingServiceConfig.newBuilder().setType(BindingTypeHolderType.BindingTypeHolder.BindingType.OPENHAB).build();
            serviceConfigList.add(ServiceConfig.newBuilder().setType(type).setBindingServiceConfig(bindingServiceConfig).build());
        }
        return serviceConfigList;
    }

    public static UnitConfig getUnitConfig(UnitTemplate.UnitType type, String label) throws CouldNotPerformException {
        UnitTemplate template = MockUnitTemplate.getTemplate(type);
        return UnitConfig.newBuilder().setPlacementConfig(getDefaultPlacement()).setType(type).addAllServiceConfig(getServiceConfig(template)).setLabel(label).setBoundToDevice(false).build();
    }

    public static DeviceConfig getDeviceConfig(String label, String serialNumber, DeviceClass clazz, ArrayList<UnitConfig> units) {
        return DeviceConfig.newBuilder()
                .setPlacementConfig(getDefaultPlacement())
                .setLabel(label)
                .setSerialNumber(serialNumber)
                .setDeviceClassId(clazz.getId())
                .addAllUnitConfig(units)
                .setInventoryState(InventoryStateType.InventoryState.newBuilder().setValue(InventoryStateType.InventoryState.State.INSTALLED))
                .build();
    }

    private static List<UnitTemplateConfig> getUnitTemplateConfigs(List<UnitTemplate.UnitType> unitTypes) throws CouldNotPerformException {
        List<UnitTemplateConfig> unitTemplateConfigs = new ArrayList<>();
        for (UnitTemplate.UnitType type : unitTypes) {
            List<ServiceTemplate> serviceTemplates = new ArrayList<>();
            for (ServiceType serviceType : MockUnitTemplate.getTemplate(type).getServiceTypeList()) {
                serviceTemplates.add(ServiceTemplate.newBuilder().setServiceType(serviceType).build());
            }
            UnitTemplateConfig config = UnitTemplateConfig.newBuilder().setType(type).addAllServiceTemplate(serviceTemplates).build();
            unitTemplateConfigs.add(config);
        }
        return unitTemplateConfigs;
    }

    public static DeviceClass getDeviceClass(String label, String productNumber, String company, UnitTemplate.UnitType... types) throws CouldNotPerformException {
        List<UnitTemplate.UnitType> unitTypeList = new ArrayList<>();
        for (UnitTemplate.UnitType type : types) {
            unitTypeList.add(type);
        }
        return DeviceClass.newBuilder().setLabel(label).setProductNumber(productNumber).setCompany(company)
                .setBindingConfig(getBindingConfig()).addAllUnitTemplateConfig(getUnitTemplateConfigs(unitTypeList)).build();
    }

    public static BindingConfig getBindingConfig() {
        BindingConfig.Builder bindingConfigBuilder = BindingConfig.newBuilder();
        bindingConfigBuilder.setType(BindingTypeHolderType.BindingTypeHolder.BindingType.OPENHAB);
        return bindingConfigBuilder.build();
    }

    public static LocationConfig getLocation() {
        return paradise;
    }
}
