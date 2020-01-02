package org.openbase.bco.registry.mock;

/*
 * #%L
 * BCO Registry Utility
 * %%
 * Copyright (C) 2014 - 2020 openbase.org
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
import org.openbase.bco.registry.activity.remote.CachedActivityRegistryRemote;
import org.openbase.bco.registry.clazz.core.ClassRegistryLauncher;
import org.openbase.bco.registry.clazz.remote.CachedClassRegistryRemote;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.bco.registry.template.core.TemplateRegistryLauncher;
import org.openbase.bco.registry.template.remote.CachedTemplateRegistryRemote;
import org.openbase.bco.registry.unit.core.UnitRegistryLauncher;
import org.openbase.bco.registry.unit.remote.CachedUnitRegistryRemote;
import org.openbase.jps.core.JPService;
import org.openbase.jps.exception.JPServiceException;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.FatalImplementationErrorException;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.exception.InvalidStateException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.exception.printer.LogLevel;
import org.openbase.jul.extension.protobuf.IdentifiableMessage;
import org.openbase.jul.extension.protobuf.container.ProtoBufMessageMap;
import org.openbase.jul.extension.type.processing.LabelProcessor;
import org.openbase.jul.processing.StringProcessor;
import org.openbase.jul.schedule.GlobalCachedExecutorService;
import org.openbase.jul.storage.registry.ConsistencyHandler;
import org.openbase.jul.storage.registry.ProtoBufRegistry;
import org.openbase.type.domotic.activity.ActivityConfigType.ActivityConfig;
import org.openbase.type.domotic.binding.BindingConfigType.BindingConfig;
import org.openbase.type.domotic.service.ServiceCommunicationTypeType.ServiceCommunicationType.CommunicationType;
import org.openbase.type.domotic.service.ServiceConfigType;
import org.openbase.type.domotic.service.ServiceConfigType.ServiceConfig;
import org.openbase.type.domotic.service.ServiceDescriptionType.ServiceDescription;
import org.openbase.type.domotic.service.ServiceTemplateConfigType.ServiceTemplateConfig;
import org.openbase.type.domotic.service.ServiceTemplateType.ServiceTemplate;
import org.openbase.type.domotic.service.ServiceTemplateType.ServiceTemplate.ServicePattern;
import org.openbase.type.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import org.openbase.type.domotic.state.EnablingStateType.EnablingState;
import org.openbase.type.domotic.state.InventoryStateType;
import org.openbase.type.domotic.state.InventoryStateType.InventoryState;
import org.openbase.type.domotic.unit.UnitConfigType.UnitConfig;
import org.openbase.type.domotic.unit.UnitConfigType.UnitConfig.Builder;
import org.openbase.type.domotic.unit.UnitTemplateConfigType.UnitTemplateConfig;
import org.openbase.type.domotic.unit.UnitTemplateType.UnitTemplate;
import org.openbase.type.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;
import org.openbase.type.domotic.unit.agent.AgentClassType.AgentClass;
import org.openbase.type.domotic.unit.connection.ConnectionConfigType.ConnectionConfig;
import org.openbase.type.domotic.unit.connection.ConnectionConfigType.ConnectionConfig.ConnectionType;
import org.openbase.type.domotic.unit.device.DeviceClassType.DeviceClass;
import org.openbase.type.domotic.unit.device.DeviceConfigType.DeviceConfig;
import org.openbase.type.domotic.unit.location.LocationConfigType.LocationConfig;
import org.openbase.type.domotic.unit.location.LocationConfigType.LocationConfig.LocationType;
import org.openbase.type.domotic.unit.user.UserConfigType.UserConfig;
import org.openbase.type.geometry.AxisAlignedBoundingBox3DFloatType.AxisAlignedBoundingBox3DFloat;
import org.openbase.type.geometry.PoseType.Pose;
import org.openbase.type.geometry.RotationType.Rotation;
import org.openbase.type.geometry.TranslationType.Translation;
import org.openbase.type.math.Vec3DDoubleType.Vec3DDouble;
import org.openbase.type.spatial.PlacementConfigType.PlacementConfig;
import org.openbase.type.spatial.ShapeType.Shape;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.openbase.bco.registry.mock.MockRegistry.MockServiceDescription.*;

/**
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class MockRegistry {

    public static final String ALIAS_DEVICE_COLORABLE_LIGHT = "PH_Hue_E27_Device";
    public static final String ALIAS_DEVICE_COLORABLE_LIGHT_BORROWED = "PH_Hue_E27_Device_BORROWED";
    public static final String ALIAS_DEVICE_COLORABLE_LIGHT_DEVICE_STAIRWAY = "PH_Hue_E27_Device_Stairway";
    public static final String ALIAS_DEVICE_COLORABLE_LIGHT_HEAVEN = "PH_Hue_E27_Device_Heaven";
    public static final String ALIAS_DEVICE_COLORABLE_LIGHT_HELL = "PH_Hue_E27_Device_Hell";
    public static final String ALIAS_DEVICE_F_FGS_221 = "F_FGS221_Device";
    public static final String ALIAS_DEVICE_GI_429496730210000_DEVICE = "GI_429496730210000_Device";
    public static final String ALIAS_DEVICE_HA_ABC = "HA_ABC_Device";
    public static final String ALIAS_DEVICE_HA_TYA_663_A = "HA_TYA663A_Device";
    public static final String ALIAS_DEVICE_HM_ROTARY_HANDLE_SENSOR = "HM_RotaryHandleSensor_Device";
    public static final String ALIAS_DEVICE_MOTION_SENSOR = "F_MotionSensor_Device";
    public static final String ALIAS_DEVICE_MOTION_SENSOR_HEAVEN = "F_MotionSensor_Device_Heaven";
    public static final String ALIAS_DEVICE_MOTION_SENSOR_HELL = "F_MotionSensor_Device_Hell";
    public static final String ALIAS_DEVICE_MOTION_SENSOR_STAIRWAY = "F_MotionSensor_Device_Stairway";
    public static final String ALIAS_DEVICE_POWER_PLUG = "PW_PowerPlug_Device";
    public static final String ALIAS_DEVICE_REED_SWITCH_HEAVEN_STAIRS_DOOR = "Reed_Heaven_Stairs";
    public static final String ALIAS_DEVICE_REED_SWITCH_HELL_STAIRS_DOOR = "Reed_Hell_Stairs";
    public static final String ALIAS_DEVICE_REED_SWITCH_HOMEMATIC = "HM_ReedSwitch_Device";
    public static final String ALIAS_DEVICE_REED_SWITCH_STAIRWAY_WINDOW = "Reed_Stairway_Window_Device";
    public static final String ALIAS_DEVICE_ROLLERSHUTTER = "HA_TYA628C_Device";
    public static final String ALIAS_DEVICE_ROLLERSHUTTER_STAIRWAY = "HA_TYA628C_Device_Stairway";
    public static final String ALIAS_DEVICE_SMOKE_DETECTOR = "Fibaro_SmokeDetector_Device";
    public static final String ALIAS_DEVICE_SMOKE_DETECTOR_STAIRWAY = "Fibaro_SmokeDetector_Device_Stairway";
    public static final String ALIAS_DEVICE_TEMPERATURE_CONTROLLER = "Gire_TemperatureController_Device";
    public static final String ALIAS_DEVICE_TEMPERATURE_CONTROLLER_STAIRWAY_TO_HEAVEN = "Gire_TemperatureController_Device_Stairway";
    public static final String ALIAS_DOOR_GATE = "Gate";
    public static final String ALIAS_DOOR_STAIRS_HEAVEN_GATE = "Stairs_Heaven_Gate";
    public static final String ALIAS_DOOR_STAIRWAY_HELL_GATE = "Stairs_Hell_Gate";
    public static final String ALIAS_LOCATION_GARDEN = "Garden";
    public static final String ALIAS_LOCATION_HEAVEN = "Heaven";
    public static final String ALIAS_LOCATION_HELL = "Hell";
    public static final String ALIAS_LOCATION_ROOT_PARADISE = "Paradise";
    public static final String ALIAS_LOCATION_STAIRWAY_TO_HEAVEN = "Stairway_to_Heaven";
    public static final String ALIAS_REED_SWITCH_STAIRWAY_HELL_WINDOW = "Reed_Stairway_Window";
    public static final String ALIAS_REED_SWITCH_HEAVEN_STAIRWAY_GATE = "Reed_Heaven_Stairs_Door";
    public static final String ALIAS_REED_SWITCH_STAIRWAY_HELL_GATE = "Reed_Hell_Staris_Door";
    public static final String ALIAS_USER_MAX_MUSTERMANN = "MaxMustermann";
    public static final String ALIAS_WINDOW_STAIRWAY_HELL_LOOKOUT = "Stairs_Hell_Lookout";
    public static final String BINDING_OPENHAB = "OPENHAB";
    public static final String COMPANY_FIBARO = "Fibaro";
    public static final String COMPANY_GIRA = "Gira";
    public static final String COMPANY_HAGER = "Hager";
    public static final String COMPANY_HOMEMATIC = "Homematic";
    public static final String COMPANY_PHILIPS = "Philips";
    public static final String COMPANY_PLUGWISE = "Plugwise";
    public static final String LABEL_AGENT_CLASS_ABSENCE_ENERGY_SAVING = "AbsenceEnergySaving";
    public static final String LABEL_AGENT_CLASS_FIRE_ALARM = "FireAlarm";
    public static final String LABEL_AGENT_CLASS_HEATER_ENERGY_SAVING = "HeaterEnergySaving";
    public static final String LABEL_AGENT_CLASS_ILLUMINATION_LIGHT_SAVING = "IlluminationLightSaving";
    public static final String LABEL_AGENT_CLASS_POWER_STATE_SYNCHRONISER = "PowerStateSynchroniser";
    public static final String LABEL_AGENT_CLASS_PRESENCE_LIGHT = "PresenceLight";
    public static final String LABEL_DEVICE_CLASS_FIBARO_FGS_221 = "Fibaro_FGS_221";
    public static final String LABEL_DEVICE_CLASS_FIBARO_FGSS_001 = "Fibaro_FGSS_001";
    public static final String LABEL_DEVICE_CLASS_FIBARO_MOTION_SENSOR = "Fibaro_MotionSensor";
    public static final String LABEL_DEVICE_CLASS_GIRA_429496730210000 = "Gira_429496730210000";
    public static final String LABEL_DEVICE_CLASS_GIRA_429496730250000 = "Gira_429496730250000";
    public static final String LABEL_DEVICE_CLASS_HAGER_ABC = "Hager_ABC";
    public static final String LABEL_DEVICE_CLASS_HAGER_TYA_628_C = "Hager_TYA628C";
    public static final String LABEL_DEVICE_CLASS_HAGER_TYA_663_A1 = "Hager_TYA663A";
    public static final String LABEL_DEVICE_CLASS_HOMEMATIC_REED_SWITCH = "Homematic_ReedSwitch";
    public static final String LABEL_DEVICE_CLASS_HOMEMATIC_ROTARY_HANDLE_SENSOR = "Homematic_RotaryHandleSensor";
    public static final String LABEL_DEVICE_CLASS_PHILIPS_HUE_E_27 = "Philips_Hue_E27";
    public static final String LABEL_DEVICE_CLASS_PLUGWISE_POWER_PLUG = "Plugwise_PowerPlug";
    public static final String USER_NAME = "uSeRnAmE";
    public static final String USER_FIRST_NAME = "Max";
    public static final String USER_LAST_NAME = "Mustermann";
    public static final Map<String, String> AGENT_CLASS_LABEL_ID_MAP = new HashMap<>();
    public static final AxisAlignedBoundingBox3DFloat DEFAULT_BOUNDING_BOX = AxisAlignedBoundingBox3DFloat.newBuilder()
            .setHeight(10)
            .setWidth(20)
            .setDepth(30)
            .setLeftFrontBottom(Translation.newBuilder().setX(0).setY(0).setZ(0).build())
            .build();
    public static String TEST_ACTIVITY_ID = "";
    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(MockRegistry.class);
    public static UnitConfig testUser;
    private static AuthenticatorLauncher authenticatorLauncher;
    private static AuthenticatorController authenticatorController;

    private static ActivityRegistryLauncher activityRegistryLauncher;
    private static ClassRegistryLauncher classRegistryLauncher;
    private static TemplateRegistryLauncher templateRegistryLauncher;
    private static UnitRegistryLauncher unitRegistryLauncher;

    protected MockRegistry() throws InstantiationException {
        try {
            JPService.setupJUnitTestMode();
            Registries.prepare();
            List<Future<Void>> registryStartupTasks = new ArrayList<>();
            registryStartupTasks.add(GlobalCachedExecutorService.submit(() -> {
                try {
                    authenticatorLauncher = new AuthenticatorLauncher();
                    authenticatorLauncher.launch().get();
                    authenticatorController = authenticatorLauncher.getLaunchable();
                    authenticatorController.waitForActivation();
                } catch (CouldNotPerformException ex) {
                    throw ExceptionPrinter.printHistoryAndReturnThrowable(ex, LOGGER, LogLevel.ERROR);
                }
                return null;
            }));
            LOGGER.debug("Starting authenticator...");
            for (Future<Void> task : registryStartupTasks) {
                task.get();
            }
            registryStartupTasks.clear();

            registryStartupTasks.add(GlobalCachedExecutorService.submit(() -> {
                try {
                    unitRegistryLauncher = new UnitRegistryLauncher();
                    unitRegistryLauncher.launch().get();
                } catch (CouldNotPerformException ex) {
                    throw ExceptionPrinter.printHistoryAndReturnThrowable(ex, LOGGER, LogLevel.ERROR);
                }
                return null;
            }));
            registryStartupTasks.add(GlobalCachedExecutorService.submit(() -> {
                try {
                    classRegistryLauncher = new ClassRegistryLauncher();
                    classRegistryLauncher.launch().get();
                } catch (CouldNotPerformException | InterruptedException ex) {
                    throw ExceptionPrinter.printHistoryAndReturnThrowable(ex, LOGGER, LogLevel.ERROR);
                }
                return null;
            }));
            registryStartupTasks.add(GlobalCachedExecutorService.submit(() -> {
                try {
                    templateRegistryLauncher = new TemplateRegistryLauncher();
                    templateRegistryLauncher.launch().get();
                } catch (CouldNotPerformException ex) {
                    throw ExceptionPrinter.printHistoryAndReturnThrowable(ex, LOGGER, LogLevel.ERROR);
                }
                return null;
            }));
            registryStartupTasks.add(GlobalCachedExecutorService.submit(() -> {
                try {
                    activityRegistryLauncher = new ActivityRegistryLauncher();
                    activityRegistryLauncher.launch().get();
                } catch (CouldNotPerformException ex) {
                    throw ExceptionPrinter.printHistoryAndReturnThrowable(ex, LOGGER, LogLevel.ERROR);
                }
                return null;
            }));
            LOGGER.debug("Starting all registries: unit, class, template, activity...");
            for (Future<Void> task : registryStartupTasks) {
                while (true) {
                    try {
                        task.get(1000, TimeUnit.SECONDS);
                        break;
                    } catch (TimeoutException e) {
                        LOGGER.info("Still waiting until mockup registry is ready...");
                    }
                }
            }
            registryStartupTasks.clear();
            LOGGER.debug("Registries started!");

            LOGGER.debug("Reinitialize remotes...");
            Registries.reinitialize();
            LOGGER.debug("Reinitialized remotes!");
            Registries.waitForData();

            registryStartupTasks.add(GlobalCachedExecutorService.submit(() -> {
                LOGGER.debug("Update serviceTemplates...");
                for (MockServiceTemplate mockServiceTemplate : MockServiceTemplate.values()) {
                    final ServiceTemplate.Builder originalServiceTemplate = Registries.getTemplateRegistry().getServiceTemplateByType(mockServiceTemplate.getServiceTemplate().getServiceType()).toBuilder();
                    originalServiceTemplate.mergeFrom(mockServiceTemplate.getServiceTemplate());
                    Registries.getTemplateRegistry().updateServiceTemplate(originalServiceTemplate.build()).get();
                }

                LOGGER.debug("Update unit templates...");
                // load templates
                for (MockUnitTemplate template : MockUnitTemplate.values()) {
                    final UnitTemplate.Builder originalUnitTemplate = Registries.getTemplateRegistry().getUnitTemplateByType(template.getUnitTemplate().getUnitType()).toBuilder();
                    originalUnitTemplate.mergeFrom(template.getUnitTemplate());
                    Registries.getTemplateRegistry().updateUnitTemplate(originalUnitTemplate.build()).get();
                }

                LOGGER.debug("Register user...");
                registerUser();

                LOGGER.debug("Register agent classes...");
                registerAgentClasses();

                LOGGER.debug("Register locations...");
                registerLocations();
                LOGGER.debug("Wait until registry is ready...");
                Registries.waitUntilReady();

                LOGGER.debug("Register devices...");
                registerDevices();
                LOGGER.debug("Wait until registry is ready...");
                Registries.waitUntilReady();

                LOGGER.debug("Register connections...");
                registerConnections();

                LOGGER.debug("Register activities...");
                registerActivities();

                LOGGER.debug("Wait for final consistency...");
                Registries.waitUntilReady();
                return null;
            }));

            LOGGER.debug("Wait for unitTemplate updates; device, location and user registration...");
            for (Future<Void> task : registryStartupTasks) {
                task.get();
            }
            registryStartupTasks.clear();
            LOGGER.debug("UnitTemplates updated and devices, locations, users and agentClasses registered!");
        } catch (JPServiceException | InterruptedException | ExecutionException | CouldNotPerformException ex) {
            shutdown();
            throw new InstantiationException(this, ex);
        }
    }

    public static String getUnitAlias(final UnitType unitType) {
        return getUnitAlias(unitType, 1);
    }

    public static String getUnitAlias(final UnitType unitType, final int number) {
        return StringProcessor.transformUpperCaseToPascalCase(unitType.name()) + "-" + number;
    }

    public static String getUnitIdByAlias(final String alias) throws CouldNotPerformException, InterruptedException {
        return Registries.getUnitRegistry(true).getUnitConfigByAlias(alias).getId();
    }

    private static void registerActivities() throws CouldNotPerformException, ExecutionException, InterruptedException {
        final String templateId = Registries.getTemplateRegistry().getActivityTemplates().get(0).getId();
        final ActivityConfig.Builder builder = ActivityConfig.newBuilder().setActivityTemplateId(templateId);
        LabelProcessor.addLabel(builder.getLabelBuilder(), Locale.getDefault(), "Test Activity");
        TEST_ACTIVITY_ID = Registries.getActivityRegistry().registerActivityConfig(builder.build()).get().getId();
    }

    /**
     * Registers the given unit.
     *
     * @param unitConfig
     *
     * @throws CouldNotPerformException
     * @throws InterruptedException
     * @throws ExecutionException
     */
    private static UnitConfig registerUnitConfig(UnitConfig unitConfig) throws CouldNotPerformException, InterruptedException, ExecutionException {
        unitConfig = Registries.getUnitRegistry().registerUnitConfig(unitConfig).get();
        if (!Registries.getUnitRegistry().containsUnitConfig(unitConfig)) {
            throw ExceptionPrinter.printHistoryAndReturnThrowable(new FatalImplementationErrorException("unit is not contained after registration task finished", MockRegistry.class), LOGGER);
        }
        return unitConfig;
    }

    public static PlacementConfig getDefaultPlacement(UnitConfig location) {
        Rotation rotation = Rotation.newBuilder().setQw(1).setQx(0).setQy(0).setQz(0).build();
        Translation translation = Translation.newBuilder().setX(0).setY(0).setZ(0).build();
        Pose pose = Pose.newBuilder().setRotation(rotation).setTranslation(translation).build();
        return PlacementConfig.newBuilder().setPose(pose).setLocationId(location.getId()).build();
    }

    public static Iterable<ServiceConfigType.ServiceConfig> generateServiceConfig(final UnitTemplate template) {
        final List<ServiceConfigType.ServiceConfig> serviceConfigList = new ArrayList<>();

        for (ServiceDescription serviceDescription : template.getServiceDescriptionList()) {
            BindingConfig bindingServiceConfig = BindingConfig.newBuilder().setBindingId(BINDING_OPENHAB).build();
            serviceConfigList.add(ServiceConfig.newBuilder().setServiceDescription(serviceDescription).setBindingConfig(bindingServiceConfig).build());
        }
        return serviceConfigList;
    }

    public static UnitConfig generateDeviceConfig(String alias, String serialNumber, DeviceClass clazz) throws CouldNotPerformException {
        return generateDeviceConfig(alias, serialNumber, InventoryState.State.INSTALLED, clazz, ALIAS_LOCATION_ROOT_PARADISE);
    }

    public static UnitConfig generateDeviceConfig(String alias, String serialNumber, DeviceClass clazz, String locationLabel) throws CouldNotPerformException {
        return generateDeviceConfig(alias, serialNumber, InventoryState.State.INSTALLED, clazz, locationLabel);
    }

    public static UnitConfig generateDeviceConfig(String alias, String serialNumber, InventoryState.State inventoryState, DeviceClass clazz) throws CouldNotPerformException {
        return generateDeviceConfig(alias, serialNumber, inventoryState, clazz, ALIAS_LOCATION_ROOT_PARADISE);
    }

    public static UnitConfig generateDeviceConfig(String alias, String serialNumber, InventoryState.State inventoryState, DeviceClass clazz, String locationLabel) throws CouldNotPerformException {
        DeviceConfig tmp = DeviceConfig.newBuilder()
                .setSerialNumber(serialNumber)
                .setDeviceClassId(clazz.getId())
                .setInventoryState(InventoryStateType.InventoryState.newBuilder().setValue(inventoryState))
                .build();
        UnitConfig.Builder deviceUnitConfig = UnitConfig.newBuilder()
                .setPlacementConfig(getDefaultPlacement(Registries.getUnitRegistry().getUnitConfigByAlias(locationLabel)))
                .setDeviceConfig(tmp)
                .setUnitType(UnitType.DEVICE);
        deviceUnitConfig.addAlias(alias);
        LabelProcessor.addLabel(deviceUnitConfig.getLabelBuilder(), Locale.ENGLISH, alias);
        return deviceUnitConfig.build();
    }

    private static List<UnitTemplateConfig> generateUnitTemplateConfigs(List<UnitTemplate.UnitType> unitTypes) throws CouldNotPerformException {
        final List<UnitTemplateConfig> unitTemplateConfigs = new ArrayList<>();
        for (UnitTemplate.UnitType type : unitTypes) {
            Set<ServiceTemplateConfig> serviceTemplateConfigs = new HashSet<>();
            for (ServiceDescription serviceDescription : MockUnitTemplate.getUnitTemplate(type).getServiceDescriptionList()) {
                serviceTemplateConfigs.add(ServiceTemplateConfig.newBuilder().setServiceType(serviceDescription.getServiceType()).build());
            }
            UnitTemplateConfig config = UnitTemplateConfig.newBuilder().setUnitType(type).addAllServiceTemplateConfig(serviceTemplateConfigs).build();
            unitTemplateConfigs.add(config);
        }
        return unitTemplateConfigs;
    }

    public static DeviceClass registerDeviceClass(String label, String productNumber, String company, UnitTemplate.UnitType... types) throws CouldNotPerformException, ExecutionException, InterruptedException {
        List<UnitTemplate.UnitType> unitTypeList = new ArrayList<>(Arrays.asList(types));
        DeviceClass.Builder deviceClass = DeviceClass.newBuilder().setProductNumber(productNumber).setCompany(company)
                .setBindingConfig(getBindingConfig()).addAllUnitTemplateConfig(generateUnitTemplateConfigs(unitTypeList))
                .setShape(Shape.newBuilder().setBoundingBox(DEFAULT_BOUNDING_BOX));
        LabelProcessor.addLabel(deviceClass.getLabelBuilder(), Locale.ENGLISH, label);
        DeviceClass registeredDeviceClass = Registries.getClassRegistry().registerDeviceClass(deviceClass.build()).get();
        if (!Registries.getClassRegistry().containsDeviceClass(registeredDeviceClass)) {
            throw ExceptionPrinter.printHistoryAndReturnThrowable(new FatalImplementationErrorException("DeviceClass is not contained after registration task finished", MockRegistry.class), LOGGER);
        }
        return registeredDeviceClass;
    }

    public static BindingConfig getBindingConfig() {
        BindingConfig.Builder bindingConfigBuilder = BindingConfig.newBuilder();
        bindingConfigBuilder.setBindingId(MockRegistry.BINDING_OPENHAB);
        return bindingConfigBuilder.build();
    }

    public static void registerUnitConsistencyHandler(final ConsistencyHandler<String, IdentifiableMessage<String, UnitConfig, Builder>, ProtoBufMessageMap<String, UnitConfig, Builder>, ProtoBufRegistry<String, UnitConfig, Builder>> consistencyHandler) throws CouldNotPerformException {
        unitRegistryLauncher.getLaunchable().getDeviceUnitConfigRegistry().registerConsistencyHandler(consistencyHandler);
    }

    public static UnitConfig.Builder generateAgentConfig(final String agentClassLabel, final String alias, final String locationAlias) throws CouldNotPerformException {
        final UnitConfig.Builder agentUnitConfig = UnitConfig.newBuilder().setUnitType(UnitType.AGENT);
        agentUnitConfig.getAgentConfigBuilder().setAgentClassId(AGENT_CLASS_LABEL_ID_MAP.get(agentClassLabel));
        agentUnitConfig.getPlacementConfigBuilder().setLocationId(Registries.getUnitRegistry().getUnitConfigByAlias(locationAlias).getId());
        LabelProcessor.addLabel(agentUnitConfig.getLabelBuilder(), Locale.ENGLISH, alias);
        agentUnitConfig.addAlias(alias);
        return agentUnitConfig;
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

        Registries.shutdown();
    }

    private void registerLocations() throws CouldNotPerformException, InterruptedException {
        try {
            // Create paradise (root location)
            final List<Vec3DDouble> paradiseVertices = new ArrayList<>();
            paradiseVertices.add(Vec3DDouble.newBuilder().setX(0).setY(0).setZ(0).build());
            paradiseVertices.add(Vec3DDouble.newBuilder().setX(0).setY(6).setZ(0).build());
            paradiseVertices.add(Vec3DDouble.newBuilder().setX(6).setY(6).setZ(0).build());
            paradiseVertices.add(Vec3DDouble.newBuilder().setX(6).setY(0).setZ(0).build());
            Shape paradiseShape = Shape.newBuilder().addAllFloor(paradiseVertices).build();

            // rename default root location home into paradise test location.
            UnitConfig.Builder rootLocation = Registries.getUnitRegistry().getRootLocationConfig().toBuilder().clearLabel();
            rootLocation.getPlacementConfigBuilder().setShape(paradiseShape);
            LabelProcessor.addLabel(rootLocation.getLabelBuilder(), Locale.ENGLISH, ALIAS_LOCATION_ROOT_PARADISE);
            rootLocation.addAlias(ALIAS_LOCATION_ROOT_PARADISE);
            UnitConfig paradise = Registries.getUnitRegistry().updateUnitConfig(rootLocation.build()).get();
            LocationConfig tileLocationConfig = LocationConfig.newBuilder().setLocationType(LocationType.TILE).build();
            LocationConfig regionLocationConfig = LocationConfig.newBuilder().setLocationType(LocationType.REGION).build();

            // Create hell
            final List<Vec3DDouble> hellVertices = new ArrayList<>();
            hellVertices.add(Vec3DDouble.newBuilder().setX(0).setY(0).setZ(0).build());
            hellVertices.add(Vec3DDouble.newBuilder().setX(0).setY(4).setZ(0).build());
            hellVertices.add(Vec3DDouble.newBuilder().setX(2).setY(4).setZ(0).build());
            hellVertices.add(Vec3DDouble.newBuilder().setX(2).setY(0).setZ(0).build());
            Shape hellShape = Shape.newBuilder().addAllFloor(hellVertices).build();
            Pose hellPosition = Pose.newBuilder().setTranslation(Translation.newBuilder().setX(3).setY(1).setZ(0).build())
                    .setRotation(Rotation.newBuilder().setQw(1).setQx(0).setQy(0).setQz(0).build()).build();
            PlacementConfig hellPlacement = PlacementConfig.newBuilder().setPose(hellPosition).setShape(hellShape).setLocationId(paradise.getId()).build();
            registerUnitConfig(getLocationUnitConfig(ALIAS_LOCATION_HELL, tileLocationConfig, hellPlacement));

            // Create stairway to heaven
            final List<Vec3DDouble> stairwayVertices = new ArrayList<>();
            stairwayVertices.add(Vec3DDouble.newBuilder().setX(0).setY(0).setZ(0).build());
            stairwayVertices.add(Vec3DDouble.newBuilder().setX(0).setY(1).setZ(0).build());
            stairwayVertices.add(Vec3DDouble.newBuilder().setX(4).setY(1).setZ(0).build());
            stairwayVertices.add(Vec3DDouble.newBuilder().setX(4).setY(0).setZ(0).build());
            Shape stairwayShape = Shape.newBuilder().addAllFloor(stairwayVertices).build();
            Pose stairwayPosition = Pose.newBuilder().setTranslation(Translation.newBuilder().setX(1).setY(0).setZ(0).build())
                    .setRotation(Rotation.newBuilder().setQw(1).setQx(0).setQy(0).setQz(0).build()).build();
            PlacementConfig stairwayPlacement = PlacementConfig.newBuilder().setPose(stairwayPosition).setShape(stairwayShape).setLocationId(paradise.getId()).build();
            UnitConfig stairwayLocation = registerUnitConfig(getLocationUnitConfig(ALIAS_LOCATION_STAIRWAY_TO_HEAVEN, tileLocationConfig, stairwayPlacement));

            // Create heaven
            final List<Vec3DDouble> heavenVertices = new ArrayList<>();
            heavenVertices.add(Vec3DDouble.newBuilder().setX(0).setY(0).setZ(0).build());
            heavenVertices.add(Vec3DDouble.newBuilder().setX(0).setY(4).setZ(0).build());
            heavenVertices.add(Vec3DDouble.newBuilder().setX(2).setY(4).setZ(0).build());
            heavenVertices.add(Vec3DDouble.newBuilder().setX(2).setY(0).setZ(0).build());
            Shape heavenShape = Shape.newBuilder().addAllFloor(heavenVertices).build();
            Pose heavenPosition = Pose.newBuilder().setTranslation(Translation.newBuilder().setX(1).setY(1).setZ(0).build())
                    .setRotation(Rotation.newBuilder().setQw(1).setQx(0).setQy(0).setQz(0).build()).build();
            PlacementConfig heavenPlacement = PlacementConfig.newBuilder().setPose(heavenPosition).setShape(heavenShape).setLocationId(paradise.getId()).build();
            UnitConfig heavenLocation = registerUnitConfig(getLocationUnitConfig(ALIAS_LOCATION_HEAVEN, tileLocationConfig, heavenPlacement));

            // Create Garden of Eden
            final List<Vec3DDouble> edenVertices = new ArrayList<>();
            edenVertices.add(Vec3DDouble.newBuilder().setX(0).setY(0).setZ(0).build());
            edenVertices.add(Vec3DDouble.newBuilder().setX(0).setY(2).setZ(0).build());
            edenVertices.add(Vec3DDouble.newBuilder().setX(1).setY(2).setZ(0).build());
            edenVertices.add(Vec3DDouble.newBuilder().setX(1).setY(0).setZ(0).build());
            Shape edenShape = Shape.newBuilder().addAllFloor(edenVertices).build();
            Pose edenPosition = Pose.newBuilder().setTranslation(Translation.newBuilder().setX(0).setY(2).setZ(0).build())
                    .setRotation(Rotation.newBuilder().setQw(1).setQx(0).setQy(0).setQz(0).build()).build();
            PlacementConfig edenPlacement = PlacementConfig.newBuilder().setPose(edenPosition).setShape(edenShape).setLocationId(heavenLocation.getId()).build();
            registerUnitConfig(getLocationUnitConfig(ALIAS_LOCATION_GARDEN, regionLocationConfig, edenPlacement));
        } catch (ExecutionException ex) {
            throw new CouldNotPerformException(ex);
        }
    }

    private UnitConfig getLocationUnitConfig(final String alias, final LocationConfig locationConfig, final PlacementConfig placementConfig) {
        UnitConfig.Builder unitConfig = UnitConfig.newBuilder().setUnitType(UnitType.LOCATION).setLocationConfig(locationConfig).setPlacementConfig(placementConfig);
        LabelProcessor.addLabel(unitConfig.getLabelBuilder(), Locale.ENGLISH, alias);
        unitConfig.addAlias(alias);
        return unitConfig.build();
    }

    private void registerConnections() throws CouldNotPerformException, InterruptedException {
        try {
            List<String> tileIds = new ArrayList<>();
            tileIds.add(Registries.getUnitRegistry().getUnitConfigByAliasAndUnitType(ALIAS_LOCATION_HEAVEN, UnitType.LOCATION).getId());
            tileIds.add(Registries.getUnitRegistry().getUnitConfigByAliasAndUnitType(ALIAS_LOCATION_HELL, UnitType.LOCATION).getId());
            String reedContactId = Registries.getUnitRegistry().getUnitConfigByAlias(getUnitAlias(UnitType.REED_CONTACT)).getId();
            ConnectionConfig connectionConfig = ConnectionConfig.newBuilder().setConnectionType(ConnectionType.DOOR).addAllTileId(tileIds).addUnitId(reedContactId).build();
            registerUnitConfig(generateConnectionUnitConfig(ALIAS_DOOR_GATE, connectionConfig));

            tileIds.clear();
            tileIds.add(Registries.getUnitRegistry().getUnitConfigByAlias(ALIAS_LOCATION_HEAVEN).getId());
            tileIds.add(Registries.getUnitRegistry().getUnitConfigByAlias(ALIAS_LOCATION_STAIRWAY_TO_HEAVEN).getId());
            reedContactId = Registries.getUnitRegistry().getUnitConfigByAlias(ALIAS_REED_SWITCH_HEAVEN_STAIRWAY_GATE).getId();
            connectionConfig = ConnectionConfig.newBuilder().setConnectionType(ConnectionType.DOOR).addAllTileId(tileIds).addUnitId(reedContactId).build();
            registerUnitConfig(generateConnectionUnitConfig(ALIAS_DOOR_STAIRS_HEAVEN_GATE, connectionConfig));

            tileIds.clear();
            tileIds.add(Registries.getUnitRegistry().getUnitConfigByAlias(ALIAS_LOCATION_HELL).getId());
            tileIds.add(Registries.getUnitRegistry().getUnitConfigByAlias(ALIAS_LOCATION_STAIRWAY_TO_HEAVEN).getId());
            reedContactId = Registries.getUnitRegistry().getUnitConfigByAlias(ALIAS_REED_SWITCH_STAIRWAY_HELL_GATE).getId();
            connectionConfig = ConnectionConfig.newBuilder().setConnectionType(ConnectionType.DOOR).addAllTileId(tileIds).addUnitId(reedContactId).build();
            registerUnitConfig(generateConnectionUnitConfig(ALIAS_DOOR_STAIRWAY_HELL_GATE, connectionConfig));

            tileIds.clear();
            tileIds.add(Registries.getUnitRegistry().getUnitConfigByAlias(ALIAS_LOCATION_HELL).getId());
            tileIds.add(Registries.getUnitRegistry().getUnitConfigByAlias(ALIAS_LOCATION_STAIRWAY_TO_HEAVEN).getId());
            reedContactId = Registries.getUnitRegistry().getUnitConfigByAlias(ALIAS_REED_SWITCH_STAIRWAY_HELL_WINDOW).getId();
            connectionConfig = ConnectionConfig.newBuilder().setConnectionType(ConnectionType.WINDOW).addAllTileId(tileIds).addUnitId(reedContactId).build();
            registerUnitConfig(generateConnectionUnitConfig(ALIAS_WINDOW_STAIRWAY_HELL_LOOKOUT, connectionConfig));

        } catch (ExecutionException | IndexOutOfBoundsException ex) {
            throw new CouldNotPerformException(ex);
        }
    }

    private UnitConfig generateConnectionUnitConfig(final String alias, final ConnectionConfig connectionConfig) {
        UnitConfig.Builder connectionUnitConfig = UnitConfig.newBuilder().setUnitType(UnitType.CONNECTION).setConnectionConfig(connectionConfig);
        LabelProcessor.addLabel(connectionUnitConfig.getLabelBuilder(), Locale.ENGLISH, alias);
        connectionUnitConfig.addAlias(alias);
        return connectionUnitConfig.build();
    }

    private void registerUser() throws CouldNotPerformException, InterruptedException {
        UserConfig.Builder config = UserConfig.newBuilder().setFirstName(USER_FIRST_NAME).setLastName(USER_LAST_NAME).setUserName(USER_NAME);
        UnitConfig.Builder userUnitConfig = UnitConfig.newBuilder().setUnitType(UnitType.USER).setUserConfig(config).setEnablingState(EnablingState.newBuilder().setValue(EnablingState.State.ENABLED));
        userUnitConfig.getPermissionConfigBuilder().getOtherPermissionBuilder().setWrite(true).setAccess(true).setRead(true);
        userUnitConfig.addAlias(ALIAS_USER_MAX_MUSTERMANN);
        try {
            testUser = registerUnitConfig(userUnitConfig.build());
        } catch (ExecutionException ex) {
            throw new CouldNotPerformException(ex);
        }
    }

    private void registerAgentClasses() throws CouldNotPerformException, InterruptedException {
        try {
            AGENT_CLASS_LABEL_ID_MAP.put(LABEL_AGENT_CLASS_ABSENCE_ENERGY_SAVING, Registries.getClassRegistry().registerAgentClass(getAgentClass(LABEL_AGENT_CLASS_ABSENCE_ENERGY_SAVING)).get().getId());
            AGENT_CLASS_LABEL_ID_MAP.put(LABEL_AGENT_CLASS_FIRE_ALARM, Registries.getClassRegistry().registerAgentClass(getAgentClass(LABEL_AGENT_CLASS_FIRE_ALARM)).get().getId());
            AGENT_CLASS_LABEL_ID_MAP.put(LABEL_AGENT_CLASS_HEATER_ENERGY_SAVING, Registries.getClassRegistry().registerAgentClass(getAgentClass(LABEL_AGENT_CLASS_HEATER_ENERGY_SAVING)).get().getId());
            AGENT_CLASS_LABEL_ID_MAP.put(LABEL_AGENT_CLASS_ILLUMINATION_LIGHT_SAVING, Registries.getClassRegistry().registerAgentClass(getAgentClass(LABEL_AGENT_CLASS_ILLUMINATION_LIGHT_SAVING)).get().getId());
            AGENT_CLASS_LABEL_ID_MAP.put(LABEL_AGENT_CLASS_POWER_STATE_SYNCHRONISER, Registries.getClassRegistry().registerAgentClass(getAgentClass(LABEL_AGENT_CLASS_POWER_STATE_SYNCHRONISER)).get().getId());
            AGENT_CLASS_LABEL_ID_MAP.put(LABEL_AGENT_CLASS_PRESENCE_LIGHT, Registries.getClassRegistry().registerAgentClass(getAgentClass(LABEL_AGENT_CLASS_PRESENCE_LIGHT)).get().getId());
        } catch (ExecutionException ex) {
            throw new CouldNotPerformException(ex);
        }
    }

    private AgentClass getAgentClass(final String label) {
        AgentClass.Builder agentClass = AgentClass.newBuilder();
        LabelProcessor.addLabel(agentClass.getLabelBuilder(), Locale.ENGLISH, label);
        return agentClass.build();
    }

    private void registerDevices() throws CouldNotPerformException, InterruptedException {
        try {
            // colorable light
            DeviceClass colorableLightClass = registerDeviceClass(LABEL_DEVICE_CLASS_PHILIPS_HUE_E_27, "KV01_18U", COMPANY_PHILIPS, UnitType.COLORABLE_LIGHT);

            String serialNumber = "1234-5678-9100";
            registerUnitConfig(generateDeviceConfig(ALIAS_DEVICE_COLORABLE_LIGHT, serialNumber, colorableLightClass));
            registerUnitConfig(generateDeviceConfig(ALIAS_DEVICE_COLORABLE_LIGHT_BORROWED, serialNumber, InventoryState.State.BORROWED, colorableLightClass));
            registerUnitConfig(generateDeviceConfig(ALIAS_DEVICE_COLORABLE_LIGHT_DEVICE_STAIRWAY, serialNumber, colorableLightClass, ALIAS_LOCATION_STAIRWAY_TO_HEAVEN));
            registerUnitConfig(generateDeviceConfig(ALIAS_DEVICE_COLORABLE_LIGHT_HEAVEN, serialNumber, colorableLightClass, ALIAS_LOCATION_HEAVEN));
            registerUnitConfig(generateDeviceConfig(ALIAS_DEVICE_COLORABLE_LIGHT_HELL, serialNumber, colorableLightClass, ALIAS_LOCATION_HELL));

            // battery, brightnessSensor, motionSensor, tamperSwitch, temperatureSensor
            DeviceClass motionSensorClass = registerDeviceClass(LABEL_DEVICE_CLASS_FIBARO_MOTION_SENSOR, "FGMS_001", COMPANY_FIBARO,
                    UnitType.MOTION_DETECTOR,
                    UnitType.BATTERY,
                    UnitType.LIGHT_SENSOR,
                    UnitType.TEMPERATURE_SENSOR,
                    UnitType.TAMPER_DETECTOR);

            registerUnitConfig(generateDeviceConfig(ALIAS_DEVICE_MOTION_SENSOR, serialNumber, motionSensorClass));
            registerUnitConfig(generateDeviceConfig(ALIAS_DEVICE_MOTION_SENSOR_STAIRWAY, serialNumber, motionSensorClass, ALIAS_LOCATION_STAIRWAY_TO_HEAVEN));
            registerUnitConfig(generateDeviceConfig(ALIAS_DEVICE_MOTION_SENSOR_HEAVEN, serialNumber, motionSensorClass, ALIAS_LOCATION_HEAVEN));
            registerUnitConfig(generateDeviceConfig(ALIAS_DEVICE_MOTION_SENSOR_HELL, serialNumber, motionSensorClass, ALIAS_LOCATION_HELL));

            // button
            DeviceClass buttonClass = registerDeviceClass(LABEL_DEVICE_CLASS_GIRA_429496730210000, "429496730210000", COMPANY_GIRA,
                    UnitType.BUTTON);

            registerUnitConfig(generateDeviceConfig(ALIAS_DEVICE_GI_429496730210000_DEVICE, serialNumber, buttonClass));

            // dimmableLight
            DeviceClass dimmableLightClass = registerDeviceClass(LABEL_DEVICE_CLASS_HAGER_ABC, "ABC", COMPANY_HAGER,
                    UnitType.DIMMABLE_LIGHT);

            registerUnitConfig(generateDeviceConfig(ALIAS_DEVICE_HA_ABC, serialNumber, dimmableLightClass));

            // dimmer
            DeviceClass dimmerClass = registerDeviceClass(LABEL_DEVICE_CLASS_HAGER_TYA_663_A1, "TYA663A", COMPANY_HAGER,
                    UnitType.DIMMER);

            registerUnitConfig(generateDeviceConfig(ALIAS_DEVICE_HA_TYA_663_A, serialNumber, dimmerClass));

            // handle
            DeviceClass handleClass = registerDeviceClass(LABEL_DEVICE_CLASS_HOMEMATIC_ROTARY_HANDLE_SENSOR, "Sec_RHS", COMPANY_HOMEMATIC,
                    UnitType.HANDLE);

            registerUnitConfig(generateDeviceConfig(ALIAS_DEVICE_HM_ROTARY_HANDLE_SENSOR, serialNumber, handleClass));

            // light
            DeviceClass lightClass = registerDeviceClass(LABEL_DEVICE_CLASS_FIBARO_FGS_221, "FGS_221", COMPANY_FIBARO,
                    UnitType.LIGHT);

            registerUnitConfig(generateDeviceConfig(ALIAS_DEVICE_F_FGS_221, serialNumber, lightClass));

            // powerConsumptionSensor, powerPlug
            DeviceClass powerPlugClass = registerDeviceClass(
                    LABEL_DEVICE_CLASS_PLUGWISE_POWER_PLUG,
                    "070140",
                    COMPANY_PLUGWISE,
                    UnitType.POWER_SWITCH,
                    UnitType.POWER_CONSUMPTION_SENSOR);

            registerUnitConfig(generateDeviceConfig(ALIAS_DEVICE_POWER_PLUG, serialNumber, powerPlugClass));

            // reedSwitch
            DeviceClass reedSwitchClass = registerDeviceClass(LABEL_DEVICE_CLASS_HOMEMATIC_REED_SWITCH, "Sec_SC_2", COMPANY_HOMEMATIC, UnitType.REED_CONTACT);

            registerUnitConfig(generateDeviceConfig(ALIAS_DEVICE_REED_SWITCH_HOMEMATIC, serialNumber, reedSwitchClass));

            registerDALUnitAlias(
                    registerUnitConfig(generateDeviceConfig(ALIAS_DEVICE_REED_SWITCH_HEAVEN_STAIRS_DOOR, serialNumber, reedSwitchClass, ALIAS_LOCATION_STAIRWAY_TO_HEAVEN)),
                    UnitType.REED_CONTACT,
                    ALIAS_REED_SWITCH_HEAVEN_STAIRWAY_GATE);

            registerDALUnitAlias(
                    registerUnitConfig(generateDeviceConfig(ALIAS_DEVICE_REED_SWITCH_HELL_STAIRS_DOOR, serialNumber, reedSwitchClass, ALIAS_LOCATION_STAIRWAY_TO_HEAVEN)),
                    UnitType.REED_CONTACT,
                    ALIAS_REED_SWITCH_STAIRWAY_HELL_GATE);

            registerDALUnitAlias(
                    registerUnitConfig(generateDeviceConfig(ALIAS_DEVICE_REED_SWITCH_STAIRWAY_WINDOW, serialNumber, reedSwitchClass, ALIAS_LOCATION_STAIRWAY_TO_HEAVEN)),
                    UnitType.REED_CONTACT,
                    ALIAS_REED_SWITCH_STAIRWAY_HELL_WINDOW);

            // roller shutter
            DeviceClass rollerShutterClass = registerDeviceClass(LABEL_DEVICE_CLASS_HAGER_TYA_628_C, "TYA628C", COMPANY_HAGER,
                    UnitType.ROLLER_SHUTTER);

            registerUnitConfig(generateDeviceConfig(ALIAS_DEVICE_ROLLERSHUTTER, serialNumber, rollerShutterClass));
            registerUnitConfig(generateDeviceConfig(ALIAS_DEVICE_ROLLERSHUTTER_STAIRWAY, serialNumber, rollerShutterClass, ALIAS_LOCATION_STAIRWAY_TO_HEAVEN));

            // smoke detector
            DeviceClass smokeDetector = registerDeviceClass(LABEL_DEVICE_CLASS_FIBARO_FGSS_001, "FGSS_001", COMPANY_FIBARO,
                    UnitType.SMOKE_DETECTOR);

            registerUnitConfig(generateDeviceConfig(ALIAS_DEVICE_SMOKE_DETECTOR, serialNumber, smokeDetector));
            registerUnitConfig(generateDeviceConfig(ALIAS_DEVICE_SMOKE_DETECTOR_STAIRWAY, serialNumber, smokeDetector, ALIAS_LOCATION_STAIRWAY_TO_HEAVEN));

            // temperature controller
            DeviceClass temperatureControllerClass = registerDeviceClass(LABEL_DEVICE_CLASS_GIRA_429496730250000, "429496730250000", COMPANY_GIRA,
                    UnitType.TEMPERATURE_CONTROLLER);

            registerUnitConfig(generateDeviceConfig(ALIAS_DEVICE_TEMPERATURE_CONTROLLER, serialNumber, temperatureControllerClass));
            registerUnitConfig(generateDeviceConfig(ALIAS_DEVICE_TEMPERATURE_CONTROLLER_STAIRWAY_TO_HEAVEN, serialNumber, temperatureControllerClass, ALIAS_LOCATION_STAIRWAY_TO_HEAVEN));

        } catch (ExecutionException ex) {
            throw new CouldNotPerformException(ex);
        }
    }

    private void registerDALUnitAlias(final UnitConfig deviceUnitConfig, final UnitType unitType, final String... alias) throws CouldNotPerformException, ExecutionException, InterruptedException {
        try {
            final ArrayList<UnitConfig> dalUnits = new ArrayList();

            // iterate over provided dal units
            for (String dalUnitId : Registries.getUnitRegistry().getUnitConfigByIdAndUnitType(deviceUnitConfig.getId(), UnitType.DEVICE).getDeviceConfig().getUnitIdList()) {
                // lookup dal unit
                final UnitConfig dalUnitConfig = Registries.getUnitRegistry().getUnitConfigById(dalUnitId);

                // lookup dal units
                if (dalUnitConfig.getUnitType() == unitType) {
                    dalUnits.add(dalUnitConfig);
                }
            }

            // validate
            if (dalUnits.isEmpty()) {
                throw new InvalidStateException(LabelProcessor.getBestMatch(deviceUnitConfig.getLabel()) + " does not provide a " + unitType.name());
            }
            if (alias.length != dalUnits.size()) {
                throw new InvalidStateException(LabelProcessor.getBestMatch(deviceUnitConfig.getLabel()) + "s amount of " + unitType.name() + " does not match alias amount of " + alias.length);
            }

            // setup aliases
            for (int i = 0; i < alias.length; i++) {
                final Builder builder = dalUnits.get(i).toBuilder();

                // add alias
                builder.addAlias(alias[i]);

                // update unit config
                Registries.getUnitRegistry().updateUnitConfig(builder.build()).get();

                // validate that get on the unit registry returns the updated config by validating if it contains the alias
                final UnitConfig unitConfigById = Registries.getUnitRegistry().getUnitConfigById(builder.getId());
                boolean containsNewAlias = false;
                for (String ali : unitConfigById.getAliasList()) {
                    if (ali.equals(alias[i])) {
                        containsNewAlias = true;
                        break;
                    }
                }
                if (!containsNewAlias) {
                    LOGGER.error("Unit [" + unitConfigById + "] does not contain new alias [" + alias[i] + "]");
                    throw ExceptionPrinter.printHistoryAndReturnThrowable(
                            new FatalImplementationErrorException("Sync error after adding alias [" + alias[i] + "]", MockRegistry.class), LOGGER);
                }
            }
        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not setup Alias[" + Arrays.toString(alias) + "] for DalUnit[" + unitType.name() + "] of Device[" + LabelProcessor.getBestMatch(deviceUnitConfig.getLabel(), "?") + "]", ex);
        }
    }

    public enum MockServiceTemplate {
        ACTIVATION_STATE_SERVICE(ServiceType.ACTIVATION_STATE_SERVICE, CommunicationType.ACTIVATION_STATE),
        ACTIVITY_MULTI_STATE_SERVICE(ServiceType.ACTIVITY_MULTI_STATE_SERVICE, CommunicationType.ACTIVITY_MULTI_STATE),
        BATTERY_STATE_SERVICE(ServiceType.BATTERY_STATE_SERVICE, CommunicationType.BATTERY_STATE),
        BLIND_STATE_SERVICE(ServiceType.BLIND_STATE_SERVICE, CommunicationType.BLIND_STATE),
        BRIGHTNESS_STATE_SERVICE(ServiceType.BRIGHTNESS_STATE_SERVICE, CommunicationType.BRIGHTNESS_STATE),
        BUTTON_STATE_SERVICE(ServiceType.BUTTON_STATE_SERVICE, CommunicationType.BUTTON_STATE),
        COLOR_STATE_SERVICE(ServiceType.COLOR_STATE_SERVICE, CommunicationType.COLOR_STATE),
        CONTACT_STATE_SERVICE(ServiceType.CONTACT_STATE_SERVICE, CommunicationType.CONTACT_STATE),
        DOOR_STATE_SERVICE(ServiceType.DOOR_STATE_SERVICE, CommunicationType.DOOR_STATE),
        EARTHQUAKE_ALARM_STATE_SERVICE(ServiceType.EARTHQUAKE_ALARM_STATE_SERVICE, CommunicationType.ALARM_STATE),
        EMPHASIS_STATE_SERVICE(ServiceType.EMPHASIS_STATE_SERVICE, CommunicationType.EMPHASIS_STATE),
        FIRE_ALARM_STATE_SERVICE(ServiceType.FIRE_ALARM_STATE_SERVICE, CommunicationType.ALARM_STATE),
        GLOBAL_POSITION_STATE_SERVICE(ServiceType.GLOBAL_POSITION_STATE_SERVICE, CommunicationType.GLOBAL_POSITION_STATE),
        HANDLE_STATE_SERVICE(ServiceType.HANDLE_STATE_SERVICE, CommunicationType.HANDLE_STATE),
        ILLUMINANCE_STATE_SERVICE(ServiceType.ILLUMINANCE_STATE_SERVICE, CommunicationType.ILLUMINANCE_STATE),
        INTRUSION_ALARM_STATE_SERVICE(ServiceType.INTRUSION_ALARM_STATE_SERVICE, CommunicationType.ALARM_STATE),
        LOCAL_POSITION_STATE_SERVICE(ServiceType.LOCAL_POSITION_STATE_SERVICE, CommunicationType.LOCAL_POSITION_STATE),
        MEDICAL_EMERGENCY_ALARM_STATE_SERVICE(ServiceType.MEDICAL_EMERGENCY_ALARM_STATE_SERVICE, CommunicationType.ALARM_STATE),
        MOTION_STATE_SERVICE(ServiceType.MOTION_STATE_SERVICE, CommunicationType.MOTION_STATE),
        PASSAGE_STATE_SERVICE(ServiceType.PASSAGE_STATE_SERVICE, CommunicationType.PASSAGE_STATE),
        POWER_CONSUMPTION_STATE_SERVICE(ServiceType.POWER_CONSUMPTION_STATE_SERVICE, CommunicationType.POWER_CONSUMPTION_STATE),
        POWER_STATE_SERVICE(ServiceType.POWER_STATE_SERVICE, CommunicationType.POWER_STATE),
        PRESENCE_STATE_SERVICE(ServiceType.PRESENCE_STATE_SERVICE, CommunicationType.PRESENCE_STATE),
        R_F_I_D_STATE_SERVICE(ServiceType.R_F_I_D_STATE_SERVICE, CommunicationType.R_F_I_D_STATE),
        SMOKE_ALARM_STATE_SERVICE(ServiceType.SMOKE_ALARM_STATE_SERVICE, CommunicationType.ALARM_STATE),
        SMOKE_STATE_SERVICE(ServiceType.SMOKE_STATE_SERVICE, CommunicationType.SMOKE_STATE),
        STANDBY_STATE_SERVICE(ServiceType.STANDBY_STATE_SERVICE, CommunicationType.STANDBY_STATE),
        SWITCH_STATE_SERVICE(ServiceType.SWITCH_STATE_SERVICE, CommunicationType.SWITCH_STATE),
        TAMPER_STATE_SERVICE(ServiceType.TAMPER_STATE_SERVICE, CommunicationType.TAMPER_STATE),
        TARGET_TEMPERATURE_STATE_SERVICE(ServiceType.TARGET_TEMPERATURE_STATE_SERVICE, CommunicationType.TEMPERATURE_STATE),
        TEMPERATURE_ALARM_STATE_SERVICE(ServiceType.TEMPERATURE_ALARM_STATE_SERVICE, CommunicationType.ALARM_STATE),
        TEMPERATURE_STATE_SERVICE(ServiceType.TEMPERATURE_STATE_SERVICE, CommunicationType.TEMPERATURE_STATE),
        TEMPEST_ALARM_STATE_SERVICE(ServiceType.TEMPEST_ALARM_STATE_SERVICE, CommunicationType.ALARM_STATE),
        USER_TRANSIT_STATE_SERVICE(ServiceType.USER_TRANSIT_STATE_SERVICE, CommunicationType.USER_TRANSIT_STATE),
        WATER_ALARM_STATE_SERVICE(ServiceType.WATER_ALARM_STATE_SERVICE, CommunicationType.ALARM_STATE),
        WINDOW_STATE_SERVICE(ServiceType.WINDOW_STATE_SERVICE, CommunicationType.WINDOW_STATE);


        private final ServiceTemplate serviceTemplate;

        MockServiceTemplate(final ServiceType serviceType, final CommunicationType communicationType) {
            ServiceTemplate.Builder serviceTemplateBuilder = ServiceTemplate.newBuilder().setServiceType(serviceType).setCommunicationType(communicationType);

            switch (serviceTemplateBuilder.getServiceType()) {
                case BRIGHTNESS_STATE_SERVICE:
                    serviceTemplateBuilder.addSuperType(ServiceType.POWER_STATE_SERVICE);
                    break;
                case COLOR_STATE_SERVICE:
                    serviceTemplateBuilder.addSuperType(ServiceType.BRIGHTNESS_STATE_SERVICE);
                    break;
            }
            this.serviceTemplate = serviceTemplateBuilder.build();
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
        ACTIVATION_SOS(ServiceType.ACTIVATION_STATE_SERVICE, ServicePattern.OPERATION),
        ACTIVATION_SPS(ServiceType.ACTIVATION_STATE_SERVICE, ServicePattern.PROVIDER),
        ACTIVITY_MULTI_SOS(ServiceType.ACTIVITY_MULTI_STATE_SERVICE, ServicePattern.OPERATION),
        ACTIVITY_MULTI_SPS(ServiceType.ACTIVITY_MULTI_STATE_SERVICE, ServicePattern.PROVIDER),
        BATTERY_SPS(ServiceType.BATTERY_STATE_SERVICE, ServicePattern.PROVIDER),
        BLIND_SOS(ServiceType.BLIND_STATE_SERVICE, ServicePattern.OPERATION),
        BLIND_SPS(ServiceType.BLIND_STATE_SERVICE, ServicePattern.PROVIDER),
        BRIGHTNESS_SOS(ServiceType.BRIGHTNESS_STATE_SERVICE, ServicePattern.OPERATION),
        BRIGHTNESS_SPS(ServiceType.BRIGHTNESS_STATE_SERVICE, ServicePattern.PROVIDER),
        EMPHASIN_SOS(ServiceType.EMPHASIS_STATE_SERVICE, ServicePattern.OPERATION),
        EMPHASIN_SPS(ServiceType.EMPHASIS_STATE_SERVICE, ServicePattern.PROVIDER),
        BUTTON_SPS(ServiceType.BUTTON_STATE_SERVICE, ServicePattern.PROVIDER),
        DOOR_SPS(ServiceType.DOOR_STATE_SERVICE, ServicePattern.PROVIDER),
        WINDOW_SPS(ServiceType.WINDOW_STATE_SERVICE, ServicePattern.PROVIDER),
        PASSAGE_SPS(ServiceType.PASSAGE_STATE_SERVICE, ServicePattern.PROVIDER),
        COLOR_SOS(ServiceType.COLOR_STATE_SERVICE, ServicePattern.OPERATION),
        COLOR_SPS(ServiceType.COLOR_STATE_SERVICE, ServicePattern.PROVIDER),
        CONTACT_SPS(ServiceType.CONTACT_STATE_SERVICE, ServicePattern.PROVIDER),
        GLOBAL_POSITION_SOS(ServiceType.GLOBAL_POSITION_STATE_SERVICE, ServicePattern.OPERATION),
        GLOBAL_POSITION_SPS(ServiceType.GLOBAL_POSITION_STATE_SERVICE, ServicePattern.PROVIDER),
        HANDLE_SPS(ServiceType.HANDLE_STATE_SERVICE, ServicePattern.PROVIDER),
        ILLUMINANCE_SPS(ServiceType.ILLUMINANCE_STATE_SERVICE, ServicePattern.PROVIDER),
        LOCAL_POSITION_SOS(ServiceType.LOCAL_POSITION_STATE_SERVICE, ServicePattern.OPERATION),
        LOCAL_POSITION_SPS(ServiceType.LOCAL_POSITION_STATE_SERVICE, ServicePattern.PROVIDER),
        MOTION_SPS(ServiceType.MOTION_STATE_SERVICE, ServicePattern.PROVIDER),
        POWER_CONSUMPTION_SPS(ServiceType.POWER_CONSUMPTION_STATE_SERVICE, ServicePattern.PROVIDER),
        POWER_SOS(ServiceType.POWER_STATE_SERVICE, ServicePattern.OPERATION),
        POWER_SPS(ServiceType.POWER_STATE_SERVICE, ServicePattern.PROVIDER),
        PRESENCE_SOS(ServiceType.PRESENCE_STATE_SERVICE, ServicePattern.OPERATION),
        PRESENCE_SPS(ServiceType.PRESENCE_STATE_SERVICE, ServicePattern.PROVIDER),
        SMOKE_ALARM_SPS(ServiceType.SMOKE_ALARM_STATE_SERVICE, ServicePattern.PROVIDER),
        SMOKE_SPS(ServiceType.SMOKE_STATE_SERVICE, ServicePattern.PROVIDER),
        STANDBY_SOS(ServiceType.STANDBY_STATE_SERVICE, ServicePattern.OPERATION),
        STANDBY_SPS(ServiceType.STANDBY_STATE_SERVICE, ServicePattern.PROVIDER),
        TAMPER_SPS(ServiceType.TAMPER_STATE_SERVICE, ServicePattern.PROVIDER),
        TARGET_TEMPERATURE_SOS(ServiceType.TARGET_TEMPERATURE_STATE_SERVICE, ServicePattern.OPERATION),
        TARGET_TEMPERATURE_SPS(ServiceType.TARGET_TEMPERATURE_STATE_SERVICE, ServicePattern.PROVIDER),
        TEMPERATURE_SPS(ServiceType.TEMPERATURE_STATE_SERVICE, ServicePattern.PROVIDER),
        USER_TRANSIT_SOS(ServiceType.USER_TRANSIT_STATE_SERVICE, ServicePattern.OPERATION),
        USER_TRANSIT_SPS(ServiceType.USER_TRANSIT_STATE_SERVICE, ServicePattern.PROVIDER);

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

        COLORABLE_LIGHT(UnitType.COLORABLE_LIGHT, UnitType.DIMMABLE_LIGHT, COLOR_SOS, COLOR_SPS, POWER_SOS, POWER_SPS, BRIGHTNESS_SOS, BRIGHTNESS_SPS),
        DIMMABLE_LIGHT(UnitType.DIMMABLE_LIGHT, UnitType.LIGHT, POWER_SOS, POWER_SPS, BRIGHTNESS_SOS, BRIGHTNESS_SPS),
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
                SMOKE_ALARM_SPS, SMOKE_SPS, BRIGHTNESS_SOS, BRIGHTNESS_SPS, STANDBY_SPS, STANDBY_SOS, TAMPER_SPS, TARGET_TEMPERATURE_SPS, TARGET_TEMPERATURE_SOS, TEMPERATURE_SPS, PRESENCE_SPS, EMPHASIN_SPS, EMPHASIN_SOS),
        CONNECTION(UnitType.CONNECTION, DOOR_SPS, WINDOW_SPS, PASSAGE_SPS),
        SCENE(UnitType.SCENE, ACTIVATION_SPS, ACTIVATION_SOS),
        AGENT(UnitType.AGENT, ACTIVATION_SPS, ACTIVATION_SOS),
        APP(UnitType.APP, ACTIVATION_SPS, ACTIVATION_SOS),
        UNIT_GROUP(UnitType.UNIT_GROUP, COLOR_SPS, COLOR_SOS, ILLUMINANCE_SPS, MOTION_SPS, POWER_CONSUMPTION_SPS, POWER_SPS, POWER_SOS, BLIND_SPS, BLIND_SOS,
                SMOKE_ALARM_SPS, SMOKE_SPS, BRIGHTNESS_SOS, BRIGHTNESS_SPS, STANDBY_SPS, STANDBY_SOS, TAMPER_SPS, TARGET_TEMPERATURE_SPS, TARGET_TEMPERATURE_SOS, TEMPERATURE_SPS, PRESENCE_SPS, EMPHASIN_SPS, EMPHASIN_SOS),
        USER(UnitType.USER, LOCAL_POSITION_SOS, LOCAL_POSITION_SPS, GLOBAL_POSITION_SOS, GLOBAL_POSITION_SPS, PRESENCE_SOS, PRESENCE_SPS,
                USER_TRANSIT_SOS, USER_TRANSIT_SPS, ACTIVITY_MULTI_SOS, ACTIVITY_MULTI_SPS);

        private final UnitTemplate template;

        MockUnitTemplate(final UnitType type, final MockServiceDescription... serviceTemplates) {
            this(type, null, serviceTemplates);
        }

        MockUnitTemplate(final UnitType type, final UnitType superType, final MockServiceDescription... serviceTemplates) {
            final UnitTemplate.Builder unitTemplateBuilder = UnitTemplate.newBuilder();
            unitTemplateBuilder.setUnitType(type);

            if (superType != null) {
                unitTemplateBuilder.addSuperType(superType);
            }

            for (final MockServiceDescription serviceTemplate : serviceTemplates) {
                unitTemplateBuilder.addServiceDescription(serviceTemplate.getDescription());
            }

            if (type == UnitType.UNIT_GROUP) {
                for (final ServiceDescription.Builder serviceDescription : unitTemplateBuilder.getServiceDescriptionBuilderList()) {
                    serviceDescription.setAggregated(true);
                }
            }

            if (type == UnitType.LOCATION) {
                for (final ServiceDescription.Builder serviceDescription : unitTemplateBuilder.getServiceDescriptionBuilderList()) {
                    switch (serviceDescription.getServiceType()) {
                        case PRESENCE_STATE_SERVICE:
                        case STANDBY_STATE_SERVICE:
                        case EMPHASIS_STATE_SERVICE:
                            break;
                        default:
                            serviceDescription.setAggregated(true);
                            break;
                    }
                }
            }

            switch (type) {
                case COLORABLE_LIGHT:
                    unitTemplateBuilder.addSuperType(UnitType.DIMMABLE_LIGHT);
                    break;
                case DIMMABLE_LIGHT:
                    unitTemplateBuilder.addSuperType(UnitType.LIGHT);
                    break;
            }

            this.template = unitTemplateBuilder.build();
        }

        public static UnitTemplate getUnitTemplate(UnitType type) throws CouldNotPerformException {
            for (MockUnitTemplate templateType : values()) {
                if (templateType.getUnitTemplate().getUnitType() == type) {
                    return templateType.getUnitTemplate();
                }
            }
            throw new CouldNotPerformException("Could not find template for " + type + "!");
        }

        public UnitTemplate getUnitTemplate() {
            return template;
        }
    }
}
