/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dm.core.registry;

import de.citec.dm.core.consistency.DeviceConfigDeviceClassIdConsistencyHandler;
import de.citec.dm.lib.registry.DeviceRegistryInterface;
import de.citec.dm.core.consistency.DeviceIdConsistencyHandler;
import de.citec.dm.core.consistency.DeviceLabelConsistencyHandler;
import de.citec.dm.core.consistency.DeviceLocationIdConsistencyHandler;
import de.citec.dm.lib.generator.DeviceConfigIdGenerator;
import de.citec.dm.lib.generator.DeviceClassIdGenerator;
import de.citec.dm.core.consistency.DeviceScopeConsistencyHandler;
import de.citec.dm.core.consistency.OpenhabServiceConfigItemIdConsistenyHandler;
import de.citec.dm.core.consistency.ServiceConfigBindingTypeConsistencyHandler;
import de.citec.dm.core.consistency.ServiceConfigUnitIdConsistencyHandler;
import de.citec.dm.core.consistency.UnitIdConsistencyHandler;
import de.citec.dm.core.consistency.UnitLabelConsistencyHandler;
import de.citec.dm.core.consistency.UnitLocationIdConsistencyHandler;
import de.citec.dm.core.consistency.UnitScopeConsistencyHandler;
import de.citec.dm.core.consistency.UnitTemplateValidationConsistencyHandler;
import de.citec.dm.core.plugin.UnitTemplateCreatorRegistryPlugin;
import de.citec.dm.lib.generator.UnitTemplateIdGenerator;
import de.citec.jp.JPDeviceClassDatabaseDirectory;
import de.citec.jp.JPDeviceConfigDatabaseDirectory;
import de.citec.jp.JPDeviceRegistryScope;
import de.citec.jp.JPUnitTemplateDatabaseDirectory;
import de.citec.jps.core.JPService;
import de.citec.jul.exception.CouldNotPerformException;
import de.citec.jul.exception.ExceptionPrinter;
import de.citec.jul.exception.InitializationException;
import de.citec.jul.exception.NotAvailableException;
import de.citec.jul.pattern.Observable;
import de.citec.jul.pattern.Observer;
import de.citec.jul.storage.file.ProtoBufJSonFileProvider;
import de.citec.jul.storage.registry.ProtoBufFileSynchronizedRegistry;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rst.homeautomation.device.DeviceClassType.DeviceClass;
import rst.homeautomation.device.DeviceConfigType.DeviceConfig;
import rst.homeautomation.device.DeviceRegistryType.DeviceRegistry;
import rst.homeautomation.service.ServiceConfigType;
import rst.homeautomation.unit.UnitConfigType;
import rst.homeautomation.unit.UnitConfigType.UnitConfig;
import de.citec.jul.exception.InstantiationException;
import de.citec.jul.extension.rsb.com.RSBCommunicationService;
import de.citec.jul.extension.rsb.iface.RSBLocalServerInterface;
import de.citec.jul.extension.protobuf.IdentifiableMessage;
import de.citec.jul.extension.rsb.com.RPCHelper;
import de.citec.lm.remote.LocationRegistryRemote;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
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
    }

    private ProtoBufFileSynchronizedRegistry<String, UnitTemplate, UnitTemplate.Builder, DeviceRegistry.Builder> unitTemplateRegistry;
    private ProtoBufFileSynchronizedRegistry<String, DeviceClass, DeviceClass.Builder, DeviceRegistry.Builder> deviceClassRegistry;
    private ProtoBufFileSynchronizedRegistry<String, DeviceConfig, DeviceConfig.Builder, DeviceRegistry.Builder> deviceConfigRegistry;

    private final LocationRegistryRemote locationRegistryRemote;
    private Observer<LocationRegistry> locationRegistryUpdateObserver;

    public DeviceRegistryService() throws InstantiationException, InterruptedException {
        super(DeviceRegistry.newBuilder());
        try {
            ProtoBufJSonFileProvider protoBufJSonFileProvider = new ProtoBufJSonFileProvider();
            unitTemplateRegistry = new ProtoBufFileSynchronizedRegistry<>(UnitTemplate.class, getBuilderSetup(), getFieldDescriptor(DeviceRegistry.UNIT_TEMPLATE_FIELD_NUMBER), new UnitTemplateIdGenerator(), JPService.getProperty(JPUnitTemplateDatabaseDirectory.class).getValue(), protoBufJSonFileProvider);
            deviceClassRegistry = new ProtoBufFileSynchronizedRegistry<>(DeviceClass.class, getBuilderSetup(), getFieldDescriptor(DeviceRegistry.DEVICE_CLASS_FIELD_NUMBER), new DeviceClassIdGenerator(), JPService.getProperty(JPDeviceClassDatabaseDirectory.class).getValue(), protoBufJSonFileProvider);
            deviceConfigRegistry = new ProtoBufFileSynchronizedRegistry<>(DeviceConfig.class, getBuilderSetup(), getFieldDescriptor(DeviceRegistry.DEVICE_CONFIG_FIELD_NUMBER), new DeviceConfigIdGenerator(), JPService.getProperty(JPDeviceConfigDatabaseDirectory.class).getValue(), protoBufJSonFileProvider);

            locationRegistryUpdateObserver = new Observer<LocationRegistry>() {

                @Override
                public void update(Observable<LocationRegistry> source, LocationRegistry data) throws Exception {
                    deviceConfigRegistry.checkConsistency();
                }
            };

            locationRegistryRemote = new LocationRegistryRemote();

            unitTemplateRegistry.loadRegistry();
            deviceClassRegistry.loadRegistry();
            deviceConfigRegistry.loadRegistry();

            deviceConfigRegistry.registerConsistencyHandler(new DeviceIdConsistencyHandler());
            deviceConfigRegistry.registerConsistencyHandler(new DeviceConfigDeviceClassIdConsistencyHandler(deviceClassRegistry));
            deviceConfigRegistry.registerConsistencyHandler(new DeviceLabelConsistencyHandler());
            deviceConfigRegistry.registerConsistencyHandler(new DeviceLocationIdConsistencyHandler(locationRegistryRemote));
            deviceConfigRegistry.registerConsistencyHandler(new DeviceScopeConsistencyHandler(locationRegistryRemote));
            deviceConfigRegistry.registerConsistencyHandler(new UnitIdConsistencyHandler());
            deviceConfigRegistry.registerConsistencyHandler(new UnitLabelConsistencyHandler());
            deviceConfigRegistry.registerConsistencyHandler(new UnitLocationIdConsistencyHandler(locationRegistryRemote));
            deviceConfigRegistry.registerConsistencyHandler(new UnitScopeConsistencyHandler(locationRegistryRemote));
            deviceConfigRegistry.registerConsistencyHandler(new ServiceConfigUnitIdConsistencyHandler());
            deviceConfigRegistry.registerConsistencyHandler(new ServiceConfigBindingTypeConsistencyHandler(deviceClassRegistry));
            deviceConfigRegistry.registerConsistencyHandler(new OpenhabServiceConfigItemIdConsistenyHandler(locationRegistryRemote, deviceClassRegistry));
            unitTemplateRegistry.registerConsistencyHandler(new UnitTemplateValidationConsistencyHandler());
            
            
            unitTemplateRegistry.addPlugin(new UnitTemplateCreatorRegistryPlugin(unitTemplateRegistry));
            
            unitTemplateRegistry.addObserver(new Observer<Map<String, IdentifiableMessage<String, UnitTemplate, UnitTemplate.Builder>>>() {

                @Override
                public void update(Observable<Map<String, IdentifiableMessage<String, UnitTemplate, UnitTemplate.Builder>>> source, Map<String, IdentifiableMessage<String, UnitTemplate, UnitTemplate.Builder>> data) throws Exception {
                    notifyChange();
                }
            });
            
            deviceClassRegistry.addObserver(new Observer<Map<String, IdentifiableMessage<String, DeviceClass, DeviceClass.Builder>>>() {

                @Override
                public void update(Observable<Map<String, IdentifiableMessage<String, DeviceClass, DeviceClass.Builder>>> source, Map<String, IdentifiableMessage<String, DeviceClass, DeviceClass.Builder>> data) throws Exception {
                    notifyChange();
                }
            });
            
            deviceConfigRegistry.addObserver(new Observer<Map<String, IdentifiableMessage<String, DeviceConfig, DeviceConfig.Builder>>>() {

                @Override
                public void update(Observable<Map<String, IdentifiableMessage<String, DeviceConfig, DeviceConfig.Builder>>> source, Map<String, IdentifiableMessage<String, DeviceConfig, DeviceConfig.Builder>> data) throws Exception {
                    notifyChange();
                }
            });

        } catch (CouldNotPerformException ex) {
            throw new InstantiationException(this, ex);
        }
    }

    public void init() throws InitializationException {
        super.init(JPService.getProperty(JPDeviceRegistryScope.class).getValue());
        locationRegistryRemote.init();
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
            deviceClassRegistry.checkConsistency();
        } catch (CouldNotPerformException ex) {
            logger.warn("Initial consistency check failed!");
        }

        try {
            deviceConfigRegistry.checkConsistency();
        } catch (CouldNotPerformException ex) {
            logger.warn("Initial consistency check failed!");
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

        try {
            deactivate();
        } catch (CouldNotPerformException | InterruptedException ex) {
            ExceptionPrinter.printHistory(logger, ex);
        }
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
    public List<UnitConfigType.UnitConfig> getUnitConfigs() throws CouldNotPerformException {
        List<UnitConfigType.UnitConfig> unitConfigs = new ArrayList<>();
        for (IdentifiableMessage<String, DeviceConfig, DeviceConfig.Builder> deviceConfig : deviceConfigRegistry.getEntries()) {
            unitConfigs.addAll(deviceConfig.getMessage().getUnitConfigList());
        }
        return unitConfigs;
    }

    @Override
    public List<ServiceConfigType.ServiceConfig> getServiceConfigs() throws CouldNotPerformException {
        List<ServiceConfigType.ServiceConfig> serviceConfigs = new ArrayList<>();
        for (UnitConfig unitConfig : getUnitConfigs()) {
            serviceConfigs.addAll(unitConfig.getServiceConfigList());
        }
        return serviceConfigs;
    }

    @Override
    public UnitTemplate getUnitTemplateByType(final UnitType type) throws CouldNotPerformException {
        for(UnitTemplate unitTemplate : unitTemplateRegistry.getMessages()) {
            if(unitTemplate.getType() == type) {
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
}
