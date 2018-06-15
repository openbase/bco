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

import org.openbase.bco.authentication.core.AuthenticatorController;
import org.openbase.bco.authentication.core.AuthenticatorLauncher;
import org.openbase.bco.authentication.lib.AuthenticatedServerManager;
import org.openbase.bco.authentication.lib.SessionManager;
import org.openbase.bco.registry.activity.core.ActivityRegistryLauncher;
import org.openbase.bco.registry.activity.lib.ActivityRegistry;
import org.openbase.bco.registry.activity.remote.CachedActivityRegistryRemote;
import org.openbase.bco.registry.clazz.core.ClassRegistryLauncher;
import org.openbase.bco.registry.clazz.lib.ClassRegistry;
import org.openbase.bco.registry.clazz.remote.CachedClassRegistryRemote;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.bco.registry.template.core.TemplateRegistryLauncher;
import org.openbase.bco.registry.template.lib.TemplateRegistry;
import org.openbase.bco.registry.template.remote.CachedTemplateRegistryRemote;
import org.openbase.bco.registry.unit.core.UnitRegistryLauncher;
import org.openbase.bco.registry.unit.core.plugin.UserCreationPlugin;
import org.openbase.bco.registry.unit.lib.UnitRegistry;
import org.openbase.bco.registry.unit.remote.CachedUnitRegistryRemote;
import org.openbase.jps.core.JPService;
import org.openbase.jps.exception.JPServiceException;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.exception.printer.LogLevel;
import org.openbase.jul.extension.rst.processing.LabelProcessor;
import org.openbase.jul.pattern.Observable;
import org.openbase.jul.pattern.Observer;
import org.openbase.jul.processing.StringProcessor;
import org.openbase.jul.schedule.GlobalCachedExecutorService;
import org.openbase.jul.schedule.SyncObject;
import org.slf4j.LoggerFactory;
import rst.domotic.binding.BindingConfigType.BindingConfig;
import rst.domotic.service.ServiceCommunicationTypeType.ServiceCommunicationType.CommunicationType;
import rst.domotic.service.ServiceConfigType;
import rst.domotic.service.ServiceConfigType.ServiceConfig;
import rst.domotic.service.ServiceDescriptionType.ServiceDescription;
import rst.domotic.service.ServiceTemplateConfigType.ServiceTemplateConfig;
import rst.domotic.service.ServiceTemplateType.ServiceTemplate;
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

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import static org.openbase.bco.registry.mock.MockRegistry.MockServiceDescription.*;

/**
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class MockRegistry {

    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(MockRegistry.class);

    public static final String USER_NAME = "uSeRnAmE";
    public static UnitConfig testUser;
    public static UnitConfig admin;
    public static String adminPassword = UserCreationPlugin.DEFAULT_ADMIN_USERNAME_AND_PASSWORD;

    public static final String LOCATION_PARADISE_LABEL = "Paradise";
    public static final String LOCATION_HELL_LABEL = "Hell";
    public static final String LOCATION_STAIRWAY_TO_HEAVEN_LABEL = "Stairway to Heaven";
    public static final String LOCATION_HEAVEN_LABEL = "Heaven";
    public static final String LOCATION_GARDEN_LABEL = "Garden";

    private static final Map<String, String> LOCATION_LABEL_ALIAS_MAP = new HashMap<>();

    public static final String ABSENCE_ENERGY_SAVING_AGENT_LABEL = "AbsenceEnergySaving";
    public static final String HEATER_ENERGY_SAVING_AGENT_LABEL = "HeaterEnergySaving";
    public static final String ILLUMINATION_LIGHT_SAVING_AGENT_LABEL = "IlluminationLightSaving";
    public static final String POWER_STATE_SYNCHRONISER_AGENT_LABEL = "PowerStateSynchroniser";
    public static final String PRESENCE_LIGHT_AGENT_LABEL = "PresenceLight";
    public static final Map<String, String> AGENT_LABEL_ID_MAP = new HashMap<>();

    public static final AxisAlignedBoundingBox3DFloat DEFAULT_BOUNDING_BOX = AxisAlignedBoundingBox3DFloat.newBuilder()
            .setHeight(10)
            .setWidth(20)
            .setDepth(30)
            .setLeftFrontBottom(Translation.newBuilder().setX(0).setY(0).setZ(0).build())
            .build();

    private static AuthenticatorLauncher authenticatorLauncher;
    private static AuthenticatorController authenticatorController;

    private static ActivityRegistryLauncher activityRegistryLauncher;
    private static ClassRegistryLauncher classRegistryLauncher;
    private static TemplateRegistryLauncher templateRegistryLauncher;
    private static UnitRegistryLauncher unitRegistryLauncher;

    private static ActivityRegistry activityRegistry;
    private static ClassRegistry classRegistry;
    private static TemplateRegistry templateRegistry;
    private static UnitRegistry unitRegistry;

    public enum MockServiceTemplate {
        ACTIVATION_STATE_SERVICE(ServiceType.ACTIVATION_STATE_SERVICE, CommunicationType.ACTIVATION_STATE),
        BATTERY_STATE_SERVICE(ServiceType.BATTERY_STATE_SERVICE, CommunicationType.BATTERY_STATE),
        BRIGHTNESS_STATE_SERVICE(ServiceType.BRIGHTNESS_STATE_SERVICE, CommunicationType.BRIGHTNESS_STATE),
        BUTTON_STATE_SERVICE(ServiceType.BUTTON_STATE_SERVICE, CommunicationType.BUTTON_STATE),
        SWITCH_STATE_SERVICE(ServiceType.SWITCH_STATE_SERVICE, CommunicationType.SWITCH_STATE),
        COLOR_STATE_SERVICE(ServiceType.COLOR_STATE_SERVICE, CommunicationType.COLOR_STATE),
        HANDLE_STATE_SERVICE(ServiceType.HANDLE_STATE_SERVICE, CommunicationType.HANDLE_STATE),
        MOTION_STATE_SERVICE(ServiceType.MOTION_STATE_SERVICE, CommunicationType.MOTION_STATE),
        POWER_CONSUMPTION_STATE_SERVICE(ServiceType.POWER_CONSUMPTION_STATE_SERVICE, CommunicationType.POWER_CONSUMPTION_STATE),
        POWER_STATE_SERVICE(ServiceType.POWER_STATE_SERVICE, CommunicationType.POWER_STATE),
        CONTACT_STATE_SERVICE(ServiceType.CONTACT_STATE_SERVICE, CommunicationType.CONTACT_STATE),
        BLIND_STATE_SERVICE(ServiceType.BLIND_STATE_SERVICE, CommunicationType.BLIND_STATE),
        TAMPER_STATE_SERVICE(ServiceType.TAMPER_STATE_SERVICE, CommunicationType.TAMPER_STATE),
        TARGET_TEMPERATURE_STATE_SERVICE(ServiceType.TARGET_TEMPERATURE_STATE_SERVICE, CommunicationType.TEMPERATURE_STATE),
        TEMPERATURE_STATE_SERVICE(ServiceType.TEMPERATURE_STATE_SERVICE, CommunicationType.TEMPERATURE_STATE),
        STANDBY_STATE_SERVICE(ServiceType.STANDBY_STATE_SERVICE, CommunicationType.STANDBY_STATE),
        SMOKE_ALARM_STATE_SERVICE(ServiceType.SMOKE_ALARM_STATE_SERVICE, CommunicationType.ALARM_STATE),
        TEMPERATURE_ALARM_STATE_SERVICE(ServiceType.TEMPERATURE_ALARM_STATE_SERVICE, CommunicationType.ALARM_STATE),
        WATER_ALARM_STATE_SERVICE(ServiceType.WATER_ALARM_STATE_SERVICE, CommunicationType.ALARM_STATE),
        FIRE_ALARM_STATE_SERVICE(ServiceType.FIRE_ALARM_STATE_SERVICE, CommunicationType.ALARM_STATE),
        TEMPEST_ALARM_STATE_SERVICE(ServiceType.TEMPEST_ALARM_STATE_SERVICE, CommunicationType.ALARM_STATE),
        EARTHQUAKE_ALARM_STATE_SERVICE(ServiceType.EARTHQUAKE_ALARM_STATE_SERVICE, CommunicationType.ALARM_STATE),
        INTRUSION_ALARM_STATE_SERVICE(ServiceType.INTRUSION_ALARM_STATE_SERVICE, CommunicationType.ALARM_STATE),
        MEDICAL_EMERGENCY_ALARM_STATE_SERVICE(ServiceType.MEDICAL_EMERGENCY_ALARM_STATE_SERVICE, CommunicationType.ALARM_STATE),
        DOOR_STATE_SERVICE(ServiceType.DOOR_STATE_SERVICE, CommunicationType.DOOR_STATE),
        WINDOW_STATE_SERVICE(ServiceType.WINDOW_STATE_SERVICE, CommunicationType.WINDOW_STATE),
        PASSAGE_STATE_SERVICE(ServiceType.PASSAGE_STATE_SERVICE, CommunicationType.PASSAGE_STATE),
        RFID_STATE_SERVICE(ServiceType.RFID_STATE_SERVICE, CommunicationType.RFID_STATE),
        PRESENCE_STATE_SERVICE(ServiceType.PRESENCE_STATE_SERVICE, CommunicationType.PRESENCE_STATE),
        ILLUMINANCE_STATE_SERVICE(ServiceType.ILLUMINANCE_STATE_SERVICE, CommunicationType.ILLUMINANCE_STATE),
        USER_TRANSIT_STATE_SERVICE(ServiceType.USER_TRANSIT_STATE_SERVICE, CommunicationType.USER_TRANSIT_STATE),
        MULTI_ACTIVITY_STATE_SERVICE(ServiceType.MULTI_ACTIVITY_STATE_SERVICE, CommunicationType.ACTIVITY_STATE),
        EMPHASIS_STATE_SERVICE(ServiceType.EMPHASIS_STATE_SERVICE, CommunicationType.EMPHASIS_STATE);


        private final ServiceTemplate serviceTemplate;

        MockServiceTemplate(final ServiceType serviceType, final CommunicationType communicationType) {
            this.serviceTemplate = ServiceTemplate.newBuilder().setType(serviceType).setCommunicationType(communicationType).build();
        }

        public ServiceTemplate getServiceTemplate() {
            return serviceTemplate;
        }
    }

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
            descriptionBuilder.setServiceType(type);
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
                    classRegistryLauncher = new ClassRegistryLauncher();
                    classRegistryLauncher.launch();
                    classRegistry = classRegistryLauncher.getLaunchable();
                } catch (CouldNotPerformException | InterruptedException ex) {
                    throw ExceptionPrinter.printHistoryAndReturnThrowable(ex, LOGGER, LogLevel.ERROR);
                }
                return null;
            }));
            registryStartupTasks.add(GlobalCachedExecutorService.submit(() -> {
                try {
                    templateRegistryLauncher = new TemplateRegistryLauncher();
                    templateRegistryLauncher.launch();
                    templateRegistry = templateRegistryLauncher.getLaunchable();
                } catch (CouldNotPerformException ex) {
                    throw ExceptionPrinter.printHistoryAndReturnThrowable(ex, LOGGER, LogLevel.ERROR);
                }
                return null;
            }));
            registryStartupTasks.add(GlobalCachedExecutorService.submit(() -> {
                try {
                    activityRegistryLauncher = new ActivityRegistryLauncher();
                    activityRegistryLauncher.launch();
                    activityRegistry = activityRegistryLauncher.getLaunchable();
                } catch (CouldNotPerformException ex) {
                    throw ExceptionPrinter.printHistoryAndReturnThrowable(ex, LOGGER, LogLevel.ERROR);
                }
                return null;
            }));
            LOGGER.info("Starting all registries: unit, class, template, activity...");
            for (Future<Void> task : registryStartupTasks) {
                task.get();
            }
            registryStartupTasks.clear();
            LOGGER.info("Registries started!");

            LOGGER.info("Reinitialize remotes...");
            Registries.reinitialize();
            LOGGER.info("Reinitialized remotes!");
            Registries.waitForData();

            registryStartupTasks.add(GlobalCachedExecutorService.submit(() -> {
                try {
                    LOGGER.info("Update serviceTemplates...");
                    for (MockServiceTemplate mockServiceTemplate : MockServiceTemplate.values()) {
                        final String id = templateRegistry.getServiceTemplateByType(mockServiceTemplate.getServiceTemplate().getType()).getId();
                        templateRegistry.updateServiceTemplate(mockServiceTemplate.getServiceTemplate().toBuilder().setId(id).build()).get();
                    }

                    LOGGER.info("Update unitTemplates...");
                    // load templates
                    for (MockUnitTemplate template : MockUnitTemplate.values()) {
                        String unitTemplateId = templateRegistry.getUnitTemplateByType(template.getTemplate().getType()).getId();
                        templateRegistry.updateUnitTemplate(template.getTemplate().toBuilder().setId(unitTemplateId).build()).get();
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
        if (unitRegistryLauncher != null) {
            unitRegistryLauncher.shutdown();
        }

        if (activityRegistryLauncher != null) {
            activityRegistryLauncher.shutdown();
        }

        if (templateRegistryLauncher != null) {
            templateRegistryLauncher.shutdown();
        }

        if (classRegistryLauncher != null) {
            classRegistryLauncher.shutdown();
        }


        if (authenticatorLauncher != null) {
            authenticatorLauncher.shutdown();
        }

        SessionManager.getInstance().completeLogout();
        AuthenticatedServerManager.shutdown();

        CachedUnitRegistryRemote.shutdown();
        CachedActivityRegistryRemote.shutdown();
        CachedClassRegistryRemote.shutdown();
        CachedTemplateRegistryRemote.shutdown();
    }

    public static UnitConfig getLocationByLabel(final String label) throws CouldNotPerformException {
        return Registries.getUnitRegistry().getUnitConfigByAlias(LOCATION_LABEL_ALIAS_MAP.get(label));
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

            // rename default root location home into paradise test location.
            UnitConfig.Builder rootLocation = unitRegistry.getRootLocationConfig().toBuilder().clearLabel();
            rootLocation.getPlacementConfigBuilder().setShape(paradiseShape);
            LabelProcessor.addLabel(rootLocation.getLabelBuilder(), Locale.ENGLISH, LOCATION_PARADISE_LABEL);
            UnitConfig paradise = unitRegistry.updateUnitConfig(rootLocation.build()).get();
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
            PlacementConfig hellPlacement = PlacementConfig.newBuilder().setPosition(hellPosition).setShape(hellShape).setLocationId(paradise.getId()).build();
            UnitConfig hell = unitRegistry.registerUnitConfig(getLocationUnitConfig(LOCATION_HELL_LABEL, tileLocationConfig, hellPlacement)).get();

            // Create stairway to heaven
            List<Vec3DDouble> stairwayVertices = new ArrayList<>();
            stairwayVertices.add(Vec3DDouble.newBuilder().setX(0).setY(0).setZ(0).build());
            stairwayVertices.add(Vec3DDouble.newBuilder().setX(0).setY(1).setZ(0).build());
            stairwayVertices.add(Vec3DDouble.newBuilder().setX(4).setY(1).setZ(0).build());
            stairwayVertices.add(Vec3DDouble.newBuilder().setX(4).setY(0).setZ(0).build());
            Shape stairwayShape = Shape.newBuilder().addAllFloor(stairwayVertices).build();
            Pose stairwayPosition = Pose.newBuilder().setTranslation(Translation.newBuilder().setX(1).setY(0).setZ(0).build())
                    .setRotation(Rotation.newBuilder().setQw(1).setQx(0).setQy(0).setQz(0).build()).build();
            PlacementConfig stairwayPlacement = PlacementConfig.newBuilder().setPosition(stairwayPosition).setShape(stairwayShape).setLocationId(paradise.getId()).build();
            UnitConfig stairwayLocation = unitRegistry.registerUnitConfig(getLocationUnitConfig(LOCATION_STAIRWAY_TO_HEAVEN_LABEL, tileLocationConfig, stairwayPlacement)).get();

            // Create heaven
            List<Vec3DDouble> heavenVertices = new ArrayList<>();
            heavenVertices.add(Vec3DDouble.newBuilder().setX(0).setY(0).setZ(0).build());
            heavenVertices.add(Vec3DDouble.newBuilder().setX(0).setY(4).setZ(0).build());
            heavenVertices.add(Vec3DDouble.newBuilder().setX(2).setY(4).setZ(0).build());
            heavenVertices.add(Vec3DDouble.newBuilder().setX(2).setY(0).setZ(0).build());
            Shape heavenShape = Shape.newBuilder().addAllFloor(heavenVertices).build();
            Pose heavenPosition = Pose.newBuilder().setTranslation(Translation.newBuilder().setX(1).setY(1).setZ(0).build())
                    .setRotation(Rotation.newBuilder().setQw(1).setQx(0).setQy(0).setQz(0).build()).build();
            PlacementConfig heavenPlacement = PlacementConfig.newBuilder().setPosition(heavenPosition).setShape(heavenShape).setLocationId(paradise.getId()).build();
            UnitConfig heavenLocation = unitRegistry.registerUnitConfig(getLocationUnitConfig(LOCATION_HEAVEN_LABEL, tileLocationConfig, heavenPlacement)).get();

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
            UnitConfig garden = unitRegistry.registerUnitConfig(getLocationUnitConfig(LOCATION_GARDEN_LABEL, regionLocationConfig, edenPlacement)).get();

            LOCATION_LABEL_ALIAS_MAP.put(LOCATION_GARDEN_LABEL, garden.getAlias(0));
            LOCATION_LABEL_ALIAS_MAP.put(LOCATION_HEAVEN_LABEL, heavenLocation.getAlias(0));
            LOCATION_LABEL_ALIAS_MAP.put(LOCATION_HELL_LABEL, hell.getAlias(0));
            LOCATION_LABEL_ALIAS_MAP.put(LOCATION_PARADISE_LABEL, paradise.getAlias(0));
            LOCATION_LABEL_ALIAS_MAP.put(LOCATION_STAIRWAY_TO_HEAVEN_LABEL, stairwayLocation.getAlias(0));
        } catch (ExecutionException ex) {
            throw new CouldNotPerformException(ex);
        }
    }

    private UnitConfig getLocationUnitConfig(final String label, final LocationConfig locationConfig, final PlacementConfig placementConfig) {
        UnitConfig.Builder unitConfig = UnitConfig.newBuilder().setUnitType(UnitType.LOCATION).setLocationConfig(locationConfig).setPlacementConfig(placementConfig);
        LabelProcessor.addLabel(unitConfig.getLabelBuilder(), Locale.ENGLISH, label);
        return unitConfig.build();
    }

    public static String getUnitAlias(final UnitType unitType) {
        return getUnitAlias(unitType, 1);
    }

    public static String getUnitAlias(final UnitType unitType, final int number) {
        return StringProcessor.transformUpperCaseToCamelCase(unitType.name()) + "-" + number;
    }

    private void registerConnections() throws CouldNotPerformException, InterruptedException {
        try {
            List<String> tileIds = new ArrayList<>();
            tileIds.add(getLocationByLabel(LOCATION_HEAVEN_LABEL).getId());
            tileIds.add(getLocationByLabel(LOCATION_HELL_LABEL).getId());
            String reedContactId = Registries.getUnitRegistry().getUnitConfigByAlias(getUnitAlias(UnitType.REED_CONTACT)).getId();
            ConnectionConfig connectionConfig = ConnectionConfig.newBuilder().setType(ConnectionConfig.ConnectionType.DOOR).addAllTileId(tileIds).addUnitId(reedContactId).build();
            unitRegistry.registerUnitConfig(getConnectionUnitConfig("Gate", connectionConfig)).get();

            tileIds.clear();
            tileIds.add(getLocationByLabel(LOCATION_HEAVEN_LABEL).getId());
            tileIds.add(getLocationByLabel(LOCATION_STAIRWAY_TO_HEAVEN_LABEL).getId());
            reedContactId = Registries.getUnitRegistry().getUnitConfigsByLabelAndUnitType("Reed_Heaven_Stairs", UnitType.REED_CONTACT).get(0).getId();
            connectionConfig = ConnectionConfig.newBuilder().setType(ConnectionConfig.ConnectionType.DOOR).addAllTileId(tileIds).addUnitId(reedContactId).build();
            unitRegistry.registerUnitConfig(getConnectionUnitConfig("Stairs_Heaven_Gate", connectionConfig)).get();

            tileIds.clear();
            tileIds.add(getLocationByLabel(LOCATION_HELL_LABEL).getId());
            tileIds.add(getLocationByLabel(LOCATION_STAIRWAY_TO_HEAVEN_LABEL).getId());
            reedContactId = Registries.getUnitRegistry().getUnitConfigsByLabelAndUnitType("Reed_Hell_Stairs", UnitType.REED_CONTACT).get(0).getId();
            connectionConfig = ConnectionConfig.newBuilder().setType(ConnectionConfig.ConnectionType.DOOR).addAllTileId(tileIds).addUnitId(reedContactId).build();
            unitRegistry.registerUnitConfig(getConnectionUnitConfig("Stairs_Hell_Gate", connectionConfig)).get();

            tileIds.clear();
            tileIds.add(getLocationByLabel(LOCATION_HELL_LABEL).getId());
            tileIds.add(getLocationByLabel(LOCATION_STAIRWAY_TO_HEAVEN_LABEL).getId());
            reedContactId = Registries.getUnitRegistry().getUnitConfigsByLabelAndUnitType("Reed_Stairway_Window", UnitType.REED_CONTACT).get(0).getId();
            connectionConfig = ConnectionConfig.newBuilder().setType(ConnectionConfig.ConnectionType.WINDOW).addAllTileId(tileIds).addUnitId(reedContactId).build();
            unitRegistry.registerUnitConfig(getConnectionUnitConfig("Stairs_Hell_Lookout", connectionConfig)).get();

        } catch (ExecutionException | IndexOutOfBoundsException ex) {
            throw new CouldNotPerformException(ex);
        }
    }

    private UnitConfig getConnectionUnitConfig(final String label, final ConnectionConfig connectionConfig) {
        UnitConfig.Builder connectionUnitConfig = UnitConfig.newBuilder().setUnitType(UnitType.CONNECTION).setConnectionConfig(connectionConfig);
        LabelProcessor.addLabel(connectionUnitConfig.getLabelBuilder(), Locale.ENGLISH, label);
        return connectionUnitConfig.build();
    }

    private void registerUser() throws CouldNotPerformException, InterruptedException {
        for (UnitConfig unitConfig : unitRegistry.getUnitConfigs(UnitType.USER)) {
            if (unitConfig.getUserConfig().getUserName().equals(UserCreationPlugin.DEFAULT_ADMIN_USERNAME_AND_PASSWORD)) {
                admin = unitConfig;
                break;
            }
        }

        UserConfig.Builder config = UserConfig.newBuilder().setFirstName("Max").setLastName("Mustermann").setUserName(USER_NAME);
        UnitConfig.Builder userUnitConfig = UnitConfig.newBuilder().setUnitType(UnitType.USER).setUserConfig(config).setEnablingState(EnablingState.newBuilder().setValue(EnablingState.State.ENABLED));
        userUnitConfig.getPermissionConfigBuilder().getOtherPermissionBuilder().setWrite(true).setAccess(true).setRead(true);
        try {
            testUser = unitRegistry.registerUnitConfig(userUnitConfig.build()).get();
        } catch (ExecutionException ex) {
            throw new CouldNotPerformException(ex);
        }
    }

    private void registerAgentClasses() throws CouldNotPerformException, InterruptedException {
        try {
            AGENT_LABEL_ID_MAP.put(ABSENCE_ENERGY_SAVING_AGENT_LABEL, classRegistry.registerAgentClass(getAgentClass(ABSENCE_ENERGY_SAVING_AGENT_LABEL)).get().getId());
            AGENT_LABEL_ID_MAP.put(HEATER_ENERGY_SAVING_AGENT_LABEL, classRegistry.registerAgentClass(getAgentClass(HEATER_ENERGY_SAVING_AGENT_LABEL)).get().getId());
            AGENT_LABEL_ID_MAP.put(ILLUMINATION_LIGHT_SAVING_AGENT_LABEL, classRegistry.registerAgentClass(getAgentClass(ILLUMINATION_LIGHT_SAVING_AGENT_LABEL)).get().getId());
            AGENT_LABEL_ID_MAP.put(POWER_STATE_SYNCHRONISER_AGENT_LABEL, classRegistry.registerAgentClass(getAgentClass(POWER_STATE_SYNCHRONISER_AGENT_LABEL)).get().getId());
            AGENT_LABEL_ID_MAP.put(PRESENCE_LIGHT_AGENT_LABEL, classRegistry.registerAgentClass(getAgentClass(PRESENCE_LIGHT_AGENT_LABEL)).get().getId());
        } catch (ExecutionException ex) {
            throw new CouldNotPerformException(ex);
        }
    }

    private AgentClass getAgentClass(final String label) {
        AgentClass.Builder agentClass = AgentClass.newBuilder();
        LabelProcessor.addLabel(agentClass.getLabelBuilder(), Locale.ENGLISH, label);
        return agentClass.build();
    }

    final SyncObject LOCK = new SyncObject("WaitForDeviceClassLock");
    final Observer notifyChangeObserver = (Observer) (Observable source, Object data) -> {
        synchronized (LOCK) {
            LOCK.notifyAll();
        }
    };

    private void registerDevices() throws CouldNotPerformException, InterruptedException {
        try {

            Registries.getClassRegistry(true).addDataObserver(notifyChangeObserver);
            // colorable light
            DeviceClass colorableLightClass = Registries.getClassRegistry().registerDeviceClass(getDeviceClass("Philips_Hue_E27", "KV01_18U", "Philips", UnitType.COLORABLE_LIGHT)).get();
            waitForDeviceClass(colorableLightClass);

            String serialNumber = "1234-5678-9100";
            registerDeviceUnitConfig(getDeviceConfig("PH_Hue_E27_Device", serialNumber, colorableLightClass));

            registerDeviceUnitConfig(getDeviceConfig("PH_Hue_E27_Device_BORROWED", serialNumber, InventoryState.State.BORROWED, colorableLightClass));
            unitRegistry.registerUnitConfig(getDeviceConfig("PH_Hue_E27_Device_Stairway", serialNumber, colorableLightClass, LOCATION_STAIRWAY_TO_HEAVEN_LABEL)).get();
            unitRegistry.registerUnitConfig(getDeviceConfig("PH_Hue_E27_Device_Heaven", serialNumber, colorableLightClass, LOCATION_HEAVEN_LABEL)).get();
            unitRegistry.registerUnitConfig(getDeviceConfig("PH_Hue_E27_Device_Hell", serialNumber, colorableLightClass, LOCATION_HELL_LABEL)).get();

            // battery, brightnessSensor, motionSensor, tamperSwitch, temperatureSensor
            DeviceClass motionSensorClass = Registries.getClassRegistry().registerDeviceClass(getDeviceClass("Fibaro_MotionSensor", "FGMS_001", "Fibaro",
                    UnitType.MOTION_DETECTOR,
                    UnitType.BATTERY,
                    UnitType.LIGHT_SENSOR,
                    UnitType.TEMPERATURE_SENSOR,
                    UnitType.TAMPER_DETECTOR)).get();
            waitForDeviceClass(motionSensorClass);

            registerDeviceUnitConfig(getDeviceConfig("F_MotionSensor_Device", serialNumber, motionSensorClass));
            unitRegistry.registerUnitConfig(getDeviceConfig("F_MotionSensor_Device_Stairway", serialNumber, motionSensorClass, LOCATION_STAIRWAY_TO_HEAVEN_LABEL)).get();
            unitRegistry.registerUnitConfig(getDeviceConfig("F_MotionSensor_Device_Heaven", serialNumber, motionSensorClass, LOCATION_HEAVEN_LABEL)).get();
            unitRegistry.registerUnitConfig(getDeviceConfig("F_MotionSensor_Device_Hell", serialNumber, motionSensorClass, LOCATION_HELL_LABEL)).get();

            // button
            DeviceClass buttonClass = Registries.getClassRegistry().registerDeviceClass(getDeviceClass("Gira_429496730210000", "429496730210000", "Gira",
                    UnitType.BUTTON)).get();
            waitForDeviceClass(buttonClass);

            registerDeviceUnitConfig(getDeviceConfig("GI_429496730210000_Device", serialNumber, buttonClass));

            // dimmableLight
            DeviceClass dimmableLightClass = Registries.getClassRegistry().registerDeviceClass(getDeviceClass("Hager_ABC", "ABC", "Hager",
                    UnitType.DIMMABLE_LIGHT)).get();
            waitForDeviceClass(dimmableLightClass);

            registerDeviceUnitConfig(getDeviceConfig("HA_ABC_Device", serialNumber, dimmableLightClass));

            // dimmer
            DeviceClass dimmerClass = Registries.getClassRegistry().registerDeviceClass(getDeviceClass("Hager_TYA663A", "TYA663A", "Hager",
                    UnitType.DIMMER)).get();
            waitForDeviceClass(dimmerClass);

            registerDeviceUnitConfig(getDeviceConfig("HA_TYA663A_Device", serialNumber, dimmerClass));

            // handle
            DeviceClass handleClass = Registries.getClassRegistry().registerDeviceClass(getDeviceClass("Homematic_RotaryHandleSensor", "Sec_RHS", "Homematic",
                    UnitType.HANDLE)).get();
            waitForDeviceClass(handleClass);

            registerDeviceUnitConfig(getDeviceConfig("HM_RotaryHandleSensor_Device", serialNumber, handleClass));

            // light
            DeviceClass lightClass = Registries.getClassRegistry().registerDeviceClass(getDeviceClass("Fibaro_FGS_221", "FGS_221", "Fibaro",
                    UnitType.LIGHT)).get();
            waitForDeviceClass(lightClass);

            registerDeviceUnitConfig(getDeviceConfig("F_FGS221_Device", serialNumber, lightClass));

            // powerConsumptionSensor, powerPlug
            DeviceClass powerPlugClass = Registries.getClassRegistry().registerDeviceClass(getDeviceClass("Plugwise_PowerPlug", "070140", "Plugwise",
                    UnitType.POWER_SWITCH,
                    UnitType.POWER_CONSUMPTION_SENSOR)).get();
            waitForDeviceClass(powerPlugClass);

            registerDeviceUnitConfig(getDeviceConfig("PW_PowerPlug_Device", serialNumber, powerPlugClass));

            // reedSwitch
            DeviceClass reedSwitchClass = Registries.getClassRegistry().registerDeviceClass(getDeviceClass("Homematic_ReedSwitch", "Sec_SC_2", "Homematic",
                    UnitType.REED_CONTACT)).get();
            waitForDeviceClass(reedSwitchClass);

            registerDeviceUnitConfig(getDeviceConfig("HM_ReedSwitch_Device", serialNumber, reedSwitchClass));
            unitRegistry.registerUnitConfig(getDeviceConfig("Reed_Heaven_Stairs", serialNumber, reedSwitchClass, LOCATION_STAIRWAY_TO_HEAVEN_LABEL)).get();
            unitRegistry.registerUnitConfig(getDeviceConfig("Reed_Hell_Stairs", serialNumber, reedSwitchClass, LOCATION_STAIRWAY_TO_HEAVEN_LABEL)).get();
            unitRegistry.registerUnitConfig(getDeviceConfig("Reed_Stairway_Window", serialNumber, reedSwitchClass, LOCATION_STAIRWAY_TO_HEAVEN_LABEL)).get();

            // roller shutter
            DeviceClass rollerShutterClass = Registries.getClassRegistry().registerDeviceClass(getDeviceClass("Hager_TYA628C", "TYA628C", "Hager",
                    UnitType.ROLLER_SHUTTER)).get();
            waitForDeviceClass(rollerShutterClass);

            registerDeviceUnitConfig(getDeviceConfig("HA_TYA628C_Device", serialNumber, rollerShutterClass));

            // smoke detector
            DeviceClass smokeDetector = Registries.getClassRegistry().registerDeviceClass(getDeviceClass("Fibaro_FGSS_001", "FGSS_001", "Fibaro",
                    UnitType.SMOKE_DETECTOR)).get();
            waitForDeviceClass(smokeDetector);

            registerDeviceUnitConfig(getDeviceConfig("Fibaro_SmokeDetector_Device", serialNumber, smokeDetector));

            // temperature controller
            DeviceClass temperatureControllerClass = Registries.getClassRegistry().registerDeviceClass(getDeviceClass("Gira_429496730250000", "429496730250000", "Gira",
                    UnitType.TEMPERATURE_CONTROLLER)).get();
            waitForDeviceClass(temperatureControllerClass);

            registerDeviceUnitConfig(getDeviceConfig("Gire_TemperatureController_Device", serialNumber, temperatureControllerClass));
            unitRegistry.registerUnitConfig(getDeviceConfig("Gire_TemperatureController_Device_Stairway", serialNumber, temperatureControllerClass, LOCATION_STAIRWAY_TO_HEAVEN_LABEL)).get();

            Registries.getClassRegistry(true).removeDataObserver(notifyChangeObserver);
        } catch (ExecutionException ex) {
            throw new CouldNotPerformException(ex);
        }
    }

    private void waitForDeviceClass(final DeviceClass deviceClass) throws CouldNotPerformException {
        synchronized (LOCK) {
            try {
                while (!Registries.getClassRegistry(false).containsDeviceClass(deviceClass)) {
                    LOGGER.info("DeviceClass[" + LabelProcessor.getBestMatch(deviceClass.getLabel()) + "] not yet available");
                    LOCK.wait();
                }
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
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
        unitRegistry.registerUnitConfig(deviceUnitConfig).get();
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

    public static UnitConfig getDeviceConfig(String label, String serialNumber, DeviceClass clazz) throws CouldNotPerformException {
        return getDeviceConfig(label, serialNumber, InventoryState.State.INSTALLED, clazz, LOCATION_PARADISE_LABEL);
    }

    public static UnitConfig getDeviceConfig(String label, String serialNumber, DeviceClass clazz, String locationLabel) throws CouldNotPerformException {
        return getDeviceConfig(label, serialNumber, InventoryState.State.INSTALLED, clazz, locationLabel);
    }

    public static UnitConfig getDeviceConfig(String label, String serialNumber, InventoryState.State inventoryState, DeviceClass clazz) throws CouldNotPerformException {
        return getDeviceConfig(label, serialNumber, inventoryState, clazz, LOCATION_PARADISE_LABEL);
    }

    public static UnitConfig getDeviceConfig(String label, String serialNumber, InventoryState.State inventoryState, DeviceClass clazz, String locationLabel) throws CouldNotPerformException {
        DeviceConfig tmp = DeviceConfig.newBuilder()
                .setSerialNumber(serialNumber)
                .setDeviceClassId(clazz.getId())
                .setInventoryState(InventoryStateType.InventoryState.newBuilder().setValue(inventoryState))
                .build();
        UnitConfig.Builder deviceUnitConfig = UnitConfig.newBuilder()
                .setPlacementConfig(getDefaultPlacement(getLocationByLabel(locationLabel)))
                .setDeviceConfig(tmp)
                .setUnitType(UnitType.DEVICE);
        LabelProcessor.addLabel(deviceUnitConfig.getLabelBuilder(), Locale.ENGLISH, label);
        return deviceUnitConfig.build();
    }

    private static List<UnitTemplateConfig> getUnitTemplateConfigs(List<UnitTemplate.UnitType> unitTypes) throws CouldNotPerformException {
        List<UnitTemplateConfig> unitTemplateConfigs = new ArrayList<>();
        for (UnitTemplate.UnitType type : unitTypes) {
            Set<ServiceTemplateConfig> serviceTemplateConfigs = new HashSet<>();
            for (ServiceDescription serviceDescription : MockUnitTemplate.getTemplate(type).getServiceDescriptionList()) {
                serviceTemplateConfigs.add(ServiceTemplateConfig.newBuilder().setServiceType(serviceDescription.getServiceType()).build());
            }
            UnitTemplateConfig config = UnitTemplateConfig.newBuilder().setType(type).addAllServiceTemplateConfig(serviceTemplateConfigs).build();
            unitTemplateConfigs.add(config);
        }
        return unitTemplateConfigs;
    }

    public static DeviceClass getDeviceClass(String label, String productNumber, String company, UnitTemplate.UnitType... types) throws CouldNotPerformException {
        List<UnitTemplate.UnitType> unitTypeList = new ArrayList<>(Arrays.asList(types));
        DeviceClass.Builder deviceClass = DeviceClass.newBuilder().setProductNumber(productNumber).setCompany(company)
                .setBindingConfig(getBindingConfig()).addAllUnitTemplateConfig(getUnitTemplateConfigs(unitTypeList))
                .setShape(Shape.newBuilder().setBoundingBox(DEFAULT_BOUNDING_BOX));
        LabelProcessor.addLabel(deviceClass.getLabelBuilder(), Locale.ENGLISH, label);
        return deviceClass.build();
    }

    public static BindingConfig getBindingConfig() {
        BindingConfig.Builder bindingConfigBuilder = BindingConfig.newBuilder();
        bindingConfigBuilder.setBindingId("OPENHAB");
        return bindingConfigBuilder.build();
    }

    public static ClassRegistry getClassRegistry() {
        return classRegistry;
    }

    public static UnitRegistry getUnitRegistry() {
        return unitRegistry;
    }

    public static TemplateRegistry getTemplateRegistry() {
        return templateRegistry;
    }

    public static ActivityRegistry getActivityRegistry() {
        return activityRegistry;
    }
}
