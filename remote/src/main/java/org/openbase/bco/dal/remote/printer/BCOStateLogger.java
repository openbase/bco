package org.openbase.bco.dal.remote.printer;

import org.openbase.bco.dal.lib.jp.JPRemoteMethod;
import org.openbase.bco.dal.lib.jp.JPRemoteMethodParameters;
import org.openbase.bco.dal.lib.jp.JPRemoteService;
import org.openbase.bco.dal.remote.DALRemote;
import org.openbase.jps.core.JPService;
import org.openbase.jps.preset.JPDebugMode;
import org.openbase.jul.exception.InitializationException;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.exception.printer.LogLevel;
import org.openbase.jul.extension.rsb.scope.jp.JPScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BCOStateLogger extends UnitStatePrinter {

    private static final Logger LOGGER = LoggerFactory.getLogger(DALRemote.class);

    public static final String APP_NAME = DALRemote.class.getSimpleName();

    public BCOStateLogger() throws InstantiationException {
        super(System.out);
    }

    public static void main(String[] args) throws InstantiationException, InterruptedException, InitializationException {

        LOGGER.info("Start " + APP_NAME + "...");

        /* Setup JPService */
        JPService.setApplicationName(APP_NAME);

        JPService.parseAndExitOnError(args);

        try {
            new BCOStateLogger().init();
        } catch (InitializationException ex) {
            throw ExceptionPrinter.printHistoryAndReturnThrowable(ex, LOGGER, LogLevel.ERROR);
        }
        LOGGER.info(APP_NAME + " successfully started.");
    }
}
