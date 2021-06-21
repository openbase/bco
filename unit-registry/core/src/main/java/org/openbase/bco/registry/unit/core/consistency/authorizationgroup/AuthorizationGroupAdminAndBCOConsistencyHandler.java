package org.openbase.bco.registry.unit.core.consistency.authorizationgroup;

/*-
 * #%L
 * BCO Registry Unit Core
 * %%
 * Copyright (C) 2014 - 2021 openbase.org
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

import org.openbase.bco.registry.unit.lib.UnitRegistry;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.extension.protobuf.IdentifiableMessage;
import org.openbase.jul.extension.protobuf.container.ProtoBufMessageMap;
import org.openbase.jul.storage.registry.AbstractProtoBufRegistryConsistencyHandler;
import org.openbase.jul.storage.registry.EntryModification;
import org.openbase.jul.storage.registry.ProtoBufRegistry;
import org.openbase.type.domotic.unit.UnitConfigType.UnitConfig;
import org.openbase.type.domotic.unit.UnitConfigType.UnitConfig.Builder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * This consistency handler makes sure that the admin and bco user are always registered in their according group.
 *
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class AuthorizationGroupAdminAndBCOConsistencyHandler extends AbstractProtoBufRegistryConsistencyHandler<String, UnitConfig, Builder> {

    private final Map<String, String> aliasIdMap;

    public AuthorizationGroupAdminAndBCOConsistencyHandler(final Map<String, String> aliasIdMap) {
        this.aliasIdMap = aliasIdMap;
    }

    @Override
    public void processData(final String id,
                            final IdentifiableMessage<String, UnitConfig, Builder> entry,
                            final ProtoBufMessageMap<String, UnitConfig, Builder> entryMap, ProtoBufRegistry<String, UnitConfig, Builder> registry)
            throws CouldNotPerformException, EntryModification {
        if (entry.getMessage().getAliasList().contains(UnitRegistry.ADMIN_GROUP_ALIAS)) {
            addUserToGroupIfMissing(UnitRegistry.ADMIN_USER_ALIAS, entry);
        } else if (entry.getMessage().getAliasList().contains(UnitRegistry.BCO_GROUP_ALIAS)) {
            addUserToGroupIfMissing(UnitRegistry.BCO_USER_ALIAS, entry);
        }
    }

    private void addUserToGroupIfMissing(final String userAlias, final IdentifiableMessage<String, UnitConfig, Builder> entry) throws EntryModification, CouldNotPerformException {
        final String userId = aliasIdMap.get(userAlias.toLowerCase());
        if (userId == null) {
            // skip if the according user is not registered
            return;
        }

        final UnitConfig.Builder authorizationGroupConfig = entry.getMessage().toBuilder();
        if (!authorizationGroupConfig.getAuthorizationGroupConfig().getMemberIdList().contains(userId)) {
            final List<String> memberIdList = new ArrayList<>(authorizationGroupConfig.getAuthorizationGroupConfig().getSerializedSize());
            memberIdList.add(userId);
            authorizationGroupConfig.getAuthorizationGroupConfigBuilder().clearMemberId();
            authorizationGroupConfig.getAuthorizationGroupConfigBuilder().addAllMemberId(memberIdList);
            throw new EntryModification(entry.setMessage(authorizationGroupConfig, this), this);
        }
    }
}
