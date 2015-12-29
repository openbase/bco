/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dm.core.consistency;

import org.dc.jul.exception.CouldNotPerformException;
import org.dc.jul.extension.protobuf.IdentifiableMessage;
import org.dc.jul.extension.protobuf.container.ProtoBufMessageMapInterface;
import org.dc.jul.storage.registry.AbstractProtoBufRegistryConsistencyHandler;
import org.dc.jul.storage.registry.EntryModification;
import org.dc.jul.storage.registry.ProtoBufRegistryInterface;
import java.util.ArrayList;
import java.util.List;
import rst.homeautomation.unit.UnitGroupConfigType.UnitGroupConfig;

/**
 *
 * @author <a href="mailto:thuxohl@techfak.uni-bielefeld.com">Tamino Huxohl</a>
 */
public class UnitGroupMemberListDuplicationConsistencyHandler extends AbstractProtoBufRegistryConsistencyHandler<String, UnitGroupConfig, UnitGroupConfig.Builder> {

    public UnitGroupMemberListDuplicationConsistencyHandler() {
    }

    @Override
    public void processData(String id, IdentifiableMessage<String, UnitGroupConfig, UnitGroupConfig.Builder> entry, ProtoBufMessageMapInterface<String, UnitGroupConfig, UnitGroupConfig.Builder> entryMap, ProtoBufRegistryInterface<String, UnitGroupConfig, UnitGroupConfig.Builder> registry) throws CouldNotPerformException, EntryModification {
        UnitGroupConfig.Builder unitGroup = entry.getMessage().toBuilder();

        unitGroup.clearMemberId();
        boolean modification = false;
        List<String> memberIds = new ArrayList<>();
        for (String memberId : entry.getMessage().getMemberIdList()) {
            if (!memberIds.contains(memberId)) {
                memberIds.add(memberId);
            } else {
                modification = true;
            }
        }

        if (modification) {
            throw new EntryModification(entry.setMessage(unitGroup.addAllMemberId(memberIds)), this);
        }
    }
}
