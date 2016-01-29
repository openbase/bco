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
import rst.authorization.UserConfigType.UserConfig;
import rst.rsb.ScopeType;

/**
 *
 * @author <a href="mailto:thuxohl@techfak.uni-bielefeld.com">Tamino Huxohl</a>
 */
public class UserConfigScopeConsistencyHandler extends AbstractProtoBufRegistryConsistencyHandler<String, UserConfig, UserConfig.Builder> {

    @Override
    public void processData(String id, IdentifiableMessage<String, UserConfig, UserConfig.Builder> entry, ProtoBufMessageMapInterface<String, UserConfig, UserConfig.Builder> entryMap, ProtoBufRegistryInterface<String, UserConfig, UserConfig.Builder> registry) throws CouldNotPerformException, EntryModification {
        UserConfig user = entry.getMessage();

        if (!user.hasUserName()|| user.getUserName().isEmpty()) {
            throw new NotAvailableException("user.userName");
        }

        ScopeType.Scope newScope = ScopeGenerator.generateSceneScope(user);

        // verify and update scope
        if (!ScopeGenerator.generateStringRep(user.getScope()).equals(ScopeGenerator.generateStringRep(newScope))) {
            entry.setMessage(user.toBuilder().setScope(newScope));
            throw new EntryModification(entry, this);
        }
    }

    @Override
    public void reset() {
    }
}
