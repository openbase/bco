package org.openbase.bco.registry.unit.core.consistency;

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

import org.openbase.bco.registry.unit.lib.UnitRegistry;
import org.openbase.bco.registry.unit.lib.generator.GenericUnitScopeGenerator;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InvalidStateException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.extension.protobuf.IdentifiableMessage;
import org.openbase.jul.extension.protobuf.container.ProtoBufMessageMap;
import org.openbase.jul.extension.type.processing.ScopeProcessor;
import org.openbase.jul.storage.registry.AbstractProtoBufRegistryConsistencyHandler;
import org.openbase.jul.storage.registry.EntryModification;
import org.openbase.jul.storage.registry.ProtoBufRegistry;
import org.openbase.type.communication.ScopeType;
import org.openbase.type.domotic.unit.UnitConfigType.UnitConfig;

import java.util.Map;
import java.util.TreeMap;

/**
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public class UnitScopeConsistencyHandler extends AbstractProtoBufRegistryConsistencyHandler<String, UnitConfig, UnitConfig.Builder> {

    private final Map<String, UnitConfig> unitIdScopeMap;
    final UnitRegistry unitRegistry;

    public UnitScopeConsistencyHandler(final UnitRegistry unitRegistry) {
        this.unitRegistry = unitRegistry;
        this.unitIdScopeMap = new TreeMap<>();
    }

    @Override
    public void processData(String id, IdentifiableMessage<String, UnitConfig, UnitConfig.Builder> entry, ProtoBufMessageMap<String, UnitConfig, UnitConfig.Builder> entryMap, ProtoBufRegistry<String, UnitConfig, UnitConfig.Builder> registry) throws CouldNotPerformException, EntryModification {
        UnitConfig.Builder unitConfig = entry.getMessage().toBuilder();

        boolean modification = false;

        if (!unitConfig.hasPlacementConfig()) {
            throw new NotAvailableException("placementconfig");
        }

        if (!unitConfig.getPlacementConfig().hasLocationId() || unitConfig.getPlacementConfig().getLocationId().isEmpty()) {
            throw new NotAvailableException("placementconfig.locationid");
        }

        ScopeType.Scope newScope = GenericUnitScopeGenerator.generateScope(unitConfig.build(), unitRegistry);

        // verify and update scope
        if (!ScopeProcessor.generateStringRep(unitConfig.getScope()).equals(ScopeProcessor.generateStringRep(newScope))) {
            unitConfig.setScope(newScope);
            modification = true;
        }

        if (unitIdScopeMap.containsKey(ScopeProcessor.generateStringRep(unitConfig.getScope()))) {
            throw new InvalidStateException("Two units with same scope[" + ScopeProcessor.generateStringRep(unitConfig.getScope()) + "]!");
        }

        unitIdScopeMap.put(ScopeProcessor.generateStringRep(unitConfig.getScope()), unitConfig.build());

        if (modification) {
            throw new EntryModification(entry.setMessage(unitConfig, this), this);
        }
    }

    @Override
    public void reset() {
        unitIdScopeMap.clear();
    }
}
