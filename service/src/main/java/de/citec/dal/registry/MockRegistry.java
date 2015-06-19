/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.registry;

import de.citec.dm.core.DeviceManager;
import de.citec.dm.remote.DeviceRegistryRemote;
import de.citec.jp.JPDeviceClassDatabaseDirectory;
import de.citec.jp.JPDeviceConfigDatabaseDirectory;
import de.citec.jp.JPDeviceDatabaseDirectory;
import de.citec.jp.JPDeviceRegistryScope;
import de.citec.jp.JPLocationConfigDatabaseDirectory;
import de.citec.jp.JPLocationDatabaseDirectory;
import de.citec.jp.JPLocationRegistryScope;
import de.citec.jps.core.JPService;
import de.citec.jul.exception.CouldNotPerformException;
import de.citec.jul.exception.ExceptionPrinter;
import de.citec.jul.exception.InstantiationException;
import de.citec.jul.extension.rsb.scope.ScopeGenerator;
import de.citec.jul.storage.jp.JPInitializeDB;
import de.citec.lm.core.LocationManager;
import de.citec.lm.remote.LocationRegistryRemote;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.LoggerFactory;
import rsb.Scope;
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
import rst.homeautomation.service.ServiceTypeHolderType;
import rst.homeautomation.service.ServiceTypeHolderType.ServiceTypeHolder.ServiceType;
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
    private final String serialNumber = "1234-5678-9100";

    private static DeviceManager deviceManager;
    private static LocationManager locationManager;

    private final DeviceRegistryRemote deviceRemote;
    private final LocationRegistryRemote locationRemote;

    private LocationConfig paradise;

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
        TEMPERATURE_CONTROLLER(UnitType.TEMPERATURE_CONTROLLER, ServiceType.TEMPERATURE_PROVIDER),
        TEMPERATURE_SENSOR(UnitType.TEMPERATURE_SENSOR, ServiceType.TEMPERATURE_PROVIDER), // TODO mpohling: whats about temperature service?
        BATTERY(UnitType.BATTERY, ServiceType.BATTERY_PROVIDER);

        private final UnitTemplate template;

        MockUnitTemplate(UnitTemplate.UnitType type, ServiceTypeHolderType.ServiceTypeHolder.ServiceType... serviceTypes) {
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
            String user = ScopeGenerator.convertIntoValidScopeComponent(System.getProperty("user.name"));
            JPService.registerProperty(JPInitializeDB.class, true);
            JPService.registerProperty(JPDeviceDatabaseDirectory.class, new File("/tmp/" + user + "/test-device-registry"));
            JPService.registerProperty(JPLocationDatabaseDirectory.class, new File("/tmp/" + user + "/test-location-registry"));
            JPService.registerProperty(JPDeviceConfigDatabaseDirectory.class);
            JPService.registerProperty(JPDeviceClassDatabaseDirectory.class);
            JPService.registerProperty(JPLocationConfigDatabaseDirectory.class);
            JPService.registerProperty(JPDeviceRegistryScope.class, new Scope("/test/" + user + "/device_registry"));
            JPService.registerProperty(JPLocationRegistryScope.class, new Scope("/test/" + user + "/location_registry"));
            JPService.setupJUnitTestMode();

            Thread deviceRegistryThread = new Thread(new Runnable() {

                @Override
                public void run() {
                    try {
                        deviceManager = new DeviceManager();
                    } catch (CouldNotPerformException | InterruptedException ex) {
                        ExceptionPrinter.printHistoryAndReturnThrowable(logger, ex);
                    }
                }
            });

            Thread locationRegistryThread = new Thread(new Runnable() {

                @Override
                public void run() {
                    try {
                        locationManager = new LocationManager();
                    } catch (CouldNotPerformException | InterruptedException ex) {
                        ExceptionPrinter.printHistoryAndReturnThrowable(logger, ex);
                    }
                }
            });

            deviceRegistryThread.start();
            locationRegistryThread.start();

            deviceRegistryThread.join();
            locationRegistryThread.join();

            deviceRemote = new DeviceRegistryRemote();
            locationRemote = new LocationRegistryRemote();

            deviceRemote.init();
            locationRemote.init();

            deviceRemote.activate();
            locationRemote.activate();

            deviceRemote.requestStatus();
            locationRemote.requestStatus();

            registerLocations();
            registerDevices();
        } catch (Exception ex) {
            throw new InstantiationException(this, ex);
        }
    }

    public void shutdown() {
        deviceRemote.shutdown();
        locationRemote.shutdown();
        deviceManager.shutdown();
        locationManager.shutdown();
    }

    private void registerLocations() throws CouldNotPerformException {
        paradise = locationRemote.registerLocationConfig(LocationConfig.newBuilder().setLabel("Paradise").build());
    }

    private void registerDevices() throws CouldNotPerformException {
        ArrayList<UnitConfig> units = new ArrayList<>();

        // ambient light
        DeviceClass ambientLightClass = deviceRemote.registerDeviceClass(getDeviceClass("Philips_Hue_E27", "KV01_18U", "Philips"));
        units.add(getUnitConfig(UnitTemplate.UnitType.AMBIENT_LIGHT, AMBIENT_LIGHT_LABEL));
        deviceRemote.registerDeviceConfig(getDeviceConfig("PH_Hue_E27_Device", serialNumber, ambientLightClass, units));

        units.clear();
        // battery, brightnessSensor, motionSensor, tamperSwitch, temperatureSensor
        DeviceClass motionSensorClass = deviceRemote.registerDeviceClass(getDeviceClass("Fibaro_MotionSensor", "FGMS_001", "Fibaro"));
        units.add(getUnitConfig(UnitTemplate.UnitType.MOTION_SENSOR, MOTION_SENSOR_LABEL));
        units.add(getUnitConfig(UnitTemplate.UnitType.BATTERY, BATTERY_LABEL));
        units.add(getUnitConfig(UnitTemplate.UnitType.BRIGHTNESS_SENSOR, BRIGHTNESS_SENSOR_LABEL));
        units.add(getUnitConfig(UnitTemplate.UnitType.TEMPERATURE_SENSOR, TEMPERATURE_SENSOR_LABEL));
        units.add(getUnitConfig(UnitTemplate.UnitType.TAMPER_SWITCH, TAMPER_SWITCH_LABEL));
        deviceRemote.registerDeviceConfig(getDeviceConfig("F_MotionSensor_Device", serialNumber, motionSensorClass, units));

        units.clear();
        // button
        DeviceClass buttonClass = deviceRemote.registerDeviceClass(getDeviceClass("Gira_429496730210000", "429496730210000", "Gira"));
        units.add(getUnitConfig(UnitTemplate.UnitType.BUTTON, BUTTON_LABEL));
        deviceRemote.registerDeviceConfig(getDeviceConfig("GI_429496730210000_Device", serialNumber, buttonClass, units));

        units.clear();
        // dimmer
        DeviceClass dimmerClass = deviceRemote.registerDeviceClass(getDeviceClass("Hager_TYA663A", "TYA663A", "Hager"));
        units.add(getUnitConfig(UnitTemplate.UnitType.DIMMER, DIMMER_LABEL));
        deviceRemote.registerDeviceConfig(getDeviceConfig("HA_TYA663A_Device", serialNumber, dimmerClass, units));

        units.clear();
        // handle
        DeviceClass handleClass = deviceRemote.registerDeviceClass(getDeviceClass("Homematic_RotaryHandleSensor", "Sec_RHS", "Homematic"));
        units.add(getUnitConfig(UnitTemplate.UnitType.HANDLE_SENSOR, HANDLE_SENSOR_LABEL));
        deviceRemote.registerDeviceConfig(getDeviceConfig("HM_RotaryHandleSensor_Device", serialNumber, handleClass, units));

        units.clear();
        // light 
        DeviceClass lightClass = deviceRemote.registerDeviceClass(getDeviceClass("Fibaro_FGS_221", "FGS_221", "Fibaro"));
        units.add(getUnitConfig(UnitTemplate.UnitType.LIGHT, LIGHT_LABEL));
        deviceRemote.registerDeviceConfig(getDeviceConfig("F_FGS221_Device", serialNumber, lightClass, units));

        units.clear();
        // powerConsumptionSensor, powerPlug 
        DeviceClass powerPlugClass = deviceRemote.registerDeviceClass(getDeviceClass("Plugwise_PowerPlug", "070140", "Plugwise"));
        units.add(getUnitConfig(UnitTemplate.UnitType.POWER_PLUG, POWER_PLUG_LABEL));
        units.add(getUnitConfig(UnitTemplate.UnitType.POWER_CONSUMPTION_SENSOR, POWER_CONSUMPTION_LABEL));
        deviceRemote.registerDeviceConfig(getDeviceConfig("PW_PowerPlug_Device", serialNumber, powerPlugClass, units));

        units.clear();
        // reedSwitch 
        DeviceClass reedSwitchClass = deviceRemote.registerDeviceClass(getDeviceClass("Homematic_ReedSwitch", "Sec_SC_2", "Homematic"));
        units.add(getUnitConfig(UnitTemplate.UnitType.REED_SWITCH, REED_SWITCH_LABEL));
        deviceRemote.registerDeviceConfig(getDeviceConfig("HM_ReedSwitch_Device", serialNumber, reedSwitchClass, units));

        units.clear();
        // rollershutter 
        DeviceClass rollershutterClass = deviceRemote.registerDeviceClass(getDeviceClass("Hager_TYA628C", "TYA628C", "Hager"));
        units.add(getUnitConfig(UnitTemplate.UnitType.ROLLERSHUTTER, ROLLERSHUTTER_LABEL));
        deviceRemote.registerDeviceConfig(getDeviceConfig("HA_TYA628C_Device", serialNumber, rollershutterClass, units));
    }

    private PlacementConfig getDefaultPlacement() {
        Rotation rotation = Rotation.newBuilder().setQw(1).setQx(0).setQy(0).setQz(0).build();
        Translation translation = Translation.newBuilder().setX(0).setY(0).setZ(0).build();
        Pose pose = Pose.newBuilder().setRotation(rotation).setTranslation(translation).build();
        return PlacementConfig.newBuilder().setPosition(pose).setLocationId(getLocation().getId()).build();
    }

    private Iterable<ServiceConfigType.ServiceConfig> getServiceConfig(final UnitTemplate template) {
        List<ServiceConfigType.ServiceConfig> serviceConfigList = new ArrayList<>();
        for (ServiceType type : template.getServiceTypeList()) {
            BindingServiceConfigType.BindingServiceConfig bindingServiceConfig = BindingServiceConfigType.BindingServiceConfig.newBuilder().setType(BindingTypeHolderType.BindingTypeHolder.BindingType.OPENHAB).build();
            serviceConfigList.add(ServiceConfig.newBuilder().setType(type).setBindingServiceConfig(bindingServiceConfig).build());
        }
        return serviceConfigList;
    }

    private UnitConfig getUnitConfig(UnitTemplate.UnitType type, String label) throws CouldNotPerformException {
        UnitTemplate template = MockUnitTemplate.getTemplate(type);
        return UnitConfig.newBuilder().setPlacementConfig(getDefaultPlacement()).setTemplate(template).addAllServiceConfig(getServiceConfig(template)).setLabel(label).build();
    }

    private DeviceConfig getDeviceConfig(String label, String serialNumber, DeviceClass clazz, ArrayList<UnitConfig> units) {
        return DeviceConfig.newBuilder()
                .setPlacementConfig(getDefaultPlacement())
                .setLabel(label)
                .setSerialNumber(serialNumber)
                .setDeviceClass(clazz)
                .addAllUnitConfig(units)
                .setInventoryState(InventoryStateType.InventoryState.newBuilder().setValue(InventoryStateType.InventoryState.State.INSTALLED))
                .build();
    }

    private DeviceClass getDeviceClass(String label, String productNumber, String company) {

        return DeviceClass.newBuilder().setLabel(label).setProductNumber(productNumber).setCompany(company).setBindingConfig(getBindingConfig()).build();

    }

    private BindingConfig getBindingConfig() {
        BindingConfig.Builder bindingConfigBuilder = BindingConfig.newBuilder();
        bindingConfigBuilder.setType(BindingTypeHolderType.BindingTypeHolder.BindingType.OPENHAB);
        return bindingConfigBuilder.build();
    }

    public LocationConfig getLocation() {
        return paradise;
    }
}
