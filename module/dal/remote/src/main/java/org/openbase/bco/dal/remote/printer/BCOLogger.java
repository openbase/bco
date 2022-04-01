package org.openbase.bco.dal.remote.printer;

/*-
 * #%L
 * BCO DAL Remote
 * %%
 * Copyright (C) 2014 - 2021 openbase.org
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-3.0.html>.
 * #L%
 */

import org.openbase.bco.authentication.lib.jp.JPCredentialsDirectory;
import org.openbase.bco.dal.remote.printer.jp.*;
import org.openbase.bco.registry.remote.login.BCOLogin;
import org.openbase.jps.core.JPService;
import org.openbase.jps.exception.JPNotAvailableException;
import org.openbase.jps.preset.JPDebugMode;
import org.openbase.jul.communication.jp.JPComHost;
import org.openbase.jul.communication.jp.JPComPort;
import org.openbase.jul.exception.*;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.exception.printer.LogLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;

public class BCOLogger extends UnitStatePrinter {

    public static final String APP_NAME = BCOLogger.class.getSimpleName();
    private static final Logger LOGGER = LoggerFactory.getLogger(BCOLogger.class);

    public BCOLogger() throws InstantiationException {
        super(getTransitionPrintStream(),
                new Config()
                        .setPrintInitialStates(JPService.getValue(JPPrintInitialServiceStates.class, false))
                        .setSkipUnknownValues(JPService.getValue(JPSkipUnknownStates.class, false)));

        if(JPService.getValue(JPPrintModel.class, false)) {
            UnitModelPrinter.printStaticRelations(getModelPrintStream());
        }
    }

    public static void main(String[] args) throws CouldNotPerformException, InterruptedException, InitializationException {

        /* Setup JPService */
        JPService.setApplicationName(APP_NAME);
        JPService.registerProperty(JPLogFormat.class);
        JPService.registerProperty(JPPrintModel.class);
        JPService.registerProperty(JPPrintInitialServiceStates.class);
        JPService.registerProperty(JPSkipUnknownStates.class);
        JPService.registerProperty(JPAppendLog.class);
        JPService.registerProperty(JPOutputDirectory.class);
        JPService.registerProperty(JPDebugMode.class);
        JPService.registerProperty(JPCredentialsDirectory.class);
        JPService.registerProperty(JPComHost.class);
        JPService.registerProperty(JPComPort.class);

        try {
            JPService.parseAndExitOnError(args);
        } catch (IllegalStateException ex) {
            ExceptionPrinter.printHistory(ex, LOGGER, LogLevel.ERROR);
            LOGGER.info(APP_NAME + " finished unexpected.");
        }

        LOGGER.info("Start " + APP_NAME + "...");

        try {
            BCOLogin.getSession().autoLogin(true);
            final BCOLogger bcoLogger = new BCOLogger();
            bcoLogger.init();
            bcoLogger.activate();
            LOGGER.info(APP_NAME + " successfully started.");
        } catch (CouldNotPerformException ex) {
            if (!ExceptionProcessor.isCausedBySystemShutdown(ex)) {
                throw ExceptionPrinter.printHistoryAndReturnThrowable(ex, LOGGER, LogLevel.ERROR);
            }
        }
    }

    private static PrintStream getModelPrintStream() {
        try {
            if (!JPService.getProperty(JPOutputDirectory.class).isParsed()) {
                return System.out;
            }
            final Boolean append = JPService.getValue(JPAppendLog.class, true);
            File logfile = new File(JPService.getProperty(JPOutputDirectory.class).getValue(), "bco-model.pl");
            if (!logfile.exists()) {
                logfile.createNewFile();
            }
            return new PrintStream(new FileOutputStream(logfile, append));
        } catch (IOException | JPNotAvailableException ex) {
            ExceptionPrinter.printHistory("Error while loading model file, use system out instead.", ex, LOGGER);
            return System.out;
        }
    }

    private static PrintStream getTransitionPrintStream() {
        try {
            if (!JPService.getProperty(JPOutputDirectory.class).isParsed()) {
                return System.out;
            }
            final Boolean append = JPService.getValue(JPAppendLog.class, true);
            File logfile = new File(JPService.getProperty(JPOutputDirectory.class).getValue(), "transitions.pl");
            if (!logfile.exists()) {
                logfile.createNewFile();
            }
            return new PrintStream(new FileOutputStream(logfile, append));
        } catch (IOException | JPNotAvailableException ex) {
            ExceptionPrinter.printHistory("Error while loading transitions file, use system out instead.", ex, LOGGER);
            return System.out;
        }
    }
}
