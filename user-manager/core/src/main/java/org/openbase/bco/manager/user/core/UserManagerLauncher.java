package org.openbase.bco.manager.user.core;

/*
 * #%L
 * COMA UserManager Core
 * %%
 * Copyright (C) 2015 - 2016 openbase.org
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

import org.openbase.bco.manager.user.lib.UserManager;
import org.openbase.jps.core.JPService;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.exception.printer.LogLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author mpohling
 */
public class UserManagerLauncher {

    protected static final Logger logger = LoggerFactory.getLogger(UserManagerLauncher.class);

    private final UserManagerController userManagerController;

    public UserManagerLauncher() throws InstantiationException, InterruptedException {
        try {
            this.userManagerController = new UserManagerController();
        } catch (CouldNotPerformException ex) {
            throw new InstantiationException(this, ex);
        }
    }

    public void launch() throws org.openbase.jul.exception.InstantiationException, InterruptedException {
        try {
            userManagerController.init();
        } catch (CouldNotPerformException ex) {
            userManagerController.shutdown();
            throw new org.openbase.jul.exception.InstantiationException(this, ex);
        }
    }

    public void shutdown() {
        userManagerController.shutdown();
    }

    /**
     * @param args the command line arguments
     * @throws java.lang.InterruptedException
     * @throws org.openbase.jul.exception.CouldNotPerformException
     */
    public static void main(final String[] args) throws InterruptedException, CouldNotPerformException {

        /* Setup JPService */
        JPService.setApplicationName(UserManager.class);
        JPService.parseAndExitOnError(args);

        /* Start main app */
        logger.info("Start " + JPService.getApplicationName() + "...");
        try {
            new UserManagerLauncher().launch();
        } catch (CouldNotPerformException ex) {
            throw ExceptionPrinter.printHistoryAndReturnThrowable(ex, logger, LogLevel.ERROR);
        }
        logger.info(JPService.getApplicationName() + " successfully started.");
    }
}
