/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dm.core.registry;

import de.citec.dm.core.consistency.DeviceConfigDeviceClassIdConsistencyHandler;
import de.citec.dm.core.consistency.DeviceConfigDeviceClassUnitConsistencyHandler;
import de.citec.dm.core.consistency.DeviceIdConsistencyHandler;
import de.citec.dm.core.consistency.DeviceLabelConsistencyHandler;
import de.citec.dm.core.consistency.DeviceLocationIdConsistencyHandler;
import de.citec.dm.core.consistency.DeviceScopeConsistencyHandler;
import de.citec.dm.core.consistency.OpenhabServiceConfigItemIdConsistencyHandler;
import de.citec.dm.core.consistency.ServiceConfigBindingTypeConsistencyHandler;
import de.citec.dm.core.consistency.ServiceConfigUnitIdConsistencyHandler;
import de.citec.dm.core.consistency.UnitBoundsToDeviceConsistencyHandler;
import de.citec.dm.core.consistency.UnitConfigUnitTemplateConfigIdConsistencyHandler;
import de.citec.dm.core.consistency.UnitConfigUnitTemplateConsistencyHandler;
import de.citec.dm.core.consistency.UnitIdConsistencyHandler;
import de.citec.dm.core.consistency.UnitLabelConsistencyHandler;
import de.citec.dm.core.consistency.UnitLocationIdConsistencyHandler;
import de.citec.dm.core.consistency.UnitScopeConsistencyHandler;
import de.citec.dm.core.consistency.UnitTemplateConfigIdConsistencyHandler;
import de.citec.dm.core.consistency.UnitTemplateConfigLabelConsistencyHandler;
import de.citec.dm.core.consistency.UnitTemplateValidationConsistencyHandler;
import de.citec.dm.core.plugin.PublishDeviceTransformationRegistryPlugin;
import de.citec.dm.core.plugin.UnitTemplateCreatorRegistryPlugin;
import de.citec.dm.core.registry.dbconvert.DeviceConfig_0_To_1_DBConverter;
import de.citec.dm.lib.generator.DeviceClassIdGenerator;
import de.citec.dm.lib.generator.DeviceConfigIdGenerator;
import de.citec.dm.lib.generator.UnitGroupIdGenerator;
import de.citec.dm.lib.generator.UnitTemplateIdGenerator;
import de.citec.dm.lib.registry.DeviceRegistryInterface;
import de.citec.jp.JPDeviceClassDatabaseDirectory;
import de.citec.jp.JPDeviceConfigDatabaseDirectory;
import de.citec.jp.JPDeviceRegistryScope;
import de.citec.jp.JPUnitGroupDatabaseDirectory;
import de.citec.jp.JPUnitTemplateDatabaseDirectory;
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
import org.dc.jul.pattern.Observable;
import org.dc.jul.pattern.Observer;
import org.dc.jul.storage.file.ProtoBufJSonFileProvider;
import org.dc.jul.storage.registry.ProtoBufFileSynchronizedRegistry;
import de.citec.lm.remote.LocationRegistryRemote;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import org.dc.jps.core.JPService;
import org.dc.jps.exception.JPServiceException;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rst.homeautomation.device.DeviceClassType.DeviceClass;
import rst.homeautomation.device.DeviceConfigType.DeviceConfig;
import rst.homeautomation.device.DeviceRegistryType.DeviceRegistry;
import rst.homeautomation.service.ServiceConfigType.ServiceConfig;
import rst.homeautomation.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import rst.homeautomation.unit.UnitConfigType.UnitConfig;
import rst.homeautomation.unit.UnitGroupConfigType.UnitGroupConfig;
import rst.homeautomation.unit.UnitTemplateType.UnitTemplate;
import rst.homeautomation.unit.UnitTemplateType.UnitTemplate.UnitType;
import rst.spatial.LocationRegistryType.LocationRegistry;

/**
 *
 * @author mpohling
 */
public class DeviceRegistryService extends RSBCommunicationService<DeviceRegistry, DeviceRegistry.Builder> implements DeviceRegistryInterface {

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
    private Observer<LocationRegistry> locationRegistryUpdateObserver;

    public DeviceRegistryService() throws InstantiationException, InterruptedException {
        super(DeviceRegistry.newBuilder());
        try {
            ProtoBufJSonFileProvider protoBufJSonFileProvider = new ProtoBufJSonFileProvider();
            unitTemplateRegistry = new ProtoBufFileSynchronizedRegistry<>(UnitTemplate.class, getBuilderSetup(), getFieldDescriptor(DeviceRegistry.UNIT_TEMPLATE_FIELD_NUMBER), new UnitTemplateIdGenerator(), JPService.getProperty(JPUnitTemplateDatabaseDirectory.class).getValue(), protoBufJSonFileProvider);
            deviceClassRegistry = new ProtoBufFileSynchronizedRegistry<>(DeviceClass.class, getBuilderSetup(), getFieldDescriptor(DeviceRegistry.DEVICE_CLASS_FIELD_NUMBER), new DeviceClassIdGenerator(), JPService.getProperty(JPDeviceClassDatabaseDirectory.class).getValue(), protoBufJSonFileProvider);
            deviceConfigRegistry = new ProtoBufFileSynchronizedRegistry<>(DeviceConfig.class, getBuilderSetup(), getFieldDescriptor(DeviceRegistry.DEVICE_CONFIG_FIELD_NUMBER), new DeviceConfigIdGenerator(), JPService.getProperty(JPDeviceConfigDatabaseDirectory.class).getValue(), protoBufJSonFileProvider);
            unitGroupConfigRegistry = new ProtoBufFileSynchronizedRegistry<>(UnitGroupConfig.class, getBuilderSetup(), getFieldDescriptor(DeviceRegistry.UNIT_GROUP_CONFIG_FIELD_NUMBER), new UnitGroupIdGenerator(), JPService.getProperty(JPUnitGroupDatabaseDirectory.class).getValue(), protoBufJSonFileProvider);

            deviceConfigRegistry.activateVersionControl(DeviceConfig_0_To_1_DBConverter.class.getPackage());

            locationRegistryRemote = new LocationRegistryRemote();

            unitTemplateRegistry.loadRegistry();
            deviceClassRegistry.loadRegistry();
            deviceConfigRegistry.loadRegistry();
            unitGroupConfigRegistry.loadRegistry();

            deviceClassRegistry.registerConsistencyHandler(new UnitTemplateConfigIdConsistencyHandler());
            deviceClassRegistry.registerConsistencyHandler(new UnitTemplateConfigLabelConsistencyHandler());

            deviceConfigRegistry.registerPlugin(new PublishDeviceTransformationRegistryPlugin());
            deviceConfigRegistry.registerConsistencyHandler(new DeviceIdConsistencyHandler());
            deviceConfigRegistry.registerConsistencyHandler(new DeviceConfigDeviceClassIdConsistencyHandler(deviceClassRegistry));
            deviceConfigRegistry.registerConsistencyHandler(new DeviceLabelConsistencyHandler());
            deviceConfigRegistry.registerConsistencyHandler(new DeviceLocationIdConsistencyHandler(locationRegistryRemote));
            deviceConfigRegistry.registerConsistencyHandler(new DeviceScopeConsistencyHandler(locationRegistryRemote));
            deviceConfigRegistry.registerConsistencyHandler(new UnitScopeConsistencyHandler(locationRegistryRemote));
            deviceConfigRegistry.registerConsistencyHandler(new UnitIdConsistencyHandler());
            deviceConfigRegistry.registerConsistencyHandler(new UnitBoundsToDeviceConsistencyHandler(deviceClassRegistry));
            deviceConfigRegistry.registerConsistencyHandler(new UnitLabelConsistencyHandler(deviceClassRegistry));
            deviceConfigRegistry.registerConsistencyHandler(new UnitLocationIdConsistencyHandler(locationRegistryRemote));
            deviceConfigRegistry.registerConsistencyHandler(new ServiceConfigUnitIdConsistencyHandler());
            deviceConfigRegistry.registerConsistencyHandler(new ServiceConfigBindingTypeConsistencyHandler(deviceClassRegistry));
            deviceConfigRegistry.registerConsistencyHandler(new OpenhabServiceConfigItemIdConsistencyHandler(locationRegistryRemote, deviceClassRegistry));
            deviceConfigRegistry.registerConsistencyHandler(new UnitConfigUnitTemplateConsistencyHandler(unitTemplateRegistry));
            deviceConfigRegistry.registerConsistencyHandler(new UnitConfigUnitTemplateConfigIdConsistencyHandler(deviceClassRegistry));
            deviceConfigRegistry.registerConsistencyHandler(new DeviceConfigDeviceClassUnitConsistencyHandler(deviceClassRegistry));

            unitTemplateRegistry.registerConsistencyHandler(new UnitTemplateValidationConsistencyHandler());
            unitTemplateRegistry.registerPlugin(new UnitTemplateCreatorRegistryPlugin(unitTemplateRegistry));

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

            // Check the device configs if the device classes has changed.
            deviceClassRegistry.addObserver((Observable<Map<String, IdentifiableMessage<String, DeviceClass, DeviceClass.Builder>>> source, Map<String, IdentifiableMessage<String, DeviceClass, DeviceClass.Builder>> data) -> {
                deviceConfigRegistry.checkConsistency();
            });

            // Check the device classes if the unit templates has changed.
            unitTemplateRegistry.addObserver((Observable<Map<String, IdentifiableMessage<String, UnitTemplate, UnitTemplate.Builder>>> source, Map<String, IdentifiableMessage<String, UnitTemplate, UnitTemplate.Builder>> data) -> {
                deviceClassRegistry.checkConsistency();
            });

        } catch (JPServiceException | CouldNotPerformException ex) {
            throw new InstantiationException(this, ex);
        }
    }

    public void init() throws InitializationException {
        try {
            super.init(JPService.getProperty(JPDeviceRegistryScope.class).getValue());
            locationRegistryRemote.init();
        } catch (JPServiceException ex) {
            throw new InitializationException(this, ex);
        }
    }

    @Override
    public void activate() throws InterruptedException, CouldNotPerformException {
        try {
            super.activate();
            locationRegistryRemote.activate();
            locationRegistryRemote.addObserver(locationRegistryUpdateObserver);
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
    }

    @Override
    public void deactivate() throws InterruptedException, CouldNotPerformException {
        locationRegistryRemote.removeObserver(locationRegistryUpdateObserver);
        super.deactivate();
    }

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
    }

    @Override
    public final void notifyChange() throws CouldNotPerformException {
        // sync read only flags
        setField(DeviceRegistry.DEVICE_CLASS_REGISTRY_READ_ONLY_FIELD_NUMBER, deviceClassRegistry.isReadOnly());
        setField(DeviceRegistry.DEVICE_CONFIG_REGISTRY_READ_ONLY_FIELD_NUMBER, deviceConfigRegistry.isReadOnly());
        setField(DeviceRegistry.UNIT_TEMPLATE_REGISTRY_READ_ONLY_FIELD_NUMBER, unitTemplateRegistry.isReadOnly());
        setField(DeviceRegistry.UNIT_GROUP_REGISTRY_READ_ONLY_FIELD_NUMBER, unitGroupConfigRegistry.isReadOnly());
        super.notifyChange();
    }

    @Override
    public void registerMethods(final RSBLocalServerInterface server) throws CouldNotPerformException {
        RPCHelper.registerInterface(DeviceRegistryInterface.class, this, server);
    }

    @Override
    public DeviceConfig registerDeviceConfig(DeviceConfig deviceConfig) throws CouldNotPerformException {
        return deviceConfigRegistry.register(deviceConfig);
    }

    @Override
    public UnitTemplate getUnitTemplateById(String unitTemplateId) throws CouldNotPerformException {
        return unitTemplateRegistry.get(unitTemplateId).getMessage();
    }

    @Override
    public DeviceClass getDeviceClassById(String deviceClassId) throws CouldNotPerformException {
        return deviceClassRegistry.get(deviceClassId).getMessage();
    }

    @Override
    public DeviceConfig getDeviceConfigById(String deviceConfigId) throws CouldNotPerformException {
        return deviceConfigRegistry.get(deviceConfigId).getMessage();
    }

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

    @Override
    public List<UnitConfig> getUnitConfigsByLabel(String unitConfigLabel) throws CouldNotPerformException, NotAvailableException {
        List<UnitConfig> unitConfigs = Collections.synchronizedList(new ArrayList<>());
        deviceConfigRegistry.getEntries().stream().forEach((deviceConfig) -> {
            deviceConfig.getMessage().getUnitConfigList().stream().filter((unitConfig) -> (unitConfig.getLabel().equals(unitConfigLabel))).forEach((unitConfig) -> {
                unitConfigs.add(unitConfig);
            });
        });

        return unitConfigs;
    }

    @Override
    public Boolean containsDeviceConfigById(String deviceConfigId) throws CouldNotPerformException {
        return deviceConfigRegistry.contains(deviceConfigId);
    }

    @Override
    public Boolean containsUnitTemplateById(String unitTemplateId) throws CouldNotPerformException {
        return unitTemplateRegistry.contains(unitTemplateId);
    }

    @Override
    public Boolean containsUnitTemplate(UnitTemplate unitTemplate) throws CouldNotPerformException {
        return unitTemplateRegistry.contains(unitTemplate);
    }

    @Override
    public Boolean containsDeviceConfig(DeviceConfig deviceConfig) throws CouldNotPerformException {
        return deviceConfigRegistry.contains(deviceConfig);
    }

    @Override
    public UnitTemplate updateUnitTemplate(UnitTemplate unitTemplate) throws CouldNotPerformException {
        return unitTemplateRegistry.update(unitTemplate);
    }

    @Override
    public DeviceConfig updateDeviceConfig(DeviceConfig deviceConfig) throws CouldNotPerformException {
        return deviceConfigRegistry.update(deviceConfig);
    }

    @Override
    public DeviceConfig removeDeviceConfig(DeviceConfig deviceConfig) throws CouldNotPerformException {
        return deviceConfigRegistry.remove(deviceConfig);
    }

    @Override
    public DeviceClass registerDeviceClass(DeviceClass deviceClass) throws CouldNotPerformException {
        return deviceClassRegistry.register(deviceClass);
    }

    @Override
    public Boolean containsDeviceClassById(String deviceClassId) throws CouldNotPerformException {
        return deviceClassRegistry.contains(deviceClassId);
    }

    @Override
    public Boolean containsDeviceClass(DeviceClass deviceClass) throws CouldNotPerformException {
        return deviceClassRegistry.contains(deviceClass);
    }

    @Override
    public DeviceClass updateDeviceClass(DeviceClass deviceClass) throws CouldNotPerformException {
        return deviceClassRegistry.update(deviceClass);
    }

    @Override
    public DeviceClass removeDeviceClass(DeviceClass deviceClass) throws CouldNotPerformException {
        return deviceClassRegistry.remove(deviceClass);
    }

    @Override
    public List<UnitTemplate> getUnitTemplates() throws CouldNotPerformException {
        return unitTemplateRegistry.getMessages();
    }

    @Override
    public List<DeviceClass> getDeviceClasses() throws CouldNotPerformException {
        return deviceClassRegistry.getMessages();
    }

    @Override
    public List<DeviceConfig> getDeviceConfigs() throws CouldNotPerformException {
        return deviceConfigRegistry.getMessages();
    }

    @Override
    public List<UnitConfig> getUnitConfigs() throws CouldNotPerformException {
        List<UnitConfig> unitConfigs = new ArrayList<>();
        for (IdentifiableMessage<String, DeviceConfig, DeviceConfig.Builder> deviceConfig : deviceConfigRegistry.getEntries()) {
            unitConfigs.addAll(deviceConfig.getMessage().getUnitConfigList());
        }
        return unitConfigs;
    }

    @Override
    public List<ServiceConfig> getServiceConfigs() throws CouldNotPerformException {
        List<ServiceConfig> serviceConfigs = new ArrayList<>();
        for (UnitConfig unitConfig : getUnitConfigs()) {
            serviceConfigs.addAll(unitConfig.getServiceConfigList());
        }
        return serviceConfigs;
    }

    @Override
    public UnitTemplate getUnitTemplateByType(final UnitType type) throws CouldNotPerformException {
        for (UnitTemplate unitTemplate : unitTemplateRegistry.getMessages()) {
            if (unitTemplate.getType() == type) {
                return unitTemplate;
            }
        }
        throw new NotAvailableException("unit template", "No UnitTemplate with given type registered!");
    }

    @Override
    public Future<Boolean> isUnitTemplateRegistryReadOnly() throws CouldNotPerformException {
        return CompletableFuture.completedFuture(unitTemplateRegistry.isReadOnly());
    }

    @Override
    public Future<Boolean> isDeviceClassRegistryReadOnly() throws CouldNotPerformException {
        return CompletableFuture.completedFuture(deviceClassRegistry.isReadOnly());
    }

    @Override
    public Future<Boolean> isDeviceConfigRegistryReadOnly() throws CouldNotPerformException {
        return CompletableFuture.completedFuture(deviceConfigRegistry.isReadOnly());
    }

    @Override
    public List<UnitConfig> getUnitConfigs(final UnitType type) throws CouldNotPerformException {
        List<UnitConfig> unitConfigs = new ArrayList<>();
        for (IdentifiableMessage<String, DeviceConfig, DeviceConfig.Builder> deviceConfig : deviceConfigRegistry.getEntries()) {
            for (UnitConfig unitConfig : deviceConfig.getMessage().getUnitConfigList()) {
                if (type == UnitType.UNKNOWN || unitConfig.getType() == type) {
                    unitConfigs.add(unitConfig);
                }
            }
        }
        return unitConfigs;
    }

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

    @Override
    public UnitGroupConfig registerUnitGroupConfig(UnitGroupConfig groupConfig) throws CouldNotPerformException {
        return unitGroupConfigRegistry.register(groupConfig);
    }

    @Override
    public Boolean containsUnitGroupConfig(UnitGroupConfig groupConfig) throws CouldNotPerformException {
        return unitGroupConfigRegistry.contains(groupConfig);
    }

    @Override
    public Boolean containsUnitGroupConfigById(String groupConfigId) throws CouldNotPerformException {
        return unitGroupConfigRegistry.contains(groupConfigId);
    }

    @Override
    public UnitGroupConfig updateUnitGroupConfig(UnitGroupConfig groupConfig) throws CouldNotPerformException {
        return unitGroupConfigRegistry.update(groupConfig);
    }

    @Override
    public UnitGroupConfig removeUnitGroupConfig(UnitGroupConfig groupConfig) throws CouldNotPerformException {
        return unitGroupConfigRegistry.remove(groupConfig);
    }

    @Override
    public UnitGroupConfig getUnitGroupConfigById(String groupConfigId) throws CouldNotPerformException {
        return unitGroupConfigRegistry.get(groupConfigId).getMessage();
    }

    @Override
    public List<UnitGroupConfig> getUnitGroupConfigs() throws CouldNotPerformException {
        List<UnitGroupConfig> unitGroups = new ArrayList<>();
        for (IdentifiableMessage<String, UnitGroupConfig, UnitGroupConfig.Builder> unitGroup : unitGroupConfigRegistry.getEntries()) {
            unitGroups.add(unitGroup.getMessage());
        }
        return unitGroups;
    }

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

    @Override
    public List<UnitGroupConfig> getUnitGroupConfigsByUnitType(UnitType type) throws CouldNotPerformException {
        List<UnitGroupConfig> unitGroups = new ArrayList<>();
        for (UnitGroupConfig unitGroup : getUnitGroupConfigs()) {
            if (unitGroup.getUnitType() == type) {
                unitGroups.add(unitGroup);
            }
        }
        return unitGroups;
    }

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

    @Override
    public List<UnitConfig> getUnitConfigsByUnitGroupConfig(UnitGroupConfig groupConfig) throws CouldNotPerformException {
        List<UnitConfig> unitConfigs = new ArrayList<>();
        for (String unitId : groupConfig.getMemberIdList()) {
            unitConfigs.add(getUnitConfigById(unitId));
        }
        return unitConfigs;
    }

    @Override
    public Future<Boolean> isUnitGroupConfigRegistryReadOnly() throws CouldNotPerformException {
        return CompletableFuture.completedFuture(unitGroupConfigRegistry.isReadOnly());
    }

    @Override
    public List<UnitConfig> getUnitConfigsByUnitTypeAndServiceTypes(final UnitType type, final List<ServiceType> serviceTypes) throws CouldNotPerformException {

        List<UnitConfig> unitConfigs = getUnitConfigs(type);

        boolean foundServiceType;

        for (UnitConfig unitConfig : new ArrayList<>(unitConfigs)) {
            foundServiceType = false;
            for (ServiceConfig serviceConfig : unitConfig.getServiceConfigList()) {
                for (ServiceType serviceType : serviceTypes) {
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
}
