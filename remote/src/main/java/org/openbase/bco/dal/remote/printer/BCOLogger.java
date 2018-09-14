package org.openbase.bco.dal.remote.printer;

/*-
 * #%L
 * BCO DAL Remote
 * %%
 * Copyright (C) 2014 - 2018 openbase.org
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

import org.openbase.bco.dal.remote.DALRemote;
import org.openbase.jps.core.JPService;
import org.openbase.jul.exception.InitializationException;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.exception.printer.LogLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BCOLogger extends UnitStatePrinter {

    private static final Logger LOGGER = LoggerFactory.getLogger(DALRemote.class);

    public static final String APP_NAME = BCOLogger.class.getSimpleName();

    public BCOLogger() throws InstantiationException {
        super(System.out);
    }

    public static void main(String[] args) throws InstantiationException, InterruptedException, InitializationException {

        LOGGER.info("Start " + APP_NAME + "...");

        /* Setup JPService */
        JPService.setApplicationName(APP_NAME);

        JPService.parseAndExitOnError(args);

        try {
            new BCOLogger().init();
        } catch (InitializationException ex) {
            throw ExceptionPrinter.printHistoryAndReturnThrowable(ex, LOGGER, LogLevel.ERROR);
        }
        LOGGER.info(APP_NAME + " successfully started.");
    }
}
