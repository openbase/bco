package org.openbase.bco.registry.unit.core.consistency;

/*-
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

import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.extension.protobuf.IdentifiableMessage;
import org.openbase.jul.extension.protobuf.container.ProtoBufMessageMap;
import org.openbase.jul.processing.StringProcessor;
import org.openbase.jul.storage.registry.AbstractProtoBufRegistryConsistencyHandler;
import org.openbase.jul.storage.registry.EntryModification;
import org.openbase.jul.storage.registry.ProtoBufRegistry;
import rst.domotic.unit.UnitConfigType.UnitConfig;
import rst.domotic.unit.UnitConfigType.UnitConfig.Builder;
import rst.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;

import java.util.HashMap;
import java.util.Map;

public class UnitAliasGenerationConsistencyHandler extends AbstractProtoBufRegistryConsistencyHandler<String, UnitConfig, Builder> {

    public static final String ALIAS_NUMBER_SEPERATOR = "-";

    private final Map<UnitType, Integer> unitTypeAliasNumberMap;

    public UnitAliasGenerationConsistencyHandler() {
        unitTypeAliasNumberMap = new HashMap<>();
    }

    @Override
    public void processData(String id, IdentifiableMessage<String, UnitConfig, Builder> entry, ProtoBufMessageMap<String, UnitConfig, Builder> entryMap, ProtoBufRegistry<String, UnitConfig, Builder> registry) throws CouldNotPerformException, EntryModification {

        if (unitTypeAliasNumberMap.isEmpty()) {
            initUnitTypeAliasNumberMap(registry);
        }

        if (entry.getMessage().getAliasList().isEmpty()) {
            UnitConfig.Builder unitConfig = entry.getMessage().toBuilder();

            unitConfig.addAlias(generateAlias(unitConfig.getType()));

            throw new EntryModification(entry.setMessage(unitConfig), this);
        }
    }

    private void initUnitTypeAliasNumberMap(final ProtoBufRegistry<String, UnitConfig, Builder> registry) throws CouldNotPerformException {
        for (UnitType unitType : UnitType.values()) {
            unitTypeAliasNumberMap.put(unitType, 0);
        }

        for (UnitConfig unitConfig : registry.getMessages()) {
            for (String alias : unitConfig.getAliasList()) {
                String split[] = alias.split(ALIAS_NUMBER_SEPERATOR);

                if (split.length != 2) {
                    continue;
                }

                if (!split[0].equals(StringProcessor.transformUpperCaseToCamelCase(unitConfig.getType().name()))) {
                    continue;
                }

                String numberFromGeneratedAlias = split[1];
                try {
                    Integer number = Integer.parseInt(numberFromGeneratedAlias);

                    if (unitTypeAliasNumberMap.get(unitConfig.getType()) < number) {
                        unitTypeAliasNumberMap.put(unitConfig.getType(), number);
                    }
                } catch (NumberFormatException ex) {
                    continue;
                }
            }
        }
    }

    private String generateAlias(final UnitType unitType) {
        Integer number = unitTypeAliasNumberMap.get(unitType);
        number++;
        unitTypeAliasNumberMap.put(unitType, number);
        return StringProcessor.transformUpperCaseToCamelCase(unitType.name()) + ALIAS_NUMBER_SEPERATOR + number;
    }
}
