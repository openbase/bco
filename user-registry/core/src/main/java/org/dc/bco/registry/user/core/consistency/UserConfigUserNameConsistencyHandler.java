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
import rst.authorization.UserConfigType.UserConfig;

/**
 *
 * @author <a href="mailto:thuxohl@techfak.uni-bielefeld.com">Tamino Huxohl</a>
 */
public class UserConfigUserNameConsistencyHandler extends AbstractProtoBufRegistryConsistencyHandler<String, UserConfig, UserConfig.Builder> {

    private final Map<String, UserConfig> userMap;

    public UserConfigUserNameConsistencyHandler() {
        this.userMap = new HashMap<>();
    }

    @Override
    public void processData(String id, IdentifiableMessage<String, UserConfig, UserConfig.Builder> entry, ProtoBufMessageMapInterface<String, UserConfig, UserConfig.Builder> entryMap, ProtoBufRegistryInterface<String, UserConfig, UserConfig.Builder> registry) throws CouldNotPerformException, EntryModification {
        UserConfig user = entry.getMessage();

        if (!user.hasUserName() || user.getUserName().isEmpty()) {
            throw new NotAvailableException("user.userName");
        }

        if (!userMap.containsKey(user.getUserName())) {
            userMap.put(user.getUserName(), user);
        } else {
            throw new InvalidStateException("User [" + user + "] and user [" + userMap.get(user.getUserName()) + "] are registered with the same user name!");
        }
    }

    @Override
    public void reset() {
        userMap.clear();
    }
}
