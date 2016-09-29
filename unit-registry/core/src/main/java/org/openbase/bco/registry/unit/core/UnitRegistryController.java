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
import org.openbase.bco.registry.unit.core.plugin.PublishConnectionTransformationRegistryPlugin;
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
    private final ProtoBufFileSynchronizedRegistry<String, UnitConfig, UnitConfig.Builder, UnitRegistryData.Builder> dalUnitRegistry;
    private final ProtoBufFileSynchronizedRegistry<String, UnitConfig, UnitConfig.Builder, UnitRegistryData.Builder> userRegistry;
    private final ProtoBufFileSynchronizedRegistry<String, UnitConfig, UnitConfig.Builder, UnitRegistryData.Builder> authorizationGroupRegistry;
    private final ProtoBufFileSynchronizedRegistry<String, UnitConfig, UnitConfig.Builder, UnitRegistryData.Builder> deviceRegistry;
    private final ProtoBufFileSynchronizedRegistry<String, UnitConfig, UnitConfig.Builder, UnitRegistryData.Builder> unitGroupRegistry;
    private final ProtoBufFileSynchronizedRegistry<String, UnitConfig, UnitConfig.Builder, UnitRegistryData.Builder> locationRegistry;
    private final ProtoBufFileSynchronizedRegistry<String, UnitConfig, UnitConfig.Builder, UnitRegistryData.Builder> connectionRegistry;
    private final ProtoBufFileSynchronizedRegistry<String, UnitConfig, UnitConfig.Builder, UnitRegistryData.Builder> agentRegistry;
    private final ProtoBufFileSynchronizedRegistry<String, UnitConfig, UnitConfig.Builder, UnitRegistryData.Builder> sceneRegistry;
    private final ProtoBufFileSynchronizedRegistry<String, UnitConfig, UnitConfig.Builder, UnitRegistryData.Builder> appRegistry;
    
    public UnitRegistryController() throws InstantiationException, InterruptedException {
        super(JPUnitRegistryScope.class, UnitRegistryData.newBuilder());
        try {
            this.dalUnitRegistry = new ProtoBufFileSynchronizedRegistry<>(UnitConfig.class, getBuilderSetup(), getDataFieldDescriptor(UnitRegistryData.UNIT_CONFIG_FIELD_NUMBER), new UnitConfigIdGenerator(), JPService.getProperty(JPUnitConfigDatabaseDirectory.class).getValue(), protoBufJSonFileProvider);
            this.unitTemplateRegistry = new ProtoBufFileSynchronizedRegistry<>(UnitTemplate.class, getBuilderSetup(), getDataFieldDescriptor(UnitRegistryData.UNIT_TEMPLATE_FIELD_NUMBER), new UnitTemplateIdGenerator(), JPService.getProperty(JPUnitTemplateDatabaseDirectory.class).getValue(), protoBufJSonFileProvider);
            this.userRegistry = new ProtoBufFileSynchronizedRegistry<>(UserConfigType.UserConfig.class, getBuilderSetup(), getDataFieldDescriptor(UserRegistryDataType.UserRegistryData.USER_CONFIG_FIELD_NUMBER), new UserConfigIdGenerator(), JPService.getProperty(JPUserConfigDatabaseDirectory.class).getValue(), protoBufJSonFileProvider);
            this.authorizationGroupRegistry = new ProtoBufFileSynchronizedRegistry<>(AuthorizationGroupConfigType.AuthorizationGroupConfig.class, getBuilderSetup(), getDataFieldDescriptor(UserRegistryDataType.UserRegistryData.AUTHORIZATION_GROUP_CONFIG_FIELD_NUMBER), new AuthorizationGroupConfigIdGenerator(), JPService.getProperty(JPAuthorizationGroupConfigDatabaseDirectory.class).getValue(), protoBufJSonFileProvider);
            this.deviceRegistry = new ProtoBufFileSynchronizedRegistry<>(DeviceConfigType.DeviceConfig.class, getBuilderSetup(), getDataFieldDescriptor(DeviceRegistryDataType.DeviceRegistryData.DEVICE_CONFIG_FIELD_NUMBER), new DeviceConfigIdGenerator(), JPService.getProperty(JPDeviceConfigDatabaseDirectory.class).getValue(), protoBufJSonFileProvider);
            this.unitGroupRegistry = new ProtoBufFileSynchronizedRegistry<>(UnitGroupConfigType.UnitGroupConfig.class, getBuilderSetup(), getDataFieldDescriptor(DeviceRegistryDataType.DeviceRegistryData.UNIT_GROUP_CONFIG_FIELD_NUMBER), new UnitGroupIdGenerator(), JPService.getProperty(JPUnitGroupDatabaseDirectory.class).getValue(), protoBufJSonFileProvider);
            this.locationRegistry = new ProtoBufFileSynchronizedRegistry<>(LocationConfigType.LocationConfig.class, getBuilderSetup(), getDataFieldDescriptor(LocationRegistryDataType.LocationRegistryData.LOCATION_CONFIG_FIELD_NUMBER), new LocationIDGenerator(), JPService.getProperty(JPLocationConfigDatabaseDirectory.class).getValue(), new ProtoBufJSonFileProvider());
            this.connectionRegistry = new ProtoBufFileSynchronizedRegistry<>(ConnectionConfigType.ConnectionConfig.class, getBuilderSetup(), getDataFieldDescriptor(LocationRegistryDataType.LocationRegistryData.CONNECTION_CONFIG_FIELD_NUMBER), new ConnectionIDGenerator(), JPService.getProperty(JPConnectionConfigDatabaseDirectory.class).getValue(), new ProtoBufJSonFileProvider());
            this.agentRegistry = new ProtoBufFileSynchronizedRegistry<>(AgentConfigType.AgentConfig.class, getBuilderSetup(), getDataFieldDescriptor(AgentRegistryDataType.AgentRegistryData.AGENT_CONFIG_FIELD_NUMBER), new AgentConfigIdGenerator(), JPService.getProperty(JPAgentConfigDatabaseDirectory.class).getValue(), protoBufJSonFileProvider);
            this.sceneRegistry = new ProtoBufFileSynchronizedRegistry<>(SceneConfigType.SceneConfig.class, getBuilderSetup(), getDataFieldDescriptor(SceneRegistryDataType.SceneRegistryData.SCENE_CONFIG_FIELD_NUMBER), new SceneConfigIdGenerator(), JPService.getProperty(JPSceneConfigDatabaseDirectory.class).getValue(), protoBufJSonFileProvider);
            this.appRegistry = new ProtoBufFileSynchronizedRegistry<>(AppConfigType.AppConfig.class, getBuilderSetup(), getDataFieldDescriptor(AppRegistryDataType.AppRegistryData.APP_CONFIG_FIELD_NUMBER), new AppConfigIdGenerator(), JPService.getProperty(JPAppConfigDatabaseDirectory.class).getValue(), protoBufJSonFileProvider);
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
        dalUnitRegistry.activateVersionControl(DummyConverter.class.getPackage());
        userRegistry.activateVersionControl(DummyConverter.class.getPackage());
        authorizationGroupRegistry.activateVersionControl(DummyConverter.class.getPackage());
        deviceRegistry.activateVersionControl(DeviceConfig_0_To_1_DBConverter.class.getPackage());
        unitTemplateRegistry.activateVersionControl(UnitTemplate_0_To_1_DBConverter.class.getPackage());
        unitGroupRegistry.activateVersionControl(UnitGroupConfig_0_To_1_DBConverter.class.getPackage());
        locationRegistry.activateVersionControl(LocationConfig_0_To_1_DBConverter.class.getPackage());
        connectionRegistry.activateVersionControl(LocationConfig_0_To_1_DBConverter.class.getPackage());
        agentRegistry.activateVersionControl(AgentConfig_0_To_1_DBConverter.class.getPackage());
        sceneRegistry.activateVersionControl(SceneConfig_0_To_1_DBConverter.class.getPackage());
        appRegistry.activateVersionControl(DummyConverter.class.getPackage());
    }

    /**
     * {@inheritDoc}
     *
     * @throws CouldNotPerformException {@inheritDoc}
     */
    @Override
    protected void loadRegistries() throws CouldNotPerformException {
        unitTemplateRegistry.loadRegistry();
        dalUnitRegistry.loadRegistry();
        userRegistry.loadRegistry();
        authorizationGroupRegistry.loadRegistry();
        unitTemplateRegistry.loadRegistry();
        deviceRegistry.loadRegistry();
        unitGroupRegistry.loadRegistry();
        locationRegistry.loadRegistry();
        connectionRegistry.loadRegistry();
        agentRegistry.loadRegistry();
        sceneRegistry.loadRegistry();
        appRegistry.loadRegistry();
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
        //agentConfigRegistry.registerConsistencyHandler(new AgentConfigAgentClassIdConsistencyHandler(agentClassRegistry));
        agentRegistry.registerConsistencyHandler(new AgentLabelConsistencyHandler());
        agentRegistry.registerConsistencyHandler(new AgentLocationConsistencyHandler(locationRegistry));
        agentRegistry.registerConsistencyHandler(new AgentScopeConsistencyHandler(locationRegistry));

        //TODO: should be activated but fails in the current db version since appClasses have just been introduced
        //appConfigRegistry.registerConsistencyHandler(new AppConfigAppClassIdConsistencyHandler(appClassRegistry));
        appRegistry.registerConsistencyHandler(new AppLabelConsistencyHandler());
        appRegistry.registerConsistencyHandler(new AppLocationConsistencyHandler(locationRegistry));
        appRegistry.registerConsistencyHandler(new AppScopeConsistencyHandler(locationRegistry));
        
        authorizationGroupRegistry.registerConsistencyHandler(new AuthorizationGroupConfigLabelConsistencyHandler());
        authorizationGroupRegistry.registerConsistencyHandler(new AuthorizationGroupConfigScopeConsistencyHandler());
        
        connectionRegistry.registerConsistencyHandler(new ConnectionLabelConsistencyHandler());
        connectionRegistry.registerConsistencyHandler(new ConnectionTilesConsistencyHandler(locationRegistry));
        connectionRegistry.registerConsistencyHandler(new ConnectionLocationConsistencyHandler(locationRegistry));
        connectionRegistry.registerConsistencyHandler(new ConnectionScopeConsistencyHandler(locationRegistry));
        connectionRegistry.registerConsistencyHandler(new ConnectionTransformationFrameConsistencyHandler(locationRegistry));
        connectionRegistry.registerPlugin(new PublishConnectionTransformationRegistryPlugin(locationRegistry));

        //TODO replace null with device class remote registry
        dalUnitRegistry.registerConsistencyHandler(new DalUnitEnablingStateConsistencyHandler(deviceRegistry));
        dalUnitRegistry.registerConsistencyHandler(new DalUnitHostIdConsistencyHandler(deviceRegistry));
        dalUnitRegistry.registerConsistencyHandler(new DalUnitLabelConsistencyHandler(null, deviceRegistry));
        dalUnitRegistry.registerConsistencyHandler(new DalUnitLocationIdConsistencyHandler(locationRegistry, deviceRegistry));
        dalUnitRegistry.registerConsistencyHandler(new DalUnitScopeConsistencyHandler(locationRegistry));
        dalUnitRegistry.registerConsistencyHandler(new UnitBoundToHostConsistencyHandler(deviceRegistry));
        dalUnitRegistry.registerConsistencyHandler(new UnitTransformationFrameConsistencyHandler(locationRegistry));

        //TODO replace null with device class remote registry
        deviceRegistry.registerConsistencyHandler(new DeviceConfigDeviceClassIdConsistencyHandler(null));
        deviceRegistry.registerConsistencyHandler(new DeviceConfigDeviceClassUnitConsistencyHandler(null, dalUnitRegistry));
        deviceRegistry.registerConsistencyHandler(new DeviceConfigLocationIdForInstalledDevicesConsistencyHandler());
        deviceRegistry.registerConsistencyHandler(new DeviceEnablingStateConsistencyHandler());
        deviceRegistry.registerConsistencyHandler(new DeviceLabelConsistencyHandler());
        deviceRegistry.registerConsistencyHandler(new DeviceLocationIdConsistencyHandler(locationRegistry));
        deviceRegistry.registerConsistencyHandler(new DeviceOwnerConsistencyHandler(userRegistry));
        deviceRegistry.registerConsistencyHandler(new DeviceScopeConsistencyHandler(locationRegistry));
        deviceRegistry.registerConsistencyHandler(new DeviceTransformationFrameConsistencyHandler(locationRegistry));
        deviceRegistry.registerConsistencyHandler(new OpenhabServiceConfigItemIdConsistencyHandler(null, locationRegistry, dalUnitRegistry));
        deviceRegistry.registerConsistencyHandler(new SyncBindingConfigDeviceClassUnitConsistencyHandler(null, dalUnitRegistry));
        
        unitTemplateRegistry.registerConsistencyHandler(new UnitTemplateValidationConsistencyHandler(unitTemplateRegistry));
        unitTemplateRegistry.registerPlugin(new UnitTemplateCreatorRegistryPlugin(unitTemplateRegistry));
        
        userRegistry.registerConsistencyHandler(new UserConfigScopeConsistencyHandler());
        userRegistry.registerConsistencyHandler(new UserConfigUserNameConsistencyHandler());
        
        unitGroupRegistry.registerConsistencyHandler(new UnitGroupMemberListDuplicationConsistencyHandler());
        unitGroupRegistry.registerConsistencyHandler(new UnitGroupMemberExistsConsistencyHandler(deviceRegistry));
        unitGroupRegistry.registerConsistencyHandler(new UnitGroupUnitTypeConsistencyHandler(unitTemplateRegistry));
        unitGroupRegistry.registerConsistencyHandler(new UnitGroupMemberListTypesConsistencyHandler(deviceRegistry, unitTemplateRegistry));
        unitGroupRegistry.registerConsistencyHandler(new UnitGroupScopeConsistencyHandler(locationRegistryRemote));
        
        locationRegistry.registerConsistencyHandler(new LocationPlacementConfigConsistencyHandler());
        locationRegistry.registerConsistencyHandler(new LocationPositionConsistencyHandler());
        locationRegistry.registerConsistencyHandler(new RootConsistencyHandler());
        locationRegistry.registerConsistencyHandler(new LocationChildConsistencyHandler());
        locationRegistry.registerConsistencyHandler(new LocationIdConsistencyHandler());
        locationRegistry.registerConsistencyHandler(new LocationParentConsistencyHandler());
        locationRegistry.registerConsistencyHandler(new RootLocationExistencConsistencyHandler());
        locationRegistry.registerConsistencyHandler(new LocationLoopConsistencyHandler());
        locationRegistry.registerConsistencyHandler(new ChildWithSameLabelConsistencyHandler());
        locationRegistry.registerConsistencyHandler(new LocationScopeConsistencyHandler());
        locationRegistry.registerConsistencyHandler(new LocationUnitIdConsistencyHandler(deviceRegistryRemote));
        locationRegistry.registerConsistencyHandler(new LocationTransformationFrameConsistencyHandler(locationRegistry));
        locationRegistry.registerPlugin(new PublishLocationTransformationRegistryPlugin());
        
        sceneRegistry.registerConsistencyHandler(new ScopeConsistencyHandler(locationRegistryRemote));
        sceneRegistry.registerConsistencyHandler(new LabelConsistencyHandler());
        
    }

    /**
     * {@inheritDoc}
     *
     * @throws CouldNotPerformException {@inheritDoc}
     */
    @Override
    protected void registerObserver() throws CouldNotPerformException {
        dalUnitRegistry.addObserver((Observable<Map<String, IdentifiableMessage<String, UnitConfig, UnitConfig.Builder>>> source, Map<String, IdentifiableMessage<String, UnitConfig, UnitConfig.Builder>> data) -> {
            notifyChange();
        });
        unitTemplateRegistry.addObserver((Observable<Map<String, IdentifiableMessage<String, UnitTemplate, UnitTemplate.Builder>>> source, Map<String, IdentifiableMessage<String, UnitTemplate, UnitTemplate.Builder>> data) -> {
            notifyChange();
        });
        
        userRegistry.addObserver((final Observable<Map<String, IdentifiableMessage<String, UserConfigType.UserConfig, UserConfigType.UserConfig.Builder>>> source, Map<String, IdentifiableMessage<String, UserConfigType.UserConfig, UserConfigType.UserConfig.Builder>> data) -> {
            notifyChange();
        });
        
        authorizationGroupRegistry.addObserver((final Observable<Map<String, IdentifiableMessage<String, AuthorizationGroupConfigType.AuthorizationGroupConfig, AuthorizationGroupConfigType.AuthorizationGroupConfig.Builder>>> source, Map<String, IdentifiableMessage<String, AuthorizationGroupConfigType.AuthorizationGroupConfig, AuthorizationGroupConfigType.AuthorizationGroupConfig.Builder>> data) -> {
            notifyChange();
        });
        
        unitTemplateRegistry.addObserver((Observable<Map<String, IdentifiableMessage<String, UnitTemplate, UnitTemplate.Builder>>> source, Map<String, IdentifiableMessage<String, UnitTemplate, UnitTemplate.Builder>> data) -> {
            notifyChange();
        });
        
        deviceRegistry.addObserver((Observable<Map<String, IdentifiableMessage<String, DeviceConfigType.DeviceConfig, DeviceConfigType.DeviceConfig.Builder>>> source, Map<String, IdentifiableMessage<String, DeviceConfigType.DeviceConfig, DeviceConfigType.DeviceConfig.Builder>> data) -> {
            notifyChange();
        });
        
        unitGroupRegistry.addObserver((Observable<Map<String, IdentifiableMessage<String, UnitGroupConfigType.UnitGroupConfig, UnitGroupConfigType.UnitGroupConfig.Builder>>> source, Map<String, IdentifiableMessage<String, UnitGroupConfigType.UnitGroupConfig, UnitGroupConfigType.UnitGroupConfig.Builder>> data) -> {
            notifyChange();
        });
        
        locationRegistry.addObserver((Observable<Map<String, IdentifiableMessage<String, LocationConfigType.LocationConfig, LocationConfigType.LocationConfig.Builder>>> source, Map<String, IdentifiableMessage<String, LocationConfigType.LocationConfig, LocationConfigType.LocationConfig.Builder>> data) -> {
            notifyChange();
        });
        
        connectionRegistry.addObserver((Observable<Map<String, IdentifiableMessage<String, ConnectionConfigType.ConnectionConfig, ConnectionConfigType.ConnectionConfig.Builder>>> source, Map<String, IdentifiableMessage<String, ConnectionConfigType.ConnectionConfig, ConnectionConfigType.ConnectionConfig.Builder>> data) -> {
            notifyChange();
        });
        
        agentRegistry.addObserver((final Observable<Map<String, IdentifiableMessage<String, AgentConfigType.AgentConfig, AgentConfigType.AgentConfig.Builder>>> source, Map<String, IdentifiableMessage<String, AgentConfigType.AgentConfig, AgentConfigType.AgentConfig.Builder>> data) -> {
            notifyChange();
        });
        
        sceneRegistry.addObserver((final Observable<Map<String, IdentifiableMessage<String, SceneConfigType.SceneConfig, SceneConfigType.SceneConfig.Builder>>> source, Map<String, IdentifiableMessage<String, SceneConfigType.SceneConfig, SceneConfigType.SceneConfig.Builder>> data) -> {
            notifyChange();
        });
        
        appRegistry.addObserver((Observable<Map<String, IdentifiableMessage<String, AppConfigType.AppConfig, AppConfigType.AppConfig.Builder>>> source, Map<String, IdentifiableMessage<String, AppConfigType.AppConfig, AppConfigType.AppConfig.Builder>> data) -> {
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
        dalUnitRegistry.registerDependency(unitTemplateRegistry);
        authorizationGroupRegistry.registerDependency(userRegistry);
        deviceRegistry.registerDependency(locationRegistryRemote.getLocationConfigRemoteRegistry());
        deviceRegistry.registerDependency(userRegistryRemote.getUserConfigRemoteRegistry());
        deviceRegistry.registerDependency(deviceClassRegistry);
        unitGroupRegistry.registerDependency(deviceRegistry);
        locationRegistry.registerDependency(deviceRegistryRemote.getDeviceConfigRemoteRegistry());
        connectionRegistry.registerDependency(deviceRegistryRemote.getDeviceConfigRemoteRegistry());
        agentRegistry.registerDependency(locationRegistryRemote.getLocationConfigRemoteRegistry());
        agentRegistry.registerDependency(agentClassRegistry);
        sceneRegistry.registerDependency(locationRegistryRemote.getLocationConfigRemoteRegistry());
        appRegistry.registerDependency(appClassRegistry);
        appRegistry.registerDependency(locationRegistryRemote.getLocationConfigRemoteRegistry());
    }

    /**
     * {@inheritDoc}
     *
     * @throws CouldNotPerformException {@inheritDoc}
     */
    @Override
    protected void removeDependencies() throws CouldNotPerformException {
        dalUnitRegistry.removeDependency(unitTemplateRegistry);
        authorizationGroupRegistry.removeDependency(userRegistry);
        deviceRegistry.removeDependency(locationRegistryRemote.getLocationConfigRemoteRegistry());
        deviceRegistry.removeDependency(userRegistryRemote.getUserConfigRemoteRegistry());
        deviceRegistry.removeDependency(deviceClassRegistry);
        unitGroupRegistry.removeDependency(deviceRegistry);
        locationRegistry.removeDependency(deviceRegistryRemote.getDeviceConfigRemoteRegistry());
        connectionRegistry.removeDependency(deviceRegistryRemote.getDeviceConfigRemoteRegistry());
        agentRegistry.removeDependency(locationRegistryRemote.getLocationConfigRemoteRegistry());
        agentRegistry.removeDependency(agentClassRegistry);
        sceneRegistry.removeDependency(locationRegistryRemote.getLocationConfigRemoteRegistry());
        appRegistry.removeDependency(appClassRegistry);
        appRegistry.removeDependency(locationRegistryRemote.getLocationConfigRemoteRegistry());
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
            dalUnitRegistry.checkConsistency();
        } catch (CouldNotPerformException ex) {
            ExceptionPrinter.printHistory(new CouldNotPerformException("Initial consistency check failed!", ex), logger, LogLevel.WARN);
            notifyChange();
        }
        
        try {
            userRegistry.checkConsistency();
        } catch (CouldNotPerformException ex) {
            ExceptionPrinter.printHistory(new CouldNotPerformException("Initial consistency check failed!", ex), logger, LogLevel.WARN);
            notifyChange();
        }
        
        try {
            authorizationGroupRegistry.checkConsistency();
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
            deviceRegistry.checkConsistency();
        } catch (CouldNotPerformException ex) {
            ExceptionPrinter.printHistory(new CouldNotPerformException("Initial consistency check failed!", ex), logger, LogLevel.WARN);
            notifyChange();
        }
        
        try {
            unitGroupRegistry.checkConsistency();
        } catch (CouldNotPerformException ex) {
            ExceptionPrinter.printHistory(new CouldNotPerformException("Initial consistency check failed!", ex), logger, LogLevel.WARN);
            notifyChange();
        }
        
        try {
            deviceRegistry.registerPlugin(new PublishDeviceTransformationRegistryPlugin(locationRegistryRemote));
        } catch (CouldNotPerformException ex) {
            ExceptionPrinter.printHistory(new CouldNotPerformException("Could not load all plugins!", ex), logger, LogLevel.ERROR);
            notifyChange();
        }
        
        try {
            locationRegistry.checkConsistency();
        } catch (CouldNotPerformException ex) {
            ExceptionPrinter.printHistory(new CouldNotPerformException("Initial consistency check failed!", ex), logger, LogLevel.WARN);
            notifyChange();
        }
        
        try {
            connectionRegistry.checkConsistency();
        } catch (CouldNotPerformException ex) {
            ExceptionPrinter.printHistory(new CouldNotPerformException("Initial consistency check failed!", ex), logger, LogLevel.WARN);
            notifyChange();
        }
        
        try {
            agentRegistry.checkConsistency();
        } catch (CouldNotPerformException ex) {
            ExceptionPrinter.printHistory(new CouldNotPerformException("Initial consistency check failed!", ex), logger, LogLevel.WARN);
            notifyChange();
        }
        
        try {
            sceneRegistry.checkConsistency();
        } catch (CouldNotPerformException ex) {
            ExceptionPrinter.printHistory(new CouldNotPerformException("Initial consistency check failed!", ex), logger, LogLevel.WARN);
            notifyChange();
        }
        
        try {
            appRegistry.checkConsistency();
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
        
        if (userRegistry != null) {
            userRegistry.deactivate();
        }
        
        if (authorizationGroupRegistry != null) {
            authorizationGroupRegistry.deactivate();
        }
        super.deactivate();
    }
    
    @Override
    public void shutdown() {
        if (dalUnitRegistry != null) {
            dalUnitRegistry.shutdown();
        }
        
        if (unitTemplateRegistry != null) {
            unitTemplateRegistry.shutdown();
        }
        
        if (deviceRegistry != null) {
            deviceRegistry.shutdown();
        }
        
        if (unitTemplateRegistry != null) {
            unitTemplateRegistry.shutdown();
        }
        
        if (unitGroupRegistry != null) {
            unitGroupRegistry.shutdown();
        }
        
        if (locationRegistry != null) {
            locationRegistry.shutdown();
        }
        
        if (connectionRegistry != null) {
            connectionRegistry.shutdown();
        }
        
        if (sceneRegistry != null) {
            sceneRegistry.shutdown();
        }
        
        if (appRegistry != null) {
            appRegistry.shutdown();
        }
        
        super.shutdown();
    }
    
    @Override
    public final void notifyChange() throws CouldNotPerformException, InterruptedException {
        // sync flags
        setDataField(UnitRegistryData.UNIT_CONFIG_REGISTRY_READ_ONLY_FIELD_NUMBER, dalUnitRegistry.isReadOnly());
        setDataField(UnitRegistryData.UNIT_CONFIG_REGISTRY_CONSISTENT_FIELD_NUMBER, dalUnitRegistry.isConsistent());
        
        setDataField(UnitRegistryData.UNIT_TEMPLATE_REGISTRY_READ_ONLY_FIELD_NUMBER, unitTemplateRegistry.isReadOnly());
        setDataField(UnitRegistryData.UNIT_TEMPLATE_REGISTRY_CONSISTENT_FIELD_NUMBER, unitTemplateRegistry.isConsistent());
        
        setDataField(UserRegistryDataType.UserRegistryData.USER_CONFIG_REGISTRY_READ_ONLY_FIELD_NUMBER, userRegistry.isReadOnly());
        setDataField(UserRegistryDataType.UserRegistryData.AUTHORIZATION_GROUP_CONFIG_REGISTRY_READ_ONLY_FIELD_NUMBER, authorizationGroupRegistry.isReadOnly());
        setDataField(UserRegistryDataType.UserRegistryData.USER_CONFIG_REGISTRY_CONSISTENT_FIELD_NUMBER, userRegistry.isConsistent());
        setDataField(UserRegistryDataType.UserRegistryData.AUTHORIZATION_GROUP_CONFIG_REGISTRY_CONSISTENT_FIELD_NUMBER, authorizationGroupRegistry.isConsistent());
        
        setDataField(DeviceRegistryDataType.DeviceRegistryData.DEVICE_CONFIG_REGISTRY_READ_ONLY_FIELD_NUMBER, deviceRegistry.isReadOnly());
        setDataField(DeviceRegistryDataType.DeviceRegistryData.UNIT_TEMPLATE_REGISTRY_READ_ONLY_FIELD_NUMBER, unitTemplateRegistry.isReadOnly());
        setDataField(DeviceRegistryDataType.DeviceRegistryData.UNIT_GROUP_REGISTRY_READ_ONLY_FIELD_NUMBER, unitGroupRegistry.isReadOnly());
        setDataField(DeviceRegistryDataType.DeviceRegistryData.DEVICE_CONFIG_REGISTRY_CONSISTENT_FIELD_NUMBER, deviceRegistry.isConsistent());
        setDataField(DeviceRegistryDataType.DeviceRegistryData.UNIT_TEMPLATE_REGISTRY_CONSISTENT_FIELD_NUMBER, unitTemplateRegistry.isConsistent());
        setDataField(DeviceRegistryDataType.DeviceRegistryData.UNIT_GROUP_REGISTRY_CONSISTENT_FIELD_NUMBER, unitGroupRegistry.isConsistent());
        
        setDataField(LocationRegistryDataType.LocationRegistryData.LOCATION_CONFIG_REGISTRY_READ_ONLY_FIELD_NUMBER, locationRegistry.isReadOnly());
        setDataField(LocationRegistryDataType.LocationRegistryData.CONNECTION_CONFIG_REGISTRY_READ_ONLY_FIELD_NUMBER, connectionRegistry.isReadOnly());
        setDataField(LocationRegistryDataType.LocationRegistryData.LOCATION_CONFIG_REGISTRY_CONSISTENT_FIELD_NUMBER, locationRegistry.isConsistent());
        setDataField(LocationRegistryDataType.LocationRegistryData.CONNECTION_CONFIG_REGISTRY_CONSISTENT_FIELD_NUMBER, connectionRegistry.isConsistent());
        
        setDataField(SceneRegistryDataType.SceneRegistryData.SCENE_CONFIG_REGISTRY_READ_ONLY_FIELD_NUMBER, sceneRegistry.isReadOnly());
        setDataField(SceneRegistryDataType.SceneRegistryData.SCENE_CONFIG_REGISTRY_CONSISTENT_FIELD_NUMBER, sceneRegistry.isConsistent());
        
        super.notifyChange();
    }
    
    @Override
    public void registerMethods(final RSBLocalServer server) throws CouldNotPerformException {
        RPCHelper.registerInterface(UnitRegistry.class, this, server);
    }
    
    @Override
    public Future<UnitConfig> registerUnitConfig(UnitConfig unitConfig) throws CouldNotPerformException {
        return GlobalExecutionService.submit(() -> dalUnitRegistry.register(unitConfig));
    }
    
    @Override
    public UnitConfig getUnitConfigById(String unitConfigId) throws CouldNotPerformException {
        return dalUnitRegistry.get(unitConfigId).getMessage();
    }
    
    @Override
    public Boolean containsUnitConfigById(String sceneConfigId) throws CouldNotPerformException {
        return dalUnitRegistry.contains(sceneConfigId);
    }
    
    @Override
    public Boolean containsUnitConfig(UnitConfig unitConfig) throws CouldNotPerformException {
        return dalUnitRegistry.contains(unitConfig);
    }
    
    @Override
    public Future<UnitConfig> updateUnitConfig(UnitConfig unitConfig) throws CouldNotPerformException {
        return GlobalExecutionService.submit(() -> dalUnitRegistry.update(unitConfig));
    }
    
    @Override
    public Future<UnitConfig> removeUnitConfig(UnitConfig unitConfig) throws CouldNotPerformException {
        return GlobalExecutionService.submit(() -> dalUnitRegistry.remove(unitConfig));
    }
    
    @Override
    public List<UnitConfig> getUnitConfigs() throws CouldNotPerformException {
        return dalUnitRegistry.getMessages();
    }
    
    @Override
    public Boolean isUnitConfigRegistryReadOnly() throws CouldNotPerformException {
        return dalUnitRegistry.isReadOnly();
    }
    
    public ProtoBufFileSynchronizedRegistry<String, UnitConfig, UnitConfig.Builder, UnitRegistryData.Builder> getUnitConfigRegistry() {
        return dalUnitRegistry;
    }

    /**
     * {@inheritDoc}
     *
     * @return {@inheritDoc}
     * @throws CouldNotPerformException {@inheritDoc}
     */
    @Override
    public Boolean isUnitConfigRegistryConsistent() throws CouldNotPerformException {
        return dalUnitRegistry.isConsistent();
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
        return userRegistry;
    }
    
    public ProtoBufFileSynchronizedRegistry<String, AuthorizationGroupConfigType.AuthorizationGroupConfig, AuthorizationGroupConfigType.AuthorizationGroupConfig.Builder, UserRegistryDataType.UserRegistryData.Builder> getAuthorizationGroupRegistry() {
        return authorizationGroupRegistry;
    }
}
