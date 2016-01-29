/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dc.bco.registry.user.core.consistency;

import org.dc.jul.exception.CouldNotPerformException;
import org.dc.jul.exception.NotAvailableException;
import org.dc.jul.extension.protobuf.IdentifiableMessage;
import org.dc.jul.extension.protobuf.container.ProtoBufMessageMapInterface;
import org.dc.jul.extension.rsb.scope.ScopeGenerator;
import org.dc.jul.storage.registry.AbstractProtoBufRegistryConsistencyHandler;
import org.dc.jul.storage.registry.EntryModification;
import org.dc.jul.storage.registry.ProtoBufRegistryInterface;
import rst.authorization.UserGroupConfigType.UserGroupConfig;
import rst.rsb.ScopeType;

/**
 *
 * @author <a href="mailto:thuxohl@techfak.uni-bielefeld.com">Tamino Huxohl</a>
 */
public class UserGroupConfigScopeConsistencyHandler extends AbstractProtoBufRegistryConsistencyHandler<String, UserGroupConfig, UserGroupConfig.Builder> {

    @Override
    public void processData(String id, IdentifiableMessage<String, UserGroupConfig, UserGroupConfig.Builder> entry, ProtoBufMessageMapInterface<String, UserGroupConfig, UserGroupConfig.Builder> entryMap, ProtoBufRegistryInterface<String, UserGroupConfig, UserGroupConfig.Builder> registry) throws CouldNotPerformException, EntryModification {
        UserGroupConfig userGroup = entry.getMessage();

        if (!userGroup.hasLabel()|| userGroup.getLabel().isEmpty()) {
            throw new NotAvailableException("user.label");
        }

        ScopeType.Scope newScope = ScopeGenerator.generateSceneScope(userGroup);

        // verify and update scope
        if (!ScopeGenerator.generateStringRep(userGroup.getScope()).equals(ScopeGenerator.generateStringRep(newScope))) {
            entry.setMessage(userGroup.toBuilder().setScope(newScope));
            throw new EntryModification(entry, this);
        }
    }

    @Override
    public void reset() {
    }
}
