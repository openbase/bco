/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.csra.dm.registry;

import de.citec.csra.dm.consistency.DeviceLabelConsistencyHandler;
import de.citec.csra.dm.generator.DeviceConfigIdGenerator;
import de.citec.csra.dm.generator.DeviceClassIdGenerator;
import de.citec.csra.dm.consistency.DeviceScopeConsistencyHandler;
import de.citec.csra.dm.consistency.TransformationConsistencyHandler;
import de.citec.csra.dm.consistency.UnitIdConsistencyHandler;
import de.citec.csra.dm.consistency.UnitScopeConsistencyHandler;
import de.citec.jp.JPDeviceClassDatabaseDirectory;
import de.citec.jp.JPDeviceConfigDatabaseDirectory;
import de.citec.jp.JPDeviceRegistryScope;
import de.citec.jps.core.JPService;
import de.citec.jul.exception.CouldNotPerformException;
import de.citec.jul.exception.NotAvailableException;
import de.citec.jul.pattern.Observable;
import de.citec.jul.pattern.Observer;
import de.citec.jul.rsb.container.IdentifiableMessage;
import de.citec.jul.rsb.container.transformer.MessageTransformer;
import de.citec.jul.rsb.util.RPCHelper;
import de.citec.jul.storage.file.ProtoBufJSonFileProvider;
import de.citec.jul.storage.registry.ProtoBufFileSynchronizedRegistry;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import rsb.RSBException;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rsb.patterns.LocalServer;
import rst.homeautomation.device.DeviceClassType;
import rst.homeautomation.device.DeviceClassType.DeviceClass;
import rst.homeautomation.device.DeviceConfigType;
import rst.homeautomation.device.DeviceConfigType.DeviceConfig;
import rst.homeautomation.device.DeviceRegistryType.DeviceRegistry;
import rst.homeautomation.service.ServiceConfigType;
import rst.homeautomation.unit.UnitConfigType;
import rst.homeautomation.unit.UnitConfigType.UnitConfig;
import de.citec.jul.exception.InstantiationException;
import de.citec.jul.rsb.com.RSBCommunicationService;

/**
 *
 * @author mpohling
 */
public class DeviceRegistryService extends RSBCommunicationService<DeviceRegistry, DeviceRegistry.Builder> implements DeviceRegistryInterface {

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(DeviceRegistry.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(DeviceClassType.DeviceClass.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(DeviceConfigType.DeviceConfig.getDefaultInstance()));
    }

    private ProtoBufFileSynchronizedRegistry<String, DeviceClass, DeviceClass.Builder, DeviceRegistry.Builder> deviceClassRegistry;
    private ProtoBufFileSynchronizedRegistry<String, DeviceConfig, DeviceConfig.Builder, DeviceRegistry.Builder> deviceConfigRegistry;
    private MessageTransformer<DeviceClass, DeviceClass.Builder> deviceClassMessageTransformer;
    private MessageTransformer<DeviceConfig, DeviceClass.Builder> deviceConfigMessageTransformer;

    public DeviceRegistryService() throws InstantiationException {
        super(JPService.getProperty(JPDeviceRegistryScope.class).getValue(), DeviceRegistry.newBuilder());
        try {
            ProtoBufJSonFileProvider protoBufJSonFileProvider = new ProtoBufJSonFileProvider();
            deviceClassRegistry = new ProtoBufFileSynchronizedRegistry<>(DeviceClass.class, getData(), getFieldDescriptor(DeviceRegistry.DEVICE_CLASS_FIELD_NUMBER), new DeviceClassIdGenerator(), JPService.getProperty(JPDeviceClassDatabaseDirectory.class).getValue(), protoBufJSonFileProvider);
            deviceConfigRegistry = new ProtoBufFileSynchronizedRegistry<>(DeviceConfig.class, getData(), getFieldDescriptor(DeviceRegistry.DEVICE_CONFIG_FIELD_NUMBER), new DeviceConfigIdGenerator(), JPService.getProperty(JPDeviceConfigDatabaseDirectory.class).getValue(), protoBufJSonFileProvider);
            deviceClassRegistry.loadRegistry();
            deviceConfigRegistry.loadRegistry();

            deviceConfigRegistry.registerConsistencyHandler(new DeviceLabelConsistencyHandler());
            deviceConfigRegistry.registerConsistencyHandler(new DeviceScopeConsistencyHandler());
            deviceConfigRegistry.registerConsistencyHandler(new UnitScopeConsistencyHandler());
            deviceConfigRegistry.registerConsistencyHandler(new UnitIdConsistencyHandler());
//            deviceConfigRegistry.registerConsistencyHandler(new TransformationConsistencyHandler());

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

    @Override
    public void shutdown() {
        if (deviceClassRegistry != null) {
            deviceClassRegistry.shutdown();
        }

        if (deviceConfigRegistry != null) {
            deviceConfigRegistry.shutdown();
        }
    }

    @Override
    public void registerMethods(LocalServer server) throws RSBException {
        RPCHelper.registerInterface(DeviceRegistryInterface.class, this, server);
    }

    @Override
    public DeviceConfig registerDeviceConfig(DeviceConfig deviceConfig) throws CouldNotPerformException {
        return deviceConfigRegistry.register(deviceConfig);
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
    public Boolean containsDeviceConfig(DeviceConfig deviceConfig) throws CouldNotPerformException {
        return deviceConfigRegistry.contains(deviceConfig);
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
}
