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

import org.openbase.bco.registry.unit.lib.UnitRegistry;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.extension.protobuf.IdentifiableMessage;
import org.openbase.jul.extension.protobuf.container.ProtoBufMessageMap;
import org.openbase.jul.processing.StringProcessor;
import org.openbase.jul.storage.registry.AbstractProtoBufRegistryConsistencyHandler;
import org.openbase.jul.storage.registry.EntryModification;
import org.openbase.jul.storage.registry.ProtoBufRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rst.domotic.unit.UnitConfigType.UnitConfig;
import rst.domotic.unit.UnitConfigType.UnitConfig.Builder;
import rst.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;

import java.util.HashMap;
import java.util.Map;

public class UnitAliasGenerationConsistencyHandler extends AbstractProtoBufRegistryConsistencyHandler<String, UnitConfig, Builder> {

    private static final Logger LOGGER = LoggerFactory.getLogger(UnitAliasGenerationConsistencyHandler.class);

    public static final String ALIAS_NUMBER_SEPERATOR = "-";

    private final Map<UnitType, Integer> unitTypeAliasNumberMap;
    private final UnitRegistry unitRegistry;
    private boolean updateNeeded = true;

    public UnitAliasGenerationConsistencyHandler(final UnitRegistry unitRegistry) {
        this.unitRegistry = unitRegistry;
        this.unitTypeAliasNumberMap = new HashMap<>();
    }

    @Override
    public void processData(String id, IdentifiableMessage<String, UnitConfig, Builder> entry, ProtoBufMessageMap<String, UnitConfig, Builder> entryMap, ProtoBufRegistry<String, UnitConfig, Builder> registry) throws CouldNotPerformException, EntryModification {
        if (entry.getMessage().getAliasList().isEmpty()) {

            if (updateNeeded) {
                try {
                    updateUnitTypeAliasNumberMap();
                } catch (CouldNotPerformException ex) {
                    ExceptionPrinter.printHistory("Could not update internal alias map!", ex, LOGGER);
                }
            }

            UnitConfig.Builder unitConfig = entry.getMessage().toBuilder();
            unitConfig.addAlias(generateAndRegisterAlias(unitConfig.getType()));
            throw new EntryModification(entry.setMessage(unitConfig), this);
        }
    }

    private void updateUnitTypeAliasNumberMap() throws CouldNotPerformException {
        for (UnitConfig unitConfig : unitRegistry.getUnitConfigs()) {
            registerAlias(unitConfig);
        }
    }

    private String generateAndRegisterAlias(final UnitType unitType) {

        // init if not exist
        if (!unitTypeAliasNumberMap.containsKey(unitType)) {
            unitTypeAliasNumberMap.put(unitType, 0);
        }

        // generate next number
        final int newNumber = unitTypeAliasNumberMap.get(unitType) + 1;

        // register number
        registerNumber(newNumber, unitType);

        // generate and return alias string
        return StringProcessor.transformUpperCaseToCamelCase(unitType.name()) + ALIAS_NUMBER_SEPERATOR + newNumber;
    }


    private void registerAlias(final UnitConfig unitConfig) {
        for (String alias : unitConfig.getAliasList()) {
            registerAlias(alias, unitConfig.getType());
        }
    }

    private void registerAlias(final String alias, final UnitType unitType) {
        String split[] = alias.split(ALIAS_NUMBER_SEPERATOR);

        if (split.length != 2) {
            return;
        }

        if (!split[0].equals(StringProcessor.transformUpperCaseToCamelCase(unitType.name()))) {
            return;
        }

        String numberFromGeneratedAlias = split[1];
        try {
            Integer number = Integer.parseInt(numberFromGeneratedAlias);
            registerNumber(number, unitType);
        } catch (NumberFormatException ex) {
            return;
        }
    }

    private void registerNumber(final int number, final UnitType unitType) {
        if (!unitTypeAliasNumberMap.containsKey(unitType) || unitTypeAliasNumberMap.get(unitType) < number) {
            unitTypeAliasNumberMap.put(unitType, number);
        }
    }

    @Override
    public void reset() {
        updateNeeded = true;
        super.reset();
    }
}
