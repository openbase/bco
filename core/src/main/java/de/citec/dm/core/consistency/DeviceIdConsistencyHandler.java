/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dm.core.consistency;

import de.citec.dm.lib.generator.UnitConfigIdGenerator;
import de.citec.jul.exception.CouldNotPerformException;
import de.citec.jul.extension.rsb.container.IdentifiableMessage;
import de.citec.jul.extension.rsb.container.ProtoBufMessageMapInterface;
import de.citec.jul.storage.registry.EntryModification;
import de.citec.jul.storage.registry.ProtoBufRegistryConsistencyHandler;
import de.citec.jul.storage.registry.ProtoBufRegistryInterface;
import rst.homeautomation.device.DeviceConfigType.DeviceConfig;
import rst.homeautomation.unit.UnitConfigType;

/**
 *
 * @author thuxohl
 */
public class DeviceIdConsistencyHandler implements ProtoBufRegistryConsistencyHandler<String, DeviceConfig, DeviceConfig.Builder> {

    public DeviceIdConsistencyHandler() {
    }

    @Override
    public void processData(String id, IdentifiableMessage<String, DeviceConfig, DeviceConfig.Builder> entry, ProtoBufMessageMapInterface<String, DeviceConfig, DeviceConfig.Builder> entryMap, ProtoBufRegistryInterface<String, DeviceConfig, DeviceConfig.Builder> registry) throws CouldNotPerformException, EntryModification {
        DeviceConfig.Builder deviceConfig = entry.getMessage().toBuilder();

        deviceConfig.clearUnitConfig();
        boolean modification = false;
        for (UnitConfigType.UnitConfig.Builder unitConfig : entry.getMessage().toBuilder().getUnitConfigBuilderList()) {
            if (!unitConfig.hasDeviceId() || unitConfig.getDeviceId().isEmpty() || !unitConfig.getDeviceId().equals(deviceConfig.getId())) {
                unitConfig.setDeviceId(deviceConfig.getId());
                modification = true;
            }
            deviceConfig.addUnitConfig(unitConfig);
        }

        if (modification) {
            throw new EntryModification(entry.setMessage(deviceConfig).getMessage(), this);
        }
    }
    
    @Override
    public void reset() {
    }
}
