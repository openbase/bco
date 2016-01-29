/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dc.bco.registry.user.core.consistency;

import java.util.HashMap;
import java.util.Map;
import org.dc.jul.exception.CouldNotPerformException;
import org.dc.jul.exception.InvalidStateException;
import org.dc.jul.exception.NotAvailableException;
import org.dc.jul.extension.protobuf.IdentifiableMessage;
import org.dc.jul.extension.protobuf.container.ProtoBufMessageMapInterface;
import org.dc.jul.storage.registry.AbstractProtoBufRegistryConsistencyHandler;
import org.dc.jul.storage.registry.EntryModification;
import org.dc.jul.storage.registry.ProtoBufRegistryInterface;
import rst.authorization.UserGroupConfigType.UserGroupConfig;

/**
 *
 * @author <a href="mailto:thuxohl@techfak.uni-bielefeld.com">Tamino Huxohl</a>
 */
public class UserGroupLabelConsistencyHandler extends AbstractProtoBufRegistryConsistencyHandler<String, UserGroupConfig, UserGroupConfig.Builder> {

    private final Map<String, UserGroupConfig> userGroupMap;

    public UserGroupLabelConsistencyHandler() {
        this.userGroupMap = new HashMap<>();
    }

    @Override
    public void processData(String id, IdentifiableMessage<String, UserGroupConfig, UserGroupConfig.Builder> entry, ProtoBufMessageMapInterface<String, UserGroupConfig, UserGroupConfig.Builder> entryMap, ProtoBufRegistryInterface<String, UserGroupConfig, UserGroupConfig.Builder> registry) throws CouldNotPerformException, EntryModification {
        UserGroupConfig userGroup = entry.getMessage();

        if (!userGroup.hasLabel() || userGroup.getLabel().isEmpty()) {
            throw new NotAvailableException("userGroup.label");
        }

        if (!userGroupMap.containsKey(userGroup.getLabel())) {
            userGroupMap.put(userGroup.getLabel(), userGroup);
        } else {
            throw new InvalidStateException("UserGroup [" + userGroup + "] and userGroup [" + userGroupMap.get(userGroup.getLabel()) + "] are registered with the same label!");
        }
    }

    @Override
    public void reset() {
        userGroupMap.clear();
    }
}
