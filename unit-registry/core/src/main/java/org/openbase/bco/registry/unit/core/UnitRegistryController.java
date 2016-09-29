package org.openbase.bco.registry.unit.core;

/*
 * #%L
 * REM UnitRegistry Core
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
import java.util.List;
import java.util.concurrent.Future;
import org.openbase.bco.registry.lib.controller.AbstractRegistryController;
import org.openbase.bco.registry.scene.lib.jp.JPUnitConfigDatabaseDirectory;
import org.openbase.bco.registry.unit.core.consistency.ServiceConfigUnitIdConsistencyHandler;
import org.openbase.bco.registry.unit.core.consistency.UnitConfigUnitTemplateConsistencyHandler;
import org.openbase.bco.registry.unit.core.consistency.UnitEnablingStateConsistencyHandler;
import org.openbase.bco.registry.unit.core.consistency.agent.AgentLabelConsistencyHandler;
import org.openbase.bco.registry.unit.core.consistency.agent.AgentLocationConsistencyHandler;
import org.openbase.bco.registry.unit.core.consistency.agent.AgentScopeConsistencyHandler;
import org.openbase.bco.registry.unit.core.consistency.app.AppLabelConsistencyHandler;
import org.openbase.bco.registry.unit.core.consistency.app.AppLocationConsistencyHandler;
import org.openbase.bco.registry.unit.core.consistency.app.AppScopeConsistencyHandler;
import org.openbase.bco.registry.unit.core.consistency.authorizationgroup.AuthorizationGroupConfigLabelConsistencyHandler;
import org.openbase.bco.registry.unit.core.consistency.authorizationgroup.AuthorizationGroupConfigScopeConsistencyHandler;
import org.openbase.bco.registry.unit.core.consistency.connection.ConnectionLabelConsistencyHandler;
import org.openbase.bco.registry.unit.core.consistency.connection.ConnectionLocationConsistencyHandler;
import org.openbase.bco.registry.unit.core.consistency.connection.ConnectionScopeConsistencyHandler;
import org.openbase.bco.registry.unit.core.consistency.connection.ConnectionTilesConsistencyHandler;
import org.openbase.bco.registry.unit.core.consistency.connection.ConnectionTransformationFrameConsistencyHandler;
import org.openbase.bco.registry.unit.core.consistency.dal.DalUnitEnablingStateConsistencyHandler;
import org.openbase.bco.registry.unit.core.consistency.dal.DalUnitHostIdConsistencyHandler;
import org.openbase.bco.registry.unit.core.consistency.dal.DalUnitLabelConsistencyHandler;
import org.openbase.bco.registry.unit.core.consistency.dal.DalUnitLocationIdConsistencyHandler;
import org.openbase.bco.registry.unit.core.consistency.dal.DalUnitScopeConsistencyHandler;
import org.openbase.bco.registry.unit.core.consistency.dal.UnitBoundToHostConsistencyHandler;
import org.openbase.bco.registry.unit.core.consistency.dal.UnitTransformationFrameConsistencyHandler;
import org.openbase.bco.registry.unit.core.consistency.device.DeviceConfigDeviceClassIdConsistencyHandler;
import org.openbase.bco.registry.unit.core.consistency.device.DeviceConfigDeviceClassUnitConsistencyHandler;
import org.openbase.bco.registry.unit.core.consistency.device.DeviceConfigLocationIdForInstalledDevicesConsistencyHandler;
import org.openbase.bco.registry.unit.core.consistency.device.DeviceEnablingStateConsistencyHandler;
import org.openbase.bco.registry.unit.core.consistency.device.DeviceLabelConsistencyHandler;
import org.openbase.bco.registry.unit.core.consistency.device.DeviceLocationIdConsistencyHandler;
import org.openbase.bco.registry.unit.core.consistency.device.DeviceOwnerConsistencyHandler;
import org.openbase.bco.registry.unit.core.consistency.device.DeviceScopeConsistencyHandler;
import org.openbase.bco.registry.unit.core.consistency.device.DeviceTransformationFrameConsistencyHandler;
import org.openbase.bco.registry.unit.core.consistency.device.OpenhabServiceConfigItemIdConsistencyHandler;
import org.openbase.bco.registry.unit.core.consistency.device.SyncBindingConfigDeviceClassUnitConsistencyHandler;
import org.openbase.bco.registry.unit.core.consistency.location.ChildWithSameLabelConsistencyHandler;
import org.openbase.bco.registry.unit.core.consistency.location.LocationChildConsistencyHandler;
import org.openbase.bco.registry.unit.core.consistency.location.LocationIdConsistencyHandler;
import org.openbase.bco.registry.unit.core.consistency.location.LocationLoopConsistencyHandler;
import org.openbase.bco.registry.unit.core.consistency.location.LocationParentConsistencyHandler;
import org.openbase.bco.registry.unit.core.consistency.location.LocationPlacementConfigConsistencyHandler;
import org.openbase.bco.registry.unit.core.consistency.location.LocationPositionConsistencyHandler;
import org.openbase.bco.registry.unit.core.consistency.location.LocationScopeConsistencyHandler;
import org.openbase.bco.registry.unit.core.consistency.location.LocationTransformationFrameConsistencyHandler;
import org.openbase.bco.registry.unit.core.consistency.location.LocationUnitIdConsistencyHandler;
import org.openbase.bco.registry.unit.core.consistency.location.RootConsistencyHandler;
import org.openbase.bco.registry.unit.core.consistency.location.RootLocationExistencConsistencyHandler;
import org.openbase.bco.registry.unit.core.consistency.scene.SceneLabelConsistencyHandler;
import org.openbase.bco.registry.unit.core.consistency.scene.SceneScopeConsistencyHandler;
import org.openbase.bco.registry.unit.core.consistency.unitgroup.UnitGroupMemberExistsConsistencyHandler;
import org.openbase.bco.registry.unit.core.consistency.unitgroup.UnitGroupMemberListDuplicationConsistencyHandler;
import org.openbase.bco.registry.unit.core.consistency.unitgroup.UnitGroupMemberListTypesConsistencyHandler;
import org.openbase.bco.registry.unit.core.consistency.unitgroup.UnitGroupScopeConsistencyHandler;
import org.openbase.bco.registry.unit.core.consistency.unitgroup.UnitGroupUnitTypeConsistencyHandler;
import org.openbase.bco.registry.unit.core.consistency.user.UserConfigScopeConsistencyHandler;
import org.openbase.bco.registry.unit.core.consistency.user.UserConfigUserNameConsistencyHandler;
import org.openbase.bco.registry.unit.core.dbconvert.AgentConfig_0_To_1_DBConverter;
import org.openbase.bco.registry.unit.core.dbconvert.DeviceConfig_0_To_1_DBConverter;
import org.openbase.bco.registry.unit.core.dbconvert.DummyConverter;
import org.openbase.bco.registry.unit.core.dbconvert.LocationConfig_0_To_1_DBConverter;
import org.openbase.bco.registry.unit.core.dbconvert.SceneConfig_0_To_1_DBConverter;
import org.openbase.bco.registry.unit.core.dbconvert.UnitGroupConfig_0_To_1_DBConverter;
import org.openbase.bco.registry.unit.core.dbconvert.UnitTemplate_0_To_1_DBConverter;
import org.openbase.bco.registry.unit.core.plugin.PublishConnectionTransformationRegistryPlugin;
import org.openbase.bco.registry.unit.core.plugin.PublishDalUnitTransformationRegistryPlugin;
import org.openbase.bco.registry.unit.core.plugin.PublishDeviceTransformationRegistryPlugin;
import org.openbase.bco.registry.unit.core.plugin.PublishLocationTransformationRegistryPlugin;
import org.openbase.bco.registry.unit.core.plugin.UnitTemplateCreatorRegistryPlugin;
import org.openbase.bco.registry.unit.lib.UnitRegistry;
import org.openbase.bco.registry.unit.lib.generator.UnitConfigIdGenerator;
import org.openbase.bco.registry.unit.lib.generator.UnitTemplateIdGenerator;
import org.openbase.bco.registry.unit.lib.jp.JPUnitRegistryScope;
import org.openbase.bco.registry.unit.lib.jp.JPUnitTemplateDatabaseDirectory;
import org.openbase.jps.core.JPService;
import org.openbase.jps.exception.JPServiceException;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.exception.printer.LogLevel;
import org.openbase.jul.extension.rsb.com.RPCHelper;
import org.openbase.jul.extension.rsb.iface.RSBLocalServer;
import org.openbase.jul.iface.Manageable;
import org.openbase.jul.schedule.GlobalExecutionService;
import org.openbase.jul.storage.file.ProtoBufJSonFileProvider;
import org.openbase.jul.storage.registry.ProtoBufFileSynchronizedRegistry;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rst.authorization.AuthorizationGroupConfigType.AuthorizationGroupConfig;
import rst.authorization.UserConfigType.UserConfig;
import rst.homeautomation.control.agent.AgentConfigType.AgentConfig;
import rst.homeautomation.control.app.AppConfigType.AppConfig;
import rst.homeautomation.control.scene.SceneConfigType.SceneConfig;
import rst.homeautomation.device.DeviceConfigType.DeviceConfig;
import rst.homeautomation.unit.UnitConfigType.UnitConfig;
import rst.homeautomation.unit.UnitGroupConfigType.UnitGroupConfig;
import rst.homeautomation.unit.UnitRegistryDataType.UnitRegistryData;
import rst.homeautomation.unit.UnitTemplateType.UnitTemplate;
import rst.homeautomation.unit.UnitTemplateType.UnitTemplate.UnitType;
import rst.rsb.ScopeType;
import rst.spatial.ConnectionConfigType.ConnectionConfig;
import rst.spatial.LocationConfigType.LocationConfig;

/**
 *
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public class UnitRegistryController extends AbstractRegistryController<UnitRegistryData, UnitRegistryData.Builder> implements UnitRegistry, Manageable<ScopeType.Scope> {

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(UnitRegistryData.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(UnitConfig.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(UnitTemplate.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(UserConfig.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(AuthorizationGroupConfig.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(DeviceConfig.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(UnitGroupConfig.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(LocationConfig.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(ConnectionConfig.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(AgentConfig.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(SceneConfig.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(AppConfig.getDefaultInstance()));
    }

    public final static UnitConfigIdGenerator UNIT_ID_GENERATOR = new UnitConfigIdGenerator();

    private final ProtoBufFileSynchronizedRegistry<String, UnitTemplate, UnitTemplate.Builder, UnitRegistryData.Builder> unitTemplateRegistry;
    private final ProtoBufFileSynchronizedRegistry<String, UnitConfig, UnitConfig.Builder, UnitRegistryData.Builder> dalUnitConfigRegistry;
    private final ProtoBufFileSynchronizedRegistry<String, UnitConfig, UnitConfig.Builder, UnitRegistryData.Builder> userUnitConfigRegistry;
    private final ProtoBufFileSynchronizedRegistry<String, UnitConfig, UnitConfig.Builder, UnitRegistryData.Builder> authorizationGroupUnitConfigRegistry;
    private final ProtoBufFileSynchronizedRegistry<String, UnitConfig, UnitConfig.Builder, UnitRegistryData.Builder> deviceUnitConfigRegistry;
    private final ProtoBufFileSynchronizedRegistry<String, UnitConfig, UnitConfig.Builder, UnitRegistryData.Builder> unitGroupUnitConfigRegistry;
    private final ProtoBufFileSynchronizedRegistry<String, UnitConfig, UnitConfig.Builder, UnitRegistryData.Builder> locationUnitConfigRegistry;
    private final ProtoBufFileSynchronizedRegistry<String, UnitConfig, UnitConfig.Builder, UnitRegistryData.Builder> connectionUnitConfigRegistry;
    private final ProtoBufFileSynchronizedRegistry<String, UnitConfig, UnitConfig.Builder, UnitRegistryData.Builder> agentUnitConfigRegistry;
    private final ProtoBufFileSynchronizedRegistry<String, UnitConfig, UnitConfig.Builder, UnitRegistryData.Builder> sceneUnitConfigRegistry;
    private final ProtoBufFileSynchronizedRegistry<String, UnitConfig, UnitConfig.Builder, UnitRegistryData.Builder> appUnitConfigRegistry;

    public UnitRegistryController() throws InstantiationException, InterruptedException {
        super(JPUnitRegistryScope.class, UnitRegistryData.newBuilder());
        try {
            this.unitTemplateRegistry = new ProtoBufFileSynchronizedRegistry<>(UnitTemplate.class, getBuilderSetup(), getDataFieldDescriptor(UnitRegistryData.UNIT_TEMPLATE_FIELD_NUMBER), new UnitTemplateIdGenerator(), JPService.getProperty(JPUnitTemplateDatabaseDirectory.class).getValue(), protoBufJSonFileProvider);
            this.dalUnitConfigRegistry = new ProtoBufFileSynchronizedRegistry<>(UnitConfig.class, getBuilderSetup(), getDataFieldDescriptor(UnitRegistryData.DAL_UNIT_CONFIG_FIELD_NUMBER), UNIT_ID_GENERATOR, JPService.getProperty(JPUnitConfigDatabaseDirectory.class).getValue(), protoBufJSonFileProvider);
            this.userUnitConfigRegistry = new ProtoBufFileSynchronizedRegistry<>(UnitConfig.class, getBuilderSetup(), getDataFieldDescriptor(UnitRegistryData.USER_UNIT_CONFIG_FIELD_NUMBER), UNIT_ID_GENERATOR, JPService.getProperty(JPUserConfigDatabaseDirectory.class).getValue(), protoBufJSonFileProvider);
            this.authorizationGroupUnitConfigRegistry = new ProtoBufFileSynchronizedRegistry<>(UnitConfig.class, getBuilderSetup(), getDataFieldDescriptor(UnitRegistryData.AUTHORIZATION_GROUP_UNIT_CONFIG_FIELD_NUMBER), UNIT_ID_GENERATOR, JPService.getProperty(JPAuthorizationGroupConfigDatabaseDirectory.class).getValue(), protoBufJSonFileProvider);
            this.deviceUnitConfigRegistry = new ProtoBufFileSynchronizedRegistry<>(UnitConfig.class, getBuilderSetup(), getDataFieldDescriptor(UnitRegistryData.DEVICE_UNIT_CONFIG_FIELD_NUMBER), UNIT_ID_GENERATOR, JPService.getProperty(JPDeviceConfigDatabaseDirectory.class).getValue(), protoBufJSonFileProvider);
            this.unitGroupUnitConfigRegistry = new ProtoBufFileSynchronizedRegistry<>(UnitConfig.class, getBuilderSetup(), getDataFieldDescriptor(UnitRegistryData.UNIT_GROUP_UNIT_CONFIG_FIELD_NUMBER), UNIT_ID_GENERATOR, JPService.getProperty(JPUnitGroupDatabaseDirectory.class).getValue(), protoBufJSonFileProvider);
            this.locationUnitConfigRegistry = new ProtoBufFileSynchronizedRegistry<>(UnitConfig.class, getBuilderSetup(), getDataFieldDescriptor(UnitRegistryData.LOCATION_UNIT_CONFIG_FIELD_NUMBER), UNIT_ID_GENERATOR, JPService.getProperty(JPLocationConfigDatabaseDirectory.class).getValue(), new ProtoBufJSonFileProvider());
            this.connectionUnitConfigRegistry = new ProtoBufFileSynchronizedRegistry<>(UnitConfig.class, getBuilderSetup(), getDataFieldDescriptor(UnitRegistryData.CONNECTION_UNIT_CONFIG_FIELD_NUMBER), UNIT_ID_GENERATOR, JPService.getProperty(JPConnectionConfigDatabaseDirectory.class).getValue(), new ProtoBufJSonFileProvider());
            this.agentUnitConfigRegistry = new ProtoBufFileSynchronizedRegistry<>(UnitConfig.class, getBuilderSetup(), getDataFieldDescriptor(UnitRegistryData.AGENT_UNIT_CONFIG_FIELD_NUMBER), UNIT_ID_GENERATOR, JPService.getProperty(JPAgentConfigDatabaseDirectory.class).getValue(), protoBufJSonFileProvider);
            this.sceneUnitConfigRegistry = new ProtoBufFileSynchronizedRegistry<>(UnitConfig.class, getBuilderSetup(), getDataFieldDescriptor(UnitRegistryData.SCENE_UNIT_CONFIG_FIELD_NUMBER), UNIT_ID_GENERATOR, JPService.getProperty(JPSceneConfigDatabaseDirectory.class).getValue(), protoBufJSonFileProvider);
            this.appUnitConfigRegistry = new ProtoBufFileSynchronizedRegistry<>(UnitConfig.class, getBuilderSetup(), getDataFieldDescriptor(UnitRegistryData.APP_UNIT_CONFIG_FIELD_NUMBER), UNIT_ID_GENERATOR, JPService.getProperty(JPAppConfigDatabaseDirectory.class).getValue(), protoBufJSonFileProvider);
        } catch (JPServiceException ex) {
            throw new InstantiationException(this, ex);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @throws CouldNotPerformException {@inheritDoc}
     */
    @Override
    protected void activateVersionControl() throws CouldNotPerformException {
        dalUnitConfigRegistry.activateVersionControl(DummyConverter.class.getPackage());
        userUnitConfigRegistry.activateVersionControl(DummyConverter.class.getPackage());
        authorizationGroupUnitConfigRegistry.activateVersionControl(DummyConverter.class.getPackage());
        deviceUnitConfigRegistry.activateVersionControl(DeviceConfig_0_To_1_DBConverter.class.getPackage());
        unitTemplateRegistry.activateVersionControl(UnitTemplate_0_To_1_DBConverter.class.getPackage());
        unitGroupUnitConfigRegistry.activateVersionControl(UnitGroupConfig_0_To_1_DBConverter.class.getPackage());
        locationUnitConfigRegistry.activateVersionControl(LocationConfig_0_To_1_DBConverter.class.getPackage());
        connectionUnitConfigRegistry.activateVersionControl(LocationConfig_0_To_1_DBConverter.class.getPackage());
        agentUnitConfigRegistry.activateVersionControl(AgentConfig_0_To_1_DBConverter.class.getPackage());
        sceneUnitConfigRegistry.activateVersionControl(SceneConfig_0_To_1_DBConverter.class.getPackage());
        appUnitConfigRegistry.activateVersionControl(DummyConverter.class.getPackage());
    }

    /**
     * {@inheritDoc}
     *
     * @throws CouldNotPerformException {@inheritDoc}
     */
    @Override
    protected void loadRegistries() throws CouldNotPerformException {
        unitTemplateRegistry.loadRegistry();
        dalUnitConfigRegistry.loadRegistry();
        userUnitConfigRegistry.loadRegistry();
        authorizationGroupUnitConfigRegistry.loadRegistry();
        unitTemplateRegistry.loadRegistry();
        deviceUnitConfigRegistry.loadRegistry();
        unitGroupUnitConfigRegistry.loadRegistry();
        locationUnitConfigRegistry.loadRegistry();
        connectionUnitConfigRegistry.loadRegistry();
        agentUnitConfigRegistry.loadRegistry();
        sceneUnitConfigRegistry.loadRegistry();
        appUnitConfigRegistry.loadRegistry();
    }

    /**
     * {@inheritDoc}
     *
     * @throws CouldNotPerformException {@inheritDoc}
     */
    @Override
    protected void registerRegistryRemotes() throws CouldNotPerformException {

    }

    /**
     * {@inheritDoc}
     *
     * @throws CouldNotPerformException {@inheritDoc}
     */
    @Override
    protected void registerRegistries() throws CouldNotPerformException {
        registerRegistry(unitTemplateRegistry);
        registerRegistry(agentUnitConfigRegistry);
        registerRegistry(appUnitConfigRegistry);
        registerRegistry(authorizationGroupUnitConfigRegistry);
        registerRegistry(dalUnitConfigRegistry);
        registerRegistry(deviceUnitConfigRegistry);
        registerRegistry(locationUnitConfigRegistry);
        registerRegistry(connectionUnitConfigRegistry);
        registerRegistry(sceneUnitConfigRegistry);
        registerRegistry(userUnitConfigRegistry);
        registerRegistry(unitGroupUnitConfigRegistry);
    }

    /**
     * {@inheritDoc}
     *
     * @throws CouldNotPerformException {@inheritDoc}
     */
    @Override
    protected void registerConsistencyHandler() throws CouldNotPerformException {
        //TODO: should be activated but fails in the current db version since agentClasses have just been introduced
        //agentUnitConfigRegistry.registerConsistencyHandler(new AgentConfigAgentClassIdConsistencyHandler(agentClassRegistry));
        agentUnitConfigRegistry.registerConsistencyHandler(new AgentLabelConsistencyHandler());
        agentUnitConfigRegistry.registerConsistencyHandler(new AgentLocationConsistencyHandler(locationUnitConfigRegistry));
        agentUnitConfigRegistry.registerConsistencyHandler(new AgentScopeConsistencyHandler(locationUnitConfigRegistry));

        //TODO: should be activated but fails in the current db version since appClasses have just been introduced
        //appConfigRegistry.registerConsistencyHandler(new AppConfigAppClassIdConsistencyHandler(appClassRegistry));
        appUnitConfigRegistry.registerConsistencyHandler(new AppLabelConsistencyHandler());
        appUnitConfigRegistry.registerConsistencyHandler(new AppLocationConsistencyHandler(locationUnitConfigRegistry));
        appUnitConfigRegistry.registerConsistencyHandler(new AppScopeConsistencyHandler(locationUnitConfigRegistry));

        authorizationGroupUnitConfigRegistry.registerConsistencyHandler(new AuthorizationGroupConfigLabelConsistencyHandler());
        authorizationGroupUnitConfigRegistry.registerConsistencyHandler(new AuthorizationGroupConfigScopeConsistencyHandler());

        connectionUnitConfigRegistry.registerConsistencyHandler(new ConnectionLabelConsistencyHandler());
        connectionUnitConfigRegistry.registerConsistencyHandler(new ConnectionTilesConsistencyHandler(locationUnitConfigRegistry));
        connectionUnitConfigRegistry.registerConsistencyHandler(new ConnectionLocationConsistencyHandler(locationUnitConfigRegistry));
        connectionUnitConfigRegistry.registerConsistencyHandler(new ConnectionScopeConsistencyHandler(locationUnitConfigRegistry));
        connectionUnitConfigRegistry.registerConsistencyHandler(new ConnectionTransformationFrameConsistencyHandler(locationUnitConfigRegistry));

        //TODO replace null with device class remote registry
        dalUnitConfigRegistry.registerConsistencyHandler(new DalUnitEnablingStateConsistencyHandler(deviceUnitConfigRegistry));
        dalUnitConfigRegistry.registerConsistencyHandler(new DalUnitHostIdConsistencyHandler(deviceUnitConfigRegistry));
        dalUnitConfigRegistry.registerConsistencyHandler(new DalUnitLabelConsistencyHandler(deviceClassRegistry, deviceUnitConfigRegistry));
        dalUnitConfigRegistry.registerConsistencyHandler(new DalUnitLocationIdConsistencyHandler(locationUnitConfigRegistry, deviceUnitConfigRegistry));
        dalUnitConfigRegistry.registerConsistencyHandler(new DalUnitScopeConsistencyHandler(locationUnitConfigRegistry));
        dalUnitConfigRegistry.registerConsistencyHandler(new UnitBoundToHostConsistencyHandler(deviceUnitConfigRegistry));
        dalUnitConfigRegistry.registerConsistencyHandler(new DalUnitEnablingStateConsistencyHandler(deviceUnitConfigRegistry));
        dalUnitConfigRegistry.registerConsistencyHandler(new UnitTransformationFrameConsistencyHandler(locationUnitConfigRegistry));

        //TODO replace null with device class remote registry
        deviceUnitConfigRegistry.registerConsistencyHandler(new DeviceConfigDeviceClassIdConsistencyHandler(deviceClassRegistry));
        deviceUnitConfigRegistry.registerConsistencyHandler(new DeviceConfigDeviceClassUnitConsistencyHandler(deviceClassRegistry, dalUnitConfigRegistry));
        deviceUnitConfigRegistry.registerConsistencyHandler(new DeviceConfigLocationIdForInstalledDevicesConsistencyHandler());
        deviceUnitConfigRegistry.registerConsistencyHandler(new DeviceEnablingStateConsistencyHandler());
        deviceUnitConfigRegistry.registerConsistencyHandler(new DeviceLabelConsistencyHandler());
        deviceUnitConfigRegistry.registerConsistencyHandler(new DeviceLocationIdConsistencyHandler(locationUnitConfigRegistry));
        deviceUnitConfigRegistry.registerConsistencyHandler(new DeviceOwnerConsistencyHandler(userUnitConfigRegistry));
        deviceUnitConfigRegistry.registerConsistencyHandler(new DeviceScopeConsistencyHandler(locationUnitConfigRegistry));
        deviceUnitConfigRegistry.registerConsistencyHandler(new DeviceTransformationFrameConsistencyHandler(locationUnitConfigRegistry));
        deviceUnitConfigRegistry.registerConsistencyHandler(new OpenhabServiceConfigItemIdConsistencyHandler(deviceClassRegistry, locationUnitConfigRegistry, dalUnitConfigRegistry));
        deviceUnitConfigRegistry.registerConsistencyHandler(new SyncBindingConfigDeviceClassUnitConsistencyHandler(deviceClassRegistry, dalUnitConfigRegistry));

        userUnitConfigRegistry.registerConsistencyHandler(new UserConfigScopeConsistencyHandler());
        userUnitConfigRegistry.registerConsistencyHandler(new UserConfigUserNameConsistencyHandler());

        unitGroupUnitConfigRegistry.registerConsistencyHandler(new UnitGroupMemberListDuplicationConsistencyHandler());
        unitGroupUnitConfigRegistry.registerConsistencyHandler(new UnitGroupMemberExistsConsistencyHandler(agentUnitConfigRegistry, appUnitConfigRegistry, authorizationGroupUnitConfigRegistry, connectionUnitConfigRegistry, dalUnitConfigRegistry, deviceUnitConfigRegistry, locationUnitConfigRegistry, sceneUnitConfigRegistry, unitGroupUnitConfigRegistry, userUnitConfigRegistry));
        unitGroupUnitConfigRegistry.registerConsistencyHandler(new UnitGroupUnitTypeConsistencyHandler(unitTemplateRegistry));
        unitGroupUnitConfigRegistry.registerConsistencyHandler(new UnitGroupMemberListTypesConsistencyHandler(agentUnitConfigRegistry, appUnitConfigRegistry, authorizationGroupUnitConfigRegistry, connectionUnitConfigRegistry, dalUnitConfigRegistry, deviceUnitConfigRegistry, locationUnitConfigRegistry, sceneUnitConfigRegistry, unitGroupUnitConfigRegistry, userUnitConfigRegistry, unitTemplateRegistry));
        unitGroupUnitConfigRegistry.registerConsistencyHandler(new UnitGroupScopeConsistencyHandler(locationUnitConfigRegistry));

        locationUnitConfigRegistry.registerConsistencyHandler(new LocationPlacementConfigConsistencyHandler());
        locationUnitConfigRegistry.registerConsistencyHandler(new LocationPositionConsistencyHandler());
        locationUnitConfigRegistry.registerConsistencyHandler(new RootConsistencyHandler());
        locationUnitConfigRegistry.registerConsistencyHandler(new LocationChildConsistencyHandler());
        locationUnitConfigRegistry.registerConsistencyHandler(new LocationIdConsistencyHandler());
        locationUnitConfigRegistry.registerConsistencyHandler(new LocationParentConsistencyHandler());
        locationUnitConfigRegistry.registerConsistencyHandler(new RootLocationExistencConsistencyHandler());
        locationUnitConfigRegistry.registerConsistencyHandler(new LocationLoopConsistencyHandler());
        locationUnitConfigRegistry.registerConsistencyHandler(new ChildWithSameLabelConsistencyHandler());
        locationUnitConfigRegistry.registerConsistencyHandler(new LocationScopeConsistencyHandler());
        locationUnitConfigRegistry.registerConsistencyHandler(new LocationUnitIdConsistencyHandler(agentUnitConfigRegistry, appUnitConfigRegistry, authorizationGroupUnitConfigRegistry, connectionUnitConfigRegistry, dalUnitConfigRegistry, deviceUnitConfigRegistry, sceneUnitConfigRegistry, unitGroupUnitConfigRegistry, userUnitConfigRegistry));
        locationUnitConfigRegistry.registerConsistencyHandler(new LocationTransformationFrameConsistencyHandler(locationUnitConfigRegistry));

        sceneUnitConfigRegistry.registerConsistencyHandler(new SceneLabelConsistencyHandler());
        sceneUnitConfigRegistry.registerConsistencyHandler(new SceneScopeConsistencyHandler(locationUnitConfigRegistry));

        // add consistency handler for all unitConfig registries
        registerConsistencyHandler(new ServiceConfigUnitIdConsistencyHandler(), UnitConfig.class);
        registerConsistencyHandler(new UnitConfigUnitTemplateConsistencyHandler(unitTemplateRegistry), UnitConfig.class);
        registerConsistencyHandler(new UnitEnablingStateConsistencyHandler(), UnitConfig.class);
    }

    /**
     * {@inheritDoc}
     *
     * @throws CouldNotPerformException {@inheritDoc}
     * @throws InterruptedException {@inheritDoc}
     */
    @Override
    protected void registerPlugins() throws CouldNotPerformException, InterruptedException {
        connectionUnitConfigRegistry.registerPlugin(new PublishConnectionTransformationRegistryPlugin(locationUnitConfigRegistry));
        dalUnitConfigRegistry.registerPlugin(new PublishDalUnitTransformationRegistryPlugin(locationUnitConfigRegistry));
        deviceUnitConfigRegistry.registerPlugin(new PublishDeviceTransformationRegistryPlugin(locationUnitConfigRegistry));
        unitTemplateRegistry.registerPlugin(new UnitTemplateCreatorRegistryPlugin(unitTemplateRegistry));
        locationUnitConfigRegistry.registerPlugin(new PublishLocationTransformationRegistryPlugin());
    }

    /**
     * {@inheritDoc}
     *
     * @throws CouldNotPerformException {@inheritDoc}
     */
    @Override
    protected void registerDependencies() throws CouldNotPerformException {
        // add dependencies handler for all unitConfig registries
        registerDependency(unitTemplateRegistry, UnitConfig.class);

        dalUnitConfigRegistry.registerDependency(deviceUnitConfigRegistry);
        dalUnitConfigRegistry.registerDependency(locationUnitConfigRegistry);

        authorizationGroupUnitConfigRegistry.registerDependency(userUnitConfigRegistry);

        deviceUnitConfigRegistry.registerDependency(locationUnitConfigRegistry);
        deviceUnitConfigRegistry.registerDependency(userUnitConfigRegistry);
        deviceUnitConfigRegistry.registerDependency(deviceClassRegistry);

        unitGroupUnitConfigRegistry.registerDependency(agentUnitConfigRegistry);
        unitGroupUnitConfigRegistry.registerDependency(appUnitConfigRegistry);
        unitGroupUnitConfigRegistry.registerDependency(authorizationGroupUnitConfigRegistry);
        unitGroupUnitConfigRegistry.registerDependency(connectionUnitConfigRegistry);
        unitGroupUnitConfigRegistry.registerDependency(dalUnitConfigRegistry);
        unitGroupUnitConfigRegistry.registerDependency(deviceUnitConfigRegistry);
        unitGroupUnitConfigRegistry.registerDependency(locationUnitConfigRegistry);
        unitGroupUnitConfigRegistry.registerDependency(sceneUnitConfigRegistry);
        unitGroupUnitConfigRegistry.registerDependency(unitTemplateRegistry);
        unitGroupUnitConfigRegistry.registerDependency(userUnitConfigRegistry);

        locationUnitConfigRegistry.registerDependency(agentUnitConfigRegistry);
        locationUnitConfigRegistry.registerDependency(appUnitConfigRegistry);
        locationUnitConfigRegistry.registerDependency(authorizationGroupUnitConfigRegistry);
        locationUnitConfigRegistry.registerDependency(connectionUnitConfigRegistry);
        locationUnitConfigRegistry.registerDependency(dalUnitConfigRegistry);
        locationUnitConfigRegistry.registerDependency(deviceUnitConfigRegistry);
        locationUnitConfigRegistry.registerDependency(sceneUnitConfigRegistry);
        locationUnitConfigRegistry.registerDependency(unitGroupUnitConfigRegistry);
        locationUnitConfigRegistry.registerDependency(userUnitConfigRegistry);

        connectionUnitConfigRegistry.registerDependency(locationUnitConfigRegistry);

        agentUnitConfigRegistry.registerDependency(locationUnitConfigRegistry);
        agentUnitConfigRegistry.registerDependency(agentClassRegistry);

        sceneUnitConfigRegistry.registerDependency(locationUnitConfigRegistry);

        appUnitConfigRegistry.registerDependency(appClassRegistry);
        appUnitConfigRegistry.registerDependency(locationUnitConfigRegistry);
    }

    /**
     * {@inheritDoc}
     *
     * @throws CouldNotPerformException {@inheritDoc}
     */
    @Override
    protected void performInitialConsistencyCheck() throws CouldNotPerformException, InterruptedException {
        try {
            unitTemplateRegistry.checkConsistency();
        } catch (CouldNotPerformException ex) {
            ExceptionPrinter.printHistory(new CouldNotPerformException("Initial consistency check failed!", ex), logger, LogLevel.WARN);
            notifyChange();
        }

        try {
            dalUnitConfigRegistry.checkConsistency();
        } catch (CouldNotPerformException ex) {
            ExceptionPrinter.printHistory(new CouldNotPerformException("Initial consistency check failed!", ex), logger, LogLevel.WARN);
            notifyChange();
        }

        try {
            userUnitConfigRegistry.checkConsistency();
        } catch (CouldNotPerformException ex) {
            ExceptionPrinter.printHistory(new CouldNotPerformException("Initial consistency check failed!", ex), logger, LogLevel.WARN);
            notifyChange();
        }

        try {
            authorizationGroupUnitConfigRegistry.checkConsistency();
        } catch (CouldNotPerformException ex) {
            ExceptionPrinter.printHistory(new CouldNotPerformException("Initial consistency check failed!", ex), logger, LogLevel.WARN);
            notifyChange();
        }
        try {
            unitTemplateRegistry.checkConsistency();
        } catch (CouldNotPerformException ex) {
            ExceptionPrinter.printHistory(new CouldNotPerformException("Initial consistency check failed!", ex), logger, LogLevel.WARN);
            notifyChange();
        }

        try {
            deviceUnitConfigRegistry.checkConsistency();
        } catch (CouldNotPerformException ex) {
            ExceptionPrinter.printHistory(new CouldNotPerformException("Initial consistency check failed!", ex), logger, LogLevel.WARN);
            notifyChange();
        }

        try {
            unitGroupUnitConfigRegistry.checkConsistency();
        } catch (CouldNotPerformException ex) {
            ExceptionPrinter.printHistory(new CouldNotPerformException("Initial consistency check failed!", ex), logger, LogLevel.WARN);
            notifyChange();
        }

        try {
            locationUnitConfigRegistry.checkConsistency();
        } catch (CouldNotPerformException ex) {
            ExceptionPrinter.printHistory(new CouldNotPerformException("Initial consistency check failed!", ex), logger, LogLevel.WARN);
            notifyChange();
        }

        try {
            connectionUnitConfigRegistry.checkConsistency();
        } catch (CouldNotPerformException ex) {
            ExceptionPrinter.printHistory(new CouldNotPerformException("Initial consistency check failed!", ex), logger, LogLevel.WARN);
            notifyChange();
        }

        try {
            agentUnitConfigRegistry.checkConsistency();
        } catch (CouldNotPerformException ex) {
            ExceptionPrinter.printHistory(new CouldNotPerformException("Initial consistency check failed!", ex), logger, LogLevel.WARN);
            notifyChange();
        }

        try {
            sceneUnitConfigRegistry.checkConsistency();
        } catch (CouldNotPerformException ex) {
            ExceptionPrinter.printHistory(new CouldNotPerformException("Initial consistency check failed!", ex), logger, LogLevel.WARN);
            notifyChange();
        }

        try {
            appUnitConfigRegistry.checkConsistency();
        } catch (CouldNotPerformException ex) {
            ExceptionPrinter.printHistory(new CouldNotPerformException("Initial consistency check failed!", ex), logger, LogLevel.WARN);
            notifyChange();
        }
    }

    @Override
    public void shutdown() {
        if (dalUnitConfigRegistry != null) {
            dalUnitConfigRegistry.shutdown();
        }

        if (unitTemplateRegistry != null) {
            unitTemplateRegistry.shutdown();
        }

        if (deviceUnitConfigRegistry != null) {
            deviceUnitConfigRegistry.shutdown();
        }

        if (unitTemplateRegistry != null) {
            unitTemplateRegistry.shutdown();
        }

        if (unitGroupUnitConfigRegistry != null) {
            unitGroupUnitConfigRegistry.shutdown();
        }

        if (locationUnitConfigRegistry != null) {
            locationUnitConfigRegistry.shutdown();
        }

        if (connectionUnitConfigRegistry != null) {
            connectionUnitConfigRegistry.shutdown();
        }

        if (sceneUnitConfigRegistry != null) {
            sceneUnitConfigRegistry.shutdown();
        }

        if (appUnitConfigRegistry != null) {
            appUnitConfigRegistry.shutdown();
        }

        if (agentUnitConfigRegistry != null) {
            agentUnitConfigRegistry.shutdown();
        }

        super.shutdown();
    }

    @Override
    public final void syncDataTypeFlags() throws CouldNotPerformException, InterruptedException {
        setDataField(UnitRegistryData.UNIT_CONFIG_REGISTRY_READ_ONLY_FIELD_NUMBER, dalUnitConfigRegistry.isReadOnly());
        setDataField(UnitRegistryData.UNIT_CONFIG_REGISTRY_CONSISTENT_FIELD_NUMBER, dalUnitConfigRegistry.isConsistent());

        setDataField(UnitRegistryData.UNIT_TEMPLATE_REGISTRY_READ_ONLY_FIELD_NUMBER, unitTemplateRegistry.isReadOnly());
        setDataField(UnitRegistryData.UNIT_TEMPLATE_REGISTRY_CONSISTENT_FIELD_NUMBER, unitTemplateRegistry.isConsistent());

        setDataField(UnitRegistryData.USER_UNIT_CONFIG_REGISTRY_READ_ONLY_FIELD_NUMBER, userUnitConfigRegistry.isReadOnly());
        setDataField(UnitRegistryData.USER_UNIT_CONFIG_REGISTRY_CONSISTENT_FIELD_NUMBER, userUnitConfigRegistry.isConsistent());

        setDataField(UnitRegistryData.AUTHORIZATION_GROUP_UNIT_CONFIG_REGISTRY_READ_ONLY_FIELD_NUMBER, authorizationGroupUnitConfigRegistry.isReadOnly());
        setDataField(UnitRegistryData.AUTHORIZATION_GROUP_UNIT_CONFIG_REGISTRY_CONSISTENT_FIELD_NUMBER, authorizationGroupUnitConfigRegistry.isConsistent());

        setDataField(UnitRegistryData.DEVICE_UNIT_CONFIG_REGISTRY_READ_ONLY_FIELD_NUMBER, deviceUnitConfigRegistry.isReadOnly());
        setDataField(UnitRegistryData.DEVICE_UNIT_CONFIG_REGISTRY_CONSISTENT_FIELD_NUMBER, deviceUnitConfigRegistry.isConsistent());

        setDataField(UnitRegistryData.UNIT_GROUP_UNIT_CONFIG_REGISTRY_READ_ONLY_FIELD_NUMBER, unitGroupUnitConfigRegistry.isReadOnly());
        setDataField(UnitRegistryData.UNIT_GROUP_UNIT_CONFIG_REGISTRY_CONSISTENT_FIELD_NUMBER, unitGroupUnitConfigRegistry.isConsistent());

        setDataField(UnitRegistryData.LOCATION_UNIT_CONFIG_REGISTRY_READ_ONLY_FIELD_NUMBER, locationUnitConfigRegistry.isReadOnly());
        setDataField(UnitRegistryData.LOCATION_UNIT_CONFIG_REGISTRY_CONSISTENT_FIELD_NUMBER, locationUnitConfigRegistry.isConsistent());

        setDataField(UnitRegistryData.CONNECTION_UNIT_CONFIG_REGISTRY_READ_ONLY_FIELD_NUMBER, connectionUnitConfigRegistry.isReadOnly());
        setDataField(UnitRegistryData.CONNECTION_UNIT_CONFIG_REGISTRY_CONSISTENT_FIELD_NUMBER, connectionUnitConfigRegistry.isConsistent());

        setDataField(UnitRegistryData.SCENE_UNIT_CONFIG_REGISTRY_READ_ONLY_FIELD_NUMBER, sceneUnitConfigRegistry.isReadOnly());
        setDataField(UnitRegistryData.SCENE_UNIT_CONFIG_REGISTRY_CONSISTENT_FIELD_NUMBER, sceneUnitConfigRegistry.isConsistent());

        setDataField(UnitRegistryData.AGENT_UNIT_CONFIG_REGISTRY_READ_ONLY_FIELD_NUMBER, agentUnitConfigRegistry.isReadOnly());
        setDataField(UnitRegistryData.AGENT_UNIT_CONFIG_REGISTRY_CONSISTENT_FIELD_NUMBER, agentUnitConfigRegistry.isConsistent());

        setDataField(UnitRegistryData.APP_UNIT_CONFIG_REGISTRY_READ_ONLY_FIELD_NUMBER, appUnitConfigRegistry.isReadOnly());
        setDataField(UnitRegistryData.APP_UNIT_CONFIG_REGISTRY_READ_ONLY_FIELD_NUMBER, appUnitConfigRegistry.isConsistent());

        super.notifyChange();
    }

    @Override
    public void registerMethods(final RSBLocalServer server) throws CouldNotPerformException {
        RPCHelper.registerInterface(UnitRegistry.class, this, server);
    }

    @Override
    public Future<UnitConfig> registerUnitConfig(UnitConfig unitConfig) throws CouldNotPerformException {
        return GlobalExecutionService.submit(() -> dalUnitConfigRegistry.register(unitConfig));
    }

    @Override
    public UnitConfig getUnitConfigById(String unitConfigId) throws CouldNotPerformException {
        return dalUnitConfigRegistry.get(unitConfigId).getMessage();
    }

    @Override
    public Boolean containsUnitConfigById(String sceneConfigId) throws CouldNotPerformException {
        return dalUnitConfigRegistry.contains(sceneConfigId);
    }

    @Override
    public Boolean containsUnitConfig(UnitConfig unitConfig) throws CouldNotPerformException {
        return dalUnitConfigRegistry.contains(unitConfig);
    }

    @Override
    public Future<UnitConfig> updateUnitConfig(UnitConfig unitConfig) throws CouldNotPerformException {
        return GlobalExecutionService.submit(() -> dalUnitConfigRegistry.update(unitConfig));
    }

    @Override
    public Future<UnitConfig> removeUnitConfig(UnitConfig unitConfig) throws CouldNotPerformException {
        return GlobalExecutionService.submit(() -> dalUnitConfigRegistry.remove(unitConfig));
    }

    @Override
    public List<UnitConfig> getUnitConfigs() throws CouldNotPerformException {
        return dalUnitConfigRegistry.getMessages();
    }

    @Override
    public Boolean isUnitConfigRegistryReadOnly() throws CouldNotPerformException {
        return dalUnitConfigRegistry.isReadOnly();
    }

    /**
     * {@inheritDoc}
     *
     * @return {@inheritDoc}
     * @throws CouldNotPerformException {@inheritDoc}
     */
    @Override
    public Boolean isUnitConfigRegistryConsistent() throws CouldNotPerformException {
        return dalUnitConfigRegistry.isConsistent();
    }

    /**
     * {@inheritDoc}
     *
     * @param unitTemplateId {@inheritDoc}
     * @return {@inheritDoc}
     * @throws CouldNotPerformException {@inheritDoc}
     */
    @Override
    public UnitTemplate getUnitTemplateById(String unitTemplateId) throws CouldNotPerformException {
        return unitTemplateRegistry.get(unitTemplateId).getMessage();
    }

    /**
     * {@inheritDoc}
     *
     * @param unitTemplateId {@inheritDoc}
     * @return {@inheritDoc}
     * @throws CouldNotPerformException {@inheritDoc}
     */
    @Override
    public Boolean containsUnitTemplateById(String unitTemplateId) throws CouldNotPerformException {
        return unitTemplateRegistry.contains(unitTemplateId);
    }

    /**
     * {@inheritDoc}
     *
     * @param unitTemplate {@inheritDoc}
     * @return {@inheritDoc}
     * @throws CouldNotPerformException {@inheritDoc}
     */
    @Override
    public Boolean containsUnitTemplate(UnitTemplate unitTemplate) throws CouldNotPerformException {
        return unitTemplateRegistry.contains(unitTemplate);
    }

    /**
     * {@inheritDoc}
     *
     * @param unitTemplate {@inheritDoc}
     * @return {@inheritDoc}
     * @throws CouldNotPerformException {@inheritDoc}
     */
    @Override
    public Future<UnitTemplate> updateUnitTemplate(UnitTemplate unitTemplate) throws CouldNotPerformException {
        return GlobalExecutionService.submit(() -> unitTemplateRegistry.update(unitTemplate));
    }

    /**
     * {@inheritDoc}
     *
     * @return
     * @throws CouldNotPerformException
     */
    @Override
    public List<UnitTemplate> getUnitTemplates() throws CouldNotPerformException {
        return unitTemplateRegistry.getMessages();
    }

    /**
     * {@inheritDoc}
     *
     * @param type
     * @return
     * @throws CouldNotPerformException
     */
    @Override
    public UnitTemplate getUnitTemplateByType(final UnitType type) throws CouldNotPerformException {
        for (UnitTemplate unitTemplate : unitTemplateRegistry.getMessages()) {
            if (unitTemplate.getType() == type) {
                return unitTemplate;
            }
        }
        throw new NotAvailableException("UnitTemplate with type [" + type + "]");
    }

    /**
     * {@inheritDoc}
     *
     * @return
     * @throws CouldNotPerformException
     */
    @Override
    public Boolean isUnitTemplateRegistryReadOnly() throws CouldNotPerformException {
        return unitTemplateRegistry.isReadOnly();
    }

    /**
     * {@inheritDoc}
     *
     * @return
     * @throws CouldNotPerformException
     */
    @Override
    public Boolean isUnitTemplateRegistryConsistent() throws CouldNotPerformException {
        return unitTemplateRegistry.isConsistent();
    }

    public ProtoBufFileSynchronizedRegistry<String, UnitConfig, UnitConfig.Builder, UnitRegistryData.Builder> getUnitConfigRegistry() {
        return dalUnitConfigRegistry;
    }

    public ProtoBufFileSynchronizedRegistry<String, UnitTemplate, UnitTemplate.Builder, UnitRegistryData.Builder> getUnitTemplateRegistry() {
        return unitTemplateRegistry;
    }

    public ProtoBufFileSynchronizedRegistry<String, UnitConfig, UnitConfig.Builder, UnitRegistryData.Builder> getUserRegistry() {
        return userUnitConfigRegistry;
    }

    public ProtoBufFileSynchronizedRegistry<String, UnitConfig, UnitConfig.Builder, UnitRegistryData.Builder> getAuthorizationGroupRegistry() {
        return authorizationGroupUnitConfigRegistry;
    }
}
