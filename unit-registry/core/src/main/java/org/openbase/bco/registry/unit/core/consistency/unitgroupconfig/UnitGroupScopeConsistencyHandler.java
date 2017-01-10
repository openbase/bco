package org.openbase.bco.registry.unit.core.consistency.unitgroupconfig;

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
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.extension.protobuf.IdentifiableMessage;
import org.openbase.jul.extension.rsb.scope.ScopeGenerator;
import org.openbase.jul.storage.registry.AbstractProtoBufRegistryConsistencyHandler;
import org.openbase.jul.storage.registry.EntryModification;
import rst.rsb.ScopeType;
import org.openbase.jul.extension.protobuf.container.ProtoBufMessageMap;
import org.openbase.jul.storage.registry.ProtoBufFileSynchronizedRegistry;
import org.openbase.jul.storage.registry.ProtoBufRegistry;
import rst.domotic.unit.UnitConfigType.UnitConfig;
import rst.domotic.registry.UnitRegistryDataType.UnitRegistryData;

/**
 *
 @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public class UnitGroupScopeConsistencyHandler extends AbstractProtoBufRegistryConsistencyHandler<String, UnitConfig, UnitConfig.Builder> {

    private final ProtoBufFileSynchronizedRegistry<String, UnitConfig, UnitConfig.Builder, UnitRegistryData.Builder> locationRegistry;

    public UnitGroupScopeConsistencyHandler(final ProtoBufFileSynchronizedRegistry<String, UnitConfig, UnitConfig.Builder, UnitRegistryData.Builder> locationRegistry) {
        this.locationRegistry = locationRegistry;
    }

    @Override
    public void processData(String id, IdentifiableMessage<String, UnitConfig, UnitConfig.Builder> entry, ProtoBufMessageMap<String, UnitConfig, UnitConfig.Builder> entryMap, ProtoBufRegistry<String, UnitConfig, UnitConfig.Builder> registry) throws CouldNotPerformException, EntryModification {
        UnitConfig unitGroupUnitConfig = entry.getMessage();

        if (!unitGroupUnitConfig.hasPlacementConfig()) {
            throw new NotAvailableException("unitGroupConfig.placementconfig");
        }
        if (!unitGroupUnitConfig.getPlacementConfig().hasLocationId() || unitGroupUnitConfig.getPlacementConfig().getLocationId().isEmpty()) {
            throw new NotAvailableException("unitGroupConfig.placementconfig.locationid");
        }

        UnitConfig locationConfig = locationRegistry.getMessage(unitGroupUnitConfig.getPlacementConfig().getLocationId());
        ScopeType.Scope newScope = ScopeGenerator.generateUnitGroupScope(unitGroupUnitConfig, locationConfig);

        // verify and update scope
        if (!ScopeGenerator.generateStringRep(unitGroupUnitConfig.getScope()).equals(ScopeGenerator.generateStringRep(newScope))) {
            entry.setMessage(unitGroupUnitConfig.toBuilder().setScope(newScope));
            throw new EntryModification(entry, this);
        }
    }
}
