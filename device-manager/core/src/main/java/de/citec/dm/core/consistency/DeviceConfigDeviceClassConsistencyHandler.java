/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dm.core.consistency;

import de.citec.jul.exception.CouldNotPerformException;
import de.citec.jul.exception.NotAvailableException;
import de.citec.jul.exception.VerificationFailedException;
import de.citec.jul.extension.rsb.container.IdentifiableMessage;
import de.citec.jul.extension.rsb.container.ProtoBufMessageMapInterface;
import de.citec.jul.storage.registry.EntryModification;
import de.citec.jul.storage.registry.ProtoBufFileSynchronizedRegistry;
import de.citec.jul.storage.registry.ProtoBufRegistryConsistencyHandler;
import de.citec.jul.storage.registry.ProtoBufRegistryInterface;
import java.util.HashMap;
import java.util.Map;
import rst.homeautomation.device.DeviceClassType;
import rst.homeautomation.device.DeviceConfigType;
import rst.homeautomation.device.DeviceConfigType.DeviceConfig;
import rst.homeautomation.device.DeviceRegistryType;

/**
 *
 * @author mpohling
 */
public class DeviceConfigDeviceClassConsistencyHandler implements ProtoBufRegistryConsistencyHandler<String, DeviceConfig, DeviceConfig.Builder> {

    private ProtoBufFileSynchronizedRegistry<String, DeviceClassType.DeviceClass, DeviceClassType.DeviceClass.Builder, DeviceRegistryType.DeviceRegistry.Builder> deviceClassRegistry;

    public DeviceConfigDeviceClassConsistencyHandler(ProtoBufFileSynchronizedRegistry<String, DeviceClassType.DeviceClass, DeviceClassType.DeviceClass.Builder, DeviceRegistryType.DeviceRegistry.Builder> deviceClassRegistry) {
        this.deviceClassRegistry = deviceClassRegistry;
    }

    @Override
    public void processData(String id, IdentifiableMessage<String, DeviceConfig, DeviceConfig.Builder> entry, ProtoBufMessageMapInterface<String, DeviceConfig, DeviceConfig.Builder> entryMap, ProtoBufRegistryInterface<String, DeviceConfig, DeviceConfig.Builder> registry) throws CouldNotPerformException, EntryModification {
        DeviceConfigType.DeviceConfig deviceConfig = entry.getMessage();

        if (!deviceConfig.hasDeviceClass()) {
            throw new NotAvailableException("deviceclass");
        }

        if (!deviceConfig.getDeviceClass().hasId() || deviceConfig.getDeviceClass().getId().isEmpty()) {
            throw new NotAvailableException("deviceclass.id");
        }

        String deviceClassId = deviceConfig.getDeviceClass().getId();

        // register device config class if the class is unknown for device class registry.
        if (!deviceClassRegistry.contains(deviceClassId)) {
            deviceClassRegistry.register(deviceConfig.getDeviceClass());
        }

        DeviceClassType.DeviceClass registeredDeviceClass = deviceClassRegistry.getMessage(deviceClassId);

        // update internal class construct on change
        if (!registeredDeviceClass.toString().equals(deviceConfig.getDeviceClass().toString())) {
            entry.setMessage(deviceConfig.toBuilder().setDeviceClass(registeredDeviceClass));
            throw new EntryModification(entry, this);
        }
    }

    @Override
    public void reset() {}
}
