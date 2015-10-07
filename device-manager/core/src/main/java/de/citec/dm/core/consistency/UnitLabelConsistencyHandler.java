/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dm.core.consistency;

import de.citec.jul.exception.CouldNotPerformException;
import de.citec.jul.exception.NotAvailableException;
import de.citec.jul.extension.protobuf.IdentifiableMessage;
import de.citec.jul.extension.protobuf.container.ProtoBufMessageMapInterface;
import de.citec.jul.storage.registry.EntryModification;
import de.citec.jul.storage.registry.ProtoBufRegistryConsistencyHandler;
import de.citec.jul.storage.registry.ProtoBufRegistryInterface;
import rst.homeautomation.device.DeviceConfigType;
import rst.homeautomation.unit.UnitConfigType;

/**
 *
 * @author mpohling
 */
public class UnitLabelConsistencyHandler implements ProtoBufRegistryConsistencyHandler<String, DeviceConfigType.DeviceConfig, DeviceConfigType.DeviceConfig.Builder> {

    @Override
    public void processData(String id, IdentifiableMessage<String, DeviceConfigType.DeviceConfig, DeviceConfigType.DeviceConfig.Builder> entry, ProtoBufMessageMapInterface<String, DeviceConfigType.DeviceConfig, DeviceConfigType.DeviceConfig.Builder> entryMap, ProtoBufRegistryInterface<String, DeviceConfigType.DeviceConfig, DeviceConfigType.DeviceConfig.Builder> registry) throws CouldNotPerformException, EntryModification {
        DeviceConfigType.DeviceConfig.Builder deviceConfig = entry.getMessage().toBuilder();

        boolean modification = false;
        deviceConfig.clearUnitConfig();
        for (UnitConfigType.UnitConfig.Builder unitConfig : entry.getMessage().toBuilder().getUnitConfigBuilderList()) {

            // Setup device label if unit has no label configured.
            if (!unitConfig.hasLabel() || unitConfig.getLabel().isEmpty()) {

                // Check if device label is available
                if (!deviceConfig.hasLabel() || deviceConfig.getLabel().isEmpty()) {
                    throw new NotAvailableException("device.label");
                }

                unitConfig.setLabel(deviceConfig.getLabel());
                modification = true;
            }

            deviceConfig.addUnitConfig(unitConfig);
        }

        if (modification) {
            throw new EntryModification(entry.setMessage(deviceConfig), this);
        }
    }

    @Override
    public void reset() {
    }
}
