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

import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.extension.protobuf.IdentifiableMessage;
import org.openbase.jul.extension.protobuf.container.ProtoBufMessageMapInterface;
import org.openbase.jul.extension.rsb.scope.ScopeGenerator;
import org.openbase.jul.storage.registry.AbstractProtoBufRegistryConsistencyHandler;
import org.openbase.jul.storage.registry.EntryModification;
import org.openbase.jul.storage.registry.ProtoBufRegistryInterface;
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
