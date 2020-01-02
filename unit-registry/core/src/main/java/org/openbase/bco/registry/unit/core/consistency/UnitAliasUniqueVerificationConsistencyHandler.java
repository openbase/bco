package org.openbase.bco.registry.unit.core.consistency;

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

import org.openbase.bco.registry.unit.lib.UnitRegistry;
import org.openbase.jul.exception.*;
import org.openbase.jul.extension.protobuf.IdentifiableMessage;
import org.openbase.jul.extension.protobuf.container.ProtoBufMessageMap;
import org.openbase.jul.extension.type.processing.ScopeProcessor;
import org.openbase.jul.storage.registry.AbstractProtoBufRegistryConsistencyHandler;
import org.openbase.jul.storage.registry.EntryModification;
import org.openbase.jul.storage.registry.ProtoBufRegistry;
import org.openbase.type.domotic.unit.UnitConfigType.UnitConfig;
import org.openbase.type.domotic.unit.UnitConfigType.UnitConfig.Builder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class UnitAliasUniqueVerificationConsistencyHandler extends AbstractProtoBufRegistryConsistencyHandler<String, UnitConfig, Builder> {

    private final Map<String, String> aliasUnitIdMap;
    private final UnitRegistry unitRegistry;

    public UnitAliasUniqueVerificationConsistencyHandler(final UnitRegistry unitRegistry) {
        this.unitRegistry = unitRegistry;
        this.aliasUnitIdMap = new HashMap<>();
    }

    @Override
    public void processData(String id, IdentifiableMessage<String, UnitConfig, Builder> entry, ProtoBufMessageMap<String, UnitConfig, Builder> entryMap, ProtoBufRegistry<String, UnitConfig, Builder> registry) throws CouldNotPerformException, EntryModification {
        final UnitConfig.Builder unitConfig = entry.getMessage().toBuilder();

        for (final String alias : unitConfig.getAliasList()) {
            if (!aliasUnitIdMap.containsKey(alias.toLowerCase())) {
                aliasUnitIdMap.put(alias.toLowerCase(), unitConfig.getId());
            } else {
                // if already known check if this unit is owning the alias otherwise throw invalid state
                if (!aliasUnitIdMap.get(alias.toLowerCase()).equals(unitConfig.getId())) {
                    throw new RejectedException("Alias[" + alias.toLowerCase() + "] of Unit[" + ScopeProcessor.generateStringRep(unitConfig.getScope()) + ", " + unitConfig.getId() + "] is already used by Unit[" + aliasUnitIdMap.get(alias.toLowerCase()) + "]");
                }
            }
        }
    }

    @Override
    public void reset() {

        // validate known aliases
        for (String alias : new ArrayList<>(aliasUnitIdMap.keySet())) {
             // remove alias entry if alias is globally unknown.
            if (!unitRegistry.containsUnitConfigByAlias(alias)) {
                logger.debug("remove alias: " + alias);
                aliasUnitIdMap.remove(alias);
            }
        }
        super.reset();
    }

    @Override
    public void shutdown() {
        aliasUnitIdMap.clear();
        // super call is not performed because those would only call reset() which fails because the unit registry is not responding during shutdown.
    }
}
