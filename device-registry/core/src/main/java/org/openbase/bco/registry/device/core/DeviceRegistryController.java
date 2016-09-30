package org.openbase.bco.registry.device.core;

/*
 * #%L
 * REM DeviceRegistry Core
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
import org.openbase.bco.registry.device.core.consistency.UnitTemplateConfigIdConsistencyHandler;
import org.openbase.bco.registry.device.core.consistency.UnitTemplateConfigLabelConsistencyHandler;
import org.openbase.bco.registry.device.core.dbconvert.DeviceClass_0_To_1_DBConverter;
import org.openbase.bco.registry.device.lib.DeviceRegistry;
import org.openbase.bco.registry.device.lib.generator.DeviceClassIdGenerator;
import org.openbase.bco.registry.device.lib.jp.JPDeviceClassDatabaseDirectory;
import org.openbase.bco.registry.device.lib.jp.JPDeviceRegistryScope;
import org.openbase.bco.registry.lib.com.AbstractVirtualRegistryController;
import org.openbase.bco.registry.lib.com.SynchronizedRemoteRegistry;
import org.openbase.bco.registry.lib.util.UnitConfigUtils;
import org.openbase.bco.registry.unit.remote.UnitRegistryRemote;
import org.openbase.jps.core.JPService;
import org.openbase.jps.exception.JPServiceException;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.exception.VerificationFailedException;
import org.openbase.jul.extension.rsb.com.RPCHelper;
import org.openbase.jul.extension.rsb.iface.RSBLocalServer;
import org.openbase.jul.iface.Manageable;
import org.openbase.jul.schedule.GlobalExecutionService;
import org.openbase.jul.storage.registry.ProtoBufFileSynchronizedRegistry;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rst.homeautomation.device.DeviceClassType.DeviceClass;
import rst.homeautomation.device.DeviceConfigType.DeviceConfig;
import rst.homeautomation.device.DeviceRegistryDataType.DeviceRegistryData;
import rst.homeautomation.service.ServiceConfigType.ServiceConfig;
import rst.homeautomation.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import rst.homeautomation.unit.UnitConfigType.UnitConfig;
import rst.homeautomation.unit.UnitGroupConfigType.UnitGroupConfig;
import rst.homeautomation.unit.UnitRegistryDataType.UnitRegistryData;
import rst.homeautomation.unit.UnitTemplateType.UnitTemplate;
import rst.homeautomation.unit.UnitTemplateType.UnitTemplate.UnitType;
import rst.rsb.ScopeType;

/**
 *
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public class DeviceRegistryController extends AbstractVirtualRegistryController<DeviceRegistryData, DeviceRegistryData.Builder, UnitRegistryData> implements DeviceRegistry, Manageable<ScopeType.Scope> {

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(DeviceRegistryData.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(DeviceClass.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(DeviceConfig.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(UnitTemplate.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(UnitGroupConfig.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(UnitConfig.getDefaultInstance()));
    }

    private ProtoBufFileSynchronizedRegistry<String, DeviceClass, DeviceClass.Builder, DeviceRegistryData.Builder> deviceClassRegistry;

    private final UnitRegistryRemote unitRegistryRemote;
    private final SynchronizedRemoteRegistry<String, UnitConfig, UnitConfig.Builder> deviceUnitConfigRemoteRegistry;

    public DeviceRegistryController() throws InstantiationException, InterruptedException {
        super(JPDeviceRegistryScope.class, DeviceRegistryData.newBuilder());
        try {
            deviceClassRegistry = new ProtoBufFileSynchronizedRegistry<>(DeviceClass.class, getBuilderSetup(), getDataFieldDescriptor(DeviceRegistryData.DEVICE_CLASS_FIELD_NUMBER), new DeviceClassIdGenerator(), JPService.getProperty(JPDeviceClassDatabaseDirectory.class).getValue(), protoBufJSonFileProvider);
            unitRegistryRemote = new UnitRegistryRemote();
            deviceUnitConfigRemoteRegistry = new SynchronizedRemoteRegistry<>(UnitRegistryData.DEVICE_UNIT_CONFIG_FIELD_NUMBER, unitRegistryRemote);
        } catch (JPServiceException | CouldNotPerformException ex) {
            throw new InstantiationException(this, ex);
        }
    }

    @Override
    protected void registerConsistencyHandler() throws CouldNotPerformException {
        deviceClassRegistry.registerConsistencyHandler(new UnitTemplateConfigIdConsistencyHandler());
        deviceClassRegistry.registerConsistencyHandler(new UnitTemplateConfigLabelConsistencyHandler());
    }

    @Override
    protected void registerDependencies() throws CouldNotPerformException {
        deviceClassRegistry.registerDependency(unitRegistryRemote.getUnitTemplateRemoteRegistry());

    }

    @Override
    protected Package getVersionConverterPackage() throws CouldNotPerformException {
        return DeviceClass_0_To_1_DBConverter.class.getPackage();
    }

    @Override
    protected void registerPlugins() throws CouldNotPerformException, InterruptedException {
    }

    @Override
    protected void registerRegistryRemotes() throws CouldNotPerformException {
        registerRegistryRemote(unitRegistryRemote);
    }

    @Override
    protected void registerRemoteRegistries() throws CouldNotPerformException {
        registerRemoteRegistry(deviceUnitConfigRemoteRegistry);
    }

    @Override
    protected void registerRegistries() throws CouldNotPerformException {
        registerRegistry(deviceClassRegistry);
    }

    /**
     * {@inheritDoc}
     *
     * @throws CouldNotPerformException {@inheritDoc}
     * @throws java.lang.InterruptedException {@inheritDoc}
     */
    @Override
    public final void syncRegistryFlags() throws CouldNotPerformException, InterruptedException {
        setDataField(DeviceRegistryData.DEVICE_CLASS_REGISTRY_READ_ONLY_FIELD_NUMBER, deviceClassRegistry.isReadOnly());
        setDataField(DeviceRegistryData.DEVICE_CLASS_REGISTRY_CONSISTENT_FIELD_NUMBER, deviceClassRegistry.isConsistent());
    }

    @Override
    protected void syncVirtualRegistryFields(final UnitRegistryData realData) throws CouldNotPerformException {
        setDataField(DeviceRegistryData.DEVICE_UNIT_CONFIG_FIELD_NUMBER, realData.getDeviceUnitConfigList());
        setDataField(DeviceRegistryData.DEVICE_UNIT_CONFIG_REGISTRY_CONSISTENT_FIELD_NUMBER, realData.getDeviceUnitConfigRegistryConsistent());
        setDataField(DeviceRegistryData.DEVICE_UNIT_CONFIG_REGISTRY_READ_ONLY_FIELD_NUMBER, realData.getDeviceUnitConfigRegistryReadOnly());
    }

    /**
     * {@inheritDoc}
     *
     * @param server {@inheritDoc}
     * @throws CouldNotPerformException {@inheritDoc}
     */
    @Override
    public void registerMethods(final RSBLocalServer server) throws CouldNotPerformException {
        RPCHelper.registerInterface(DeviceRegistry.class, this, server);
    }

    private void verifyDeviceUnitConfig(UnitConfig unitConfig) throws VerificationFailedException {
        UnitConfigUtils.verifyUnitType(unitConfig, UnitType.DEVICE);
    }

    /**
     * {@inheritDoc}
     *
     * @param deviceConfig {@inheritDoc}
     * @return {@inheritDoc}
     * @throws CouldNotPerformException {@inheritDoc}
     */
    @Override
    public Future<UnitConfig> registerDeviceConfig(UnitConfig deviceConfig) throws CouldNotPerformException {
        verifyDeviceUnitConfig(deviceConfig);
        return unitRegistryRemote.registerUnitConfig(deviceConfig);
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
        return unitRegistryRemote.getUnitTemplateById(unitTemplateId);
    }

    /**
     * {@inheritDoc}
     *
     * @param deviceClassId {@inheritDoc}
     * @return {@inheritDoc}
     * @throws CouldNotPerformException {@inheritDoc}
     */
    @Override
    public DeviceClass getDeviceClassById(String deviceClassId) throws CouldNotPerformException {
        return deviceClassRegistry.get(deviceClassId).getMessage();
    }

    /**
     * {@inheritDoc}
     *
     * @param deviceConfigId {@inheritDoc}
     * @return {@inheritDoc}
     * @throws CouldNotPerformException {@inheritDoc}
     */
    @Override
    public UnitConfig getDeviceConfigById(String deviceConfigId) throws CouldNotPerformException {
        unitRegistryRemote.validateData();
        return deviceUnitConfigRemoteRegistry.getMessage(deviceConfigId);
    }

    /**
     * {@inheritDoc}
     *
     * @param unitConfigId {@inheritDoc}
     * @return {@inheritDoc}
     * @throws CouldNotPerformException {@inheritDoc}
     */
    @Override
    public UnitConfig getUnitConfigById(String unitConfigId) throws CouldNotPerformException {
        return unitRegistryRemote.getUnitConfigById(unitConfigId);
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
    public List<UnitConfig> getUnitConfigsByLabel(String unitConfigLabel) throws CouldNotPerformException, NotAvailableException {
        return unitRegistryRemote.getUnitConfigsByLabel(unitConfigLabel);
    }

    /**
     * {@inheritDoc}
     *
     * @param deviceConfigId {@inheritDoc}
     * @return {@inheritDoc}
     * @throws CouldNotPerformException {@inheritDoc}
     */
    @Override
    public Boolean containsDeviceConfigById(String deviceConfigId) throws CouldNotPerformException {
        unitRegistryRemote.validateData();
        return deviceUnitConfigRemoteRegistry.contains(deviceConfigId);
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
        return unitRegistryRemote.containsUnitTemplateById(unitTemplateId);
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
        return unitRegistryRemote.containsUnitTemplate(unitTemplate);
    }

    /**
     * {@inheritDoc}
     *
     * @param deviceConfig {@inheritDoc}
     * @return {@inheritDoc}
     * @throws CouldNotPerformException {@inheritDoc}
     */
    @Override
    public Boolean containsDeviceConfig(UnitConfig deviceConfig) throws CouldNotPerformException {
        unitRegistryRemote.validateData();
        return deviceUnitConfigRemoteRegistry.contains(deviceConfig);
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
        return unitRegistryRemote.updateUnitTemplate(unitTemplate);
    }

    /**
     * {@inheritDoc}
     *
     * @param deviceConfig {@inheritDoc}
     * @return {@inheritDoc}
     * @throws CouldNotPerformException {@inheritDoc}
     */
    @Override
    public Future<UnitConfig> updateDeviceConfig(UnitConfig deviceConfig) throws CouldNotPerformException {
        verifyDeviceUnitConfig(deviceConfig);
        return unitRegistryRemote.updateUnitConfig(deviceConfig);
    }

    /**
     * {@inheritDoc}
     *
     * @param deviceConfig {@inheritDoc}
     * @return {@inheritDoc}
     * @throws CouldNotPerformException {@inheritDoc}
     */
    @Override
    public Future<UnitConfig> removeDeviceConfig(UnitConfig deviceConfig) throws CouldNotPerformException {
        verifyDeviceUnitConfig(deviceConfig);
        return unitRegistryRemote.removeUnitConfig(deviceConfig);
    }

    /**
     * {@inheritDoc}
     *
     * @param deviceClass {@inheritDoc}
     * @return {@inheritDoc}
     * @throws CouldNotPerformException {@inheritDoc}
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
        return unitRegistryRemote.getUnitTemplates();
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
    public List<UnitConfig> getDeviceConfigs() throws CouldNotPerformException {
        unitRegistryRemote.validateData();
        return deviceUnitConfigRemoteRegistry.getMessages();
    }

    /**
     * {@inheritDoc}
     *
     * @return
     * @throws CouldNotPerformException
     */
    @Override
    public List<UnitConfig> getUnitConfigs() throws CouldNotPerformException {
        return unitRegistryRemote.getUnitConfigs();
    }

    /**
     * {@inheritDoc}
     *
     * @return
     * @throws CouldNotPerformException
     */
    @Override
    public List<ServiceConfig> getServiceConfigs() throws CouldNotPerformException {
        return unitRegistryRemote.getServiceConfigs();
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
        return unitRegistryRemote.getUnitTemplateByType(type);
    }

    /**
     * {@inheritDoc}
     *
     * @return
     * @throws CouldNotPerformException
     */
    @Override
    public Boolean isUnitTemplateRegistryReadOnly() throws CouldNotPerformException {
        return unitRegistryRemote.isUnitTemplateRegistryReadOnly();
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
        unitRegistryRemote.validateData();
        return getData().getDeviceUnitConfigRegistryReadOnly();
    }

    /**
     * {@inheritDoc}
     *
     * @return
     * @throws CouldNotPerformException
     */
    @Override
    public Boolean isUnitGroupConfigRegistryReadOnly() throws CouldNotPerformException {
        return unitRegistryRemote.isUnitGroupConfigRegistryReadOnly();
    }

    /**
     * {@inheritDoc}
     *
     * @return
     * @throws CouldNotPerformException
     */
    @Override
    public Boolean isUnitTemplateRegistryConsistent() throws CouldNotPerformException {
        return unitRegistryRemote.isUnitConfigRegistryConsistent();
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
        unitRegistryRemote.validateData();
        return getData().getDeviceUnitConfigRegistryConsistent();
    }

    /**
     * {@inheritDoc}
     *
     * @return
     * @throws CouldNotPerformException
     */
    @Override
    public Boolean isUnitGroupConfigRegistryConsistent() throws CouldNotPerformException {
        return unitRegistryRemote.isUnitGroupConfigRegistryConsistent();
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
        return unitRegistryRemote.getUnitConfigs(type);
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
        return unitRegistryRemote.getServiceConfigs(serviceType);
    }

    public ProtoBufFileSynchronizedRegistry<String, DeviceClass, DeviceClass.Builder, DeviceRegistryData.Builder> getDeviceClassRegistry() {
        return deviceClassRegistry;
    }

    /**
     * {@inheritDoc}
     *
     * @param groupConfig
     * @return
     * @throws CouldNotPerformException
     */
    @Override
    public Future<UnitConfig> registerUnitGroupConfig(UnitConfig groupConfig) throws CouldNotPerformException {
        return unitRegistryRemote.registerUnitConfig(groupConfig);
    }

    /**
     * {@inheritDoc}
     *
     * @param groupConfig
     * @return
     * @throws CouldNotPerformException
     */
    @Override
    public Boolean containsUnitGroupConfig(UnitConfig groupConfig) throws CouldNotPerformException {
        return unitRegistryRemote.containsUnitConfig(groupConfig);
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
        return unitRegistryRemote.containsUnitConfigById(groupConfigId);
    }

    /**
     * {@inheritDoc}
     *
     * @param groupConfig
     * @return
     * @throws CouldNotPerformException
     */
    @Override
    public Future<UnitConfig> updateUnitGroupConfig(UnitConfig groupConfig) throws CouldNotPerformException {
        return unitRegistryRemote.updateUnitConfig(groupConfig);
    }

    /**
     * {@inheritDoc}
     *
     * @param groupConfig
     * @return
     * @throws CouldNotPerformException
     */
    @Override
    public Future<UnitConfig> removeUnitGroupConfig(UnitConfig groupConfig) throws CouldNotPerformException {
        return unitRegistryRemote.removeUnitConfig(groupConfig);
    }

    /**
     * {@inheritDoc}
     *
     * @param groupConfigId
     * @return
     * @throws CouldNotPerformException
     */
    @Override
    public UnitConfig getUnitGroupConfigById(String groupConfigId) throws CouldNotPerformException {
        return unitRegistryRemote.getUnitConfigById(groupConfigId);
    }

    /**
     * {@inheritDoc}
     *
     * @return
     * @throws CouldNotPerformException
     */
    @Override
    public List<UnitConfig> getUnitGroupConfigs() throws CouldNotPerformException {
        return unitRegistryRemote.getUnitGroupConfigs();
    }

    /**
     * {@inheritDoc}
     *
     * @param unitConfig
     * @return
     * @throws CouldNotPerformException
     */
    @Override
    public List<UnitConfig> getUnitGroupConfigsbyUnitConfig(UnitConfig unitConfig) throws CouldNotPerformException {
        return unitRegistryRemote.getUnitGroupConfigsByUnitConfig(unitConfig);
    }

    /**
     * {@inheritDoc}
     *
     * @param type
     * @return
     * @throws CouldNotPerformException
     */
    @Override
    public List<UnitConfig> getUnitGroupConfigsByUnitType(UnitType type) throws CouldNotPerformException {
        return unitRegistryRemote.getUnitGroupConfigsByUnitType(type);
    }

    /**
     * {@inheritDoc}
     *
     * @param serviceTypes
     * @return
     * @throws CouldNotPerformException
     */
    @Override
    public List<UnitConfig> getUnitGroupConfigsByServiceTypes(List<ServiceType> serviceTypes) throws CouldNotPerformException {
        return unitRegistryRemote.getUnitGroupConfigsByServiceTypes(serviceTypes);
    }

    /**
     * {@inheritDoc}
     *
     * @param groupConfig
     * @return
     * @throws CouldNotPerformException
     */
    @Override
    public List<UnitConfig> getUnitConfigsByUnitGroupConfig(UnitConfig groupConfig) throws CouldNotPerformException {
        return unitRegistryRemote.getUnitConfigsByUnitGroupConfig(groupConfig);
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
        return unitRegistryRemote.getUnitConfigsByUnitTypeAndServiceTypes(type, serviceTypes);
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
        return unitRegistryRemote.getUnitConfigByScope(scope);
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
        return unitRegistryRemote.getSubUnitTypesOfUnitType(type);
    }
}
