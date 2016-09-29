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
import java.util.Map;
import java.util.concurrent.Future;
import org.openbase.bco.registry.lib.AbstractRegistryController;
import org.openbase.bco.registry.scene.lib.jp.JPUnitConfigDatabaseDirectory;
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
import org.openbase.bco.registry.unit.core.consistency.unitgroup.UnitGroupMemberExistsConsistencyHandler;
import org.openbase.bco.registry.unit.core.consistency.unitgroup.UnitGroupMemberListDuplicationConsistencyHandler;
import org.openbase.bco.registry.unit.core.consistency.unitgroup.UnitGroupMemberListTypesConsistencyHandler;
import org.openbase.bco.registry.unit.core.consistency.unitgroup.UnitGroupScopeConsistencyHandler;
import org.openbase.bco.registry.unit.core.consistency.unitgroup.UnitGroupUnitTypeConsistencyHandler;
import org.openbase.bco.registry.unit.core.consistency.unittemplate.UnitTemplateValidationConsistencyHandler;
import org.openbase.bco.registry.unit.core.consistency.user.UserConfigScopeConsistencyHandler;
import org.openbase.bco.registry.unit.core.consistency.user.UserConfigUserNameConsistencyHandler;
import org.openbase.bco.registry.unit.core.dbconvert.DummyConverter;
import org.openbase.bco.registry.unit.core.dbconvert.SceneConfig_0_To_1_DBConverter;
import org.openbase.bco.registry.unit.core.plugin.PublishConnectionTransformationRegistryPlugin;
import org.openbase.bco.registry.unit.core.plugin.PublishLocationTransformationRegistryPlugin;
import org.openbase.bco.registry.unit.core.plugin.UnitTemplateCreatorRegistryPlugin;
import org.openbase.bco.registry.unit.lib.UnitRegistry;
import org.openbase.bco.registry.unit.lib.generator.AuthorizationGroupConfigIdGenerator;
import org.openbase.bco.registry.unit.lib.generator.SceneConfigIdGenerator;
import org.openbase.bco.registry.unit.lib.generator.UnitConfigIdGenerator;
import org.openbase.bco.registry.unit.lib.generator.UnitTemplateIdGenerator;
import org.openbase.bco.registry.unit.lib.generator.UserConfigIdGenerator;
import org.openbase.bco.registry.unit.lib.jp.JPUnitRegistryScope;
import org.openbase.bco.registry.unit.lib.jp.JPUnitTemplateDatabaseDirectory;
import org.openbase.jps.core.JPService;
import org.openbase.jps.exception.JPServiceException;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.exception.printer.LogLevel;
import org.openbase.jul.extension.protobuf.IdentifiableMessage;
import org.openbase.jul.extension.rsb.com.RPCHelper;
import org.openbase.jul.extension.rsb.iface.RSBLocalServer;
import org.openbase.jul.iface.Manageable;
import org.openbase.jul.pattern.Observable;
import org.openbase.jul.schedule.GlobalExecutionService;
import org.openbase.jul.storage.file.ProtoBufJSonFileProvider;
import org.openbase.jul.storage.registry.ProtoBufFileSynchronizedRegistry;
import org.openbase.jul.storage.registry.Registry;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rst.authorization.AuthorizationGroupConfigType;
import rst.authorization.UserConfigType;
import rst.authorization.UserRegistryDataType;
import rst.homeautomation.control.agent.AgentClassType;
import rst.homeautomation.control.agent.AgentConfigType;
import rst.homeautomation.control.agent.AgentRegistryDataType;
import rst.homeautomation.control.app.AppClassType;
import rst.homeautomation.control.app.AppConfigType;
import rst.homeautomation.control.app.AppRegistryDataType;
import rst.homeautomation.control.scene.SceneConfigType;
import rst.homeautomation.control.scene.SceneRegistryDataType;
import rst.homeautomation.device.DeviceClassType;
import rst.homeautomation.device.DeviceConfigType;
import rst.homeautomation.device.DeviceRegistryDataType;
import rst.homeautomation.unit.UnitConfigType.UnitConfig;
import rst.homeautomation.unit.UnitGroupConfigType;
import rst.homeautomation.unit.UnitRegistryDataType.UnitRegistryData;
import rst.homeautomation.unit.UnitTemplateType.UnitTemplate;
import rst.homeautomation.unit.UnitTemplateType.UnitTemplate.UnitType;
import rst.rsb.ScopeType;
import rst.spatial.ConnectionConfigType;
import rst.spatial.LocationConfigType;
import rst.spatial.LocationRegistryDataType;

/**
 *
 * @author mpohling
 */
public class UnitRegistryController extends AbstractRegistryController<UnitRegistryData, UnitRegistryData.Builder> implements UnitRegistry, Manageable<ScopeType.Scope> {

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(UnitRegistryData.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(UnitConfig.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(UnitTemplate.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(UserRegistryDataType.UserRegistryData.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(UserConfigType.UserConfig.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(AuthorizationGroupConfigType.AuthorizationGroupConfig.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(DeviceRegistryDataType.DeviceRegistryData.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(DeviceClassType.DeviceClass.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(DeviceConfigType.DeviceConfig.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(UnitTemplate.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(UnitGroupConfigType.UnitGroupConfig.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(UnitConfig.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(LocationRegistryDataType.LocationRegistryData.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(LocationConfigType.LocationConfig.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(ConnectionConfigType.ConnectionConfig.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(AgentRegistryDataType.AgentRegistryData.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(AgentConfigType.AgentConfig.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(AgentClassType.AgentClass.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(SceneRegistryDataType.SceneRegistryData.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(SceneConfigType.SceneConfig.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(AppRegistryDataType.AppRegistryData.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(AppConfigType.AppConfig.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(AppClassType.AppClass.getDefaultInstance()));
    }

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
            this.dalUnitConfigRegistry = new ProtoBufFileSynchronizedRegistry<>(UnitConfig.class, getBuilderSetup(), getDataFieldDescriptor(UnitRegistryData.DAL_UNIT_CONFIG_FIELD_NUMBER), new UnitConfigIdGenerator(), JPService.getProperty(JPUnitConfigDatabaseDirectory.class).getValue(), protoBufJSonFileProvider);
            this.unitTemplateRegistry = new ProtoBufFileSynchronizedRegistry<>(UnitTemplate.class, getBuilderSetup(), getDataFieldDescriptor(UnitRegistryData.UNIT_TEMPLATE_FIELD_NUMBER), new UnitTemplateIdGenerator(), JPService.getProperty(JPUnitTemplateDatabaseDirectory.class).getValue(), protoBufJSonFileProvider);
            this.userUnitConfigRegistry = new ProtoBufFileSynchronizedRegistry<>(UserConfigType.UserConfig.class, getBuilderSetup(), getDataFieldDescriptor(UnitRegistryData.USER_UNIT_CONFIG_FIELD_NUMBER), new UserConfigIdGenerator(), JPService.getProperty(JPUserConfigDatabaseDirectory.class).getValue(), protoBufJSonFileProvider);
            this.authorizationGroupUnitConfigRegistry = new ProtoBufFileSynchronizedRegistry<>(AuthorizationGroupConfigType.AuthorizationGroupConfig.class, getBuilderSetup(), getDataFieldDescriptor(UnitRegistryData.AUTHORIZATION_GROUP_UNIT_CONFIG_FIELD_NUMBER), new AuthorizationGroupConfigIdGenerator(), JPService.getProperty(JPAuthorizationGroupConfigDatabaseDirectory.class).getValue(), protoBufJSonFileProvider);
            this.deviceUnitConfigRegistry = new ProtoBufFileSynchronizedRegistry<>(DeviceConfigType.DeviceConfig.class, getBuilderSetup(), getDataFieldDescriptor(UnitRegistryData.DEVICE_UNIT_CONFIG_FIELD_NUMBER), new DeviceConfigIdGenerator(), JPService.getProperty(JPDeviceConfigDatabaseDirectory.class).getValue(), protoBufJSonFileProvider);
            this.unitGroupUnitConfigRegistry = new ProtoBufFileSynchronizedRegistry<>(UnitGroupConfigType.UnitGroupConfig.class, getBuilderSetup(), getDataFieldDescriptor(UnitRegistryData.UNIT_GROUP_UNIT_CONFIG_FIELD_NUMBER), new UnitGroupIdGenerator(), JPService.getProperty(JPUnitGroupDatabaseDirectory.class).getValue(), protoBufJSonFileProvider);
            this.locationUnitConfigRegistry = new ProtoBufFileSynchronizedRegistry<>(LocationConfigType.LocationConfig.class, getBuilderSetup(), getDataFieldDescriptor(UnitRegistryData.LOCATION_UNIT_CONFIG_FIELD_NUMBER), new LocationIDGenerator(), JPService.getProperty(JPLocationConfigDatabaseDirectory.class).getValue(), new ProtoBufJSonFileProvider());
            this.connectionUnitConfigRegistry = new ProtoBufFileSynchronizedRegistry<>(ConnectionConfigType.ConnectionConfig.class, getBuilderSetup(), getDataFieldDescriptor(UnitRegistryData.CONNECTION_UNIT_CONFIG_FIELD_NUMBER), new ConnectionIDGenerator(), JPService.getProperty(JPConnectionConfigDatabaseDirectory.class).getValue(), new ProtoBufJSonFileProvider());
            this.agentUnitConfigRegistry = new ProtoBufFileSynchronizedRegistry<>(AgentConfigType.AgentConfig.class, getBuilderSetup(), getDataFieldDescriptor(UnitRegistryData.AGENT_UNIT_CONFIG_FIELD_NUMBER), new AgentConfigIdGenerator(), JPService.getProperty(JPAgentConfigDatabaseDirectory.class).getValue(), protoBufJSonFileProvider);
            this.sceneUnitConfigRegistry = new ProtoBufFileSynchronizedRegistry<>(SceneConfigType.SceneConfig.class, getBuilderSetup(), getDataFieldDescriptor(UnitRegistryData.SCENE_UNIT_CONFIG_FIELD_NUMBER), new SceneConfigIdGenerator(), JPService.getProperty(JPSceneConfigDatabaseDirectory.class).getValue(), protoBufJSonFileProvider);
            appUnitConfigRegistry = new ProtoBufFileSynchronizedRegistry<>(AppConfigType.AppConfig.class, getBuilderSetup(), getDataFieldDescriptor(UnitRegistryData.APP_UNIT_CONFIG_FIELD_NUMBER), new AppConfigIdGenerator(), JPService.getProperty(JPAppConfigDatabaseDirectory.class).getValue(), protoBufJSonFileProvider);
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
    protected void registerConsistencyHandler() throws CouldNotPerformException {
        //TODO: should be activated but fails in the current db version since agentClasses have just been introduced
        //agentUnitConfigRegistry.registerConsistencyHandler(new AgentConfigAgentClassIdConsistencyHandler(agentClassRegistry));
        agentUnitConfigRegistry.registerConsistencyHandler(new AgentLabelConsistencyHandler());
        agentUnitConfigRegistry.registerConsistencyHandler(new AgentLocationConsistencyHandler(locationRegistry));
        agentUnitConfigRegistry.registerConsistencyHandler(new AgentScopeConsistencyHandler(locationRegistry));

        //TODO: should be activated but fails in the current db version since appClasses have just been introduced
        //appConfigRegistry.registerConsistencyHandler(new AppConfigAppClassIdConsistencyHandler(appClassRegistry));
        appUnitConfigRegistry.registerConsistencyHandler(new AppLabelConsistencyHandler());
        appUnitConfigRegistry.registerConsistencyHandler(new AppLocationConsistencyHandler(locationRegistry));
        appUnitConfigRegistry.registerConsistencyHandler(new AppScopeConsistencyHandler(locationRegistry));

        authorizationGroupUnitConfigRegistry.registerConsistencyHandler(new AuthorizationGroupConfigLabelConsistencyHandler());
        authorizationGroupUnitConfigRegistry.registerConsistencyHandler(new AuthorizationGroupConfigScopeConsistencyHandler());

        connectionUnitConfigRegistry.registerConsistencyHandler(new ConnectionLabelConsistencyHandler());
        connectionUnitConfigRegistry.registerConsistencyHandler(new ConnectionTilesConsistencyHandler(locationRegistry));
        connectionUnitConfigRegistry.registerConsistencyHandler(new ConnectionLocationConsistencyHandler(locationRegistry));
        connectionUnitConfigRegistry.registerConsistencyHandler(new ConnectionScopeConsistencyHandler(locationRegistry));
        connectionUnitConfigRegistry.registerConsistencyHandler(new ConnectionTransformationFrameConsistencyHandler(locationRegistry));
        connectionUnitConfigRegistry.registerPlugin(new PublishConnectionTransformationRegistryPlugin(locationRegistry));

        //TODO replace null with device class remote registry
        dalUnitConfigRegistry.registerConsistencyHandler(new DalUnitEnablingStateConsistencyHandler(deviceRegistry));
        dalUnitConfigRegistry.registerConsistencyHandler(new DalUnitHostIdConsistencyHandler(deviceRegistry));
        dalUnitConfigRegistry.registerConsistencyHandler(new DalUnitLabelConsistencyHandler(null, deviceRegistry));
        dalUnitConfigRegistry.registerConsistencyHandler(new DalUnitLocationIdConsistencyHandler(locationRegistry, deviceRegistry));
        dalUnitConfigRegistry.registerConsistencyHandler(new DalUnitScopeConsistencyHandler(locationRegistry));
        dalUnitConfigRegistry.registerConsistencyHandler(new UnitBoundToHostConsistencyHandler(deviceRegistry));
        dalUnitConfigRegistry.registerConsistencyHandler(new DalUnitEnablingStateConsistencyHandler(deviceRegistry));
        Registry.registerConsistencyHandler(new UnitTransformationFrameConsistencyHandler(locationRegistry));

        //TODO replace null with device class remote registry
        deviceUnitConfigRegistry.registerConsistencyHandler(new DeviceConfigDeviceClassIdConsistencyHandler(null));
        deviceUnitConfigRegistry.registerConsistencyHandler(new DeviceConfigDeviceClassUnitConsistencyHandler(null, dalUnitRegistry));
        deviceUnitConfigRegistry.registerConsistencyHandler(new DeviceConfigLocationIdForInstalledDevicesConsistencyHandler());
        deviceUnitConfigRegistry.registerConsistencyHandler(new DeviceEnablingStateConsistencyHandler());
        deviceUnitConfigRegistry.registerConsistencyHandler(new DeviceLabelConsistencyHandler());
        deviceUnitConfigRegistry.registerConsistencyHandler(new DeviceLocationIdConsistencyHandler(locationRegistry));
        deviceUnitConfigRegistry.registerConsistencyHandler(new DeviceOwnerConsistencyHandler(userUnitConfigRegistry));
        deviceUnitConfigRegistry.registerConsistencyHandler(new DeviceScopeConsistencyHandler(locationRegistry));
        deviceUnitConfigRegistry.registerConsistencyHandler(new DeviceTransformationFrameConsistencyHandler(locationRegistry));
        deviceUnitConfigRegistry.registerConsistencyHandler(new OpenhabServiceConfigItemIdConsistencyHandler(null, locationRegistry, dalUnitRegistry));
        deviceUnitConfigRegistry.registerConsistencyHandler(new SyncBindingConfigDeviceClassUnitConsistencyHandler(null, dalUnitRegistry));

        unitTemplateRegistry.registerConsistencyHandler(new UnitTemplateValidationConsistencyHandler(unitTemplateRegistry));
        unitTemplateRegistry.registerPlugin(new UnitTemplateCreatorRegistryPlugin(unitTemplateRegistry));

        userUnitConfigRegistry.registerConsistencyHandler(new UserConfigScopeConsistencyHandler());
        userUnitConfigRegistry.registerConsistencyHandler(new UserConfigUserNameConsistencyHandler());

        unitGroupUnitConfigRegistry.registerConsistencyHandler(new UnitGroupMemberListDuplicationConsistencyHandler());
        unitGroupUnitConfigRegistry.registerConsistencyHandler(new UnitGroupMemberExistsConsistencyHandler(deviceRegistry));
        unitGroupUnitConfigRegistry.registerConsistencyHandler(new UnitGroupUnitTypeConsistencyHandler(unitTemplateRegistry));
        unitGroupUnitConfigRegistry.registerConsistencyHandler(new UnitGroupMemberListTypesConsistencyHandler(deviceRegistry, unitTemplateRegistry));
        unitGroupUnitConfigRegistry.registerConsistencyHandler(new UnitGroupScopeConsistencyHandler(locationRegistryRemote));

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
        locationUnitConfigRegistry.registerConsistencyHandler(new LocationUnitIdConsistencyHandler(deviceRegistryRemote));
        locationUnitConfigRegistry.registerConsistencyHandler(new LocationTransformationFrameConsistencyHandler(locationRegistry));
        locationUnitConfigRegistry.registerPlugin(new PublishLocationTransformationRegistryPlugin());

        sceneUnitConfigRegistry.registerConsistencyHandler(new ScopeConsistencyHandler(locationRegistryRemote));
        sceneUnitConfigRegistry.registerConsistencyHandler(new LabelConsistencyHandler());
    }

    /**
     * {@inheritDoc}
     *
     * @throws CouldNotPerformException {@inheritDoc}
     */
    @Override
    protected void registerObserver() throws CouldNotPerformException {
        dalUnitConfigRegistry.addObserver((Observable<Map<String, IdentifiableMessage<String, UnitConfig, UnitConfig.Builder>>> source, Map<String, IdentifiableMessage<String, UnitConfig, UnitConfig.Builder>> data) -> {
            notifyChange();
        });
        unitTemplateRegistry.addObserver((Observable<Map<String, IdentifiableMessage<String, UnitTemplate, UnitTemplate.Builder>>> source, Map<String, IdentifiableMessage<String, UnitTemplate, UnitTemplate.Builder>> data) -> {
            notifyChange();
        });

        userUnitConfigRegistry.addObserver((final Observable<Map<String, IdentifiableMessage<String, UserConfigType.UserConfig, UserConfigType.UserConfig.Builder>>> source, Map<String, IdentifiableMessage<String, UserConfigType.UserConfig, UserConfigType.UserConfig.Builder>> data) -> {
            notifyChange();
        });

        authorizationGroupUnitConfigRegistry.addObserver((final Observable<Map<String, IdentifiableMessage<String, AuthorizationGroupConfigType.AuthorizationGroupConfig, AuthorizationGroupConfigType.AuthorizationGroupConfig.Builder>>> source, Map<String, IdentifiableMessage<String, AuthorizationGroupConfigType.AuthorizationGroupConfig, AuthorizationGroupConfigType.AuthorizationGroupConfig.Builder>> data) -> {
            notifyChange();
        });

        unitTemplateRegistry.addObserver((Observable<Map<String, IdentifiableMessage<String, UnitTemplate, UnitTemplate.Builder>>> source, Map<String, IdentifiableMessage<String, UnitTemplate, UnitTemplate.Builder>> data) -> {
            notifyChange();
        });

        deviceUnitConfigRegistry.addObserver((Observable<Map<String, IdentifiableMessage<String, DeviceConfigType.DeviceConfig, DeviceConfigType.DeviceConfig.Builder>>> source, Map<String, IdentifiableMessage<String, DeviceConfigType.DeviceConfig, DeviceConfigType.DeviceConfig.Builder>> data) -> {
            notifyChange();
        });

        unitGroupUnitConfigRegistry.addObserver((Observable<Map<String, IdentifiableMessage<String, UnitGroupConfigType.UnitGroupConfig, UnitGroupConfigType.UnitGroupConfig.Builder>>> source, Map<String, IdentifiableMessage<String, UnitGroupConfigType.UnitGroupConfig, UnitGroupConfigType.UnitGroupConfig.Builder>> data) -> {
            notifyChange();
        });

        locationUnitConfigRegistry.addObserver((Observable<Map<String, IdentifiableMessage<String, LocationConfigType.LocationConfig, LocationConfigType.LocationConfig.Builder>>> source, Map<String, IdentifiableMessage<String, LocationConfigType.LocationConfig, LocationConfigType.LocationConfig.Builder>> data) -> {
            notifyChange();
        });

        connectionUnitConfigRegistry.addObserver((Observable<Map<String, IdentifiableMessage<String, ConnectionConfigType.ConnectionConfig, ConnectionConfigType.ConnectionConfig.Builder>>> source, Map<String, IdentifiableMessage<String, ConnectionConfigType.ConnectionConfig, ConnectionConfigType.ConnectionConfig.Builder>> data) -> {
            notifyChange();
        });

        agentUnitConfigRegistry.addObserver((final Observable<Map<String, IdentifiableMessage<String, AgentConfigType.AgentConfig, AgentConfigType.AgentConfig.Builder>>> source, Map<String, IdentifiableMessage<String, AgentConfigType.AgentConfig, AgentConfigType.AgentConfig.Builder>> data) -> {
            notifyChange();
        });

        sceneUnitConfigRegistry.addObserver((final Observable<Map<String, IdentifiableMessage<String, SceneConfigType.SceneConfig, SceneConfigType.SceneConfig.Builder>>> source, Map<String, IdentifiableMessage<String, SceneConfigType.SceneConfig, SceneConfigType.SceneConfig.Builder>> data) -> {
            notifyChange();
        });

        appUnitConfigRegistry.addObserver((Observable<Map<String, IdentifiableMessage<String, AppConfigType.AppConfig, AppConfigType.AppConfig.Builder>>> source, Map<String, IdentifiableMessage<String, AppConfigType.AppConfig, AppConfigType.AppConfig.Builder>> data) -> {
            notifyChange();
        });

    }

    /**
     * {@inheritDoc}
     *
     * @throws CouldNotPerformException {@inheritDoc}
     */
    @Override
    protected void registerDependencies() throws CouldNotPerformException {
        dalUnitConfigRegistry.registerDependency(unitTemplateRegistry);
        authorizationGroupUnitConfigRegistry.registerDependency(userUnitConfigRegistry);
        deviceUnitConfigRegistry.registerDependency(locationRegistryRemote.getLocationConfigRemoteRegistry());
        deviceUnitConfigRegistry.registerDependency(userRegistryRemote.getUserConfigRemoteRegistry());
        deviceUnitConfigRegistry.registerDependency(deviceClassRegistry);
        unitGroupUnitConfigRegistry.registerDependency(deviceUnitConfigRegistry);
        locationUnitConfigRegistry.registerDependency(deviceRegistryRemote.getDeviceConfigRemoteRegistry());
        connectionUnitConfigRegistry.registerDependency(deviceRegistryRemote.getDeviceConfigRemoteRegistry());
        agentUnitConfigRegistry.registerDependency(locationRegistryRemote.getLocationConfigRemoteRegistry());
        agentUnitConfigRegistry.registerDependency(agentClassRegistry);
        sceneUnitConfigRegistry.registerDependency(locationRegistryRemote.getLocationConfigRemoteRegistry());
        appUnitConfigRegistry.registerDependency(appClassRegistry);
        appUnitConfigRegistry.registerDependency(locationRegistryRemote.getLocationConfigRemoteRegistry());
    }

    /**
     * {@inheritDoc}
     *
     * @throws CouldNotPerformException {@inheritDoc}
     */
    @Override
    protected void removeDependencies() throws CouldNotPerformException {
        dalUnitConfigRegistry.removeDependency(unitTemplateRegistry);
        authorizationGroupUnitConfigRegistry.removeDependency(userUnitConfigRegistry);
        deviceUnitConfigRegistry.removeDependency(locationRegistryRemote.getLocationConfigRemoteRegistry());
        deviceUnitConfigRegistry.removeDependency(userRegistryRemote.getUserConfigRemoteRegistry());
        deviceUnitConfigRegistry.removeDependency(deviceClassRegistry);
        unitGroupUnitConfigRegistry.removeDependency(deviceUnitConfigRegistry);
        locationUnitConfigRegistry.removeDependency(deviceRegistryRemote.getDeviceConfigRemoteRegistry());
        connectionUnitConfigRegistry.agentUnitConfigRegistry(deviceRegistryRemote.getDeviceConfigRemoteRegistry());
        agentUnitConfigRegistry.removeDependency(locationRegistryRemote.getLocationConfigRemoteRegistry());
        agentUnitConfigRegistry.removeDependency(agentClassRegistry);
        sceneUnitConfigRegistry.removeDependency(locationRegistryRemote.getLocationConfigRemoteRegistry());
        appUnitConfigRegistry.removeDependency(appClassRegistry);
        appUnitConfigRegistry.removeDependency(locationRegistryRemote.getLocationConfigRemoteRegistry());
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
            deviceUnitConfigRegistry.registerPlugin(new PublishDeviceTransformationRegistryPlugin(locationRegistryRemote));
        } catch (CouldNotPerformException ex) {
            ExceptionPrinter.printHistory(new CouldNotPerformException("Could not load all plugins!", ex), logger, LogLevel.ERROR);
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

    /**
     * {@inheritDoc}
     *
     * @throws InterruptedException
     * @throws CouldNotPerformException
     */
    @Override
    public void deactivate() throws InterruptedException, CouldNotPerformException {

        if (userUnitConfigRegistry != null) {
            userUnitConfigRegistry.deactivate();
        }

        if (authorizationGroupUnitConfigRegistry != null) {
            authorizationGroupUnitConfigRegistry.deactivate();
        }
        super.deactivate();
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

        setDataField(UserRegistryDataType.UserRegistryData.USER_CONFIG_REGISTRY_READ_ONLY_FIELD_NUMBER, userUnitConfigRegistry.isReadOnly());
        setDataField(UserRegistryDataType.UserRegistryData.AUTHORIZATION_GROUP_CONFIG_REGISTRY_READ_ONLY_FIELD_NUMBER, authorizationGroupUnitConfigRegistry.isReadOnly());
        setDataField(UserRegistryDataType.UserRegistryData.USER_CONFIG_REGISTRY_CONSISTENT_FIELD_NUMBER, userUnitConfigRegistry.isConsistent());
        setDataField(UserRegistryDataType.UserRegistryData.AUTHORIZATION_GROUP_CONFIG_REGISTRY_CONSISTENT_FIELD_NUMBER, authorizationGroupUnitConfigRegistry.isConsistent());

        setDataField(DeviceRegistryDataType.DeviceRegistryData.DEVICE_CONFIG_REGISTRY_READ_ONLY_FIELD_NUMBER, deviceUnitConfigRegistry.isReadOnly());
        setDataField(DeviceRegistryDataType.DeviceRegistryData.UNIT_TEMPLATE_REGISTRY_READ_ONLY_FIELD_NUMBER, unitTemplateRegistry.isReadOnly());
        setDataField(DeviceRegistryDataType.DeviceRegistryData.UNIT_GROUP_REGISTRY_READ_ONLY_FIELD_NUMBER, unitGroupUnitConfigRegistry.isReadOnly());
        setDataField(DeviceRegistryDataType.DeviceRegistryData.DEVICE_CONFIG_REGISTRY_CONSISTENT_FIELD_NUMBER, deviceUnitConfigRegistry.isConsistent());
        setDataField(DeviceRegistryDataType.DeviceRegistryData.UNIT_TEMPLATE_REGISTRY_CONSISTENT_FIELD_NUMBER, unitTemplateRegistry.isConsistent());
        setDataField(DeviceRegistryDataType.DeviceRegistryData.UNIT_GROUP_REGISTRY_CONSISTENT_FIELD_NUMBER, unitGroupUnitConfigRegistry.isConsistent());

        setDataField(LocationRegistryDataType.LocationRegistryData.LOCATION_CONFIG_REGISTRY_READ_ONLY_FIELD_NUMBER, locationUnitConfigRegistry.isReadOnly());
        setDataField(LocationRegistryDataType.LocationRegistryData.CONNECTION_CONFIG_REGISTRY_READ_ONLY_FIELD_NUMBER, connectionUnitConfigRegistry.isReadOnly());
        setDataField(LocationRegistryDataType.LocationRegistryData.LOCATION_CONFIG_REGISTRY_CONSISTENT_FIELD_NUMBER, locationUnitConfigRegistry.isConsistent());
        setDataField(LocationRegistryDataType.LocationRegistryData.CONNECTION_CONFIG_REGISTRY_CONSISTENT_FIELD_NUMBER, connectionUnitConfigRegistry.isConsistent());

        setDataField(SceneRegistryDataType.SceneRegistryData.SCENE_CONFIG_REGISTRY_READ_ONLY_FIELD_NUMBER, sceneUnitConfigRegistry.isReadOnly());
        setDataField(SceneRegistryDataType.SceneRegistryData.SCENE_CONFIG_REGISTRY_CONSISTENT_FIELD_NUMBER, sceneUnitConfigRegistry.isConsistent());

        setDataField(AgentRegistryDataType.AgentRegistryData.AGENT_CONFIG_REGISTRY_READ_ONLY_FIELD_NUMBER, agentUnitConfigRegistry.isReadOnly());
        agentUnitConfigRegistry(AgentRegistryDataType.AgentRegistryData.AGENT_CONFIG_REGISTRY_CONSISTENT_FIELD_NUMBER, agentUnitConfigRegistry.isConsistent());

        setDataField(AppRegistryDataType.AppRegistryData.APP_CONFIG_REGISTRY_READ_ONLY_FIELD_NUMBER, appUnitConfigRegistry.isReadOnly());
        setDataField(AppRegistryDataType.AppRegistryData.APP_CONFIG_REGISTRY_CONSISTENT_FIELD_NUMBER, appUnitConfigRegistry.isConsistent());

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

    public ProtoBufFileSynchronizedRegistry<String, UnitConfig, UnitConfig.Builder, UnitRegistryData.Builder> getUnitConfigRegistry() {
        return dalUnitConfigRegistry;
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
        return unitTemplateRegistry.getMessage(((UnitTemplateIdGenerator) unitTemplateRegistry.getIdGenerator()).generateId(type));
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

    public ProtoBufFileSynchronizedRegistry<String, UnitTemplate, UnitTemplate.Builder, UnitRegistryData.Builder> getUnitTemplateRegistry() {
        return unitTemplateRegistry;
    }

    public ProtoBufFileSynchronizedRegistry<String, UserConfigType.UserConfig, UserConfigType.UserConfig.Builder, UserRegistryDataType.UserRegistryData.Builder> getUserRegistry() {
        return userUnitConfigRegistry;
    }

    public ProtoBufFileSynchronizedRegistry<String, AuthorizationGroupConfigType.AuthorizationGroupConfig, AuthorizationGroupConfigType.AuthorizationGroupConfig.Builder, UserRegistryDataType.UserRegistryData.Builder> getAuthorizationGroupRegistry() {
        return authorizationGroupUnitConfigRegistry;
    }
}
