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
import org.openbase.bco.registry.lib.BCO;
import org.openbase.bco.registry.lib.util.UnitConfigProcessor;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.jps.core.JPService;
import org.openbase.jps.preset.JPDebugMode;
import org.openbase.jps.preset.JPVerbose;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.FatalImplementationErrorException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.extension.rsb.com.RSBRemote;
import org.openbase.jul.extension.rsb.scope.ScopeGenerator;
import org.openbase.jul.iface.annotations.RPCMethod;
import org.openbase.jul.pattern.Remote;
import org.openbase.jul.pattern.Remote.ConnectionState;
import org.openbase.jul.processing.StringProcessor;
import org.openbase.jul.processing.StringProcessor.Alignment;
import org.openbase.jul.storage.registry.RegistryRemote;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rst.domotic.registry.UnitRegistryDataType.UnitRegistryData;
import rst.domotic.unit.UnitConfigType.UnitConfig;
import rst.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;

import java.lang.reflect.Method;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;

import static org.openbase.bco.manager.util.launch.RSBInterfacePrinter.detectParameterType;
import static org.openbase.bco.manager.util.launch.RSBInterfacePrinter.detectReturnType;
import static org.openbase.bco.manager.util.launch.RSBInterfacePrinter.transformToRSTTypeName;

/**
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public class BCOSystemValidator {

    protected static final Logger LOGGER = LoggerFactory.getLogger(BCOSystemValidator.class);

    public static final String OK = "OK";
    public static final int STATE_RANGE = 12;
    public static final int LABEL_RANGE = 22;

    public static final long DELAYED_TIME = TimeUnit.MILLISECONDS.toMillis(500);
    public static final long DEFAULT_UNIT_POOL_DELAY_TIME = TimeUnit.SECONDS.toMillis(3);
    public static final long REQUEST_TIMEOUT = TimeUnit.SECONDS.toMillis(3);

    public static final DecimalFormat pingFormat = new DecimalFormat("#.###");

    private static int errorCounter = 0;

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

    public static void countError() {
        ++errorCounter;
    }

    public static void main(String[] args) {
        BCO.printLogo();
        JPService.setApplicationName("bco-validate");
        JPService.registerProperty(JPDebugMode.class);
        JPService.registerProperty(JPVerbose.class);
        JPService.parseAndExitOnError(args);

        try {
            System.out.println("==================================================");
            System.out.println("BaseCubeOne - System Validator");
            System.out.println("==================================================");
            System.out.println();

            System.out.println("=== " + AnsiColor.colorize("Check Registries", AnsiColor.ANSI_BLUE) + " ===\n");

            // check
            for (final RegistryRemote registry : Registries.getRegistries(false)) {
                if (!check(registry)) {
                    System.out.println(StringProcessor.fillWithSpaces(registry.getName(), LABEL_RANGE, Alignment.RIGHT) + "  " + AnsiColor.colorize(OK, AnsiColor.ANSI_GREEN));
                }
            }
            System.out.println();

            System.out.println("=== " + AnsiColor.colorize("Check Units", AnsiColor.ANSI_BLUE) + " ===\n");
            Future<List<UnitRemote<?>>> futureUnits = Units.getFutureUnits(false);
            System.out.println(StringProcessor.fillWithSpaces("Unit Pool", LABEL_RANGE, Alignment.RIGHT) + "  " + check(futureUnits, DEFAULT_UNIT_POOL_DELAY_TIME));
            System.out.println();

            if (!futureUnits.isDone() || futureUnits.isCancelled()) {
                System.out.println(AnsiColor.colorize("Connection could not be established, please make sure BaseCubeOne is up and running!\n", AnsiColor.ANSI_YELLOW));
            } else {
                boolean printed = false;
                for (final UnitRemote<?> unit : futureUnits.get()) {
                    printed = check(unit) || printed;
                }
                if (!printed) {
                    System.out.println(StringProcessor.fillWithSpaces("Unit Connections", LABEL_RANGE, Alignment.RIGHT) + "  " + AnsiColor.colorize(OK, AnsiColor.ANSI_GREEN));
                }
            }
        } catch (InterruptedException ex) {
            System.out.println("killed");
            System.exit(253);
            return;
        } catch (Exception ex) {
            ExceptionPrinter.printHistory(new CouldNotPerformException("Could not validate system!", ex), System.err);
            System.exit(254);
        }

        System.out.println();
        System.out.println("==============================================================");
        System.out.print("===  ");
        switch (errorCounter) {
            case 0:
                System.out.print(AnsiColor.colorize("VALIDATION SUCCESSFUL", AnsiColor.ANSI_GREEN));
                break;
            default:
                System.out.print(errorCounter + " " + AnsiColor.colorize("ERROR" + (errorCounter > 1 ? "S" : "") + " DETECTED", AnsiColor.ANSI_RED));
                break;
        }
        System.out.println(" average ping is "+AnsiColor.colorize(pingFormat.format(getGlobalPing()), AnsiColor.ANSI_CYAN)+" milli");
        System.out.println("==============================================================");
        System.out.println();
        System.exit(Math.min(errorCounter, 200));
    }

    public static String check(final Future future) {
        return check(future, DELAYED_TIME, null);
    }

    public static String check(final Future future, final long delayTime) {
        return check(future, delayTime, null);
    }

    public static String check(final Future future, final String suffixCallable) {
        return check(future, DELAYED_TIME, null);
    }

    public static String check(final Future future, final long delayTime, final Callable<String> suffixCallable) {
        try {
            try {
                future.get(delayTime, TimeUnit.MILLISECONDS);
            } catch (TimeoutException e) {
                future.get(REQUEST_TIMEOUT, TimeUnit.MILLISECONDS);
                return AnsiColor.colorize(StringProcessor.fillWithSpaces("DELAYED", STATE_RANGE, Alignment.LEFT), AnsiColor.ANSI_GREEN);
            }
        } catch (InterruptedException ex) {
            countError();
            return AnsiColor.colorize(StringProcessor.fillWithSpaces("INTERRUPTED", STATE_RANGE, Alignment.LEFT), AnsiColor.ANSI_YELLOW);
        } catch (ExecutionException ex) {
            countError();
            return AnsiColor.colorize(StringProcessor.fillWithSpaces("FAILED", STATE_RANGE, Alignment.LEFT), AnsiColor.ANSI_RED);
        } catch (TimeoutException ex) {
            countError();
            return AnsiColor.colorize(StringProcessor.fillWithSpaces("TIMEOUT", STATE_RANGE, Alignment.LEFT), AnsiColor.ANSI_CYAN);
        }

        String suffix;
        try {
            suffix = (suffixCallable != null ? suffixCallable.call() : "");
        } catch (Exception e) {
            suffix = "?";
        }

        return AnsiColor.colorize(StringProcessor.fillWithSpaces("OK" + suffix, STATE_RANGE, Alignment.LEFT), AnsiColor.ANSI_GREEN);
    }

    public static boolean check(final Remote<?> remote) {
        return check(remote, DELAYED_TIME);
    }

    public static boolean check(final Remote<?> remote, final long delayTime) {


        boolean printed = false;

        Future<Long> futurePing = remote.ping();

        try {
            printed |= print(remote, StringProcessor.fillWithSpaces("Ping", LABEL_RANGE, Alignment.RIGHT) + "  " + check(futurePing, delayTime, new Callable<String>() {
                @Override
                public String call() throws Exception {
                    if (futurePing.isDone() && !futurePing.isCancelled()) {
                        BCOSystemValidator.computeGlobalPing(futurePing.get());
                        return " (" + futurePing.get() + ")";
                    } else {
                        return "";
                    }
                }
            }));
        } catch (CancellationException ex) {
            throw ex;
        } catch (Exception ex) {
            ExceptionPrinter.printHistory(new FatalImplementationErrorException(BCOSystemValidator.class, ex), LOGGER);
        }

        boolean online = futurePing.isDone() && !futurePing.isCancelled();

        if (online) {
            final ConnectionState connectionState = remote.getConnectionState();
            String connectionDescription = StringProcessor.fillWithSpaces("Connection", LABEL_RANGE, Alignment.RIGHT) + "  ";
            switch (connectionState) {
                case CONNECTED:
                    connectionDescription += AnsiColor.colorize(StringProcessor.fillWithSpaces(OK, STATE_RANGE, Alignment.LEFT), AnsiColor.ANSI_GREEN);
                    break;
                case DISCONNECTED:
                case CONNECTING:
                    countError();
                    connectionDescription += AnsiColor.colorize(StringProcessor.fillWithSpaces(connectionState.name(), STATE_RANGE, Alignment.LEFT), AnsiColor.ANSI_RED);
                    break;
                case RECONNECTING:
                    connectionDescription += AnsiColor.colorize(StringProcessor.fillWithSpaces(connectionState.name(), STATE_RANGE, Alignment.LEFT), AnsiColor.ANSI_YELLOW);
                    break;
                default:
                    connectionDescription += AnsiColor.colorize(StringProcessor.fillWithSpaces("UNKNOWN", STATE_RANGE, Alignment.LEFT), AnsiColor.ANSI_RED);
                    countError();
                    break;
            }
            printed |= print(remote, connectionDescription);
        }

        if (online) {
            printed |= print(remote, StringProcessor.fillWithSpaces("Data Cache", LABEL_RANGE, Alignment.RIGHT) + "  " + check(remote.getDataFuture()));
        } else {
            printed |= print(remote, StringProcessor.fillWithSpaces("Data Cache", LABEL_RANGE, Alignment.RIGHT) + "  " + (remote.isDataAvailable() ? AnsiColor.colorize(StringProcessor.fillWithSpaces("OFFLINE", STATE_RANGE, Alignment.LEFT), AnsiColor.ANSI_YELLOW) : AnsiColor.colorize(StringProcessor.fillWithSpaces("EMPTY", STATE_RANGE, Alignment.LEFT), AnsiColor.ANSI_RED)));
        }

        if (online) {
            try {
                printed |= print(remote, StringProcessor.fillWithSpaces("Synchronization", LABEL_RANGE, Alignment.RIGHT) + "  " + check(remote.requestData()));
            } catch (CouldNotPerformException e) {
                countError();
                printed |= print(remote, StringProcessor.fillWithSpaces("Synchronization", LABEL_RANGE, Alignment.RIGHT) +
                        "  " + AnsiColor.colorize(StringProcessor.fillWithSpaces("CANCELED", STATE_RANGE, Alignment.LEFT), AnsiColor.ANSI_RED));
            }
        }

        // add new line separator for better overview
        if (printed) {
            System.out.println();
        }

        return printed;
    }

    public static boolean print(final Remote remote, final String result) {
        // only print in error case or if verbose mode is enabled.
        if (!result.contains(OK) || JPService.verboseMode()) {
            System.out.println(result + "[" + remote + "]");
            return true;
        }
        return false;
    }


    private static double globalPingAverage = 0;
    private static double globalPingComputations = 0;

    public synchronized static double computeGlobalPing(long ping) {

        // skip 0 ping.
        if(ping <= 0) {
            return globalPingAverage;
        }
        globalPingAverage = (globalPingComputations * globalPingAverage + ping) / (globalPingComputations + 1);
        globalPingComputations++;
        return globalPingAverage;
    }

    private static double getGlobalPing() {
        return globalPingAverage;
    }
}
