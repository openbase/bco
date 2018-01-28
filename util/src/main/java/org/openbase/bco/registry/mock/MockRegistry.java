package org.openbase.bco.registry.mock;

/*
 * #%L
 * BCO Registry Utility
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import org.openbase.bco.authentication.core.AuthenticatorController;
import org.openbase.bco.authentication.core.AuthenticatorLauncher;
import org.openbase.bco.authentication.lib.AuthenticatedServerManager;
import org.openbase.bco.authentication.lib.SessionManager;
import org.openbase.bco.registry.agent.core.AgentRegistryLauncher;
import org.openbase.bco.registry.agent.lib.AgentRegistry;
import org.openbase.bco.registry.agent.remote.CachedAgentRegistryRemote;
import org.openbase.bco.registry.app.core.AppRegistryLauncher;
import org.openbase.bco.registry.app.lib.AppRegistry;
import org.openbase.bco.registry.app.remote.CachedAppRegistryRemote;
import org.openbase.bco.registry.device.core.DeviceRegistryLauncher;
import org.openbase.bco.registry.device.lib.DeviceRegistry;
import org.openbase.bco.registry.device.remote.CachedDeviceRegistryRemote;
import org.openbase.bco.registry.location.core.LocationRegistryLauncher;
import org.openbase.bco.registry.location.lib.LocationRegistry;
import org.openbase.bco.registry.location.remote.CachedLocationRegistryRemote;
import static org.openbase.bco.registry.mock.MockRegistry.MockServiceDescription.ACTIVATION_SOS;
import static org.openbase.bco.registry.mock.MockRegistry.MockServiceDescription.ACTIVATION_SPS;
import static org.openbase.bco.registry.mock.MockRegistry.MockServiceDescription.BATTERY_SPS;
import static org.openbase.bco.registry.mock.MockRegistry.MockServiceDescription.BLIND_SOS;
import static org.openbase.bco.registry.mock.MockRegistry.MockServiceDescription.BLIND_SPS;
import static org.openbase.bco.registry.mock.MockRegistry.MockServiceDescription.BRIGHTNESS_SOS;
import static org.openbase.bco.registry.mock.MockRegistry.MockServiceDescription.BRIGHTNESS_SPS;
import static org.openbase.bco.registry.mock.MockRegistry.MockServiceDescription.BUTTON_SPS;
import static org.openbase.bco.registry.mock.MockRegistry.MockServiceDescription.COLOR_SOS;
import static org.openbase.bco.registry.mock.MockRegistry.MockServiceDescription.COLOR_SPS;
import static org.openbase.bco.registry.mock.MockRegistry.MockServiceDescription.CONTACT_SPS;
import static org.openbase.bco.registry.mock.MockRegistry.MockServiceDescription.HANDLE_SPS;
import static org.openbase.bco.registry.mock.MockRegistry.MockServiceDescription.ILLUMINANCE_SPS;
import static org.openbase.bco.registry.mock.MockRegistry.MockServiceDescription.MOTION_SPS;
import static org.openbase.bco.registry.mock.MockRegistry.MockServiceDescription.POWER_CONSUMPTION_SPS;
import static org.openbase.bco.registry.mock.MockRegistry.MockServiceDescription.POWER_SOS;
import static org.openbase.bco.registry.mock.MockRegistry.MockServiceDescription.POWER_SPS;
import static org.openbase.bco.registry.mock.MockRegistry.MockServiceDescription.SMOKE_ALARM_SPS;
import static org.openbase.bco.registry.mock.MockRegistry.MockServiceDescription.SMOKE_SPS;
import static org.openbase.bco.registry.mock.MockRegistry.MockServiceDescription.STANDBY_SOS;
import static org.openbase.bco.registry.mock.MockRegistry.MockServiceDescription.STANDBY_SPS;
import static org.openbase.bco.registry.mock.MockRegistry.MockServiceDescription.TAMPER_SPS;
import static org.openbase.bco.registry.mock.MockRegistry.MockServiceDescription.TARGET_TEMPERATURE_SOS;
import static org.openbase.bco.registry.mock.MockRegistry.MockServiceDescription.TARGET_TEMPERATURE_SPS;
import static org.openbase.bco.registry.mock.MockRegistry.MockServiceDescription.TEMPERATURE_SPS;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.bco.registry.scene.core.SceneRegistryLauncher;
import org.openbase.bco.registry.scene.lib.SceneRegistry;
import org.openbase.bco.registry.scene.remote.CachedSceneRegistryRemote;
import org.openbase.bco.registry.unit.core.UnitRegistryLauncher;
import org.openbase.bco.registry.unit.core.plugin.UserCreationPlugin;
import org.openbase.bco.registry.unit.lib.UnitRegistry;
import org.openbase.bco.registry.unit.remote.CachedUnitRegistryRemote;
import org.openbase.bco.registry.user.activity.core.UserActivityRegistryLauncher;
import org.openbase.bco.registry.user.activity.lib.UserActivityRegistry;
import org.openbase.bco.registry.user.activity.remote.CachedUserActivityRegistryRemote;
import org.openbase.bco.registry.user.core.UserRegistryLauncher;
import org.openbase.bco.registry.user.lib.UserRegistry;
import org.openbase.bco.registry.user.remote.CachedUserRegistryRemote;
import org.openbase.jps.core.JPService;
import org.openbase.jps.exception.JPServiceException;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.exception.printer.LogLevel;
import org.openbase.jul.pattern.Observable;
import org.openbase.jul.pattern.Observer;
import org.openbase.jul.schedule.GlobalCachedExecutorService;
import org.openbase.jul.schedule.SyncObject;
import org.slf4j.LoggerFactory;
import rst.domotic.authentication.PermissionConfigType.PermissionConfig;
import rst.domotic.binding.BindingConfigType.BindingConfig;
import rst.domotic.service.ServiceConfigType;
import rst.domotic.service.ServiceConfigType.ServiceConfig;
import rst.domotic.service.ServiceDescriptionType.ServiceDescription;
import rst.domotic.service.ServiceTemplateConfigType.ServiceTemplateConfig;
import rst.domotic.service.ServiceTemplateType.ServiceTemplate.ServicePattern;
import rst.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import rst.domotic.state.EnablingStateType.EnablingState;
import rst.domotic.state.InventoryStateType;
import rst.domotic.state.InventoryStateType.InventoryState;
import rst.domotic.unit.UnitConfigType.UnitConfig;
import rst.domotic.unit.UnitTemplateConfigType.UnitTemplateConfig;
import rst.domotic.unit.UnitTemplateType.UnitTemplate;
import rst.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;
import rst.domotic.unit.agent.AgentClassType.AgentClass;
import rst.domotic.unit.connection.ConnectionConfigType.ConnectionConfig;
import rst.domotic.unit.device.DeviceClassType.DeviceClass;
import rst.domotic.unit.device.DeviceConfigType.DeviceConfig;
import rst.domotic.unit.location.LocationConfigType.LocationConfig;
import rst.domotic.unit.location.LocationConfigType.LocationConfig.LocationType;
import rst.domotic.unit.user.UserConfigType.UserConfig;
import rst.geometry.AxisAlignedBoundingBox3DFloatType.AxisAlignedBoundingBox3DFloat;
import rst.geometry.PoseType.Pose;
import rst.geometry.RotationType.Rotation;
import rst.geometry.TranslationType.Translation;
import rst.math.Vec3DDoubleType.Vec3DDouble;
import rst.spatial.PlacementConfigType.PlacementConfig;
import rst.spatial.ShapeType.Shape;

/**
 *
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class MockRegistry {

    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(MockRegistry.class);

    public static final String USER_NAME = "uSeRnAmE";
    public static UnitConfig testUser;
    public static UnitConfig admin;
    public static String adminPassword = UserCreationPlugin.DEFAULT_ADMIN_USERNAME_AND_PASSWORD;

    public static final String COLORABLE_LIGHT_LABEL = "Colorable_Light_Unit_Test";
    public static final String BATTERY_LABEL = "Battery_Unit_Test";
    public static final String BUTTON_LABEL = "Button_Unit_Test";
    public static final String DIMMABLE_LIGHT_LABEL = "DimmableLight_Unit_Test";
    public static final String DIMMER_LABEL = "Dimmer_Unit_Test";
    public static final String HANDLE_LABEL = "Handle_Sensor_Unit_Test";
    public static final String LIGHT_LABEL = "Light_Unit_Test";
    public static final String MOTION_DETECTOR_LABEL = "Motion_Sensor_Unit_Test";
    public static final String POWER_CONSUMPTION_LABEL = "Power_Consumption_Sensor_Unit_Test";
    public static final String POWER_SWITCH_LABEL = "Power_Plug_Unit_Test";
    public static final String REED_CONTACT_LABEL = "Reed_Switch_Unit_Test";
    public static final String ROLLER_SHUTTER_LABEL = "Rollershutter_Unit_Test";
    public static final String TAMPER_DETECTOR_LABEL = "Tamper_Switch_Unit_Test";
    public static final String TEMPERATURE_SENSOR_LABEL = "Temperature_Sensor_Unit_Test";
    public static final String TEMPERATURE_CONTROLLER_LABEL = "Temperature_Controller_Unit_Test";
    public static final String SMOKE_DETECTOR_LABEL = "Smoke_Detector_Unit_Test";
    public static final String LIGHT_SENSOR_LABEL = "Light_Sensor_Unit_Test";
    private final String serialNumber = "1234-5678-9100";

    public static final String ABSENCE_ENERGY_SAVING_AGENT_LABEL = "AbsenceEnergySaving";
    public static final String HEATER_ENERGY_SAVING_AGENT_LABEL = "HeaterEnergySaving";
    public static final String ILLUMINATION_LIGHT_SAVING_AGENT_LABEL = "IlluminationLightSaving";
    public static final String POWER_STATE_SYNCHRONISER_AGENT_LABEL = "PowerStateSynchroniser";
    public static final String PRESENCE_LIGHT_AGENT_LABEL = "PresenceLight";

    public static final AxisAlignedBoundingBox3DFloat DEFAULT_BOUNDING_BOX = AxisAlignedBoundingBox3DFloat.newBuilder()
            .setHeight(10)
            .setWidth(20)
            .setDepth(30)
            .setLeftFrontBottom(Translation.newBuilder().setX(0).setY(0).setZ(0).build())
            .build();

    private static AuthenticatorLauncher authenticatorLauncher;
    private static AuthenticatorController authenticatorController;

    private static DeviceRegistryLauncher deviceRegistryLauncher;
    private static LocationRegistryLauncher locationRegistryLauncher;
    private static AgentRegistryLauncher agentRegistryLauncher;
    private static AppRegistryLauncher appRegistryLauncher;
    private static SceneRegistryLauncher sceneRegistryLauncher;
    private static UserRegistryLauncher userRegistryLauncher;
    private static UnitRegistryLauncher unitRegistryLauncher;
    private static UserActivityRegistryLauncher userActivityRegistryLauncher;

    private static DeviceRegistry deviceRegistry;
    private static LocationRegistry locationRegistry;
    private static AgentRegistry agentRegistry;
    private static AppRegistry appRegistry;
    private static SceneRegistry sceneRegistry;
    private static UserRegistry userRegisty;
    private static UnitRegistry unitRegistry;
    private static UserActivityRegistry userActivityRegistry;

    private static UnitConfig paradiseLocation;
    private static UnitConfig hellLocation;
    private static UnitConfig heavenLocation;
    private static UnitConfig stairwayLocation;

    public static final Map<UnitType, String> UNIT_TYPE_LABEL_MAP = new HashMap<>();

    public enum MockServiceDescription {

        // endings:
        // SOS = STATE_OPERATION_SERVICE
        // SPS = STATE_PROVIDER_SERVICE
        // SCS = STATE_CONSUMER_SERVICE
        ACTIVATION_SPS(ServiceType.ACTIVATION_STATE_SERVICE, ServicePattern.PROVIDER),
        ACTIVATION_SOS(ServiceType.ACTIVATION_STATE_SERVICE, ServicePattern.OPERATION),
        BATTERY_SPS(ServiceType.BATTERY_STATE_SERVICE, ServicePattern.PROVIDER),
        BLIND_SOS(ServiceType.BLIND_STATE_SERVICE, ServicePattern.OPERATION),
        BLIND_SPS(ServiceType.BLIND_STATE_SERVICE, ServicePattern.PROVIDER),
        BRIGHTNESS_SOS(ServiceType.BRIGHTNESS_STATE_SERVICE, ServicePattern.OPERATION),
        BRIGHTNESS_SPS(ServiceType.BRIGHTNESS_STATE_SERVICE, ServicePattern.PROVIDER),
        ILLUMINANCE_SPS(ServiceType.ILLUMINANCE_STATE_SERVICE, ServicePattern.PROVIDER),
        BUTTON_SPS(ServiceType.BUTTON_STATE_SERVICE, ServicePattern.PROVIDER),
        COLOR_SOS(ServiceType.COLOR_STATE_SERVICE, ServicePattern.OPERATION),
        COLOR_SPS(ServiceType.COLOR_STATE_SERVICE, ServicePattern.PROVIDER),
        CONTACT_SPS(ServiceType.CONTACT_STATE_SERVICE, ServicePattern.PROVIDER),
        HANDLE_SPS(ServiceType.HANDLE_STATE_SERVICE, ServicePattern.PROVIDER),
        MOTION_SPS(ServiceType.MOTION_STATE_SERVICE, ServicePattern.PROVIDER),
        POWER_CONSUMPTION_SPS(ServiceType.POWER_CONSUMPTION_STATE_SERVICE, ServicePattern.PROVIDER),
        POWER_SOS(ServiceType.POWER_STATE_SERVICE, ServicePattern.OPERATION),
        POWER_SPS(ServiceType.POWER_STATE_SERVICE, ServicePattern.PROVIDER),
        SMOKE_ALARM_SPS(ServiceType.SMOKE_ALARM_STATE_SERVICE, ServicePattern.PROVIDER),
        SMOKE_SPS(ServiceType.SMOKE_STATE_SERVICE, ServicePattern.PROVIDER),
        STANDBY_SPS(ServiceType.STANDBY_STATE_SERVICE, ServicePattern.PROVIDER),
        STANDBY_SOS(ServiceType.STANDBY_STATE_SERVICE, ServicePattern.OPERATION),
        TAMPER_SPS(ServiceType.TAMPER_STATE_SERVICE, ServicePattern.PROVIDER),
        TARGET_TEMPERATURE_SOS(ServiceType.TARGET_TEMPERATURE_STATE_SERVICE, ServicePattern.OPERATION),
        TARGET_TEMPERATURE_SPS(ServiceType.TARGET_TEMPERATURE_STATE_SERVICE, ServicePattern.PROVIDER),
        TEMPERATURE_SPS(ServiceType.TEMPERATURE_STATE_SERVICE, ServicePattern.PROVIDER);

        private final ServiceDescription description;

        MockServiceDescription(ServiceType type, ServicePattern servicePattern) {
            ServiceDescription.Builder descriptionBuilder = ServiceDescription.newBuilder();
            descriptionBuilder.setType(type);
            descriptionBuilder.setPattern(servicePattern);
            this.description = descriptionBuilder.build();
        }

        public ServiceDescription getDescription() {
            return description;
        }
    }

    public enum MockUnitTemplate {

        COLORABLE_LIGHT(UnitType.COLORABLE_LIGHT, COLOR_SOS, COLOR_SPS, POWER_SOS, POWER_SPS, BRIGHTNESS_SOS, BRIGHTNESS_SPS),
        DIMMABLE_LIGHT(UnitType.DIMMABLE_LIGHT, POWER_SOS, POWER_SPS, BRIGHTNESS_SOS, BRIGHTNESS_SPS),
        LIGHT(UnitType.LIGHT, POWER_SOS, POWER_SPS),
        MOTION_DETECTOR(UnitType.MOTION_DETECTOR, MOTION_SPS),
        LIGHT_SENSOR(UnitType.LIGHT_SENSOR, ILLUMINANCE_SPS),
        BUTTON(UnitType.BUTTON, BUTTON_SPS),
        DIMMER(UnitType.DIMMER, BRIGHTNESS_SOS, BRIGHTNESS_SPS, POWER_SOS, POWER_SPS),
        HANDLE(UnitType.HANDLE, HANDLE_SPS),
        POWER_CONSUMPTION_SENSOR(UnitType.POWER_CONSUMPTION_SENSOR, POWER_CONSUMPTION_SPS),
        POWER_SOURCE(UnitType.POWER_SWITCH, POWER_SOS, POWER_SPS),
        REED_CONTACT(UnitType.REED_CONTACT, CONTACT_SPS),
        ROLLER_SHUTTER(UnitType.ROLLER_SHUTTER, BLIND_SOS, BLIND_SPS),
        TAMPER_DETECTOR(UnitType.TAMPER_DETECTOR, TAMPER_SPS),
        TEMPERATURE_CONTROLLER(UnitType.TEMPERATURE_CONTROLLER, TARGET_TEMPERATURE_SOS, TARGET_TEMPERATURE_SPS, TEMPERATURE_SPS),
        SMOKE_DETECTOR_CONTROLLER(UnitType.SMOKE_DETECTOR, SMOKE_SPS, SMOKE_ALARM_SPS),
        TEMPERATURE_SENSOR(UnitType.TEMPERATURE_SENSOR, TEMPERATURE_SPS),
        BATTERY(UnitType.BATTERY, BATTERY_SPS),
        LOCATION(UnitType.LOCATION, COLOR_SPS, COLOR_SOS, ILLUMINANCE_SPS, MOTION_SPS, POWER_CONSUMPTION_SPS, POWER_SPS, POWER_SOS, BLIND_SPS, BLIND_SOS,
                SMOKE_ALARM_SPS, SMOKE_SPS, STANDBY_SPS, STANDBY_SOS, TAMPER_SPS, TARGET_TEMPERATURE_SPS, TARGET_TEMPERATURE_SOS, TEMPERATURE_SPS),
        CONNECTION(UnitType.CONNECTION),
        SCENE(UnitType.SCENE, ACTIVATION_SPS, ACTIVATION_SOS),
        AGENT(UnitType.AGENT, ACTIVATION_SPS, ACTIVATION_SOS),
        APP(UnitType.APP, ACTIVATION_SPS, ACTIVATION_SOS),
        UNIT_GROUP(UnitType.UNIT_GROUP, COLOR_SPS, COLOR_SOS, POWER_SPS, POWER_SOS);

        private final UnitTemplate template;

        MockUnitTemplate(UnitType type, MockServiceDescription... serviceTemplates) {
            UnitTemplate.Builder templateBuilder = UnitTemplate.newBuilder();
            templateBuilder.setType(type);
            for (MockServiceDescription serviceTemplate : serviceTemplates) {
                templateBuilder.addServiceDescription(serviceTemplate.getDescription());
            }

            switch (type) {
                case COLORABLE_LIGHT:
                    templateBuilder.addIncludedType(UnitType.DIMMABLE_LIGHT);
                    break;
                case DIMMABLE_LIGHT:
                    templateBuilder.addIncludedType(UnitType.LIGHT);
                    break;
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

    protected MockRegistry() throws InstantiationException {
        if (UNIT_TYPE_LABEL_MAP.isEmpty()) {
            UNIT_TYPE_LABEL_MAP.put(UnitType.COLORABLE_LIGHT, COLORABLE_LIGHT_LABEL);
            UNIT_TYPE_LABEL_MAP.put(UnitType.LIGHT, LIGHT_LABEL);
            UNIT_TYPE_LABEL_MAP.put(UnitType.MOTION_DETECTOR, MOTION_DETECTOR_LABEL);
            UNIT_TYPE_LABEL_MAP.put(UnitType.BUTTON, BUTTON_LABEL);
            UNIT_TYPE_LABEL_MAP.put(UnitType.DIMMABLE_LIGHT, DIMMABLE_LIGHT_LABEL);
            UNIT_TYPE_LABEL_MAP.put(UnitType.DIMMER, DIMMER_LABEL);
            UNIT_TYPE_LABEL_MAP.put(UnitType.HANDLE, HANDLE_LABEL);
            UNIT_TYPE_LABEL_MAP.put(UnitType.POWER_CONSUMPTION_SENSOR, POWER_CONSUMPTION_LABEL);
            UNIT_TYPE_LABEL_MAP.put(UnitType.POWER_SWITCH, POWER_SWITCH_LABEL);
            UNIT_TYPE_LABEL_MAP.put(UnitType.REED_CONTACT, REED_CONTACT_LABEL);
            UNIT_TYPE_LABEL_MAP.put(UnitType.ROLLER_SHUTTER, ROLLER_SHUTTER_LABEL);
            UNIT_TYPE_LABEL_MAP.put(UnitType.TAMPER_DETECTOR, TAMPER_DETECTOR_LABEL);
            UNIT_TYPE_LABEL_MAP.put(UnitType.TEMPERATURE_CONTROLLER, TEMPERATURE_CONTROLLER_LABEL);
            UNIT_TYPE_LABEL_MAP.put(UnitType.SMOKE_DETECTOR, SMOKE_DETECTOR_LABEL);
            UNIT_TYPE_LABEL_MAP.put(UnitType.TEMPERATURE_SENSOR, TEMPERATURE_SENSOR_LABEL);
            UNIT_TYPE_LABEL_MAP.put(UnitType.BATTERY, BATTERY_LABEL);
            UNIT_TYPE_LABEL_MAP.put(UnitType.LIGHT_SENSOR, LIGHT_SENSOR_LABEL);
        }

        try {
            JPService.setupJUnitTestMode();
            List<Future<Void>> registryStartupTasks = new ArrayList<>();
            registryStartupTasks.add(GlobalCachedExecutorService.submit(() -> {
                try {
                    authenticatorLauncher = new AuthenticatorLauncher();
                    authenticatorLauncher.launch();
                    authenticatorController = authenticatorLauncher.getLaunchable();
                    authenticatorController.waitForActivation();
                } catch (CouldNotPerformException ex) {
                    throw ExceptionPrinter.printHistoryAndReturnThrowable(ex, LOGGER, LogLevel.ERROR);
                }
                return null;
            }));
            LOGGER.info("Starting authenticator...");
            for (Future<Void> task : registryStartupTasks) {
                task.get();
            }
            registryStartupTasks.clear();

            registryStartupTasks.add(GlobalCachedExecutorService.submit(() -> {
                try {
                    unitRegistryLauncher = new UnitRegistryLauncher();
                    unitRegistryLauncher.launch();
                    unitRegistry = unitRegistryLauncher.getLaunchable();
                } catch (CouldNotPerformException ex) {
                    throw ExceptionPrinter.printHistoryAndReturnThrowable(ex, LOGGER, LogLevel.ERROR);
                }
                return null;
            }));
            registryStartupTasks.add(GlobalCachedExecutorService.submit(() -> {
                try {
                    deviceRegistryLauncher = new DeviceRegistryLauncher();
                    deviceRegistryLauncher.launch();
                    deviceRegistry = deviceRegistryLauncher.getLaunchable();
                } catch (CouldNotPerformException | InterruptedException ex) {
                    throw ExceptionPrinter.printHistoryAndReturnThrowable(ex, LOGGER, LogLevel.ERROR);
                }
                return null;
//                    throw new CouldNotPerformException("Bad case!");
            }));
            registryStartupTasks.add(GlobalCachedExecutorService.submit(() -> {
                try {
                    agentRegistryLauncher = new AgentRegistryLauncher();
                    agentRegistryLauncher.launch();
                    agentRegistry = agentRegistryLauncher.getLaunchable();
                } catch (CouldNotPerformException ex) {
                    throw ExceptionPrinter.printHistoryAndReturnThrowable(ex, LOGGER, LogLevel.ERROR);
                }
                return null;
            }));
            registryStartupTasks.add(GlobalCachedExecutorService.submit(() -> {
                try {
                    appRegistryLauncher = new AppRegistryLauncher();
                    appRegistryLauncher.launch();
                    appRegistry = appRegistryLauncher.getLaunchable();
                } catch (CouldNotPerformException ex) {
                    throw ExceptionPrinter.printHistoryAndReturnThrowable(ex, LOGGER, LogLevel.ERROR);
                }
                return null;
            }));
            registryStartupTasks.add(GlobalCachedExecutorService.submit(() -> {
                try {
                    userActivityRegistryLauncher = new UserActivityRegistryLauncher();
                    userActivityRegistryLauncher.launch();
                    userActivityRegistry = userActivityRegistryLauncher.getLaunchable();
                } catch (CouldNotPerformException ex) {
                    throw ExceptionPrinter.printHistoryAndReturnThrowable(ex, LOGGER, LogLevel.ERROR);
                }
                return null;
            }));
            LOGGER.info("Starting all real registries: unit, device, agent, app, user-activity ...");
            for (Future<Void> task : registryStartupTasks) {
                task.get();
            }
            registryStartupTasks.clear();

            LOGGER.info("Real registries started!");

            registryStartupTasks.add(GlobalCachedExecutorService.submit(() -> {
                try {
                    locationRegistryLauncher = new LocationRegistryLauncher();
                    locationRegistryLauncher.launch();
                    locationRegistry = locationRegistryLauncher.getLaunchable();
                } catch (CouldNotPerformException ex) {
                    throw ExceptionPrinter.printHistoryAndReturnThrowable(ex, LOGGER, LogLevel.ERROR);
                }
                return null;
            }));
            registryStartupTasks.add(GlobalCachedExecutorService.submit(() -> {
                try {
                    userRegistryLauncher = new UserRegistryLauncher();
                    userRegistryLauncher.launch();
                    userRegisty = userRegistryLauncher.getLaunchable();
                } catch (CouldNotPerformException | InterruptedException ex) {
                    throw ExceptionPrinter.printHistoryAndReturnThrowable(ex, LOGGER, LogLevel.ERROR);
                }
                return null;
            }));
            registryStartupTasks.add(GlobalCachedExecutorService.submit(() -> {
                try {
                    sceneRegistryLauncher = new SceneRegistryLauncher();
                    sceneRegistryLauncher.launch();
                    sceneRegistry = sceneRegistryLauncher.getLaunchable();
                } catch (CouldNotPerformException ex) {
                    throw ExceptionPrinter.printHistoryAndReturnThrowable(ex, LOGGER, LogLevel.ERROR);
                }
                return null;
            }));
            LOGGER.info("Waiting for purely virtual registries: location, user, scene ...");
            for (Future<Void> task : registryStartupTasks) {
                task.get();
            }
            registryStartupTasks.clear();
            LOGGER.info("Virtual registries started!");

            LOGGER.info("Reinitialize remotes...");
            Registries.reinitialize();
            LOGGER.info("Reinitialized remotes!");
            Registries.waitForData();

            registryStartupTasks.add(GlobalCachedExecutorService.submit(() -> {
                try {
                    LOGGER.info("Update unitTemplates...");
                    // load templates
                    for (MockUnitTemplate template : MockUnitTemplate.values()) {
                        String unitTemplateId = unitRegistry.getUnitTemplateByType(template.getTemplate().getType()).getId();
                        unitRegistry.updateUnitTemplate(template.getTemplate().toBuilder().setId(unitTemplateId).build()).get();
                    }

                    LOGGER.info("Register user...");
                    registerUser();

                    LOGGER.info("Register agentClasses...");
                    registerAgentClasses();

                    LOGGER.info("Register locations...");
                    registerLocations();
                    LOGGER.info("Wait until registry is ready...");
                    Registries.waitUntilReady();

                    LOGGER.info("Register devices...");
                    registerDevices();
                    LOGGER.info("Wait until registry is ready...");
                    Registries.waitUntilReady();

                    LOGGER.info("Register connections...");
                    registerConnections();

                    LOGGER.info("Wait for final consistency...");
                    Registries.waitUntilReady();

                } catch (CouldNotPerformException | InterruptedException ex) {
                    throw ExceptionPrinter.printHistoryAndReturnThrowable(ex, LOGGER, LogLevel.ERROR);
                }
                return null;
            }));

            LOGGER.info("Wait for unitTemplate updates; device, location and user registration...");
            for (Future<Void> task : registryStartupTasks) {
                task.get();
            }
            registryStartupTasks.clear();
            LOGGER.info("UnitTemplates updated and devices, locations, users and agentClasses registered!");
        } catch (JPServiceException | InterruptedException | ExecutionException | CouldNotPerformException ex) {
            shutdown();
            throw new InstantiationException(this, ex);
        }
    }

    protected void shutdown() {
        if (locationRegistryLauncher != null) {
            locationRegistryLauncher.shutdown();
        }

        if (sceneRegistryLauncher != null) {
            sceneRegistryLauncher.shutdown();
        }

        if (userRegistryLauncher != null) {
            userRegistryLauncher.shutdown();
        }

        if (deviceRegistryLauncher != null) {
            deviceRegistryLauncher.shutdown();
        }

        if (agentRegistryLauncher != null) {
            agentRegistryLauncher.shutdown();
        }

        if (appRegistryLauncher != null) {
            appRegistryLauncher.shutdown();
        }

        if (unitRegistryLauncher != null) {
            unitRegistryLauncher.shutdown();
        }

        if (authenticatorLauncher != null) {
            authenticatorLauncher.shutdown();
        }

        if (userActivityRegistryLauncher != null) {
            userActivityRegistryLauncher.shutdown();
        }

        SessionManager.getInstance().completeLogout();
        AuthenticatedServerManager.shutdown();

        CachedLocationRegistryRemote.shutdown();
        CachedSceneRegistryRemote.shutdown();
        CachedUserRegistryRemote.shutdown();

        CachedDeviceRegistryRemote.shutdown();
        CachedAgentRegistryRemote.shutdown();
        CachedAppRegistryRemote.shutdown();

        CachedUnitRegistryRemote.shutdown();

        CachedUserActivityRegistryRemote.shutdown();
    }

    private void registerLocations() throws CouldNotPerformException, InterruptedException {
        try {
            // Create paradise
            List<Vec3DDouble> paradiseVertices = new ArrayList<>();
            paradiseVertices.add(Vec3DDouble.newBuilder().setX(0).setY(0).setZ(0).build());
            paradiseVertices.add(Vec3DDouble.newBuilder().setX(0).setY(6).setZ(0).build());
            paradiseVertices.add(Vec3DDouble.newBuilder().setX(6).setY(6).setZ(0).build());
            paradiseVertices.add(Vec3DDouble.newBuilder().setX(6).setY(0).setZ(0).build());
            Shape paradiseShape = Shape.newBuilder().addAllFloor(paradiseVertices).build();
            PlacementConfig paradisePlacement = PlacementConfig.newBuilder().setShape(paradiseShape).build();

            // rename default root location home into paradise test location.
            paradiseLocation = locationRegistry.updateLocationConfig(locationRegistry.getRootLocationConfig().toBuilder().setLabel("Paradise").setPlacementConfig(paradisePlacement).build()).get();
            LocationConfig tileLocationConfig = LocationConfig.newBuilder().setType(LocationType.TILE).build();
            LocationConfig regionLocationConfig = LocationConfig.newBuilder().setType(LocationType.REGION).build();

            // Create hell
            List<Vec3DDouble> hellVertices = new ArrayList<>();
            hellVertices.add(Vec3DDouble.newBuilder().setX(0).setY(0).setZ(0).build());
            hellVertices.add(Vec3DDouble.newBuilder().setX(0).setY(4).setZ(0).build());
            hellVertices.add(Vec3DDouble.newBuilder().setX(2).setY(4).setZ(0).build());
            hellVertices.add(Vec3DDouble.newBuilder().setX(2).setY(0).setZ(0).build());
            Shape hellShape = Shape.newBuilder().addAllFloor(hellVertices).build();
            Pose hellPosition = Pose.newBuilder().setTranslation(Translation.newBuilder().setX(3).setY(1).setZ(0).build())
                    .setRotation(Rotation.newBuilder().setQw(1).setQx(0).setQy(0).setQz(0).build()).build();
            PlacementConfig hellPlacement = PlacementConfig.newBuilder().setPosition(hellPosition).setShape(hellShape).setLocationId(paradiseLocation.getId()).build();
            hellLocation = locationRegistry.registerLocationConfig(UnitConfig.newBuilder().setType(UnitType.LOCATION)
                    .setLabel("Hell").setLocationConfig(tileLocationConfig).setPlacementConfig(hellPlacement).build()).get();

            // Create stairway to heaven
            List<Vec3DDouble> stairwayVertices = new ArrayList<>();
            stairwayVertices.add(Vec3DDouble.newBuilder().setX(0).setY(0).setZ(0).build());
            stairwayVertices.add(Vec3DDouble.newBuilder().setX(0).setY(1).setZ(0).build());
            stairwayVertices.add(Vec3DDouble.newBuilder().setX(4).setY(1).setZ(0).build());
            stairwayVertices.add(Vec3DDouble.newBuilder().setX(4).setY(0).setZ(0).build());
            Shape stairwayShape = Shape.newBuilder().addAllFloor(stairwayVertices).build();
            Pose stairwayPosition = Pose.newBuilder().setTranslation(Translation.newBuilder().setX(1).setY(0).setZ(0).build())
                    .setRotation(Rotation.newBuilder().setQw(1).setQx(0).setQy(0).setQz(0).build()).build();
            PlacementConfig stairwayPlacement = PlacementConfig.newBuilder().setPosition(stairwayPosition).setShape(stairwayShape).setLocationId(paradiseLocation.getId()).build();
            stairwayLocation = locationRegistry.registerLocationConfig(UnitConfig.newBuilder().setType(UnitType.LOCATION)
                    .setLabel("Stairway to Heaven").setLocationConfig(tileLocationConfig).setPlacementConfig(stairwayPlacement).build()).get();

            // Create heaven
            List<Vec3DDouble> heavenVertices = new ArrayList<>();
            heavenVertices.add(Vec3DDouble.newBuilder().setX(0).setY(0).setZ(0).build());
            heavenVertices.add(Vec3DDouble.newBuilder().setX(0).setY(4).setZ(0).build());
            heavenVertices.add(Vec3DDouble.newBuilder().setX(2).setY(4).setZ(0).build());
            heavenVertices.add(Vec3DDouble.newBuilder().setX(2).setY(0).setZ(0).build());
            Shape heavenShape = Shape.newBuilder().addAllFloor(heavenVertices).build();
            Pose heavenPosition = Pose.newBuilder().setTranslation(Translation.newBuilder().setX(1).setY(1).setZ(0).build())
                    .setRotation(Rotation.newBuilder().setQw(1).setQx(0).setQy(0).setQz(0).build()).build();
            PlacementConfig heavenPlacement = PlacementConfig.newBuilder().setPosition(heavenPosition).setShape(heavenShape).setLocationId(paradiseLocation.getId()).build();
            heavenLocation = locationRegistry.registerLocationConfig(UnitConfig.newBuilder().setType(UnitType.LOCATION)
                    .setLabel("Heaven").setLocationConfig(tileLocationConfig).setPlacementConfig(heavenPlacement).build()).get();

            // Create Garden of Eden
            List<Vec3DDouble> edenVertices = new ArrayList<>();
            edenVertices.add(Vec3DDouble.newBuilder().setX(0).setY(0).setZ(0).build());
            edenVertices.add(Vec3DDouble.newBuilder().setX(0).setY(2).setZ(0).build());
            edenVertices.add(Vec3DDouble.newBuilder().setX(1).setY(2).setZ(0).build());
            edenVertices.add(Vec3DDouble.newBuilder().setX(1).setY(0).setZ(0).build());
            Shape edenShape = Shape.newBuilder().addAllFloor(edenVertices).build();
            Pose edenPosition = Pose.newBuilder().setTranslation(Translation.newBuilder().setX(0).setY(2).setZ(0).build())
                    .setRotation(Rotation.newBuilder().setQw(1).setQx(0).setQy(0).setQz(0).build()).build();
            PlacementConfig edenPlacement = PlacementConfig.newBuilder().setPosition(edenPosition).setShape(edenShape).setLocationId(heavenLocation.getId()).build();
            locationRegistry.registerLocationConfig(UnitConfig.newBuilder().setType(UnitType.LOCATION)
                    .setLabel("Garden of Eden").setLocationConfig(regionLocationConfig).setPlacementConfig(edenPlacement).build()).get();

        } catch (ExecutionException ex) {
            throw new CouldNotPerformException(ex);
        }
    }

    private void registerConnections() throws CouldNotPerformException, InterruptedException {
        try {
            List<String> tileIds = new ArrayList<>();
            tileIds.add(heavenLocation.getId());
            tileIds.add(hellLocation.getId());
            String reedContactId = Registries.getUnitRegistry().getUnitConfigsByLabel(REED_CONTACT_LABEL).get(0).getId();
            ConnectionConfig connectionConfig = ConnectionConfig.newBuilder().setType(ConnectionConfig.ConnectionType.DOOR).addAllTileId(tileIds).addUnitId(reedContactId).build();
            locationRegistry.registerConnectionConfig(UnitConfig.newBuilder().setType(UnitType.CONNECTION).setLabel("Gate").setConnectionConfig(connectionConfig).build()).get();

            tileIds.clear();
            tileIds.add(heavenLocation.getId());
            tileIds.add(stairwayLocation.getId());
            reedContactId = Registries.getUnitRegistry().getUnitConfigsByLabelAndUnitType("Reed_Heaven_Stairs", UnitType.REED_CONTACT).get(0).getId();
            connectionConfig = ConnectionConfig.newBuilder().setType(ConnectionConfig.ConnectionType.DOOR).addAllTileId(tileIds).addUnitId(reedContactId).build();
            locationRegistry.registerConnectionConfig(UnitConfig.newBuilder().setType(UnitType.CONNECTION).setLabel("Stairs_Heaven_Gate").setConnectionConfig(connectionConfig).build()).get();

            tileIds.clear();
            tileIds.add(hellLocation.getId());
            tileIds.add(stairwayLocation.getId());
            reedContactId = Registries.getUnitRegistry().getUnitConfigsByLabelAndUnitType("Reed_Hell_Stairs", UnitType.REED_CONTACT).get(0).getId();
            connectionConfig = ConnectionConfig.newBuilder().setType(ConnectionConfig.ConnectionType.DOOR).addAllTileId(tileIds).addUnitId(reedContactId).build();
            locationRegistry.registerConnectionConfig(UnitConfig.newBuilder().setType(UnitType.CONNECTION).setLabel("Stairs_Hell_Gate").setConnectionConfig(connectionConfig).build()).get();

            tileIds.clear();
            tileIds.add(hellLocation.getId());
            tileIds.add(stairwayLocation.getId());
            reedContactId = Registries.getUnitRegistry().getUnitConfigsByLabelAndUnitType("Reed_Stairway_Window", UnitType.REED_CONTACT).get(0).getId();
            connectionConfig = ConnectionConfig.newBuilder().setType(ConnectionConfig.ConnectionType.WINDOW).addAllTileId(tileIds).addUnitId(reedContactId).build();
            locationRegistry.registerConnectionConfig(UnitConfig.newBuilder().setType(UnitType.CONNECTION).setLabel("Stairs_Hell_Lookout").setConnectionConfig(connectionConfig).build()).get();

        } catch (ExecutionException | IndexOutOfBoundsException ex) {
            throw new CouldNotPerformException(ex);
        }
    }

    private void registerUser() throws CouldNotPerformException, InterruptedException {
        for (UnitConfig unitConfig : unitRegistry.getUnitConfigs(UnitType.USER)) {
            if (unitConfig.getUserConfig().getUserName().equals(UserCreationPlugin.DEFAULT_ADMIN_USERNAME_AND_PASSWORD)) {
                admin = unitConfig;
                break;
            }
        }

        UserConfig.Builder config = UserConfig.newBuilder().setFirstName("Max").setLastName("Mustermann").setUserName(USER_NAME);
        UnitConfig.Builder userUnitConfig = UnitConfig.newBuilder().setType(UnitType.USER).setUserConfig(config).setEnablingState(EnablingState.newBuilder().setValue(EnablingState.State.ENABLED));
        userUnitConfig.getPermissionConfigBuilder().getOtherPermissionBuilder().setWrite(true).setAccess(true).setRead(true);
        try {
            testUser = userRegisty.registerUserConfig(userUnitConfig.build()).get();
        } catch (ExecutionException ex) {
            throw new CouldNotPerformException(ex);
        }
    }

    private void registerAgentClasses() throws CouldNotPerformException, InterruptedException {
        try {
            agentRegistry.registerAgentClass(AgentClass.newBuilder().setLabel(ABSENCE_ENERGY_SAVING_AGENT_LABEL).build()).get();
            agentRegistry.registerAgentClass(AgentClass.newBuilder().setLabel(HEATER_ENERGY_SAVING_AGENT_LABEL).build()).get();
            agentRegistry.registerAgentClass(AgentClass.newBuilder().setLabel(ILLUMINATION_LIGHT_SAVING_AGENT_LABEL).build()).get();
            agentRegistry.registerAgentClass(AgentClass.newBuilder().setLabel(POWER_STATE_SYNCHRONISER_AGENT_LABEL).build()).get();
            agentRegistry.registerAgentClass(AgentClass.newBuilder().setLabel(PRESENCE_LIGHT_AGENT_LABEL).build()).get();
        } catch (ExecutionException ex) {
            throw new CouldNotPerformException(ex);
        }
    }

    final SyncObject LOCK = new SyncObject("WaitForDeviceClassLock");
    final Observer notifyChangeObserver = (Observer) (Observable source, Object data) -> {
        synchronized (LOCK) {
            LOCK.notifyAll();
        }
    };

    private void registerDevices() throws CouldNotPerformException, InterruptedException {
        try {

            Registries.getDeviceRegistry(true).addDataObserver(notifyChangeObserver);
            // colorable light
            DeviceClass colorableLightClass = deviceRegistry.registerDeviceClass(getDeviceClass("Philips_Hue_E27", "KV01_18U", "Philips", UnitType.COLORABLE_LIGHT)).get();
            waitForDeviceClass(colorableLightClass);

            registerDeviceUnitConfig(getDeviceConfig("PH_Hue_E27_Device", serialNumber, colorableLightClass));

            registerDeviceUnitConfig(getDeviceConfig("PH_Hue_E27_Device_BORROWED", serialNumber, InventoryState.State.BORROWED, colorableLightClass));
            deviceRegistry.registerDeviceConfig(getDeviceConfig("PH_Hue_E27_Device_Stairway", serialNumber, colorableLightClass, stairwayLocation)).get();
            deviceRegistry.registerDeviceConfig(getDeviceConfig("PH_Hue_E27_Device_Heaven", serialNumber, colorableLightClass, stairwayLocation)).get();
            deviceRegistry.registerDeviceConfig(getDeviceConfig("PH_Hue_E27_Device_Hell", serialNumber, colorableLightClass, stairwayLocation)).get();

            // battery, brightnessSensor, motionSensor, tamperSwitch, temperatureSensor
            DeviceClass motionSensorClass = deviceRegistry.registerDeviceClass(getDeviceClass("Fibaro_MotionSensor", "FGMS_001", "Fibaro",
                    UnitType.MOTION_DETECTOR,
                    UnitType.BATTERY,
                    UnitType.LIGHT_SENSOR,
                    UnitType.TEMPERATURE_SENSOR,
                    UnitType.TAMPER_DETECTOR)).get();
            waitForDeviceClass(motionSensorClass);

            registerDeviceUnitConfig(getDeviceConfig("F_MotionSensor_Device", serialNumber, motionSensorClass));
            deviceRegistry.registerDeviceConfig(getDeviceConfig("F_MotionSensor_Device_Stairway", serialNumber, motionSensorClass, stairwayLocation)).get();
            deviceRegistry.registerDeviceConfig(getDeviceConfig("F_MotionSensor_Device_Heaven", serialNumber, motionSensorClass, heavenLocation)).get();
            deviceRegistry.registerDeviceConfig(getDeviceConfig("F_MotionSensor_Device_Hell", serialNumber, motionSensorClass, hellLocation)).get();

            // button
            DeviceClass buttonClass = deviceRegistry.registerDeviceClass(getDeviceClass("Gira_429496730210000", "429496730210000", "Gira",
                    UnitType.BUTTON)).get();
            waitForDeviceClass(buttonClass);

            registerDeviceUnitConfig(getDeviceConfig("GI_429496730210000_Device", serialNumber, buttonClass));

            // dimmableLight
            DeviceClass dimmableLightClass = deviceRegistry.registerDeviceClass(getDeviceClass("Hager_ABC", "ABC", "Hager",
                    UnitType.DIMMABLE_LIGHT)).get();
            waitForDeviceClass(dimmableLightClass);

            registerDeviceUnitConfig(getDeviceConfig("HA_ABC_Device", serialNumber, dimmableLightClass));

            // dimmer
            DeviceClass dimmerClass = deviceRegistry.registerDeviceClass(getDeviceClass("Hager_TYA663A", "TYA663A", "Hager",
                    UnitType.DIMMER)).get();
            waitForDeviceClass(dimmerClass);

            registerDeviceUnitConfig(getDeviceConfig("HA_TYA663A_Device", serialNumber, dimmerClass));

            // handle
            DeviceClass handleClass = deviceRegistry.registerDeviceClass(getDeviceClass("Homematic_RotaryHandleSensor", "Sec_RHS", "Homematic",
                    UnitType.HANDLE)).get();
            waitForDeviceClass(handleClass);

            registerDeviceUnitConfig(getDeviceConfig("HM_RotaryHandleSensor_Device", serialNumber, handleClass));

            // light
            DeviceClass lightClass = deviceRegistry.registerDeviceClass(getDeviceClass("Fibaro_FGS_221", "FGS_221", "Fibaro",
                    UnitType.LIGHT)).get();
            waitForDeviceClass(lightClass);

            registerDeviceUnitConfig(getDeviceConfig("F_FGS221_Device", serialNumber, lightClass));

            // powerConsumptionSensor, powerPlug
            DeviceClass powerPlugClass = deviceRegistry.registerDeviceClass(getDeviceClass("Plugwise_PowerPlug", "070140", "Plugwise",
                    UnitType.POWER_SWITCH,
                    UnitType.POWER_CONSUMPTION_SENSOR)).get();
            waitForDeviceClass(powerPlugClass);

            registerDeviceUnitConfig(getDeviceConfig("PW_PowerPlug_Device", serialNumber, powerPlugClass));

            // reedSwitch
            DeviceClass reedSwitchClass = deviceRegistry.registerDeviceClass(getDeviceClass("Homematic_ReedSwitch", "Sec_SC_2", "Homematic",
                    UnitType.REED_CONTACT)).get();
            waitForDeviceClass(reedSwitchClass);

            registerDeviceUnitConfig(getDeviceConfig("HM_ReedSwitch_Device", serialNumber, reedSwitchClass));
            deviceRegistry.registerDeviceConfig(getDeviceConfig("Reed_Heaven_Stairs", serialNumber, reedSwitchClass, stairwayLocation)).get();
            deviceRegistry.registerDeviceConfig(getDeviceConfig("Reed_Hell_Stairs", serialNumber, reedSwitchClass, stairwayLocation)).get();
            deviceRegistry.registerDeviceConfig(getDeviceConfig("Reed_Stairway_Window", serialNumber, reedSwitchClass, stairwayLocation)).get();

            // rollershutter
            DeviceClass rollershutterClass = deviceRegistry.registerDeviceClass(getDeviceClass("Hager_TYA628C", "TYA628C", "Hager",
                    UnitType.ROLLER_SHUTTER)).get();
            waitForDeviceClass(rollershutterClass);

            registerDeviceUnitConfig(getDeviceConfig("HA_TYA628C_Device", serialNumber, rollershutterClass));

            // smoke detector
            DeviceClass smokeDetector = deviceRegistry.registerDeviceClass(getDeviceClass("Fibaro_FGSS_001", "FGSS_001", "Fibaro",
                    UnitType.SMOKE_DETECTOR)).get();
            waitForDeviceClass(smokeDetector);

            registerDeviceUnitConfig(getDeviceConfig("Fibaro_SmokeDetector_Device", serialNumber, smokeDetector));

            // temperature controller
            DeviceClass temperatureControllerClass = deviceRegistry.registerDeviceClass(getDeviceClass("Gira_429496730250000", "429496730250000", "Gira",
                    UnitType.TEMPERATURE_CONTROLLER)).get();
            waitForDeviceClass(temperatureControllerClass);

            registerDeviceUnitConfig(getDeviceConfig("Gire_TemperatureController_Device", serialNumber, temperatureControllerClass));
            deviceRegistry.registerDeviceConfig(getDeviceConfig("Gire_TemperatureController_Device_Stairway", serialNumber, temperatureControllerClass, stairwayLocation)).get();

            Registries.getDeviceRegistry(true).removeDataObserver(notifyChangeObserver);
        } catch (ExecutionException ex) {
            throw new CouldNotPerformException(ex);
        }
    }

    public void waitForDeviceClass(final DeviceClass deviceClass) throws CouldNotPerformException {
        try {
            Registries.getDeviceRegistry(true).waitForData();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
        synchronized (LOCK) {
            try {
                while (!Registries.getDeviceRegistry(true).containsDeviceClass(deviceClass)) {
                    LOCK.wait();
                }
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private static void updateUnitLabel(final List<String> unitIds) throws CouldNotPerformException, InterruptedException, ExecutionException {
        for (String unitId : unitIds) {
            UnitConfig tmp = unitRegistry.getUnitConfigById(unitId);

            // ignore disabled test unit otherwise they would registered twice with the same label if another enabled instance of the same class exists.
            if (tmp.getEnablingState().getValue().equals(EnablingState.State.DISABLED)) {
                continue;
            }

            unitRegistry.updateUnitConfig(tmp.toBuilder().setLabel(UNIT_TYPE_LABEL_MAP.get(tmp.getType())).build()).get();
        }
    }

    /**
     * Registers the given device and updates the label to standard unit type label.
     *
     * @param deviceUnitConfig
     * @throws CouldNotPerformException
     * @throws InterruptedException
     * @throws ExecutionException
     */
    private static void registerDeviceUnitConfig(final UnitConfig deviceUnitConfig) throws CouldNotPerformException, InterruptedException, ExecutionException {
        UnitConfig tmp = deviceRegistry.registerDeviceConfig(deviceUnitConfig).get();
        updateUnitLabel(tmp.getDeviceConfig().getUnitIdList());
    }

    public static PlacementConfig getDefaultPlacement(UnitConfig location) {
        Rotation rotation = Rotation.newBuilder().setQw(1).setQx(0).setQy(0).setQz(0).build();
        Translation translation = Translation.newBuilder().setX(0).setY(0).setZ(0).build();
        Pose pose = Pose.newBuilder().setRotation(rotation).setTranslation(translation).build();
        return PlacementConfig.newBuilder().setPosition(pose).setLocationId(location.getId()).build();
    }

    public static Iterable<ServiceConfigType.ServiceConfig> getServiceConfig(final UnitTemplate template) {
        List<ServiceConfigType.ServiceConfig> serviceConfigList = new ArrayList<>();
        template.getServiceDescriptionList().stream().forEach((serviceDescription) -> {
            BindingConfig bindingServiceConfig = BindingConfig.newBuilder().setBindingId("OPENHAB").build();
            serviceConfigList.add(ServiceConfig.newBuilder().setServiceDescription(serviceDescription).setBindingConfig(bindingServiceConfig).build());
        });
        return serviceConfigList;
    }

    public static UnitConfig getDeviceConfig(String label, String serialNumber, DeviceClass clazz) {
        return getDeviceConfig(label, serialNumber, InventoryState.State.INSTALLED, clazz, paradiseLocation);
    }

    public static UnitConfig getDeviceConfig(String label, String serialNumber, DeviceClass clazz, UnitConfig location) {
        return getDeviceConfig(label, serialNumber, InventoryState.State.INSTALLED, clazz, location);
    }

    public static UnitConfig getDeviceConfig(String label, String serialNumber, InventoryState.State inventoryState, DeviceClass clazz) {
        return getDeviceConfig(label, serialNumber, inventoryState, clazz, paradiseLocation);
    }

    public static UnitConfig getDeviceConfig(String label, String serialNumber, InventoryState.State inventoryState, DeviceClass clazz, UnitConfig location) {
        DeviceConfig tmp = DeviceConfig.newBuilder()
                .setSerialNumber(serialNumber)
                .setDeviceClassId(clazz.getId())
                .setInventoryState(InventoryStateType.InventoryState.newBuilder().setValue(inventoryState))
                .build();
        return UnitConfig.newBuilder()
                .setPlacementConfig(getDefaultPlacement(location))
                .setLabel(label)
                .setDeviceConfig(tmp)
                .setType(UnitType.DEVICE)
                .build();
    }

    private static List<UnitTemplateConfig> getUnitTemplateConfigs(List<UnitTemplate.UnitType> unitTypes) throws CouldNotPerformException {
        List<UnitTemplateConfig> unitTemplateConfigs = new ArrayList<>();
        for (UnitTemplate.UnitType type : unitTypes) {
            Set<ServiceTemplateConfig> serviceTemplateConfigs = new HashSet<>();
            for (ServiceDescription serviceDescription : MockUnitTemplate.getTemplate(type).getServiceDescriptionList()) {
                serviceTemplateConfigs.add(ServiceTemplateConfig.newBuilder().setServiceType(serviceDescription.getType()).build());
            }
            UnitTemplateConfig config = UnitTemplateConfig.newBuilder().setType(type).addAllServiceTemplateConfig(serviceTemplateConfigs).build();
            unitTemplateConfigs.add(config);
        }
        return unitTemplateConfigs;
    }

    public static DeviceClass getDeviceClass(String label, String productNumber, String company, UnitTemplate.UnitType... types) throws CouldNotPerformException {
        List<UnitTemplate.UnitType> unitTypeList = new ArrayList<>();
        unitTypeList.addAll(Arrays.asList(types));
        return DeviceClass.newBuilder().setLabel(label).setProductNumber(productNumber).setCompany(company)
                .setBindingConfig(getBindingConfig()).addAllUnitTemplateConfig(getUnitTemplateConfigs(unitTypeList))
                .setShape(Shape.newBuilder().setBoundingBox(DEFAULT_BOUNDING_BOX))
                .build();
    }

    public static BindingConfig getBindingConfig() {
        BindingConfig.Builder bindingConfigBuilder = BindingConfig.newBuilder();
        bindingConfigBuilder.setBindingId("OPENHAB");
        return bindingConfigBuilder.build();
    }

    public static DeviceRegistry getDeviceRegistry() {
        return deviceRegistry;
    }

    public static UnitRegistry getUnitRegistry() {
        return unitRegistry;
    }

    public static LocationRegistry getLocationRegistry() {
        return locationRegistry;
    }
}
