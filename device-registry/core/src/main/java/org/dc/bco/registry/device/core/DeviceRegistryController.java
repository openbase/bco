package org.dc.bco.registry.device.core;

/*
 * #%L
 * REM DeviceRegistry Core
 * %%
 * Copyright (C) 2014 - 2016 DivineCooperation
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
import java.util.Map;
import java.util.concurrent.Future;
import org.dc.bco.registry.device.core.consistency.DeviceConfigDeviceClassIdConsistencyHandler;
import org.dc.bco.registry.device.core.consistency.DeviceConfigDeviceClassUnitConsistencyHandler;
import org.dc.bco.registry.device.core.consistency.DeviceConfigLocationIdForInstalledDevicesConsistencyHandler;
import org.dc.bco.registry.device.core.consistency.DeviceIdConsistencyHandler;
import org.dc.bco.registry.device.core.consistency.DeviceLabelConsistencyHandler;
import org.dc.bco.registry.device.core.consistency.DeviceLocationIdConsistencyHandler;
import org.dc.bco.registry.device.core.consistency.DeviceOwnerConsistencyHandler;
import org.dc.bco.registry.device.core.consistency.DeviceScopeConsistencyHandler;
import org.dc.bco.registry.device.core.consistency.DeviceTransformationFrameConsistencyHandler;
import org.dc.bco.registry.device.core.consistency.OpenhabServiceConfigItemIdConsistencyHandler;
import org.dc.bco.registry.device.core.consistency.ServiceConfigBindingTypeConsistencyHandler;
import org.dc.bco.registry.device.core.consistency.ServiceConfigUnitIdConsistencyHandler;
import org.dc.bco.registry.device.core.consistency.UnitBoundsToDeviceConsistencyHandler;
import org.dc.bco.registry.device.core.consistency.UnitConfigUnitTemplateConfigIdConsistencyHandler;
import org.dc.bco.registry.device.core.consistency.UnitConfigUnitTemplateConsistencyHandler;
import org.dc.bco.registry.device.core.consistency.UnitGroupMemberExistsConsistencyHandler;
import org.dc.bco.registry.device.core.consistency.UnitGroupMemberListDuplicationConsistencyHandler;
import org.dc.bco.registry.device.core.consistency.UnitGroupMemberListTypesConsistencyHandler;
import org.dc.bco.registry.device.core.consistency.UnitGroupUnitTypeConsistencyHandler;
import org.dc.bco.registry.device.core.consistency.UnitIdConsistencyHandler;
import org.dc.bco.registry.device.core.consistency.UnitLabelConsistencyHandler;
import org.dc.bco.registry.device.core.consistency.UnitLocationIdConsistencyHandler;
import org.dc.bco.registry.device.core.consistency.UnitScopeConsistencyHandler;
import org.dc.bco.registry.device.core.consistency.UnitTemplateConfigIdConsistencyHandler;
import org.dc.bco.registry.device.core.consistency.UnitTemplateConfigLabelConsistencyHandler;
import org.dc.bco.registry.device.core.consistency.UnitTemplateValidationConsistencyHandler;
import org.dc.bco.registry.device.core.consistency.UnitTransformationFrameConsistencyHandler;
import org.dc.bco.registry.device.core.dbconvert.DeviceConfig_0_To_1_DBConverter;
import org.dc.bco.registry.device.core.plugin.PublishDeviceTransformationRegistryPlugin;
import org.dc.bco.registry.device.core.plugin.UnitTemplateCreatorRegistryPlugin;
import org.dc.bco.registry.device.lib.generator.DeviceClassIdGenerator;
import org.dc.bco.registry.device.lib.generator.DeviceConfigIdGenerator;
import org.dc.bco.registry.device.lib.generator.UnitGroupIdGenerator;
import org.dc.bco.registry.device.lib.generator.UnitTemplateIdGenerator;
import org.dc.bco.registry.device.lib.jp.JPDeviceClassDatabaseDirectory;
import org.dc.bco.registry.device.lib.jp.JPDeviceConfigDatabaseDirectory;
import org.dc.bco.registry.device.lib.jp.JPDeviceRegistryScope;
import org.dc.bco.registry.device.lib.jp.JPUnitGroupDatabaseDirectory;
import org.dc.bco.registry.device.lib.jp.JPUnitTemplateDatabaseDirectory;
import org.dc.bco.registry.location.remote.LocationRegistryRemote;
import org.dc.bco.registry.user.remote.UserRegistryRemote;
import org.dc.jps.core.JPService;
import org.dc.jps.exception.JPServiceException;
import org.dc.jul.exception.CouldNotPerformException;
import org.dc.jul.exception.InitializationException;
import org.dc.jul.exception.InstantiationException;
import org.dc.jul.exception.NotAvailableException;
import org.dc.jul.exception.printer.ExceptionPrinter;
import org.dc.jul.exception.printer.LogLevel;
import org.dc.jul.extension.protobuf.IdentifiableMessage;
import org.dc.jul.extension.rsb.com.RPCHelper;
import org.dc.jul.extension.rsb.com.RSBCommunicationService;
import org.dc.jul.extension.rsb.iface.RSBLocalServerInterface;
import org.dc.jul.iface.Manageable;
import org.dc.jul.pattern.Observable;
import org.dc.jul.pattern.Observer;
import org.dc.jul.schedule.GlobalExecutionService;
import org.dc.jul.storage.file.ProtoBufJSonFileProvider;
import org.dc.jul.storage.registry.ProtoBufFileSynchronizedRegistry;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rst.authorization.UserRegistryType.UserRegistry;
import rst.homeautomation.device.DeviceClassType.DeviceClass;
import rst.homeautomation.device.DeviceConfigType.DeviceConfig;
import rst.homeautomation.device.DeviceRegistryType.DeviceRegistry;
import rst.homeautomation.service.ServiceConfigType.ServiceConfig;
import rst.homeautomation.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import rst.homeautomation.unit.UnitConfigType.UnitConfig;
import rst.homeautomation.unit.UnitGroupConfigType.UnitGroupConfig;
import rst.homeautomation.unit.UnitTemplateType.UnitTemplate;
import rst.homeautomation.unit.UnitTemplateType.UnitTemplate.UnitType;
import rst.rsb.ScopeType;
import rst.spatial.LocationRegistryType.LocationRegistry;

/**
 *
 * @author mpohling
 */
public class DeviceRegistryController extends RSBCommunicationService<DeviceRegistry, DeviceRegistry.Builder> implements org.dc.bco.registry.device.lib.DeviceRegistry, Manageable<ScopeType.Scope> {

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(DeviceRegistry.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(DeviceClass.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(DeviceConfig.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(UnitTemplate.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(UnitGroupConfig.getDefaultInstance()));
    }

    private ProtoBufFileSynchronizedRegistry<String, UnitTemplate, UnitTemplate.Builder, DeviceRegistry.Builder> unitTemplateRegistry;
    private ProtoBufFileSynchronizedRegistry<String, DeviceClass, DeviceClass.Builder, DeviceRegistry.Builder> deviceClassRegistry;
    private ProtoBufFileSynchronizedRegistry<String, DeviceConfig, DeviceConfig.Builder, DeviceRegistry.Builder> deviceConfigRegistry;
    private ProtoBufFileSynchronizedRegistry<String, UnitGroupConfig, UnitGroupConfig.Builder, DeviceRegistry.Builder> unitGroupConfigRegistry;

    private final LocationRegistryRemote locationRegistryRemote;
    private final UserRegistryRemote userRegistryRemote;
    private Observer<LocationRegistry> locationRegistryUpdateObserver;
    private Observer<UserRegistry> userRegistryUpdateObserver;

    public DeviceRegistryController() throws InstantiationException, InterruptedException {
        super(DeviceRegistry.newBuilder());
        try {
            ProtoBufJSonFileProvider protoBufJSonFileProvider = new ProtoBufJSonFileProvider();
            unitTemplateRegistry = new ProtoBufFileSynchronizedRegistry<>(UnitTemplate.class, getBuilderSetup(), getDataFieldDescriptor(DeviceRegistry.UNIT_TEMPLATE_FIELD_NUMBER), new UnitTemplateIdGenerator(), JPService.getProperty(JPUnitTemplateDatabaseDirectory.class).getValue(), protoBufJSonFileProvider);
            deviceClassRegistry = new ProtoBufFileSynchronizedRegistry<>(DeviceClass.class, getBuilderSetup(), getDataFieldDescriptor(DeviceRegistry.DEVICE_CLASS_FIELD_NUMBER), new DeviceClassIdGenerator(), JPService.getProperty(JPDeviceClassDatabaseDirectory.class).getValue(), protoBufJSonFileProvider);
            deviceConfigRegistry = new ProtoBufFileSynchronizedRegistry<>(DeviceConfig.class, getBuilderSetup(), getDataFieldDescriptor(DeviceRegistry.DEVICE_CONFIG_FIELD_NUMBER), new DeviceConfigIdGenerator(), JPService.getProperty(JPDeviceConfigDatabaseDirectory.class).getValue(), protoBufJSonFileProvider);
            unitGroupConfigRegistry = new ProtoBufFileSynchronizedRegistry<>(UnitGroupConfig.class, getBuilderSetup(), getDataFieldDescriptor(DeviceRegistry.UNIT_GROUP_CONFIG_FIELD_NUMBER), new UnitGroupIdGenerator(), JPService.getProperty(JPUnitGroupDatabaseDirectory.class).getValue(), protoBufJSonFileProvider);

            deviceConfigRegistry.activateVersionControl(DeviceConfig_0_To_1_DBConverter.class.getPackage());

            locationRegistryRemote = new LocationRegistryRemote();
            userRegistryRemote = new UserRegistryRemote();

            unitTemplateRegistry.setName("UnitTemplateRegistry");
            deviceClassRegistry.setName("DeviceClassRegistry");
            deviceConfigRegistry.setName("DeviceConfigRegistry");
            unitGroupConfigRegistry.setName("unitGroupConfigRegistry");

            unitTemplateRegistry.loadRegistry();
            deviceClassRegistry.loadRegistry();
            deviceConfigRegistry.loadRegistry();
            unitGroupConfigRegistry.loadRegistry();

            deviceClassRegistry.registerConsistencyHandler(new UnitTemplateConfigIdConsistencyHandler());
            deviceClassRegistry.registerConsistencyHandler(new UnitTemplateConfigLabelConsistencyHandler());

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

            unitTemplateRegistry.addObserver((Observable<Map<String, IdentifiableMessage<String, UnitTemplate, UnitTemplate.Builder>>> source, Map<String, IdentifiableMessage<String, UnitTemplate, UnitTemplate.Builder>> data) -> {
                notifyChange();
            });

            deviceClassRegistry.addObserver((Observable<Map<String, IdentifiableMessage<String, DeviceClass, DeviceClass.Builder>>> source, Map<String, IdentifiableMessage<String, DeviceClass, DeviceClass.Builder>> data) -> {
                notifyChange();
            });

            deviceConfigRegistry.addObserver((Observable<Map<String, IdentifiableMessage<String, DeviceConfig, DeviceConfig.Builder>>> source, Map<String, IdentifiableMessage<String, DeviceConfig, DeviceConfig.Builder>> data) -> {
                notifyChange();
            });

            unitGroupConfigRegistry.addObserver((Observable<Map<String, IdentifiableMessage<String, UnitGroupConfig, UnitGroupConfig.Builder>>> source, Map<String, IdentifiableMessage<String, UnitGroupConfig, UnitGroupConfig.Builder>> data) -> {
                notifyChange();
            });

            // Check the device configs if the locations are modifiered.
            locationRegistryUpdateObserver = (Observable<LocationRegistry> source, LocationRegistry data) -> {
                deviceConfigRegistry.checkConsistency();
            };

            userRegistryUpdateObserver = (Observable<UserRegistry> source, UserRegistry data) -> {
                deviceConfigRegistry.checkConsistency();
            };

            // Check the device configs if the device classes have changed.
            deviceClassRegistry.addObserver((Observable<Map<String, IdentifiableMessage<String, DeviceClass, DeviceClass.Builder>>> source, Map<String, IdentifiableMessage<String, DeviceClass, DeviceClass.Builder>> data) -> {
                deviceConfigRegistry.checkConsistency();
            });

            // Check the device classes if the unit templates have changed.
            unitTemplateRegistry.addObserver((Observable<Map<String, IdentifiableMessage<String, UnitTemplate, UnitTemplate.Builder>>> source, Map<String, IdentifiableMessage<String, UnitTemplate, UnitTemplate.Builder>> data) -> {
                deviceClassRegistry.checkConsistency();
            });

            // Check the unit groups if the device configs have changed.
            deviceConfigRegistry.addObserver((Observable<Map<String, IdentifiableMessage<String, DeviceConfig, DeviceConfig.Builder>>> source, Map<String, IdentifiableMessage<String, DeviceConfig, DeviceConfig.Builder>> data) -> {
                unitGroupConfigRegistry.checkConsistency();
            });

        } catch (JPServiceException | CouldNotPerformException ex) {
            throw new InstantiationException(this, ex);
        }
    }

    public void init() throws InitializationException, InterruptedException {
        try {
            super.init(JPService.getProperty(JPDeviceRegistryScope.class).getValue());
            locationRegistryRemote.init();
            userRegistryRemote.init();
        } catch (JPServiceException ex) {
            throw new InitializationException(this, ex);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @throws InterruptedException
     * @throws CouldNotPerformException
     */
    @Override
    public void activate() throws InterruptedException, CouldNotPerformException {
        try {
            super.activate();
            locationRegistryRemote.activate();
            userRegistryRemote.activate();
            locationRegistryRemote.waitForData();
            userRegistryRemote.waitForData();
            locationRegistryRemote.addDataObserver(locationRegistryUpdateObserver);
            userRegistryRemote.addDataObserver(userRegistryUpdateObserver);
        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not activate location registry!", ex);
        }

        try {
            unitTemplateRegistry.checkConsistency();
        } catch (CouldNotPerformException ex) {
            ExceptionPrinter.printHistory(new CouldNotPerformException("Initial consistency check failed!", ex), logger, LogLevel.WARN);
        }

        try {
            deviceClassRegistry.checkConsistency();
        } catch (CouldNotPerformException ex) {
            ExceptionPrinter.printHistory(new CouldNotPerformException("Initial consistency check failed!", ex), logger, LogLevel.WARN);
        }

        try {
            deviceConfigRegistry.checkConsistency();
        } catch (CouldNotPerformException ex) {
            ExceptionPrinter.printHistory(new CouldNotPerformException("Initial consistency check failed!", ex), logger, LogLevel.WARN);
        }

        try {
            unitGroupConfigRegistry.checkConsistency();
        } catch (CouldNotPerformException ex) {
            ExceptionPrinter.printHistory(new CouldNotPerformException("Initial consistency check failed!", ex), logger, LogLevel.WARN);
        }

        try {
            deviceConfigRegistry.registerPlugin(new PublishDeviceTransformationRegistryPlugin(locationRegistryRemote));
        } catch (CouldNotPerformException ex) {
            ExceptionPrinter.printHistory(new CouldNotPerformException("Could not load all plugins!", ex), logger, LogLevel.ERROR);
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
        locationRegistryRemote.removeDataObserver(locationRegistryUpdateObserver);
        userRegistryRemote.removeDataObserver(userRegistryUpdateObserver);
        locationRegistryRemote.deactivate();
        userRegistryRemote.deactivate();
        super.deactivate();
    }

    /**
     * {@inheritDoc}
     *
     */
    @Override
    public void shutdown() {
        if (deviceClassRegistry != null) {
            deviceClassRegistry.shutdown();
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
            ExceptionPrinter.printHistory(ex, logger, LogLevel.WARN);
        }
        locationRegistryRemote.shutdown();
        userRegistryRemote.shutdown();
    }

    /**
     * {@inheritDoc}
     *
     * @throws CouldNotPerformException
     */
    @Override
    public final void notifyChange() throws CouldNotPerformException {
        // sync read only flags
        setDataField(DeviceRegistry.DEVICE_CLASS_REGISTRY_READ_ONLY_FIELD_NUMBER, deviceClassRegistry.isReadOnly());
        setDataField(DeviceRegistry.DEVICE_CONFIG_REGISTRY_READ_ONLY_FIELD_NUMBER, deviceConfigRegistry.isReadOnly());
        setDataField(DeviceRegistry.UNIT_TEMPLATE_REGISTRY_READ_ONLY_FIELD_NUMBER, unitTemplateRegistry.isReadOnly());
        setDataField(DeviceRegistry.UNIT_GROUP_REGISTRY_READ_ONLY_FIELD_NUMBER, unitGroupConfigRegistry.isReadOnly());
        super.notifyChange();
    }

    /**
     * {@inheritDoc}
     *
     * @param server
     * @throws CouldNotPerformException
     */
    @Override
    public void registerMethods(final RSBLocalServerInterface server) throws CouldNotPerformException {
        RPCHelper.registerInterface(org.dc.bco.registry.device.lib.DeviceRegistry.class, this, server);
    }

    /**
     * {@inheritDoc}
     *
     * @param deviceConfig
     * @return
     * @throws CouldNotPerformException
     */
    @Override
    public Future<DeviceConfig> registerDeviceConfig(DeviceConfig deviceConfig) throws CouldNotPerformException {
        return GlobalExecutionService.submit(() -> deviceConfigRegistry.register(deviceConfig));
    }

    /**
     * {@inheritDoc}
     *
     * @param unitTemplateId
     * @return
     * @throws CouldNotPerformException
     */
    @Override
    public UnitTemplate getUnitTemplateById(String unitTemplateId) throws CouldNotPerformException {
        return unitTemplateRegistry.get(unitTemplateId).getMessage();
    }

    /**
     * {@inheritDoc}
     *
     * @param deviceClassId
     * @return
     * @throws CouldNotPerformException
     */
    @Override
    public DeviceClass getDeviceClassById(String deviceClassId) throws CouldNotPerformException {
        return deviceClassRegistry.get(deviceClassId).getMessage();
    }

    /**
     * {@inheritDoc}
     *
     * @param deviceConfigId
     * @return
     * @throws CouldNotPerformException
     */
    @Override
    public DeviceConfig getDeviceConfigById(String deviceConfigId) throws CouldNotPerformException {
        return deviceConfigRegistry.get(deviceConfigId).getMessage();
    }

    /**
     * {@inheritDoc}
     *
     * @param unitConfigId
     * @return
     * @throws CouldNotPerformException
     */
    @Override
    public UnitConfig getUnitConfigById(String unitConfigId) throws CouldNotPerformException {
        for (IdentifiableMessage<String, DeviceConfig, DeviceConfig.Builder> deviceConfig : deviceConfigRegistry.getEntries()) {
            for (UnitConfig unitConfig : deviceConfig.getMessage().getUnitConfigList()) {
                if (unitConfig.getId().equals(unitConfigId)) {
                    return unitConfig;
                }
            }
        }
        throw new NotAvailableException(unitConfigId);
    }

    /**
     * {@inheritDoc}
     *
     * @param unitConfigLabel
     * @return
     * @throws CouldNotPerformException
     * @throws NotAvailableException
     */
    @Override
    public List<UnitConfig> getUnitConfigsByLabel(String unitConfigLabel) throws CouldNotPerformException, NotAvailableException {
        List<UnitConfig> unitConfigs = Collections.synchronizedList(new ArrayList<>());
        deviceConfigRegistry.getEntries().stream().forEach((deviceConfig) -> {
            deviceConfig.getMessage().getUnitConfigList().stream().filter((unitConfig) -> (unitConfig.getLabel().equalsIgnoreCase(unitConfigLabel))).forEach((unitConfig) -> {
                unitConfigs.add(unitConfig);
            });
        });

        return unitConfigs;
    }

    /**
     * {@inheritDoc}
     *
     * @param deviceConfigId
     * @return
     * @throws CouldNotPerformException
     */
    @Override
    public Boolean containsDeviceConfigById(String deviceConfigId) throws CouldNotPerformException {
        return deviceConfigRegistry.contains(deviceConfigId);
    }

    /**
     * {@inheritDoc}
     *
     * @param unitTemplateId
     * @return
     * @throws CouldNotPerformException
     */
    @Override
    public Boolean containsUnitTemplateById(String unitTemplateId) throws CouldNotPerformException {
        return unitTemplateRegistry.contains(unitTemplateId);
    }

    /**
     * {@inheritDoc}
     *
     * @param unitTemplate
     * @return
     * @throws CouldNotPerformException
     */
    @Override
    public Boolean containsUnitTemplate(UnitTemplate unitTemplate) throws CouldNotPerformException {
        return unitTemplateRegistry.contains(unitTemplate);
    }

    /**
     * {@inheritDoc}
     *
     * @param deviceConfig
     * @return
     * @throws CouldNotPerformException
     */
    @Override
    public Boolean containsDeviceConfig(DeviceConfig deviceConfig) throws CouldNotPerformException {
        return deviceConfigRegistry.contains(deviceConfig);
    }

    /**
     * {@inheritDoc}
     *
     * @param unitTemplate
     * @return
     * @throws CouldNotPerformException
     */
    @Override
    public Future<UnitTemplate> updateUnitTemplate(UnitTemplate unitTemplate) throws CouldNotPerformException {
        return GlobalExecutionService.submit(() -> unitTemplateRegistry.update(unitTemplate));
    }

    /**
     * {@inheritDoc}
     *
     * @param deviceConfig
     * @return
     * @throws CouldNotPerformException
     */
    @Override
    public Future<DeviceConfig> updateDeviceConfig(DeviceConfig deviceConfig) throws CouldNotPerformException {
        return GlobalExecutionService.submit(() -> deviceConfigRegistry.update(deviceConfig));
    }

    /**
     * {@inheritDoc}
     *
     * @param deviceConfig
     * @return
     * @throws CouldNotPerformException
     */
    @Override
    public Future<DeviceConfig> removeDeviceConfig(DeviceConfig deviceConfig) throws CouldNotPerformException {
        return GlobalExecutionService.submit(() -> deviceConfigRegistry.remove(deviceConfig));
    }

    /**
     * {@inheritDoc}
     *
     * @param deviceClass
     * @return
     * @throws CouldNotPerformException
     */
    @Override
    public Future<DeviceClass> registerDeviceClass(DeviceClass deviceClass) throws CouldNotPerformException {
        return GlobalExecutionService.submit(() -> deviceClassRegistry.register(deviceClass));
    }

    /**
     * {@inheritDoc}
     *
     * @param deviceClassId
     * @return
     * @throws CouldNotPerformException
     */
    @Override
    public Boolean containsDeviceClassById(String deviceClassId) throws CouldNotPerformException {
        return deviceClassRegistry.contains(deviceClassId);
    }

    /**
     * {@inheritDoc}
     *
     * @param deviceClass
     * @return
     * @throws CouldNotPerformException
     */
    @Override
    public Boolean containsDeviceClass(DeviceClass deviceClass) throws CouldNotPerformException {
        return deviceClassRegistry.contains(deviceClass);
    }

    /**
     * {@inheritDoc}
     *
     * @param deviceClass
     * @return
     * @throws CouldNotPerformException
     */
    @Override
    public Future<DeviceClass> updateDeviceClass(DeviceClass deviceClass) throws CouldNotPerformException {
        return GlobalExecutionService.submit(() -> deviceClassRegistry.update(deviceClass));
    }

    /**
     * {@inheritDoc}
     *
     * @param deviceClass
     * @return
     * @throws CouldNotPerformException
     */
    @Override
    public Future<DeviceClass> removeDeviceClass(DeviceClass deviceClass) throws CouldNotPerformException {
        return GlobalExecutionService.submit(() -> deviceClassRegistry.remove(deviceClass));
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
     * @return
     * @throws CouldNotPerformException
     */
    @Override
    public List<DeviceClass> getDeviceClasses() throws CouldNotPerformException {
        return deviceClassRegistry.getMessages();
    }

    /**
     * {@inheritDoc}
     *
     * @return
     * @throws CouldNotPerformException
     */
    @Override
    public List<DeviceConfig> getDeviceConfigs() throws CouldNotPerformException {
        return deviceConfigRegistry.getMessages();
    }

    /**
     * {@inheritDoc}
     *
     * @return
     * @throws CouldNotPerformException
     */
    @Override
    public List<UnitConfig> getUnitConfigs() throws CouldNotPerformException {
        List<UnitConfig> unitConfigs = new ArrayList<>();
        for (IdentifiableMessage<String, DeviceConfig, DeviceConfig.Builder> deviceConfig : deviceConfigRegistry.getEntries()) {
            unitConfigs.addAll(deviceConfig.getMessage().getUnitConfigList());
        }
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
        throw new NotAvailableException("unit template", "No UnitTemplate with given type registered!");
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
    public Boolean isDeviceClassRegistryReadOnly() throws CouldNotPerformException {
        return deviceClassRegistry.isReadOnly();
    }

    /**
     * {@inheritDoc}
     *
     * @return
     * @throws CouldNotPerformException
     */
    @Override
    public Boolean isDeviceConfigRegistryReadOnly() throws CouldNotPerformException {
        return deviceConfigRegistry.isReadOnly();
    }

    /**
     * {@inheritDoc}
     *
     * @return
     * @throws CouldNotPerformException
     */
    @Override
    public Boolean isUnitGroupConfigRegistryReadOnly() throws CouldNotPerformException {
        return unitGroupConfigRegistry.isReadOnly();
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

    /**
     * {@inheritDoc}
     *
     * @return
     * @throws CouldNotPerformException
     */
    @Override
    public Boolean isDeviceClassRegistryConsistent() throws CouldNotPerformException {
        return deviceClassRegistry.isConsistent();
    }

    /**
     * {@inheritDoc}
     *
     * @return
     * @throws CouldNotPerformException
     */
    @Override
    public Boolean isDeviceConfigRegistryConsistent() throws CouldNotPerformException {
        return deviceConfigRegistry.isConsistent();
    }

    /**
     * {@inheritDoc}
     *
     * @return
     * @throws CouldNotPerformException
     */
    @Override
    public Boolean isUnitGroupConfigRegistryConsistent() throws CouldNotPerformException {
        return unitGroupConfigRegistry.isConsistent();
    }
    
    /**
     * {@inheritDoc}
     *
     * @param type
     * @return
     * @throws CouldNotPerformException
     */
    @Override
    public List<UnitConfig> getUnitConfigs(final UnitType type) throws CouldNotPerformException {
        List<UnitConfig> unitConfigs = new ArrayList<>();
        for (IdentifiableMessage<String, DeviceConfig, DeviceConfig.Builder> deviceConfig : deviceConfigRegistry.getEntries()) {
            for (UnitConfig unitConfig : deviceConfig.getMessage().getUnitConfigList()) {
                if (type == UnitType.UNKNOWN || unitConfig.getType() == type || getSubUnitTypesOfUnitType(type).contains(unitConfig.getType())) {
                    unitConfigs.add(unitConfig);
                }
            }
        }
        return unitConfigs;
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
                if (serviceConfig.getType() == serviceType) {
                    serviceConfigs.add(serviceConfig);
                }
            }
        }
        return serviceConfigs;
    }

    public ProtoBufFileSynchronizedRegistry<String, UnitTemplate, UnitTemplate.Builder, DeviceRegistry.Builder> getUnitTemplateRegistry() {
        return unitTemplateRegistry;
    }

    public ProtoBufFileSynchronizedRegistry<String, DeviceClass, DeviceClass.Builder, DeviceRegistry.Builder> getDeviceClassRegistry() {
        return deviceClassRegistry;
    }

    public ProtoBufFileSynchronizedRegistry<String, DeviceConfig, DeviceConfig.Builder, DeviceRegistry.Builder> getDeviceConfigRegistry() {
        return deviceConfigRegistry;
    }

    public ProtoBufFileSynchronizedRegistry<String, UnitGroupConfig, UnitGroupConfig.Builder, DeviceRegistry.Builder> getUnitGroupRegistry() {
        return unitGroupConfigRegistry;
    }

    /**
     * {@inheritDoc}
     *
     * @param groupConfig
     * @return
     * @throws CouldNotPerformException
     */
    @Override
    public Future<UnitGroupConfig> registerUnitGroupConfig(UnitGroupConfig groupConfig) throws CouldNotPerformException {
        return GlobalExecutionService.submit(() -> unitGroupConfigRegistry.register(groupConfig));
    }

    /**
     * {@inheritDoc}
     *
     * @param groupConfig
     * @return
     * @throws CouldNotPerformException
     */
    @Override
    public Boolean containsUnitGroupConfig(UnitGroupConfig groupConfig) throws CouldNotPerformException {
        return unitGroupConfigRegistry.contains(groupConfig);
    }

    /**
     * {@inheritDoc}
     *
     * @param groupConfigId
     * @return
     * @throws CouldNotPerformException
     */
    @Override
    public Boolean containsUnitGroupConfigById(String groupConfigId) throws CouldNotPerformException {
        return unitGroupConfigRegistry.contains(groupConfigId);
    }

    /**
     * {@inheritDoc}
     *
     * @param groupConfig
     * @return
     * @throws CouldNotPerformException
     */
    @Override
    public Future<UnitGroupConfig> updateUnitGroupConfig(UnitGroupConfig groupConfig) throws CouldNotPerformException {
        return GlobalExecutionService.submit(() -> unitGroupConfigRegistry.update(groupConfig));
    }

    /**
     * {@inheritDoc}
     *
     * @param groupConfig
     * @return
     * @throws CouldNotPerformException
     */
    @Override
    public Future<UnitGroupConfig> removeUnitGroupConfig(UnitGroupConfig groupConfig) throws CouldNotPerformException {
        return GlobalExecutionService.submit(() -> unitGroupConfigRegistry.remove(groupConfig));
    }

    /**
     * {@inheritDoc}
     *
     * @param groupConfigId
     * @return
     * @throws CouldNotPerformException
     */
    @Override
    public UnitGroupConfig getUnitGroupConfigById(String groupConfigId) throws CouldNotPerformException {
        return unitGroupConfigRegistry.get(groupConfigId).getMessage();
    }

    /**
     * {@inheritDoc}
     *
     * @return
     * @throws CouldNotPerformException
     */
    @Override
    public List<UnitGroupConfig> getUnitGroupConfigs() throws CouldNotPerformException {
        List<UnitGroupConfig> unitGroups = new ArrayList<>();
        for (IdentifiableMessage<String, UnitGroupConfig, UnitGroupConfig.Builder> unitGroup : unitGroupConfigRegistry.getEntries()) {
            unitGroups.add(unitGroup.getMessage());
        }
        return unitGroups;
    }

    /**
     * {@inheritDoc}
     *
     * @param unitConfig
     * @return
     * @throws CouldNotPerformException
     */
    @Override
    public List<UnitGroupConfig> getUnitGroupConfigsbyUnitConfig(UnitConfig unitConfig) throws CouldNotPerformException {
        List<UnitGroupConfig> unitGroups = new ArrayList<>();
        for (UnitGroupConfig unitGroup : getUnitGroupConfigs()) {
            if (unitGroup.getMemberIdList().contains(unitConfig.getId())) {
                unitGroups.add(unitGroup);
            }
        }
        return unitGroups;
    }

    /**
     * {@inheritDoc}
     *
     * @param type
     * @return
     * @throws CouldNotPerformException
     */
    @Override
    public List<UnitGroupConfig> getUnitGroupConfigsByUnitType(UnitType type) throws CouldNotPerformException {
        List<UnitGroupConfig> unitGroups = new ArrayList<>();
        for (UnitGroupConfig unitGroup : getUnitGroupConfigs()) {
            if (unitGroup.getUnitType() == type || getSubUnitTypesOfUnitType(type).contains(unitGroup.getUnitType())) {
                unitGroups.add(unitGroup);
            }
        }
        return unitGroups;
    }

    /**
     * {@inheritDoc}
     *
     * @param serviceTypes
     * @return
     * @throws CouldNotPerformException
     */
    @Override
    public List<UnitGroupConfig> getUnitGroupConfigsByServiceTypes(List<ServiceType> serviceTypes) throws CouldNotPerformException {
        List<UnitGroupConfig> unitGroups = new ArrayList<>();
        for (UnitGroupConfig unitGroup : getUnitGroupConfigs()) {
            boolean skipGroup = false;
            for (ServiceType serviceType : unitGroup.getServiceTypeList()) {
                if (!serviceTypes.contains(serviceType)) {
                    skipGroup = true;
                }
            }
            if (skipGroup) {
                continue;
            }
            unitGroups.add(unitGroup);
        }
        return unitGroups;
    }

    /**
     * {@inheritDoc}
     *
     * @param groupConfig
     * @return
     * @throws CouldNotPerformException
     */
    @Override
    public List<UnitConfig> getUnitConfigsByUnitGroupConfig(UnitGroupConfig groupConfig) throws CouldNotPerformException {
        List<UnitConfig> unitConfigs = new ArrayList<>();
        for (String unitId : groupConfig.getMemberIdList()) {
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
                    if (serviceConfig.getType() == serviceType) {
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
     * @param type
     * @return
     * @throws CouldNotPerformException
     */
    @Override
    public List<UnitType> getSubUnitTypesOfUnitType(UnitType type) throws CouldNotPerformException {
        List<UnitType> unitTypes = new ArrayList<>();
        for (UnitTemplate template : unitTemplateRegistry.getMessages()) {
            if (template.getIncludedTypeList().contains(type)) {
                unitTypes.add(template.getType());
            }
        }
        return unitTypes;
    }
}
