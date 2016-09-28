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
import org.openbase.bco.registry.scene.lib.jp.JPUnitConfigDatabaseDirectory;
import org.openbase.bco.registry.unit.core.consistency.unittemplate.UnitTemplateValidationConsistencyHandler;
import org.openbase.bco.registry.unit.core.dbconvert.DummyConverter;
import org.openbase.bco.registry.unit.core.plugin.UnitTemplateCreatorRegistryPlugin;
import org.openbase.bco.registry.unit.lib.UnitRegistry;
import org.openbase.bco.registry.unit.lib.generator.UnitConfigIdGenerator;
import org.openbase.bco.registry.unit.lib.generator.UnitTemplateIdGenerator;
import org.openbase.bco.registry.unit.lib.jp.JPUnitRegistryScope;
import org.openbase.bco.registry.unit.lib.jp.JPUnitTemplateDatabaseDirectory;
import org.openbase.jps.core.JPService;
import org.openbase.jps.exception.JPServiceException;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InitializationException;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.exception.printer.LogLevel;
import org.openbase.jul.extension.protobuf.IdentifiableMessage;
import org.openbase.jul.extension.rsb.com.RPCHelper;
import org.openbase.jul.extension.rsb.com.RSBCommunicationService;
import org.openbase.jul.extension.rsb.iface.RSBLocalServer;
import org.openbase.jul.iface.Manageable;
import org.openbase.jul.pattern.Observable;
import org.openbase.jul.pattern.Observer;
import org.openbase.jul.schedule.GlobalExecutionService;
import org.openbase.jul.storage.file.ProtoBufJSonFileProvider;
import org.openbase.jul.storage.registry.ProtoBufFileSynchronizedRegistry;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rst.authorization.AuthorizationGroupConfigType;
import rst.authorization.UserConfigType;
import rst.authorization.UserRegistryDataType;
import rst.homeautomation.device.DeviceClassType;
import rst.homeautomation.device.DeviceConfigType;
import rst.homeautomation.device.DeviceRegistryDataType;
import rst.homeautomation.unit.UnitConfigType.UnitConfig;
import rst.homeautomation.unit.UnitGroupConfigType;
import rst.homeautomation.unit.UnitRegistryDataType.UnitRegistryData;
import rst.homeautomation.unit.UnitTemplateType.UnitTemplate;
import rst.homeautomation.unit.UnitTemplateType.UnitTemplate.UnitType;
import rst.rsb.ScopeType;

/**
 *
 * @author mpohling
 */
public class UnitRegistryController extends RSBCommunicationService<UnitRegistryData, UnitRegistryData.Builder> implements UnitRegistry, Manageable<ScopeType.Scope> {

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
    }

    private ProtoBufFileSynchronizedRegistry<String, UnitConfig, UnitConfig.Builder, UnitRegistryData.Builder> unitConfigRegistry;
    private ProtoBufFileSynchronizedRegistry<String, UnitTemplate, UnitTemplate.Builder, UnitRegistryData.Builder> unitTemplateRegistry;
    private ProtoBufFileSynchronizedRegistry<String, UserConfigType.UserConfig, UserConfigType.UserConfig.Builder, UserRegistryDataType.UserRegistryData.Builder> userRegistry;
    private ProtoBufFileSynchronizedRegistry<String, AuthorizationGroupConfigType.AuthorizationGroupConfig, AuthorizationGroupConfigType.AuthorizationGroupConfig.Builder, UserRegistryDataType.UserRegistryData.Builder> authorizationGroupRegistry;
    private ProtoBufFileSynchronizedRegistry<String, DeviceConfigType.DeviceConfig, DeviceConfigType.DeviceConfig.Builder, DeviceRegistryDataType.DeviceRegistryData.Builder> deviceConfigRegistry;
    private ProtoBufFileSynchronizedRegistry<String, UnitGroupConfigType.UnitGroupConfig, UnitGroupConfigType.UnitGroupConfig.Builder, DeviceRegistryDataType.DeviceRegistryData.Builder> unitGroupConfigRegistry;

    public UnitRegistryController() throws InstantiationException, InterruptedException {
        super(UnitRegistryData.newBuilder());
        try {
            ProtoBufJSonFileProvider protoBufJSonFileProvider = new ProtoBufJSonFileProvider();
            unitConfigRegistry = new ProtoBufFileSynchronizedRegistry<>(UnitConfig.class, getBuilderSetup(), getDataFieldDescriptor(UnitRegistryData.UNIT_CONFIG_FIELD_NUMBER), new UnitConfigIdGenerator(), JPService.getProperty(JPUnitConfigDatabaseDirectory.class).getValue(), protoBufJSonFileProvider);
            unitTemplateRegistry = new ProtoBufFileSynchronizedRegistry<>(UnitTemplate.class, getBuilderSetup(), getDataFieldDescriptor(UnitRegistryData.UNIT_TEMPLATE_FIELD_NUMBER), new UnitTemplateIdGenerator(), JPService.getProperty(JPUnitTemplateDatabaseDirectory.class).getValue(), protoBufJSonFileProvider);
            userRegistry = new ProtoBufFileSynchronizedRegistry<>(UserConfigType.UserConfig.class, getBuilderSetup(), getDataFieldDescriptor(UserRegistryDataType.UserRegistryData.USER_CONFIG_FIELD_NUMBER), new UserConfigIdGenerator(), JPService.getProperty(JPUserConfigDatabaseDirectory.class).getValue(), protoBufJSonFileProvider);
            authorizationGroupRegistry = new ProtoBufFileSynchronizedRegistry<>(AuthorizationGroupConfigType.AuthorizationGroupConfig.class, getBuilderSetup(), getDataFieldDescriptor(UserRegistryDataType.UserRegistryData.AUTHORIZATION_GROUP_CONFIG_FIELD_NUMBER), new AuthorizationGroupConfigIdGenerator(), JPService.getProperty(JPAuthorizationGroupConfigDatabaseDirectory.class).getValue(), protoBufJSonFileProvider);
            deviceConfigRegistry = new ProtoBufFileSynchronizedRegistry<>(DeviceConfigType.DeviceConfig.class, getBuilderSetup(), getDataFieldDescriptor(DeviceRegistryDataType.DeviceRegistryData.DEVICE_CONFIG_FIELD_NUMBER), new DeviceConfigIdGenerator(), JPService.getProperty(JPDeviceConfigDatabaseDirectory.class).getValue(), protoBufJSonFileProvider);
            unitGroupConfigRegistry = new ProtoBufFileSynchronizedRegistry<>(UnitGroupConfigType.UnitGroupConfig.class, getBuilderSetup(), getDataFieldDescriptor(DeviceRegistryDataType.DeviceRegistryData.UNIT_GROUP_CONFIG_FIELD_NUMBER), new UnitGroupIdGenerator(), JPService.getProperty(JPUnitGroupDatabaseDirectory.class).getValue(), protoBufJSonFileProvider);
        } catch (JPServiceException ex) {
            throw new InstantiationException(this, ex);
        }
    }

    public void init() throws InitializationException, InterruptedException {
        try {
            super.init(JPService.getProperty(JPUnitRegistryScope.class).getValue());
        } catch (JPServiceException ex) {
            throw new InitializationException(this, ex);
        }
    }

    @Override
    protected void activateVersionControl() throws CouldNotPerformException {
        unitConfigRegistry.activateVersionControl(DummyConverter.class.getPackage());
        userRegistry.activateVersionControl(DummyConverter.class.getPackage());
        authorizationGroupRegistry.activateVersionControl(DummyConverter.class.getPackage());
        deviceConfigRegistry.activateVersionControl(DeviceConfig_0_To_1_DBConverter.class.getPackage());
        unitTemplateRegistry.activateVersionControl(UnitTemplate_0_To_1_DBConverter.class.getPackage());
        unitGroupConfigRegistry.activateVersionControl(UnitGroupConfig_0_To_1_DBConverter.class.getPackage());
    }

    @Override
    protected void loadInternalRegistries() throws CouldNotPerformException {
        unitTemplateRegistry.loadRegistry();
        unitConfigRegistry.loadRegistry();
        userRegistry.loadRegistry();
        authorizationGroupRegistry.loadRegistry();
        unitTemplateRegistry.loadRegistry();
        deviceConfigRegistry.loadRegistry();
        unitGroupConfigRegistry.loadRegistry();
    }

    @Override
    protected void registerConsistencyHandler() {
        unitTemplateRegistry.registerConsistencyHandler(new UnitTemplateValidationConsistencyHandler(unitTemplateRegistry));
        unitTemplateRegistry.registerPlugin(new UnitTemplateCreatorRegistryPlugin(unitTemplateRegistry));
        userRegistry.registerConsistencyHandler(new UserConfigUserNameConsistencyHandler());
        userRegistry.registerConsistencyHandler(new UserConfigScopeConsistencyHandler());
        authorizationGroupRegistry.registerConsistencyHandler(new AuthorizationGroupConfigLabelConsistencyHandler());
        authorizationGroupRegistry.registerConsistencyHandler(new AuthorizationGroupConfigScopeConsistencyHandler());
        deviceConfigRegistry.registerConsistencyHandler(new DeviceIdConsistencyHandler());
        deviceConfigRegistry.registerConsistencyHandler(new DeviceConfigDeviceClassIdConsistencyHandler(deviceClassRegistry));
        deviceConfigRegistry.registerConsistencyHandler(new DeviceLabelConsistencyHandler());
        deviceConfigRegistry.registerConsistencyHandler(new DeviceLocationIdConsistencyHandler(locationRegistryRemote));
        deviceConfigRegistry.registerConsistencyHandler(new DeviceOwnerConsistencyHandler(userRegistryRemote));
        deviceConfigRegistry.registerConsistencyHandler(new DeviceScopeConsistencyHandler(locationRegistryRemote));
        deviceConfigRegistry.registerConsistencyHandler(new DeviceTransformationFrameConsistencyHandler(locationRegistryRemote.getLocationConfigRemoteRegistry()));

        deviceConfigRegistry.registerConsistencyHandler(new UnitScopeConsistencyHandler(locationRegistryRemote));
        deviceConfigRegistry.registerConsistencyHandler(new UnitIdConsistencyHandler());
        deviceConfigRegistry.registerConsistencyHandler(new UnitBoundsToDeviceConsistencyHandler(deviceClassRegistry));
        deviceConfigRegistry.registerConsistencyHandler(new UnitLabelConsistencyHandler(deviceClassRegistry));
        deviceConfigRegistry.registerConsistencyHandler(new UnitLocationIdConsistencyHandler(locationRegistryRemote));
        deviceConfigRegistry.registerConsistencyHandler(new UnitTransformationFrameConsistencyHandler(locationRegistryRemote.getLocationConfigRemoteRegistry()));
        deviceConfigRegistry.registerConsistencyHandler(new ServiceConfigUnitIdConsistencyHandler());
        deviceConfigRegistry.registerConsistencyHandler(new ServiceConfigBindingTypeConsistencyHandler(deviceClassRegistry));
        deviceConfigRegistry.registerConsistencyHandler(new OpenhabServiceConfigItemIdConsistencyHandler(locationRegistryRemote, deviceClassRegistry));
        deviceConfigRegistry.registerConsistencyHandler(new UnitConfigUnitTemplateConsistencyHandler(unitTemplateRegistry));
        deviceConfigRegistry.registerConsistencyHandler(new UnitConfigUnitTemplateConfigIdConsistencyHandler(deviceClassRegistry));
        deviceConfigRegistry.registerConsistencyHandler(new DeviceConfigDeviceClassUnitConsistencyHandler(deviceClassRegistry));
        deviceConfigRegistry.registerConsistencyHandler(new DeviceConfigLocationIdForInstalledDevicesConsistencyHandler());

        unitTemplateRegistry.registerConsistencyHandler(new UnitTemplateValidationConsistencyHandler(unitTemplateRegistry));
        unitTemplateRegistry.registerPlugin(new UnitTemplateCreatorRegistryPlugin(unitTemplateRegistry));

        unitGroupConfigRegistry.registerConsistencyHandler(new UnitGroupMemberListDuplicationConsistencyHandler());
        unitGroupConfigRegistry.registerConsistencyHandler(new UnitGroupMemberExistsConsistencyHandler(deviceConfigRegistry));
        unitGroupConfigRegistry.registerConsistencyHandler(new UnitGroupUnitTypeConsistencyHandler(unitTemplateRegistry));
        unitGroupConfigRegistry.registerConsistencyHandler(new UnitGroupMemberListTypesConsistencyHandler(deviceConfigRegistry, unitTemplateRegistry));
        unitGroupConfigRegistry.registerConsistencyHandler(new UnitGroupScopeConsistencyHandler(locationRegistryRemote));
    }

    @Override
    protected void registerObserver() {
        unitConfigRegistry.addObserver((Observable<Map<String, IdentifiableMessage<String, UnitConfig, UnitConfig.Builder>>> source, Map<String, IdentifiableMessage<String, UnitConfig, UnitConfig.Builder>> data) -> {
            notifyChange();
        });
        unitTemplateRegistry.addObserver((Observable<Map<String, IdentifiableMessage<String, UnitTemplate, UnitTemplate.Builder>>> source, Map<String, IdentifiableMessage<String, UnitTemplate, UnitTemplate.Builder>> data) -> {
            notifyChange();
        });

        userRegistry.addObserver(new Observer<Map<String, IdentifiableMessage<String, UserConfigType.UserConfig, UserConfigType.UserConfig.Builder>>>() {

            @Override
            public void update(final Observable<Map<String, IdentifiableMessage<String, UserConfigType.UserConfig, UserConfigType.UserConfig.Builder>>> source, Map<String, IdentifiableMessage<String, UserConfigType.UserConfig, UserConfigType.UserConfig.Builder>> data) throws Exception {
                notifyChange();
            }
        });

        authorizationGroupRegistry.addObserver(new Observer<Map<String, IdentifiableMessage<String, AuthorizationGroupConfigType.AuthorizationGroupConfig, AuthorizationGroupConfigType.AuthorizationGroupConfig.Builder>>>() {

            @Override
            public void update(final Observable<Map<String, IdentifiableMessage<String, AuthorizationGroupConfigType.AuthorizationGroupConfig, AuthorizationGroupConfigType.AuthorizationGroupConfig.Builder>>> source, Map<String, IdentifiableMessage<String, AuthorizationGroupConfigType.AuthorizationGroupConfig, AuthorizationGroupConfigType.AuthorizationGroupConfig.Builder>> data) throws Exception {
                notifyChange();
            }
        });

        unitTemplateRegistry.addObserver((Observable<Map<String, IdentifiableMessage<String, UnitTemplate, UnitTemplate.Builder>>> source, Map<String, IdentifiableMessage<String, UnitTemplate, UnitTemplate.Builder>> data) -> {
            notifyChange();
        });

        deviceConfigRegistry.addObserver((Observable<Map<String, IdentifiableMessage<String, DeviceConfigType.DeviceConfig, DeviceConfigType.DeviceConfig.Builder>>> source, Map<String, IdentifiableMessage<String, DeviceConfigType.DeviceConfig, DeviceConfigType.DeviceConfig.Builder>> data) -> {
            notifyChange();
        });

        unitGroupConfigRegistry.addObserver((Observable<Map<String, IdentifiableMessage<String, UnitGroupConfigType.UnitGroupConfig, UnitGroupConfigType.UnitGroupConfig.Builder>>> source, Map<String, IdentifiableMessage<String, UnitGroupConfigType.UnitGroupConfig, UnitGroupConfigType.UnitGroupConfig.Builder>> data) -> {
            notifyChange();
        });
    }

    @Override
    protected void registerDependencies() throws CouldNotPerformException {
        unitConfigRegistry.registerDependency(unitTemplateRegistry);
        authorizationGroupRegistry.registerDependency(userRegistry);
        deviceConfigRegistry.registerDependency(locationRegistryRemote.getLocationConfigRemoteRegistry());
        deviceConfigRegistry.registerDependency(userRegistryRemote.getUserConfigRemoteRegistry());
        deviceConfigRegistry.registerDependency(deviceClassRegistry);
        unitGroupConfigRegistry.registerDependency(deviceConfigRegistry);
    }

    @Override
    protected void removeDependencies() throws CouldNotPerformException {
        unitConfigRegistry.removeDependency(unitTemplateRegistry);
        authorizationGroupRegistry.removeDependency(userRegistry);
        deviceConfigRegistry.removeDependency(locationRegistryRemote.getLocationConfigRemoteRegistry());
        deviceConfigRegistry.removeDependency(userRegistryRemote.getUserConfigRemoteRegistry());
        deviceConfigRegistry.removeDependency(deviceClassRegistry);
        unitGroupConfigRegistry.removeDependency(deviceConfigRegistry);
    }

    @Override
    protected void performInitialConsistencyCheck() throws CouldNotPerformException {
        try {
            unitTemplateRegistry.checkConsistency();
        } catch (CouldNotPerformException ex) {
            ExceptionPrinter.printHistory(new CouldNotPerformException("Initial consistency check failed!", ex), logger, LogLevel.WARN);
            notifyChange();
        }

        try {
            unitConfigRegistry.checkConsistency();
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
            deviceConfigRegistry.checkConsistency();
        } catch (CouldNotPerformException ex) {
            ExceptionPrinter.printHistory(new CouldNotPerformException("Initial consistency check failed!", ex), logger, LogLevel.WARN);
            notifyChange();
        }

        try {
            unitGroupConfigRegistry.checkConsistency();
        } catch (CouldNotPerformException ex) {
            ExceptionPrinter.printHistory(new CouldNotPerformException("Initial consistency check failed!", ex), logger, LogLevel.WARN);
            notifyChange();
        }

        try {
            deviceConfigRegistry.registerPlugin(new PublishDeviceTransformationRegistryPlugin(locationRegistryRemote));
        } catch (CouldNotPerformException ex) {
            ExceptionPrinter.printHistory(new CouldNotPerformException("Could not load all plugins!", ex), logger, LogLevel.ERROR);
            notifyChange();
        }
    }

    @Override
    public void activate() throws InterruptedException, CouldNotPerformException {
        try {
            super.activate();
        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not activate unit registry!", ex);
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
        if (unitConfigRegistry != null) {
            unitConfigRegistry.shutdown();
        }

        if (unitTemplateRegistry != null) {
            unitTemplateRegistry.shutdown();
        }
        
        if (deviceConfigRegistry != null) {
            deviceConfigRegistry.shutdown();
        }

        if (unitTemplateRegistry != null) {
            unitTemplateRegistry.shutdown();
        }

        if (unitGroupConfigRegistry != null) {
            unitGroupConfigRegistry.shutdown();
        }

        try {
            deactivate();
        } catch (CouldNotPerformException | InterruptedException ex) {
            ExceptionPrinter.printHistory(ex, logger);
        }
    }

    @Override
    public final void notifyChange() throws CouldNotPerformException, InterruptedException {
        // sync flags
        setDataField(UnitRegistryData.UNIT_CONFIG_REGISTRY_READ_ONLY_FIELD_NUMBER, unitConfigRegistry.isReadOnly());
        setDataField(UnitRegistryData.UNIT_CONFIG_REGISTRY_CONSISTENT_FIELD_NUMBER, unitConfigRegistry.isConsistent());

        setDataField(UnitRegistryData.UNIT_TEMPLATE_REGISTRY_READ_ONLY_FIELD_NUMBER, unitTemplateRegistry.isReadOnly());
        setDataField(UnitRegistryData.UNIT_TEMPLATE_REGISTRY_CONSISTENT_FIELD_NUMBER, unitTemplateRegistry.isConsistent());

        setDataField(UserRegistryDataType.UserRegistryData.USER_CONFIG_REGISTRY_READ_ONLY_FIELD_NUMBER, userRegistry.isReadOnly());
        setDataField(UserRegistryDataType.UserRegistryData.AUTHORIZATION_GROUP_CONFIG_REGISTRY_READ_ONLY_FIELD_NUMBER, authorizationGroupRegistry.isReadOnly());
        setDataField(UserRegistryDataType.UserRegistryData.USER_CONFIG_REGISTRY_CONSISTENT_FIELD_NUMBER, userRegistry.isConsistent());
        setDataField(UserRegistryDataType.UserRegistryData.AUTHORIZATION_GROUP_CONFIG_REGISTRY_CONSISTENT_FIELD_NUMBER, authorizationGroupRegistry.isConsistent());
        
        setDataField(DeviceRegistryDataType.DeviceRegistryData.DEVICE_CONFIG_REGISTRY_READ_ONLY_FIELD_NUMBER, deviceConfigRegistry.isReadOnly());
        setDataField(DeviceRegistryDataType.DeviceRegistryData.UNIT_TEMPLATE_REGISTRY_READ_ONLY_FIELD_NUMBER, unitTemplateRegistry.isReadOnly());
        setDataField(DeviceRegistryDataType.DeviceRegistryData.UNIT_GROUP_REGISTRY_READ_ONLY_FIELD_NUMBER, unitGroupConfigRegistry.isReadOnly());
        setDataField(DeviceRegistryDataType.DeviceRegistryData.DEVICE_CONFIG_REGISTRY_CONSISTENT_FIELD_NUMBER, deviceConfigRegistry.isConsistent());
        setDataField(DeviceRegistryDataType.DeviceRegistryData.UNIT_TEMPLATE_REGISTRY_CONSISTENT_FIELD_NUMBER, unitTemplateRegistry.isConsistent());
        setDataField(DeviceRegistryDataType.DeviceRegistryData.UNIT_GROUP_REGISTRY_CONSISTENT_FIELD_NUMBER, unitGroupConfigRegistry.isConsistent());

        super.notifyChange();
    }

    @Override
    public void registerMethods(final RSBLocalServer server) throws CouldNotPerformException {
        RPCHelper.registerInterface(UnitRegistry.class, this, server);
    }

    @Override
    public Future<UnitConfig> registerUnitConfig(UnitConfig unitConfig) throws CouldNotPerformException {
        return GlobalExecutionService.submit(() -> unitConfigRegistry.register(unitConfig));
    }

    @Override
    public UnitConfig getUnitConfigById(String unitConfigId) throws CouldNotPerformException {
        return unitConfigRegistry.get(unitConfigId).getMessage();
    }

    @Override
    public Boolean containsUnitConfigById(String sceneConfigId) throws CouldNotPerformException {
        return unitConfigRegistry.contains(sceneConfigId);
    }

    @Override
    public Boolean containsUnitConfig(UnitConfig unitConfig) throws CouldNotPerformException {
        return unitConfigRegistry.contains(unitConfig);
    }

    @Override
    public Future<UnitConfig> updateUnitConfig(UnitConfig unitConfig) throws CouldNotPerformException {
        return GlobalExecutionService.submit(() -> unitConfigRegistry.update(unitConfig));
    }

    @Override
    public Future<UnitConfig> removeUnitConfig(UnitConfig unitConfig) throws CouldNotPerformException {
        return GlobalExecutionService.submit(() -> unitConfigRegistry.remove(unitConfig));
    }

    @Override
    public List<UnitConfig> getUnitConfigs() throws CouldNotPerformException {
        return unitConfigRegistry.getMessages();
    }

    @Override
    public Boolean isUnitConfigRegistryReadOnly() throws CouldNotPerformException {
        return unitConfigRegistry.isReadOnly();
    }

    public ProtoBufFileSynchronizedRegistry<String, UnitConfig, UnitConfig.Builder, UnitRegistryData.Builder> getUnitConfigRegistry() {
        return unitConfigRegistry;
    }

    /**
     * {@inheritDoc}
     *
     * @return {@inheritDoc}
     * @throws CouldNotPerformException {@inheritDoc}
     */
    @Override
    public Boolean isUnitConfigRegistryConsistent() throws CouldNotPerformException {
        return unitConfigRegistry.isConsistent();
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
