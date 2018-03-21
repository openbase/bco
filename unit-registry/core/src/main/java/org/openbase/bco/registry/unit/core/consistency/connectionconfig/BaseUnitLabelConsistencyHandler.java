package org.openbase.bco.registry.unit.core.consistency.connectionconfig;

/*
 * #%L
 * BCO Registry Unit Core
 * %%
 * Copyright (C) 2014 - 2018 openbase.org
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
import org.openbase.jul.processing.StringProcessor;
import org.openbase.jul.storage.registry.AbstractProtoBufRegistryConsistencyHandler;
import org.openbase.jul.storage.registry.EntryModification;
import org.openbase.jul.extension.protobuf.container.ProtoBufMessageMap;
import org.openbase.jul.storage.registry.ProtoBufRegistry;
import rst.domotic.unit.UnitConfigType.UnitConfig;

/**
 *
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class BaseUnitLabelConsistencyHandler extends AbstractProtoBufRegistryConsistencyHandler<String, UnitConfig, UnitConfig.Builder> {

    private final Map<String, UnitConfig> baseUnitMap;

    public BaseUnitLabelConsistencyHandler() {
        this.baseUnitMap = new HashMap<>();
    }

    @Override
    public void processData(String id, IdentifiableMessage<String, UnitConfig, UnitConfig.Builder> entry, ProtoBufMessageMap<String, UnitConfig, UnitConfig.Builder> entryMap, ProtoBufRegistry<String, UnitConfig, UnitConfig.Builder> registry) throws CouldNotPerformException, EntryModification {
        final UnitConfig unitConfig = entry.getMessage();

        if (!unitConfig.hasLabel() || unitConfig.getLabel().isEmpty()) {
            throw new EntryModification(entry.setMessage(unitConfig.toBuilder().setLabel(generateDefaultLabel(unitConfig))), this);
        }

        if (!unitConfig.hasPlacementConfig() || !unitConfig.getPlacementConfig().hasLocationId() || unitConfig.getPlacementConfig().getLocationId().isEmpty()) {
            throw new NotAvailableException("baseunit.placement.locationId");
        }

        String key = unitConfig.getLabel() + unitConfig.getPlacementConfig().getLocationId();

        if (baseUnitMap.containsKey(key)) {
            final String typeName = StringProcessor.transformUpperCaseToCamelCase(unitConfig.getType().name());
            throw new InvalidStateException(typeName+"[" + unitConfig.getAlias(0) + "] and "+typeName+"[" + baseUnitMap.get(key).getAlias(0) + "] are registered with the same label and type at the same location.");
        }

        baseUnitMap.put(key, unitConfig);
    }

    public String generateDefaultLabel(final UnitConfig unitConfig) throws CouldNotPerformException {
        if (unitConfig.getAliasCount() < 1) {
            throw new InvalidStateException("Alias not provided by Unit[" + unitConfig.getId() + "]!");
        }
        return unitConfig.getAlias(0);
    }

    @Override
    public void reset() {
        baseUnitMap.clear();
    }
}
