/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.csra.dm.registry;

import de.citec.jp.JPDeviceClassDatabaseDirectory;
import de.citec.jul.storage.SynchronizedRegistry;
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
import de.citec.jul.iface.Identifiable;
import de.citec.jul.pattern.Observable;
import de.citec.jul.rsb.ProtobufMessageMap;
import de.citec.jul.rsb.RPCHelper;
import de.citec.jul.storage.FileNameProvider;
import java.util.Map;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rst.homeautomation.device.DeviceClassType;
import rst.homeautomation.device.DeviceConfigType;
import rst.homeautomation.device.DeviceRegistryType.DeviceRegistry;

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

    private SynchronizedRegistry<String, IdentifiableMessage<DeviceClass>> deviceClassRegistry;
    private SynchronizedRegistry<String, IdentifiableMessage<DeviceConfig>> deviceConfigRegistry;

    public DeviceRegistryImpl() throws InstantiationException {
        super(JPService.getProperty(JPDeviceRegistryScope.class).getValue(), DeviceRegistry.newBuilder());
        try {
            ProtobufMessageMap<String, IdentifiableMessage<DeviceClass>, DeviceRegistry.Builder> deviceClassMap = new ProtobufMessageMap<>(getData(), getFieldDescriptor(DeviceRegistry.DEVICE_CLASSES_FIELD_NUMBER));
            ProtobufMessageMap<String, IdentifiableMessage<DeviceConfig>, DeviceRegistry.Builder> deviceConfigMap = new ProtobufMessageMap<>(getData(), getFieldDescriptor(DeviceRegistry.DEVICE_CONFIGS_FIELD_NUMBER));
            deviceClassRegistry = new SynchronizedRegistry(deviceClassMap, JPService.getProperty(JPDeviceClassDatabaseDirectory.class).getValue(), new ProtoBufFileProcessor<>(new IdentifiableMessage.MessageTransformer()), new DBFileNameProvider());
            deviceConfigRegistry = new SynchronizedRegistry(deviceConfigMap, JPService.getProperty(JPDeviceConfigDatabaseDirectory.class).getValue(), new ProtoBufFileProcessor<>(new IdentifiableMessage.MessageTransformer()), new DBFileNameProvider());
            deviceClassRegistry.loadRegistry();
			deviceConfigRegistry.loadRegistry();

			deviceClassRegistry.addObserver((Observable<Map<String, IdentifiableMessage<DeviceClass>>> source, Map<String, IdentifiableMessage<DeviceClass>> data1) -> {
				notifyChange();
			});
			deviceConfigRegistry.addObserver((Observable<Map<String, IdentifiableMessage<DeviceConfig>>> source, Map<String, IdentifiableMessage<DeviceConfig>> data1) -> {
				notifyChange();
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
        return deviceConfigRegistry.register(new IdentifiableMessage<>(setupDeviceConfigID(deviceConfig))).getMessageOrBuilder();
    }
    
    @Override
    public Boolean containsDeviceConfigById(String deviceConfigId) throws CouldNotPerformException {
        return deviceConfigRegistry.contrains(deviceConfigId);
    }
    
    @Override
    public Boolean containsDeviceConfig(DeviceConfig deviceConfig) throws CouldNotPerformException {
        if(!deviceConfig.hasId()) {
            deviceConfig = setupDeviceConfigID(deviceConfig);
        }
        return containsDeviceConfigById(deviceConfig.getId());
    }

    @Override
    public DeviceConfig updateDeviceConfig(DeviceConfig deviceConfig) throws CouldNotPerformException {
        return deviceConfigRegistry.update(new IdentifiableMessage<>(deviceConfig)).getMessageOrBuilder();
    }

    @Override
    public DeviceConfig removeDeviceConfig(DeviceConfig deviceConfig) throws CouldNotPerformException {
        return deviceConfigRegistry.remove(new IdentifiableMessage<>(deviceConfig)).getMessageOrBuilder();
    }

    @Override
    public DeviceClass registerDeviceClass(DeviceClass deviceClass) throws CouldNotPerformException {
        return deviceClassRegistry.register(new IdentifiableMessage<>(setupDeviceClassID(deviceClass))).getMessageOrBuilder();
    }
    
    @Override
    public Boolean containsDeviceClassById(String deviceClassId) throws CouldNotPerformException {
        return deviceClassRegistry.contrains(deviceClassId);
    }
    
    @Override
    public Boolean containsDeviceClass(DeviceClass deviceClass) throws CouldNotPerformException {
        if(!deviceClass.hasId()) {
            deviceClass = setupDeviceClassID(deviceClass);
        }
        return containsDeviceClassById(deviceClass.getId());
    }

    @Override
    public DeviceClass updateDeviceClass(DeviceClass deviceClass) throws CouldNotPerformException {
        return deviceClassRegistry.update(new IdentifiableMessage<>(deviceClass)).getMessageOrBuilder();
    }

    @Override
    public DeviceClass removeDeviceClass(DeviceClass deviceClass) throws CouldNotPerformException {
        return deviceClassRegistry.remove(new IdentifiableMessage<>(deviceClass)).getMessageOrBuilder();
    }

    public DeviceClass setupDeviceClassID(final DeviceClass deviceClass) throws InvalidStateException, CouldNotPerformException {
        try {
            if (deviceClass.hasId()) {
                throw new InvalidStateException("ID already specified!");
            }
            return deviceClass.toBuilder().setId(generateDeviceClassID(deviceClass)).build();
        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not setup id!", ex);
        }
    }

    public String generateDeviceClassID(final DeviceClass deviceClass) throws InvalidStateException, CouldNotPerformException {
        try {
            if (!deviceClass.hasLabel()) {
                throw new InvalidStateException("Field [Label] is missing!");
            }
            return convertIntoValidFileName(deviceClass.getLabel());
        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not generate id!", ex);
        }
    }

    public DeviceConfig setupDeviceConfigID(final DeviceConfig deviceConfig) throws InvalidStateException, CouldNotPerformException {
        try {
            if (deviceConfig.hasId()) {
                throw new InvalidStateException("ID already specified!");
            }
            return deviceConfig.toBuilder().setId(generateDeviceConfigID(deviceConfig)).build();
        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not setup id!", ex);
        }
    }

    public String generateDeviceConfigID(final DeviceConfig deviceConfig) throws InvalidStateException, CouldNotPerformException {
        try {
            if (!deviceConfig.hasDeviceClass() | !deviceConfig.getDeviceClass().hasId()) {
                throw new InvalidStateException("Field [DeviceClass] is missing!");
            }

            if (!deviceConfig.hasSerialNumber()) {
                throw new InvalidStateException("Field [SerialNumber] is missing!");
            }

            String id;
            
            id = deviceConfig.getDeviceClass().getId();
            id += "_";
            id += convertIntoValidFileName(deviceConfig.getSerialNumber());
            return id;

        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not generate id!", ex);
        }
    }
    
    public String convertIntoValidFileName(final String filename) {
        return filename.replaceAll("[^0-9a-zA-Z\\-_]+", "_");
    }

    public class DBFileNameProvider implements FileNameProvider<Identifiable<String>> {

        @Override
        public String getFileName(Identifiable<String> context) {
            return context.getId().replaceAll("/", "_");
        }
    }
    
    
}
