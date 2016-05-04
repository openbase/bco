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
import java.util.concurrent.ExecutionException;
import org.dc.bco.registry.agent.core.AgentRegistryLauncher;
import org.dc.bco.registry.app.core.AppRegistryLauncher;
import org.dc.bco.registry.device.core.DeviceRegistryLauncher;
import org.dc.bco.registry.device.remote.DeviceRegistryRemote;
import org.dc.bco.registry.location.core.LocationRegistryLauncher;
import org.dc.bco.registry.location.remote.LocationRegistryRemote;
import org.dc.bco.registry.scene.core.SceneRegistryLauncher;
import org.dc.bco.registry.user.core.UserRegistryLauncher;
import org.dc.bco.registry.user.remote.UserRegistryRemote;
import org.dc.jps.core.JPService;
import org.dc.jul.exception.CouldNotPerformException;
import org.dc.jul.exception.InstantiationException;
import org.dc.jul.exception.printer.ExceptionPrinter;
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
import rst.homeautomation.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import rst.homeautomation.state.EnablingStateType.EnablingState;
import rst.homeautomation.state.InventoryStateType;
import rst.homeautomation.unit.UnitConfigType.UnitConfig;
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

    private static DeviceRegistryLauncher deviceRegistry;
    private static LocationRegistryLauncher locationRegistry;
    private static AgentRegistryLauncher agentRegistry;
    private static AppRegistryLauncher appRegistry;
    private static SceneRegistryLauncher sceneRegistry;
    private static UserRegistryLauncher userRegistry;

    private final DeviceRegistryRemote deviceRemote;
    private final LocationRegistryRemote locationRemote;
    private final UserRegistryRemote userRemote;

    private static LocationConfig paradise;

    public enum MockUnitTemplate {

        AMBIENT_LIGHT(UnitType.AMBIENT_LIGHT, ServiceType.COLOR_SERVICE, ServiceType.POWER_SERVICE, ServiceType.BRIGHTNESS_SERVICE),
        LIGHT(UnitType.LIGHT, ServiceType.POWER_SERVICE, ServiceType.BRIGHTNESS_SERVICE),
        MOTION_SENSOR(UnitType.MOTION_SENSOR, ServiceType.MOTION_PROVIDER),
        BRIGHTNESS_SENSOR(UnitType.BRIGHTNESS_SENSOR, ServiceType.BRIGHTNESS_PROVIDER),
        BUTTON(UnitType.BUTTON, ServiceType.BUTTON_PROVIDER),
        DIMMER(UnitType.DIMMER, ServiceType.DIM_SERVICE, ServiceType.POWER_SERVICE),
        HANDLE_SENSOR(UnitType.HANDLE_SENSOR, ServiceType.HANDLE_PROVIDER),
        POWER_CONSUMPTION_SENSOR(UnitType.POWER_CONSUMPTION_SENSOR, ServiceType.POWER_CONSUMPTION_PROVIDER),
        POWER_PLUG(UnitType.POWER_PLUG, ServiceType.POWER_SERVICE),
        REED_SWITCH(UnitType.REED_SWITCH, ServiceType.REED_SWITCH_PROVIDER),
        ROLLERSHUTTER(UnitType.ROLLERSHUTTER, ServiceType.SHUTTER_SERVICE, ServiceType.OPENING_RATIO_PROVIDER),
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
//            String user = ScopeGenerator.convertIntoValidScopeComponent(System.getProperty("user.name"));
//            JPService.registerProperty(JPInitializeDB.class, true);
            JPService.setupJUnitTestMode();

            Thread deviceRegistryThread = new Thread(new Runnable() {

                @Override
                public void run() {
                    try {
                        deviceRegistry = new DeviceRegistryLauncher();
                    } catch (CouldNotPerformException | InterruptedException ex) {
                        ExceptionPrinter.printHistory(ex, logger, org.dc.jul.exception.printer.LogLevel.ERROR);
                    }
                }
            });

            Thread locationRegistryThread = new Thread(new Runnable() {

                @Override
                public void run() {
                    try {
                        locationRegistry = new LocationRegistryLauncher();
                    } catch (CouldNotPerformException | InterruptedException ex) {
                        ExceptionPrinter.printHistory(ex, logger, org.dc.jul.exception.printer.LogLevel.ERROR);
                    }
                }
            });

            Thread agentRegistryThread = new Thread(new Runnable() {

                @Override
                public void run() {
                    try {
                        agentRegistry = new AgentRegistryLauncher();
                    } catch (CouldNotPerformException | InterruptedException ex) {
                        ExceptionPrinter.printHistory(ex, logger, org.dc.jul.exception.printer.LogLevel.ERROR);
                    }
                }
            });

            Thread appRegistryThread = new Thread(new Runnable() {

                @Override
                public void run() {
                    try {
                        appRegistry = new AppRegistryLauncher();
                    } catch (CouldNotPerformException | InterruptedException ex) {
                        ExceptionPrinter.printHistory(ex, logger, org.dc.jul.exception.printer.LogLevel.ERROR);
                    }
                }
            });

            Thread sceneRegistryThread = new Thread(new Runnable() {

                @Override
                public void run() {
                    try {
                        sceneRegistry = new SceneRegistryLauncher();
                    } catch (CouldNotPerformException | InterruptedException ex) {
                        ExceptionPrinter.printHistory(ex, logger, org.dc.jul.exception.printer.LogLevel.ERROR);
                    }
                }
            });

            Thread userRegistryThread = new Thread(new Runnable() {

                @Override
                public void run() {
                    try {
                        userRegistry = new UserRegistryLauncher();
                    } catch (CouldNotPerformException | InterruptedException ex) {
                        ExceptionPrinter.printHistory(ex, logger, org.dc.jul.exception.printer.LogLevel.ERROR);
                    }
                }
            });

            deviceRegistryThread.start();
            locationRegistryThread.start();
            agentRegistryThread.start();
            appRegistryThread.start();
            sceneRegistryThread.start();
            userRegistryThread.start();

            deviceRegistryThread.join();
            locationRegistryThread.join();
            agentRegistryThread.join();
            appRegistryThread.join();
            sceneRegistryThread.join();
            userRegistryThread.join();

            deviceRemote = new DeviceRegistryRemote();
            locationRemote = new LocationRegistryRemote();
            userRemote = new UserRegistryRemote();

            deviceRemote.init();
            locationRemote.init();
            userRemote.init();

            deviceRemote.activate();
            locationRemote.activate();
            userRemote.activate();

            for (MockUnitTemplate template : MockUnitTemplate.values()) {
                deviceRemote.updateUnitTemplate(template.getTemplate());
            }
            registerUser();
            registerLocations();
            registerDevices();
        } catch (Exception ex) {
            throw new InstantiationException(this, ex);
        }
    }

    public void shutdown() {
        deviceRemote.shutdown();
        locationRemote.shutdown();
        userRemote.shutdown();
        deviceRegistry.shutdown();
        locationRegistry.shutdown();
        agentRegistry.shutdown();
        appRegistry.shutdown();
        sceneRegistry.shutdown();
        userRegistry.shutdown();
    }

    private void registerLocations() throws CouldNotPerformException, InterruptedException {
        try {
            paradise = locationRemote.registerLocationConfig(LocationConfig.newBuilder().setLabel("Paradise").build()).get();
        } catch (ExecutionException ex) {
            throw new CouldNotPerformException(ex);
        }
    }

    private void registerUser() throws CouldNotPerformException, InterruptedException {
        UserConfig.Builder config = UserConfig.newBuilder().setFirstName("Max").setLastName("Mustermann").setUserName(USER_NAME);
        config.setEnablingState(EnablingState.newBuilder().setValue(EnablingState.State.ENABLED));
        try {
            testUser = userRemote.registerUserConfig(config.build()).get();
        } catch (ExecutionException ex) {
            throw new CouldNotPerformException(ex);
        }
    }

    private void registerDevices() throws CouldNotPerformException, InterruptedException {
        ArrayList<UnitConfig> units = new ArrayList<>();

        try {
        // ambient light
        DeviceClass ambientLightClass = deviceRemote.registerDeviceClass(getDeviceClass("Philips_Hue_E27", "KV01_18U", "Philips")).get();
        units.add(getUnitConfig(UnitTemplate.UnitType.AMBIENT_LIGHT, AMBIENT_LIGHT_LABEL));
        deviceRemote.registerDeviceConfig(getDeviceConfig("PH_Hue_E27_Device", serialNumber, ambientLightClass, units));

        units.clear();
        // battery, brightnessSensor, motionSensor, tamperSwitch, temperatureSensor
        DeviceClass motionSensorClass = deviceRemote.registerDeviceClass(getDeviceClass("Fibaro_MotionSensor", "FGMS_001", "Fibaro")).get();
        units.add(getUnitConfig(UnitTemplate.UnitType.MOTION_SENSOR, MOTION_SENSOR_LABEL));
        units.add(getUnitConfig(UnitTemplate.UnitType.BATTERY, BATTERY_LABEL));
        units.add(getUnitConfig(UnitTemplate.UnitType.BRIGHTNESS_SENSOR, BRIGHTNESS_SENSOR_LABEL));
        units.add(getUnitConfig(UnitTemplate.UnitType.TEMPERATURE_SENSOR, TEMPERATURE_SENSOR_LABEL));
        units.add(getUnitConfig(UnitTemplate.UnitType.TAMPER_SWITCH, TAMPER_SWITCH_LABEL));
        deviceRemote.registerDeviceConfig(getDeviceConfig("F_MotionSensor_Device", serialNumber, motionSensorClass, units));

        units.clear();
        // button
        DeviceClass buttonClass = deviceRemote.registerDeviceClass(getDeviceClass("Gira_429496730210000", "429496730210000", "Gira")).get();
        units.add(getUnitConfig(UnitTemplate.UnitType.BUTTON, BUTTON_LABEL));
        deviceRemote.registerDeviceConfig(getDeviceConfig("GI_429496730210000_Device", serialNumber, buttonClass, units));

        units.clear();
        // dimmer
        DeviceClass dimmerClass = deviceRemote.registerDeviceClass(getDeviceClass("Hager_TYA663A", "TYA663A", "Hager")).get();
        units.add(getUnitConfig(UnitTemplate.UnitType.DIMMER, DIMMER_LABEL));
        deviceRemote.registerDeviceConfig(getDeviceConfig("HA_TYA663A_Device", serialNumber, dimmerClass, units));

        units.clear();
        // handle
        DeviceClass handleClass = deviceRemote.registerDeviceClass(getDeviceClass("Homematic_RotaryHandleSensor", "Sec_RHS", "Homematic")).get();
        units.add(getUnitConfig(UnitTemplate.UnitType.HANDLE_SENSOR, HANDLE_SENSOR_LABEL));
        deviceRemote.registerDeviceConfig(getDeviceConfig("HM_RotaryHandleSensor_Device", serialNumber, handleClass, units));

        units.clear();
        // light
        DeviceClass lightClass = deviceRemote.registerDeviceClass(getDeviceClass("Fibaro_FGS_221", "FGS_221", "Fibaro")).get();
        units.add(getUnitConfig(UnitTemplate.UnitType.LIGHT, LIGHT_LABEL));
        deviceRemote.registerDeviceConfig(getDeviceConfig("F_FGS221_Device", serialNumber, lightClass, units));

        units.clear();
        // powerConsumptionSensor, powerPlug
        DeviceClass powerPlugClass = deviceRemote.registerDeviceClass(getDeviceClass("Plugwise_PowerPlug", "070140", "Plugwise")).get();
        units.add(getUnitConfig(UnitTemplate.UnitType.POWER_PLUG, POWER_PLUG_LABEL));
        units.add(getUnitConfig(UnitTemplate.UnitType.POWER_CONSUMPTION_SENSOR, POWER_CONSUMPTION_LABEL));
        deviceRemote.registerDeviceConfig(getDeviceConfig("PW_PowerPlug_Device", serialNumber, powerPlugClass, units));

        units.clear();
        // reedSwitch
        DeviceClass reedSwitchClass = deviceRemote.registerDeviceClass(getDeviceClass("Homematic_ReedSwitch", "Sec_SC_2", "Homematic")).get();
        units.add(getUnitConfig(UnitTemplate.UnitType.REED_SWITCH, REED_SWITCH_LABEL));
        deviceRemote.registerDeviceConfig(getDeviceConfig("HM_ReedSwitch_Device", serialNumber, reedSwitchClass, units));

        units.clear();
        // rollershutter
        DeviceClass rollershutterClass = deviceRemote.registerDeviceClass(getDeviceClass("Hager_TYA628C", "TYA628C", "Hager")).get();
        units.add(getUnitConfig(UnitTemplate.UnitType.ROLLERSHUTTER, ROLLERSHUTTER_LABEL));
        deviceRemote.registerDeviceConfig(getDeviceConfig("HA_TYA628C_Device", serialNumber, rollershutterClass, units));

        units.clear();
        // smoke detector
        DeviceClass smokeDetector = deviceRemote.registerDeviceClass(getDeviceClass("Fibaro_FGSS_001", "FGSS_001", "Fibaro")).get();
        units.add(getUnitConfig(UnitTemplate.UnitType.SMOKE_DETECTOR, SMOKE_DETECTOR_LABEL));
        deviceRemote.registerDeviceConfig(getDeviceConfig("Fibaro_SmokeDetector_Device", serialNumber, smokeDetector, units));

        units.clear();
        // temperature controller
        DeviceClass temperatureControllerClass = deviceRemote.registerDeviceClass(getDeviceClass("Gira_429496730250000", "429496730250000", "Gira")).get();
        units.add(getUnitConfig(UnitTemplate.UnitType.TEMPERATURE_CONTROLLER, TEMPERATURE_CONTROLLER_LABEL));
        deviceRemote.registerDeviceConfig(getDeviceConfig("Gire_TemperatureController_Device", serialNumber, temperatureControllerClass, units));
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

    public static DeviceClass getDeviceClass(String label, String productNumber, String company) {

        return DeviceClass.newBuilder().setLabel(label).setProductNumber(productNumber).setCompany(company).setBindingConfig(getBindingConfig()).build();

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
