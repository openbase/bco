/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.csra.dm.registry;

import de.citec.jp.JPDeviceClassDatabaseDirectory;
import de.citec.jul.rsb.IdentifiableMessage;
import de.citec.jp.JPDeviceConfigDatabaseDirectory;
import de.citec.jp.JPDeviceRegistryScope;
import de.citec.jps.core.JPService;
import de.citec.jul.exception.CouldNotPerformException;
import de.citec.jul.rsb.processing.ProtoBufFileProcessor;
import de.citec.jul.rsb.RSBCommunicationService;
import rsb.RSBException;
import rsb.patterns.LocalServer;
import rst.homeautomation.device.DeviceClassType.DeviceClass;
import rst.homeautomation.device.DeviceConfigType.DeviceConfig;
import de.citec.jul.exception.InstantiationException;
import de.citec.jul.exception.InvalidStateException;
import de.citec.jul.exception.NotAvailableException;
import de.citec.jul.pattern.Observable;
import de.citec.jul.pattern.Observer;
import de.citec.jul.rsb.MessageTransformer;
import de.citec.jul.rsb.ProtobufMessageMap;
import de.citec.jul.rsb.RPCHelper;
import de.citec.jul.storage.file.ProtoBufJSonFileProvider;
import de.citec.jul.storage.registry.ProtoBufFileSynchronizedRegistry;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rst.homeautomation.device.DeviceClassType;
import rst.homeautomation.device.DeviceConfigType;
import rst.homeautomation.device.DeviceRegistryType.DeviceRegistry;
import rst.homeautomation.service.ServiceConfigType;
import rst.homeautomation.unit.UnitConfigType;
import rst.homeautomation.unit.UnitConfigType.UnitConfig;

/**
 *
 * @author mpohling
 */
public class DeviceRegistryImpl extends RSBCommunicationService<DeviceRegistry, DeviceRegistry.Builder> implements DeviceRegistryInterface {

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(DeviceRegistry.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(DeviceClassType.DeviceClass.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(DeviceConfigType.DeviceConfig.getDefaultInstance()));
    }

    private ProtoBufFileSynchronizedRegistry<String, DeviceClass, DeviceClass.Builder, DeviceRegistry.Builder> deviceClassRegistry;
    private ProtoBufFileSynchronizedRegistry<String, DeviceConfig, DeviceConfig.Builder, DeviceRegistry.Builder> deviceConfigRegistry;
    private MessageTransformer<DeviceClass, DeviceClass.Builder> deviceClassMessageTransformer;
    private MessageTransformer<DeviceConfig, DeviceClass.Builder> deviceConfigMessageTransformer;

    public DeviceRegistryImpl() throws InstantiationException {
        super(JPService.getProperty(JPDeviceRegistryScope.class).getValue(), DeviceRegistry.newBuilder());
        try {
			ProtoBufJSonFileProvider protoBufJSonFileProvider = new ProtoBufJSonFileProvider();
			ProtoBufFileProcessor<IdentifiableMessage<String, DeviceClass>, DeviceClass, DeviceClass.Builder> deviceClassFileProcessor = new ProtoBufFileProcessor<>(deviceClassMessageTransformer);
            deviceClassRegistry = new ProtoBufFileSynchronizedRegistry<>(DeviceClass.class, getData(), getFieldDescriptor(DeviceRegistry.DEVICE_CLASSES_FIELD_NUMBER), new DeviceClassIdGenerator(), JPService.getProperty(JPDeviceClassDatabaseDirectory.class).getValue(), protoBufJSonFileProvider);
			ProtoBufFileProcessor<IdentifiableMessage<String, DeviceConfig>, DeviceConfig, DeviceConfig.Builder> deviceConfigFileProcessor = new ProtoBufFileProcessor<>(deviceConfigMessageTransformer);
            deviceConfigRegistry = new ProtoBufFileSynchronizedRegistry<>(DeviceConfig.class, getData(), getFieldDescriptor(DeviceRegistry.DEVICE_CONFIGS_FIELD_NUMBER), new DeviceConfigIdGenerator(), JPService.getProperty(JPDeviceConfigDatabaseDirectory.class).getValue(), protoBufJSonFileProvider);
            deviceClassRegistry.loadRegistry();
            deviceConfigRegistry.loadRegistry();

            deviceClassRegistry.addObserver(new Observer<Map<String, IdentifiableMessage<String, DeviceClass>>>() {

                @Override
                public void update(Observable<Map<String, IdentifiableMessage<String, DeviceClass>>> source, Map<String, IdentifiableMessage<String, DeviceClass>> data) throws Exception {
                    notifyChange();
                }
            });
            deviceConfigRegistry.addObserver(new Observer<Map<String, IdentifiableMessage<String, DeviceConfig>>>() {

                @Override
                public void update(Observable<Map<String, IdentifiableMessage<String, DeviceConfig>>> source, Map<String, IdentifiableMessage<String, DeviceConfig>> data) throws Exception {
                    notifyChange();
                }
            });

        } catch (CouldNotPerformException ex) {
            throw new InstantiationException(this, ex);
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
        for (IdentifiableMessage<String, DeviceConfig> deviceConfig : deviceConfigRegistry.getEntries()) {
            for (UnitConfig unitConfig : deviceConfig.getMessage().getUnitConfigsList()) {
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
    public List<UnitConfigType.UnitConfig> getUnitConfigs() throws CouldNotPerformException {
        List<UnitConfigType.UnitConfig> unitConfigs = new ArrayList<>();
        for (IdentifiableMessage<String, DeviceConfig> deviceConfig : deviceConfigRegistry.getEntries()) {
            unitConfigs.addAll(deviceConfig.getMessage().getUnitConfigsList());
        }
        return unitConfigs;
    }

    @Override
    public List<ServiceConfigType.ServiceConfig> getServiceConfigs() throws CouldNotPerformException {
        List<ServiceConfigType.ServiceConfig> serviceConfigs = new ArrayList<>();
        for (UnitConfig unitConfig : getUnitConfigs()) {
            serviceConfigs.addAll(unitConfig.getServiceConfigsList());
        }
        return serviceConfigs;
    }
}
