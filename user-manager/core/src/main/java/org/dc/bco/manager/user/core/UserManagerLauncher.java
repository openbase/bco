/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dc.bco.manager.user.core;

import org.dc.bco.manager.user.lib.UserManager;
import org.dc.jps.core.JPService;
import org.dc.jul.exception.CouldNotPerformException;
import org.dc.jul.exception.InstantiationException;
import org.dc.jul.exception.printer.ExceptionPrinter;
import org.dc.jul.exception.printer.LogLevel;
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

    public void launch() throws org.dc.jul.exception.InstantiationException, InterruptedException {
        try {
            userManagerController.init();
        } catch (CouldNotPerformException ex) {
            userManagerController.shutdown();
            throw new org.dc.jul.exception.InstantiationException(this, ex);
        }
    }

    public void shutdown() {
        userManagerController.shutdown();
    }

    /**
     * @param args the command line arguments
     * @throws java.lang.InterruptedException
     * @throws org.dc.jul.exception.CouldNotPerformException
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
