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
import org.openbase.bco.dal.lib.layer.unit.Unit;
import org.openbase.bco.dal.lib.layer.unit.UnitRemote;
import org.openbase.bco.dal.remote.unit.UnitRemoteFactoryImpl;
import org.openbase.bco.dal.remote.unit.Units;
import org.openbase.bco.registry.lib.util.UnitConfigProcessor;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.jps.core.JPService;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.extension.rsb.com.RSBRemote;
import org.openbase.jul.extension.rsb.scope.ScopeGenerator;
import org.openbase.jul.iface.annotations.RPCMethod;
import org.openbase.jul.pattern.Remote;
import org.openbase.jul.pattern.Remote.ConnectionState;
import org.openbase.jul.storage.registry.RegistryRemote;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rst.domotic.registry.UnitRegistryDataType.UnitRegistryData;
import rst.domotic.unit.UnitConfigType.UnitConfig;
import rst.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.openbase.bco.manager.util.launch.RSBInterfacePrinter.detectParameterType;
import static org.openbase.bco.manager.util.launch.RSBInterfacePrinter.detectReturnType;
import static org.openbase.bco.manager.util.launch.RSBInterfacePrinter.transformToRSTTypeName;

/**
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public class BCOSystemValidator {

    protected static final Logger LOGGER = LoggerFactory.getLogger(BCOSystemValidator.class);

    public static final String OK = "OK";

    public static final long DELAYED_TIME = TimeUnit.MILLISECONDS.toMillis(500);
    public static final long REQUEST_TIMEOUT = TimeUnit.SECONDS.toMillis(10);

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

    public static void main(String[] args) {

        JPService.setApplicationName("bco-validate");
        JPService.parseAndExitOnError(args);

        try {
            System.out.println("==================================================");
            System.out.println("BaseCubeOne - System Validator");
            System.out.println("==================================================");
            System.out.println();

            System.out.println("Check registries:\n");
            // initial trigger
            Registries.getRegistries(false);
            // wait for connection
            Thread.sleep(1000);
            // check
            for (final RegistryRemote registry : Registries.getRegistries(false)) {
                check(registry);
            }

            System.out.print("Check units\n\n");
            Future<List<UnitRemote<?>>> futureUnits = Units.getFutureUnits(false);
            System.out.println("Request "+check(futureUnits));
            System.out.println();
            for (final UnitRemote<?> unit : futureUnits.get()) {
                check(unit);
            }
        } catch (InterruptedException ex) {
            System.out.println("killed");
            System.exit(253);
            return;
        } catch (Exception ex) {
            ExceptionPrinter.printHistory(new CouldNotPerformException("Could not validate system!", ex), System.err);
            System.exit(254);
        }

        System.exit(0);
    }

    public static String check(final Future future) {
        try {
            try {
                future.get(DELAYED_TIME, TimeUnit.MILLISECONDS);
            } catch (TimeoutException e) {
                future.get(REQUEST_TIMEOUT, TimeUnit.MILLISECONDS);
                return AnsiColor.colorize("DELAYED", AnsiColor.ANSI_GREEN);
            }
        } catch (InterruptedException e1) {
            return AnsiColor.colorize("INTERRUPTED", AnsiColor.ANSI_YELLOW);
        } catch (ExecutionException e1) {
            return AnsiColor.colorize("FAILED", AnsiColor.ANSI_RED);
        } catch (TimeoutException e1) {
            return AnsiColor.colorize("TIMEOUT", AnsiColor.ANSI_CYAN);
        }
        return AnsiColor.colorize("OK", AnsiColor.ANSI_GREEN);
    }

    public static void check(final Remote<?> remote) {
        final List<String> resultList = new ArrayList<>();

        final ConnectionState connectionState = remote.getConnectionState();
        String connectionDescription = "Connection\t\t";
        switch (connectionState) {
            case CONNECTED:
                connectionDescription += AnsiColor.colorize(OK, AnsiColor.ANSI_GREEN);
                break;
            case DISCONNECTED:
                connectionDescription += AnsiColor.colorize(connectionState.name(), AnsiColor.ANSI_RED);
                break;
            case CONNECTING:
            case RECONNECTING:
                connectionDescription += AnsiColor.colorize(connectionState.name(), AnsiColor.ANSI_RED);
                break;
            default:
                connectionDescription += AnsiColor.colorize("UNKNOWN", AnsiColor.ANSI_RED);
                break;
        }
        connectionDescription += " [" + remote + "]";
        resultList.add(connectionDescription);

        resultList.add("Data Available \t" + check(remote.getDataFuture()) + " [" + remote + "]");

        try {
            resultList.add("Data Synced \t" + check(remote.requestData()) + " [" + remote + "]");
        } catch (CouldNotPerformException e) {
            resultList.add("Data Synced \t" + AnsiColor.colorize("CANCELED", AnsiColor.ANSI_RED) + " [" + remote + "]");
        }

        resultList.add("Ping \t\t\t" + check(remote.ping()) + " [" + remote + "]");

        // only print in error case or if verbose mode is enabled.
        for(final String result : resultList) {
            if(!result.contains(OK) || JPService.verboseMode()) {
                System.out.println(result);
            }
        }
    }
}
