/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.csra.dm.registry;

import de.citec.csra.dm.DeviceManager;
import de.citec.jp.JPDeviceClassDatabaseDirectory;
import de.citec.jul.storage.SynchronizedRegistry;
import de.citec.jul.rsb.IdentifiableMessage;
import de.citec.jp.JPDeviceConfigDatabaseDirectory;
import de.citec.jps.core.JPService;
import de.citec.jul.exception.CouldNotPerformException;
import de.citec.jul.processing.ProtoBufFileProcessor;
import de.citec.jul.rsb.RSBCommunicationService;
import de.citec.jul.rsb.jp.JPScope;
import rsb.RSBException;
import rsb.patterns.LocalServer;
import rst.homeautomation.device.DeviceClassType.DeviceClass;
import rst.homeautomation.device.DeviceConfigType.DeviceConfig;
import rst.homeautomation.registry.DeviceRegistryType.DeviceRegistry;
import de.citec.jul.exception.InstantiationException;
import de.citec.jul.iface.Identifiable;
import de.citec.jul.rsb.ProtobufMessageMap;
import de.citec.jul.storage.FileNameProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author mpohling
 */
public class DeviceRegistryImpl extends RSBCommunicationService<DeviceRegistry, DeviceRegistry.Builder> {

    private static final Logger logger = LoggerFactory.getLogger(DeviceRegistryImpl.class);
    
    private SynchronizedRegistry<String, IdentifiableMessage<DeviceClass>> deviceClassRegistry;
    private SynchronizedRegistry<String, IdentifiableMessage<DeviceConfig>> deviceConfigRegistry;

    public DeviceRegistryImpl() throws InstantiationException {
        super(JPService.getAttribute(JPScope.class).getValue(), DeviceRegistry.newBuilder());
        try {
            ProtobufMessageMap<String, IdentifiableMessage<DeviceClass>, DeviceRegistry.Builder> deviceClassMap = new ProtobufMessageMap<>(getData(), getFieldDescriptor(DeviceRegistry.DEVICE_CLASSES_FIELD_NUMBER));
            ProtobufMessageMap<String, IdentifiableMessage<DeviceConfig>, DeviceRegistry.Builder> deviceConfigMap = new ProtobufMessageMap<>(getData(), getFieldDescriptor(DeviceRegistry.DEVICE_CONFIGS_FIELD_NUMBER));
            deviceClassRegistry = new SynchronizedRegistry(deviceClassMap, JPService.getAttribute(JPDeviceClassDatabaseDirectory.class).getValue(), new ProtoBufFileProcessor<DeviceClass>(), new DBFileNameProvider());
            deviceConfigRegistry = new SynchronizedRegistry(deviceConfigMap, JPService.getAttribute(JPDeviceConfigDatabaseDirectory.class).getValue(), new ProtoBufFileProcessor<DeviceConfig>(), new DBFileNameProvider());
            deviceClassRegistry.loadRegistry();
            deviceConfigRegistry.loadRegistry();
        } catch (CouldNotPerformException ex) {
            throw new InstantiationException(this, ex);
        }
    }

    @Override
    public void registerMethods(LocalServer server) throws RSBException {
//        server.addMethod(RPC_REQUEST_STATUS, null);
    }

    public class DBFileNameProvider implements FileNameProvider<Identifiable<String>> {

        @Override
        public String getFileName(Identifiable<String> context) {
            return context.getId().replaceAll("/", "_");
        }
    }
}
