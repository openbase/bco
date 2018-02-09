package org.openbase.bco.registry.print;

/*
 * #%L
 * BCO Registry Utility
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

import java.util.*;

import org.openbase.bco.registry.remote.Registries;
import org.openbase.jps.core.JPService;
import org.openbase.jps.preset.JPVerbose;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.extension.rsb.scope.ScopeGenerator;
import org.openbase.jul.processing.StringProcessor;
import org.openbase.jul.processing.StringProcessor.Alignment;
import rst.domotic.unit.UnitConfigType.UnitConfig;
import rst.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;

/**
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public class BCOUnitQueryPrinter {

    /**
     * This is a command line tool to query registry entries.
     * <p>
     * TODOs
     * - JPService should be used in a future release.
     * - Resolve units via raw protobuf message.
     *
     * @param args
     */
    public static void main(String[] args) {
        boolean resultsFound = false;
        try {

            JPService.setApplicationName("bco-query");
            JPService.registerProperty(JPVerbose.class, true);

            // help
            if (args.length > 0 && (args[0].equals("-h") || args[0].equals("--help"))) {
                printHelp();
                System.exit(0);
            }

            // init
            Registries.waitForData();
            final ArrayList<UnitConfig> unitConfigs = new ArrayList<>();

            // print all
            if (args.length == 0) {
                printUnits(Registries.getUnitRegistry().getUnitConfigs());
                System.exit(0);
            }

            // print by unit type
            unitConfigs.clear();
            for (final UnitType unitType : UnitType.values()) {
                if (unitType.name().toLowerCase().equals(args[0].toLowerCase())) {
                    unitConfigs.addAll(Registries.getUnitRegistry().getUnitConfigs(unitType));
                    break;
                }
            }
            if (!unitConfigs.isEmpty()) {
                resultsFound = true;
                System.out.println("");
                System.out.println("resolved by unit type:");
                System.out.println("");
                printUnits(unitConfigs);
                System.exit(0);
            }

            // print by location
            unitConfigs.clear();
            for (final UnitConfig unitConfig : Registries.getUnitRegistry().getUnitConfigs()) {
                if (getLocationLabel(unitConfig).toLowerCase().contains(args[0].toLowerCase())) {
                    unitConfigs.add(unitConfig);
                }
            }
            if (!unitConfigs.isEmpty()) {
                resultsFound = true;
                System.out.println("");
                System.out.println("resolved by location:");
                System.out.println("");
                printUnits(unitConfigs);
                System.exit(0);
            }

            // print by containing label
            unitConfigs.clear();
            for (final UnitConfig unitConfig : Registries.getUnitRegistry().getUnitConfigs()) {
                if (unitConfig.getLabel().toLowerCase().contains(args[0].toLowerCase())) {
                    unitConfigs.add(unitConfig);
                }
            }
            if (!unitConfigs.isEmpty()) {
                resultsFound = true;
                System.out.println("");
                System.out.println("resolved by label:");
                System.out.println("");
                printUnits(unitConfigs);
                System.exit(0);
            }

            // print by scope
            unitConfigs.clear();
            for (final UnitConfig unitConfig : Registries.getUnitRegistry().getUnitConfigs()) {
                if (ScopeGenerator.generateStringRep(unitConfig.getScope()).contains(args[0].toLowerCase())) {
                    unitConfigs.add(unitConfig);
                }
            }
            if (!unitConfigs.isEmpty()) {
                resultsFound = true;
                System.out.println("");
                System.out.println("resolved by scope:");
                System.out.println("");
                printUnits(unitConfigs);
                System.exit(0);
            }

            // print by id
            unitConfigs.clear();
            for (final UnitConfig unitConfig : Registries.getUnitRegistry().getUnitConfigs()) {
                if (unitConfig.getId().contains(args[0].toLowerCase())) {
                    unitConfigs.add(unitConfig);
                }
            }
            if (!unitConfigs.isEmpty()) {
                resultsFound = true;
                System.out.println("");
                System.out.println("resolved by id:");
                System.out.println("");
                printUnits(unitConfigs);
                System.exit(0);
            }

            // print by alias
            unitConfigs.clear();
            unitConfigs.add(Registries.getUnitRegistry().getUnitConfigByAlias(args[0]));

            if (!unitConfigs.isEmpty()) {
                resultsFound = true;
                System.out.println("");
                System.out.println("resolved by alias:");
                System.out.println("");
                printUnits(unitConfigs);
                System.exit(0);
            }

            // print by description
            unitConfigs.clear();
            for (final UnitConfig unitConfig : Registries.getUnitRegistry().getUnitConfigs()) {
                if (unitConfig.getDescription().toLowerCase().contains(args[0].toLowerCase())) {
                    unitConfigs.add(unitConfig);
                }
            }
            if (!unitConfigs.isEmpty()) {
                resultsFound = true;
                System.out.println("");
                System.out.println("resolved by description:");
                System.out.println("");
                printUnits(unitConfigs);
                System.exit(0);
            }
        } catch (InterruptedException ex) {
            System.out.println("killed");
            System.exit(253);
            return;
        } catch (Exception ex) {
            ExceptionPrinter.printHistory(new CouldNotPerformException("Could not query!", ex), System.err);
            printHelp();
            System.exit(254);
        }

        if (resultsFound) {
            System.exit(0);
        }
        System.exit(255);
    }

    private static void printHelp() {
        System.out.println("");
        System.out.println("");
        System.out.println("Usage:   bco-registry-query ${UNIT_TYPE}");
        System.out.println("         bco-registry-query ${UNIT_LOCATION}");
        System.out.println("         bco-registry-query ${UNIT_LABEL}");
        System.out.println("         bco-registry-query ${UNIT_ID}");
        System.out.println("         bco-registry-query ${UNIT_ALIAS}");
        System.out.println("");
        System.out.println("Example: bco-registry-query colorablelight");
        System.out.println("         bco-registry-query livingroom");
        System.out.println("         bco-registry-query ceilinglamp");
        System.out.println("         bco-registry-query 844a5b35-4b9c-4db2-9d22-4842db77bc95");
        System.out.println("         bco-registry-query colorablelight-12");
        System.out.println("");
        System.out.println("Print:   ${ID} ${LABEL} @ ${LOCATION} ${SCOPE}");
        System.out.println("");
    }

    public static void printUnits(List<UnitConfig> unitConfigList) throws InterruptedException, CouldNotPerformException {

        // sort by scope
        Collections.sort(unitConfigList, new Comparator<UnitConfig>() {
            @Override
            public int compare(UnitConfig o1, UnitConfig o2) {
                try {
                    return ScopeGenerator.generateStringRep(o1.getScope()).compareTo(ScopeGenerator.generateStringRep(o2.getScope()));
                } catch (CouldNotPerformException ex) {
                    ExceptionPrinter.printHistory("Could not sort scope!", ex, System.err);
                    return 0;
                }
            }
        });

        // calculate max unit label length
        int maxUnitLabelLength = 0;
        int maxLocationUnitLabelLength = 0;
        int maxScopeLength = 0;
        int maxAliasLength = 0;
        for (final UnitConfig unitConfig : unitConfigList) {
            maxUnitLabelLength = Math.max(maxUnitLabelLength, unitConfig.getLabel().length());
            maxLocationUnitLabelLength = Math.max(maxLocationUnitLabelLength, getLocationLabel(unitConfig).length());
            maxScopeLength = Math.max(maxScopeLength, ScopeGenerator.generateStringRep(unitConfig.getScope()).length());
            maxAliasLength = Math.max(maxAliasLength, Arrays.toString(unitConfig.getAliasList().toArray()).length());
        }

        // print
        for (final UnitConfig unitConfig : unitConfigList) {
            printUnit(unitConfig, maxAliasLength, maxUnitLabelLength, maxLocationUnitLabelLength, maxScopeLength);
        }
    }

    public static void printUnit(final UnitConfig unitConfig, final int maxAliasLength, final int maxUnitLabelLength, final int maxLocationUnitLabelLength, final int maxScopeLength) throws InterruptedException, CouldNotPerformException {
        System.out.println(unitConfig.getId()
                + "  "
                + "[ " + StringProcessor.fillWithSpaces(generateStringRep(unitConfig.getAliasList()), maxAliasLength) + " ]"
                + "  "
                + StringProcessor.fillWithSpaces(unitConfig.getLabel(), maxUnitLabelLength, StringProcessor.Alignment.RIGHT)
                + " @ " + StringProcessor.fillWithSpaces(getLocationLabel(unitConfig), maxLocationUnitLabelLength)
                + "  "
                + "[ " + StringProcessor.fillWithSpaces(ScopeGenerator.generateStringRep(unitConfig.getScope()), maxScopeLength) + " ]"
        );
    }

    private static String getLocationLabel(final UnitConfig unitConfig) throws InterruptedException {
        try {
            return Registries.getLocationRegistry().getLocationConfigById(unitConfig.getPlacementConfig().getLocationId()).getLabel();
        } catch (CouldNotPerformException ex) {
            return "?";
        }
    }

    private static String generateStringRep(final Collection collection) {
        String rep = "";
        for (Iterator iterator = collection.iterator(); iterator.hasNext(); ) {
            rep += iterator.next();
            if (iterator.hasNext()) {
                rep += ", ";
            }
        }
        return rep;
    }
}
