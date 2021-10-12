package org.openbase.bco.registry.print;

/*
 * #%L
 * BCO Registry Utility
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

import org.openbase.bco.registry.remote.Registries;
import org.openbase.bco.registry.remote.login.BCOLogin;
import org.openbase.jps.core.JPService;
import org.openbase.jps.preset.JPVerbose;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.extension.type.processing.ScopeProcessor;
import org.openbase.jul.extension.type.processing.LabelProcessor;
import org.openbase.jul.processing.StringProcessor;
import org.openbase.type.configuration.EntryType.Entry;
import org.openbase.type.domotic.state.EnablingStateType.EnablingState.State;
import org.openbase.type.domotic.unit.UnitConfigType.UnitConfig;
import org.openbase.type.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;
import org.openbase.type.language.LabelType.Label;
import org.openbase.type.language.MultiLanguageTextType.MultiLanguageText.MapFieldEntry;

import java.util.*;

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

        final String query;

        if (args.length == 0) {
            query = "";
        } else {
            query = args[args.length - 1];
        }

        try {

            // handle jps
            if (args.length != 0) {
                // remove query from jp args
                String[] jpArgs = Arrays.copyOfRange(args, 0, args.length - 1);

                JPService.setApplicationName("bco-query");

                // help
                if (args.length > 0 && (args[0].equals("-h") || args[0].equals("--help"))) {
                    JPService.printHelp();
                    printHelp();
                    System.exit(0);
                }

                JPService.parseAndExitOnError(jpArgs);
            }

            // init
            Registries.waitForData();
            BCOLogin.getSession().autoLogin(true).get();
            final ArrayList<UnitConfig> unitConfigs = new ArrayList<>();

            // print all
            if (args.length == 0) {
                printUnits(Registries.getUnitRegistry().getUnitConfigsFiltered(false));
                System.exit(0);
            }

            // print by unit type
            unitConfigs.clear();
            for (final UnitType unitType : UnitType.values()) {
                if (unitType.name().equalsIgnoreCase(query)) {
                    unitConfigs.addAll(Registries.getUnitRegistry().getUnitConfigsByUnitType(unitType));
                    break;
                }
            }
            if (!unitConfigs.isEmpty()) {
                resultsFound = true;
                System.out.println();
                System.out.println("resolved by unit type:");
                System.out.println();
                printUnits(unitConfigs);
            }

            // print by location
            unitConfigs.clear();
            for (final UnitConfig unitConfig : Registries.getUnitRegistry().getUnitConfigsFiltered(false)) {
                if (getLocationLabel(unitConfig).toLowerCase().contains(query.toLowerCase())) {
                    unitConfigs.add(unitConfig);
                }
            }
            if (!unitConfigs.isEmpty()) {
                resultsFound = true;
                System.out.println();
                System.out.println("resolved by location:");
                System.out.println();
                printUnits(unitConfigs);
            }

            // print by containing label
            unitConfigs.clear();
            unitLoop:
            for (final UnitConfig unitConfig : Registries.getUnitRegistry().getUnitConfigsFiltered(false)) {
                for (Label.MapFieldEntry mapFieldEntry : unitConfig.getLabel().getEntryList()) {
                    for (String label : mapFieldEntry.getValueList()) {
                        if (label.toLowerCase().contains(query.toLowerCase())) {
                            unitConfigs.add(unitConfig);
                            continue unitLoop;
                        }
                    }
                }
            }
            if (!unitConfigs.isEmpty()) {
                resultsFound = true;
                System.out.println();
                System.out.println("resolved by label:");
                System.out.println();
                printUnits(unitConfigs);
            }

            // print by scope
            unitConfigs.clear();
            for (final UnitConfig unitConfig : Registries.getUnitRegistry().getUnitConfigsFiltered(false)) {
                if (ScopeProcessor.generateStringRep(unitConfig.getScope()).contains(query.toLowerCase())) {
                    unitConfigs.add(unitConfig);
                }
            }
            if (!unitConfigs.isEmpty()) {
                resultsFound = true;
                System.out.println();
                System.out.println("resolved by scope:");
                System.out.println();
                printUnits(unitConfigs);
            }

            // print by id
            unitConfigs.clear();
            for (final UnitConfig unitConfig : Registries.getUnitRegistry().getUnitConfigsFiltered(false)) {
                if (unitConfig.getId().contains(query.toLowerCase())) {
                    unitConfigs.add(unitConfig);
                }
            }
            if (!unitConfigs.isEmpty()) {
                resultsFound = true;
                System.out.println();
                System.out.println("resolved by id:");
                System.out.println();
                printUnits(unitConfigs);
            }

            // print by alias
            unitConfigs.clear();
            try {
                unitConfigs.add(Registries.getUnitRegistry().getUnitConfigByAlias(query));
            } catch (NotAvailableException ex) {
                // continue if not available
            }

            if (!unitConfigs.isEmpty()) {
                resultsFound = true;
                System.out.println();
                System.out.println("resolved by alias:");
                System.out.println();
                printUnits(unitConfigs);
            }

            // print by meta config
            unitConfigs.clear();
            unitLoop:
            for (final UnitConfig unitConfig : Registries.getUnitRegistry().getUnitConfigsFiltered(false)) {
                for (Entry entry : unitConfig.getMetaConfig().getEntryList()) {
                    if (entry.getKey().toLowerCase().contains(query.toLowerCase()) || entry.getValue().toLowerCase().contains(query.toLowerCase())) {
                        unitConfigs.add(unitConfig);
                        continue unitLoop;
                    }
                }
            }
            if (!unitConfigs.isEmpty()) {
                resultsFound = true;
                System.out.println();
                System.out.println("resolved by meta config:");
                System.out.println();
                printUnits(unitConfigs);
            }

            // print by description
            unitConfigs.clear();
            unitLoop:
            for (final UnitConfig unitConfig : Registries.getUnitRegistry().getUnitConfigsFiltered(false)) {
                for (MapFieldEntry mapFieldEntry : unitConfig.getDescription().getEntryList()) {
                    if (mapFieldEntry.getValue().toLowerCase().contains(query.toLowerCase())) {
                        unitConfigs.add(unitConfig);
                        continue unitLoop;
                    }
                }
            }
            if (!unitConfigs.isEmpty()) {
                resultsFound = true;
                System.out.println();
                System.out.println("resolved by description:");
                System.out.println();
                printUnits(unitConfigs);
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

        System.out.println();

        if (resultsFound) {
            System.exit(0);
        } else {
            System.out.println(ConsoleColors.CYAN + "No match for " + ConsoleColors.RESET + query + ConsoleColors.CYAN + " found" + ConsoleColors.RESET + " :(");
            printHelp();
        }
        System.exit(255);
    }

    private static void printHelp() {
        System.out.println();
        System.out.println("Usage:     bco-query [options] [<query>]");
        System.out.println();
        System.out.println("Queries:   UNIT_TYPE, UNIT_LOCATION, UNIT_LABEL, UNIT_ID, UNIT_ALIAS, UNIT_SCOPE, UNIT_DESCRIPTION, UNIT_META_CONFIG");
        System.out.println();
        System.out.println("UnitTypes: " + StringProcessor.transformCollectionToString(Arrays.asList(UnitType.values()), ", "));
        System.out.println();
        System.out.println("Example:   bco-query");
        System.out.println("           bco-query colorablelight");
        System.out.println("           bco-query livingroom");
        System.out.println("           bco-query ceilinglamp");
        System.out.println("           bco-query 844a5b35-4b9c-4db2-9d22-4842db77bc95");
        System.out.println("           bco-query -v colorablelight-12");
        System.out.println();
        System.out.println("Print:     ${ID} ${LABEL} @ ${LOCATION}");
        System.out.println();
    }

    public static void printUnits(List<UnitConfig> unitConfigList) throws CouldNotPerformException {

        // sort by scope
        Collections.sort(unitConfigList, (o1, o2) -> {
            try {
                return ScopeProcessor.generateStringRep(o1.getScope()).compareTo(ScopeProcessor.generateStringRep(o2.getScope()));
            } catch (CouldNotPerformException ex) {
                ExceptionPrinter.printHistory("Could not sort scope!", ex, System.err);
                return 0;
            }
        });

        // calculate max unit label length
        int maxUnitLabelLength = 0;
        int maxLocationUnitLabelLength = 0;
        int maxScopeLength = 0;
        int maxAliasLength = 0;
        for (final UnitConfig unitConfig : unitConfigList) {
            maxUnitLabelLength = Math.max(maxUnitLabelLength, LabelProcessor.getBestMatch(unitConfig.getLabel()).length());
            maxLocationUnitLabelLength = Math.max(maxLocationUnitLabelLength, getLocationLabel(unitConfig).length());
            maxScopeLength = Math.max(maxScopeLength, ScopeProcessor.generateStringRep(unitConfig.getScope()).length());
            maxAliasLength = Math.max(maxAliasLength, Arrays.toString(unitConfig.getAliasList().toArray()).length());
        }

        // print
        for (final UnitConfig unitConfig : unitConfigList) {
            printUnit(unitConfig, maxAliasLength, maxUnitLabelLength, maxLocationUnitLabelLength, maxScopeLength);
        }
    }

    // todo move to jul
    public class ConsoleColors {
        // Reset
        public static final String RESET = "\033[0m";  // Text Reset

        // Regular Colors
        public static final String BLACK = "\033[0;30m";   // BLACK
        public static final String RED = "\033[0;31m";     // RED
        public static final String GREEN = "\033[0;32m";   // GREEN
        public static final String YELLOW = "\033[0;33m";  // YELLOW
        public static final String BLUE = "\033[0;34m";    // BLUE
        public static final String PURPLE = "\033[0;35m";  // PURPLE
        public static final String CYAN = "\033[0;36m";    // CYAN
        public static final String WHITE = "\033[0;37m";   // WHITE

        // Bold
        public static final String BLACK_BOLD = "\033[1;30m";  // BLACK
        public static final String RED_BOLD = "\033[1;31m";    // RED
        public static final String GREEN_BOLD = "\033[1;32m";  // GREEN
        public static final String YELLOW_BOLD = "\033[1;33m"; // YELLOW
        public static final String BLUE_BOLD = "\033[1;34m";   // BLUE
        public static final String PURPLE_BOLD = "\033[1;35m"; // PURPLE
        public static final String CYAN_BOLD = "\033[1;36m";   // CYAN
        public static final String WHITE_BOLD = "\033[1;37m";  // WHITE

        // Underline
        public static final String BLACK_UNDERLINED = "\033[4;30m";  // BLACK
        public static final String RED_UNDERLINED = "\033[4;31m";    // RED
        public static final String GREEN_UNDERLINED = "\033[4;32m";  // GREEN
        public static final String YELLOW_UNDERLINED = "\033[4;33m"; // YELLOW
        public static final String BLUE_UNDERLINED = "\033[4;34m";   // BLUE
        public static final String PURPLE_UNDERLINED = "\033[4;35m"; // PURPLE
        public static final String CYAN_UNDERLINED = "\033[4;36m";   // CYAN
        public static final String WHITE_UNDERLINED = "\033[4;37m";  // WHITE

        // Background
        public static final String BLACK_BACKGROUND = "\033[40m";  // BLACK
        public static final String RED_BACKGROUND = "\033[41m";    // RED
        public static final String GREEN_BACKGROUND = "\033[42m";  // GREEN
        public static final String YELLOW_BACKGROUND = "\033[43m"; // YELLOW
        public static final String BLUE_BACKGROUND = "\033[44m";   // BLUE
        public static final String PURPLE_BACKGROUND = "\033[45m"; // PURPLE
        public static final String CYAN_BACKGROUND = "\033[46m";   // CYAN
        public static final String WHITE_BACKGROUND = "\033[47m";  // WHITE

        // High Intensity
        public static final String BLACK_BRIGHT = "\033[0;90m";  // BLACK
        public static final String RED_BRIGHT = "\033[0;91m";    // RED
        public static final String GREEN_BRIGHT = "\033[0;92m";  // GREEN
        public static final String YELLOW_BRIGHT = "\033[0;93m"; // YELLOW
        public static final String BLUE_BRIGHT = "\033[0;94m";   // BLUE
        public static final String PURPLE_BRIGHT = "\033[0;95m"; // PURPLE
        public static final String CYAN_BRIGHT = "\033[0;96m";   // CYAN
        public static final String WHITE_BRIGHT = "\033[0;97m";  // WHITE

        // Bold High Intensity
        public static final String BLACK_BOLD_BRIGHT = "\033[1;90m"; // BLACK
        public static final String RED_BOLD_BRIGHT = "\033[1;91m";   // RED
        public static final String GREEN_BOLD_BRIGHT = "\033[1;92m"; // GREEN
        public static final String YELLOW_BOLD_BRIGHT = "\033[1;93m";// YELLOW
        public static final String BLUE_BOLD_BRIGHT = "\033[1;94m";  // BLUE
        public static final String PURPLE_BOLD_BRIGHT = "\033[1;95m";// PURPLE
        public static final String CYAN_BOLD_BRIGHT = "\033[1;96m";  // CYAN
        public static final String WHITE_BOLD_BRIGHT = "\033[1;97m"; // WHITE

        // High Intensity backgrounds
        public static final String BLACK_BACKGROUND_BRIGHT = "\033[0;100m";// BLACK
        public static final String RED_BACKGROUND_BRIGHT = "\033[0;101m";// RED
        public static final String GREEN_BACKGROUND_BRIGHT = "\033[0;102m";// GREEN
        public static final String YELLOW_BACKGROUND_BRIGHT = "\033[0;103m";// YELLOW
        public static final String BLUE_BACKGROUND_BRIGHT = "\033[0;104m";// BLUE
        public static final String PURPLE_BACKGROUND_BRIGHT = "\033[0;105m"; // PURPLE
        public static final String CYAN_BACKGROUND_BRIGHT = "\033[0;106m";  // CYAN
        public static final String WHITE_BACKGROUND_BRIGHT = "\033[0;107m";   // WHITE
    }

    public static void printUnit(final UnitConfig unitConfig, final int maxAliasLength, final int maxUnitLabelLength, final int maxLocationUnitLabelLength, final int maxScopeLength) throws CouldNotPerformException {


        final String prefix, suffix;

        if (unitConfig.getEnablingState().getValue() == State.ENABLED) {
            prefix = ConsoleColors.GREEN;
            suffix = "";
        } else {
            prefix = ConsoleColors.RED;
            suffix = " (" + ConsoleColors.YELLOW + "DISABLED" + ConsoleColors.RESET + ")";
        }

        System.out.println(
                prefix
                        + unitConfig.getId()
                        + "  "
                        + "[ " + StringProcessor.fillWithSpaces(generateStringRep(unitConfig.getAliasList()), maxAliasLength) + " ]"
                        + "  "
                        + StringProcessor.fillWithSpaces(LabelProcessor.getBestMatch(unitConfig.getLabel()), maxUnitLabelLength, StringProcessor.Alignment.RIGHT)
                        + " @ " + StringProcessor.fillWithSpaces(getLocationLabel(unitConfig), maxLocationUnitLabelLength)
                        + "  "
                        + "[ " + StringProcessor.fillWithSpaces(ScopeProcessor.generateStringRep(unitConfig.getScope()), maxScopeLength) + " ]" + ConsoleColors.RESET
                        + suffix
        );
    }

    private static String getLocationLabel(final UnitConfig unitConfig) {
        try {
            return LabelProcessor.getBestMatch(Registries.getUnitRegistry().getUnitConfigById(unitConfig.getPlacementConfig().getLocationId()).getLabel());
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
