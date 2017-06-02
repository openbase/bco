package org.openbase.bco.manager.util.launch;

/*-
 * #%L
 * BCO Manager Utility
 * %%
 * Copyright (C) 2015 - 2017 openbase.org
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
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Future;
import org.openbase.bco.dal.remote.unit.UnitRemoteFactoryImpl;
import org.openbase.bco.registry.lib.util.UnitConfigProcessor;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.jps.core.JPService;
import static org.openbase.jps.core.JPService.printHelp;
import org.openbase.jps.preset.JPVerbose;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.extension.rsb.scope.ScopeGenerator;
import org.openbase.jul.iface.annotations.RPCMethod;
import rst.domotic.unit.UnitConfigType.UnitConfig;
import rst.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;

/**
 *
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public class RSBInterfacePrinter {

    public enum AnsiColor {

        ANSI_RESET("\u001B[0m"),
        ANSI_BLACK("\u001B[30m"),
        ANSI_RED("\u001B[31m"),
        ANSI_GREEN("\u001B[32m"),
        ANSI_YELLOW("\u001B[33m"),
        ANSI_BLUE("\u001B[34m"),
        ANSI_PURPLE("\u001B[35m"),
        ANSI_CYAN("\u001B[36m"),
        ANSI_WHITE("\u001B[37m");

        private String color;

        private AnsiColor(String color) {
            this.color = color;
        }

        public String getColor() {
            return color;
        }

        public static String colorize(final String text, final AnsiColor color) {
            return color.getColor() + text + ANSI_RESET.getColor();
        }

        public static String colorizeRegex(final String text, final String regex, final AnsiColor color) {
            return text.replaceAll(regex, colorize(regex, color));
        }
    }

    public static final AnsiColor SCOPE_COLOR = AnsiColor.ANSI_GREEN;
    public static final AnsiColor RETURN_LIMITER_COLOR = AnsiColor.ANSI_GREEN;
    public static final AnsiColor PARAMETER_LIMITER_COLOR = AnsiColor.ANSI_RED;
    public static final AnsiColor TYPE_LIMITER_COLOR = AnsiColor.ANSI_RED;
    public static final AnsiColor UNIT_TYPE_COLOR = AnsiColor.ANSI_CYAN;

    public static String colorize(String text) {

        text = AnsiColor.colorizeRegex(text, "\\/", SCOPE_COLOR);
        text = AnsiColor.colorizeRegex(text, "\\:", RETURN_LIMITER_COLOR);
        text = AnsiColor.colorizeRegex(text, "\\)", PARAMETER_LIMITER_COLOR);
        text = AnsiColor.colorizeRegex(text, "\\(", PARAMETER_LIMITER_COLOR);
        text = AnsiColor.colorizeRegex(text, "\\>", TYPE_LIMITER_COLOR);
        text = AnsiColor.colorizeRegex(text, "\\<", TYPE_LIMITER_COLOR);

        return text;
    }

    /**
     * This is a command line tool to query registry entries.
     *
     * TODOs
     * - JPService should be used in a future release.
     * - Resolve units via raw protobuf message.
     *
     * @param args
     */
    public static void main(String[] args) {
        boolean resultsFound = false;

        try {

            JPService.setApplicationName("bco-interface-printer");
            JPService.registerProperty(JPVerbose.class, true);

            // help
            if (args.length > 0 && (args[0].equals("-h") || args[0].equals("--help"))) {
                printHelp();
                System.exit(0);
            }

            // init
            Registries.waitForData();
            final ArrayList<UnitConfig> unitConfigs = new ArrayList<>();

            System.out.println(colorize("BaseCubeOne DAL <UnitScope> Structure"));
            System.out.println(colorize("    / <LocationScope> / <UnitType> / <UnitLabel> / <CommunicationPattern> : <UnitDataType>"));
            System.out.println("");
            System.out.println("    Each unit publishes status changes via a dedecated rsb informer on the following status scope:");
            System.out.println(colorize("    / <UnitScope> / status"));
            System.out.println("");
            System.out.println("    Each unit provides a set of rsb remote procedure call (rpc) methods accessable by there control scope:");
            System.out.println(colorize("    / <UnitScope> / ctrl"));
            System.out.println("");
            System.out.println("");
            System.out.println("This static set of rpc methods is provided by all units:");
            System.out.println("");
            System.out.println(colorize("    / <UnitScope> / ctrl / ping() : The connection delay as integer"));
            System.out.println(colorize("    / <UnitScope> / ctrl / requestStatus() : <UnitDataType>"));
            System.out.println("");
            System.out.println("Furthermore each unit provids a type specific set of rpc methods related to there services:");
            System.out.println("");

            List<Method> methodSet = new ArrayList();
            String unitDataType;

            // print by unit type
            unitConfigs.clear();
            for (final UnitType unitType : UnitType.values()) {
                try {
                    // skip unknown type
                    if (unitType == UnitType.UNKNOWN) {
                        continue;
                    }

                    try {
                        unitDataType = transformToRSTTypeName(UnitConfigProcessor.getUnitDataClass(unitType).getName());
                    } catch (final NotAvailableException ex) {
                        unitDataType = "?";
                    }

                    System.out.println("    " + AnsiColor.colorize(unitType.name(), UNIT_TYPE_COLOR) + colorize(" : " + unitDataType));

                    // clear methods
                    methodSet.clear();

                    try {
                        Object newInstance = UnitRemoteFactoryImpl.getInstance().newInstance(unitType);
                        Class<?>[] interfaces = newInstance.getClass().getInterfaces();
                        for (Class<?> iface : interfaces) {
//                        System.out.println("found interface: " + iface.getName());
                            methodSet.addAll(Arrays.asList(iface.getMethods()));
                        }
                        methodSet.addAll(Arrays.asList(newInstance.getClass().getMethods()));
                    } catch (CouldNotPerformException ex) {
                        System.out.println("        Auto detection not possible.\n");
                        continue;
                    }

                    final List<Method> methodList = new ArrayList();
                    for (final Method method : methodSet) {

                        // filter non rpc methods
                        if (method.getAnnotation(RPCMethod.class) == null) {

                            if (method.getName().equals("requestStatus")) {
                                System.out.println("halt!");
                            }
                            continue;
                        }

                        // filter default methods because they do not provide real data types.
                        if (method.isDefault()) {
                            continue;
                        }
                        methodList.add(method);
                    }

                    Collections.sort(methodList, new Comparator<Method>() {
                        @Override
                        public int compare(Method o1, Method o2) {
                            return o1.getName().compareTo(o2.getName());
                        }
                    });

                    for (final Method method : methodList) {

                        // detect method param type
                        final String params = (method.getParameterTypes().length != 0) ? method.getParameterTypes()[0].getName() : "";

//                    System.out.println("getAnnotatedReturnType" + method.getAnnotatedReturnType());
//                    System.out.println("getReturnType" + method.getReturnType());
//                    System.out.println("getGenericReturnType" + method.getGenericReturnType());
                        // detect method return type
                        String returnType;
                        if (method.getReturnType().isAssignableFrom(Future.class)) {
                            returnType = method.getGenericReturnType().toString();
                        } else {
                            returnType = method.getGenericReturnType().toString();
                        }

                        returnType = transformToRSTTypeName(returnType);

                        // print method representation
                        System.out.println(colorize("        / <LocationScope> / " + ScopeGenerator.convertIntoValidScopeComponent(unitType.name()) + " / <UnitLabel> / ctrl / " + method.getName() + "(" + transformToRSTTypeName(params) + ")" + (returnType.isEmpty() ? "" : " : " + returnType)));
                    }
                    System.out.println("");
                } catch (Exception ex) {
                    ExceptionPrinter.printHistory(new CouldNotPerformException("Could not extract rsb interface of :" + unitType.name()), System.err);
                }
            }
        } catch (InterruptedException ex) {
            System.out.println("killed");
            System.exit(253);
            return;
        } catch (Exception ex) {
            ExceptionPrinter.printHistory(new CouldNotPerformException("Could not detect interface!", ex), System.err);
//            printHelp();
            System.exit(254);
        }

        System.exit(0);
    }

    public static String transformToRSTTypeName(final String inputClassName) {

        String className = inputClassName;
        // transfom void argument to empty arg string.
        className = className.replaceAll(Void.class.getName(), "");
        className = className.replaceAll("void", "");
        className = className.replaceAll("class", "");
        className = className.replaceAll(Integer.class.getName(), Integer.class.getSimpleName());
        className = className.replaceAll(Boolean.class.getName(), Boolean.class.getSimpleName());
        className = className.replaceAll(String.class.getName(), String.class.getSimpleName());
        className = className.replaceAll(Long.class.getName(), Long.class.getSimpleName());
        className = className.replaceAll(Float.class.getName(), Float.class.getSimpleName());
        className = className.replaceAll("java.util.concurrent.Future", "");
        className = className.replaceAll("\\<", "");
        className = className.replaceAll("\\>", "");
        className = className.replaceAll("\\$", ".");
//        
////        if(!className.equals(inputClassName)) {
////            return transformToRSTTypeName(className);
////        }
        return className.trim();
    }

//    private static void printHelp() {
//        System.out.println("");
//        System.out.println("");
//        System.out.println("Usage:   bco-registry-query ${UNIT_TYPE}");
//        System.out.println("         bco-registry-query ${UNIT_LOCATION}");
//        System.out.println("         bco-registry-query ${UNIT_LABEL}");
//        System.out.println("");
//        System.out.println("Example: bco-registry-query colorablelight");
//        System.out.println("         bco-registry-query livingroom");
//        System.out.println("         bco-registry-query ceilinglamp");
//        System.out.println("");
//        System.out.println("Print:   ${ID} ${LABEL} @ ${LOCATION} ${SCOPE}");
//        System.out.println("");
//    }
//
//    public static void printUnits(List<UnitConfig> unitConfigList) throws InterruptedException, CouldNotPerformException {
//
//        // sort by scope
//        Collections.sort(unitConfigList, new Comparator<UnitConfig>() {
//            @Override
//            public int compare(UnitConfig o1, UnitConfig o2) {
//                try {
//                    return ScopeGenerator.generateStringRep(o1.getScope()).compareTo(ScopeGenerator.generateStringRep(o2.getScope()));
//                } catch (CouldNotPerformException ex) {
//                    ExceptionPrinter.printHistory("Could not sort scope!", ex, System.err);
//                    return 0;
//                }
//            }
//        });
//
//        // calculate max unit label length
//        int maxUnitLabelLength = 0;
//        int maxLocationUnitLabelLength = 0;
//        int maxScopeLength = 0;
//        for (final UnitConfig unitConfig : unitConfigList) {
//            maxUnitLabelLength = Math.max(maxUnitLabelLength, unitConfig.getLabel().length());
//            maxLocationUnitLabelLength = Math.max(maxLocationUnitLabelLength, getLocationLabel(unitConfig).length());
//            maxScopeLength = Math.max(maxScopeLength, ScopeGenerator.generateStringRep(unitConfig.getScope()).length());
//        }
//
//        // print
//        for (final UnitConfig unitConfig : unitConfigList) {
//            printUnit(unitConfig, maxUnitLabelLength, maxLocationUnitLabelLength, maxScopeLength);
//        }
//    }
//
//    public static void printUnit(final UnitConfig unitConfig, final int maxUnitLabelLength, final int maxLocationUnitLabelLength, final int maxScopeLength) throws InterruptedException, CouldNotPerformException {
//        System.out.println(unitConfig.getId()
//                + "  "
//                + StringProcessor.fillWithSpaces(unitConfig.getLabel(), maxUnitLabelLength, StringProcessor.Alignment.RIGHT)
//                + " @ " + StringProcessor.fillWithSpaces(getLocationLabel(unitConfig), maxLocationUnitLabelLength)
//                + "  "
//                + "[ " + StringProcessor.fillWithSpaces(ScopeGenerator.generateStringRep(unitConfig.getScope()), maxScopeLength) + " ]"
//        );
//    }
//
//    private static String getLocationLabel(final UnitConfig unitConfig) throws InterruptedException {
//        try {
//            return Registries.getLocationRegistry().getLocationConfigById(unitConfig.getPlacementConfig().getLocationId()).getLabel();
//        } catch (CouldNotPerformException ex) {
//            return "?";
//        }
//    }
}
