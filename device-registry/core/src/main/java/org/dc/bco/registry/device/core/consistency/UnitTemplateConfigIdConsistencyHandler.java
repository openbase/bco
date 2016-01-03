/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dc.bco.registry.device.core.consistency;

import org.dc.jul.exception.CouldNotPerformException;
import org.dc.jul.extension.protobuf.IdentifiableMessage;
import org.dc.jul.extension.protobuf.container.ProtoBufMessageMapInterface;
import org.dc.jul.storage.registry.AbstractProtoBufRegistryConsistencyHandler;
import org.dc.jul.storage.registry.EntryModification;
import org.dc.jul.storage.registry.ProtoBufRegistryInterface;
import java.util.Map;
import java.util.TreeMap;
import rst.homeautomation.device.DeviceClassType.DeviceClass;
import rst.homeautomation.unit.UnitTemplateConfigType.UnitTemplateConfig;

/**
 *
 * @author <a href="mailto:thuxohl@techfak.uni-bielefeld.com">Tamino Huxohl</a>
 */
public class UnitTemplateConfigIdConsistencyHandler extends AbstractProtoBufRegistryConsistencyHandler<String, DeviceClass, DeviceClass.Builder> {

    private final Map<String, UnitTemplateConfig> unitTemplateMap;

    public UnitTemplateConfigIdConsistencyHandler() {
        this.unitTemplateMap = new TreeMap<>();
    }

    @Override
    public void processData(String id, IdentifiableMessage<String, DeviceClass, DeviceClass.Builder> entry, ProtoBufMessageMapInterface<String, DeviceClass, DeviceClass.Builder> entryMap, ProtoBufRegistryInterface<String, DeviceClass, DeviceClass.Builder> registry) throws CouldNotPerformException, EntryModification {
        DeviceClass.Builder deviceClass = entry.getMessage().toBuilder();

        deviceClass.clearUnitTemplateConfig();
        boolean modification = false;
        for (UnitTemplateConfig.Builder unitTemplateConfig : entry.getMessage().toBuilder().getUnitTemplateConfigBuilderList()) {
            if (!unitTemplateConfig.hasId() || unitTemplateConfig.getId().isEmpty()) {
                unitTemplateConfig.setId(generateUnitTemplateConfigId(deviceClass.getId(), unitTemplateConfig));
                modification = true;
            }
            unitTemplateMap.put(unitTemplateConfig.getId(), unitTemplateConfig.build());
            deviceClass.addUnitTemplateConfig(unitTemplateConfig);
        }

        if (modification) {
            throw new EntryModification(entry.setMessage(deviceClass), this);
        }
    }

    private String generateUnitTemplateConfigId(String deviceClassId, UnitTemplateConfig.Builder unitTemplateConfig) {
        int number = 0;
        String unitConfigTemplateTypeId = deviceClassId + "_" + unitTemplateConfig.getType().name() + "_" + number;
        while (unitTemplateMap.containsKey(unitConfigTemplateTypeId)) {
            number++;
            unitConfigTemplateTypeId = deviceClassId + "_" + unitTemplateConfig.getType().name() + "_" + number;
        }
        return unitConfigTemplateTypeId;
    }

    @Override
    public void reset() {
        unitTemplateMap.clear();
    }
}
