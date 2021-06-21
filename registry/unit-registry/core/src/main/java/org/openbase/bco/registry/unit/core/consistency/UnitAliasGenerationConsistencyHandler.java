package org.openbase.bco.registry.unit.core.consistency;

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
import org.openbase.type.domotic.unit.UnitConfigType.UnitConfig;
import org.openbase.type.domotic.unit.UnitConfigType.UnitConfig.Builder;
import org.openbase.type.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;

import java.util.*;
import java.util.stream.Collectors;

public class UnitAliasGenerationConsistencyHandler extends AbstractProtoBufRegistryConsistencyHandler<String, UnitConfig, Builder> {

    private static final Logger LOGGER = LoggerFactory.getLogger(UnitAliasGenerationConsistencyHandler.class);

    public static final String ALIAS_NUMBER_SEPARATOR = "-";

    private Map<UnitType, Integer> unitTypeAliasNumberMap;
    private final UnitRegistry unitRegistry;
    private boolean updateNeeded = true;

    public UnitAliasGenerationConsistencyHandler(final UnitRegistry unitRegistry) {
        this.unitRegistry = unitRegistry;
        this.unitTypeAliasNumberMap = new HashMap<>();
    }

    @Override
    public void processData(String id, IdentifiableMessage<String, UnitConfig, Builder> entry, ProtoBufMessageMap<String, UnitConfig, Builder> entryMap, ProtoBufRegistry<String, UnitConfig, Builder> registry) throws CouldNotPerformException, EntryModification {
        final UnitConfig.Builder unitConfig = entry.getMessage().toBuilder();
        final String aliasPrefix = generateAliasPrefix(unitConfig.getUnitType());

        // create comparator that sorts the default alias on top.
        final Comparator<String> aliasComparator = (o1, o2) -> {
            final boolean o1StartsWithDefaultPrefix = o1.startsWith(generateAliasPrefix(unitConfig.getUnitType()));
            final boolean o2StartsWithDefaultPrefix = o2.startsWith(generateAliasPrefix(unitConfig.getUnitType()));
            if (o1StartsWithDefaultPrefix && o2StartsWithDefaultPrefix) {
                return o1.compareTo(o2);
            } else if (o1StartsWithDefaultPrefix) {
                return -1000+o1.compareTo(o2);
            } else if (o2StartsWithDefaultPrefix) {
                return 1000+o1.compareTo(o2);
            } else {
                return o1.compareTo(o2);
            }
        };

        if (unitConfig.getAliasList().isEmpty() ||
            unitConfig.getAliasList().stream().noneMatch((it) -> it.startsWith(aliasPrefix))
        ) {
            Map<UnitType, Integer> copy = null;
            if (registry.isSandbox()) {
                copy = new HashMap<>(unitTypeAliasNumberMap);
            }

            if (updateNeeded) {
                try {
                    updateUnitTypeAliasNumberMap();
                } catch (CouldNotPerformException ex) {
                    ExceptionPrinter.printHistory("Could not update internal alias map!", ex, LOGGER);
                }
            }

            final String alias = generateAndRegisterAlias(unitConfig.getUnitType());

            final List<String> newAliasList = new ArrayList<>(unitConfig.getAliasList());
            newAliasList.add(alias);
            newAliasList.sort(aliasComparator);
            unitConfig.clearAlias();
            unitConfig.addAllAlias(newAliasList);

            if (registry.isSandbox()) {
                unitTypeAliasNumberMap = copy;
            }
            throw new EntryModification(entry.setMessage(unitConfig, this), this);
        }

        // make sure default alias is always on top
        final List<String> sortedAliasList = new ArrayList<>(unitConfig.getAliasList())
            .stream()
            .sorted(aliasComparator)
            .distinct()
            .collect(Collectors.toList());
        if (!sortedAliasList.equals(unitConfig.getAliasList())) {
            unitConfig.clearAlias();
            unitConfig.addAllAlias(sortedAliasList);
            throw new EntryModification(entry.setMessage(unitConfig, this), this);
        }
    }

    private void updateUnitTypeAliasNumberMap() throws CouldNotPerformException {
        for (UnitConfig unitConfig : unitRegistry.getUnitConfigsFiltered(false)) {
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
        return generateAliasPrefix(unitType) + newNumber;
    }

    private String generateAliasPrefix(final UnitType unitType) {
        return StringProcessor.transformUpperCaseToPascalCase(unitType.name()) + ALIAS_NUMBER_SEPARATOR;
    }


    private void registerAlias(final UnitConfig unitConfig) {
        for (String alias : unitConfig.getAliasList()) {
            registerAlias(alias, unitConfig.getUnitType());
        }
    }

    private void registerAlias(final String alias, final UnitType unitType) {
        String[] split = alias.split(ALIAS_NUMBER_SEPARATOR);

        if (split.length != 2) {
            return;
        }

        if (!split[0].equals(StringProcessor.transformUpperCaseToPascalCase(unitType.name()))) {
            return;
        }

        String numberFromGeneratedAlias = split[1];
        try {
            Integer number = Integer.parseInt(numberFromGeneratedAlias);
            registerNumber(number, unitType);
        } catch (NumberFormatException ex) {
            // do nothing
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
    }
}
