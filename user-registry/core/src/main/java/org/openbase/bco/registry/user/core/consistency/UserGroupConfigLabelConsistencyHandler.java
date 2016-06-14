package org.openbase.bco.registry.user.core.consistency;

/*
 * #%L
 * REM UserRegistry Core
 * %%
 * Copyright (C) 2014 - 2016 openbase.org
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
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InvalidStateException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.extension.protobuf.IdentifiableMessage;
import org.openbase.jul.extension.protobuf.container.ProtoBufMessageMapInterface;
import org.openbase.jul.storage.registry.AbstractProtoBufRegistryConsistencyHandler;
import org.openbase.jul.storage.registry.EntryModification;
import org.openbase.jul.storage.registry.ProtoBufRegistryInterface;
import rst.authorization.UserGroupConfigType.UserGroupConfig;

/**
 *
 * @author <a href="mailto:thuxohl@techfak.uni-bielefeld.com">Tamino Huxohl</a>
 */
public class UserGroupConfigLabelConsistencyHandler extends AbstractProtoBufRegistryConsistencyHandler<String, UserGroupConfig, UserGroupConfig.Builder> {

    private final Map<String, UserGroupConfig> userGroupMap;

    public UserGroupConfigLabelConsistencyHandler() {
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
