package org.openbase.bco.manager.util.launch;

/*-
 * #%L
 * BCO Manager Utility
 * %%
 * Copyright (C) 2015 - 2018 openbase.org
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
import com.google.protobuf.GeneratedMessage;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Future;
import org.openbase.bco.dal.remote.unit.UnitRemoteFactoryImpl;
import org.openbase.bco.registry.lib.util.UnitConfigProcessor;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.jps.core.JPService;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.extension.rsb.com.RSBRemote;
import org.openbase.jul.extension.rsb.scope.ScopeGenerator;
import org.openbase.jul.iface.annotations.RPCMethod;
import org.openbase.jul.storage.registry.RegistryRemote;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rst.domotic.registry.UnitRegistryDataType.UnitRegistryData;
import rst.domotic.unit.UnitConfigType.UnitConfig;
import rst.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;

/**
 *
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public class RSBInterfacePrinter {

    protected static final Logger LOGGER = LoggerFactory.getLogger(RSBInterfacePrinter.class);

    public enum AnsiColor {

        // todo: move to jul
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
    public static final AnsiColor SUB_HEADLINE = AnsiColor.ANSI_CYAN;
    public static final AnsiColor TYPE_LIMITER_COLOR = AnsiColor.ANSI_RED;
    public static final AnsiColor UNIT_TYPE_COLOR = SUB_HEADLINE;
    public static final AnsiColor REGISTRY_TYPE_COLOR = SUB_HEADLINE;

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
     * This method returns the registry data class resolved by the given registry name.
     *
     * @param registryDataClassSimpleName the name is used to extract the registry data class.
     * @return the unit data class.
     * @throws org.openbase.jul.exception.NotAvailableException is thrown if the data class name could not be detected.
     */
    public static Class<? extends GeneratedMessage> getRegistryDataClass(final String registryDataClassSimpleName) throws NotAvailableException {
        final String registryDataClassName = UnitRegistryData.class.getPackage().getName() + "." + registryDataClassSimpleName + "DataType$" + registryDataClassSimpleName + "Data";

        try {
            return (Class<? extends GeneratedMessage>) Class.forName(registryDataClassName);
        } catch (NullPointerException | ClassNotFoundException | ClassCastException ex) {
            throw new NotAvailableException("RegistryDataClass", registryDataClassName, new CouldNotPerformException("Could not detect class!", ex));
        }
    }

    public static void main(String[] args) {

        JPService.setApplicationName("bco-interface-printer");
        JPService.parseAndExitOnError(args);

        try {
            System.out.println("==================================================");
            System.out.println("BaseCubeOne - Registry (Service Discovery) RSB API ");
            System.out.println("==================================================");
            System.out.println();
            System.out.println("General Information:");
            System.out.println();
            System.out.println(AnsiColor.colorize("    Registry Scope Structure:", SUB_HEADLINE));
            System.out.println(colorize("        / registry / <RegistryName> / <CommunicationPattern> : <RegistryDataType>"));
            System.out.println();
            System.out.println(AnsiColor.colorize("    Each registry publishes status changes via a dedecated rsb informer on the following status scope:", SUB_HEADLINE));
            System.out.println(colorize("        / <RegistryScope> / status"));
            System.out.println();
            System.out.println(AnsiColor.colorize("    Each registry provides a set of rsb remote procedure call (rpc) methods accessable by there control scope:", SUB_HEADLINE));
            System.out.println(colorize("        / <RegistryScope> / ctrl"));
            System.out.println();
            System.out.println();
            System.out.println("This is the set of rpc methods provided by all registries:");
            System.out.println();
            System.out.println("    " + AnsiColor.colorize("All", UNIT_TYPE_COLOR));
            System.out.println(colorize("        / <RegistryScope> / ctrl / ping() : The connection delay as integer"));
            System.out.println(colorize("        / <RegistryScope> / ctrl / requestStatus() : <RegistryDataType>"));
            System.out.println();
            System.out.println("This is the registry specific set of rpc methods:");
            System.out.println();
            String unitDataType;

            for (final RegistryRemote registry : Registries.getRegistries(true)) {

                // print unit data type
                try {
                    unitDataType = transformToRSTTypeName(getRegistryDataClass(registry.getName()).getName());
                } catch (final NotAvailableException ex) {
                    ExceptionPrinter.printHistory(ex, LOGGER);
                    unitDataType = "?";
                }

                System.out.println("    " + AnsiColor.colorize(registry.getName(), UNIT_TYPE_COLOR) + colorize(" : " + unitDataType));

                final String scope = ScopeGenerator.generateStringRep(((RSBRemote) registry).getScope());
                for (final Method method : detectRPCMethods(registry)) {
                    final String params = detectParameterType(method);
                    final String returnType = detectReturnType(method);
                    System.out.println(colorize("       " + scope.replace("/", " / ") + "ctrl / " + method.getName() + "(" + transformToRSTTypeName(params) + ")" + (returnType.isEmpty() ? "" : " : " + returnType)));
                }
                System.out.println();
            }

            // init
            Registries.waitForData();
            final ArrayList<UnitConfig> unitConfigs = new ArrayList<>();
            System.out.println("===========================================================");
            System.out.println(colorize("BaseCubeOne - DAL (Domotic Abstract Layer) RSB API"));
            System.out.println("===========================================================");
            System.out.println();
            System.out.println("General Information:");
            System.out.println();
            System.out.println(AnsiColor.colorize("    Unit Scope Structure:", SUB_HEADLINE));
            System.out.println(colorize("        / <LocationScope> / <UnitType> / <UnitLabel> / <CommunicationPattern> : <UnitDataType>"));
            System.out.println();
            System.out.println(AnsiColor.colorize("    Each unit publishes status changes via a dedecated rsb informer on the following status scope:", SUB_HEADLINE));
            System.out.println(colorize("        / <UnitScope> / status"));
            System.out.println();
            System.out.println(AnsiColor.colorize("    Each unit provides a set of rsb remote procedure call (rpc) methods accessable by there control scope:", SUB_HEADLINE));
            System.out.println(colorize("        / <UnitScope> / ctrl"));
            System.out.println();
            System.out.println();
            System.out.println("This static set of rpc methods is provided by all units:");
            System.out.println();
            System.out.println("    " + AnsiColor.colorize("All", UNIT_TYPE_COLOR));
            System.out.println(colorize("        / <UnitScope> / ctrl / ping() : The connection delay as integer"));
            System.out.println(colorize("        / <UnitScope> / ctrl / requestStatus() : <UnitDataType>"));
            System.out.println();
            System.out.println("Furthermore each unit provids a type specific set of rpc methods related to there services:");
            System.out.println();

            // print by unit type
            unitConfigs.clear();
            for (final UnitType unitType : UnitType.values()) {
                try {
                    // skip unknown type
                    if (unitType == UnitType.UNKNOWN) {
                        continue;
                    }

                    // print unit data type
                    try {
                        unitDataType = transformToRSTTypeName(UnitConfigProcessor.getUnitDataClass(unitType).getName());
                    } catch (final NotAvailableException ex) {
                        unitDataType = "?";
                    }
                    System.out.println("    " + AnsiColor.colorize(unitType.name(), UNIT_TYPE_COLOR) + colorize(" : " + unitDataType));

                    // print unit rpc methods
                    final Object unitInstance;
                    try {
                        unitInstance = UnitRemoteFactoryImpl.getInstance().newInstance(unitType);
                    } catch (CouldNotPerformException ex) {
                        System.out.println("        Auto detection not possible.\n");
                        continue;
                    }

                    for (final Method method : detectRPCMethods(unitInstance)) {
                        final String params = detectParameterType(method);
                        final String returnType = detectReturnType(method);
                        System.out.println(colorize("        / <LocationScope> / " + ScopeGenerator.convertIntoValidScopeComponent(unitType.name()) + " / <UnitLabel> / ctrl / " + method.getName() + "(" + transformToRSTTypeName(params) + ")" + (returnType.isEmpty() ? "" : " : " + returnType)));
                    }
                    System.out.println();
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
            System.exit(254);
        }

        System.exit(0);
    }

    public static List<Method> detectRPCMethods(final Object instance) {
        final List<Method> methodList = new ArrayList();
        for (final Method method : detectMethods(instance)) {

            // filter non rpc methods
            if (method.getAnnotation(RPCMethod.class) == null) {
                continue;
            }

            // filter default methods because they do not provide real data types.
            if (method.isDefault()) {
                continue;
            }
            methodList.add(method);
        }
        return sortMethodList(methodList);
    }

    public static List<Method> detectMethods(final Object instance) {
        final List<Method> methodSet = new ArrayList();
        Class<?>[] interfaces = instance.getClass().getInterfaces();
        for (Class<?> iface : interfaces) {
            // System.out.println("found interface: " + iface.getName());
            methodSet.addAll(Arrays.asList(iface.getMethods()));
        }
        methodSet.addAll(Arrays.asList(instance.getClass().getMethods()));
        return methodSet;
    }

    public static String detectParameterType(final Method method) {
        // detect method param type
        return (method.getParameterTypes().length != 0) ? method.getParameterTypes()[0].getName() : "";
    }

    public static String detectReturnType(final Method method) {
        // detect method return type
        String returnType;
        if (method.getReturnType().isAssignableFrom(Future.class)) {
            returnType = method.getGenericReturnType().toString();
        } else {
            returnType = method.getGenericReturnType().toString();
        }

        // System.out.println("getAnnotatedReturnType" + method.getAnnotatedReturnType());
        // System.out.println("getReturnType" + method.getReturnType());
        // System.out.println("getGenericReturnType" + method.getGenericReturnType());
        return transformToRSTTypeName(returnType);
    }

    public static List<Method> sortMethodList(final List<Method> methodList) {
        Collections.sort(methodList, (Method o1, Method o2) -> o1.getName().compareTo(o2.getName()));
        return methodList;
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
        return className.trim();
    }
}
