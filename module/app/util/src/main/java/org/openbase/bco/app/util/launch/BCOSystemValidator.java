package org.openbase.bco.app.util.launch;

/*-
 * #%L
 * BCO App Utility
 * %%
 * Copyright (C) 2018 - 2021 openbase.org
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

import org.apache.commons.collections4.comparators.BooleanComparator;
import org.openbase.bco.app.util.launch.jp.JPExitOnError;
import org.openbase.bco.app.util.launch.jp.JPWaitForData;
import org.openbase.bco.authentication.lib.BCO;
import org.openbase.bco.dal.lib.layer.unit.UnitRemote;
import org.openbase.bco.dal.remote.layer.unit.Units;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.jps.core.JPService;
import org.openbase.jps.preset.JPDebugMode;
import org.openbase.jps.preset.JPVerbose;
import org.openbase.jul.communication.controller.AbstractRemoteClient;
import org.openbase.jul.communication.jp.JPComHost;
import org.openbase.jul.communication.jp.JPComPort;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.ExceptionProcessor;
import org.openbase.jul.exception.FatalImplementationErrorException;
import org.openbase.jul.exception.InvalidStateException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.exception.printer.LogLevel;
import org.openbase.jul.pattern.controller.Remote;
import org.openbase.jul.processing.StringProcessor;
import org.openbase.jul.processing.StringProcessor.Alignment;
import org.openbase.type.domotic.state.ConnectionStateType.ConnectionState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

/**
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public class BCOSystemValidator {

    public static final String OK = "OK";
    public static final String BUSY = "BUSY";
    public static final int STATE_RANGE = 12;
    public static final int LABEL_RANGE = 22;
    public static final long DELAYED_TIME = TimeUnit.MILLISECONDS.toMillis(500);
    public static final long DEFAULT_UNIT_POOL_DELAY_TIME = TimeUnit.SECONDS.toMillis(5);
    public static final long REQUEST_TIMEOUT = TimeUnit.SECONDS.toMillis(10);
    public static final DecimalFormat pingFormat = new DecimalFormat("#.###");
    protected static final Logger LOGGER = LoggerFactory.getLogger(BCOSystemValidator.class);
    protected static int errorCounter = 0;
    protected static double globalPingAverage = 0;
    protected static double globalPingComputations = 0;

    public static final BooleanComparator TRUE_FIRST_BOOLEAN_COMPARATOR = new BooleanComparator(true);
    public static final BooleanComparator FALSE_FIRST_BOOLEAN_COMPARATOR = new BooleanComparator(false);

    public static void countError() {
        ++errorCounter;
    }

    public static void main(String[] args) {
        BCO.printLogo();
        JPService.setApplicationName("bco-validate");
        JPService.registerProperty(JPDebugMode.class);
        JPService.registerProperty(JPVerbose.class);
        JPService.registerProperty(JPWaitForData.class);
        JPService.registerProperty(JPExitOnError.class);
        JPService.registerProperty(JPComPort.class);
        JPService.registerProperty(JPComHost.class);
        JPService.parseAndExitOnError(args);

        try {
            System.out.println("==================================================");
            System.out.println("BaseCubeOne - System Validator");
            System.out.println("==================================================");
            System.out.println();

            // skip validation if middleware is not ready
            if (!checkMiddleware(Registries.getTemplateRegistry(false))) {
                return;
            }

            // validate registry
            BCORegistryValidator.validateRegistries();
            System.out.println();

            System.out.println("=== " + AnsiColor.colorize("Check Units", AnsiColor.ANSI_BLUE) + " ===\n");
            Future<List<UnitRemote<?>>> futureUnits = Units.getFutureUnits(JPService.getValue(JPWaitForData.class, false));

            System.out.println(StringProcessor.fillWithSpaces("Unit Pool", LABEL_RANGE, Alignment.RIGHT) + "  " + check(futureUnits, DEFAULT_UNIT_POOL_DELAY_TIME));
            System.out.println();

            if (!taskSuccessful(futureUnits)) {
                System.out.println(AnsiColor.colorize("Connection could not be established, please make sure BaseCubeOne is up and running!\n", AnsiColor.ANSI_YELLOW));
                try {
                    futureUnits.get();
                } catch (ExecutionException | CancellationException ex) {
                    ExceptionPrinter.printHistory("Error Details", ex, System.err);
                }
            } else {
                boolean printed = false;
                final List<UnitRemote<?>> unitList = new ArrayList<>(futureUnits.get());
                unitList.sort((unitRemote, t1) -> {
                    try {
                        return TRUE_FIRST_BOOLEAN_COMPARATOR.compare(unitRemote.isDalUnit(), t1.isDalUnit());
                    } catch (CouldNotPerformException ex) {
                        ExceptionPrinter.printHistory("Could not compare unit[" + unitRemote + "] and unit[" + t1 + "]", ex, LOGGER, LogLevel.WARN);
                        return 0;
                    }
                });
                for (final UnitRemote<?> unit : unitList) {
                    printed = check(unit) || printed;
                }
                if (!printed) {
                    System.out.println(StringProcessor.fillWithSpaces("Unit Connections", LABEL_RANGE, Alignment.RIGHT) + "  " + AnsiColor.colorize(OK, AnsiColor.ANSI_GREEN));
                }
            }
        } catch (ExitOnErrorException ex) {
            // just print errors via result print.
        } catch (InterruptedException | CancellationException ex) {
            System.exit(253);
            return;
        } catch (Throwable ex) {
            if (!ExceptionProcessor.isCausedBySystemShutdown(ex)) {
                ExceptionPrinter.printHistory(new CouldNotPerformException("Could not validate system!", ex), System.err);
                System.exit(254);
            }
            System.exit(251);
            return;
        }

        printResult();
    }

    public static void printResult() {

        // print header
        System.out.println();
        System.out.println("==============================================================");
        System.out.print("===  ");

        // print error report
        switch (errorCounter) {
            case 0:
                System.out.print(AnsiColor.colorize("VALIDATION SUCCESSFUL", AnsiColor.ANSI_GREEN));
                break;
            default:
                System.out.print(errorCounter + " " + AnsiColor.colorize("ERROR" + (errorCounter > 1 ? "S" : "") + " DETECTED", AnsiColor.ANSI_RED));
                break;
        }

        // print average ping
        if (getGlobalPing() > 0) {
            System.out.println(" average ping is " + AnsiColor.colorize(pingFormat.format(getGlobalPing()), AnsiColor.ANSI_CYAN) + " milli");
        } else {
            System.out.println();
        }

        // print footer
        System.out.println("==============================================================");
        System.out.println();
        System.exit(Math.min(errorCounter, 200));
    }

    public static boolean checkMiddleware(final AbstractRemoteClient<?> remote) throws CouldNotPerformException, InterruptedException {
        try {
            remote.waitForMiddleware(3, TimeUnit.SECONDS);
            remote.validateMiddleware();
            return true;
        } catch (InvalidStateException | org.openbase.jul.exception.TimeoutException ex) {
            System.out.println();
            System.out.println(AnsiColor.colorize("VALIDATION FAILED", AnsiColor.ANSI_RED) + " Middleware not reachable! Please check your broker connection!");
            ExceptionPrinter.printHistory(ex, LoggerFactory.getLogger(BCOSystemValidator.class), LogLevel.DEBUG);
            System.exit(252);
        }
        return false;
    }

    public static String check(final Future future) throws ExitOnErrorException {
        return check(future, DELAYED_TIME, null);
    }

    public static String check(final Future future, final long delayTime) throws ExitOnErrorException {
        return check(future, delayTime, null);
    }

    public static String check(final Future future, final String suffixCallable) throws ExitOnErrorException {
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
        } finally {
            if (!future.isDone()) {
                future.cancel(true);
            }
        }

        String suffix;
        try {
            suffix = (suffixCallable != null ? suffixCallable.call() : "");
        } catch (Exception e) {
            suffix = "?";
        }

        return AnsiColor.colorize(StringProcessor.fillWithSpaces("OK" + suffix, STATE_RANGE, Alignment.LEFT), AnsiColor.ANSI_GREEN);
    }

    public static boolean check(final Remote<?> remote) throws InterruptedException, ExitOnErrorException {
        return check(remote, DELAYED_TIME);
    }

    public static boolean check(final Remote<?> remote, final long delayTime) throws InterruptedException, ExitOnErrorException {

        try {
            boolean printed = false;

            Future<Long> futurePing = remote.ping();

            try {
                printed |= print(remote, StringProcessor.fillWithSpaces("Ping", LABEL_RANGE, Alignment.RIGHT) + "  " + check(futurePing, delayTime, () -> {
                    if (taskSuccessful(futurePing)) {
                        BCOSystemValidator.computeGlobalPing(futurePing.get());
                        return " (" + futurePing.get() + ")";
                    } else {
                        return "";
                    }
                }));
            } catch (CancellationException ex) {
                throw ex;
            } catch (Exception ex) {
                ExceptionPrinter.printHistory(new FatalImplementationErrorException(BCOSystemValidator.class, ex), LOGGER);
            }

            boolean online = taskSuccessful(futurePing);

            if (online) {
                // ping does not cause the connection state to be connected, this is done by a data update, therefore wait a bit for the connection state
                try {
                    remote.waitForConnectionState(ConnectionState.State.CONNECTED, delayTime);
                } catch (org.openbase.jul.exception.TimeoutException ex) {
                    // just continue and print error for next step
                } catch (CouldNotPerformException ex) {
                    ExceptionPrinter.printHistory(ex, LOGGER);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
                final ConnectionState.State connectionState = remote.getConnectionState();
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
                printed |= print(remote, StringProcessor.fillWithSpaces("Data Cache", LABEL_RANGE, Alignment.RIGHT) + "  " + check(remote.getDataFuture(), delayTime));
            } else {
                printed |= print(remote, StringProcessor.fillWithSpaces("Data Cache", LABEL_RANGE, Alignment.RIGHT) + "  " + (remote.isDataAvailable() ? AnsiColor.colorize(StringProcessor.fillWithSpaces("OFFLINE", STATE_RANGE, Alignment.LEFT), AnsiColor.ANSI_YELLOW) : AnsiColor.colorize(StringProcessor.fillWithSpaces("EMPTY", STATE_RANGE, Alignment.LEFT), AnsiColor.ANSI_RED)));
            }

            if (online) {
                printed |= print(remote, StringProcessor.fillWithSpaces("Synchronization", LABEL_RANGE, Alignment.RIGHT) + "  " + check(remote.requestData(), delayTime));
            }

            // add new line separator for better overview
            if (printed) {
                System.out.println();
            }

            return printed;
        } finally {
            if (JPService.getValue(JPExitOnError.class, false) && errorCounter > 0) {
                throw new ExitOnErrorException("Error occured!");
            }
        }
    }

    public static boolean taskSuccessful(final Future<?> future) throws InterruptedException {
        try {
            if (future.isDone() && !future.isCancelled()) {
                future.get();
                return true;
            }
        } catch (ExecutionException | CancellationException ex) {
            // ping failed
        }
        return false;
    }

    public static boolean print(final Remote remote, final String result) {
        // only print in error case or if verbose mode is enabled.
        if (!result.contains(OK) || JPService.verboseMode()) {
            System.out.println(result + "[" + remote + "]");
            return true;
        }
        return false;
    }

    public synchronized static double computeGlobalPing(long ping) {

        // skip 0 ping.
        if (ping <= 0) {
            return globalPingAverage;
        }
        globalPingAverage = (globalPingComputations * globalPingAverage + ping) / (globalPingComputations + 1);
        globalPingComputations++;
        return globalPingAverage;
    }

    protected static double getGlobalPing() {
        return globalPingAverage;
    }

    public static class ExitOnErrorException extends InvalidStateException {

        public ExitOnErrorException(String message) {
            super(message);
        }

        public ExitOnErrorException(String message, Throwable cause) {
            super(message, cause);
        }

        public ExitOnErrorException(Throwable cause) {
            super(cause);
        }

        public ExitOnErrorException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
            super(message, cause, enableSuppression, writableStackTrace);
        }
    }

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

        AnsiColor(String color) {
            this.color = color;
        }

        public static String colorize(final String text, final AnsiColor color) {
            return color.getColor() + text + ANSI_RESET.getColor();
        }

        public static String colorizeRegex(final String text, final String regex, final AnsiColor color) {
            return text.replaceAll(regex, colorize(regex, color));
        }

        public String getColor() {
            return color;
        }
    }
}
