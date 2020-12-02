package org.openbase.bco.registry.unit.core;

/*
 * #%L
 * BCO Registry Unit Core
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

import com.google.protobuf.ByteString;
import com.google.protobuf.Descriptors.FieldDescriptor;
import org.openbase.bco.authentication.lib.*;
import org.openbase.bco.authentication.lib.AuthorizationHelper.PermissionType;
import org.openbase.bco.authentication.lib.jp.JPAuthentication;
import org.openbase.bco.registry.clazz.remote.CachedClassRegistryRemote;
import org.openbase.bco.registry.lib.com.AbstractRegistryController;
import org.openbase.bco.registry.lib.jp.JPBCODatabaseDirectory;
import org.openbase.bco.registry.lib.util.UnitConfigProcessor;
import org.openbase.bco.registry.template.remote.CachedTemplateRegistryRemote;
import org.openbase.bco.registry.unit.core.consistency.*;
import org.openbase.bco.registry.unit.core.consistency.agentconfig.AgentConfigAgentClassIdConsistencyHandler;
import org.openbase.bco.registry.unit.core.consistency.appconfig.AppConfigAppClassIdConsistencyHandler;
import org.openbase.bco.registry.unit.core.consistency.authorizationgroup.*;
import org.openbase.bco.registry.unit.core.consistency.connectionconfig.ConnectionLocationConsistencyHandler;
import org.openbase.bco.registry.unit.core.consistency.connectionconfig.ConnectionTilesConsistencyHandler;
import org.openbase.bco.registry.unit.core.consistency.dalunitconfig.*;
import org.openbase.bco.registry.unit.core.consistency.deviceconfig.*;
import org.openbase.bco.registry.unit.core.consistency.gatewayconfig.GatewayConfigGatewayClassIdConsistencyHandler;
import org.openbase.bco.registry.unit.core.consistency.gatewayconfig.GatewayUnitLabelConsistencyHandler;
import org.openbase.bco.registry.unit.core.consistency.locationconfig.*;
import org.openbase.bco.registry.unit.core.consistency.sceneconfig.SceneServiceStateConsistencyHandler;
import org.openbase.bco.registry.unit.core.consistency.sceneconfig.ServiceStateDescriptionHierarchyConsistencyHandler;
import org.openbase.bco.registry.unit.core.consistency.unitgroupconfig.*;
import org.openbase.bco.registry.unit.core.consistency.userconfig.*;
import org.openbase.bco.registry.unit.core.plugin.*;
import org.openbase.bco.registry.unit.lib.UnitRegistry;
import org.openbase.bco.registry.unit.lib.auth.AuthorizationWithTokenHelper;
import org.openbase.bco.registry.unit.lib.generator.UnitConfigIdGenerator;
import org.openbase.bco.registry.unit.lib.generator.UnitShapeGenerator;
import org.openbase.bco.registry.unit.lib.jp.*;
import org.openbase.jps.core.JPService;
import org.openbase.jps.exception.JPNotAvailableException;
import org.openbase.jps.exception.JPServiceException;
import org.openbase.jul.communication.controller.RPCHelper;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.exception.*;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.exception.printer.LogLevel;
import org.openbase.jul.extension.protobuf.IdentifiableMessage;
import org.openbase.jul.extension.rsb.iface.RSBLocalServer;
import org.openbase.jul.extension.type.storage.registry.consistency.TransformationFrameConsistencyHandler;
import org.openbase.jul.pattern.ListFilter;
import org.openbase.jul.schedule.GlobalCachedExecutorService;
import org.openbase.jul.schedule.SyncObject;
import org.openbase.jul.storage.file.ProtoBufJSonFileProvider;
import org.openbase.jul.storage.registry.ProtoBufFileSynchronizedRegistry;
import org.openbase.type.domotic.authentication.AuthenticatedValueType.AuthenticatedValue;
import org.openbase.type.domotic.authentication.AuthenticationTokenType.AuthenticationToken;
import org.openbase.type.domotic.authentication.AuthorizationTokenType.AuthorizationToken;
import org.openbase.type.domotic.authentication.UserClientPairType.UserClientPair;
import org.openbase.type.domotic.registry.UnitRegistryDataType.UnitRegistryData;
import org.openbase.type.domotic.service.ServiceConfigType.ServiceConfig;
import org.openbase.type.domotic.service.ServiceTemplateType.ServiceTemplate;
import org.openbase.type.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import org.openbase.type.domotic.unit.UnitConfigType.UnitConfig;
import org.openbase.type.domotic.unit.UnitConfigType.UnitConfig.Builder;
import org.openbase.type.domotic.unit.UnitTemplateType.UnitTemplate;
import org.openbase.type.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;
import org.openbase.type.domotic.unit.agent.AgentConfigType.AgentConfig;
import org.openbase.type.domotic.unit.app.AppConfigType.AppConfig;
import org.openbase.type.domotic.unit.authorizationgroup.AuthorizationGroupConfigType.AuthorizationGroupConfig;
import org.openbase.type.domotic.unit.connection.ConnectionConfigType.ConnectionConfig;
import org.openbase.type.domotic.unit.device.DeviceConfigType.DeviceConfig;
import org.openbase.type.domotic.unit.gateway.GatewayConfigType.GatewayConfig;
import org.openbase.type.domotic.unit.location.LocationConfigType.LocationConfig;
import org.openbase.type.domotic.unit.scene.SceneConfigType.SceneConfig;
import org.openbase.type.domotic.unit.unitgroup.UnitGroupConfigType.UnitGroupConfig;
import org.openbase.type.domotic.unit.user.UserConfigType.UserConfig;
import org.openbase.type.spatial.ShapeType.Shape;
import org.slf4j.LoggerFactory;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;


/**
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public class UnitRegistryController extends AbstractRegistryController<UnitRegistryData, UnitRegistryData.Builder> implements UnitRegistry {

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
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(GatewayConfig.getDefaultInstance()));
    }

    public final static UnitConfigIdGenerator UNIT_ID_GENERATOR = new UnitConfigIdGenerator();

    private final static org.slf4j.Logger LOGGER = LoggerFactory.getLogger(UnitRegistryController.class);

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
    private final ProtoBufFileSynchronizedRegistry<String, UnitConfig, UnitConfig.Builder, UnitRegistryData.Builder> objectUnitConfigRegistry;
    private final ProtoBufFileSynchronizedRegistry<String, UnitConfig, UnitConfig.Builder, UnitRegistryData.Builder> gatewayUnitConfigRegistry;

    private final ArrayList<ProtoBufFileSynchronizedRegistry<String, UnitConfig, UnitConfig.Builder, UnitRegistryData.Builder>> unitConfigRegistryList, baseUnitConfigRegistryList;

    private final SyncObject aliasIdMapLock;
    private final TreeMap<String, String> aliasIdMap;

    public UnitRegistryController() throws InstantiationException, InterruptedException {
        super(JPUnitRegistryScope.class, UnitRegistryData.newBuilder());
        try {
            this.aliasIdMap = new TreeMap<>();
            this.aliasIdMapLock = new SyncObject("AliasIdMapLock");

            this.unitConfigRegistryList = new ArrayList<>();
            this.baseUnitConfigRegistryList = new ArrayList<>();

            // verify that database exists and fail if not so no further errors are printed because they are based on this property.
            try {
                LOGGER.info("Use bco registry at " + JPService.getProperty(JPBCODatabaseDirectory.class).getValue());
            } catch (JPServiceException ex) {
                throw new NotAvailableException("Database");
            }

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
            this.objectUnitConfigRegistry = new ProtoBufFileSynchronizedRegistry<>(UnitConfig.class, getBuilderSetup(), getDataFieldDescriptor(UnitRegistryData.OBJECT_UNIT_CONFIG_FIELD_NUMBER), UNIT_ID_GENERATOR, JPService.getProperty(JPObjectConfigDatabaseDirectory.class).getValue(), protoBufJSonFileProvider);
            this.gatewayUnitConfigRegistry = new ProtoBufFileSynchronizedRegistry<>(UnitConfig.class, getBuilderSetup(), getDataFieldDescriptor(UnitRegistryData.GATEWAY_UNIT_CONFIG_FIELD_NUMBER), UNIT_ID_GENERATOR, JPService.getProperty(JPGatewayConfigDatabaseDirectory.class).getValue(), protoBufJSonFileProvider);

            // ===== Attention the order over here is important because it defines the order of the initially performed consistency checks! ==== //
            this.unitConfigRegistryList.add(locationUnitConfigRegistry);
            this.unitConfigRegistryList.add(connectionUnitConfigRegistry);
            this.unitConfigRegistryList.add(authorizationGroupUnitConfigRegistry);
            this.unitConfigRegistryList.add(userUnitConfigRegistry);
            this.unitConfigRegistryList.add(gatewayUnitConfigRegistry);
            this.unitConfigRegistryList.add(deviceUnitConfigRegistry);
            this.unitConfigRegistryList.add(dalUnitConfigRegistry);
            this.unitConfigRegistryList.add(unitGroupUnitConfigRegistry);
            this.unitConfigRegistryList.add(sceneUnitConfigRegistry);
            this.unitConfigRegistryList.add(agentUnitConfigRegistry);
            this.unitConfigRegistryList.add(appUnitConfigRegistry);
            this.unitConfigRegistryList.add(objectUnitConfigRegistry);
            // ================================================================================================================================= //

            this.baseUnitConfigRegistryList.add(userUnitConfigRegistry);
            this.baseUnitConfigRegistryList.add(authorizationGroupUnitConfigRegistry);
            this.baseUnitConfigRegistryList.add(deviceUnitConfigRegistry);
            this.baseUnitConfigRegistryList.add(unitGroupUnitConfigRegistry);
            this.baseUnitConfigRegistryList.add(locationUnitConfigRegistry);
            this.baseUnitConfigRegistryList.add(connectionUnitConfigRegistry);
            this.baseUnitConfigRegistryList.add(sceneUnitConfigRegistry);
            this.baseUnitConfigRegistryList.add(agentUnitConfigRegistry);
            this.baseUnitConfigRegistryList.add(appUnitConfigRegistry);
            this.baseUnitConfigRegistryList.add(objectUnitConfigRegistry);
            this.baseUnitConfigRegistryList.add(gatewayUnitConfigRegistry);
        } catch (JPServiceException | NullPointerException | CouldNotPerformException ex) {
            throw new InstantiationException(this, ex);
        }
    }

    @Override
    protected void postInit() throws InitializationException, InterruptedException {
        // post init loads registries
        super.postInit();

        // initially fill the alias to id map
        // afterwards the {@code AliasMapUpdatePlugin} will manage changes on registering, removing or updating of units
        synchronized (aliasIdMapLock) {
            try {
                for (ProtoBufFileSynchronizedRegistry<String, UnitConfig, Builder, UnitRegistryData.Builder> registry : unitConfigRegistryList) {
                    registry.getMessages().forEach((unitConfig) -> unitConfig.getAliasList().forEach(alias -> aliasIdMap.put(alias.toLowerCase(), unitConfig.getId())));
                }
            } catch (CouldNotPerformException ex) {
                throw new InitializationException(this, ex);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void registerRegistries() {
        unitConfigRegistryList.forEach(this::registerRegistry);
    }

    /**
     * {@inheritDoc}
     *
     * @throws CouldNotPerformException {@inheritDoc}
     */
    @Override
    protected void registerConsistencyHandler() throws CouldNotPerformException, InterruptedException {
        // register alias generator first to make sure other consistency checks can access the alias
        registerConsistencyHandler(new UnitIdUniqueVerificationConsistencyHandler(), UnitConfig.class);
        registerConsistencyHandler(new UnitAliasGenerationConsistencyHandler(this), UnitConfig.class);
        registerConsistencyHandler(new UnitAliasUniqueVerificationConsistencyHandler(this), UnitConfig.class);
        registerConsistencyHandler(new UnitLabelConsistencyHandler(), UnitConfig.class);

        agentUnitConfigRegistry.registerConsistencyHandler(new AgentConfigAgentClassIdConsistencyHandler());
        agentUnitConfigRegistry.registerConsistencyHandler(new DefaultUnitLabelConsistencyHandler());
        agentUnitConfigRegistry.registerConsistencyHandler(new ExecutableUnitAutostartConsistencyHandler());

        appUnitConfigRegistry.registerConsistencyHandler(new AppConfigAppClassIdConsistencyHandler());
        appUnitConfigRegistry.registerConsistencyHandler(new DefaultUnitLabelConsistencyHandler());
        appUnitConfigRegistry.registerConsistencyHandler(new ExecutableUnitAutostartConsistencyHandler());

        authorizationGroupUnitConfigRegistry.registerConsistencyHandler(new AuthorizationGroupConfigLabelConsistencyHandler());
        authorizationGroupUnitConfigRegistry.registerConsistencyHandler(new AuthorziationGroupDuplicateMemberConsistencyHandler());
        authorizationGroupUnitConfigRegistry.registerConsistencyHandler(new AuthorizationGroupPermissionConsistencyHandler());
        authorizationGroupUnitConfigRegistry.registerConsistencyHandler(new AuthorizationGroupClassGroupConsistencyHandler(userUnitConfigRegistry, agentUnitConfigRegistry, appUnitConfigRegistry));

        connectionUnitConfigRegistry.registerConsistencyHandler(new DefaultUnitLabelConsistencyHandler());
        connectionUnitConfigRegistry.registerConsistencyHandler(new ConnectionTilesConsistencyHandler(locationUnitConfigRegistry));
        connectionUnitConfigRegistry.registerConsistencyHandler(new ConnectionLocationConsistencyHandler(locationUnitConfigRegistry));

        dalUnitConfigRegistry.registerConsistencyHandler(new DalUnitEnablingStateConsistencyHandler(deviceUnitConfigRegistry));
        dalUnitConfigRegistry.registerConsistencyHandler(new DalUnitHostIdConsistencyHandler(deviceUnitConfigRegistry, appUnitConfigRegistry));
        dalUnitConfigRegistry.registerConsistencyHandler(new DalUnitLabelConsistencyHandler(deviceUnitConfigRegistry, appUnitConfigRegistry));
        dalUnitConfigRegistry.registerConsistencyHandler(new DalUnitLocationIdConsistencyHandler(locationUnitConfigRegistry, deviceUnitConfigRegistry));
        dalUnitConfigRegistry.registerConsistencyHandler(new SyncBindingConfigDeviceClassUnitConsistencyHandler(deviceUnitConfigRegistry));
        dalUnitConfigRegistry.registerConsistencyHandler(new UnitBoundToHostConsistencyHandler(deviceUnitConfigRegistry, locationUnitConfigRegistry));

        deviceUnitConfigRegistry.registerConsistencyHandler(new DeviceBoundToHostConsistencyHandler());
        deviceUnitConfigRegistry.registerConsistencyHandler(new DeviceConfigDeviceClassIdConsistencyHandler());
        deviceUnitConfigRegistry.registerConsistencyHandler(new DeviceConfigLocationIdForInstalledDevicesConsistencyHandler());
        deviceUnitConfigRegistry.registerConsistencyHandler(new DeviceInventoryStateConsistencyHandler());
        deviceUnitConfigRegistry.registerConsistencyHandler(new DeviceEnablingStateConsistencyHandler());
        deviceUnitConfigRegistry.registerConsistencyHandler(new DeviceUnitLabelConsistencyHandler());
        deviceUnitConfigRegistry.registerConsistencyHandler(new UnitLocationIdAutoSetupConsistencyHandler(locationUnitConfigRegistry));
        deviceUnitConfigRegistry.registerConsistencyHandler(new UnitOwnerConsistencyHandler(userUnitConfigRegistry));

        userUnitConfigRegistry.registerConsistencyHandler(new UserConfigUserNameConsistencyHandler());
        userUnitConfigRegistry.registerConsistencyHandler(new UserUnitLabelConsistencyHandler());
        userUnitConfigRegistry.registerConsistencyHandler(new UserPermissionConsistencyHandler());
        userUnitConfigRegistry.registerConsistencyHandler(new UserConfigLanguageConsistencyHandler());

        unitGroupUnitConfigRegistry.registerConsistencyHandler(new DefaultUnitLabelConsistencyHandler());
        unitGroupUnitConfigRegistry.registerConsistencyHandler(new UnitGroupMemberListDuplicationConsistencyHandler());
        unitGroupUnitConfigRegistry.registerConsistencyHandler(new UnitGroupMemberExistsConsistencyHandler(agentUnitConfigRegistry, appUnitConfigRegistry, authorizationGroupUnitConfigRegistry, connectionUnitConfigRegistry, dalUnitConfigRegistry, deviceUnitConfigRegistry, locationUnitConfigRegistry, sceneUnitConfigRegistry, unitGroupUnitConfigRegistry, userUnitConfigRegistry));
        unitGroupUnitConfigRegistry.registerConsistencyHandler(new UnitGroupServiceDescriptionServiceTemplateIdConsistencyHandler());
        unitGroupUnitConfigRegistry.registerConsistencyHandler(new UnitGroupUnitTypeConsistencyHandler());
        unitGroupUnitConfigRegistry.registerConsistencyHandler(new UnitGroupMemberListTypesConsistencyHandler(agentUnitConfigRegistry, appUnitConfigRegistry, authorizationGroupUnitConfigRegistry, connectionUnitConfigRegistry, dalUnitConfigRegistry, deviceUnitConfigRegistry, locationUnitConfigRegistry, sceneUnitConfigRegistry, unitGroupUnitConfigRegistry, userUnitConfigRegistry));
        unitGroupUnitConfigRegistry.registerConsistencyHandler(new UnitGroupPlacementConfigConsistencyHandler(unitConfigRegistryList, locationUnitConfigRegistry));

        locationUnitConfigRegistry.registerConsistencyHandler(new LocationPlacementConfigConsistencyHandler());
        locationUnitConfigRegistry.registerConsistencyHandler(new LocationPositionConsistencyHandler());
        locationUnitConfigRegistry.registerConsistencyHandler(new RootConsistencyHandler());
        locationUnitConfigRegistry.registerConsistencyHandler(new LocationChildConsistencyHandler());
        locationUnitConfigRegistry.registerConsistencyHandler(new LocationParentConsistencyHandler());
        locationUnitConfigRegistry.registerConsistencyHandler(new RootLocationExistenceConsistencyHandler());
        locationUnitConfigRegistry.registerConsistencyHandler(new LocationLoopConsistencyHandler());
        locationUnitConfigRegistry.registerConsistencyHandler(new DefaultUnitLabelConsistencyHandler());
        locationUnitConfigRegistry.registerConsistencyHandler(new LocationUnitIdConsistencyHandler(unitConfigRegistryList));
        locationUnitConfigRegistry.registerConsistencyHandler(new LocationTypeConsistencyHandler());
        locationUnitConfigRegistry.registerConsistencyHandler(new LocationHierarchyConsistencyHandler());
        locationUnitConfigRegistry.registerConsistencyHandler(new LocationShapeConsistencyHandler());
        locationUnitConfigRegistry.registerConsistencyHandler(new TileConnectionIdConsistencyHandler(connectionUnitConfigRegistry));
        locationUnitConfigRegistry.registerConsistencyHandler(new RootLocationPermissionConsistencyHandler(aliasIdMap));
        locationUnitConfigRegistry.registerConsistencyHandler(new UnitUserPermissionConsistencyHandler(this));

        sceneUnitConfigRegistry.registerConsistencyHandler(new DefaultUnitLabelConsistencyHandler());
        sceneUnitConfigRegistry.registerConsistencyHandler(new SceneServiceStateConsistencyHandler(unitConfigRegistryList));
        sceneUnitConfigRegistry.registerConsistencyHandler(new ServiceStateDescriptionHierarchyConsistencyHandler());

        gatewayUnitConfigRegistry.registerConsistencyHandler(new GatewayConfigGatewayClassIdConsistencyHandler());
        gatewayUnitConfigRegistry.registerConsistencyHandler(new GatewayUnitLabelConsistencyHandler());
        gatewayUnitConfigRegistry.registerConsistencyHandler(new UnitLocationIdAutoSetupConsistencyHandler(locationUnitConfigRegistry));
        gatewayUnitConfigRegistry.registerConsistencyHandler(new UnitOwnerConsistencyHandler(userUnitConfigRegistry));

        // add consistency handler for all unitConfig registries
        registerConsistencyHandler(new BaseUnitTypeFieldConsistencyHandler(), UnitConfig.class);
        registerConsistencyHandler(new UnitLocationIdConsistencyHandler(locationUnitConfigRegistry, dalUnitConfigRegistry), UnitConfig.class);
        registerConsistencyHandler(new ServiceConfigUnitIdConsistencyHandler(), UnitConfig.class);
        registerConsistencyHandler(new UnitServiceConfigConsistencyHandler(), UnitConfig.class);
        registerConsistencyHandler(new UnitConfigUnitTemplateConsistencyHandler(), UnitConfig.class);
        registerConsistencyHandler(new UnitEnablingStateConsistencyHandler(), UnitConfig.class);
        registerConsistencyHandler(new BoundingBoxConsistencyHandler(), UnitConfig.class);
        registerConsistencyHandler(new TransformationFrameConsistencyHandler(locationUnitConfigRegistry), UnitConfig.class);
        registerConsistencyHandler(new UnitPermissionCleanerConsistencyHandler(authorizationGroupUnitConfigRegistry, locationUnitConfigRegistry), UnitConfig.class);
        registerConsistencyHandler(new AccessPermissionConsistencyHandler(), UnitConfig.class);
        registerConsistencyHandler(new UnitScopeConsistencyHandler(this), UnitConfig.class);

        if (JPService.getValue(JPAuthentication.class, true)) {
            authorizationGroupUnitConfigRegistry.registerConsistencyHandler(new AuthorizationGroupAdminAndBCOConsistencyHandler(aliasIdMap));
            registerConsistencyHandler(new OtherPermissionConsistencyHandler(), UnitConfig.class);
            registerConsistencyHandler(new GroupPermissionConsistencyHandler(aliasIdMap), UnitConfig.class);
        }
        if (JPService.getValue(JPClearUnitPosition.class, false)) {
            registerConsistencyHandler(new UnitPositionCleanerConsistencyHandler(), UnitConfig.class);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @throws CouldNotPerformException {@inheritDoc}
     * @throws InterruptedException     {@inheritDoc}
     */
    @Override
    protected void registerPlugins() throws CouldNotPerformException, InterruptedException {
        // the AliasMapUpdatePlugin has to be registered first to apply changes done by the following plugins
        for (ProtoBufFileSynchronizedRegistry<String, UnitConfig, Builder, UnitRegistryData.Builder> registry : unitConfigRegistryList) {
            registry.registerPlugin(new AliasMapUpdatePlugin(aliasIdMap, aliasIdMapLock));
        }

        locationUnitConfigRegistry.registerPlugin(new RootLocationPlugin());
        try {
            if (JPService.getProperty(JPAuthentication.class).getValue()) {
                authorizationGroupUnitConfigRegistry.registerPlugin(new AuthorizationGroupCreationPlugin());
                userUnitConfigRegistry.registerPlugin(new UserCreationPlugin(locationUnitConfigRegistry));
            }
        } catch (JPNotAvailableException ex) {
            ExceptionPrinter.printHistory("Could not load " + JPAuthentication.class.getSimpleName(), ex, LOGGER, LogLevel.WARN);
        }

        deviceUnitConfigRegistry.registerPlugin(new DeviceConfigDeviceClassUnitConsistencyPlugin(dalUnitConfigRegistry, deviceUnitConfigRegistry));
        gatewayUnitConfigRegistry.registerPlugin(new GatewayUnitIdConsistencyPlugin(dalUnitConfigRegistry, gatewayUnitConfigRegistry));

        dalUnitConfigRegistry.registerPlugin(new DalUnitBoundToHostPlugin(deviceUnitConfigRegistry));
        locationUnitConfigRegistry.registerPlugin(new LocationRemovalPlugin(unitConfigRegistryList, locationUnitConfigRegistry, connectionUnitConfigRegistry));

        agentUnitConfigRegistry.registerPlugin(new UnitUserCreationPlugin(userUnitConfigRegistry, locationUnitConfigRegistry));
        appUnitConfigRegistry.registerPlugin(new UnitUserCreationPlugin(userUnitConfigRegistry, locationUnitConfigRegistry));

        authorizationGroupUnitConfigRegistry.registerPlugin(new ClassAuthorizationGroupCreationPlugin(/*this*/));

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
        gatewayUnitConfigRegistry.registerPlugin(new PublishUnitTransformationRegistryPlugin(locationUnitConfigRegistry));
    }

    /**
     * {@inheritDoc}
     *
     * @throws CouldNotPerformException {@inheritDoc}
     */
    @Override
    protected void registerDependencies() throws CouldNotPerformException {
        registerDependency(CachedTemplateRegistryRemote.getRegistry().getUnitTemplateRemoteRegistry(false), UnitConfig.class);

        dalUnitConfigRegistry.registerDependency(deviceUnitConfigRegistry);
        dalUnitConfigRegistry.registerDependency(locationUnitConfigRegistry);

        authorizationGroupUnitConfigRegistry.registerDependency(userUnitConfigRegistry);
        authorizationGroupUnitConfigRegistry.registerDependency(agentUnitConfigRegistry);
        authorizationGroupUnitConfigRegistry.registerDependency(appUnitConfigRegistry);
        authorizationGroupUnitConfigRegistry.registerDependency(CachedClassRegistryRemote.getRegistry().getAgentClassRemoteRegistry(false));
        authorizationGroupUnitConfigRegistry.registerDependency(CachedClassRegistryRemote.getRegistry().getAppClassRemoteRegistry(false));

        deviceUnitConfigRegistry.registerDependency(gatewayUnitConfigRegistry);
        deviceUnitConfigRegistry.registerDependency(locationUnitConfigRegistry);
        deviceUnitConfigRegistry.registerDependency(userUnitConfigRegistry);
        deviceUnitConfigRegistry.registerDependency(CachedClassRegistryRemote.getRegistry().getDeviceClassRemoteRegistry(false));

        gatewayUnitConfigRegistry.registerDependency(locationUnitConfigRegistry);
        gatewayUnitConfigRegistry.registerDependency(userUnitConfigRegistry);
        gatewayUnitConfigRegistry.registerDependency(CachedClassRegistryRemote.getRegistry().getGatewayClassRemoteRegistry(false));

        unitGroupUnitConfigRegistry.registerDependency(agentUnitConfigRegistry);
        unitGroupUnitConfigRegistry.registerDependency(appUnitConfigRegistry);
        unitGroupUnitConfigRegistry.registerDependency(authorizationGroupUnitConfigRegistry);
        unitGroupUnitConfigRegistry.registerDependency(connectionUnitConfigRegistry);
        unitGroupUnitConfigRegistry.registerDependency(dalUnitConfigRegistry);
        unitGroupUnitConfigRegistry.registerDependency(deviceUnitConfigRegistry);
        unitGroupUnitConfigRegistry.registerDependency(locationUnitConfigRegistry);
        unitGroupUnitConfigRegistry.registerDependency(sceneUnitConfigRegistry);
        unitGroupUnitConfigRegistry.registerDependency(userUnitConfigRegistry);
        unitGroupUnitConfigRegistry.registerDependency(CachedClassRegistryRemote.getRegistry().getDeviceClassRemoteRegistry(false));
        unitGroupUnitConfigRegistry.registerDependency(CachedClassRegistryRemote.getRegistry().getAppClassRemoteRegistry(false));
        unitGroupUnitConfigRegistry.registerDependency(CachedClassRegistryRemote.getRegistry().getGatewayClassRemoteRegistry(false));

        locationUnitConfigRegistry.registerDependency(agentUnitConfigRegistry);
        locationUnitConfigRegistry.registerDependency(appUnitConfigRegistry);
        locationUnitConfigRegistry.registerDependency(authorizationGroupUnitConfigRegistry);
        locationUnitConfigRegistry.registerDependency(connectionUnitConfigRegistry);
        locationUnitConfigRegistry.registerDependency(dalUnitConfigRegistry);
        locationUnitConfigRegistry.registerDependency(deviceUnitConfigRegistry);
        locationUnitConfigRegistry.registerDependency(gatewayUnitConfigRegistry);
        locationUnitConfigRegistry.registerDependency(sceneUnitConfigRegistry);
        locationUnitConfigRegistry.registerDependency(unitGroupUnitConfigRegistry);
        locationUnitConfigRegistry.registerDependency(userUnitConfigRegistry);

        connectionUnitConfigRegistry.registerDependency(locationUnitConfigRegistry);

        agentUnitConfigRegistry.registerDependency(locationUnitConfigRegistry);
        agentUnitConfigRegistry.registerDependency(CachedClassRegistryRemote.getRegistry().getAgentClassRemoteRegistry(false));

        sceneUnitConfigRegistry.registerDependency(locationUnitConfigRegistry);

        appUnitConfigRegistry.registerDependency(CachedClassRegistryRemote.getRegistry().getAppClassRemoteRegistry(false));
        appUnitConfigRegistry.registerDependency(locationUnitConfigRegistry);
    }

    @Override
    public final void syncRegistryFlags() throws CouldNotPerformException {
        setDataField(UnitRegistryData.DAL_UNIT_CONFIG_REGISTRY_READ_ONLY_FIELD_NUMBER, dalUnitConfigRegistry.isReadOnly());
        setDataField(UnitRegistryData.DAL_UNIT_CONFIG_REGISTRY_CONSISTENT_FIELD_NUMBER, dalUnitConfigRegistry.isConsistent());

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

        setDataField(UnitRegistryData.OBJECT_UNIT_CONFIG_REGISTRY_READ_ONLY_FIELD_NUMBER, isObjectUnitRegistryReadOnly());
        setDataField(UnitRegistryData.OBJECT_UNIT_CONFIG_REGISTRY_CONSISTENT_FIELD_NUMBER, isObjectUnitRegistryConsistent());

        setDataField(UnitRegistryData.GATEWAY_UNIT_CONFIG_REGISTRY_READ_ONLY_FIELD_NUMBER, isGatewayUnitRegistryReadOnly());
        setDataField(UnitRegistryData.GATEWAY_UNIT_CONFIG_REGISTRY_CONSISTENT_FIELD_NUMBER, isGatewayUnitRegistryConsistent());
    }

    @Override
    public void registerMethods(final RSBLocalServer server) throws CouldNotPerformException {
        super.registerMethods(server);
        RPCHelper.registerInterface(UnitRegistry.class, this, server);
    }

    @Override
    public void notifyChange() throws CouldNotPerformException, InterruptedException {
        updateTransactionId();
        super.notifyChange();
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
            case OBJECT:
                return objectUnitConfigRegistry;
            case GATEWAY:
                return gatewayUnitConfigRegistry;
            default:
                return dalUnitConfigRegistry;
        }
    }

    @Override
    public Future<UnitConfig> registerUnitConfig(final UnitConfig unitConfig) {
        return GlobalCachedExecutorService.submit(() -> {
            UnitConfig result = getUnitConfigRegistry(unitConfig.getUnitType()).register(unitConfig);
            return result;
        });
    }

    @Override
    public Future<AuthenticatedValue> registerUnitConfigAuthenticated(final AuthenticatedValue authenticatedValue) {
        return GlobalCachedExecutorService.submit(() -> AuthenticatedServiceProcessor.authenticatedAction(authenticatedValue, UnitConfig.class, this, (unitConfig, authenticationBaseData) -> {
                    // find the location the unit will be placed at
                    final UnitConfig location;
                    if (unitConfig.hasPlacementConfig() && unitConfig.getPlacementConfig().hasLocationId()) {
                        location = getLocationUnitConfigRegistry().getMessage(unitConfig.getPlacementConfig().getLocationId());
                    } else {
                        location = getRootLocationConfig();
                    }

                    // check the authentication data include write permissions for the location
                    // this will throw an exception if it is not the case
                    AuthorizationWithTokenHelper.canDo(authenticationBaseData, location, PermissionType.WRITE, this);

                    // register the new unit config
                    return getUnitConfigRegistry(unitConfig.getUnitType()).register(unitConfig);
                }
        ));
    }

    @Override
    public UnitConfig getUnitConfigById(final String unitConfigId) throws NotAvailableException {
        for (ProtoBufFileSynchronizedRegistry registry : getRegistries()) {
            // filter to avoid useless and heavy lookups
            if (!registry.contains(unitConfigId)) {
                continue;
            }
            try {
                return (UnitConfig) registry.getMessage(unitConfigId);
            } catch (CouldNotPerformException ex) {
                throw new NotAvailableException("UnitConfigId", unitConfigId, new CouldNotPerformException("Lookup via " + registry.getName() + " of id [" + unitConfigId + "] failed!", ex));
            }
        }
        throw new NotAvailableException("UnitConfigId", unitConfigId, new CouldNotPerformException("None of the unit registries contains an entry with the id [" + unitConfigId + "]"));
    }

    /**
     * {@inheritDoc}
     *
     * @param unitAlias {@inheritDoc}
     *
     * @return {@inheritDoc}
     *
     * @throws NotAvailableException {@inheritDoc}
     */
    @Override
    public UnitConfig getUnitConfigByAlias(final String unitAlias) throws NotAvailableException {
        try {
            synchronized (aliasIdMapLock) {
                if (aliasIdMap.containsKey(unitAlias.toLowerCase())) {
                    return getUnitConfigById(aliasIdMap.get(unitAlias.toLowerCase()));
                }
            }
            throw new NotAvailableException("Alias", unitAlias);
        } catch (CouldNotPerformException ex) {
            throw new NotAvailableException("UnitConfig with Alias", unitAlias, ex);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param unitAlias {@inheritDoc}
     * @param unitType  {@inheritDoc}
     *
     * @return {@inheritDoc}
     *
     * @throws NotAvailableException {@inheritDoc}
     */
    @Override
    public UnitConfig getUnitConfigByAliasAndUnitType(final String unitAlias, final UnitType unitType) throws NotAvailableException {
        try {
            synchronized (aliasIdMapLock) {
                if (aliasIdMap.containsKey(unitAlias.toLowerCase())) {
                    return getUnitConfigByIdAndUnitType(aliasIdMap.get(unitAlias.toLowerCase()), unitType);
                }
            }
            throw new NotAvailableException("Alias", unitAlias);
        } catch (CouldNotPerformException ex) {
            throw new NotAvailableException("UnitConfig of UnitType[" + unitType.name() + "] with Alias", unitAlias, ex);
        }
    }

    @Override
    public Boolean containsUnitConfigById(final String unitConfigId) {
        for (ProtoBufFileSynchronizedRegistry registry : getRegistries()) {
            if (registry.contains(unitConfigId)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Boolean containsUnitConfig(final UnitConfig unitConfig) {
        return getUnitConfigRegistry(unitConfig.getUnitType()).contains(unitConfig);
    }

    @Override
    public Future<UnitConfig> updateUnitConfig(final UnitConfig unitConfig) {
        return GlobalCachedExecutorService.submit(() -> {
            UnitConfig result = getUnitConfigRegistry(unitConfig.getUnitType()).update(unitConfig);
            return result;
        });
    }

    @Override
    public Future<AuthenticatedValue> updateUnitConfigAuthenticated(final AuthenticatedValue authenticatedValue) {
        return GlobalCachedExecutorService.submit(() -> AuthenticatedServiceProcessor.authenticatedAction(authenticatedValue, UnitConfig.class, this,
                (unitConfig, authenticationBaseData) -> {
                    // verify write permissions for the old unit config
                    final UnitConfig old = getUnitConfigRegistry(unitConfig.getUnitType()).getMessage(unitConfig.getId());
                    AuthorizationWithTokenHelper.canDo(authenticationBaseData, old, PermissionType.WRITE, this);
                    return getUnitConfigRegistry(unitConfig.getUnitType()).update(unitConfig);
                }
        ));
    }

    @Override
    public Future<UnitConfig> removeUnitConfig(final UnitConfig unitConfig) {
        return GlobalCachedExecutorService.submit(() -> {
            UnitConfig result = getUnitConfigRegistry(unitConfig.getUnitType()).remove(unitConfig);
            return result;
        });
    }

    @Override
    public Future<AuthenticatedValue> removeUnitConfigAuthenticated(final AuthenticatedValue authenticatedValue) {
        return GlobalCachedExecutorService.submit(() -> AuthenticatedServiceProcessor.authenticatedAction(authenticatedValue, UnitConfig.class, this,
                (unitConfig, authenticationBaseData) -> {
                    // verify write permissions for the old unit config
                    final UnitConfig old = getUnitConfigRegistry(unitConfig.getUnitType()).getMessage(unitConfig.getId());
                    AuthorizationWithTokenHelper.canDo(authenticationBaseData, old, PermissionType.WRITE, this);
                    return getUnitConfigRegistry(unitConfig.getUnitType()).remove(unitConfig);
                }
        ));
    }

    /**
     * {@inheritDoc}
     *
     * @param filterDisabledUnits {@inheritDoc}
     *
     * @return {@inheritDoc}
     *
     * @throws CouldNotPerformException {@inheritDoc}
     * @throws NotAvailableException    {@inheritDoc}
     */
    @Override
    public List<UnitConfig> getUnitConfigsFiltered(boolean filterDisabledUnits) throws CouldNotPerformException, NotAvailableException {
        validateData();
        final List<UnitConfig> unitConfigs = new ArrayList<>();
        for (final ProtoBufFileSynchronizedRegistry<String, UnitConfig, UnitConfig.Builder, UnitRegistryData.Builder> unitConfigRegistry : unitConfigRegistryList) {
            for (UnitConfig unitConfig : unitConfigRegistry.getMessages()) {
                if (filterDisabledUnits && !UnitConfigProcessor.isEnabled(unitConfig)) {
                    continue;
                }

                unitConfigs.add(unitConfig);
            }
        }
        return unitConfigs;
    }

    /**
     * {@inheritDoc}
     *
     * @return {@inheritDoc}
     *
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
     *
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
    public Boolean isUnitConfigRegistryReadOnly() {
        return unitConfigRegistryList.stream().anyMatch((unitConfigRegistry) -> (unitConfigRegistry.isReadOnly()));
    }

    /**
     * {@inheritDoc}
     *
     * @return {@inheritDoc}
     */
    @Override
    public Boolean isUnitConfigRegistryConsistent() {
        return unitConfigRegistryList.stream().anyMatch((unitConfigRegistry) -> (unitConfigRegistry.isConsistent()));
    }

    /**
     * {@inheritDoc}
     *
     * @return
     */
    @Override
    public Boolean isUnitGroupConfigRegistryReadOnly() {
        return unitGroupUnitConfigRegistry.isReadOnly();
    }

    /**
     * {@inheritDoc}
     *
     * @return
     */
    @Override
    public Boolean isUnitGroupConfigRegistryConsistent() {
        return unitGroupUnitConfigRegistry.isConsistent();
    }

    /**
     * {@inheritDoc}
     *
     * @param serviceType
     *
     * @return
     *
     * @throws CouldNotPerformException
     */
    @Override
    public List<ServiceConfig> getServiceConfigsByServiceType(final ServiceType serviceType) throws CouldNotPerformException {
        List<ServiceConfig> serviceConfigs = new ArrayList<>();
        for (UnitConfig unitConfig : getUnitConfigs()) {
            for (ServiceConfig serviceConfig : unitConfig.getServiceConfigList()) {
                if (serviceConfig.getServiceDescription().getServiceType() == serviceType) {
                    serviceConfigs.add(serviceConfig);
                }
            }
        }
        return serviceConfigs;
    }

    /**
     * {@inheritDoc}
     *
     * @param unitType
     * @param serviceTypes
     *
     * @return
     *
     * @throws CouldNotPerformException
     */
    @Override
    public List<UnitConfig> getUnitConfigsByUnitTypeAndServiceTypes(final UnitType unitType, final List<ServiceType> serviceTypes) throws CouldNotPerformException {
        List<UnitConfig> unitConfigs = getUnitConfigsByUnitType(unitType);
        boolean foundServiceType;

        for (UnitConfig unitConfig : new ArrayList<>(unitConfigs)) {
            foundServiceType = false;
            for (ServiceType serviceType : serviceTypes) {
                for (ServiceConfig serviceConfig : unitConfig.getServiceConfigList()) {
                    if (serviceConfig.getServiceDescription().getServiceType() == serviceType) {
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

    public ProtoBufFileSynchronizedRegistry<String, UnitConfig, UnitConfig.Builder, UnitRegistryData.Builder> getGatewayUnitConfigRegistry() {
        return gatewayUnitConfigRegistry;
    }

    public ArrayList<ProtoBufFileSynchronizedRegistry<String, UnitConfig, UnitConfig.Builder, UnitRegistryData.Builder>> getUnitConfigRegistryList() {
        return unitConfigRegistryList;
    }

    @Override
    public Boolean isDalUnitConfigRegistryReadOnly() {
        return dalUnitConfigRegistry.isReadOnly();
    }

    @Override
    public Boolean isUserUnitRegistryReadOnly() {
        return userUnitConfigRegistry.isReadOnly();
    }

    @Override
    public Boolean isAuthorizationGroupUnitRegistryReadOnly() {
        return authorizationGroupUnitConfigRegistry.isReadOnly();
    }

    @Override
    public Boolean isDeviceUnitRegistryReadOnly() {
        return deviceUnitConfigRegistry.isReadOnly();
    }

    @Override
    public Boolean isUnitGroupUnitRegistryReadOnly() {
        return unitGroupUnitConfigRegistry.isReadOnly();
    }

    @Override
    public Boolean isLocationUnitRegistryReadOnly() {
        return locationUnitConfigRegistry.isReadOnly();
    }

    @Override
    public Boolean isConnectionUnitRegistryReadOnly() {
        return connectionUnitConfigRegistry.isReadOnly();
    }

    @Override
    public Boolean isAgentUnitRegistryReadOnly() {
        return agentUnitConfigRegistry.isReadOnly();
    }

    @Override
    public Boolean isAppUnitRegistryReadOnly() {
        return appUnitConfigRegistry.isReadOnly();
    }

    @Override
    public Boolean isSceneUnitRegistryReadOnly() {
        return sceneUnitConfigRegistry.isReadOnly();
    }

    @Override
    public Boolean isObjectUnitRegistryReadOnly() {
        return objectUnitConfigRegistry.isReadOnly();
    }

    @Override
    public Boolean isGatewayUnitRegistryReadOnly() {
        return gatewayUnitConfigRegistry.isReadOnly();
    }

    @Override
    public Boolean isDalUnitConfigRegistryConsistent() {
        return dalUnitConfigRegistry.isConsistent();
    }

    @Override
    public Boolean isUserUnitRegistryConsistent() {
        return userUnitConfigRegistry.isConsistent();
    }

    @Override
    public Boolean isAuthorizationGroupUnitRegistryConsistent() {
        return authorizationGroupUnitConfigRegistry.isConsistent();
    }

    @Override
    public Boolean isDeviceUnitRegistryConsistent() {
        return deviceUnitConfigRegistry.isConsistent();
    }

    @Override
    public Boolean isUnitGroupUnitRegistryConsistent() {
        return unitGroupUnitConfigRegistry.isConsistent();
    }

    @Override
    public Boolean isLocationUnitRegistryConsistent() {
        return locationUnitConfigRegistry.isConsistent();
    }

    @Override
    public Boolean isConnectionUnitRegistryConsistent() {
        return connectionUnitConfigRegistry.isConsistent();
    }

    @Override
    public Boolean isAgentUnitRegistryConsistent() {
        return agentUnitConfigRegistry.isConsistent();
    }

    @Override
    public Boolean isAppUnitRegistryConsistent() {
        return appUnitConfigRegistry.isConsistent();
    }

    @Override
    public Boolean isSceneUnitRegistryConsistent() {
        return sceneUnitConfigRegistry.isConsistent();
    }

    @Override
    public Boolean isObjectUnitRegistryConsistent() {
        return objectUnitConfigRegistry.isConsistent();
    }

    @Override
    public Boolean isGatewayUnitRegistryConsistent() {
        return gatewayUnitConfigRegistry.isConsistent();
    }

    @Override
    protected void registerRemoteRegistries() {
    }

    @Override
    public Shape getUnitShapeByUnitConfig(final UnitConfig unitConfig) throws NotAvailableException {
        return UnitShapeGenerator.generateUnitShape(unitConfig, this, CachedClassRegistryRemote.getRegistry());
    }

    @Override
    protected UnitRegistryData filterDataForUser(final UnitRegistryData.Builder dataBuilder, final UserClientPair userClientPair) throws CouldNotPerformException {
        // Create a filter which removes all unit configs from a list without read permissions to its location by the user
        final ListFilter<UnitConfig> readFilter = unitConfig -> {
            try {
                return !AuthorizationHelper.canRead(getUnitConfigById(unitConfig.getPlacementConfig().getLocationId()), userClientPair, authorizationGroupUnitConfigRegistry.getEntryMap(), locationUnitConfigRegistry.getEntryMap());
            } catch (CouldNotPerformException e) {
                // if id could not resolved, than we filter the element.
                return true;
            }
        };
        // Create a filter which removes unit ids if the user does not have access permissions for them
        final ListFilter<String> readFilterByUnitId = unitId -> {
            try {
                return !AuthorizationHelper.canRead(getUnitConfigById(unitId), userClientPair,
                        authorizationGroupUnitConfigRegistry.getEntryMap(),
                        locationUnitConfigRegistry.getEntryMap());
            } catch (CouldNotPerformException ex) {
                // this can happen if more than one unit are removed in rapid succession
                // if getUnitConfigById fails the unit should be filtered anyway
                return true;
            }
        };
        // iterate over all fields of unit registry data
        for (FieldDescriptor fieldDescriptor : dataBuilder.getAllFields().keySet()) {
            // only filter repeated fields
            if (!fieldDescriptor.isRepeated()) {
                continue;
            }

            // only filter fields of type UnitConfig
            if (!fieldDescriptor.getMessageType().getName().equals(UnitConfig.getDescriptor().getName())) {
                continue;
            }

            // copy list, filter it and set as new list for the field
            dataBuilder.setField(fieldDescriptor, readFilter.filter(new ArrayList<>((List<UnitConfig>) dataBuilder.getField(fieldDescriptor))));
        }

        // for all locations which are left filter unit ids and child ids
        for (final UnitConfig.Builder locationUnitConfig : dataBuilder.getLocationUnitConfigBuilderList()) {
            final LocationConfig.Builder locationConfig = locationUnitConfig.getLocationConfigBuilder();
            final List<String> filteredUnitIdList = readFilterByUnitId.filter(new ArrayList<>(locationConfig.getUnitIdList()));
            locationConfig.clearUnitId();
            locationConfig.addAllUnitId(filteredUnitIdList);

            final List<String> filteredChildIdList = readFilterByUnitId.filter(new ArrayList<>(locationConfig.getChildIdList()));
            locationConfig.clearChildId();
            locationConfig.addAllChildId(filteredChildIdList);
        }

        // for all connections which are left filter unit ids
        for (final UnitConfig.Builder connectionUnitConfig : dataBuilder.getConnectionUnitConfigBuilderList()) {
            final ConnectionConfig.Builder connectionConfig = connectionUnitConfig.getConnectionConfigBuilder();
            final List<String> filteredUnitIdList = readFilterByUnitId.filter(new ArrayList<>(connectionConfig.getUnitIdList()));
            connectionConfig.clearUnitId();
            connectionConfig.addAllUnitId(filteredUnitIdList);
        }

//        logger.warn("Filtering data for user[" + userId + "] took: " + stopwatch.stop());
        return dataBuilder.build();
    }

    /**
     * {@inheritDoc}
     *
     * @param authorizationToken {@inheritDoc}
     *
     * @return {@inheritDoc}
     */
    @Override
    public Future<String> requestAuthorizationToken(final AuthorizationToken authorizationToken) {
        return GlobalCachedExecutorService.submit(() -> {
            // verify that the user has all permissions he defined in the token
            AuthorizationWithTokenHelper.verifyAuthorizationToken(authorizationToken, this);

            // the requested authorization token is valid, so encrypt it with the service server secret key and return it
            final ByteString encrypted = EncryptionHelper.encryptSymmetric(authorizationToken, AuthenticatedServerManager.getInstance().getServiceServerSecretKey());
            return Base64.getEncoder().encodeToString(encrypted.toByteArray());
        });
    }

    /**
     * {@inheritDoc}
     *
     * @param authenticatedValue {@inheritDoc}
     *
     * @return {@inheritDoc}
     */
    @Override
    public Future<AuthenticatedValue> requestAuthorizationTokenAuthenticated(final AuthenticatedValue authenticatedValue) {
        return GlobalCachedExecutorService.submit(() -> {
            try {
                if (!authenticatedValue.hasTicketAuthenticatorWrapper()) {
                    throw new NotAvailableException("TicketAuthenticatorWrapper");
                }

                final AuthenticatedValue.Builder response = AuthenticatedValue.newBuilder();
                Future<String> internalFuture = null;
                try {
                    // evaluate the users ticket
                    final AuthenticationBaseData authenticationBaseData = AuthenticatedServerManager.getInstance().verifyClientServerTicket(authenticatedValue.getTicketAuthenticatorWrapper());

                    // decrypt authorization token
                    final AuthorizationToken.Builder authorizationToken =
                            EncryptionHelper.decryptSymmetric(authenticatedValue.getValue(), authenticationBaseData.getSessionKey(), AuthorizationToken.class).toBuilder();

                    // validate that user in token matches the authenticated user, and when not available set it
                    if (authorizationToken.hasUserId() && !authorizationToken.getUserId().isEmpty()) {
                        if (!authorizationToken.getUserId().equals(authenticationBaseData.getUserClientPair().getUserId())) {
                            //TODO: maybe this should be possible for admins
                            throw new RejectedException("Authorized user[" + authenticationBaseData.getUserClientPair().getUserId() + "] cannot request a token for another user");
                        }
                    } else {
                        authorizationToken.setUserId(authenticationBaseData.getAuthenticationToken().getUserId());
                    }

                    internalFuture = requestAuthorizationToken(authorizationToken.build());

                    response.setTicketAuthenticatorWrapper(authenticationBaseData.getTicketAuthenticatorWrapper());
                    response.setValue(EncryptionHelper.encryptSymmetric(internalFuture.get(), authenticationBaseData.getSessionKey()));
                    return response.build();
                } catch (InterruptedException ex) {
                    if (internalFuture != null && !internalFuture.isDone()) {
                        internalFuture.cancel(true);
                    }
                    Thread.currentThread().interrupt();
                    throw new CouldNotPerformException("Interrupted while verifying and encrypting authorizationToken", ex);
                }
            } catch (ExecutionException | CouldNotPerformException ex) {
                throw new CouldNotPerformException("Could not verify and encrypt authorizationToken", ex);
            }
        });
    }

    @Override
    public Future<String> requestAuthenticationToken(final AuthenticationToken authenticationToken) {
        return GlobalCachedExecutorService.submit(() -> {
            // encrypt the authentication token
            final ByteString encrypted = EncryptionHelper.encryptSymmetric(authenticationToken, AuthenticatedServerManager.getInstance().getServiceServerSecretKey());
            // encode using base 64
            return Base64.getEncoder().encodeToString(encrypted.toByteArray());
        });
    }

    @Override
    public Future<AuthenticatedValue> requestAuthenticationTokenAuthenticated(AuthenticatedValue authenticatedValue) {
        return GlobalCachedExecutorService.submit(() -> {
            try {
                if (!authenticatedValue.hasTicketAuthenticatorWrapper()) {
                    throw new NotAvailableException("TicketAuthenticatorWrapper");
                }

                final AuthenticatedValue.Builder response = AuthenticatedValue.newBuilder();
                Future<String> internalFuture = null;
                try {
                    // evaluate the users ticket
                    final AuthenticationBaseData authenticationBaseData = AuthenticatedServerManager.getInstance().verifyClientServerTicket(authenticatedValue.getTicketAuthenticatorWrapper());

                    // decrypt authorization token
                    final AuthenticationToken.Builder authenticationToken =
                            EncryptionHelper.decryptSymmetric(authenticatedValue.getValue(), authenticationBaseData.getSessionKey(), AuthenticationToken.class).toBuilder();

                    // validate that user in token matches the authenticated user, and when not available set it
                    if (authenticationToken.hasUserId() && !authenticationToken.getUserId().isEmpty()) {
                        if (!authenticationToken.getUserId().equals(authenticationBaseData.getUserClientPair().getUserId())) {
                            //TODO: maybe this should be possible for admins
                            throw new RejectedException("Authorized user[" + authenticationBaseData.getUserClientPair().getUserId() + "] cannot request a token for another user");
                        }
                    } else {
                        authenticationToken.setUserId(authenticationBaseData.getUserClientPair().getUserId());
                    }

                    internalFuture = requestAuthenticationToken(authenticationToken.build());

                    response.setTicketAuthenticatorWrapper(authenticationBaseData.getTicketAuthenticatorWrapper());
                    response.setValue(EncryptionHelper.encryptSymmetric(internalFuture.get(), authenticationBaseData.getSessionKey()));
                    return response.build();
                } catch (InterruptedException ex) {
                    if (internalFuture != null && !internalFuture.isDone()) {
                        internalFuture.cancel(true);
                    }
                    Thread.currentThread().interrupt();
                    throw new CouldNotPerformException("Interrupted while verifying and encrypting authenticationToken", ex);
                }
            } catch (ExecutionException | CouldNotPerformException ex) {
                throw new CouldNotPerformException("Could not verify and encrypt authorizationToken", ex);
            }
        });
    }

    @Override
    public Map<String, IdentifiableMessage<String, UnitConfig, Builder>> getAuthorizationGroupMap() throws CouldNotPerformException {
        return authorizationGroupUnitConfigRegistry.getEntryMap();
    }

    @Override
    public Map<String, IdentifiableMessage<String, UnitConfig, Builder>> getLocationMap() throws CouldNotPerformException {
        return locationUnitConfigRegistry.getEntryMap();
    }
}
