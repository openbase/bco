/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.remote.unit;

import de.citec.csra.dm.DeviceManager;
import de.citec.csra.dm.remote.DeviceRegistryRemote;
import de.citec.csra.lm.LocationManager;
import de.citec.csra.lm.remote.LocationRegistryRemote;
import de.citec.jp.JPDeviceClassDatabaseDirectory;
import de.citec.jp.JPDeviceConfigDatabaseDirectory;
import de.citec.jp.JPDeviceDatabaseDirectory;
import de.citec.jp.JPDeviceRegistryScope;
import de.citec.jp.JPLocationConfigDatabaseDirectory;
import de.citec.jp.JPLocationDatabaseDirectory;
import de.citec.jp.JPLocationRegistryScope;
import de.citec.jps.core.JPService;
import de.citec.jps.preset.JPTestMode;
import de.citec.jul.exception.CouldNotPerformException;
import de.citec.jul.exception.InitializationException;
import de.citec.jul.exception.InstantiationException;
import de.citec.jul.exception.InvalidStateException;
import de.citec.jul.storage.jp.JPInitializeDB;
import java.io.File;
import java.util.ArrayList;
import org.slf4j.LoggerFactory;
import rsb.Scope;
import rst.geometry.PoseType.Pose;
import rst.geometry.RotationType.Rotation;
import rst.geometry.TranslationType.Translation;
import rst.homeautomation.device.DeviceClassType.DeviceClass;
import rst.homeautomation.device.DeviceConfigType.DeviceConfig;
import rst.homeautomation.unit.UnitConfigType.UnitConfig;
import rst.homeautomation.unit.UnitTemplateType.UnitTemplate;
import rst.spatial.LocationConfigType.LocationConfig;
import rst.spatial.PlacementConfigType.PlacementConfig;

/**
 *
 * @author thuxohl
 */
public class MockRegistryHolder {

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(MockRegistryHolder.class);

    public static final String AMBIENT_LIGHT_LABEL = "Ambient_Light_Unit_Test";
    public static final String BATTERY_LABEL = "Battery_Unit_Test";
    public static final String BRIGHTNESS_SENSOR_LABEL = "Brightness_Sensor_Unit_Test";
    public static final String BUTTON_LABEL = "Button_Unit_Test";
    public static final String DIMMER_LABEL = "Dimmer_Unit_Test";
    public static final String HANDLE_SENSOR_LABEL = "Handle_Sensor_Unit_Test";
    public static final String LIGHT_LABEL = "Light_Unit_Test";
    public static final String MOTION_SENSOR_LABEL = "Motion_Sensor_Unit_Test";
    public static final String POWER_CONSUMPTION_LABEL = "Power_Consumption_Unit_Test";
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

    public MockRegistryHolder() throws InitializationException, InstantiationException, InvalidStateException, CouldNotPerformException {
        JPService.registerProperty(JPTestMode.class, true);
        JPService.registerProperty(JPInitializeDB.class, true);
        JPService.registerProperty(JPDeviceDatabaseDirectory.class, new File("/tmp/test-registry"));
        JPService.registerProperty(JPDeviceConfigDatabaseDirectory.class, new File("/tmp/test-registry/device_class"));
        JPService.registerProperty(JPDeviceClassDatabaseDirectory.class, new File("/tmp/test-registry/device_config"));
        JPService.registerProperty(JPLocationDatabaseDirectory.class, new File("/tmp/test-registry"));
        JPService.registerProperty(JPLocationConfigDatabaseDirectory.class, new File("/tmp/test-registry/location_config"));
        JPService.registerProperty(JPDeviceRegistryScope.class, new Scope("/unit_tests/device_registry"));
        JPService.registerProperty(JPLocationRegistryScope.class, new Scope("/unit_tests/location_registry"));
        deviceManager = new DeviceManager();
        locationManager = new LocationManager();

        deviceRemote = new DeviceRegistryRemote();
        locationRemote = new LocationRegistryRemote();

        deviceRemote.init(JPService.getProperty(JPDeviceRegistryScope.class).getValue());
        locationRemote.init(JPService.getProperty(JPLocationRegistryScope.class).getValue());

        deviceRemote.activate();
        locationRemote.activate();

        deviceRemote.requestStatus();
        locationRemote.requestStatus();

        registerLocation();
        registerDevices();
    }

    public void shutdown() throws InterruptedException, CouldNotPerformException {
        deviceRemote.deactivate();
        locationRemote.deactivate();

        deviceManager.shutdown();
        locationManager.shutdown();
    }

    public void registerLocation() throws CouldNotPerformException {
        paradise = locationRemote.registerLocationConfig(LocationConfig.newBuilder().setLabel("Paradise").build());
    }

    public void registerDevices() throws CouldNotPerformException {
        ArrayList<UnitConfig> units = new ArrayList<>();

        // ambient light
        DeviceClass ambientLightClass = deviceRemote.registerDeviceClass(getDeviceClass("PH_Hue_E27"));
        units.add(getUnitConfig(UnitTemplate.UnitType.AMBIENT_LIGHT, AMBIENT_LIGHT_LABEL));
        deviceRemote.registerDeviceConfig(getDeviceConfig("PH_Hue_E27_Device", serialNumber, ambientLightClass, units));

        units.clear();
        // battery, brightnessSensor, motionSensor, tamperSwitch, temperatureSensor
        DeviceClass motionSensorClass = deviceRemote.registerDeviceClass(getDeviceClass("F_MotionSensor"));
        units.add(getUnitConfig(UnitTemplate.UnitType.MOTION_SENSOR, MOTION_SENSOR_LABEL));
        units.add(getUnitConfig(UnitTemplate.UnitType.BATTERY, BATTERY_LABEL));
        units.add(getUnitConfig(UnitTemplate.UnitType.BRIGHTNESS_SENSOR, BRIGHTNESS_SENSOR_LABEL));
        units.add(getUnitConfig(UnitTemplate.UnitType.TEMPERATURE_SENSOR, TEMPERATURE_SENSOR_LABEL));
        units.add(getUnitConfig(UnitTemplate.UnitType.TAMPER_SWITCH, TAMPER_SWITCH_LABEL));
        deviceRemote.registerDeviceConfig(getDeviceConfig("F_MotionSensor_Device", serialNumber, motionSensorClass, units));

        units.clear();
        // button
        DeviceClass buttonClass = deviceRemote.registerDeviceClass(getDeviceClass("GI_5133"));
        units.add(getUnitConfig(UnitTemplate.UnitType.BUTTON, BUTTON_LABEL));
        deviceRemote.registerDeviceConfig(getDeviceConfig("GI_5133_Device", serialNumber, buttonClass, units));

        units.clear();
        // dimmer
        DeviceClass dimmerClass = deviceRemote.registerDeviceClass(DeviceClass.newBuilder().setLabel("HA_TYA663A").build());
        units.add(getUnitConfig(UnitTemplate.UnitType.DIMMER, DIMMER_LABEL));
        deviceRemote.registerDeviceConfig(getDeviceConfig("HA_TYA663A_Device", serialNumber, dimmerClass, units));

        units.clear();
        // handle
        DeviceClass handleClass = deviceRemote.registerDeviceClass(getDeviceClass("HM_RotaryHandleSensor"));
        units.add(getUnitConfig(UnitTemplate.UnitType.HANDLE_SENSOR, HANDLE_SENSOR_LABEL));
        deviceRemote.registerDeviceConfig(getDeviceConfig("HM_RotaryHandleSensor_Device", serialNumber, handleClass, units));

        units.clear();
        // light 
        DeviceClass lightClass = deviceRemote.registerDeviceClass(getDeviceClass("F_FGS221"));
        units.add(getUnitConfig(UnitTemplate.UnitType.LIGHT, LIGHT_LABEL));
        deviceRemote.registerDeviceConfig(getDeviceConfig("F_FGS221_Device", serialNumber, lightClass, units));

        units.clear();
        // powerConsumptionSensor, powerPlug 
        DeviceClass powerPlugClass = deviceRemote.registerDeviceClass(getDeviceClass("PW_PowerPlug"));
        units.add(getUnitConfig(UnitTemplate.UnitType.POWER_PLUG, POWER_PLUG_LABEL));
        units.add(getUnitConfig(UnitTemplate.UnitType.POWER_CONSUMPTION_SENSOR, POWER_CONSUMPTION_LABEL));
        deviceRemote.registerDeviceConfig(getDeviceConfig("PW_PowerPlug_Device", serialNumber, powerPlugClass, units));

        units.clear();
        // reedSwitch 
        DeviceClass reedSwitchClass = deviceRemote.registerDeviceClass(getDeviceClass("HM_ReedSwitch"));
        units.add(getUnitConfig(UnitTemplate.UnitType.REED_SWITCH, REED_SWITCH_LABEL));
        deviceRemote.registerDeviceConfig(getDeviceConfig("HM_ReedSwitch_Device", serialNumber, reedSwitchClass, units));

        units.clear();
        // rollershutter 
        DeviceClass rollershutterClass = deviceRemote.registerDeviceClass(getDeviceClass("HA_TYA628C"));
        units.add(getUnitConfig(UnitTemplate.UnitType.ROLLERSHUTTER, ROLLERSHUTTER_LABEL));
        deviceRemote.registerDeviceConfig(getDeviceConfig("HA_TYA628C_Device", serialNumber, rollershutterClass, units));
    }

    private PlacementConfig getDefaultPlacement() {
        Rotation rotation = Rotation.newBuilder().setQw(1).setQx(0).setQy(0).setQz(0).build();
        Translation translation = Translation.newBuilder().setX(0).setY(0).setZ(0).build();
        Pose pose = Pose.newBuilder().setRotation(rotation).setTranslation(translation).build();
        return PlacementConfig.newBuilder().setPosition(pose).setLocation(paradise).build();
    }

    private UnitConfig getUnitConfig(UnitTemplate.UnitType type, String label) {
        return UnitConfig.newBuilder().setPlacement(getDefaultPlacement()).setTemplate(UnitTemplate.newBuilder().setType(type).build()).setLabel(label).build();
    }

    private DeviceConfig getDeviceConfig(String label, String serialNumber, DeviceClass clazz, ArrayList<UnitConfig> units) {
        return DeviceConfig.newBuilder().setPlacementConfig(getDefaultPlacement()).setLabel(label).setSerialNumber(serialNumber).setDeviceClass(clazz).addAllUnitConfig(units).build();
    }

    private DeviceClass getDeviceClass(String label) {
        return DeviceClass.newBuilder().setLabel(label).build();
    }

    public LocationConfig getLocation() {
        return paradise;
    }
}
