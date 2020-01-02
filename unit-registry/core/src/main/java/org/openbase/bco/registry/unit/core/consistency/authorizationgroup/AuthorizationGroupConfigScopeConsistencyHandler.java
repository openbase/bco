package org.openbase.bco.registry.unit.core.consistency.authorizationgroup;

/*
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
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.extension.protobuf.IdentifiableMessage;
import org.openbase.jul.extension.protobuf.container.ProtoBufMessageMap;
import org.openbase.bco.registry.lib.generator.ScopeGenerator;
import org.openbase.jul.extension.type.processing.ScopeProcessor;
import org.openbase.jul.storage.registry.AbstractProtoBufRegistryConsistencyHandler;
import org.openbase.jul.storage.registry.EntryModification;
import org.openbase.jul.storage.registry.ProtoBufRegistry;
import org.openbase.type.domotic.unit.UnitConfigType.UnitConfig;
import org.openbase.type.communication.ScopeType;

/**
 *
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class AuthorizationGroupConfigScopeConsistencyHandler extends AbstractProtoBufRegistryConsistencyHandler<String, UnitConfig, UnitConfig.Builder> {

    @Override
    public void processData(String id, IdentifiableMessage<String, UnitConfig, UnitConfig.Builder> entry, ProtoBufMessageMap<String, UnitConfig, UnitConfig.Builder> entryMap, ProtoBufRegistry<String, UnitConfig, UnitConfig.Builder> registry) throws CouldNotPerformException, EntryModification {
        UnitConfig authorizationGroupUnitConfig = entry.getMessage();

        if (!authorizationGroupUnitConfig.hasLabel()) {
            throw new NotAvailableException("user.label");
        }

        ScopeType.Scope newScope = ScopeGenerator.generateAuthorizationGroupScope(authorizationGroupUnitConfig);

        // verify and update scope
        if (!ScopeProcessor.generateStringRep(authorizationGroupUnitConfig.getScope()).equals(ScopeProcessor.generateStringRep(newScope))) {
            entry.setMessage(authorizationGroupUnitConfig.toBuilder().setScope(newScope), this);
            throw new EntryModification(entry, this);
        }
    }

    @Override
    public void reset() {
    }
}
