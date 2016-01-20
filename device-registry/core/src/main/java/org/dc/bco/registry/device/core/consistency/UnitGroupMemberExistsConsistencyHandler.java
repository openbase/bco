/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dc.bco.registry.device.core.consistency;

import java.util.ArrayList;
import java.util.List;
import org.dc.jul.exception.CouldNotPerformException;
import org.dc.jul.extension.protobuf.IdentifiableMessage;
import org.dc.jul.extension.protobuf.container.ProtoBufMessageMapInterface;
import org.dc.jul.storage.registry.AbstractProtoBufRegistryConsistencyHandler;
import org.dc.jul.storage.registry.EntryModification;
import org.dc.jul.storage.registry.ProtoBufFileSynchronizedRegistry;
import org.dc.jul.storage.registry.ProtoBufRegistryInterface;
import rst.homeautomation.device.DeviceConfigType.DeviceConfig;
import rst.homeautomation.device.DeviceRegistryType.DeviceRegistry;
import rst.homeautomation.unit.UnitConfigType.UnitConfig;
import rst.homeautomation.unit.UnitGroupConfigType.UnitGroupConfig;

/**
 *
 * @author <a href="mailto:thuxohl@techfak.uni-bielefeld.com">Tamino Huxohl</a>
 */
public class UnitGroupMemberExistsConsistencyHandler extends AbstractProtoBufRegistryConsistencyHandler<String, UnitGroupConfig, UnitGroupConfig.Builder> {

    private ProtoBufFileSynchronizedRegistry<String, DeviceConfig, DeviceConfig.Builder, DeviceRegistry.Builder> deviceConfigRegistry;

    public UnitGroupMemberExistsConsistencyHandler(ProtoBufFileSynchronizedRegistry<String, DeviceConfig, DeviceConfig.Builder, DeviceRegistry.Builder> deviceConfigRegistry) {
        this.deviceConfigRegistry = deviceConfigRegistry;
    }

    @Override
    public void processData(String id, IdentifiableMessage<String, UnitGroupConfig, UnitGroupConfig.Builder> entry, ProtoBufMessageMapInterface<String, UnitGroupConfig, UnitGroupConfig.Builder> entryMap, ProtoBufRegistryInterface<String, UnitGroupConfig, UnitGroupConfig.Builder> registry) throws CouldNotPerformException, EntryModification {
        UnitGroupConfig.Builder unitGroup = entry.getMessage().toBuilder();

        // remove a member id if the according unit does not exists.. is the case if the unit gets removed globally
        boolean modification = false;
        List<String> unitConfigsIds = new ArrayList<>();
        unitGroup.clearMemberId();
        for (String unitConfigId : entry.getMessage().getMemberIdList()) {
            if (containsUnitConfigById(unitConfigId)) {
                unitConfigsIds.add(unitConfigId);
            } else {
                modification = true;
            }
        }
        unitGroup.addAllMemberId(unitConfigsIds);

        if (modification) {
            throw new EntryModification(entry.setMessage(unitGroup.build()), this);
        }
    }

    public boolean containsUnitConfigById(String unitConfigId) throws CouldNotPerformException {
        for (IdentifiableMessage<String, DeviceConfig, DeviceConfig.Builder> deviceConfig : deviceConfigRegistry.getEntries()) {
            for (UnitConfig unitConfig : deviceConfig.getMessage().getUnitConfigList()) {
                if (unitConfig.getId().equals(unitConfigId)) {
                    return true;
                }
            }
        }
        return false;
    }
}
