/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dm.core.consistency;

import de.citec.jul.exception.CouldNotPerformException;
import de.citec.jul.exception.InstantiationException;
import de.citec.jul.extension.protobuf.IdentifiableMessage;
import de.citec.jul.extension.protobuf.container.ProtoBufMessageMapInterface;
import de.citec.jul.storage.registry.EntryModification;
import de.citec.jul.storage.registry.ProtoBufRegistryConsistencyHandler;
import de.citec.jul.storage.registry.ProtoBufRegistryInterface;
import java.util.ArrayList;
import java.util.List;
import rst.homeautomation.device.DeviceConfigType;
import rst.homeautomation.device.DeviceConfigType.DeviceConfig;
import rst.homeautomation.unit.UnitConfigType.UnitConfig;
import rst.homeautomation.unit.UnitTemplateType;

/**
 *
 * @author mpohling
 */
public class UnitBoundsToDeviceConsistencyHandler implements ProtoBufRegistryConsistencyHandler<String, DeviceConfig, DeviceConfig.Builder> {

    public static final boolean DEFAULT_BOUND_TO_DEVICE = true;

    public UnitBoundsToDeviceConsistencyHandler() throws InstantiationException {
    }

    @Override
    public void processData(String id, IdentifiableMessage<String, DeviceConfig, DeviceConfig.Builder> entry, ProtoBufMessageMapInterface<String, DeviceConfig, DeviceConfig.Builder> entryMap, ProtoBufRegistryInterface<String, DeviceConfig, DeviceConfig.Builder> registry) throws CouldNotPerformException, EntryModification {
        DeviceConfigType.DeviceConfig.Builder deviceConfig = entry.getMessage().toBuilder();

        
        boolean hasDuplicatedUnitType = checkDuplicatedUnitType(deviceConfig.build());
        
        deviceConfig.clearUnitConfig();
        boolean modification = false;
        for (UnitConfig.Builder unitConfig : entry.getMessage().toBuilder().getUnitConfigBuilderList()) {

            // Setup default bounding
            if (!unitConfig.hasBoundToDevice()) {
                unitConfig.setBoundToDevice(DEFAULT_BOUND_TO_DEVICE);
                modification = true;
            }

            // Copy device placement and label if bound to device is enabled.
            if (unitConfig.getBoundToDevice()) {
                
                // copy placement
                if (!unitConfig.getPlacementConfig().equals(deviceConfig.getPlacementConfig())) {
                    unitConfig.setPlacementConfig(deviceConfig.getPlacementConfig());
                    modification = true;
                }
                
                // copy labels
                if (!unitConfig.getLabel().equals(deviceConfig.getLabel()) && !hasDuplicatedUnitType) {
                    unitConfig.setLabel(deviceConfig.getLabel());
                    modification = true;
                }

            }
            deviceConfig.addUnitConfig(unitConfig);
        }

        if (modification) {
            throw new EntryModification(entry.setMessage(deviceConfig).getMessage(), this);
        }
    }
    
    /**
     * Check if the given device configuration contains one unit template type more than once.
     * @param deviceConfig
     * @return true if a duplicated unit type is detected.
     */
    public static boolean checkDuplicatedUnitType(final DeviceConfig deviceConfig) {
        
        List<UnitTemplateType.UnitTemplate.UnitType> unitTypeList = new ArrayList<>();
        for (UnitConfig unitConfig : deviceConfig.getUnitConfigList()) {
            if(unitTypeList.contains(unitConfig.getType())) {
                return true;
            }
            unitTypeList.add(unitConfig.getType());
        }
        return false;
    }

    @Override
    public void reset() {
    }
}
