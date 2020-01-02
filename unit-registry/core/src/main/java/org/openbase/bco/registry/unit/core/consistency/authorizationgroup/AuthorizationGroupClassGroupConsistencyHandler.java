package org.openbase.bco.registry.unit.core.consistency.authorizationgroup;

/*-
 * #%L
 * BCO Registry Unit Core
 * %%
 * Copyright (C) 2014 - 2020 openbase.org
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

import org.openbase.bco.registry.unit.core.plugin.ClassAuthorizationGroupCreationPlugin;
import org.openbase.bco.registry.unit.core.plugin.UnitUserCreationPlugin;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.extension.protobuf.IdentifiableMessage;
import org.openbase.jul.extension.protobuf.container.ProtoBufMessageMap;
import org.openbase.jul.extension.type.processing.MetaConfigPool;
import org.openbase.jul.extension.type.processing.MetaConfigVariableProvider;
import org.openbase.jul.storage.registry.AbstractProtoBufRegistryConsistencyHandler;
import org.openbase.jul.storage.registry.EntryModification;
import org.openbase.jul.storage.registry.ProtoBufRegistry;
import org.openbase.type.configuration.MetaConfigType.MetaConfig;
import org.openbase.type.domotic.unit.UnitConfigType.UnitConfig;
import org.openbase.type.domotic.unit.UnitConfigType.UnitConfig.Builder;
import org.openbase.type.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;
import org.openbase.type.domotic.unit.authorizationgroup.AuthorizationGroupConfigType.AuthorizationGroupConfig;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This consistency handler makes sure that users generated for agents and apps are always inside
 * the authorization group belonging to this agent or app class.
 *
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class AuthorizationGroupClassGroupConsistencyHandler extends AbstractProtoBufRegistryConsistencyHandler<String, UnitConfig, Builder> {

    private final ProtoBufRegistry<String, UnitConfig, Builder> userRegistry;
    private final Map<UnitType, ProtoBufRegistry<String, UnitConfig, Builder>> typeRegistryMap;

    public AuthorizationGroupClassGroupConsistencyHandler(
            final ProtoBufRegistry<String, UnitConfig, Builder> userRegistry,
            final ProtoBufRegistry<String, UnitConfig, Builder> agentRegistry,
            final ProtoBufRegistry<String, UnitConfig, Builder> appRegistry) {
        this.userRegistry = userRegistry;
        typeRegistryMap = new HashMap<>();
        typeRegistryMap.put(UnitType.AGENT, agentRegistry);
        typeRegistryMap.put(UnitType.APP, appRegistry);
    }

    @Override
    public void processData(final String id,
                            final IdentifiableMessage<String, UnitConfig, Builder> entry,
                            final ProtoBufMessageMap<String, UnitConfig, Builder> entryMap,
                            final ProtoBufRegistry<String, UnitConfig, Builder> registry) throws CouldNotPerformException, EntryModification {
        final UnitConfig.Builder authorizationGroup = entry.getMessage().toBuilder();

        // build meta config pool
        final MetaConfigPool metaConfigPool = new MetaConfigPool();
        metaConfigPool.register(new MetaConfigVariableProvider(authorizationGroup.getAlias(0) + MetaConfig.class.getSimpleName(),
                authorizationGroup.getMetaConfig()));

        // test if this authorization group belongs to an agent or app class
        UnitType unitType;
        String classId;
        try {
            classId = metaConfigPool.getValue(ClassAuthorizationGroupCreationPlugin.AGENT_CLASS_ID_KEY);
            unitType = UnitType.AGENT;
        } catch (NotAvailableException ex) {
            try {
                classId = metaConfigPool.getValue(ClassAuthorizationGroupCreationPlugin.APP_CLASS_ID_KEY);
                unitType = UnitType.APP;
            } catch (NotAvailableException exx) {
                // do nothing because the authorization group is not for an agent class or app class
                return;
            }
        }

        // resolve all users for agents or apps from this class
        final List<String> userIdList = new ArrayList<>();
        for (final UnitConfig message : typeRegistryMap.get(unitType).getMessages()) {
            final String unitClassId;
            if (unitType == UnitType.AGENT) {
                unitClassId = message.getAgentConfig().getAgentClassId();
            } else {
                unitClassId = message.getAppConfig().getAppClassId();
            }

            // ignore all agents or apps not belonging to this class
            if (!unitClassId.equals(classId)) {
                continue;
            }

            // find the user belonging to this app or agent
            try {
                userIdList.add(UnitUserCreationPlugin.findUser(message.getId(), userRegistry).getId());
            } catch (NotAvailableException ex) {
                // if the use is no longer available it will be removed in a later loop
            }
        }


        boolean modification = false;
        final AuthorizationGroupConfig.Builder authorizationGroupConfig = authorizationGroup.getAuthorizationGroupConfigBuilder();

        // copy list of current members
        final List<String> memberIdList = new ArrayList<>(authorizationGroupConfig.getMemberIdList());
        // add all users that are currently not part of the group
        for (String userId : userIdList) {
            if (!memberIdList.contains(userId)) {
                modification = true;
                memberIdList.add(userId);
            }
        }

        // remove all users which should not longer be part of the group
        for (String memberId : authorizationGroupConfig.getMemberIdList()) {
            if (!userIdList.contains(memberId)) {
                modification = true;
                memberIdList.remove(memberId);
            }
        }

        // if a user is has been removed or added notify the modification
        if (modification) {
            authorizationGroupConfig.clearMemberId();
            authorizationGroupConfig.addAllMemberId(memberIdList);
            throw new EntryModification(entry.setMessage(authorizationGroup, this), this);
        }
    }
}
