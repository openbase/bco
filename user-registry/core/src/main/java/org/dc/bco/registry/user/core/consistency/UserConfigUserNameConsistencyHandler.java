/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dc.bco.registry.user.core.consistency;

/*
 * #%L
 * REM UserRegistry Core
 * %%
 * Copyright (C) 2014 - 2016 DivineCooperation
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */

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
