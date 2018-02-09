package org.openbase.bco.registry.unit.core;

/*
 * #%L
 * BCO Registry Unit Core
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
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Future;
import org.openbase.bco.authentication.lib.jp.JPAuthentication;
import org.openbase.bco.authentication.lib.AuthenticatedServiceProcessor;
import org.openbase.bco.registry.agent.remote.CachedAgentRegistryRemote;
import org.openbase.bco.registry.app.remote.CachedAppRegistryRemote;
import org.openbase.bco.registry.device.remote.CachedDeviceRegistryRemote;
import org.openbase.bco.registry.device.remote.DeviceRegistryRemote;
import org.openbase.bco.registry.lib.com.AbstractRegistryController;
import org.openbase.bco.registry.unit.core.consistency.*;
import org.openbase.bco.registry.unit.core.consistency.agentconfig.AgentLabelConsistencyHandler;
import org.openbase.bco.registry.unit.core.consistency.agentconfig.AgentLocationConsistencyHandler;
import org.openbase.bco.registry.unit.core.consistency.agentconfig.AgentScopeConsistencyHandler;
import org.openbase.bco.registry.unit.core.consistency.appconfig.AppLabelConsistencyHandler;
import org.openbase.bco.registry.unit.core.consistency.appconfig.AppLocationConsistencyHandler;
import org.openbase.bco.registry.unit.core.consistency.appconfig.AppScopeConsistencyHandler;
import org.openbase.bco.registry.unit.core.consistency.authorizationgroup.AuthorizationGroupConfigLabelConsistencyHandler;
import org.openbase.bco.registry.unit.core.consistency.authorizationgroup.AuthorizationGroupConfigScopeConsistencyHandler;
import org.openbase.bco.registry.unit.core.consistency.authorizationgroup.AuthorizationGroupPermissionConsistencyHandler;
import org.openbase.bco.registry.unit.core.consistency.authorizationgroup.AuthorziationGroupDuplicateMemberConsistencyHandler;
import org.openbase.bco.registry.unit.core.consistency.connectionconfig.ConnectionLabelConsistencyHandler;
import org.openbase.bco.registry.unit.core.consistency.connectionconfig.ConnectionLocationConsistencyHandler;
import org.openbase.bco.registry.unit.core.consistency.connectionconfig.ConnectionScopeConsistencyHandler;
import org.openbase.bco.registry.unit.core.consistency.connectionconfig.ConnectionTilesConsistencyHandler;
import org.openbase.bco.registry.unit.core.consistency.dalunitconfig.DalUnitEnablingStateConsistencyHandler;
import org.openbase.bco.registry.unit.core.consistency.dalunitconfig.DalUnitHostIdConsistencyHandler;
import org.openbase.bco.registry.unit.core.consistency.dalunitconfig.DalUnitLabelConsistencyHandler;
import org.openbase.bco.registry.unit.core.consistency.dalunitconfig.DalUnitLocationIdConsistencyHandler;
import org.openbase.bco.registry.unit.core.consistency.dalunitconfig.DalUnitScopeConsistencyHandler;
import org.openbase.bco.registry.unit.core.consistency.dalunitconfig.OpenhabServiceConfigItemIdConsistencyHandler;
import org.openbase.bco.registry.unit.core.consistency.dalunitconfig.SyncBindingConfigDeviceClassUnitConsistencyHandler;
import org.openbase.bco.registry.unit.core.consistency.dalunitconfig.UnitBoundToHostConsistencyHandler;
import org.openbase.bco.registry.unit.core.consistency.dalunitconfig.UnitTransformationFrameConsistencyHandler;
import org.openbase.bco.registry.unit.core.consistency.deviceconfig.DeviceBoundToHostConsistencyHandler;
import org.openbase.bco.registry.unit.core.consistency.deviceconfig.DeviceConfigDeviceClassIdConsistencyHandler;
import org.openbase.bco.registry.unit.core.consistency.deviceconfig.DeviceConfigLocationIdForInstalledDevicesConsistencyHandler;
import org.openbase.bco.registry.unit.core.consistency.deviceconfig.DeviceEnablingStateConsistencyHandler;
import org.openbase.bco.registry.unit.core.consistency.deviceconfig.DeviceLabelConsistencyHandler;
import org.openbase.bco.registry.unit.core.consistency.deviceconfig.DeviceLocationIdConsistencyHandler;
import org.openbase.bco.registry.unit.core.consistency.deviceconfig.DeviceOwnerConsistencyHandler;
import org.openbase.bco.registry.unit.core.consistency.deviceconfig.DeviceScopeConsistencyHandler;
import org.openbase.bco.registry.unit.core.consistency.locationconfig.ChildWithSameLabelConsistencyHandler;
import org.openbase.bco.registry.unit.core.consistency.locationconfig.LocationChildConsistencyHandler;
import org.openbase.bco.registry.unit.core.consistency.locationconfig.LocationHierarchyConsistencyHandler;
import org.openbase.bco.registry.unit.core.consistency.locationconfig.LocationLoopConsistencyHandler;
import org.openbase.bco.registry.unit.core.consistency.locationconfig.LocationParentConsistencyHandler;
import org.openbase.bco.registry.unit.core.consistency.locationconfig.LocationPlacementConfigConsistencyHandler;
import org.openbase.bco.registry.unit.core.consistency.locationconfig.LocationPositionConsistencyHandler;
import org.openbase.bco.registry.unit.core.consistency.locationconfig.LocationScopeConsistencyHandler;
import org.openbase.bco.registry.unit.core.consistency.locationconfig.LocationTypeConsistencyHandler;
import org.openbase.bco.registry.unit.core.consistency.locationconfig.LocationUnitIdConsistencyHandler;
import org.openbase.bco.registry.unit.core.consistency.locationconfig.RootConsistencyHandler;
import org.openbase.bco.registry.unit.core.consistency.locationconfig.RootLocationExistencConsistencyHandler;
import org.openbase.bco.registry.unit.core.consistency.locationconfig.TileConnectionIdConsistencyHandler;
import org.openbase.bco.registry.unit.core.consistency.sceneconfig.SceneLabelConsistencyHandler;
import org.openbase.bco.registry.unit.core.consistency.sceneconfig.SceneScopeConsistencyHandler;
import org.openbase.bco.registry.unit.core.consistency.unitgroupconfig.UnitGroupPlacementConfigConsistencyHandler;
import org.openbase.bco.registry.unit.core.consistency.unitgroupconfig.UnitGroupMemberExistsConsistencyHandler;
import org.openbase.bco.registry.unit.core.consistency.unitgroupconfig.UnitGroupMemberListDuplicationConsistencyHandler;
import org.openbase.bco.registry.unit.core.consistency.unitgroupconfig.UnitGroupMemberListTypesConsistencyHandler;
import org.openbase.bco.registry.unit.core.consistency.unitgroupconfig.UnitGroupScopeConsistencyHandler;
import org.openbase.bco.registry.unit.core.consistency.unitgroupconfig.UnitGroupServiceDescriptionServiceTemplateIdConsistencyHandler;
import org.openbase.bco.registry.unit.core.consistency.unitgroupconfig.UnitGroupUnitTypeConsistencyHandler;
import org.openbase.bco.registry.unit.core.consistency.unittemplate.UniteTemplateServiceTemplateConsistencyHandler;
import org.openbase.bco.registry.unit.core.consistency.userconfig.UserConfigLabelConsistencyHandler;
import org.openbase.bco.registry.unit.core.consistency.userconfig.UserConfigScopeConsistencyHandler;
import org.openbase.bco.registry.unit.core.consistency.userconfig.UserConfigUserNameConsistencyHandler;
import org.openbase.bco.registry.unit.core.consistency.userconfig.UserPermissionConsistencyHandler;
import org.openbase.bco.registry.unit.core.plugin.*;
import org.openbase.bco.registry.unit.lib.UnitRegistry;
import org.openbase.bco.registry.unit.lib.generator.ServiceTemplateIdGenerator;
import org.openbase.bco.registry.unit.lib.generator.UnitConfigIdGenerator;
import org.openbase.bco.registry.unit.lib.generator.UnitTemplateIdGenerator;
import org.openbase.bco.registry.unit.lib.generator.UntShapeGenerator;
import org.openbase.bco.registry.unit.lib.jp.JPAgentConfigDatabaseDirectory;
import org.openbase.bco.registry.unit.lib.jp.JPAppConfigDatabaseDirectory;
import org.openbase.bco.registry.unit.lib.jp.JPAuthorizationGroupConfigDatabaseDirectory;
import org.openbase.bco.registry.unit.lib.jp.JPClearUnitPosition;
import org.openbase.bco.registry.unit.lib.jp.JPConnectionConfigDatabaseDirectory;
import org.openbase.bco.registry.unit.lib.jp.JPDalUnitConfigDatabaseDirectory;
import org.openbase.bco.registry.unit.lib.jp.JPDeviceConfigDatabaseDirectory;
import org.openbase.bco.registry.unit.lib.jp.JPLocationConfigDatabaseDirectory;
import org.openbase.bco.registry.unit.lib.jp.JPSceneConfigDatabaseDirectory;
import org.openbase.bco.registry.unit.lib.jp.JPServiceTemplateDatabaseDirectory;
import org.openbase.bco.registry.unit.lib.jp.JPUnitGroupConfigDatabaseDirectory;
import org.openbase.bco.registry.unit.lib.jp.JPUnitRegistryScope;
import org.openbase.bco.registry.unit.lib.jp.JPUnitTemplateDatabaseDirectory;
import org.openbase.bco.registry.unit.lib.jp.JPUserConfigDatabaseDirectory;
import org.openbase.jps.core.JPService;
import org.openbase.jps.exception.JPNotAvailableException;
import org.openbase.jps.exception.JPServiceException;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.FatalImplementationErrorException;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.exception.InvalidStateException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.exception.printer.LogLevel;
import org.openbase.jul.extension.rsb.com.RPCHelper;
import org.openbase.jul.extension.rsb.iface.RSBLocalServer;
import org.openbase.jul.iface.Manageable;
import org.openbase.jul.schedule.GlobalCachedExecutorService;
import org.openbase.jul.storage.file.ProtoBufJSonFileProvider;
import org.openbase.jul.storage.registry.ProtoBufFileSynchronizedRegistry;
import org.slf4j.LoggerFactory;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rst.domotic.authentication.AuthenticatedValueType.AuthenticatedValue;
import rst.domotic.authentication.PermissionConfigType.PermissionConfig;
import rst.domotic.authentication.PermissionType.Permission;
import rst.domotic.registry.UnitRegistryDataType.UnitRegistryData;
import rst.domotic.service.ServiceConfigType.ServiceConfig;
import rst.domotic.service.ServiceDescriptionType.ServiceDescription;
import rst.domotic.service.ServiceTemplateType.ServiceTemplate;
import rst.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import rst.domotic.unit.UnitConfigType.UnitConfig;
import rst.domotic.unit.UnitTemplateType.UnitTemplate;
import rst.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;
import rst.domotic.unit.agent.AgentConfigType.AgentConfig;
import rst.domotic.unit.app.AppConfigType.AppConfig;
import rst.domotic.unit.authorizationgroup.AuthorizationGroupConfigType.AuthorizationGroupConfig;
import rst.domotic.unit.connection.ConnectionConfigType.ConnectionConfig;
import rst.domotic.unit.device.DeviceConfigType.DeviceConfig;
import rst.domotic.unit.location.LocationConfigType.LocationConfig;
import rst.domotic.unit.scene.SceneConfigType.SceneConfig;
import rst.domotic.unit.unitgroup.UnitGroupConfigType.UnitGroupConfig;
import rst.domotic.unit.user.UserConfigType.UserConfig;
import rst.rsb.ScopeType;
import rst.spatial.ShapeType.Shape;

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
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(ServiceTemplate.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(AuthenticatedValue.getDefaultInstance()));
    }
    
    public final static UnitConfigIdGenerator UNIT_ID_GENERATOR = new UnitConfigIdGenerator();
    
    private final static org.slf4j.Logger LOGGER = LoggerFactory.getLogger(UnitRegistryController.class);
    
    private final ProtoBufFileSynchronizedRegistry<String, UnitTemplate, UnitTemplate.Builder, UnitRegistryData.Builder> unitTemplateRegistry;
    private final ProtoBufFileSynchronizedRegistry<String, ServiceTemplate, ServiceTemplate.Builder, UnitRegistryData.Builder> serviceTemplateRegistry;
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
    
    private final ArrayList<ProtoBufFileSynchronizedRegistry<String, UnitConfig, UnitConfig.Builder, UnitRegistryData.Builder>> unitConfigRegistryList, baseUnitConfigRegistryList;
    
    public UnitRegistryController() throws InstantiationException, InterruptedException {
        super(JPUnitRegistryScope.class, UnitRegistryData.newBuilder());
        try {
            this.unitConfigRegistryList = new ArrayList();
            this.baseUnitConfigRegistryList = new ArrayList();
            this.serviceTemplateRegistry = new ProtoBufFileSynchronizedRegistry<>(ServiceTemplate.class, getBuilderSetup(), getDataFieldDescriptor(UnitRegistryData.SERVICE_TEMPLATE_FIELD_NUMBER), new ServiceTemplateIdGenerator(), JPService.getProperty(JPServiceTemplateDatabaseDirectory.class).getValue(), protoBufJSonFileProvider);
            this.unitTemplateRegistry = new ProtoBufFileSynchronizedRegistry<>(UnitTemplate.class, getBuilderSetup(), getDataFieldDescriptor(UnitRegistryData.UNIT_TEMPLATE_FIELD_NUMBER), new UnitTemplateIdGenerator(), JPService.getProperty(JPUnitTemplateDatabaseDirectory.class).getValue(), protoBufJSonFileProvider);
            this.dalUnitConfigRegistry = new ProtoBufFileSynchronizedRegistry<>(UnitConfig.class, getBuilderSetup(), getDataFieldDescriptor(UnitRegistryData.DAL_UNIT_CONFIG_FIELD_NUMBER), UNIT_ID_GENERATOR, JPService.getProperty(JPDalUnitConfigDatabaseDirectory.class).getValue(), protoBufJSonFileProvider);
            this.userUnitConfigRegistry = new ProtoBufFileSynchronizedRegistry<>(UnitConfig.class, getBuilderSetup(), getDataFieldDescriptor(UnitRegistryData.USER_UNIT_CONFIG_FIELD_NUMBER), UNIT_ID_GENERATOR, JPService.getProperty(JPUserConfigDatabaseDirectory.class).getValue(), protoBufJSonFileProvider);
            this.authorizationGroupUnitConfigRegistry = new ProtoBufFileSynchronizedRegistry<>(UnitConfig.class, getBuilderSetup(), getDataFieldDescriptor(UnitRegistryData.AUTHORIZATION_GROUP_UNIT_CONFIG_FIELD_NUMBER), UNIT_ID_GENERATOR, JPService.getProperty(JPAuthorizationGroupConfigDatabaseDirectory.class).getValue(), protoBufJSonFileProvider);
            this.deviceUnitConfigRegistry = new ProtoBufFileSynchronizedRegistry<>(UnitConfig.class, getBuilderSetup(), getDataFieldDescriptor(UnitRegistryData.DEVICE_UNIT_CONFIG_FIELD_NUMBER), UNIT_ID_GENERATOR, JPService.getProperty(JPDeviceConfigDatabaseDirectory.class).getValue(), protoBufJSonFileProvider);
            this.unitGroupUnitConfigRegistry = new ProtoBufFileSynchronizedRegistry<>(UnitConfig.class, getBuilderSetup(), getDataFieldDescriptor(UnitRegistryData.UNIT_GROUP_UNIT_CONFIG_FIELD_NUMBER), UNIT_ID_GENERATOR, JPService.getProperty(JPUnitGroupConfigDatabaseDirectory.class).getValue(), protoBufJSonFileProvider);
            this.locationUnitConfigRegistry = new ProtoBufFileSynchronizedRegistry<>(UnitConfig.class, getBuilderSetup(), getDataFieldDescriptor(UnitRegistryData.LOCATION_UNIT_CONFIG_FIELD_NUMBER), UNIT_ID_GENERATOR, JPService.getProperty(JPLocationConfigDatabaseDirectory.class).getValue(), new ProtoBufJSonFileProvider());
            this.connectionUnitConfigRegistry = new ProtoBufFileSynchronizedRegistry<>(UnitConfig.class, getBuilderSetup(), getDataFieldDescriptor(UnitRegistryData.CONNECTION_UNIT_CONFIG_FIELD_NUMBER), UNIT_ID_GENERATOR, JPService.getProperty(JPConnectionConfigDatabaseDirectory.class).getValue(), new ProtoBufJSonFileProvider());
            this.agentUnitConfigRegistry = new ProtoBufFileSynchronizedRegistry<>(UnitConfig.class, getBuilderSetup(), getDataFieldDescriptor(UnitRegistryData.AGENT_UNIT_CONFIG_FIELD_NUMBER), UNIT_ID_GENERATOR, JPService.getProperty(JPAgentConfigDatabaseDirectory.class).getValue(), protoBufJSonFileProvider);
            this.sceneUnitConfigRegistry = new ProtoBufFileSynchronizedRegistry<>(UnitConfig.class, getBuilderSetup(), getDataFieldDescriptor(UnitRegistryData.SCENE_UNIT_CONFIG_FIELD_NUMBER), UNIT_ID_GENERATOR, JPService.getProperty(JPSceneConfigDatabaseDirectory.class).getValue(), protoBufJSonFileProvider);
            this.appUnitConfigRegistry = new ProtoBufFileSynchronizedRegistry<>(UnitConfig.class, getBuilderSetup(), getDataFieldDescriptor(UnitRegistryData.APP_UNIT_CONFIG_FIELD_NUMBER), UNIT_ID_GENERATOR, JPService.getProperty(JPAppConfigDatabaseDirectory.class).getValue(), protoBufJSonFileProvider);
            this.unitConfigRegistryList.add(dalUnitConfigRegistry);
            this.unitConfigRegistryList.add(locationUnitConfigRegistry);
            this.unitConfigRegistryList.add(authorizationGroupUnitConfigRegistry);
            this.unitConfigRegistryList.add(userUnitConfigRegistry);
            this.unitConfigRegistryList.add(deviceUnitConfigRegistry);
            this.unitConfigRegistryList.add(unitGroupUnitConfigRegistry);
            this.unitConfigRegistryList.add(connectionUnitConfigRegistry);
            this.unitConfigRegistryList.add(sceneUnitConfigRegistry);
            this.unitConfigRegistryList.add(agentUnitConfigRegistry);
            this.unitConfigRegistryList.add(appUnitConfigRegistry);
            this.baseUnitConfigRegistryList.add(userUnitConfigRegistry);
            this.baseUnitConfigRegistryList.add(authorizationGroupUnitConfigRegistry);
            this.baseUnitConfigRegistryList.add(deviceUnitConfigRegistry);
            this.baseUnitConfigRegistryList.add(unitGroupUnitConfigRegistry);
            this.baseUnitConfigRegistryList.add(locationUnitConfigRegistry);
            this.baseUnitConfigRegistryList.add(connectionUnitConfigRegistry);
            this.baseUnitConfigRegistryList.add(sceneUnitConfigRegistry);
            this.baseUnitConfigRegistryList.add(agentUnitConfigRegistry);
            this.baseUnitConfigRegistryList.add(appUnitConfigRegistry);
        } catch (JPServiceException | NullPointerException ex) {
            throw new InstantiationException(this, ex);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @throws CouldNotPerformException {@inheritDoc}
     */
    @Override
    protected void registerRegistries() throws CouldNotPerformException {
        registerRegistry(serviceTemplateRegistry);
        registerRegistry(unitTemplateRegistry);
        unitConfigRegistryList.stream().forEach((registry) -> {
            registerRegistry(registry);
        });
    }

    /**
     * {@inheritDoc}
     *
     * @throws CouldNotPerformException {@inheritDoc}
     */
    @Override
    protected void registerConsistencyHandler() throws CouldNotPerformException {
        try {
            unitTemplateRegistry.registerConsistencyHandler(new UniteTemplateServiceTemplateConsistencyHandler(serviceTemplateRegistry));

            //TODO: should be activated but fails in the current db version since agentClasses have just been introduced
            //agentUnitConfigRegistry.registerConsistencyHandler(new AgentConfigAgentClassIdConsistencyHandler(agentClassRegistry));
            agentUnitConfigRegistry.registerConsistencyHandler(new AgentLabelConsistencyHandler());
            agentUnitConfigRegistry.registerConsistencyHandler(new AgentLocationConsistencyHandler(locationUnitConfigRegistry));
            agentUnitConfigRegistry.registerConsistencyHandler(new AgentScopeConsistencyHandler(locationUnitConfigRegistry, CachedAgentRegistryRemote.getRegistry().getAgentClassRemoteRegistry()));
            agentUnitConfigRegistry.registerConsistencyHandler(new ExecutableUnitAutostartConsistencyHandler());

            //TODO: should be activated but fails in the current db version since appClasses have just been introduced
            //appConfigRegistry.registerConsistencyHandler(new AppConfigAppClassIdConsistencyHandler(appClassRegistry));
            appUnitConfigRegistry.registerConsistencyHandler(new AppLabelConsistencyHandler());
            appUnitConfigRegistry.registerConsistencyHandler(new AppLocationConsistencyHandler(locationUnitConfigRegistry));
            appUnitConfigRegistry.registerConsistencyHandler(new AppScopeConsistencyHandler(locationUnitConfigRegistry, CachedAppRegistryRemote.getRegistry().getAppClassRemoteRegistry()));
            appUnitConfigRegistry.registerConsistencyHandler(new ExecutableUnitAutostartConsistencyHandler());
            
            authorizationGroupUnitConfigRegistry.registerConsistencyHandler(new AuthorizationGroupConfigLabelConsistencyHandler());
            authorizationGroupUnitConfigRegistry.registerConsistencyHandler(new AuthorizationGroupConfigScopeConsistencyHandler());
            authorizationGroupUnitConfigRegistry.registerConsistencyHandler(new AuthorziationGroupDuplicateMemberConsistencyHandler());
            authorizationGroupUnitConfigRegistry.registerConsistencyHandler(new AuthorizationGroupPermissionConsistencyHandler());

            connectionUnitConfigRegistry.registerConsistencyHandler(new ConnectionLabelConsistencyHandler());
            connectionUnitConfigRegistry.registerConsistencyHandler(new ConnectionTilesConsistencyHandler(locationUnitConfigRegistry));
            connectionUnitConfigRegistry.registerConsistencyHandler(new ConnectionLocationConsistencyHandler(locationUnitConfigRegistry));
            connectionUnitConfigRegistry.registerConsistencyHandler(new ConnectionScopeConsistencyHandler(locationUnitConfigRegistry));
            
            dalUnitConfigRegistry.registerConsistencyHandler(new DalUnitEnablingStateConsistencyHandler(deviceUnitConfigRegistry));
            dalUnitConfigRegistry.registerConsistencyHandler(new DalUnitHostIdConsistencyHandler(deviceUnitConfigRegistry));
            dalUnitConfigRegistry.registerConsistencyHandler(new DalUnitLabelConsistencyHandler(CachedDeviceRegistryRemote.getRegistry().getDeviceClassRemoteRegistry(), deviceUnitConfigRegistry));
            dalUnitConfigRegistry.registerConsistencyHandler(new DalUnitLocationIdConsistencyHandler(locationUnitConfigRegistry, deviceUnitConfigRegistry));
            dalUnitConfigRegistry.registerConsistencyHandler(new DalUnitScopeConsistencyHandler(locationUnitConfigRegistry));
            dalUnitConfigRegistry.registerConsistencyHandler(new SyncBindingConfigDeviceClassUnitConsistencyHandler(CachedDeviceRegistryRemote.getRegistry().getDeviceClassRemoteRegistry(), deviceUnitConfigRegistry));
            dalUnitConfigRegistry.registerConsistencyHandler(new OpenhabServiceConfigItemIdConsistencyHandler(CachedDeviceRegistryRemote.getRegistry().getDeviceClassRemoteRegistry(), locationUnitConfigRegistry, deviceUnitConfigRegistry));
            dalUnitConfigRegistry.registerConsistencyHandler(new UnitBoundToHostConsistencyHandler(CachedDeviceRegistryRemote.getRegistry().getDeviceClassRemoteRegistry(), deviceUnitConfigRegistry));
            
            deviceUnitConfigRegistry.registerConsistencyHandler(new DeviceBoundToHostConsistencyHandler(CachedDeviceRegistryRemote.getRegistry().getDeviceClassRemoteRegistry()));
            deviceUnitConfigRegistry.registerConsistencyHandler(new DeviceConfigDeviceClassIdConsistencyHandler(CachedDeviceRegistryRemote.getRegistry().getDeviceClassRemoteRegistry()));
            deviceUnitConfigRegistry.registerConsistencyHandler(new DeviceConfigLocationIdForInstalledDevicesConsistencyHandler());
            deviceUnitConfigRegistry.registerConsistencyHandler(new DeviceInventoryStateConsistencyHandler());
            deviceUnitConfigRegistry.registerConsistencyHandler(new DeviceEnablingStateConsistencyHandler());
            deviceUnitConfigRegistry.registerConsistencyHandler(new DeviceLabelConsistencyHandler());
            deviceUnitConfigRegistry.registerConsistencyHandler(new DeviceLocationIdConsistencyHandler(locationUnitConfigRegistry));
            deviceUnitConfigRegistry.registerConsistencyHandler(new DeviceOwnerConsistencyHandler(userUnitConfigRegistry));
            deviceUnitConfigRegistry.registerConsistencyHandler(new DeviceScopeConsistencyHandler(locationUnitConfigRegistry));
            
            userUnitConfigRegistry.registerConsistencyHandler(new UserConfigScopeConsistencyHandler());
            userUnitConfigRegistry.registerConsistencyHandler(new UserConfigUserNameConsistencyHandler());
            userUnitConfigRegistry.registerConsistencyHandler(new UserConfigLabelConsistencyHandler());
            userUnitConfigRegistry.registerConsistencyHandler(new UserPermissionConsistencyHandler());

            unitGroupUnitConfigRegistry.registerConsistencyHandler(new UnitGroupMemberListDuplicationConsistencyHandler());
            unitGroupUnitConfigRegistry.registerConsistencyHandler(new UnitGroupMemberExistsConsistencyHandler(agentUnitConfigRegistry, appUnitConfigRegistry, authorizationGroupUnitConfigRegistry, connectionUnitConfigRegistry, dalUnitConfigRegistry, deviceUnitConfigRegistry, locationUnitConfigRegistry, sceneUnitConfigRegistry, unitGroupUnitConfigRegistry, userUnitConfigRegistry));
            unitGroupUnitConfigRegistry.registerConsistencyHandler(new UnitGroupServiceDescriptionServiceTemplateIdConsistencyHandler(serviceTemplateRegistry));
            unitGroupUnitConfigRegistry.registerConsistencyHandler(new UnitGroupUnitTypeConsistencyHandler(unitTemplateRegistry));
            unitGroupUnitConfigRegistry.registerConsistencyHandler(new UnitGroupMemberListTypesConsistencyHandler(agentUnitConfigRegistry, appUnitConfigRegistry, authorizationGroupUnitConfigRegistry, connectionUnitConfigRegistry, dalUnitConfigRegistry, deviceUnitConfigRegistry, locationUnitConfigRegistry, sceneUnitConfigRegistry, unitGroupUnitConfigRegistry, userUnitConfigRegistry, unitTemplateRegistry));
            unitGroupUnitConfigRegistry.registerConsistencyHandler(new UnitGroupScopeConsistencyHandler(locationUnitConfigRegistry));
            unitGroupUnitConfigRegistry.registerConsistencyHandler(new UnitGroupPlacementConfigConsistencyHandler(unitConfigRegistryList, locationUnitConfigRegistry, CachedDeviceRegistryRemote.getRegistry().getDeviceClassRemoteRegistry()));
            
            locationUnitConfigRegistry.registerConsistencyHandler(new LocationPlacementConfigConsistencyHandler());
            locationUnitConfigRegistry.registerConsistencyHandler(new LocationPositionConsistencyHandler());
            locationUnitConfigRegistry.registerConsistencyHandler(new RootConsistencyHandler());
            locationUnitConfigRegistry.registerConsistencyHandler(new LocationChildConsistencyHandler());
            locationUnitConfigRegistry.registerConsistencyHandler(new LocationParentConsistencyHandler());
            locationUnitConfigRegistry.registerConsistencyHandler(new RootLocationExistencConsistencyHandler());
            locationUnitConfigRegistry.registerConsistencyHandler(new LocationLoopConsistencyHandler());
            locationUnitConfigRegistry.registerConsistencyHandler(new ChildWithSameLabelConsistencyHandler());
            locationUnitConfigRegistry.registerConsistencyHandler(new LocationScopeConsistencyHandler());
            locationUnitConfigRegistry.registerConsistencyHandler(new LocationUnitIdConsistencyHandler(agentUnitConfigRegistry, appUnitConfigRegistry, authorizationGroupUnitConfigRegistry, connectionUnitConfigRegistry, dalUnitConfigRegistry, deviceUnitConfigRegistry, sceneUnitConfigRegistry, unitGroupUnitConfigRegistry, userUnitConfigRegistry));
            locationUnitConfigRegistry.registerConsistencyHandler(new LocationTypeConsistencyHandler());
            locationUnitConfigRegistry.registerConsistencyHandler(new LocationHierarchyConsistencyHandler());
            locationUnitConfigRegistry.registerConsistencyHandler(new LocationShapeConsistencyHandler());
            locationUnitConfigRegistry.registerConsistencyHandler(new TileConnectionIdConsistencyHandler(connectionUnitConfigRegistry));
            
            sceneUnitConfigRegistry.registerConsistencyHandler(new SceneLabelConsistencyHandler());
            sceneUnitConfigRegistry.registerConsistencyHandler(new SceneScopeConsistencyHandler(locationUnitConfigRegistry));

            // add consistency handler for all unitConfig registries
            registerConsistencyHandler(new UnitLocationIdConsistencyHandler(locationUnitConfigRegistry), UnitConfig.class);
            registerConsistencyHandler(new ServiceConfigUnitIdConsistencyHandler(), UnitConfig.class);
            registerConsistencyHandler(new UnitConfigUnitTemplateConsistencyHandler(unitTemplateRegistry), UnitConfig.class);
            registerConsistencyHandler(new UnitAliasUniqueVerificationConsistencyHandler(this), UnitConfig.class);
            registerConsistencyHandler(new UnitAliasGenerationConsistencyHandler(this), UnitConfig.class);
            registerConsistencyHandler(new UnitEnablingStateConsistencyHandler(), UnitConfig.class);
            registerConsistencyHandler(new ServiceConfigServiceTemplateIdConsistencyHandler(serviceTemplateRegistry), UnitConfig.class);
            registerConsistencyHandler(new BoundingBoxCleanerConsistencyHandler(), UnitConfig.class);
            registerConsistencyHandler(new UnitTransformationFrameConsistencyHandler(locationUnitConfigRegistry), UnitConfig.class);
            registerConsistencyHandler(new UnitPermissionCleanerConsistencyHandler(authorizationGroupUnitConfigRegistry, locationUnitConfigRegistry), UnitConfig.class);

            try {
                if (JPService.getProperty(JPAuthentication.class).getValue()) {
                    registerConsistencyHandler(new OtherPermissionConsistencyHandler(), UnitConfig.class);
                    registerConsistencyHandler(new GroupPermissionConsistencyHandler(authorizationGroupUnitConfigRegistry), UnitConfig.class);
                }
                if (JPService.getProperty(JPClearUnitPosition.class).getValue()) {
                    registerConsistencyHandler(new UnitPositionCleanerConsistencyHandler(), UnitConfig.class);
                }
            } catch (JPNotAvailableException ex) {
                throw new CouldNotPerformException("JPProperty not available", ex);
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            // registries do not throw interrupted exception within the next release.
        }
    }

    /**
     * {@inheritDoc}
     *
     * @throws CouldNotPerformException {@inheritDoc}
     * @throws InterruptedException {@inheritDoc}
     */
    @Override
    protected void registerPlugins() throws CouldNotPerformException, InterruptedException {
        serviceTemplateRegistry.registerPlugin(new ServiceTemplateCreatorRegistryPlugin(serviceTemplateRegistry));
        unitTemplateRegistry.registerPlugin(new UnitTemplateCreatorRegistryPlugin(unitTemplateRegistry));
        locationUnitConfigRegistry.registerPlugin(new RootLocationPlugin());
        try {
            if (JPService.getProperty(JPAuthentication.class).getValue()) {
                authorizationGroupUnitConfigRegistry.registerPlugin(new AuthorizationGroupCreationPlugin(authorizationGroupUnitConfigRegistry));
                userUnitConfigRegistry.registerPlugin(new UserCreationPlugin(userUnitConfigRegistry, authorizationGroupUnitConfigRegistry));
            }
        } catch (JPNotAvailableException ex) {
            ExceptionPrinter.printHistory("Could not load " + JPAuthentication.class.getSimpleName(), ex, LOGGER, LogLevel.WARN);
        }
        
        deviceUnitConfigRegistry.registerPlugin(new DeviceConfigDeviceClassUnitConsistencyPlugin(CachedDeviceRegistryRemote.getRegistry().getDeviceClassRemoteRegistry(), dalUnitConfigRegistry, deviceUnitConfigRegistry));
        
        dalUnitConfigRegistry.registerPlugin(new DalUnitBoundToHostPlugin(deviceUnitConfigRegistry));
        locationUnitConfigRegistry.registerPlugin(new LocationRemovalPlugin(unitConfigRegistryList, locationUnitConfigRegistry, connectionUnitConfigRegistry));
        // register transformation publisher plugins.
        locationUnitConfigRegistry.registerPlugin(new PublishLocationTransformationRegistryPlugin());
        connectionUnitConfigRegistry.registerPlugin(new PublishUnitTransformationRegistryPlugin(locationUnitConfigRegistry));
        dalUnitConfigRegistry.registerPlugin(new PublishUnitTransformationRegistryPlugin(locationUnitConfigRegistry));
        deviceUnitConfigRegistry.registerPlugin(new PublishUnitTransformationRegistryPlugin(locationUnitConfigRegistry));
        userUnitConfigRegistry.registerPlugin(new PublishUnitTransformationRegistryPlugin(locationUnitConfigRegistry));
        sceneUnitConfigRegistry.registerPlugin(new PublishUnitTransformationRegistryPlugin(locationUnitConfigRegistry));
        appUnitConfigRegistry.registerPlugin(new PublishUnitTransformationRegistryPlugin(locationUnitConfigRegistry));
        agentUnitConfigRegistry.registerPlugin(new PublishUnitTransformationRegistryPlugin(locationUnitConfigRegistry));
        authorizationGroupUnitConfigRegistry.registerPlugin(new PublishUnitTransformationRegistryPlugin(locationUnitConfigRegistry));
        unitGroupUnitConfigRegistry.registerPlugin(new PublishUnitTransformationRegistryPlugin(locationUnitConfigRegistry));
    }

    /**
     * {@inheritDoc}
     *
     * @throws CouldNotPerformException {@inheritDoc}
     */
    @Override
    protected void registerDependencies() throws CouldNotPerformException {
        try {
            unitTemplateRegistry.registerDependency(serviceTemplateRegistry);
            
            registerDependency(unitTemplateRegistry, UnitConfig.class);
            
            dalUnitConfigRegistry.registerDependency(deviceUnitConfigRegistry);
            dalUnitConfigRegistry.registerDependency(locationUnitConfigRegistry);
            
            authorizationGroupUnitConfigRegistry.registerDependency(userUnitConfigRegistry);
            
            deviceUnitConfigRegistry.registerDependency(locationUnitConfigRegistry);
            deviceUnitConfigRegistry.registerDependency(userUnitConfigRegistry);
            deviceUnitConfigRegistry.registerDependency(CachedDeviceRegistryRemote.getRegistry().getDeviceClassRemoteRegistry());
            
            unitGroupUnitConfigRegistry.registerDependency(agentUnitConfigRegistry);
            unitGroupUnitConfigRegistry.registerDependency(appUnitConfigRegistry);
            unitGroupUnitConfigRegistry.registerDependency(authorizationGroupUnitConfigRegistry);
            unitGroupUnitConfigRegistry.registerDependency(connectionUnitConfigRegistry);
            unitGroupUnitConfigRegistry.registerDependency(dalUnitConfigRegistry);
            unitGroupUnitConfigRegistry.registerDependency(deviceUnitConfigRegistry);
            unitGroupUnitConfigRegistry.registerDependency(locationUnitConfigRegistry);
            unitGroupUnitConfigRegistry.registerDependency(sceneUnitConfigRegistry);
            unitGroupUnitConfigRegistry.registerDependency(userUnitConfigRegistry);
            unitGroupUnitConfigRegistry.registerDependency(CachedDeviceRegistryRemote.getRegistry().getDeviceClassRemoteRegistry());
            
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
            agentUnitConfigRegistry.registerDependency(CachedAgentRegistryRemote.getRegistry().getAgentClassRemoteRegistry());
            
            sceneUnitConfigRegistry.registerDependency(locationUnitConfigRegistry);
            
            appUnitConfigRegistry.registerDependency(CachedAppRegistryRemote.getRegistry().getAppClassRemoteRegistry());
            appUnitConfigRegistry.registerDependency(locationUnitConfigRegistry);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            // registries do not throw interrupted exception within the next release.
        }
    }
    
    @Override
    public final void syncRegistryFlags() throws CouldNotPerformException, InterruptedException {
        setDataField(UnitRegistryData.DAL_UNIT_CONFIG_REGISTRY_READ_ONLY_FIELD_NUMBER, dalUnitConfigRegistry.isReadOnly());
        setDataField(UnitRegistryData.DAL_UNIT_CONFIG_REGISTRY_CONSISTENT_FIELD_NUMBER, dalUnitConfigRegistry.isConsistent());
        
        setDataField(UnitRegistryData.UNIT_TEMPLATE_REGISTRY_READ_ONLY_FIELD_NUMBER, unitTemplateRegistry.isReadOnly());
        setDataField(UnitRegistryData.UNIT_TEMPLATE_REGISTRY_CONSISTENT_FIELD_NUMBER, unitTemplateRegistry.isConsistent());
        
        setDataField(UnitRegistryData.SERVICE_TEMPLATE_REGISTRY_READ_ONLY_FIELD_NUMBER, unitTemplateRegistry.isReadOnly());
        setDataField(UnitRegistryData.SERVICE_TEMPLATE_REGISTRY_CONSISTENT_FIELD_NUMBER, unitTemplateRegistry.isConsistent());
        
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
        setDataField(UnitRegistryData.APP_UNIT_CONFIG_REGISTRY_CONSISTENT_FIELD_NUMBER, appUnitConfigRegistry.isConsistent());
        
        setDataField(UnitRegistryData.UNIT_CONFIG_REGISTRY_READ_ONLY_FIELD_NUMBER, isUnitConfigRegistryReadOnly());
        setDataField(UnitRegistryData.UNIT_CONFIG_REGISTRY_CONSISTENT_FIELD_NUMBER, isUnitConfigRegistryConsistent());
    }
    
    @Override
    public void registerMethods(final RSBLocalServer server) throws CouldNotPerformException {
        super.registerMethods(server);
        RPCHelper.registerInterface(UnitRegistry.class, this, server);
    }
    
    private ProtoBufFileSynchronizedRegistry<String, UnitConfig, UnitConfig.Builder, UnitRegistryData.Builder> getUnitConfigRegistry(final UnitType unitType) {
        switch (unitType) {
            case AUTHORIZATION_GROUP:
                return authorizationGroupUnitConfigRegistry;
            case AGENT:
                return agentUnitConfigRegistry;
            case APP:
                return appUnitConfigRegistry;
            case CONNECTION:
                return connectionUnitConfigRegistry;
            case DEVICE:
                return deviceUnitConfigRegistry;
            case LOCATION:
                return locationUnitConfigRegistry;
            case SCENE:
                return sceneUnitConfigRegistry;
            case UNIT_GROUP:
                return unitGroupUnitConfigRegistry;
            case USER:
                return userUnitConfigRegistry;
            default:
                return dalUnitConfigRegistry;
        }
    }
    
    @Override
    public Future<UnitConfig> registerUnitConfig(final UnitConfig unitConfig) throws CouldNotPerformException {
        return GlobalCachedExecutorService.submit(() -> getUnitConfigRegistry(unitConfig.getType()).register(unitConfig));
    }
    
    @Override
    public Future<AuthenticatedValue> registerUnitConfigAuthenticated(final AuthenticatedValue authenticatedValue) throws CouldNotPerformException {
        return GlobalCachedExecutorService.submit(() -> {
            return AuthenticatedServiceProcessor.authenticatedAction(authenticatedValue, getAuthorizationGroupUnitConfigRegistry().getEntryMap(), getLocationUnitConfigRegistry().getEntryMap(), UnitConfig.class,
                    (UnitConfig unitConfig) -> getUnitConfigRegistry(unitConfig.getType()).register(unitConfig),
                    (UnitConfig unitConfig) -> {
                        // If the unit has a location, use the location's UnitConfig for the permissions.
                        if (unitConfig.hasPlacementConfig() && unitConfig.getPlacementConfig().hasLocationId()) {
                            return getLocationUnitConfigRegistry().getMessage(unitConfig.getPlacementConfig().getLocationId());
                        }

                        // Else, user the permission for the root location
                        UnitConfig rootLocation = null;
                        for (UnitConfig locationUnitConfig : getLocationUnitConfigRegistry().getMessages()) {
                            if (locationUnitConfig.getLocationConfig().getRoot()) {
                                rootLocation = locationUnitConfig;
                                break;
                            }
                        }
                        if (rootLocation == null) {
                            // no root location yet available so use all rights
                            PermissionConfig.Builder permissionConfig = PermissionConfig.newBuilder();
                            permissionConfig.setOtherPermission(Permission.newBuilder().setAccess(true).setRead(true).setWrite(true));
                            rootLocation = UnitConfig.newBuilder().setPermissionConfig(permissionConfig).build();
                        }
                        return rootLocation;
                    }
            );
        });
    }
    
    @Override
    public UnitConfig getUnitConfigById(final String unitConfigId) throws CouldNotPerformException {
        for (ProtoBufFileSynchronizedRegistry registry : getRegistries()) {
            try {
                return (UnitConfig) registry.getMessage(unitConfigId);
            } catch (CouldNotPerformException ex) {
                // ignore and throw a new exception if no registry contains the entry
            }
        }
        throw new CouldNotPerformException("None of the unit registries contains an entry with the id [" + unitConfigId + "]");
    }
    
    @Override
    public Boolean containsUnitConfigById(final String unitConfigId) throws CouldNotPerformException {
        for (ProtoBufFileSynchronizedRegistry registry : getRegistries()) {
            try {
                return registry.contains(unitConfigId);
            } catch (CouldNotPerformException ex) {
                // ignore and throw a new exception if no registry contains the entry
            }
        }
        throw new CouldNotPerformException("None of the unit registries contains an entry with the id [" + unitConfigId + "]");
    }
    
    @Override
    public Boolean containsUnitConfig(final UnitConfig unitConfig) throws CouldNotPerformException {
        return getUnitConfigRegistry(unitConfig.getType()).contains(unitConfig);
    }
    
    @Override
    public Future<UnitConfig> updateUnitConfig(final UnitConfig unitConfig) throws CouldNotPerformException {
        return GlobalCachedExecutorService.submit(() -> getUnitConfigRegistry(unitConfig.getType()).update(unitConfig));
    }
    
    @Override
    public Future<AuthenticatedValue> updateUnitConfigAuthenticated(final AuthenticatedValue authenticatedValue) throws CouldNotPerformException {
        return GlobalCachedExecutorService.submit(() -> {
            return AuthenticatedServiceProcessor.authenticatedAction(authenticatedValue, getAuthorizationGroupUnitConfigRegistry().getEntryMap(), getLocationUnitConfigRegistry().getEntryMap(), UnitConfig.class,
                    (UnitConfig unitConfig) -> getUnitConfigRegistry(unitConfig.getType()).update(unitConfig),
                    (UnitConfig unitConfig) -> getUnitConfigById(unitConfig.getId())
            );
        });
    }
    
    @Override
    public Future<UnitConfig> removeUnitConfig(final UnitConfig unitConfig) throws CouldNotPerformException {
        return GlobalCachedExecutorService.submit(() -> getUnitConfigRegistry(unitConfig.getType()).remove(unitConfig));
    }
    
    @Override
    public Future<AuthenticatedValue> removeUnitConfigAuthenticated(final AuthenticatedValue authenticatedValue) throws CouldNotPerformException {
        return GlobalCachedExecutorService.submit(() -> {
            return AuthenticatedServiceProcessor.authenticatedAction(authenticatedValue, getAuthorizationGroupUnitConfigRegistry().getEntryMap(), getLocationUnitConfigRegistry().getEntryMap(), UnitConfig.class,
                    (UnitConfig unitConfig) -> getUnitConfigRegistry(unitConfig.getType()).remove(unitConfig),
                    (UnitConfig unitConfig) -> getUnitConfigById(unitConfig.getId())
            );
        });
    }
    
    @Override
    public List<UnitConfig> getUnitConfigs() throws CouldNotPerformException {
        ArrayList<UnitConfig> unitConfigList = new ArrayList<>();
        for (final ProtoBufFileSynchronizedRegistry<String, UnitConfig, UnitConfig.Builder, UnitRegistryData.Builder> unitConfigRegistry : unitConfigRegistryList) {
            unitConfigList.addAll(unitConfigRegistry.getMessages());
        }
        return unitConfigList;
    }

    /**
     * {@inheritDoc}
     *
     * @return {@inheritDoc}
     * @throws CouldNotPerformException {@inheritDoc}
     */
    @Override
    public List<UnitConfig> getDalUnitConfigs() throws CouldNotPerformException {
        return dalUnitConfigRegistry.getMessages();
    }

    /**
     * {@inheritDoc}
     *
     * @return {@inheritDoc}
     * @throws CouldNotPerformException {@inheritDoc}
     */
    @Override
    public List<UnitConfig> getBaseUnitConfigs() throws CouldNotPerformException {
        ArrayList<UnitConfig> unitConfigList = new ArrayList<>();
        for (final ProtoBufFileSynchronizedRegistry<String, UnitConfig, UnitConfig.Builder, UnitRegistryData.Builder> unitConfigRegistry : baseUnitConfigRegistryList) {
            unitConfigList.addAll(unitConfigRegistry.getMessages());
        }
        return unitConfigList;
    }
    
    @Override
    public Boolean isUnitConfigRegistryReadOnly() throws CouldNotPerformException {
        return unitConfigRegistryList.stream().anyMatch((unitConfigRegistry) -> (unitConfigRegistry.isReadOnly()));
    }

    /**
     * {@inheritDoc}
     *
     * @return {@inheritDoc}
     * @throws CouldNotPerformException {@inheritDoc}
     */
    @Override
    public Boolean isUnitConfigRegistryConsistent() throws CouldNotPerformException {
        return unitConfigRegistryList.stream().anyMatch((unitConfigRegistry) -> (unitConfigRegistry.isConsistent()));
    }

    /**
     * {@inheritDoc}
     *
     * @param unitTemplateId {@inheritDoc}
     * @return {@inheritDoc}
     * @throws CouldNotPerformException {@inheritDoc}
     */
    @Override
    public UnitTemplate getUnitTemplateById(final String unitTemplateId) throws CouldNotPerformException {
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
    public Boolean containsUnitTemplateById(final String unitTemplateId) throws CouldNotPerformException {
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
    public Boolean containsUnitTemplate(final UnitTemplate unitTemplate) throws CouldNotPerformException {
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
    public Future<UnitTemplate> updateUnitTemplate(final UnitTemplate unitTemplate) throws CouldNotPerformException {
        return GlobalCachedExecutorService.submit(() -> unitTemplateRegistry.update(unitTemplate));
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
     * @param unitConfigLabel {@inheritDoc}
     * @return {@inheritDoc}
     * @throws CouldNotPerformException {@inheritDoc}
     * @throws NotAvailableException {@inheritDoc}
     */
    @Override
    public List<UnitConfig> getUnitConfigsByLabel(final String unitConfigLabel) throws CouldNotPerformException, NotAvailableException {
        List<UnitConfig> unitConfigs = Collections.synchronizedList(new ArrayList<>());
        getUnitConfigs().parallelStream().filter((unitConfig) -> (unitConfig.getLabel().equalsIgnoreCase(unitConfigLabel))).forEach((unitConfig) -> {
            unitConfigs.add(unitConfig);
        });
        return unitConfigs;
    }

    /**
     * {@inheritDoc}
     *
     * @return
     * @throws CouldNotPerformException
     */
    @Override
    public List<ServiceConfig> getServiceConfigs() throws CouldNotPerformException {
        List<ServiceConfig> serviceConfigs = new ArrayList<>();
        for (UnitConfig unitConfig : getUnitConfigs()) {
            serviceConfigs.addAll(unitConfig.getServiceConfigList());
        }
        return serviceConfigs;
    }

    /**
     * {@inheritDoc}
     *
     * @return
     * @throws CouldNotPerformException
     */
    @Override
    public Boolean isUnitGroupConfigRegistryReadOnly() throws CouldNotPerformException {
        return unitGroupUnitConfigRegistry.isReadOnly();
    }

    /**
     * {@inheritDoc}
     *
     * @return
     * @throws CouldNotPerformException
     */
    @Override
    public Boolean isUnitGroupConfigRegistryConsistent() throws CouldNotPerformException {
        return unitGroupUnitConfigRegistry.isConsistent();
    }

    /**
     * {@inheritDoc}
     *
     * @param serviceType
     * @return
     * @throws CouldNotPerformException
     */
    @Override
    public List<ServiceConfig> getServiceConfigs(final ServiceType serviceType) throws CouldNotPerformException {
        List<ServiceConfig> serviceConfigs = new ArrayList<>();
        for (UnitConfig unitConfig : getUnitConfigs()) {
            for (ServiceConfig serviceConfig : unitConfig.getServiceConfigList()) {
                if (serviceConfig.getServiceDescription().getType() == serviceType) {
                    serviceConfigs.add(serviceConfig);
                }
            }
        }
        return serviceConfigs;
    }

    /**
     * {@inheritDoc}
     *
     * @param groupConfig
     * @return
     * @throws CouldNotPerformException
     */
    @Override
    public Future<UnitConfig> registerUnitGroupConfig(final UnitConfig groupConfig) throws CouldNotPerformException {
        verifyUnitGroupUnitConfig(groupConfig);
        return GlobalCachedExecutorService.submit(() -> unitGroupUnitConfigRegistry.register(groupConfig));
    }

    /**
     * {@inheritDoc}
     *
     * @param groupConfig
     * @return
     * @throws CouldNotPerformException
     */
    @Override
    public Boolean containsUnitGroupConfig(final UnitConfig groupConfig) throws CouldNotPerformException {
        return unitGroupUnitConfigRegistry.contains(groupConfig);
    }

    /**
     * {@inheritDoc}
     *
     * @param groupConfigId
     * @return
     * @throws CouldNotPerformException
     */
    @Override
    public Boolean containsUnitGroupConfigById(final String groupConfigId) throws CouldNotPerformException {
        return unitGroupUnitConfigRegistry.contains(groupConfigId);
    }

    /**
     * {@inheritDoc}
     *
     * @param groupConfig
     * @return
     * @throws CouldNotPerformException
     */
    @Override
    public Future<UnitConfig> updateUnitGroupConfig(final UnitConfig groupConfig) throws CouldNotPerformException {
        verifyUnitGroupUnitConfig(groupConfig);
        return GlobalCachedExecutorService.submit(() -> unitGroupUnitConfigRegistry.update(groupConfig));
    }

    /**
     * {@inheritDoc}
     *
     * @param groupConfig
     * @return
     * @throws CouldNotPerformException
     */
    @Override
    public Future<UnitConfig> removeUnitGroupConfig(final UnitConfig groupConfig) throws CouldNotPerformException {
        verifyUnitGroupUnitConfig(groupConfig);
        return GlobalCachedExecutorService.submit(() -> unitGroupUnitConfigRegistry.remove(groupConfig));
    }

    /**
     * {@inheritDoc}
     *
     * @param groupConfigId
     * @return
     * @throws CouldNotPerformException
     */
    @Override
    public UnitConfig getUnitGroupConfigById(final String groupConfigId) throws CouldNotPerformException {
        return unitGroupUnitConfigRegistry.get(groupConfigId).getMessage();
    }

    /**
     * {@inheritDoc}
     *
     * @return
     * @throws CouldNotPerformException
     */
    @Override
    public List<UnitConfig> getUnitGroupConfigs() throws CouldNotPerformException {
        return new ArrayList<>(unitGroupUnitConfigRegistry.getMessages());
    }

    /**
     * {@inheritDoc}
     *
     * @param unitConfig
     * @return
     * @throws CouldNotPerformException
     */
    @Override
    public List<UnitConfig> getUnitGroupConfigsByUnitConfig(final UnitConfig unitConfig) throws CouldNotPerformException {
        List<UnitConfig> unitConfigList = new ArrayList<>();
        for (UnitConfig unitGroupUnitConfig : unitGroupUnitConfigRegistry.getMessages()) {
            if (unitGroupUnitConfig.getUnitGroupConfig().getMemberIdList().contains(unitConfig.getId())) {
                unitConfigList.add(unitGroupUnitConfig);
            }
        }
        return unitConfigList;
    }

    /**
     * {@inheritDoc}
     *
     * @param type
     * @return
     * @throws CouldNotPerformException
     */
    @Override
    public List<UnitConfig> getUnitGroupConfigsByUnitType(final UnitType type) throws CouldNotPerformException {
        List<UnitConfig> unitConfigList = new ArrayList<>();
        for (UnitConfig unitGroupUnitConfig : unitGroupUnitConfigRegistry.getMessages()) {
            if (unitGroupUnitConfig.getType() == type || getSubUnitTypesOfUnitType(type).contains(unitGroupUnitConfig.getType())) {
                unitConfigList.add(unitGroupUnitConfig);
            }
        }
        return unitConfigList;
    }

    /**
     * {@inheritDoc}
     *
     * @param serviceTypes
     * @return
     * @throws CouldNotPerformException
     */
    @Override
    public List<UnitConfig> getUnitGroupConfigsByServiceTypes(final List<ServiceType> serviceTypes) throws CouldNotPerformException {
        List<UnitConfig> unitGroups = new ArrayList<>();
        for (UnitConfig unitGroupUnitConfig : unitGroupUnitConfigRegistry.getMessages()) {
            boolean skipGroup = false;
            for (ServiceDescription serviceDescription : unitGroupUnitConfig.getUnitGroupConfig().getServiceDescriptionList()) {
                if (!serviceTypes.contains(serviceDescription.getType())) {
                    skipGroup = true;
                }
            }
            if (skipGroup) {
                continue;
            }
            unitGroups.add(unitGroupUnitConfig);
        }
        return unitGroups;
    }

    /**
     * {@inheritDoc}
     *
     * @param unitGroupUnitConfig
     * @return
     * @throws CouldNotPerformException
     */
    @Override
    public List<UnitConfig> getUnitConfigsByUnitGroupConfig(final UnitConfig unitGroupUnitConfig) throws CouldNotPerformException {
        verifyUnitGroupUnitConfig(unitGroupUnitConfig);
        List<UnitConfig> unitConfigs = new ArrayList<>();
        for (String unitId : unitGroupUnitConfig.getUnitGroupConfig().getMemberIdList()) {
            unitConfigs.add(getUnitConfigById(unitId));
        }
        return unitConfigs;
    }

    /**
     * {@inheritDoc}
     *
     * @param type
     * @param serviceTypes
     * @return
     * @throws CouldNotPerformException
     */
    @Override
    public List<UnitConfig> getUnitConfigsByUnitTypeAndServiceTypes(final UnitType type, final List<ServiceType> serviceTypes) throws CouldNotPerformException {
        List<UnitConfig> unitConfigs = getUnitConfigs(type);
        boolean foundServiceType;
        
        for (UnitConfig unitConfig : new ArrayList<>(unitConfigs)) {
            foundServiceType = false;
            for (ServiceType serviceType : serviceTypes) {
                for (ServiceConfig serviceConfig : unitConfig.getServiceConfigList()) {
                    if (serviceConfig.getServiceDescription().getType() == serviceType) {
                        foundServiceType = true;
                    }
                }
                if (!foundServiceType) {
                    unitConfigs.remove(unitConfig);
                }
            }
        }
        return unitConfigs;
    }

    /**
     * {@inheritDoc}
     *
     * @param scope
     * @return
     * @throws CouldNotPerformException
     */
    @Override
    public UnitConfig getUnitConfigByScope(final ScopeType.Scope scope) throws CouldNotPerformException {
        for (UnitConfig unitConfig : getUnitConfigs()) {
            if (unitConfig.getScope().equals(scope)) {
                return unitConfig;
            }
        }
        throw new NotAvailableException("No unit config available for given scope!");
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

    // TODO: implement and interface the is consistent and is readonly flags for all internal registries.
    public ProtoBufFileSynchronizedRegistry<String, UnitTemplate, UnitTemplate.Builder, UnitRegistryData.Builder> getUnitTemplateRegistry() {
        return unitTemplateRegistry;
    }
    
    public ProtoBufFileSynchronizedRegistry<String, UnitConfig, UnitConfig.Builder, UnitRegistryData.Builder> getDalUnitConfigRegistry() {
        return dalUnitConfigRegistry;
    }
    
    public ProtoBufFileSynchronizedRegistry<String, UnitConfig, UnitConfig.Builder, UnitRegistryData.Builder> getUserUnitConfigRegistry() {
        return userUnitConfigRegistry;
    }
    
    public ProtoBufFileSynchronizedRegistry<String, UnitConfig, UnitConfig.Builder, UnitRegistryData.Builder> getAuthorizationGroupUnitConfigRegistry() {
        return authorizationGroupUnitConfigRegistry;
    }
    
    public ProtoBufFileSynchronizedRegistry<String, UnitConfig, UnitConfig.Builder, UnitRegistryData.Builder> getDeviceUnitConfigRegistry() {
        return deviceUnitConfigRegistry;
    }
    
    public ProtoBufFileSynchronizedRegistry<String, UnitConfig, UnitConfig.Builder, UnitRegistryData.Builder> getUnitGroupUnitConfigRegistry() {
        return unitGroupUnitConfigRegistry;
    }
    
    public ProtoBufFileSynchronizedRegistry<String, UnitConfig, UnitConfig.Builder, UnitRegistryData.Builder> getLocationUnitConfigRegistry() {
        return locationUnitConfigRegistry;
    }
    
    public ProtoBufFileSynchronizedRegistry<String, UnitConfig, UnitConfig.Builder, UnitRegistryData.Builder> getConnectionUnitConfigRegistry() {
        return connectionUnitConfigRegistry;
    }
    
    public ProtoBufFileSynchronizedRegistry<String, UnitConfig, UnitConfig.Builder, UnitRegistryData.Builder> getAgentUnitConfigRegistry() {
        return agentUnitConfigRegistry;
    }
    
    public ProtoBufFileSynchronizedRegistry<String, UnitConfig, UnitConfig.Builder, UnitRegistryData.Builder> getSceneUnitConfigRegistry() {
        return sceneUnitConfigRegistry;
    }
    
    public ProtoBufFileSynchronizedRegistry<String, UnitConfig, UnitConfig.Builder, UnitRegistryData.Builder> getAppUnitConfigRegistry() {
        return appUnitConfigRegistry;
    }
    
    public ArrayList<ProtoBufFileSynchronizedRegistry<String, UnitConfig, UnitConfig.Builder, UnitRegistryData.Builder>> getUnitConfigRegistryList() {
        return unitConfigRegistryList;
    }
    
    @Override
    public Boolean isDalUnitConfigRegistryReadOnly() throws CouldNotPerformException {
        return dalUnitConfigRegistry.isReadOnly();
    }
    
    @Override
    public Boolean isUserUnitRegistryReadOnly() throws CouldNotPerformException {
        return userUnitConfigRegistry.isReadOnly();
    }
    
    @Override
    public Boolean isAuthorizationGroupUnitRegistryReadOnly() throws CouldNotPerformException {
        return authorizationGroupUnitConfigRegistry.isReadOnly();
    }
    
    @Override
    public Boolean isDeviceUnitRegistryReadOnly() throws CouldNotPerformException {
        return deviceUnitConfigRegistry.isReadOnly();
    }
    
    @Override
    public Boolean isUnitGroupUnitRegistryReadOnly() throws CouldNotPerformException {
        return unitGroupUnitConfigRegistry.isReadOnly();
    }
    
    @Override
    public Boolean isLocationUnitRegistryReadOnly() throws CouldNotPerformException {
        return locationUnitConfigRegistry.isReadOnly();
    }
    
    @Override
    public Boolean isConnectionUnitRegistryReadOnly() throws CouldNotPerformException {
        return connectionUnitConfigRegistry.isReadOnly();
    }
    
    @Override
    public Boolean isAgentUnitRegistryReadOnly() throws CouldNotPerformException {
        return agentUnitConfigRegistry.isReadOnly();
    }
    
    @Override
    public Boolean isAppUnitRegistryReadOnly() throws CouldNotPerformException {
        return appUnitConfigRegistry.isReadOnly();
    }
    
    @Override
    public Boolean isSceneUnitRegistryReadOnly() throws CouldNotPerformException {
        return sceneUnitConfigRegistry.isReadOnly();
    }
    
    @Override
    public Boolean isDalUnitConfigRegistryConsistent() throws CouldNotPerformException {
        return dalUnitConfigRegistry.isConsistent();
    }
    
    @Override
    public Boolean isUserUnitRegistryConsistent() throws CouldNotPerformException {
        return userUnitConfigRegistry.isConsistent();
    }
    
    @Override
    public Boolean isAuthorizationGroupUnitRegistryConsistent() throws CouldNotPerformException {
        return authorizationGroupUnitConfigRegistry.isConsistent();
    }
    
    @Override
    public Boolean isDeviceUnitRegistryConsistent() throws CouldNotPerformException {
        return deviceUnitConfigRegistry.isConsistent();
    }
    
    @Override
    public Boolean isUnitGroupUnitRegistryConsistent() throws CouldNotPerformException {
        return unitGroupUnitConfigRegistry.isConsistent();
    }
    
    @Override
    public Boolean isLocationUnitRegistryConsistent() throws CouldNotPerformException {
        return locationUnitConfigRegistry.isConsistent();
    }
    
    @Override
    public Boolean isConnectionUnitRegistryConsistent() throws CouldNotPerformException {
        return connectionUnitConfigRegistry.isConsistent();
    }
    
    @Override
    public Boolean isAgentUnitRegistryConsistent() throws CouldNotPerformException {
        return agentUnitConfigRegistry.isConsistent();
    }
    
    @Override
    public Boolean isAppUnitRegistryConsistent() throws CouldNotPerformException {
        return appUnitConfigRegistry.isConsistent();
    }
    
    @Override
    public Boolean isSceneUnitRegistryConsistent() throws CouldNotPerformException {
        return sceneUnitConfigRegistry.isConsistent();
    }
    
    @Override
    protected void registerRemoteRegistries() throws CouldNotPerformException {
    }

    /**
     *
     * @return
     * @deprecated get your own instance via the registry pool.
     */
    @Deprecated
    public DeviceRegistryRemote getDeviceRegistryRemote() {
        try {
            return CachedDeviceRegistryRemote.getRegistry();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        } catch (NotAvailableException ex) {
            new FatalImplementationErrorException("registry not available", this, ex);
        }
        return null;
    }

    /**
     * {@inheritDoc}
     *
     * @param type
     * @return
     * @throws CouldNotPerformException
     */
    @Override
    public List<UnitTemplate.UnitType> getSubUnitTypes(UnitTemplate.UnitType type) throws CouldNotPerformException {
        List<UnitTemplate.UnitType> unitTypes = new ArrayList<>();
        for (UnitTemplate template : unitTemplateRegistry.getMessages()) {
            if (template.getIncludedTypeList().contains(type)) {
                unitTypes.add(template.getType());
                unitTypes.addAll(getSubUnitTypes(template.getType()));
            }
        }
        return unitTypes;
    }

    /**
     * {@inheritDoc}
     *
     * @param type
     * @return
     * @throws CouldNotPerformException
     */
    @Override
    public List<UnitTemplate.UnitType> getSuperUnitTypes(UnitTemplate.UnitType type) throws CouldNotPerformException {
        UnitTemplate unitTemplate = getUnitTemplateByType(type);
        List<UnitTemplate.UnitType> unitTypes = new ArrayList<>();
        for (UnitTemplate template : unitTemplateRegistry.getMessages()) {
            if (unitTemplate.getIncludedTypeList().contains(template.getType())) {
                unitTypes.add(template.getType());
                unitTypes.addAll(getSuperUnitTypes(template.getType()));
            }
        }
        return unitTypes;
    }
    
    @Override
    public void validateData() throws InvalidStateException {
        if (!isDataAvailable()) {
            throw new InvalidStateException(this + " not synchronized yet!", new NotAvailableException("data"));
        }
    }
    
    @Override
    public Future<ServiceTemplate> updateServiceTemplate(ServiceTemplate serviceTemplate) throws CouldNotPerformException {
        return GlobalCachedExecutorService.submit(() -> serviceTemplateRegistry.update(serviceTemplate));
    }
    
    @Override
    public Boolean containsServiceTemplate(ServiceTemplate serviceTemplate) throws CouldNotPerformException {
        return serviceTemplateRegistry.contains(serviceTemplate);
    }
    
    @Override
    public Boolean containsServiceTemplateById(String serviceTemplateId) throws CouldNotPerformException {
        return serviceTemplateRegistry.contains(serviceTemplateId);
    }
    
    @Override
    public ServiceTemplate getServiceTemplateById(String serviceTemplateId) throws CouldNotPerformException {
        return serviceTemplateRegistry.getMessage(serviceTemplateId);
    }
    
    @Override
    public List<ServiceTemplate> getServiceTemplates() throws CouldNotPerformException {
        return serviceTemplateRegistry.getMessages();
    }
    
    @Override
    public ServiceTemplate getServiceTemplateByType(ServiceType type) throws CouldNotPerformException {
        for (ServiceTemplate serviceTemplate : serviceTemplateRegistry.getMessages()) {
            if (serviceTemplate.getType() == type) {
                return serviceTemplate;
            }
        }
        throw new NotAvailableException("ServiceTemplate with type [" + type + "]");
    }
    
    @Override
    public Boolean isServiceTemplateRegistryReadOnly() throws CouldNotPerformException {
        return serviceTemplateRegistry.isReadOnly();
    }
    
    @Override
    public Boolean isServiceTemplateRegistryConsistent() throws CouldNotPerformException {
        return serviceTemplateRegistry.isConsistent();
    }

    @Override
    public Shape getUnitShape(final UnitConfig unitConfig) throws NotAvailableException {
        try {
            return UntShapeGenerator.generateUnitShape(unitConfig, this, CachedDeviceRegistryRemote.getRegistry());
        } catch (InterruptedException ex) {
            // because registries should not throw interrupted exceptions in a future release this exception is already transformed into a NotAvailableException.
            Thread.currentThread().interrupt();
            throw new NotAvailableException("UnitShape", new CouldNotPerformException("Shutdown in progress"));
        }
    }
}
