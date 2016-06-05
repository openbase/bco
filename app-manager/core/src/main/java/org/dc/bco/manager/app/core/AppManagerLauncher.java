package org.dc.bco.manager.app.core;

import org.dc.bco.manager.app.lib.AppManager;
import org.dc.jps.core.JPService;
import org.dc.jul.exception.CouldNotPerformException;
import org.dc.jul.exception.printer.ExceptionPrinter;
import org.dc.jul.exception.printer.LogLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
 * #%L
 * COMA AppManager Core
 * %%
 * Copyright (C) 2015 - 2016 DivineCooperation
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
/**
 *
 * @author <a href="mailto:mpohling@cit-ec.uni-bielefeld.de">Divine
 * Threepwood</a>
 */
public class AppManagerLauncher {

    protected static final Logger logger = LoggerFactory.getLogger(AppManagerLauncher.class);

    private final AppManagerController appManagerController;

    public AppManagerLauncher() throws org.dc.jul.exception.InstantiationException, InterruptedException {
        try {
            this.appManagerController = new AppManagerController();
        } catch (CouldNotPerformException ex) {
            throw new org.dc.jul.exception.InstantiationException(this, ex);
        }
    }

    public void launch() throws org.dc.jul.exception.InstantiationException, InterruptedException {
        try {
            appManagerController.init();
        } catch (CouldNotPerformException ex) {
            appManagerController.shutdown();
            throw new org.dc.jul.exception.InstantiationException(this, ex);
        }
    }

    public void shutdown() {
        appManagerController.shutdown();
    }

    /**
     * @param args the command line arguments
     * @throws java.lang.InterruptedException
     * @throws org.dc.jul.exception.CouldNotPerformException
     */
    public static void main(final String[] args) throws InterruptedException, CouldNotPerformException {

        /* Setup JPService */
        JPService.setApplicationName(AppManager.class);
        JPService.parseAndExitOnError(args);

        /* Start main app */
        logger.info("Start " + JPService.getApplicationName() + "...");
        try {
            new AppManagerLauncher().launch();
        } catch (CouldNotPerformException ex) {
            throw ExceptionPrinter.printHistoryAndReturnThrowable(ex, logger, LogLevel.ERROR);
        }
        logger.info(JPService.getApplicationName() + " successfully started.");
    }
}
