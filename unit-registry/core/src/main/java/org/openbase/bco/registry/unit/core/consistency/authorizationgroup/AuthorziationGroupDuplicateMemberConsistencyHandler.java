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

import java.util.HashSet;
import java.util.Set;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.extension.protobuf.IdentifiableMessage;
import org.openbase.jul.extension.protobuf.container.ProtoBufMessageMap;
import org.openbase.jul.storage.registry.AbstractProtoBufRegistryConsistencyHandler;
import org.openbase.jul.storage.registry.EntryModification;
import org.openbase.jul.storage.registry.ProtoBufRegistry;
import org.openbase.type.domotic.unit.UnitConfigType.UnitConfig;
import org.openbase.type.domotic.unit.authorizationgroup.AuthorizationGroupConfigType.AuthorizationGroupConfig;

/**
 * Consistency Handler which removes duplicate member id's.
 * 
 * @author <a href="mailto:thuxohl@techfak.uni-bielefeld.de">Tamino Huxohl</a>
 */
public class AuthorziationGroupDuplicateMemberConsistencyHandler extends AbstractProtoBufRegistryConsistencyHandler<String, UnitConfig, UnitConfig.Builder> {


    public AuthorziationGroupDuplicateMemberConsistencyHandler() {
    }

    @Override
    public void processData(String id, IdentifiableMessage<String, UnitConfig, UnitConfig.Builder> entry, ProtoBufMessageMap<String, UnitConfig, UnitConfig.Builder> entryMap, ProtoBufRegistry<String, UnitConfig, UnitConfig.Builder> registry) throws CouldNotPerformException, EntryModification {
        UnitConfig.Builder authorizationGroupUnitConfig = entry.getMessage().toBuilder();
        AuthorizationGroupConfig.Builder authorizationGroup = authorizationGroupUnitConfig.getAuthorizationGroupConfigBuilder();
        
        Set<String> memberIdSet = new HashSet<>();
        boolean modification = false;
        for(String memberId : authorizationGroup.getMemberIdList()) {
            if(memberIdSet.contains(memberId)) {
                modification = true;
            } else {
                memberIdSet.add(memberId);
            }
        }
        
        if(modification) {
            authorizationGroup.clearMemberId();
            authorizationGroup.addAllMemberId(memberIdSet);
            throw new EntryModification(entry.setMessage(authorizationGroupUnitConfig, this), this);
        }
    }
    
}
