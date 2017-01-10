package org.openbase.bco.registry.unit.core.consistency.dalunitconfig;

/*
 * #%L
 * BCO Registry Unit Core
 * %%
 * Copyright (C) 2014 - 2017 openbase.org
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
import java.util.Map;
import java.util.TreeMap;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InvalidStateException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.extension.protobuf.IdentifiableMessage;
import org.openbase.jul.extension.protobuf.container.ProtoBufMessageMap;
import org.openbase.jul.extension.rsb.scope.ScopeGenerator;
import org.openbase.jul.storage.registry.AbstractProtoBufRegistryConsistencyHandler;
import org.openbase.jul.storage.registry.EntryModification;
import org.openbase.jul.storage.registry.ProtoBufFileSynchronizedRegistry;
import org.openbase.jul.storage.registry.ProtoBufRegistry;
import rst.domotic.unit.UnitConfigType.UnitConfig;
import rst.domotic.registry.UnitRegistryDataType.UnitRegistryData;
import rst.rsb.ScopeType;

/**
 *
 @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public class DalUnitScopeConsistencyHandler extends AbstractProtoBufRegistryConsistencyHandler<String, UnitConfig, UnitConfig.Builder> {

    private final ProtoBufFileSynchronizedRegistry<String, UnitConfig, UnitConfig.Builder, UnitRegistryData.Builder> locationRegistry;
    private final Map<String, UnitConfig> unitScopeMap;

    public DalUnitScopeConsistencyHandler(final ProtoBufFileSynchronizedRegistry<String, UnitConfig, UnitConfig.Builder, UnitRegistryData.Builder> locationRegistry) {
        this.locationRegistry = locationRegistry;
        this.unitScopeMap = new TreeMap<>();
    }

    @Override
    public void processData(String id, IdentifiableMessage<String, UnitConfig, UnitConfig.Builder> entry, ProtoBufMessageMap<String, UnitConfig, UnitConfig.Builder> entryMap, ProtoBufRegistry<String, UnitConfig, UnitConfig.Builder> registry) throws CouldNotPerformException, EntryModification {
        UnitConfig.Builder dalUnitConfig = entry.getMessage().toBuilder();

        boolean modification = false;
        UnitConfig unitConfigClone = UnitConfig.newBuilder(dalUnitConfig.build()).build();

        if (!dalUnitConfig.hasPlacementConfig()) {
            throw new NotAvailableException("placementconfig");
        }

        if (!dalUnitConfig.getPlacementConfig().hasLocationId() || dalUnitConfig.getPlacementConfig().getLocationId().isEmpty()) {
            throw new NotAvailableException("placementconfig.locationid");
        }

        ScopeType.Scope newScope = ScopeGenerator.generateUnitScope(unitConfigClone, locationRegistry.getMessage((dalUnitConfig.getPlacementConfig().getLocationId())));

        // verify and update scope
        if (!ScopeGenerator.generateStringRep(dalUnitConfig.getScope()).equals(ScopeGenerator.generateStringRep(newScope))) {
            dalUnitConfig.setScope(newScope);
            modification = true;
        }

        if (unitScopeMap.containsKey(ScopeGenerator.generateStringRep(dalUnitConfig.getScope()))) {
            throw new InvalidStateException("Two units with same scope[" + ScopeGenerator.generateStringRep(dalUnitConfig.getScope()) + "]!");
        }
        unitScopeMap.put(ScopeGenerator.generateStringRep(dalUnitConfig.getScope()), dalUnitConfig.build());

        if (modification) {
            throw new EntryModification(entry.setMessage(dalUnitConfig), this);
        }
    }

    @Override
    public void reset() {
        unitScopeMap.clear();
    }
}
